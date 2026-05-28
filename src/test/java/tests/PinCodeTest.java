package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.PinCodePage;

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
}
