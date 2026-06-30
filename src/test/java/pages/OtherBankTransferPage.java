package pages;

import core.Platform;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The "Перевод в другой банк" (inter-bank transfer) form, opened from the Быстрое меню after a
 * fraud-warning sheet ("Банк уведомляет" → "Продолжить"). Fields: a source account, the recipient's
 * IBAN, their ИИН/БИН, the recipient bank's БИК (a picker, e.g. Kaspi = {@code CASPKZKA}), a КНП picker,
 * a free-text purpose, the amount and a terms checkbox; the resident/actual-sender/actual-recipient
 * switches keep their defaults. Submitting opens the "Подтверждение" review screen — the test STOPS
 * there, so <b>no money moves</b>.
 *
 * <p><b>Cross-platform.</b> The two platforms differ in how fields are reached:
 * <ul>
 *   <li><b>Android</b> exposes clean resource-ids; the source account is NOT pre-selected
 *       ("Выберите счет") so {@link #selectSourceAccount} picks one.</li>
 *   <li><b>iOS</b> drives the form by TextFields (matched by placeholder) + label-tap pickers; the
 *       source account comes pre-selected (a funded current account) so {@link #selectSourceAccount} is
 *       a no-op. The tall form pushes the purpose/terms/submit below the fold and the keyboard covers
 *       them, so fields are filled with keyboard dismissal and the lower controls are scrolled into view.</li>
 * </ul>
 */
public class OtherBankTransferPage extends BasePage {

    private static final String PKG = "kz.bnk.app.dev:id/";
    private static final String SOURCE_CARD = PKG + "include_account_from";
    private static final String ACCOUNT_ID = PKG + "et_account_number";
    private static final String IIN_ID = PKG + "et_iin";
    private static final String BIC_ID = PKG + "et_bic";
    private static final String KNP_ID = PKG + "et_knp";
    private static final String PURPOSE_ID = PKG + "et_purpose";
    private static final String AMOUNT_ID = PKG + "et_amount";
    private static final String CHECK_ID = PKG + "check";
    private static final String SUBMIT_ID = PKG + "button";
    private static final String SEARCH_ID = PKG + "etSearch";
    private static final String CODE_ID = PKG + "tv_code";

    private static final String ACCOUNT_HINT = "Номер счета";
    private static final String IIN_HINT = "ИИН/БИН";
    private static final String AMOUNT_HINT = "Сумма перевода";
    private static final String PURPOSE_HINT = "Цель перевода";
    private static final String BIC_LABEL = "БИК";
    private static final String KNP_LABEL = "КНП";
    private static final String AGREE_TEXT = "Я согласен";
    private static final String SUBMIT = "Перевести";
    private static final String CONTINUE = "Продолжить";
    private static final String CONFIRM_TITLE = "Подтверждение";
    private static final String CONFIRM_BUTTON = "Подтвердить";
    private static final String INVALID_FIELDS = "Некоторые поля неверно заполнены"; // submit-validation alert
    private static final String BIC_MISMATCH = "IBAN не принадлежит выбранному банку"; // inline БИК/IBAN error
    private static final String SEARCH_HINT = "Поиск";   // picker search field (iOS + Android share the label)

    public OtherBankTransferPage(AppiumDriver driver) {
        super(driver);
    }

    /** Dismisses the fraud-warning sheet ("Банк уведомляет" → "Продолжить") shown before the form. */
    public void dismissWarning() {
        List<WebElement> cont = driver.findElements(textLocator(CONTINUE));
        if (!cont.isEmpty()) cont.get(0).click();
    }

    /** True once the form is shown (the recipient account field is present). */
    public boolean isShown() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15)).until(d -> accountField() != null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Selects the source account. On Android the form opens with no source pre-selected, so the picker is
     * opened and the row containing {@code marker} (or the first row) is taken. On iOS a funded current
     * account is pre-selected, so this is a no-op.
     */
    public void selectSourceAccount(String marker) {
        if (Platform.current() == Platform.IOS) {
            return;  // a funded source is pre-selected by default
        }
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(By.id(SOURCE_CARD))).click();
        if (marker != null && !marker.isBlank()) {
            List<WebElement> byMarker = driver.findElements(
                    AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + marker + "\")"));
            if (!byMarker.isEmpty()) {
                byMarker.get(0).click();
                return;
            }
        }
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(By.id(PKG + "recycler_view")));
        List<WebElement> rows = driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().resourceId(\"" + PKG + "recycler_view\").childSelector("
                        + "new UiSelector().clickable(true))"));
        if (rows.isEmpty()) {
            throw new org.openqa.selenium.NoSuchElementException("no source-account rows found");
        }
        rows.get(0).click();
    }

    /** Enters the recipient's IBAN. */
    public void enterRecipientAccount(String iban) {
        WebElement f = accountField();
        f.click();
        f.sendKeys(iban);
        dismissKeyboardIos();
    }

    /** Enters the recipient's ИИН/БИН and dismisses the keyboard. */
    public void enterIin(String iin) {
        WebElement f = fieldByHint(IIN_ID, IIN_HINT);
        f.click();
        f.sendKeys(iin);
        dismissKeyboard();
    }

    /** Opens the БИК picker, searches for {@code bic} and taps the matching bank row (e.g. "CASPKZKA"). */
    public void selectBic(String bic) {
        switch (Platform.current()) {
            case ANDROID -> {
                driver.findElement(By.id(BIC_ID)).click();
                WebElement search = new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(ExpectedConditions.elementToBeClickable(By.id(SEARCH_ID)));
                search.click();
                search.sendKeys(bic);
                new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(ExpectedConditions.elementToBeClickable(AppiumBy.androidUIAutomator(
                                "new UiSelector().resourceId(\"" + CODE_ID + "\").text(\"" + bic + "\")")))
                        .click();
            }
            case IOS -> {
                driver.findElement(labelLocator(BIC_LABEL)).click();
                searchAndPickIos(bic);
            }
        }
    }

    /** Opens the КНП picker and taps the row whose code is {@code code} (e.g. "119"). */
    public void selectKnp(String code) {
        switch (Platform.current()) {
            case ANDROID -> {
                driver.findElement(By.id(KNP_ID)).click();
                new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(ExpectedConditions.elementToBeClickable(
                                AppiumBy.androidUIAutomator("new UiSelector().text(\"" + code + "\")")))
                        .click();
            }
            case IOS -> {
                driver.findElement(labelLocator(KNP_LABEL)).click();
                // The КНП list may be searchable; type the code if a search field is offered, then pick.
                List<WebElement> search = driver.findElements(
                        AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeSearchField' OR "
                                + "(type == 'XCUIElementTypeTextField' AND value == '" + SEARCH_HINT + "')"));
                if (!search.isEmpty()) {
                    search.get(0).click();
                    search.get(0).sendKeys(code);
                }
                new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(ExpectedConditions.elementToBeClickable(AppiumBy.iOSNsPredicateString(
                                "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' "
                                        + "OR type == 'XCUIElementTypeCell') AND "
                                        + "(name BEGINSWITH '" + code + "' OR label BEGINSWITH '" + code + "')")))
                        .click();
            }
        }
    }

    /** Enters the free-text transfer purpose ("Цель перевода"). */
    public void enterPurpose(String purpose) {
        WebElement f = fieldByHint(PURPOSE_ID, PURPOSE_HINT);
        f.click();
        f.sendKeys(purpose);
        dismissKeyboard();
    }

    /** Enters the amount and dismisses the keyboard. */
    public void enterAmount(String amount) {
        WebElement f = fieldByHint(AMOUNT_ID, AMOUNT_HINT);
        f.click();
        f.sendKeys(amount);
        dismissKeyboard();
    }

    /**
     * Checks the "Я согласен…" terms checkbox. Android: the real CheckBox (id "check"). iOS: there is no
     * a11y node for the toggle, so the checkbox to the left of the agree row is tapped by coordinates.
     */
    public void acceptTerms() {
        if (Platform.current() == Platform.ANDROID) {
            List<WebElement> check = driver.findElements(By.id(CHECK_ID));
            if (!check.isEmpty()) {
                check.get(0).click();
                return;
            }
        }
        if (Platform.current() == Platform.IOS) {
            acceptTermsIos();
            return;
        }
        List<WebElement> agree = driver.findElements(textLocator(AGREE_TEXT));
        if (agree.isEmpty()) return;
        Rectangle r = agree.get(0).getRect();
        tapXY(70, r.getY() + r.getHeight() / 2);
    }

    /**
     * iOS: the agree row + its checkbox sit below the fold and the box is a small unnamed custom control,
     * so it is finicky to toggle. Scroll it into view, then try several tap strategies (the checkbox
     * button's own centre, then the agree-row left margin) and stop as soon as the submit button enables
     * — that is the only reliable signal the box actually checked.
     */
    private void acceptTermsIos() {
        for (int i = 0; i < 4; i++) {
            List<WebElement> a = driver.findElements(textLocator(AGREE_TEXT));
            if (!a.isEmpty() && a.get(0).getRect().getY() < 740) break;  // on screen, clear of any keyboard
            swipeUpIos();
        }
        // Let the scroll fully settle — an immediate tap after a swipe is eaten by scroll deceleration.
        try { Thread.sleep(1200); } catch (InterruptedException ignored) {}

        WebElement box = smallCheckbox();
        if (box == null) return;
        // The agree checkbox is a custom SwiftUI control: a W3C pointer "tap" (down → 0-distance move → up)
        // reads as a drag and is rejected, so use a native XCUICoordinate tap on the element's centre
        // (mobile: tap) — the closest thing to a real finger. Fall back to a clean pause-based W3C tap.
        tapElementNative(box);
        if (settleSubmitEnabled()) return;
        Rectangle rb = box.getRect();
        cleanTapXY(rb.getX() + rb.getWidth() / 2, rb.getY() + rb.getHeight() / 2);
        settleSubmitEnabled();
    }

    // Native XCUICoordinate tap on an element's centre (mobile: tap, element-relative) — behaves like a
    // real touch on custom iOS controls that ignore W3C pointer taps.
    private void tapElementNative(WebElement el) {
        try {
            Rectangle r = el.getRect();
            ((io.appium.java_client.ios.IOSDriver) driver).executeScript("mobile: tap", Map.of(
                    "element", ((org.openqa.selenium.remote.RemoteWebElement) el).getId(),
                    "x", r.getWidth() / 2, "y", r.getHeight() / 2));
        } catch (Exception e) {
            Rectangle r = el.getRect();
            cleanTapXY(r.getX() + r.getWidth() / 2, r.getY() + r.getHeight() / 2);
        }
    }

    // A clean W3C tap: move → down → pause → up (no 0-distance move that SwiftUI mistakes for a drag).
    private void cleanTapXY(int x, int y) {
        PointerInput f = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        driver.perform(Collections.singletonList(new Sequence(f, 1)
                .addAction(f.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y))
                .addAction(f.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(new org.openqa.selenium.interactions.Pause(f, Duration.ofMillis(120)))
                .addAction(f.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))));
    }

    // The agree-row checkbox: a small (~22px) visible Button.
    private WebElement smallCheckbox() {
        for (WebElement b : driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND visible == true"))) {
            Rectangle rb = b.getRect();
            if (rb.getWidth() <= 30 && rb.getHeight() <= 30) return b;
        }
        return null;
    }

    // Brief wait for the submit button to flip to enabled (the form validates asynchronously).
    private boolean settleSubmitEnabled() {
        for (int i = 0; i < 6; i++) {
            if (isSubmitEnabled()) return true;
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        }
        return false;
    }

    /** True if the "Перевести" submit is enabled (gated until the form is valid). */
    public boolean isSubmitEnabled() {
        List<WebElement> b = driver.findElements(submitLocator());
        return !b.isEmpty() && b.get(0).isEnabled();
    }

    /** Taps the "Перевести" submit to open the review screen. */
    public void tapTransfer() {
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(submitLocator())).click();
    }

    /**
     * True if the form-validation error ("Некоторые поля неверно заполнены") is shown after submitting —
     * raised e.g. when the БИК does not match the recipient account's bank. Used by the negative tests.
     */
    public boolean isInvalidFieldsErrorShown(Duration timeout) {
        return waitVisible(textLocator(INVALID_FIELDS), timeout);
    }

    /**
     * True if the inline "IBAN не принадлежит выбранному банку" error is shown — the client-side check
     * that fires when the selected БИК does not match the recipient account's bank (keeps submit disabled).
     */
    public boolean isBicMismatchErrorShown(Duration timeout) {
        return waitVisible(textLocator(BIC_MISMATCH), timeout);
    }

    /**
     * Dismisses the "Некоторые поля неверно заполнены" validation alert if it popped (taps "Хорошо"),
     * so the form underneath — including its inline field errors — becomes readable again. Android-only,
     * best-effort.
     */
    public void dismissValidationAlert() {
        if (Platform.current() != Platform.ANDROID) return;
        waitVisible(textLocator(INVALID_FIELDS), Duration.ofSeconds(5));  // let the alert appear
        List<WebElement> ok = driver.findElements(By.id("android:id/button1"));
        if (ok.isEmpty()) {
            ok = driver.findElements(AppiumBy.androidUIAutomator("new UiSelector().text(\"Хорошо\")"));
        }
        if (!ok.isEmpty()) ok.get(0).click();
    }

    /** True once the "Подтверждение" review screen with its "Подтвердить" button is shown. */
    public boolean isConfirmationShown() {
        return waitVisible(textLocator(CONFIRM_TITLE), Duration.ofSeconds(25))
                && !driver.findElements(textLocator(CONFIRM_BUTTON)).isEmpty();
    }

    /** True if the confirmation screen shows the given text (amount / recipient / bank). */
    public boolean confirmationShows(String text) {
        return !driver.findElements(textLocator(text)).isEmpty();
    }

    // ---- helpers ----

    // The recipient-account field: Android resource-id; iOS the TextField hinting "Номер счета".
    private WebElement accountField() {
        return fieldByHint(ACCOUNT_ID, ACCOUNT_HINT);
    }

    // Resolves a form field: Android by resource-id, iOS by its placeholder/value hint.
    private WebElement fieldByHint(String androidId, String iosHint) {
        return switch (Platform.current()) {
            case ANDROID -> {
                List<WebElement> e = driver.findElements(By.id(androidId));
                yield e.isEmpty() ? null : e.get(0);
            }
            case IOS -> {
                List<WebElement> e = driver.findElements(AppiumBy.iOSNsPredicateString(
                        "type == 'XCUIElementTypeTextField' AND "
                                + "(placeholderValue == '" + iosHint + "' OR value == '" + iosHint + "')"));
                yield e.isEmpty() ? null : e.get(0);
            }
        };
    }

    // iOS: types a code into the picker's search field then taps the matching row (БИК list).
    private void searchAndPickIos(String code) {
        List<WebElement> search = driver.findElements(AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeSearchField' OR "
                        + "(type == 'XCUIElementTypeTextField' AND value == '" + SEARCH_HINT + "')"));
        if (!search.isEmpty()) {
            search.get(0).click();
            search.get(0).sendKeys(code);
        }
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(AppiumBy.iOSNsPredicateString(
                        "(type == 'XCUIElementTypeButton' OR type == 'XCUIElementTypeStaticText' "
                                + "OR type == 'XCUIElementTypeCell') AND "
                                + "(name CONTAINS '" + code + "' OR label CONTAINS '" + code + "')")))
                .click();
    }

    // Dismisses the keyboard (cross-platform): Android IME "done"; iOS taps "Готово" if present.
    private void dismissKeyboard() {
        switch (Platform.current()) {
            case ANDROID -> {
                try {
                    ((AndroidDriver) driver).executeScript("mobile: performEditorAction", Map.of("action", "done"));
                } catch (Exception ignored) {
                }
            }
            case IOS -> dismissKeyboardIos();
        }
    }

    private void dismissKeyboardIos() {
        if (Platform.current() != Platform.IOS) return;
        List<WebElement> done = driver.findElements(
                AppiumBy.iOSNsPredicateString("label == 'Готово' OR name == 'Готово' "
                        + "OR label == 'Done' OR name == 'Done'"));
        if (!done.isEmpty()) done.get(0).click();
    }

    private By submitLocator() {
        return switch (Platform.current()) {
            case ANDROID -> By.id(SUBMIT_ID);
            case IOS -> AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND "
                    + "(label == '" + SUBMIT + "' OR name == '" + SUBMIT + "')");
        };
    }

    // CONTAINS match on visible text/label (cross-platform).
    private By textLocator(String text) {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.iOSNsPredicateString("label CONTAINS '" + text + "' OR name CONTAINS '" + text + "'");
            case ANDROID -> AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + text + "\")");
        };
    }

    // Exact-label match (iOS picker entry points like "БИК"/"КНП").
    private By labelLocator(String text) {
        return AppiumBy.iOSNsPredicateString("(label == '" + text + "' OR name == '" + text + "') "
                + "AND type == 'XCUIElementTypeStaticText'");
    }

    private boolean waitVisible(By locator, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // iOS: scroll the form content up (drag from lower to upper) to reveal the terms row + submit.
    private void swipeUpIos() {
        PointerInput f = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        driver.perform(Collections.singletonList(new Sequence(f, 1)
                .addAction(f.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), 200, 700))
                .addAction(f.createPointerDown(0))
                .addAction(f.createPointerMove(Duration.ofMillis(400), PointerInput.Origin.viewport(), 200, 300))
                .addAction(f.createPointerUp(0))));
        try { Thread.sleep(400); } catch (InterruptedException ignored) {}
    }

    private void tapXY(int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        driver.perform(Collections.singletonList(new Sequence(finger, 1)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger.createPointerMove(Duration.ofMillis(80), PointerInput.Origin.viewport(), x, y))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))));
    }
}
