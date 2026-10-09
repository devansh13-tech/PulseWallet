package com.pulsewallet.pulsewallet.dto;

import java.util.List;

import com.pulsewallet.pulsewallet.dto.BankIngestionDtos.ParsedTransactionDto;

public class AiAssistantDtos {

    public record AiChatRequest(
            String message,
            String language, // en, hi, mr
            List<String> conversationHistory) {}

    public record AiChatResponse(
            String reply,
            String language,
            String intent,
            List<String> suggestedQuickPrompts,
            ParsedTransactionDto stagedTransaction) {}
}
