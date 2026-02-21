package org.zcdada.shortlink_zc.admin.common.biz.user;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;

/**
 * 用户信息传输过滤器
 *
 */
@Component
@RequiredArgsConstructor
public class UserTransmitFilter implements Filter {

    private final StringRedisTemplate stringRedisTemplate;

    private final List<String> IGNORE_URLS = Lists.newArrayList("/api/short-link/admin/v1/user/login",
//            注册不拦截，修改要拦截
            "/api/short-link/admin/v1/user",
            "/api/short-link/admin/v1/user/has-username");





    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String requestURI = httpServletRequest.getRequestURI();
        String method = httpServletRequest.getMethod();

        if(!IGNORE_URLS.contains(requestURI)||Objects.equals(method, "PUT")){

                String userId = httpServletRequest.getHeader("username");
                String token = httpServletRequest.getHeader("token");

                if(!StrUtil.isAllNotBlank(userId,token)){
                    servletRequest.getRequestDispatcher("/error/filter").forward(servletRequest, servletResponse);
                    return;
                }
                Object userInfoJsonStr = null;
                try{
                    userInfoJsonStr = stringRedisTemplate.opsForHash().get("login_"+userId, token);
                    if(Objects.isNull(userInfoJsonStr)){
                        // 指定异常处理路径
                        servletRequest.getRequestDispatcher("/error/filter").forward(servletRequest, servletResponse);
                        return;
                    }
                }catch (Exception e){
                    servletRequest.getRequestDispatcher("/error/filter").forward(servletRequest, servletResponse);
                    return;
                }
                UserInfoDTO userInfoDTO = JSON.parseObject(userInfoJsonStr.toString(), UserInfoDTO.class);
                UserContext.setUser(userInfoDTO);

        }

        try {
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            UserContext.removeUser();
        }
    }

    private void returnJson(HttpServletResponse response, String json) throws Exception {
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html; charset=utf-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.print(json);
        }
    }
}