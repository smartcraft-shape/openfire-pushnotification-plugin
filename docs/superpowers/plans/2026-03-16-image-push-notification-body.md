# Image Push Notification Body Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace raw image URLs in push notification bodies with "Sent a photo" when the message body is a bare image URL.

**Architecture:** A new package-private static helper method `resolveNotificationBody(String)` is added to `PushInterceptor`. It detects bare image URLs by checking for no internal whitespace, an `http(s)://` scheme, and a known image file extension (case-insensitive). The existing `includedBody` assignment in `tryPushNotification` is updated to pass through this method. The method is package-private to allow direct unit testing without reflection.

**Tech Stack:** Java, Maven, JUnit 5 (junit-jupiter 5.10.0)

---

## Chunk 1: Tests + Implementation

### Task 1: Add JUnit 5 test dependency

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Add JUnit 5 dependency and Surefire plugin configuration to `pom.xml`**

  Add inside `<project>`, after the existing `<build>` block:

  ```xml
  <dependencies>
      <dependency>
          <groupId>org.junit.jupiter</groupId>
          <artifactId>junit-jupiter</artifactId>
          <version>5.10.0</version>
          <scope>test</scope>
      </dependency>
  </dependencies>
  ```

  Also add inside the existing `<build><plugins>` block:

  ```xml
  <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-surefire-plugin</artifactId>
      <version>3.2.5</version>
  </plugin>
  ```

- [ ] **Step 2: Verify the dependency resolves**

  Run from the plugin root:
  ```bash
  mvn dependency:resolve -q
  ```
  Expected: exits 0, no resolution errors for `junit-jupiter`.

---

### Task 2: Write failing tests for `resolveNotificationBody`

**Files:**
- Create: `src/test/java/org/igniterealtime/openfire/plugins/pushnotification/PushInterceptorTest.java`

- [ ] **Step 1: Create the test file**

  ```java
  package org.igniterealtime.openfire.plugins.pushnotification;

  import org.junit.jupiter.api.Test;
  import static org.junit.jupiter.api.Assertions.assertEquals;

  class PushInterceptorTest {

      // --- Image URLs that should return "Sent a photo" ---

      @Test
      void jpg_url_returns_sent_a_photo() {
          assertEquals("Sent a photo",
              PushInterceptor.resolveNotificationBody("https://cdn.example.com/img/photo.jpg"));
      }

      @Test
      void jpeg_url_returns_sent_a_photo() {
          assertEquals("Sent a photo",
              PushInterceptor.resolveNotificationBody("https://cdn.example.com/img/photo.jpeg"));
      }

      @Test
      void png_url_returns_sent_a_photo() {
          assertEquals("Sent a photo",
              PushInterceptor.resolveNotificationBody("https://cdn.example.com/img/photo.png"));
      }

      @Test
      void gif_url_returns_sent_a_photo() {
          assertEquals("Sent a photo",
              PushInterceptor.resolveNotificationBody("https://cdn.example.com/img/anim.gif"));
      }

      @Test
      void webp_url_returns_sent_a_photo() {
          assertEquals("Sent a photo",
              PushInterceptor.resolveNotificationBody("https://cdn.example.com/img/photo.webp"));
      }

      @Test
      void bmp_url_returns_sent_a_photo() {
          assertEquals("Sent a photo",
              PushInterceptor.resolveNotificationBody("https://cdn.example.com/img/photo.bmp"));
      }

      @Test
      void uppercase_extension_returns_sent_a_photo() {
          assertEquals("Sent a photo",
              PushInterceptor.resolveNotificationBody("https://cdn.example.com/img/photo.PNG"));
      }

      @Test
      void mixed_case_extension_returns_sent_a_photo() {
          assertEquals("Sent a photo",
              PushInterceptor.resolveNotificationBody("https://cdn.example.com/img/photo.Jpeg"));
      }

      @Test
      void image_url_with_query_string_returns_sent_a_photo() {
          assertEquals("Sent a photo",
              PushInterceptor.resolveNotificationBody("https://cdn.example.com/img/photo.png?v=123&size=large"));
      }

      @Test
      void image_url_with_fragment_returns_sent_a_photo() {
          assertEquals("Sent a photo",
              PushInterceptor.resolveNotificationBody("https://cdn.example.com/img/photo.jpg#anchor"));
      }

      @Test
      void http_scheme_image_url_returns_sent_a_photo() {
          assertEquals("Sent a photo",
              PushInterceptor.resolveNotificationBody("http://cdn.example.com/img/photo.jpg"));
      }

      // --- Bodies that should pass through unchanged ---

      @Test
      void plain_text_passes_through() {
          assertEquals("Hello world",
              PushInterceptor.resolveNotificationBody("Hello world"));
      }

      @Test
      void link_with_surrounding_text_passes_through() {
          String body = "check this out https://cdn.example.com/photo.jpg";
          assertEquals(body, PushInterceptor.resolveNotificationBody(body));
      }

      @Test
      void non_image_url_passes_through() {
          String body = "https://cdn.example.com/document.pdf";
          assertEquals(body, PushInterceptor.resolveNotificationBody(body));
      }

      @Test
      void url_without_extension_passes_through() {
          String body = "https://example.com/page";
          assertEquals(body, PushInterceptor.resolveNotificationBody(body));
      }

      @Test
      void non_http_scheme_passes_through() {
          String body = "ftp://cdn.example.com/img/photo.jpg";
          assertEquals(body, PushInterceptor.resolveNotificationBody(body));
      }
  }
  ```

