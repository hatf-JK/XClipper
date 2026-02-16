package com.kpstv.xclipper.ui.adapter

import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.kpstv.xclipper.data.converters.DateFormatConverter
import com.kpstv.xclipper.data.model.Clip
import com.kpstv.xclipper.data.model.ClipTag
import com.kpstv.xclipper.extensions.*
import com.kpstv.xclipper.extensions.utils.ClipUtils
import com.kpstv.xclipper.feature_home.R
import com.kpstv.xclipper.feature_home.databinding.ItemClipBinding
import com.kpstv.xclipper.ui.helpers.AppThemeHelper.CARD_CLICK_COLOR
import com.kpstv.xclipper.ui.helpers.AppThemeHelper.CARD_COLOR
import com.kpstv.xclipper.ui.helpers.AppThemeHelper.CARD_SELECTED_COLOR

data class ClipAdapterItem constructor(
    val clip: Clip,
    var expanded: Boolean = false,
    var selected: Boolean = false,
    var selectedClipboard: Boolean = false,
    var multiSelectionState: Boolean = false
) {
    companion object {
        fun from(clip: Clip) = ClipAdapterItem(clip = clip)

        fun List<ClipAdapterItem>.toClips(): List<Clip> = map { it.clip }
    }
}

class ClipAdapter(
    private val onClick: (ClipAdapterItem, Int) -> Unit,
    private val onLongClick: (ClipAdapterItem, Int) -> Unit,
) : ListAdapter<ClipAdapterItem, ClipAdapterHolder>(DiffCallback.asConfig(isBackground = true)) {

    private object DiffCallback : DiffUtil.ItemCallback<ClipAdapterItem>() {
        override fun areItemsTheSame(oldItem: ClipAdapterItem, newItem: ClipAdapterItem): Boolean =
            oldItem.clip.data == newItem.clip.data

        override fun areContentsTheSame(oldItem: ClipAdapterItem, newItem: ClipAdapterItem): Boolean = oldItem.clip == newItem.clip
    }

    private val TAG = javaClass.simpleName

    private lateinit var copyClick: (Clip, Int) -> Unit
    private lateinit var menuClick: (Clip, Int, MenuType) -> Unit

    private var trimClipText : Boolean = false
    private var loadImageMarkdownText : Boolean = true

    fun setTextTrimmingEnabled(value : Boolean) {
        trimClipText = value
    }

    fun setIsLoadingMarkdownEnabled(value: Boolean) {
        loadImageMarkdownText = value
    }

    private fun getSafeItem(position: Int) : ClipAdapterItem? {
        if (position == RecyclerView.NO_POSITION) return null
        return getItem(position)
    }

    override fun getItemViewType(position: Int) = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClipAdapterHolder {
        return ClipAdapterHolder(ItemClipBinding.inflate(parent.context.layoutInflater(), parent, false)).apply {
            with(binding) {
                mainCard.setOnClickListener call@{
                    val clip = getSafeItem(bindingAdapterPosition) ?: return@call
                    onClick.invoke(clip, bindingAdapterPosition)
                }
                mainCard.setOnLongClickListener call@{
                    val clip = getItem(bindingAdapterPosition) ?: return@call false
                    onLongClick.invoke(clip, bindingAdapterPosition)
                    true
                }
                ciCopyButton.setOnClickListener {
                    val clip = getItem(bindingAdapterPosition).clip
                    copyClick.invoke(clip, bindingAdapterPosition)
                }
                ciBtnEdit.setOnClickListener {
                    val clip = getItem(bindingAdapterPosition).clip
                    menuClick.invoke(clip, bindingAdapterPosition, MenuType.Edit)
                }
                ciBtnPin.setOnClickListener {
                    val clip = getItem(bindingAdapterPosition).clip
                    menuClick.invoke(clip, bindingAdapterPosition, MenuType.Pin)
                }
                ciBtnSpecial.setOnClickListener {
                    val clip = getItem(bindingAdapterPosition).clip
                    menuClick.invoke(clip, bindingAdapterPosition, MenuType.Special)
                }
                ciBtnShare.setOnClickListener {
                    val clip = getItem(bindingAdapterPosition).clip
                    menuClick.invoke(clip, bindingAdapterPosition, MenuType.Share)
                }
                ciBtnEncrypt.setOnClickListener {
                    val clip = getItem(bindingAdapterPosition).clip
                    menuClick.invoke(clip, bindingAdapterPosition, MenuType.Encrypt)
                }
                ciBtnMask.setOnClickListener {
                    val clip = getItem(bindingAdapterPosition).clip
                    menuClick.invoke(clip, bindingAdapterPosition, MenuType.Mask)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: ClipAdapterHolder, position: Int, payloads: MutableList<Any>) {
        val clipAdapterItem = getItem(position)
        for (payload in payloads) {
            when(payload) {
                ClipAdapterHolder.Payloads.UpdateExpandedState -> holder.applyForExpandedItem(clipAdapterItem)
                ClipAdapterHolder.Payloads.UpdateSelectedState -> holder.applyForSelectedItem(clipAdapterItem)
                ClipAdapterHolder.Payloads.UpdateMultiSelectionState -> holder.applyForMultiSelectionState(clipAdapterItem)
                ClipAdapterHolder.Payloads.UpdateCurrentClipboardText -> holder.applyForCurrentClipboardText(clipAdapterItem)
            }
        }
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: ClipAdapterHolder, position: Int) = with(holder) {
        val clipAdapterItem = getItem(position)
        val clip = clipAdapterItem.clip

        if (clip.isMasked) {
             binding.ciTextView.text = "******"
        } else if (clip.isAdHocEncrypted) {
             binding.ciTextView.text = binding.root.context.getString(R.string.encrypted_content)
        } else {
             binding.ciTextView.text = if (trimClipText) clip.data.trim() else clip.data
        }
        
        binding.root.tag = clip.id // used for unsubscribing.

        if (clip.isPinned) {
            binding.icPinView.show()
        } else {
            binding.icPinView.hide()
        }

        if (loadImageMarkdownText && !clip.isMasked && !clip.isAdHocEncrypted) {
            renderImageMarkdown(clip.data)
        } else {
            binding.ciImageView.collapse()
            binding.ciTextView.show()
        }

        binding.ciTimeText.text = DateFormatConverter.getFormattedDate(clip.time)

        updatePinButton(clip)
        updateEncryptButton(clip)
        updateMaskButton(clip)

        updateTags(clip)
        updateHolderTags(clip)

        applyForCurrentClipboardText(clipAdapterItem)
        applyForExpandedItem(clipAdapterItem)
        applyForMultiSelectionState(clipAdapterItem)
        applyForSelectedItem(clipAdapterItem)
    }

    // ... (helper methods)

    fun applyForExpandedItem(clipAdapterItem: ClipAdapterItem): Unit = with(binding) {
        updatePinButton(clipAdapterItem.clip)
        updateEncryptButton(clipAdapterItem.clip)
        updateMaskButton(clipAdapterItem.clip)
        if (clipAdapterItem.expanded) {
            hiddenLayout.show()
            mainCard.setCardBackgroundColor(CARD_CLICK_COLOR)
            mainCard.cardElevation = context.toPx(3)
        } else {
            mainCard.setCardBackgroundColor(CARD_COLOR)
            mainCard.cardElevation = context.toPx(0)
            hiddenLayout.collapse()
        }
    }

    // ...

    fun updatePinButton(clip: Clip): Unit = with(binding) {
        if (clip.isPinned) {
            setButtonDrawable(ciBtnPin, R.drawable.ic_unpin)
            ciBtnPin.text = context.getString(R.string.unpin)
            ciPinImage.show()
        } else {
            setButtonDrawable(ciBtnPin, R.drawable.ic_pin)
            ciBtnPin.text = context.getString(R.string.pin)
            ciPinImage.collapse()
        }
    }

    fun updateEncryptButton(clip: Clip): Unit = with(binding) {
        if (clip.isAdHocEncrypted) {
            ciBtnEncrypt.text = context.getString(R.string.decrypt)
            // setButtonDrawable(ciBtnEncrypt, R.drawable.ic_lock_open) // If available
        } else {
            ciBtnEncrypt.text = context.getString(R.string.encrypt)
            setButtonDrawable(ciBtnEncrypt, R.drawable.fh_ic_lock)
        }
    }

    fun updateMaskButton(clip: Clip): Unit = with(binding) {
        if (clip.isMasked) {
             ciBtnMask.text = context.getString(R.string.unmask)
             setButtonDrawable(ciBtnMask, R.drawable.ic_content_unmask) 
        } else {
             ciBtnMask.text = context.getString(R.string.mask)
             setButtonDrawable(ciBtnMask, R.drawable.ic_content_mask)
        }
    }

    private fun setButtonDrawable(view: TextView, @DrawableRes imageId: Int) {
        view.setCompoundDrawablesWithIntrinsicBounds(
            null, ContextCompat.getDrawable(view.context, imageId), null, null
        )
    }

    fun updateTags(clip: Clip): Unit = with(binding) {
        ciTagLayout.removeAllViews()
        clip.tags?.keys()?.forEach mainLoop@{ key ->
            if (key.isNotBlank()) {
                val textView = root.context.layoutInflater().inflate(R.layout.item_tag, null) as TextView
                val layoutParams = FlexboxLayout.LayoutParams(
                    FlexboxLayout.LayoutParams.WRAP_CONTENT,
                    FlexboxLayout.LayoutParams.WRAP_CONTENT
                )

                layoutParams.topMargin = 2
                layoutParams.bottomMargin = 2
                layoutParams.marginEnd = 5
                layoutParams.marginStart = 5
                textView.layoutParams = layoutParams
                textView.text = key

                ciTagLayout.addView(textView)
            }
        }
    }

    fun renderImageMarkdown(data: String) : Unit = with(binding) {
        if (ClipUtils.isMarkdownImage(data)) {
            val imageUrl = ClipUtils.getMarkdownImageUrl(data)

            ciImageView.show()

            ciImageView.load(
                uri = imageUrl,
                onSuccess = {
                    ciTextView.hide()
                },
                onError = {
                    ciImageView.collapse()
                    ciTextView.show()
                }
            )
        } else {
            ciTextView.show()
            ciImageView.collapse()
        }
    }

    fun updateHolderTags(clip: Clip) {
        tag.isSwipeEnabled = (clip.tags?.none { it.key == ClipTag.LOCK.small() } == true)
    }

    enum class Payloads {
        UpdateExpandedState,
        UpdateSelectedState,
        UpdateMultiSelectionState,
        UpdateCurrentClipboardText
    }
}

