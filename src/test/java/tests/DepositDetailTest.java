package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.AccountDetailPage;
import pages.MainScreenPage;

/**
 * Tests the DEPOSIT detail screen reached by tapping a deposit card on the main screen (EPIC: deposits).
 * The deposit detail is the SAME layout as an account's — deposit name title, balance + "Доступный",
 * "Перевести"/"Реквизиты" actions, "История"/"Настройки" tabs (История shows the deposit's operations),
 * toolbar back — so it reuses {@link AccountDetailPage} via {@link MainScreenPage#openDepositDetail()}.
 * Read-only: no money moved, no product opened. One NEGATIVE case opens a current account instead
 * (via {@link MainScreenPage#openFirstAccount()}) to prove the deposit-only requisite fields
 * (rate / end-of-term / contract number) are absent there.
 *
 * <p>Like {@link AccountDetailTest}, these cases navigate INTO the detail and don't return, so the class
 * does NOT share a session — each case logs in fresh (BaseTest reset-per-method). Registered in
 * android.xml and ios.xml (iOS opening scrolls to the deposit then coordinate-taps, see {@code MainScreenPage}).
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

    @Test(description = "Deposit detail shows the deposit name, balance and the transfer/Реквизиты actions")
    public void depositDetailShowsBalanceAndName() {
        AccountDetailPage detail = mainScreen.openDepositDetail();
        Assert.assertTrue(detail.isDisplayed(), "Deposit detail screen should open");
        Assert.assertTrue(detail.showsText("Депозит"), "Detail should show the deposit name ('Депозит …')");
        Assert.assertTrue(detail.showsAmount(), "Detail should show a balance amount (₸/$)");
        Assert.assertTrue(detail.hasActions(), "Detail should show the transfer / 'Реквизиты' actions");
    }

    @Test(description = "Deposit detail exposes История/Настройки tabs and lists the deposit's operations")
    public void depositDetailHasTabsAndHistory() {
        AccountDetailPage detail = mainScreen.openDepositDetail();
        Assert.assertTrue(detail.isDisplayed(), "Deposit detail should open");
        Assert.assertTrue(detail.hasTabs(), "Deposit detail should expose the История and Настройки tabs");
        Assert.assertTrue(detail.showsText("Посмотреть историю"),
                "The История tab should list the deposit's operations ('Посмотреть историю')");
    }

    @Test(description = "'Реквизиты' shows the deposit-specific fields: rate, end-of-term date, contract number")
    public void depositDetailShowsRequisites() {
        AccountDetailPage detail = mainScreen.openDepositDetail();
        Assert.assertTrue(detail.isDisplayed(), "Deposit detail should open");
        detail.openRequisites();
        // Common requisite fields (also present on a plain account).
        Assert.assertTrue(detail.showsText("Номер счета"),
                "Requisites should show the account number ('Номер счета')");
        Assert.assertTrue(detail.showsText("Банк"), "Requisites should show the bank ('Банк')");
        // Deposit-SPECIFIC fields — these distinguish a deposit's requisites from a current account's.
        Assert.assertTrue(detail.showsText("Ставка вознаграждения"),
                "Deposit requisites should show the interest rate ('Ставка вознаграждения')");
        Assert.assertTrue(detail.showsText("Дата окончания срока"),
                "Deposit requisites should show the end-of-term date ('Дата окончания срока')");
        Assert.assertTrue(detail.showsText("Номер контракта"),
                "Deposit requisites should show the contract number ('Номер контракта')");
    }

    @Test(description = "NEGATIVE: a current account's requisites do NOT show the deposit-only fields")
    public void accountRequisitesLackDepositFields() {
        AccountDetailPage detail = mainScreen.openFirstAccount();
        Assert.assertTrue(detail.isDisplayed(), "Account detail should open");
        detail.openRequisites();
        // Sanity: it IS a requisites screen (shares the common fields with a deposit)...
        Assert.assertTrue(detail.showsText("Номер счета"), "Account requisites should show 'Номер счета'");
        Assert.assertTrue(detail.showsText("Банк"), "Account requisites should show 'Банк'");
        // ...but it must NOT carry the deposit contract fields.
        Assert.assertFalse(detail.isTextPresent("Ставка вознаграждения"),
                "A current account has no interest rate — 'Ставка вознаграждения' must be absent");
        Assert.assertFalse(detail.isTextPresent("Дата окончания срока"),
                "A current account has no term — 'Дата окончания срока' must be absent");
        Assert.assertFalse(detail.isTextPresent("Номер контракта"),
                "A current account has no deposit contract — 'Номер контракта' must be absent");
    }

    @Test(description = "Back arrow on deposit detail returns to the main screen")
    public void backFromDetailReturnsToMain() {
        AccountDetailPage detail = mainScreen.openDepositDetail();
        Assert.assertTrue(detail.isDisplayed(), "Deposit detail should open before going back");
        detail.goBack();
        Assert.assertTrue(mainScreen.isDisplayed(), "Back should return to the main screen");
    }
}
