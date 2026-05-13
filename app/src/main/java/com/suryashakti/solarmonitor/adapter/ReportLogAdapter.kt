package com.suryashakti.solarmonitor.adapter

import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.suryashakti.solarmonitor.data.EnergyLog
import com.suryashakti.solarmonitor.databinding.ItemReportLogBinding
import com.suryashakti.solarmonitor.util.DateUtils
import com.suryashakti.solarmonitor.util.ThemeManager

class ReportLogAdapter :
    ListAdapter<EnergyLog, ReportLogAdapter.VH>(DIFF) {

    private val expandedPositions = mutableSetOf<Long>()

    inner class VH(private val b: ItemReportLogBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(log: EnergyLog) {
            val isExpanded = expandedPositions.contains(log.id)

            // ── Summary row (always visible) ─────────────────────────────
            b.tvReportDate.text    = DateUtils.toDisplayString(log.dateMillis)
            b.tvReportWeather.text = log.weatherCondition.label
            b.tvReportGenerated.text = "☀️ %.2f kWh".format(log.generatedKwh)
            b.tvReportConsumed.text  = "🏠 %.2f kWh".format(log.consumedKwh)
            b.tvReportSavings.text   = "₹%.2f".format(log.totalBenefitRupees)
            b.tvReportScore.text     = "${log.independenceScore}%"
            ThemeManager.applyToViewTree(b.root)
            b.tvReportScore.setTextColor(
                when {
                    log.independenceScore >= 75 -> 0xFF00E676.toInt()
                    log.independenceScore >= 50 -> 0xFFFFD700.toInt()
                    else -> 0xFFFF5252.toInt()
                }
            )
            b.ivExpandArrow.rotation = if (isExpanded) 180f else 0f

            // ── Expanded detail section ───────────────────────────────────
            b.layoutExpanded.visibility = if (isExpanded) View.VISIBLE else View.GONE

            if (isExpanded) {
                b.tvDetailNetEnergy.text =
                    if (log.netEnergyKwh >= 0) "+%.2f kWh (Exporting)".format(log.netEnergyKwh)
                    else "%.2f kWh (Importing)".format(log.netEnergyKwh)
                b.tvDetailNetEnergy.setTextColor(
                    if (log.netEnergyKwh >= 0) 0xFF00E676.toInt() else 0xFFFF5252.toInt()
                )

                b.tvDetailSolarCoverage.text  = "Solar Coverage:  %.1f%%".format(log.solarCoveragePercent)
                b.tvDetailExported.text       = "Exported to Grid:  %.2f kWh".format(log.exportedKwh)
                b.tvDetailImported.text       = "Imported from Grid:  %.2f kWh".format(log.importedKwh)
                b.tvDetailGridSaved.text      = "Grid Cost Saved:  ₹%.2f".format(log.moneySavedRupees)
                b.tvDetailExportEarned.text   = "Export Earned:  ₹%.2f".format(log.moneyEarnedRupees)
                b.tvDetailTotalBenefit.text   = "Total Benefit:  ₹%.2f".format(log.totalBenefitRupees)
                b.tvDetailEfficiency.text     = "Panel Efficiency:  %.1f%%".format(log.panelEfficiency)
                b.tvDetailRate.text           = "Grid Rate:  ₹%.2f/kWh".format(log.perUnitRate)
                b.tvDetailExportRate.text     = "Export Rate:  ₹%.2f/kWh".format(log.exportRate)
                b.tvDetailCapacity.text       = "Panel Capacity:  %.1f kW".format(log.panelCapacityKw)
                b.tvDetailCo2.text            = "CO₂ Saved:  %.2f kg".format(log.generatedKwh * 0.82)

                if (log.notes.isNotBlank()) {
                    b.tvDetailNotes.visibility = View.VISIBLE
                    b.tvDetailNotes.text = "📝 ${log.notes}"
                } else {
                    b.tvDetailNotes.visibility = View.GONE
                }
            }

            // Toggle expand on click
            b.root.setOnClickListener {
                val wasExpanded = expandedPositions.contains(log.id)
                if (wasExpanded) expandedPositions.remove(log.id)
                else expandedPositions.add(log.id)
                notifyItemChanged(bindingAdapterPosition)
            }

            // Entry animation
            b.root.alpha = 0f
            b.root.animate().alpha(1f).setDuration(250).start()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemReportLogBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<EnergyLog>() {
            override fun areItemsTheSame(a: EnergyLog, b: EnergyLog) = a.id == b.id
            override fun areContentsTheSame(a: EnergyLog, b: EnergyLog) = a == b
        }
    }
}
