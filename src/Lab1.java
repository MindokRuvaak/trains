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
    private int currentSpeed;
    private Section currentTrack;
    private Heading dir;

    TrainHandler(TSimInterface tsi, int id, int initSpeed, Section startSec, Heading dir) {
      this.tsi = tsi;
      this.id = id;
      this.speed = initSpeed;

      this.criticalSection=false;
      this.upperStation=false;
      this.belowStation=false;

      this.currentSpeed = initSpeed;
      this.currentTrack = startSec;
      this.dir = dir;
    }

    void setSpeed(int speed) {
      currentSpeed = Math.clamp(speed, -MAXSPEED, MAXSPEED);
      try {
        this.tsi.setSpeed(this.id, this.currentSpeed);
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

    @Override
    public void run() {
      setSpeed(this.currentSpeed);
    }
  }

  private class TrackSec {
    private final Semaphore sem;
    private final Section sec;

    TrackSec(Section sec) {
      this.sem = new Semaphore(1);
      this.sec = sec;
    }

  }

  private enum Section {
    North(0), /* North2(0), */
    East(1),
    Center(2), /* Center2(2), */
    West(3),
    South(4)/* , South2(4) */;

    private final int id;
    private final int[][] next = { { 1, 1 },
        { 0, 2 },
        { 1, 3 },
        { 2, 4 },
        { 3, 3 } };

    Section(int id) {
      this.id = id;
    }

    int n() {
      return this.id;
    }

    Section next(Heading dir) {
      return fromId(next[this.id][dir.n()]);
    }

    Section fromId(int id) {
      Section sec;
      switch (id) {
        case 0:
        sec = Section.North;
          break;
        case 1:
        sec = Section.East;
          break;
        case 2:
        sec = Section.Center;
          break;
        case 3:
        sec = Section.West;
          break;
        case 4:
        sec = Section.South;
          break;

        default:
        sec = null;
          break;
      }
      return sec;
    }
  }

  private enum Heading {
    North(0), South(1);

    private final int id;

    Heading(int id) {
      this.id = id;
    }

    int n() {
      return this.id;
    }
  }
}
