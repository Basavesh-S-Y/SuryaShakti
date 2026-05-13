package com.suryashakti.solarmonitor.ui.report

import android.animation.ValueAnimator
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.suryashakti.solarmonitor.adapter.ReportLogAdapter
import com.suryashakti.solarmonitor.databinding.FragmentReportBinding
import com.suryashakti.solarmonitor.util.DateUtils
import com.suryashakti.solarmonitor.util.EnergyUtils
import com.suryashakti.solarmonitor.viewmodel.EnergyViewModel
import com.suryashakti.solarmonitor.viewmodel.EnergyViewModelFactory
import java.io.OutputStreamWriter

class ReportFragment : Fragment() {

    private var _b: FragmentReportBinding? = null
    private val b get() = _b!!

    private val vm: EnergyViewModel by activityViewModels {
        EnergyViewModelFactory(requireActivity().application)
    }

    private lateinit var reportAdapter: ReportLogAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentReportBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        reportAdapter = ReportLogAdapter()
        b.rvDailyLogs.layoutManager = LinearLayoutManager(requireContext())
        b.rvDailyLogs.adapter = reportAdapter

        vm.last30Logs.observe(viewLifecycleOwner) { logs ->
            b.barChart.setLogs(logs)
            reportAdapter.submitList(logs.sortedByDescending { it.dateMillis })
        }

        vm.allLogs.observe(viewLifecycleOwner) { allLogs ->
            b.tvTotalRecords.text = "${allLogs.size} total records"
            b.btnExportCsv.setOnClickListener {
                pulse(b.btnExportCsv)
                exportAllAsCsv(allLogs)
            }
        }

        vm.reportStats.observe(viewLifecycleOwner) { stats ->
            animateCount(b.tvTotalSaved,     "₹%.0f",      stats.totalMoneySaved)
            animateCount(b.tvTotalEarned,    "₹%.0f",      stats.totalMoneyEarned)
            animateCount(b.tvTotalBenefit,   "₹%.0f",      stats.totalBenefit)
            animateCount(b.tvTotalGenerated, "%.1f\nkWh",  stats.totalGeneratedKwh)
            animateCount(b.tvTotalConsumed,  "%.1f\nkWh",  stats.totalConsumedKwh)
            animateCount(b.tvTotalExported,  "%.1f\nkWh",  stats.totalExportedKwh)
            animateCount(b.tvTotalImported,  "%.1f\nkWh",  stats.totalImportedKwh)
            animateCount(b.tvCo2Saved,       "%.1f\nkg CO", stats.co2SavedKg)

            b.tvAvgScore.text = "${stats.avgIndependenceScore}%"
            b.progressAvgScore.progress = stats.avgIndependenceScore
            b.tvDaysLogged.text = "${stats.daysLogged}/30 days"

            val (grade, label) = EnergyUtils.getScoreGrade(stats.avgIndependenceScore)
            b.tvGrade.text = grade; b.tvGradeLabel.text = label

            b.tvSolarCoverage.text = "%.1f%%".format(stats.solarCoveragePercent)
            b.progressSolarCoverage.progress = stats.solarCoveragePercent.toInt()

            val billWithout = stats.totalConsumedKwh * 8.0
            val actualBill  = stats.totalImportedKwh * 8.0
            b.tvBillWithout.text = "₹%.0f".format(billWithout)
            b.tvActualBill.text  = "₹%.0f".format(actualBill)
            b.tvBillSaving.text  = "₹%.0f saved!".format(stats.totalMoneySaved)
        }

        animateCards()
    }

    private fun exportAllAsCsv(logs: List<com.suryashakti.solarmonitor.data.EnergyLog>) {
        if (logs.isEmpty()) {
            Toast.makeText(requireContext(), "No records to export", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = "SuryaShakti_Energy_Export_${System.currentTimeMillis()}.csv"
        val csv = buildString {
            appendLine("Date,Generated(kWh),Consumed(kWh),Net(kWh),Solar Coverage(%),Exported(kWh),Imported(kWh),Money Saved(Rs),Export Earned(Rs),Total Benefit(Rs),Independence Score,Weather,Panel Efficiency(%),Notes")
            logs.sortedBy { it.dateMillis }.forEach { log ->
                appendLine(
                    "${DateUtils.toDisplayString(log.dateMillis)}," +
                    "%.2f,%.2f,%.2f,%.1f,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%s,%.1f,%s".format(
                        log.generatedKwh, log.consumedKwh, log.netEnergyKwh,
                        log.solarCoveragePercent, log.exportedKwh, log.importedKwh,
                        log.moneySavedRupees, log.moneyEarnedRupees, log.totalBenefitRupees,
                        log.independenceScore, log.weatherCondition.label,
                        log.panelEfficiency, log.notes.replace(",", ";")
                    )
                )
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = requireContext().contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                uri?.let {
                    resolver.openOutputStream(it)?.use { os ->
                        OutputStreamWriter(os).use { w -> w.write(csv) }
                    }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(it, values, null, null)
                    showExportSuccess(it, fileName, logs.size)
                }
            } else {
                val file = java.io.File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    fileName
                )
                file.writeText(csv)
                showExportSuccess(Uri.fromFile(file), fileName, logs.size)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showExportSuccess(uri: Uri, fileName: String, count: Int) {
        Toast.makeText(
            requireContext(),
            "✅ Exported $count records to Downloads/$fileName",
            Toast.LENGTH_LONG
        ).show()
        // Offer to share via intent
        val share = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "text/csv")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (share.resolveActivity(requireContext().packageManager) != null) {
            startActivity(Intent.createChooser(share, "Open CSV with…"))
        }
    }

    private fun animateCount(tv: android.widget.TextView, fmt: String, target: Double) {
        ValueAnimator.ofFloat(0f, target.toFloat()).apply {
            duration = 1200; interpolator = DecelerateInterpolator()
            addUpdateListener { tv.text = fmt.format(it.animatedValue as Float) }
            start()
        }
    }

    private fun animateCards() {
        listOf(b.cardChart, b.cardTotals, b.cardFinancials, b.cardGrade, b.cardBill, b.cardDailyLogs)
            .forEachIndexed { i, c ->
                c.alpha = 0f; c.translationY = 60f
                c.animate().alpha(1f).translationY(0f)
                    .setDuration(500).setStartDelay(100L * i)
                    .setInterpolator(DecelerateInterpolator()).start()
            }
    }

    private fun pulse(v: View) {
        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80)
            .withEndAction { v.animate().scaleX(1f).scaleY(1f).setDuration(120).start() }.start()
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
