package com.example.spendtrackr.sms.parser;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the SMS parser Java port.
 * Test messages mirror those in Finance-backend-API/run_tests.py :: test_sms_parser_direct().
 */
public class SmsParserTest {

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private SmsModels.TransactionInfo parse(String sms) {
        return SmsEngine.getTransactionInfo(sms);
    }

    // -------------------------------------------------------------------------
    // Test 1 – debit from account with available balance (INR prefix, A/c format)
    // -------------------------------------------------------------------------

    @Test
    public void test_debit_from_account_avlBal() {
        String sms = "INR 2000 debited from A/c no. XX3423 on 05-02-19 07:27:11 IST at ECS PAY. Avl Bal- INR 2343.23.";
        SmsModels.TransactionInfo info = parse(sms);

        assertEquals("debit", info.transaction.type);
        assertEquals("2000.00", info.transaction.amount);
        assertEquals(SmsModels.AccountType.ACCOUNT, info.account.type);
        assertEquals("3423", info.account.number);
        assertEquals("2343.23", info.balance.available);
    }

    // -------------------------------------------------------------------------
    // Test 2 – NEFT credit to account
    // -------------------------------------------------------------------------

    @Test
    public void test_credit_to_account_neft() {
        String sms = "Your a/c no. XX1234 has been credited with INR 5,000.00 on 10-03-23 through NEFT. Avl Bal: INR 12,435.50";
        SmsModels.TransactionInfo info = parse(sms);

        assertEquals("credit", info.transaction.type);
        assertEquals("5000.00", info.transaction.amount);
        assertEquals(SmsModels.AccountType.ACCOUNT, info.account.type);
        assertEquals("1234", info.account.number);
        assertNotNull(info.balance.available);
    }

    // -------------------------------------------------------------------------
    // Test 3 – card spend with available limit (HDFC Card)
    // -------------------------------------------------------------------------

    @Test
    public void test_card_spend_with_avl_limit() {
        String sms = "INR 1,500.00 spent on HDFC Card XX7890 at AMAZON RETAIL on 15-04-23. Avl limit: INR 35,000.00";
        SmsModels.TransactionInfo info = parse(sms);

        assertEquals("debit", info.transaction.type);
        assertEquals("1500.00", info.transaction.amount);
        assertEquals(SmsModels.AccountType.CARD, info.account.type);
        assertEquals("7890", info.account.number);
        // "Avl limit" is not in AVAILABLE_BALANCE_KEYWORDS; both Python and Java return null here.
        assertNull(info.balance.available);
    }

    // -------------------------------------------------------------------------
    // Test 4 – Paytm wallet debit
    // -------------------------------------------------------------------------

    @Test
    public void test_paytm_wallet_debit() {
        String sms = "Your Paytm wallet was debited for Rs. 299.00 for payment to NETFLIX. Avl Bal: Rs. 1,211.50";
        SmsModels.TransactionInfo info = parse(sms);

        assertEquals("debit", info.transaction.type);
        assertEquals("299.00", info.transaction.amount);
        assertEquals(SmsModels.AccountType.WALLET, info.account.type);
        assertNotNull(info.balance.available);
    }

    // -------------------------------------------------------------------------
    // Test 5 – Slice Card debit with outstanding balance
    // -------------------------------------------------------------------------

    @Test
    public void test_slice_card_debit_outstanding() {
        String sms = "Rs.435.00 debited from your Slice Card for Swiggy order on 28-06-23. Outstanding: Rs.1,235.00";
        SmsModels.TransactionInfo info = parse(sms);

        // Slice Card has no card number + no available balance → isValid=false → type=null (same as Python)
        assertNull(info.transaction.type);
        assertEquals("435.00", info.transaction.amount);
        assertEquals(SmsModels.AccountType.CARD, info.account.type);
        assertNotNull(info.balance.outstanding);
    }

    // -------------------------------------------------------------------------
    // Test 6 – null / empty input guard
    // -------------------------------------------------------------------------

    @Test
    public void test_null_input_returns_empty_info() {
        SmsModels.TransactionInfo info = parse(null);
        assertNotNull(info);
        assertNull(info.transaction.type);
        assertNull(info.transaction.amount);
    }

    @Test
    public void test_empty_input_returns_empty_info() {
        SmsModels.TransactionInfo info = parse("   ");
        assertNotNull(info);
        assertNull(info.transaction.type);
        assertNull(info.transaction.amount);
    }

    // -------------------------------------------------------------------------
    // Test 7 – getTransactionType helpers
    // -------------------------------------------------------------------------

    @Test
    public void test_getTransactionType_debit_keyword() {
        java.util.List<String> tokens = SmsUtils.processMessage("Your account debited Rs. 500");
        assertEquals("debit", SmsEngine.getTransactionType(tokens));
    }

    @Test
    public void test_getTransactionType_credit_keyword() {
        java.util.List<String> tokens = SmsUtils.processMessage("Account credited with Rs. 500");
        assertEquals("credit", SmsEngine.getTransactionType(tokens));
    }

    @Test
    public void test_getTransactionType_misc_keyword() {
        java.util.List<String> tokens = SmsUtils.processMessage("Rs. 250 spent on Zomato");
        assertEquals("debit", SmsEngine.getTransactionType(tokens));
    }

    // -------------------------------------------------------------------------
    // Test 8 – getTransactionAmount edge cases
    // -------------------------------------------------------------------------

    @Test
    public void test_amount_with_commas() {
        java.util.List<String> tokens = SmsUtils.processMessage("INR 1,23,456.78 debited");
        // After processMessage, "1,23,456.78" → next token after "rs."
        String amount = SmsEngine.getTransactionAmount(tokens);
        assertFalse(amount.isEmpty());
    }

    @Test
    public void test_amount_no_rs_marker_returns_empty() {
        java.util.List<String> tokens = SmsUtils.processMessage("No currency marker here at all");
        assertEquals("", SmsEngine.getTransactionAmount(tokens));
    }

    // -------------------------------------------------------------------------
    // Test 9 – padCurrencyValue
    // -------------------------------------------------------------------------

    @Test
    public void test_padCurrencyValue_no_decimal() {
        assertEquals("100.00", SmsUtils.padCurrencyValue("100"));
    }

    @Test
    public void test_padCurrencyValue_one_decimal() {
        assertEquals("100.50", SmsUtils.padCurrencyValue("100.5"));
    }

    @Test
    public void test_padCurrencyValue_two_decimals() {
        assertEquals("100.50", SmsUtils.padCurrencyValue("100.50"));
    }

    // -------------------------------------------------------------------------
    // Test 10 – merchant / UPI reference extraction
    // -------------------------------------------------------------------------

    @Test
    public void test_merchant_at_on_pattern() {
        String sms = "Rs. 500.00 debited at Swiggy on 01-01-24.";
        SmsModels.TransactionInfo info = parse(sms);
        // merchant should be extracted from "at Swiggy on"
        assertNotNull(info.transaction.merchant);
        assertTrue(info.transaction.merchant.toLowerCase().contains("swiggy"));
    }
}
