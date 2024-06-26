package fr.opoc.koth;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.entity.Player;

public class KothListener implements Listener {
  private final KothManager kothManager;

  public KothListener(KothManager kothManager) {
    this.kothManager = kothManager;
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    if (player.getInventory().getItemInMainHand().getType() == Material.IRON_AXE) {
      if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
        Location pos1 = event.getClickedBlock().getLocation();
        kothManager.setPos1(pos1);
        player.sendMessage("Position 1 set to: " + pos1.toString());
      } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
        Location pos2 = event.getClickedBlock().getLocation();
        kothManager.setPos2(pos2);
        player.sendMessage("Position 2 set to: " + pos2.toString());
        player.sendMessage("Both positions set! Use /koth create [name] to create the KOTH area.");
      }
    }
  }
}