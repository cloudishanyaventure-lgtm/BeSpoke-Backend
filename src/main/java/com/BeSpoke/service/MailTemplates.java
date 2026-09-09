package com.BeSpoke.service;

/**
 * The website's "Warm Atelier" system rendered as email: ivory ground, white card under a
 * terracotta hairline, Fraunces-style serif headline with the brand's signature italic
 * terracotta emphasis, and the dotted eyebrow from the marketing pages.
 *
 * <p>Email clients are a decade behind browsers, so the base layer is deliberately old:
 * tables, inline styles, no flexbox, no webfonts, 600px fixed width, {@code bgcolor}
 * attributes for Outlook's Word engine and a VML button so its pill stays round. The
 * {@code <style>} block on top is progressive enhancement only — mobile sizing and dark
 * mode. Strip it and every mail still reads correctly.
 */
final class MailTemplates {

    private MailTemplates() {
    }

    static final String INK = "#1C1917";
    static final String INK_SOFT = "#44403C";
    static final String INK_MUTED = "#78716C";
    static final String CANVAS = "#F7F4EF";
    static final String PARCHMENT = "#FAF9F6";
    static final String LINE = "#E7E2D9";
    static final String TERRA = "#B4552D";
    static final String TERRA_DEEP = "#9A4525";
    static final String TERRA_SOFT = "#F6E8E0";
    static final String NIGHT = "#211D1A";

    private static final String SERIF = "Fraunces,'Playfair Display',Georgia,'Times New Roman',serif";
    private static final String SANS = "Inter,-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif";

