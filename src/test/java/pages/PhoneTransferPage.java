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
 * The within-bank, by-phone transfer flow (EPIC 1 / T-06): a transfer to another person by phone
 * number. Entered from the Быстрое меню (see {@link MainScreenPage#openInBankTransfer()}) after a
 * fraud-warning sheet. The form has a phone field (entering a number resolves the recipient's name),
 * an amount field, a "Я согласен…" terms checkbox and a "Перевести" submit. Submitting opens the
 * "Подтверждение" review screen — where the test STOPS (never taps the final "Подтвердить", so no
 * money moves).
 *
 * <p>Cross-platform. Android uses resource-ids; on iOS the form fields are TextFields (phone = first,
 * amount = the one hinting "Сумма перевода") and the terms checkbox / submit are matched by label.
 */
public class PhoneTransferPage extends BasePage {

    private static final String ANDROID_PHONE_ID = "kz.bnk.app.dev:id/et_phone";
    private static final String ANDROID_AMOUNT_ID = "kz.bnk.app.dev:id/et_amount";
    private static final String WARNING_CONTINUE = "Продолжить";
    private static final String RECIPIENT_MARKER = "Получит перевод";   // "…в BNK" (Android) / "…на BNK" (iOS)
    private static final String AGREE_TEXT = "Я согласен";
    private static final String SUBMIT = "Перевести";
    private static final String CONFIRM_TITLE = "Подтверждение";
    private static final String CONFIRM_BUTTON = "Подтвердить";
    private static final String AMOUNT_HINT = "Сумма";                  // iOS amount TextField hint

    public PhoneTransferPage(AppiumDriver driver) {
        super(driver);
    }

    /** Dismisses the fraud-warning sheet shown right after opening the transfer (taps "Продолжить"). */
    public void dismissWarning() {
        List<WebElement> cont = driver.findElements(textLocator(WARNING_CONTINUE));
        if (!cont.isEmpty()) {
            cont.get(0).click();
            isShown();
        }
    }

    /** True once the form is shown (the phone field is present). */
    public boolean isShown() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15)).until(d -> phoneField() != null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Enters the recipient's phone number. */
    public void enterPhone(String phone) {
        WebElement field = phoneField();
        field.click();
        field.sendKeys(phone);
    }

    /** True once the entered phone resolves to a recipient ("Получит перевод …" appears). */
    public boolean isRecipientResolved() {
        return waitVisible(textLocator(RECIPIENT_MARKER), Duration.ofSeconds(15));
    }

    /** True if the given recipient name/marker is shown (e.g. a surname fragment). */
    public boolean recipientShown(String namePart) {
        return !driver.findElements(textLocator(namePart)).isEmpty();
    }

    /** Types the amount and dismisses the keyboard (so it stops covering the submit button). */
    public void enterAmount(String amount) {
        WebElement field = amountField();
        field.click();
        field.sendKeys(amount);
        switch (Platform.current()) {
            case ANDROID -> {
                try {
                    ((AndroidDriver) driver).executeScript("mobile: performEditorAction", Map.of("action", "done"));
                } catch (Exception ignored) {
                }
            }
            case IOS -> {
                List<WebElement> done = driver.findElements(
                        AppiumBy.iOSNsPredicateString("label == 'Готово' OR name == 'Готово'"));
                if (!done.isEmpty()) done.get(0).click();
            }
        }
    }

    /**
     * Checks the "Я согласен…" terms checkbox. The checkbox sits at the left margin of the agree row;
     * tapping the text itself does NOT toggle it, so we tap to the left of the label.
     */
    public void acceptTerms() {
        List<WebElement> agree = driver.findElements(textLocator(AGREE_TEXT));
        if (agree.isEmpty()) return;
        Rectangle r = agree.get(0).getRect();
        int x = Platform.current() == Platform.IOS ? 30 : 70;
        tapXY(x, r.getY() + r.getHeight() / 2);
    }

    /** Taps the "Перевести" submit to open the review screen. */
    public void tapTransfer() {
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(textLocator(SUBMIT)))
                .click();
    }

    /** True once the "Подтверждение" review screen with its final "Подтвердить" button is shown. */
    public boolean isConfirmationShown() {
        return waitVisible(textLocator(CONFIRM_TITLE), Duration.ofSeconds(25))
                && !driver.findElements(textLocator(CONFIRM_BUTTON)).isEmpty();
    }

    /** True if the confirmation screen shows the given text (amount or recipient). */
    public boolean confirmationShows(String text) {
        return !driver.findElements(textLocator(text)).isEmpty();
    }

    // The phone field: Android resource-id; iOS = the first TextField (top of the form).
    private WebElement phoneField() {
        return switch (Platform.current()) {
            case ANDROID -> {
                List<WebElement> e = driver.findElements(By.id(ANDROID_PHONE_ID));
                yield e.isEmpty() ? null : e.get(0);
            }
            case IOS -> {
                List<WebElement> tfs = textFieldsIos();
                yield tfs.isEmpty() ? null : tfs.get(0);
            }
        };
    }

    // The amount field: Android resource-id; iOS = the TextField hinting "Сумма перевода".
    private WebElement amountField() {
        return switch (Platform.current()) {
            case ANDROID -> driver.findElement(By.id(ANDROID_AMOUNT_ID));
            case IOS -> {
                for (WebElement f : textFieldsIos()) {
                    String v = f.getText();
                    if (v != null && v.contains(AMOUNT_HINT)) yield f;
                }
                List<WebElement> tfs = textFieldsIos();
                yield tfs.size() > 1 ? tfs.get(1) : tfs.get(0);
            }
        };
    }

    private List<WebElement> textFieldsIos() {
        return driver.findElements(AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeTextField'"));
    }

    // CONTAINS match on visible text/label (cross-platform).
    private By textLocator(String text) {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.iOSNsPredicateString("label CONTAINS '" + text + "' OR name CONTAINS '" + text + "'");
            case ANDROID -> AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + text + "\")");
        };
    }

    private void tapXY(int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        driver.perform(Collections.singletonList(new Sequence(finger, 1)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger.createPointerMove(Duration.ofMillis(80), PointerInput.Origin.viewport(), x, y))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))));
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
