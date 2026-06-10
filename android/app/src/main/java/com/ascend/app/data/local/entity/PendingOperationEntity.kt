package com.ascend.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A write that failed because the device was offline, queued for replay by
 * [com.ascend.app.workers.SyncWorker] when connectivity returns.
 */
@Entity(tableName = "pending_operations")
data class PendingOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** One of [TYPE_COMPLETE_QUEST], [TYPE_SKIP_QUEST], [TYPE_COMPLETE_HABIT]. */
    val type: String,
    /** Quest or habit id the operation applies to. */
    val targetId: String,
    val createdAt: Long,
    val attempts: Int = 0
) {
    companion object {
        const val TYPE_COMPLETE_QUEST = "complete_quest"
        const val TYPE_SKIP_QUEST = "skip_quest"
        const val TYPE_COMPLETE_HABIT = "complete_habit"

        /** Drop an op after this many failed replay attempts. */
        const val MAX_ATTEMPTS = 10
    }
}
