package com.trval.plan;

import jakarta.validation.constraints.NotBlank;

public record PlanRequest(
        @NotBlank(message = "请选择出发地") String departText,
        String departCode,
        @NotBlank(message = "请选择目的地") String destText,
        String destCode,
        String budget,
        String days,
        String extra
) {
}
