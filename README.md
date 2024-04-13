# csopesy-mco2
GITHUB LINK: https://github.com/Pierre-123/csopesy-mco2

To run:  
Run `javac -d classes src/*.java` to compile.  
Run `java -classpath ./classes src.Main` to run the program.

Process Synchronization

I. Synchronization Technique  
Semaphore was the synchronization technique utilized for this MCO. There are at least three semaphores used in this program. All semaphores are used in Main.java. The semaphore variables are as follows:
- superCitizenSemaphore – line 30, to keep track of Super Citizens placed in the current team. Maximum of 2 and refreshes by the end of the team formation.
- regularCitizenSemaphore – line 31, to keep track of Regular Citizens placed in the current team. Maximum of 3 and refreshes by the end of the team formation.
- cur_team – line 32, to keep track of the number of citizens placed in the current team. Maximum of 4 and refreshes by the end of the team formation.

II. Team Composition Rules & Constraints
1. Each mission must consist of exactly 4 Helldivers

Line 30:
```
cur_team = new Semaphore(4);
```
Line 30 shows that there are a maximum of 4 Helldivers in a team. There are conditions in the code that checks if the rules below are being met, for that citizen to be placed in a team, which is the citizen processing portion of the code at Lines 101-149. Line 151 in the formed team send portion of the code checks whether or not a team of 4 has been made, which allows a team of 4 to be sent out.
2. Among the 4 Helldivers, there must be at least 1 "Super Citizen," an elite member highly skilled in combat and tactics

The program checks for permits in the code. There are limitations on how many Super Citizens (2) and how many Regular Citizens (3) that can be placed in a team. For processing Super Citizens specifically:

Line 125-134:
```
} else {
   System.out.println("Super Citizen " + sc_ID + " is signing up");
   if(superCitizenSemaphore.tryAcquire() && superCitizensRemaining > 0){
       superCitizensRemaining--;
       n_sC++;


       System.out.println("Super Citizen " + sc_ID + " has joined team " + checkTeam);
       sc_ID++;
       cur_team.acquire();
   } else { //To avoid starving out regulars, regular is requested.
```
Lines 126-133 are for placing Super Citizens specifically. To guarantee a Super Citizen in the team, there is also a check in Line 101 to see if there is space for Regular Citizens. If there is no more space for Regular Citizens, it processes a Super Citizen instead.

Line 101:
```
if(type.equals("Regular") && regularCitizensRemaining > 0){
```
Lines 111-124:
```
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

```
3. Missions can accommodate up to 2 Super Citizens at most

Line 28:
```
superCitizenSemaphore = new Semaphore(2);//max 2, need logic to have at least 1 later in team up
```
Line 28 sets up superCitizenSemaphore, which can only accept 2 Super Citizens at any given time. Similar to regularCitizenSemaphore, this semaphore is used to process citizens to be placed in a team at Lines 101-149.

Lines 101:
```
if(type.equals("Regular") && regularCitizensRemaining > 0){
```
Lines 125-148:
```
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


           cur_team.acquire();
           System.out.println("Regular Citizen " + rc_ID + " has joined team " + checkTeam + " after Super Citizen's request");
           rc_ID++;
       } else {
           System.out.println("Regular failed to join even after waiting");
           return; // Can't form a team, return
       }
   }
}
```
Lines 125-148 are for processing the Super Citizens. The condition at Line 101 checks if an incoming citizen is a Regular Citizen, otherwise, the program will process a Super Citizen. If there are no more permits (2 Super Citizens already - line 127), then the process will process a Regular Citizen instead, disallowing for more than 2 Super Citizens in one team.
4. Regular Citizens, though not as highly skilled as Super Citizens, are still valuable assets and must compose the remaining slots in the team.

Line 29:
```
regularCitizenSemaphore = new Semaphore(3);//max 3 cause at least 1 super
```
Line 29 sets up regularCitizenSemaphore, which can only accept 2 Regular Citizens at any given time.
Lines 100-111:
```
//Citizen Processing
if(type.equals("Regular") && regularCitizensRemaining > 0){
   System.out.println("Regular Citizen " + rc_ID + " is signing up");
   if(regularCitizenSemaphore.tryAcquire()){
       regularCitizensRemaining--;
       n_rC++;


       System.out.println("Regular Citizen " + rc_ID + " has joined team " + checkTeam);
       rc_ID++;


       cur_team.acquire();//only then we get a permit.
   } else {
```
This portion is for checking and placing Regular Citizens in the team. If the slots for Regular Citizens are full (need at least 1 Super Citizen), then a Super Citizen will be processed.

