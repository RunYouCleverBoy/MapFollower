package com.playgrounds.mapfollower.history.ui

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.Geofence
import com.playgrounds.mapfollower.R
import com.playgrounds.mapfollower.history.model.HistoryItem
import java.text.SimpleDateFormat
import java.util.*

// TODO: In real life we may need to consider PagingDataAdapter
class HistoryAdapter(private val onClick: (HistoryItem) -> Unit) : ListAdapter<HistoryItem, HistoryAdapter.Holder>(ItemDiff()) {
    class ItemDiff : DiffUtil.ItemCallback<HistoryItem>() {
        override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean = oldItem == newItem
        override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean = oldItem == newItem
    }

    class Holder(view: View, private val onClick: (HistoryItem) -> Unit) : RecyclerView.ViewHolder(view) {
        private val timeDateFormatter = SimpleDateFormat("MM.dd HH:mm:ss", Locale.ROOT)
        private val timeStampText: TextView = view.findViewById(R.id.history_date)
        private val locationText: TextView = view.findViewById(R.id.history_geolocation)
        private val transitionTypeIcon: AppCompatImageView = view.findViewById(R.id.transition_type_icon)
        fun bind(item: HistoryItem?) {
            if (item != null) {
                itemView.setOnClickListener { onClick.invoke(item) }
                timeStampText.text = timeDateFormatter.format(item.timeStamp)
                locationText.text = itemView.context.getString(R.string.lat_lon_format, item.lat, item.lon)
            } else {
                itemView.setOnClickListener { }
                timeStampText.text = ""
                locationText.text = itemView.context.getString(R.string.location_not_available)
            }
            transitionTypeIcon.setImageResource(
                when (item?.crossingType) {
                    Geofence.GEOFENCE_TRANSITION_ENTER -> R.drawable.ic_baseline_flight_land_24
                    Geofence.GEOFENCE_TRANSITION_EXIT -> R.drawable.ic_baseline_flight_takeoff_24
                    else -> R.drawable.ic_baseline_map_24
                }
            )
        }
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(parent.inflateChild(R.layout.fragment_history_cell)!!, onClick)

    private fun ViewGroup.inflateChild(@LayoutRes layoutId: Int) = (this.context as? Activity)?.layoutInflater?.inflate(layoutId, this, false)
}
