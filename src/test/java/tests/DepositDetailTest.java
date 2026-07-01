package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.AccountDetailPage;
import pages.MainScreenPage;

/**
 * Tests the DEPOSIT detail screen reached by tapping a deposit card on the main screen (EPIC: deposits).
 * The deposit detail is the SAME layout as an account's — balance + "Доступный", "Перевести"/"Реквизиты"
 * actions, "История"/"Настройки" tabs, toolbar back — so it reuses {@link AccountDetailPage} via
 * {@link MainScreenPage#openDepositDetail()}. Read-only: no money moved, no product opened.
 *
 * <p>Like {@link AccountDetailTest}, these cases navigate INTO the detail and don't return, so the class
 * does NOT share a session — each case logs in fresh (BaseTest reset-per-method). Registered in
 * android.xml and ios.xml (iOS opening uses a coordinate tap, see {@code MainScreenPage}).
 */
public class DepositDetailTest extends BaseTest {

    private MainScreenPage mainScreen;

    @BeforeMethod(alwaysRun = true)
    public void reachMainScreen() {
        mainScreen = LoginFlow.reachMainScreen(driver, LoginFlow.PRIMARY);
        if (mainScreen == null) {
            reinstallAndRestart();
            mainScreen = LoginFlow.reachMainScreen(driver, LoginFlow.PRIMARY);
        }
        Assert.assertNotNull(mainScreen, "Main screen must open after completing login and PIN setup");
    }

    @Test(description = "Tapping a deposit card opens its detail screen with balance and actions")
    public void tappingDepositOpensDetail() {
        AccountDetailPage detail = mainScreen.openDepositDetail();
        Assert.assertTrue(detail.isDisplayed(), "Deposit detail screen should open");
        Assert.assertTrue(detail.hasActions(), "Deposit detail should show the transfer / 'Реквизиты' actions");
    }

    @Test(description = "Deposit detail exposes the История and Настройки tabs")
    public void depositDetailHasTabs() {
        AccountDetailPage detail = mainScreen.openDepositDetail();
        Assert.assertTrue(detail.isDisplayed(), "Deposit detail should open");
        Assert.assertTrue(detail.hasTabs(), "Deposit detail should expose the История and Настройки tabs");
    }

    @Test(description = "Back arrow on deposit detail returns to the main screen")
    public void backFromDetailReturnsToMain() {
        AccountDetailPage detail = mainScreen.openDepositDetail();
        Assert.assertTrue(detail.isDisplayed(), "Deposit detail should open before going back");
        detail.goBack();
        Assert.assertTrue(mainScreen.isDisplayed(), "Back should return to the main screen");
    }
}
