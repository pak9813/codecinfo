package com.parseus.codecinfo.ui.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.divider.MaterialDividerItemDecoration
import com.google.android.material.snackbar.Snackbar
import com.parseus.codecinfo.R
import com.parseus.codecinfo.data.InfoType
import com.parseus.codecinfo.data.codecinfo.CodecSimpleInfo
import com.parseus.codecinfo.data.codecinfo.getSimpleCodecInfoList
import com.parseus.codecinfo.data.drm.DrmSimpleInfo
import com.parseus.codecinfo.data.drm.getSimpleDrmInfoList
import com.parseus.codecinfo.databinding.TabContentLayoutBinding
import com.parseus.codecinfo.ui.MainActivity
import com.parseus.codecinfo.ui.adapters.CodecAdapter
import com.parseus.codecinfo.ui.adapters.DrmAdapter
import com.parseus.codecinfo.ui.adapters.SearchListenerDestroyedListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal var emptyListInformed = false

class ItemFragment : Fragment(), SearchView.OnQueryTextListener {

    private var _binding: TabContentLayoutBinding? = null
    private val binding get() = _binding!!

    private var emptyList = false

    var searchListenerDestroyedListener: SearchListenerDestroyedListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = TabContentLayoutBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onDestroyView() {
        searchListenerDestroyedListener?.onSearchListenerDestroyed(this)
        searchListenerDestroyedListener = null

        if (activity as? MainActivity != null) {
            val searchListenerList = (activity as MainActivity).searchListeners
            searchListenerList.remove(this)
        }

        _binding = null

        super.onDestroyView()
    }

    @SuppressLint("NewApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            binding.loadingProgress.isVisible = true

            val infoType = InfoType.fromInt(requireArguments().getInt("infoType"))
            val itemAdapter = withContext(Dispatchers.IO) {
                if (infoType != InfoType.DRM) {
                    val codecSimpleInfoList = getSimpleCodecInfoList(requireContext(),
                        infoType == InfoType.Audio)
                    if (codecSimpleInfoList.isEmpty()) emptyList = true
                    CodecAdapter().also {
                        if (!emptyList) {
                            it.add(codecSimpleInfoList)
                        }
                    }
                } else {
                    val drmSimpleInfoList = getSimpleDrmInfoList(requireContext())
                    if (drmSimpleInfoList.isEmpty()) emptyList = true
                    DrmAdapter(drmSimpleInfoList)
                }
            }

            binding.loadingProgress.isVisible = false

            if (!emptyList) {
                binding.simpleCodecListView.apply {
                    layoutManager = LinearLayoutManager(context)
                    adapter = itemAdapter
                    ViewCompat.setNestedScrollingEnabled(this, false)
                    addItemDecoration(MaterialDividerItemDecoration(context, MaterialDividerItemDecoration.VERTICAL))
                }
            } else if (!emptyListInformed) {
                // Do not spam the user with multiple snackbars.
                emptyListInformed = true
                val errorId = if (InfoType.currentInfoType != InfoType.DRM)
                    R.string.unable_to_get_codec_info_error
                else R.string.unable_to_get_drm_info_error
                Snackbar.make(requireActivity().findViewById(android.R.id.content),
                    errorId, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onQueryTextChange(newText: String): Boolean {
        if (isVisible) {
            handleSearch(newText)
        }
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        if (isVisible) {
            handleSearch(query)
        }
        return true
    }

    @SuppressLint("NewApi")
    private fun handleSearch(query: String) {
        if (emptyList) {
            return
        }

        val adapter = binding.simpleCodecListView.adapter
        if (adapter is CodecAdapter) {
            val fullList = getSimpleCodecInfoList(requireContext(), InfoType.currentInfoType == InfoType.Audio)
            adapter.replaceAll(filterCodecs(fullList, query))
        } else {
            (adapter as DrmAdapter)
            val fullList = getSimpleDrmInfoList(requireContext())
            adapter.replaceAll(filterDrm(fullList, query))
        }
    }

    private fun filterCodecs(infoList: List<CodecSimpleInfo>, query: String): List<CodecSimpleInfo> {
        return infoList.filter {
            it.codecId.contains(query, true) || it.codecName.contains(query, true)
        }
    }

    private fun filterDrm(infoList: List<DrmSimpleInfo>, query: String): List<DrmSimpleInfo> {
        return infoList.filter {
            it.drmName.contains(query, true)
        }
    }

}