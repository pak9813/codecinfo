package com.parseus.codecinfo.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.fragment.app.commit
import androidx.recyclerview.widget.RecyclerView
import com.parseus.codecinfo.R
import com.parseus.codecinfo.buildContainerTransform
import com.parseus.codecinfo.codecinfo.CodecSimpleInfo
import com.parseus.codecinfo.databinding.CodecAdapterRowBinding
import com.parseus.codecinfo.fragments.DetailsFragment
import com.parseus.codecinfo.isInTwoPaneMode

class CodecAdapter(private val codecList: List<CodecSimpleInfo>) : RecyclerView.Adapter<CodecAdapter.CodecInfoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CodecInfoViewHolder {
        val binding = CodecAdapterRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CodecInfoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CodecInfoViewHolder, position: Int) {
        val codecInfoItem = codecList[position]
        holder.bindCodecInfo(codecInfoItem, position)
    }

    override fun getItemCount() = codecList.size

    class CodecInfoViewHolder(binding: CodecAdapterRowBinding) : RecyclerView.ViewHolder(binding.root) {

        private val layout = binding.simpleCodecRow
        private val codecId = binding.codecName
        private val codecName = binding.codecFullName
        private val codecType = binding.codecType
        private val moreInfo = binding.moreInfo

        fun bindCodecInfo(codecInfo: CodecSimpleInfo, position: Int) {
            codecId.text = codecInfo.codecId
            codecName.text = codecInfo.codecName
            codecType.text = itemView.resources.getString(
                    if (codecInfo.isEncoder) R.string.encoder else R.string.decoder)
            if (itemView.context.isInTwoPaneMode()) {
                moreInfo.visibility = View.GONE
            }
            layout.contentDescription = layout.context.getString(R.string.codec_row_content_description,
                    position, codecName, codecId)

            ViewCompat.setTransitionName(layout, "$codecId/$codecName")
            layout.setOnClickListener {
                val activity = (layout.context as AppCompatActivity)

                // Do not create the same fragment again.
                activity.supportFragmentManager
                        .findFragmentByTag(activity.getString(R.string.details_fragment_tag))?.let {
                            (it as DetailsFragment)
                            if (codecInfo.codecId == it.codecId && codecInfo.codecName == it.codecName) {
                                return@setOnClickListener
                            }
                        }

                val detailsFragment = DetailsFragment().also { fragment ->
                    fragment.arguments = bundleOf(
                            "codecId" to codecInfo.codecId,
                            "codecName" to codecInfo.codecName
                    )
                    if (!activity.isInTwoPaneMode()) {
                        fragment.sharedElementEnterTransition = buildContainerTransform(layout, true)
                        fragment.sharedElementReturnTransition = buildContainerTransform(layout, false)
                    }
                }

                activity.supportFragmentManager.commit {
                    setReorderingAllowed(true)
                    if (activity.isInTwoPaneMode()) {
                        replace(R.id.itemDetailsFragment, detailsFragment,
                                activity.getString(R.string.details_fragment_tag))
                    } else {
                        addSharedElement(layout, ViewCompat.getTransitionName(layout)!!)
                        replace(R.id.content_fragment, detailsFragment,
                                activity.getString(R.string.details_fragment_tag))
                        addToBackStack(null)

                        activity.supportActionBar!!.apply {
                            setDisplayHomeAsUpEnabled(true)
                            setHomeButtonEnabled(true)
                            setHomeActionContentDescription(R.string.close_details)
                        }
                    }
                }
            }
        }

    }

}