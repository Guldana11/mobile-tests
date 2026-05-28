package pages;

import core.Platform;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Login password step. On iOS this is the same screen as {@link PhoneLoginPage}, with the masked
 * password field revealed after the phone number is submitted. The password is NOT validated by
 * format client-side — tapping "Продолжить" sends it to the server, which either opens the PIN
 * creation screen (correct) or raises a native "Ошибка / Неверные данные для входа" alert (wrong).
 */
public class PasswordPage extends BasePage {

    // iOS: the password field is the screen's only SecureTextField; the submit and "forgot
    // password" controls expose their labels as accessibility ids; a wrong password raises a
    // native alert whose message is "Неверные данные для входа".
    private static final String IOS_FORGOT_PASSWORD = "Забыли пароль?";
    private static final String IOS_CONTINUE = "Продолжить";
    private static final String IOS_WRONG_CREDENTIALS = "Неверные данные для входа";
    private static final String IOS_ALERT_OK = "OK";

    private static final String ANDROID_TODO = "PasswordPage: Android not implemented yet";

    public PasswordPage(AppiumDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        return waitForDisplayed(Duration.ofSeconds(20));
    }

    public boolean waitForDisplayed(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(
                    ExpectedConditions.visibilityOfElementLocated(passwordFieldLocator()));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void enterPassword(String password) {
        WebElement field = driver.findElement(passwordFieldLocator());
        field.click();
        ensureLatinKeyboard();
        field.sendKeys(password);
    }

    /**
     * The simulator's on-screen keyboard defaults to Kazakh/Russian (Cyrillic), which has no Latin
     * letters — typing a Latin password over it drops every letter. Cycle the globe key to the
     * English layout (it follows Russian in the rotation) so sendKeys can type the password.
     */
    private void ensureLatinKeyboard() {
        if (Platform.current() != Platform.IOS) return;
        By latinKey = AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeKey' AND name == 'q'");
        By nextKeyboard = AppiumBy.accessibilityId("Следующая клавиатура");
        for (int i = 0; i < 5; i++) {
            if (!driver.findElements(latinKey).isEmpty()) return;
            var globe = driver.findElements(nextKeyboard);
            if (globe.isEmpty()) return;
            globe.get(0).click();
        }
    }

    public void tapContinue() {
        driver.findElement(continueLocator()).click();
    }

    public boolean isContinueEnabled() {
        return driver.findElement(continueLocator()).isEnabled();
    }

    public boolean hasForgotPassword() {
        return !driver.findElements(forgotPasswordLocator()).isEmpty();
    }

    /**
     * Best-effort check for the wrong-credentials alert. Note: with {@code autoAcceptAlerts=true}
     * the alert may be auto-dismissed before this runs, so tests should not rely on it as their
     * only signal — assert that the PIN screen did NOT open instead.
     */
    public boolean isWrongCredentialsErrorShown(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(
                    ExpectedConditions.visibilityOfElementLocated(wrongCredentialsLocator()));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void dismissErrorIfPresent() {
        var okButtons = driver.findElements(AppiumBy.accessibilityId(IOS_ALERT_OK));
        if (!okButtons.isEmpty()) {
            okButtons.get(0).click();
        }
    }

    private By passwordFieldLocator() {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.className("XCUIElementTypeSecureTextField");
            case ANDROID -> throw new UnsupportedOperationException(ANDROID_TODO);
        };
    }

    private By continueLocator() {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.accessibilityId(IOS_CONTINUE);
            case ANDROID -> throw new UnsupportedOperationException(ANDROID_TODO);
        };
    }

    private By forgotPasswordLocator() {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.accessibilityId(IOS_FORGOT_PASSWORD);
            case ANDROID -> throw new UnsupportedOperationException(ANDROID_TODO);
        };
    }

    private By wrongCredentialsLocator() {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.accessibilityId(IOS_WRONG_CREDENTIALS);
            case ANDROID -> throw new UnsupportedOperationException(ANDROID_TODO);
        };
    }
}
