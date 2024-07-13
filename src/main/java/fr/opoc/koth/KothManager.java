package fr.opoc.koth;

import fr.naruse.factions.faction.Faction;
import fr.naruse.gamescore.math.FireworkUtils;
import fr.naruse.gamescore.utils.GamePlanning;
import fr.naruse.gamescore.utils.IYCScoreboard;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;


import java.time.DayOfWeek;
import java.util.*;


public class KothManager {
  private final KothPlugin plugin;
  private Location pos1;
  private Location pos2;
  private Faction currentFaction; // Assuming you have a Faction class that manages factions
  int victoryTimer; // 5 minutes to hold the area for victory
  int maxGameTimer; // 30 minutes total game time
  int controlChangeDelay; // 5 seconds delay before control changes
  private final Map<String, ScheduledTask> runningTasks = new HashMap<>();
  private final Map<String, KothArea> kothAreas;
  private IYCScoreboard kothScoreboard;

  int DEFAULT_CONTROL_CHANGE_DELAY = 0; // 5 seconds

  private Player currentControlPlayer; // Track the player who last controlled the area
  private boolean captureByFaction = false; // Default is capture by faction, set to false for capture by individual players


  private boolean timerLineVisible = false; // Track visibility of the timer line




  public KothManager(KothPlugin plugin) {
    this.plugin = plugin;
    this.kothAreas = new HashMap<>();

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


  public Map<String, KothArea> getKothAreas() {
      return kothAreas;
  }


  public void startGame(KothArea area) {
    victoryTimer = 60; // 60 seconds to hold the area for victory
    maxGameTimer = 1800; // 30 minutes total game time
    controlChangeDelay = DEFAULT_CONTROL_CHANGE_DELAY; // 10 seconds to capture control
    currentFaction = null;
    kothScoreboard = new IYCScoreboard();

    if (area == null) {
      plugin.getLogger().warning("KOTH area is null. Cannot start game.");
      return;
    }

    RegionScheduler scheduler = Bukkit.getRegionScheduler();
    Location center = getCenter(area);
    kothScoreboard.getOptions().enableZone(center, 20);
    kothScoreboard.insertLine(0, new IYCScoreboard.Line("§6§lKOTH",IYCScoreboard.Position.CENTER));
    kothScoreboard.insertLine(1, new IYCScoreboard.Line("§eNom: §b" + area.getName(),IYCScoreboard.Position.LEFT));
    kothScoreboard.insertLine(2, new IYCScoreboard.Line("§eCoordonnées: §b" + center.getX() + ", " + center.getY() + ", " + center.getZ(),IYCScoreboard.Position.LEFT));
    kothScoreboard.insertLine(3, new IYCScoreboard.Line("§eAucune faction présente",IYCScoreboard.Position.LEFT));
    Bukkit.broadcast(Component.text("§eLe KOTH §b§l" + area.getName() + "§r§a a commencé aux coordonnées: " + center.getX() + ", " + center.getY() + ", " + center.getZ()));
    ScheduledTask task = scheduler.runAtFixedRate(this.plugin, Bukkit.getWorlds().get(0).getSpawnLocation().add(0, 40, 0), task1 -> {
      if (victoryTimer <= 0) {
        task1.cancel();
        Bukkit.broadcast(Component.text("§aLa faction §5§l" + currentFaction.getName() + "§r§a a gagné!"));
        kothScoreboard.destroy();
        FireworkUtils.build(this.plugin, center, 10);
        return;
      }
      if (maxGameTimer <= 0) {
        task1.cancel();
        Bukkit.broadcast(Component.text("§cLe jeu est terminé, aucune faction n'a gagné."));
        kothScoreboard.destroy();
        return;
      }
      updateAreaControl(area);
      maxGameTimer--;
    }, 20, 20);

    runningTasks.put(area.getName(), task);
  }


  private void updateAreaControl(KothArea area) {
    List<Player> playersInArea = getPlayersInArea(area);
    Map<Object, Integer> controlCounts = new HashMap<>();

    if (captureByFaction) {
      for (Player player : playersInArea) {
        Faction faction = Faction.getPlayerFaction(player);
        if (faction != null) {
          controlCounts.put(faction, controlCounts.getOrDefault(faction, 0) + 1);
        }
      }
    } else {
      // Treat players as individuals but consider factions for conflict resolution
      for (Player player : playersInArea) {
        controlCounts.put(player, controlCounts.getOrDefault(player, 0) + 1);
      }
    }

    updateControlBasedOnCounts(controlCounts, playersInArea);
  }

  private void updateControlBasedOnCounts(Map<Object, Integer> controlCounts, List<Player> playersInArea) {
    if (captureByFaction) {
      if (controlCounts.size() > 1) {
        controlChangeDelay = DEFAULT_CONTROL_CHANGE_DELAY;
        kothScoreboard.updateLine(3, new IYCScoreboard.Line("§c§lContesté", IYCScoreboard.Position.LEFT));
        resetTimerLine();
      } else if (controlCounts.size() == 1) {
        Object controlUnit = controlCounts.keySet().iterator().next();
        updateControlUnit(controlUnit);
      } else {
        currentFaction = null;
        currentControlPlayer = null;
        controlChangeDelay = DEFAULT_CONTROL_CHANGE_DELAY;
        kothScoreboard.updateLine(3, new IYCScoreboard.Line("§eAucune faction présente", IYCScoreboard.Position.LEFT));
        resetTimerLine();
      }
    } else {
      // Treat players as individuals but handle same faction players
      Map<Faction, Integer> factionCounts = new HashMap<>();
      for (Player player : playersInArea) {
        Faction faction = Faction.getPlayerFaction(player);
        if (faction != null) {
          factionCounts.put(faction, factionCounts.getOrDefault(faction, 0) + 1);
        }
      }

      if (controlCounts.size() > 1 && factionCounts.size() > 1) {
        controlChangeDelay = DEFAULT_CONTROL_CHANGE_DELAY;
        kothScoreboard.updateLine(3, new IYCScoreboard.Line("§c§lContesté", IYCScoreboard.Position.LEFT));
        resetTimerLine();
      } else if (!controlCounts.isEmpty()) {
        Object controlUnit = controlCounts.keySet().iterator().next();
        if (currentControlPlayer !=null && !currentControlPlayer.equals(controlUnit)) {
          currentFaction = null;
          currentControlPlayer = null;
          controlChangeDelay = DEFAULT_CONTROL_CHANGE_DELAY;
          resetTimerLine();
        }
        updateControlUnit(controlUnit);
      } else {
        currentFaction = null;
        currentControlPlayer = null;
        controlChangeDelay = DEFAULT_CONTROL_CHANGE_DELAY;
        kothScoreboard.updateLine(3, new IYCScoreboard.Line("§eAucune faction présente", IYCScoreboard.Position.LEFT));
        resetTimerLine();
      }
    }
  }

  private void updateControlUnit(Object controlUnit) {
    Faction faction = null;
    Player player = null;
    String factionName = "Aucune faction";

    if (controlUnit instanceof Faction) {
      faction = (Faction) controlUnit;
      player = findPlayerFromFaction(faction);
    } else if (controlUnit instanceof Player) {
      player = (Player) controlUnit;
      faction = Faction.getPlayerFaction(player);
      factionName = faction != null ? faction.getName() : "Aucune faction";

    }
    if (currentFaction == null || !currentFaction.equals(faction) || currentControlPlayer == null || !currentControlPlayer.equals(player)) {
      if (controlChangeDelay > 0) {
        kothScoreboard.updateLine(3, new IYCScoreboard.Line("§eCapture par: §5§l" + factionName + " (" + player.getName() + ") en " + controlChangeDelay + " sec.", IYCScoreboard.Position.LEFT));
        controlChangeDelay--;
      } else {
        currentFaction = faction;
        currentControlPlayer = player;
        victoryTimer = 60;
        updateScoreboard();

      }
    } else {
      updateScoreboard();

      victoryTimer--;
    }
  }

  private void updateScoreboard() {
    kothScoreboard.updateLine(3, new IYCScoreboard.Line("§eContrôlé par:", IYCScoreboard.Position.LEFT));
    kothScoreboard.updateLine(4, new IYCScoreboard.Line("§eJoueur: §b" + currentControlPlayer.getName(), IYCScoreboard.Position.LEFT));
    kothScoreboard.updateLine(5, new IYCScoreboard.Line("§eFaction: §b" + currentFaction.getName(), IYCScoreboard.Position.LEFT));
    kothScoreboard.updateLine(6, new IYCScoreboard.Line("§eTemps: §b" + victoryTimer + " sec.", IYCScoreboard.Position.LEFT));
  }


//  private void resetTimerLine() {
//    if (timerLineVisible) {
//      kothScoreboard.removeLine(4);
//      timerLineVisible = false;
//    }
//  }
private void resetTimerLine() {
    kothScoreboard.removeLine(4);

}




  private Player findPlayerFromFaction(Faction faction) {
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (Faction.getPlayerFaction(player).equals(faction)) {
        return player;
      }
    }
    return null;
  }





