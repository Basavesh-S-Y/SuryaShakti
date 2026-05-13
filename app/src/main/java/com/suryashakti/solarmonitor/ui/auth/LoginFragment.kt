package com.suryashakti.solarmonitor.ui.auth

import android.os.Bundle
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.suryashakti.solarmonitor.R
import com.suryashakti.solarmonitor.custom.StatusOverlayView
import com.suryashakti.solarmonitor.data.AuthState
import com.suryashakti.solarmonitor.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    private var _b: FragmentLoginBinding? = null
    private val b get() = _b!!

    private val authViewModel get() = (requireActivity() as AuthActivity).authViewModel

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentLoginBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeAuthState()
        animateEntrance()
    }

    private fun setupListeners() {
        b.btnLogin.setOnClickListener {
            pulse(b.btnLogin)
            authViewModel.loginWithEmail(
                b.etEmail.text.toString(),
                b.etPassword.text.toString()
            )
        }
        b.btnCreateAccount.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_register)
        }
        b.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_forgot)
        }
    }

    private fun observeAuthState() {
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.LoggingIn -> b.statusOverlay.show(
                    StatusOverlayView.StatusConfig(
                        type      = StatusOverlayView.Type.LOADING,
                        emoji     = "☀️",
                        title     = "Logging in…",
                        subtitle  = "Connecting to your solar account",
                        tintColor = 0xFFFFD700.toInt()
                    )
                )
                is AuthState.LoggedIn -> {
                    b.statusOverlay.show(StatusOverlayView.StatusConfig(
                        type      = StatusOverlayView.Type.SUCCESS,
                        emoji     = "✅",
                        title     = "Welcome, ${state.user.shortName}!",
                        subtitle  = "Loading your energy dashboard…",
                        tintColor = 0xFF00E676.toInt()
                    ))
                    // AuthActivity handles navigation (shows welcome popup then location/main)
                }
                is AuthState.Error -> {
                    b.statusOverlay.show(StatusOverlayView.StatusConfig(
                        type      = StatusOverlayView.Type.ERROR,
                        emoji     = "⚠️",
                        title     = "Login failed",
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
        listOf(b.ivAuthSun, b.tvWelcome, b.tvSubWelcome, b.cardLogin, b.btnCreateAccount)
            .forEachIndexed { i, v ->
                v.alpha = 0f; v.translationY = 60f
                v.animate().alpha(1f).translationY(0f)
                    .setDuration(480).setStartDelay(90L * i)
                    .setInterpolator(DecelerateInterpolator()).start()
            }
        b.ivAuthSun.scaleX = 0f; b.ivAuthSun.scaleY = 0f
        b.ivAuthSun.animate().scaleX(1f).scaleY(1f)
            .setDuration(700).setInterpolator(OvershootInterpolator(1.5f)).start()
    }

    private fun pulse(v: View) {
        v.animate().scaleX(0.93f).scaleY(0.93f).setDuration(80)
            .withEndAction { v.animate().scaleX(1f).scaleY(1f).setDuration(120).start() }.start()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
