package com.trval.chat;

import java.util.List;

public record ChatRequest(List<ChatMessage> messages, TripContext collected) {
}
