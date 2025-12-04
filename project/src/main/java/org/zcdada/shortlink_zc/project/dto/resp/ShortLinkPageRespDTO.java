package org.zcdada.shortlink_zc.project.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * @Author: zcdada
 * @Description: 短链接分页返回参数
 * @DateTime: 2025/11/28 15:47
 */
@Data
public class ShortLinkPageRespDTO {
    //ID
    private Long id;
    //域名
    private String domain;
    //短链接
    private String shortUri;
    //完整短链接
    private String fullShortUrl;
    //原始链接
    private String originUrl;
    //点击量
    private Integer clickNum;
    //分组标识
    private String gid;
    //网站图标
    private String favicon;
    //启用标识 0：启用 1：未启用
    private Integer enableStatus;
    //创建类型 0：接口创建 1：控制台创建
    private Integer createdType;
    //有效期类型 0：永久有效 1：自定义
    private Integer validDateType;
    //有效期
    @JsonFormat(pattern = "yyyy-MM-DD HH:mm:ss",timezone = "GMT+8")
    private Date validDate;
    //描述
    private String describe;

    @JsonFormat(pattern = "yyyy-MM-DD HH:mm:ss",timezone = "GMT+8")
    private Date createTime;
}
