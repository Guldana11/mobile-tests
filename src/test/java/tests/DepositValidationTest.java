package tests;

import core.BaseTest;
import org.testng.Assert;
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
 * <p><b>Cross-platform.</b> Android uses native ids; iOS drives the form by labels — the source is picked
 * via the "Выберите счет" selector and the consent checkboxes toggle with an absolute tap just BELOW the
 * button's a11y frame (its visible circle sits lower). See {@link pages.DepositPage}.
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
