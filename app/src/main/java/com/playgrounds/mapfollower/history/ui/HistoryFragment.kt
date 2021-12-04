package com.playgrounds.mapfollower.history.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.LatLng
import com.playgrounds.mapfollower.R
import com.playgrounds.mapfollower.history.model.HistoryItem
import com.playgrounds.mapfollower.history.viewmodel.HistoryViewModel
import com.playgrounds.mapfollower.misc.MainViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {
    private lateinit var viewModel: HistoryViewModel
    private val activityViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val context = requireContext()
        val recycler: RecyclerView = view.findViewById(R.id.history_list)

        @Suppress("ReplaceGetOrSet")
        viewModel = ViewModelProvider(this, ViewModelProvider.AndroidViewModelFactory(requireActivity().application))
            .get(HistoryViewModel::class.java)

        recycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        val historyAdapter = HistoryAdapter(this::onHistoryItemClicked)
        recycler.adapter = historyAdapter
        historyAdapter.registerAdapterDataObserver(DataObserver(recycler))

        lifecycleScope.launch {
            viewModel.getDataFlow().collect {
                historyAdapter.submitList(it)
            }
        }
    }

    private fun onHistoryItemClicked(item: HistoryItem) {
        activityViewModel.selectedEvent.tryEmit(LatLng(item.lat, item.lon))
    }

    class DataObserver(private val recyclerView: RecyclerView) : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            scrollToStart()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            scrollToStart()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            scrollToStart()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            scrollToStart()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            scrollToStart()
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            scrollToStart()
        }

        private fun scrollToStart() {
            recyclerView.scrollToPosition(0)
        }
    }
}
