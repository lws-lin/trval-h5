package com.trval.plan;

import com.trval.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    @PostMapping("/plan")
    public ApiResponse<PlanResult> create(
            @Valid @RequestBody PlanRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-Model", required = false) String model
    ) {
        String apiKey = apiKey(authorization);
        if (apiKey.isBlank()) {
            return ApiResponse.fail(401, "请先填写 API Key");
        }
        return ApiResponse.ok(planService.create(request, apiKey, model));
    }

    private String apiKey(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return "";
        }
        String value = authorization.trim();
        if (value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return value.substring(7).trim();
        }
        return value;
    }
}
