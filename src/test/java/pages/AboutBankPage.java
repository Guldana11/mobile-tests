package pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class AboutBankPage extends BasePage {

    private static final String BACK_ID = "kz.bnk.app.dev:id/iv_back";
    private static final String SCROLL_VIEW_ID = "kz.bnk.app.dev:id/nested_scroll_view";
    private static final String SCREEN_TITLE = "О Банке";

    public AboutBankPage(AppiumDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        return waitForDisplayed(Duration.ofSeconds(10));
    }

    public boolean waitForDisplayed(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(
                    ExpectedConditions.visibilityOfElementLocated(AppiumBy.id(SCROLL_VIEW_ID))
            );
            return hasText(SCREEN_TITLE);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasSection(String sectionTitle) {
        return hasText(sectionTitle);
    }

    public void tapBack() {
        driver.findElement(AppiumBy.id(BACK_ID)).click();
    }

    private boolean hasText(String text) {
        String selector = String.format("new UiSelector().text(\"%s\")", text);
        return !driver.findElements(AppiumBy.androidUIAutomator(selector)).isEmpty();
    }
}
