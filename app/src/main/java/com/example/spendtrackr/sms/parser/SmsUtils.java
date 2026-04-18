package com.example.spendtrackr.sms.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class SmsUtils {

    public static boolean isNumber(String val) {
        if (val == null || val.isEmpty()) return false;
        try {
            Double.parseDouble(val);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Remove non-numeric characters from the beginning and end of the string.
     * Ports trim_leading_and_trailing_chars from Python.
     */
    public static String trimLeadingAndTrailingChars(String string) {
        if (string == null || string.isEmpty()) return "";
        char first = string.charAt(0);
        char last = string.charAt(string.length() - 1);
        String finalStr = !isNumber(String.valueOf(last)) ? string.substring(0, string.length() - 1) : string;
        if (!finalStr.isEmpty() && !isNumber(String.valueOf(first))) {
            finalStr = finalStr.substring(1);
        }
        return finalStr;
    }

    /**
     * Extract account number from a string containing 'ac'.
     */
    public static String extractBondedAccountNo(String accountNo) {
        String stripped = accountNo.replace("ac", "");
        return isNumber(stripped) ? stripped : "";
    }

    /**
     * Process the raw SMS message into a list of tokens, normalising currency
     * markers, account keywords, and combined words.
     * Ports process_message from Python.
     */
    public static List<String> processMessage(String message) {
        String s = message.toLowerCase();
        s = s.replace("!", "");
        s = s.replace(":", " ");
        s = s.replace("/", "");
        s = s.replace("=", " ");
        s = s.replaceAll("[{}]", " ");
        s = s.replace("\n", " ");
        s = s.replace("\r", " ");
        s = s.replace("ending ", "");
        // replace 'x' and '*' (common masking chars in bank SMS)
        s = s.replaceAll("x|\\*", "");
        s = s.replace("is ", "");
        s = s.replace("with ", "");
        s = s.replace("no. ", "");
        // normalise account-related words to "ac"
        s = s.replaceAll("\\bac\\b|\\bacct\\b|\\baccount\\b", "ac");
        // normalise "rs" variants to "rs. "
        s = s.replaceAll("rs(?=\\w)", "rs. ");
        s = s.replace("rs ", "rs. ");
        s = s.replaceAll("inr(?=\\w)", "rs. ");
        s = s.replace("inr ", "rs. ");
        s = s.replace("rs. ", "rs.");
        // NOTE: the dot below is a regex wildcard (matches any char), mirroring
        // the original Python which uses r"rs.(?=\w)" (not r"rs\.(?=\w)").
        s = s.replaceAll("rs.(?=\\w)", "rs. ");
        s = s.replace("debited", " debited ");
        s = s.replace("credited", " credited ");

        // replace combined multi-word phrases with single tokens
        for (SmsModels.CombinedWord word : SmsConstants.COMBINED_WORDS) {
            s = word.regex.matcher(s).replaceAll(word.word);
        }

        List<String> result = new ArrayList<>();
        for (String part : s.split(" ")) {
            if (!part.isEmpty()) result.add(part);
        }
        return result;
    }

    /**
     * Ensure currency values have two decimal places.
     */
    public static String padCurrencyValue(String val) {
        if (val == null || val.isEmpty()) return "";
        String[] parts = val.split("\\.");
        if (parts.length == 1) return parts[0] + ".00";
        String lhs = parts[0];
        String rhs = parts[1];
        while (rhs.length() < 2) rhs += "0";
        return lhs + "." + rhs;
    }

    /**
     * Get the next {@code count} words appearing after {@code searchWord} in
     * {@code source}.
     */
    /**
     * Accept a raw String or a pre-processed List&lt;String&gt; and always return a
     * List&lt;String&gt; token list. Ports get_processed_message from Python.
     */
    @SuppressWarnings("unchecked")
    public static List<String> getProcessedMessage(Object message) {
        if (message instanceof String) {
            return processMessage((String) message);
        }
        return (List<String>) message;
    }

    public static String getNextWords(String source, String searchWord, int count) {
        String[] splits = source.split(Pattern.quote(searchWord), 2);
        if (splits.length < 2) return "";
        String nextGroup = splits[1];
        if (nextGroup != null && !nextGroup.isEmpty()) {
            String[] words = nextGroup.trim().split("[^0-9a-zA-Z]+");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(count, words.length); i++) {
                if (i > 0) sb.append(" ");
                sb.append(words[i]);
            }
            return sb.toString();
        }
        return "";
    }
}
