/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package simpletooling;

import java.time.Duration;
import java.util.Scanner;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 *
 * @author noahp
 */
public class DataManager {

    //final variables
    final int SEGMENTS_OF_DATA = 288; //24 hours * 12 5 minute intervals per hour
    final int DIST_FROM_LEFT = 40; //Distance between the very left side of the SVG and where the data actually starts

    //variables initialized by constructor
    public WebDriver driver;
    public WebElement svg;
    public Session session;
    public String receiver;

    //other variables
    public double size = 0;
    public double distBetweenData = 0;
    public double startPos = 0;
    public String data = "";
    public String time = "";
    public String beforeTime = "";
    public String beforeData = "";
    public boolean run = true;

    int temp = 0;

    public DataManager(WebDriver driver, Session session, String receiver) {
        this.driver = driver;
        this.svg = new WebDriverWait(driver, Duration.ofSeconds(3)).until(ExpectedConditions.presenceOfElementLocated(By.tagName("svg")));
        this.session = session;
        this.receiver = receiver;
    }

    public void getRecentData() {        
        this.size = svg.getSize().getWidth() - this.DIST_FROM_LEFT;
        this.startPos = ((svg.getSize().getWidth() / 2)) - (this.size);
        this.distBetweenData = this.size / (double) this.SEGMENTS_OF_DATA;

        //Determine the time of the most recent data point as well as how many 5 minute intervals exist between this time and 00:00
        WebElement time = this.driver.findElement(By.id("realTimeTotalLoad-1-title-details"));
        String[] linesOfTime = time.getText().split(" ");
        int amtOfElapsedIntervals = 0;

        for (String thisLine : linesOfTime) {
            if (thisLine.contains(":")) {
                //Remove the leading 0 from AM times unless its 00:xx
                if (thisLine.charAt(0) == '0' && thisLine.charAt(1) != '0'){
                    thisLine = thisLine.substring(1, thisLine.length());
                }
                this.time = thisLine;
                Scanner sc = new Scanner(this.time);
                sc.useDelimiter(":");
                int hours = sc.nextInt();
                int minutes = sc.nextInt();
                amtOfElapsedIntervals = (hours * 12) + (minutes / 5) - 5;
                //The -5 is to reduce the likelyhood of a NoSuchElementException by trying to place the estimated position slightly to
                //the left of the desired datapoint, meaning a tooltip that is to be held in the "tooltip" WebElement does in fact exist
            }
        }

        //Declare the approximate position and make slight adjustments to this position until the desired datapoint is found
        int approxPosition = (int) (this.startPos + (distBetweenData * amtOfElapsedIntervals));
        Actions moveCursorAction = new Actions(this.driver);
        boolean currentIntervalFound = false;

        while (currentIntervalFound == false) {
            moveCursorAction.moveToElement(this.svg, approxPosition, 20).perform();
            try {
                //Runs if the current position is to the left of the datapoint (a tooltip is shown, but it does not contain the right data)
                WebElement tooltip = driver.findElement(By.className("k-chart-tooltip"));
                if (tooltip.getText().contains(this.time)) {
                    //Data is found
                    this.getDataFromString(tooltip.getText());
                    valueManager();
                    currentIntervalFound = true;
                } else if (tooltip.getText().contains("Interval")) {
                    //Still too far to the left
                    approxPosition++;
                } else {
                    //Too far to the right, but an undesired tooltip is shown meaning the NoSuchElementException is not thrown
                    approxPosition--;
                }
            } catch (NoSuchElementException ex) {
                //Runs if the current position is to the right of the datapoint (no tooltip is shown)
                //Should rarely run due to the subtraction of 5 intervals from "amtOfElapsedIntervals"
                approxPosition--;
            }
        }
    }

    public void getDataFromString(String stringData) {
        String[] splitData = stringData.split("\n");
        for (int i = 0; i < splitData.length; i++) {
            if (splitData[i].contains("Actual Load (MW) : ")) {
                this.data = splitData[i];
            }
        }
    }

    public void valueManager() {
        //If the data has changed, send the email
        if (!this.data.equals(this.beforeData) && !this.beforeTime.equals("") && !this.beforeData.equals("")) {
            try {
                Message message = new MimeMessage(this.session);
                message.setFrom(new InternetAddress("testnotifier1234@gmail.com"));
                message.setRecipient(Message.RecipientType.TO, new InternetAddress(this.receiver));
                message.setSubject("Load Change Detected (at " + this.time + " EST)");
                message.setText("Previous data (at " + this.beforeTime + "): " + this.beforeData
                        + "\nNew data (at " + this.time + "): " + this.data);
                Transport.send(message);
                this.beforeTime = this.time;
                this.beforeData = this.data;
            } catch (MessagingException ex) {
                ex.printStackTrace();
            }
        } else if (beforeTime.equals("") && this.beforeData.equals("")) {
            this.beforeTime = this.time;
            this.beforeData = this.data;
        }
    }

    public void runner() throws InterruptedException {
        while (this.run == true) {
            this.getRecentData();
            this.driver.navigate().refresh();
            driver.switchTo().frame("56440ec5-34c3-4219-a280-eb7756441501");
            Thread.sleep(10000);
            this.svg = new WebDriverWait(this.driver, Duration.ofSeconds(3)).until(ExpectedConditions.presenceOfElementLocated(By.tagName("svg")));
        }
    }

//----------TESTING METHOD(S) NO LONGER USED----------
//    public void getDistFromLeft() {
//        Actions moveCursorAction = new Actions(this.driver);
//        int farLeftPos = ((svg.getSize().getWidth()) / 2) - svg.getSize().getWidth();
//        double counter = 0;
//        for (double i = farLeftPos; i < this.svg.getSize().getWidth() / 2; i = i + 0.25) {
//            try {
//                moveCursorAction.moveToElement(this.svg, (int) i, 20).perform();
//                WebElement tooltip = driver.findElement(By.className("k-chart-tooltip"));
//                //Left start of data found, break out of loop
//                break;
//            } catch (NoSuchElementException ex) {
//            }
//            counter += 0.25;
//        }
//    }
}
