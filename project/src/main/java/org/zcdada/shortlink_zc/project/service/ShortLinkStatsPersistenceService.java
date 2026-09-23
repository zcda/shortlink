package org.zcdada.shortlink_zc.project.service;

import org.zcdada.shortlink_zc.project.dto.biz.ShortLinkStatsRecordDTO;

/**
 * 短链接访问统计的实际落库服务。
 *
 * <p>同步模式由跳转线程直接调用，MQ 模式由消费者调用，保证两种模式使用同一套落库逻辑。</p>
 */
public interface ShortLinkStatsPersistenceService {

    void save(ShortLinkStatsRecordDTO statsRecord);
}
