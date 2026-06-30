package tests;

import io.appium.java_client.AppiumDriver;
import pages.LanguageSelectionPage;
import pages.LanguageSelectionPage.Language;
import pages.MainScreenPage;
import pages.PasswordPage;
import pages.PermissionDialog;
import pages.PhoneLoginPage;
import pages.PinCodePage;
import pages.WelcomePage;

import java.time.Duration;

/**
 * Shared navigation for the login flow (language → Welcome → phone → password → PIN), with a
 * primary and a fallback test account. Tests try {@link #PRIMARY}; if it is not accepted they
 * reinstall the app (clean slate) and try {@link #FALLBACK}.
 */
public final class LoginFlow {

    public record Account(String phone, String password) {}

    // Fixed PIN used when a test needs to get past PIN setup to the main screen.
    public static final String PIN = "1234";

    // Phones are entered without the +7 prefix already shown in the field.
    public static final Account PRIMARY = new Account("7074771448", "POIUpoiu0@");
    // Серікбаева Гаухар Бахытбекқызы — fallback when the primary account is locked/unavailable.
    public static final Account FALLBACK = new Account("7054600032", "Nazia2007!");
    // Dedicated BURNER account for the destructive brute-force lockout tests (SEC-1 password, SEC-2
    // PIN). Running those tests deliberately LOCKS this account ("Вход заблокирован до …"), so it must
    // never be PRIMARY/FALLBACK — those are needed by every other test. Used only by SecurityLockoutTest
    // (opt-in "destructive" group, not wired into android.xml/ios.xml).
    public static final Account BURNER = new Account("7074514182", "Qwerty!23");

    private LoginFlow() {}

    /**
     * Navigates from a freshly launched app to the password screen with the account's phone
     * submitted. Returns the open {@link PasswordPage}, or {@code null} if any step did not appear.
     */
    public static PasswordPage openPasswordScreen(AppiumDriver driver, Account account) {
        new LanguageSelectionPage(driver).selectLanguage(Language.RUSSIAN);
        WelcomePage welcome = new WelcomePage(driver);
        if (!welcome.isDisplayed()) return null;
        welcome.tapStart();
        new PermissionDialog(driver).acceptIfPresent();

        PhoneLoginPage phone = new PhoneLoginPage(driver);
        if (!phone.isDisplayed()) return null;
        phone.enterPhone(account.phone());
        phone.tapContinue();

        PasswordPage password = new PasswordPage(driver);
        return password.isDisplayed() ? password : null;
    }

    /**
     * Full login with the given account. Returns true if the PIN creation screen opens (i.e. the
     * credentials were accepted).
     */
    public static boolean tryReachPinCreation(AppiumDriver driver, Account account) {
        PasswordPage password = openPasswordScreen(driver, account);
        if (password == null) return false;
        password.enterPassword(account.password());
        password.tapContinue();
        return new PinCodePage(driver).waitForDisplayed(Duration.ofSeconds(10));
    }

    /**
     * Full login through to the main (home) screen: password login → PIN setup ({@link #PIN}) →
     * dismiss the post-PIN onboarding prompts. Returns the open {@link MainScreenPage}, or
     * {@code null} if PIN creation was not reached or the main screen never appeared.
     */
    public static MainScreenPage reachMainScreen(AppiumDriver driver, Account account) {
        if (!tryReachPinCreation(driver, account)) return null;
        if (!new PinCodePage(driver).completeSetup(PIN)) return null;
        MainScreenPage main = new MainScreenPage(driver);
        main.dismissOnboardingPrompts();
        return main.isDisplayed() ? main : null;
    }
}
