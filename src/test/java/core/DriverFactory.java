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
        opts.setCapability("appium:adbExecTimeout", 60000);
        opts.setCapability("appium:appWaitDuration", 30000);
        opts.setCapability("appium:uiautomator2ServerLaunchTimeout", 60000);
        opts.setCapability("appium:uiautomator2ServerInstallTimeout", 60000);

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
                .setDeviceName(p.getProperty("device.name"));

        String pv = p.getProperty("platform.version");
        if (pv != null && !pv.isBlank()) opts.setPlatformVersion(pv);

        String appPath = p.getProperty("app.path");
        if (appPath != null && !appPath.isBlank()) {
            opts.setApp(Paths.get(appPath).toAbsolutePath().toString());
        }
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
