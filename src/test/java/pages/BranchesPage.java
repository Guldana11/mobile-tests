package pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class BranchesPage extends BasePage {

    private static final String TITLE_ID = "kz.bnk.app.dev:id/title";
    private static final String MAP_ID = "kz.bnk.app.dev:id/map";
    private static final String BACK_ID = "kz.bnk.app.dev:id/v_back";
    private static final String SHARE_ID = "kz.bnk.app.dev:id/v_share";
    private static final String BRANCH_ITEM_ID = "kz.bnk.app.dev:id/ll_parent";
    private static final String BRANCH_TITLE_ID = "kz.bnk.app.dev:id/tv_point_title";

    public BranchesPage(AppiumDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        return waitForDisplayed(Duration.ofSeconds(15));
    }

    public boolean waitForDisplayed(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(
                    ExpectedConditions.visibilityOfElementLocated(AppiumBy.id(TITLE_ID))
            );
            return "Филиалы".equals(driver.findElement(AppiumBy.id(TITLE_ID)).getText());
        } catch (Exception e) {
            return false;
        }
    }

    public boolean hasMap() {
        return !driver.findElements(AppiumBy.id(MAP_ID)).isEmpty();
    }

    public int getBranchCount() {
        return driver.findElements(AppiumBy.id(BRANCH_ITEM_ID)).size();
    }

    public String getFirstBranchTitle() {
        List<WebElement> titles = driver.findElements(AppiumBy.id(BRANCH_TITLE_ID));
        return titles.isEmpty() ? null : titles.get(0).getText();
    }

    public void tapBack() {
        driver.findElement(AppiumBy.id(BACK_ID)).click();
    }

    public void tapShare() {
        driver.findElement(AppiumBy.id(SHARE_ID)).click();
    }
}
