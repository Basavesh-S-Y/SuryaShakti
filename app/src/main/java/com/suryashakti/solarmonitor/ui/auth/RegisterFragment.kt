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
import com.suryashakti.solarmonitor.databinding.FragmentRegisterBinding
import com.suryashakti.solarmonitor.viewmodel.AuthViewModel

class RegisterFragment : Fragment() {

    private var _b: FragmentRegisterBinding? = null
    private val b get() = _b!!

    private val authViewModel: AuthViewModel by lazy {
        (requireActivity() as AuthActivity).authViewModel
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentRegisterBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        observeAuthState()
        animateEntrance()
    }

    private fun setupListeners() {
        b.btnRegister.setOnClickListener {
            pulse(b.btnRegister)
            authViewModel.registerWithEmail(
                name            = b.etName.text.toString(),
                email           = b.etEmail.text.toString(),
                password        = b.etPassword.text.toString(),
                confirmPassword = b.etConfirmPassword.text.toString()
            )
        }

        b.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        b.tvAlreadyHaveAccount.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun observeAuthState() {
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Registering -> b.statusOverlay.show(
                    StatusOverlayView.StatusConfig(
                        type      = StatusOverlayView.Type.LOADING,
                        emoji     = "🌱",
                        title     = "Creating your account…",
                        subtitle  = "Setting up your solar profile",
                        tintColor = 0xFF00E676.toInt()
                    )
                )
                is AuthState.LoggedIn -> {
                    b.statusOverlay.show(StatusOverlayView.StatusConfig(
                        type      = StatusOverlayView.Type.SUCCESS,
                        emoji     = "🎉",
                        title     = "Account created!",
                        subtitle  = "Welcome to ಸೂರ್ಯ ಶಕ್ತಿ, ${state.user.shortName}!",
                        tintColor = 0xFF00E676.toInt()
                    ))
                    b.statusOverlay.hide(1400)
                }
                is AuthState.Error -> {
                    b.statusOverlay.show(StatusOverlayView.StatusConfig(
                        type      = StatusOverlayView.Type.ERROR,
                        emoji     = "⚠️",
                        title     = "Registration failed",
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
        val views = listOf(b.tvCreateTitle, b.tvCreateSubtitle, b.cardRegister, b.tvAlreadyHaveAccount)
        views.forEachIndexed { i, v ->
            v.alpha = 0f; v.translationY = 60f
            v.animate().alpha(1f).translationY(0f)
                .setDuration(450).setStartDelay(80L * i)
                .setInterpolator(DecelerateInterpolator()).start()
        }
        b.ivRegisterSun.scaleX = 0f; b.ivRegisterSun.scaleY = 0f
        b.ivRegisterSun.animate().scaleX(1f).scaleY(1f)
            .setDuration(600).setInterpolator(OvershootInterpolator(1.5f)).start()
    }

    private fun pulse(v: View) {
        v.animate().scaleX(0.93f).scaleY(0.93f).setDuration(80)
            .withEndAction { v.animate().scaleX(1f).scaleY(1f).setDuration(120).start() }.start()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
