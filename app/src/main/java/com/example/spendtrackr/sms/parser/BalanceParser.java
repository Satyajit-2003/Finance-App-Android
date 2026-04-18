package com.example.spendtrackr.sms.parser;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BalanceParser {

    /**
     * Extract a balance amount from {@code message} starting at {@code index}.
     * Ports extract_balance from Python.
     */
    private static String extractBalance(int index, String message, int length) {
        StringBuilder balance = new StringBuilder();
        boolean sawNumber = false;
        int invalidCharCount = 0;
        int start = index;

        while (start < length) {
            char c = message.charAt(start);

            if (c >= '0' && c <= '9') {
                sawNumber = true;
                balance.append(c);
            } else if (sawNumber) {
                if (c == '.') {
                    if (invalidCharCount == 1) break;
                    balance.append(c);
                    invalidCharCount++;
                } else if (c != ',') {
                    break;
                }
            }
            start++;
        }
        return balance.toString();
    }

    /**
     * Find balance in messages with non-standard formats.
     * Ports find_non_standard_balance from Python.
     */
    private static String findNonStandardBalance(String messageString, SmsModels.BalanceKeywordType keywordType) {
        List<String> balanceKeywords = keywordType == SmsModels.BalanceKeywordType.AVAILABLE
                ? SmsConstants.AVAILABLE_BALANCE_KEYWORDS
                : SmsConstants.OUTSTANDING_BALANCE_KEYWORDS;

        String balKeywordRegex = "(" + join("|", balanceKeywords) + ")";
        String amountRegex = "([\\d]+\\.[\\d]+|[\\d]+)";

        // Case 1: "balance 100.00"
        Pattern p1 = Pattern.compile(balKeywordRegex + "\\s*" + amountRegex, Pattern.CASE_INSENSITIVE);
        Matcher m1 = p1.matcher(messageString);
        if (m1.find()) {
            String balance = m1.group(2);
            if (balance != null && balance.replace(".", "").matches("\\d+")) {
                return balance;
            }
        }

        // Case 2: "100.00 available"
        Pattern p2 = Pattern.compile(amountRegex + "\\s*" + balKeywordRegex, Pattern.CASE_INSENSITIVE);
        Matcher m2 = p2.matcher(messageString);
        if (m2.find()) {
            String balance = m2.group(1);
            if (balance != null && balance.replace(".", "").matches("\\d+")) {
                return balance;
            }
        }

        return null;
    }

    /**
     * Extract balance information from the processed message tokens.
     * Ports get_balance from Python.
     */
    public static String getBalance(List<String> processedMessage, SmsModels.BalanceKeywordType keywordType) {
        String messageString = join(" ", processedMessage);
        int indexOfKeyword = -1;

        List<String> balanceKeywords = keywordType == SmsModels.BalanceKeywordType.AVAILABLE
                ? SmsConstants.AVAILABLE_BALANCE_KEYWORDS
                : SmsConstants.OUTSTANDING_BALANCE_KEYWORDS;

        // Find the first matching keyword
        for (String word : balanceKeywords) {
            int idx = messageString.indexOf(word);
            if (idx != -1) {
                indexOfKeyword = idx + word.length();
                break;
            }
        }

        // Find "rs." occurring after the keyword
        int index = indexOfKeyword;
        int indexOfRs = -1;

        if (index != -1) {
            while (index + 3 <= messageString.length()) {
                String nextThree = messageString.substring(index, index + 3);
                if (nextThree.equals("rs.")) {
                    indexOfRs = index + 2;
                    break;
                }
                index++;
            }
        }

        // No "rs." found – try non-standard format
        if (indexOfRs == -1) {
            String balance = findNonStandardBalance(messageString, keywordType);
            return (balance != null) ? SmsUtils.padCurrencyValue(balance) : null;
        }

        // Extract balance after "rs."
        String balance = extractBalance(indexOfRs, messageString, messageString.length());
        return (!balance.isEmpty()) ? SmsUtils.padCurrencyValue(balance) : null;
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