    /** Anything that came from a person — a name, a studio, a subject line — goes through this. */
    static String esc(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /** Headline, plain. */
    static String h(String text) {
        return esc(text);
    }

    /**
     * Headline with the brand's emphasis: the second half turns italic terracotta, the same
     * move the marketing pages make with {@code <em>}. "Let's find your designer, <em>Poonam.</em>"
     */
    static String h(String text, String emphasis) {
        return esc(text) + " <em style=\"font-style:italic;color:" + TERRA + ";\">"
                + esc(emphasis) + "</em>";
    }

    /**
     * The full document. {@code preheader} is the grey line inboxes print next to the
     * subject; left empty they scrape the first words of markup instead, which looks careless.
     */
    static String page(String preheader, String eyebrow, String headingHtml, String content) {
        return """
                <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
                  "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
                <html xmlns="http://www.w3.org/1999/xhtml" lang="en">
                <head>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width,initial-scale=1" />
                <meta http-equiv="X-UA-Compatible" content="IE=edge" />
                <meta name="color-scheme" content="light dark" />
                <meta name="supported-color-schemes" content="light dark" />
                <title>BeSpoke</title>
                <!--[if mso]><xml><o:OfficeDocumentSettings>
                <o:PixelsPerInch>96</o:PixelsPerInch></o:OfficeDocumentSettings></xml><![endif]-->
                <style>
                  a { text-decoration: none; }
                  .card { box-shadow: 0 1px 2px rgba(28,25,23,.04), 0 8px 24px rgba(28,25,23,.05); }
                  @media only screen and (max-width:480px) {
                    .pad { padding: 28px 24px 26px !important; }
                    .hero { font-size: 24px !important; }
                    .shell { padding: 20px 8px !important; }
                  }
                  /* Every selector here is a named class. A blanket rule like `.shell td`
                     also repaints the button, the eyebrow dot and the accent rule — they are
                     table cells too — and the mail loses its accent colour entirely. */
                  @media (prefers-color-scheme: dark) {
                    body, .dm-bg { background-color: #14110F !important; }
                    .dm-card { background-color: #1C1917 !important; border-color: #322C28 !important; }
                    .dm-h { color: #F7F4EF !important; }
                    .dm-p { color: #D6D3D1 !important; }
                    .dm-muted, .dm-muted a { color: #A8A29E !important; }
                    .dm-panel { background-color: #241F1C !important; border-color: #362F2A !important; }
                    .dm-label { color: #A8A29E !important; }
                    .dm-value { color: #F7F4EF !important; }
                    .dm-code { background-color: #2B211B !important; border-color: #3D2E25 !important; }
                    .dm-code-v { color: #FBEDE4 !important; }
                    .dm-code-n { color: #E0A882 !important; }
                  }
                </style>
                </head>
                <body style="margin:0;padding:0;background-color:%1$s;-webkit-font-smoothing:antialiased;">
                <div style="display:none;max-height:0;overflow:hidden;opacity:0;mso-hide:all;">%2$s</div>
                <div style="display:none;max-height:0;overflow:hidden;mso-hide:all;">
                  &#847;&zwnj;&nbsp;&#847;&zwnj;&nbsp;&#847;&zwnj;&nbsp;&#847;&zwnj;&nbsp;</div>
                <table role="presentation" class="shell dm-bg" width="100%%" cellpadding="0" cellspacing="0"
                       border="0" bgcolor="%1$s" style="background-color:%1$s;padding:36px 12px 44px;">
                  <tr><td align="center">
                    <table role="presentation" width="600" cellpadding="0" cellspacing="0" border="0"
                           style="width:100%%;max-width:600px;">

                      <tr><td align="center" style="padding:4px 0 26px;">
                        <table role="presentation" cellpadding="0" cellspacing="0" border="0">
                          <tr>
                            <td width="9" valign="middle" style="font-size:0;line-height:0;">
                              <div style="width:9px;height:9px;background-color:%3$s;border-radius:50%%;
                                   font-size:0;line-height:9px;mso-line-height-rule:exactly;">&nbsp;</div></td>
                            <td class="dm-h" style="padding-left:9px;font-family:%4$s;font-size:23px;font-weight:600;
                                       letter-spacing:-0.01em;color:%5$s;">BeSpoke</td>
                          </tr>
                        </table>
                      </td></tr>

                      <tr><td bgcolor="%3$s" style="background-color:%3$s;font-size:0;line-height:0;
                               height:3px;border-radius:16px 16px 0 0;">&nbsp;</td></tr>
                      <tr><td class="card pad dm-card" bgcolor="#FFFFFF" style="background-color:#FFFFFF;
                               border:1px solid %6$s;border-top:none;border-radius:0 0 16px 16px;
                               padding:38px 40px 34px;">
                        %7$s
                        <h1 class="hero dm-h" style="margin:0 0 18px;font-family:%4$s;font-size:28px;
                                   line-height:1.18;font-weight:500;letter-spacing:-0.015em;color:%5$s;">%8$s</h1>
                        %9$s
                      </td></tr>

                      <tr><td class="dm-muted" align="center" style="padding:28px 16px 0;font-family:%10$s;
                               font-size:12px;line-height:1.8;color:%11$s;">
                        <a href="https://bespokedesign.in" style="color:%11$s;text-decoration:none;">Website</a>
                        &nbsp;·&nbsp;
                        <a href="https://bespokedesign.in/how-it-works" style="color:%11$s;text-decoration:none;">How it works</a>
                        &nbsp;·&nbsp;
                        <a href="mailto:contact@bespokedesign.in" style="color:%11$s;text-decoration:none;">Contact us</a>
                        <br />
                        <span style="color:%11$s;">Questions? Write to
                          <a href="mailto:contact@bespokedesign.in" style="color:%3$s;font-weight:600;text-decoration:none;"
                          >contact@bespokedesign.in</a> — a real person reads it.</span>
                        <br />
                        <span style="color:%11$s;">© BeSpoke · Ishanya Innovation Venture Pvt Ltd
                          · Made with care in India.</span>
                      </td></tr>

                    </table>
                  </td></tr>
                </table>
                </body>
                </html>
                """.formatted(CANVAS, esc(preheader), TERRA, SERIF, INK, LINE,
                eyebrow == null || eyebrow.isBlank() ? "" : eyebrow(eyebrow),
                headingHtml, content, SANS, INK_MUTED);
    }

    /** The marketing pages' dotted label, rebuilt with a table cell so Outlook keeps the dot. */
    private static String eyebrow(String text) {
        return """
                <table role="presentation" cellpadding="0" cellspacing="0" border="0" style="margin:0 0 15px;">
                  <tr>
                    <td width="6" valign="middle" style="font-size:0;line-height:0;">
                      <div style="width:6px;height:6px;background-color:%1$s;border-radius:50%%;
                           font-size:0;line-height:6px;mso-line-height-rule:exactly;">&nbsp;</div></td>
                    <td style="padding-left:9px;font-family:%2$s;font-size:11px;font-weight:600;
                        letter-spacing:0.18em;text-transform:uppercase;color:%3$s;">%4$s</td>
                  </tr>
                </table>
                """.formatted(TERRA, SANS, INK_MUTED, esc(text));
    }

    /** Body copy. Callers pass HTML, so anything person-supplied must be escaped first. */
    static String p(String html) {
        return """
                <p class="dm-p" style="margin:0 0 16px;font-family:%s;font-size:15.5px;
                          line-height:1.72;color:%s;">%s</p>
                """.formatted(SANS, INK_SOFT, html);
    }

    /**
     * The one thing we want them to do. VML underneath so Outlook draws a real pill instead
     * of a rectangle; every other client uses the anchor.
     */
    static String button(String href, String label) {
        return """
                <table role="presentation" cellpadding="0" cellspacing="0" border="0" style="margin:26px 0 6px;">
                  <tr><td>
                    <!--[if mso]>
                    <v:roundrect xmlns:v="urn:schemas-microsoft-com:vml" xmlns:w="urn:schemas-microsoft-com:office:word"
                      href="%1$s" style="height:48px;v-text-anchor:middle;width:260px;" arcsize="50%%"
                      stroke="f" fillcolor="%2$s">
                      <w:anchorlock/>
                      <center style="color:#ffffff;font-family:Helvetica,Arial,sans-serif;font-size:14px;
                        font-weight:bold;">%3$s</center>
                    </v:roundrect>
                    <![endif]-->
                    <!--[if !mso]><!-- -->
                    <table role="presentation" cellpadding="0" cellspacing="0" border="0">
                      <tr><td bgcolor="%2$s" style="background-color:%2$s;border-radius:999px;">
                        <a href="%1$s" style="display:inline-block;padding:15px 32px;font-family:%4$s;
                           font-size:14.5px;font-weight:600;letter-spacing:0.01em;color:#FFFFFF;
                           text-decoration:none;border-radius:999px;">%3$s</a>
                      </td></tr>
                    </table>
                    <!--<![endif]-->
                  </td></tr>
                </table>
                """.formatted(href, TERRA, esc(label), SANS);
    }

    /** A one-time code: big, spaced, readable off a phone and easy to copy by hand. */
    static String code(String value, String note) {
        return """
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0"
                       style="margin:26px 0 8px;">
                  <tr><td class="dm-code" align="center" bgcolor="%1$s" style="background-color:%1$s;
                          border:1px solid %2$s;border-radius:14px;padding:26px 16px 22px;">
                    <div class="dm-code-v" style="font-family:%3$s;font-size:38px;line-height:1.1;font-weight:600;
                                letter-spacing:0.2em;text-indent:0.2em;color:%4$s;">%5$s</div>
                    <div class="dm-code-n" style="margin-top:10px;font-family:%6$s;font-size:12px;
                                letter-spacing:0.08em;text-transform:uppercase;color:%7$s;">%8$s</div>
                  </td></tr>
                </table>
                """.formatted(TERRA_SOFT, "#EFDCD1", SERIF, NIGHT, esc(value), SANS, TERRA_DEEP, esc(note));
    }

    /** Label/value pairs on a parchment panel — lead details, credentials, an order total. */
    static String facts(String... labelValuePairs) {
        StringBuilder rows = new StringBuilder();
        for (int i = 0; i + 1 < labelValuePairs.length; i += 2) {
            String border = i == 0 ? "none" : "1px solid " + LINE;
            rows.append("""
                    <tr>
                      <td style="padding:11px 0 11px 20px;border-top:%1$s;font-family:%2$s;font-size:11px;
                                 font-weight:600;letter-spacing:0.12em;text-transform:uppercase;color:%3$s;
                                 width:42%%;vertical-align:top;" class="dm-label">%4$s</td>
                      <td class="dm-value" style="padding:11px 20px 11px 0;border-top:%1$s;font-family:%2$s;
                                 font-size:14.5px;font-weight:500;color:%5$s;">%6$s</td>
                    </tr>
                    """.formatted(border, SANS, INK_MUTED, esc(labelValuePairs[i]),
                    INK, esc(labelValuePairs[i + 1])));
        }
        return """
                <table role="presentation" class="dm-panel" width="100%%" cellpadding="0" cellspacing="0"
                       border="0" bgcolor="%1$s" style="background-color:%1$s;border:1px solid %2$s;
                       border-radius:14px;margin:24px 0 6px;">%3$s</table>
                """.formatted(PARCHMENT, LINE, rows);
    }

    /** Quieter closing note — expiry warnings, "ignore this if it wasn't you". */
    static String note(String text) {
        return """
                <p class="dm-muted" style="margin:24px 0 0;padding-top:20px;border-top:1px solid %s;
                          font-family:%s;font-size:13px;line-height:1.65;color:%s;">%s</p>
                """.formatted(LINE, SANS, INK_MUTED, esc(text));
    }

    static String link(String href, String label) {
        return "<a href=\"%s\" style=\"color:%s;font-weight:600;text-decoration:underline;\">%s</a>"
                .formatted(href, TERRA, esc(label));
    }
}
