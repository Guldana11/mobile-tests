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
 * <p>Opening an account on iOS is a coordinate tap with y-offset retry, since the a11y tree has no
 * tappable account cell (see {@link AccountDetailTest}); then the detail's "Переводы" action (iOS
 * label, plural; Android uses "Перевести") reaches the "Между своими счетами" screen.
 *
 * <p><b>Full happy path (verified on a live simulator):</b> the screen auto-selects a SOURCE that is a
 * deposit (cannot be debited), so we re-pick a funded current account, pick a distinct destination via
 * "Выберите счет", enter an amount and tap "Перевести" to reach the "Подтверждение" review screen —
 * where the test STOPS (never taps the final "Подтвердить", so <b>no money moves</b>). Both accounts
 * are the user's own.
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
    }

    @Test(description = "iOS: a between-own-accounts transfer reaches the confirmation screen (no money moved)")
    public void reachesConfirmationScreen() {
        AccountDetailPage detail = mainScreen.openFirstAccount();
        TransferPage transfer = detail.tapTransfer();
        Assert.assertTrue(transfer.isTransferEntryShown(),
                "Tapping 'Перевести' should open the 'Между своими счетами' transfer screen");

        // Source auto-selects a deposit (cannot be debited) → re-pick a funded current account.
        transfer.selectSourceIos(TransferPage.IOS_SOURCE_MARKER);
        transfer.selectDestinationIos(TransferPage.IOS_DESTINATION_MARKER);
        transfer.enterAmountIos("100");
        transfer.tapSubmitIos();

        // Must reach the review screen — the test stops here and never taps the final "Подтвердить".
        Assert.assertTrue(transfer.isConfirmationShownIos(),
                "Submitting should open the 'Подтверждение' review screen with a 'Подтвердить' button");
        Assert.assertTrue(transfer.confirmationShowsIos("100"),
                "The confirmation should show the entered amount (100)");
        Assert.assertTrue(transfer.confirmationShowsIos(TransferPage.IOS_DESTINATION_MARKER),
                "The confirmation should show the destination account (" + TransferPage.IOS_DESTINATION_MARKER + ")");
    }
}
