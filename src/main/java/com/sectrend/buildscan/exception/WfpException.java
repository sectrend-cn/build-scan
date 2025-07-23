package com.sectrend.buildscan.exception;

public class WfpException extends Exception{

    private String detail;

    public WfpException(String a) {
        this.detail = a;
    }

    public String getMessage(){
        return "WfpException: " + detail;
    }
}
