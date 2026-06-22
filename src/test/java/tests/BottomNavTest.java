package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.MainScreenPage;

/**
 * Bottom-navigation traversal tests (EPIC 0 / T-03). The app's bottom bar holds three entries that
 * behave in two different ways, which this class pins down:
 * <ul>
 *   <li><b>Главная</b> and <b>Продукты</b> are real sibling tabs — selecting one keeps the bottom bar
 *       and swaps the content, so you can switch back and forth freely.</li>
 *   <li><b>Быстрое меню</b> is NOT a sibling tab: it opens a modal bottom sheet that replaces the
 *       bottom bar entirely. It has no close button and is dismissed with the system Back gesture,
 *       which returns to the previous screen (Главная).</li>
 * </ul>
 * {@link MainScreenTest} only checks that each entry opens its content in isolation; this class
 * verifies the navigation as a whole — repeated Главная↔Продукты switching, and that the Быстрое меню
 * sheet opens and closes cleanly back to Главная.
 *
 * <p>Reached ONCE per class ({@link #resetBeforeEachMethod()} = false), session reused;
 * {@link #ensureMainScreen()} restores Главная before each case (closing the quick-menu sheet first,
 * since it hides the bottom bar that {@code openHomeTab()} needs).
 *
 * <p><b>Android-only for now:</b> dismissing the quick-menu sheet relies on the Android system Back
 * gesture (iOS sheets have no hardware back), so this class is registered in android.xml only.
 */
public class BottomNavTest extends BaseTest {

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
            // The quick-menu sheet hides the bottom bar, so close it before tapping a tab.
            mainScreen.closeQuickMenuIfOpen();
            mainScreen.openHomeTab();
        }
    }

    @Test(description = "Главная and Продукты are sibling tabs you can switch between repeatedly")
    public void switchesBetweenHomeAndProducts() {
        Assert.assertTrue(mainScreen.hasGreeting(), "Should start on Главная (greeting visible)");

        mainScreen.openProductsTab();
        Assert.assertTrue(mainScreen.isProductsShown(), "Продукты tab should open its screen");

        mainScreen.openHomeTab();
        Assert.assertTrue(mainScreen.hasGreeting(), "Главная should restore the home screen");

        // Second round — the bottom bar stays usable after switching back and forth.
        mainScreen.openProductsTab();
        Assert.assertTrue(mainScreen.isProductsShown(), "Продукты should reopen on the second switch");
    }

    @Test(description = "Быстрое меню opens its bottom sheet and Back closes it to Главная")
    public void quickMenuSheetOpensAndClosesToHome() {
        mainScreen.openQuickMenuTab();
        Assert.assertTrue(mainScreen.isQuickMenuShown(),
                "Быстрое меню should open its sheet ('Перевод в другой банк')");

        // The sheet has no close button — system Back dismisses it back to Главная.
        mainScreen.closeQuickMenu();
        Assert.assertTrue(mainScreen.hasGreeting(),
                "Closing the Быстрое меню sheet should return to Главная");
    }
}
