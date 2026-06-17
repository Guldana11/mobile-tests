package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.MainScreenPage;

/**
 * Тесты главного (домашнего) экрана, который открывается после полного входа: вход по паролю →
 * создание и подтверждение PIN-кода → закрытие онбординг-промптов (Face ID, уведомления на iOS).
 * Вход выполняется основным аккаунтом с откатом на запасной (см. {@link LoginFlow}). Имя в
 * приветствии зависит от аккаунта, поэтому проверяется только префикс «Привет,».
 *
 * <p>Все кейсы только ЧИТАЮТ один и тот же экран, поэтому до главного экрана доходим ОДИН раз на
 * класс ({@link #resetBeforeEachMethod()} = false) и переиспользуем сессию между кейсами — без
 * переустановки приложения на каждый кейс (быстрее и не нагружает эмулятор). Чтобы кейс со скроллом
 * не ломал остальные, перед каждым кейсом позиция скролла нормализуется к началу.
 */
public class MainScreenTest extends BaseTest {

    private MainScreenPage mainScreen;

    @Override
    protected boolean resetBeforeEachMethod() {
        return false;
    }

    @BeforeMethod(alwaysRun = true)
    public void ensureMainScreen() {
        if (mainScreen == null) {
            // Первый кейс класса: один раз доводим до главного экрана.
            mainScreen = LoginFlow.reachMainScreen(driver, LoginFlow.PRIMARY);
            if (mainScreen == null) {
                reinstallAndRestart();
                mainScreen = LoginFlow.reachMainScreen(driver, LoginFlow.FALLBACK);
            }
            Assert.assertNotNull(mainScreen,
                    "Main screen must open after completing login and PIN setup");
        } else {
            // Переиспользуемая сессия: если приложение авто-залочилось («Введите код») за время
            // прогона — разблокировать PIN-ом; закрыть боковое меню если открыто; вернуться на
            // «Главную» (кейсы вкладок уводят с неё) и к началу списка (кейс скролла) — чтобы кейсы
            // не зависели от порядка.
            mainScreen.unlockIfLocked(LoginFlow.PIN);
            mainScreen.dismissSideMenuIfOpen();
            mainScreen.openHomeTab();
            mainScreen.scrollToTop();
        }
    }

    @Test(description = "Completing PIN setup opens the main screen with its bottom tabs")
    public void pinSetupOpensMainScreen() {
        Assert.assertTrue(mainScreen.isDisplayed(), "Main screen should be visible");
        Assert.assertTrue(mainScreen.hasExpectedTabs(),
                "Bottom navigation should expose 'Главная', 'Продукты' and 'Быстрое меню'");
    }

    @Test(description = "Main screen greets the logged-in user")
    public void mainScreenGreetsUser() {
        Assert.assertTrue(mainScreen.hasGreeting(),
                "Main screen should show the 'Привет, <name>' greeting");
    }

    @Test(description = "Main screen renders the account list with balances")
    public void mainScreenShowsAccounts() {
        Assert.assertTrue(mainScreen.hasAccountBalances(),
                "Main screen should list at least one account balance (a currency amount)");
    }

    @Test(description = "Tapping the Продукты tab opens the products screen")
    public void productsTabOpensProductsScreen() {
        mainScreen.openProductsTab();
        Assert.assertTrue(mainScreen.isProductsShown(),
                "Tapping the Продукты tab should open the products screen");
    }

    @Test(description = "Tapping the Быстрое меню tab opens the quick menu")
    public void quickMenuTabOpensQuickMenu() {
        mainScreen.openQuickMenuTab();
        Assert.assertTrue(mainScreen.isQuickMenuShown(),
                "Tapping the Быстрое меню tab should open the quick menu ('Перевод в другой банк')");
    }

    @Test(description = "Tapping the header burger opens the side menu")
    public void burgerOpensSideMenu() {
        mainScreen.openSideMenu();
        Assert.assertTrue(mainScreen.isSideMenuShown(),
                "The side menu ('Служба поддержки') should open from the header burger");
    }

    @Test(description = "Scrolling the account list reveals the bottom actions")
    public void accountListScrolls() {
        // The bottom-most action starts below the fold...
        Assert.assertFalse(mainScreen.isBottomActionVisible(),
                "'Открыть депозит' should be off-screen before scrolling");
        // ...and scrolling the list down brings it into view, proving the list scrolls.
        mainScreen.scrollAccountsDown();
        Assert.assertTrue(mainScreen.isBottomActionVisible(),
                "'Открыть депозит' should be visible after scrolling the account list down");
    }
}
