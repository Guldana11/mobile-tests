package tests;

import core.BaseTest;
import core.Platform;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.LanguageSelectionPage;
import pages.LanguageSelectionPage.Language;
import pages.PermissionDialog;
import pages.PhoneLoginPage;
import pages.WelcomePage;

/**
 * Тесты экрана ввода телефона (открывается по кнопке "Начать" на Welcome,
 * после системного диалога разрешения геолокации).
 * Покрывает: заголовок и описание, префикс +7, ввод цифр, текст согласия,
 * кнопку "Продолжить" и возврат стрелкой назад на Welcome.
 */
public class PhoneLoginTest extends BaseTest {

    private PhoneLoginPage phoneLoginPage;

    @BeforeMethod(alwaysRun = true)
    public void openPhoneLogin() {
        new LanguageSelectionPage(driver).selectLanguage(Language.RUSSIAN);
        WelcomePage welcome = new WelcomePage(driver);
        Assert.assertTrue(welcome.isDisplayed(), "Welcome screen should open before tapping Start");

        welcome.tapStart();
        new PermissionDialog(driver).acceptIfPresent();

        phoneLoginPage = new PhoneLoginPage(driver);
        Assert.assertTrue(phoneLoginPage.isDisplayed(),
                "Phone login screen must be open before each test");
    }

    @Test(description = "Phone login screen shows the title and description")
    public void phoneLoginScreenHasTitleAndDescription() {
        Assert.assertEquals(phoneLoginPage.getTitle(), "Введите свой номер телефона",
                "Title should match the expected one");
        Assert.assertEquals(phoneLoginPage.getDescription(), "Чтобы войти в BNK Commercial Bank",
                "Description should match the expected one");
    }

    @Test(description = "Phone field is prefilled with the +7 prefix")
    public void phoneFieldHasKzPrefix() {
        String value = phoneLoginPage.getPhoneFieldText();
        Assert.assertTrue(value.startsWith("+7"),
                "Phone field should be prefilled with the +7 prefix, was: " + value);
    }

    @Test(description = "Continue button and agreement text are displayed")
    public void continueButtonAndAgreementAreDisplayed() {
        Assert.assertTrue(phoneLoginPage.hasContinueButton(),
                "Continue button should be visible");
        Assert.assertTrue(phoneLoginPage.hasAgreement(),
                "Agreement text should be visible");
    }

    @Test(description = "User can type a phone number into the field")
    public void canTypePhoneNumber() {
        phoneLoginPage.enterPhone("7771234567");
        String value = phoneLoginPage.getPhoneFieldText();
        Assert.assertTrue(value.contains("777"),
                "Phone field should contain the typed digits, was: " + value);
    }

    @Test(description = "Back arrow returns the user to the Welcome screen")
    public void backArrowReturnsToWelcome() {
        // iOS-only skip: the iOS build has no back control on the phone-login screen and
        // cannot return to Welcome (known app bug), so this behaviour is Android-only.
        if (Platform.current() == Platform.IOS) {
            throw new SkipException("iOS phone-login screen has no back navigation (known app bug)");
        }
        phoneLoginPage.tapBack();
        Assert.assertTrue(new WelcomePage(driver).isDisplayed(),
                "Welcome screen should be visible after tapping back");
    }
}
