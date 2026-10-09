package dev.dungeonfog;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class FogManager {

    private final DungeonFog plugin;
    private final File file;
    private final Map<String, FogRegion> regions = new LinkedHashMap<>();
    public final Map<UUID, Location> pos1 = new HashMap<>();
    public final Map<UUID, Location> pos2 = new HashMap<>();

    public FogManager(DungeonFog plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "regions.yml");
    }

    public Collection<FogRegion> all() { return regions.values(); }
    public FogRegion get(String name) { return regions.get(name.toLowerCase()); }
    public void add(FogRegion r) { regions.put(r.name, r); save(); }
    public boolean remove(String name) {
        boolean ok = regions.remove(name.toLowerCase()) != null;
        if (ok) save();
        return ok;
    }

    public void load() {
        regions.clear();
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = yml.getConfigurationSection("regions");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(key);
            if (s == null) continue;
            try {
                FogRegion r = new FogRegion(key);
                r.world = s.getString("world");
                r.minX = s.getInt("min.x"); r.minY = s.getInt("min.y"); r.minZ = s.getInt("min.z");
                r.maxX = s.getInt("max.x"); r.maxY = s.getInt("max.y"); r.maxZ = s.getInt("max.z");
                r.color = FogRegion.parseColor(s.getString("color", "#C8CDD2"));
                r.density = s.getInt("density", 15);
                r.size = (float) s.getDouble("size", 3.0);
                r.type = FogRegion.Type.valueOf(s.getString("type", "DUST").toUpperCase());
                r.radius = s.getInt("radius", 10);
                r.darkness = s.getBoolean("darkness", false);
                regions.put(key, r);
            } catch (Exception e) {
                plugin.getLogger().warning("No se pudo cargar la region '" + key + "': " + e.getMessage());
            }
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (FogRegion r : regions.values()) {
            String p = "regions." + r.name + ".";
            yml.set(p + "world", r.world);
            yml.set(p + "min.x", r.minX); yml.set(p + "min.y", r.minY); yml.set(p + "min.z", r.minZ);
            yml.set(p + "max.x", r.maxX); yml.set(p + "max.y", r.maxY); yml.set(p + "max.z", r.maxZ);
            yml.set(p + "color", FogRegion.hex(r.color));
            yml.set(p + "density", r.density);
            yml.set(p + "size", r.size);
            yml.set(p + "type", r.type.name());
            yml.set(p + "radius", r.radius);
            yml.set(p + "darkness", r.darkness);
        }
        try {
            plugin.getDataFolder().mkdirs();
            yml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("No se pudo guardar regions.yml: " + e.getMessage());
        }
    }
}
