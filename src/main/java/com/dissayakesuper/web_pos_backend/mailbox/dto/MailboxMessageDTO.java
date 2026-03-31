package com.dissayakesuper.web_pos_backend.mailbox.dto;

import java.util.List;

public record MailboxMessageDTO(
        long id,
        String from,
        String fromEmail,
        String subject,
        String preview,
        String body,
        String category,
        String sentAt,
        boolean unread,
        boolean starred,
        List<String> tags
) {
}
