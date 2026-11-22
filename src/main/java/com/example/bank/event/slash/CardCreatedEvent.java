package com.example.bank.event.slash;

public record CardCreatedEvent(
        Long cardId,
        String slashCardId
) {}
