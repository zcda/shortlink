package org.zcdada.shortlink_zc.admin.remote.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.List;

@Data
public class RecycleBinPageReqDTO extends Page{

    List<String> gidList;
}
