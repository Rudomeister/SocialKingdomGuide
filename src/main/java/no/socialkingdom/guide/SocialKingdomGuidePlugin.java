package no.socialkingdom.guide;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class SocialKingdomGuidePlugin extends JavaPlugin implements CommandExecutor, TabCompleter {
    private static final String ADMIN_PERMISSION = "socialkingdomguide.admin";

    private final Map<Integer, String> announcements = new LinkedHashMap<>();
    private BukkitTask announcementTask;
    private int intervalMinutes;
    private int nextAnnouncementIndex;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadGuideConfig();

        PluginCommand command = getCommand("socialkingdomguide");
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        }
    }

    @Override
    public void onDisable() {
        stopAnnouncementTask();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            send(sender, msg("no-permission"));
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        String action = args[0].toLowerCase(Locale.ROOT);
        switch (action) {
            case "add" -> handleAdd(sender, args);
            case "list" -> handleList(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "set" -> handleSet(sender, args);
            case "reload" -> handleReload(sender);
            default -> sendUsage(sender, label);
        }
        return true;
    }

    private void handleAdd(CommandSender sender, String[] args) {
        if (args.length < 3 || !args[1].equalsIgnoreCase("announce")) {
            send(sender, "&e/socialkingdomguide add announce <tekst>");
            return;
        }

        String message = stripWrappingQuotes(String.join(" ", Arrays.asList(args).subList(2, args.length)).trim());
        if (message.isBlank()) {
            send(sender, "&e/socialkingdomguide add announce <tekst>");
            return;
        }

        int id = nextId();
        announcements.put(id, message);
        saveAnnouncements();
        restartAnnouncementTask();
        send(sender, msg("added").replace("{id}", String.valueOf(id)));
    }

    private void handleList(CommandSender sender, String[] args) {
        if (args.length != 2 || !args[1].equalsIgnoreCase("announcement")) {
            send(sender, "&e/socialkingdomguide list announcement");
            return;
        }

        if (announcements.isEmpty()) {
            send(sender, msg("list-empty"));
            return;
        }

        send(sender, msg("list-header"));
        announcements.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> send(sender, msg("list-line")
                        .replace("{id}", String.valueOf(entry.getKey()))
                        .replace("{message}", entry.getValue())));
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length != 3 || !args[1].equalsIgnoreCase("announce")) {
            send(sender, "&e/socialkingdomguide remove announce <id>");
            return;
        }

        Integer id = parsePositiveInt(args[2]);
        if (id == null) {
            send(sender, msg("invalid-number"));
            return;
        }

        if (announcements.remove(id) == null) {
            send(sender, msg("missing-announcement").replace("{id}", String.valueOf(id)));
            return;
        }

        saveAnnouncements();
        restartAnnouncementTask();
        send(sender, msg("removed").replace("{id}", String.valueOf(id)));
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length != 2) {
            send(sender, "&e/socialkingdomguide set <minutter>");
            return;
        }

        Integer minutes = parsePositiveInt(args[1]);
        if (minutes == null) {
            send(sender, msg("invalid-number"));
            return;
        }

        intervalMinutes = minutes;
        getConfig().set("interval-minutes", intervalMinutes);
        saveConfig();
        restartAnnouncementTask();
        send(sender, msg("interval-set").replace("{interval}", String.valueOf(intervalMinutes)));
    }

    private void handleReload(CommandSender sender) {
        reloadConfig();
        loadGuideConfig();
        send(sender, msg("reloaded")
                .replace("{count}", String.valueOf(announcements.size()))
                .replace("{interval}", String.valueOf(intervalMinutes)));
    }

    private void loadGuideConfig() {
        intervalMinutes = Math.max(1, getConfig().getInt("interval-minutes", 15));
        announcements.clear();
        nextAnnouncementIndex = 0;

        List<Map<?, ?>> list = getConfig().getMapList("announcements");
        for (Map<?, ?> raw : list) {
            Object idValue = raw.get("id");
            Object messageValue = raw.get("message");
            if (!(idValue instanceof Number number) || messageValue == null) {
                continue;
            }

            int id = number.intValue();
            String message = String.valueOf(messageValue);
            if (id > 0 && !message.isBlank()) {
                announcements.put(id, message);
            }
        }

        restartAnnouncementTask();
    }

    private void restartAnnouncementTask() {
        stopAnnouncementTask();
        if (announcements.isEmpty()) {
            return;
        }

        long intervalTicks = Math.max(1L, intervalMinutes) * 60L * 20L;
        announcementTask = Bukkit.getScheduler().runTaskTimer(this, this::broadcastNextAnnouncement, intervalTicks, intervalTicks);
    }

    private void stopAnnouncementTask() {
        if (announcementTask != null) {
            announcementTask.cancel();
            announcementTask = null;
        }
    }

    private void broadcastNextAnnouncement() {
        if (announcements.isEmpty()) {
            return;
        }

        List<Map.Entry<Integer, String>> ordered = announcements.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList();
        if (nextAnnouncementIndex >= ordered.size()) {
            nextAnnouncementIndex = 0;
        }

        String message = ordered.get(nextAnnouncementIndex).getValue();
        nextAnnouncementIndex++;
        String format = msg("announce-format");
        if (format.isBlank()) {
            format = "&8[&6Info&8] &f{message}";
        }

        String rendered = color(format.replace("{message}", message));
        if (!ChatColor.stripColor(rendered).isBlank()) {
            Bukkit.broadcastMessage(rendered);
        }
    }

    private void saveAnnouncements() {
        List<Map<String, Object>> list = announcements.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    Map<String, Object> values = new LinkedHashMap<>();
                    values.put("id", entry.getKey());
                    values.put("message", entry.getValue());
                    return values;
                })
                .collect(Collectors.toList());

        getConfig().set("announcements", list);
        saveConfig();
    }

    private int nextId() {
        return announcements.keySet().stream().max(Comparator.naturalOrder()).orElse(0) + 1;
    }

    private void sendUsage(CommandSender sender, String label) {
        for (String line : getConfig().getStringList("messages.usage")) {
            send(sender, line.replace("{label}", label));
        }
    }

    private void send(CommandSender sender, String message) {
        sender.sendMessage(color(msg("prefix") + message));
    }

    private String msg(String path) {
        return getConfig().getString("messages." + path, "");
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    private Integer parsePositiveInt(String raw) {
        try {
            int value = Integer.parseInt(raw);
            return value > 0 ? value : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String stripWrappingQuotes(String value) {
        if (value.length() >= 2 && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return matching(args[0], List.of("add", "list", "remove", "set", "reload"));
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))) {
            return matching(args[1], List.of("announce"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            return matching(args[1], List.of("announcement"));
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("remove") && args[1].equalsIgnoreCase("announce")) {
            return matching(args[2], announcements.keySet().stream().sorted().map(String::valueOf).toList());
        }
        return Collections.emptyList();
    }

    private List<String> matching(String token, List<String> values) {
        String lower = token.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(Objects::nonNull)
                .filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower))
                .collect(Collectors.toList());
    }
}
