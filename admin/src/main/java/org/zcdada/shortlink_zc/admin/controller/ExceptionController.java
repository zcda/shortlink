package org.zcdada.shortlink_zc.admin.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zcdada.shortlink_zc.admin.common.convention.exception.ClientException;

import static org.zcdada.shortlink_zc.admin.common.enums.UserErrorCodeEnum.USER_TOKEN_FAIL;

@RestController
public class ExceptionController {

    @RequestMapping("/error/filter")
    public ResponseEntity<?> handleFilterException(HttpServletRequest request) {
        Exception e = (Exception) request.getAttribute("filterError");
        // 这里会触发全局异常处理器
        throw new ClientException(USER_TOKEN_FAIL);
    }
}