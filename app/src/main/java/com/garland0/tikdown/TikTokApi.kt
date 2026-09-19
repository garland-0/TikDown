package com.garland0.tikdown

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Talks to the tikwm.com public API to resolve a TikTok share link into either
 * a direct (no-watermark) video URL or a list of image URLs for slideshow posts.
 */
object TikTokApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    sealed class Result {
        data class Video(val playUrl: String) : Result()
        data class Images(val imageUrls: List<String>) : Result()
        data class Error(val message: String) : Result()
    }

    /** Blocking call — always invoke from a background dispatcher (e.g. Dispatchers.IO). */
    fun resolve(tiktokUrl: String): Result {
        return try {
            val httpUrl = "https://www.tikwm.com/api/".toHttpUrl()
                .newBuilder()
                .addQueryParameter("url", tiktokUrl)
                .build()

            val request = Request.Builder().url(httpUrl).build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                    ?: return Result.Error("Empty response from server")

                val json = JSONObject(bodyString)
                val data = json.optJSONObject("data")
                    ?: return Result.Error(json.optString("msg", "No data returned"))

                val play = data.optString("play", "")
                if (play.isNotEmpty()) {
                    return Result.Video(play)
                }

                val imagesArray = data.optJSONArray("images")
                if (imagesArray != null && imagesArray.length() > 0) {
                    val urls = (0 until imagesArray.length()).map { imagesArray.getString(it) }
                    return Result.Images(urls)
                }

                Result.Error("Unrecognized post type")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}
