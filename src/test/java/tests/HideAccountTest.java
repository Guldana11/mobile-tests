package tests;

import core.BaseTest;
import core.Platform;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.HiddenAccountsPage;
import pages.MainScreenPage;

import java.util.List;

/**
 * Tests the account-visibility ("Скрытые счета") screen reached from the main-screen bottom action
 * (EPIC 3). The screen lists accounts in two sections — "Появляется на главной странице" and
 * "Скрытый" — which the user reorders / hides by dragging rows, then persists with "Сохранить".
 *
 * <p>Read-only: these cases assert the screen's structure and never tap "Сохранить", so the shared
 * account's visibility is never mutated. Reached via {@link MainScreenPage#openHiddenAccounts()}.
 */
public class HideAccountTest extends BaseTest {

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

    @Test(description = "The visibility screen opens with the shown-accounts section, accounts and the Save button")
    public void hiddenAccountsPageOpens() {
        HiddenAccountsPage page = mainScreen.openHiddenAccounts();
        Assert.assertTrue(page.isDisplayed(),
                "The visibility screen should show 'Появляется на главной странице'");
        Assert.assertTrue(page.showsText("Текущий счет"), "It should list the current accounts");
        Assert.assertTrue(page.hasSaveButton(), "It should show the 'Сохранить' button");
    }

    @Test(description = "The visibility screen has a 'Скрытый' (hidden) section (Android; iOS has no text header)")
    public void hiddenSectionPresent() {
        if (Platform.current() == Platform.IOS) {
            throw new SkipException("iOS does not expose the 'Скрытый' section as a text header");
        }
        HiddenAccountsPage page = mainScreen.openHiddenAccounts();
        Assert.assertTrue(page.isDisplayed(), "The visibility screen should open");
        Assert.assertTrue(page.hasHiddenSection(), "It should have the 'Скрытый' section");
    }

    @Test(description = "Dragging a row by its handle reorders the accounts (change discarded, not saved)")
    public void draggingReordersAccounts() {
        if (Platform.current() == Platform.IOS) {
            throw new SkipException("iOS reordering on the visibility screen is not characterised "
                    + "(the drag handle has no accessibilityId)");
        }
        HiddenAccountsPage page = mainScreen.openHiddenAccounts();
        Assert.assertTrue(page.isDisplayed(), "The visibility screen should open");
        List<String> before = page.aliasesInOrder();
        Assert.assertTrue(before.size() >= 3, "Need at least 3 rows to reorder, got: " + before);

        page.dragRow(0, 2);   // pick up the first row and drop it onto the third position

        List<String> after = page.aliasesInOrder();
        Assert.assertNotEquals(after, before,
                "Dragging a row by its handle should change the account order (was " + before + ")");
        // Intentionally NOT tapping "Сохранить": leaving the screen discards the reorder, so the
        // shared account's real visibility/order is never mutated.
    }
}
