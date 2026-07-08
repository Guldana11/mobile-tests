package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pages.MainScreenPage;

/**
 * Tests the "Быстрое меню" tab sheet (EPIC 0). Verifies it lists its items and that its non-transfer
 * destinations open their screens with the expected content and return. The two transfer items
 * ("Перевод внутри банка" / "Перевод в другой банк") are covered as full flows by
 * {@link PhoneTransferTest} / {@link OtherBankTransferTest}; the shared info items (Новости / Курсы /
 * ЧаВо) are covered from the drawer by {@link SideMenuTest}.
 *
 * <p>Shared session (reset once per class) with {@link #ensureMainScreen()} restoring a clean main
 * screen before each case — like {@link SideMenuTest}.
 */
public class QuickMenuTest extends BaseTest {

    // Stable items expected in the Быстрое меню sheet (locale text, CONTAINS-matched to survive iOS plurals).
    private static final String[] EXPECTED_ITEMS = {
            "Избранное и автоперевод", "Цифровой кредит", "Новости",
            "Курсы обмена валют", "Часто задаваемые вопросы"
    };

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
            Assert.assertNotNull(mainScreen, "Main screen must open after completing login and PIN setup");
        } else {
            mainScreen.unlockIfLocked(LoginFlow.PIN);
            mainScreen.dismissSideMenuIfOpen();
            if (!mainScreen.isDisplayed()) {
                mainScreen.tapToolbarBack();
            }
            mainScreen.openHomeTab();
        }
    }

    @Test(description = "The Быстрое меню sheet lists its expected items")
    public void quickMenuListsItems() {
        mainScreen.openQuickMenuTab();
        for (String item : EXPECTED_ITEMS) {
            Assert.assertTrue(mainScreen.showsText(item), "Быстрое меню should list '" + item + "'");
        }
    }

    // Быстрое меню destinations paired with a unique content marker on the screen they open.
    @DataProvider(name = "quickDestinations")
    public Object[][] quickDestinations() {
        return new Object[][]{
                {"Избранное и автоперевод", "Избранные"},
                {"Цифровой кредит", "скоро будет доступен"},
        };
    }

    @Test(dataProvider = "quickDestinations",
            description = "Each Быстрое меню destination opens its screen; back returns to the main screen")
    public void quickMenuDestinationOpensAndReturns(String item, String marker) {
        mainScreen.openQuickMenuItem(item);
        Assert.assertTrue(mainScreen.showsText(marker),
                "The '" + item + "' screen should show '" + marker + "'");
        mainScreen.tapToolbarBack();
        mainScreen.openHomeTab();
        Assert.assertTrue(mainScreen.isDisplayed(),
                "Back from '" + item + "' should return to the main screen");
    }
}
