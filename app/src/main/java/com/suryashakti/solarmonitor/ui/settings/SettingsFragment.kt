package com.suryashakti.solarmonitor.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.suryashakti.solarmonitor.data.AuthState
import com.suryashakti.solarmonitor.data.PanelLocationManager
import com.suryashakti.solarmonitor.databinding.FragmentSettingsBinding
import com.suryashakti.solarmonitor.ui.location.PanelLocationActivity
import com.suryashakti.solarmonitor.util.AppTheme
import com.suryashakti.solarmonitor.util.AuthManager
import com.suryashakti.solarmonitor.util.PreferenceManager
import com.suryashakti.solarmonitor.util.ThemeManager
import com.suryashakti.solarmonitor.viewmodel.AuthViewModel
import com.suryashakti.solarmonitor.viewmodel.AuthViewModelFactory

class SettingsFragment : Fragment() {

    private var _b: FragmentSettingsBinding? = null
    private val b get() = _b!!

    private lateinit var prefs: PreferenceManager
    private lateinit var authViewModel: AuthViewModel

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentSettingsBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PreferenceManager(requireContext())
        authViewModel = ViewModelProvider(
            this, AuthViewModelFactory(requireActivity().application)
        )[AuthViewModel::class.java]

        loadSettings()
        setupListeners()
        observeAuth()
        animateEntrance()
    }

    override fun onResume() {
        super.onResume()
        loadPanelLocationDisplay()
    }

    // ── Load ──────────────────────────────────────────────────────────────

    private fun loadSettings() {
        // Profile — read-only from Firebase / saved prefs
        val user = AuthManager.currentUser
        val displayName = user?.displayName?.ifBlank { prefs.userName }
            ?: prefs.userName.ifBlank { "Solar User" }
        val email = user?.email ?: ""

        b.tvSettingsUserName.text  = displayName.ifBlank { "Solar User" }
        b.tvSettingsUserEmail.text = email
        b.tvSettingsInitials.text  = if (displayName.isNotBlank())
            displayName.trim().first().uppercase() else "U"

        if (user != null) {
            b.cardAccountInfo.visibility = View.VISIBLE
            b.tvAccountEmail.text = email
            b.tvAccountName.text  = displayName.ifBlank { "Solar User" }
            b.tvAccountType.text  =
                if (user.isGoogleAccount) "🔵 Google Account" else "📧 Email Account"
        } else {
            b.cardAccountInfo.visibility = View.GONE
        }

        // Energy settings
        b.etGridRate.setText(prefs.gridRatePerUnit.toString())
        b.etExportRate.setText(prefs.exportRatePerUnit.toString())
        b.etPanelCapacity.setText(prefs.panelCapacityKw.toString())
        b.switchNotifications.isChecked = prefs.notificationsEnabled
        when (ThemeManager.getSavedTheme(requireContext())) {
            AppTheme.DARK -> b.rbDarkTheme.isChecked = true
            AppTheme.LIGHT -> b.rbLightTheme.isChecked = true
        }

        loadPanelLocationDisplay()
        ThemeManager.applyToViewTree(b.root)
    }

    private fun loadPanelLocationDisplay() {
        val loc = PanelLocationManager.getLocation(requireContext())
        if (loc != null) {
            b.tvCurrentPanelLocation.text = loc.displayName
            b.tvCurrentPanelCoords.text   = loc.coordinateLabel
            b.tvCurrentPanelLocation.setTextColor(0xFFFFD700.toInt())
        } else {
            b.tvCurrentPanelLocation.text = "Not set — tap to choose"
            b.tvCurrentPanelCoords.text   = ""
            b.tvCurrentPanelLocation.setTextColor(0xFFFF5252.toInt())
        }
    }

    // ── Listeners ─────────────────────────────────────────────────────────

    private fun setupListeners() {
        b.btnResetLocation.setOnClickListener {
            pulse(b.btnResetLocation)
            startActivity(
                Intent(requireContext(), PanelLocationActivity::class.java)
                    .putExtra(PanelLocationActivity.EXTRA_IS_RESET, true)
            )
        }

        b.btnSaveSettings.setOnClickListener {
            pulse(b.btnSaveSettings)
            saveSettings()
        }

        b.switchNotifications.setOnCheckedChangeListener { _, checked ->
            prefs.setNotifications(checked)
        }

        b.rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val selectedTheme = when (checkedId) {
                b.rbLightTheme.id -> AppTheme.LIGHT
                else -> AppTheme.DARK
            }

            if (selectedTheme != ThemeManager.getSavedTheme(requireContext())) {
                Toast.makeText(requireContext(), "${selectedTheme.label} theme applied", Toast.LENGTH_SHORT).show()
                ThemeManager.applyAndRestart(requireActivity(), selectedTheme)
            }
        }

        b.btnSignOut.setOnClickListener {
            pulse(b.btnSignOut)
            authViewModel.signOut(requireContext())
        }
    }

    private fun saveSettings() {
        val gridRate   = b.etGridRate.text.toString().toDoubleOrNull()
        val exportRate = b.etExportRate.text.toString().toDoubleOrNull()
        val capacity   = b.etPanelCapacity.text.toString().toDoubleOrNull()

        if (gridRate == null   || gridRate <= 0)   { b.etGridRate.error   = "Enter valid rate";     return }
        if (exportRate == null || exportRate <= 0) { b.etExportRate.error = "Enter valid rate";     return }
        if (capacity == null   || capacity <= 0)   { b.etPanelCapacity.error = "Enter valid capacity"; return }

        prefs.setGridRate(gridRate)
        prefs.setExportRate(exportRate)
        prefs.setPanelCapacityKw(capacity)

        // Flash confirm
        b.tvSaveConfirm.visibility = View.VISIBLE
        b.tvSaveConfirm.alpha = 0f
        b.tvSaveConfirm.animate().alpha(1f).setDuration(300).withEndAction {
            b.tvSaveConfirm.animate().alpha(0f).setStartDelay(1500).setDuration(400)
                .withEndAction { b.tvSaveConfirm.visibility = View.GONE }.start()
        }.start()

        Toast.makeText(requireContext(), "✅ Settings saved!", Toast.LENGTH_SHORT).show()
    }

    // ── Auth observer ─────────────────────────────────────────────────────

    private fun observeAuth() {
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            if (state is AuthState.LoggedOut) {
                requireActivity().finish()
                startActivity(Intent(requireContext(),
                    com.suryashakti.solarmonitor.ui.splash.SplashActivity::class.java))
            }
        }
    }

    // ── UI helpers ────────────────────────────────────────────────────────

    private fun animateEntrance() {
        listOf(b.cardPanelLocation, b.cardProfile2, b.cardEnergyRates,
               b.cardTheme, b.cardPanelSettings, b.cardNotifications, b.cardAccountInfo,
               b.btnSaveSettings)
            .forEachIndexed { i, v ->
                v.alpha = 0f; v.translationY = 50f
                v.animate().alpha(1f).translationY(0f)
                    .setDuration(380).setStartDelay(50L * i)
                    .setInterpolator(DecelerateInterpolator()).start()
            }
    }

    private fun pulse(v: View) {
        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80)
            .withEndAction { v.animate().scaleX(1f).scaleY(1f).setDuration(120).start() }.start()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
