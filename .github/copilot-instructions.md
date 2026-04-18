# SpendTrackr – Copilot Instructions

## Architecture Overview

Layered Android app (Java, minSdk 29) that auto-logs banking SMS to a remote backend (Google Sheets via REST API).

```
com.example.spendtrackr/
├── api/       Retrofit client, endpoints, response POJOs, retry handler
├── service/   SmsMonitorService (keepalive foreground), BootReceiver (auto-start)
├── sms/       SmsReceiver – intercepts banking SMS, calls logTransaction endpoint
├── ui/        MainActivity, 3 Fragments, 2 DialogFragments, 2 RecyclerView Adapters
└── utils/     SharedPrefHelper, CategoryManager (17 fixed categories), NotificationHelper
```

## Core Data Flow

**Incoming SMS → `SmsReceiver` → `ApiRetryHandler.enqueueWithRetry()` → `POST /api/v1/log-sms`**  
`SmsReceiver` forwards the raw SMS body without parsing — all amount/type/category extraction is done by a Python script on the backend. UI reads back via `GET /api/v1/transactions?date=YYYY-MM-DD`.

- `SmsMonitorService` is a START_STICKY foreground keepalive; actual SMS handling is done only in `SmsReceiver`.
- `BootReceiver` re-starts `SmsMonitorService` on device reboot.

## Configuration – Runtime, Not Hardcoded

Base URL and API key are stored in `SharedPreferences` ("SpendTrackrPrefs"), entered by the user in `SettingsFragment`.  
**When either value changes, call `ApiClient.rebuildService(context)`** to reinitialize Retrofit. The `X-API-Key` header is injected automatically via OkHttp `HeaderInterceptor`.

Key SharedPrefs keys (see `SharedPrefHelper`): `base_url`, `api_key`, `show_success_notification`, `show_failure_notification`, `show_error_notification`.

## Retry Logic

`ApiRetryHandler.enqueueWithRetry(call, maxRetries=10, callback)` retries on HTTP 404, 5xx, or `IOException`. Delay: `3000ms + (attemptNumber × 2000ms)`. Always clone the call before retry (`call.clone()`).

## API Endpoints (`ApiService.java`)

| Method | Endpoint                               | Usage                             |
| ------ | -------------------------------------- | --------------------------------- |
| GET    | `/health`                              | Connection test in Settings       |
| GET    | `/api/v1/check-auth`                   | Auth validation in Settings       |
| POST   | `/api/v1/log-sms`                      | Auto-log from SmsReceiver         |
| POST   | `/api/v1/transactions`                 | Manual add (AddTransactionDialog) |
| GET    | `/api/v1/transactions?date=YYYY-MM-DD` | TransactionFragment               |
| PATCH  | `/api/v1/transactions`                 | Edit (EditTransactionDialog)      |
| DELETE | `/api/v1/transactions`                 | Delete with request body          |
| GET    | `/api/v1/stats?month_year=YYYY-MM`     | ChartFragment                     |

All responses are wrapped: `BaseResponse<T>` with `success`, `message`, `data`, `error` fields.

## UI Patterns

- **No ViewModel/LiveData**: Dialogs communicate back via listener interfaces (e.g., `OnTransactionUpdatedListener`, `OnTransactionAddedListener`).
- **60-second client cache**: Both `TransactionFragment` and `ChartFragment` cache API responses in local Maps with a timestamp check. Invalidate by clearing the cache map and calling the fetch method.
- **Category system**: `CategoryManager` defines 17 fixed categories with color integers. Do not invent new categories; edit `CategoryManager.java`. Some categories are excluded from pie charts via a client-side filter list.
- **ViewBinding** is enabled. Access views as `binding.viewId`, not `findViewById`.
- Compose is declared as a dependency but **not used** — all UI is Views + Fragments.

## Build & Development

- **Backend repo**: [Finance-backend-API](https://github.com/Satyajit-2003/Finance-backend-API) — must be deployed and configured before the app is functional.
- Build: `./gradlew assembleDebug` | Install: `./gradlew installDebug`
- `compileSdk 34`, `targetSdk 34`, `minSdk 29`, Java 8 source/target compatibility.
- Key libraries: Retrofit 2.9.0, OkHttp 5.0.0-alpha, Gson, MPAndroidChart v3.1.0, Material 1.12.0.
