package pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class PdfDocumentPage extends BasePage {

    private static final String TOOLBAR_TITLE_ID = "kz.bnk.app.dev:id/title";
    private static final String BACK_ID = "kz.bnk.app.dev:id/v_back";
    private static final String PDF_VIEW_ID = "kz.bnk.app.dev:id/pdfView";
    private static final String PAGE_VIEW_ID = "kz.bnk.app.dev:id/pageView";
    private static final String ACCEPT_BUTTON_ID = "kz.bnk.app.dev:id/btn";

    public PdfDocumentPage(AppiumDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        return waitForDisplayed(Duration.ofSeconds(15));
    }

    public boolean waitForDisplayed(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(
                    ExpectedConditions.visibilityOfElementLocated(AppiumBy.id(PDF_VIEW_ID))
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getTitle() {
        return driver.findElement(AppiumBy.id(TOOLBAR_TITLE_ID)).getText();
    }

    public boolean hasPdfPages() {
        return !driver.findElements(AppiumBy.id(PAGE_VIEW_ID)).isEmpty();
    }

    public boolean hasAcceptButton() {
        return !driver.findElements(AppiumBy.id(ACCEPT_BUTTON_ID)).isEmpty();
    }

    public void tapBack() {
        driver.findElement(AppiumBy.id(BACK_ID)).click();
    }
}
