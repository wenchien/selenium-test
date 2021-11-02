package com.selenium.test;

import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.support.pagefactory.ByChained;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.proxy.CaptureType;

public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    private static Gson jsonFormatter = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        // System properties and options
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\TerenceChang\\Downloads\\chromedriver.exe");
        List<String> chromeOptionList = new ArrayList<String>();
        // chromeOptionList.add("--incognito");
        chromeOptionList.add("--start-maximized");
        chromeOptionList.add("--auto-open-devtools-for-tabs");
        chromeOptionList.add("--proxy-bypass-list=*nxp.com*");
        ChromeOptions options = new ChromeOptions();
        options.addArguments(chromeOptionList);

        WebDriver driver = new ChromeDriver(options);

        try {
            // runHeartBeat(driver, options);
            completeHealthDelcarationForMe(driver);

        } catch (NoSuchElementException e) {
            LOGGER.error("Couldn't find the desired element. See stacktrace");
            LOGGER.error(e.getMessage(), e);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            driver.close();
            driver.quit();
        }
    }

    public static void waitFor(int milliseconds) throws InterruptedException {
        Thread.sleep(milliseconds);
    }

    @SuppressWarnings("deprecation")
    public static void completeHealthDelcarationForMe(WebDriver driver) throws Exception {
        driver.navigate().to("http://wtu-portal-prod.wtmec.com/eip/index");
        Wait<WebDriver> wait = new FluentWait<WebDriver>(driver).withTimeout(30, TimeUnit.SECONDS)
                .pollingEvery(1, TimeUnit.SECONDS).ignoring(NoSuchElementException.class);

        // Wait for OA button
        wait.until(new Function<WebDriver, WebElement>() {
            public WebElement apply(WebDriver driver) {
                return driver.findElement(By.xpath("/html/body/div[1]/nav/div/div[2]/div/ul/li[3]"));
            }
        });

        waitFor(5000);

        JavascriptExecutor jsExecutor = (JavascriptExecutor) driver;
        jsExecutor.executeScript(
                "portalUi.changeUrl('http://wtu-app1-prod.wtmec.com:8080/oa/userStatus/index.do', 'false', 'false', 'Health Declaration', this)");

        waitFor(3000);

        // WebElement iframe = driver.findElement(By.cssSelector("#modal>iframe"));

        // Wait until oa iframe exists
        WebDriverWait dWait = new WebDriverWait(driver, 30);
        dWait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("iframeId")));
        waitFor(3000);

        By notVaccinated = By.id("hasVaccinationNo");
        List<WebElement> isVaccinatedNoOption = driver.findElements(notVaccinated);
        if (!isVaccinatedNoOption.isEmpty()) {
            // probably not needed
            wait.until(ExpectedConditions.visibilityOfElementLocated(notVaccinated));
            isVaccinatedNoOption.get(0).click();
            if (!isVaccinatedNoOption.get(0).isSelected()) {
                throw new IllegalStateException("Vaccination option is not selected");
            }
        }
        waitFor(3000);

        // driver.switchTo().frame(iframe);
        WebElement saveButton = driver.findElement(By.id("saveBtn"));
        saveButton.click();
        waitFor(3000);

        driver.switchTo().activeElement();
        WebElement confirmOkButton = driver
                .findElement(new ByChained(By.xpath("/html/body/div[3]/div/div/div[2]/button[2]")));
        confirmOkButton.click();

        Object isFinished = jsExecutor.executeScript("console.log('Finished!'); return true;");

        waitFor(1000);

        System.out.println("isFinished Class: " + (Boolean) isFinished);
    }

    @SuppressWarnings("deprecation")
    public static boolean runHeartBeat(WebDriver driver, ChromeOptions options) throws Exception {
        boolean taskStatus = false;

        // Start the proxy server
        BrowserMobProxy proxy = new BrowserMobProxyServer();
        proxy.start(0);

        Proxy seleniumProxy = ClientUtil.createSeleniumProxy(proxy);

        options.setCapability(CapabilityType.PROXY, seleniumProxy);

        options.setAcceptInsecureCerts(true);

        if (driver == null) {
            driver = new ChromeDriver(options);
        }
        proxy.enableHarCaptureTypes(CaptureType.REQUEST_CONTENT, CaptureType.RESPONSE_CONTENT);
        proxy.newHar("Model N");

        driver.get("http://www.mn.nxp.com/index.jsp");

        Har har = proxy.getHar();
        har.writeTo(new File("C:\\Users\\TerenceChang\\Downloads\\kmz80.txt"));

        String harString = Files.toString(new File("C:\\Users\\TerenceChang\\Downloads\\kmz80.txt"),
                StandardCharsets.UTF_8);
        System.out.println(jsonFormatter.toJson(harString));
        taskStatus = true;
        return taskStatus;
    }

    public static void runModelNLogin(WebDriver driver) throws Exception {
        driver.get("https://www.mn.nxp.com/index.jsp");
        WebDriverWait dWait = new WebDriverWait(driver, 30);
        dWait.until(ExpectedConditions.elementToBeClickable(By.name("USER")));

        waitFor(5000);

        WebElement userPassField = driver.findElement(By.name("USER"));
        userPassField.sendKeys("jamie.xu@wtmec.com");

        waitFor(1000);

        userPassField = driver.findElement(By.name("PASS"));
        userPassField.sendKeys("Wtmec521");

        WebElement loginBtn = driver.findElement(By.id("button"));
        loginBtn.click();

        // Wait until home button appears
        By homeLocator = By.xpath("/html/body/form/div[1]/div[1]/div[2]/ul/li[1]/a");
        dWait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(homeLocator));

        if (driver.getTitle().toLowerCase().contains("Session".toLowerCase())) {
            // Eliminate all concurrent sessions
            By terminateButtonLocator = By.xpath("/html/body/form/div[2]/div[3]/table/tbody/tr/td/div/ul/li[1]/a");
            WebElement terminateButton = driver.findElement(terminateButtonLocator);
            terminateButton.click();
        } else {
            driver.findElement(homeLocator).click();
        }
    }

    public static void runQuoteCreation(WebDriver driver) {

    }
}