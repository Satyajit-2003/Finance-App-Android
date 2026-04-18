package com.example.spendtrackr.sms.parser;

/**
 * Standalone test runner for the SMS parser Java port.
 * Mirrors the SMS messages tested in Finance-backend-API/run_tests.py :: test_sms_parser_direct().
 *
 * Run from project root:
 *   javac -d /tmp/out app/src/main/java/com/example/spendtrackr/sms/parser/*.java \
 *                      app/src/test/java/com/example/spendtrackr/sms/parser/SmsParserRunner.java
 *   java -cp /tmp/out com.example.spendtrackr.sms.parser.SmsParserRunner
 */
public class SmsParserRunner {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== SMS Parser Test Runner ===\n");

        test_debit_from_account_avlBal();
        test_credit_to_account_neft();
        test_card_spend_with_avl_limit();
        test_paytm_wallet_debit();
        test_slice_card_debit_outstanding();
        test_null_input();
        test_empty_input();
        test_getTransactionType_keywords();
        test_padCurrencyValue();
        test_merchant_at_on_pattern();
        test_validation_rules();

        System.out.println("\n==============================");
        System.out.printf("Results: %d passed, %d failed%n", passed, failed);
        if (failed > 0) System.exit(1);
    }

    // -------------------------------------------------------------------------

    static void test_debit_from_account_avlBal() {
        String sms = "INR 2000 debited from A/c no. XX3423 on 05-02-19 07:27:11 IST at ECS PAY. Avl Bal- INR 2343.23.";
        SmsModels.TransactionInfo info = SmsEngine.getTransactionInfo(sms);
        printSms(1, sms, info);
        check("type=debit",       "debit".equals(info.transaction.type));
        check("amount=2000.00",   "2000.00".equals(info.transaction.amount));
        check("accountType=ACCOUNT", info.account.type == SmsModels.AccountType.ACCOUNT);
        check("accountNo=3423",   "3423".equals(info.account.number));
        check("availBal present", info.balance != null && info.balance.available != null);
    }

    static void test_credit_to_account_neft() {
        String sms = "Your a/c no. XX1234 has been credited with INR 5,000.00 on 10-03-23 through NEFT. Avl Bal: INR 12,435.50";
        SmsModels.TransactionInfo info = SmsEngine.getTransactionInfo(sms);
        printSms(2, sms, info);
        check("type=credit",      "credit".equals(info.transaction.type));
        check("amount=5000.00",   "5000.00".equals(info.transaction.amount));
        check("accountType=ACCOUNT", info.account.type == SmsModels.AccountType.ACCOUNT);
        check("accountNo=1234",   "1234".equals(info.account.number));
        check("availBal present", info.balance != null && info.balance.available != null);
    }

    static void test_card_spend_with_avl_limit() {
        String sms = "INR 1,500.00 spent on HDFC Card XX7890 at AMAZON RETAIL on 15-04-23. Avl limit: INR 35,000.00";
        SmsModels.TransactionInfo info = SmsEngine.getTransactionInfo(sms);
        printSms(3, sms, info);
        check("type=debit",       "debit".equals(info.transaction.type));
        check("amount=1500.00",   "1500.00".equals(info.transaction.amount));
        check("accountType=CARD", info.account.type == SmsModels.AccountType.CARD);
        check("accountNo=7890",   "7890".equals(info.account.number));
        // NOTE: "Avl limit" is not in AVAILABLE_BALANCE_KEYWORDS ("avl lmt" is).
        // Both Python and Java correctly return null here. Known parser gap.
        check("availBal=null (avl limit not in keywords)", info.balance == null || info.balance.available == null);
    }

    static void test_paytm_wallet_debit() {
        String sms = "Your Paytm wallet was debited for Rs. 299.00 for payment to NETFLIX. Avl Bal: Rs. 1,211.50";
        SmsModels.TransactionInfo info = SmsEngine.getTransactionInfo(sms);
        printSms(4, sms, info);
        check("type=debit",         "debit".equals(info.transaction.type));
        check("amount=299.00",      "299.00".equals(info.transaction.amount));
        check("accountType=WALLET", info.account.type == SmsModels.AccountType.WALLET);
        check("availBal present",   info.balance != null && info.balance.available != null);
    }

    static void test_slice_card_debit_outstanding() {
        String sms = "Rs.435.00 debited from your Slice Card for Swiggy order on 28-06-23. Outstanding: Rs.1,235.00";
        SmsModels.TransactionInfo info = SmsEngine.getTransactionInfo(sms);
        printSms(5, sms, info);
        // NOTE: Slice Card has no card number and no available balance.
        // isValid requires 2 of {balance,amount,accountNo}; only amount present → type=null.
        // Both Python and Java behave identically here.
        check("type=null (insufficient fields for validation)", info.transaction.type == null);
        check("amount=435.00",           "435.00".equals(info.transaction.amount));
        check("accountType=CARD",        info.account.type == SmsModels.AccountType.CARD);
        check("outstanding present",     info.balance != null && info.balance.outstanding != null);
    }

    static void test_null_input() {
        SmsModels.TransactionInfo info = SmsEngine.getTransactionInfo(null);
        System.out.println("\n[Test 6] null input");
        check("type=null",   info.transaction.type == null);
        check("amount=null", info.transaction.amount == null);
    }

    static void test_empty_input() {
        SmsModels.TransactionInfo info = SmsEngine.getTransactionInfo("   ");
        System.out.println("\n[Test 7] blank input");
        check("type=null",   info.transaction.type == null);
        check("amount=null", info.transaction.amount == null);
    }

    static void test_getTransactionType_keywords() {
        System.out.println("\n[Test 8] getTransactionType keywords");
        check("debit keyword",
                "debit".equals(SmsEngine.getTransactionType(SmsUtils.processMessage("Your account debited Rs. 500"))));
        check("credit keyword",
                "credit".equals(SmsEngine.getTransactionType(SmsUtils.processMessage("Account credited with Rs. 500"))));
        check("misc→debit",
                "debit".equals(SmsEngine.getTransactionType(SmsUtils.processMessage("Rs. 250 spent on Zomato"))));
    }

    static void test_padCurrencyValue() {
        System.out.println("\n[Test 9] padCurrencyValue");
        check("no decimal  → .00", "100.00".equals(SmsUtils.padCurrencyValue("100")));
        check("one decimal → .50", "100.50".equals(SmsUtils.padCurrencyValue("100.5")));
        check("two decimals kept", "100.50".equals(SmsUtils.padCurrencyValue("100.50")));
    }

    static void test_validation_rules() {
        System.out.println("\n[Test 11] ValidationRules.isValidTransaction");

        // valid debit
        String sms1 = "INR 2000 debited from A/c no. XX3423. Avl Bal- INR 2343.23.";
        SmsModels.TransactionInfo t1 = SmsEngine.getTransactionInfo(sms1);
        check("valid debit → true",     ValidationRules.isValidTransaction(t1, sms1));

        // credit rejected
        String sms2 = "Your a/c no. XX1234 has been credited with INR 5,000.00. Avl Bal: INR 12,435.50";
        SmsModels.TransactionInfo t2 = SmsEngine.getTransactionInfo(sms2);
        check("credit → false",         !ValidationRules.isValidTransaction(t2, sms2));

        // invalid keyword: "failed"
        String sms3 = "Transaction failed. INR 500 debited from A/c XX1234.";
        SmsModels.TransactionInfo t3 = SmsEngine.getTransactionInfo(sms3);
        check("'failed' keyword → false", !ValidationRules.isValidTransaction(t3, sms3));

        // invalid keyword: "otp"
        String sms4 = "Your OTP is 123456. Do not share.";
        SmsModels.TransactionInfo t4 = SmsEngine.getTransactionInfo(sms4);
        check("'otp' keyword → false",  !ValidationRules.isValidTransaction(t4, sms4));

        // null info
        check("null info → false",      !ValidationRules.isValidTransaction(null, "some sms"));

        // missing account number (named card only)
        String sms5 = "Rs.435.00 debited from your Slice Card for Swiggy order on 28-06-23.";
        SmsModels.TransactionInfo t5 = SmsEngine.getTransactionInfo(sms5);
        check("no account number → false", !ValidationRules.isValidTransaction(t5, sms5));
    }

    static void test_merchant_at_on_pattern() {        String sms = "Rs. 500.00 debited at Swiggy on 01-01-24.";
        SmsModels.TransactionInfo info = SmsEngine.getTransactionInfo(sms);
        System.out.println("\n[Test 10] merchant 'at X on' pattern");
        printSms(10, sms, info);
        check("merchant contains 'swiggy'",
                info.transaction.merchant != null &&
                info.transaction.merchant.toLowerCase().contains("swiggy"));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void printSms(int n, String sms, SmsModels.TransactionInfo info) {
        System.out.printf("%n[Test %d] %s%n", n, sms);
        System.out.printf("  → type=%-8s amount=%-10s account=%s/%s  availBal=%s outstanding=%s merchant=%s refNo=%s%n",
                info.transaction.type,
                info.transaction.amount,
                info.account.type,
                info.account.number,
                info.balance != null ? info.balance.available : "—",
                info.balance != null ? info.balance.outstanding : "—",
                info.transaction.merchant,
                info.transaction.referenceNo);
    }

    private static void check(String label, boolean condition) {
        if (condition) {
            System.out.println("  PASS  " + label);
            passed++;
        } else {
            System.out.println("  FAIL  " + label);
            failed++;
        }
    }
}
