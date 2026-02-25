
package org.zcdada.shortlink_zc.admin.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zcdada.shortlink_zc.admin.common.biz.user.UserContext;

/**
 * openFeign 微服务调用传递用户信息配置 admin 调用 project 需要传递用户上下文
 */
@Configuration
public class OpenFeignConfiguration {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            template.header("username", UserContext.getUsername());
            template.header("userId", UserContext.getUserId());
            template.header("realName", UserContext.getRealName());
        };
    }
}
