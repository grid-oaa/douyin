package com.douyin.liverecorder.exception;

/**
 * 保存目录校验异常
 */
public class SaveDirException extends RuntimeException {
    private final String code;

    public SaveDirException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
