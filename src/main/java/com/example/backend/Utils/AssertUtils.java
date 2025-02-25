package com.example.backend.Utils;

public class AssertUtils {
    public static void isError(boolean condition, String message) throws Exception {
        if (condition) {
            throw new Exception(message);
        }
    }
}