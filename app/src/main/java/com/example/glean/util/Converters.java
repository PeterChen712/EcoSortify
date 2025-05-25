package com.example.glean.util;

import androidx.room.TypeConverter;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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
    public static List<String> fromString(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    @TypeConverter
    public static String fromList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return String.join(",", list);
    }
}