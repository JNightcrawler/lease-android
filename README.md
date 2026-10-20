# Billing App — Standalone Android App (Java)

A fully standalone billing/POS app with two sections: **Admin** (manage
materials/utilities) and **Billing** (sell items, generate and print a
bill). No backend, no network calls at all — everything lives in a local
Room (SQLite) database on the device.

This is a separate app from the Lease Application — deliberately not
merged into it, per your last instruction.

---

## 1. Tech stack

- Java, Android SDK (minSdk 26, target/compileSdk 34)
- AppCompat + Material Components (Material3 theme)
- ViewBinding
- Room (local SQLite) — the "SQL layer"
- **No networking dependency at all** — no Retrofit, no OkHttp, no
  `INTERNET` permission declared

---

## 2. The two sections

### Admin tab
CRUD for products (materials/utilities): name, price, quantity, and a
**nullable** expiry date (a "Clear" button unsets it — non-perishable
materials just don't have one). Beyond your spec, I added:
- **Unit** (pcs/kg/ltr/etc.) — utilities and materials often need different units
- **Category** (optional) — simple grouping, not required
- **Low-stock badge** — shown when quantity is at or below 5 (see
  `ProductAdapter.LOW_STOCK_THRESHOLD` to change it)
- **Expiry badges** — "Expired" (red) once past the date, "Expires soon"
  (amber) within 7 days of it (see `EXPIRING_SOON_WINDOW_MS`)

### Billing tab
Build a cart (product + quantity, validated against current stock),
optionally attach a customer name/phone, then **Generate & Print**. This:
1. Re-validates stock against the database (not just what the screen last
   showed) and decrements it — all inside one Room transaction, so a
   mid-way stock failure rolls back everything rather than leaving a
   half-billed, half-decremented state
2. Saves the Bill + its line items
3. Sends the bill to whichever printer path is configured (see below)

### History tab (added, not in your original spec)
Every generated bill, newest first. Tap one to see its line items,
**reprint**, or **toggle paid/unpaid** — pure local bookkeeping, no
payment gateway, matching "payment UI is not necessary."

---

## 3. The printer flag — two full paths, pick one

Accessible via the printer icon in the Billing tab's toolbar (**Printer
Setup**). Choice is stored locally (`PrinterPreferences`, backed by
SharedPreferences) and read by `PrinterServiceFactory`, which hands back
whichever `PrinterService` implementation the flag selects. Both
`generateAndPrint()` and `reprint()` go through this same factory, so
switching the flag changes behavior everywhere without touching the rest
of the app.

### Option A — System Print (`SystemPrintService`)
Renders the bill as HTML in an invisible WebView, then hands it to
Android's built-in Print Framework — the same dialog you'd see printing
any document. Works with any printer already set up on the device (WiFi
printers, "Save as PDF," etc.). **No pairing step, no extra permissions.**
This is the default.

### Option B — Bluetooth Thermal Receipt Printer (`BluetoothThermalPrintService`)
Sends raw **ESC/POS** command bytes directly over a Bluetooth SPP socket
to a paired thermal receipt printer (`EscPosBuilder` builds the byte
sequence — init, bold, center, cut paper, etc.; this is the same command
set Epson TM-series and most compatible/clone receipt printers use).

**Setup required before this works:**
1. Pair the printer in Android's own Bluetooth settings first (outside
   this app) — standard Bluetooth pairing, same as pairing headphones.
2. In this app, go to **Printer Setup** -> select "Bluetooth Thermal
   Receipt Printer" -> grant the Bluetooth permission if prompted (only
   needed on Android 12+) -> pick your printer from the paired-devices
   list -> **Save**.

If your specific printer needs a different command set (some cheaper
clones deviate slightly, or you have a non-ESC/POS printer), the byte
sequence is entirely isolated in `EscPosBuilder.java` — that's the one
file you'd adjust.

### Why two full implementations instead of one
These solve genuinely different situations: System Print needs zero setup
but requires *some* printer already configured on the Android device
(WiFi or otherwise) — most billing/POS setups instead use a small
dedicated Bluetooth receipt printer, which needs the ESC/POS path
instead. Rather than guess which one you have, both are built and the
flag switches between them — change it anytime from Printer Setup without
reinstalling anything.

---

## 4. Data model (Room / SQLite)

### `products`
| Field | Type | Notes |
|---|---|---|
| id | String (UUID, PK) | |
| name | String | |
| price | double | |
| expiryDate | Long (nullable) | epoch millis; null = does not expire |
| quantity | int | current stock |
| unit | String | defaults to "pcs" |
| category | String (nullable) | |

### `bills`
| Field | Type | Notes |
|---|---|---|
| billId | long (auto-increment PK) | displayed as "INV-0001" etc. |
| customerName / customerPhone | String (nullable) | optional, walk-in-friendly |
| createdAt | long | |
| totalAmount | double | |
| isPaid | boolean | local bookkeeping only, no gateway |

### `bill_items`
| Field | Type | Notes |
|---|---|---|
| id | long (auto-increment PK) | |
| billId | long (FK -> bills, cascade delete) | |
| productId | String | not a Room FK on purpose — see below |
| productName, unit, unitPrice, quantity, lineTotal | — | **snapshotted** at sale time |

Line items snapshot the product's name/price at the moment of sale rather
than joining live to `products`, so a bill stays accurate and readable
even if a product is later renamed, repriced, or deleted.

---

## 5. Opening the project

Same as the other Java Android projects: Android Studio will offer to
regenerate the missing `gradle-wrapper.jar` on first open (it's a binary
file, not included). Or manually:
```bash
gradle wrapper --gradle-version 8.7
```
then build with `./gradlew assembleDebug` / `.\gradlew.bat assembleDebug`
on Windows.

---

## 6. What's deliberately not here

- **No payments** — matches "payment UI is not necessary now." The
  `isPaid` toggle is bookkeeping, not a transaction.
- **No sync/backend** — this app doesn't talk to the Lease Application
  backend or anywhere else. If you ever want these two apps' data to meet
  in the middle, that's a separate integration project, not something
  this app does today.
- **No customer database** — customer name/phone are free-text fields on
  a bill, not a managed entity. Easy to add later (a `customers` table +
  a picker) if you want repeat-customer lookup.
