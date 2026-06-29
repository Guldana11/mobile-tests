package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.MainScreenPage;
import pages.PhoneTransferPage;

/**
 * Within-bank transfer BY ACCOUNT NUMBER ("По номеру счёта", EPIC 1).
 *
 * <p>Logged in as PRIMARY, the test opens the in-bank transfer, switches to the "По номеру счета" tab
 * and sends to Гаухар (recipient IIN {@link #RECIPIENT_IIN} + account {@link #RECIPIENT_ACCOUNT}). The
 * by-account form is the same as the by-phone one but with extra fields — entering the IIN + account
 * resolves the recipient inline ("Получит перевод …"), then a source account, a КНП (payment purpose),
 * an amount and the terms checkbox are filled. The test taps "Перевести" to reach the "Подтверждение"
 * review screen and STOPS there — it never taps the final "Подтвердить", so <b>no money moves</b>.
 *
 * <p><b>Cross-platform.</b> On Android the form has clean resource-ids and the source account comes
 * pre-selected (a funded current account), so {@link PhoneTransferPage#selectSourceAccount} keeps the
 * default and {@link #SOURCE_MARKER} only matters on iOS. <b>iOS quirk handled:</b> the form is tall, so
 * the on-screen keyboard covers the agree row + submit (the checkbox is not hittable and "Перевести"
 * stays disabled) — {@link PhoneTransferPage#dismissKeyboard()} must run before accepting the terms (on
 * Android it just submits the IME). A fresh login per case.
 */
public class AccountNumberTransferTest extends BaseTest {

    private static final String RECIPIENT_IIN = "750413402142";       // Гаухар
    private static final String RECIPIENT_ACCOUNT = "220400080";       // last 9 of KZ74724BKZT220400080
    private static final String RECIPIENT_NAME = "ГАУХАР";             // resolved on the form + confirmation
    private static final String SOURCE_MARKER = "400132";              // iOS: PRIMARY's funded KZT account (Android keeps its default)
    private static final String KNP_CODE = "119";                      // Прочие безвозмездные переводы денег
    private static final String AMOUNT = "1000";

    private MainScreenPage mainScreen;

    @BeforeMethod(alwaysRun = true)
    public void reachMainScreen() {
        mainScreen = LoginFlow.reachMainScreen(driver, LoginFlow.PRIMARY);
        if (mainScreen == null) {
            reinstallAndRestart();
            mainScreen = LoginFlow.reachMainScreen(driver, LoginFlow.PRIMARY);
        }
        Assert.assertNotNull(mainScreen, "Main screen must open after completing login and PIN setup");
    }

    @Test(description = "Entering a recipient's IIN + account number resolves the recipient name")
    public void recipientResolvesByAccountNumber() {
        PhoneTransferPage transfer = mainScreen.openInBankTransfer();
        Assert.assertTrue(transfer.isShown(), "The transfer form should open");

        transfer.switchToByAccount();
        transfer.enterAccountRecipient(RECIPIENT_IIN, RECIPIENT_ACCOUNT);

        Assert.assertTrue(transfer.isRecipientResolved(),
                "Entering the IIN + account should resolve a recipient ('Получит перевод …')");
        Assert.assertTrue(transfer.recipientShown(RECIPIENT_NAME),
                "The resolved recipient should be Гаухар (" + RECIPIENT_NAME + ")");
    }

    @Test(description = "A by-account transfer reaches the confirmation screen (no money moved)")
    public void reachesConfirmationScreen() {
        PhoneTransferPage transfer = mainScreen.openInBankTransfer();
        Assert.assertTrue(transfer.isShown(), "The transfer form should open");

        transfer.switchToByAccount();
        transfer.enterAccountRecipient(RECIPIENT_IIN, RECIPIENT_ACCOUNT);
        Assert.assertTrue(transfer.isRecipientResolved(), "Recipient should resolve before proceeding");

        transfer.selectSourceAccount(SOURCE_MARKER);
        transfer.selectKnp(KNP_CODE);
        transfer.enterAmount(AMOUNT);
        transfer.dismissKeyboard();   // keyboard covers the agree row + submit → must be dismissed first
        transfer.acceptTerms();

        Assert.assertTrue(transfer.isSubmitEnabled(),
                "Once recipient, source, КНП, amount and terms are set, 'Перевести' should be enabled");
        transfer.tapTransfer();

        // Must reach the review screen — the test STOPS here and never taps the final "Подтвердить".
        Assert.assertTrue(transfer.isConfirmationShown(),
                "Submitting should open the 'Подтверждение' review screen with a 'Подтвердить' button");
        Assert.assertTrue(transfer.confirmationShows(RECIPIENT_NAME),
                "The confirmation should show the recipient (" + RECIPIENT_NAME + ")");
    }
}
