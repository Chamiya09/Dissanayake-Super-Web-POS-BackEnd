package com.dissayakesuper.web_pos_backend.reorder.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import com.dissayakesuper.web_pos_backend.reorder.dto.ReorderItemRequestDTO;
import com.dissayakesuper.web_pos_backend.shared.mail.EmailUiTemplate;

import java.util.List;

/**
 * Handles all outbound email operations for the Reorder module.
 *
 * <p>Two categories of mail are sent on every new purchase order:
 * <ol>
 *   <li><b>Supplier PO</b> â€” an invoice-style HTML email sent to the supplier with
 *       full line-item detail, totals, and the authorising managerâ€™s name.</li>
 *   <li><b>Admin Notification</b> â€” a concise dashboard-alert email sent to the
 *       configured admin address ({@code app.admin.email}) summarising the order.</li>
 * </ol>
 *
 * <p>Both methods are {@code @Async}: failures are logged and swallowed so that
 * a mail outage never rolls back a successfully saved database transaction.
 *
 * <p>SMTP credentials are injected through {@code application.properties}:
 * <pre>
 *   spring.mail.host
 *   spring.mail.port
 *   spring.mail.username      / MAIL_USERNAME env-var
 *   spring.mail.password      / MAIL_APP_PASSWORD env-var
 *   app.mail.sender-display-name
 *   app.admin.email
 * </pre>
 */
