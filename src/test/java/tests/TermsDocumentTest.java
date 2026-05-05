package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.AgreementBottomSheet;
import pages.LanguageSelectionPage;
import pages.LanguageSelectionPage.Language;
import pages.PdfDocumentPage;
import pages.PermissionDialog;
import pages.PhoneLoginPage;
import pages.WelcomePage;

/**
 * Тесты PDF-вьюера, который открывается из bottom sheet "Условия Банка".
 * Оба пункта (Условия соглашений / Согласие на ПД) открывают одинаковый по структуре viewer
 * с одинаковым заголовком — содержимое PDF разное, но программно различить страницы
 * нельзя (рендерится как растровая картинка).
 * Покрывает: открытие viewer для каждого пункта, наличие страниц и кнопки "Принять",
 * возврат назад, последовательное открытие двух документов, сохранение введённого номера.
 */
public class TermsDocumentTest extends BaseTest {

    // Both documents open inside the same PDF viewer with the same toolbar title
    // ("Условия соглашений"). Document content (the PDF pages) differs but is rendered
    // as a raster image — we can't programmatically distinguish them without OCR.
    private static final String SHARED_PDF_TITLE = "Условия соглашений";

    private PhoneLoginPage phoneLoginPage;

    @BeforeMethod(alwaysRun = true)
    public void openAgreementSheet() {
        new LanguageSelectionPage(driver).selectLanguage(Language.RUSSIAN);
        WelcomePage welcome = new WelcomePage(driver);
        Assert.assertTrue(welcome.isDisplayed(), "Welcome screen should open");

        welcome.tapStart();
        new PermissionDialog(driver).acceptIfPresent();

        phoneLoginPage = new PhoneLoginPage(driver);
        Assert.assertTrue(phoneLoginPage.isDisplayed(), "Phone login screen must be open");
    }

    @Test(description = "Tapping 'Условия соглашений' opens the PDF viewer")
    public void tappingTermsOpensPdfViewer() {
        openSheet().tapTermsOfAgreements();

        PdfDocumentPage pdf = new PdfDocumentPage(driver);
        Assert.assertTrue(pdf.isDisplayed(), "PDF viewer should open");
        Assert.assertEquals(pdf.getTitle(), SHARED_PDF_TITLE,
                "PDF viewer toolbar title should be the shared one");
    }

    @Test(description = "Tapping 'Согласие на ПД' also opens the PDF viewer")
    public void tappingPersonalDataConsentOpensPdfViewer() {
        openSheet().tapPersonalDataConsent();

        PdfDocumentPage pdf = new PdfDocumentPage(driver);
        Assert.assertTrue(pdf.isDisplayed(), "PDF viewer should open");
        Assert.assertEquals(pdf.getTitle(), SHARED_PDF_TITLE,
                "PDF viewer toolbar title should be the shared one");
    }

    @Test(description = "PDF viewer renders pages and shows the Accept button")
    public void pdfHasPagesAndAcceptButton() {
        openSheet().tapTermsOfAgreements();

        PdfDocumentPage pdf = new PdfDocumentPage(driver);
        Assert.assertTrue(pdf.isDisplayed(), "PDF viewer should open");
        Assert.assertTrue(pdf.hasPdfPages(), "PDF should render at least one page");
        Assert.assertTrue(pdf.hasAcceptButton(), "PDF should show the Accept button");
    }

    @Test(description = "Back button from PDF returns user back to phone login screen")
    public void backFromPdfReturnsToPhoneLogin() {
        openSheet().tapTermsOfAgreements();
        PdfDocumentPage pdf = new PdfDocumentPage(driver);
        Assert.assertTrue(pdf.isDisplayed(), "PDF should be open");

        pdf.tapBack();

        Assert.assertTrue(phoneLoginPage.isDisplayed(),
                "Phone login should be visible after closing PDF");
    }

    @Test(description = "User can open the second document after closing the first one")
    public void canOpenBothDocumentsSequentially() {
        // open first document
        openSheet().tapTermsOfAgreements();
        PdfDocumentPage firstPdf = new PdfDocumentPage(driver);
        Assert.assertTrue(firstPdf.isDisplayed(), "First PDF should open");
        Assert.assertTrue(firstPdf.hasPdfPages(), "First PDF should render pages");
        firstPdf.tapBack();
        Assert.assertTrue(phoneLoginPage.isDisplayed(), "Should return to phone login");

        // open second document — verifies navigation is not stuck after the first one
        openSheet().tapPersonalDataConsent();
        PdfDocumentPage secondPdf = new PdfDocumentPage(driver);
        Assert.assertTrue(secondPdf.isDisplayed(), "Second PDF should open");
        Assert.assertTrue(secondPdf.hasPdfPages(), "Second PDF should also render pages");
    }

    @Test(description = "Phone digits are preserved after viewing a PDF document")
    public void phoneDigitsPreservedAfterViewingPdf() {
        phoneLoginPage.enterPhone("7771234567");
        String before = phoneLoginPage.getPhoneFieldText();
        Assert.assertTrue(before.contains("777"), "Sanity: digits should be in field");

        openSheet().tapTermsOfAgreements();
        PdfDocumentPage pdf = new PdfDocumentPage(driver);
        Assert.assertTrue(pdf.isDisplayed(), "PDF should open");
        pdf.tapBack();

        Assert.assertTrue(phoneLoginPage.isDisplayed(), "Should return to phone login");
        Assert.assertEquals(phoneLoginPage.getPhoneFieldText(), before,
                "Phone digits should be preserved after viewing the PDF");
    }

    private AgreementBottomSheet openSheet() {
        phoneLoginPage.tapAgreementLink();
        AgreementBottomSheet sheet = new AgreementBottomSheet(driver);
        Assert.assertTrue(sheet.isDisplayed(), "Agreement bottom sheet should open");
        return sheet;
    }
}
