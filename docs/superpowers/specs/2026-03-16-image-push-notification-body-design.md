# Design: Friendly Push Notification Body for Image Uploads

**Date:** 2026-03-16
**Status:** Approved

## Problem

When a user sends an image, the XMPP message body is the raw image URL. With `SUMMARY_INCLUDE_LAST_MESSAGE_BODY` enabled, this URL is forwarded verbatim as the push notification body, which is uninformative and visually poor.

## Goal

Replace the raw image URL with `"Sent a photo"` in the push notification body, while leaving all other message bodies (plain text, shared links with surrounding text) unchanged.

## Scope

Changes are confined to `PushInterceptor.java` in the openfire-pushnotification-plugin.

## Design

### Detection Logic

A package-private static helper method `resolveNotificationBody(String body)` is added to `PushInterceptor`. It is package-private (rather than private) so it can be called directly in unit tests without reflection:

1. **Bare URL check** — if the body contains whitespace, it cannot be a standalone URL attachment; return as-is immediately.
2. **URL scheme check** — if the body does not start with `http://` or `https://`, return as-is.
3. **Image extension check** — strip query string and fragment from the URL path, convert to lowercase, then check if the path ends with a known image extension: `.jpg`, `.jpeg`, `.png`, `.gif`, `.webp`, `.bmp`. The check is case-insensitive to handle any mixed-case extensions that may appear.
4. **Match** → return `"Sent a photo"`.
5. **No match** → return the body unchanged (covers plain links, non-image file URLs, etc.).

### Call Site

In `tryPushNotification`, the assignment inside the existing null/empty guard is updated to pass the trimmed body through `resolveNotificationBody`. The guard itself is unchanged.

```java
// Before
if ( message.getBody() != null && !message.getBody().trim().isEmpty() ) {
    includedBody = message.getBody().trim();
}

// After
if ( message.getBody() != null && !message.getBody().trim().isEmpty() ) {
    includedBody = resolveNotificationBody(message.getBody().trim());
}
```

`resolveNotificationBody` always receives an already-trimmed, non-empty string. The whitespace check in step 1 of the detection logic therefore only matches internal whitespace, which is the intended behaviour (a bare URL has no internal spaces).

## What Is Not Changing

- No changes to `XmppPushComponent.java` or `ExpoPushNotificationService.java` in ted-chat-backend.
- No new fields or interfaces introduced.
- The `"New Message"` fallback when `SUMMARY_INCLUDE_LAST_MESSAGE_BODY` is false is untouched.

## Extensibility Note

Future content types (video, PDF, etc.) can be handled by extending `resolveNotificationBody` with additional extension lists and return strings. The method is intentionally structured as a sequence of checks to make this straightforward.

## Out of Scope

- Video/document/audio detection (future work).
- Strategy pattern or registry (deferred per decision to keep this simple for now).
