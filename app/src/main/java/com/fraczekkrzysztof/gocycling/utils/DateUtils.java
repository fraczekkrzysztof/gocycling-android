package com.fraczekkrzysztof.gocycling.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateUtils {

    public static final String DEFAULT_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    public static String formatDefaultDateToDateWithTime(String date) throws ParseException {
        SimpleDateFormat defaultFormat = new SimpleDateFormat(DEFAULT_TIME_FORMAT);
        SimpleDateFormat targetFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date parsedDate = defaultFormat.parse(date);
        return targetFormat.format(parsedDate);
    }

    public static String formatDefaultDateToDateWithFullTime(String date) throws ParseException {
        SimpleDateFormat defaultFormat = new SimpleDateFormat(DEFAULT_TIME_FORMAT);
        SimpleDateFormat targetFormat = new SimpleDateFormat(DEFAULT_TIME_FORMAT);
        Date parsedDate = defaultFormat.parse(date);
        return targetFormat.format(parsedDate);
    }

    public static String formatDefaultDateToDateWithoutTime(String date) throws ParseException {
        SimpleDateFormat defaultFormat = new SimpleDateFormat(DEFAULT_TIME_FORMAT);
        SimpleDateFormat targetFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date parsedDate = defaultFormat.parse(date);
        return targetFormat.format(parsedDate);
    }

    public static String formatDateWithFullTimeToDefaultTime(String date) throws ParseException {
        SimpleDateFormat sourceFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        SimpleDateFormat defaultFormat = new SimpleDateFormat(DEFAULT_TIME_FORMAT);
        Date parsedDate = sourceFormat.parse(date);
        return defaultFormat.format(parsedDate);
    }

    public static String formatDateToDateWithTime(Date date) {
        SimpleDateFormat targetFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return targetFormat.format(date);
    }
}
