package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.BranchesPage;
import pages.LanguageSelectionPage;
import pages.LanguageSelectionPage.Language;
import pages.PermissionDialog;
import pages.WelcomePage;

/**
 * Тесты экрана "Филиалы" (карта + список отделений банка).
 * Включает отдельный кейс на системный диалог разрешения геолокации,
 * который появляется при первом нажатии "Филиалы".
 */
public class BranchesTest extends BaseTest {

    private WelcomePage welcomePage;

    @BeforeMethod(alwaysRun = true)
    public void openWelcomeScreen() {
        new LanguageSelectionPage(driver).selectLanguage(Language.RUSSIAN);
        welcomePage = new WelcomePage(driver);
        Assert.assertTrue(welcomePage.isDisplayed(),
                "Welcome screen must be open before each Branches test");
    }

    @Test(description = "Tapping Branches triggers the system location-permission dialog")
    public void tappingBranchesShowsPermissionDialog() {
        welcomePage.tapBranches();

        PermissionDialog dialog = new PermissionDialog(driver);
        Assert.assertTrue(dialog.isDisplayed(),
                "Location permission dialog should appear after tapping Branches");
        Assert.assertTrue(dialog.getMessage().toLowerCase().contains("location"),
                "Dialog message should mention location");
        Assert.assertTrue(dialog.hasApproximateOption(),
                "Dialog should offer Approximate accuracy option");
    }

    @Test(description = "Allowing location permission opens the Branches screen with map and list")
    public void allowingPermissionOpensBranchesScreen() {
        welcomePage.tapBranches();
        new PermissionDialog(driver).acceptIfPresent();

        BranchesPage branches = new BranchesPage(driver);
        Assert.assertTrue(branches.isDisplayed(),
                "Branches screen should open after granting permission");
        Assert.assertTrue(branches.hasMap(), "Branches screen should contain a map");
        Assert.assertTrue(branches.getBranchCount() > 0,
                "Branches list should contain at least one branch");
    }

    @Test(description = "First branch in the list has a non-empty title")
    public void firstBranchHasTitle() {
        welcomePage.tapBranches();
        new PermissionDialog(driver).acceptIfPresent();

        BranchesPage branches = new BranchesPage(driver);
        Assert.assertTrue(branches.isDisplayed(), "Branches screen should be open");

        String title = branches.getFirstBranchTitle();
        Assert.assertNotNull(title, "First branch should have a title element");
        Assert.assertFalse(title.isBlank(), "First branch title should not be empty");
    }
}
