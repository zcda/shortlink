package org.zcdada.shortlink_zc.admin.remote;


import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.bind.annotation.RequestParam;
import org.zcdada.shortlink_zc.admin.common.convention.result.Result;
import org.zcdada.shortlink_zc.admin.remote.dto.req.ShortLinkCreateReqDTO;
import org.zcdada.shortlink_zc.admin.remote.dto.req.ShortLinkPageReqDTO;
import org.zcdada.shortlink_zc.admin.remote.dto.req.ShortLinkUpdateReqDTO;
import org.zcdada.shortlink_zc.admin.remote.dto.resp.ShortLinkCreateRespDTO;
import org.zcdada.shortlink_zc.admin.remote.dto.resp.ShortLinkGroupRemoteRespDTO;
import org.zcdada.shortlink_zc.admin.remote.dto.resp.ShortLinkPageRespDTO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author: zcdada
 * @Description: 远程调用 后期要改成 orin
 * @DateTime: 2025/12/4 10:58
 */
public interface ShortLinkRemoteService {

    default Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO requestParam){
        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("gid", requestParam.getGid());
        requestMap.put("current", requestParam.getCurrent());
        requestMap.put("size", requestParam.getSize());
//        String jsonString = JSON.toJSONString(requestParam);
        String resultPageStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/page",requestMap);

        return JSON.parseObject(resultPageStr, new TypeReference<>() {
        });
    }

    default Result<ShortLinkCreateRespDTO> createShortLink(ShortLinkCreateReqDTO requestParam){
        String jsonStr = JSON.toJSONString(requestParam);
        String resultPageStr = HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/create",jsonStr);

        return  JSON.parseObject(resultPageStr, new TypeReference<>() {});
    }

    default Result<ShortLinkGroupRemoteRespDTO> shortLinkCount(@RequestParam("requestParam") List<String> requestParam){
        Map<String,Object> requestMap = new HashMap<>();
        requestMap.put("requestParam", requestParam);
        String resultPageStr = HttpUtil.get("http://127.0.0.1:8001/api/short-link/v1/count",requestMap);
        return JSON.parseObject(resultPageStr, new TypeReference<>() {
        });

    }

    default void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        HttpUtil.post("http://127.0.0.1:8001/api/short-link/v1/update",JSON.toJSONString(requestParam));
    }
}
