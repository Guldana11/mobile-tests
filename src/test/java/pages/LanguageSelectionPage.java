package pages;

import core.Platform;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class LanguageSelectionPage extends BasePage {

    private static final String ANDROID_CARD_ID = "kz.bnk.app.dev:id/mcv_content";
    private static final String IOS_CHECKMARK_ON_NAME = "Common/checkmark-on";

    public enum Language {
        KAZAKH("Қазақша"),
        RUSSIAN("Русский"),
        ENGLISH("English"),
        KOREAN("한국어");

        public final String label;

        Language(String label) {
            this.label = label;
        }
    }

    public LanguageSelectionPage(AppiumDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        return waitForDisplayed(Duration.ofSeconds(60));
    }

    public boolean waitForDisplayed(Duration timeout) {
        try {
            return switch (Platform.current()) {
                case ANDROID -> {
                    new WebDriverWait(driver, timeout).until(
                            d -> !d.findElements(AppiumBy.id(ANDROID_CARD_ID)).isEmpty()
                    );
                    yield driver.findElements(AppiumBy.id(ANDROID_CARD_ID)).size() == Language.values().length;
                }
                case IOS -> {
                    // Wait until all 4 language buttons are present — accessibility id == label.
                    new WebDriverWait(driver, timeout).until(d -> {
                        for (Language l : Language.values()) {
                            if (d.findElements(AppiumBy.accessibilityId(l.label)).isEmpty()) return false;
                        }
                        return true;
                    });
                    yield true;
                }
            };
        } catch (Exception e) {
            return false;
        }
    }

    public void selectLanguage(Language language) {
        // Wait until the specific language card we want is in the tree — bottom-sheet
        // items appear with animation, so a generic "any card visible" check races us.
        WebElement card = new WebDriverWait(driver, Duration.ofSeconds(60))
                .ignoring(org.openqa.selenium.NoSuchElementException.class)
                .until(d -> languageCard(language));
        card.click();
        // Wait for the language screen to dismiss — guarantees we won't race the next screen.
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.invisibilityOf(card));
    }

    public boolean isLanguageSelected(Language language) {
        return switch (Platform.current()) {
            case ANDROID -> Boolean.parseBoolean(languageCard(language).getAttribute("checked"));
            // On iOS the card itself has no checked-state attribute — selection is shown by swapping
            // the child image from Common/checkmark-off to Common/checkmark-on.
            case IOS -> !driver.findElements(
                    AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeImage' AND name == '" + IOS_CHECKMARK_ON_NAME + "'"
                    )
            ).isEmpty()
                    && languageCard(language).findElements(
                            AppiumBy.iOSNsPredicateString("name == '" + IOS_CHECKMARK_ON_NAME + "'")
                    ).size() > 0;
        };
    }

    private WebElement languageCard(Language language) {
        return driver.findElement(languageCardLocator(language));
    }

    private By languageCardLocator(Language language) {
        return switch (Platform.current()) {
            // Android: find the clickable CardView (mcv_content) by its child TextView's text —
            // id alone isn't unique across the 4 rows.
            case ANDROID -> AppiumBy.androidUIAutomator(String.format(
                    "new UiSelector().resourceId(\"%s\").childSelector(new UiSelector().text(\"%s\"))",
                    ANDROID_CARD_ID, language.label));
            // iOS: each language button exposes its label as the accessibility id.
            case IOS -> AppiumBy.accessibilityId(language.label);
        };
    }
}
