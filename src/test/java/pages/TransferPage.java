package pages;

import core.Platform;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
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
 * The "between own accounts" transfer flow opened from an account's "Перевести" action (EPIC 1 /
 * T-05). The entry is a "Сумма перевода" bottom sheet with a destination (credit) card on top, a
 * source (debit) card below, an amount field and a "Продолжить" button. Tapping the destination card
 * opens a "Выберите счет" picker listing the user's OWN accounts; picking one, entering an amount and
 * tapping "Продолжить" leads to the "Подтверждение" review screen — where the test STOPS (it never
 * taps the final "Подтвердить", so no money moves).
 *
 * <p><b>Cross-platform.</b> Android uses the "Сумма перевода" sheet (resource-ids, see the methods
 * below). iOS uses a dedicated "Между своими счетами" screen with a source selector, a "Выберите счет"
 * destination selector, an amount TextField and a "Перевести" submit — see the {@code …Ios} methods.
 * Both stop at the "Подтверждение" review screen (never tap the final "Подтвердить", so no money moves).
 */
public class TransferPage extends BasePage {

    private static final String AMOUNT_ID = "kz.bnk.app.dev:id/et_amount";
    private static final String CONTINUE_ID = "kz.bnk.app.dev:id/btn_continue";
    // The account cards on the sheet (and the rows inside the picker) share this id; index 0 on the
    // sheet is the destination (credit) card.
    private static final String CARD_ID = "kz.bnk.app.dev:id/mcv_content";

    private static final String PICKER_TITLE = "Выберите счет";
    private static final String CONFIRM_TITLE = "Подтверждение";
    private static final String CONFIRM_BUTTON = "Подтвердить";
    // A top-up-able own account in the test data — a valid, distinct destination (never the source
    // current account), so source != destination for a valid "between own accounts" transfer.
    private static final String OWN_DESTINATION = "Совместный";

    public TransferPage(AppiumDriver driver) {
        super(driver);
    }

    // ---- iOS "Между своими счетами" happy path (verified on a live simulator) ----
    // The iOS transfer screen differs from Android: a dedicated "Между своими счетами" screen with a
    // SOURCE selector (auto-selects a deposit, which cannot be debited — so we re-pick a funded current
    // account), a "Выберите счет" DESTINATION selector, an amount TextField ("Сумма перевода") and a
    // "Перевести" submit. Both selectors open the SAME full account picker (rows are buttons whose name
    // is "<balance>, <account>, <IBAN>"); back = "BackButton".
    private static final String IOS_BETWEEN_OWN = "Между своими счетами";
    private static final String IOS_SUBMIT = "Перевести";
    private static final String IOS_PICK_DESTINATION = "Выберите счет";
    private static final String IOS_CONFIRM_TITLE = "Подтверждение";
    private static final String IOS_CONFIRM_BUTTON = "Подтвердить";
    private static final String IOS_SOURCE_CHEVRON = "Common/alt-arrow-down";
    private static final String IOS_DONE_KEY = "Готово";
    // Test-data own accounts: a funded current account (source) and a distinct current account (dest),
    // identified by their masked numbers. Both are own accounts, so stopping at confirmation moves no money.
    public static final String IOS_SOURCE_MARKER = "*400896";        // 49 585 688 ₸, funded current
    public static final String IOS_DESTINATION_MARKER = "*400888";   // 500 ₸, distinct current

    /** True once the "Сумма перевода" sheet is shown (amount field + Продолжить button present). */
    public boolean isAmountSheetShown() {
        return waitVisible(By.id(AMOUNT_ID), Duration.ofSeconds(15))
                && !driver.findElements(By.id(CONTINUE_ID)).isEmpty();
    }

