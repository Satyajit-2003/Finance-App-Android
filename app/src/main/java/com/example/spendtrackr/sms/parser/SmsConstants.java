package com.example.spendtrackr.sms.parser;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class SmsConstants {

    public static final List<String> AVAILABLE_BALANCE_KEYWORDS = Arrays.asList(
            "avbl bal",
            "available balance",
            "available limit",
            "available credit limit",
            "avbl. credit limit",
            "limit available",
            "a/c bal",
            "ac bal",
            "available bal",
            "avl bal",
            "updated balance",
            "total balance",
            "new balance",
            "bal",
            "avl lmt",
            "available"
    );

    public static final List<String> OUTSTANDING_BALANCE_KEYWORDS = Arrays.asList("outstanding");

    public static final List<String> WALLETS = Arrays.asList("paytm", "simpl", "lazypay", "amazon_pay");

    public static final List<String> UPI_KEYWORDS = Arrays.asList("upi", "ref no", "upi ref", "upi ref no");

    public static final List<SmsModels.CombinedWord> COMBINED_WORDS = Arrays.asList(
            new SmsModels.CombinedWord(
                    Pattern.compile("credit\\scard", Pattern.CASE_INSENSITIVE), "c_card", SmsModels.AccountType.CARD),
            new SmsModels.CombinedWord(
                    Pattern.compile("amazon\\spay", Pattern.CASE_INSENSITIVE), "amazon_pay", SmsModels.AccountType.WALLET),
            new SmsModels.CombinedWord(
                    Pattern.compile("uni\\scard", Pattern.CASE_INSENSITIVE), "uni_card", SmsModels.AccountType.CARD),
            new SmsModels.CombinedWord(
                    Pattern.compile("niyo\\scard", Pattern.CASE_INSENSITIVE), "niyo", SmsModels.AccountType.ACCOUNT),
            new SmsModels.CombinedWord(
                    Pattern.compile("slice\\scard", Pattern.CASE_INSENSITIVE), "slice_card", SmsModels.AccountType.CARD),
            new SmsModels.CombinedWord(
                    Pattern.compile("one\\s*card", Pattern.CASE_INSENSITIVE), "one_card", SmsModels.AccountType.CARD)
    );

    public static final List<String> UPI_HANDLES = Arrays.asList(
            "@BARODAMPAY", "@rbl", "@idbi", "@upi", "@aubank", "@axisbank", "@bandhan",
            "@dlb", "@indus", "@kbl", "@federal", "@sbi", "@uco", "@citi", "@citigold",
            "@dbs", "@freecharge", "@okhdfcbank", "@okaxis", "@oksbi", "@okicici",
            "@yesg", "@hsbc", "@icici", "@indianbank", "@allbank", "@kotak",
            "@ikwik", "@unionbankofindia", "@uboi", "@unionbank", "@paytm", "@ybl",
            "@axl", "@ibl", "@sib", "@yespay"
    );
}
