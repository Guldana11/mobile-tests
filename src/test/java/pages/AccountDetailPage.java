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
 * <p><b>Cross-platform.</b> The iOS detail screen was captured on the simulator and differs: it has
 * no "Доступный" label (so iOS identifies the screen by the detail-only "Реквизиты" action), the
 * transfer action reads "Переводы" (plural) instead of "Перевести", and the toolbar back arrow is
 * "chevron.left" (not "BackButton"). The tabs ("История"/"Настройки") match. These differences are
 * handled per-platform below.
 */
public class AccountDetailPage extends BasePage {

    // Available-balance label — present only on the detail screen, not on the account list.
    private static final String AVAILABLE_LABEL = "Доступный";
    private static final String ACTION_TRANSFER = "Перевести";       // Android detail action label
    private static final String IOS_ACTION_TRANSFER = "Переводы";     // iOS detail action label (plural)
    private static final String ACTION_REQUISITES = "Реквизиты";
    private static final String TAB_HISTORY = "История";
    private static final String TAB_SETTINGS = "Настройки";

    private static final String ANDROID_BACK_ID = "kz.bnk.app.dev:id/iv_back";
    private static final String IOS_BACK = "chevron.left";  // the detail screen's toolbar back (not "BackButton")

    public AccountDetailPage(AppiumDriver driver) {
        super(driver);
    }

    /**
     * True once the detail screen is shown. Android has a unique "Доступный" balance label; the iOS
     * build does not, so iOS keys off the detail-only "Реквизиты" action instead.
     */
    public boolean isDisplayed() {
        String marker = switch (Platform.current()) {
            case ANDROID -> AVAILABLE_LABEL;
            case IOS -> ACTION_REQUISITES;
        };
        return waitVisible(textLocator(marker), Duration.ofSeconds(15));
    }

    /** True if both action buttons (transfer and "Реквизиты") are present. */
    public boolean hasActions() {
        String transfer = switch (Platform.current()) {
            case ANDROID -> ACTION_TRANSFER;
            case IOS -> IOS_ACTION_TRANSFER;
        };
        return !driver.findElements(textLocator(transfer)).isEmpty()
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

    /**
     * Taps the transfer action on the detail screen to open the transfer flow, returning the
     * {@link TransferPage}. The label differs by platform: Android "Перевести", iOS "Переводы".
     */
    public TransferPage tapTransfer() {
        String label = switch (Platform.current()) {
            case ANDROID -> ACTION_TRANSFER;
            case IOS -> IOS_ACTION_TRANSFER;
        };
        driver.findElement(textLocator(label)).click();
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
