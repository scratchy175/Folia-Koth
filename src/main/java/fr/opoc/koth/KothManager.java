package fr.opoc.koth;

import fr.naruse.factions.faction.Faction;
import fr.naruse.factions.faction.politic.FactionPolitic;
import fr.naruse.factions.faction.politic.FactionRelation;
import fr.naruse.gamescore.utils.GamePlanning;
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
  int victoryTimer = 60; // 5 minutes to hold the area for victory
  int maxGameTimer = 1800; // 30 minutes total game time
  int controlChangeDelay = 10 ; // 5 seconds delay before control changes
  private final Map<String, ScheduledTask> runningTasks = new HashMap<>();
  private final Map<String, KothArea> kothAreas;


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
    if (area == null) {
      plugin.getLogger().warning("KOTH area is null. Cannot start game.");
      return;
    }

    RegionScheduler scheduler = Bukkit.getRegionScheduler();
    Location center = getCenter(area);
    Bukkit.broadcast(Component.text("§eLe KOTH §b§l" + area.getName() + "§r§a a commencé aux coordonnées: " + center.getX() + ", " + center.getY() + ", " + center.getZ()));
    ScheduledTask task = scheduler.runAtFixedRate(this.plugin, Bukkit.getWorlds().get(0).getSpawnLocation().add(0, 40, 0), task1 -> {
      if ( victoryTimer <= 0) {
        task1.cancel();
        Bukkit.broadcast(Component.text("§aLa faction §5§l" + (currentFaction != null ? currentFaction.getName() : "inconnue") + "§r§a a gagné!"));
        return;
      }
      if (maxGameTimer <= 0) {
        task1.cancel();
        Bukkit.broadcast(Component.text("§cLe jeu est terminé, aucune faction n'a gagné."));
        return;
      }
      updateAreaControl(area);
      maxGameTimer--;
    }, 20, 20);

    runningTasks.put(area.getName(), task);
  }

  private void updateAreaControl(KothArea area) {
    List<Player> playersInArea = getPlayersInArea(area);
      Map<Faction, Integer> factionCounts = new HashMap<>();

      for (Player player : playersInArea) {
        Faction faction = Faction.getPlayerFaction(player);
        if (faction != null) {
          factionCounts.put(faction, factionCounts.getOrDefault(faction, 0) + 1);
        }
      }

      if (!factionCounts.isEmpty()){
        if (factionCounts.size() > 1 ) {
          sendActionBarToPlayersNearby(getCenter(area), 50, "§eTemps: " + victoryTimer + " sec. || §4§lContesté ");
          return;
        } else {
          Faction factionInControl = factionCounts.keySet().iterator().next();
          if (currentFaction == null || !currentFaction.equals(factionInControl)) {
            //inverser les deux conditions peut etre
            if (controlChangeDelay > 0) {
              sendActionBarToPlayersNearby(getCenter(area), 50, "§eTemps: " + controlChangeDelay + " sec. || Capture par: §5§l" + factionInControl.getName());
              controlChangeDelay--;
            } else {
              currentFaction = factionInControl;
              Bukkit.broadcast(Component.text("§eLa faction §5§l" + currentFaction.getName() + "§r§e Commence à contrôler la zone!"));
              controlChangeDelay = 10;
              victoryTimer = 60;
            }
            return;
          }
        }
      }
      sendActionBarToPlayersNearby(getCenter(area), 50, "§eTemps: " + victoryTimer + " sec. || Contrôlé par: §5§l" + (currentFaction != null ? currentFaction.getName() : "inconnue"));
      victoryTimer--;
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

  public void sendActionBarToPlayersNearby(Location center, double radius, String message) {
    for (Player player : Bukkit.getServer().getOnlinePlayers()) {
      if (center.getWorld().equals(player.getWorld()) && center.distance(player.getLocation()) <= radius) {
        player.sendActionBar(Component.text(message));
      }
    }
  }
}