package tests;

import core.BaseTest;
import core.Platform;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.PasswordPage;
import pages.PasswordRecoveryPage;
import pages.PermissionDialog;

import java.time.Duration;

/**
 * Тесты флоу восстановления пароля: "Забыли пароль?" → экран ввода SMS-кода ("Введите код") →
 * после верного кода запускается видеоидентификация (запрос камеры на запись видео).
 *
 * <p>Покрытие доходит до СТАРТА видеоидентификации — сам liveness на эмуляторе пройти нельзя (нет
 * живой камеры/лица), поэтому тест проверяет, что флоу доводит пользователя до видео-шлюза.
 * Используется фиксированный dev-код {@code 0000}. Android-only: iOS-флоу восстановления не
 * охарактеризован (см. {@link pages.PasswordPage}). Открытие экрана пароля входа не требует, поэтому
 * тест работает даже при заблокированном аккаунте.
 */
public class PasswordRecoveryTest extends BaseTest {

    // Фиксированный тестовый SMS-код dev-окружения (поле на 5 цифр, но 0000 принимается и сабмитит).
    private static final String DEV_TEST_CODE = "0000";

    private PasswordPage passwordPage;

    @BeforeMethod(alwaysRun = true)
    public void openPasswordScreen() {
        if (Platform.current() == Platform.IOS) {
            throw new SkipException("iOS password-recovery flow is not characterised yet");
        }
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

    @Test(description = "Entering the valid SMS code triggers video identification (camera request)")
    public void validCodeTriggersVideoIdentification() {
        passwordPage.tapForgotPassword();

        PasswordRecoveryPage recovery = new PasswordRecoveryPage(driver);
        Assert.assertTrue(recovery.isDisplayed(), "Recovery code screen should open");

        recovery.enterCode(DEV_TEST_CODE);

        // A valid code advances to video identification, which immediately requests camera access to
        // record video. That permission request is the stable, automatable proof the video-id step
        // was reached — the liveness check itself cannot run on an emulator.
        PermissionDialog permission = new PermissionDialog(driver);
        Assert.assertTrue(permission.isVideoRecordingRequestShown(Duration.ofSeconds(10)),
                "A camera/record-video permission request should appear (video identification)");
    }
}
