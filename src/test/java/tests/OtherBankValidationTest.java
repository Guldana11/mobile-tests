package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.MainScreenPage;
import pages.OtherBankTransferPage;

import java.time.Duration;

/**
 * Negative checks for the "Перевод в другой банк" form (EPIC 1, sibling of {@link OtherBankTransferTest}).
 * Both cases stop before any money moves:
 * <ul>
 *   <li><b>submit gated by consent</b> — with every field filled, "Перевести" stays disabled until the
 *       terms checkbox is accepted (and enables once it is).</li>
 *   <li><b>wrong БИК rejected</b> — a БИК that does not match the recipient account's bank is refused on
 *       submit ("Некоторые поля неверно заполнены") and the confirmation screen is NOT reached.</li>
 * </ul>
 *
 * <p><b>Cross-platform.</b> The validations reuse the cross-platform {@link OtherBankTransferPage}; on
 * iOS {@code autoAcceptAlerts} dismisses the "Некоторые поля неверно заполнены" alert for us, so the
 * inline error is read directly.
 */
public class OtherBankValidationTest extends BaseTest {

    private static final String RECIPIENT_ACCOUNT = "KZ66722C000039376798"; // Kaspi IBAN
    private static final String RECIPIENT_IIN = "030205651456";
    private static final String CORRECT_BIC = "CASPKZKA";                   // Kaspi — matches the account
    private static final String WRONG_BIC = "HSBKKZKX";                     // Halyk — does NOT match the account
    private static final String SOURCE_MARKER = "400132";
    private static final String KNP_CODE = "119";
    private static final String PURPOSE = "Перевод";
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

    @Test(description = "'Перевести' stays disabled until the terms checkbox is accepted")
    public void submitDisabledWithoutConsent() {
        OtherBankTransferPage transfer = mainScreen.openOtherBankTransfer();
        Assert.assertTrue(transfer.isShown(), "The inter-bank transfer form should open");

        transfer.selectSourceAccount(SOURCE_MARKER);
        transfer.enterRecipientAccount(RECIPIENT_ACCOUNT);
        transfer.enterIin(RECIPIENT_IIN);
        transfer.selectBic(CORRECT_BIC);
        transfer.selectKnp(KNP_CODE);
        transfer.enterPurpose(PURPOSE);
        transfer.enterAmount(AMOUNT);
        // Terms NOT accepted yet.

        Assert.assertFalse(transfer.isSubmitEnabled(),
                "'Перевести' must stay disabled while the terms are not accepted");

        transfer.acceptTerms();
        Assert.assertTrue(transfer.isSubmitEnabled(),
                "'Перевести' must become enabled once the terms are accepted");
    }

    @Test(description = "A БИК that doesn't match the recipient account's bank is rejected (client-side)")
    public void wrongBicRejected() {
        OtherBankTransferPage transfer = mainScreen.openOtherBankTransfer();
        Assert.assertTrue(transfer.isShown(), "The inter-bank transfer form should open");

        transfer.selectSourceAccount(SOURCE_MARKER);
        transfer.enterRecipientAccount(RECIPIENT_ACCOUNT);
        transfer.enterIin(RECIPIENT_IIN);
        transfer.selectBic(WRONG_BIC);              // Halyk БИК on a Kaspi account → mismatch
        transfer.selectKnp(KNP_CODE);
        transfer.enterPurpose(PURPOSE);
        transfer.enterAmount(AMOUNT);
        transfer.acceptTerms();

        // The app validates the БИК against the IBAN client-side: it pops a "Некоторые поля неверно
        // заполнены" alert, flags the mismatch inline and keeps "Перевести" disabled. Dismiss the alert so
        // the underlying inline error is readable, then assert the specific error + that submit stays off.
        transfer.dismissValidationAlert();
        Assert.assertTrue(transfer.isBicMismatchErrorShown(Duration.ofSeconds(10)),
                "A mismatched БИК must be flagged inline ('IBAN не принадлежит выбранному банку')");
        Assert.assertFalse(transfer.isSubmitEnabled(),
                "A mismatched БИК must keep 'Перевести' disabled — the transfer cannot proceed");
    }
}
