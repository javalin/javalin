package io.javalin.testing

import io.github.bonigarcia.wdm.WebDriverManager
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions

object WebDriverUtil {

    init {
        WebDriverManager.chromedriver().setup()
    }

    fun getDriver(): ChromeDriver {
        if (TestEnvironment.isNotCiServer && Math.random() < 0.8) {
            assumeTrue(false, "Skipping random WebDriver tests because we're not on a CI server and the tests are sooo slow")
        }
        if (TestEnvironment.isCiServer && TestEnvironment.isWindows) { // TODO: remove this when flakiness is fixed
            assumeTrue(false, "Skipping WebDriver tests on Windows CI server because they are flaky")
        }
        return ChromeDriver(ChromeOptions().apply {
            addArguments("--no-sandbox")
            addArguments("--headless=new")
            addArguments("--disable-gpu")
            addArguments("--remote-allow-origins=*")
            addArguments("--disable-dev-shm-usage")
        })
    }

}
