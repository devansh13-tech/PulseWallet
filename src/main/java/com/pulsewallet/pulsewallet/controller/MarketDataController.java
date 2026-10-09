package com.pulsewallet.pulsewallet.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pulsewallet.pulsewallet.dto.ApiResponse;
import com.pulsewallet.pulsewallet.dto.MarketIndicesResponse;
import com.pulsewallet.pulsewallet.dto.MarketMoversResponse;
import com.pulsewallet.pulsewallet.service.MarketIndexService;

@RestController
@RequestMapping("/api/market")
public class MarketDataController {

    private final MarketIndexService marketIndexService;

    public MarketDataController(MarketIndexService marketIndexService) {
        this.marketIndexService = marketIndexService;
    }

    @GetMapping("/indices")
    public ApiResponse<MarketIndicesResponse> getIndices(
            @RequestParam(required = false, defaultValue = "1D") String range) {
        return ApiResponse.ok(marketIndexService.getMarketIndices(range));
    }

    @GetMapping("/movers")
    public ApiResponse<MarketMoversResponse> getMovers() {
        return ApiResponse.ok(marketIndexService.getMarketMovers());
    }
}
