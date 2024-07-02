package fr.opoc.koth;

import org.bukkit.Location;

import java.time.DayOfWeek;

public class KothArea {
  private final String name;
  private final Location pos1;
  private final Location pos2;
  private DayOfWeek scheduledDay;
  private int scheduledHour;
  private int scheduledMinute;


  public KothArea(String name, Location pos1, Location pos2) {
    this.name = name;
    this.pos1 = pos1;
    this.pos2 = pos2;
    this.scheduledDay = null;
    this.scheduledHour = -1;
    this.scheduledMinute = -1;
  }

  public String getName() {
    return name;
  }

  public Location getPos1() {
    return pos1;
  }

  public Location getPos2() {
    return pos2;
  }


  public DayOfWeek getScheduledDay() {
    return scheduledDay;
  }

  public int getScheduledHour() {
    return scheduledHour;
  }
  public int getScheduledMinute() {
    return scheduledMinute;
  }
  public void setSchedule(DayOfWeek day, int hour, int minute) {
    this.scheduledDay = day;
    this.scheduledHour = hour;
    this.scheduledMinute = minute;
  }
}
