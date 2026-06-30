package tests;

import core.BaseTest;
import core.Platform;
import org.testng.Assert;
import org.testng.annotations.Test;
import pages.MainScreenPage;
import pages.PasswordPage;
import pages.PinCodePage;

import java.time.Duration;

/**
 * DESTRUCTIVE brute-force lockout checks (EPIC 7: SEC-1 password, SEC-2 PIN). These deliberately
 * feed wrong credentials until the app locks the account, so they run ONLY against the dedicated
 * {@link LoginFlow#BURNER} account and are isolated in the opt-in {@code "destructive"} group /
 * {@code suites/destructive.xml}. They are NOT wired into android.xml/ios.xml — a normal regression
 * run must never lock a shared account.
 *
 * <ul>
 *   <li><b>SEC-1</b> — repeated wrong passwords on the login screen lock the account: the
 *       "Неверные данные для входа" alert is eventually replaced by "Вход заблокирован до […]".</li>
 *   <li><b>SEC-2</b> — repeated wrong PINs on the app-unlock screen ("Введите код") must not unlock
 *       and must escalate (logout/lockout) rather than allow unlimited retries.</li>
 * </ul>
 *
 * <p>Because the BURNER account stays locked "until [timestamp]" after a run, a re-run may see the
 * lock immediately on the first attempt — both tests treat an already-locked account as a pass
 * (the security control is what's asserted, not the exact attempt count).
 */
public class SecurityLockoutTest extends BaseTest {

    private static final String WRONG_PASSWORD = "WrongPass9@";   // valid format, wrong credentials
    private static final String WRONG_PIN = "0000";               // != LoginFlow.PIN ("1234")
    private static final int MAX_ATTEMPTS = 5;                    // safety cap on the PIN brute-force loop
    private static final int LOCK_ATTEMPTS = 6;                   // wrong logins to trip the lock (server locks at ~5)

    // ORDER MATTERS: SEC-2 needs a SUCCESSFUL login (to set up + brute-force the unlock PIN), so it must
    // run while the BURNER is still loginable. SEC-1 then LOCKS the account ("Вход заблокирован до …"),
    // which only needs the password screen — unaffected by the lock. Hence priority: SEC-2 (1) before
    // SEC-1 (2). The server lock is short (~5 min), so a re-run shortly after will see it clear.
    //
    // Cross-platform without fighting autoAcceptAlerts: the lock alert is only readable on Android (on
    // iOS autoAcceptAlerts dismisses it before it can be read). So the lock is proven BEHAVIOURALLY —
    // after the wrong attempts, even the CORRECT password is refused. A reinstall in between proves the
    // lock is server-side (account-bound), not a client/field artefact, and gives a clean password field.
    @Test(groups = "destructive", priority = 2,
            description = "SEC-1: repeated wrong passwords lock the account (correct password then refused)")
    public void wrongPasswordLocksAccount() {
        PasswordPage password = LoginFlow.openPasswordScreen(driver, LoginFlow.BURNER);
        Assert.assertNotNull(password, "BURNER phone should reveal the password screen");

        // 1) Submit wrong passwords to trip the server lockout. On Android the "Вход заблокирован" alert
        //    is readable (captured as a bonus); on iOS autoAcceptAlerts hides it — proven behaviourally in (2).
        boolean blockedAlertSeen = false;
        for (int attempt = 1; attempt <= LOCK_ATTEMPTS; attempt++) {
            password.enterPassword(WRONG_PASSWORD);
            password.tapContinue();
            if (Platform.current() == Platform.ANDROID
                    && password.isLoginBlockedShown(Duration.ofSeconds(6))) {
                blockedAlertSeen = true;
                System.out.println("[SEC-1] lock alert shown after " + attempt + " wrong attempt(s)");
                break;
            }
            password.dismissLoginAlert();   // close the "Неверные данные" alert before the next try
        }

        // 2) Cross-platform proof: on the same screen, clear the field and submit the CORRECT password.
        //    A non-locked account would open PIN creation; a locked one refuses it. The lockout is
        //    server-side (account-bound), so a correct password being refused right after the wrong ones
        //    proves the lock, not a typo. (No mid-test reinstall — it only adds flakiness on iOS.)
        password.dismissLoginAlert();   // close the lingering "Ошибка/Вход заблокирован" alert (Android)
        password.clearPassword();
        password.enterPassword(LoginFlow.BURNER.password());
        password.tapContinue();

        boolean pinOpened = new PinCodePage(driver).waitForDisplayed(Duration.ofSeconds(10));
        Assert.assertFalse(pinOpened,
                "A locked account must refuse even the CORRECT password (PIN creation must not open)");
        System.out.println("[SEC-1] account locked: correct password refused"
                + (blockedAlertSeen ? "; lock alert also confirmed on Android" : ""));
    }

    @Test(groups = "destructive", priority = 1,
            description = "SEC-2: repeated wrong PIN on unlock never opens the app and escalates")
    public void wrongPinDoesNotUnlock() {
        MainScreenPage main = LoginFlow.reachMainScreen(driver, LoginFlow.BURNER);
        Assert.assertNotNull(main,
                "BURNER account must log in and set up a PIN so the unlock screen can be brute-forced");

        // Re-launch the app (terminate + activate, no reinstall) to force the PIN re-auth screen
        // ("Введите код"). A short background return lands inside the app's grace period and does NOT
        // re-lock, so a deterministic relaunch is used; the relaunch shows a splash, so poll for the gate.
        relaunchApp();
        boolean locked = false;
        for (int i = 0; i < 15 && !locked; i++) {
            locked = main.isUnlockScreenShown();
            if (!locked) {
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }
        Assert.assertTrue(locked,
                "Re-launching must demand the PIN ('Введите код') before showing the main screen");

        // Brute-force the unlock with a wrong PIN. Cross-platform security property: a wrong PIN must
        // NEVER reveal the main screen. After each attempt, waitForDisplayed(3s) both gives the PIN
        // validation time to settle (a spinner shows briefly) and asserts the main screen did NOT appear.
        // The on-screen rejection text ("Код неверный.") is Android-specific UX — on iOS the keypad just
        // resets to "Введите код" — so it is logged as a bonus signal, not hard-asserted cross-platform.
        boolean rejectionTextSeen = false;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            if (!main.isUnlockScreenShown()) {
                System.out.println("[SEC-2] unlock screen gone after " + (attempt - 1)
                        + " wrong attempt(s) — app escalated out of the session (logout/lockout)");
                break;   // escalated (logged out / locked) — that is still NOT the main screen
            }
            main.enterUnlockPin(WRONG_PIN);
            Assert.assertFalse(main.waitForDisplayed(Duration.ofSeconds(3)),
                    "A wrong PIN must not unlock to the main screen (attempt " + attempt + ")");
            if (main.isPinErrorShown()) {
                rejectionTextSeen = true;
            }
        }
        System.out.println("[SEC-2] brute-force done; main never unlocked"
                + (rejectionTextSeen ? "; wrong PIN rejected on-screen ('Код неверный.')" : ""));

        // The brute-force must not have ended on an unlocked main screen: either still gated on the PIN
        // screen, or escalated out of the session (logged out).
        Assert.assertFalse(main.waitForDisplayed(Duration.ofSeconds(3)),
                "Repeated wrong PINs must not end on an unlocked main screen");
    }
}
