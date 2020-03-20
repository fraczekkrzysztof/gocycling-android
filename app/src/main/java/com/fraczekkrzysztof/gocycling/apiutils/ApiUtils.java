package com.fraczekkrzysztof.gocycling.apiutils;

public class ApiUtils {

    public static final String PARAMS_START = "?";
    public static final String PARAMS_AND = "&";
    private static final String PAGE = "page=";
    private static final String SORT = "sort=";
    private static final String SIZE = "size=";
    public static final String USER_UID = "userUid=";
    public static final String EVENT_ID = "eventId=";

    public static String getPageToRequest(){
        return getPageToRequest(0);
    }

    public static String getPageToRequest(int page){
        return PAGE + page;
    }

    public static String getSortToRequest(String fieldName){
        return getSortToRequest(fieldName,SortTypes.ASC);
    }

    public static String getSiezeToRequest(int pageSize){
        return SIZE + pageSize;
    }


    public static String getSortToRequest(String fieldName, SortTypes sort){
        return SORT + fieldName + "," + sort.getValue();
    }

}
