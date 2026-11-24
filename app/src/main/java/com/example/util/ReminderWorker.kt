package com.example.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.R
import com.example.data.model.AgencyTask
import com.example.data.model.Invoice
import com.example.data.model.InvoiceStatus
import com.example.data.model.Lead
import com.example.data.model.LeadStatus
import com.example.data.model.Project
import com.example.data.model.ProjectStatus
import com.example.data.model.TaskStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val uid = Firebase.auth.currentUser?.uid ?: return Result.success()
        val dbId = applicationContext.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(dbId)
        val now = System.currentTimeMillis()
        val upcomingWindow = now + TimeUnit.HOURS.toMillis(48)

        return try {
            val userDoc = db.collection("users").document(uid)
            val projects = userDoc.collection("projects").get().await()
                .toObjects(Project::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .filter { it.statusEnum() != ProjectStatus.COMPLETED && it.deadlineMillis in 1..upcomingWindow }

            val tasks = userDoc.collection("tasks").get().await()
                .toObjects(AgencyTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .filter { it.statusEnum() != TaskStatus.COMPLETED && it.dueDateMillis in 1..upcomingWindow }

            val invoices = userDoc.collection("invoices").get().await()
                .toObjects(Invoice::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .filter { it.statusEnum() != InvoiceStatus.PAID && it.dueDateMillis in 1..upcomingWindow }

            val leads = userDoc.collection("leads").get().await()
                .toObjects(Lead::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .filter {
                    val s = it.statusEnum()
                    s != LeadStatus.CONVERTED && s != LeadStatus.LOST &&
                        (it.followUpDateMillis ?: 0L) in 1..upcomingWindow
                }

            val alerts = mutableListOf<String>()
            if (projects.isNotEmpty()) alerts.add("${projects.size} project deadline(s) soon")
            if (tasks.isNotEmpty()) alerts.add("${tasks.size} pending task(s) due")
            if (invoices.isNotEmpty()) alerts.add("${invoices.size} unpaid invoice(s) due")
            if (leads.isNotEmpty()) alerts.add("${leads.size} lead follow-up(s) scheduled")

            if (alerts.isNotEmpty()) {
                NotificationHelper.showReminderNotification(
                    context = applicationContext,
                    notificationId = 1001,
                    title = "Rajeev Digital • Daily Agency Digest",
                    body = alerts.joinToString(" • ")
                )
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "rajeev_digital_reminders_periodic"

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(12, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<ReminderWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
