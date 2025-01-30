import TSim.*;
import java.util.concurrent.Semaphore;
import java.lang.Math;


public class Lab1 {

  public Lab1(int speed1, int speed2) {
    TSimInterface tsi = TSimInterface.getInstance();

    /* public */ TrainHandler train1 = new TrainHandler(tsi, 1, speed1, Section.North1);
    /* public */ TrainHandler train2 = new TrainHandler(tsi, 2, speed2, Section.South1);
    Thread t1 = new Thread(train1);
    Thread t2 = new Thread(train2);

    t1.run();
    t2.run();

  }

  private class TrainHandler implements Runnable{
    private static final int MAXSPEED = 30; // TODO: find actual max

    private final TSimInterface tsi;
    private final int id;
    private int currentSpeed;
    private Section currentTrack;

    TrainHandler(TSimInterface tsi, int id, int initSpeed, Section startSec) {
      this.tsi = tsi;
      this.id = id;
      this.currentSpeed = initSpeed;
      this.currentTrack = startSec;
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
    North1, North2,
    East,
    Center1, Center2,
    West,
    South1, South2
  }

  private enum Heading {
    North, South
  }
}
