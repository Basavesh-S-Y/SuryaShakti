package com.suryashakti.solarmonitor.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.suryashakti.solarmonitor.data.EnergyLog
import com.suryashakti.solarmonitor.databinding.ItemEnergyLogBinding
import com.suryashakti.solarmonitor.util.DateUtils
import com.suryashakti.solarmonitor.util.ThemeManager

class EnergyLogAdapter(
    private val onLongClick: (EnergyLog) -> Unit
) : ListAdapter<EnergyLog, EnergyLogAdapter.ViewHolder>(DIFF_CALLBACK) {

    inner class ViewHolder(private val binding: ItemEnergyLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(log: EnergyLog) {
            binding.tvDate.text = DateUtils.toDisplayString(log.dateMillis)
            binding.tvWeather.text = log.weatherCondition.label
            binding.tvGenerated.text = "⚡ %.2f kWh".format(log.generatedKwh)
            binding.tvConsumed.text = "🏠 %.2f kWh".format(log.consumedKwh)
            binding.tvSavings.text = "₹%.2f".format(log.totalBenefitRupees)
            binding.tvScore.text = "${log.independenceScore}%"
            ThemeManager.applyToViewTree(binding.root)

            // Color score
            val scoreColor = when {
                log.independenceScore >= 75 -> 0xFF00E676.toInt()
                log.independenceScore >= 50 -> 0xFFFFD700.toInt()
                else -> 0xFFFF5252.toInt()
            }
            binding.tvScore.setTextColor(scoreColor)

            // Net energy indicator
            if (log.netEnergyKwh >= 0) {
                binding.tvNetBadge.text = "▲ EXPORT"
                binding.tvNetBadge.setBackgroundColor(0xFF1B5E20.toInt())
                binding.tvNetBadge.setTextColor(0xFF00E676.toInt())
            } else {
                binding.tvNetBadge.text = "▼ IMPORT"
                binding.tvNetBadge.setBackgroundColor(0xFF4A0000.toInt())
                binding.tvNetBadge.setTextColor(0xFFFF5252.toInt())
            }

            binding.root.setOnLongClickListener {
                onLongClick(log)
                true
            }

            // Animate row on bind
            binding.root.alpha = 0f
            binding.root.animate().alpha(1f).setDuration(300).start()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEnergyLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<EnergyLog>() {
            override fun areItemsTheSame(old: EnergyLog, new: EnergyLog) = old.id == new.id
            override fun areContentsTheSame(old: EnergyLog, new: EnergyLog) = old == new
        }
    }
}
