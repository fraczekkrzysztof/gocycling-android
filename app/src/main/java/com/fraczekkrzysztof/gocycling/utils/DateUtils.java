package com.fraczekkrzysztof.gocycling.utils;

import java.text.SimpleDateFormat;

public class DateUtils {

    public static final SimpleDateFormat sdfWithFullTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    public static final SimpleDateFormat sdfWithoutTime = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat sdfWithTime = new SimpleDateFormat("yyyy-MM-dd HH:mm");
}
