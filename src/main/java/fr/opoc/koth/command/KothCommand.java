package fr.opoc.koth.command;

import fr.opoc.koth.KothArea;
import fr.opoc.koth.KothManager;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Calendar;
import java.util.Map;

public class KothCommand implements CommandExecutor {
  private final KothManager kothManager;
  private final Map<String, KothArea> kothAreas;

  public KothCommand(KothManager kothManager, Map<String, KothArea> kothAreas) {
    this.kothManager = kothManager;
    this.kothAreas = kothAreas;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
    if (sender instanceof Player player) {

      if (args.length > 0) {
        switch (args[0].toLowerCase()) {
          case "start":
            if (args.length > 1) {
              String name = args[1];
              if (kothAreas.containsKey(name)) {
                kothManager.startGame(kothAreas.get(name));
                player.sendMessage("KOTH game started for area: " + name);
              } else {
                player.sendMessage("No KOTH area found with that name.");
              }
            } else {
              player.sendMessage("Please specify the name of the KOTH area.");
            }
            return true;
          case "axe":
            ItemStack axe = new ItemStack(Material.IRON_AXE);
            player.getInventory().addItem(axe);
            player.sendMessage("You have been given the KOTH area selection axe!");
            return true;
          case "create":
            if (args.length > 1) {
              String name = args[1];
              if (kothManager.isSelectionComplete()) {
                KothArea area = new KothArea(name, kothManager.getPos1(), kothManager.getPos2());
                kothAreas.put(name, area);
                player.sendMessage("KOTH area '" + name + "' created successfully.");
              } else {
                player.sendMessage("You need to set both positions before creating a KOTH area.");
              }
            } else {
              player.sendMessage("Please specify a name for the KOTH area.");
            }
            return true;
          case "list":
            player.sendMessage("KOTH areas:");
            for (String areaName : kothAreas.keySet()) {
              player.sendMessage("- " + areaName);
            }
            return true;
          case "stop":
            if (args.length > 1) {
              String name = args[1];
              if (kothAreas.containsKey(name)) {
                kothManager.stopGame(name);
                player.sendMessage("KOTH game stopped for area: " + name);
              } else {
                player.sendMessage("No KOTH area found with that name.");
              }
            } else {
              player.sendMessage("Please specify the name of the KOTH area to stop.");
            }
            return true;

          case "schedule":
            if (args.length == 4) {
              String name = args[1];
              String day = args[2];
              String time = args[3];
              if (kothAreas.containsKey(name)) {
                int dayOfWeek;
                switch (day.toLowerCase()) {
                  case "sunday": dayOfWeek = Calendar.SUNDAY; break;
                  case "monday": dayOfWeek = Calendar.MONDAY; break;
                  case "tuesday": dayOfWeek = Calendar.TUESDAY; break;
                  case "wednesday": dayOfWeek = Calendar.WEDNESDAY; break;
                  case "thursday": dayOfWeek = Calendar.THURSDAY; break;
                  case "friday": dayOfWeek = Calendar.FRIDAY; break;
                  case "saturday": dayOfWeek = Calendar.SATURDAY; break;
                  default:
                    player.sendMessage("Invalid day of the week.");
                    return true;
                }

                String[] timeParts = time.split(":");
                int hour = Integer.parseInt(timeParts[0]);
                int minute = Integer.parseInt(timeParts[1]);

                kothManager.scheduleGame(kothAreas.get(name), dayOfWeek, hour, minute);
                player.sendMessage("KOTH game for area '" + name + "' scheduled to start every " + day + " at " + time);
              } else {
                player.sendMessage("No KOTH area found with that name.");
              }
            } else {
              player.sendMessage("Usage: /koth schedule <name> <day> <HH:MM>");
            }
            return true;
          default:
            player.sendMessage("Usage: /koth <start|axe|create|list>");
            return true;
        }
      } else {
        player.sendMessage("Usage: /koth <start|axe|create|list>");
        return true;
      }
    }
    return false;
  }
}