    /**
     * True once the transfer entry screen is shown. Cross-platform: Android = the "Сумма перевода"
     * sheet; iOS = the "Между своими счетами" screen (title + "Перевести" submit button).
     */
    public boolean isTransferEntryShown() {
        return switch (Platform.current()) {
            case ANDROID -> isAmountSheetShown();
            case IOS -> waitVisible(iosLabel(IOS_BETWEEN_OWN), Duration.ofSeconds(15))
                    && !driver.findElements(AppiumBy.accessibilityId(IOS_SUBMIT)).isEmpty();
        };
    }

    /**
     * iOS: re-pick the SOURCE account (the screen auto-selects a deposit that cannot be debited) to a
     * funded current account, via the top selector's chevron, by masked-number marker.
     */
    public void selectSourceIos(String marker) {
        driver.findElements(AppiumBy.accessibilityId(IOS_SOURCE_CHEVRON)).get(0).click();
        pickAccountIos(marker);
    }

    /** iOS: pick a distinct DESTINATION own account via the "Выберите счет" selector, by marker. */
    public void selectDestinationIos(String marker) {
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(iosLabel(IOS_PICK_DESTINATION)))
                .click();
        pickAccountIos(marker);
    }

    /** iOS: type the amount into the "Сумма перевода" TextField and dismiss the keyboard. */
    public void enterAmountIos(String amount) {
        WebElement field = driver.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField'")).get(0);
        field.click();
        field.sendKeys(amount);
        List<WebElement> done = driver.findElements(iosLabel(IOS_DONE_KEY));
        if (!done.isEmpty()) done.get(0).click();
    }

    /** iOS: tap the "Перевести" submit to open the "Подтверждение" review screen. */
    public void tapSubmitIos() {
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(AppiumBy.accessibilityId(IOS_SUBMIT)))
                .click();
    }

    /** iOS: true once the "Подтверждение" review screen with its "Подтвердить" button is shown. */
    public boolean isConfirmationShownIos() {
        return waitVisible(iosLabel(IOS_CONFIRM_TITLE), Duration.ofSeconds(25))
                && !driver.findElements(iosLabel(IOS_CONFIRM_BUTTON)).isEmpty();
    }

    /** iOS: true if the confirmation screen shows the given text (amount or account). */
    public boolean confirmationShowsIos(String text) {
        return !driver.findElements(AppiumBy.iOSNsPredicateString(
                "label CONTAINS '" + text + "' OR name CONTAINS '" + text + "'")).isEmpty();
    }

    // Picks an account row (a button named "<balance>, <account>, <IBAN>") in the iOS picker by a
    // CONTAINS match on the marker, scrolling the list until the matching row is on-screen (hittable).
    private void pickAccountIos(String marker) {
        By row = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeButton' AND name CONTAINS '" + marker + "'");
        // Wait for the picker list to render (any balance row present) before scanning/scrolling.
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> !d.findElements(
                AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeButton' AND (name CONTAINS '₸' "
                        + "OR name CONTAINS '$')")).isEmpty());
        for (int i = 0; i < 12; i++) {
            for (WebElement el : driver.findElements(row)) {
                if ("true".equals(el.getAttribute("visible"))) {
                    el.click();
                    return;
                }
            }
            swipeUpIos();
        }
        driver.findElement(row).click();  // last resort
    }

    private void swipeUpIos() {
        PointerInput f = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        driver.perform(Collections.singletonList(new Sequence(f, 1)
                .addAction(f.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), 200, 600))
                .addAction(f.createPointerDown(0))
                .addAction(f.createPointerMove(Duration.ofMillis(300), PointerInput.Origin.viewport(), 200, 280))
                .addAction(f.createPointerUp(0))));
    }

    private By iosLabel(String text) {
        return AppiumBy.iOSNsPredicateString("label == '" + text + "' OR name == '" + text + "'");
    }

    /** Opens the destination-account picker by tapping the top (credit) card on the sheet. */
    public void openDestinationPicker() {
        driver.findElements(By.id(CARD_ID)).get(0).click();
    }

    /** True once the "Выберите счет" account picker is shown. */
    public boolean isAccountPickerShown() {
        return waitVisible(textLocator(PICKER_TITLE), Duration.ofSeconds(10));
    }

    /**
     * Picks a distinct OWN account ("Совместный Сберегательный", a top-up-able account in the test
     * data) as the destination, returning to the sheet. The picker may need scrolling to bring the
     * row into view, so we scroll it into view first; then we confirm the picker actually closed
     * (the tap occasionally doesn't register on a loaded list) and retry once if it didn't.
     */
    public void selectOwnDestination() {
        for (int attempt = 0; attempt < 2; attempt++) {
            scrollIntoViewAndClick(OWN_DESTINATION);
            try {
                new WebDriverWait(driver, Duration.ofSeconds(6)).until(
                        ExpectedConditions.invisibilityOfElementLocated(textLocator(PICKER_TITLE)));
                return;  // picker closed → destination selected
            } catch (Exception retry) {
                // still on the picker — loop and tap again
            }
        }
    }

    private void scrollIntoViewAndClick(String text) {
        By target = AppiumBy.androidUIAutomator(
                "new UiScrollable(new UiSelector().scrollable(true))"
                        + ".scrollIntoView(new UiSelector().textContains(\"" + text + "\"))");
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(target))
                .click();
    }

    /**
     * Types the transfer amount, then closes the soft keyboard via the IME "Done" action. We must NOT
     * close it with Back/hideKeyboard — on Android that dismisses the whole bottom sheet. Leaving the
     * keyboard up would cover "Продолжить", so a tap on it would silently hit the keyboard instead
     * (the flake this fixes).
     */
    public void enterAmount(String amount) {
        WebElement field = driver.findElement(By.id(AMOUNT_ID));
        field.click();
        field.sendKeys(amount);
        try {
            ((AndroidDriver) driver).executeScript("mobile: performEditorAction", Map.of("action", "done"));
        } catch (Exception ignored) {
            // Some keyboards have no Done action; the continue tap below still waits for clickability.
        }
        waitForKeyboardGone();
    }

    private void waitForKeyboardGone() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(5)).until(d ->
                    !((AndroidDriver) d).isKeyboardShown());
        } catch (Exception ignored) {
        }
    }

    /**
     * Taps "Продолжить" to move from the amount sheet to the confirmation screen. Note: we must NOT
     * dismiss the keyboard with hideKeyboard() first — on Android that sends Back, which closes the
     * whole bottom sheet. The button is found and tapped directly.
     */
    public void tapContinue() {
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(By.id(CONTINUE_ID)))
                .click();
    }

    /**
     * True once the "Подтверждение" review screen is shown with its final "Подтвердить" button. This
     * is the stop point — the test asserts we got here but never taps "Подтвердить".
     */
    public boolean isConfirmationShown() {
        // The review screen is built after a backend call, so allow generous time under load.
        return waitVisible(textLocator(CONFIRM_TITLE), Duration.ofSeconds(25))
                && !driver.findElements(textLocator(CONFIRM_BUTTON)).isEmpty();
    }

    /** The destination ("куда" / credit) account shown on the sheet, e.g. "Текущий счет *400896". */
    public String destinationAccount() {
        return accountIn("kz.bnk.app.dev:id/recycler_view_credit");
    }

    /** The source ("откуда" / debit) account shown on the sheet. */
    public String sourceAccount() {
        return accountIn("kz.bnk.app.dev:id/recycler_view_debit");
    }

    private String accountIn(String recyclerId) {
        return driver.findElement(By.id(recyclerId))
                .findElement(By.id("kz.bnk.app.dev:id/tv_type")).getText();
    }

    /** True if the confirmation screen shows the given amount (e.g. "1 000,00 ₸"). */
    public boolean confirmationShowsAmount(String amountText) {
        return !driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().textContains(\"" + amountText + "\")")).isEmpty();
    }

    private By textLocator(String text) {
        return AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + text + "\")");
    }

    private boolean waitVisible(By locator, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(
                    ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
