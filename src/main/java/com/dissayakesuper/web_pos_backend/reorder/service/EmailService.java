package com.dissayakesuper.web_pos_backend.reorder.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.dissayakesuper.web_pos_backend.reorder.dto.ReorderItemRequestDTO;

import java.util.List;

/**
 * Handles all outbound email operations for the Reorder module.
 *
 * <p>Sending is performed on a dedicated async thread so it never blocks
 * the HTTP request that triggers {@code createOrder}.
 *
 * <p>SMTP credentials are injected through {@code application.properties}:
 * <pre>
 *   spring.mail.host
 *   spring.mail.port
 *   spring.mail.username
 *   spring.mail.password
 *   app.mail.sender-display-name
 * </pre>
 */
@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.mail.sender-display-name:Dissanayake Super – Orders}")
    private String displayName;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Sends a professional HTML purchase-order email asynchronously.
     *
     * @param to          Supplier's email address
     * @param orderRef    Unique PO reference (e.g. PO-1717000000000)
     * @param items       Order line items
     * @param totalAmount Calculated order total
     * @param managerName Name of the authorising manager
     */
    @Async
    public void sendPurchaseOrderEmail(
            String to,
            String orderRef,
            List<ReorderItemRequestDTO> items,
            double totalAmount,
            String managerName) {

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, displayName);
            helper.setTo(to);
            helper.setSubject("Purchase Order — " + orderRef);
            helper.setText(buildHtmlBody(to, orderRef, items, totalAmount, managerName), true);

            mailSender.send(message);
            log.info("[EmailService] Purchase order email sent → {} (ref: {})", to, orderRef);

        } catch (MailException | MessagingException | java.io.UnsupportedEncodingException ex) {
            // Log and swallow — a failed email must not roll back the saved order
            log.error("[EmailService] Failed to send purchase order email to {} (ref: {}): {}",
                    to, orderRef, ex.getMessage(), ex);
        }
    }

    // ── HTML Builder ──────────────────────────────────────────────────────────

    private String buildHtmlBody(
            String supplierEmail,
            String orderRef,
            List<ReorderItemRequestDTO> items,
            double totalAmount,
            String managerName) {

        String today = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy"));

        // Build item rows
        StringBuilder rows = new StringBuilder();
        int rowNum = 1;
        for (ReorderItemRequestDTO item : items) {
            double lineTotal = item.quantity() * item.unitPrice();
            String rowBg = (rowNum % 2 == 0) ? "#f8fafc" : "#ffffff";
            rows.append(String.format("""
                    <tr style="background:%s;">
                      <td style="%s text-align:center;">%d</td>
                      <td style="%s">%s</td>
                      <td style="%s text-align:center;">%d</td>
                      <td style="%s text-align:right;">LKR %.2f</td>
                      <td style="%s text-align:right;font-weight:600;">LKR %.2f</td>
                    </tr>
                    """,
                    rowBg,
                    TD_STYLE, rowNum,
                    TD_STYLE, escapeHtml(item.productName()),
                    TD_STYLE, item.quantity(),
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

                        <!-- ── Top bar ── -->
                        <tr>
                          <td style="background:#1e1b4b;padding:24px 32px;">
                            <table width="100%%" cellpadding="0" cellspacing="0">
                              <tr>
                                <td>
                                  <div style="color:#ffffff;font-size:18px;font-weight:700;
                                              letter-spacing:-.2px;">Dissanayake Super</div>
                                  <div style="color:#c7d2fe;font-size:11px;margin-top:3px;">
                                    No. 45, Main Street, Colombo 03 &nbsp;·&nbsp;
                                    %s
                                  </div>
                                </td>
                                <td align="right">
                                  <div style="color:#ffffff;font-size:22px;font-weight:800;
                                              letter-spacing:-.5px;">PURCHASE ORDER</div>
                                  <div style="color:#a5b4fc;font-size:11px;margin-top:3px;">
                                    Ref: %s
                                  </div>
                                </td>
                              </tr>
                            </table>
                          </td>
                        </tr>

                        <!-- ── Meta row ── -->
                        <tr>
                          <td style="background:#f8fafc;border-bottom:1px solid #e2e8f0;
                                     padding:14px 32px;">
                            <table width="100%%" cellpadding="0" cellspacing="0">
                              <tr>
                                <td style="font-size:11px;color:#64748b;">
                                  <strong style="color:#0f172a;">Date:</strong> %s
                                </td>
                                <td align="right" style="font-size:11px;color:#64748b;">
                                  <strong style="color:#0f172a;">To:</strong> %s
                                </td>
                              </tr>
                            </table>
                          </td>
                        </tr>

                        <!-- ── Greeting ── -->
                        <tr>
                          <td style="padding:24px 32px 0;">
                            <p style="margin:0 0 6px;font-size:14px;color:#0f172a;">
                              Dear Supplier,
                            </p>
                            <p style="margin:0 0 20px;font-size:13px;color:#475569;line-height:1.6;">
                              Please find below the purchase order details. Kindly confirm
                              receipt and advise on the expected delivery date at your earliest
                              convenience.
                            </p>
                          </td>
                        </tr>

                        <!-- ── Items table ── -->
                        <tr>
                          <td style="padding:0 32px 24px;">
                            <table width="100%%" cellpadding="0" cellspacing="0"
                                   style="border-collapse:collapse;border-radius:8px;
                                          overflow:hidden;border:1px solid #e2e8f0;">
                              <thead>
                                <tr style="background:#1e1b4b;">
                                  <th style="%s text-align:center;width:40px;">#</th>
                                  <th style="%s text-align:left;">Product Name</th>
                                  <th style="%s text-align:center;width:80px;">Qty</th>
                                  <th style="%s text-align:right;width:110px;">Unit Price</th>
                                  <th style="%s text-align:right;width:110px;">Line Total</th>
                                </tr>
                              </thead>
                              <tbody>
                                %s
                              </tbody>
                              <tfoot>
                                <tr style="background:#1e1b4b;">
                                  <td colspan="4"
                                      style="padding:10px 14px;color:#c7d2fe;
                                             font-size:12px;font-weight:700;text-align:right;">
                                    ORDER TOTAL
                                  </td>
                                  <td style="padding:10px 14px;color:#ffffff;
                                             font-size:13px;font-weight:800;text-align:right;">
                                    LKR %.2f
                                  </td>
                                </tr>
                              </tfoot>
                            </table>
                          </td>
                        </tr>

                        <!-- ── Signature ── -->
                        <tr>
                          <td style="padding:0 32px 28px;">
                            <p style="margin:0 0 4px;font-size:12px;color:#64748b;">
                              Authorised by:
                            </p>
                            <p style="margin:0;font-size:14px;font-weight:700;color:#0f172a;">
                              %s
                            </p>
                            <p style="margin:2px 0 0;font-size:11px;color:#94a3b8;">
                              Purchasing Department — Dissanayake Super
                            </p>
                          </td>
                        </tr>

                        <!-- ── Footer ── -->
                        <tr>
                          <td style="background:#f8fafc;border-top:1px solid #e2e8f0;
                                     padding:12px 32px;text-align:center;">
                            <p style="margin:0;font-size:10px;color:#94a3b8;">
                              This is an automated message from the Dissanayake Super
                              Inventory System &nbsp;·&nbsp; Do not reply to this email
                            </p>
                          </td>
                        </tr>

                      </table>
                    </td></tr>
                  </table>

                </body>
                </html>
                """,
                // title
                orderRef,
                // top bar: sender email, ref
                escapeHtml(fromAddress), orderRef,
                // meta: date, to
                today, escapeHtml(supplierEmail),
                // thead styles (×5)
                TH_STYLE, TH_STYLE, TH_STYLE, TH_STYLE, TH_STYLE,
                // tbody rows
                rows,
                // total
                totalAmount,
                // signature
                escapeHtml(managerName)
        );
    }

    // ── Shared cell styles ────────────────────────────────────────────────────

    private static final String TH_STYLE =
            "padding:10px 14px;color:#ffffff;font-size:11px;" +
            "font-weight:700;letter-spacing:.04em;text-transform:uppercase;";

    private static final String TD_STYLE =
            "padding:10px 14px;font-size:12px;color:#334155;" +
            "border-bottom:1px solid #e2e8f0;";

    // ── Utility ───────────────────────────────────────────────────────────────

    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
