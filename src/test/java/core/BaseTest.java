package core;

import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class BaseTest {

    private static final Path ARTIFACTS_DIR = Paths.get("build", "test-artifacts");
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    protected AppiumDriver driver;

    private static final String ANDROID_PACKAGE = "kz.bnk.app.dev";

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        // Force-uninstall via ADB before driver creation. Appium's fullReset alone
        // doesn't reliably reinstall this app — we have to remove it ourselves.
        // Uninstalling the app does NOT affect the UiAutomator2 server (different package).
        if (Platform.current() == Platform.ANDROID) {
            uninstallAndroidApp();
        }
        driver = DriverFactory.create(Platform.current());
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
    }

    private void uninstallAndroidApp() {
        try {
            Process p = new ProcessBuilder("adb", "uninstall", ANDROID_PACKAGE)
                    .redirectErrorStream(true)
                    .start();
            p.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception e) {
            // Ignore — on the very first run the app might not be installed yet.
        }
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        if (driver == null) {
            return;
        }
        try {
            if (!result.isSuccess()) {
                saveFailureArtifacts(result);
            }
        } finally {
            driver.quit();
            driver = null;
        }
    }

    private void saveFailureArtifacts(ITestResult result) {
        String baseName = result.getTestClass().getRealClass().getSimpleName()
                + "_" + result.getMethod().getMethodName()
                + "_" + LocalDateTime.now().format(TIMESTAMP);

        try {
            Files.createDirectories(ARTIFACTS_DIR);
        } catch (IOException e) {
            System.err.println("[artifact] cannot create " + ARTIFACTS_DIR + ": " + e.getMessage());
            return;
        }

        Path png = ARTIFACTS_DIR.resolve(baseName + ".png");
        try {
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Files.write(png, screenshot);
            System.out.println("[artifact] screenshot: " + png.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("[artifact] screenshot failed: " + e.getMessage());
        }

        Path xml = ARTIFACTS_DIR.resolve(baseName + ".xml");
        try {
            Files.writeString(xml, driver.getPageSource());
            System.out.println("[artifact] page source: " + xml.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("[artifact] page source failed: " + e.getMessage());
        }
    }
}
