package fr.opoc.koth;

import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;


import java.util.*;
import java.util.function.Consumer;


public class KothManager {
  private final KothPlugin plugin;
  private Location pos1;
  private Location pos2;
  private Player currentKing;
  private int timeAsKing;
  private final Map<String, ScheduledTask> runningTasks = new HashMap<>();


  public KothManager(KothPlugin plugin) {
    this.plugin = plugin;
    this.timeAsKing = 0;
  }

  public void setPos1(Location pos1) {
    this.pos1 = pos1;
  }

  public void setPos2(Location pos2) {
    this.pos2 = pos2;
  }

  public Location getPos1() {
    return pos1;
  }

  public Location getPos2() {
    return pos2;
  }

  public boolean isSelectionComplete() {
    return pos1 != null && pos2 != null;
  }



  public void startGame(KothArea area) {
    //stopGame(area.getName());  // Stop any existing game for this area before starting a new one

    if (area == null) {
      plugin.getLogger().warning("KOTH area is null. Cannot start game.");
      return;
    }

    RegionScheduler scheduler = Bukkit.getRegionScheduler();

    ScheduledTask task = scheduler.runAtFixedRate(this.plugin, Bukkit.getWorlds().get(0).getSpawnLocation().add(0, 40, 0), new Consumer<ScheduledTask>() {

      int timer = 300;

      @Override
      public void accept(ScheduledTask task) {
        if (timer <= 0) {
          task.cancel();
          Bukkit.broadcast(Component.text("The king has won!"));
          return;
        }
        if (currentKing != null) {
          timeAsKing++;
          plugin.getLogger().info(currentKing.getName() + " is the king for " + timeAsKing + " seconds.");
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
          if (isInKothArea(player.getLocation(), area)) {
            if (currentKing == null || !currentKing.equals(player)) {
              currentKing = player;
              timeAsKing = 0;
              plugin.getLogger().info(player.getName() + " is the new king!");
            }
          }
        }
        timer--;
      }
    }, 20, 20);

    runningTasks.put(area.getName(), task);

  }

  public void stopGame(String areaName) {
    ScheduledTask task = runningTasks.remove(areaName);
    if (task != null) {
      task.cancel();
      currentKing = null;
      timeAsKing = 0;
      plugin.getLogger().info("KOTH game for area '" + areaName + "' stopped.");
    }
  }

  private boolean isInKothArea(Location location, KothArea area) {
    Location pos1 = area.getPos1();
    Location pos2 = area.getPos2();

    double minX = Math.min(pos1.getX(), pos2.getX());
    double minY = Math.min(pos1.getY(), pos2.getY());
    double minZ = Math.min(pos1.getZ(), pos2.getZ());

    double maxX = Math.max(pos1.getX(), pos2.getX());
    double maxY = Math.max(pos1.getY(), pos2.getY());
    double maxZ = Math.max(pos1.getZ(), pos2.getZ());

    return location.getX() >= minX && location.getX() <= maxX &&
            location.getY() >= minY && location.getY() <= maxY &&
            location.getZ() >= minZ && location.getZ() <= maxZ;
  }
  public void scheduleGame(KothArea area, int dayOfWeek, int hour, int minute) {
    Timer timer = new Timer();
    TimerTask task = new TimerTask() {
      @Override
      public void run() {
        Bukkit.getScheduler().runTask(plugin, () -> startGame(area));
      }
    };

    Calendar startTime = Calendar.getInstance();
    startTime.set(Calendar.DAY_OF_WEEK, dayOfWeek);
    startTime.set(Calendar.HOUR_OF_DAY, hour);
    startTime.set(Calendar.MINUTE, minute);
    startTime.set(Calendar.SECOND, 0);

    // If the scheduled time is before the current time, add one week to the start time
    if (startTime.getTime().before(new Date())) {
      startTime.add(Calendar.WEEK_OF_YEAR, 1);
    }

    timer.scheduleAtFixedRate(task, startTime.getTime(), 7 * 24 * 60 * 60 * 1000); // Repeat weekly
    area.setScheduledTimer(timer);
    plugin.getLogger().info("Scheduled KOTH game for area '" + area.getName() + "' to start every " + startTime.getTime());
  }

  private KothArea getKothArea(String name) {
    // This method should return the KothArea by name
    // Implement this method according to your KothArea storage logic
    return null;
  }
}