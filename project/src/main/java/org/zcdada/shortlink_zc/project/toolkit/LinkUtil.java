package org.zcdada.shortlink_zc.project.toolkit;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;

import java.util.Date;
import java.util.Optional;

import static org.zcdada.shortlink_zc.project.common.constant.ShortLinkConstant.DEFAULT_CACHE_TIME;

/**
 * @Author: zcdada
 * @Description: 短链接工具类
 * @DateTime: 2025/12/5 20:35
 */
public class LinkUtil {

    public static Long getLinkCacheValidTime(Date date) {
        return Optional.ofNullable(date)
                .map(each->DateUtil.between(new Date(),each, DateUnit.MS))
                .orElse(DEFAULT_CACHE_TIME);
    }
}
