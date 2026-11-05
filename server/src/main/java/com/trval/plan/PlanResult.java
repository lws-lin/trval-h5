package com.trval.plan;

import java.util.List;

public record PlanResult(
        String departText,
        String destText,
        String budget,
        String days,
        String summary,
        List<ScheduleDay> schedule
) {
}
