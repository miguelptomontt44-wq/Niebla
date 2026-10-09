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
    private final Set<UUID> densed = new HashSet<>();

    public FogTask(DungeonFog plugin, FogManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void forget(UUID id) {
        darkened.remove(id);
        densed.remove(id);
    }

    /** Quita los efectos que puso el plugin (al apagar/recargar). */
    public void clearAll() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (darkened.remove(p.getUniqueId())) p.removePotionEffect(PotionEffectType.DARKNESS);
            if (densed.remove(p.getUniqueId())) {
                p.removePotionEffect(PotionEffectType.BLINDNESS);
                p.removePotionEffect(PotionEffectType.SPEED);
            }
        }
    }

    @Override
    public void run() {
        int maxDensity = plugin.getConfig().getInt("max-density", 120);
        int maxRadius = plugin.getConfig().getInt("max-radius", 24);
        boolean speedBoost = plugin.getConfig().getBoolean("dense-speed-boost", true);

        for (Player p : plugin.getServer().getOnlinePlayers()) {
            Location loc = p.getLocation();
            boolean inDarkness = false;
            boolean inDense = false;

            for (FogRegion r : manager.all()) {
                if (!r.contains(loc)) continue;
                spawnFog(p, r, Math.min(r.density, maxDensity), Math.min(r.radius, maxRadius));
                if (r.darkness) inDarkness = true;
                if (r.dense) inDense = true;
            }

            // Oscuridad (efecto del warden): su parpadeo lo hace el cliente, no se puede quitar desde el servidor
            if (inDarkness) {
                PotionEffect cur = p.getPotionEffect(PotionEffectType.DARKNESS);
                if (cur == null || cur.getDuration() < 40) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 120, 0, true, false, false));
                }
                darkened.add(p.getUniqueId());
            } else if (darkened.remove(p.getUniqueId())) {
                p.removePotionEffect(PotionEffectType.DARKNESS);
            }

            // Niebla espesa permanente: Ceguera constante (sin pulso). Se renueva antes de que empiece a desvanecerse.
            if (inDense) {
                PotionEffect cur = p.getPotionEffect(PotionEffectType.BLINDNESS);
                if (cur == null || cur.getDuration() < 70) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 140, 0, true, false, false));
                }
                if (speedBoost) {
                    PotionEffect sp = p.getPotionEffect(PotionEffectType.SPEED);
                    if (sp == null || (sp.getDuration() < 70 && sp.isAmbient())) {
                        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 140, 0, true, false, false));
                    }
                }
                densed.add(p.getUniqueId());
            } else if (densed.remove(p.getUniqueId())) {
                p.removePotionEffect(PotionEffectType.BLINDNESS);
                if (speedBoost) p.removePotionEffect(PotionEffectType.SPEED);
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
