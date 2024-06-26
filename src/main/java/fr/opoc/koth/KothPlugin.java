package fr.opoc.koth;

import fr.opoc.koth.command.KothCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class KothPlugin extends JavaPlugin {
  private KothManager kothManager;
  private Map<String, KothArea> kothAreas;

  @Override
  public void onEnable() {
    getLogger().info("KOTH Plugin Enabled!");
    kothManager = new KothManager(this);
    kothAreas = new HashMap<>();
    getServer().getPluginManager().registerEvents(new KothListener(kothManager), this);
    getCommand("koth").setExecutor(new KothCommand(kothManager, kothAreas));
  }

  @Override
  public void onDisable() {
    getLogger().info("KOTH Plugin Disabled!");
  }
}