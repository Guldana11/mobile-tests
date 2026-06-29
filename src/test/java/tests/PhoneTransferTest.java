package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.MainScreenPage;
import pages.PhoneTransferPage;

/**
 * Transfer to another person by phone number — "Перевод внутри банка → По номеру телефона"
 * (EPIC 1 / T-06), happy path up to the confirmation screen.
 *
 * <p>The recipient is the SECOND test account (its phone {@link LoginFlow#FALLBACK}, resolving to
 * "ГАУХАР С.") — a controlled within-bank recipient. The test fills the phone, amount and terms
 * checkbox, submits, and asserts the "Подтверждение" review screen shows the recipient and amount. It
 * STOPS there and never taps the final "Подтвердить", so <b>no money moves</b>.
 *
 * <p>A second case goes the whole way: it taps "Подтвердить", enters the mock SMS code "0000" and
 * asserts the operation-status (receipt) screen — this one <b>actually completes the transfer</b>
 * (1 000 ₸ from the primary test account to the second one, ГАУХАР С.). Both are within-bank test
 * accounts with large balances, so the movement is sustainable for a regression suite.
 *
 * <p>Reset-per-method (the BaseTest default): the flow navigates deep, so each case logs in fresh
 * (like {@link TransferTest}). <b>Cross-platform</b> — unlike the "Между своими счетами" transfer, this
 * flow's submit responds on iOS, so the full happy path reaches the confirmation on both platforms.
 * (The completing case is currently verified on iOS; Android's SMS/status screens still need a run.)
 */
public class PhoneTransferTest extends BaseTest {

    private MainScreenPage mainScreen;

    @BeforeMethod(alwaysRun = true)
    public void reachMainScreen() {
        mainScreen = LoginFlow.reachMainScreen(driver, LoginFlow.PRIMARY);
        if (mainScreen == null) {
            reinstallAndRestart();
            mainScreen = LoginFlow.reachMainScreen(driver, LoginFlow.FALLBACK);
        }
        Assert.assertNotNull(mainScreen,
                "Main screen must open after completing login and PIN setup");
    }

    @Test(description = "Transfer by phone number reaches the confirmation screen (no money moved)")
    public void transferByPhoneReachesConfirmation() {
        PhoneTransferPage transfer = mainScreen.openInBankTransfer();
        Assert.assertTrue(transfer.isShown(),
                "'Перевод внутри банка' should open the by-phone transfer form");

        // Recipient = the second test account's phone; entering it resolves the recipient name.
        transfer.enterPhone(LoginFlow.FALLBACK.phone());
        Assert.assertTrue(transfer.isRecipientResolved(),
                "Entering the recipient phone should resolve a recipient ('Получит перевод в BNK')");
        Assert.assertTrue(transfer.recipientShown("ГАУХАР"),
                "The resolved recipient should be the second test account (ГАУХАР)");

        transfer.enterAmount("1000");
        transfer.acceptTerms();
        transfer.tapTransfer();

        // Must reach the confirmation screen — but the test stops here and never confirms.
        Assert.assertTrue(transfer.isConfirmationShown(),
                "Submitting should open the 'Подтверждение' review screen with a 'Подтвердить' button");
        Assert.assertTrue(transfer.confirmationShows("1 000"),
                "The confirmation should show the entered amount (1 000)");
        Assert.assertTrue(transfer.confirmationShows("ГАУХАР"),
                "The confirmation should show the recipient (ГАУХАР)");
    }

    @Test(description = "Completing the by-phone transfer with the mock SMS code reaches the operation-status screen")
    public void transferByPhoneCompletesWithSmsCode() {
        PhoneTransferPage transfer = mainScreen.openInBankTransfer();
        Assert.assertTrue(transfer.isShown(),
                "'Перевод внутри банка' should open the by-phone transfer form");

        transfer.enterPhone(LoginFlow.FALLBACK.phone());
        Assert.assertTrue(transfer.isRecipientResolved(),
                "Entering the recipient phone should resolve a recipient ('Получит перевод в BNK')");

        transfer.enterAmount("1000");
        transfer.acceptTerms();
        transfer.tapTransfer();
        Assert.assertTrue(transfer.isConfirmationShown(),
                "Submitting should open the 'Подтверждение' review screen");

        // Confirm → SMS-code screen → mock code "0000" → operation-status screen. THIS MOVES MONEY.
        transfer.tapConfirm();
        Assert.assertTrue(transfer.isSmsCodeShown(),
                "Tapping 'Подтвердить' should open the SMS-code screen ('Введите код')");

        transfer.enterSmsCode("0000");
        Assert.assertTrue(transfer.isOperationStatusShown(),
                "Entering the mock code should reach the operation-status screen ('Номер операции')");
        Assert.assertTrue(transfer.statusShows("ГАУХАР"),
                "The operation status should show the recipient (ГАУХАР)");
        Assert.assertTrue(transfer.statusShows("1 000"),
                "The operation status should show the transferred amount (1 000)");
    }
}
