package com.aliothmoon.maameow.koin

import com.aliothmoon.maameow.ui.viewmodel.BackgroundTaskViewModel
import com.aliothmoon.maameow.ui.viewmodel.CopilotViewModel
import com.aliothmoon.maameow.ui.viewmodel.ErrorLogViewModel
import com.aliothmoon.maameow.ui.viewmodel.ExpandedControlPanelViewModel
import com.aliothmoon.maameow.ui.viewmodel.HomeViewModel
import com.aliothmoon.maameow.ui.viewmodel.LogHistoryViewModel
import com.aliothmoon.maameow.ui.viewmodel.TaskOverrideEditorViewModel
import com.aliothmoon.maameow.ui.viewmodel.ToolboxViewModel
import com.aliothmoon.maameow.ui.viewmodel.SettingsViewModel
import com.aliothmoon.maameow.ui.viewmodel.NotificationSettingsViewModel
import com.aliothmoon.maameow.ui.viewmodel.UpdateViewModel
import com.aliothmoon.maameow.schedule.ui.ScheduleEditViewModel
import com.aliothmoon.maameow.schedule.ui.ScheduleListViewModel
import com.aliothmoon.maameow.schedule.ui.ScheduleTriggerLogViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module


val viewModelModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::UpdateViewModel)
    viewModelOf(::LogHistoryViewModel)
    viewModelOf(::ErrorLogViewModel)
    viewModelOf(::BackgroundTaskViewModel)
    viewModelOf(::ScheduleListViewModel)
    viewModelOf(::ScheduleEditViewModel)
    viewModelOf(::ScheduleTriggerLogViewModel)
    viewModelOf(::NotificationSettingsViewModel)
    viewModelOf(::TaskOverrideEditorViewModel)
}


val floatingWindowModule = module {
    singleOf(::ExpandedControlPanelViewModel)
    singleOf(::CopilotViewModel)
    singleOf(::ToolboxViewModel)
}
