package tests;

import core.BaseTest;
import core.Platform;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.AccountDetailPage;
import pages.MainScreenPage;
import pages.TransferPage;

/**
 * iOS transfer between own accounts (EPIC 1 / T-05, iOS).
 *
 * <p><b>Covered (verified):</b> opening an account on iOS — a coordinate tap with y-offset retry,
 * since the a11y tree has no tappable account cell (see {@link AccountDetailTest}) — then tapping the
 * detail's "Переводы" action (iOS label, plural; Android uses "Перевести") to reach the "Между своими
 * счетами" transfer screen.
 *
 * <p><b>Not yet covered (TODO):</b> the rest of the happy path to the confirmation screen. On iOS the
 * "Между своими счетами" screen auto-selects a SOURCE that may be a deposit (which cannot be debited),
 * so reaching a valid confirmation needs source = a funded current account + a distinct destination
 * via "Выберите счет" (a picker that, like Android, mixes own accounts with saved recipients). The iOS
 * confirmation screen markers are not captured yet. This is the same depth of work as the Android
 * happy-path plus the coordinate-tap fragility.
 *
 * <p>Guarded to iOS only; reset-per-method (a fresh login per case), like {@link TransferTest}.
 */
public class TransferIosTest extends BaseTest {

    private MainScreenPage mainScreen;

    @BeforeMethod(alwaysRun = true)
    public void reachMainScreen() {
        if (Platform.current() != Platform.IOS) {
            throw new SkipException("iOS-only draft (the Android transfer flow lives in TransferTest)");
        }
        mainScreen = LoginFlow.reachMainScreen(driver, LoginFlow.PRIMARY);
        if (mainScreen == null) {
            reinstallAndRestart();
            mainScreen = LoginFlow.reachMainScreen(driver, LoginFlow.FALLBACK);
        }
        Assert.assertNotNull(mainScreen,
                "Main screen must open after completing login and PIN setup");
    }

    @Test(description = "iOS: opening an account and tapping Перевести reaches the 'Между своими счетами' screen")
    public void reachesBetweenOwnAccountsScreen() {
        // iOS account-open is a coordinate tap with retry (see MainScreenPage.openFirstAccount).
        AccountDetailPage detail = mainScreen.openFirstAccount();

        TransferPage transfer = detail.tapTransfer();
        Assert.assertTrue(transfer.isTransferEntryShown(),
                "Tapping 'Перевести' should open the 'Между своими счетами' transfer screen");

        // TODO(next session, live sim): continue the happy path —
        //   tap "Выберите счет" → pick a distinct own account → enter amount in the TextField →
        //   tap "Перевести" → assert the confirmation screen (STOP before the final confirm).
    }
}
