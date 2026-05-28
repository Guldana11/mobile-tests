package pages;

import core.Platform;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class PdfDocumentPage extends BasePage {

    private static final String TOOLBAR_TITLE_ID = "kz.bnk.app.dev:id/title";
    private static final String BACK_ID = "kz.bnk.app.dev:id/v_back";
    private static final String PDF_VIEW_ID = "kz.bnk.app.dev:id/pdfView";
    private static final String PAGE_VIEW_ID = "kz.bnk.app.dev:id/pageView";
    private static final String ACCEPT_BUTTON_ID = "kz.bnk.app.dev:id/btn";

    // iOS renders the document inside a WebView; the screen title sits in the NavigationBar and
    // the back control is the shared "BackButton" accessibility id. This build's iOS PDF viewer
    // has NO Accept button (Android-only), so hasAcceptButton() returns false on iOS.
    private static final String IOS_BACK_BUTTON = "BackButton";

    public PdfDocumentPage(AppiumDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        return waitForDisplayed(Duration.ofSeconds(15));
    }

    public boolean waitForDisplayed(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(
                    ExpectedConditions.visibilityOfElementLocated(pdfContentLocator())
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getTitle() {
        return switch (Platform.current()) {
            case ANDROID -> driver.findElement(AppiumBy.id(TOOLBAR_TITLE_ID)).getText();
            case IOS -> driver.findElement(AppiumBy.className("XCUIElementTypeNavigationBar"))
                    .getAttribute("name");
        };
    }

    public boolean hasPdfPages() {
        return switch (Platform.current()) {
            case ANDROID -> !driver.findElements(AppiumBy.id(PAGE_VIEW_ID)).isEmpty();
            case IOS -> !driver.findElements(AppiumBy.className("XCUIElementTypeWebView")).isEmpty();
        };
    }

    public boolean hasAcceptButton() {
        return switch (Platform.current()) {
            case ANDROID -> !driver.findElements(AppiumBy.id(ACCEPT_BUTTON_ID)).isEmpty();
            // No Accept button on iOS in this build — assertions on it are Android-only.
            case IOS -> false;
        };
    }

    public void tapBack() {
        switch (Platform.current()) {
            case ANDROID -> driver.findElement(AppiumBy.id(BACK_ID)).click();
            case IOS -> driver.findElement(AppiumBy.accessibilityId(IOS_BACK_BUTTON)).click();
        }
    }

    private By pdfContentLocator() {
        return switch (Platform.current()) {
            case ANDROID -> AppiumBy.id(PDF_VIEW_ID);
            case IOS -> AppiumBy.className("XCUIElementTypeWebView");
        };
    }
}
