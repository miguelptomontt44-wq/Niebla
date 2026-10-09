package dev.dungeonfog;

import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class DungeonFog extends JavaPlugin implements Listener {

    private FogManager manager;
    private FogTask task;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new FogManager(this);
        manager.load();

        FogCommand cmd = new FogCommand(this, manager);
        PluginCommand pc = getCommand("fog");
        if (pc != null) {
            pc.setExecutor(cmd);
            pc.setTabCompleter(cmd);
        }

        getServer().getPluginManager().registerEvents(this, this);
        startTask();
        getLogger().info("DungeonFog activado con " + manager.all().size() + " zonas.");
    }

    @Override
    public void onDisable() {
        if (task != null) task.cancel();
    }

    public void startTask() {
        if (task != null) task.cancel();
        long interval = Math.max(2L, getConfig().getLong("interval-ticks", 10L));
        task = new FogTask(this, manager);
        task.runTaskTimer(this, 20L, interval);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (task != null) task.forget(e.getPlayer().getUniqueId());
    }
}
