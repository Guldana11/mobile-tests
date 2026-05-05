package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;
import pages.LanguageSelectionPage;
import pages.LanguageSelectionPage.Language;
import pages.WelcomePage;

/**
 * Тесты экрана выбора языка (первый экран при первом запуске приложения):
 * проверяет наличие 4 языков (Қазақша / Русский / English / 한국어)
 * и что выбор языка ведёт на экран Welcome.
 */
public class LanguageSelectionTest extends BaseTest {

    @Test(description = "Language selection screen is shown on first launch with all 4 languages")
    public void languageScreenIsDisplayed() {
        LanguageSelectionPage page = new LanguageSelectionPage(driver);
        Assert.assertTrue(page.isDisplayed(),
                "Language selection bottom sheet with 4 languages should be visible");
    }

    @Test(description = "Selecting Russian opens the Welcome screen")
    public void selectingRussianOpensWelcomeScreen() {
        LanguageSelectionPage langPage = new LanguageSelectionPage(driver);
        Assert.assertTrue(langPage.isDisplayed(), "Language screen should be visible before selecting");

        langPage.selectLanguage(Language.RUSSIAN);

        WelcomePage welcomePage = new WelcomePage(driver);
        Assert.assertTrue(welcomePage.isDisplayed(),
                "Welcome screen should appear after selecting a language");
    }
}
