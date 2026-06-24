package pages;

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
 * The "Перевод внутри банка → По номеру телефона" flow (EPIC 1 / T-06): a within-bank transfer to
 * another person by phone number. Entered from the Быстрое меню (see
 * {@link MainScreenPage#openInBankTransfer()}), after a fraud-warning sheet. The form has a phone
 * field (entering a number resolves the recipient's name), an amount field, a "Я согласен…" terms
 * checkbox, and a submit button that reads "Продолжить" until the form is complete and then becomes
 * "Перевести". Submitting opens the "Подтверждение" review screen — where the test STOPS (it never
 * taps the final "Подтвердить", so no money moves).
 *
 * <p><b>Android-only.</b> iOS uses a different screen and is blocked by the iOS transfer-submit issue
 * (see {@code TransferIosTest} / project memory).
 */
public class PhoneTransferPage extends BasePage {

    private static final String PHONE_ID = "kz.bnk.app.dev:id/et_phone";
    private static final String AMOUNT_ID = "kz.bnk.app.dev:id/et_amount";
    private static final String WARNING_CONTINUE = "Продолжить";
    private static final String RECIPIENT_MARKER = "Получит перевод";       // appears once the phone resolves
    private static final String AGREE_TEXT = "Я согласен";
    private static final String SUBMIT = "Перевести";                       // submit after the form is complete
    private static final String CONFIRM_TITLE = "Подтверждение";
    private static final String CONFIRM_BUTTON = "Подтвердить";

    public PhoneTransferPage(AppiumDriver driver) {
        super(driver);
    }

    /** Dismisses the fraud-warning sheet shown right after opening the transfer (taps "Продолжить"). */
    public void dismissWarning() {
        List<WebElement> cont = driver.findElements(textLocator(WARNING_CONTINUE));
        if (!cont.isEmpty()) {
            cont.get(0).click();
            waitVisible(By.id(PHONE_ID), Duration.ofSeconds(10));
        }
    }

    /** True once the form is shown (the phone field is present). */
    public boolean isShown() {
        return waitVisible(By.id(PHONE_ID), Duration.ofSeconds(15));
    }

    /** Enters the recipient's phone number and waits for the recipient to resolve. */
    public void enterPhone(String phone) {
        WebElement field = driver.findElement(By.id(PHONE_ID));
        field.click();
        field.sendKeys(phone);
    }

    /** True once the entered phone resolves to a recipient ("Получит перевод в BNK" appears). */
    public boolean isRecipientResolved() {
        return waitVisible(textLocator(RECIPIENT_MARKER), Duration.ofSeconds(15));
    }

    /** True if the given recipient name/marker is shown (e.g. a surname fragment). */
    public boolean recipientShown(String namePart) {
        return !driver.findElements(textLocator(namePart)).isEmpty();
    }

    /** Types the amount and closes the keyboard via the IME "Done" action (Back would leave the form). */
    public void enterAmount(String amount) {
        WebElement field = driver.findElement(By.id(AMOUNT_ID));
        field.click();
        field.sendKeys(amount);
        try {
            ((AndroidDriver) driver).executeScript("mobile: performEditorAction", Map.of("action", "done"));
        } catch (Exception ignored) {
        }
    }

    /**
     * Checks the "Я согласен…" terms checkbox. The checkbox sits at the left margin of the agree row;
     * tapping the text itself does NOT toggle it, so we tap the checkbox to the left of the label.
     */
    public void acceptTerms() {
        List<WebElement> agree = driver.findElements(textLocator(AGREE_TEXT));
        if (agree.isEmpty()) return;
        Rectangle r = agree.get(0).getRect();
        tapXY(70, r.getY() + r.getHeight() / 2);
    }

    /** Taps the submit button ("Перевести", shown once the form is complete) to open the review screen. */
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

    private By textLocator(String text) {
        return AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + text + "\")");
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
