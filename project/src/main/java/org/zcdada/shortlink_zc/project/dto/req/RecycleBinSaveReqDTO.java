
package org.zcdada.shortlink_zc.project.dto.req;

import lombok.Data;

@Data
public class RecycleBinSaveReqDTO {
    //分组标识
    private String gid;

    //完整短链接
    private String fullShortUrl;

}
