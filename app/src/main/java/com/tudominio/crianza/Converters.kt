package com.tudominio.crianza

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.joinToString(",") ?: ""
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        return if (value.isNullOrEmpty()) {
            emptyList()
        } else {
            value.split(",")
        }
    }
}
