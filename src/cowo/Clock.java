/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cowo;

import GUI.Screen1;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author C. Levallois
 */
public class Clock {

    private long start;
    private String action;
    private String logText;
    private final String newLine = "\n";
    private final String interval = "-------------------------------\n\n";
            
    
    public Clock (String action){
    
    this.action = action;    
        
    startClock();
    }
    
    
    void startClock(){
        
        start = System.currentTimeMillis();
        logText = action + "..."+newLine;
        Screen1.logArea.setText(Screen1.logArea.getText().concat(logText));
        Screen1.logArea.setCaretPosition(Screen1.logArea.getText().length());

        //GUI_Screen_1.logArea.repaint();
        
        System.out.print(logText);
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
            logText = "finished [took: " + totalTime + " milliseconds]"+newLine+interval;
            System.out.print(logText);
        } else {
            logText = "finished [took: " + totalTime / 1000 + " seconds]"+newLine+interval;
            System.out.print(logText);
        
               
        }
        Screen1.logArea.setText(Screen1.logArea.getText().concat(logText));
        Screen1.logArea.setCaretPosition(Screen1.logArea.getText().length());


    
    }
    
    
    
}
