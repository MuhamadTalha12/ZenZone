package com.zenzone.app.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.zenzone.app.model.ChallengeEntity
import com.zenzone.app.repository.ChallengeRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChallengesViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private val application = mockk<Application>(relaxed = true)

    private lateinit var viewModel: ChallengesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkConstructor(ChallengeRepository::class)
        coEvery { anyConstructed<ChallengeRepository>().getChallengesForDateSync(any()) } returns emptyList()
        viewModel = ChallengesViewModel(application)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `getTodaysChallenges triggers Firestore cache fetch when local DB is empty`() = runTest {
        val todayStr = com.zenzone.app.utils.DateUtils.getTodayString()
        
        coEvery { anyConstructed<ChallengeRepository>().getChallengesForDateSync(todayStr) } returns emptyList()
        coEvery { anyConstructed<ChallengeRepository>().fetchAndCacheChallenges(todayStr) } returns true

        viewModel.getTodaysChallenges()
        advanceUntilIdle()

        coVerify(exactly = 1) { 
            anyConstructed<ChallengeRepository>().fetchAndCacheChallenges(todayStr) 
        }
    }

    @Test
    fun `updateChallengeProgress passes callback that posts complete event`() = runTest {
        val challenge = ChallengeEntity(
            id = "c1",
            title = "Test",
            isCompleted = true,
            xpReward = 50,
            seedReward = 1
        )

        val slot = slot<((ChallengeEntity) -> Unit)>()
        coEvery { 
            anyConstructed<ChallengeRepository>().updateChallengeProgress("FOCUS_DURATION", 10, capture(slot)) 
        } answers {
            slot.captured.invoke(challenge)
        }

        val observer = mockk<Observer<ChallengeEntity?>>(relaxed = true)
        viewModel.challengeCompletedEvent.observeForever(observer)

        viewModel.updateChallengeProgress("FOCUS_DURATION", 10)
        advanceUntilIdle()

        verify { observer.onChanged(challenge) }
        viewModel.challengeCompletedEvent.removeObserver(observer)
    }
}
