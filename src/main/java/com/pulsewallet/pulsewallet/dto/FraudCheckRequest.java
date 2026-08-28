package com.pulsewallet.pulsewallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FraudCheckRequest(
        @JsonProperty("Time") double time,
        @JsonProperty("V1") double v1,
        @JsonProperty("V2") double v2,
        @JsonProperty("V3") double v3,
        @JsonProperty("V4") double v4,
        @JsonProperty("V5") double v5,
        @JsonProperty("V6") double v6,
        @JsonProperty("V7") double v7,
        @JsonProperty("V8") double v8,
        @JsonProperty("V9") double v9,
        @JsonProperty("V10") double v10,
        @JsonProperty("V11") double v11,
        @JsonProperty("V12") double v12,
        @JsonProperty("V13") double v13,
        @JsonProperty("V14") double v14,
        @JsonProperty("V15") double v15,
        @JsonProperty("V16") double v16,
        @JsonProperty("V17") double v17,
        @JsonProperty("V18") double v18,
        @JsonProperty("V19") double v19,
        @JsonProperty("V20") double v20,
        @JsonProperty("V21") double v21,
        @JsonProperty("V22") double v22,
        @JsonProperty("V23") double v23,
        @JsonProperty("V24") double v24,
        @JsonProperty("V25") double v25,
        @JsonProperty("V26") double v26,
        @JsonProperty("V27") double v27,
        @JsonProperty("V28") double v28,
        @JsonProperty("Amount") double amount) {
}