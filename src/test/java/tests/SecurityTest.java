package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;
import pages.MainScreenPage;
import pages.PasswordPage;
import pages.PhoneTransferPage;

/**
 * Security checks for the authorized/login zone (EPIC 7) — client-side, UI-level security controls,
 * exercised on the test accounts only.
 *
 * <p>Security testing starts at the <b>login</b> screen (SEC-4) and then covers the transfer OTP gate
 * (SEC-3).
 *
 * <ul>
 *   <li><b>SEC-4</b> — the login password field uses masked / secure entry (no plaintext echo) AND the
 *       typed password never leaks as plaintext into the accessibility tree.</li>
 *   <li><b>SEC-3</b> — a money transfer cannot complete without the SMS (OTP) confirmation step:
 *       confirming demands a code, and the operation-status screen is NOT reached without it.</li>
 * </ul>
 *
 * <p><b>Non-destructive:</b> neither case locks an account or moves money — SEC-3 stops at the SMS
 * screen without entering a code. The destructive brute-force lockout checks (SEC-1 password, SEC-2
 * PIN) would lock whatever account they use, so they need a <i>dedicated burner account</i> and an
 * opt-in group; they are tracked in {@code BACKLOG.md} (EPIC 7), not run here.
 */
public class SecurityTest extends BaseTest {

    private static final String SECRET = "Secret123@";   // dummy password used only to test masking

    /** Opens the login (password) screen, falling back to the second account on flaky navigation. */
    private PasswordPage openLogin() {
        PasswordPage password = LoginFlow.openPasswordScreen(driver, LoginFlow.PRIMARY);
        if (password == null) {
            reinstallAndRestart();
            password = LoginFlow.openPasswordScreen(driver, LoginFlow.FALLBACK);
        }
        Assert.assertNotNull(password, "Password screen should open after submitting the phone number");
        return password;
    }

    @Test(description = "SEC-4: the login password field is masked (secure entry, no plaintext)")
    public void passwordFieldIsMasked() {
        PasswordPage password = openLogin();
        password.enterPassword(SECRET);
        Assert.assertTrue(password.isPasswordMasked(),
                "The password field must mask its content (secure entry), not echo plaintext");
    }

    @Test(description = "SEC-4b: the typed password never leaks as plaintext into the accessibility tree")
    public void passwordNotExposedAsPlaintext() {
        PasswordPage password = openLogin();
        password.enterPassword(SECRET);
        Assert.assertFalse(driver.getPageSource().contains(SECRET),
                "The plaintext password must not appear anywhere in the UI/accessibility tree "
                        + "(a masked field whose value is still readable defeats secure entry)");
    }

    @Test(description = "SEC-3: a by-phone transfer cannot complete without the SMS (OTP) confirmation")
    public void transferRequiresOtpConfirmation() {
        MainScreenPage main = LoginFlow.reachMainScreen(driver, LoginFlow.PRIMARY);
        if (main == null) {
            reinstallAndRestart();
            main = LoginFlow.reachMainScreen(driver, LoginFlow.FALLBACK);
        }
        Assert.assertNotNull(main, "Main screen must open after completing login and PIN setup");

        PhoneTransferPage transfer = main.openInBankTransfer();
        Assert.assertTrue(transfer.isShown(), "The by-phone transfer form should open");
        transfer.enterPhone(LoginFlow.FALLBACK.phone());
        Assert.assertTrue(transfer.isRecipientResolved(), "Entering the recipient phone should resolve a recipient");
        transfer.enterAmount("1000");
        transfer.acceptTerms();
        transfer.tapTransfer();
        Assert.assertTrue(transfer.isConfirmationShown(), "Submitting should open the 'Подтверждение' review screen");

        // Tapping "Подтвердить" must demand an SMS code — money must NOT move without it.
        transfer.tapConfirm();
        Assert.assertTrue(transfer.isSmsCodeShown(),
                "Confirming a transfer must require an SMS (OTP) code ('Введите код')");
        Assert.assertFalse(transfer.statusShows("Номер операции"),
                "The transfer must NOT complete (reach the operation-status screen) without entering the OTP");
    }
}
