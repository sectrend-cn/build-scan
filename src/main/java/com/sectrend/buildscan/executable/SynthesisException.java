package com.sectrend.buildscan.executable;

public class SynthesisException extends Exception{

    private static final long serialVersionUID = 3954366584461978848L;

    public SynthesisException() {}

    public SynthesisException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public SynthesisException(String message, Throwable cause) {
        super(message, cause);
    }

    public SynthesisException(String message) {
        super(message);
    }

    public SynthesisException(Throwable cause) {
        super(cause);
    }

}
