package pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Collections;

public class PhoneLoginPage extends BasePage {

    private static final String BACK_ID = "kz.bnk.app.dev:id/iv_arrow_back";
    private static final String TITLE_ID = "kz.bnk.app.dev:id/tv_title";
    private static final String DESCRIPTION_ID = "kz.bnk.app.dev:id/tv_description";
    private static final String PHONE_INPUT_ID = "kz.bnk.app.dev:id/et_phone";
    private static final String AGREEMENT_ID = "kz.bnk.app.dev:id/tv_agreement";
    private static final String CONTINUE_BUTTON_ID = "kz.bnk.app.dev:id/btn";

    public PhoneLoginPage(AppiumDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        return waitForDisplayed(Duration.ofSeconds(20));
    }

    public boolean waitForDisplayed(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(
                    ExpectedConditions.visibilityOfElementLocated(AppiumBy.id(PHONE_INPUT_ID))
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getTitle() {
        return driver.findElement(AppiumBy.id(TITLE_ID)).getText();
    }

    public String getDescription() {
        return driver.findElement(AppiumBy.id(DESCRIPTION_ID)).getText();
    }

    public String getPhoneFieldText() {
        return driver.findElement(AppiumBy.id(PHONE_INPUT_ID)).getText();
    }

    public void enterPhone(String digits) {
        WebElement field = driver.findElement(AppiumBy.id(PHONE_INPUT_ID));
        field.click();
        field.sendKeys(digits);
    }

    public void tapPhoneField() {
        driver.findElement(AppiumBy.id(PHONE_INPUT_ID)).click();
    }

    public boolean hasAgreement() {
        return !driver.findElements(AppiumBy.id(AGREEMENT_ID)).isEmpty();
    }

    public boolean hasContinueButton() {
        return !driver.findElements(AppiumBy.id(CONTINUE_BUTTON_ID)).isEmpty();
    }

    public void tapContinue() {
        driver.findElement(AppiumBy.id(CONTINUE_BUTTON_ID)).click();
    }

    public void tapBack() {
        driver.findElement(AppiumBy.id(BACK_ID)).click();
    }

    /**
     * Tap on the "условиями Банка" link inside the agreement TextView.
     * The link is a ClickableSpan — not a separate UI element — so we tap by coordinates
     * inside the agreement bounds. The phrase sits in the first line, right of center.
     */
    public void tapAgreementLink() {
        WebElement agreement = driver.findElement(AppiumBy.id(AGREEMENT_ID));
        Rectangle rect = agreement.getRect();
        // "условиями Банка" is the underlined link in the right half of the first line.
        // Tap roughly on "условиями" — the whole phrase is one clickable span.
        int x = rect.getX() + (int) (rect.getWidth() * 0.75);
        int y = rect.getY() + (int) (rect.getHeight() * 0.20);
        tap(x, y);
    }

    public boolean isKeyboardShown() {
        try {
            return ((io.appium.java_client.HasOnScreenKeyboard) driver).isKeyboardShown();
        } catch (Exception e) {
            return false;
        }
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
