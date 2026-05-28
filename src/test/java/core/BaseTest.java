package core;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
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
    private static final Path DUMPS_DIR = Paths.get("inspector-dumps");
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    protected AppiumDriver driver;

    private static final String ANDROID_PACKAGE = "kz.bnk.app.dev";
    private static final String IOS_BUNDLE_ID = "ibn.bnk.kz";

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        // Force-uninstall before driver creation. Appium's fullReset alone doesn't reliably
        // reinstall this app — we have to remove it ourselves. Uninstalling does NOT affect
        // the Appium servers (UiAutomator2 / WebDriverAgent live in their own packages).
        switch (Platform.current()) {
            case ANDROID -> uninstallAndroidApp();
            case IOS -> uninstallIOSApp();
        }
        driver = DriverFactory.create(Platform.current());
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        dismissAndroidAnrDialogIfPresent();
    }

    /**
     * Quits the current session, reinstalls the app and starts a fresh session — equivalent to a
     * clean re-run of {@link #setUp()}. Used by tests that need a clean slate mid-test, e.g. to
     * retry a login flow with a fallback account after the primary one is not accepted.
     */
    protected void reinstallAndRestart() {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
        switch (Platform.current()) {
            case ANDROID -> uninstallAndroidApp();
            case IOS -> uninstallIOSApp();
        }
        driver = DriverFactory.create(Platform.current());
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        dismissAndroidAnrDialogIfPresent();
    }

    /**
     * The Android System UI can throw an "isn't responding" ANR dialog when the device
     * is under heavy reinstall pressure (our fullReset-per-test pattern triggers it).
     * The dialog overlays the app and blocks the accessibility tree, so any findElements
     * call returns empty even though the real UI is right behind it. Dismiss by tapping
     * "Wait" — that keeps the app running and the underlying screen accessible.
     */
    private void dismissAndroidAnrDialogIfPresent() {
        if (Platform.current() != Platform.ANDROID) return;
        try {
            var waitButtons = driver.findElements(
                    AppiumBy.androidUIAutomator("new UiSelector().text(\"Wait\")"));
            if (!waitButtons.isEmpty()) {
                waitButtons.get(0).click();
                System.out.println("[anr-dismiss] closed System UI ANR dialog");
            }
        } catch (Exception ignored) {
        }
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

    private void uninstallIOSApp() {
        try {
            Process p = new ProcessBuilder("xcrun", "simctl", "uninstall", "booted", IOS_BUNDLE_ID)
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
            if (Boolean.getBoolean("dumpPageSource")) {
                savePageSourceDump(result);
            }
        } finally {
            driver.quit();
            driver = null;
        }
    }

    // Saves the current screen XML to inspector-dumps/<platform>/ so we can build locators
    // for the other platform without re-running the app each time. Opt-in via -DdumpPageSource=true.
    private void savePageSourceDump(ITestResult result) {
        Path dir = DUMPS_DIR.resolve(Platform.current().name().toLowerCase());
        String baseName = result.getTestClass().getRealClass().getSimpleName()
                + "_" + result.getMethod().getMethodName()
                + "_" + LocalDateTime.now().format(TIMESTAMP);
        try {
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(baseName + ".xml"), driver.getPageSource());
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            Files.write(dir.resolve(baseName + ".png"), screenshot);
            System.out.println("[dump] " + dir.resolve(baseName).toAbsolutePath() + ".{xml,png}");
        } catch (Exception e) {
            System.err.println("[dump] failed: " + e.getMessage());
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
