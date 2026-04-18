package com.example.spendtrackr.sms.parser;

import java.util.Arrays;
import java.util.List;

/**
 * Validation rules for parsed SMS transactions.
 * Ports ValidationRules from Finance-backend-API/config.py.
 */
public class ValidationRules {

    public static final List<String> INVALID_TRANSACTION_KEYWORDS = Arrays.asList(
            "failed", "declined", "otp", "secret"
    );
    public static int MAX_SMS_LENGTH = 1000;
    public static int MIN_SMS_LENGTH = 20;

    // -------------------------------------------------------------------------

    /** Carries the validation outcome and a human-readable reason. */
    public static class ValidationResult {
        public final boolean valid;
        public final String reason;

        private ValidationResult(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }

        public static ValidationResult ok() {
            return new ValidationResult(true, "");
        }

        public static ValidationResult fail(String reason) {
            return new ValidationResult(false, reason);
        }

        @Override
        public String toString() {
            return valid ? "VALID" : "INVALID: " + reason;
        }
    }

    // -------------------------------------------------------------------------

    /**
     * Validate a parsed SMS transaction and return a {@link ValidationResult}
     * with a human-readable reason when invalid.
     */
    public static ValidationResult validate(SmsModels.TransactionInfo info, String transactionText) {
        if (info == null) return ValidationResult.fail("No Transaction Info");

        if (transactionText != null && transactionText.length() > MAX_SMS_LENGTH)
            return ValidationResult.fail("SMS too long (" + transactionText.length() + " > " + MAX_SMS_LENGTH + ")");
        if (transactionText != null && transactionText.length() < MIN_SMS_LENGTH)
            return ValidationResult.fail("SMS too short (" + transactionText.length() + " < " + MIN_SMS_LENGTH + ")");

        if ("credit".equals(info.transaction.type))
            return ValidationResult.fail("Credit Transaction");

        if (transactionText != null) {
            String lower = transactionText.toLowerCase();
            for (String keyword : INVALID_TRANSACTION_KEYWORDS) {
                if (lower.contains(keyword))
                    return ValidationResult.fail("Contains Keyword '" + keyword.toUpperCase() + "'");
            }
        }


        if (info.transaction == null || info.transaction.amount == null || info.transaction.amount.isEmpty())
            return ValidationResult.fail("Missing Transaction Amount");
        if (info.account == null || info.account.type == null)
            return ValidationResult.fail("Missing Account Type");
        if (info.account.number == null || info.account.number.isEmpty())
            return ValidationResult.fail("Missing Account Number");



        return ValidationResult.ok();
    }

    /**
     * Convenience wrapper — returns plain boolean, identical to the Python backend.
     */
    public static boolean isValidTransaction(SmsModels.TransactionInfo info, String transactionText) {
        return validate(info, transactionText).valid;
    }
}
