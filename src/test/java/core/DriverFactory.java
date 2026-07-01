package core;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.ios.options.XCUITestOptions;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Properties;

public final class DriverFactory {

    private DriverFactory() {}

    public static AppiumDriver create(Platform platform) {
        Properties props = loadProperties(platform);
        String serverUrl = props.getProperty("appium.server.url", "http://127.0.0.1:4723");
        try {
            URL url = new URL(serverUrl);
            return switch (platform) {
                case ANDROID -> new AndroidDriver(url, buildAndroid(props));
                case IOS -> new IOSDriver(url, buildIOS(props));
            };
        } catch (Exception e) {
            throw new RuntimeException("Failed to create driver for " + platform, e);
        }
    }

    private static UiAutomator2Options buildAndroid(Properties p) {
        UiAutomator2Options opts = new UiAutomator2Options()
                .setPlatformName("Android")
                .setAutomationName("UiAutomator2")
                .setDeviceName(p.getProperty("device.name"))
                .setAppPackage(p.getProperty("app.package"))
                .setAppActivity(p.getProperty("app.activity"))
                .setNoReset(Boolean.parseBoolean(p.getProperty("no.reset", "false")))
                .setFullReset(Boolean.parseBoolean(p.getProperty("full.reset", "true")));
        // Generous ADB / app-start timeouts — slow emulators time out at the default 20 s.
        // Bumped above the original defaults because cold-boot + fullReset on every test
        // doubles install/instrument time vs. a warm emulator. On a software-rendered
        // (-gpu swiftshader_indirect) emulator the splash → main launch can exceed 60 s, so
        // appWaitDuration is 180 s — the 60 s default is the real cause of the "flaky login"
        // (SessionNotCreated: 'am start-activity -W' timed out after 60000ms).
        opts.setCapability("appium:adbExecTimeout", 180000);
        opts.setCapability("appium:appWaitDuration", 180000);
        opts.setCapability("appium:uiautomator2ServerLaunchTimeout", 180000);
        opts.setCapability("appium:uiautomator2ServerInstallTimeout", 120000);
        opts.setCapability("appium:androidInstallTimeout", 240000);

        String pv = p.getProperty("platform.version");
        if (pv != null && !pv.isBlank()) opts.setPlatformVersion(pv);

        String appPath = p.getProperty("app.path");
        if (appPath != null && !appPath.isBlank()) {
            opts.setApp(Paths.get(appPath).toAbsolutePath().toString());
        }
        return opts;
    }

    private static XCUITestOptions buildIOS(Properties p) {
        XCUITestOptions opts = new XCUITestOptions()
                .setPlatformName("iOS")
                .setAutomationName("XCUITest")
                .setDeviceName(p.getProperty("device.name"))
                .setNoReset(Boolean.parseBoolean(p.getProperty("no.reset", "false")))
                .setFullReset(Boolean.parseBoolean(p.getProperty("full.reset", "true")));

        String pv = p.getProperty("platform.version");
        if (pv != null && !pv.isBlank()) opts.setPlatformVersion(pv);

        String bundleId = p.getProperty("bundle.id");
        if (bundleId != null && !bundleId.isBlank()) opts.setBundleId(bundleId);

        String appPath = p.getProperty("app.path");
        if (appPath != null && !appPath.isBlank()) {
            opts.setApp(Paths.get(appPath).toAbsolutePath().toString());
        }

        // First-time WDA build can take a minute or two on a cold simulator.
        opts.setCapability("appium:wdaLaunchTimeout", 120000);
        opts.setCapability("appium:wdaConnectionTimeout", 120000);
        // Auto-accept system alerts. On iOS the location-permission alert is raised on app start
        // (unlike Android, where it shows only after tapping "Filials"), and it blocks every tap
        // underneath. Auto-accepting unblocks the UI; the iOS PermissionDialog page is a no-op.
        opts.setCapability("appium:autoAcceptAlerts", true);
        return opts;
    }

    private static Properties loadProperties(Platform platform) {
        String filename = platform.name().toLowerCase() + ".properties";
        Properties props = new Properties();
        try (InputStream is = DriverFactory.class.getClassLoader().getResourceAsStream(filename)) {
            if (is == null) throw new RuntimeException("Properties file not found on classpath: " + filename);
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + filename, e);
        }
        return props;
    }
}
