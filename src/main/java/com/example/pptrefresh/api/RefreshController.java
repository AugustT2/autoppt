package com.example.pptrefresh.api;

import com.example.pptrefresh.orchestration.RefreshJobRequest;
import com.example.pptrefresh.orchestration.RefreshJobResult;
import com.example.pptrefresh.orchestration.RefreshOrchestrator;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class RefreshController {

    private final RefreshOrchestrator orchestrator;

    public RefreshController(RefreshOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshJobResult> refresh(@Valid @RequestBody RefreshJobRequest request) {
        RefreshJobResult result = orchestrator.run(request);
        if (result.success()) {
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.status(422).body(result);
    }
}
