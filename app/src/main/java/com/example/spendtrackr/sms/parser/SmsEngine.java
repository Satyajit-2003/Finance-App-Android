package com.example.spendtrackr.sms.parser;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Main entry point for SMS parsing.
 * Call {@link #getTransactionInfo(String)} with the raw SMS body to get a
 * fully-parsed {@link SmsModels.TransactionInfo}.
 *
 * Ports engine.py from the backend Python sms_parser module.
 */
public class SmsEngine {

    private static final Pattern CREDIT_PATTERN = Pattern.compile(
            "(?:credited|credit|deposited|added|received|refund|repayment)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern DEBIT_PATTERN = Pattern.compile(
            "(?:debited|debit|deducted)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern MISC_PATTERN = Pattern.compile(
            "(?:payment|spent|paid|used\\s+at|charged|transaction\\son|transaction\\sfee|"
                    + "tran|booked|purchased|sent\\s+to|purchase\\s+of|spent\\s+on)",
            Pattern.CASE_INSENSITIVE);

    /**
     * Extract the transaction amount from a processed message token list.
     * Ports get_transaction_amount from Python.
     */
    public static String getTransactionAmount(List<String> processedMessage) {
        int index = processedMessage.indexOf("rs.");
        if (index == -1) return "";

        if (index + 1 < processedMessage.size()) {
            String money = processedMessage.get(index + 1).replace(",", "");
            try {
                Double.parseDouble(money);
                return SmsUtils.padCurrencyValue(money);
            } catch (NumberFormatException e) {
                // look one step further
                if (index + 2 < processedMessage.size()) {
                    money = processedMessage.get(index + 2).replace(",", "");
                    try {
                        Double.parseDouble(money);
                        return SmsUtils.padCurrencyValue(money);
                    } catch (NumberFormatException ex) {
                        return "";
                    }
                }
            }
        }
        return "";
    }

    /**
     * Determine the transaction type ("debit", "credit", or null) from a message.
     * Ports get_transaction_type from Python.
     */
    public static String getTransactionType(List<String> processedMessage) {
        String messageStr = join(" ", processedMessage);
        if (DEBIT_PATTERN.matcher(messageStr).find()) return "debit";
        if (MISC_PATTERN.matcher(messageStr).find()) return "debit";
        if (CREDIT_PATTERN.matcher(messageStr).find()) return "credit";
        return null;
    }

    /**
     * Parse a raw banking SMS into a {@link SmsModels.TransactionInfo} containing
     * account, balance, and transaction details.
     * Ports get_transaction_info from Python.
     *
     * @param message raw SMS body string
     */
    public static SmsModels.TransactionInfo getTransactionInfo(String message) {
        if (message == null || message.trim().isEmpty()) {
            return new SmsModels.TransactionInfo(
                    new SmsModels.AccountInfo(),
                    null,
                    new SmsModels.Transaction()
            );
        }

        List<String> processedMessage = SmsUtils.processMessage(message);

        // Account
        SmsModels.AccountInfo account = AccountParser.getAccount(processedMessage);

        // Available balance
        String availableBalance = BalanceParser.getBalance(
                processedMessage, SmsModels.BalanceKeywordType.AVAILABLE);

        // Transaction amount
        String transactionAmount = getTransactionAmount(processedMessage);

        // Need at least 2 of {balance, amount, accountNumber} to infer type
        int validCount = 0;
        if (availableBalance != null && !availableBalance.isEmpty()) validCount++;
        if (transactionAmount != null && !transactionAmount.isEmpty()) validCount++;
        if (account.number != null && !account.number.isEmpty()) validCount++;
        boolean isValid = validCount >= 2;

        String transactionType = isValid ? getTransactionType(processedMessage) : null;

        // Balance object
        SmsModels.Balance balance = new SmsModels.Balance(availableBalance, null);

        // For cards, also extract outstanding balance
        if (account.type == SmsModels.AccountType.CARD) {
            String outstandingBalance = BalanceParser.getBalance(
                    processedMessage, SmsModels.BalanceKeywordType.OUTSTANDING);
            balance.outstanding = outstandingBalance;
        }

        // Merchant / reference number
        Map<String, String> merchantInfo = MerchantParser.extractMerchantInfo(message);

        return new SmsModels.TransactionInfo(
                account,
                balance,
                new SmsModels.Transaction(
                        transactionType,
                        transactionAmount,
                        merchantInfo.get("referenceNo"),
                        merchantInfo.get("merchant")
                )
        );
    }

    // -------------------------------------------------------------------------

    private static String join(String delimiter, List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(delimiter);
            sb.append(list.get(i));
        }
        return sb.toString();
    }
}
