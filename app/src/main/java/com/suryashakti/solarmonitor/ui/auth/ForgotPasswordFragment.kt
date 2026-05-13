package com.suryashakti.solarmonitor.ui.auth

import android.os.Bundle
import android.view.*
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.suryashakti.solarmonitor.custom.StatusOverlayView
import com.suryashakti.solarmonitor.data.AuthState
import com.suryashakti.solarmonitor.databinding.FragmentForgotPasswordBinding
import com.suryashakti.solarmonitor.viewmodel.AuthViewModel

class ForgotPasswordFragment : Fragment() {

    private var _b: FragmentForgotPasswordBinding? = null
    private val b get() = _b!!

    private val authViewModel: AuthViewModel by lazy {
        (requireActivity() as AuthActivity).authViewModel
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentForgotPasswordBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        b.btnSendReset.setOnClickListener {
            pulse(b.btnSendReset)
            authViewModel.sendPasswordReset(b.etEmail.text.toString())
        }
        b.btnBack.setOnClickListener { findNavController().navigateUp() }

        observeState()
        animateEntrance()
    }

    private fun observeState() {
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.SendingReset -> b.statusOverlay.show(
                    StatusOverlayView.StatusConfig(
                        type      = StatusOverlayView.Type.LOADING,
                        emoji     = "📧",
                        title     = "Sending reset link…",
                        subtitle  = "Check your inbox shortly",
                        tintColor = 0xFFFFD700.toInt()
                    )
                )
                is AuthState.ResetSent -> {
                    b.statusOverlay.show(StatusOverlayView.StatusConfig(
                        type      = StatusOverlayView.Type.SUCCESS,
                        emoji     = "✅",
                        title     = "Reset link sent!",
                        subtitle  = "Check your email inbox and spam folder.",
                        tintColor = 0xFF00E676.toInt()
                    ))
                    b.statusOverlay.hide(2000)
                    b.root.postDelayed({ findNavController().navigateUp() }, 2200)
                }
                is AuthState.Error -> {
                    b.statusOverlay.show(StatusOverlayView.StatusConfig(
                        type      = StatusOverlayView.Type.ERROR,
                        emoji     = "⚠️",
                        title     = "Failed to send",
                        subtitle  = state.message,
                        tintColor = 0xFFFF5252.toInt()
                    ))
                    b.statusOverlay.hide(2500)
                }
                else -> b.statusOverlay.hide()
            }
        }
    }

    private fun animateEntrance() {
        listOf(b.tvForgotTitle, b.tvForgotSub, b.cardForgot, b.btnBack)
            .forEachIndexed { i, v ->
                v.alpha = 0f; v.translationY = 50f
                v.animate().alpha(1f).translationY(0f)
                    .setDuration(400).setStartDelay(80L * i)
                    .setInterpolator(DecelerateInterpolator()).start()
            }
    }

    private fun pulse(v: View) {
        v.animate().scaleX(0.93f).scaleY(0.93f).setDuration(80)
            .withEndAction { v.animate().scaleX(1f).scaleY(1f).setDuration(120).start() }.start()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
