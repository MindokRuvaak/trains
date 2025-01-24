import TSim.*;
import java.util.concurrent.Semaphore;
import java.lang.Math;


public class Lab1 {

  public Lab1(int speed1, int speed2) {
    TSimInterface tsi = TSimInterface.getInstance();

    trainHandler train1 = new trainHandler(tsi, 1, speed1);
    trainHandler train2 = new trainHandler(tsi, 2, speed2);


  }

  private class trainHandler {
    private static final int MAXSPEED = 30;
    
    private final TSimInterface tsi;
    private final int id;
  

    trainHandler(TSimInterface tsi, int id, int initSpeed) {
      this.tsi = tsi;
      this.id = id;
      setSpeed(initSpeed);
    }

    void setSpeed(int speed) {
      try {
        this.tsi.setSpeed(this.id, Math.clamp(speed, -MAXSPEED, MAXSPEED) );
      } catch (CommandException e) {
        e.printStackTrace(); // or only e.getMessage() for the error
        System.exit(1);
      }
    }
  }
}
