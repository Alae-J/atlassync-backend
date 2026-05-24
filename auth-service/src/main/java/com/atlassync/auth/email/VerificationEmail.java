package com.atlassync.auth.email;

import java.time.Duration;

/**
 * Re-sent verification code. Same code-chip motif as the welcome email but stripped
 * down — no welcome copy, just the code, expiry, and a quick safety line.
 */
final class VerificationEmail {

    private VerificationEmail() {}

    static EmailTemplate render(String name, String code, Duration ttl) {
        String safeName = EmailTheme.escape(displayName(name));
        long minutes = Math.max(1, ttl.toMinutes());

        String inner = EmailTheme.eyebrow("Verify your email")
                + EmailTheme.hero("One " + EmailTheme.accent("more step") + ".")
                + EmailTheme.paragraph("Hi " + safeName + ", here's the code you asked for. Enter it "
                        + "on the verification screen in the app:")
                + EmailTheme.codeChip(code)
                + EmailTheme.fineprint("Expires in " + minutes + " minutes. "
                        + "If this wasn't you, you can ignore this email — your account stays as it was.");

        return new EmailTemplate(
                "Your AtlasSync verification code",
                renderText(displayName(name), code, minutes),
                EmailTheme.shell(inner)
        );
    }

    private static String renderText(String name, String code, long minutes) {
        return """
                Hi %s,

                Here's your AtlasSync verification code:

                    %s

                It expires in %d minutes. If you didn't ask for this, you can
                safely ignore this email.

                — AtlasSync
                """.formatted(name, code, minutes);
    }

    private static String displayName(String raw) {
        return raw == null || raw.isBlank() ? "there" : raw.trim();
    }
}
