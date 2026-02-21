package org.zcdada.shortlink_zc.project.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @Author: zcdada
 * @Description: 有效期 类型的枚举
 * @DateTime: 2025/12/5 10:55
 */
@RequiredArgsConstructor
public enum VailDateTypeEnum {

    PERMANENT(0),
    
    CUSTOM(1);

    @Getter
    private final int type;

}
