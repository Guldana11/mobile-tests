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
 * The SMS-code (OTP) screen of the password-recovery flow, reached by tapping "Забыли пароль?" on
 * the password screen. Title "Введите код", a description "Код был отправлен на номер ...", a resend
 * timer ("Отправить новый код после 0:XX") and a 5-digit code field. Entering the dev test code
 * submits and triggers video identification (a camera/record-video permission request).
 *
 * <p>Android-only: the iOS recovery flow is not characterised (see {@link PasswordPage}).
 */
public class PasswordRecoveryPage extends BasePage {

    private static final String TITLE_ID = "kz.bnk.app.dev:id/tv_title";
    private static final String DESCRIPTION_ID = "kz.bnk.app.dev:id/tv_description";
    private static final String TIMER_ID = "kz.bnk.app.dev:id/tv_timer";
    private static final String CODE_FIELD_ID = "kz.bnk.app.dev:id/codem";
    private static final String TITLE_TEXT = "Введите код";

    public PasswordRecoveryPage(AppiumDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        return waitForDisplayed(Duration.ofSeconds(20));
    }

    public boolean waitForDisplayed(Duration timeout) {
        requireAndroid();
        try {
            new WebDriverWait(driver, timeout).until(
                    ExpectedConditions.visibilityOfElementLocated(titleLocator()));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getDescription() {
        requireAndroid();
        return driver.findElement(AppiumBy.id(DESCRIPTION_ID)).getText();
    }

    public boolean hasResendTimer() {
        requireAndroid();
        return !driver.findElements(AppiumBy.id(TIMER_ID)).isEmpty();
    }

    public boolean hasCodeField() {
        requireAndroid();
        return !driver.findElements(AppiumBy.id(CODE_FIELD_ID)).isEmpty();
    }

    /** Types the code into the OTP field. The dev test code submits automatically once complete. */
    public void enterCode(String code) {
        requireAndroid();
        WebElement field = driver.findElement(AppiumBy.id(CODE_FIELD_ID));
        field.click();
        field.sendKeys(code);
    }

    private By titleLocator() {
        return AppiumBy.androidUIAutomator(
                "new UiSelector().resourceId(\"" + TITLE_ID + "\").text(\"" + TITLE_TEXT + "\")");
    }

    private void requireAndroid() {
        if (Platform.current() != Platform.ANDROID) {
            throw new UnsupportedOperationException(
                    "PasswordRecoveryPage is Android-only — iOS recovery flow not characterised");
        }
    }
}
