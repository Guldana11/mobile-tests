package pages;

import core.Platform;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class AboutBankPage extends BasePage {

    private static final String ANDROID_BACK_ID = "kz.bnk.app.dev:id/iv_back";
    private static final String ANDROID_SCROLL_VIEW_ID = "kz.bnk.app.dev:id/nested_scroll_view";

    private static final String SCREEN_TITLE = "О Банке";
    private static final String IOS_BACK_BUTTON = "BackButton";

    public AboutBankPage(AppiumDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        return waitForDisplayed(Duration.ofSeconds(10));
    }

    public boolean waitForDisplayed(Duration timeout) {
        try {
            return switch (Platform.current()) {
                case ANDROID -> {
                    new WebDriverWait(driver, timeout).until(
                            ExpectedConditions.visibilityOfElementLocated(AppiumBy.id(ANDROID_SCROLL_VIEW_ID))
                    );
                    yield hasText(SCREEN_TITLE);
                }
                case IOS -> {
                    new WebDriverWait(driver, timeout).until(
                            ExpectedConditions.visibilityOfElementLocated(iosNavBar())
                    );
                    yield true;
                }
            };
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasSection(String sectionTitle) {
        return switch (Platform.current()) {
            case ANDROID -> hasText(sectionTitle);
            // iOS: each section header is a StaticText whose accessibility id matches the title.
            case IOS -> !driver.findElements(AppiumBy.accessibilityId(sectionTitle)).isEmpty();
        };
    }

    public void tapBack() {
        driver.findElement(backButton()).click();
    }

    private By backButton() {
        return switch (Platform.current()) {
            case ANDROID -> AppiumBy.id(ANDROID_BACK_ID);
            case IOS -> AppiumBy.accessibilityId(IOS_BACK_BUTTON);
        };
    }

    private By iosNavBar() {
        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeNavigationBar' AND name == '" + SCREEN_TITLE + "'");
    }

    private boolean hasText(String text) {
        String selector = String.format("new UiSelector().text(\"%s\")", text);
        return !driver.findElements(AppiumBy.androidUIAutomator(selector)).isEmpty();
    }
}
