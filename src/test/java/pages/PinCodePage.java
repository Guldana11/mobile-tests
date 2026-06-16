package pages;

import core.Platform;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * PIN-code creation screen ("Создайте код входа"), shown right after a successful password login on
 * a fresh install. Android exposes the keypad as buttons btn0..btn9 + btn_delete; iOS exposes the
 * digits as buttons whose accessibility id is the digit, plus a "Common/backspace" button.
 */
public class PinCodePage extends BasePage {

    private static final String TITLE = "Создайте код входа";
    // After 4 digits are entered on the create step the SAME screen swaps its title to ask for the
    // code again (the confirmation step).
    private static final String CONFIRM_TITLE = "Введите код еще раз";

    private static final String ANDROID_TITLE_ID = "kz.bnk.app.dev:id/tv_title";
    private static final String ANDROID_DIGIT_PREFIX = "kz.bnk.app.dev:id/btn";
    private static final String ANDROID_BACKSPACE = "kz.bnk.app.dev:id/btn_delete";

    private static final String IOS_BACKSPACE = "Common/backspace";

    public PinCodePage(AppiumDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        return waitForDisplayed(Duration.ofSeconds(20));
    }

    public boolean waitForDisplayed(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(
                    ExpectedConditions.visibilityOfElementLocated(titleLocator()));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasAllDigitKeys() {
        for (int d = 0; d <= 9; d++) {
            if (driver.findElements(digitLocator(d)).isEmpty()) return false;
        }
        return true;
    }

    public boolean hasBackspaceKey() {
        return !driver.findElements(backspaceLocator()).isEmpty();
    }

    public void enterPin(String pin) {
        for (char c : pin.toCharArray()) {
            driver.findElement(digitLocator(Character.getNumericValue(c))).click();
        }
    }

    public void tapBackspace() {
        driver.findElement(backspaceLocator()).click();
    }

    /** True if the screen is still on the create step ("Создайте код входа"). */
    public boolean isCreateStepDisplayed() {
        return !driver.findElements(titleLocator()).isEmpty();
    }

    /**
     * Completes the full PIN setup: enters {@code pin} on the create step, waits for the confirmation
     * step, then re-enters the same {@code pin}. Returns false if the confirmation step never appears.
     * After this the app advances to the main screen (behind onboarding prompts on iOS).
     */
    public boolean completeSetup(String pin) {
        enterPin(pin);
        if (!waitForConfirmStep(Duration.ofSeconds(10))) return false;
        enterPin(pin);
        return true;
    }

    /** Waits for the confirmation step ("Введите код еще раз") to appear after 4 digits. */
    public boolean waitForConfirmStep(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(
                    ExpectedConditions.visibilityOfElementLocated(confirmTitleLocator()));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private By titleLocator() {
        return switch (Platform.current()) {
            case ANDROID -> AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceId(\"" + ANDROID_TITLE_ID + "\").text(\"" + TITLE + "\")");
            case IOS -> AppiumBy.accessibilityId(TITLE);
        };
    }

    private By confirmTitleLocator() {
        return switch (Platform.current()) {
            case ANDROID -> AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceId(\"" + ANDROID_TITLE_ID + "\").text(\"" + CONFIRM_TITLE + "\")");
            case IOS -> AppiumBy.accessibilityId(CONFIRM_TITLE);
        };
    }

    private By digitLocator(int digit) {
        return switch (Platform.current()) {
            case ANDROID -> AppiumBy.id(ANDROID_DIGIT_PREFIX + digit);
            case IOS -> AppiumBy.accessibilityId(String.valueOf(digit));
        };
    }

    private By backspaceLocator() {
        return switch (Platform.current()) {
            case ANDROID -> AppiumBy.id(ANDROID_BACKSPACE);
            case IOS -> AppiumBy.accessibilityId(IOS_BACKSPACE);
        };
    }
}
