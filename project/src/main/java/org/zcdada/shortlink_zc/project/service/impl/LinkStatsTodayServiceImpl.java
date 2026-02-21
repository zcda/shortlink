
package org.zcdada.shortlink_zc.project.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.zcdada.shortlink_zc.project.dao.entity.ShortLinkStatsTodayDO;
import org.zcdada.shortlink_zc.project.dao.mapper.ShortLinkStatsTodayMapper;
import org.zcdada.shortlink_zc.project.service.LinkStatsTodayService;

/**
 * 短链接今日统计接口实现层
 */
@Service
public class LinkStatsTodayServiceImpl extends ServiceImpl<ShortLinkStatsTodayMapper, ShortLinkStatsTodayDO> implements LinkStatsTodayService {
}
