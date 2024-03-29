package src;

import java.util.Scanner;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.io.*;

public class Main {
    private static final int TEAM_SIZE = 4;
    private static final int MAX_SUPER_CITIZENS = 2;
    private static Semaphore superCitizenSemaphore;
    private static Semaphore regularCitizenSemaphore;
    private static Semaphore teamSemaphore;
    private static int regularCitizensRemaining;
    private static int superCitizensRemaining;
    private static int teamsSent;
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the number of Regular Citizens: ");
        int regularCitizens = scanner.nextInt();
        System.out.print("Enter the number of Super Citizens: ");
        int superCitizens = scanner.nextInt();

        superCitizenSemaphore = new Semaphore(Math.min(2, superCitizens));
        regularCitizenSemaphore = new Semaphore(0);
        teamSemaphore = new Semaphore(0);
        regularCitizensRemaining = regularCitizens;
        superCitizensRemaining = superCitizens;
        teamsSent = 0;
        
        //functions for making threads
        for (int i = 0; i < regularCitizens; i++) {
            //thread
        }

        for (int i = 0; i < superCitizens; i++) {
           //thread
        }

        scanner.close();
    }

    public static void teamUp(String type, int id) {
    }
}