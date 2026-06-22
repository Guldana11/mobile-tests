package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.MainScreenPage;

/**
 * Pull-to-refresh on the main screen (EPIC 0 / T-04). The account list is wrapped in a
 * SwipeRefreshLayout; pulling it down triggers a refresh. The refresh spinner is a drawable, not an
 * accessibility node, and no progress/loading element appears in the tree during the refresh — so
 * these cases verify the control is present and that the gesture reloads the home screen intact
 * (greeting + account balances), rather than asserting a spinner that cannot be observed.
 *
 * <p>Reached ONCE per class ({@link #resetBeforeEachMethod()} = false), session reused;
 * {@link #ensureMainScreen()} restores a clean Главная before each case.
 *
 * <p><b>Android-only:</b> the pull-to-refresh container is matched by an Android id; iOS uses a
 * UIRefreshControl (unverified), so this class is registered in android.xml only.
 */
public class PullToRefreshTest extends BaseTest {

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
            mainScreen.dismissSideMenuIfOpen();
            mainScreen.closeQuickMenuIfOpen();
            mainScreen.openHomeTab();
            mainScreen.scrollToTop();
        }
    }

    @Test(description = "The home account list exposes a pull-to-refresh control")
    public void homeExposesPullToRefresh() {
        Assert.assertTrue(mainScreen.hasPullToRefresh(),
                "The home screen should wrap the account list in a SwipeRefreshLayout");
    }

    @Test(description = "Pull-to-refresh reloads the home screen intact (greeting and balances)")
    public void pullToRefreshReloadsHome() {
        mainScreen.pullToRefresh();
        // The refresh must complete and re-render the home screen, not blank it out.
        Assert.assertTrue(mainScreen.isDisplayed(), "Home screen should still be shown after refresh");
        Assert.assertTrue(mainScreen.hasGreeting(),
                "Greeting should remain after pull-to-refresh");
        Assert.assertTrue(mainScreen.hasAccountBalances(),
                "Account balances should re-render after pull-to-refresh");
    }
}
