package org.zcdada.shortlink_zc.project.common.enums;


import org.zcdada.shortlink_zc.project.common.convention.errorcode.IErrorCode;

public enum UserErrorCodeEnum implements IErrorCode {

    USER_NULL("B000200","用户记录不存在"),
    USERNAME_EXIT("B000201","用户名已存在"),
    USE_SAVE_ERROR("B000203","用户记录失败"),
    USER_EXIT("B000202","用户记录已存在");

    private final String code;

    private final String message;

    UserErrorCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

}
