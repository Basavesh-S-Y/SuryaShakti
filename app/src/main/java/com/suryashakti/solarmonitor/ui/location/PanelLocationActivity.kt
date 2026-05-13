package com.suryashakti.solarmonitor.ui.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.suryashakti.solarmonitor.R
import com.suryashakti.solarmonitor.adapter.LocationResultAdapter
import com.suryashakti.solarmonitor.data.LocationSearchResult
import com.suryashakti.solarmonitor.data.PanelLocationManager
import com.suryashakti.solarmonitor.databinding.ActivityPanelLocationBinding
import com.suryashakti.solarmonitor.ui.auth.AuthActivity
import com.suryashakti.solarmonitor.ui.main.MainActivity
import com.suryashakti.solarmonitor.util.AppTheme
import com.suryashakti.solarmonitor.util.AuthManager
import com.suryashakti.solarmonitor.util.GeocoderService
import com.suryashakti.solarmonitor.util.ThemeManager
import com.suryashakti.solarmonitor.viewmodel.PanelLocationViewModel
import com.suryashakti.solarmonitor.viewmodel.PanelLocationViewModelFactory
import com.suryashakti.solarmonitor.viewmodel.SearchState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PanelLocationActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityPanelLocationBinding
    private lateinit var vm: PanelLocationViewModel
    private lateinit var adapter: LocationResultAdapter

    private var googleMap: GoogleMap? = null
    private var pendingMapLocation: LatLng? = null
    private var currentTab = TAB_SEARCH   // "search" or "map"

    private val isReset get() = intent.getBooleanExtra(EXTRA_IS_RESET, false)

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) moveMapToCurrentLocation()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)
        ThemeManager.applyWindow(window, this)
        binding = ActivityPanelLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeManager.applyToViewTree(binding.root)

        vm = ViewModelProvider(this, PanelLocationViewModelFactory(application))[PanelLocationViewModel::class.java]

        adapter = LocationResultAdapter { result -> vm.selectLocation(result) }

        setupTabs()
        setupSearchTab()
        setupMapTab(savedInstanceState)
        observeViewModel()

        binding.btnBack.visibility = if (isReset) View.VISIBLE else View.GONE
        binding.btnBack.setOnClickListener { finish() }

        animateEntrance()
    }

    // ── Tabs ──────────────────────────────────────────────────────────────

    private fun setupTabs() {
        binding.btnTabSearch.setOnClickListener { switchTab(TAB_SEARCH) }
        binding.btnTabMap.setOnClickListener    { switchTab(TAB_MAP) }
        switchTab(TAB_SEARCH)
    }

    private fun switchTab(tab: String) {
        currentTab = tab
        val isSearch = tab == TAB_SEARCH

        // Tab highlight
        binding.btnTabSearch.alpha = if (isSearch) 1f else 0.45f
        binding.btnTabMap.alpha    = if (isSearch) 0.45f else 1f
        binding.viewTabIndicatorSearch.visibility = if (isSearch) View.VISIBLE else View.INVISIBLE
        binding.viewTabIndicatorMap.visibility    = if (isSearch) View.INVISIBLE else View.VISIBLE

        // Panel visibility
        binding.panelSearch.visibility = if (isSearch) View.VISIBLE else View.GONE
        binding.panelMap.visibility    = if (isSearch) View.GONE else View.VISIBLE

        // When switching to map, reload if map is ready
        if (tab == TAB_MAP) {
            googleMap?.let { ensureMapMarker(it) }
            requestLocationIfNeeded()
        }
    }

    // ── Search tab ────────────────────────────────────────────────────────

    private fun setupSearchTab() {
        binding.rvResults.layoutManager = LinearLayoutManager(this)
        binding.rvResults.adapter = adapter

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                vm.search(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etSearch.setOnFocusChangeListener { _, hasFocus ->
            binding.cardSearch.cardElevation = if (hasFocus) 14f else 6f
        }
    }

    // ── Map tab ───────────────────────────────────────────────────────────

    private fun setupMapTab(savedInstanceState: Bundle?) {
        val mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.mapContainer, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)

        binding.btnUseCurrentLocation.setOnClickListener {
            requestLocationIfNeeded()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isZoomControlsEnabled   = true
        map.uiSettings.isMyLocationButtonEnabled = false

        if (ThemeManager.getSavedTheme(this) == AppTheme.DARK) {
            try {
                map.setMapStyle(
                    com.google.android.gms.maps.model.MapStyleOptions.loadRawResourceStyle(
                        this, R.raw.map_style_dark
                    )
                )
            } catch (_: Exception) {}
        }

        // Default center: Bengaluru
        val default = LatLng(12.9716, 77.5946)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(default, 11f))

        // Tap on map to pick location
        map.setOnMapClickListener { latLng ->
            map.clear()
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Solar Panel Location")
                    .snippet("Tap confirm to use this")
            )
            pendingMapLocation = latLng
            binding.btnConfirmMapLocation.visibility = View.VISIBLE

            // Reverse geocode asynchronously
            binding.tvMapLocationName.text = "Resolving address…"
            CoroutineScope(Dispatchers.Main).launch {
                val name = GeocoderService.getPlaceName(
                    this@PanelLocationActivity, latLng.latitude, latLng.longitude
                )
                binding.tvMapLocationName.text = name
                binding.tvMapCoords.text = "%.4f°, %.4f°".format(latLng.latitude, latLng.longitude)
            }
        }

        // Confirm map-picked location
        binding.btnConfirmMapLocation.setOnClickListener {
            pendingMapLocation?.let { ll ->
                val name = binding.tvMapLocationName.text.toString()
                    .takeIf { it.isNotBlank() && it != "Tap the map to place your panel" }
                    ?: "%.4f°N, %.4f°E".format(ll.latitude, ll.longitude)

                vm.selectLocation(
                    LocationSearchResult(
                        displayName = name,
                        shortName   = name.split(",").firstOrNull()?.trim() ?: name,
                        latitude    = ll.latitude,
                        longitude   = ll.longitude
                    )
                )
            }
        }

        ensureMapMarker(map)

        // Move to existing panel location if set
        PanelLocationManager.getLocation(this)?.let { loc ->
            val ll = LatLng(loc.latitude, loc.longitude)
            map.addMarker(MarkerOptions().position(ll).title(loc.shortName))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(ll, 13f))
        }
    }

    private fun ensureMapMarker(map: GoogleMap) {
        binding.tvMapLocationName.text = "Tap the map to place your panel"
        binding.tvMapCoords.text = ""
        binding.btnConfirmMapLocation.visibility = View.GONE
    }

    // ── GPS current location ──────────────────────────────────────────────

    private fun requestLocationIfNeeded() {
        val fineOk  = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseOk = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (fineOk || coarseOk) {
            moveMapToCurrentLocation()
        } else {
            locationPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    @SuppressLint("MissingPermission")
    private fun moveMapToCurrentLocation() {
        val map = googleMap ?: return
        map.isMyLocationEnabled = true
        LocationServices.getFusedLocationProviderClient(this)
            .lastLocation
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    val ll = LatLng(loc.latitude, loc.longitude)
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(ll, 14f))
                    // Auto-place a marker at current location
                    map.clear()
                    map.addMarker(MarkerOptions().position(ll).title("Your current location"))
                    pendingMapLocation = ll
                    binding.btnConfirmMapLocation.visibility = View.VISIBLE
                    binding.tvMapLocationName.text = "Locating address…"
                    binding.tvMapCoords.text = "%.4f°, %.4f°".format(ll.latitude, ll.longitude)

                    CoroutineScope(Dispatchers.Main).launch {
                        val name = GeocoderService.getPlaceName(this@PanelLocationActivity, ll.latitude, ll.longitude)
                        binding.tvMapLocationName.text = name
                    }
                }
            }
    }

    // ── ViewModel observation ─────────────────────────────────────────────

    private fun observeViewModel() {
        vm.searchState.observe(this) { state ->
            when (state) {
                is SearchState.Idle -> {
                    binding.progressSearch.visibility = View.GONE
                    binding.tvSearchHint.visibility   = View.VISIBLE
                    binding.tvNoResults.visibility    = View.GONE
                    adapter.submitList(emptyList())
                }
                is SearchState.Searching -> {
                    binding.progressSearch.visibility = View.VISIBLE
                    binding.tvSearchHint.visibility   = View.GONE
                    binding.tvNoResults.visibility    = View.GONE
                }
                is SearchState.Results -> {
                    binding.progressSearch.visibility = View.GONE
                    binding.tvSearchHint.visibility   = View.GONE
                    binding.tvNoResults.visibility    = View.GONE
                    adapter.submitList(state.items)
                }
                is SearchState.Empty -> {
                    binding.progressSearch.visibility = View.GONE
                    binding.tvSearchHint.visibility   = View.GONE
                    binding.tvNoResults.visibility    = View.VISIBLE
                    binding.tvNoResults.text = "No locations found. Try a different name."
                    adapter.submitList(emptyList())
                }
                is SearchState.Error -> {
                    binding.progressSearch.visibility = View.GONE
                    binding.tvNoResults.visibility    = View.VISIBLE
                    binding.tvNoResults.text = "Connection error — check internet"
                    adapter.submitList(emptyList())
                }
            }
        }

        vm.savedLocation.observe(this) { loc ->
            if (loc != null) {
                binding.cardConfirm.visibility = View.VISIBLE
                binding.tvConfirmName.text     = loc.displayName
                binding.tvConfirmCoords.text   = loc.coordinateLabel
                binding.btnConfirm.setOnClickListener {
                    val dest = when {
                        isReset -> null   // just finish
                        AuthManager.isLoggedIn -> Intent(this, MainActivity::class.java)
                        else -> Intent(this, AuthActivity::class.java)
                    }
                    if (dest != null) {
                        startActivity(dest)
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    }
                    finish()
                }
            }
        }
    }

    private fun animateEntrance() {
        listOf(binding.tvTitle, binding.tvSubtitle, binding.tabRow, binding.panelSearch)
            .forEachIndexed { i, v ->
                v.alpha = 0f; v.translationY = 40f
                v.animate().alpha(1f).translationY(0f)
                    .setDuration(400).setStartDelay(70L * i)
                    .setInterpolator(DecelerateInterpolator()).start()
            }
    }

    companion object {
        const val EXTRA_IS_RESET = "is_reset"
        private const val TAB_SEARCH = "search"
        private const val TAB_MAP    = "map"
    }
}
