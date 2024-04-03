package src;

import java.util.Scanner;
import java.util.*;
import java.util.concurrent.*;
import java.io.*;

public class Main {
    private static final int TEAM_SIZE = 4;
    private static Semaphore superCitizenSemaphore;
    private static Semaphore regularCitizenSemaphore;
    private static Semaphore teamSemaphore;
    private static int teamsSent;
    private static Semaphore cur_team;
    private static ExecutorService executor;
    private static int regularCitizensRemaining;
    private static int superCitizensRemaining;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the number of Regular Citizens: ");
        int regularCitizens = scanner.nextInt();
        System.out.print("Enter the number of Super Citizens: ");
        int superCitizens = scanner.nextInt();

        superCitizenSemaphore = new Semaphore(2);//max 2, need logic to have at least 1 later in team up
        regularCitizenSemaphore = new Semaphore(3);//max 3 cause at least 1 super
        teamSemaphore = new Semaphore(1); //sending one team at a time?
        teamsSent = 0;
        cur_team = new Semaphore(4);
        executor = Executors.newCachedThreadPool();
        
        regularCitizensRemaining = regularCitizens;
        superCitizensRemaining = superCitizens;

        Main mainInstance = new Main();

        //functions for making threads
        for (int i = 0; i < regularCitizens; i++) {
            executor.submit(new Helldiver("Regular", i, mainInstance));
        }

        for (int i = 0; i < superCitizens; i++) {
            executor.submit(new Helldiver("Super", i, mainInstance));
        }

        scanner.close();
    }

    public synchronized void teamUp(String type, int id) {
        int n_rC=0;
        int n_sC=0;
        try {
            if(type.equals("Regular")){
                if(regularCitizenSemaphore.tryAcquire()){
                    regularCitizensRemaining--;
                    n_rC++;
                    cur_team.acquire();
                    System.out.println("Regular joined up");
                } else {
                    System.out.println("Regular failed to join");
                }
            } else {
                if(superCitizenSemaphore.tryAcquire()){
                    superCitizensRemaining--;
                    n_sC++;
                    cur_team.acquire();
                    System.out.println("Super joined up");
                } else {
                    System.out.println("Super failed to join");
                }
            }
            System.out.println("Current team: " + teamsSent);
            System.out.println("Current Citizens: " + (4 - cur_team.availablePermits()));


            if(cur_team.availablePermits()==0){
                for(;n_rC >= 0; n_rC--){
                    regularCitizenSemaphore.release();
                }
                for(;n_sC >= 0; n_sC--){
                    superCitizenSemaphore.release();
                }
                cur_team.release(4);
                System.out.println("Team " + teamsSent + " going out.");
                teamsSent++;
            }

            if(superCitizensRemaining == 0 && regularCitizensRemaining > 0 && (superCitizensRemaining+regularCitizensRemaining) < 4) {
                System.out.println("Remaining Citizens went back home: " + regularCitizensRemaining);
                executor.shutdown();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}