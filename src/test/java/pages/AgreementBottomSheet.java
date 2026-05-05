package pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class AgreementBottomSheet extends BasePage {

    private static final String SHEET_ID = "kz.bnk.app.dev:id/design_bottom_sheet";
    private static final String TITLE_ID = "kz.bnk.app.dev:id/tv_title";
    private static final String TERMS_ITEM_ID = "kz.bnk.app.dev:id/ll_terms_of_agreements";
    private static final String TERMS_TEXT_ID = "kz.bnk.app.dev:id/tv_terms_of_agreements";
    private static final String CONSENT_ITEM_ID = "kz.bnk.app.dev:id/ll_personal_data_consent";
    private static final String CONSENT_TEXT_ID = "kz.bnk.app.dev:id/tv_personal_data_consent";
    private static final String TOUCH_OUTSIDE_ID = "kz.bnk.app.dev:id/touch_outside";

    public AgreementBottomSheet(AppiumDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        return waitForDisplayed(Duration.ofSeconds(10));
    }

    public boolean waitForDisplayed(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(
                    ExpectedConditions.visibilityOfElementLocated(AppiumBy.id(SHEET_ID))
            );
            return "Условия Банка".equals(getTitle());
        } catch (Exception e) {
            return false;
        }
    }

    public String getTitle() {
        return driver.findElement(AppiumBy.id(TITLE_ID)).getText();
    }

    public boolean hasTermsOfAgreementsItem() {
        WebElement el = first(TERMS_TEXT_ID);
        return el != null && "Условия соглашений".equals(el.getText());
    }

    public boolean hasPersonalDataConsentItem() {
        WebElement el = first(CONSENT_TEXT_ID);
        return el != null && el.getText().contains("Согласие");
    }

    public void tapTermsOfAgreements() {
        driver.findElement(AppiumBy.id(TERMS_ITEM_ID)).click();
    }

    public void tapPersonalDataConsent() {
        driver.findElement(AppiumBy.id(CONSENT_ITEM_ID)).click();
    }

    public void dismissByTappingOutside() {
        driver.findElement(AppiumBy.id(TOUCH_OUTSIDE_ID)).click();
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.invisibilityOfElementLocated(AppiumBy.id(SHEET_ID)));
    }

    private WebElement first(String id) {
        var list = driver.findElements(AppiumBy.id(id));
        return list.isEmpty() ? null : list.get(0);
    }
}
