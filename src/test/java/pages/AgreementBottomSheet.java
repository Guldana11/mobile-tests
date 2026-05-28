package pages;

import core.Platform;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Collections;

public class AgreementBottomSheet extends BasePage {

    private static final String SHEET_ID = "kz.bnk.app.dev:id/design_bottom_sheet";
    private static final String TITLE_ID = "kz.bnk.app.dev:id/tv_title";
    private static final String TERMS_ITEM_ID = "kz.bnk.app.dev:id/ll_terms_of_agreements";
    private static final String TERMS_TEXT_ID = "kz.bnk.app.dev:id/tv_terms_of_agreements";
    private static final String CONSENT_ITEM_ID = "kz.bnk.app.dev:id/ll_personal_data_consent";
    private static final String CONSENT_TEXT_ID = "kz.bnk.app.dev:id/tv_personal_data_consent";
    private static final String TOUCH_OUTSIDE_ID = "kz.bnk.app.dev:id/touch_outside";

    // iOS renders the sheet items as buttons whose accessibility id == the visible label.
    // Note the title casing differs from Android: iOS shows "Условия банка" (lowercase б).
    private static final String IOS_TITLE = "Условия банка";
    private static final String IOS_TERMS_ITEM = "Условия соглашений";
    private static final String IOS_CONSENT_PREDICATE =
            "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'Согласие'";

    private static final String EXPECTED_TITLE = "Условия Банка";

    public AgreementBottomSheet(AppiumDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        return waitForDisplayed(Duration.ofSeconds(10));
    }

    public boolean waitForDisplayed(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(
                    ExpectedConditions.visibilityOfElementLocated(termsItemLocator())
            );
            // Title casing differs across platforms ("Условия Банка" vs "Условия банка").
            return getTitle().equalsIgnoreCase(EXPECTED_TITLE);
        } catch (Exception e) {
            return false;
        }
    }

    public String getTitle() {
        return switch (Platform.current()) {
            case ANDROID -> driver.findElement(AppiumBy.id(TITLE_ID)).getText();
            case IOS -> driver.findElement(AppiumBy.accessibilityId(IOS_TITLE)).getText();
        };
    }

    public boolean hasTermsOfAgreementsItem() {
        return switch (Platform.current()) {
            case ANDROID -> {
                WebElement el = first(AppiumBy.id(TERMS_TEXT_ID));
                yield el != null && "Условия соглашений".equals(el.getText());
            }
            case IOS -> !driver.findElements(AppiumBy.accessibilityId(IOS_TERMS_ITEM)).isEmpty();
        };
    }

    public boolean hasPersonalDataConsentItem() {
        return switch (Platform.current()) {
            case ANDROID -> {
                WebElement el = first(AppiumBy.id(CONSENT_TEXT_ID));
                yield el != null && el.getText().contains("Согласие");
            }
            case IOS -> !driver.findElements(AppiumBy.iOSNsPredicateString(IOS_CONSENT_PREDICATE)).isEmpty();
        };
    }

    public void tapTermsOfAgreements() {
        switch (Platform.current()) {
            case ANDROID -> driver.findElement(AppiumBy.id(TERMS_ITEM_ID)).click();
            case IOS -> driver.findElement(AppiumBy.accessibilityId(IOS_TERMS_ITEM)).click();
        }
    }

    public void tapPersonalDataConsent() {
        switch (Platform.current()) {
            case ANDROID -> driver.findElement(AppiumBy.id(CONSENT_ITEM_ID)).click();
            case IOS -> driver.findElement(AppiumBy.iOSNsPredicateString(IOS_CONSENT_PREDICATE)).click();
        }
    }

    public void dismissByTappingOutside() {
        switch (Platform.current()) {
            case ANDROID -> {
                driver.findElement(AppiumBy.id(TOUCH_OUTSIDE_ID)).click();
                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.invisibilityOfElementLocated(AppiumBy.id(SHEET_ID)));
            }
            // iOS has no "touch_outside" element — tap the dimmed backdrop above the sheet.
            // The sheet occupies the bottom of the screen; the upper third is the backdrop.
            case IOS -> {
                Dimension size = driver.manage().window().getSize();
                tap(size.width / 2, (int) (size.height * 0.25));
                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.invisibilityOfElementLocated(termsItemLocator()));
            }
        }
    }

    private By termsItemLocator() {
        return switch (Platform.current()) {
            case ANDROID -> AppiumBy.id(SHEET_ID);
            case IOS -> AppiumBy.accessibilityId(IOS_TERMS_ITEM);
        };
    }

    private WebElement first(By by) {
        var list = driver.findElements(by);
        return list.isEmpty() ? null : list.get(0);
    }

    private void tap(int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 1)
                .addAction(finger.createPointerMove(Duration.ZERO,
                        PointerInput.Origin.viewport(), x, y))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(tap));
    }
}
