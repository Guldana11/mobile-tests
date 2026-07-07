package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.AccountDetailPage;
import pages.MainScreenPage;

/**
 * Tests the account-detail screen reached by tapping an account card on the main screen
 * (EPIC 0 / T-01). Verifies the detail screen opens with the account name, balance, action buttons
 * and tabs, that "Реквизиты" shows the account number (KZ IBAN) / bank / currency, and that the
 * toolbar back arrow returns to the main screen.
 *
 * <p>These cases navigate into the detail screen and don't return, so — unlike {@link MainScreenTest}
 * — the class does NOT share a session: {@link #reachMainScreen()} reinstalls and logs in fresh before
 * every case (the BaseTest reset-per-method default). Slower, but robust and order-independent; a
 * shared session needed fragile back-navigation recovery that flaked under load in the full suite.
 *
 * <p><b>Cross-platform.</b> Opening an account on iOS uses a coordinate tap (the a11y tree has no
 * tappable account cell — see {@link MainScreenPage#openFirstAccount()}); the detail screen itself is
 * handled per-platform in {@link AccountDetailPage} (iOS has no "Доступный" label, the transfer action
 * is "Переводы", back is "chevron.left"). Registered in both android.xml and ios.xml.
 */
public class AccountDetailTest extends BaseTest {

    private MainScreenPage mainScreen;

    // Reset-per-method (the BaseTest default): every case starts from a fresh install and logs in to a
    // clean main screen. These cases navigate INTO the detail screen and don't navigate back, so a
    // shared session would need fragile recovery to return home between cases (which flaked under load
    // in the full suite). A fresh login per case is slower but robust and order-independent.
    @BeforeMethod(alwaysRun = true)
    public void reachMainScreen() {
        mainScreen = LoginFlow.reachMainScreen(driver, LoginFlow.PRIMARY);
        if (mainScreen == null) {
            reinstallAndRestart();
            mainScreen = LoginFlow.reachMainScreen(driver, LoginFlow.FALLBACK);
        }
        Assert.assertNotNull(mainScreen,
                "Main screen must open after completing login and PIN setup");
    }

    @Test(description = "Tapping an account card opens its detail with the account name, balance and actions")
    public void tappingAccountOpensDetail() {
        AccountDetailPage detail = mainScreen.openFirstAccount();
        Assert.assertTrue(detail.isDisplayed(),
                "Account detail should open showing the 'Доступный' available-balance label");
        Assert.assertTrue(detail.showsText("Текущий счет"),
                "Detail should show the account name ('Текущий счет …')");
        Assert.assertTrue(detail.showsAmount(), "Detail should show a balance amount (₸/$)");
        Assert.assertTrue(detail.hasActions(),
                "Account detail should expose the 'Перевести' and 'Реквизиты' actions");
    }

    @Test(description = "'Реквизиты' shows the account number (KZ IBAN), bank and currency")
    public void accountDetailShowsRequisites() {
        AccountDetailPage detail = mainScreen.openFirstAccount();
        Assert.assertTrue(detail.isDisplayed(), "Account detail should open");
        detail.openRequisites();
        Assert.assertTrue(detail.showsText("Номер счета"),
                "Requisites should show the account number ('Номер счета')");
        Assert.assertTrue(detail.showsText("KZ"),
                "Requisites should show a KZ IBAN account number");
        Assert.assertTrue(detail.showsText("Банк"), "Requisites should show the bank ('Банк')");
        Assert.assertTrue(detail.showsText("Валюта"), "Requisites should show the currency ('Валюта')");
    }

    @Test(description = "NEGATIVE: the account number / IBAN is hidden on the detail screen until 'Реквизиты' is tapped")
    public void requisitesHiddenUntilTapped() {
        AccountDetailPage detail = mainScreen.openFirstAccount();
        Assert.assertTrue(detail.isDisplayed(), "Account detail should open");
        // On the plain detail screen the requisite fields are NOT shown — they live behind "Реквизиты".
        Assert.assertFalse(detail.isTextPresent("Номер счета"),
                "The detail screen must NOT show 'Номер счета' before 'Реквизиты' is tapped");
        // ...and they DO appear once the action is tapped (confirms the assertion above isn't vacuous).
        detail.openRequisites();
        Assert.assertTrue(detail.showsText("Номер счета"),
                "'Номер счета' should appear after tapping 'Реквизиты'");
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
