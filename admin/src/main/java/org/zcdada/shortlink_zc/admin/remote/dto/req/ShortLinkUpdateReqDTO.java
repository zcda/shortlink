package org.zcdada.shortlink_zc.admin.remote.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * @Author: zcdada
 * @Description: 修改短链接所需参数
 * @DateTime: 2025/12/5 10:54
 */
@Data
public class ShortLinkUpdateReqDTO {

    //完整短链接
    private String fullShortUrl;

    //原始链接
    private String originUrl;

    //分组标识
    private String gid;

    //有效期类型 0：永久有效 1：自定义
    private Integer validDateType;


    //有效期
    @JsonFormat(pattern = "yyyy-MM-DD HH:mm:ss",timezone = "GMT+8")
    private Date validDate;

    //描述
    private String describe;

}
