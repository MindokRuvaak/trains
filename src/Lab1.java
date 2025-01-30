import TSim.*;
import java.util.concurrent.Semaphore;
import java.lang.Math;

public class Lab1 {

  public Lab1(int speed1, int speed2) {
    TSimInterface tsi = TSimInterface.getInstance();

    /* public */ TrainHandler train1 = new TrainHandler(tsi, 1, speed1, Section.North, Heading.South);
    /* public */ TrainHandler train2 = new TrainHandler(tsi, 2, speed2, Section.South, Heading.North);
    Thread t1 = new Thread(train1);
    Thread t2 = new Thread(train2);

    t1.run();
    t2.run();

  }

  private class TrainHandler implements Runnable {
    private static final int MAXSPEED = 30; // TODO: find actual max

    private final TSimInterface tsi;
    private final int id;
    private int currentSpeed;
    private Section currentTrack;
    private Heading dir;

    TrainHandler(TSimInterface tsi, int id, int initSpeed, Section startSec, Heading dir) {
      this.tsi = tsi;
      this.id = id;
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
