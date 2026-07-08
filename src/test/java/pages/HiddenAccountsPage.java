package pages;

import core.Platform;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The account-visibility screen opened by the "Скрытые счета" bottom action on the main screen (see
 * {@link MainScreenPage#openHiddenAccounts()}). It lists every account in two sections —
 * "Появляется на главной странице" (shown on the home screen) and "Скрытый" (hidden) — with a per-row
 * hide toggle ({@code btn_hide}) and drag handle ({@code btn_drag}), plus a "Сохранить" button that
 * persists the layout. Reordering / hiding is done by dragging a row between the sections.
 *
 * <p>Read-only helpers here assert the page's structure. Any test that changes visibility must leave
 * WITHOUT tapping Сохранить (unsaved changes are discarded) so the shared account is never mutated.
 */
public class HiddenAccountsPage extends BasePage {

    static final String SHOWN_SECTION = "Появляется на главной странице";
    static final String HIDDEN_SECTION = "Скрытый";
    static final String SAVE_BUTTON = "Сохранить";
    private static final String ANDROID_ALIAS_ID = "kz.bnk.app.dev:id/tv_alias";
    private static final String ANDROID_DRAG_ID = "kz.bnk.app.dev:id/btn_drag";

    public HiddenAccountsPage(AppiumDriver driver) {
        super(driver);
    }

    /** True once the visibility screen is shown (its "Появляется на главной странице" title). */
    public boolean isDisplayed() {
        return showsText(SHOWN_SECTION);
    }

    /** True if the "Скрытый" (hidden) section is present, scrolling the list down to reveal it. */
    public boolean hasHiddenSection() {
        for (int i = 0; i < 5 && !isTextPresent(HIDDEN_SECTION); i++) {
            scrollForward();
        }
        return isTextPresent(HIDDEN_SECTION);
    }

    private void scrollForward() {
        switch (Platform.current()) {
            case ANDROID -> driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true)).scrollForward()"));
            case IOS -> driver.executeScript("mobile: swipe", java.util.Map.of("direction", "up"));
        }
    }

    /** True if the "Сохранить" button is present. */
    public boolean hasSaveButton() {
        return isTextPresent(SAVE_BUTTON);
    }

    /** True if any visible element's text/label CONTAINS the substring (waits up to 10s). */
    public boolean showsText(String substring) {
        return waitVisible(textContainsLocator(substring), Duration.ofSeconds(10));
    }

    /** Immediate (no-wait) presence check. */
    public boolean isTextPresent(String substring) {
        return !driver.findElements(textContainsLocator(substring)).isEmpty();
    }

    /** The account aliases (names) currently rendered on the page, top to bottom (Android). */
    public List<String> aliasesInOrder() {
        List<String> names = new ArrayList<>();
        for (WebElement e : driver.findElements(AppiumBy.id(ANDROID_ALIAS_ID))) {
            names.add(e.getText());
        }
        return names;
    }

    /**
     * Drags the row at {@code fromIndex} onto the position of the row at {@code toIndex} using its drag
     * handle ({@code btn_drag}) — a long-press to pick the row up, then a slow drag and settle before
     * release. Android only (the handle has a resource-id; iOS reordering is not characterised).
     */
    public void dragRow(int fromIndex, int toIndex) {
        List<WebElement> handles = driver.findElements(AppiumBy.id(ANDROID_DRAG_ID));
        Rectangle from = handles.get(fromIndex).getRect();
        Rectangle to = handles.get(toIndex).getRect();
        dragAndDrop(from.getX() + from.getWidth() / 2, from.getY() + from.getHeight() / 2,
                to.getX() + to.getWidth() / 2, to.getY() + to.getHeight() / 2);
    }

    private void dragAndDrop(int x1, int y1, int x2, int y2) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence drag = new Sequence(finger, 1)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x1, y1))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger.createPointerMove(Duration.ofMillis(1000), PointerInput.Origin.viewport(), x1, y1))
                .addAction(finger.createPointerMove(Duration.ofMillis(1600), PointerInput.Origin.viewport(), x2, y2))
                .addAction(finger.createPointerMove(Duration.ofMillis(400), PointerInput.Origin.viewport(), x2, y2))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(drag));
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
