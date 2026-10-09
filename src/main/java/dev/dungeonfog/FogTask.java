package dev.dungeonfog;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class FogTask extends BukkitRunnable {

    private final DungeonFog plugin;
    private final FogManager manager;
    private final Set<UUID> darkened = new HashSet<>();

    public FogTask(DungeonFog plugin, FogManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void forget(UUID id) { darkened.remove(id); }

    @Override
    public void run() {
        int maxDensity = plugin.getConfig().getInt("max-density", 120);
        int maxRadius = plugin.getConfig().getInt("max-radius", 24);

        for (Player p : plugin.getServer().getOnlinePlayers()) {
            Location loc = p.getLocation();
            boolean inDarkness = false;

            for (FogRegion r : manager.all()) {
                if (!r.contains(loc)) continue;
                spawnFog(p, r, Math.min(r.density, maxDensity), Math.min(r.radius, maxRadius));
                if (r.darkness) inDarkness = true;
            }

            if (inDarkness) {
                PotionEffect cur = p.getPotionEffect(PotionEffectType.DARKNESS);
                if (cur == null || cur.getDuration() < 40) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 120, 0, true, false, false));
                }
                darkened.add(p.getUniqueId());
            } else if (darkened.remove(p.getUniqueId())) {
                p.removePotionEffect(PotionEffectType.DARKNESS);
            }
        }
    }

    private void spawnFog(Player p, FogRegion r, int count, int radius) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        Location base = p.getLocation();
        Particle.DustOptions dust = new Particle.DustOptions(r.color, r.size);

        for (int i = 0; i < count; i++) {
            double x = clamp(base.getX() + rnd.nextDouble(-radius, radius), r.minX, r.maxX + 1);
            double y = clamp(base.getY() + rnd.nextDouble(-1.0, 3.5), r.minY, r.maxY + 1);
            double z = clamp(base.getZ() + rnd.nextDouble(-radius, radius), r.minZ, r.maxZ + 1);

            switch (r.type) {
                case DUST -> p.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0, dust);
                case CLOUD -> p.spawnParticle(Particle.CLOUD, x, y, z, 1, 0, 0, 0, 0);
                case ASH -> p.spawnParticle(Particle.WHITE_ASH, x, y, z, 1, 0, 0, 0, 0);
                case SMOKE -> p.spawnParticle(Particle.LARGE_SMOKE, x, y, z, 1, 0, 0, 0, 0);
            }
        }
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
