package com.sinus.pinmap.ui.utils

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * 媒体文件清理工具。只删除应用私有目录（filesDir/images、filesDir/avatars）下的文件，
 * 外部 URI（content://、相册原图）一律不碰。
 */
object MediaFileHelper {

    fun resolvePath(uri: String?): File? {
        if (uri == null) return null
        val path = when {
            uri.startsWith("file://") -> Uri.parse(uri).path
            uri.startsWith("/") -> uri
            else -> return null
        }
        return File(path)
    }

    /**
     * 批量删除文件，仅限应用私有目录内的文件，防止误删外部资源。
     */
    fun deleteFiles(context: Context, paths: Collection<String?>) {
        val root = context.filesDir.canonicalPath
        paths.mapNotNull { resolvePath(it) }.forEach { file ->
            try {
                val canonical = file.canonicalPath
                if (canonical.startsWith(root)) {
                    file.delete()
                }
            } catch (_: Exception) {
            }
        }
    }
}
