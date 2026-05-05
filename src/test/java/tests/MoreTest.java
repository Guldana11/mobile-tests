package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.AboutBankPage;
import pages.LanguageSelectionPage;
import pages.LanguageSelectionPage.Language;
import pages.WelcomePage;

/**
 * Тесты экрана "О Банке" (открывается по кнопке "Еще" на Welcome):
 * проверяет переход и наличие текстовых секций (Кто мы, Наш акционер, Наши услуги).
 */
public class MoreTest extends BaseTest {

    private WelcomePage welcomePage;

    @BeforeMethod(alwaysRun = true)
    public void openWelcomeScreen() {
        new LanguageSelectionPage(driver).selectLanguage(Language.RUSSIAN);
        welcomePage = new WelcomePage(driver);
        Assert.assertTrue(welcomePage.isDisplayed(),
                "Welcome screen must be open before each More test");
    }

    @Test(description = "Tapping More opens the About Bank screen")
    public void tappingMoreOpensAboutBankScreen() {
        welcomePage.tapMore();
        AboutBankPage about = new AboutBankPage(driver);
        Assert.assertTrue(about.isDisplayed(),
                "About Bank screen should open after tapping More");
    }

    @Test(description = "About Bank screen contains the expected sections")
    public void aboutBankScreenContainsSections() {
        welcomePage.tapMore();
        AboutBankPage about = new AboutBankPage(driver);
        Assert.assertTrue(about.isDisplayed(), "About Bank screen should be open");

        Assert.assertTrue(about.hasSection("Кто мы"), "Should contain 'Кто мы' section");
        Assert.assertTrue(about.hasSection("Наш акционер"), "Should contain 'Наш акционер' section");
        Assert.assertTrue(about.hasSection("Наши услуги"), "Should contain 'Наши услуги' section");
    }
}