Line 127:
```
if(superCitizenSemaphore.tryAcquire() && superCitizensRemaining > 0){
```
Lines 134-147:
```
} else { //To avoid starving out regulars, regular is requested.
   System.out.println("Super is waiting for Regular");
   if (regularCitizenSemaphore.tryAcquire() && regularCitizensRemaining > 0) {
       regularCitizensRemaining--;
       n_rC++;


       cur_team.acquire();
       System.out.println("Regular Citizen " + rc_ID + " has joined team " + checkTeam + " after Super Citizen's request");
       rc_ID++;
   } else {
       System.out.println("Regular failed to join even after waiting");
       return; // Can't form a team, return
   }
}
```
Similar to Super Citizens, Regular Citizens also have checks on Super Citizens whether or not there are already 2 Super Citizens in a team. If there are, a Regular Citizen will be processed next.

5. Mission signups are on a first-come, first-served basis, so if a certain type of citizen builds up a queue, they must wait until they can be served (headquarters can only do so much...)

Lines 100-148:
```
//Citizen Processing
if(type.equals("Regular") && regularCitizensRemaining > 0){
   System.out.println("Regular Citizen " + rc_ID + " is signing up");
   if(regularCitizenSemaphore.tryAcquire()){
       regularCitizensRemaining--;
       n_rC++;


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


           cur_team.acquire();
           System.out.println("Regular Citizen " + rc_ID + " has joined team " + checkTeam + " after Super Citizen's request");
           rc_ID++;
       } else {
           System.out.println("Regular failed to join even after waiting");
           return; // Can't form a team, return
       }
   }
}
```
The if-else statement in this portion of the code checks for the type of citizen, meaning what citizen is being processed next will be checked and be either placed in the team or will be held and the other type of citizen will be processed if needed.
6. Once the team has been properly composed, the Helldivers are directly launched into their mission.

Lines 150-166:
```
//Formed Team Send
if(cur_team.availablePermits()==0){
   regularCitizenSemaphore.release(n_rC); // signal
   superCitizenSemaphore.release(n_sC);
   cur_team.release(4);
   checkTeam++;


   teamsSent++;
   System.out.println("Team " + teamsSent + " is ready and now launching to battle (sc: " + n_sC + " | rc: " + n_rC + ")");
   System.out.println("------------------------------------");




   n_rC=0;
   n_sC=0;
   if(superCitizensRemaining == 0 && regularCitizensRemaining == 0){
       dismissed();//for perfect scenarios like 3 1, if both supers and regulars manage to empty out.
   }
```
Lines 150-166 hold the code for sending out teams. It checks if the current team is full, which would mean the team’s semaphore value is at zero. If this is true, it releases the relevant semaphores before incrementing the count of the number of teams set. It then prints the message with the team number that was sent, and should there be no longer any Citizens in the queue, it calls for the dismissed() function, the function that signals that the process should terminate.
7. If a team cannot be formed (e.g. only 2 Regular Citizens and 1 Super Citizen are left), then the remaining citizens are sent home

Lines 79-94:
```
if(cur_team.availablePermits() > 0){
   if(regularCitizensRemaining > 0){//Leftover Regulars
       if(superCitizensRemaining == 0 && superCitizenSemaphore.availablePermits()==2){
           if(regularCitizenSemaphore.availablePermits()>=0){
               dismissed(); //10 0, 4 0, 3 0, 1 0
           }
       } else if(regularCitizensRemaining <= 2 && regularCitizenSemaphore.availablePermits() == 3 &&
               superCitizensRemaining <= 1 && superCitizenSemaphore.availablePermits() == 2) { //Both with leftovers
           dismissed();
       }
   } else if(superCitizensRemaining > 0){//Leftover Supers
       if(regularCitizensRemaining==0 && regularCitizenSemaphore.availablePermits()>1){
           if(superCitizenSemaphore.availablePermits()>=0){
               dismissed(); //0 10, 0 4, 0 3, 0 1, 1 3
           }
       }
   }
```
Lines 79 to 95 of the Main.java file, as shown above, serve as the code that determines if there are any soldiers to send home. First, it checks if there are still available slots within the current team.
If there are, it checks if there are Regular Citizens waiting in the queue. If this is true, it then checks for two possible incomplete states with this true: if only Regular Citizens are waiting now, or if there is only at most two Regular Citizens and at most one Super Citizen waiting in the queue.  If either of these are true, it calls the dismissed() function. If there are no longer any Regular Citizens, but there are still Super Citizens, the dismissed() function is also called.
Lines 51-72:
```
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
```
The dismissed() function, found on lines 51-72 on Main.java, is what is in charge of releasing any Citizens waiting in the team when no teams can be made anymore. It then proceeds to display how many Citizens were sent home and how many Teams were sent in total before signaling for the program to shut down.		


