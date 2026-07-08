package tests;

import core.BaseTest;
import core.Platform;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.MainScreenPage;
import pages.ProfilePage;

/**
 * Tests the PROFILE screen opened by tapping the header avatar on the main screen (EPIC 5 / T-20).
 * Reached via {@link MainScreenPage#openProfile()} (the profile is NOT a side-menu item — it is the
 * top-left avatar). Read-only: no data changed, no logout performed.
 */
public class ProfileTest extends BaseTest {

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

    @Test(description = "Tapping the avatar opens the profile with the user's full name and phone")
    public void profileShowsUserIdentity() {
        ProfilePage profile = mainScreen.openProfile();
        Assert.assertTrue(profile.isDisplayed(), "Profile screen should open");
        Assert.assertTrue(profile.showsText("Серікбаева Гаухар"),
                "Profile should show the user's full name");
        Assert.assertTrue(profile.showsText("+7("), "Profile should show the user's phone number");
    }

    @Test(description = "The profile lists its sections: Персональная информация, Номер телефона, Настройки")
    public void profileListsSections() {
        ProfilePage profile = mainScreen.openProfile();
        Assert.assertTrue(profile.isDisplayed(), "Profile screen should open");
        Assert.assertTrue(profile.showsText("Персональная информация"), "Profile should list 'Персональная информация'");
        Assert.assertTrue(profile.showsText("Номер телефона"), "Profile should list 'Номер телефона'");
        Assert.assertTrue(profile.showsText("Настройки"), "Profile should list 'Настройки'");
    }

    @Test(description = "The profile shows the build version (Android; iOS profile does not expose it)")
    public void profileShowsBuildVersion() {
        if (Platform.current() == Platform.IOS) {
            throw new SkipException("iOS profile does not expose the build version in the a11y tree");
        }
        ProfilePage profile = mainScreen.openProfile();
        Assert.assertTrue(profile.isDisplayed(), "Profile screen should open");
        Assert.assertTrue(profile.showsText("Версия сборки"), "Profile should show the build version ('Версия сборки')");
    }

    @Test(description = "'Персональная информация' is a not-yet-built stub (Android; the iOS row does not navigate)")
    public void personalInfoIsUnderDevelopment() {
        if (Platform.current() == Platform.IOS) {
            throw new SkipException("iOS: tapping the 'Персональная информация' row does not navigate to "
                    + "an exposable stub screen (the under-development notice is Android-only)");
        }
        ProfilePage profile = mainScreen.openProfile();
        Assert.assertTrue(profile.isDisplayed(), "Profile screen should open");
        profile.openPersonalInfo();
        Assert.assertTrue(profile.showsText("в разработке"),
                "'Персональная информация' should show the under-development notice");
    }

    @Test(description = "The in-profile Настройки list exposes change-PIN / change-password / language")
    public void profileSettingsListsActions() {
        ProfilePage profile = mainScreen.openProfile();
        Assert.assertTrue(profile.isDisplayed(), "Profile screen should open");
        profile.openSettings();
        Assert.assertTrue(profile.showsText("Изменить ПИН-код"), "Settings should expose 'Изменить ПИН-код'");
        Assert.assertTrue(profile.showsText("Изменить пароль"), "Settings should expose 'Изменить пароль'");
        Assert.assertTrue(profile.showsText("Язык"), "Settings should expose 'Язык'");
    }

    @Test(description = "Back on the profile returns to the main screen")
    public void backFromProfileReturnsToMain() {
        ProfilePage profile = mainScreen.openProfile();
        Assert.assertTrue(profile.isDisplayed(), "Profile screen should open before going back");
        profile.goBack();
        Assert.assertTrue(mainScreen.isDisplayed(), "Back should return to the main screen");
    }
}
