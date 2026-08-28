package com.pulsewallet.pulsewallet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

import com.pulsewallet.pulsewallet.dto.FraudCheckRequest;
import com.pulsewallet.pulsewallet.dto.FraudCheckResponse;
import com.pulsewallet.pulsewallet.service.FraudDetectionService;

/**
 * Slice test for {@link FraudDetectionController}.
 *
 * <p>{@code addFilters = false} disables the JWT filter chain so these tests
 * exercise controller logic in isolation from authentication.
 */
@WebMvcTest(FraudDetectionController.class)
@AutoConfigureMockMvc(addFilters = false)
class FraudDetectionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FraudDetectionService fraudDetectionService;

    private static final String VALID_REQUEST_JSON = """
            {
                "Time": 0.0,
                "V1": -1.3598071336738, "V2": -0.0727811733098497,
                "V3": 2.53634673796914, "V4": 1.37815522427443,
                "V5": -0.338320769942518, "V6": 0.462387777762292,
                "V7": 0.239598554061257, "V8": 0.0986979012610507,
                "V9": 0.363786969611213, "V10": 0.0907941719789316,
                "V11": -0.551599533260813, "V12": -0.617800855762348,
                "V13": -0.991389847235408, "V14": -0.311169353699879,
                "V15": 1.46817697209427, "V16": -0.470400525259478,
                "V17": 0.207971241929242, "V18": 0.0257905801985591,
                "V19": 0.403992960255733, "V20": 0.251412098239705,
                "V21": -0.018306777944153, "V22": 0.277837575558899,
                "V23": -0.110473910188767, "V24": 0.0669280749146731,
                "V25": 0.128539358273528, "V26": -0.189114843888824,
                "V27": 0.133558376740387, "V28": -0.0210530534538215,
                "Amount": 149.62
            }
            """;

    @Test
    void checkFraud_returnsOkWithFraudResponse() throws Exception {
        FraudCheckResponse mockResponse = new FraudCheckResponse(true, 0.92, 92.0, "CRITICAL");
        when(fraudDetectionService.checkFraud(any(FraudCheckRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/fraud/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.is_fraud").value(true))
                .andExpect(jsonPath("$.data.fraud_probability").value(0.92))
                .andExpect(jsonPath("$.data.risk_score").value(92.0))
                .andExpect(jsonPath("$.data.risk_level").value("CRITICAL"));

        verify(fraudDetectionService).checkFraud(any(FraudCheckRequest.class));
    }

    @Test
    void checkFraud_returnsLowRiskResponse() throws Exception {
        FraudCheckResponse mockResponse = new FraudCheckResponse(false, 0.05, 5.0, "LOW");
        when(fraudDetectionService.checkFraud(any(FraudCheckRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/fraud/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.is_fraud").value(false))
                .andExpect(jsonPath("$.data.fraud_probability").value(0.05))
                .andExpect(jsonPath("$.data.risk_level").value("LOW"));
    }

    @Test
    void checkFraud_returns500WhenServiceThrows() throws Exception {
        when(fraudDetectionService.checkFraud(any(FraudCheckRequest.class)))
                .thenThrow(new ResourceAccessException("Connection refused"));

        mockMvc.perform(post("/api/fraud/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void checkFraud_invokesServiceWithRequest() throws Exception {
        FraudCheckResponse mockResponse = new FraudCheckResponse(false, 0.1, 10.0, "LOW");
        when(fraudDetectionService.checkFraud(any(FraudCheckRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/fraud/check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_JSON))
                .andExpect(status().isOk());

        verify(fraudDetectionService).checkFraud(any(FraudCheckRequest.class));
    }
}
