package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pages.MainScreenPage;

/**
 * Tests the side menu (drawer) opened from the main-screen header burger (EPIC 0 / T-02). Verifies
 * the drawer lists its expected navigation items and that tapping one ("Курсы обмена валют") opens
 * its screen, from which the toolbar back arrow returns to the main screen.
 *
 * <p>Like {@link MainScreenTest}, the main screen is reached ONCE per class
 * ({@link #resetBeforeEachMethod()} = false) and the session is reused. {@link #ensureMainScreen()}
 * restores a clean main-screen state before each case (unlock if auto-locked, close the drawer or
 * step back out of a sub-screen a previous case may have left open).
 *
 * <p>Cross-platform: side-menu items are locale text and the toolbar back arrow is "BackButton" on
 * iOS (verified). Registered in both android.xml and ios.xml.
 */
public class SideMenuTest extends BaseTest {

    // A stable subset of drawer items expected to always be present.
    private static final String[] EXPECTED_ITEMS = {
            "Новости", "Филиалы", "Курсы обмена валют",
            "Подать обращение", "Часто задаваемые вопросы", "Служба поддержки", "Выйти"
    };
    private static final String RATES_ITEM = "Курсы обмена валют";

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
            mainScreen.unlockIfLocked(LoginFlow.PIN);
            // A previous case may have left the drawer open or navigated into a sub-screen.
            mainScreen.dismissSideMenuIfOpen();
            if (!mainScreen.isDisplayed()) {
                mainScreen.tapToolbarBack();
            }
            mainScreen.openHomeTab();
        }
    }

    // Side-menu destinations paired with a unique content marker on the screen they open. "Филиалы" is
    // covered by BranchesTest, "Курсы обмена валют" by sideMenuItemOpensAndBackReturns, "Выйти" is logout.
    @DataProvider(name = "sideDestinations")
    public Object[][] sideDestinations() {
        return new Object[][]{
                {"Новости", "Новости"},
                {"Подать обращение", "Тип обращения"},
                {"Часто задаваемые вопросы", "Как войти в приложение"},
                {"Служба поддержки", "Контакты"},
        };
    }

    @Test(dataProvider = "sideDestinations",
            description = "Each side-menu destination opens its screen with the expected content; back returns to main")
    public void sideMenuDestinationOpensAndReturns(String item, String marker) {
        mainScreen.openSideMenu();
        Assert.assertTrue(mainScreen.isSideMenuShown(), "The side menu should open");
        mainScreen.openSideMenuItem(item);
        Assert.assertTrue(mainScreen.showsText(marker),
                "The '" + item + "' screen should show '" + marker + "'");
        mainScreen.tapToolbarBack();
        Assert.assertTrue(mainScreen.isDisplayed(),
                "Back from '" + item + "' should return to the main screen");
    }

    @Test(description = "The side menu lists its expected navigation items")
    public void sideMenuListsExpectedItems() {
        mainScreen.openSideMenu();
        Assert.assertTrue(mainScreen.isSideMenuShown(), "The side menu should open");
        for (String item : EXPECTED_ITEMS) {
            Assert.assertTrue(mainScreen.sideMenuHasItem(item),
                    "The side menu should list '" + item + "'");
        }
    }

    @Test(description = "Tapping a side-menu item opens its screen; back returns to the main screen")
    public void sideMenuItemOpensAndBackReturns() {
        mainScreen.openSideMenu();
        Assert.assertTrue(mainScreen.isSideMenuShown(), "The side menu should open");

        mainScreen.openSideMenuItem(RATES_ITEM);
        // The drawer's unique marker ("Служба поддержки") is gone once we left it for the sub-screen.
        Assert.assertFalse(mainScreen.isSideMenuShown(),
                "Tapping '" + RATES_ITEM + "' should leave the drawer for its own screen");

        mainScreen.tapToolbarBack();
        Assert.assertTrue(mainScreen.isDisplayed(),
                "Back from the '" + RATES_ITEM + "' screen should return to the main screen");
    }
}
