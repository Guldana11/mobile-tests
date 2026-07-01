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

/**
 * "Открыть текущий счет" flow — reached from the home bottom action "Открыть счет" (next to "Открыть
 * депозит"). A simple form: an account-currency selector (KZT is pre-selected / USD) and a "Продолжить"
 * button; the agreement ("Продолжая, вы соглашаетесь с договором присоединения") is plain text, NOT a
 * checkbox. Tapping "Продолжить" OPENS A REAL current account — it just creates the account (no amount,
 * no funding, no money moved).
 *
 * <p><b>Cross-platform:</b> Android uses ids ({@code btn_continue}), iOS uses labels. Unlike the deposit
 * form there is no consent checkbox, so iOS needs no checkbox workaround.
 */
public class AccountOpenPage extends BasePage {

    private static final String PKG = "kz.bnk.app.dev:id/";
    private static final String CONTINUE_ID = PKG + "btn_continue";
    private static final String TITLE = "Открыть текущий счет";
    private static final String CURRENCY_TITLE = "Валюта счета";

    public AccountOpenPage(AppiumDriver driver) {
        super(driver);
    }

    /** True once the "Открыть текущий счет" form is shown. */
    public boolean isFormShown() {
        return waitVisible(textLocator(TITLE), Duration.ofSeconds(15));
    }

    /** Selects the account currency ("KZT" is pre-selected by default; pass "USD" for a dollar account). */
    public void selectCurrency(String code) {
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(textLocator(code))).click();
    }

    /** True if "Продолжить" is enabled (a currency is selected). */
    public boolean isContinueEnabled() {
        List<WebElement> b = driver.findElements(continueLocator());
        return !b.isEmpty() && b.get(0).isEnabled();
    }

    /** Taps "Продолжить" — OPENS A REAL ACCOUNT. iOS may add a "Подтверждение" → "Подтвердить" sheet. */
    public void tapContinue() {
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(continueLocator())).click();
        if (Platform.current() == Platform.IOS) {
            By confirm = AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND (label == 'Подтвердить' OR name == 'Подтвердить')");
            try {
                new WebDriverWait(driver, Duration.ofSeconds(8))
                        .until(ExpectedConditions.elementToBeClickable(confirm)).click();
            } catch (Exception ignored) {
                // no confirmation sheet — already proceeding
            }
        }
    }

    /** True once the form is left after continuing (the "Валюта счета" header disappears). */
    public boolean leftForm(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.invisibilityOfElementLocated(textLocator(CURRENCY_TITLE)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private By continueLocator() {
        return switch (Platform.current()) {
            case ANDROID -> By.id(CONTINUE_ID);
            case IOS -> AppiumBy.iOSNsPredicateString(
                    "type == 'XCUIElementTypeButton' AND (label == 'Продолжить' OR name == 'Продолжить')");
        };
    }

    private By textLocator(String text) {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.iOSNsPredicateString("label CONTAINS '" + text + "' OR name CONTAINS '" + text + "'");
            case ANDROID -> AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + text + "\")");
        };
    }

    private boolean waitVisible(By locator, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