@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${spring.mail.password}")
    private String smtpPassword;

    @Value("${app.mail.sender-display-name:Dissanayake Super \u2013 Orders}")
    private String displayName;

    /** Destination for admin order-alert emails. */
    @Value("${app.admin.email:admin@dissanayakesuper.lk}")
    private String adminEmail;

    @Value("${app.reorder.accept-url-base:http://localhost:8080/api/v1/reorder/accept}")
    private String supplierAcceptUrlBase;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // â”€â”€ Public API â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Sends a professional invoice-style HTML Purchase Order to the supplier.
     *
     * @param to           Supplier's email address
     * @param supplierName Supplier's company name (used in the greeting line)
     * @param orderRef     Unique PO reference (e.g. PO-1717000000000)
     * @param items        Order line items
     * @param totalAmount  Calculated order total (LKR)
     * @param managerName  Full name of the manager who authorised the order
     */
    public void sendSupplierPO(
            String to,
            String supplierName,
            String orderRef,
            List<ReorderItemRequestDTO> items,
            double totalAmount,
        String managerName,
        String acceptToken,
        boolean revisedOrder) {

      String subject = (revisedOrder ? "Updated Purchase Order \u2014 " : "Purchase Order \u2014 ") + orderRef;
      String acceptUrl = supplierAcceptUrlBase + "?token=" + acceptToken;
      String supplierBodyHtml = buildSupplierHtml(
          to,
          supplierName,
          orderRef,
          items,
          totalAmount,
          managerName,
          acceptUrl,
          revisedOrder
      );
      String html = EmailUiTemplate.wrapInCommonLayout(
              subject,
              revisedOrder ? "Revised" : "Purchase Order",
              "Supplier Purchase Order",
              "Reference: " + orderRef,
              supplierBodyHtml,
              "This is an automated message from Dissanayake Super Inventory System."
      );
        validateMailConfiguration();
        sendHtmlOrThrow(to, subject, html, "supplier PO", orderRef);
    }

    /**
     * Sends a concise dashboard-alert email to the configured admin address
     * ({@code app.admin.email} in {@code application.properties}).
     *
     * @param orderRef      Unique PO reference
     * @param supplierName  Supplier's company name
     * @param supplierEmail Supplier's email address (shown in the alert)
     * @param totalAmount   Order total (LKR)
     * @param managerName   Full name of the manager who placed the order
     * @param placedAt      Date the order was placed (yyyy-MM-dd)
     */
    public void sendAdminNotification(
            String orderRef,
            String supplierName,
            String supplierEmail,
            double totalAmount,
            String managerName,
            String placedAt) {

        String subject = "\uD83D\uDCE6 New Purchase Order: " + orderRef;
        String adminBodyHtml = buildAdminHtml(orderRef, supplierName, supplierEmail, totalAmount, managerName, placedAt);
        String html = EmailUiTemplate.wrapInCommonLayout(
          subject,
          "Admin Alert",
          "Purchase Order Notification",
          "Reference: " + orderRef,
          adminBodyHtml,
          "Admin notification generated by Dissanayake Super Inventory System."
        );
    sendHtmlBestEffort(adminEmail, subject, html, "admin notification", orderRef);
    }

    // â”€â”€ Shared send helper â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Creates and sends a {@link MimeMessage}.
     * All checked exceptions and {@link MailException}s are caught here so
     * that mail failures never propagate to the transaction layer.
     */
    private void sendHtmlOrThrow(String to, String subject, String html,
                   String kind, String orderRef) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress, displayName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("[EmailService] {} sent \u2192 {} (ref: {})", kind, to, orderRef);
        } catch (MailException | MessagingException | java.io.UnsupportedEncodingException ex) {
            log.error("[EmailService] Failed to send {} to {} (ref: {}): {}",
                    kind, to, orderRef, ex.getMessage(), ex);
        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Failed to send supplier email via Gmail SMTP. Check MAIL_USERNAME/MAIL_APP_PASSWORD and Gmail App Password setup."
        );
        }
    }

    private void sendHtmlBestEffort(String to, String subject, String html,
                    String kind, String orderRef) {
      try {
        sendHtmlOrThrow(to, subject, html, kind, orderRef);
      } catch (ResponseStatusException ex) {
        log.warn("[EmailService] Best-effort email '{}' skipped: {}", kind, ex.getReason());
      }
    }

    private void validateMailConfiguration() {
      if (!StringUtils.hasText(fromAddress)
          || !StringUtils.hasText(smtpPassword)
          || "changeme".equalsIgnoreCase(smtpPassword.trim())) {
        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Mail is not configured. Set MAIL_USERNAME and MAIL_APP_PASSWORD (Gmail App Password)."
        );
      }
    }

    // â”€â”€ Supplier PO HTML builder â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private String buildSupplierHtml(
            String supplierEmail,
            String supplierName,
            String orderRef,
            List<ReorderItemRequestDTO> items,
            double totalAmount,
          String managerName,
          String acceptUrl,
          boolean revisedOrder) {

        String today = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy"));

        String greeting = (supplierName != null && !supplierName.isBlank())
                ? "Dear " + escapeHtml(supplierName) + ","
                : "Dear Supplier,";

        String orderMetaLine = revisedOrder
          ? "This purchase order has been revised. Please review the latest details and confirm acceptance."
          : "Please find below the purchase order details. Kindly confirm receipt and advise on the expected delivery date at your earliest convenience.";

        StringBuilder rows = new StringBuilder();
        int rowNum = 1;
        for (ReorderItemRequestDTO item : items) {
            double lineTotal = item.quantity().doubleValue() * item.unitPrice();
            String rowBg = (rowNum % 2 == 0) ? "#f8fafc" : "#ffffff";
            rows.append(String.format("""
                    <tr style="background:%s;">
                      <td style="%s text-align:center;">%d</td>
                      <td style="%s">%s</td>
                      <td style="%s text-align:center;">%s</td>
                      <td style="%s text-align:right;">LKR %.2f</td>
                      <td style="%s text-align:right;font-weight:600;">LKR %.2f</td>
                    </tr>
                    """,
                    rowBg,
                    TD_STYLE, rowNum,
                    TD_STYLE, escapeHtml(item.productName()),
                    TD_STYLE, item.quantity().toPlainString(),
                    TD_STYLE, item.unitPrice(),
                    TD_STYLE, lineTotal
            ));
            rowNum++;
        }

        return String.format("""
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
                <title>Purchase Order %s</title></head>
                <body style="margin:0;padding:0;background:#f1f5f9;font-family:Arial,Helvetica,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f1f5f9;padding:32px 0;">
                    <tr><td align="center">
                      <table width="620" cellpadding="0" cellspacing="0"
                             style="background:#ffffff;border-radius:12px;overflow:hidden;
                                    box-shadow:0 4px 20px rgba(0,0,0,.08);max-width:620px;width:100%%;">
                        <!-- Top bar -->
                        <tr>
                          <td style="background:#1e1b4b;padding:24px 32px;">
                            <table width="100%%" cellpadding="0" cellspacing="0">
                              <tr>
                                <td>
                                  <div style="color:#ffffff;font-size:18px;font-weight:700;letter-spacing:-.2px;">Dissanayake Super</div>
                                  <div style="color:#c7d2fe;font-size:11px;margin-top:3px;">No. 45, Main Street, Colombo 03 &nbsp;&middot;&nbsp; %s</div>
                                </td>
                                <td align="right">
                                  <div style="color:#ffffff;font-size:22px;font-weight:800;letter-spacing:-.5px;">PURCHASE ORDER</div>
                                  <div style="color:#a5b4fc;font-size:11px;margin-top:3px;">Ref: %s</div>
                                </td>
                              </tr>
                            </table>
                          </td>
                        </tr>
                        <!-- Meta row -->
                        <tr>
                          <td style="background:#f8fafc;border-bottom:1px solid #e2e8f0;padding:14px 32px;">
                            <table width="100%%" cellpadding="0" cellspacing="0">
                              <tr>
                                <td style="font-size:11px;color:#64748b;"><strong style="color:#0f172a;">Date:</strong> %s</td>
                                <td align="right" style="font-size:11px;color:#64748b;"><strong style="color:#0f172a;">To:</strong> %s</td>
                              </tr>
                            </table>
                          </td>
                        </tr>
                        <!-- Greeting -->
                        <tr>
                          <td style="padding:24px 32px 0;">
                            <p style="margin:0 0 6px;font-size:14px;color:#0f172a;">%s</p>
                            <p style="margin:0 0 20px;font-size:13px;color:#475569;line-height:1.6;">
                              %s
                            </p>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:0 32px 20px;">
                            <table width="100%%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;">
                              <tr>
                                <td style="background:#ecfeff;border:1px solid #99f6e4;border-radius:10px;padding:14px;">
                                  <div style="font-size:12px;color:#0f766e;font-weight:700;letter-spacing:.02em;margin-bottom:8px;">Supplier Action Required</div>
                                  <p style="margin:0 0 12px;font-size:12px;color:#134e4a;line-height:1.6;">
                                    Click the button below to accept this order. Once accepted, this order will be locked and cannot be revised again.
                                  </p>
                                  <a href="%s" style="display:inline-block;background:#0d9488;color:#ffffff;text-decoration:none;padding:10px 16px;border-radius:8px;font-size:12px;font-weight:700;letter-spacing:.01em;">Accept Order</a>
                                </td>
                              </tr>
                            </table>
                          </td>
                        </tr>
                        <!-- Items table -->
                        <tr>
                          <td style="padding:0 32px 24px;">
                            <table width="100%%" cellpadding="0" cellspacing="0"
                                   style="border-collapse:collapse;border-radius:8px;overflow:hidden;border:1px solid #e2e8f0;">
                              <thead>
                                <tr style="background:#1e1b4b;">
                                  <th style="%s text-align:center;width:40px;">#</th>
                                  <th style="%s text-align:left;">Product Name</th>
                                  <th style="%s text-align:center;width:80px;">Qty</th>
                                  <th style="%s text-align:right;width:110px;">Unit Price</th>
                                  <th style="%s text-align:right;width:110px;">Line Total</th>
                                </tr>
                              </thead>
                              <tbody>%s</tbody>
                              <tfoot>
                                <tr style="background:#1e1b4b;">
                                  <td colspan="4" style="padding:10px 14px;color:#c7d2fe;font-size:12px;font-weight:700;text-align:right;">ORDER TOTAL</td>
                                  <td style="padding:10px 14px;color:#ffffff;font-size:13px;font-weight:800;text-align:right;">LKR %.2f</td>
                                </tr>
                              </tfoot>
                            </table>
                          </td>
                        </tr>
                        <!-- Signature -->
                        <tr>
                          <td style="padding:0 32px 28px;">
                            <p style="margin:0 0 4px;font-size:12px;color:#64748b;">Authorised by:</p>
                            <p style="margin:0;font-size:14px;font-weight:700;color:#0f172a;">%s</p>
                            <p style="margin:2px 0 0;font-size:11px;color:#94a3b8;">Purchasing Department &mdash; Dissanayake Super</p>
                          </td>
                        </tr>
                        <!-- Footer -->
                        <tr>
                          <td style="background:#f8fafc;border-top:1px solid #e2e8f0;padding:12px 32px;text-align:center;">
                            <p style="margin:0;font-size:10px;color:#94a3b8;">
                              This is an automated message from the Dissanayake Super Inventory System &nbsp;&middot;&nbsp; Do not reply to this email
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """,
                orderRef,
                escapeHtml(fromAddress), orderRef,
                today, escapeHtml(supplierEmail),
                greeting,
                escapeHtml(orderMetaLine),
                escapeHtml(acceptUrl),
                TH_STYLE, TH_STYLE, TH_STYLE, TH_STYLE, TH_STYLE,
                rows,
                totalAmount,
                escapeHtml(managerName)
        );
    }

    // â”€â”€ Admin notification HTML builder â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private String buildAdminHtml(
            String orderRef,
            String supplierName,
            String supplierEmail,
            double totalAmount,
            String managerName,
            String placedAt) {

        String safeSupplier = (supplierName != null && !supplierName.isBlank())
                ? escapeHtml(supplierName)
                : escapeHtml(supplierEmail);

        return String.format("""
                <!DOCTYPE html>
                <html lang="en">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
                <title>New Purchase Order: %s</title></head>
                <body style="margin:0;padding:0;background:#f1f5f9;font-family:Arial,Helvetica,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f1f5f9;padding:32px 0;">
                    <tr><td align="center">
                      <table width="560" cellpadding="0" cellspacing="0"
                             style="background:#ffffff;border-radius:12px;overflow:hidden;
                                    box-shadow:0 4px 20px rgba(0,0,0,.08);max-width:560px;width:100%%;">
                        <!-- Header -->
                        <tr>
                          <td style="background:#0f172a;padding:20px 28px;">
                            <table width="100%%" cellpadding="0" cellspacing="0">
                              <tr>
                                <td>
                                  <div style="color:#94a3b8;font-size:11px;font-weight:700;letter-spacing:.08em;text-transform:uppercase;">Dissanayake Super &mdash; Admin Alerts</div>
                                  <div style="color:#ffffff;font-size:17px;font-weight:800;margin-top:4px;">New Purchase Order Placed</div>
                                </td>
                                <td align="right">
                                  <span style="display:inline-block;background:#fef9c3;color:#713f12;font-size:11px;font-weight:700;padding:4px 10px;border-radius:999px;letter-spacing:.04em;">&#x23F3; PENDING</span>
                                </td>
                              </tr>
                            </table>
                          </td>
                        </tr>
                        <!-- Alert banner -->
                        <tr>
                          <td style="background:#eff6ff;border-left:4px solid #3b82f6;padding:12px 28px;">
                            <p style="margin:0;font-size:13px;color:#1e40af;font-weight:600;">
                              &#x2139;&#xFE0F;&nbsp; A new purchase order has been created and the supplier has been notified automatically.
                            </p>
                          </td>
                        </tr>
                        <!-- Detail cards -->
                        <tr>
                          <td style="padding:24px 28px 0;">
                            <table width="100%%" cellpadding="0" cellspacing="0" style="border-collapse:collapse;">
                              <tr>
                                <td width="50%%" style="padding:0 8px 16px 0;vertical-align:top;">
                                  <div style="%s">
                                    <div style="font-size:10px;font-weight:700;color:#64748b;letter-spacing:.06em;text-transform:uppercase;margin-bottom:4px;">Order Reference</div>
                                    <div style="font-size:15px;font-weight:800;color:#0f172a;">%s</div>
                                  </div>
                                </td>
                                <td width="50%%" style="padding:0 0 16px 8px;vertical-align:top;">
                                  <div style="%s">
                                    <div style="font-size:10px;font-weight:700;color:#64748b;letter-spacing:.06em;text-transform:uppercase;margin-bottom:4px;">Date Placed</div>
                                    <div style="font-size:15px;font-weight:800;color:#0f172a;">%s</div>
                                  </div>
                                </td>
                              </tr>
                              <tr>
                                <td width="50%%" style="padding:0 8px 16px 0;vertical-align:top;">
                                  <div style="%s">
                                    <div style="font-size:10px;font-weight:700;color:#64748b;letter-spacing:.06em;text-transform:uppercase;margin-bottom:4px;">Supplier</div>
                                    <div style="font-size:13px;font-weight:700;color:#0f172a;">%s</div>
                                    <div style="font-size:11px;color:#64748b;margin-top:2px;">%s</div>
                                  </div>
                                </td>
                                <td width="50%%" style="padding:0 0 16px 8px;vertical-align:top;">
                                  <div style="%s">
                                    <div style="font-size:10px;font-weight:700;color:#64748b;letter-spacing:.06em;text-transform:uppercase;margin-bottom:4px;">Placed By</div>
                                    <div style="font-size:13px;font-weight:700;color:#0f172a;">%s</div>
                                    <div style="font-size:11px;color:#64748b;margin-top:2px;">Purchasing Manager</div>
                                  </div>
                                </td>
                              </tr>
                            </table>
                          </td>
                        </tr>
                        <!-- Total -->
                        <tr>
                          <td style="padding:0 28px 24px;">
                            <div style="background:#f0fdf4;border:1px solid #bbf7d0;border-radius:8px;padding:16px 20px;">
                              <table width="100%%" cellpadding="0" cellspacing="0">
                                <tr>
                                  <td>
                                    <div style="font-size:11px;font-weight:700;color:#16a34a;letter-spacing:.06em;text-transform:uppercase;">Order Total</div>
                                    <div style="font-size:26px;font-weight:900;color:#0f172a;margin-top:2px;">LKR %.2f</div>
                                  </td>
                                  <td align="right">
                                    <div style="background:#16a34a;color:#ffffff;font-size:11px;font-weight:700;padding:6px 14px;border-radius:999px;letter-spacing:.04em;">&#x2713; Saved to DB</div>
                                  </td>
                                </tr>
                              </table>
                            </div>
                          </td>
                        </tr>
                        <!-- Action hint -->
                        <tr>
                          <td style="padding:0 28px 28px;">
                            <p style="margin:0;font-size:12px;color:#64748b;line-height:1.6;">
                              Log in to the <a href="http://localhost:5173/reorder" style="color:#6366f1;font-weight:600;">Reorder Management dashboard</a> to review, confirm, or cancel this order.
                            </p>
                          </td>
                        </tr>
                        <!-- Footer -->
                        <tr>
                          <td style="background:#f8fafc;border-top:1px solid #e2e8f0;padding:12px 28px;text-align:center;">
                            <p style="margin:0;font-size:10px;color:#94a3b8;">
                              Dissanayake Super Inventory System &nbsp;&middot;&nbsp; Admin notification &nbsp;&middot;&nbsp; Do not reply
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """,
                orderRef,
                CARD_STYLE, escapeHtml(orderRef),
                CARD_STYLE, escapeHtml(placedAt),
                CARD_STYLE, safeSupplier, escapeHtml(supplierEmail),
                CARD_STYLE, escapeHtml(managerName),
                totalAmount
        );
    }

    // â”€â”€ Shared cell styles â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private static final String TH_STYLE =
            "padding:10px 14px;color:#ffffff;font-size:11px;" +
            "font-weight:700;letter-spacing:.04em;text-transform:uppercase;";

    private static final String TD_STYLE =
            "padding:10px 14px;font-size:12px;color:#334155;" +
            "border-bottom:1px solid #e2e8f0;";

    private static final String CARD_STYLE =
            "background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;padding:12px 16px;";

    // â”€â”€ Utility â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&",  "&amp;")
                .replace("<",  "&lt;")
                .replace(">",  "&gt;")
                .replace("\"", "&quot;");
    }
}
