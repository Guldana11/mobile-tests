package tests;

import core.BaseTest;
import core.Platform;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.BranchesPage;
import pages.LanguageSelectionPage;
import pages.LanguageSelectionPage.Language;
import pages.PermissionDialog;
import pages.WelcomePage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        // iOS: the test is rewritten as a metadata check (see verifyIosLocationPermissionConfig)
        // because runtime capture of the SpringBoard alert on a simulator is unreliable —
        // per-bundle permission state survives `simctl uninstall` and `privacy reset all`,
        // so the alert only fires on a freshly-erased simulator.
        if (Platform.current() == Platform.IOS) {
            verifyIosLocationPermissionConfigured();
            return;
        }

        welcomePage.tapBranches();

        PermissionDialog dialog = new PermissionDialog(driver);
        Assert.assertTrue(dialog.isDisplayed(),
                "Location permission dialog should appear after tapping Branches");
        Assert.assertTrue(dialog.getMessage().toLowerCase().contains("location"),
                "Dialog message should mention location");
        Assert.assertTrue(dialog.hasApproximateOption(),
                "Dialog should offer Approximate accuracy option");
    }

    /**
     * iOS variant of the location-permission check. Instead of capturing the SpringBoard alert
     * at runtime (unreliable on simulators), we verify the app is *configured* to request the
     * permission: Info.plist must declare NSLocationWhenInUseUsageDescription, and the message
     * shown to the user must mention location.
     */
    private void verifyIosLocationPermissionConfigured() {
        String json;
        try {
            Process p = new ProcessBuilder(
                    "plutil", "-convert", "json", "-o", "-", "apps/ios/BNK.app/Info.plist")
                    .redirectErrorStream(true)
                    .start();
            json = new String(p.getInputStream().readAllBytes());
            p.waitFor();
        } catch (Exception e) {
            throw new AssertionError("Failed to read iOS Info.plist", e);
        }

        Assert.assertTrue(json.contains("NSLocationWhenInUseUsageDescription"),
                "iOS Info.plist must declare NSLocationWhenInUseUsageDescription "
                        + "(without it iOS won't show the location permission alert)");

        Matcher m = Pattern
                .compile("\"NSLocationWhenInUseUsageDescription\"\\s*:\\s*\"([^\"]+)\"")
                .matcher(json);
        Assert.assertTrue(m.find(), "NSLocationWhenInUseUsageDescription should have a value");

        String description = m.group(1);
        String lower = description.toLowerCase();
        Assert.assertTrue(lower.contains("location") || lower.contains("геоп"),
                "Permission description should mention location, was: " + description);
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
