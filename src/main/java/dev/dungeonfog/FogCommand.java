package dev.dungeonfog;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public final class FogCommand implements TabExecutor {

    private static final List<String> SUBS = List.of("help", "pos1", "pos2", "create", "delete", "list", "info", "set", "reload");
    private static final List<String> PROPS = List.of("color", "density", "size", "type", "radius", "darkness");

    private final DungeonFog plugin;
    private final FogManager m;

    public FogCommand(DungeonFog plugin, FogManager m) {
        this.plugin = plugin;
        this.m = m;
    }

    private void msg(CommandSender s, String t) {
        s.sendMessage(Component.text("[Fog] ", NamedTextColor.GRAY).append(Component.text(t, NamedTextColor.WHITE)));
    }

    private void err(CommandSender s, String t) {
        s.sendMessage(Component.text("[Fog] ", NamedTextColor.GRAY).append(Component.text(t, NamedTextColor.RED)));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] a) {
        if (!sender.hasPermission("dungeonfog.admin")) {
            err(sender, "No tienes permiso.");
            return true;
        }
        if (a.length == 0 || a[0].equalsIgnoreCase("help")) {
            help(sender);
            return true;
        }

        switch (a[0].toLowerCase()) {
            case "pos1", "pos2" -> {
                if (!(sender instanceof Player p)) { err(sender, "Solo jugadores."); return true; }
                Location l = p.getLocation().getBlock().getLocation();
                if (a[0].equalsIgnoreCase("pos1")) m.pos1.put(p.getUniqueId(), l); else m.pos2.put(p.getUniqueId(), l);
                msg(sender, a[0] + " = " + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ());
            }
            case "create" -> {
                if (!(sender instanceof Player p)) { err(sender, "Solo jugadores."); return true; }
                if (a.length < 2) { err(sender, "Uso: /fog create <nombre>"); return true; }
                String name = a[1].toLowerCase();
                if (!name.matches("[a-z0-9_-]+")) { err(sender, "Nombre invalido (a-z, 0-9, _ y -)."); return true; }
                if (m.get(name) != null) { err(sender, "Ya existe esa zona."); return true; }
                Location l1 = m.pos1.get(p.getUniqueId()), l2 = m.pos2.get(p.getUniqueId());
                if (l1 == null || l2 == null) { err(sender, "Primero define /fog pos1 y /fog pos2."); return true; }
                if (!l1.getWorld().equals(l2.getWorld())) { err(sender, "pos1 y pos2 estan en mundos distintos."); return true; }
                FogRegion r = new FogRegion(name);
                r.world = l1.getWorld().getName();
                r.minX = Math.min(l1.getBlockX(), l2.getBlockX()); r.maxX = Math.max(l1.getBlockX(), l2.getBlockX());
                r.minY = Math.min(l1.getBlockY(), l2.getBlockY()); r.maxY = Math.max(l1.getBlockY(), l2.getBlockY());
                r.minZ = Math.min(l1.getBlockZ(), l2.getBlockZ()); r.maxZ = Math.max(l1.getBlockZ(), l2.getBlockZ());
                m.add(r);
                msg(sender, "Zona '" + name + "' creada. Ajustala con /fog set " + name + " <color|density|size|type|radius|darkness> <valor>");
            }
            case "delete" -> {
                if (a.length < 2) { err(sender, "Uso: /fog delete <nombre>"); return true; }
                if (m.remove(a[1])) msg(sender, "Zona eliminada."); else err(sender, "No existe esa zona.");
            }
            case "list" -> {
                if (m.all().isEmpty()) { msg(sender, "No hay zonas."); return true; }
                msg(sender, "Zonas: " + m.all().stream().map(r -> r.name).collect(Collectors.joining(", ")));
            }
            case "info" -> {
                if (a.length < 2) { err(sender, "Uso: /fog info <nombre>"); return true; }
                FogRegion r = m.get(a[1]);
                if (r == null) { err(sender, "No existe esa zona."); return true; }
                msg(sender, r.name + " [" + r.world + "] (" + r.minX + "," + r.minY + "," + r.minZ + ") -> (" + r.maxX + "," + r.maxY + "," + r.maxZ + ")");
                msg(sender, "color=" + FogRegion.hex(r.color) + " density=" + r.density + " size=" + r.size
                        + " type=" + r.type + " radius=" + r.radius + " darkness=" + r.darkness);
            }
            case "set" -> {
                if (a.length < 4) { err(sender, "Uso: /fog set <nombre> <propiedad> <valor>"); return true; }
                FogRegion r = m.get(a[1]);
                if (r == null) { err(sender, "No existe esa zona."); return true; }
                try {
                    switch (a[2].toLowerCase()) {
                        case "color" -> r.color = FogRegion.parseColor(a[3]);
                        case "density" -> r.density = Math.max(0, Integer.parseInt(a[3]));
                        case "size" -> r.size = Math.max(0.1f, Math.min(4f, Float.parseFloat(a[3])));
                        case "type" -> r.type = FogRegion.Type.valueOf(a[3].toUpperCase());
                        case "radius" -> r.radius = Math.max(1, Integer.parseInt(a[3]));
                        case "darkness" -> r.darkness = Boolean.parseBoolean(a[3]);
                        default -> { err(sender, "Propiedad desconocida. Usa: " + String.join(", ", PROPS)); return true; }
                    }
                } catch (IllegalArgumentException ex) {
                    err(sender, "Valor invalido para " + a[2] + ".");
                    return true;
                }
                m.save();
                msg(sender, "Actualizado " + a[2] + " de '" + r.name + "'.");
            }
            case "reload" -> {
                plugin.reloadConfig();
                m.load();
                plugin.startTask();
                msg(sender, "Recargado (" + m.all().size() + " zonas).");
            }
            default -> help(sender);
        }
        return true;
    }

    private void help(CommandSender s) {
        msg(s, "/fog pos1 | pos2  - define las esquinas de la zona (donde estas parado)");
        msg(s, "/fog create <nombre>  - crea la zona entre pos1 y pos2");
        msg(s, "/fog set <nombre> color <white|light_gray|gray|black|... | #RRGGBB | fog>");
        msg(s, "/fog set <nombre> density <n>  - particulas por ciclo (menos = mas sutil)");
        msg(s, "/fog set <nombre> size <0.1-4>  - tamano de la particula (DUST)");
        msg(s, "/fog set <nombre> type <dust|cloud|ash|smoke>");
        msg(s, "/fog set <nombre> radius <n>  - radio de niebla alrededor del jugador");
        msg(s, "/fog set <nombre> darkness <true|false>  - efecto Oscuridad extra");
        msg(s, "/fog list | info <nombre> | delete <nombre> | reload");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] a) {
        if (!sender.hasPermission("dungeonfog.admin")) return List.of();
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            out.addAll(SUBS);
        } else if (a.length == 2 && List.of("delete", "info", "set").contains(a[0].toLowerCase())) {
            m.all().forEach(r -> out.add(r.name));
        } else if (a.length == 3 && a[0].equalsIgnoreCase("set")) {
            out.addAll(PROPS);
        } else if (a.length == 4 && a[0].equalsIgnoreCase("set")) {
            switch (a[2].toLowerCase()) {
                case "color" -> {
                    out.add("fog");
                    for (DyeColor d : DyeColor.values()) out.add(d.name().toLowerCase());
                }
                case "type" -> Arrays.stream(FogRegion.Type.values()).forEach(t -> out.add(t.name().toLowerCase()));
                case "darkness" -> { out.add("true"); out.add("false"); }
                case "size" -> out.addAll(List.of("2", "3", "4"));
                case "density" -> out.addAll(List.of("5", "10", "20", "40"));
                case "radius" -> out.addAll(List.of("6", "10", "16"));
            }
        }
        String last = a[a.length - 1].toLowerCase();
        return out.stream().filter(s -> s.toLowerCase().startsWith(last)).collect(Collectors.toList());
    }
}
