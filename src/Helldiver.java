package src;

public class Helldiver implements Runnable{
    String type;
    int id;
    Main main;

    public Helldiver(String type, int id, Main main){
        this.type = type;
        this.id = id;
        this.main = main;
    }

    public String getType(){
        return type;
    }

    public int getId(){
        return id;
    }

    @Override
    public void run() {
        main.teamUp(type, id);
    }
}
