-- OTP challenges now accept either a phone number or an email address as the recipient.
-- The column gets a generic name and a wider length so it fits standard email max (255).

ALTER TABLE otp_challenges RENAME COLUMN phone TO recipient;
ALTER TABLE otp_challenges ALTER COLUMN recipient TYPE VARCHAR(255);

DROP INDEX IF EXISTS idx_otp_phone_status;
CREATE INDEX idx_otp_recipient_status ON otp_challenges(recipient, status);
