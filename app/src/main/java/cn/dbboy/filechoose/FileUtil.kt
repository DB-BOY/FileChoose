package cn.dbboy.filechoose

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.support.annotation.RequiresApi
import android.support.v4.content.FileProvider
import android.text.TextUtils
import android.util.Log
import java.io.File
import java.text.DecimalFormat
import kotlin.math.log10
import kotlin.math.pow

/**
 * Created by DB_BOY on 2019/6/24.
 */
object FileUtil {
    private const val TAG = "FileUtil"

    fun readFileSize(path: String?): String {
        return readableFileSize(File(path).length())
    }

    private fun readableFileSize(size: Long): String {
        if (size <= 0) return "0"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(size.toDouble()) / log10(1024.0)).toInt()
        return DecimalFormat("#,##0.#").format(size / 1024.0.pow(digitGroups)) + " " + units[digitGroups]
    }


    /**
     * file --> uri
     *
     * @param context
     * @param file
     * @return
     */
    @JvmStatic
    fun getUriFromFile(context: Context?, file: File?): Uri {
        Log.i(TAG, "getUriFromFile: $file")
        if (context == null || file == null) {
            throw NullPointerException()
        }
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context.applicationContext,
                BuildConfig.APPLICATION_ID + ".fileprovider",
                file
            )
        } else {
            Uri.fromFile(file)
        }
        Log.i(TAG, "return uri: $uri")
        return uri
    }

    /**
     * 专为Android4.4设计的从Uri获取文件绝对路径，以前的方法已不好使
     */
    @JvmStatic
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    fun getPath(context: Context, uri: Uri): String? {
        Log.i(TAG, "getPath VERSION: " + Build.VERSION.SDK_INT + ", uri: " + uri)
        Log.i(TAG, "getScheme:" + uri.scheme)
        Log.i(TAG, "getAuthority:" + uri.authority)
        Log.i(TAG, "getPath:" + uri.path)
        Log.i(TAG, "getPathSegments:" + uri.pathSegments)
        Log.i(TAG, "getLastPathSegment:" + uri.lastPathSegment)
        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            //一些三方的文件浏览器会进入到这个方法中，例如ES
            //QQ文件管理器不在此列
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
            } else if (isDownloadsDocument(uri)) {
                val id = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads/public_downloads"),
                    id.toLong()
                )

                return getDataColumn(context, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                var contentUri: Uri? = null
                if ("image" == type) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if ("video" == type) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if ("audio" == type) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])

                return getDataColumn(context, contentUri, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) { // MediaStore (and general)
            val third = getThirdPartyUriFile(uri)
            Log.i(TAG, "third:$third")
            if (third != null) {
                Log.i(TAG, "getAbsolutePath:" + third.absolutePath)
            }
            // Return the remote address
            if (isGooglePhotosUri(uri)) return uri.lastPathSegment

            if (isQQMediaDocument(uri)) {
                getDataColumn(context, uri, null, null)
                val path = uri.path
                val fileDir = Environment.getExternalStorageDirectory()
                Log.i(TAG, "qq:$path, fileDir: $fileDir")
                //                content://com.tencent.mtt.fileprovider/external_path/DCIM/Camera/IMG_20250929_171518.jpg
                var file: File? = null
                if (path.contains("/QQBrowser")) {
                    file = File(fileDir, path.substring("/QQBrowser".length, path.length))
                } else if (path.contains("/external_path")) {
                    file = File(fileDir, path.substring("/external_path".length, path.length))
                }
                if (file == null) {
                    return null
                }
                return if (file.exists()) file.toString() else null
            }
            return getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) { // File
            Log.i(TAG, "file uri: $uri")
            return uri.path
        }
        return null
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    fun getDataColumn(
        context: Context,
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        Log.i(TAG, "getDataColumn uri: $uri")
        var cursor: Cursor? = null
        val column = MediaStore.MediaColumns.DATA
        val projection = arrayOf(column)
        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            Log.i(TAG, "getDataColumn cursor: $cursor")
            if (cursor != null && cursor.moveToFirst()) {
                val names = cursor.columnNames
                Log.i(TAG, "getColumnNames names: " + names.contentToString())
                val column_index = cursor.getColumnIndexOrThrow(column)
                val data = cursor.getString(column_index)
                Log.i(TAG, "getDataColumn cursor-data: $data")
                return data
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
        return null
    }


    @JvmStatic
    fun getRealPathFromURI(context: Context, contentUri: Uri?): String? {
        return getDataColumn(context, contentUri, null, null)
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        Log.i(TAG, "isExternalStorageDocument uri: $uri")
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private fun isDownloadsDocument(uri: Uri): Boolean {
        Log.i(TAG, "isDownloadsDocument uri: $uri")
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private fun isMediaDocument(uri: Uri): Boolean {
        Log.i(TAG, "isMediaDocument uri: $uri")
        return "com.android.providers.media.documents" == uri.authority
    }


    /**
     * 使用第三方qq文件管理器打开
     *
     * @param uri
     * @return
     */
    private fun isQQMediaDocument(uri: Uri): Boolean {
        Log.i(TAG, "isQQMediaDocument uri: $uri")
        return "com.tencent.mtt.fileprovider" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private fun isGooglePhotosUri(uri: Uri): Boolean {
        Log.i(TAG, "isGooglePhotosUri uri: $uri")
        return "com.google.android.apps.photos.content" == uri.authority
    }

    /**
     * 处理第三方文件管理器的文件Uri
     *
     * @param uri
     * @return ==========================================
     * Android12 API31
     * es浏览器格式: content://com.estrongs.files/storage/emulated/0/1756472359484   不通过
     * qq浏览器格式: content://com.tencent.mtt.fileprovider/external_path/Pictures/Youku-1758176723727.png   通过
     */
    private fun getThirdPartyUriFile(uri: Uri): File? {
        var path = uri.path
        Log.i(TAG, "getThirdPartyUriFile:$path")
        if (!TextUtils.isEmpty(path)) {
            // 去掉第一个'/'
            path = path.substring(1)
            // 去掉新path第一个'/'前面的内容
            path = path.substring(path.indexOf('/'))

            Log.i(TAG, "getThirdPartyUriFile:path1:$path")
            Log.i(TAG, "getThirdPartyUriFile:path2:" + Environment.getExternalStorageDirectory())
            val file = File(Environment.getExternalStorageDirectory(), path)
            if (file.exists()) {
                return file
            }
        }
        return null
    }
}
