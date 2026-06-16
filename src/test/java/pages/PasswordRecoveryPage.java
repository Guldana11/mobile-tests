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
 * timer ("Отправить новый код после 0:XX") and a code field. Entering the dev test code submits and
 * triggers video identification.
 *
 * <p>Cross-platform. The "video identification was reached" signal differs per platform:
 * <ul>
 *   <li><b>Android</b>: the flow requests the camera/record-video system permission — see
 *       {@link PermissionDialog#isVideoRecordingRequestShown(Duration)}.</li>
 *   <li><b>iOS</b>: the camera permission is auto-granted ({@code autoAcceptAlerts}), then video
 *       recording fails on the simulator (no camera) and the app raises an "Ошибка / Не удается
 *       выполнить запись" alert. That recording-failure alert is the proof the video-id step was
 *       reached. It is short-lived (autoAcceptAlerts dismisses it within a few seconds), so
 *       {@link #triggersVideoIdentification(Duration)} polls fast with the implicit wait disabled.</li>
 * </ul>
 * The iOS code screen loads slower than Android (~10s behind a loading overlay), hence the longer
 * {@link #isDisplayed()} timeout.
 */
public class PasswordRecoveryPage extends BasePage {

    private static final String TITLE_ID = "kz.bnk.app.dev:id/tv_title";
    private static final String DESCRIPTION_ID = "kz.bnk.app.dev:id/tv_description";
    private static final String TIMER_ID = "kz.bnk.app.dev:id/tv_timer";
    private static final String CODE_FIELD_ID = "kz.bnk.app.dev:id/codem";
    private static final String TITLE_TEXT = "Введите код";

    // iOS labels (accessibility ids). Description and timer carry a dynamic tail (phone number /
    // countdown), so they are matched by prefix.
    private static final String IOS_DESC_PREFIX = "Код был отправлен";
    private static final String IOS_TIMER_PREFIX = "Отправить новый код";
    private static final String IOS_RECORDING_ERROR = "Не удается выполнить запись";

    public PasswordRecoveryPage(AppiumDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        // iOS loads this screen behind a ~10s loading overlay; give it room.
        return waitForDisplayed(Duration.ofSeconds(30));
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

    public String getDescription() {
        return switch (Platform.current()) {
            case ANDROID -> driver.findElement(AppiumBy.id(DESCRIPTION_ID)).getText();
            case IOS -> driver.findElement(iosPrefix(IOS_DESC_PREFIX)).getText();
        };
    }

    public boolean hasResendTimer() {
        return switch (Platform.current()) {
            case ANDROID -> !driver.findElements(AppiumBy.id(TIMER_ID)).isEmpty();
            case IOS -> !driver.findElements(iosPrefix(IOS_TIMER_PREFIX)).isEmpty();
        };
    }

    public boolean hasCodeField() {
        return switch (Platform.current()) {
            case ANDROID -> !driver.findElements(AppiumBy.id(CODE_FIELD_ID)).isEmpty();
            // iOS backs the digit cells with a single (visually hidden) text field.
            case IOS -> !driver.findElements(AppiumBy.className("XCUIElementTypeTextField")).isEmpty();
        };
    }

    /** Types the code into the OTP field. The dev test code submits automatically once complete. */
    public void enterCode(String code) {
        WebElement field = switch (Platform.current()) {
            case ANDROID -> driver.findElement(AppiumBy.id(CODE_FIELD_ID));
            case IOS -> driver.findElement(AppiumBy.className("XCUIElementTypeTextField"));
        };
        field.click();
        field.sendKeys(code);
    }

    /**
     * True if, after a valid code, the flow reaches video identification within {@code timeout}:
     * the camera/record-video permission request on Android, or the simulator recording-failure
     * alert on iOS (see class javadoc).
     */
    public boolean triggersVideoIdentification(Duration timeout) {
        return switch (Platform.current()) {
            case ANDROID -> new PermissionDialog(driver).isVideoRecordingRequestShown(timeout);
            case IOS -> waitForIosRecordingError(timeout);
        };
    }

    // The iOS recording-failure alert is auto-dismissed by autoAcceptAlerts, so poll fast (250ms) with
    // the implicit wait disabled — otherwise each negative findElements would block the full implicit
    // timeout and the short-lived alert would be missed.
    private boolean waitForIosRecordingError(Duration timeout) {
        By error = AppiumBy.iOSNsPredicateString(
                "label CONTAINS '" + IOS_RECORDING_ERROR + "' OR name CONTAINS '" + IOS_RECORDING_ERROR + "'");
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            new WebDriverWait(driver, timeout, Duration.ofMillis(250))
                    .until(d -> !d.findElements(error).isEmpty());
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        }
    }

    private By titleLocator() {
        return switch (Platform.current()) {
            case ANDROID -> AppiumBy.androidUIAutomator(
                    "new UiSelector().resourceId(\"" + TITLE_ID + "\").text(\"" + TITLE_TEXT + "\")");
            case IOS -> AppiumBy.accessibilityId(TITLE_TEXT);
        };
    }

    private By iosPrefix(String prefix) {
        return AppiumBy.iOSNsPredicateString("label BEGINSWITH '" + prefix + "'");
    }
}
