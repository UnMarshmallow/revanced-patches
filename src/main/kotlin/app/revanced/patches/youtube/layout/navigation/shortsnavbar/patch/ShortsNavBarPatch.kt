package app.revanced.patches.youtube.layout.navigation.shortsnavbar.patch

import app.revanced.extensions.toErrorResult
import app.revanced.patcher.annotation.Description
import app.revanced.patcher.annotation.Name
import app.revanced.patcher.annotation.Version
import app.revanced.patcher.data.BytecodeContext
import app.revanced.patcher.data.toMethodWalker
import app.revanced.patcher.extensions.addInstruction
import app.revanced.patcher.fingerprint.method.impl.MethodFingerprint.Companion.resolve
import app.revanced.patcher.patch.BytecodePatch
import app.revanced.patcher.patch.PatchResult
import app.revanced.patcher.patch.PatchResultSuccess
import app.revanced.patcher.patch.annotations.DependsOn
import app.revanced.patcher.patch.annotations.Patch
import app.revanced.patcher.util.proxy.mutableTypes.MutableMethod
import app.revanced.patches.shared.annotation.YouTubeCompatibility
import app.revanced.patches.shared.fingerprints.PivotBarCreateButtonViewFingerprint
import app.revanced.patches.youtube.layout.navigation.shortsnavbar.fingerprints.*
import app.revanced.patches.youtube.misc.settings.resource.patch.SettingsPatch
import app.revanced.util.integrations.Constants.NAVIGATION
import org.jf.dexlib2.dexbacked.reference.DexBackedTypeReference
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction
import org.jf.dexlib2.iface.instruction.ReferenceInstruction

@Patch
@Name("hide-shorts-navbar")
@Description("Hide navigation bar when playing shorts.")
@DependsOn([SettingsPatch::class])
@YouTubeCompatibility
@Version("0.0.1")
class ShortsNavBarPatch : BytecodePatch(
    listOf(
        PivotBarCreateButtonViewFingerprint,
        ReelWatchBundleFingerprint,
        ReelWatchEndpointParentFingerprint
    )
) {
    override fun execute(context: BytecodeContext): PatchResult {

        PivotBarCreateButtonViewFingerprint.result?.let { parentResult ->
            SetPivotBarFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.let {
                with (it.mutableMethod) {
                    val startIndex = it.scanResult.patternScanResult!!.startIndex
                    val instructions = implementation!!.instructions

                    val indexReference = ((instructions[startIndex] as ReferenceInstruction).reference as DexBackedTypeReference).toString()
                    if (indexReference != targetReference) return SetPivotBarFingerprint.toErrorResult()
                    val register = (instructions[startIndex] as OneRegisterInstruction).registerA

                    addInstruction(
                        startIndex + 1,
                        "sput-object v$register, $NAVIGATION->pivotbar:$targetReference"
                    )
                }
            } ?: return SetPivotBarFingerprint.toErrorResult()
        } ?: return PivotBarCreateButtonViewFingerprint.toErrorResult()

        ReelWatchBundleFingerprint.result?.let {
            with (context
                .toMethodWalker(it.method)
                .nextMethod(it.scanResult.patternScanResult!!.endIndex, true)
                .getMethod() as MutableMethod
            ) {
                addInstruction(
                    0,
                    "invoke-static {}, $NAVIGATION->hideShortsPlayerNavBar()V"
                )
            }
        } ?: return ReelWatchBundleFingerprint.toErrorResult()

        ReelWatchEndpointParentFingerprint.result?.let { parentResult ->
            ReelWatchEndpointFingerprint.also { it.resolve(context, parentResult.classDef) }.result?.mutableMethod?.addInstruction(
                0,
                "sput-object p1, $NAVIGATION->shortsContext:Landroid/content/Context;"
            ) ?: return ReelWatchEndpointFingerprint.toErrorResult()
        } ?: return ReelWatchEndpointParentFingerprint.toErrorResult()

        /*
         * Add settings
         */
        SettingsPatch.addPreference(
            arrayOf(
                "PREFERENCE: NAVIGATION_SETTINGS",
                "SETTINGS: HIDE_SHORTS_NAVIGATION_BAR"
            )
        )

        SettingsPatch.updatePatchStatus("hide-shorts-navbar")

        return PatchResultSuccess()
    }

    private companion object {
        const val targetReference =
            "Lcom/google/android/apps/youtube/app/ui/pivotbar/PivotBar;"
    }
}