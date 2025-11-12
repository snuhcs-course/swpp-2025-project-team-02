package com.example.fortuna_android.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.fortuna_android.R
import com.google.android.material.button.MaterialButton

/**
 * Dialog for displaying raw VLM output and classification results
 */
class VLMOutputDialog : DialogFragment() {

    private var detectedObject: DetectedObject? = null

    companion object {
        private const val ARG_OBJECT_ID = "object_id"
        private const val ARG_LABEL = "label"
        private const val ARG_CONFIDENCE = "confidence"
        private const val ARG_ELEMENT = "element"
        private const val ARG_RAW_OUTPUT = "raw_output"
        private const val ARG_ERROR = "error"

        /**
         * Create a new instance of VLMOutputDialog
         */
        fun newInstance(detectedObject: DetectedObject): VLMOutputDialog {
            val dialog = VLMOutputDialog()
            val args = Bundle().apply {
                putString(ARG_OBJECT_ID, detectedObject.id)
                putString(ARG_LABEL, detectedObject.label)
                putFloat(ARG_CONFIDENCE, detectedObject.confidence)
                putString(ARG_ELEMENT, detectedObject.classifiedElement?.displayName)
                putString(ARG_RAW_OUTPUT, detectedObject.rawVlmOutput)
                putString(ARG_ERROR, detectedObject.vlmError)
            }
            dialog.arguments = args
            return dialog
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_vlm_output, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get arguments
        val label = arguments?.getString(ARG_LABEL) ?: "Unknown"
        val confidence = arguments?.getFloat(ARG_CONFIDENCE) ?: 0f
        val element = arguments?.getString(ARG_ELEMENT)
        val rawOutput = arguments?.getString(ARG_RAW_OUTPUT)
        val error = arguments?.getString(ARG_ERROR)

        // Bind views
        val titleText = view.findViewById<TextView>(R.id.dialogTitle)
        val detectionInfoText = view.findViewById<TextView>(R.id.detectionInfoText)
        val elementText = view.findViewById<TextView>(R.id.elementText)
        val rawOutputText = view.findViewById<TextView>(R.id.rawOutputText)
        val closeButton = view.findViewById<MaterialButton>(R.id.closeButton)

        // Set title
        titleText.text = "VLM Analysis Results"

        // Set detection info
        detectionInfoText.text = "Detected: $label (${(confidence * 100).toInt()}% confidence)"

        // Set element result
        if (error != null) {
            elementText.text = "Classification Failed"
            elementText.setTextColor(resources.getColor(android.R.color.holo_red_light, null))
        } else {
            elementText.text = "Element: ${element ?: "Unknown"}"
            elementText.setTextColor(resources.getColor(android.R.color.holo_green_light, null))
        }

        // Set raw output
        if (error != null) {
            rawOutputText.text = "Error: $error"
            rawOutputText.setTextColor(resources.getColor(android.R.color.holo_red_light, null))
        } else {
            rawOutputText.text = rawOutput ?: "No output available"
            rawOutputText.setTextColor(resources.getColor(android.R.color.white, null))
        }

        // Close button
        closeButton.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        // Make dialog full width with some margin
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
