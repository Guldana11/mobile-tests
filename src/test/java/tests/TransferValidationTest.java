package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.MainScreenPage;
import pages.PhoneTransferPage;

/**
 * Validation of the by-phone (within-bank) transfer form (EPIC 1 / T-08). Verifies the form rejects
 * invalid input <b>before</b> any money could move:
 *
 * <ul>
 *   <li><b>Empty fields</b> — the "Перевести" submit is disabled until a recipient resolves AND an
 *       amount is entered AND the terms are accepted; it becomes enabled once they are.</li>
 *   <li><b>Over the per-transfer limit</b> — an amount above the cap (100 000 000 ₸) is rejected with
 *       "должно быть меньше или равно 100000000" and never reaches the confirmation screen.</li>
 * </ul>
 *
 * <p>Reset-per-method (the BaseTest default): each case logs in fresh, like {@link PhoneTransferTest}.
 * No money moves (the over-limit case is rejected; the gating case never submits). Cross-platform —
 * verified on iOS; the Android branch reuses the same locators (pending a live Android run).
 */
public class TransferValidationTest extends BaseTest {

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

    @Test(description = "Submit stays disabled until amount and terms are provided, then enables")
    public void submitGatedUntilFormIsValid() {
        PhoneTransferPage transfer = mainScreen.openInBankTransfer();
        Assert.assertTrue(transfer.isShown(), "The by-phone transfer form should open");

        transfer.enterPhone(LoginFlow.FALLBACK.phone());
        Assert.assertTrue(transfer.isRecipientResolved(), "The recipient should resolve");

        // Recipient resolved but no amount and no terms → submit must be disabled.
        Assert.assertFalse(transfer.isSubmitEnabled(),
                "'Перевести' must be disabled while the amount is empty and terms are not accepted");

        transfer.enterAmount("1000");
        transfer.acceptTerms();
        Assert.assertTrue(transfer.isSubmitEnabled(),
                "'Перевести' should become enabled once a valid amount and the terms are provided");
    }

    @Test(description = "An amount over the per-transfer limit is rejected (no confirmation)")
    public void amountOverLimitIsRejected() {
        PhoneTransferPage transfer = mainScreen.openInBankTransfer();
        Assert.assertTrue(transfer.isShown(), "The by-phone transfer form should open");

        transfer.enterPhone(LoginFlow.FALLBACK.phone());
        Assert.assertTrue(transfer.isRecipientResolved(), "The recipient should resolve");

        transfer.enterAmount("99999999999");   // far above the 100 000 000 ₸ cap
        transfer.acceptTerms();
        transfer.tapTransfer();

        Assert.assertTrue(transfer.isOverLimitErrorShown(),
                "An over-limit amount should show the 'должно быть меньше…' validation error");
        Assert.assertFalse(transfer.confirmationShows("Подтверждение"),
                "An over-limit amount must NOT reach the 'Подтверждение' confirmation screen");
    }
}
