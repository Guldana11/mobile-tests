package pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Map;

/**
 * The "between own accounts" transfer flow opened from an account's "Перевести" action (EPIC 1 /
 * T-05). The entry is a "Сумма перевода" bottom sheet with a destination (credit) card on top, a
 * source (debit) card below, an amount field and a "Продолжить" button. Tapping the destination card
 * opens a "Выберите счет" picker listing the user's OWN accounts; picking one, entering an amount and
 * tapping "Продолжить" leads to the "Подтверждение" review screen — where the test STOPS (it never
 * taps the final "Подтвердить", so no money moves).
 *
 * <p><b>Android-only.</b> The iOS flow is a different screen ("Между своими счетами" with a single
 * destination selector and a TextField) and is blocked upstream by the iOS account-open issue (no
 * tappable account cell in the a11y tree — see {@code AccountDetailTest}). iOS support is a separate
 * task; this page deliberately uses Android resource-ids only.
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

    /** True once the "Сумма перевода" sheet is shown (amount field + Продолжить button present). */
    public boolean isAmountSheetShown() {
        return waitVisible(By.id(AMOUNT_ID), Duration.ofSeconds(15))
                && !driver.findElements(By.id(CONTINUE_ID)).isEmpty();
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
