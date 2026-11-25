package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.R
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * MediaStorageManager handles robust persistence of uploaded media files
 * (video banners, posters, and profile avatars) into the application's local
 * internal storage and coordinates with cloud Firestore to prevent missing file errors.
 */
object MediaStorageManager {
    private const val TAG = "MediaStorageManager"
    private const val MEDIA_DIR = "app_media"
    const val BANNER_VIDEO_NAME = "banner_video_custom.mp4"
    const val BANNER_POSTER_NAME = "banner_poster_custom.png"
    const val PROFILE_AVATAR_NAME = "profile_avatar_custom.png"

    private fun getMediaDirectory(context: Context): File {
        val dir = File(context.filesDir, MEDIA_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Persists an uploaded content URI (from photo picker or file picker)
     * safely into the app's internal private storage directory.
     */
    fun persistMediaUri(
        context: Context,
        sourceUri: Uri,
        targetFileName: String
    ): Result<File> {
        return try {
            val targetDir = getMediaDirectory(context)
            val tempFile = File(targetDir, "${targetFileName}.tmp")
            val targetFile = File(targetDir, targetFileName)

            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: return Result.failure(Exception("Cannot open stream from URI: $sourceUri"))

            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (!tempFile.exists() || tempFile.length() == 0L) {
                tempFile.delete()
                return Result.failure(Exception("Saved file is empty or missing"))
            }

            if (targetFile.exists()) {
                targetFile.delete()
            }
            val renamed = tempFile.renameTo(targetFile)
            if (!renamed) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }

            Log.d(TAG, "Successfully persisted media to: ${targetFile.absolutePath} (${targetFile.length()} bytes)")
            Result.success(targetFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist media: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Persists an in-memory Bitmap (e.g. cropped profile image) into internal storage.
     */
    fun persistBitmap(
        context: Context,
        bitmap: Bitmap,
        targetFileName: String
    ): Result<File> {
        return try {
            val targetDir = getMediaDirectory(context)
            val targetFile = File(targetDir, targetFileName)
            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
            Result.success(targetFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist bitmap: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Resolves a video URI safely without missing file errors.
     * Checks custom source, local persistent file, or falls back to bundled raw asset.
     */
    fun resolveVideoUri(context: Context, customSource: String?): Uri {
        try {
            if (!customSource.isNullOrBlank()) {
                if (customSource.startsWith("http://") || customSource.startsWith("https://")) {
                    return Uri.parse(customSource)
                }
                if (customSource.startsWith("file://")) {
                    val filePath = customSource.removePrefix("file://")
                    val file = File(filePath)
                    if (file.exists() && file.length() > 0L) {
                        return Uri.fromFile(file)
                    }
                }
                val localFile = File(customSource)
                if (localFile.exists() && localFile.length() > 0L) {
                    return Uri.fromFile(localFile)
                }
            }

            // Check if local persistent custom video banner exists
            val localBanner = File(getMediaDirectory(context), BANNER_VIDEO_NAME)
            if (localBanner.exists() && localBanner.length() > 0L) {
                return Uri.fromFile(localBanner)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error resolving custom video URI, falling back to raw: ${e.message}")
        }

        // Safe, reliable fallback to bundled raw video resource
        return Uri.parse("android.resource://" + context.packageName + "/" + R.raw.banner_video)
    }

    /**
     * Checks if a local persistent media file exists and has content.
     */
    fun getPersistedFile(context: Context, fileName: String): File? {
        val file = File(getMediaDirectory(context), fileName)
        return if (file.exists() && file.length() > 0L) file else null
    }

    /**
     * Persists an image picked from photo gallery into internal storage with a unique filename.
     * Returns the absolute file path suitable for AsyncOrLocalImage.
     */
    fun persistCustomImage(context: Context, sourceUri: Uri): String? {
        val fileName = "img_${System.currentTimeMillis()}.jpg"
        val result = persistMediaUri(context, sourceUri, fileName)
        return result.getOrNull()?.absolutePath
    }

    /**
     * Uploads a document (PDF) or media file to Firebase Storage.
     * Supports destination folders like "development_documents/" and "development_media/".
     * Persists locally as fallback and uploads to Cloud Storage for persistent sharing.
     */
    fun uploadToFirebaseStorage(
        context: Context,
        sourceUri: Uri,
        destinationFolder: String,
        customFileName: String? = null,
        onSuccess: (downloadUrl: String) -> Unit,
        onError: (String) -> Unit
    ) {
        val extension = when {
            destinationFolder.contains("document") -> "pdf"
            destinationFolder.contains("video") -> "mp4"
            else -> "jpg"
        }
        val fileName = customFileName ?: "media_${System.currentTimeMillis()}.$extension"
        val localFileResult = persistMediaUri(context, sourceUri, fileName)
        val localFilePath = localFileResult.getOrNull()?.absolutePath

        try {
            val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
            val storageRef = storage.reference
            val fileRef = storageRef.child("$destinationFolder/$fileName")
            val uploadTask = fileRef.putFile(sourceUri)

            uploadTask.addOnSuccessListener {
                fileRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    Log.d(TAG, "Uploaded to Firebase Storage: $downloadUri")
                    onSuccess(downloadUri.toString())
                }.addOnFailureListener {
                    onError(it.localizedMessage ?: "Failed to retrieve cloud download URL")
                }
            }.addOnFailureListener { e ->
                Log.w(TAG, "Firebase Storage upload failed: ${e.message}")
                onError(e.localizedMessage ?: "Cloud upload failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Storage exception: ${e.message}", e)
            onError(e.localizedMessage ?: "Storage service unavailable")
        }
    }

    /**
     * Opens a document or media link safely using Android Intents.
     */
    fun openDocumentOrMedia(context: Context, urlOrPath: String) {
        if (urlOrPath.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                if (urlOrPath.startsWith("http://") || urlOrPath.startsWith("https://")) {
                    data = Uri.parse(urlOrPath)
                } else {
                    val file = File(urlOrPath)
                    if (file.exists()) {
                        val contentUri = androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val mime = when {
                            urlOrPath.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
                            urlOrPath.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
                            else -> "image/*"
                        }
                        setDataAndType(contentUri, mime)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } else {
                        data = Uri.parse(urlOrPath)
                    }
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot open document: ${e.message}")
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(urlOrPath)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "ডকুমেন্ট খোলার উপযুক্ত অ্যাপ পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
