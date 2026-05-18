package com.example.pptrefresh.orchestration;

import jakarta.validation.constraints.NotBlank;

public class RefreshJobRequest {

    @NotBlank private String sourcePptxPath;

    @NotBlank private String outputPptxPath;

    public String getSourcePptxPath() {
        return sourcePptxPath;
    }

    public void setSourcePptxPath(String sourcePptxPath) {
        this.sourcePptxPath = sourcePptxPath;
    }

    public String getOutputPptxPath() {
        return outputPptxPath;
    }

    public void setOutputPptxPath(String outputPptxPath) {
        this.outputPptxPath = outputPptxPath;
    }
}
