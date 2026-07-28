package com.aliothmoon.maameow.schedule.model

import android.content.Context
import android.content.Intent
import com.aliothmoon.maameow.MainActivity
import java.util.UUID

data class ScheduledExecutionRequest(
    val requestId: String = UUID.randomUUID().toString(),
    val strategyId: String,
    val strategyName: String,
    /** 用户 Profile；[useSequence]=false 时使用 */
    val profileId: String = "",
    /** 任务链配置；[useSequence]=true 时使用 */
    val sequenceConfigId: String = "",
    /** true：按任务链配置拼接执行；false：跑单个用户配置 */
    val useSequence: Boolean = false,
    val scheduledTimeMs: Long,
    val forceStart: Boolean = false,
) {
    companion object {
        const val ACTION_SHOW_SCHEDULE_EXECUTION =
            "com.maa.maameow.action.SHOW_SCHEDULE_EXECUTION"
        const val ACTION_LAUNCH_PROFILE =
            "com.maa.maameow.action.LAUNCH_PROFILE"
        const val EXTRA_REQUEST_ID = "extra_request_id"
        const val EXTRA_STRATEGY_ID = "extra_strategy_id"
        const val EXTRA_STRATEGY_NAME = "extra_strategy_name"
        const val EXTRA_PROFILE_ID = "extra_profile_id"
        const val EXTRA_SEQUENCE_CONFIG_ID = "extra_sequence_config_id"
        const val EXTRA_USE_SEQUENCE = "extra_use_sequence"
        const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
        const val EXTRA_FORCE_START = "extra_force_start"
        const val COUNTDOWN_SECONDS = 30

        fun fromIntent(intent: Intent?): ScheduledExecutionRequest? {
            if (intent?.action != ACTION_SHOW_SCHEDULE_EXECUTION) {
                return null
            }
            val requestId = intent.getStringExtra(EXTRA_REQUEST_ID) ?: return null
            val strategyId = intent.getStringExtra(EXTRA_STRATEGY_ID) ?: return null
            val strategyName = intent.getStringExtra(EXTRA_STRATEGY_NAME) ?: return null
            val useSequence = intent.getBooleanExtra(EXTRA_USE_SEQUENCE, false)
            val profileId = intent.getStringExtra(EXTRA_PROFILE_ID).orEmpty()
            val sequenceConfigId = intent.getStringExtra(EXTRA_SEQUENCE_CONFIG_ID).orEmpty()
            if (useSequence) {
                if (sequenceConfigId.isEmpty()) return null
            } else if (profileId.isEmpty()) {
                return null
            }
            val scheduledTimeMs = intent.getLongExtra(EXTRA_SCHEDULED_TIME, 0L)
            if (scheduledTimeMs <= 0L) {
                return null
            }
            return ScheduledExecutionRequest(
                requestId = requestId,
                strategyId = strategyId,
                strategyName = strategyName,
                profileId = profileId,
                sequenceConfigId = sequenceConfigId,
                useSequence = useSequence,
                scheduledTimeMs = scheduledTimeMs,
                forceStart = intent.getBooleanExtra(EXTRA_FORCE_START, false),
            )
        }

        fun fromExternalIntent(
            intent: Intent?,
            strategyName: String,
        ): ScheduledExecutionRequest? {
            if (intent?.action != ACTION_LAUNCH_PROFILE) return null
            val profileId = intent.getStringExtra(EXTRA_PROFILE_ID) ?: return null
            return ScheduledExecutionRequest(
                requestId = UUID.randomUUID().toString(),
                strategyId = "",
                strategyName = strategyName,
                profileId = profileId,
                useSequence = false,
                scheduledTimeMs = System.currentTimeMillis(),
                forceStart = intent.getBooleanExtra(EXTRA_FORCE_START, false),
            )
        }
    }

    fun toLaunchIntent(context: Context): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = ACTION_SHOW_SCHEDULE_EXECUTION
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_REQUEST_ID, requestId)
            putExtra(EXTRA_STRATEGY_ID, strategyId)
            putExtra(EXTRA_STRATEGY_NAME, strategyName)
            putExtra(EXTRA_PROFILE_ID, profileId)
            putExtra(EXTRA_SEQUENCE_CONFIG_ID, sequenceConfigId)
            putExtra(EXTRA_USE_SEQUENCE, useSequence)
            putExtra(EXTRA_SCHEDULED_TIME, scheduledTimeMs)
            putExtra(EXTRA_FORCE_START, forceStart)
        }
    }
}