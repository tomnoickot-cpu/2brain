package com.twobrain.app.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class PhotosRepository(private val context: Context) {
    /** Photos taken between [from] and [to] (inclusive dates); null bounds mean "most recent". */
    suspend fun photos(from: LocalDate?, to: LocalDate?, limit: Int = 60): List<Photo> = withContext(Dispatchers.IO) {
        val zone = ZoneId.systemDefault()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_TAKEN)
        val (selection, args) = if (from != null && to != null) {
            "${MediaStore.Images.Media.DATE_TAKEN} >= ? AND ${MediaStore.Images.Media.DATE_TAKEN} < ?" to arrayOf(
                from.atStartOfDay(zone).toInstant().toEpochMilli().toString(),
                to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli().toString(),
            )
        } else null to null
        val out = mutableListOf<Photo>()
        context.contentResolver.query(collection, projection, selection, args, "${MediaStore.Images.Media.DATE_TAKEN} DESC")?.use { c ->
            while (c.moveToNext() && out.size < limit) {
                val id = c.getLong(0)
                val taken = if (c.isNull(1)) null else LocalDateTime.ofInstant(Instant.ofEpochMilli(c.getLong(1)), zone)
                out += Photo(ContentUris.withAppendedId(collection, id), taken)
            }
        }
        out
    }
}
