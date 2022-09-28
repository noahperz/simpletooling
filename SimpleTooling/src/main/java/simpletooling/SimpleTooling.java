/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package simpletooling;

import java.time.Duration;

import java.util.Properties;
import java.util.Scanner;

import javax.mail.PasswordAuthentication;
import javax.mail.Session;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.github.bonigarcia.wdm.WebDriverManager;

/**
 *
 * @author noahp
 */
public class SimpleTooling {

    public static void main(String[] args) throws InterruptedException {
        //Prepare automated email messaging
        final String username = "email";
        final String password = "app password";

        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "587");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(prop,
                new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        System.out.println("\n\n\n\nPlease enter the email adress that will be responsible for recieving email updates:");
        Scanner kbScanner = new Scanner(System.in);
        String receiver = kbScanner.nextLine();

        //Set up ChromeDriver and DataManager object
        WebDriverManager.chromedriver().setup();
        WebDriver driver = new ChromeDriver();
        driver.get("https://www.misoenergy.org/markets-and-operations/real-time--market-data/real-time-displays/");

        driver.switchTo().frame("56440ec5-34c3-4219-a280-eb7756441501");
        DataManager thisManager = new DataManager(driver, session, receiver);

        thisManager.runner();
    }
}
