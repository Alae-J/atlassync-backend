package com.atlassync.auth.email;

/**
 * Pre-rendered email body in both formats. Brevo wants both: text fallback for
 * mail clients that disable HTML, plus the styled HTML body.
 */
public record EmailTemplate(String subject, String text, String html) {}
