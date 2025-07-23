package com.sectrend.buildscan.buildTools;

public class ScannableException extends Exception {


    public ScannableException() {
    }

    public ScannableException(String message) {
        super(message);
    }

    public ScannableException(String message, Throwable cause) {
        super(message, cause);
    }

    public ScannableException(Throwable cause) {
        super(cause);
    }

    public ScannableException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

