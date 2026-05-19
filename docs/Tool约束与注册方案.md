# Tool 约束与注册方案

> **文档状态**：方案记录稿；**首期不实现硬约束**，继续用 **Prompt 软约束** 引导模型选 Tool。  
> 相关实现：`LangChain4jLlmTaskRunner`、`DemoDataTools`（`@Tool`）、`DemoToolExecutor`、`PromptBuilder`。

---

## 1. 背景

刷新链路中，每个 `task` 会调用 LLM，模型可通过 **Function Calling / Tool** 拉取业务数据，再输出写回 JSON。

需要明确两件事：

1. **有哪些 Tool 可以注册给模型**（注册范围）  
2. **某次 task 允许模型看到/调用哪些 Tool**（约束范围）

**默认 Agent 模式**：YAML 不配 `tool` → 挂全部 Tool，SYSTEM 按 `taskType`/`intent` 引导选型，User 仅 intent + 上下文 + 写回格式。  
**可选流水线模式**：YAML 配置 `tool:` → 只挂该 Tool（生产要稳时用）。

---

## 2. 当前实现（软约束）

### 2.1 Tool 注册（硬编码在代码里）

| 组件 | 作用 |
|------|------|
| `DemoDataTools` | 各 task 对应方法标注 `@Tool`，由 LangChain4j 生成 schema |
| `ToolCatalog` | 从 `DemoDataTools` 收集全部 Tool 名与 schema |
| `LangChain4jLlmTaskRunner` | `toolCatalog.specificationsForTask(task.tool)` → **仅 1 个** Tool 传入 `ChatRequest` |
| `DemoToolExecutor` | 模型发起调用时，`execute(name, argsJson)` 分发到 `DemoDataTools` |

演示阶段 Tool 列表（与 `DemoToolExecutor.toolDefinitions()` 一致）：

| name | 说明 |
|------|------|
| `lookupProductCode` | 按产品展示名查基金代码（演示库 / 硬编码表） |
| `fetchQuarterReturnSummary` | 按 `productCode` + `quarter` 查收益率摘要 |
| `fetchTitleText` | 主标题（`title` task） |
| `fetchFundMetaAfterAnchor` | 基金基本信息后缀（`fund_meta`） |
| `fetchStrategyAfterAnchor` | 投资策略正文（`strategy`） |
| `fetchPerformanceTable` | 业绩表 `cells`（`performance_table`） |
| `fetchAllocationChart` | 资产配置柱图 |
| `fetchNavChart` | 累计收益折线图 |

### 2.2 配置与 Prompt

| 模式 | YAML | LLM 可见 Tool | User 消息 |
|------|------|---------------|-----------|
| Agent（默认） | 无 `tool` | 全部 | intent + 上下文 + `writeback=` |
| 流水线 | `tool: fetchNavChart` | 仅 1 个 | 同上 + `tool=` |

- **`tool` 若填写**：`RulesValidator` 校验名存在；错误调用抛 `TOOL_NOT_ALLOWED`。  
- **`hints`**：deck 特例，可选。

### 2.3 与 LangChain4j Spring Boot Starter 的关系

- `langchain4j-spring-boot-starter` 可扫描带 `@Tool` 的 `@Component`。  
- 本项目中，**发给模型的 Tool 列表**仍以 `LangChain4jLlmTaskRunner` 内 `toolSpecificationsFrom(demoDataTools)` 为准，与 Starter 全局扫描解耦，避免误把未审核的 Tool 暴露给 LLM。

---

## 3. 目标能力（后续迭代）

### 3.1 硬约束：按 deck / task 白名单

在 YAML 中声明本次任务允许的工具名，Java 在构造 `ChatRequest` 时 **只挂载子集**。

**拟定 deck 级默认（可选）：**

```yaml
# decks/偏债混-M1.yaml（示意，未实现）
defaultAllowedTools:
  - fetchTitleText
  - lookupProductCode
```

**拟定 task 级覆盖（可选）：**

