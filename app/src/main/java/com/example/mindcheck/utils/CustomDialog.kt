package com.mindcheck.app.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import com.mindcheck.app.R

/**
 * Custom Dialog Builder - Glassmorphic Style
 * Matches the app's design aesthetic
 */
class CustomDialog private constructor(
    private val context: Context
) {
    private var dialog: Dialog? = null
    private var title: String = ""
    private var message: String = ""
    private var positiveText: String = "OK"
    private var negativeText: String? = null
    private var positiveAction: (() -> Unit)? = null
    private var negativeAction: (() -> Unit)? = null
    private var customView: View? = null
    private var cancelable: Boolean = true
    private var items: Array<String>? = null
    private var itemClickListener: ((Int) -> Unit)? = null
    private var multiChoiceItems: Array<String>? = null
    private var checkedItems: BooleanArray? = null
    private var multiChoiceListener: ((Int, Boolean) -> Unit)? = null

    companion object {
        fun build(context: Context, block: CustomDialog.() -> Unit): Dialog {
            val builder = CustomDialog(context)
            builder.block()
            return builder.create()
        }
    }

    fun setTitle(title: String) {
        this.title = title
    }

    fun setMessage(message: String) {
        this.message = message
    }

    fun setPositiveButton(text: String, action: (() -> Unit)? = null) {
        this.positiveText = text
        this.positiveAction = action
    }

    fun setNegativeButton(text: String, action: (() -> Unit)? = null) {
        this.negativeText = text
        this.negativeAction = action
    }

    fun setCustomView(view: View) {
        this.customView = view
    }

    fun setCancelable(cancelable: Boolean) {
        this.cancelable = cancelable
    }

    fun setItems(items: Array<String>, listener: (Int) -> Unit) {
        this.items = items
        this.itemClickListener = listener
    }

    fun setMultiChoiceItems(
        items: Array<String>,
        checkedItems: BooleanArray,
        listener: (Int, Boolean) -> Unit
    ) {
        this.multiChoiceItems = items
        this.checkedItems = checkedItems
        this.multiChoiceListener = listener
    }

    private fun create(): Dialog {
        val dialog = Dialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_custom_base, null)

        // Set transparent background
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val titleView = view.findViewById<TextView>(R.id.dialog_title)
        val messageView = view.findViewById<TextView>(R.id.dialog_message)
        val contentContainer = view.findViewById<LinearLayout>(R.id.dialog_content)
        val btnPositive = view.findViewById<Button>(R.id.btn_positive)
        val btnNegative = view.findViewById<Button>(R.id.btn_negative)

        // Set title
        titleView.text = title

        // Set message
        if (message.isNotEmpty()) {
            messageView.text = message
            messageView.visibility = View.VISIBLE
        } else {
            messageView.visibility = View.GONE
        }

        // Handle custom view
        if (customView != null) {
            contentContainer.removeAllViews()
            contentContainer.addView(customView)
        }

        // Handle simple list items
        if (items != null) {
            contentContainer.removeAllViews()
            items?.forEachIndexed { index, item ->
                val itemView = LayoutInflater.from(context).inflate(
                    android.R.layout.simple_list_item_1,
                    contentContainer,
                    false
                ) as TextView

                itemView.text = item
                itemView.setTextColor(Color.WHITE)
                itemView.textSize = 16f
                itemView.setPadding(16, 24, 16, 24)
                itemView.setOnClickListener {
                    itemClickListener?.invoke(index)
                    dialog.dismiss()
                }

                // Add ripple effect
                val outValue = android.util.TypedValue()
                context.theme.resolveAttribute(
                    android.R.attr.selectableItemBackground,
                    outValue,
                    true
                )
                itemView.setBackgroundResource(outValue.resourceId)

                contentContainer.addView(itemView)
            }
        }

        // Handle multi-choice items
        if (multiChoiceItems != null && checkedItems != null) {
            contentContainer.removeAllViews()
            multiChoiceItems?.forEachIndexed { index, item ->
                val checkBox = CheckBox(context)
                checkBox.text = item
                checkBox.setTextColor(Color.WHITE)
                checkBox.textSize = 16f
                checkBox.setPadding(16, 16, 16, 16)
                checkBox.isChecked = checkedItems!![index]
                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    checkedItems!![index] = isChecked
                    multiChoiceListener?.invoke(index, isChecked)
                }

                // Checkbox button tint
                checkBox.buttonTintList = android.content.res.ColorStateList.valueOf(
                    Color.parseColor("#B4D96C")
                )

                contentContainer.addView(checkBox)
            }
        }

        // Set positive button
        btnPositive.text = positiveText
        btnPositive.setOnClickListener {
            positiveAction?.invoke()
            dialog.dismiss()
        }

        // Set negative button
        if (negativeText != null) {
            btnNegative.text = negativeText
            btnNegative.visibility = View.VISIBLE
            btnNegative.setOnClickListener {
                negativeAction?.invoke()
                dialog.dismiss()
            }
        } else {
            btnNegative.visibility = View.GONE
        }

        dialog.setContentView(view)
        dialog.setCancelable(cancelable)

        this.dialog = dialog
        return dialog
    }

    fun show(): Dialog {
        val d = create()
        d.show()
        return d
    }

    fun updatePositiveButtonText(text: String) {
        dialog?.findViewById<Button>(R.id.btn_positive)?.text = text
    }
}
