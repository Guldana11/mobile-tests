package pages;

import core.Platform;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * The main (home) screen shown after a successful PIN setup. Its defining element is the bottom tab
 * bar with "Главная" / "Продукты" / "Быстрое меню", plus a "Привет, &lt;name&gt;" greeting and the
 * account list.
 *
 * <p>On iOS, two onboarding prompts (Face ID, then notifications) appear over the main screen right
 * after the PIN is confirmed — {@link #dismissOnboardingPrompts()} taps their "Позже" buttons. The
 * greeting name is account-specific, so tests assert the "Привет," prefix, not a literal name.
 */
public class MainScreenPage extends BasePage {

    // Bottom tab labels — locale text, identical on the home screen of both platforms.
    private static final String TAB_HOME = "Главная";
    private static final String TAB_PRODUCTS = "Продукты";
    private static final String TAB_QUICK_MENU = "Быстрое меню";
    private static final String GREETING_PREFIX = "Привет,";

    // The last action at the very bottom of the account list — off-screen until the list is scrolled,
    // so it doubles as the "did the list scroll" marker.
    private static final String BOTTOM_ACTION = "Открыть депозит";

    // A menu item unique to the Quick-menu tab (absent on Home/Products), identical on both platforms.
    private static final String QUICK_MENU_MARKER = "Перевод в другой банк";
    // Android marks the active bottom-nav item as selected; used to confirm the Products tab opened.
    private static final String ANDROID_NAV_PRODUCTS_ID = "kz.bnk.app.dev:id/navigation_open_product";

    // The header burger opens a side menu (drawer) whose unique item is "Служба поддержки".
    private static final String SIDE_MENU_MARKER = "Служба поддержки";
    private static final String IOS_BURGER = "Home/menu";
    private static final String IOS_SIDE_MENU_CLOSE = "Common/rounded_close";
    private static final String ANDROID_BURGER_ID = "kz.bnk.app.dev:id/mcv_menu";
    private static final String ANDROID_SIDE_MENU_CLOSE_ID = "kz.bnk.app.dev:id/btn_close";
    private static final String ANDROID_AVATAR_ID = "kz.bnk.app.dev:id/mcv_avatar";  // header avatar → profile
    private static final String IOS_AVATAR = "Profile/user";                         // iOS avatar accessibilityId (recon-confirmed)

    private static final String LATER_BUTTON = "Позже";
    // Title of the app-lock screen the app shows when it auto-locks during a long session.
    private static final String PIN_LOCK_TITLE = "Введите код";
    private static final String PIN_WRONG_CODE = "Код неверный";   // unlock-screen error after a wrong PIN

    private static final String ANDROID_GREETING_ID = "kz.bnk.app.dev:id/tv_greetings";
    private static final String ANDROID_AMOUNT_ID = "kz.bnk.app.dev:id/tv_amount";
    // Account name/number ("Текущий счет *xxxx") on the home account cards (the transfer sheet uses
    // a different id, tv_type, for the same text).
    private static final String ANDROID_ACCOUNT_NAME_ID = "kz.bnk.app.dev:id/tv_alias";

    // Shared toolbar back arrow on in-app sub-screens (account detail, side-menu destinations).
    private static final String ANDROID_TOOLBAR_BACK_ID = "kz.bnk.app.dev:id/iv_back";
    private static final String IOS_TOOLBAR_BACK = "BackButton";  // verified on the "Курсы обмена валют" screen.

    // The SwipeRefreshLayout wrapping the account list — its presence proves pull-to-refresh is wired.
    private static final String ANDROID_SWIPE_REFRESH_ID = "kz.bnk.app.dev:id/swipe_refresh";

    public MainScreenPage(AppiumDriver driver) {
        super(driver);
    }

    public boolean isDisplayed() {
        return waitForDisplayed(Duration.ofSeconds(30));
    }

    public boolean waitForDisplayed(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(
                    ExpectedConditions.visibilityOfElementLocated(homeTabLocator()));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** True if the bottom navigation exposes all three expected tabs. */
    public boolean hasExpectedTabs() {
        return !driver.findElements(tabLocator(TAB_HOME)).isEmpty()
                && !driver.findElements(tabLocator(TAB_PRODUCTS)).isEmpty()
                && !driver.findElements(tabLocator(TAB_QUICK_MENU)).isEmpty();
    }

    /** True if the "Привет, &lt;name&gt;" greeting is present. */
    public boolean hasGreeting() {
        return !driver.findElements(greetingLocator()).isEmpty();
    }

    /** True if the account list rendered at least one account balance (a currency amount). */
    public boolean hasAccountBalances() {
        return !driver.findElements(balanceLocator()).isEmpty();
    }

    /** Opens the "Продукты" tab. */
    public void openProductsTab() {
        driver.findElement(tabLocator(TAB_PRODUCTS)).click();
    }

    /** Opens the "Быстрое меню" tab. */
    public void openQuickMenuTab() {
        driver.findElement(tabLocator(TAB_QUICK_MENU)).click();
    }

    /** Returns to the "Главная" tab. */
    public void openHomeTab() {
        driver.findElement(tabLocator(TAB_HOME)).click();
    }

    /**
     * Opens the within-bank, by-phone transfer from the Быстрое меню sheet, dismissing the
     * fraud-warning sheet, and returns the {@link PhoneTransferPage} on the form. The menu item label
     * differs by platform: Android "Перевод внутри банка", iOS "Переводы внутри банка" (plural).
     */
    public PhoneTransferPage openInBankTransfer() {
        String item = switch (Platform.current()) {
            case ANDROID -> "Перевод внутри банка";
            case IOS -> "Переводы внутри банка";
        };
        openQuickMenuTab();
        new org.openqa.selenium.support.ui.WebDriverWait(driver, Duration.ofSeconds(10))
                .until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(
                        textContainsLocator(item)))
                .click();
        PhoneTransferPage page = new PhoneTransferPage(driver);
        page.dismissWarning();
        return page;
    }

    /**
     * Opens the "Перевод в другой банк" (inter-bank transfer) from the Быстрое меню, dismissing the
     * fraud-warning sheet, and returns the {@link OtherBankTransferPage} on the form.
     */
    public OtherBankTransferPage openOtherBankTransfer() {
        openQuickMenuTab();
        new org.openqa.selenium.support.ui.WebDriverWait(driver, Duration.ofSeconds(10))
                .until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(
                        textContainsLocator(QUICK_MENU_MARKER)))
                .click();
        OtherBankTransferPage page = new OtherBankTransferPage(driver);
        page.dismissWarning();
        return page;
    }

    // Locates by a CONTAINS match on visible text/label (cross-platform).
    private By textContainsLocator(String text) {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.iOSNsPredicateString("label CONTAINS '" + text + "' OR name CONTAINS '" + text + "'");
            case ANDROID -> AppiumBy.androidUIAutomator("new UiSelector().textContains(\"" + text + "\")");
        };
    }

    /**
     * Scrolls the home account list to the bottom action "Открыть депозит" and taps it, opening the
     * "Депозиты" product list. Returns a {@link DepositPage} positioned on that list.
     */
    public DepositPage openDepositList() {
        scrollAccountsDown();
        new org.openqa.selenium.support.ui.WebDriverWait(driver, Duration.ofSeconds(10))
                .until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(
                        bottomActionLocator()))
                .click();
        return new DepositPage(driver);
    }

    /**
     * Scrolls the home account list to the bottom action "Открыть счет" (next to "Открыть депозит") and
     * taps it, opening the "Открыть текущий счет" form. Returns an {@link AccountOpenPage}.
     */
    public AccountOpenPage openAccountForm() {
        scrollAccountsDown();
        new org.openqa.selenium.support.ui.WebDriverWait(driver, Duration.ofSeconds(10))
                .until(org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable(
                        openAccountLocator()))
                .click();
        return new AccountOpenPage(driver);
    }

    private By openAccountLocator() {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.accessibilityId("Открыть счет");
            case ANDROID -> AppiumBy.androidUIAutomator("new UiSelector().text(\"Открыть счет\")");
        };
    }

    /** Opens the side menu (drawer) via the header burger button. */
    public void openSideMenu() {
        driver.findElement(burgerLocator()).click();
    }

    /** Opens the profile screen by tapping the header avatar (top-left). */
    public ProfilePage openProfile() {
        driver.findElement(avatarLocator()).click();
        return new ProfilePage(driver);
    }

    /**
     * The "Текущий счет *xxxx" current accounts on the home screen, sorted by balance DESC (the most
     * funded first). Drag-and-drop must drag FROM a funded account — the app rejects a drag whose
     * source account has no money ("Перевод невозможен — недостаточно средств"). Pairs each account's
     * alias (name) with its balance by row y-proximity.
     */
    public java.util.List<String> homeCurrentAccountsByBalanceDesc() {
        java.util.List<WebElement> aliases = driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().resourceId(\"" + ANDROID_ACCOUNT_NAME_ID + "\").textContains(\"Текущий счет\")"));
        java.util.List<WebElement> amounts = driver.findElements(AppiumBy.id(ANDROID_AMOUNT_ID));
        java.util.List<String> names = new java.util.ArrayList<>();
        java.util.Map<String, Long> balances = new java.util.HashMap<>();
        for (WebElement alias : aliases) {
            String name = alias.getText();
            int y = alias.getRect().getY();
            long bal = 0;
            int best = Integer.MAX_VALUE;
            for (WebElement amount : amounts) {
                int d = Math.abs(amount.getRect().getY() - y);
                if (d < best) { best = d; bal = parseBalance(amount.getText()); }
            }
            if (!balances.containsKey(name)) { names.add(name); balances.put(name, bal); }
        }
        names.sort((a, b) -> Long.compare(balances.get(b), balances.get(a)));
        return names;
    }

    // Integer part of a balance string like "49 570 400,00 ₸" → 49570400 (0 if none).
    private long parseBalance(String text) {
        String intPart = text.split(",")[0].replaceAll("[^0-9]", "");
        return intPart.isEmpty() ? 0 : Long.parseLong(intPart);
    }

    /**
     * Drag-and-drop transfer between two own accounts on the home screen: drags the card named
     * {@code fromAccount} onto the card named {@code ontoAccount}, which opens the "Сумма перевода"
     * sheet pre-filled from the gesture. Per the app's behaviour the dragged-FROM account becomes the
     * destination (credit / куда) and the dragged-ONTO account becomes the source (debit / откуда).
     * Scrolls to the top first so both cards are in view.
     */
    public TransferPage dragTransfer(String fromAccount, String ontoAccount) {
        scrollToTop();
        WebElement from = driver.findElement(AppiumBy.androidUIAutomator(
                "new UiSelector().textContains(\"" + fromAccount + "\")"));
        WebElement onto = driver.findElement(AppiumBy.androidUIAutomator(
                "new UiSelector().textContains(\"" + ontoAccount + "\")"));
        Rectangle a = from.getRect(), b = onto.getRect();
        dragAndDrop(a.getX() + a.getWidth() / 2, a.getY() + a.getHeight() / 2,
                b.getX() + b.getWidth() / 2, b.getY() + b.getHeight() / 2);
        return new TransferPage(driver);
    }

    // Long-press to pick up the card, then a slow drag to the target and a settle before release.
    private void dragAndDrop(int x1, int y1, int x2, int y2) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence drag = new Sequence(finger, 1)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x1, y1))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger.createPointerMove(Duration.ofMillis(1000), PointerInput.Origin.viewport(), x1, y1))
                .addAction(finger.createPointerMove(Duration.ofMillis(1600), PointerInput.Origin.viewport(), x2, y2))
                .addAction(finger.createPointerMove(Duration.ofMillis(400), PointerInput.Origin.viewport(), x2, y2))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(drag));
    }

    /**
     * Closes the "Быстрое меню" bottom sheet and returns to the previous screen. The sheet has no
     * close button — Android dismisses it with the system Back gesture (verified: returns to Главная).
     */
    public void closeQuickMenu() {
        driver.navigate().back();
    }

    /**
     * Closes the "Быстрое меню" bottom sheet if it is open, so a reused session returns to a clean
     * home state. The sheet replaces the bottom navigation, so it must be closed BEFORE any
     * {@code openHomeTab()} call. Implicit wait is dropped so the common "nothing open" path is fast.
     */
    public void closeQuickMenuIfOpen() {
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            if (!driver.findElements(textLocator(QUICK_MENU_MARKER)).isEmpty()) {
                driver.navigate().back();
                Thread.sleep(800);  // let the sheet animate away before the next step
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        }
    }

    /**
     * Opens the first account's detail screen. On Android the balance amount is the clickable target.
     * On iOS the account list has no tappable cell in the a11y tree (see {@code AccountDetailTest}),
     * so we tap the account row by coordinates, retrying a few y-offsets until the detail's "Перевести"
     * action appears — UNVERIFIED end-to-end (written from inspector dumps; confirm on a live sim).
     */
    public AccountDetailPage openFirstAccount() {
        switch (Platform.current()) {
            case ANDROID -> driver.findElement(balanceLocator()).click();
            case IOS -> openFirstAccountByCoordinateIos();
        }
        return new AccountDetailPage(driver);
    }

    // iOS: coordinate-tap the first "Текущий счет" row, retrying y-offsets until the detail opens
    // (signalled by the "Реквизиты" action, a stable detail marker). The leaf StaticText doesn't
    // propagate a tap, so we hit the card body by coordinates. Flaky by nature — see
    // project_ios_a11y_tree.
    private void openFirstAccountByCoordinateIos() {
        By accName = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label BEGINSWITH 'Текущий счет'");
        By detailMarker = AppiumBy.iOSNsPredicateString("label == 'Реквизиты' OR name == 'Реквизиты'");
        for (int offset : new int[]{0, -28, 28, -56, 14}) {
            java.util.List<WebElement> rows = driver.findElements(accName);
            if (rows.isEmpty()) return;
            Rectangle r = rows.get(0).getRect();
            tapXY(r.getX() + r.getWidth() / 2, r.getY() + offset);
            sleepQuietly(2000);
            if (!driver.findElements(detailMarker).isEmpty()) return;
        }
    }

    /**
     * Opens a DEPOSIT card's detail screen (a card whose name contains "Депозит"). The detail screen is
     * the SAME layout as an account's (balance/"Доступный", "Перевести"/"Реквизиты" actions, История/
     * Настройки tabs, back arrow), so it returns an {@link AccountDetailPage}. On Android the deposit
     * name is tappable; on iOS the a11y tree has no tappable cell, so we coordinate-tap the row.
     */
    public AccountDetailPage openDepositDetail() {
        switch (Platform.current()) {
            case ANDROID -> {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView("
                                + "new UiSelector().textContains(\"Депозит\"))"));
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiSelector().textContains(\"Депозит\")")).click();
            }
            case IOS -> openDepositDetailByCoordinateIos();
        }
        return new AccountDetailPage(driver);
    }

    // iOS: deposits sit BELOW the current accounts (off-screen at first), so swipe up until a "Депозит…"
    // row is in the tappable area, then coordinate-tap it retrying y-offsets until the detail opens
    // (signalled by the "Реквизиты" action). Mirrors openFirstAccountByCoordinateIos — see
    // project_ios_a11y_tree.
    private void openDepositDetailByCoordinateIos() {
        By depName = AppiumBy.iOSNsPredicateString(
                "type == 'XCUIElementTypeStaticText' AND label CONTAINS 'Депозит'");
        By detailMarker = AppiumBy.iOSNsPredicateString("label == 'Реквизиты' OR name == 'Реквизиты'");
        Rectangle target = null;
        for (int i = 0; i < 7 && target == null; i++) {   // scroll a deposit row into the tappable band
            for (WebElement e : driver.findElements(depName)) {
                Rectangle r = e.getRect();
                if (r.getY() > 150 && r.getY() < 680) { target = r; break; }
            }
            if (target == null) swipeUpIos();
        }
        if (target == null) {
            java.util.List<WebElement> all = driver.findElements(depName);
            if (all.isEmpty()) return;
            target = all.get(0).getRect();
        }
        for (int offset : new int[]{0, -28, 28, -56, 14}) {
            tapXY(target.getX() + target.getWidth() / 2, target.getY() + offset);
            sleepQuietly(2000);
            if (!driver.findElements(detailMarker).isEmpty()) return;
        }
    }

    private void swipeUpIos() {
        PointerInput f = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        driver.perform(Collections.singletonList(new Sequence(f, 1)
                .addAction(f.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), 200, 650))
                .addAction(f.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(f.createPointerMove(Duration.ofMillis(400), PointerInput.Origin.viewport(), 200, 300))
                .addAction(f.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))));
        sleepQuietly(800);
    }

    private void tapXY(int x, int y) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        driver.perform(Collections.singletonList(new Sequence(finger, 1)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger.createPointerMove(Duration.ofMillis(120), PointerInput.Origin.viewport(), x, y))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()))));
    }

    private void sleepQuietly(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /** True if the open side menu lists the given item by its visible label. */
    public boolean sideMenuHasItem(String label) {
        return !driver.findElements(textLocator(label)).isEmpty();
    }

    /** Taps a side-menu item by its visible label (the menu must already be open). */
    public void openSideMenuItem(String label) {
        driver.findElement(textLocator(label)).click();
    }

    /**
     * Taps the toolbar back arrow shared by the in-app sub-screens (account detail, side-menu
     * destinations like "Курсы обмена валют"). Returns to the previous screen — the main screen when
     * the sub-screen was reached from there.
     */
    public void tapToolbarBack() {
        driver.findElement(toolbarBackLocator()).click();
    }

    /**
     * If the app auto-locked to its PIN screen ("Введите код") during a long shared session, re-enters
     * {@code pin} to unlock. No-op when not locked. The implicit wait is dropped so the common
     * "not locked" path returns immediately.
     */
    public void unlockIfLocked(String pin) {
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            if (!driver.findElements(pinLockTitleLocator()).isEmpty()) {
                for (char c : pin.toCharArray()) {
                    driver.findElement(pinDigitLocator(c)).click();
                }
                Thread.sleep(2000);  // let the unlock animate back to the previous screen
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        }
    }

    /** True if the app's PIN-unlock screen ("Введите код") is currently shown. */
    public boolean isPinLockShown() {
        return !driver.findElements(pinLockTitleLocator()).isEmpty();
    }

    /**
     * True if the PIN-unlock screen is showing in ANY of its states — the initial "Введите код" title
     * OR the post-wrong-entry "Код неверный." state (which replaces the title but keeps the keypad).
     * Used by SEC-2 to keep brute-forcing across attempts: {@link #isPinLockShown} alone returns false
     * after the first wrong PIN (the title is gone) even though we are still locked on the keypad.
     */
    public boolean isUnlockScreenShown() {
        return isPinLockShown() || isPinErrorShown();
    }

    /** True if the unlock screen shows the "Код неверный." rejection (the app refused a wrong PIN). */
    public boolean isPinErrorShown() {
        return !driver.findElements(textLocator(PIN_WRONG_CODE)).isEmpty();
    }

    /**
     * Waits up to {@code timeout} for the "Код неверный." rejection to appear. The unlock PIN is
     * validated with a short delay (a spinner shows briefly), so the rejection is NOT instant after the
     * 4th digit — SEC-2 must wait for it rather than poll once.
     */
    public boolean waitForPinError(Duration timeout) {
        return waitVisible(textLocator(PIN_WRONG_CODE), timeout);
    }

    /**
     * Types {@code pin} on the PIN-unlock screen — unlike {@link #unlockIfLocked} this makes no
     * assumption that the code is correct, so SEC-2 can feed a deliberately wrong PIN. The keypad
     * auto-submits on the 4th digit (no confirm button) and resets for the next attempt.
     */
    public void enterUnlockPin(String pin) {
        for (char c : pin.toCharArray()) {
            driver.findElement(pinDigitLocator(c)).click();
        }
    }

    /** True once the side menu is shown (its unique "Служба поддержки" item appears). */
    public boolean isSideMenuShown() {
        return waitVisible(textLocator(SIDE_MENU_MARKER), Duration.ofSeconds(10));
    }

    /**
     * Closes the side-menu drawer if it is open, so a reused session returns to a clean home state.
     * Both platforms have a dedicated close button (Android: btn_close; iOS: Common/rounded_close) —
     * the Android system Back is NOT used here because on this screen it raises an "exit app?" dialog.
     * Implicit wait is dropped so the common "nothing open" path returns immediately.
     */
    public void dismissSideMenuIfOpen() {
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        try {
            var close = driver.findElements(sideMenuCloseLocator());
            if (!close.isEmpty()) close.get(0).click();
        } catch (Exception ignored) {
        } finally {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        }
    }

    /** True once the Quick-menu screen is shown (its unique "Перевод в другой банк" item appears). */
    public boolean isQuickMenuShown() {
        return waitVisible(textLocator(QUICK_MENU_MARKER), Duration.ofSeconds(10));
    }

    /**
     * True once the Products screen is shown after tapping the Продукты tab. Android exposes a clean
     * "selected" flag on the nav item; iOS has no such flag, so we assert we left Home (greeting gone)
     * and are not on the Quick menu — which, given we tapped Продукты, identifies the Products screen.
     */
    public boolean isProductsShown() {
        return switch (Platform.current()) {
            case ANDROID -> waitTrue(d -> {
                var els = d.findElements(AppiumBy.id(ANDROID_NAV_PRODUCTS_ID));
                return !els.isEmpty() && "true".equals(els.get(0).getAttribute("selected"));
            });
            case IOS -> waitTrue(d -> d.findElements(greetingLocator()).isEmpty()
                    && d.findElements(textLocator(QUICK_MENU_MARKER)).isEmpty());
        };
    }

    /**
     * True if the bottom-most account action ("Открыть депозит") is currently on-screen. It starts
     * below the fold, so this is false until the account list is scrolled down (see
     * {@link #scrollAccountsDown()}).
     */
    public boolean isBottomActionVisible() {
        List<WebElement> els = driver.findElements(bottomActionLocator());
        if (els.isEmpty()) return false;
        // Check the element's coordinates rather than isDisplayed()/visible: both platforms keep
        // off-screen list items in the tree (iOS visible=false, Android bound RecyclerView rows that
        // still report displayed=true), so only the bounds tell us if it actually sits in the viewport.
        Rectangle r = els.get(0).getRect();
        int screenHeight = driver.manage().window().getSize().getHeight();
        return r.getY() >= 0 && r.getY() < screenHeight;
    }

    /** True if the home screen exposes the pull-to-refresh container (SwipeRefreshLayout). */
    public boolean hasPullToRefresh() {
        return !driver.findElements(swipeRefreshLocator()).isEmpty();
    }

    /**
     * Performs a pull-to-refresh: scrolls to the top of the account list (so the gesture lands on the
     * SwipeRefreshLayout rather than scrolling content) and drags down. The refresh spinner is a
     * drawable, not an accessibility node, so callers verify the OUTCOME (the home screen reloads
     * intact) rather than the spinner itself.
     */
    public void pullToRefresh() {
        scrollToTop();
        verticalSwipe(0.20, 0.85);
    }

    /**
     * Scrolls the account list down until the bottom action ("Открыть депозит") is in view, up to a
     * few swipes. Looping until the marker appears (rather than a fixed two swipes) makes this robust
     * to a slow emulator under-scrolling or a tall list — the cause of the earlier accountListScrolls
     * flake.
     */
    public void scrollAccountsDown() {
        for (int i = 0; i < 6 && !isBottomActionVisible(); i++) {
            verticalSwipe(0.80, 0.30);
        }
    }

    /**
     * Scrolls the account list back to the top. Used to normalise scroll position between cases when
     * the session is shared across a class (see {@code MainScreenTest}).
     */
    public void scrollToTop() {
        verticalSwipe(0.30, 0.80);
        verticalSwipe(0.30, 0.80);
    }

    // Swipes vertically from startFrac to endFrac of the screen height (fractions, 0=top..1=bottom).
    // A start below the end scrolls the content up (toward the list bottom); the reverse scrolls up.
    private void verticalSwipe(double startFrac, double endFrac) {
        Dimension size = driver.manage().window().getSize();
        int x = size.getWidth() / 2;
        int startY = (int) (size.getHeight() * startFrac);
        int endY = (int) (size.getHeight() * endFrac);
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence swipe = new Sequence(finger, 1)
                .addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, startY))
                .addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()))
                .addAction(finger.createPointerMove(Duration.ofMillis(600), PointerInput.Origin.viewport(), x, endY))
                .addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(swipe));
    }

    /**
     * Dismisses the post-PIN onboarding prompts (Face ID, then notifications) by tapping their
     * "Позже" (Later) button. Each prompt is a separate full-screen overlay that appears a few
     * seconds after the previous step, so every round WAITS up to 10s for the next "Позже" to show;
     * the loop ends as soon as a round times out with no prompt (the final wait is the stop signal).
     */
    public void dismissOnboardingPrompts() {
        By later = laterButtonLocator();
        for (int round = 0; round < 3; round++) {
            try {
                new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(ExpectedConditions.elementToBeClickable(later))
                        .click();
            } catch (Exception e) {
                return;  // no more prompts appeared within the timeout
            }
        }
    }

    private By homeTabLocator() {
        return tabLocator(TAB_HOME);
    }

    private By tabLocator(String label) {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.accessibilityId(label);
            // Android bottom-nav items expose the tab label as their content-description.
            case ANDROID -> AppiumBy.androidUIAutomator(
                    "new UiSelector().description(\"" + label + "\")");
        };
    }

    private By greetingLocator() {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.iOSNsPredicateString("label BEGINSWITH '" + GREETING_PREFIX + "'");
            case ANDROID -> AppiumBy.id(ANDROID_GREETING_ID);
        };
    }

    // Account balances are rendered with a currency suffix (₸ / $). Matching the symbol is locale- and
    // account-agnostic, so it survives a different test account or balance values.
    private By balanceLocator() {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.iOSNsPredicateString("label CONTAINS '₸' OR label CONTAINS '$'");
            case ANDROID -> AppiumBy.id(ANDROID_AMOUNT_ID);
        };
    }

    private By bottomActionLocator() {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.accessibilityId(BOTTOM_ACTION);
            case ANDROID -> AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + BOTTOM_ACTION + "\")");
        };
    }

    private By swipeRefreshLocator() {
        return switch (Platform.current()) {
            // iOS uses a UIRefreshControl, not this id — UNVERIFIED, this page runs on Android only.
            case IOS -> AppiumBy.iOSNsPredicateString("type == 'XCUIElementTypeOther'");
            case ANDROID -> AppiumBy.id(ANDROID_SWIPE_REFRESH_ID);
        };
    }

    private By toolbarBackLocator() {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.accessibilityId(IOS_TOOLBAR_BACK);
            case ANDROID -> AppiumBy.id(ANDROID_TOOLBAR_BACK_ID);
        };
    }

    private By burgerLocator() {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.accessibilityId(IOS_BURGER);
            case ANDROID -> AppiumBy.id(ANDROID_BURGER_ID);
        };
    }

    private By avatarLocator() {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.accessibilityId(IOS_AVATAR);
            case ANDROID -> AppiumBy.id(ANDROID_AVATAR_ID);
        };
    }

    private By sideMenuCloseLocator() {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.accessibilityId(IOS_SIDE_MENU_CLOSE);
            case ANDROID -> AppiumBy.id(ANDROID_SIDE_MENU_CLOSE_ID);
        };
    }

    private By pinLockTitleLocator() {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.accessibilityId(PIN_LOCK_TITLE);
            case ANDROID -> AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + PIN_LOCK_TITLE + "\")");
        };
    }

    private By pinDigitLocator(char digit) {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.accessibilityId(String.valueOf(digit));
            case ANDROID -> AppiumBy.id("kz.bnk.app.dev:id/btn" + digit);
        };
    }

    private By laterButtonLocator() {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.accessibilityId(LATER_BUTTON);
            case ANDROID -> AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + LATER_BUTTON + "\")");
        };
    }

    // Locates an element by its visible text/label (iOS labels ≈ Android text).
    private By textLocator(String text) {
        return switch (Platform.current()) {
            case IOS -> AppiumBy.iOSNsPredicateString(
                    "label == '" + text + "' OR name == '" + text + "'");
            case ANDROID -> AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"" + text + "\")");
        };
    }

    private boolean waitVisible(By locator, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(
                    ExpectedConditions.visibilityOfElementLocated(locator));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean waitTrue(java.util.function.Function<org.openqa.selenium.WebDriver, Boolean> condition) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10)).until(condition::apply);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
