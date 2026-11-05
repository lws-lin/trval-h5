package com.trval.mine;

public record PlanRecord(
        String id,
        String departText,
        String departCode,
        String destText,
        String destCode,
        String budget,
        String days,
        String createdAt
) {
}
