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

import java.time.Duration;

/**
 * Тесты экрана ввода пароля (login). Открывается после ввода номера телефона тестового аккаунта
 * и нажатия "Продолжить". Пароль проверяется на сервере при нажатии: верный → экран создания PIN,
 * неверный → нативный алерт "Неверные данные для входа".
 */
public class PasswordLoginTest extends BaseTest {

    private static final String TEST_PHONE = "7074771448";        // +7 707 477 14 48
    private static final String CORRECT_PASSWORD = "POIUpoiu0@";
    private static final String WRONG_PASSWORD = "WrongPass1@";    // валидный формат, неверные данные

    private PasswordPage passwordPage;

    @BeforeMethod(alwaysRun = true)
    public void openPasswordScreen() {
        new LanguageSelectionPage(driver).selectLanguage(Language.RUSSIAN);
        WelcomePage welcome = new WelcomePage(driver);
        Assert.assertTrue(welcome.isDisplayed(), "Welcome screen should open");
        welcome.tapStart();
        new PermissionDialog(driver).acceptIfPresent();

        PhoneLoginPage phone = new PhoneLoginPage(driver);
        Assert.assertTrue(phone.isDisplayed(), "Phone login screen should open");
        phone.enterPhone(TEST_PHONE);
        phone.tapContinue();

        passwordPage = new PasswordPage(driver);
        Assert.assertTrue(passwordPage.isDisplayed(),
                "Password field should appear after submitting the phone number");
    }

    @Test(description = "Masked password field is shown after entering the phone number")
    public void passwordFieldIsDisplayed() {
        Assert.assertTrue(passwordPage.isDisplayed(), "Masked password field should be visible");
    }

    @Test(description = "Forgot-password button is displayed on the login screen")
    public void forgotPasswordButtonIsDisplayed() {
        Assert.assertTrue(passwordPage.hasForgotPassword(),
                "'Забыли пароль?' button should be visible");
    }

    @Test(description = "Continue button is disabled until a password is entered")
    public void continueButtonDisabledUntilPasswordEntered() {
        Assert.assertFalse(passwordPage.isContinueEnabled(),
                "Continue should be disabled while the password field is empty");
        passwordPage.enterPassword(CORRECT_PASSWORD);
        Assert.assertTrue(passwordPage.isContinueEnabled(),
                "Continue should become enabled once a password is entered");
    }

    @Test(description = "Wrong password does not log in (PIN creation does not open)")
    public void wrongPasswordDoesNotLogIn() {
        passwordPage.enterPassword(WRONG_PASSWORD);
        passwordPage.tapContinue();

        // Robust signal: a wrong password must not advance to PIN creation. The "Неверные данные"
        // alert is not asserted here because autoAcceptAlerts may dismiss it before we can read it.
        PinCodePage pin = new PinCodePage(driver);
        Assert.assertFalse(pin.waitForDisplayed(Duration.ofSeconds(8)),
                "Wrong password must not open the PIN creation screen");
    }

    @Test(description = "Correct password opens the PIN creation screen")
    public void correctPasswordOpensPinCreation() {
        passwordPage.enterPassword(CORRECT_PASSWORD);
        passwordPage.tapContinue();

        PinCodePage pin = new PinCodePage(driver);
        Assert.assertTrue(pin.isDisplayed(),
                "PIN creation screen should open after a correct password");
    }
}
