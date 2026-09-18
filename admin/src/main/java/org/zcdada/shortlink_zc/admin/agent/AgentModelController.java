package org.zcdada.shortlink_zc.admin.agent;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zcdada.shortlink_zc.admin.agent.router.ModelRouter;

import java.util.List;
import java.util.Map;

/**
 * 可选模型列表接口，供前端渲染"模型选择"下拉框。
 */
@RestController
@RequiredArgsConstructor
public class AgentModelController {

    private final ModelRouter modelRouter;

    @GetMapping("/api/short-link/admin/v1/agent/models")
    public List<Map<String, Object>> models() {
        return modelRouter.listModels();
    }
}
