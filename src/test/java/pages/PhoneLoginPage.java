package pages;

import core.Platform;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class PhoneLoginPage extends BasePage {

    private static final String BACK_ID = "kz.bnk.app.dev:id/iv_arrow_back";
    private static final String TITLE_ID = "kz.bnk.app.dev:id/tv_title";
    private static final String DESCRIPTION_ID = "kz.bnk.app.dev:id/tv_description";
    private static final String PHONE_INPUT_ID = "kz.bnk.app.dev:id/et_phone";
    private static final String AGREEMENT_ID = "kz.bnk.app.dev:id/tv_agreement";
    private static final String CONTINUE_BUTTON_ID = "kz.bnk.app.dev:id/btn";

    // iOS exposes labels as accessibility ids. The phone field is the only XCUIElementTypeTextField;
    // "+7" is a separate StaticText (not part of the field's value); the "условиями Банка" link is a
    // real XCUIElementTypeLink whose name is the deep-link "app://terms" (locale-independent).
    private static final String IOS_TITLE = "Введите свой номер телефона";
    private static final String IOS_DESCRIPTION = "Чтобы войти в BNK Commercial Bank";
    private static final String IOS_PREFIX = "+7";
    private static final String IOS_CONTINUE_BUTTON = "Продолжить";
    private static final String IOS_AGREEMENT_LINK = "app://terms";

    public PhoneLoginPage(AppiumDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        return waitForDisplayed(Duration.ofSeconds(20));
    }

    public boolean waitForDisplayed(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(
                    ExpectedConditions.visibilityOfElementLocated(titleLocator())
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getTitle() {
        return driver.findElement(titleLocator()).getText();
    }

    public String getDescription() {
        return switch (Platform.current()) {
            case ANDROID -> driver.findElement(AppiumBy.id(DESCRIPTION_ID)).getText();
            case IOS -> driver.findElement(AppiumBy.accessibilityId(IOS_DESCRIPTION)).getText();
        };
    }

    public String getPhoneFieldText() {
        return switch (Platform.current()) {
            case ANDROID -> driver.findElement(AppiumBy.id(PHONE_INPUT_ID)).getText();
            // iOS keeps the "+7" prefix in a separate label, outside the text field. Stitch them
            // back together so the cross-platform assertions (startsWith "+7", contains digits) hold.
            case IOS -> {
                List<WebElement> prefixes = driver.findElements(AppiumBy.accessibilityId(IOS_PREFIX));
                String prefix = prefixes.isEmpty() ? "" : prefixes.get(0).getText();
                String value = phoneField().getText();
                yield prefix + (value == null ? "" : value);
            }
        };
    }

    public void enterPhone(String digits) {
        WebElement field = phoneField();
        field.click();
        field.sendKeys(digits);
    }

    public void tapPhoneField() {
        phoneField().click();
    }

    public boolean hasAgreement() {
        return switch (Platform.current()) {
            case ANDROID -> !driver.findElements(AppiumBy.id(AGREEMENT_ID)).isEmpty();
            case IOS -> !driver.findElements(agreementLinkLocator()).isEmpty();
        };
    }

    public boolean hasContinueButton() {
        return switch (Platform.current()) {
            case ANDROID -> !driver.findElements(AppiumBy.id(CONTINUE_BUTTON_ID)).isEmpty();
            case IOS -> !driver.findElements(AppiumBy.accessibilityId(IOS_CONTINUE_BUTTON)).isEmpty();
        };
    }

    public void tapContinue() {
        switch (Platform.current()) {
            case ANDROID -> driver.findElement(AppiumBy.id(CONTINUE_BUTTON_ID)).click();
            case IOS -> driver.findElement(AppiumBy.accessibilityId(IOS_CONTINUE_BUTTON)).click();
        }
    }

    public void tapBack() {
        switch (Platform.current()) {
            case ANDROID -> driver.findElement(AppiumBy.id(BACK_ID)).click();
            // The iOS build has no back control on this screen and cannot navigate back to
            // Welcome (known app bug). Tests must not call this on iOS.
            case IOS -> throw new UnsupportedOperationException(
                    "iOS phone-login screen has no back navigation (known app bug)");
        }
    }

    public void tapAgreementLink() {
        switch (Platform.current()) {
            // Android: the link is a ClickableSpan inside the agreement TextView (not its own
            // element), so we tap by coordinates on "условиями Банка" in the right half of line 1.
            case ANDROID -> {
                WebElement agreement = driver.findElement(AppiumBy.id(AGREEMENT_ID));
                Rectangle rect = agreement.getRect();
                int x = rect.getX() + (int) (rect.getWidth() * 0.75);
                int y = rect.getY() + (int) (rect.getHeight() * 0.20);
                tap(x, y);
            }
            // iOS: the link is a real XCUIElementTypeLink — tap it directly.
            case IOS -> driver.findElement(agreementLinkLocator()).click();
        }
    }

    public boolean isKeyboardShown() {
        try {
            return ((io.appium.java_client.HasOnScreenKeyboard) driver).isKeyboardShown();
        } catch (Exception e) {
            return false;
        }
    }

    private WebElement phoneField() {
        return switch (Platform.current()) {
            case ANDROID -> driver.findElement(AppiumBy.id(PHONE_INPUT_ID));
            case IOS -> driver.findElement(AppiumBy.className("XCUIElementTypeTextField"));
        };
    }

    private By titleLocator() {
        return switch (Platform.current()) {
            case ANDROID -> AppiumBy.id(TITLE_ID);
            case IOS -> AppiumBy.accessibilityId(IOS_TITLE);
        };
    }

    private By agreementLinkLocator() {
        return AppiumBy.iOSNsPredicateString("name == '" + IOS_AGREEMENT_LINK + "'");
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
