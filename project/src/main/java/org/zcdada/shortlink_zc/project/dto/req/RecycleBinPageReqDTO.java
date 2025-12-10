package org.zcdada.shortlink_zc.project.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import org.zcdada.shortlink_zc.project.dao.entity.ShortLinkDO;

import java.util.List;

@Data
public class RecycleBinPageReqDTO extends Page<ShortLinkDO> {

    List<String> gidList;
}
