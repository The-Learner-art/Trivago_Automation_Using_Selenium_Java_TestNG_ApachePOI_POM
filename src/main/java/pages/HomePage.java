package pages;

import org.openqa.selenium.*;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class HomePage {
    private final WebDriver driver;
    private final WebDriverWait wait;

    public HomePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        PageFactory.initElements(driver, this);
    }

    // --- Core Search Elements ---
    @FindBy(id = "input-auto-complete")
    private WebElement destinationInput;

    @FindBy(className = "Qrvi3L")
    private List<WebElement> suggestionList;

    // --- Calendar Elements ---
    @FindBy(xpath = "//button[@data-testid='search-form-calendar' or contains(@aria-label,'calendar')]")
    private WebElement calendarOpenButton;

    @FindBy(xpath = "//*[contains(@data-testid,'calendar-popover') or contains(@class,'calendar')]")
    private WebElement calendarRoot;

    @FindBy(xpath = "//button[contains(@aria-label,'Next') or @data-testid='calendar-button-next' or contains(@data-testid,'next')]")
    private WebElement nextMonthButton;

    private By dayButton(LocalDate date) {
        String iso = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        return By.cssSelector("button[data-testid='valid-calendar-day-" + iso + "']");
    }

    /** Type destination and click the first suggestion. */
    public void enterDestination(String city) {
        WebElement searchBox = wait.until(ExpectedConditions.elementToBeClickable(destinationInput));
        searchBox.clear();
        searchBox.sendKeys(city);
        wait.until(d -> !suggestionList.isEmpty() && suggestionList.get(0).isDisplayed());
        suggestionList.get(0).click();
    }

    /** check-in + check-out selection. */
    public void selectDateRange(LocalDate checkIn, LocalDate checkOut) {
        if (checkOut.isBefore(checkIn)) {
            throw new IllegalArgumentException("Check-out date must be after check-in date.");
        }
        openCalendarIfClosed();
        navigateUntilDayVisible(checkIn);
        clickDay(checkIn);
        navigateUntilDayVisible(checkOut);
        clickDay(checkOut);
    }

    /** Adjust guests with preferred absolute XPath + Apply. */
    public void adjustGuests() {
        // Decrease adults once (kept strategy; targeting the first button in Adults fieldset)
//        By adultsMinus = By.xpath(
//                "/html/body/div[1]/div[1]/div[2]/section[1]/div[2]/div/div/div/div/div/div[2]/div/section/div/div/div[1]/fieldset[1]/div/button[1]");
        By adultsMinus = By.xpath("//button[@data-testid='adults-amount-minus-button']");
        try {
            wait.until(ExpectedConditions.elementToBeClickable(adultsMinus)).click();
        } catch (TimeoutException | NoSuchElementException e) {}

        // Apply / Done
        By applyBtn = By.xpath("//button[text()='Apply']");
        wait.until(ExpectedConditions.elementToBeClickable(applyBtn)).click();
    }

    // --- Helpers ---
    private void openCalendarIfClosed() {
        if (!isCalendarOpen()) {
            wait.until(ExpectedConditions.elementToBeClickable(calendarOpenButton)).click();
            wait.until(ExpectedConditions.visibilityOf(calendarRoot));
        }
    }

    private boolean isCalendarOpen() {
        try {
            return calendarRoot.isDisplayed();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return false;
        }
    }

    private void navigateUntilDayVisible(LocalDate date) {
        final int MAX_MONTHS = 24;
        int hops = 0;
        if (isDayVisible(date)) return;
        while (!isDayVisible(date)) {
            if (hops++ >= MAX_MONTHS) {
                throw new TimeoutException("Target date not found within " + MAX_MONTHS + " months: " + date);
            }
            wait.until(ExpectedConditions.elementToBeClickable(nextMonthButton)).click();
            sleepQuiet(150);
        }
    }

    private boolean isDayVisible(LocalDate date) {
        try {
            return driver.findElements(dayButton(date)).stream().anyMatch(WebElement::isDisplayed);
        } catch (StaleElementReferenceException ignored) {
            return false;
        }
    }

    private void clickDay(LocalDate date) {
        By locator = dayButton(date);
        try {
            wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
        } catch (StaleElementReferenceException e) {
            sleepQuiet(150);
            wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
        }
    }

    private void sleepQuiet(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
