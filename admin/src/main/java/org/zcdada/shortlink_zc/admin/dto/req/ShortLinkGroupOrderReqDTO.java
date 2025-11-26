package org.zcdada.shortlink_zc.admin.dto.req;

import lombok.Data;

@Data
public class ShortLinkGroupOrderReqDTO {
    //分组标识
    private String gid;
    //分组排序
    private Integer sortOrder;
}
