package tests;

import core.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Smoke test: проверяет что приложение в принципе запускается и отрисовывает экран.
 * Самый базовый санити-тест — должен пройти раньше всех остальных.
 */
public class SmokeTest extends BaseTest {

    @Test(description = "Application launches and renders a screen")
    public void appLaunches() {
        Assert.assertNotNull(driver, "Driver should be initialized");
        String pageSource = driver.getPageSource();
        Assert.assertNotNull(pageSource, "App should render a screen");
        Assert.assertFalse(pageSource.isBlank(), "Page source should not be blank");
    }
}
