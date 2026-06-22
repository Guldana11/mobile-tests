package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.AccountDetailPage;
import pages.MainScreenPage;

/**
 * Tests the account-detail screen reached by tapping an account card on the main screen
 * (EPIC 0 / T-01). Verifies the detail screen opens with its balance, action buttons and tabs,
 * and that the toolbar back arrow returns to the main screen.
 *
 * <p>Login to the main screen is expensive, so — like {@link MainScreenTest} — we reach it ONCE per
 * class ({@link #resetBeforeEachMethod()} = false) and reuse the session. Each case opens the detail
 * screen itself; {@link #ensureMainScreen()} restores a clean main-screen state before every case
 * (unlock if auto-locked, navigate back if a previous case left the detail screen open).
 *
 * <p><b>Android-only for now:</b> the detail screen's iOS locators are unverified (see
 * {@link AccountDetailPage}), so this class is registered in android.xml only.
 */
public class AccountDetailTest extends BaseTest {

    private MainScreenPage mainScreen;

    @Override
    protected boolean resetBeforeEachMethod() {
        return false;
    }

    @BeforeMethod(alwaysRun = true)
    public void ensureMainScreen() {
        if (mainScreen == null) {
            mainScreen = LoginFlow.reachMainScreen(driver, LoginFlow.PRIMARY);
            if (mainScreen == null) {
                reinstallAndRestart();
                mainScreen = LoginFlow.reachMainScreen(driver, LoginFlow.FALLBACK);
            }
            Assert.assertNotNull(mainScreen,
                    "Main screen must open after completing login and PIN setup");
        } else {
            // Reused session: unlock if it auto-locked, then make sure we are back on the main screen
            // (a previous case may have left the detail screen open).
            mainScreen.unlockIfLocked(LoginFlow.PIN);
            if (!mainScreen.isDisplayed()) {
                new AccountDetailPage(driver).goBack();
            }
            mainScreen.openHomeTab();
            mainScreen.scrollToTop();
        }
    }

    @Test(description = "Tapping an account card opens its detail screen with balance and actions")
    public void tappingAccountOpensDetail() {
        AccountDetailPage detail = mainScreen.openFirstAccount();
        Assert.assertTrue(detail.isDisplayed(),
                "Account detail should open showing the 'Доступный' available-balance label");
        Assert.assertTrue(detail.hasActions(),
                "Account detail should expose the 'Перевести' and 'Реквизиты' actions");
    }

    @Test(description = "Account detail exposes the История and Настройки tabs")
    public void accountDetailHasTabs() {
        AccountDetailPage detail = mainScreen.openFirstAccount();
        Assert.assertTrue(detail.isDisplayed(), "Account detail should open");
        Assert.assertTrue(detail.hasTabs(),
                "Account detail should expose the 'История' and 'Настройки' tabs");
    }

    @Test(description = "Back arrow on account detail returns to the main screen")
    public void backFromDetailReturnsToMain() {
        AccountDetailPage detail = mainScreen.openFirstAccount();
        Assert.assertTrue(detail.isDisplayed(), "Account detail should open before going back");
        detail.goBack();
        Assert.assertTrue(mainScreen.isDisplayed(),
                "Tapping back on the detail screen should return to the main screen");
    }
}
