package com.sergej.jornalapp.data.local

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.sergej.jornalapp.domain.repository.VideoStorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.net.toUri

const val CAPTURE_DIR = "capture"
const val SAVED_VIDEOS_DIR = "videos"
const val FILE_PREFIX = "capture_"
const val FILE_SUFFIX = ".mp4"

class FileVideoStorageRepository(
    private val context: Context,
) : VideoStorageRepository {

    override fun createPendingCaptureUri(): String {
        val captureDir = File(context.cacheDir, CAPTURE_DIR).apply { mkdirs() }
        val captureFile = File.createTempFile(FILE_PREFIX, FILE_SUFFIX, captureDir)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            captureFile,
        )
        return uri.toString()
    }

    override suspend fun persistCapturedVideo(captureUri: String): String =
        withContext(Dispatchers.IO) {
            val sourceUri = captureUri.toUri()
            val localVideoDir = File(context.filesDir, SAVED_VIDEOS_DIR).apply { mkdirs() }
            val targetFile =
                File(localVideoDir, "journal_${System.currentTimeMillis()}$FILE_SUFFIX")

            val inputStream = context.contentResolver.openInputStream(sourceUri)
                ?: error("Unable to open captured video stream")

            inputStream.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            context.contentResolver.delete(sourceUri, null, null)
            targetFile.absolutePath
        }
}
