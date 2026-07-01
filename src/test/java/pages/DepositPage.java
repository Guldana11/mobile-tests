package pages;

import core.Platform;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Deposit-opening flow ("Открыть депозит"): home bottom action → "Депозиты" product list
 * (Срочный / Сберегательный) → product INFO screen (read-only details + bottom "Открыть депозит") →
 * application FORM (currency KZT/USD, source account, amount, term slider default 6, three consent
 * checkboxes, "Открыть депозит" submit). Submitting OPENS A REAL DEPOSIT (debits the source) — only
 * {@code DepositOpenTest} (opt-in, min amount, not in regression suites) taps it.
 *
 * <p><b>Cross-platform.</b> Android exposes clean resource-ids and the source is pre-selected; the three
 * consents are native {@code CheckBox}es (id {@code check}) toggled by {@code click()}. iOS drives the
 * form by labels/TextFields, but its consent checkboxes are custom SwiftUI controls that no synthetic tap
 * toggles — so the real open runs on Android (see {@code DepositOpenTest}).
 */
public class DepositPage extends BasePage {

    private static final String PKG = "kz.bnk.app.dev:id/";
    private static final String AMOUNT_ID = PKG + "et_amount";
    private static final String CHECK_ID = PKG + "check";          // the 3 consent CheckBoxes share this id
    private static final String SUBMIT_ID = PKG + "btn";           // "Открыть депозит" submit (Button)
    private static final String CURRENCY_LABEL_ID = PKG + "tv_label"; // KZT/USD chips in rv_currency_list

    private static final String LIST_TITLE = "Депозиты";
    private static final String OPEN_DEPOSIT = "Открыть депозит";  // info-screen button AND form submit
    private static final String AMOUNT_HINT = "Сумма депозита";
    private static final String SOURCE_SELECTOR = "Выберите счет";

    public DepositPage(AppiumDriver driver) {
        super(driver);
    }

    /** True once the "Депозиты" product list is shown. */
    public boolean isProductListShown() {
        return waitVisible(textLocator(LIST_TITLE), Duration.ofSeconds(15));
    }

