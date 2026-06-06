package com.zenzone.app.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.zenzone.app.model.ChallengeEntity
import com.zenzone.app.utils.ChainCalculator
import com.zenzone.app.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ChallengeRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val challengeDao = db.challengeDao()
    private val userRepo = UserRepository(context)
    private val firestore: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    fun getTodaysChallenges(date: String): LiveData<List<ChallengeEntity>> =
        challengeDao.getTodaysChallenges(date)

    suspend fun getChallengesForDateSync(date: String): List<ChallengeEntity> =
        challengeDao.getChallengesForDateSync(date)

    suspend fun insertChallenges(challenges: List<ChallengeEntity>) =
        challengeDao.insertChallenges(challenges)

    suspend fun deleteOldChallenges(date: String) =
        challengeDao.deleteOldChallenges(date)

    suspend fun updateChallengeProgress(
        type: String,
        incrementValue: Int,
        onChallengeCompleted: ((ChallengeEntity) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        val today = DateUtils.getTodayString()
        val challenges = challengeDao.getChallengesForDateSync(today)
        for (challenge in challenges) {
            if (challenge.type == type && !challenge.isCompleted) {
                val newProgress = challenge.currentProgress + incrementValue
                val isNowCompleted = newProgress >= challenge.targetValue

                if (isNowCompleted) {
                    challengeDao.updateProgress(challenge.id, challenge.targetValue)
                    challengeDao.markComplete(challenge.id)

                    awardXPAndSeeds(challenge.xpReward, challenge.seedReward)

                    onChallengeCompleted?.invoke(
                        challenge.copy(
                            currentProgress = challenge.targetValue,
                            isCompleted = true
                        )
                    )
                } else {
                    challengeDao.updateProgress(challenge.id, newProgress)
                }
            }
        }
    }

    private suspend fun awardXPAndSeeds(xpReward: Int, seedReward: Int) {
        try {
            val profile = userRepo.loadProfile()
            val newXp = profile.zenXP + xpReward
            val (newLevel, _) = ChainCalculator.calculateZenLevel(newXp)
            val newSeeds = profile.rareSeeds + seedReward
            val updatedProfile = profile.copy(
                zenXP = newXp,
                zenLevel = newLevel,
                rareSeeds = newSeeds
            )
            userRepo.saveProfile(updatedProfile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun fetchAndCacheChallenges(date: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val docRef = firestore.collection("daily_challenges").document(date)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val challengesList = mutableListOf<ChallengeEntity>()
                for (slotName in listOf("challengeSlot1", "challengeSlot2", "challengeSlot3")) {
                    @Suppress("UNCHECKED_CAST")
                    val slotMap = snapshot.get(slotName) as? Map<String, Any>
                    if (slotMap != null) {
                        val title = slotMap["title"] as? String ?: ""
                        val description = slotMap["description"] as? String ?: ""
                        val type = slotMap["type"] as? String ?: ""
                        val targetValue = (slotMap["targetValue"] as? Long)?.toInt() ?: 0
                        val xpReward = (slotMap["xpReward"] as? Long)?.toInt() ?: 0
                        val seedReward = (slotMap["seedReward"] as? Long)?.toInt() ?: 0

                        challengesList.add(
                            ChallengeEntity(
                                id = "${date}_${slotName}",
                                title = title,
                                description = description,
                                type = type,
                                targetValue = targetValue,
                                currentProgress = 0,
                                xpReward = xpReward,
                                seedReward = seedReward,
                                isCompleted = false,
                                date = date
                            )
                        )
                    }
                }
                if (challengesList.isNotEmpty()) {
                    challengeDao.insertChallenges(challengesList)
                    return@withContext true
                }
            }
            return@withContext false
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }

    suspend fun generateAndUploadDefaultChallenges(date: String) = withContext(Dispatchers.IO) {
        val challengesList = listOf(
            ChallengeEntity(
                id = "${date}_challengeSlot1",
                title = "Daily Focus Session",
                description = "Focus for 25 minutes total",
                type = "FOCUS_DURATION",
                targetValue = 25,
                currentProgress = 0,
                xpReward = 50,
                seedReward = 1,
                isCompleted = false,
                date = date
            ),
            ChallengeEntity(
                id = "${date}_challengeSlot2",
                title = "Stay Focused",
                description = "Complete 2 focus sessions",
                type = "SESSION_COUNT",
                targetValue = 2,
                currentProgress = 0,
                xpReward = 30,
                seedReward = 1,
                isCompleted = false,
                date = date
            ),
            ChallengeEntity(
                id = "${date}_challengeSlot3",
                title = "Maintain the Streak",
                description = "Keep your focus streak going",
                type = "STREAK_MAINTAIN",
                targetValue = 1,
                currentProgress = 0,
                xpReward = 40,
                seedReward = 1,
                isCompleted = false,
                date = date
            )
        )

        challengeDao.insertChallenges(challengesList)

        try {
            val docData = mapOf(
                "challengeSlot1" to mapOf(
                    "title" to challengesList[0].title,
                    "description" to challengesList[0].description,
                    "type" to challengesList[0].type,
                    "targetValue" to challengesList[0].targetValue,
                    "xpReward" to challengesList[0].xpReward,
                    "seedReward" to challengesList[0].seedReward
                ),
                "challengeSlot2" to mapOf(
                    "title" to challengesList[1].title,
                    "description" to challengesList[1].description,
                    "type" to challengesList[1].type,
                    "targetValue" to challengesList[1].targetValue,
                    "xpReward" to challengesList[1].xpReward,
                    "seedReward" to challengesList[1].seedReward
                ),
                "challengeSlot3" to mapOf(
                    "title" to challengesList[2].title,
                    "description" to challengesList[2].description,
                    "type" to challengesList[2].type,
                    "targetValue" to challengesList[2].targetValue,
                    "xpReward" to challengesList[2].xpReward,
                    "seedReward" to challengesList[2].seedReward
                )
            )
            firestore.collection("daily_challenges").document(date)
                .set(docData, SetOptions.merge())
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
