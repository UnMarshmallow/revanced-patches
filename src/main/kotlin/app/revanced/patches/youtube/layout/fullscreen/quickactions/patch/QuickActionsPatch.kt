package app.revanced.patches.youtube.layout.fullscreen.quickactions.patch

import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.ResourceContext
import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.instruction
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.ResourcePatch
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.smali.ExternalLabel
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.youtube.misc.litho.patch.LithoFilterPatch
import app.revanced.patches.youtube.misc.playertype.patch.PlayerTypeHookPatch
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.FULLSCREEN

@Patch
@Name("hide-quick-actions")
@Description("Adds the options to hide quick actions components in the fullscreen.")
@DependsOn(
    [
        LithoFilterPatch::class,
        PlayerTypeHookPatch::class,
        SettingsPatch::class
    ]
)
@YouTubeCompatibility
@Version("0.0.1")
class QuickActionsPatch : ResourcePatch {
    override fun execute(context: ResourceContext): PatchResult {

        val instructionList =
            """
                move-object/from16 v3, p2
                invoke-static {v3, v10}, $FULLSCREEN->hideQuickActionButtons(Ljava/lang/Object;Ljava/nio/ByteBuffer;)Z
                move-result v10
                if-eqz v10, :do_not_block
                move-object/from16 v15, p1
                invoke-static {v15}, ${LithoFilterPatch.builderMethodDescriptor}
                move-result-object v0
                iget-object v0, v0, ${LithoFilterPatch.emptyComponentFieldDescriptor}
                return-object v0
                """

        with(LithoFilterPatch.lithoMethod) {
            addInstructions(
                0, """
                        move-object/from16 v10, p3
                        iget-object v10, v10, ${LithoFilterPatch.objectReference.definingClass}->${LithoFilterPatch.objectReference.name}:${LithoFilterPatch.objectReference.type}
                        if-eqz v10, :do_not_block
                        check-cast v10, ${LithoFilterPatch.bufferReference}
                        iget-object v10, v10, ${LithoFilterPatch.bufferReference}->b:Ljava/nio/ByteBuffer;
                        """ + instructionList,listOf(ExternalLabel("do_not_block", LithoFilterPatch.lithoMethod.instruction(0)))
            )
        }

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: BOTTOM_PLAYER_SETTINGS",
                "SETTINGS: HIDE_QUICK_ACTIONS"
            )
        )

        SettingsPatch.updatePatchStatus("hide-quick-actions")

        return PatchResultSuccess()
    }
}