  private List<Player> getPlayersInArea(KothArea area) {
    List<Player> players = new ArrayList<>();
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (isInKothArea(player.getLocation(), area)) {
        players.add(player);
      }
    }
    return players;
  }

  private boolean isInKothArea(Location location, KothArea area) {
    double minX = Math.min(area.getPos1().getX(), area.getPos2().getX());
    double minY = Math.min(area.getPos1().getY(), area.getPos2().getY());
    double minZ = Math.min(area.getPos1().getZ(), area.getPos2().getZ());
    double maxX = Math.max(area.getPos1().getX(), area.getPos2().getX());
    double maxY = Math.max(area.getPos1().getY(), area.getPos2().getY());
    double maxZ = Math.max(area.getPos1().getZ(), area.getPos2().getZ());

    return location.getX() >= minX && location.getX() <= maxX &&
            location.getY() >= minY && location.getY() <= maxY &&
            location.getZ() >= minZ && location.getZ() <= maxZ;
  }

  public void stopGame(String areaName) {
    ScheduledTask task = runningTasks.remove(areaName);
    if (task != null) {
      task.cancel();
      Bukkit.broadcast(Component.text("KOTH game for area '" + areaName + "' stopped."));
      //resetGame();
    }
  }

  public void saveKothArea(KothArea area) {
    FileConfiguration config = plugin.getKothConfig();
    String pathBase = "kothAreas." + area.getName();
    config.set(pathBase + ".pos1", locationToString(area.getPos1()));
    config.set(pathBase + ".pos2", locationToString(area.getPos2()));
    plugin.saveConfig(config);
  }
  public void updateSchedule(KothArea area) {
    FileConfiguration config = plugin.getKothConfig();
    String pathBase = "kothAreas." + area.getName();
    config.set(pathBase + ".day", area.getScheduledDay().toString());
    config.set(pathBase + ".hour", area.getScheduledHour());
    config.set(pathBase + ".minute", area.getScheduledMinute());
    plugin.saveConfig(config);
  }

  private String locationToString(Location location) {
    if (location == null) return "";
    return location.getWorld().getName() + "," + location.getX() + "," + location.getY() + "," + location.getZ();
  }

  public void loadKothAreas() {
    FileConfiguration config = plugin.getKothConfig();
    ConfigurationSection kothSection = config.getConfigurationSection("kothAreas");
    if (kothSection == null) return;

    for (String key : kothSection.getKeys(false)) {
      ConfigurationSection section = kothSection.getConfigurationSection(key);
      if (section != null) {

          Location pos1 = stringToLocation(section.getString("pos1"));
          Location pos2 = stringToLocation(section.getString("pos2"));
          KothArea area = new KothArea(key, pos1, pos2);
          kothAreas.put(key, area);

            String dayString = section.getString("day");
            if (dayString != null) {
              DayOfWeek day = DayOfWeek.valueOf(dayString);
              int hour = section.getInt("hour");
              int minute = section.getInt("minute");
              area.setSchedule(day, hour, minute);
              GamePlanning.planEveryWeek(this.plugin, day, hour, minute, () -> this.startGame(area), 5 * 60, integer -> {
                Bukkit.broadcast(Component.text("§aLe KOTH " + area.getName() + " commence dans 5 minutes."));
                if (integer == 60) {
                  Bukkit.broadcast(Component.text("§eLe KOTH " + area.getName() + " commence dans 1 minute."));
                }
                if (integer == 30) {
                  Bukkit.broadcast(Component.text("§eLe KOTH " + area.getName() + " commence dans 30 secondes."));
                }
                if (integer == 10) {
                  Bukkit.broadcast(Component.text("§eLe KOTH " + area.getName() + " commence dans 10 secondes."));
                }
              });
            }
      }
    }
  }


  private Location stringToLocation(String locString) {
    if (locString == null || locString.trim().isEmpty()) return null;
    String[] parts = locString.split(",");
    World world = Bukkit.getWorld(parts[0]);
    double x = Double.parseDouble(parts[1]);
    double y = Double.parseDouble(parts[2]);
    double z = Double.parseDouble(parts[3]);
    return new Location(world, x, y, z);
  }
  public Location getCenter(KothArea area) {
    double centerX = (area.getPos1().getX() + area.getPos2().getX()) / 2;
    double centerY = (area.getPos1().getY() + area.getPos2().getY()) / 2;
    double centerZ = (area.getPos1().getZ() + area.getPos2().getZ()) / 2;
    return new Location(area.getPos1().getWorld(), centerX, centerY, centerZ);
  }
}