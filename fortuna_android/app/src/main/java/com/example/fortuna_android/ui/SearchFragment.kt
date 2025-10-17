package com.example.fortuna_android.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.example.fortuna_android.util.CustomToast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.fortuna_android.api.RetrofitClient
import com.example.fortuna_android.databinding.FragmentSearchBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val TAG = "SearchFragment"
        private const val PREFS_NAME = "fortuna_prefs"
        private const val KEY_TOKEN = "jwt_token"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load images when fragment is created
        loadImagesFromApi()
    }

    private fun loadImagesFromApi() {
        // Safe binding access - check if view is still available
        val binding = _binding ?: return

        // Show loading indicator
        binding.progressBar.visibility = View.VISIBLE
        binding.imageGrid.visibility = View.GONE
        binding.tvError.visibility = View.GONE

        // Get current date in yyyy-MM-dd format as required by API
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        // Get JWT token from SharedPreferences
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val accessToken = prefs.getString(KEY_TOKEN, null)
        Log.d(TAG, "Accesstoken: $accessToken")
        if (accessToken.isNullOrEmpty()) {
            showError("Authentication required. Please log in again.")
            return
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getImages(currentDate)

                // Check binding before updating UI
                val currentBinding = _binding ?: return@launch

                if (response.isSuccessful && response.body() != null) {
                    val imageResponse = response.body()!!

                    if (imageResponse.status == "success" && imageResponse.data.images.isNotEmpty()) {
                        // Hide loading, show images
                        currentBinding.progressBar.visibility = View.GONE
                        currentBinding.imageGrid.visibility = View.VISIBLE

                        // Load images into ImageViews
                        loadImagesIntoViews(imageResponse.data.images, imageResponse.data.count)

                        Log.d(TAG, "Successfully loaded ${imageResponse.data.count} images for date ${imageResponse.data.date}")
                    } else {
                        showError("No images available")
                    }
                } else {
                    showError("Failed to load images: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading images", e)
                showError("Network error: ${e.message}")
            }
        }
    }

    private fun loadImagesIntoViews(images: List<com.example.fortuna_android.api.ImageItem>, count: Int) {
        // Safe binding access - check if view is still available
        val binding = _binding ?: return

        val imageViews = listOf(
            binding.imageView1,
            binding.imageView2,
            binding.imageView3,
            binding.imageView4
        )

        // Load up to 4 images
        for (i in 0 until minOf(count, 4)) {
            loadImageWithGlide(imageViews[i], images[i].url)
        }

        // If we have fewer than 4 images, hide the extra ImageViews
        for (i in images.size until 4) {
            imageViews[i].visibility = View.GONE
        }
    }

    private fun loadImageWithGlide(imageView: ImageView, imageUrl: String) {
        Glide.with(this)
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_menu_close_clear_cancel)
            .into(imageView)
    }

    private fun showError(message: String) {
        // Safe binding access - check if view is still available
        val binding = _binding ?: return

        binding.progressBar.visibility = View.GONE
        binding.imageGrid.visibility = View.GONE
        binding.tvError.visibility = View.VISIBLE
        binding.tvError.text = message

        // Check if fragment is still attached before showing toast
        if (isAdded) {
            CustomToast.show(requireContext(), message)
        }
        Log.e(TAG, message)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
