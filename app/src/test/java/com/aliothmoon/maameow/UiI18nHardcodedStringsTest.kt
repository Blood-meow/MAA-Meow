package com.aliothmoon.maameow

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class UiI18nHardcodedStringsTest {

    @Test
    fun navigationAndPanelBatch_doesNotContainUnexpectedHardcodedChineseStringLiterals() {
        val failures = TARGETS.mapNotNull { target ->
            val file = resolveSourceFile(target.relativePath)
            val literals = HARD_CODED_CHINESE_LITERAL.findAll(file.readText())
                .map { it.value.trim('"') }
                .filterNot { it in target.allowedLiterals }
                .toList()
            if (literals.isEmpty()) null else "${target.relativePath}: ${literals.joinToString()}"
        }

        assertTrue(
            "Unexpected hardcoded Chinese string literals remain:\n${failures.joinToString("\n")}",
            failures.isEmpty()
        )
    }

    private fun resolveSourceFile(relativePath: String): File {
        val candidates = listOf(
            File(relativePath),
            File("app/$relativePath"),
            File("../app/$relativePath"),
        )
        val file = candidates.firstOrNull { it.isFile }
        checkNotNull(file) { "Source file not found for test: $relativePath" }
        return file
    }

    private data class TargetFile(
        val relativePath: String,
        val allowedLiterals: Set<String> = emptySet(),
    )

    companion object {
        private val HARD_CODED_CHINESE_LITERAL = Regex("\"[^\"\\n]*\\p{IsHan}[^\"\\n]*\"")

        private val TARGETS = listOf(
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/navigation/BottomNavigation.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/navigation/AppNavigation.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/screen/panel/FloatingPanelState.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/screen/panel/PanelHeader.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/screen/panel/ToolboxPanel.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/viewmodel/ToolboxViewModel.kt"),
            TargetFile(
"src/main/java/com/aliothmoon/maameow/ui/viewmodel/MiniGameDelegate.kt",
                allowedLiterals = setOf("不选择", "支援作战平台", "游侠", "诡影迷踪"),
            ),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/screen/panel/AwardConfigPanel.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/screen/panel/DepotRecognitionPanel.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/screen/panel/MiniGamePanel.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/screen/panel/TaskListPanel.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/screen/panel/LogPanel.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/screen/panel/RecruitCalcPanel.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/screen/panel/WakeUpConfigPanel.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/screen/panel/fight/MedicineAndStoneSection.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/screen/panel/fight/SpecifiedDropsSection.kt"),
            TargetFile(
                "src/main/java/com/aliothmoon/maameow/ui/screen/panel/fight/TodayStagesHint.kt",
                allowedLiterals = setOf("常驻关卡", "资源收集"),
            ),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/screen/panel/mall/AdvancedOptionsSection.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/screen/panel/mall/PriorityItemRow.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/screen/panel/mall/BlacklistItemRow.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/screen/panel/mall/ReorderablePriorityList.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/screen/panel/roguelike/RoguelikeConfigPanel.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/screen/panel/roguelike/AdvancedRoguelikeSettings.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/screen/panel/roguelike/ModeSpecificSettings.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/screen/panel/roguelike/ThemeSpecificSettings.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/schedule/ui/ScheduleListView.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/schedule/ui/ScheduleTriggerLogView.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/schedule/ui/ScheduleEditView.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/schedule/ui/ScheduleEditViewModel.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/screen/notification/NotificationSettingsView.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/component/ResourceInitDialog.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/component/UpdateConfirmDialog.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/component/TopAppBar.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/component/ShizukuPermissionDialog.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/component/tip/ExpandableTipIcon.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/component/AdaptiveTaskPromptDialog.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/component/OverlayDialog.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/component/PanelComponents.kt"),
            TargetFile(
                "src/main/java/com/aliothmoon/maameow/ui/component/CoreCharSelector.kt",
                allowedLiterals = setOf(
                    "[CoreCharSelector] 空字符串，设置 isValid=true",
                    "[CoreCharSelector] 更新配置为空字符串",
                    "[CoreCharSelector] 开始校验: '${'$'}newValue'",
                    "[CoreCharSelector] 开始校验: isValidCharacterName",
                    "[CoreCharSelector] 校验结果: validationResult=${'$'}validationResult, newValue='${'$'}newValue'",
                    "[CoreCharSelector] 建议列表计算完成: ${'$'}{newSuggestions.size} 个结果",
                    "[CoreCharSelector] 输入已变化，跳过此次校验结果: 当前='${'$'}inputText', 校验='${'$'}newValue'",
                    "[CoreCharSelector] UI更新完成: isValid=${'$'}isValid, isValidating=${'$'}isValidating",
                    "[CoreCharSelector] 校验通过，更新配置: '${'$'}newValue'",
                    "[CoreCharSelector] 校验失败，不更新配置。当前配置值保持: '${'$'}value'",
                    "[CoreCharSelector] 渲染状态: inputText='${'$'}inputText', isValid=${'$'}isValid, isValidating=${'$'}isValidating, showError=${'$'}{!isValid && !isValidating && inputText.isNotBlank()}",
                ),
            ),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/screen/background/VirtualDisplayPreview.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/schedule/ui/CountdownDialog.kt"),
            TargetFile(
                "src/main/java/com/aliothmoon/maameow/ui/viewmodel/UpdateViewModel.kt",
                allowedLiterals = setOf(
                    "读取资源版本失败",
                    "当前资源版本: ${'$'}currentVersion, 下载源: ${'$'}{updateSource.value}",
                    "检查 App 更新 (MirrorChyan)",
                    "确认下载 App 更新: version=${'$'}version",
                ),
            ),
TargetFile("src/main/java/com/aliothmoon/maameow/ui/viewmodel/TaskStartUiHelpers.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/viewmodel/ExpandedControlPanelViewModel.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/viewmodel/BackgroundTaskViewModel.kt"),
            TargetFile(
                "src/main/java/com/aliothmoon/maameow/ui/viewmodel/CopilotViewModel.kt",
                allowedLiterals = setOf(
                    "${'$'}TAG: 解析本地文件失败: ${'$'}fileName",
                ),
            ),
            TargetFile("src/main/java/com/aliothmoon/maameow/ui/state/HomeUiState.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/domain/models/RunMode.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/domain/models/OverlayControlMode.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/domain/models/RemoteBackend.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/data/model/update/UpdateSource.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/data/model/update/UpdateChannel.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/data/model/WakeUpConfig.kt"),
            TargetFile(
                "src/main/java/com/aliothmoon/maameow/domain/service/MaaResourceLoader.kt",
                // 仅经 Exception/日志流转的诊断文案，UI 层（HomeViewModel/TaskStartUiHelpers）均映射为资源串
                allowedLiterals = setOf("资源未就绪，请重新初始化"),
            ),
            TargetFile("src/main/java/com/aliothmoon/maameow/domain/service/MaaEventNotifier.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/domain/usecase/PrepareTaskStartUseCase.kt"),
            TargetFile("src/main/java/com/aliothmoon/maameow/domain/usecase/AnalyzeTaskChainUseCase.kt"),
        )
    }
}
