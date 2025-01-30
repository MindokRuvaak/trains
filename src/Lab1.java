import TSim.*;
import java.util.concurrent.Semaphore;
import java.lang.Math;


public class Lab1 {

  public Lab1(int speed1, int speed2) throws InterruptedException  {
    TSimInterface tsi = TSimInterface.getInstance();

    trainHandler train1 = new trainHandler(tsi, 1, speed1);
    trainHandler train2 = new trainHandler(tsi, 2, speed2);

    Thread t1 =new Thread(train1);
    Thread t2 =new Thread(train2);

    t1.start();
    t2.start();
    
    Semaphore[] sems = new Semaphore[9];
    //sempahore 0: upperTopStation
    //sempahore 1: upperBotStation
    //sempahore 2: belowTopStation
    //sempahore 3: belowBotStation
    //sempahore 4: 
    //sempahore 5:
    //sempahore 6:
    //sempahore 7:
    //sempahore 8:

  }

  class trainHandler implements Runnable{
    private static final int MAXSPEED = 30;

    private final TSimInterface tsi;
    private final int id;
    private int speed;

    private boolean criticalSection, upperStation, belowStation;
  

    trainHandler(TSimInterface tsi, int id, int initSpeed) {
      this.tsi = tsi;
      this.id = id;
      this.speed = initSpeed;

      this.criticalSection=false;
      this.upperStation=false;
      this.belowStation=false;

      setSpeed(initSpeed);
    }

    void setSpeed(int speed) {
      try {
        this.tsi.setSpeed(this.id, Math.clamp(speed, -MAXSPEED, MAXSPEED));
      } catch (CommandException e) {
        e.printStackTrace(); // or only e.getMessage() for the error
        System.exit(1);
      }
    }

    @Override
    public void run() {
      try {
        while (true) {
          SensorEvent sensor = tsi.getSensor(id);
          //sensorHandler(sensor);
        }
      } catch (CommandException | InterruptedException e) {
        e.printStackTrace(); // or only e.getMessage() for the error
      }
    }

    public void sensorHandler(SensorEvent sens, Semaphore sem){
      int x = sens.getXpos();
      int y = sens.getYpos();

    if(train1.getXpos==x && train1.getPos ==y ){
        //TODO
      }
    }
  }
}
