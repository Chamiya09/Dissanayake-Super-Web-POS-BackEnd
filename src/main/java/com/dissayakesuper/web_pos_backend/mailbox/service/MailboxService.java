package com.dissayakesuper.web_pos_backend.mailbox.service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.dissayakesuper.web_pos_backend.mailbox.dto.MailboxMessageDTO;
import com.dissayakesuper.web_pos_backend.mailbox.dto.SendMailboxEmailRequestDTO;

import jakarta.mail.BodyPart;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MailboxService {

    private static final int MAX_PREVIEW_LEN = 160;
    private static final int MAX_BODY_LEN = 12000;

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String gmailUsername;

    @Value("${spring.mail.password}")
    private String gmailAppPassword;

    @Value("${app.mail.sender-display-name:Dissanayake Super - Orders}")
    private String senderDisplayName;

    @Value("${app.mail.imap.host:imap.gmail.com}")
    private String imapHost;

    @Value("${app.mail.imap.port:993}")
    private int imapPort;

    public MailboxService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public List<MailboxMessageDTO> listInbox(int limit) {
        return readFolder("INBOX", "Inbox", limit);
    }

    public List<MailboxMessageDTO> listSent(int limit) {
        String[] sentCandidates = new String[]{"[Gmail]/Sent Mail", "[Gmail]/Sent", "Sent Mail", "Sent"};
        return readFirstAvailableFolder(sentCandidates, "Sent", limit);
    }

    public void send(SendMailboxEmailRequestDTO request) {
        validateMailConfig();
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(gmailUsername, senderDisplayName);
            helper.setTo(request.to().trim());
            helper.setSubject(request.subject().trim());
            helper.setText(request.body().trim(), false);
            mailSender.send(message);
        } catch (Exception ex) {
            log.error("[MailboxService] Failed to send Gmail message: {}", ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Failed to send email via Gmail. Check MAIL_USERNAME and MAIL_APP_PASSWORD.");
        }
    }

    private List<MailboxMessageDTO> readFirstAvailableFolder(String[] folderNames, String category, int limit) {
        for (String folderName : folderNames) {
            try {
                return readFolder(folderName, category, limit);
            } catch (ResponseStatusException ex) {
                if (ex.getStatusCode() != HttpStatus.NOT_FOUND) {
                    throw ex;
                }
            }
        }

        // Fallback: scan all top-level folders and pick one that looks like "sent".
        try (Store store = buildImapStore()) {
            Folder[] allFolders = store.getDefaultFolder().list("*");
            String dynamicSent = Stream.of(allFolders)
                    .map(Folder::getFullName)
                    .filter(name -> name != null && name.toLowerCase(Locale.ENGLISH).contains("sent"))
                    .findFirst()
                    .orElse(null);
            if (dynamicSent != null) {
                return readFolder(dynamicSent, category, limit);
            }
        } catch (Exception ex) {
            log.warn("[MailboxService] Could not discover Sent folder dynamically: {}", ex.getMessage());
        }

        return List.of();
    }

    private List<MailboxMessageDTO> readFolder(String folderName, String category, int limit) {
        validateMailConfig();

        int safeLimit = Math.max(1, Math.min(limit, 100));

        try (Store store = buildImapStore()) {
            Folder folder = store.getFolder(folderName);
            if (folder == null || !folder.exists()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Folder not found: " + folderName);
            }

            folder.open(Folder.READ_ONLY);
            int total = folder.getMessageCount();
            if (total <= 0) {
                folder.close(false);
                return List.of();
            }

            int from = Math.max(1, total - safeLimit + 1);
            Message[] messages = folder.getMessages(from, total);

            List<MailboxMessageDTO> result = new ArrayList<>(messages.length);
            for (int i = messages.length - 1; i >= 0; i--) {
                try {
                    result.add(toDto(messages[i], category));
                } catch (Exception perMailEx) {
                    log.warn("[MailboxService] Skipping unreadable message #{} in {}: {}",
                            messages[i].getMessageNumber(), folderName, perMailEx.getMessage());
                }
            }

            folder.close(false);
            return result;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[MailboxService] Failed to read '{}' from Gmail: {}", folderName, ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Failed to read Gmail mailbox. Check IMAP access and Gmail App Password.");
        }
    }

    private Store buildImapStore() throws Exception {
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", imapHost);
        props.put("mail.imaps.port", String.valueOf(imapPort));
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.connectiontimeout", "5000");
        props.put("mail.imaps.timeout", "5000");

        Session session = Session.getInstance(props);
        Store store = session.getStore("imaps");
        store.connect(imapHost, imapPort, gmailUsername, gmailAppPassword);
        return store;
    }

    private MailboxMessageDTO toDto(Message message, String category) throws Exception {
        InternetAddress fromAddr = extractFrom(message);
        String subject = sanitizeOrDefault(message.getSubject(), "(No Subject)");
        String body = extractText(message);
        String preview = shorten(body, MAX_PREVIEW_LEN);

        String sentAt = "";
        if (message.getSentDate() != null) {
            sentAt = message.getSentDate().toInstant().atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMAT);
        } else if (message.getReceivedDate() != null) {
            sentAt = message.getReceivedDate().toInstant().atZone(ZoneId.systemDefault()).format(DATE_TIME_FORMAT);
        }

        boolean unread = "Inbox".equalsIgnoreCase(category) && !message.isSet(Flags.Flag.SEEN);
        boolean starred = message.isSet(Flags.Flag.FLAGGED);

        return new MailboxMessageDTO(
                message.getMessageNumber(),
                fromAddr != null ? sanitizeOrDefault(fromAddr.getPersonal(), fromAddr.getAddress()) : "Unknown Sender",
                fromAddr != null ? sanitizeOrDefault(fromAddr.getAddress(), "") : "",
                subject,
                preview,
                body,
                category,
                sentAt,
                unread,
                starred,
                Collections.emptyList()
        );
    }

    private InternetAddress extractFrom(Message message) throws Exception {
        var from = message.getFrom();
        if (from == null || from.length == 0) return null;
        if (from[0] instanceof InternetAddress addr) return addr;
        return new InternetAddress(from[0].toString());
    }

    private String extractText(Part part) throws Exception {
        if (part.isMimeType("text/plain")) {
            Object content = part.getContent();
            return shorten(sanitizeOrDefault(content == null ? "" : String.valueOf(content), ""), MAX_BODY_LEN);
        }

        if (part.isMimeType("text/html")) {
            Object content = part.getContent();
            String html = content == null ? "" : String.valueOf(content);
            return shorten(stripHtml(html), MAX_BODY_LEN);
        }

        if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            String fallback = "";
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                String disposition = bodyPart.getDisposition();
                if (Part.ATTACHMENT.equalsIgnoreCase(disposition)) {
                    continue;
                }
                if (bodyPart.isMimeType("text/plain")) {
                    return shorten(sanitizeOrDefault(String.valueOf(bodyPart.getContent()), ""), MAX_BODY_LEN);
                }
                if (bodyPart.isMimeType("multipart/*")) {
                    String nested = extractText(bodyPart);
                    if (!nested.isBlank()) {
                        return shorten(nested, MAX_BODY_LEN);
                    }
                }
                if (fallback.isBlank() && bodyPart.isMimeType("text/html")) {
                    fallback = stripHtml(String.valueOf(bodyPart.getContent()));
                }
            }
            return shorten(fallback, MAX_BODY_LEN);
        }

        if (part.isMimeType("message/rfc822")) {
            Object content = part.getContent();
            if (content instanceof Part nestedPart) {
                return shorten(extractText(nestedPart), MAX_BODY_LEN);
            }
        }

        Object content = part.getContent();
        if (content instanceof InputStream stream) {
            byte[] data = stream.readAllBytes();
            return shorten(new String(data, StandardCharsets.UTF_8), MAX_BODY_LEN);
        }

        return "";
    }

    private String stripHtml(String html) {
        if (html == null || html.isBlank()) return "";
        String withoutScripts = html.replaceAll("<script[^>]*>[\\s\\S]*?</script>", " ");
        String withoutStyles = withoutScripts.replaceAll("<style[^>]*>[\\s\\S]*?</style>", " ");
        String plain = withoutStyles.replaceAll("<[^>]+>", " ");
        return plain.replaceAll("\\s+", " ").trim();
    }

    private String shorten(String value, int maxLen) {
        String safe = sanitizeOrDefault(value, "").trim();
        if (safe.length() <= maxLen) return safe;
        return safe.substring(0, maxLen - 1) + "...";
    }

    private String sanitizeOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value;
    }

    private void validateMailConfig() {
        if (gmailUsername == null || gmailUsername.isBlank() || gmailAppPassword == null || gmailAppPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Mail credentials are missing. Set MAIL_USERNAME and MAIL_APP_PASSWORD.");
        }
    }
}
