package org.zcdada.shortlink_zc.admin.dto.resp;

import lombok.Data;

@Data
public class ShortLinkGroupRespDTO {
    //分组标识
    private String gid;
    //分组名称
    private String name;

    //分组排序
    private Integer sortOrder;

    //该分组下短链接的数量
    private Integer shortLinkCount;
}