- [ ] **Step 2: Run tests to verify they fail**

  ```bash
  mvn test -pl . -Dtest=PushInterceptorTest -q 2>&1 | tail -20
  ```
  Expected: compilation error — `resolveNotificationBody` does not exist yet.

---

### Task 3: Implement `resolveNotificationBody` and wire it in

**Files:**
- Modify: `src/main/java/org/igniterealtime/openfire/plugins/pushnotification/PushInterceptor.java`

- [ ] **Step 1: Add the `resolveNotificationBody` method**

  Add this method anywhere in `PushInterceptor` (e.g. just before `getMessageIdentifier`):

  ```java
  /**
   * Replaces a bare image URL with a human-friendly label for push notification display.
   * A body is considered a bare image URL if it contains no whitespace, starts with
   * http:// or https://, and the URL path (ignoring query string and fragment) ends with
   * a known image extension (case-insensitive).
   *
   * @param body a trimmed, non-empty message body
   * @return "Sent a photo" if the body is a bare image URL; otherwise the original body
   */
  static String resolveNotificationBody( final String body )
  {
      // Must be a bare URL — no internal whitespace
      if ( body.contains( " " ) ) {
          return body;
      }
      // Must use http or https scheme
      if ( !body.startsWith( "http://" ) && !body.startsWith( "https://" ) ) {
          return body;
      }
      // Strip query string and fragment, then check extension (case-insensitive)
      String path = body;
      final int queryIdx = path.indexOf( '?' );
      if ( queryIdx != -1 ) { path = path.substring( 0, queryIdx ); }
      final int fragmentIdx = path.indexOf( '#' );
      if ( fragmentIdx != -1 ) { path = path.substring( 0, fragmentIdx ); }
      final String lower = path.toLowerCase();
      if ( lower.endsWith( ".jpg" ) || lower.endsWith( ".jpeg" ) || lower.endsWith( ".png" )
              || lower.endsWith( ".gif" ) || lower.endsWith( ".webp" ) || lower.endsWith( ".bmp" ) ) {
          return "Sent a photo";
      }
      return body;
  }
  ```

- [ ] **Step 2: Wire the method into `tryPushNotification`**

  In `tryPushNotification`, find the `includedBody` assignment inside the `SUMMARY_INCLUDE_LAST_MESSAGE_BODY` block (around line 382):

  ```java
  // Before
  if ( message.getBody() != null && !message.getBody().trim().isEmpty() ) {
      includedBody = message.getBody().trim();
  }

  // After
  if ( message.getBody() != null && !message.getBody().trim().isEmpty() ) {
      includedBody = resolveNotificationBody( message.getBody().trim() );
  }
  ```

  The null/empty guard is unchanged. Only the assignment line changes.

- [ ] **Step 3: Run tests to verify they all pass**

  ```bash
  mvn test -pl . -Dtest=PushInterceptorTest 2>&1 | tail -20
  ```
  Expected: `BUILD SUCCESS`, all 16 tests pass.

- [ ] **Step 4: Run full test suite to confirm nothing is broken**

  ```bash
  mvn test 2>&1 | tail -20
  ```
  Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit**

  ```bash
  git add pom.xml \
    src/main/java/org/igniterealtime/openfire/plugins/pushnotification/PushInterceptor.java \
    src/test/java/org/igniterealtime/openfire/plugins/pushnotification/PushInterceptorTest.java
  git commit -m "SSC-203 : Replace bare image URLs in push notification body with 'Sent a photo'"
  ```
