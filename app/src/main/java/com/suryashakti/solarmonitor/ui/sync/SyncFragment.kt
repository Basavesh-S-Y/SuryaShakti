package com.suryashakti.solarmonitor.ui.sync

import android.os.Bundle
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.suryashakti.solarmonitor.custom.StatusOverlayView
import com.suryashakti.solarmonitor.data.AuthState
import com.suryashakti.solarmonitor.data.SyncState
import com.suryashakti.solarmonitor.databinding.FragmentSyncBinding
import com.suryashakti.solarmonitor.util.AuthManager
import com.suryashakti.solarmonitor.util.DateUtils
import com.suryashakti.solarmonitor.viewmodel.AuthViewModel
import com.suryashakti.solarmonitor.viewmodel.AuthViewModelFactory
import com.suryashakti.solarmonitor.viewmodel.SyncViewModel
import com.suryashakti.solarmonitor.viewmodel.SyncViewModelFactory
import com.suryashakti.solarmonitor.viewmodel.EnergyViewModel
import com.suryashakti.solarmonitor.viewmodel.EnergyViewModelFactory

class SyncFragment : Fragment() {

    private var _b: FragmentSyncBinding? = null
    private val b get() = _b!!

    private lateinit var syncViewModel: SyncViewModel
    private lateinit var authViewModel: AuthViewModel

    private val energyViewModel: EnergyViewModel by activityViewModels {
        EnergyViewModelFactory(requireActivity().application)
    }

    private var rotateAnimator: android.animation.ObjectAnimator? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentSyncBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        syncViewModel = ViewModelProvider(
            this, SyncViewModelFactory(requireActivity().application)
        )[SyncViewModel::class.java]

        authViewModel = ViewModelProvider(
            this, AuthViewModelFactory(requireActivity().application)
        )[AuthViewModel::class.java]

