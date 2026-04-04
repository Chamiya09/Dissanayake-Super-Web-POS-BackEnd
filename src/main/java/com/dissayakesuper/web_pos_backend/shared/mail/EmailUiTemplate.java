package com.dissayakesuper.web_pos_backend.shared.mail;

public final class EmailUiTemplate {

    private EmailUiTemplate() {
    }

    public static String wrapInCommonLayout(
            String title,
            String badge,
            String headline,
            String subtitle,
            String rawBodyHtml,
            String footerNote
    ) {
        String bodyHtml = extractBodyContent(rawBodyHtml);

        return String.format("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width,initial-scale=1.0">
                  <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background:#eef2ff;font-family:Arial,Helvetica,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#eef2ff;padding:24px 10px;">
                    <tr>
                      <td align="center">
                        <table width="700" cellpadding="0" cellspacing="0" style="width:100%%;max-width:700px;background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;overflow:hidden;box-shadow:0 6px 24px rgba(15,23,42,.08);">
                          <tr>
                            <td style="background:linear-gradient(135deg,#0f172a,#1d4ed8);padding:18px 24px;">
                              <table width="100%%" cellpadding="0" cellspacing="0">
                                <tr>
                                  <td>
                                    <div style="color:#bfdbfe;font-size:11px;font-weight:700;letter-spacing:.07em;text-transform:uppercase;">Dissanayake Super</div>
                                    <div style="color:#ffffff;font-size:20px;font-weight:800;letter-spacing:-.02em;margin-top:4px;">%s</div>
                                    <div style="color:#dbeafe;font-size:12px;margin-top:6px;line-height:1.4;">%s</div>
                                  </td>
                                  <td align="right" style="vertical-align:top;">
                                    <span style="display:inline-block;background:#ffffff;color:#1e3a8a;font-size:11px;font-weight:800;letter-spacing:.04em;text-transform:uppercase;padding:6px 10px;border-radius:999px;">%s</span>
                                  </td>
                                </tr>
                              </table>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:20px 20px 8px;">
                              %s
                            </td>
                          </tr>
                          <tr>
                            <td style="background:#f8fafc;border-top:1px solid #e2e8f0;padding:10px 20px;text-align:center;">
                              <p style="margin:0;font-size:11px;color:#64748b;line-height:1.5;">%s</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """,
                escapeHtml(defaultString(title, "Notification")),
                escapeHtml(defaultString(headline, "Notification")),
                escapeHtml(defaultString(subtitle, "")),
                escapeHtml(defaultString(badge, "Email")),
                bodyHtml,
                escapeHtml(defaultString(footerNote, "This is an automated message."))
        );
    }

    public static String plainTextToHtml(String plainText) {
        String escaped = escapeHtml(defaultString(plainText, ""));
        return escaped
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace("\n", "<br/>");
    }

    public static String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String extractBodyContent(String rawHtml) {
        if (rawHtml == null || rawHtml.isBlank()) {
            return "";
        }

        String lower = rawHtml.toLowerCase();
        int bodyStart = lower.indexOf("<body");
        if (bodyStart < 0) {
            return rawHtml;
        }

        int bodyStartEnd = lower.indexOf('>', bodyStart);
        int bodyEnd = lower.indexOf("</body>", bodyStartEnd + 1);
        if (bodyStartEnd < 0 || bodyEnd < 0 || bodyEnd <= bodyStartEnd) {
            return rawHtml;
        }

        return rawHtml.substring(bodyStartEnd + 1, bodyEnd).trim();
    }

    private static String defaultString(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
