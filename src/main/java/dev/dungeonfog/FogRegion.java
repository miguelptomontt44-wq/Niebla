package dev.dungeonfog;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;

public final class FogRegion {

    public enum Type { DUST, CLOUD, ASH, SMOKE }

    public final String name;
    public String world;
    public int minX, minY, minZ, maxX, maxY, maxZ;

    public Color color = Color.fromRGB(0xC8CDD2);
    public int density = 15;     // particulas por ciclo por jugador
    public float size = 3.0f;    // tamano (solo DUST)
    public Type type = Type.DUST;
    public int radius = 10;      // radio alrededor del jugador donde aparece la niebla
    public boolean darkness = false;

    public FogRegion(String name) {
        this.name = name;
    }

    public boolean contains(Location l) {
        if (l.getWorld() == null || !l.getWorld().getName().equals(world)) return false;
        return l.getX() >= minX && l.getX() < maxX + 1
                && l.getY() >= minY && l.getY() < maxY + 1
                && l.getZ() >= minZ && l.getZ() < maxZ + 1;
    }

    public static Color parseColor(String s) {
        s = s.trim();
        if (s.equalsIgnoreCase("fog") || s.equalsIgnoreCase("niebla")) return Color.fromRGB(0xC8CDD2);
        if (s.startsWith("#")) {
            return Color.fromRGB(Integer.parseInt(s.substring(1), 16));
        }
        return DyeColor.valueOf(s.toUpperCase()).getColor();
    }

    public static String hex(Color c) {
        return String.format("#%06X", c.asRGB());
    }
}
