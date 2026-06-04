package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.PinCodePage;

import java.time.Duration;

/**
 * Тесты экрана создания PIN-кода ("Создайте код входа"), который открывается сразу после успешного
 * входа по паролю (на свежей установке). Вход выполняется основным аккаунтом с откатом на запасной
 * (см. {@link LoginFlow}). Проверяет заголовок и наличие цифровой клавиатуры.
 */
public class PinCodeTest extends BaseTest {

    private PinCodePage pinPage;

    @BeforeMethod(alwaysRun = true)
    public void openPinScreen() {
        boolean opened = LoginFlow.tryReachPinCreation(driver, LoginFlow.PRIMARY);
        if (!opened) {
            // Primary account not accepted — reinstall for a clean slate and try the fallback.
            reinstallAndRestart();
            opened = LoginFlow.tryReachPinCreation(driver, LoginFlow.FALLBACK);
        }
        Assert.assertTrue(opened, "PIN creation screen must be open before each test");
        pinPage = new PinCodePage(driver);
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

    @Test(description = "Entering 4 digits advances to the confirmation step")
    public void enteringFourDigitsOpensConfirmStep() {
        pinPage.enterPin("1234");
        Assert.assertTrue(pinPage.waitForConfirmStep(Duration.ofSeconds(5)),
                "After 4 digits the screen should ask to repeat the code ('Введите код еще раз')");
    }

    @Test(description = "Backspace removes a digit, so 3 digits + backspace + 1 does not advance")
    public void backspaceRemovesADigit() {
        // 3 digits, then delete one (2 left), then one more = 3 entered total. The screen must NOT
        // advance to the confirmation step — proving backspace removed a digit (without it, 3+1=4
        // would have advanced).
        pinPage.enterPin("123");
        pinPage.tapBackspace();
        pinPage.enterPin("4");
        Assert.assertFalse(pinPage.waitForConfirmStep(Duration.ofSeconds(2)),
                "With only 3 digits entered the screen should stay on the create step");
        Assert.assertTrue(pinPage.isCreateStepDisplayed(),
                "Screen should still show the 'Создайте код входа' create step");

        // One more digit completes 4 -> the screen now advances to confirmation.
        pinPage.enterPin("5");
        Assert.assertTrue(pinPage.waitForConfirmStep(Duration.ofSeconds(5)),
                "The 4th digit should advance to the confirmation step");
    }
}
