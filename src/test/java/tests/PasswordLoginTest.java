package tests;

import core.BaseTest;
import core.Platform;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.PasswordPage;
import pages.PinCodePage;

import java.time.Duration;

/**
 * Тесты экрана ввода пароля (login). Открывается после ввода номера телефона тестового аккаунта
 * и нажатия "Продолжить". Пароль проверяется на сервере при нажатии: верный → экран создания PIN,
 * неверный → нативный алерт "Неверные данные для входа". Успешный вход использует основной аккаунт
 * с откатом на запасной (см. {@link LoginFlow}).
 */
public class PasswordLoginTest extends BaseTest {

    private static final String WRONG_PASSWORD = "WrongPass1@";    // валидный формат, неверные данные

    private PasswordPage passwordPage;

    @BeforeMethod(alwaysRun = true)
    public void openPasswordScreen() {
        passwordPage = LoginFlow.openPasswordScreen(driver, LoginFlow.PRIMARY);
        if (passwordPage == null) {
            // Primary phone didn't reveal the password screen (flaky navigation / throttling) —
            // reinstall for a clean slate and try the fallback account.
            reinstallAndRestart();
            passwordPage = LoginFlow.openPasswordScreen(driver, LoginFlow.FALLBACK);
        }
        Assert.assertNotNull(passwordPage,
                "Password screen should open after submitting the phone number");
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

    @Test(description = "Continue button is disabled until a password is entered (iOS)")
    public void continueButtonDisabledUntilPasswordEntered() {
        // Android keeps the auth button enabled and validates on press, so the gating check is
        // iOS-specific.
        if (Platform.current() == Platform.ANDROID) {
            throw new SkipException("Android: the auth button is always enabled (no client-side gating)");
        }
        Assert.assertFalse(passwordPage.isContinueEnabled(),
                "Continue should be disabled while the password field is empty");
        passwordPage.enterPassword(LoginFlow.PRIMARY.password());
        Assert.assertTrue(passwordPage.isContinueEnabled(),
                "Continue should become enabled once a password is entered");
    }

    @Test(description = "Wrong password does not log in (PIN creation does not open)")
    public void wrongPasswordDoesNotLogIn() {
        passwordPage.enterPassword(WRONG_PASSWORD);
        passwordPage.tapContinue();

        // Robust signal on both platforms: a wrong password must not advance to PIN creation.
        PinCodePage pin = new PinCodePage(driver);
        Assert.assertFalse(pin.waitForDisplayed(Duration.ofSeconds(8)),
                "Wrong password must not open the PIN creation screen");

        // On Android the wrong-credentials alert stays put (no autoAcceptAlerts), so we also verify
        // it. On iOS autoAcceptAlerts may dismiss it before we can read it, so we don't assert there.
        if (Platform.current() == Platform.ANDROID) {
            Assert.assertTrue(passwordPage.isLoginRejectedErrorShown(Duration.ofSeconds(3)),
                    "Wrong password should show a rejection alert "
                            + "('Неверные данные для входа' or 'Вход заблокирован …')");
        }
    }

    @Test(description = "A valid account opens the PIN creation screen (primary, fallback on failure)")
    public void correctPasswordOpensPinCreation() {
        // BeforeMethod already opened the password screen with the primary account's phone.
        passwordPage.enterPassword(LoginFlow.PRIMARY.password());
        passwordPage.tapContinue();

        boolean opened = new PinCodePage(driver).waitForDisplayed(Duration.ofSeconds(10));
        if (!opened) {
            // Primary account not accepted — reinstall for a clean slate and try the fallback.
            reinstallAndRestart();
            opened = LoginFlow.tryReachPinCreation(driver, LoginFlow.FALLBACK);
        }
        Assert.assertTrue(opened, "A valid account should open the PIN creation screen");
    }
}
