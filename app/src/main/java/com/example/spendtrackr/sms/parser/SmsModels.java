package com.example.spendtrackr.sms.parser;

import java.util.regex.Pattern;

public class SmsModels {

    public enum AccountType {
        CARD, WALLET, ACCOUNT
    }

    public enum BalanceKeywordType {
        AVAILABLE, OUTSTANDING
    }

    public static class AccountInfo {
        public AccountType type;
        public String number;
        public String name;

        public AccountInfo() {}

        public AccountInfo(AccountType type, String number, String name) {
            this.type = type;
            this.number = number;
            this.name = name;
        }

        @Override
        public String toString() {
            return "AccountInfo{type=" + type + ", number=" + number + ", name=" + name + "}";
        }
    }

    public static class Balance {
        public String available;
        public String outstanding;

        public Balance(String available, String outstanding) {
            this.available = available;
            this.outstanding = outstanding;
        }

        @Override
        public String toString() {
            return "Balance{available=" + available + ", outstanding=" + outstanding + "}";
        }
    }

    public static class Transaction {
        public String type; // "debit", "credit", or null
        public String amount;
        public String referenceNo;
        public String merchant;

        public Transaction() {}

        public Transaction(String type, String amount, String referenceNo, String merchant) {
            this.type = type;
            this.amount = amount;
            this.referenceNo = referenceNo;
            this.merchant = merchant;
        }

        @Override
        public String toString() {
            return "Transaction{type=" + type + ", amount=" + amount
                    + ", merchant=" + merchant + ", referenceNo=" + referenceNo + "}";
        }
    }

    public static class TransactionInfo {
        public AccountInfo account;
        public Balance balance;
        public Transaction transaction;

        public TransactionInfo(AccountInfo account, Balance balance, Transaction transaction) {
            this.account = account;
            this.balance = balance;
            this.transaction = transaction != null ? transaction : new Transaction();
        }

        @Override
        public String toString() {
            return "TransactionInfo{\n  " + account + "\n  " + balance + "\n  " + transaction + "\n}";
        }
    }

    public static class CombinedWord {
        public Pattern regex;
        public String word;
        public AccountType type;

        public CombinedWord(Pattern regex, String word, AccountType type) {
            this.regex = regex;
            this.word = word;
            this.type = type;
        }
    }
}
