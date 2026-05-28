package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.LanguageSelectionPage;
import pages.LanguageSelectionPage.Language;
import pages.PasswordPage;
import pages.PermissionDialog;
import pages.PhoneLoginPage;
import pages.PinCodePage;
import pages.WelcomePage;

/**
 * Тесты экрана создания PIN-кода ("Создайте код входа"), который открывается сразу после успешного
 * входа по паролю (на свежей установке). Проверяет заголовок и наличие цифровой клавиатуры.
 */
public class PinCodeTest extends BaseTest {

    private static final String TEST_PHONE = "7074771448";
    private static final String CORRECT_PASSWORD = "POIUpoiu0@";

    private PinCodePage pinPage;

    @BeforeMethod(alwaysRun = true)
    public void openPinScreen() {
        new LanguageSelectionPage(driver).selectLanguage(Language.RUSSIAN);
        WelcomePage welcome = new WelcomePage(driver);
        Assert.assertTrue(welcome.isDisplayed(), "Welcome screen should open");
        welcome.tapStart();
        new PermissionDialog(driver).acceptIfPresent();

        PhoneLoginPage phone = new PhoneLoginPage(driver);
        Assert.assertTrue(phone.isDisplayed(), "Phone login screen should open");
        phone.enterPhone(TEST_PHONE);
        phone.tapContinue();

        PasswordPage password = new PasswordPage(driver);
        Assert.assertTrue(password.isDisplayed(), "Password field should appear");
        password.enterPassword(CORRECT_PASSWORD);
        password.tapContinue();

        pinPage = new PinCodePage(driver);
        Assert.assertTrue(pinPage.isDisplayed(),
                "PIN creation screen must be open before each test");
    }

    @Test(description = "PIN creation screen shows the 'Создайте код входа' title")
    public void pinCreationScreenIsDisplayed() {
        Assert.assertTrue(pinPage.isDisplayed(), "PIN creation screen should be visible");
    }

    @Test(description = "PIN keypad shows all digit keys and a backspace key")
    public void pinKeypadIsComplete() {
        Assert.assertTrue(pinPage.hasAllDigitKeys(), "Keypad should expose digits 0-9");
        Assert.assertTrue(pinPage.hasBackspaceKey(), "Keypad should have a backspace key");
    }
}
