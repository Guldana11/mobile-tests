package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.MainScreenPage;

/**
 * Tests the recent-operations history revealed by swiping the home screen LEFT (EPIC 4 / T-17). The
 * page shows "Недавнее" / "История" sections with operation rows (type / counterparty / amount) and
 * an "Избранные" strip. Read-only: nothing is changed.
 *
 * <p>Cross-platform: Android moves the pager with the UiAutomator2 {@code mobile: swipeGesture},
 * iOS with a quick W3C swipe — both handled in {@link MainScreenPage#openRecentHistory()}.
 */
public class HistoryTest extends BaseTest {

    private MainScreenPage mainScreen;

    @BeforeMethod(alwaysRun = true)
    public void reachMainScreen() {
        mainScreen = LoginFlow.reachMainScreen(driver, LoginFlow.PRIMARY);
        if (mainScreen == null) {
            reinstallAndRestart();
            mainScreen = LoginFlow.reachMainScreen(driver, LoginFlow.FALLBACK);
        }
        Assert.assertNotNull(mainScreen, "Main screen must open after completing login and PIN setup");
    }

    @Test(description = "Swiping the home screen left reveals the recent-operations history sections")
    public void swipeRevealsRecentHistory() {
        mainScreen.openRecentHistory();
        Assert.assertTrue(mainScreen.showsText("Недавнее"), "Swiping left should reveal the 'Недавнее' section");
        Assert.assertTrue(mainScreen.showsText("История"), "The history page should show the 'История' header");
    }

    @Test(description = "The recent history lists operations with type and amount")
    public void recentHistoryListsOperations() {
        mainScreen.openRecentHistory();
        Assert.assertTrue(mainScreen.showsText("Недавнее"), "The history page should be revealed");
        Assert.assertTrue(mainScreen.showsText("Со счета на счет"),
                "The history should list operations showing their type ('Со счета на счет …')");
        Assert.assertTrue(mainScreen.showsText("₸"), "Operations should show an amount");
    }

    @Test(description = "The 'История' link opens the full date-grouped history with the period filter")
    public void opensGeneralHistory() {
        mainScreen.openGeneralHistory();
        // The "Месяц" period filter is unique to the full history (the recent page has no such filter).
        Assert.assertTrue(mainScreen.showsText("Месяц"),
                "The full history should show the period filter ('Месяц')");
        Assert.assertTrue(mainScreen.showsText("Со счета на счет"),
                "The full history should list operations ('Со счета на счет …')");
    }

    @Test(description = "Tapping an operation in the general history opens its info screen (from/to, operation number)")
    public void operationOpensInfo() {
        mainScreen.openGeneralHistory();
        mainScreen.openFirstOperation();
        Assert.assertTrue(mainScreen.showsText("Откуда"), "Operation info should show the source ('Откуда')");
        Assert.assertTrue(mainScreen.showsText("Куда"), "Operation info should show the destination ('Куда')");
        Assert.assertTrue(mainScreen.showsText("Номер операции"),
                "Operation info should show the operation number ('Номер операции')");
    }
}
