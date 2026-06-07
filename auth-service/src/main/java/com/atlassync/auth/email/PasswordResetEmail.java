package com.atlassync.auth.email;

import java.time.Duration;

/**
 * Password-reset code mail. Tone is calmer than verification — someone forgot
 * their password, we're not celebrating anything. Includes a "wasn't you?"
 * safety line so the recipient knows what to do if this was unsolicited.
 */
final class PasswordResetEmail {

    private PasswordResetEmail() {}

    static EmailTemplate render(String name, String code, Duration ttl) {
        String safeName = EmailTheme.escape(displayName(name));
        long minutes = Math.max(1, ttl.toMinutes());

        String inner = EmailTheme.eyebrow("Reset your password")
                + EmailTheme.hero("Forgot " + EmailTheme.accent("again?") + ".")
                + EmailTheme.paragraph("Hi " + safeName + ", we got a request to reset your AtlasSync "
                        + "password. Enter the code below in the app to choose a new one:")
                + EmailTheme.codeChip(code)
                + EmailTheme.fineprint("Expires in " + minutes + " minutes. "
                        + "If you didn't request this, ignore this email — your password stays the same, "
                        + "and we'll log any new device out of your account just in case.");

        return new EmailTemplate(
                "Reset your AtlasSync password",
                renderText(displayName(name), code, minutes),
                EmailTheme.shell(inner)
        );
    }

    private static String renderText(String name, String code, long minutes) {
        return """
                Hi %s,

                We got a request to reset your AtlasSync password. Enter this
                code in the app to choose a new one:

                    %s

                It expires in %d minutes. If you didn't ask for this, ignore
                this email — your password stays as it was.

                — AtlasSync
                """.formatted(name, code, minutes);
    }

    private static String displayName(String raw) {
        return raw == null || raw.isBlank() ? "there" : raw.trim();
    }
}
