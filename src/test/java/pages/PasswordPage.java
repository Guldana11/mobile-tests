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
    // After several wrong attempts the server locks the account, swapping the wrong-credentials
    // alert for "Вход заблокирован до [dd.MM.yyyy HH:mm:ss]". The timestamp is dynamic, so match the
    // stable prefix only.
    private static final String LOGIN_BLOCKED_PREFIX = "Вход заблокирован";

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
     * iOS only: the simulator's software keyboard defaults to Kazakh/Russian (Cyrillic), which has
     * no Latin letters, so a Latin password types as garbage. Cycle the globe ("Следующая
     * клавиатура") until a Latin-letter key appears (the English layout). Implicit wait is dropped
     * during the loop so the "no Latin yet" checks don't each block for the full implicit timeout,
     * and a short pause lets the keyboard redraw after each switch. Android types via UiAutomator2
     * directly, so no switch is needed. Requires an English keyboard to be installed in the sim.
     */
    private void ensureLatinKeyboard() {
        if (Platform.current() != Platform.IOS) return;
        By latinKey = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeKey' AND name MATCHES '[a-zA-Z]'");
        By globe = AppiumBy.accessibilityId("Следующая клавиатура");
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            for (int i = 0; i < 4; i++) {
                if (!driver.findElements(latinKey).isEmpty()) return;
                var keys = driver.findElements(globe);
                if (keys.isEmpty()) return;
                keys.get(0).click();
                try {
                    Thread.sleep(400);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
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
     * True if the app surfaced a "login rejected" alert: either "Неверные данные для входа" (wrong
     * password) or "Вход заблокирован до [...]" (the server lock that kicks in after repeated wrong
     * attempts). Both confirm the credentials did not log in, so the wrong-password test accepts
     * either rather than flaking once an over-tested account gets locked.
     */
    public boolean isLoginRejectedErrorShown(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(d ->
                    !d.findElements(wrongCredentialsLocator()).isEmpty()
                            || !d.findElements(loginBlockedLocator()).isEmpty());
            return true;
        } catch (Exception e) {
            return false;
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

    private By loginBlockedLocator() {
        return switch (Platform.current()) {
            case ANDROID -> AppiumBy.androidUIAutomator(
                    "new UiSelector().textStartsWith(\"" + LOGIN_BLOCKED_PREFIX + "\")");
            case IOS -> AppiumBy.iOSNsPredicateString(
                    "label BEGINSWITH '" + LOGIN_BLOCKED_PREFIX + "'");
        };
    }
}