```yaml
tasks:
  - id: title
    type: text
    allowedTools:
      - fetchTitleText
    ...
  - id: fund_meta
    type: text
    allowedTools:
      - lookupProductCode
      - fetchQuarterReturnSummary
    ...
```

**合并规则（建议）：**

```
task.allowedTools 非空 → 用 task 列表
否则 deck.defaultAllowedTools 非空 → 用 deck 列表
否则 → 全部已注册 Tool（与现网一致）
```

### 3.2 执行层校验（建议与硬约束同做）

即使模型请求了白名单外的 Tool，`DemoToolExecutor`（或统一 `ToolGateway`）应拒绝并返回明确错误，写入失败报告 `stage=TASK_TOOL`。

### 3.3 生产 Tool 替换演示实现

`DemoDataTools` / `HardcodedFundCodeLookup` 换为真实 API / DB；Tool 名与参数 schema 尽量稳定，仅改实现类，YAML 白名单可不变。

---

## 4. 方案对比

| 维度 | 软约束（当前） | 硬约束（后续） |
|------|----------------|----------------|
| 配置位置 | `PromptBuilder` 文案 | deck/task YAML `allowedTools` |
| 模型可见 Tool 数 | 全部 | 子集 |
| 误调其它 Tool | 可能发生 | 请求层不可见，执行层可拒绝 |
| 实现成本 | 已完成 | 需改 `TaskDefinition`、`RulesValidator`、`LangChain4jLlmTaskRunner` |
| 适用阶段 | 联调、样例 deck | 多 deck、多 Tool、合规要求高 |

**当前决策**：维持软约束，硬约束待有明确 deck 差异或合规要求后再做。

---

## 5. 首期联调建议（偏债混-M1）

在不动 YAML 硬约束的前提下，建议模型路径：

1. 每个 task **只调** YAML `tool` 字段（如 `tool: fetchNavChart`）；参数由 Tool schema + 上下文供模型填写。  
2. 仅当 intent 明确要求「按季度查收益」时，再调 `fetchQuarterReturnSummary`。  
3. `lookupProductCode` 通常 **不需要** LLM 再查——编排阶段已解析 `fundCode` 并写入 user 消息。

上述第 3 点依赖 [`产品名称提取方案.md`](./产品名称提取方案.md) 中的 `productNameResolution` + `HardcodedFundCodeLookup`。

---

## 6. 实现清单（ backlog ）

| 优先级 | 项 | 说明 |
|--------|-----|------|
| P2 | `TaskDefinition.allowedTools` | YAML 字段 + 校验工具名存在 |
| P2 | `ToolCatalog` | 统一注册名 → `ToolSpecification` / 执行器 |
| P2 | `LangChain4jLlmTaskRunner` 按 task 过滤 | `fetch(context)` 内组装 `toolSpecifications` |
| P3 | deck 级 `defaultAllowedTools` | 减少每个 task 重复配置 |
| P3 | 失败报告 `toolName` | 记录非法或失败的 Tool 调用 |
| P4 | 真实数据源 Tool | 替换 `DemoDataTools` |

---

## 7. 相关文档与代码

| 类型 | 路径 |
|------|------|
| 编排 | `RefreshOrchestrator` → `LlmTaskRunner.fetch` |
| LLM + Tool 循环 | `LangChain4jLlmTaskRunner` |
| Prompt | `PromptBuilder` |
| 演示 Tool | `DemoDataTools`、`DemoToolExecutor` |
| 样例 deck 规则 | `rules/decks/偏债混-M1.yaml` |
| 架构说明 | `ppt-data-refresh-technical-architecture.md`（§ Tool 相关章节待与本文对齐） |

---

## 8. 修订记录

| 日期 | 说明 |
|------|------|
| 2026-05-18 | 初稿：记录现状（全量注册 + Prompt 软约束）与后续 YAML 白名单方案 |
| 2026-05-19 | 移除 `fetchDeckDataBundle`；默认 Agent 全量 Tool；`tool` 可选用于流水线模式 |
