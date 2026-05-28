package pages;

import core.Platform;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * PIN-code creation screen ("Создайте код входа"), shown right after a successful password login
 * on a fresh install. iOS exposes the keypad digits as buttons whose accessibility id is the digit
 * and a "Common/backspace" button. There are no accessible PIN indicators in the tree.
 */
public class PinCodePage extends BasePage {

    private static final String IOS_TITLE = "Создайте код входа";
    private static final String IOS_BACKSPACE = "Common/backspace";

    private static final String ANDROID_TODO = "PinCodePage: Android not implemented yet";

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

    private By titleLocator() {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.accessibilityId(IOS_TITLE);
            case ANDROID -> throw new UnsupportedOperationException(ANDROID_TODO);
        };
    }

    private By digitLocator(int digit) {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.accessibilityId(String.valueOf(digit));
            case ANDROID -> throw new UnsupportedOperationException(ANDROID_TODO);
        };
    }

    private By backspaceLocator() {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.accessibilityId(IOS_BACKSPACE);
            case ANDROID -> throw new UnsupportedOperationException(ANDROID_TODO);
        };
    }
}
