# Jaja Blocker

A simple and efficient Android application written in Kotlin (Jetpack Compose) that serves as an uncompromising incoming call blocker. It runs completely in the background as a native system service, seamlessly protecting your privacy from unwanted spam.

## Features

The application automatically evaluates every incoming call based on the following strict logic:

1. **Hidden & Restricted Numbers:** Any call from a hidden, private, unknown, or restricted number is **immediately blocked**.
2. **Whitelisted Countries (Always Allowed):** Calls from **Germany (+49)**, **Austria (+43)**, and **Italy (+39)** are **always permitted** and allowed to ring.
3. **Slovak Numbers (+421, 09xx, 02xx):**
   * If the number **is found in your contacts**, the call is permitted and rings normally.
   * If the number **is not in your contacts**, the call is **automatically rejected**.
4. **Other International Numbers:** Permitted by default (not blocked).

## How a Blocked Call Behaves & Notifications

When a call is intercepted and blocked:
* Your phone **will not ring** and the screen **will not wake up**. The rejection happens completely silently on the system level to prevent any distraction.
* **Instant Status Visibility:** The application automatically generates a custom, silent notification containing the **exact blocked phone number** directly in your phone's top notification bar (status bar).
* **Quick Callback & Security:** This keeps you fully informed about who tried to reach you. If a blocked number turns out to be important or unexpected, you can immediately see it in the status bar and choose to call back manually.
* All blocked calls also remain properly logged and visible in your device's native phone app call history.

## Requirements

* **Minimum SDK:** Android 14 (API 34) or higher.
* **Permissions:** * Requires Contact Read access (`READ_CONTACTS`) to verify Slovak numbers against your local phonebook whitelist.
   * Requires Notification post access (`POST_NOTIFICATIONS`) to display the blocked numbers in the status bar.
* **System Role:** Upon first launch, the app must be granted the default `Call Screening Service` role via the native system prompt.

## Tech Stack

* **Jetpack Compose** – Minimalist and lightweight user interface (`MainActivity`).
* **CallScreeningService** – Android's low-level background service for high-performance call interception.
* **NotificationManager** – System service utilized to push custom, non-intrusive status bar logs.
* **ContactsContract** – Secure, local querying of the device's address book.