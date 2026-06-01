package tests;

import core.BaseTest;
import core.Platform;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.PasswordPage;

import java.time.Duration;

/**
 * Тесты ссылки "Забыли пароль?" на экране ввода пароля. В dev/test-окружении экран восстановления
 * сам по себе не догружается (зависит от бэкенда), поэтому стабильно проверяемый сигнал — оверлей
 * загрузки (Lottie-спиннер lav_loading), который появляется сразу после тапа. Android-only: iOS-флоу
 * восстановления не охарактеризован. Открытие экрана пароля не требует входа (пароль не отправляется),
 * поэтому тест работает даже при заблокированном аккаунте.
 */
public class ForgotPasswordTest extends BaseTest {

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

    @Test(description = "Tapping 'Забыли пароль?' triggers the recovery loading overlay")
    public void tappingForgotPasswordShowsLoadingOverlay() {
        if (Platform.current() == Platform.IOS) {
            throw new SkipException("iOS password-recovery flow is not characterised yet");
        }
        passwordPage.tapForgotPassword();
        Assert.assertTrue(passwordPage.isRecoveryLoadingShown(Duration.ofSeconds(5)),
                "A loading overlay should appear after tapping 'Забыли пароль?'");
    }
}
