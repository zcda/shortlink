package org.zcdada.shortlink_zc.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import groovy.util.logging.Slf4j;
import org.springframework.stereotype.Service;
import org.zcdada.shortlink_zc.admin.dao.entity.GroupDO;
import org.zcdada.shortlink_zc.admin.dao.mapper.GroupMapper;
import org.zcdada.shortlink_zc.admin.service.GroupService;

/**
 * @Author: zcdada
 * @Description: 短链接分组实现层
 * @DateTime: 2025/11/26 10:25
 */
@Service
@Slf4j
public class GroupServiceImpl extends ServiceImpl<GroupMapper,GroupDO> implements GroupService {
}
