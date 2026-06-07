package com.zenzone.app.ui.challenge

import android.animation.Animator
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.zenzone.app.R
import com.zenzone.app.model.ChallengeEntity
import com.zenzone.app.repository.UserRepository
import com.zenzone.app.ui.main.MainActivity
import com.zenzone.app.utils.ImageUtils
import com.zenzone.app.viewmodel.ChallengesViewModel
import kotlinx.coroutines.launch

class DailyChallengeFragment : Fragment() {

    private val viewModel: ChallengesViewModel by viewModels()
    private var loadedDate: String = ""
    private var challengesLiveData: LiveData<List<com.zenzone.app.model.ChallengeEntity>>? = null

    private lateinit var lottieCelebration: LottieAnimationView

    // Card 1 Views
    private lateinit var cardChallenge1: MaterialCardView
    private lateinit var tvTitleChallenge1: TextView
    private lateinit var tvDescChallenge1: TextView
    private lateinit var progressChallenge1: LinearProgressIndicator
    private lateinit var tvRatioChallenge1: TextView
    private lateinit var ivCheckmark1: ImageView
    private lateinit var chipXp1: Chip
    private lateinit var chipSeed1: Chip

    // Card 2 Views
    private lateinit var cardChallenge2: MaterialCardView
    private lateinit var tvTitleChallenge2: TextView
    private lateinit var tvDescChallenge2: TextView
    private lateinit var progressChallenge2: LinearProgressIndicator
    private lateinit var tvRatioChallenge2: TextView
    private lateinit var ivCheckmark2: ImageView
    private lateinit var chipXp2: Chip
    private lateinit var chipSeed2: Chip

