package pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class LanguageSelectionPage extends BasePage {

    private static final String LANGUAGE_CARD_ID = "kz.bnk.app.dev:id/mcv_content";
    private static final String RECYCLER_VIEW_ID = "kz.bnk.app.dev:id/recycler_view";

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
            new WebDriverWait(driver, timeout).until(
                    d -> !d.findElements(AppiumBy.id(LANGUAGE_CARD_ID)).isEmpty()
            );
            return driver.findElements(AppiumBy.id(LANGUAGE_CARD_ID)).size() == Language.values().length;
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
        // Wait for the bottom sheet to dismiss — guarantees we won't race the next screen.
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.invisibilityOf(card));
    }

    public boolean isLanguageSelected(Language language) {
        return Boolean.parseBoolean(languageCard(language).getAttribute("checked"));
    }

    private WebElement languageCard(Language language) {
        // Find clickable CardView (mcv_content) by its child TextView's text — id alone isn't unique.
        String selector = String.format(
                "new UiSelector().resourceId(\"%s\").childSelector(new UiSelector().text(\"%s\"))",
                LANGUAGE_CARD_ID, language.label
        );
        return driver.findElement(AppiumBy.androidUIAutomator(selector));
    }
}
