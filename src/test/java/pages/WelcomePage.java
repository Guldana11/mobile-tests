package pages;

import core.Platform;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class WelcomePage extends BasePage {

    private static final String ANDROID_START_BUTTON_ID = "kz.bnk.app.dev:id/btn";
    private static final String ANDROID_BRANCHES_CARD_ID = "kz.bnk.app.dev:id/mcv_branch";
    private static final String ANDROID_MORE_LINK_ID = "kz.bnk.app.dev:id/tv_more";
    private static final String ANDROID_INFO_CARD_ID = "kz.bnk.app.dev:id/mcv_info_first";

    // iOS uses accessibility ids that mirror the button label. The info card has no single
    // wrapper element — we anchor on the bank logo image inside it (locale-independent).
    private static final String IOS_START_BUTTON = "Начать";
    private static final String IOS_BRANCHES_BUTTON = "Филиалы";
    private static final String IOS_MORE_BUTTON = "Еще";
    private static final String IOS_INFO_LOGO = "Common/logo";

    public WelcomePage(AppiumDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        return waitForDisplayed(Duration.ofSeconds(45));
    }

    public boolean waitForDisplayed(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(
                    ExpectedConditions.visibilityOfElementLocated(startButton())
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void tapStart() {
        driver.findElement(startButton()).click();
    }

    public void tapBranches() {
        driver.findElement(branchesCard()).click();
    }

    public void tapMore() {
        driver.findElement(moreLink()).click();
    }

    public boolean hasInfoCard() {
        return !driver.findElements(infoCard()).isEmpty();
    }

    public boolean hasBranchesCard() {
        return !driver.findElements(branchesCard()).isEmpty();
    }

    private By startButton() {
        return switch (Platform.current()) {
            case ANDROID -> AppiumBy.id(ANDROID_START_BUTTON_ID);
            case IOS -> AppiumBy.accessibilityId(IOS_START_BUTTON);
        };
    }

    private By branchesCard() {
        return switch (Platform.current()) {
            case ANDROID -> AppiumBy.id(ANDROID_BRANCHES_CARD_ID);
            case IOS -> AppiumBy.accessibilityId(IOS_BRANCHES_BUTTON);
        };
    }

    private By moreLink() {
        return switch (Platform.current()) {
            case ANDROID -> AppiumBy.id(ANDROID_MORE_LINK_ID);
            case IOS -> AppiumBy.accessibilityId(IOS_MORE_BUTTON);
        };
    }

    private By infoCard() {
        return switch (Platform.current()) {
            case ANDROID -> AppiumBy.id(ANDROID_INFO_CARD_ID);
            case IOS -> AppiumBy.accessibilityId(IOS_INFO_LOGO);
        };
    }
}
