package com.example.descargadortiktok.network.models

import com.google.gson.annotations.SerializedName

data class TikWmResponse(
    @SerializedName("code") val code: Int,
    @SerializedName("msg") val msg: String,
    @SerializedName("data") val data: VideoData?
)

data class VideoData(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("play") val play: String, // No watermark video (also rendered video for slideshows)
    @SerializedName("wmplay") val wmplay: String, // With watermark video
    @SerializedName("music") val music: String, // Audio only URL
    @SerializedName("cover") val cover: String,
    @SerializedName("origin_cover") val originCover: String,
    @SerializedName("images") val images: List<String>? = null // List of photos if it's a slideshow
)