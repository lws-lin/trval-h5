package com.trval.mine;

import java.util.List;

public record MineProfile(UserProfile user, List<PlanRecord> records) {
}
