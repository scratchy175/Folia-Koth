package fr.opoc.koth;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.Objects;

public class KothListener implements Listener {
  private final KothManager kothManager;


  public KothListener(KothManager kothManager) {
    this.kothManager = kothManager;
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    ItemStack itemInHand = player.getInventory().getItemInMainHand();
    if (itemInHand.getType() == Material.IRON_AXE && itemInHand.hasItemMeta()) {
      ItemMeta meta = itemInHand.getItemMeta();
      if (meta != null && Component.text("§4La KOTHache").equals(meta.displayName()) && player.hasPermission("koth.admin")) {
        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
          event.setCancelled(true);
          Location pos1 = Objects.requireNonNull(event.getClickedBlock()).getLocation();
          kothManager.setPos1(pos1);
          player.sendMessage("La position 1 a été définie sur: " + pos1.getX() + ", " + pos1.getY() + ", " + pos1.getZ());
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
          event.setCancelled(true);
          Location pos2 = Objects.requireNonNull(event.getClickedBlock()).getLocation();
          kothManager.setPos2(pos2);
          player.sendMessage("La position 2 a été définie sur: " + pos2.getX() + ", " + pos2.getY() + ", " + pos2.getZ());
          player.sendMessage("Les deux positions sont définies! Utilisez /koth create [nom] pour créer le KOTH.");
        }
      }
    }
  }
}