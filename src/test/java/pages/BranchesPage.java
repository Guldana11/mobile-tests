package pages;

import core.Platform;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class BranchesPage extends BasePage {

    private static final String ANDROID_TITLE_ID = "kz.bnk.app.dev:id/title";
    private static final String ANDROID_MAP_ID = "kz.bnk.app.dev:id/map";
    private static final String ANDROID_BACK_ID = "kz.bnk.app.dev:id/v_back";
    private static final String ANDROID_SHARE_ID = "kz.bnk.app.dev:id/v_share";
    private static final String ANDROID_BRANCH_ITEM_ID = "kz.bnk.app.dev:id/ll_parent";
    private static final String ANDROID_BRANCH_TITLE_ID = "kz.bnk.app.dev:id/tv_point_title";

    private static final String SCREEN_TITLE = "Филиалы";
    private static final String IOS_BACK_BUTTON = "BackButton";
    private static final String IOS_BRANCH_PIN_NAME = "Map/location";
    private static final String IOS_BRANCH_TITLE_PREDICATE =
            "type == 'XCUIElementTypeStaticText' AND value BEGINSWITH 'г.'";

    public BranchesPage(AppiumDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        return waitForDisplayed(Duration.ofSeconds(15));
    }

    public boolean waitForDisplayed(Duration timeout) {
        try {
            return switch (Platform.current()) {
                case ANDROID -> {
                    new WebDriverWait(driver, timeout).until(
                            ExpectedConditions.visibilityOfElementLocated(AppiumBy.id(ANDROID_TITLE_ID))
                    );
                    yield SCREEN_TITLE.equals(driver.findElement(AppiumBy.id(ANDROID_TITLE_ID)).getText());
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

    public boolean hasMap() {
        return switch (Platform.current()) {
            case ANDROID -> !driver.findElements(AppiumBy.id(ANDROID_MAP_ID)).isEmpty();
            case IOS -> !driver.findElements(AppiumBy.className("XCUIElementTypeMap")).isEmpty();
        };
    }

    public int getBranchCount() {
        return switch (Platform.current()) {
            case ANDROID -> driver.findElements(AppiumBy.id(ANDROID_BRANCH_ITEM_ID)).size();
            // Each branch row has its own "Map/location" pin image — counting pins == branches.
            case IOS -> driver.findElements(AppiumBy.accessibilityId(IOS_BRANCH_PIN_NAME)).size();
        };
    }

    public String getFirstBranchTitle() {
        return switch (Platform.current()) {
            case ANDROID -> {
                List<WebElement> titles = driver.findElements(AppiumBy.id(ANDROID_BRANCH_TITLE_ID));
                yield titles.isEmpty() ? null : titles.get(0).getText();
            }
            case IOS -> {
                // Branch addresses are static texts whose value begins with "г." (KZ addresses).
                List<WebElement> rows = driver.findElements(
                        AppiumBy.iOSNsPredicateString(IOS_BRANCH_TITLE_PREDICATE));
                yield rows.isEmpty() ? null : rows.get(0).getText();
            }
        };
    }

    public void tapBack() {
        driver.findElement(backButton()).click();
    }

    public void tapShare() {
        // Android only — the iOS Branches screen has no share button in this build.
        if (Platform.current() != Platform.ANDROID) {
            throw new UnsupportedOperationException("Share is not available on iOS");
        }
        driver.findElement(AppiumBy.id(ANDROID_SHARE_ID)).click();
    }

    private By backButton() {
        return switch (Platform.current()) {
            case ANDROID -> AppiumBy.id(ANDROID_BACK_ID);
            case IOS -> AppiumBy.accessibilityId(IOS_BACK_BUTTON);
        };
    }

    private By iosNavBar() {
        // Match the NavigationBar by its name — title StaticText also carries the same name,
        // but the nav bar is more stable to wait for as a "screen-is-here" anchor.
        return AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeNavigationBar' AND name == '" + SCREEN_TITLE + "'");
    }
}
