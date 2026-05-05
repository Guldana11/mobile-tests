package pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class WelcomePage extends BasePage {

    private static final String START_BUTTON_ID = "kz.bnk.app.dev:id/btn";
    private static final String BRANCHES_CARD_ID = "kz.bnk.app.dev:id/mcv_branch";
    private static final String MORE_LINK_ID = "kz.bnk.app.dev:id/tv_more";
    private static final String INFO_CARD_ID = "kz.bnk.app.dev:id/mcv_info_first";

    public WelcomePage(AppiumDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        return waitForDisplayed(Duration.ofSeconds(45));
    }

    public boolean waitForDisplayed(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(
                    ExpectedConditions.visibilityOfElementLocated(AppiumBy.id(START_BUTTON_ID))
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void tapStart() {
        startButton().click();
    }

    public void tapBranches() {
        driver.findElement(AppiumBy.id(BRANCHES_CARD_ID)).click();
    }

    public void tapMore() {
        driver.findElement(AppiumBy.id(MORE_LINK_ID)).click();
    }

    public boolean hasInfoCard() {
        return !driver.findElements(AppiumBy.id(INFO_CARD_ID)).isEmpty();
    }

    public boolean hasBranchesCard() {
        return !driver.findElements(AppiumBy.id(BRANCHES_CARD_ID)).isEmpty();
    }

    private WebElement startButton() {
        return driver.findElement(AppiumBy.id(START_BUTTON_ID));
    }
}
