package pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

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
        return driver.findElement(AppiumBy.id(MESSAGE_ID)).getText();
    }

    public boolean isPreciseSelectedByDefault() {
        return Boolean.parseBoolean(
                driver.findElement(AppiumBy.id(PRECISE_RADIO_ID)).getAttribute("checked")
        );
    }

    public boolean hasApproximateOption() {
        return !driver.findElements(AppiumBy.id(APPROXIMATE_RADIO_ID)).isEmpty();
    }

    public void tapWhileUsingApp() {
        driver.findElement(AppiumBy.id(ALLOW_FOREGROUND_ID)).click();
    }

    public void tapOnlyThisTime() {
        driver.findElement(AppiumBy.id(ALLOW_ONE_TIME_ID)).click();
    }

    public void tapDontAllow() {
        driver.findElement(AppiumBy.id(DENY_ID)).click();
    }

    public void acceptIfPresent() {
        if (isDisplayed()) {
            tapWhileUsingApp();
        }
    }
}
