package com.figago.data.db

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromFloatList(list: List<Float>?): String? {
        if (list == null) return null
        return list.joinToString(separator = ",")
    }

    @TypeConverter
    fun toFloatList(data: String?): List<Float>? {
        if (data.isNullOrEmpty()) return null
        return data.split(",").mapNotNull { it.toFloatOrNull() }
    }
}
