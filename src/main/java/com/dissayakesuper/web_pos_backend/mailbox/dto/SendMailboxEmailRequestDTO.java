package com.dissayakesuper.web_pos_backend.mailbox.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendMailboxEmailRequestDTO(
        @NotBlank(message = "Recipient email is required")
        @Email(message = "Recipient email is invalid")
        String to,

        @NotBlank(message = "Subject is required")
        String subject,

        @NotBlank(message = "Body is required")
        String body
) {
}
