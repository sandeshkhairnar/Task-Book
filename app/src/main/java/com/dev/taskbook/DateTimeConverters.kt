package com.dev.taskbook

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class DateTimeConverters {
    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): Long? {
        return dateTime?.let {
            java.time.Instant.from(dateTime).toEpochMilli()
        }
    }

    @TypeConverter
    fun toLocalDateTime(timestamp: Long?): LocalDateTime? {
        return timestamp?.let {
            LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(it),
                java.time.ZoneOffset.UTC
            )
        }
    }

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? {
        return date?.let {
            date.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        }
    }

    @TypeConverter
    fun toLocalDate(timestamp: Long?): LocalDate? {
        return timestamp?.let {
            java.time.Instant.ofEpochMilli(it)
                .atZone(java.time.ZoneOffset.UTC)
                .toLocalDate()
        }
    }

    @TypeConverter
    fun fromLocalTime(time: LocalTime?): Long? {
        return time?.let {
            time.toSecondOfDay().toLong()
        }
    }

    @TypeConverter
    fun toLocalTime(secondOfDay: Long?): LocalTime? {
        return secondOfDay?.let {
            LocalTime.ofSecondOfDay(it)
        }
    }
}