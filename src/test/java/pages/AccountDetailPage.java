package pages;

import core.Platform;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * The account-detail screen opened by tapping an account card on the main screen. Its defining
 * elements are the available-balance label ("Доступный"), the action buttons ("Перевести" /
 * "Реквизиты") and the two tabs ("История" / "Настройки"). A back arrow in the toolbar returns to
 * the main screen.
 *
 * <p><b>iOS:</b> locators below are VERIFIED on Android only — the screen has not yet been captured
 * on the simulator, so this page (and {@code AccountDetailTest}) runs in the Android suite only.
 * The text markers ("Доступный", "Перевести", …) are locale labels shared by both platforms and
 * should port directly; the back arrow id needs confirming against an iOS dump before adding to
 * ios.xml.
 */
public class AccountDetailPage extends BasePage {

    // Available-balance label — present only on the detail screen, not on the account list.
    private static final String AVAILABLE_LABEL = "Доступный";
    private static final String ACTION_TRANSFER = "Перевести";
    private static final String ACTION_REQUISITES = "Реквизиты";
    private static final String TAB_HISTORY = "История";
    private static final String TAB_SETTINGS = "Настройки";

    private static final String ANDROID_BACK_ID = "kz.bnk.app.dev:id/iv_back";
    private static final String IOS_BACK = "BackButton";  // verified on iOS sub-screens; this class is Android-only.

    public AccountDetailPage(AppiumDriver driver) {
        super(driver);
    }

    /** True once the detail screen is shown (its unique "Доступный" balance label appears). */
    public boolean isDisplayed() {
        return waitVisible(textLocator(AVAILABLE_LABEL), Duration.ofSeconds(15));
    }

    /** True if both action buttons ("Перевести" and "Реквизиты") are present. */
    public boolean hasActions() {
        return !driver.findElements(textLocator(ACTION_TRANSFER)).isEmpty()
                && !driver.findElements(textLocator(ACTION_REQUISITES)).isEmpty();
    }

    /** True if both tabs ("История" and "Настройки") are present. */
    public boolean hasTabs() {
        return !driver.findElements(textLocator(TAB_HISTORY)).isEmpty()
                && !driver.findElements(textLocator(TAB_SETTINGS)).isEmpty();
    }

    /** Taps the toolbar back arrow to return to the main screen. */
    public void goBack() {
        driver.findElement(backLocator()).click();
    }

    /** Taps the "Перевести" action to open the transfer flow, returning the {@link TransferPage}. */
    public TransferPage tapTransfer() {
        driver.findElement(textLocator(ACTION_TRANSFER)).click();
        return new TransferPage(driver);
    }

    private By backLocator() {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.accessibilityId(IOS_BACK);
            case ANDROID -> AppiumBy.id(ANDROID_BACK_ID);
        };
    }

    // Locates an element by its visible text/label (iOS labels ≈ Android text).
    private By textLocator(String text) {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.iOSNsPredicateString(
                    "label == '" + text + "' OR name == '" + text + "'");
            case ANDROID -> AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + text + "\")");
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
