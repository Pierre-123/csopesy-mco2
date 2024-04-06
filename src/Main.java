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
    private static int teamsSent = 0;
    private static int checkTeam = 0;
    private static Semaphore cur_team;
    private static ExecutorService executor;
    private static int regularCitizensRemaining;
    private static int superCitizensRemaining;
    private static boolean shutdownRequested = false;
    private static int n_rC = 0;
    private static int n_sC = 0;
    private static int rc_ID = 0;
    private static int sc_ID = 0;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the number of Regular Citizens: ");
        int regularCitizens = scanner.nextInt();
        System.out.print("Enter the number of Super Citizens: ");
        int superCitizens = scanner.nextInt();

        superCitizenSemaphore = new Semaphore(2);//max 2, need logic to have at least 1 later in team up
        regularCitizenSemaphore = new Semaphore(3);//max 3 cause at least 1 super
        teamSemaphore = new Semaphore(1); //sending one team at a time?
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
            if (shutdownRequested) return; // NEED THIS

            //Exit Code
            //Incomplete team
            if(cur_team.availablePermits()>=0){//while theres still space on the current team.
                if( regularCitizensRemaining==0 && superCitizensRemaining==0 || //No more citizens
                    regularCitizensRemaining==0 && superCitizensRemaining>2 && superCitizenSemaphore.availablePermits() >= 0 && regularCitizenSemaphore.availablePermits() >= 1 || //if no more regs but lots of supers. when super is filled in >=0 1-2 Supers and still more to have but reg can't >= 1
                    superCitizensRemaining==0 && regularCitizensRemaining>3 && regularCitizenSemaphore.availablePermits() >= 0 && superCitizenSemaphore.availablePermits() > 0  //if no more supers but lots of regs. when regular is filled in >=0 2-3 Regulars but super can't > 0
                    ){
                    //recovery phase, release those in incomplete teams 
                    while(regularCitizenSemaphore.availablePermits() != 3){ //all released if 3
                        regularCitizensRemaining++;
                        regularCitizenSemaphore.release();
                    }
                    while(superCitizenSemaphore.availablePermits() != 2){ //all released if 2
                        superCitizensRemaining++;
                        superCitizenSemaphore.release();
                    }
                    //output
                    System.out.println(" ");
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
            }



            if (shutdownRequested) return; // ALSO NEED THIS

            //Citizen Processing
            if(type.equals("Regular")){
                if(regularCitizenSemaphore.tryAcquire() && regularCitizensRemaining > 0){
                    regularCitizensRemaining--;
                    n_rC++;
                    rc_ID++;
                    if(cur_team.availablePermits()==4){ //means current team is empty
                        System.out.println("Regular Citizen " + rc_ID + " is signing up");
                    } else {
                        System.out.println("Regular Citizen " + rc_ID + " has joined team " + checkTeam);
                    }

                    cur_team.acquire();//only then we get a permit.
                } else {
                    System.out.println("Regular is waiting for Super"); //to avoid starving out super, we instead call for one if available.
                    if (superCitizenSemaphore.tryAcquire() && superCitizensRemaining > 0) {
                        superCitizensRemaining--;
                        n_sC++;
                        sc_ID++;
                        cur_team.acquire();
                        System.out.println("Super Citizen " + sc_ID + " has joined team " + checkTeam + " after Regular Citizen's request");
                    } else {
                        System.out.println("Super failed to join even after waiting");
                        return; // Can't form a team, return
                    }
                }
            } else {
                if(superCitizenSemaphore.tryAcquire() && regularCitizensRemaining > 0){
                    superCitizensRemaining--;
                    n_sC++;
                    sc_ID++;
                    if(cur_team.availablePermits()==4){ //means current team is empty
                        System.out.println("Super Citizen " + sc_ID + " is signing up");
                    } else {
                        System.out.println("Super Citizen " + sc_ID + " has joined team " + checkTeam);
                    }
                    cur_team.acquire();
                } else { //To avoid starving out regulars, regular is requested.
                    System.out.println("Super is waiting for Regular");
                    if (regularCitizenSemaphore.tryAcquire() && regularCitizensRemaining > 0) {
                        regularCitizensRemaining--;
                        n_rC++;
                        rc_ID++;
                        cur_team.acquire();
                        System.out.println("Regular Citizen " + rc_ID + " has joined team " + checkTeam + " after Super Citizen's request");
                    } else {
                        System.out.println("Regular failed to join even after waiting");
                        return; // Can't form a team, return
                    }
                }
            }
            //System.out.println("Current team: " + teamsSent);
            //System.out.println("Current Citizens: " + (4 - cur_team.availablePermits()));

            //Formed Team Send
            if(cur_team.availablePermits()==0){
                regularCitizenSemaphore.release(n_rC); // signal
                superCitizenSemaphore.release(n_sC);
                cur_team.release(4);
                //System.out.println("Regular Citizen Permits: " + regularCitizenSemaphore.availablePermits());
                //System.out.println("Super Citizen Permits: " + superCitizenSemaphore.availablePermits());
                //System.out.println("Current Team Reset:" + cur_team.availablePermits());
                checkTeam++;
                System.out.println("Team " + teamsSent + " is ready and now launching to battle (sc: " + n_sC + " | rc: " + n_rC + ")");
                teamsSent++;
                n_rC=0;
                n_sC=0;
                //System.out.println("n_rC: " + n_rC + " n_sC: " + n_sC);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}