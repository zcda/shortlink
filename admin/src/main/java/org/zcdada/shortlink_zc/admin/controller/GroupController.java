package org.zcdada.shortlink_zc.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.zcdada.shortlink_zc.admin.common.convention.result.Result;
import org.zcdada.shortlink_zc.admin.common.convention.result.Results;
import org.zcdada.shortlink_zc.admin.dto.req.ShortLinkGroupOrderReqDTO;
import org.zcdada.shortlink_zc.admin.dto.req.ShortLinkGroupSaveReqDTO;
import org.zcdada.shortlink_zc.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import org.zcdada.shortlink_zc.admin.dto.resp.ShortLinkGroupRespDTO;
import org.zcdada.shortlink_zc.admin.service.GroupService;

import java.util.List;

/**
 * @Author: zcdada
 * @Description: 短链接 分组控制层
 * @DateTime: 2025/11/26 10:26
 */
@RestController
@RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;

/**
 * @Author: zcdada
 * @Description: 新增 短链接组
 * @DateTime: 2025/11/26 16:15
 */
    @PostMapping("/api/short-link/admin/v1/group")
    public Result<Void> save(@RequestBody ShortLinkGroupSaveReqDTO requestParam){
        groupService.saveGroup(requestParam);
        return Results.success();
    }
    /**
     * @Author: zcdada
     * @Description: 查询短链接组
     * @DateTime: 2025/11/26 16:15
     */
    @GetMapping("/api/short-link/admin/v1/group")
    public Result<List<ShortLinkGroupRespDTO>> groupList(){
        return Results.success(groupService.groupList());
    }
    /**
     * @Author: zcdada
     * @Description: 修改短链接组
     * @DateTime: 2025/11/26 16:15
     */
    @PutMapping("/api/short-link/admin/v1/group")
    public Result<Void> updateGroup(@RequestBody ShortLinkGroupUpdateReqDTO requestParam){
        groupService.updateGroup(requestParam);
        return Results.success();
    }

    /**
     * @Author: zcdada
     * @Description: 短链接分组删除
     * @DateTime: 2025/11/26 16:29
     */
    @DeleteMapping("/api/short-link/admin/v1/group")
    public Result<Void> deleteGroup(@RequestParam String gid){
        groupService.deleteGroup(gid);
        return Results.success();
    }
    /**
     * @Author: zcdada
     * @Description: 改变短链接分组的排序
     * @DateTime: 2025/11/26 16:40
     */
    @PutMapping("/api/short-link/v1/admin/group/order")
    public Result<Void> updateOrderGroup(@RequestBody List<ShortLinkGroupOrderReqDTO> requestParam){
        groupService.updateGroupOrder(requestParam);

        return Results.success();
    }
}
