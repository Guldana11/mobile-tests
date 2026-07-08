package pages;

import core.Platform;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * The profile screen opened by tapping the header avatar (see {@link MainScreenPage#openProfile()}).
 * Shows the user's full name and phone, three rows ("Персональная информация", "Номер телефона",
 * "Настройки") and the build version. "Настройки" opens an in-profile settings list with
 * "Изменить ПИН-код" / "Изменить пароль" / "Язык"; "Персональная информация" is a not-yet-built stub.
 *
 * <p>Read-only helpers only — the tests never change the PIN/password/language (that would break the
 * shared test account). Cross-platform: rows are locale text; the back control differs by platform.
 */
public class ProfilePage extends BasePage {

    private static final String ANDROID_BACK_ID = "kz.bnk.app.dev:id/v_back";
    private static final String IOS_BACK = "BackButton";
    // iOS profile rows are buttons with these accessibilityIds (Android taps the row by text).
    private static final String IOS_ROW_PERSONAL = "Profile/info";
    private static final String IOS_ROW_SETTINGS = "Profile/settings";

    public ProfilePage(AppiumDriver driver) {
        super(driver);
    }

    /** True once the profile screen is shown (its "Персональная информация" row is a stable marker). */
    public boolean isDisplayed() {
        return showsText("Персональная информация");
    }

    /** True if any visible element's text/label CONTAINS the substring (waits up to 10s). */
    public boolean showsText(String substring) {
        return waitVisible(textContainsLocator(substring), Duration.ofSeconds(10));
    }

    /** Immediate (no-wait) presence check — for absence assertions once the screen is loaded. */
    public boolean isTextPresent(String substring) {
        return !driver.findElements(textContainsLocator(substring)).isEmpty();
    }

    /** Taps a profile row / settings action by its visible label (Android; iOS uses the *Row methods). */
    public void openRow(String label) {
        driver.findElement(textContainsLocator(label)).click();
    }

    /** Opens the "Персональная информация" row (iOS taps the Profile/info button; Android taps the text). */
    public void openPersonalInfo() {
        driver.findElement(rowLocator("Персональная информация", IOS_ROW_PERSONAL)).click();
    }

    /** Opens the in-profile "Настройки" row (iOS taps the Profile/settings button; Android taps the text). */
    public void openSettings() {
        driver.findElement(rowLocator("Настройки", IOS_ROW_SETTINGS)).click();
    }

    private By rowLocator(String androidText, String iosAccessibilityId) {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.accessibilityId(iosAccessibilityId);
            case ANDROID -> textContainsLocator(androidText);
        };
    }

    /** Taps the toolbar back arrow to return to the previous screen. */
    public void goBack() {
        driver.findElement(backLocator()).click();
    }

    private By backLocator() {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.accessibilityId(IOS_BACK);
            case ANDROID -> AppiumBy.id(ANDROID_BACK_ID);
        };
    }

    private By textContainsLocator(String text) {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.iOSNsPredicateString(
                    "label CONTAINS '" + text + "' OR name CONTAINS '" + text + "'");
            case ANDROID -> AppiumBy.androidUIAutomator(
                    "new UiSelector().textContains(\"" + text + "\")");
        };
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
