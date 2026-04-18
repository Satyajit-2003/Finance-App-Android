package com.example.spendtrackr.sms.parser;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AccountParser {

    /**
     * Stub for the Python load_acc_info() which read CARD_INFO / ACC_INFO
     * environment variables mapping bank names to last-4-digit card numbers.
     * Returns an empty map until a storage mechanism is decided.
     *
     * TODO: populate via SharedPreferences or a config screen when ready.
     */
    private static Map<String, String> loadAccInfo(String type) {
        return Collections.emptyMap();
    }

    /**
     * Extract credit-card account information from the processed message tokens.
     * Ports get_card from Python.
     */
    static SmsModels.AccountInfo getCard(List<String> message) {
        String combinedCardName = "";
        SmsModels.AccountInfo card = new SmsModels.AccountInfo();
        int cardIndex = -1;

        for (int idx = 0; idx < message.size(); idx++) {
            String word = message.get(idx);
            if (word.equals("card")) {
                cardIndex = idx;
                break;
            }
            // check for combined words of CARD type
            for (SmsModels.CombinedWord cw : SmsConstants.COMBINED_WORDS) {
                if (cw.type == SmsModels.AccountType.CARD && cw.word.equals(word)) {
                    combinedCardName = cw.word;
                    cardIndex = idx;
                    break;
                }
            }
            if (cardIndex != -1) break;
        }

        if (cardIndex != -1 && cardIndex + 1 < message.size()) {
            card.number = message.get(cardIndex + 1);
            card.type = SmsModels.AccountType.CARD;

            if (card.number != null && !isDigit(card.number)) {
                // try to look up from acc info mapping (currently always empty)
                for (Map.Entry<String, String> entry : loadAccInfo("CARD").entrySet()) {
                    if (message.contains(entry.getKey())) {
                        card.number = entry.getValue();
                        break;
                    }
                }
            }

            // validate that the resolved number is an integer
            try {
                //noinspection ResultOfMethodCallIgnored
                Integer.parseInt(card.number);
            } catch (NumberFormatException e) {
                // false positive – return with just the combined card name if available
                return new SmsModels.AccountInfo(
                        !combinedCardName.isEmpty() ? card.type : null,
                        null,
                        !combinedCardName.isEmpty() ? combinedCardName : null
                );
            }
            return card;
        }

        return new SmsModels.AccountInfo();
    }

    /**
     * Extract account information (account, card, or wallet) from the processed
     * message tokens.
     * Ports get_account from Python.
     */
    public static SmsModels.AccountInfo getAccount(List<String> processedMessage) {
        int accountIndex = -1;
        SmsModels.AccountInfo account = new SmsModels.AccountInfo();

        // First: look for explicit "ac" references
        for (int idx = 0; idx < processedMessage.size(); idx++) {
            String word = processedMessage.get(idx);
            if (word.equals("ac")) {
                if (idx + 1 < processedMessage.size()) {
                    String accountNo = SmsUtils.trimLeadingAndTrailingChars(processedMessage.get(idx + 1));
                    try {
                        //noinspection ResultOfMethodCallIgnored
                        Integer.parseInt(accountNo);
                        accountIndex = idx;
                        account.type = SmsModels.AccountType.ACCOUNT;
                        account.number = accountNo;
                        break;
                    } catch (NumberFormatException e) {
                        // continue searching
                    }
                }
            } else if (word.contains("ac")) {
                String extracted = SmsUtils.extractBondedAccountNo(word);
                if (!extracted.isEmpty()) {
                    accountIndex = idx;
                    account.type = SmsModels.AccountType.ACCOUNT;
                    account.number = extracted;
                    break;
                }
            }
        }

        // No "ac" found – check for card
        if (accountIndex == -1) {
            account = getCard(processedMessage);
        }

        // Check for wallets
        if (account.type == null) {
            for (String word : processedMessage) {
                if (SmsConstants.WALLETS.contains(word)) {
                    account.type = SmsModels.AccountType.WALLET;
                    account.name = word;
                    break;
                }
            }
        }

        // Check for special ACCOUNT-type combined words (e.g. niyo)
        if (account.type == null) {
            for (SmsModels.CombinedWord cw : SmsConstants.COMBINED_WORDS) {
                if (cw.type == SmsModels.AccountType.ACCOUNT && processedMessage.contains(cw.word)) {
                    account.type = cw.type;
                    account.name = cw.word;
                    break;
                }
            }
        }

        // Trim to last 4 digits if account number is longer
        if (account.number != null && account.number.length() > 4) {
            account.number = account.number.substring(account.number.length() - 4);
        }

        return account;
    }

    // -------------------------------------------------------------------------

    private static boolean isDigit(String s) {
        if (s == null || s.isEmpty()) return false;
        for (char c : s.toCharArray()) {
            if (!Character.isDigit(c)) return false;
        }
        return true;
    }
}
