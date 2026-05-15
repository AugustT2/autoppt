package com.example.pptrefresh.rules;

import com.example.pptrefresh.exception.FailureStage;
import com.example.pptrefresh.exception.RefreshException;
import com.example.pptrefresh.naming.WhitelistRegistry;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Component
public class RulesLoader {

    private final ResourceLoader resourceLoader;
    private final RulesValidator validator;

    public RulesLoader(ResourceLoader resourceLoader, RulesValidator validator) {
        this.resourceLoader = resourceLoader;
        this.validator = validator;
    }

    public WhitelistRegistry loadRegistry(String rulesDir) {
        try (InputStream in = openRulesResource(rulesDir, "registry.yaml")) {
            Yaml yaml = new Yaml();
            @SuppressWarnings("unchecked")
            Map<String, Object> root = yaml.load(in);
            @SuppressWarnings("unchecked")
            List<String> seg2 = (List<String>) root.get("allowedSegment2");
            @SuppressWarnings("unchecked")
            List<String> seg3 = (List<String>) root.get("allowedSegment3");
            @SuppressWarnings("unchecked")
            Map<String, String> deckRules = (Map<String, String>) root.get("deckRules");
            if (seg2 == null || seg3 == null || deckRules == null) {
                throw new RefreshException(
                        FailureStage.RULES_LOAD, "REGISTRY_INVALID", "registry.yaml 缺少必要字段");
            }
            return new WhitelistRegistry(seg2, seg3, deckRules);
        } catch (IOException e) {
            throw new RefreshException(
                    FailureStage.RULES_LOAD, "REGISTRY_IO", "无法读取 registry.yaml", null, e);
        }
    }

    public DeckRules loadDeckRules(String rulesDir, String rulesFileName) {
        try (InputStream in = openRulesResource(rulesDir, "decks/" + rulesFileName)) {
            LoaderOptions options = new LoaderOptions();
            Constructor constructor = new Constructor(DeckRules.class, options);
            Yaml yaml = new Yaml(constructor);
            DeckRules rules = yaml.load(in);
            validator.validate(rules);
            return rules;
        } catch (RefreshException e) {
            throw e;
        } catch (Exception e) {
            throw new RefreshException(
                    FailureStage.RULES_LOAD,
                    "DECK_RULES_IO",
                    "无法加载规则: " + rulesFileName,
                    null,
                    e);
        }
    }

    private InputStream openRulesResource(String rulesDir, String relative) throws IOException {
        if (rulesDir.startsWith("classpath:")) {
            String base = rulesDir.substring("classpath:".length());
            if (!base.startsWith("/")) {
                base = "/" + base;
            }
            Resource resource = resourceLoader.getResource("classpath:" + base + "/" + relative);
            if (!resource.exists()) {
                throw new IOException("资源不存在: " + relative);
            }
            return resource.getInputStream();
        }
        Path path = Path.of(rulesDir, relative);
        if (!Files.exists(path)) {
            throw new IOException("文件不存在: " + path);
        }
        return Files.newInputStream(path);
    }
}
