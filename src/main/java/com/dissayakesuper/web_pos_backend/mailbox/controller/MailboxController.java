package com.dissayakesuper.web_pos_backend.mailbox.controller;

import com.dissayakesuper.web_pos_backend.mailbox.dto.MailboxMessageDTO;
import com.dissayakesuper.web_pos_backend.mailbox.dto.SendMailboxEmailRequestDTO;
import com.dissayakesuper.web_pos_backend.mailbox.service.MailboxService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/mailbox")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class MailboxController {

    private final MailboxService mailboxService;

    public MailboxController(MailboxService mailboxService) {
        this.mailboxService = mailboxService;
    }

    @GetMapping("/inbox")
    public ResponseEntity<List<MailboxMessageDTO>> inbox(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(mailboxService.listInbox(limit));
    }

    @GetMapping("/sent")
    public ResponseEntity<List<MailboxMessageDTO>> sent(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(mailboxService.listSent(limit));
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> send(
            @Valid @RequestBody SendMailboxEmailRequestDTO request
    ) {
        mailboxService.send(request);
        return ResponseEntity.ok(Map.of("message", "Email sent successfully"));
    }
}
