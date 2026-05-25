package com.example.pptrefresh.orchestration;

import com.example.pptrefresh.config.PptRefreshProperties;
import com.example.pptrefresh.document.PptWriteService;
import com.example.pptrefresh.document.ResolvedTarget;
import com.example.pptrefresh.document.SlideStructure;
import com.example.pptrefresh.document.TargetResolver;
import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;
import com.example.pptrefresh.failure.FailureReport;
import com.example.pptrefresh.failure.FailureReportWriter;
import com.example.pptrefresh.llm.LlmTaskRunner;
import com.example.pptrefresh.llm.TaskContext;
import com.example.pptrefresh.naming.FilenameParser;
import com.example.pptrefresh.naming.ParsedFilename;
import com.example.pptrefresh.naming.ProductNameResolver;
import com.example.pptrefresh.naming.ResolvedProduct;
import com.example.pptrefresh.naming.WhitelistRegistry;
import com.example.pptrefresh.rules.DeckRules;
import com.example.pptrefresh.rules.RulesLoader;
import com.example.pptrefresh.rules.TaskDefinition;
import com.example.pptrefresh.rules.TextReplaceMode;
import com.example.pptrefresh.query.QueryPlan;
import com.example.pptrefresh.query.QueryPlanService;
import com.example.pptrefresh.query.ReportingContext;
import com.example.pptrefresh.query.ReportingContextBuilder;
import com.example.pptrefresh.rules.TaskType;
import com.example.pptrefresh.time.TimeContext;
import com.example.pptrefresh.time.TimeRuleResolver;
import com.example.pptrefresh.write.TaskWritePayload;
import com.example.pptrefresh.write.WritePayloadValidator;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.UUID;

