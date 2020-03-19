package com.fraczekkrzysztof.gocycling.apiutils;

public enum SortTypes {
    ASC("asc"),
    DESC("desc");

    private String value;

    SortTypes(String value){
        this.value = value;
    }

    public String getValue(){
        return this.value;
    }
}