    // Card 3 Views
    private lateinit var cardChallenge3: MaterialCardView
    private lateinit var tvTitleChallenge3: TextView
    private lateinit var tvDescChallenge3: TextView
    private lateinit var progressChallenge3: LinearProgressIndicator
    private lateinit var tvRatioChallenge3: TextView
    private lateinit var ivCheckmark3: ImageView
    private lateinit var chipXp3: Chip
    private lateinit var chipSeed3: Chip

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_daily_challenge, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            loadedDate = com.zenzone.app.utils.DateUtils.getTodayString()
            initViews(view)
            setupNavbar(view)
            observeViewModel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        val today = com.zenzone.app.utils.DateUtils.getTodayString()
        if (loadedDate != today) {
            loadedDate = today
            observeViewModel()
        }
    }

    private fun initViews(view: View) {
        lottieCelebration = view.findViewById(R.id.lottie_celebration)
        lottieCelebration.setFailureListener { throwable ->
            throwable.printStackTrace()
        }
        lottieCelebration.setAnimationFromUrl("https://assets2.lottiefiles.com/packages/lf20_81x92z.json")
        lottieCelebration.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                lottieCelebration.visibility = View.GONE
            }
            override fun onAnimationCancel(animation: Animator) {
                lottieCelebration.visibility = View.GONE
            }
            override fun onAnimationRepeat(animation: Animator) {}
        })

        // Card 1
        cardChallenge1 = view.findViewById(R.id.card_challenge_1)
        tvTitleChallenge1 = view.findViewById(R.id.tv_title_challenge_1)
        tvDescChallenge1 = view.findViewById(R.id.tv_desc_challenge_1)
        progressChallenge1 = view.findViewById(R.id.progress_challenge_1)
        tvRatioChallenge1 = view.findViewById(R.id.tv_ratio_challenge_1)
        ivCheckmark1 = view.findViewById(R.id.iv_checkmark_1)
        chipXp1 = view.findViewById(R.id.chip_xp_1)
        chipSeed1 = view.findViewById(R.id.chip_seed_1)

        // Card 2
        cardChallenge2 = view.findViewById(R.id.card_challenge_2)
        tvTitleChallenge2 = view.findViewById(R.id.tv_title_challenge_2)
        tvDescChallenge2 = view.findViewById(R.id.tv_desc_challenge_2)
        progressChallenge2 = view.findViewById(R.id.progress_challenge_2)
        tvRatioChallenge2 = view.findViewById(R.id.tv_ratio_challenge_2)
        ivCheckmark2 = view.findViewById(R.id.iv_checkmark_2)
        chipXp2 = view.findViewById(R.id.chip_xp_2)
        chipSeed2 = view.findViewById(R.id.chip_seed_2)

        // Card 3
        cardChallenge3 = view.findViewById(R.id.card_challenge_3)
        tvTitleChallenge3 = view.findViewById(R.id.tv_title_challenge_3)
        tvDescChallenge3 = view.findViewById(R.id.tv_desc_challenge_3)
        progressChallenge3 = view.findViewById(R.id.progress_challenge_3)
        tvRatioChallenge3 = view.findViewById(R.id.tv_ratio_challenge_3)
        ivCheckmark3 = view.findViewById(R.id.iv_checkmark_3)
        chipXp3 = view.findViewById(R.id.chip_xp_3)
        chipSeed3 = view.findViewById(R.id.chip_seed_3)

        // Default visibility to GONE until data loads
        cardChallenge1.visibility = View.GONE
        cardChallenge2.visibility = View.GONE
        cardChallenge3.visibility = View.GONE
    }

    private fun setupNavbar(view: View) {
        view.findViewById<View>(R.id.iv_common_menu)?.setOnClickListener {
            (activity as? MainActivity)?.openDrawer()
        }
        view.findViewById<View>(R.id.iv_common_agent)?.setOnClickListener {
            com.zenzone.app.ui.social.ZenAgentDialog.show(requireContext(), parentFragmentManager, activity as? MainActivity)
        }
        view.findViewById<View>(R.id.cv_common_profile_mini)?.setOnClickListener {
            (activity as? MainActivity)?.navigateToMenuItem(R.id.nav_profile)
        }

        view.findViewById<View>(R.id.iv_common_info)?.setOnClickListener {
            // Can show a bottom sheet or simple toast explaining challenges
            Toast.makeText(context, "Complete focus sessions and streaks to finish daily challenges!", Toast.LENGTH_SHORT).show()
        }

        lifecycleScope.launch {
            try {
                val profile = UserRepository(requireContext()).loadProfile()
                val tvCommonInitial = view.findViewById<TextView>(R.id.tv_common_profile_initial_mini)
                val ivCommonProfileImage = view.findViewById<ImageView>(R.id.iv_common_profile_image_mini)

                val displayName = profile.userName.ifBlank { "ZenZone" }
                view.findViewById<TextView>(R.id.tv_app_logo_name)?.text = "🧘 $displayName"

                val initial = if (profile.userName.isNotEmpty()) {
                    profile.userName.first().uppercaseChar().toString()
                } else {
                    "Z"
                }
                tvCommonInitial.text = initial

                if (!profile.profileImageUri.isNullOrBlank()) {
                    if (ImageUtils.isBase64Image(profile.profileImageUri)) {
                        val bitmap = ImageUtils.base64ToBitmap(profile.profileImageUri)
                        ivCommonProfileImage.setImageBitmap(bitmap)
                        ivCommonProfileImage.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun observeViewModel() {
        challengesLiveData?.removeObservers(viewLifecycleOwner)
        val newLiveData = viewModel.getTodaysChallenges()
        challengesLiveData = newLiveData
        newLiveData.observe(viewLifecycleOwner) { challenges ->
            cardChallenge1.visibility = View.GONE
            cardChallenge2.visibility = View.GONE
            cardChallenge3.visibility = View.GONE

            if (challenges != null && challenges.isNotEmpty()) {
                if (challenges.size >= 1) {
                    bindChallengeCard(
                        challenges[0],
                        cardChallenge1,
                        tvTitleChallenge1,
                        tvDescChallenge1,
                        progressChallenge1,
                        tvRatioChallenge1,
                        ivCheckmark1,
                        chipXp1,
                        chipSeed1
                    )
                }
                if (challenges.size >= 2) {
                    bindChallengeCard(
                        challenges[1],
                        cardChallenge2,
                        tvTitleChallenge2,
                        tvDescChallenge2,
                        progressChallenge2,
                        tvRatioChallenge2,
                        ivCheckmark2,
                        chipXp2,
                        chipSeed2
                    )
                }
                if (challenges.size >= 3) {
                    bindChallengeCard(
                        challenges[2],
                        cardChallenge3,
                        tvTitleChallenge3,
                        tvDescChallenge3,
                        progressChallenge3,
                        tvRatioChallenge3,
                        ivCheckmark3,
                        chipXp3,
                        chipSeed3
                    )
                }
            }
        }

        viewModel.challengeCompletedEvent.observe(viewLifecycleOwner) { completedChallenge ->
            completedChallenge?.let {
                // Show celebration lottie
                lottieCelebration.visibility = View.VISIBLE
                lottieCelebration.playAnimation()
                Toast.makeText(context, "🏆 Challenge Completed: ${it.title}!", Toast.LENGTH_LONG).show()
                
                // Clear event so it doesn't trigger on configuration changes
                viewModel.clearCompletedEvent()
                
                // Refresh drawer details in case level changed
                (activity as? MainActivity)?.updateDrawerHeader()
            }
        }
    }

    private fun bindChallengeCard(
        challenge: ChallengeEntity,
        card: MaterialCardView,
        tvTitle: TextView,
        tvDesc: TextView,
        progress: LinearProgressIndicator,
        tvRatio: TextView,
        ivCheckmark: ImageView,
        chipXp: Chip,
        chipSeed: Chip
    ) {
        card.visibility = View.VISIBLE
        tvTitle.text = challenge.title
        tvDesc.text = challenge.description

        progress.max = challenge.targetValue
        progress.progress = challenge.currentProgress

        tvRatio.text = "${challenge.currentProgress}/${challenge.targetValue}"

        if (challenge.isCompleted) {
            ivCheckmark.visibility = View.VISIBLE
            val color = ContextCompat.getColor(card.context, R.color.zen_teal_primary)
            card.setStrokeColor(ColorStateList.valueOf(color))
            card.strokeWidth = 4
        } else {
            ivCheckmark.visibility = View.GONE
            card.strokeWidth = 0
        }

        chipXp.text = "+${challenge.xpReward} XP"
        chipSeed.text = "🌱 ${challenge.seedReward} Seed"
    }
}
