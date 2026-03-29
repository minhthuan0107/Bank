package com.example.bank.event;

public record CardCreatedEvent(
        Long cardId,
        String slashCardId
) {}
