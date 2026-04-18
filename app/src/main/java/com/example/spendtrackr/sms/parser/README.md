# SMS Parser – Usage

## Parse an SMS

```java
import com.example.spendtrackr.sms.parser.SmsEngine;
import com.example.spendtrackr.sms.parser.SmsModels;

SmsModels.TransactionInfo info = SmsEngine.getTransactionInfo(rawSmsBody);

info.transaction.type       // "debit" | "credit" | null
info.transaction.amount     // "2000.00" | null
info.transaction.merchant   // "swiggy" | null
info.transaction.referenceNo// UPI ref number | null

info.account.type           // AccountType.CARD | ACCOUNT | WALLET | null
info.account.number         // last-4 digits | null
info.account.name           // wallet/combined-card name | null

info.balance.available      // "2343.23" | null
info.balance.outstanding    // "1235.00" | null (cards only)
```

## Typical call site (SmsReceiver.java)

```java
String body = /* raw SMS body from PDU */;
SmsModels.TransactionInfo info = SmsEngine.getTransactionInfo(body);

// Only send what's needed to the backend
String amount  = info.transaction.amount;
String type    = info.transaction.type;
String account = info.account.number;
```

## Run tests (no Android SDK required)

```bash
export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"

SRC=app/src/main/java
TEST=app/src/test/java
OUT=/tmp/sms_parser_out
mkdir -p $OUT

# compile parser
javac -d $OUT $SRC/com/example/spendtrackr/sms/parser/*.java

# compile + run test runner
javac -d $OUT -cp $OUT $TEST/com/example/spendtrackr/sms/parser/SmsParserRunner.java
java -cp $OUT com.example.spendtrackr.sms.parser.SmsParserRunner
```

## Known gaps

| Gap | Affected SMS | Fix |
|-----|-------------|-----|
| `"Avl limit"` not a balance keyword | `"Avl limit: INR 35,000"` → `balance.available = null` | Add `"avl limit"` to `SmsConstants.AVAILABLE_BALANCE_KEYWORDS` |
| Named cards (no digit after card token) miss type | Slice Card, One Card with no number | Relax `isValid` check for named cards |
| `loadAccInfo()` always empty | Bank-name → card-number mapping | Implement in `AccountParser.loadAccInfo()` via SharedPreferences |