@Service
public class RefreshOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(RefreshOrchestrator.class);

    private final PptRefreshProperties properties;
    private final RulesLoader rulesLoader;
    private final TimeRuleResolver timeRuleResolver;
    private final TargetResolver targetResolver;
    private final ProductNameResolver productNameResolver;
    private final LlmTaskRunner llmTaskRunner;
    private final WritePayloadValidator payloadValidator;
    private final PptWriteService pptWriteService;
    private final FailureReportWriter failureReportWriter;
    private final ReportingContextBuilder reportingContextBuilder;
    private final QueryPlanService queryPlanService;

    public RefreshOrchestrator(
            PptRefreshProperties properties,
            RulesLoader rulesLoader,
            TimeRuleResolver timeRuleResolver,
            TargetResolver targetResolver,
            ProductNameResolver productNameResolver,
            LlmTaskRunner llmTaskRunner,
            WritePayloadValidator payloadValidator,
            PptWriteService pptWriteService,
            FailureReportWriter failureReportWriter,
            ReportingContextBuilder reportingContextBuilder,
            QueryPlanService queryPlanService) {
        this.properties = properties;
        this.rulesLoader = rulesLoader;
        this.timeRuleResolver = timeRuleResolver;
        this.targetResolver = targetResolver;
        this.productNameResolver = productNameResolver;
        this.llmTaskRunner = llmTaskRunner;
        this.payloadValidator = payloadValidator;
        this.pptWriteService = pptWriteService;
        this.failureReportWriter = failureReportWriter;
        this.reportingContextBuilder = reportingContextBuilder;
        this.queryPlanService = queryPlanService;
    }

    public RefreshJobResult run(RefreshJobRequest request) {
        String jobId = UUID.randomUUID().toString();
        ParsedFilename parsed = null;
        ResolvedProduct resolved = null;
        Path source;
        Path output;
        try {
            RefreshOutputPaths.Resolved paths =
                    RefreshOutputPaths.resolve(
                            Path.of(request.getSourcePptxPath()),
                            Path.of(request.getOutputPptxPath()));
            source = paths.source();
            output = paths.output();
            if (paths.outputAutoDerived()) {
                log.warn(
                        "output 与 source 相同或会覆盖源文件，结果已写入: {}",
                        output.toAbsolutePath());
            }
        } catch (IOException e) {
            return new RefreshJobResult(false, jobId, null, null, "路径无效: " + e.getMessage());
        }
        try {
            parsed = FilenameParser.parse(source);
            WhitelistRegistry registry = rulesLoader.loadRegistry(properties.getRulesDir());
            registry.validate(parsed);
            DeckRules rules =
                    rulesLoader.loadDeckRules(
                            properties.getRulesDir(),
                            registry.rulesFileName(parsed.deckType()));
            if (!rules.getDeckType().equals(parsed.deckType())) {
                throw new RefreshException(
                        FailureStage.RULES_SCHEMA,
                        "DECK_TYPE_MISMATCH",
                        "规则 deckType 与文件名不一致");
            }
            TimeContext time = timeRuleResolver.resolve(rules.getTimeRules());
            Files.createDirectories(output.getParent());
            Path workCopy = output.resolveSibling(output.getFileName() + ".working");
            Files.copy(source, workCopy, StandardCopyOption.REPLACE_EXISTING);

            try (InputStream in = Files.newInputStream(workCopy);
                    XMLSlideShow ppt = new XMLSlideShow(in)) {
                resolved = productNameResolver.resolve(ppt, rules);
                ReportingContext reporting =
                        reportingContextBuilder.build(
                                rules.getReporting(), time, resolved.fundCode());
                log.info(
                        "Resolved product displayName={} fundCode={} asOfDate={} asOfQuarter={}",
                        resolved.displayName(),
                        resolved.fundCode(),
                        reporting.asOfDate(),
                        reporting.asOfQuarter());
                for (TaskDefinition task : rules.getTasks()) {
                    log.info("开始任务 task={} type={}", task.getId(), task.getType());
                    ResolvedTarget target =
                            targetResolver.resolve(ppt, properties.getSlideIndexBase(), task);
                    SlideStructure structure = target.structure();
                    QueryPlan queryPlan = null;
                    if (task.getType() == TaskType.table || task.getType() == TaskType.chart) {
                        queryPlan =
                                queryPlanService.build(
                                        task, reporting, target, resolved.displayName());
                        log.info(
                                "QueryPlan 已构建 task={} dimensions={}",
                                task.getId(),
                                queryPlan.dimensions().size());
                    }
                    TaskWritePayload payload =
                            llmTaskRunner.fetch(
                                    new TaskContext(
                                            parsed.deckType(),
                                            resolved.displayName(),
                                            resolved.fundCode(),
                                            reporting.toTimeContext(),
                                            reporting,
                                            queryPlan,
                                            task,
                                            structure));
                    if (task.getType() == com.example.pptrefresh.rules.TaskType.table) {
                        if (structure == null) {
                            throw new RefreshException(
                                    FailureStage.TASK_RESOLVE_TARGET,
                                    "TABLE_STRUCTURE_MISSING",
                                    "表格结构未解析",
                                    task.getId(),
                                    null);
                        }
                        payloadValidator.validate(
                                task, payload, structure.tableRows(), structure.tableCols());
                    } else {
                        payloadValidator.validate(task, payload, 0, 0);
                    }
                    pptWriteService.apply(task, target, payload);
                }
                try (OutputStream out = Files.newOutputStream(workCopy)) {
                    ppt.write(out);
                }
            }
            Files.move(workCopy, output, StandardCopyOption.REPLACE_EXISTING);
            log.info("Refresh job {} succeeded: {}", jobId, output);
            return new RefreshJobResult(true, jobId, output.toAbsolutePath().toString(), null, "OK");
        } catch (Exception e) {
            return handleFailure(jobId, source, output, parsed, resolved, e);
        }
    }

    private RefreshJobResult handleFailure(
            String jobId,
            Path source,
            Path output,
            ParsedFilename parsed,
            ResolvedProduct resolved,
            Exception e) {
        RefreshException refresh;
        if (e instanceof RefreshException) {
            refresh = (RefreshException) e;
        } else {
            refresh =
                    new RefreshException(
                            FailureStage.UNKNOWN,
                            "UNEXPECTED",
                            e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName(),
                            null,
                            e);
        }
        log.warn(
                "Refresh job {} failed stage={} errorCode={} taskId={} message={}",
                jobId,
                refresh.getStage(),
                refresh.getErrorCode(),
                refresh.getTaskId(),
                refresh.getMessage());
        if (refresh.getCause() != null) {
            log.warn("Refresh job {} failure cause", jobId, refresh.getCause());
        } else if (e != refresh) {
            log.warn("Refresh job {} failure exception", jobId, e);
        }
        try {
            if (Files.exists(output)) {
                Files.delete(output);
            }
            Path working = output.resolveSibling(output.getFileName() + ".working");
            if (Files.exists(working)) {
                Files.delete(working);
            }
        } catch (IOException ignored) {
            // best effort cleanup
        }
        FailureReport report = new FailureReport();
        report.setFailedAt(Instant.now().toString());
        report.setJobId(jobId);
        report.setTraceId(jobId);
        report.setSourceFile(source.toAbsolutePath().toString());
        report.setExpectedOutputFile(output.toAbsolutePath().toString());
        if (resolved != null) {
            report.setProductName(resolved.displayName());
            report.setFundCode(resolved.fundCode());
        }
        if (parsed != null) {
            report.setDeckType(parsed.deckType());
        }
        report.setStage(refresh.getStage());
        report.setTaskId(refresh.getTaskId());
        report.setErrorCode(refresh.getErrorCode());
        report.setMessage(refresh.getMessage());
        if (refresh.getCause() != null) {
            report.setCause(refresh.getCause().getClass().getSimpleName());
        }
        String failedPath = null;
        try {
            failureReportWriter.write(output, report);
            failedPath = failureReportWriter.failedJsonPath(output).toAbsolutePath().toString();
        } catch (IOException io) {
            log.error("无法写入 .failed.json", io);
        }
        return new RefreshJobResult(false, jobId, null, failedPath, refresh.getMessage());
    }
}
