package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.LanguageSelectionPage;
import pages.LanguageSelectionPage.Language;
import pages.WelcomePage;

/**
 * Тесты экрана Welcome (приветствие после выбора языка):
 * проверяет наличие основных элементов (карточка с описанием банка, кнопка Филиалы)
 * и что нажатие "Начать" ведёт пользователя дальше по флоу.
 */
public class WelcomeTest extends BaseTest {

    private WelcomePage welcomePage;

    @BeforeMethod(alwaysRun = true)
    public void openWelcomeScreen() {
        new LanguageSelectionPage(driver).selectLanguage(Language.RUSSIAN);
        welcomePage = new WelcomePage(driver);
        Assert.assertTrue(welcomePage.isDisplayed(),
                "Welcome screen should be open before each test");
    }

    @Test(description = "Welcome screen shows the bank info card and the Branches card")
    public void welcomeScreenHasMainElements() {
        Assert.assertTrue(welcomePage.hasInfoCard(), "Bank info card should be visible");
        Assert.assertTrue(welcomePage.hasBranchesCard(), "Branches card should be visible");
    }

    @Test(description = "Tapping the Start button leaves the Welcome screen")
    public void tappingStartLeavesWelcomeScreen() {
        welcomePage.tapStart();
        Assert.assertFalse(welcomePage.waitForDisplayed(java.time.Duration.ofSeconds(3)),
                "Welcome screen should disappear after tapping Start");
    }
}
