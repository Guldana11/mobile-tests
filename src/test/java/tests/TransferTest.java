package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.AccountDetailPage;
import pages.MainScreenPage;
import pages.TransferPage;

/**
 * Transfer between the user's own accounts — happy path up to the confirmation screen (EPIC 1 / T-05).
 * The test drives the full flow (open an account → "Перевести" → pick a distinct own destination →
 * enter an amount → "Продолжить") and asserts the "Подтверждение" review screen appears. It STOPS
 * there and never taps the final "Подтвердить", so <b>no money is moved</b> — the run is deterministic
 * and side-effect free.
 *
 * <p>Reset-per-method (the BaseTest default): the flow navigates deep into a modal sheet, so — like
 * {@link AccountDetailTest} — each case starts from a fresh login rather than recovering a shared
 * session.
 *
 * <p><b>Android-only.</b> The iOS transfer screen differs and is blocked upstream by the iOS
 * account-open limitation (see {@link AccountDetailTest} / {@link TransferPage}); iOS is a separate
 * task. Registered in android.xml only.
 */
public class TransferTest extends BaseTest {

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

    @Test(description = "Transfer between own accounts reaches the confirmation screen (no money moved)")
    public void transferBetweenOwnAccountsReachesConfirmation() {
        AccountDetailPage detail = mainScreen.openFirstAccount();
        Assert.assertTrue(detail.isDisplayed(), "Account detail should open");

        TransferPage transfer = detail.tapTransfer();
        Assert.assertTrue(transfer.isAmountSheetShown(),
                "'Перевести' should open the 'Сумма перевода' sheet with an amount field and Продолжить");

        // Choose a distinct OWN account as the destination.
        transfer.openDestinationPicker();
        Assert.assertTrue(transfer.isAccountPickerShown(),
                "Tapping the destination card should open the 'Выберите счет' account picker");
        transfer.selectOwnDestination();

        transfer.enterAmount("1000");
        transfer.tapContinue();

        // We must reach the confirmation screen — but the test stops here and never confirms.
        Assert.assertTrue(transfer.isConfirmationShown(),
                "Продолжить should lead to the 'Подтверждение' review screen with a 'Подтвердить' button");
        Assert.assertTrue(transfer.confirmationShowsAmount("1 000"),
                "The confirmation screen should show the entered amount (1 000)");
    }
}
