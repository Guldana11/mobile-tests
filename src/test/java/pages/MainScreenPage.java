package pages;

import core.Platform;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * The main (home) screen shown after a successful PIN setup. Its defining element is the bottom tab
 * bar with "Главная" / "Продукты" / "Быстрое меню", plus a "Привет, &lt;name&gt;" greeting and the
 * account list.
 *
 * <p>On iOS, two onboarding prompts (Face ID, then notifications) appear over the main screen right
 * after the PIN is confirmed — {@link #dismissOnboardingPrompts()} taps their "Позже" buttons. The
 * greeting name is account-specific, so tests assert the "Привет," prefix, not a literal name.
 */
public class MainScreenPage extends BasePage {

    // Bottom tab labels — locale text, identical on the home screen of both platforms.
    private static final String TAB_HOME = "Главная";
    private static final String TAB_PRODUCTS = "Продукты";
    private static final String TAB_QUICK_MENU = "Быстрое меню";
    private static final String GREETING_PREFIX = "Привет,";

    // The last action at the very bottom of the account list — off-screen until the list is scrolled,
    // so it doubles as the "did the list scroll" marker.
    private static final String BOTTOM_ACTION = "Открыть депозит";

    private static final String IOS_TAB_BAR = "Панель вкладок";
    private static final String IOS_LATER_BUTTON = "Позже";

    public MainScreenPage(AppiumDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        return waitForDisplayed(Duration.ofSeconds(30));
    }

    public boolean waitForDisplayed(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(
                    ExpectedConditions.visibilityOfElementLocated(homeTabLocator()));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** True if the bottom navigation exposes all three expected tabs. */
    public boolean hasExpectedTabs() {
        return !driver.findElements(tabLocator(TAB_HOME)).isEmpty()
                && !driver.findElements(tabLocator(TAB_PRODUCTS)).isEmpty()
                && !driver.findElements(tabLocator(TAB_QUICK_MENU)).isEmpty();
    }

    /** True if the "Привет, &lt;name&gt;" greeting is present. */
    public boolean hasGreeting() {
        return !driver.findElements(greetingLocator()).isEmpty();
    }

    /** True if the account list rendered at least one account balance (a currency amount). */
    public boolean hasAccountBalances() {
        return !driver.findElements(balanceLocator()).isEmpty();
    }

    /**
     * True if the bottom-most account action ("Открыть депозит") is currently on-screen. It starts
     * below the fold, so this is false until the account list is scrolled down (see
     * {@link #scrollAccountsDown()}).
     */
    public boolean isBottomActionVisible() {
        List<WebElement> els = driver.findElements(bottomActionLocator());
        return !els.isEmpty() && "true".equals(els.get(0).getAttribute("visible"));
    }

    /** Scrolls the account list down with two upward swipes to bring the bottom actions into view. */
    public void scrollAccountsDown() {
        swipeUp();
        swipeUp();
    }

    private void swipeUp() {
        Dimension size = driver.manage().window().getSize();
        int x = size.getWidth() / 2;
        int startY = (int) (size.getHeight() * 0.80);
        int endY = (int) (size.getHeight() * 0.30);
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 1)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger.createPointerMove(Duration.ofMillis(600), PointerInput.Origin.viewport(), x, endY))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(swipe));
    }

    /**
     * Dismisses the post-PIN onboarding prompts (Face ID, then notifications) by tapping their
     * "Позже" (Later) button. Each prompt is a separate full-screen overlay that appears a few
     * seconds after the previous step, so every round WAITS up to 10s for the next "Позже" to show;
     * the loop ends as soon as a round times out with no prompt (the final wait is the stop signal).
     */
    public void dismissOnboardingPrompts() {
        By later = laterButtonLocator();
        for (int round = 0; round < 3; round++) {
            try {
                new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(ExpectedConditions.elementToBeClickable(later))
                        .click();
            } catch (Exception e) {
                return;  // no more prompts appeared within the timeout
            }
        }
    }

    private By homeTabLocator() {
        return tabLocator(TAB_HOME);
    }

    private By tabLocator(String label) {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.accessibilityId(label);
            // Android home-screen locators are filled in during the Android phase.
            case ANDROID -> throw new UnsupportedOperationException(
                    "Android main screen not characterised yet");
        };
    }

    private By greetingLocator() {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.iOSNsPredicateString("label BEGINSWITH '" + GREETING_PREFIX + "'");
            case ANDROID -> throw new UnsupportedOperationException(
                    "Android main screen not characterised yet");
        };
    }

    // Account balances are rendered with a currency suffix (₸ / $). Matching the symbol is locale- and
    // account-agnostic, so it survives a different test account or balance values.
    private By balanceLocator() {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.iOSNsPredicateString("label CONTAINS '₸' OR label CONTAINS '$'");
            case ANDROID -> throw new UnsupportedOperationException(
                    "Android main screen not characterised yet");
        };
    }

    private By bottomActionLocator() {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.accessibilityId(BOTTOM_ACTION);
            case ANDROID -> throw new UnsupportedOperationException(
                    "Android main screen not characterised yet");
        };
    }

    private By laterButtonLocator() {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.accessibilityId(IOS_LATER_BUTTON);
            case ANDROID -> throw new UnsupportedOperationException(
                    "Android main screen not characterised yet");
        };
    }
}
