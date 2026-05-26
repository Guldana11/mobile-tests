package pages;

import core.Platform;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Wraps the system location-permission dialog.
 *
 * <p><b>Android</b>: shown by com.android.permissioncontroller after the user taps "Филиалы".
 * <p><b>iOS</b>: shown by the system on app start. We rely on {@code appium:autoAcceptAlerts}
 * to accept it automatically, so all methods are no-ops on iOS. Tests that directly assert
 * dialog properties (message text, accuracy radios, etc.) are Android-only by design.
 */
public class PermissionDialog extends BasePage {

    private static final String PKG = "com.android.permissioncontroller";
    private static final String MESSAGE_ID = PKG + ":id/permission_message";
    private static final String ALLOW_FOREGROUND_ID = PKG + ":id/permission_allow_foreground_only_button";
    private static final String ALLOW_ONE_TIME_ID = PKG + ":id/permission_allow_one_time_button";
    private static final String DENY_ID = PKG + ":id/permission_deny_button";
    private static final String PRECISE_RADIO_ID = PKG + ":id/permission_location_accuracy_radio_fine";
    private static final String APPROXIMATE_RADIO_ID = PKG + ":id/permission_location_accuracy_radio_coarse";

    public PermissionDialog(AppiumDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        return waitForDisplayed(Duration.ofSeconds(5));
    }

    public boolean waitForDisplayed(Duration timeout) {
        if (Platform.current() == Platform.IOS) return false;  // accepted via autoAcceptAlerts
        try {
            new WebDriverWait(driver, timeout).until(
                    ExpectedConditions.visibilityOfElementLocated(AppiumBy.id(MESSAGE_ID))
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getMessage() {
        requireAndroid();
        return driver.findElement(AppiumBy.id(MESSAGE_ID)).getText();
    }

    public boolean isPreciseSelectedByDefault() {
        requireAndroid();
        return Boolean.parseBoolean(
                driver.findElement(AppiumBy.id(PRECISE_RADIO_ID)).getAttribute("checked")
        );
    }

    public boolean hasApproximateOption() {
        requireAndroid();
        return !driver.findElements(AppiumBy.id(APPROXIMATE_RADIO_ID)).isEmpty();
    }

    public void tapWhileUsingApp() {
        requireAndroid();
        driver.findElement(AppiumBy.id(ALLOW_FOREGROUND_ID)).click();
    }

    public void tapOnlyThisTime() {
        requireAndroid();
        driver.findElement(AppiumBy.id(ALLOW_ONE_TIME_ID)).click();
    }

    public void tapDontAllow() {
        requireAndroid();
        driver.findElement(AppiumBy.id(DENY_ID)).click();
    }

    /**
     * Accepts the dialog if it's visible. Safe to call always: on Android it allows location
     * if the dialog is showing, on iOS it's a no-op because autoAcceptAlerts handles it.
     */
    public void acceptIfPresent() {
        if (Platform.current() == Platform.IOS) return;
        if (isDisplayed()) {
            tapWhileUsingApp();
        }
    }

    private void requireAndroid() {
        if (Platform.current() != Platform.ANDROID) {
            throw new UnsupportedOperationException(
                    "PermissionDialog assertions are Android-only — iOS uses autoAcceptAlerts");
        }
    }
}
