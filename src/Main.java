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
    private static boolean shutdownRequested = false;
    private static int n_rC=0;
    private static int n_sC=0;

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
        try {
            if (shutdownRequested) return;
            if(type.equals("Regular")){
                if(regularCitizenSemaphore.tryAcquire()){
                    regularCitizensRemaining--;
                    n_rC++;
                    cur_team.acquire();
                    System.out.println("Regular joined up");
                    System.out.println("n_rC: " + n_rC);
                } else {
                    System.out.println("Regular is waiting for Super");
                    Thread.sleep(1000); // Wait for Super citizens to join
                    if (superCitizenSemaphore.tryAcquire()) {
                        superCitizensRemaining--;
                        n_sC++;
                        cur_team.acquire();
                        System.out.println("Super joined up after Regular's request");
                    } else {
                        System.out.println("Super failed to join even after waiting");
                        return; // Can't form a team, return
                    }
                }
            } else {
                if(superCitizenSemaphore.tryAcquire()){
                    superCitizensRemaining--;
                    n_sC++;
                    cur_team.acquire();
                    System.out.println("Super joined up");
                    System.out.println("n_sC: " + n_sC);
                } else {
                    System.out.println("Super is waiting for Regular");
                    Thread.sleep(1000); // Wait for Regular citizens to join
                    if (regularCitizenSemaphore.tryAcquire()) {
                        regularCitizensRemaining--;
                        n_rC++;
                        cur_team.acquire();
                        System.out.println("Regular joined up after Super's request");
                    } else {
                        System.out.println("Regular failed to join even after waiting");
                        return; // Can't form a team, return
                    }
                }
            }
            System.out.println("Current team: " + teamsSent);
            System.out.println("Current Citizens: " + (4 - cur_team.availablePermits()));


            if(cur_team.availablePermits()==0){
                regularCitizenSemaphore.release(n_rC);
                superCitizenSemaphore.release(n_sC);
                cur_team.release(4);
                System.out.println("Regular Citizen Permits: " + regularCitizenSemaphore.availablePermits());
                System.out.println("Super Citizen Permits: " + superCitizenSemaphore.availablePermits());
                System.out.println("Current Team Reset:" + cur_team.availablePermits());
                System.out.println("Team " + teamsSent + " going out.");
                teamsSent++;
                n_rC=0;
                n_sC=0;
                System.out.println("n_rC: " + n_rC + " n_sC: " + n_sC);
            }
            //if regular citizens is in between 0-3 (assuming no more supers) or if no more supers and reg citizens is over 4 and cur_team is like not lacking in reg citizens
            if(regularCitizensRemaining == 0 && regularCitizensRemaining < 4 || superCitizensRemaining == 0 && regularCitizensRemaining > 4 && cur_team.availablePermits() > 2 || superCitizensRemaining > 1 && regularCitizensRemaining < 2) {
                System.out.println(cur_team.availablePermits());
                System.out.println("Remaining Regular Citizens went back home: " + regularCitizensRemaining);
                System.out.println("Remaining Super Citizen went back home: " + superCitizensRemaining);
                System.out.println("Total Teams sent: " + teamsSent);
                synchronized (Main.class) {
                    if (!shutdownRequested) {
                        executor.shutdown();
                        shutdownRequested = true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}