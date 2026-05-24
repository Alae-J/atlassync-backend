package com.atlassync.auth.email;

import java.time.Duration;

/**
 * The first email a user gets after signing up. Bundles the verification CTA so we
 * don't double-spam — one polished welcome that doubles as the verification handoff.
 */
final class WelcomeEmail {

    private WelcomeEmail() {}

    static EmailTemplate render(String name, String code, Duration ttl) {
        String safeName = EmailTheme.escape(displayName(name));
        long minutes = Math.max(1, ttl.toMinutes());

        String inner = EmailTheme.eyebrow("Welcome aboard")
                + EmailTheme.hero("Hi " + EmailTheme.accent(safeName) + ".")
                + EmailTheme.paragraph(
                        "Your AtlasSync account is live. Walk into a partner store, scan what "
                      + "you grab, walk out — your phone is the checkout line.")
                + EmailTheme.paragraph(
                        "Confirm this email so we know it's really you. Pop the code below into "
                      + "the app on the verification screen:")
                + EmailTheme.codeChip(code)
                + EmailTheme.fineprint("Expires in " + minutes + " minutes. "
                        + "If you didn't sign up, ignore this email and your account will stay dormant.");

        return new EmailTemplate(
                "Welcome to AtlasSync — verify your email",
                renderText(displayName(name), code, minutes),
                EmailTheme.shell(inner)
        );
    }

    private static String renderText(String name, String code, long minutes) {
        return """
                Hi %s,

                Your AtlasSync account is live. Walk into a partner store, scan what
                you grab, walk out — your phone is the checkout line.

                Confirm this email so we know it's really you. Enter this code on
                the verification screen in the app:

                    %s

                The code expires in %d minutes. If you didn't sign up, ignore this
                email and your account will stay dormant.

                — AtlasSync
                """.formatted(name, code, minutes);
    }

    private static String displayName(String raw) {
        return raw == null || raw.isBlank() ? "there" : raw.trim();
    }
}
