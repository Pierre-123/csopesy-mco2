package src;

import java.util.Scanner;
import java.util.concurrent.*;

public class Main {
    private static final int TEAM_SIZE = 4;
    private static Semaphore superCitizenSemaphore;
    private static Semaphore regularCitizenSemaphore;
    private static Semaphore teamSemaphore;
    private static int teamsSent = 0;
    private static int checkTeam = 1;
    private static Semaphore cur_team;
    private static ExecutorService executor;
    private static int regularCitizensRemaining;
    private static int superCitizensRemaining;
    private static boolean shutdownRequested = false;
    private static int n_rC = 0;
    private static int n_sC = 0;
    private static int rc_ID = 1;
    private static int sc_ID = 1;

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


    public synchronized void dismissed() {
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

    public synchronized void teamUp(String type, int id) {
        try {
            if (shutdownRequested) return; // NEED THIS, closing out for 3 1, empty out regulars and supers

            //Exit Code for Incomplete Team
            if(cur_team.availablePermits() > 0){
                if(regularCitizensRemaining > 0){//Leftover Regulars
                    if(superCitizensRemaining == 0 && superCitizenSemaphore.availablePermits()==2){
                        if(regularCitizenSemaphore.availablePermits()>=0){
                            dismissed(); //10 0, 4 0, 3 0, 1 0
                        }
                    }
                }
                if(superCitizensRemaining > 0){//Leftover Supers
                    if(regularCitizensRemaining==0 && regularCitizenSemaphore.availablePermits()>1){
                        if(superCitizenSemaphore.availablePermits()>=0){
                            dismissed(); //0 10, 0 4, 0 3, 0 1, 1 3
                        }
                    }
                }
                if(regularCitizensRemaining <= 2 && regularCitizenSemaphore.availablePermits() > 1 && superCitizensRemaining <= 1 && superCitizenSemaphore.availablePermits() == 2) { //Both with leftovers
                    dismissed();
                }
            }

            if (shutdownRequested) return; // ALSO NEED THIS, closing out leftover regulars and supers

            //Citizen Processing
            if(type.equals("Regular") && regularCitizensRemaining > 0){
                System.out.println("Regular Citizen " + rc_ID + " is signing up");
                if(regularCitizenSemaphore.tryAcquire()){
                    regularCitizensRemaining--;
                    n_rC++;
                    System.out.println(regularCitizensRemaining);
                    System.out.println("Regular Citizen " + rc_ID + " has joined team " + checkTeam);
                    rc_ID++;

                    cur_team.acquire();//only then we get a permit.
                } else {
                    System.out.println("Regular is waiting for Super"); //to avoid starving out super, we instead call for one if available.
                    if (superCitizenSemaphore.tryAcquire() && superCitizensRemaining > 0) {
                        superCitizensRemaining--;
                        n_sC++;

                        cur_team.acquire();
                        System.out.println("Super Citizen " + sc_ID + " has joined team " + checkTeam + " after Regular Citizen's request");
                        sc_ID++;
                    } else {
                        System.out.println("Super failed to join even after waiting");
                        return; // Can't form a team, return
                    }
                }
            } else {
                System.out.println("Super Citizen " + sc_ID + " is signing up");
                if(superCitizenSemaphore.tryAcquire() && superCitizensRemaining > 0){
                    superCitizensRemaining--;
                    n_sC++;

                    System.out.println("Super Citizen " + sc_ID + " has joined team " + checkTeam);
                    sc_ID++;
                    cur_team.acquire();
                } else { //To avoid starving out regulars, regular is requested.
                    System.out.println("Super is waiting for Regular");
                    if (regularCitizenSemaphore.tryAcquire() && regularCitizensRemaining > 0) {
                        regularCitizensRemaining--;
                        n_rC++;
                        System.out.println(regularCitizensRemaining);
                        cur_team.acquire();
                        System.out.println("Regular Citizen " + rc_ID + " has joined team " + checkTeam + " after Super Citizen's request");
                        rc_ID++;
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
                //System.out.println("Regular Citizens Remaining: " + regularCitizensRemaining);
                //System.out.println("Super Citizens Remaining: " + superCitizensRemaining);
                checkTeam++;

                teamsSent++;
                System.out.println("Team " + teamsSent + " is ready and now launching to battle (sc: " + n_sC + " | rc: " + n_rC + ")");
                System.out.println("------------------------------------");


                n_rC=0;
                n_sC=0;
                if(superCitizensRemaining == 0 && regularCitizensRemaining == 0){
                    dismissed();//for perfect scenarios like 3 1, if both supers and regulars manage to empty out.
                }

                //System.out.println("n_rC: " + n_rC + " n_sC: " + n_sC);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}