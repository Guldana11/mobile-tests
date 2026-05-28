package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.AgreementBottomSheet;
import pages.LanguageSelectionPage;
import pages.LanguageSelectionPage.Language;
import pages.PermissionDialog;
import pages.PhoneLoginPage;
import pages.WelcomePage;

/**
 * Тесты кликабельной ссылки "условиями Банка" в тексте согласия на экране ввода телефона.
 * Ссылка открывает bottom sheet "Условия Банка" с двумя пунктами документов.
 * Покрывает: открытие sheet, наличие пунктов, закрытие, сохранение введённого номера,
 * работу при открытой клавиатуре, доступность кнопки "Продолжить" после закрытия.
 */
public class AgreementLinkTest extends BaseTest {

    private PhoneLoginPage phoneLoginPage;

    @BeforeMethod(alwaysRun = true)
    public void openPhoneLogin() {
        new LanguageSelectionPage(driver).selectLanguage(Language.RUSSIAN);
        WelcomePage welcome = new WelcomePage(driver);
        Assert.assertTrue(welcome.isDisplayed(), "Welcome screen should open");

        welcome.tapStart();
        new PermissionDialog(driver).acceptIfPresent();

        phoneLoginPage = new PhoneLoginPage(driver);
        Assert.assertTrue(phoneLoginPage.isDisplayed(),
                "Phone login screen must be open before each test");
    }

    @Test(description = "Tapping the 'условиями Банка' link opens the Bank Terms bottom sheet")
    public void tappingLinkOpensBottomSheet() {
        phoneLoginPage.tapAgreementLink();
        AgreementBottomSheet sheet = new AgreementBottomSheet(driver);
        Assert.assertTrue(sheet.isDisplayed(),
                "Bank Terms bottom sheet should appear");
        // Title casing differs across platforms: Android "Условия Банка" vs iOS "Условия банка".
        Assert.assertTrue(sheet.getTitle().equalsIgnoreCase("Условия Банка"),
                "Bottom sheet title should be the Bank Terms title, was: " + sheet.getTitle());
    }

    @Test(description = "Bottom sheet contains both terms documents")
    public void bottomSheetHasBothItems() {
        phoneLoginPage.tapAgreementLink();
        AgreementBottomSheet sheet = new AgreementBottomSheet(driver);
        Assert.assertTrue(sheet.isDisplayed(), "Sheet should be visible");
        Assert.assertTrue(sheet.hasTermsOfAgreementsItem(),
                "Sheet should contain 'Условия соглашений' item");
        Assert.assertTrue(sheet.hasPersonalDataConsentItem(),
                "Sheet should contain 'Согласие на сбор...' item");
    }

    @Test(description = "Tapping outside the sheet dismisses it and returns to phone login")
    public void dismissingSheetReturnsToPhoneLogin() {
        phoneLoginPage.tapAgreementLink();
        AgreementBottomSheet sheet = new AgreementBottomSheet(driver);
        Assert.assertTrue(sheet.isDisplayed(), "Sheet should be visible");

        sheet.dismissByTappingOutside();

        Assert.assertTrue(phoneLoginPage.isDisplayed(),
                "Phone login should be visible again after dismissing the sheet");
    }

    @Test(description = "Phone digits are preserved after opening and closing the bottom sheet")
    public void phoneDigitsPreservedAfterOpeningSheet() {
        phoneLoginPage.enterPhone("7771234567");
        String before = phoneLoginPage.getPhoneFieldText();
        Assert.assertTrue(before.contains("777"),
                "Sanity check: digits should be in the field, was: " + before);

        phoneLoginPage.tapAgreementLink();
        AgreementBottomSheet sheet = new AgreementBottomSheet(driver);
        Assert.assertTrue(sheet.isDisplayed(), "Sheet should be visible");
        sheet.dismissByTappingOutside();

        String after = phoneLoginPage.getPhoneFieldText();
        Assert.assertEquals(after, before,
                "Phone field value should be preserved after opening and closing the sheet");
    }

    @Test(description = "Tapping the link with the keyboard open still opens the bottom sheet")
    public void linkWorksWhenKeyboardIsOpen() {
        phoneLoginPage.tapPhoneField();
        Assert.assertTrue(phoneLoginPage.isKeyboardShown(),
                "Keyboard should be visible after tapping phone field");

        phoneLoginPage.tapAgreementLink();

        AgreementBottomSheet sheet = new AgreementBottomSheet(driver);
        Assert.assertTrue(sheet.isDisplayed(),
                "Bottom sheet should still appear when the keyboard is open");
    }

    @Test(description = "Continue button is still visible after closing the bottom sheet")
    public void continueButtonVisibleAfterClosingSheet() {
        phoneLoginPage.tapAgreementLink();
        AgreementBottomSheet sheet = new AgreementBottomSheet(driver);
        Assert.assertTrue(sheet.isDisplayed(), "Sheet should be visible");
        sheet.dismissByTappingOutside();

        Assert.assertTrue(phoneLoginPage.hasContinueButton(),
                "Continue button should still be visible after closing the sheet");
    }
}
