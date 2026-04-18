package com.example.spendtrackr.sms.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MerchantParser {

    /**
     * Extract merchant information and reference number from the SMS message.
     * Ports extract_merchant_info from Python.
     *
     * @param message raw or pre-processed message (String or List&lt;String&gt;)
     */
    public static Map<String, String> extractMerchantInfo(Object message) {
        List<String> processedMessage = SmsUtils.getProcessedMessage(message);
        String messageString = join(" ", processedMessage);

        Map<String, String> transactionDetails = new HashMap<>();
        transactionDetails.put("merchant", null);
        transactionDetails.put("referenceNo", null);

        // Check for VPA (Virtual Payment Address)
        if (processedMessage.contains("vpa")) {
            int idx = processedMessage.indexOf("vpa");
            if (idx < processedMessage.size() - 1) {
                String nextStr = processedMessage.get(idx + 1);
                String name = nextStr.replace("(", " ").replace(")", " ").split(" ")[0];
                transactionDetails.put("merchant", name);
            }
        }

        // Check for UPI keywords
        String match = "";
        for (String keyword : SmsConstants.UPI_KEYWORDS) {
            if (messageString.contains(keyword)) {
                match = keyword;
                break;
            }
        }

        if (!match.isEmpty()) {
            String nextWord = SmsUtils.getNextWords(messageString, match, 1);
            if (SmsUtils.isNumber(nextWord)) {
                transactionDetails.put("referenceNo", nextWord);
            } else if (transactionDetails.get("merchant") != null) {
                // try to extract numeric part as reference number
                String[] numericParts = nextWord.split("[^0-9]");
                String longestNumeric = "";
                for (String part : numericParts) {
                    if (part.length() > longestNumeric.length()) longestNumeric = part;
                }
                if (!longestNumeric.isEmpty()) {
                    transactionDetails.put("referenceNo", longestNumeric);
                }
            } else {
                transactionDetails.put("merchant", nextWord);
            }

            // If merchant still not found, look for UPI handles
            if (transactionDetails.get("merchant") == null) {
                String upiHandleAlternation = buildUpiHandleAlternation();
                Pattern upiRegex = Pattern.compile(
                        "[a-zA-Z0-9_-]+(" + upiHandleAlternation + ")",
                        Pattern.CASE_INSENSITIVE);
                Matcher upiMatcher = upiRegex.matcher(messageString);
                if (upiMatcher.find()) {
                    transactionDetails.put("merchant", upiMatcher.group(0));
                }
            }
        }

        // "at * on" pattern
        if (transactionDetails.get("merchant") == null) {
            Pattern atOnRegex = Pattern.compile("at\\s+(.+?)\\s+on\\s+", Pattern.CASE_INSENSITIVE);
            Matcher atOnMatcher = atOnRegex.matcher(messageString);
            if (atOnMatcher.find()) {
                transactionDetails.put("merchant", atOnMatcher.group(1).trim());
            }
        }

        // "at *" pattern
        if (transactionDetails.get("merchant") == null) {
            Pattern atRegex = Pattern.compile("at\\s+(.+?)", Pattern.CASE_INSENSITIVE);
            Matcher atMatcher = atRegex.matcher(messageString);
            if (atMatcher.find()) {
                transactionDetails.put("merchant", atMatcher.group(1).trim());
            }
        }

        // "on *" pattern
        if (transactionDetails.get("merchant") == null) {
            Pattern onRegex = Pattern.compile("on\\s+(.+?)\\s", Pattern.CASE_INSENSITIVE);
            Matcher onMatcher = onRegex.matcher(messageString);
            if (onMatcher.find()) {
                transactionDetails.put("merchant", onMatcher.group(1).trim());
            }
        }

        return transactionDetails;
    }

    // -------------------------------------------------------------------------

    private static String buildUpiHandleAlternation() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < SmsConstants.UPI_HANDLES.size(); i++) {
            if (i > 0) sb.append("|");
            // escape the '@' – not strictly necessary in Java regex but keeps it clear
            sb.append(Pattern.quote(SmsConstants.UPI_HANDLES.get(i)));
        }
        return sb.toString();
    }

    private static String join(String delimiter, List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(delimiter);
            sb.append(list.get(i));
        }
        return sb.toString();
    }
}
