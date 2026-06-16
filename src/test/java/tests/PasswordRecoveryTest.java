package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.PasswordPage;
import pages.PasswordRecoveryPage;

import java.time.Duration;

/**
 * Тесты флоу восстановления пароля: "Забыли пароль?" → экран ввода SMS-кода ("Введите код") →
 * после верного кода запускается видеоидентификация.
 *
 * <p>Покрытие доходит до СТАРТА видеоидентификации — сам liveness на эмуляторе/симуляторе пройти
 * нельзя (нет живой камеры/лица), поэтому тест проверяет, что флоу доводит пользователя до
 * видео-шлюза. Сигнал шлюза платформозависимый: на Android — системный запрос камеры, на iOS —
 * алерт «Не удается выполнить запись» (камера не пишет на симуляторе). Подробности — в
 * {@link PasswordRecoveryPage}. Используется фиксированный dev-код {@code 0000}. Открытие экрана
 * пароля входа не требует ввода пароля, поэтому тест работает даже при заблокированном аккаунте.
 */
public class PasswordRecoveryTest extends BaseTest {

    // Фиксированный тестовый SMS-код dev-окружения (0000 принимается и сабмитит).
    private static final String DEV_TEST_CODE = "0000";

    private PasswordPage passwordPage;

    @BeforeMethod(alwaysRun = true)
    public void openPasswordScreen() {
        passwordPage = LoginFlow.openPasswordScreen(driver, LoginFlow.PRIMARY);
        if (passwordPage == null) {
            reinstallAndRestart();
            passwordPage = LoginFlow.openPasswordScreen(driver, LoginFlow.FALLBACK);
        }
        Assert.assertNotNull(passwordPage,
                "Password screen should open after submitting the phone number");
    }

    @Test(description = "'Забыли пароль?' opens the SMS-code (OTP) recovery screen")
    public void forgotPasswordOpensCodeScreen() {
        passwordPage.tapForgotPassword();

        PasswordRecoveryPage recovery = new PasswordRecoveryPage(driver);
        Assert.assertTrue(recovery.isDisplayed(),
                "Recovery code screen ('Введите код') should open");
        Assert.assertTrue(recovery.getDescription().contains("Код был отправлен"),
                "Screen should state the code was sent to the phone number");
        Assert.assertTrue(recovery.hasResendTimer(), "A resend timer should be shown");
        Assert.assertTrue(recovery.hasCodeField(), "A code input field should be present");
    }

    @Test(description = "Entering the valid SMS code triggers video identification")
    public void validCodeTriggersVideoIdentification() {
        passwordPage.tapForgotPassword();

        PasswordRecoveryPage recovery = new PasswordRecoveryPage(driver);
        Assert.assertTrue(recovery.isDisplayed(), "Recovery code screen should open");

        recovery.enterCode(DEV_TEST_CODE);

        // A valid code advances to video identification. The "reached video-id" signal is platform
        // specific (Android: camera permission request; iOS: simulator recording-failure alert) — see
        // PasswordRecoveryPage. The liveness check itself cannot run without a live camera/face.
        Assert.assertTrue(recovery.triggersVideoIdentification(Duration.ofSeconds(15)),
                "The flow should reach video identification after a valid code");
    }
}
