package com.atlassync.auth.email;

/**
 * Shared visual constants for transactional emails. Pulled from the Phygital design
 * handoff so welcome / verification / future password-reset emails feel like the same
 * brand. Centralising here means tweaking the cream paper or accent terracotta in one
 * place flows everywhere.
 */
final class EmailTheme {

    private EmailTheme() {}

    static final String BG_PAPER     = "#f4ede0";
    static final String SURFACE_CARD = "#fffdf8";
    static final String INK_PRIMARY  = "#15140f";
    static final String INK_MUTED    = "#5a5448";
    static final String INK_FAINT    = "#7a7163";
    static final String ACCENT       = "#c87a3a";
    static final String RULE         = "#e8dec8";
    static final String CODE_BG      = "#15140f";
    static final String CODE_INK     = "#fbf6e8";

    static final String SANS_STACK   = "-apple-system,BlinkMacSystemFont,Segoe UI,Helvetica,Arial,sans-serif";
    static final String SERIF_STACK  = "'Instrument Serif',Georgia,'Times New Roman',serif";
    static final String MONO_STACK   = "'SF Mono','JetBrains Mono',Menlo,Consolas,monospace";

    /** Outer chrome shared by every email — full-width cream background + max-width card. */
    static String shell(String innerHtml) {
        return """
                <!doctype html>
                <html><head><meta charset="utf-8" /><meta name="viewport" content="width=device-width,initial-scale=1" />
                <title>AtlasSync</title></head>
                <body style="margin:0;padding:0;background:%s;font-family:%s;color:%s;\
                -webkit-font-smoothing:antialiased;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" \
                style="background:%s;padding:48px 16px;">
                <tr><td align="center">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" \
                style="max-width:520px;margin:0 auto;background:%s;border-radius:18px;\
                box-shadow:0 1px 0 rgba(21,20,15,0.04),0 12px 28px rgba(21,20,15,0.06);overflow:hidden;">
                <tr><td style="padding:36px 36px 32px;">%s</td></tr>
                <tr><td style="padding:0 36px 28px;">
                <hr style="border:none;border-top:1px solid %s;margin:0 0 18px;" />
                <p style="margin:0;font-size:11px;letter-spacing:0.6px;color:%s;text-transform:uppercase;">
                AtlasSync — phygital supermarket
                </p>
                </td></tr>
                </table>
                </td></tr>
                </table>
                </body></html>
                """.formatted(
                        BG_PAPER, SANS_STACK, INK_PRIMARY,
                        BG_PAPER,
                        SURFACE_CARD,
                        innerHtml,
                        RULE,
                        INK_FAINT
                );
    }

    /** Section eyebrow — small, uppercase, tracked. */
    static String eyebrow(String text) {
        return """
                <p style="margin:0 0 14px;font-size:11px;letter-spacing:1.6px;color:%s;\
                text-transform:uppercase;">%s</p>
                """.formatted(INK_FAINT, text);
    }

    /** Serif hero heading with optional accent inline. */
    static String hero(String html) {
        return """
                <h1 style="margin:0 0 18px;font-family:%s;font-weight:400;font-size:36px;\
                line-height:1.05;letter-spacing:-0.6px;color:%s;">%s</h1>
                """.formatted(SERIF_STACK, INK_PRIMARY, html);
    }

    static String paragraph(String text) {
        return """
                <p style="margin:0 0 14px;font-size:15px;line-height:1.6;color:%s;">%s</p>
                """.formatted(INK_MUTED, text);
    }

    /** Big tracked monospace code chip — the focal point of a verification email. */
    static String codeChip(String code) {
        return """
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0" \
                style="margin:18px 0 6px;">
                <tr><td align="center" style="background:%s;border-radius:14px;padding:22px 0;">
                <p style="margin:0;font-family:%s;font-size:30px;letter-spacing:8px;\
                color:%s;font-weight:500;">%s</p>
                </td></tr>
                </table>
                """.formatted(CODE_BG, MONO_STACK, CODE_INK, code);
    }

    /** Short note under the code chip — expiry + ignore line. */
    static String fineprint(String text) {
        return """
                <p style="margin:14px 0 0;font-size:12px;line-height:1.5;color:%s;">%s</p>
                """.formatted(INK_FAINT, text);
    }

    /** Minimal HTML escaping for user-supplied names. */
    static String escape(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /** Wraps a span with the accent terracotta colour. */
    static String accent(String text) {
        return "<em style=\"color:" + ACCENT + ";font-style:normal;\">" + text + "</em>";
    }
}
