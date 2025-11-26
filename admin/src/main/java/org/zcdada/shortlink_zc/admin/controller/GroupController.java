package org.zcdada.shortlink_zc.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import org.zcdada.shortlink_zc.admin.service.GroupService;

/**
 * @Author: zcdada
 * @Description: 短链接 分组控制层
 * @DateTime: 2025/11/26 10:26
 */
@RestController
@RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;

}
