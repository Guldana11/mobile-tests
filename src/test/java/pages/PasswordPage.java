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
 * Login password step — the same screen as {@link PhoneLoginPage} with the masked password field
 * revealed after the phone number is submitted. The password is not validated by format: tapping
 * the submit button sends it to the server, which opens the PIN creation screen (correct) or raises
 * a "Ошибка / Неверные данные для входа" alert (wrong). On Android this is an AlertDialog that stays
 * put; on iOS it is a native alert that {@code autoAcceptAlerts} may auto-dismiss. The submit button
 * is gated by a non-empty password on iOS only (always enabled on Android).
 */
public class PasswordPage extends BasePage {

    private static final String ANDROID_PASSWORD_FIELD = "kz.bnk.app.dev:id/et_pwd";
    private static final String ANDROID_AUTH_BUTTON = "kz.bnk.app.dev:id/btn";
    private static final String ANDROID_FORGOT_PASSWORD = "kz.bnk.app.dev:id/forgot_password";

    private static final String IOS_FORGOT_PASSWORD = "Забыли пароль?";
    private static final String IOS_CONTINUE = "Продолжить";

    private static final String WRONG_CREDENTIALS_TEXT = "Неверные данные для входа";

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

    public void tapContinue() {
        driver.findElement(continueLocator()).click();
    }

    public boolean isContinueEnabled() {
        return driver.findElement(continueLocator()).isEnabled();
    }

    public boolean hasForgotPassword() {
        return !driver.findElements(forgotPasswordLocator()).isEmpty();
    }

    public boolean isWrongCredentialsErrorShown(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(
                    ExpectedConditions.visibilityOfElementLocated(wrongCredentialsLocator()));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * iOS only: the simulator's on-screen keyboard defaults to Kazakh/Russian (Cyrillic), which has
     * no Latin letters — typing a Latin password over it drops every letter. Cycle the globe key to
     * the English layout (it follows Russian in the rotation) so sendKeys can type the password.
     * Android types via UiAutomator2 directly, so no switch is needed.
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

    private By passwordFieldLocator() {
        return switch (Platform.current()) {
            case ANDROID -> AppiumBy.id(ANDROID_PASSWORD_FIELD);
            case IOS -> AppiumBy.className("XCUIElementTypeSecureTextField");
        };
    }

    private By continueLocator() {
        return switch (Platform.current()) {
            case ANDROID -> AppiumBy.id(ANDROID_AUTH_BUTTON);
            case IOS -> AppiumBy.accessibilityId(IOS_CONTINUE);
        };
    }

    private By forgotPasswordLocator() {
        return switch (Platform.current()) {
            case ANDROID -> AppiumBy.id(ANDROID_FORGOT_PASSWORD);
            case IOS -> AppiumBy.accessibilityId(IOS_FORGOT_PASSWORD);
        };
    }

    private By wrongCredentialsLocator() {
        return switch (Platform.current()) {
            case ANDROID -> AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + WRONG_CREDENTIALS_TEXT + "\")");
            case IOS -> AppiumBy.accessibilityId(WRONG_CREDENTIALS_TEXT);
        };
    }
}