        setupUI()
        observeSync()
        observeAuth()
        animateEntrance()
    }

    private fun setupUI() {
        b.btnSync.setOnClickListener {
            pulse(b.btnSync)
            syncViewModel.startSync()
        }
        b.btnSignOut.setOnClickListener {
            pulse(b.btnSignOut)
            authViewModel.signOut(requireContext())
        }
        b.btnRetrySync.setOnClickListener {
            syncViewModel.startSync()
        }
    }

    private fun observeAuth() {
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.LoggedIn -> {
                    val user = state.user
                    b.cardProfile.visibility = View.VISIBLE
                    b.cardNotLoggedIn.visibility = View.GONE
                    b.tvProfileName.text = user.displayName.ifBlank { user.email }
                    b.tvProfileEmail.text = user.email
                    b.tvProfileInitials.text = user.initials
                    b.tvAuthBadge.text = if (user.isGoogleAccount) "Google Account" else "Email Account"
                    b.tvAuthBadge.setBackgroundColor(
                        if (user.isGoogleAccount) 0xFF1565C0.toInt() else 0xFF2E7D32.toInt()
                    )
                }
                is AuthState.LoggedOut -> {
                    b.cardProfile.visibility = View.GONE
                    b.cardNotLoggedIn.visibility = View.VISIBLE
                }
                else -> {}
            }
        }

        // Seed from current auth state
        val user = AuthManager.currentUser
        if (user != null) {
            authViewModel.authState.value.let { /* already observed */ }
        }
    }

    private fun observeSync() {
        syncViewModel.syncState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is SyncState.Idle -> {
                    b.statusOverlay.hide()
                    stopRotate()
                    b.btnSync.isEnabled = true
                    b.tvSyncStatus.text = "Ready to sync"
                    b.tvSyncStatus.setTextColor(0xFFAAAAAA.toInt())
                }
                is SyncState.Syncing -> {
                    startRotate()
                    b.btnSync.isEnabled = false
                    b.statusOverlay.show(
                        StatusOverlayView.StatusConfig(
                            type      = StatusOverlayView.Type.LOADING,
                            emoji     = "🔄",
                            title     = "Syncing to Cloud…",
                            subtitle  = "Uploading your energy logs to Firebase",
                            tintColor = 0xFF00E676.toInt()
                        )
                    )
                }
                is SyncState.Success -> {
                    stopRotate()
                    b.btnSync.isEnabled = true
                    b.statusOverlay.show(
                        StatusOverlayView.StatusConfig(
                            type      = StatusOverlayView.Type.SUCCESS,
                            emoji     = "✅",
                            title     = "Sync complete!",
                            subtitle  = "↑ ${state.uploaded} uploaded  •  ↓ ${state.downloaded} downloaded",
                            tintColor = 0xFF00E676.toInt()
                        )
                    )
                    b.statusOverlay.hide(2000)
                    b.tvSyncStatus.text = "Last sync: just now"
                    b.tvSyncStatus.setTextColor(0xFF00E676.toInt())
                    b.cardLastSync.visibility = View.VISIBLE
                    b.tvLastSyncUploaded.text = "↑ ${state.uploaded} records uploaded"
                    b.tvLastSyncDownloaded.text = "↓ ${state.downloaded} records downloaded"
                    syncViewModel.resetState()
                }
                is SyncState.Error -> {
                    stopRotate()
                    b.btnSync.isEnabled = true
                    b.statusOverlay.show(
                        StatusOverlayView.StatusConfig(
                            type      = StatusOverlayView.Type.ERROR,
                            emoji     = "❌",
                            title     = "Sync failed",
                            subtitle  = state.message,
                            tintColor = 0xFFFF5252.toInt()
                        )
                    )
                    b.statusOverlay.hide(2500)
                    b.tvSyncStatus.text = "Sync failed — tap retry"
                    b.tvSyncStatus.setTextColor(0xFFFF5252.toInt())
                    b.btnRetrySync.visibility = View.VISIBLE
                }
                is SyncState.NotLoggedIn -> {
                    b.statusOverlay.show(
                        StatusOverlayView.StatusConfig(
                            type      = StatusOverlayView.Type.ERROR,
                            emoji     = "🔒",
                            title     = "Not logged in",
                            subtitle  = "Please log in to sync your data",
                            tintColor = 0xFFFFD700.toInt()
                        )
                    )
                    b.statusOverlay.hide(2000)
                }
            }
        }

        syncViewModel.allLogs.observe(viewLifecycleOwner) { logs ->
            b.tvLocalRecords.text = "${logs.size} local records"
        }

        syncViewModel.lastSyncTime.observe(viewLifecycleOwner) { millis ->
            if (millis > 0) {
                b.tvLastSyncTime.text = DateUtils.toDisplayString(millis)
            }
        }
    }

    private fun startRotate() {
        rotateAnimator?.cancel()
        rotateAnimator = android.animation.ObjectAnimator
            .ofFloat(b.ivSyncIcon, View.ROTATION, 0f, 360f).apply {
                duration     = 1200
                repeatCount  = android.animation.ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                start()
            }
    }

    private fun stopRotate() {
        rotateAnimator?.cancel()
        rotateAnimator = null
        b.ivSyncIcon.rotation = 0f
    }

    private fun animateEntrance() {
        listOf(b.cardProfile, b.cardSyncInfo, b.btnSync, b.cardLastSync)
            .forEachIndexed { i, v ->
                v.alpha = 0f; v.translationY = 70f
                v.animate().alpha(1f).translationY(0f)
                    .setDuration(480).setStartDelay(90L * i)
                    .setInterpolator(DecelerateInterpolator()).start()
            }
    }

    private fun pulse(v: View) {
        v.animate().scaleX(0.93f).scaleY(0.93f).setDuration(80)
            .withEndAction { v.animate().scaleX(1f).scaleY(1f).setDuration(120).start() }.start()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
