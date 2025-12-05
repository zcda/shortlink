package org.zcdada.shortlink_zc.project.common.enums;

/**
 * @Author: zcdada
 * @Description: 有效期 类型的枚举
 * @DateTime: 2025/12/5 10:55
 */
public enum VailDateTypeEnum {

    PERMANENT(0),
    
    CUSTOM(1);
    private final Integer value;
    VailDateTypeEnum(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}