    /** Taps a deposit product (e.g. "Срочный") to open its info screen. */
    public void selectProduct(String name) {
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(textLocator(name))).click();
    }

    /**
     * Taps the bottom "Открыть депозит" button on the product info screen to reach the application form.
     * On Android the info screen is a tall list and the button sits below the fold — scroll to it first.
     */
    public void openApplicationForm() {
        switch (Platform.current()) {
            case ANDROID -> {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView("
                                + "new UiSelector().text(\"" + OPEN_DEPOSIT + "\"))"));
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().text(\"" + OPEN_DEPOSIT + "\")")).click();
            }
            case IOS -> new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(openButtonLocator())).click();
        }
    }

    /** True once the application form is shown (the amount field is present). */
    public boolean isFormShown() {
        return switch (Platform.current()) {
            case ANDROID -> waitVisible(By.id(AMOUNT_ID), Duration.ofSeconds(15));
            case IOS -> waitVisible(AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeTextField' AND (placeholderValue == '" + AMOUNT_HINT
                            + "' OR value == '" + AMOUNT_HINT + "')"), Duration.ofSeconds(15));
        };
    }

    /** Selects the deposit currency (KZT is the default). */
    public void selectCurrency(String code) {
        switch (Platform.current()) {
            case ANDROID -> {
                for (WebElement chip : driver.findElements(By.id(CURRENCY_LABEL_ID))) {
                    if (code.equals(chip.getText())) { chip.click(); return; }
                }
            }
            case IOS -> {
                List<WebElement> c = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeButton' AND (label == '" + code + "' OR name == '" + code + "')"));
                if (!c.isEmpty()) c.get(0).click();
            }
        }
    }

    /**
     * Selects the source account. On Android a funded current account is pre-selected, so this is a
     * no-op; on iOS the source is also pre-selected. {@code marker} is reserved for a future picker.
     */
    public void selectSourceAccount(String marker) {
        if (Platform.current() != Platform.ANDROID) {
            return;  // iOS opens with a funded current account pre-selected
        }
        // Android: if a source is already chosen, nothing to do.
        if (driver.findElements(textLocator(SOURCE_SELECTOR)).isEmpty()) {
            return;
        }
        // Open the picker. Its rows show only "Текущий счет" + balance (NO account number, so a marker
        // can't select), and it lists only accounts of the chosen currency — pick the highest-balance one
        // (guaranteed funded). {@code marker} is ignored (kept for signature symmetry with other pages).
        driver.findElement(By.id(PKG + "include_account_from")).click();
        List<WebElement> rows = List.of();
        for (int i = 0; i < 12; i++) {   // rows load async
            rows = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceId(\"" + PKG + "design_bottom_sheet\").childSelector("
                            + "new UiSelector().clickable(true))"));
            if (!rows.isEmpty()) break;
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
        if (rows.isEmpty()) {
            throw new org.openqa.selenium.NoSuchElementException(
                    "deposit source-account picker is EMPTY — no funded account for this currency");
        }
        WebElement best = rows.get(0);
        long bestBalance = -1;
        for (WebElement row : rows) {
            long bal = rowBalance(row);
            if (bal > bestBalance) { bestBalance = bal; best = row; }
        }
        best.click();
    }

    // The integer part of a picker row's balance ("49 606 506,00 ₸" → 49606506), or -1 if unreadable.
    private long rowBalance(WebElement row) {
        try {
            String t = row.findElement(By.id(PKG + "tv_amount")).getText();
            String intPart = t.split(",")[0].replaceAll("[^0-9]", "");
            return intPart.isEmpty() ? -1 : Long.parseLong(intPart);
        } catch (Exception e) {
            return -1;
        }
    }

    /** Enters the deposit amount and dismisses the keyboard. */
    public void enterAmount(String amount) {
        switch (Platform.current()) {
            case ANDROID -> {
                WebElement f = driver.findElement(By.id(AMOUNT_ID));
                f.click();
                f.sendKeys(amount);
                try {
                    ((AndroidDriver) driver).executeScript("mobile: performEditorAction", Map.of("action", "done"));
                } catch (Exception ignored) {
                }
            }
            case IOS -> {
                WebElement f = driver.findElement(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeTextField' AND (placeholderValue == '" + AMOUNT_HINT
                                + "' OR value == '" + AMOUNT_HINT + "')"));
                f.click();
                f.sendKeys(amount);
                dismissKeyboardIos();
            }
        }
    }

    /**
     * Accepts ALL consent checkboxes. Android: the three native {@code CheckBox}es (id {@code check}) are
     * clicked directly. iOS: custom SwiftUI controls — best-effort native taps (do not reliably toggle).
     */
    public void acceptAllConsents() {
        switch (Platform.current()) {
            case ANDROID -> {
                // The consents sit at the bottom of the scroll view — scroll the last one into view first.
                try {
                    driver.findElement(AppiumBy.androidUIAutomator(
                            "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView("
                                    + "new UiSelector().resourceId(\"" + CHECK_ID + "\"))"));
                } catch (Exception ignored) {
                }
                for (WebElement c : driver.findElements(By.id(CHECK_ID))) {
                    if (!"true".equals(c.getAttribute("checked"))) c.click();
                }
            }
            case IOS -> acceptConsentsIos();
        }
    }

    /** True if the "Открыть депозит" submit is enabled (form valid). */
    public boolean isOpenEnabled() {
        List<WebElement> b = driver.findElements(submitLocator());
        return !b.isEmpty() && b.get(0).isEnabled();
    }

    /** Taps the "Открыть депозит" submit — OPENS A REAL DEPOSIT (debits the source). */
    public void tapOpen() {
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(submitLocator())).click();
    }

    /** True if the given text is shown (used to assert the post-open result screen). */
    public boolean shows(String text, Duration timeout) {
        return waitVisible(textLocator(text), timeout);
    }

    /**
     * True once the application form is left after submitting (the amount field disappears) — the open
     * proceeded to its confirmation / receipt / OTP screen. Tolerant signal so the real-open test is green
     * regardless of the exact post-open screen wording (captured separately via the page-source dump).
     */
    public boolean leftForm(Duration timeout) {
        By amount = Platform.current() == Platform.ANDROID
                ? By.id(AMOUNT_ID)
                : AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField' AND "
                        + "(placeholderValue == '" + AMOUNT_HINT + "' OR value == '" + AMOUNT_HINT + "')");
        try {
            new WebDriverWait(driver, timeout).until(ExpectedConditions.invisibilityOfElementLocated(amount));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ---- locators ----

    private By submitLocator() {
        return switch (Platform.current()) {
            case ANDROID -> By.id(SUBMIT_ID);
            case IOS -> openButtonLocator();
        };
    }

    private By openButtonLocator() {
        return AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND "
                + "(label == '" + OPEN_DEPOSIT + "' OR name == '" + OPEN_DEPOSIT + "')");
    }

    private By textLocator(String text) {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.iOSNsPredicateString("label CONTAINS '" + text + "' OR name CONTAINS '" + text + "'");
            case ANDROID -> AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + text + "\")");
        };
    }

    // ---- iOS consent helpers (best-effort; the SwiftUI checkboxes resist synthetic taps) ----

    private void acceptConsentsIos() {
        for (int i = 0; i < 5; i++) {
            List<WebElement> last = driver.findElements(textLocator("Уведомление депозитора"));
            if (!last.isEmpty()) {
                int y = last.get(0).getRect().getY();
                if (y > 0 && y < 760) break;
            }
            swipeUpIos();
        }
        for (int i = 0; i < 3; i++) {
            List<WebElement> boxes = smallCheckboxesSortedByY();
            if (boxes.size() > i) {
                tapElementNative(boxes.get(i));
                try { Thread.sleep(250); } catch (InterruptedException ignored) {}
            }
        }
    }

    private List<WebElement> smallCheckboxesSortedByY() {
        List<WebElement> boxes = new ArrayList<>();
        for (WebElement b : driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND visible == true"))) {
            Rectangle r = b.getRect();
            if (r.getWidth() <= 26 && r.getHeight() <= 26) boxes.add(b);
        }
        boxes.sort((a, b) -> Integer.compare(a.getRect().getY(), b.getRect().getY()));
        return boxes;
    }

    private void tapElementNative(WebElement el) {
        Rectangle r = el.getRect();
        try {
            ((IOSDriver) driver).executeScript("mobile: tap", Map.of(
                    "element", ((RemoteWebElement) el).getId(),
                    "x", r.getWidth() / 2, "y", r.getHeight() / 2));
        } catch (Exception e) {
            // best-effort
        }
    }

    private void swipeUpIos() {
        var f = new org.openqa.selenium.interactions.PointerInput(
                org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        driver.perform(java.util.Collections.singletonList(
                new org.openqa.selenium.interactions.Sequence(f, 1)
                        .addAction(f.createPointerMove(Duration.ZERO,
                                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 200, 680))
                        .addAction(f.createPointerDown(0))
                        .addAction(f.createPointerMove(Duration.ofMillis(400),
                                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), 200, 300))
                        .addAction(f.createPointerUp(0))));
        try { Thread.sleep(400); } catch (InterruptedException ignored) {}
    }

    private void dismissKeyboardIos() {
        List<WebElement> done = driver.findElements(AppiumBy.iOSNsPredicateString(
                "label == 'Готово' OR name == 'Готово' OR label == 'Done' OR name == 'Done'"));
        if (!done.isEmpty()) done.get(0).click();
    }

    private boolean waitVisible(By locator, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
