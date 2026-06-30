package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.MainScreenPage;
import pages.OtherBankTransferPage;

/**
 * Inter-bank transfer ("Перевод в другой банк", EPIC 1). Logged in as PRIMARY, the test opens the
 * transfer from the Быстрое меню, fills the recipient's IBAN + ИИН/БИН, picks the recipient bank's БИК
 * (Kaspi = {@link #RECIPIENT_BIC}) and a КНП, enters a purpose + amount, accepts the terms and taps
 * "Перевести" to reach the "Подтверждение" review screen — it STOPS there, so <b>no money moves</b>.
 *
 * <p><b>Cross-platform.</b> On Android the source account is picked ({@link #SOURCE_MARKER}); on iOS a
 * funded source is pre-selected so the marker is ignored. The БИК must match the recipient account's
 * bank (Kaspi here) or the form rejects it ("БИК неверный").
 */
public class OtherBankTransferTest extends BaseTest {

    private static final String RECIPIENT_ACCOUNT = "KZ66722C000039376798"; // recipient IBAN (Kaspi)
    private static final String RECIPIENT_IIN = "030205651456";             // recipient ИИН/БИН
    private static final String RECIPIENT_BIC = "CASPKZKA";                 // Kaspi Bank БИК (must match the account's bank)
    private static final String SOURCE_MARKER = "400132";                   // PRIMARY's funded KZT account (Android; iOS keeps its default)
    private static final String KNP_CODE = "119";                           // Прочие безвозмездные переводы
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

    @Test(description = "An inter-bank transfer reaches the confirmation screen (no money moved)")
    public void reachesConfirmationScreen() {
        OtherBankTransferPage transfer = mainScreen.openOtherBankTransfer();
        Assert.assertTrue(transfer.isShown(), "The inter-bank transfer form should open");

        transfer.selectSourceAccount(SOURCE_MARKER);
        transfer.enterRecipientAccount(RECIPIENT_ACCOUNT);
        transfer.enterIin(RECIPIENT_IIN);
        transfer.selectBic(RECIPIENT_BIC);
        transfer.selectKnp(KNP_CODE);
        transfer.enterPurpose(PURPOSE);
        transfer.enterAmount(AMOUNT);
        transfer.acceptTerms();

        Assert.assertTrue(transfer.isSubmitEnabled(),
                "Once source, recipient, БИК, КНП, purpose, amount and terms are set, 'Перевести' should be enabled");
        transfer.tapTransfer();

        // Must reach the review screen — the test STOPS here and never taps the final "Подтвердить".
        Assert.assertTrue(transfer.isConfirmationShown(),
                "Submitting should open the 'Подтверждение' review screen with a 'Подтвердить' button");
    }
}
