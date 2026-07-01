package tests;

import core.BaseTest;
import core.Platform;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.DepositPage;
import pages.MainScreenPage;

/**
 * Negative checks for the deposit application form ("Открыть депозит", sibling of {@link DepositOpenTest}).
 * Both are NON-DESTRUCTIVE — they stop at the disabled submit and never open a real deposit:
 * <ul>
 *   <li><b>consent-gated submit</b> — with currency/source/amount filled, "Открыть депозит" stays disabled
 *       until all three consents are accepted (and enables once they are).</li>
 *   <li><b>below-minimum amount</b> — an amount under the product minimum keeps the submit disabled.</li>
 * </ul>
 *
 * <p><b>Android-only</b> (the deposit form's clean ids / native consent CheckBoxes; iOS consents don't
 * toggle — see {@link DepositOpenTest}).
 */
public class DepositValidationTest extends BaseTest {

    private static final String PRODUCT = "Срочный";
    private static final String CURRENCY = "KZT";
    private static final String SOURCE_MARKER = "400132";
    private static final String VALID_AMOUNT = "1000";   // Срочный minimum
    private static final String BELOW_MIN_AMOUNT = "500"; // under the 1 000 ₸ minimum

    private MainScreenPage mainScreen;

    @BeforeMethod(alwaysRun = true)
    public void reachMainScreen() {
        if (Platform.current() != Platform.ANDROID) {
            throw new SkipException("Deposit validations are Android-only (iOS consent checkboxes don't toggle)");
        }
        mainScreen = LoginFlow.reachMainScreen(driver, LoginFlow.PRIMARY);
        if (mainScreen == null) {
            reinstallAndRestart();
            mainScreen = LoginFlow.reachMainScreen(driver, LoginFlow.PRIMARY);
        }
        Assert.assertNotNull(mainScreen, "Main screen must open after completing login and PIN setup");
    }

    @Test(groups = "deposit", description = "'Открыть депозит' stays disabled until all consents are accepted")
    public void submitDisabledWithoutConsent() {
        DepositPage deposit = openTermForm();
        deposit.selectCurrency(CURRENCY);
        deposit.selectSourceAccount(SOURCE_MARKER);
        deposit.enterAmount(VALID_AMOUNT);
        // Consents NOT accepted yet.

        Assert.assertFalse(deposit.isOpenEnabled(),
                "'Открыть депозит' must stay disabled while the consents are not accepted");

        deposit.acceptAllConsents();
        Assert.assertTrue(deposit.isOpenEnabled(),
                "'Открыть депозит' must become enabled once all consents are accepted");
    }

    @Test(groups = "deposit", description = "An amount below the product minimum keeps 'Открыть депозит' disabled")
    public void belowMinimumAmountDisablesSubmit() {
        DepositPage deposit = openTermForm();
        deposit.selectCurrency(CURRENCY);
        deposit.selectSourceAccount(SOURCE_MARKER);
        deposit.enterAmount(BELOW_MIN_AMOUNT);   // 500 < 1 000 minimum
        deposit.acceptAllConsents();

        Assert.assertFalse(deposit.isOpenEnabled(),
                "An amount below the minimum must keep 'Открыть депозит' disabled");
    }

    private DepositPage openTermForm() {
        DepositPage deposit = mainScreen.openDepositList();
        Assert.assertTrue(deposit.isProductListShown(), "The 'Депозиты' product list should open");
        deposit.selectProduct(PRODUCT);
        deposit.openApplicationForm();
        Assert.assertTrue(deposit.isFormShown(), "The deposit application form should open");
        return deposit;
    }
}
