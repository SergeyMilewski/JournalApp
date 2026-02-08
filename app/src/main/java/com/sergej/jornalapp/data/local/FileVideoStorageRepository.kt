package com.sergej.jornalapp.data.local

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.sergej.jornalapp.common.coroutines.DispatcherProvider
import com.sergej.jornalapp.domain.repository.VideoStorageRepository
import kotlinx.coroutines.withContext
import java.io.File

const val CAPTURE_DIR = "capture"
const val SAVED_VIDEOS_DIR = "videos"
const val FILE_PREFIX = "capture_"
const val FILE_SUFFIX = ".mp4"

class FileVideoStorageRepository(
    private val context: Context,
    private val dispatcherProvider: DispatcherProvider,
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
        withContext(dispatcherProvider.io) {
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

            deleteTempCaptureFile(sourceUri)
            targetFile.absolutePath
        }

    override suspend fun deleteVideoFile(filePath: String) =
        withContext(dispatcherProvider.io) {
            val file = File(filePath)
            if (file.exists() && !file.delete()) {
                error("Unable to delete video file at path: $filePath")
            }
        }

    private fun deleteTempCaptureFile(sourceUri: Uri) {
        if (sourceUri.authority != "${context.packageName}.fileprovider") return
        val fileName = sourceUri.lastPathSegment ?: return
        val captureFile = File(File(context.cacheDir, CAPTURE_DIR), fileName)
        if (captureFile.exists()) {
            captureFile.delete()
        }
    }
}
