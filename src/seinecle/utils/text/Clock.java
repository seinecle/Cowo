/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package seinecle.utils.text;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author C. Levallois
 */
public class Clock {

    private long start;
    private final String action;
    public Clock (String action){
    
    this.action = action;    
        
    startClock();
    }
    
    
    void startClock(){
        
        start = System.currentTimeMillis();
        System.out.println(action + "...");
    }

    
    
    void printElapsedTime(){

        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - start;
       
        if (elapsedTime
                < 1000) {
            System.out.println("still " + StringUtils.lowerCase(action) + ", " + elapsedTime + " milliseconds]");
        } else {
            System.out.println("still " + StringUtils.lowerCase(action) + ", " + elapsedTime / 1000 + " seconds]");
        }
        
    }
    
    void closeAndPrintClock(){
    
        long currentTime = System.currentTimeMillis();
        long totalTime = currentTime - start;
       
        if (totalTime
                < 1000) {
            System.out.println("finished " + StringUtils.lowerCase(action) + " [took: " + totalTime + " milliseconds]");
        } else {
            System.out.println("finished " + StringUtils.lowerCase(action) + " [took: " + totalTime / 1000 + " seconds]");
        
        
        }
       
        System.out.println(
                "---------------------------------");
        System.out.println();

    
    }
    
    
    
}
