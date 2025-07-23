package com.sectrend.buildscan.exception;

import java.io.IOException;

public class DetectorFinderDirectoryListException extends Exception {
    public DetectorFinderDirectoryListException(String message, IOException e) {
        super(message, e);
    }
}