package com.suryashakti.solarmonitor.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.suryashakti.solarmonitor.data.LocationSearchResult
import com.suryashakti.solarmonitor.databinding.ItemLocationResultBinding
import com.suryashakti.solarmonitor.util.ThemeManager

class LocationResultAdapter(
    private val onSelect: (LocationSearchResult) -> Unit
) : ListAdapter<LocationSearchResult, LocationResultAdapter.VH>(DIFF) {

    inner class VH(private val b: ItemLocationResultBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(item: LocationSearchResult) {
            b.tvLocationName.text  = item.shortName
            b.tvLocationDetail.text = item.displayName
            b.tvLocationType.text  = when (item.type) {
                "city", "town", "village" -> "🏙️ ${item.type.replaceFirstChar { it.uppercase() }}"
                "suburb", "neighbourhood" -> "🏘️ ${item.type.replaceFirstChar { it.uppercase() }}"
                "administrative"          -> "📍 Region"
                "industrial"              -> "🏭 Industrial"
                else                      -> "📍 ${item.type.ifBlank { "Location" }.replaceFirstChar { it.uppercase() }}"
            }
            b.tvCoords.text = "%.4f°, %.4f°".format(item.latitude, item.longitude)
            ThemeManager.applyToViewTree(b.root)
            b.root.setOnClickListener {
                b.root.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80)
                    .withEndAction {
                        b.root.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                    }.start()
                onSelect(item)
            }
            b.root.alpha = 0f
            b.root.animate().alpha(1f).setDuration(250).start()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemLocationResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<LocationSearchResult>() {
            override fun areItemsTheSame(a: LocationSearchResult, b: LocationSearchResult) =
                a.latitude == b.latitude && a.longitude == b.longitude
            override fun areContentsTheSame(a: LocationSearchResult, b: LocationSearchResult) =
                a == b
        }
    }
}
