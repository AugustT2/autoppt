package com.example.pptrefresh.tools;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 已注册 Tool 的名称与 {@link ToolSpecification}（来自 {@link DemoDataTools}）。 */
@Component
public class ToolCatalog {

    private final Map<String, ToolSpecification> byName;
    private final List<ToolSpecification> allSpecifications;

    public ToolCatalog(DemoDataTools demoDataTools) {
        Map<String, ToolSpecification> map = new LinkedHashMap<>();
        for (ToolSpecification spec : ToolSpecifications.toolSpecificationsFrom(demoDataTools)) {
            map.put(spec.name(), spec);
        }
        this.byName = Collections.unmodifiableMap(map);
        this.allSpecifications = List.copyOf(new ArrayList<>(byName.values()));
    }

    public Set<String> knownToolNames() {
        return byName.keySet();
    }

    public List<ToolSpecification> allSpecifications() {
        return allSpecifications;
    }

    public List<ToolSpecification> specificationsForTask(String toolName) {
        ToolSpecification spec = byName.get(toolName);
        if (spec == null) {
            throw new IllegalArgumentException("未知 tool: " + toolName);
        }
        return List.of(spec);
    }

    /** YAML 未配置 {@code tool} 时挂全部；配置了则只挂一个（流水线模式）。 */
    public List<ToolSpecification> resolveForTask(String configuredTool) {
        if (StringUtils.hasText(configuredTool)) {
            return specificationsForTask(configuredTool.trim());
        }
        return allSpecifications;
    }

    public boolean isAgentMode(String configuredTool) {
        return !StringUtils.hasText(configuredTool);
    }
}
