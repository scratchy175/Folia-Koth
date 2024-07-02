package fr.opoc.koth;

import fr.opoc.koth.command.KothCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;


import java.io.File;
import java.io.IOException;


public class KothPlugin extends JavaPlugin {
  private KothManager kothManager;

  private File configFile;
  private FileConfiguration kothConfig;



  @Override
  public void onEnable() {
    getLogger().info("KOTH Plugin Enabled!");
    kothManager = new KothManager(this);
    createConfig();
    getServer().getPluginManager().registerEvents(new KothListener(kothManager), this);
    getCommand("koth").setExecutor(new KothCommand(this));
  }

  @Override
  public void onDisable() {
    getLogger().info("KOTH Plugin Disabled!");
  }


  private void createConfig() {
    configFile = new File(getDataFolder(), "koth.yml");
    if (!configFile.exists()) {
      configFile.getParentFile().mkdirs();
      try {
        configFile.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    kothConfig = YamlConfiguration.loadConfiguration(configFile);
    kothManager.loadKothAreas(); // Calling the load method right after configuration is initialized
  }

  public void saveConfig(FileConfiguration config) {
    try {
      config.save(configFile);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public FileConfiguration getKothConfig() {
    return kothConfig;
  }

  public KothManager getKothManager() {
    return kothManager;
  }



}
