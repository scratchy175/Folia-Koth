package fr.opoc.koth.command;

import fr.naruse.gamescore.utils.GamePlanning;
import fr.opoc.koth.KothArea;
import fr.opoc.koth.KothManager;
import fr.opoc.koth.KothPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

public class KothCommand implements CommandExecutor {
  private final KothPlugin plugin; // Instance of your main class
  private final KothManager kothManager;

  private final Map<String, KothArea> kothAreas;



  public KothCommand(KothPlugin plugin) {
    this.plugin = plugin;
    this.kothManager = plugin.getKothManager(); // Assuming KothManager can be accessed via the plugin instance
    this.kothAreas = kothManager.getKothAreas();
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
    if (sender instanceof Player player) {

      if (args.length > 0 && player.hasPermission("koth.admin")) {
        switch (args[0].toLowerCase()) {
          case "start":
            if (args.length > 1) {
              String name = args[1];
              if (kothAreas.containsKey(name)) {
                kothManager.startGame(kothAreas.get(name));
              } else {
                player.sendMessage("§cAucun KOTH trouvée avec ce nom.");
              }
            } else {
              player.sendMessage("§cVeuillez spécifier le nom du KOTH.");
            }
            return true;
          case "axe":
            ItemStack axe = new ItemStack(Material.IRON_AXE);
            ItemMeta meta = axe.getItemMeta();
            if (meta != null) {
              meta.displayName(Component.text("§4La KOTHache"));
              List<Component> lore = meta.lore();
                if (lore != null) {
                    lore.add(Component.text("§eClique gauche pour définir la position 1"));
                    lore.add(Component.text("§eClique droit pour définir la position 2"));
                }
              meta.lore(lore);
              axe.setItemMeta(meta);
            }
            player.getInventory().addItem(axe);
            player.sendMessage("§aVous avez reçu la hache de sélection de zone KOTH!");
            return true;
          case "create":
            if (args.length > 1) {
              String name = args[1];
              if (kothManager.isSelectionComplete()) {
                KothArea area = new KothArea(name, kothManager.getPos1(), kothManager.getPos2());
                kothAreas.put(name, area);
                kothManager.saveKothArea(area);
                player.sendMessage("§aLe KOTH " + name + " a été créé avec succès.");
              } else {
                player.sendMessage("§cVous devez définir les deux positions avant de créer un KOTH.");
              }
            } else {
              player.sendMessage("§cVeuillez spécifier un nom pour le KOTH.");
            }
            return true;
          case "list":
            player.sendMessage("§eListe des KOTHs:");
            for (String areaName : kothAreas.keySet()) {
              player.sendMessage("§e- " + areaName);
            }
            return true;
          case "stop":
            if (args.length > 1) {
              String name = args[1];
              if (kothAreas.containsKey(name)) {
                kothManager.stopGame(name);
                player.sendMessage("§aLe KOTH " + name + " a été arrêté.");
              } else {
                player.sendMessage("§cAucun KOTH trouvé avec ce nom.");
              }
            } else {
              player.sendMessage("§cVeuillez spécifier le nom du KOTH à arrêter.");
            }
            return true;

          case "schedule":
            if (args.length == 4) {
              String name = args[1];
              if (kothAreas.containsKey(name)) {
                String day = args[2].toUpperCase();  // Convert day to uppercase
                String time = args[3];
                String[] timeParts = time.split(":");
                int hour = Integer.parseInt(timeParts[0]);
                int minute = Integer.parseInt(timeParts[1]);
                KothArea area = kothAreas.get(name);
                try {
                  area.setSchedule(DayOfWeek.valueOf(day), hour, minute); // This will work with uppercase
                  kothManager.updateSchedule(area);
                  GamePlanning.planEveryWeek(this.plugin, DayOfWeek.valueOf(day), hour, minute, () -> kothManager.startGame(area), 5 * 60, integer -> {
                    player.sendMessage("§aLe KOTH " + name + " commence dans 5 minutes.");
                    if (integer == 60) {
                      player.sendMessage("§eLe KOTH " + name + " commence dans 1 minute.");
                    }
                    if (integer == 30) {
                      player.sendMessage("§eLe KOTH " + name + " commence dans 30 secondes.");
                    }
                    if (integer == 10) {
                      player.sendMessage("§eLe KOTH " + name + " commence dans 10 secondes.");
                    }

                  });
                  player.sendMessage("§aLe KOTH " + name + " a été planifié pour commencer chaque " + day + " à " + time);
                } catch (IllegalArgumentException e) {
                  player.sendMessage("§cJour invalide: " + args[2] + ". Veuillez utiliser un jour de la semaine valide, par exemple, Monday, Tuesday, etc.");
                }
              } else {
                player.sendMessage("§cAucun KOTH trouvé avec ce nom.");
              }
            } else {
              player.sendMessage("§eUtilisation: /koth schedule <name> <day> <HH:MM>");
            }
            return true;


          default:
            player.sendMessage("§eUtilisation: /koth <start|axe|create|list>");
            return true;
        }
      } else {
        player.sendMessage("§eUtilisation: /koth <start|axe|create|list>");
        return true;
      }
    }
    return false;
  }
}
