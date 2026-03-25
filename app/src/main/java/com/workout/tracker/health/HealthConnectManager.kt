package com.workout.tracker.health

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Mass
import com.workout.tracker.data.entity.WorkoutType
import java.time.Instant
import java.time.ZoneOffset

class HealthConnectManager(private val context: Context) {

    companion object {
        private const val TAG = "HealthConnectManager"

        val PERMISSIONS = setOf(
            HealthPermission.getWritePermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getWritePermission(WeightRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class)
        )

        fun isAvailable(context: Context): Boolean {
            return try {
                val status = HealthConnectClient.getSdkStatus(context)
                status == HealthConnectClient.SDK_AVAILABLE
            } catch (e: Exception) {
                false
            }
        }

        fun getInstallIntent(): Intent {
            return Intent(Intent.ACTION_VIEW).apply {
                setPackage("com.google.android.apps.healthdata")
            }
        }
    }

    private val client: HealthConnectClient? by lazy {
        try {
            if (isAvailable(context)) HealthConnectClient.getOrCreate(context) else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create Health Connect client", e)
            null
        }
    }

    suspend fun hasPermissions(): Boolean {
        return try {
            val granted = client?.permissionController?.getGrantedPermissions() ?: return false
            PERMISSIONS.all { it in granted }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permissions", e)
            false
        }
    }

    suspend fun writeWorkoutSession(
        workoutName: String,
        workoutType: WorkoutType,
        startTimeMillis: Long,
        endTimeMillis: Long,
        notes: String? = null
    ): Boolean {
        val healthClient = client ?: return false
        return try {
            val exerciseType = when (workoutType) {
                WorkoutType.STRENGTH -> ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING
                WorkoutType.CARDIO -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
                WorkoutType.PELOTON -> ExerciseSessionRecord.EXERCISE_TYPE_BIKING
                WorkoutType.BODYWEIGHT_QUICK -> ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS
                WorkoutType.OTHER -> ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT
            }

            val record = ExerciseSessionRecord(
                startTime = Instant.ofEpochMilli(startTimeMillis),
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(Instant.ofEpochMilli(startTimeMillis)),
                endTime = Instant.ofEpochMilli(endTimeMillis),
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(Instant.ofEpochMilli(endTimeMillis)),
                exerciseType = exerciseType,
                title = workoutName,
                notes = notes,
                metadata = Metadata()
            )

            healthClient.insertRecords(listOf(record))
            Log.d(TAG, "Workout synced to Health Connect: $workoutName")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write workout to Health Connect", e)
            false
        }
    }

    suspend fun writeBodyWeight(weightLbs: Double): Boolean {
        val healthClient = client ?: return false
        return try {
            val record = WeightRecord(
                weight = Mass.pounds(weightLbs),
                time = Instant.now(),
                zoneOffset = ZoneOffset.systemDefault().rules.getOffset(Instant.now()),
                metadata = Metadata()
            )
            healthClient.insertRecords(listOf(record))
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write body weight", e)
            false
        }
    }

    suspend fun readRecentBodyWeight(): Double? {
        val healthClient = client ?: return null
        return try {
            val now = Instant.now()
            val thirtyDaysAgo = now.minusSeconds(30 * 24 * 3600)
            val request = androidx.health.connect.client.request.ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = androidx.health.connect.client.time.TimeRangeFilter.between(thirtyDaysAgo, now)
            )
            val response = healthClient.readRecords(request)
            response.records
                .maxByOrNull { it.time }
                ?.weight
                ?.inPounds
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read body weight", e)
            null
        }
    }
}
