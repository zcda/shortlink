

package org.zcdada.shortlink_zc.project.config;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.zcdada.shortlink_zc.project.common.user.UserTransmitInterceptor;

/**
 * 用户配置自动装配
 */
@Configuration(value = "userConfigurationByProject")
@RequiredArgsConstructor
public class UserConfiguration implements WebMvcConfigurer {

    private final UserTransmitInterceptor userTransmitInterceptor;

    /**
     * 用户信息传递过滤器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userTransmitInterceptor)
                .addPathPatterns("/**");
    }
}