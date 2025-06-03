package com.example.glean.db;

import androidx.room.TypeConverter;
import java.util.Date;

public class Converters {
    
    @TypeConverter
    public static Date fromTimestamp(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }
    
    @TypeConverter
    public static String fromString(String value) {
        return value;
    }
    
    @TypeConverter
    public static String toString(String value) {
        return value;
    }
}