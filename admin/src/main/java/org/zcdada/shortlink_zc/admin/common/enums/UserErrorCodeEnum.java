package org.zcdada.shortlink_zc.admin.common.enums;

import org.zcdada.shortlink_zc.admin.common.convention.errorcode.IErrorCode;

public enum UserErrorCodeEnum implements IErrorCode {


    FLOW_LIMIT_ERROR("A000300", "当前系统繁忙，请稍后再试"),
    USER_NULL("B000200","用户记录不存在"),

    // ========== 二级宏观错误码 系统请求操作频繁 ==========

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
