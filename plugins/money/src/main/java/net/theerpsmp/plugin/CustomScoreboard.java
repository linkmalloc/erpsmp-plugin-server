package net.theerpsmp.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.block.SignChangeEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.entity.Arrow;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Block;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Particle;
import org.bukkit.event.block.Action;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display;
import org.bukkit.Color;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.bukkit.scheduler.BukkitRunnable;

public class CustomScoreboard extends JavaPlugin implements Listener, CommandExecutor {

    private final HashMap<UUID, Integer> hoursPlayedMap = new HashMap<>();
    private final HashMap<UUID, Long> erpiesMap = new HashMap<>();
    private final HashMap<UUID, Long> derpiesMap = new HashMap<>();
    private final HashMap<UUID, Integer> keysMap = new HashMap<>();
    private final HashMap<UUID, Integer> killsMap = new HashMap<>();
    private final HashMap<UUID, Integer> deathsMap = new HashMap<>();

    // Key tracking for /derpshop
    private final HashMap<UUID, Integer> regularKeysMap = new HashMap<>();
    private final HashMap<UUID, Integer> crimsonKeysMap = new HashMap<>();
    private final HashMap<UUID, Integer> echoKeysMap = new HashMap<>();

    // Auction variables
    private final List<AuctionListing> listings = new ArrayList<>();
    private final List<OrderRequest> orders = new ArrayList<>();
    private final HashMap<UUID, PendingSignInput> pendingSigns = new HashMap<>();
    private final HashMap<UUID, ItemStack> pendingListItems = new HashMap<>();
    private final HashMap<UUID, String> pendingOrderItemName = new HashMap<>(); // stores item name between ORDER_ITEM and ORDER_PRICE signs
    private boolean breakingCustom = false;

    // Combat tag system
    private final HashMap<UUID, Integer> combatTagTicks = new HashMap<>();

    // Clipboard system for /copy and /paste
    private final HashMap<UUID, List<CopiedBlock>> playerClipboards = new HashMap<>();

    public static class CopiedBlock {
        public final int offsetX;
        public final int offsetY;
        public final int offsetZ;
        public final Material material;
        public final org.bukkit.block.data.BlockData blockData;

        public CopiedBlock(int offsetX, int offsetY, int offsetZ, Material material, org.bukkit.block.data.BlockData blockData) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.material = material;
            this.blockData = blockData;
        }
    }

    // TPA tracking (requester UUID -> target UUID)
    private final HashMap<UUID, UUID> tpaRequests = new HashMap<>();

    // Homes & Settings
    private final HashMap<UUID, Location[]> playerHomes = new HashMap<>();
    private final HashMap<UUID, Boolean> chatSpamDisabled = new HashMap<>();
    private final HashMap<UUID, Boolean> tpaDisabled = new HashMap<>();
    private final List<UUID> xrayPlayers = new ArrayList<>();
    private final Location[] customSpawnPoints = new Location[5];

    // Shop Crates
    private final HashMap<Location, ShopCrateData> shopCrates = new HashMap<>();
    private final HashMap<UUID, Location> activeCrateSetup = new HashMap<>();
    private final HashMap<UUID, Location> activeCratePurchase = new HashMap<>();
    private final HashMap<UUID, ItemStack[]> pendingCrateItemsArray = new HashMap<>();

    private final Random random = new Random();

    public enum SignAction { SEARCH, LIST_PRICE, SET_CRATE_PRICE, ORDER_ITEM, ORDER_PRICE }

    public static class PendingSignInput {
        public final Location loc;
        public final org.bukkit.block.data.BlockData originalData;
        public final SignAction action;
        public final ItemStack item;

        public PendingSignInput(Location loc, org.bukkit.block.data.BlockData originalData, SignAction action, ItemStack item) {
            this.loc = loc;
            this.originalData = originalData;
            this.action = action;
            this.item = item;
        }
    }

    public static class ShopCrateData {
        public final Location loc;
        public final ItemStack[] items;
        public final long price;
        public final String priceType;
        public final UUID owner;
        public final String ownerName;
        public boolean active;
        public UUID hologramId;
        public String crateType = "normal";

        public ShopCrateData(Location loc, ItemStack[] items, long price, String priceType, UUID owner, String ownerName, boolean active) {
            this.loc = loc;
            this.items = items;
            this.price = price;
            this.priceType = priceType;
            this.owner = owner;
            this.ownerName = ownerName;
            this.active = active;
        }
    }

    public static class AuctionListing {
        public final UUID id;
        public final UUID seller;
        public final String sellerName;
        public final ItemStack item;
        public final long price;

        public AuctionListing(UUID seller, String sellerName, ItemStack item, long price) {
            this.id = UUID.randomUUID();
            this.seller = seller;
            this.sellerName = sellerName;
            this.item = item;
            this.price = price;
        }
    }

    public static class OrderRequest {
        public final UUID id;
        public final UUID buyer;
        public final String buyerName;
        public final String itemName;
        public final int quantity;
        public final long price;

        public OrderRequest(UUID buyer, String buyerName, String itemName, int quantity, long price) {
            this.id = UUID.randomUUID();
            this.buyer = buyer;
            this.buyerName = buyerName;
            this.itemName = itemName;
            this.quantity = quantity;
            this.price = price;
        }
    }


    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);

        // Ensure "spawn" world is loaded/created flat
        World spawnWorld = Bukkit.getWorld("spawn");
        if (spawnWorld == null) {
            WorldCreator creator = new WorldCreator("spawn");
            creator.type(WorldType.FLAT);
            creator.generateStructures(false);
            spawnWorld = Bukkit.createWorld(creator);
        }

        // Load custom spawnpoints from config
        for (int i = 0; i < 5; i++) {
            String spawnPath = "spawnpoints." + i;
            if (getConfig().contains(spawnPath)) {
                String worldName = getConfig().getString(spawnPath + ".world");
                double x = getConfig().getDouble(spawnPath + ".x");
                double y = getConfig().getDouble(spawnPath + ".y");
                double z = getConfig().getDouble(spawnPath + ".z");
                float pitch = (float) getConfig().getDouble(spawnPath + ".pitch");
                float yaw = (float) getConfig().getDouble(spawnPath + ".yaw");
                World w = Bukkit.getWorld(worldName);
                if (w != null) {
                    customSpawnPoints[i] = new Location(w, x, y, z, yaw, pitch);
                }
            }
        }
        
        if (getCommand("setspawn") != null) getCommand("setspawn").setExecutor(this);
        if (getCommand("sell") != null) getCommand("sell").setExecutor(this);
        if (getCommand("shop") != null) getCommand("shop").setExecutor(this);
        if (getCommand("derpshop") != null) getCommand("derpshop").setExecutor(this);
        if (getCommand("spawn") != null) getCommand("spawn").setExecutor(this);
        if (getCommand("auction") != null) getCommand("auction").setExecutor(this);
        if (getCommand("orders") != null) getCommand("orders").setExecutor(this);
        if (getCommand("bh") != null) getCommand("bh").setExecutor(this);
        if (getCommand("rtp") != null) getCommand("rtp").setExecutor(this);
        if (getCommand("tpa") != null) getCommand("tpa").setExecutor(this);
        if (getCommand("tpaccept") != null) getCommand("tpaccept").setExecutor(this);
        if (getCommand("pay") != null) getCommand("pay").setExecutor(this);
        if (getCommand("stash") != null) getCommand("stash").setExecutor(this);
        if (getCommand("erpies") != null) getCommand("erpies").setExecutor(this);
        if (getCommand("echokeys") != null) getCommand("echokeys").setExecutor(this);
        if (getCommand("crimsonkeys") != null) getCommand("crimsonkeys").setExecutor(this);
        if (getCommand("adminroom") != null) getCommand("adminroom").setExecutor(this);
        if (getCommand("erpitem") != null) getCommand("erpitem").setExecutor(this);
        if (getCommand("sethome") != null) getCommand("sethome").setExecutor(this);
        if (getCommand("home") != null) getCommand("home").setExecutor(this);
        if (getCommand("afk") != null) getCommand("afk").setExecutor(this);
        if (getCommand("setting") != null) getCommand("setting").setExecutor(this);
        if (getCommand("dupe") != null) getCommand("dupe").setExecutor(this);
        if (getCommand("viewhome") != null) getCommand("viewhome").setExecutor(this);
        if (getCommand("admin") != null) getCommand("admin").setExecutor(this);
        if (getCommand("rules") != null) getCommand("rules").setExecutor(this);
        if (getCommand("item") != null) getCommand("item").setExecutor(this);
        if (getCommand("gm") != null) getCommand("gm").setExecutor(this);
        if (getCommand("xray") != null) getCommand("xray").setExecutor(this);
        if (getCommand("unxray") != null) getCommand("unxray").setExecutor(this);
        if (getCommand("keys") != null) getCommand("keys").setExecutor(this);
        if (getCommand("copy") != null) getCommand("copy").setExecutor(this);
        if (getCommand("paste") != null) getCommand("paste").setExecutor(this);
        if (getCommand("dtp") != null) getCommand("dtp").setExecutor(this);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateScoreboard(player);
            }
            updateCustomCrateHolograms();

            // Combat tag countdown
            java.util.Iterator<java.util.Map.Entry<UUID, Integer>> iterator = combatTagTicks.entrySet().iterator();
            while (iterator.hasNext()) {
                java.util.Map.Entry<UUID, Integer> entry = iterator.next();
                UUID uuid = entry.getKey();
                int secondsLeft = entry.getValue() - 1;
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline() && secondsLeft > 0) {
                    entry.setValue(secondsLeft);
                    player.sendActionBar(Component.text("combat " + secondsLeft + "s", NamedTextColor.RED));
                } else {
                    if (player != null && player.isOnline()) {
                        player.sendActionBar(Component.text("Combat expired!", NamedTextColor.GREEN));
                    }
                    iterator.remove();
                }
            }
        }, 20L, 20L);

        // X-Ray periodic block updates task
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (UUID uuid : xrayPlayers) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) continue;
                Location loc = player.getLocation();
                World world = loc.getWorld();
                int px = loc.getBlockX();
                int py = loc.getBlockY();
                int pz = loc.getBlockZ();
                for (int dx = -10; dx <= 10; dx++) {
                    for (int dy = -10; dy <= 10; dy++) {
                        for (int dz = -10; dz <= 10; dz++) {
                            Block block = world.getBlockAt(px + dx, py + dy, pz + dz);
                            if (isCommonBlock(block.getType())) {
                                player.sendBlockChange(block.getLocation(), Material.BARRIER.createBlockData());
                            }
                        }
                    }
                }
            }
        }, 0L, 10L); // run every 10 ticks (0.5 seconds) for faster rendering

        // Hourly reward tracking tasks
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                hoursPlayedMap.put(uuid, hoursPlayedMap.getOrDefault(uuid, 0) + 1);
                keysMap.put(uuid, keysMap.getOrDefault(uuid, 0) + 1);
                if (!chatSpamDisabled.getOrDefault(uuid, false)) {
                    player.sendMessage(Component.text("🎉 You received 1 Hour Played and 1 Key reward!", NamedTextColor.GOLD));
                }
            }
        }, 72000L, 72000L);

        // AFK Zone minute reward tracking task
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().getName().equals("afk_zone")) {
                    UUID uuid = player.getUniqueId();
                    derpiesMap.put(uuid, derpiesMap.getOrDefault(uuid, 0L) + 1L);
                    if (!chatSpamDisabled.getOrDefault(uuid, false)) {
                        player.sendMessage(Component.text("🎁 You received 1 Derpy for being AFK!", NamedTextColor.LIGHT_PURPLE));
                    }
                }
            }
        }, 1200L, 1200L);
        loadShopCrates();
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            savePlayerData(player);
        }
        saveShopCrates();
    }

    private void loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        String path = "players." + uuid.toString() + ".";
        
        hoursPlayedMap.put(uuid, getConfig().getInt(path + "hoursPlayed", 0));
        erpiesMap.put(uuid, getConfig().getLong(path + "erpies", 0L));
        derpiesMap.put(uuid, getConfig().getLong(path + "derpies", 0L));
        keysMap.put(uuid, getConfig().getInt(path + "keys", 0));
        killsMap.put(uuid, getConfig().getInt(path + "kills", 0));
        deathsMap.put(uuid, getConfig().getInt(path + "deaths", 0));
        
        regularKeysMap.put(uuid, getConfig().getInt(path + "regularKeys", 0));
        crimsonKeysMap.put(uuid, getConfig().getInt(path + "crimsonKeys", 0));
        echoKeysMap.put(uuid, getConfig().getInt(path + "echoKeys", 0));

        chatSpamDisabled.put(uuid, getConfig().getBoolean(path + "chatSpamDisabled", false));
        tpaDisabled.put(uuid, getConfig().getBoolean(path + "tpaDisabled", false));

        Location[] homes = new Location[5];
        for (int i = 0; i < 5; i++) {
            String homePath = path + "homes." + i;
            if (getConfig().contains(homePath)) {
                String worldName = getConfig().getString(homePath + ".world");
                double x = getConfig().getDouble(homePath + ".x");
                double y = getConfig().getDouble(homePath + ".y");
                double z = getConfig().getDouble(homePath + ".z");
                float pitch = (float) getConfig().getDouble(homePath + ".pitch");
                float yaw = (float) getConfig().getDouble(homePath + ".yaw");
                World w = Bukkit.getWorld(worldName);
                if (w != null) {
                    homes[i] = new Location(w, x, y, z, yaw, pitch);
                }
            }
        }
        playerHomes.put(uuid, homes);
    }

    private void savePlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        String path = "players." + uuid.toString() + ".";
        
        getConfig().set(path + "hoursPlayed", hoursPlayedMap.getOrDefault(uuid, 0));
        getConfig().set(path + "erpies", erpiesMap.getOrDefault(uuid, 0L));
        getConfig().set(path + "derpies", derpiesMap.getOrDefault(uuid, 0L));
        getConfig().set(path + "keys", keysMap.getOrDefault(uuid, 0));
        getConfig().set(path + "kills", killsMap.getOrDefault(uuid, 0));
        getConfig().set(path + "deaths", deathsMap.getOrDefault(uuid, 0));
        
        getConfig().set(path + "regularKeys", regularKeysMap.getOrDefault(uuid, 0));
        getConfig().set(path + "crimsonKeys", crimsonKeysMap.getOrDefault(uuid, 0));
        getConfig().set(path + "echoKeys", echoKeysMap.getOrDefault(uuid, 0));

        getConfig().set(path + "chatSpamDisabled", chatSpamDisabled.getOrDefault(uuid, false));
        getConfig().set(path + "tpaDisabled", tpaDisabled.getOrDefault(uuid, false));

        Location[] homes = playerHomes.get(uuid);
        if (homes != null) {
            for (int i = 0; i < 5; i++) {
                String homePath = path + "homes." + i;
                if (homes[i] != null) {
                    getConfig().set(homePath + ".world", homes[i].getWorld().getName());
                    getConfig().set(homePath + ".x", homes[i].getX());
                    getConfig().set(homePath + ".y", homes[i].getY());
                    getConfig().set(homePath + ".z", homes[i].getZ());
                    getConfig().set(homePath + ".pitch", homes[i].getPitch());
                    getConfig().set(homePath + ".yaw", homes[i].getYaw());
                } else {
                    getConfig().set(homePath, null);
                }
            }
        }
        
        saveConfig();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        loadPlayerData(player);
        updateScoreboard(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        combatTagTicks.remove(uuid);

        savePlayerData(player);

        hoursPlayedMap.remove(uuid);
        erpiesMap.remove(uuid);
        derpiesMap.remove(uuid);
        keysMap.remove(uuid);
        killsMap.remove(uuid);
        deathsMap.remove(uuid);

        regularKeysMap.remove(uuid);
        crimsonKeysMap.remove(uuid);
        echoKeysMap.remove(uuid);

        PendingSignInput signPending = pendingSigns.remove(uuid);
        if (signPending != null) {
            signPending.loc.getBlock().setBlockData(signPending.originalData);
            if (signPending.item != null) {
                player.getInventory().addItem(signPending.item);
            }
        }
    }

    @EventHandler
    public void onPlayerKill(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        UUID victimUUID = victim.getUniqueId();
        deathsMap.put(victimUUID, deathsMap.getOrDefault(victimUUID, 0) + 1);

        Player killer = victim.getKiller();
        if (killer != null) {
            UUID killerUUID = killer.getUniqueId();
            killsMap.put(killerUUID, killsMap.getOrDefault(killerUUID, 0) + 1);

            ItemStack weapon = killer.getInventory().getItemInMainHand();
            if (weapon != null && weapon.hasItemMeta()) {
                String customItem = weapon.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING);
                if (customItem != null && customItem.equals("sword_derp")) {
                    event.deathMessage(Component.text(victim.getName() + " Just Got Derped", NamedTextColor.RED));
                    victim.sendMessage(Component.text("You Just Got Derped", NamedTextColor.RED));
                }
            }

            // Give killer the victim's player head
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwningPlayer(victim);
                skullMeta.displayName(Component.text(victim.getName() + "'s head", NamedTextColor.YELLOW));
                head.setItemMeta(skullMeta);
            }
            HashMap<Integer, ItemStack> remaining = killer.getInventory().addItem(head);
            for (ItemStack left : remaining.values()) {
                killer.getWorld().dropItemNaturally(killer.getLocation(), left);
            }
            killer.sendMessage(Component.text("\ud83d\udc80 You got " + victim.getName() + "'s head!", NamedTextColor.YELLOW));
        }

        combatTagTicks.remove(victimUUID);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this system command!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("sell")) {
            Inventory sellInv = Bukkit.createInventory(null, 27, Component.text("Drop items here to Sell"));
            player.openInventory(sellInv);
            return true;
        }

        if (command.getName().equalsIgnoreCase("shop")) {
            openMainMenu(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("derpshop")) {
            openDerpShop(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("spawn")) {
            Location spawnLoc = getRandomSpawnPoint();
            performTeleportCountdown(player, spawnLoc, "Spawn");
            return true;
        }

        if (command.getName().equalsIgnoreCase("auction")) {
            String query = args.length > 0 ? String.join(" ", args) : null;
            openAuctionGui(player, query);
            return true;
        }

        if (command.getName().equalsIgnoreCase("orders")) {
            String query = args.length > 0 ? String.join(" ", args) : null;
            openOrdersGui(player, query);
            return true;
        }

        if (command.getName().equalsIgnoreCase("player")) {
            String pName = player.getName();
            if (!pName.equals(".Redtoppat208") && !pName.equals(".Boreas4052")) {
                player.sendMessage(Component.text("❌ You do not have permission to use this command!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(Component.text("❌ Usage: /player (ban/unban/kick) (playername)", NamedTextColor.RED));
                return true;
            }
            String action = args[0].toLowerCase();
            String targetName = args[1];

            if (action.equals("ban")) {
                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(targetName, "Banned by administrator", (java.util.Date) null, null);
                Player target = Bukkit.getPlayer(targetName);
                if (target != null) {
                    target.kick(Component.text("You have been banned from the server."));
                }
                player.sendMessage(Component.text("✅ Banned player " + targetName, NamedTextColor.GREEN));
            } else if (action.equals("unban")) {
                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).pardon(targetName);
                player.sendMessage(Component.text("✅ Unbanned player " + targetName, NamedTextColor.GREEN));
            } else if (action.equals("kick")) {
                Player target = Bukkit.getPlayer(targetName);
                if (target != null) {
                    target.kick(Component.text("Kicked by administrator."));
                    player.sendMessage(Component.text("✅ Kicked player " + targetName, NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("❌ Player " + targetName + " is not online!", NamedTextColor.RED));
                }
            } else {
                player.sendMessage(Component.text("❌ Usage: /player (ban/unban/kick) (playername)", NamedTextColor.RED));
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("bh")) {
            openBountyHunter(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("rtp")) {
            if (args.length > 0) {
                String dim = args[0].toLowerCase();
                if (dim.equals("overworld")) {
                    performRtp(player, "overworld");
                    return true;
                } else if (dim.equals("nether") || dim.equals("nethher")) {
                    performRtp(player, "nether");
                    return true;
                } else if (dim.equals("end")) {
                    performRtp(player, "end");
                    return true;
                }
            }
            openRtpGui(player);
            return true;
        }

        // --- /tpa <player> ---
        if (command.getName().equalsIgnoreCase("tpa")) {
            if (args.length == 0) {
                player.sendMessage(Component.text("❌ Usage: /tpa <player>", NamedTextColor.RED));
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(Component.text("❌ Player not found!", NamedTextColor.RED));
                return true;
            }
            if (target.equals(player)) {
                player.sendMessage(Component.text("❌ You cannot teleport to yourself!", NamedTextColor.RED));
                return true;
            }
            if (tpaDisabled.getOrDefault(target.getUniqueId(), false)) {
                player.sendMessage(Component.text("❌ " + target.getName() + " has disabled TPA requests!", NamedTextColor.RED));
                return true;
            }
            tpaRequests.put(player.getUniqueId(), target.getUniqueId());
            player.sendMessage(Component.text("📨 Teleport request sent to " + target.getName() + "!", NamedTextColor.GREEN));
            target.sendMessage(Component.text("📨 " + player.getName() + " wants to teleport to you!", NamedTextColor.YELLOW)
                    .append(Component.text("\nType ", NamedTextColor.GOLD))
                    .append(Component.text("/tpaccept", NamedTextColor.GREEN))
                    .append(Component.text(" to accept!", NamedTextColor.GOLD)));
            return true;
        }

        // --- /tpaccept ---
        if (command.getName().equalsIgnoreCase("tpaccept")) {
            UUID accepterUUID = player.getUniqueId();
            UUID requesterUUID = null;
            for (var entry : tpaRequests.entrySet()) {
                if (entry.getValue().equals(accepterUUID)) {
                    requesterUUID = entry.getKey();
                    break;
                }
            }
            if (requesterUUID == null) {
                player.sendMessage(Component.text("❌ You have no pending teleport requests!", NamedTextColor.RED));
                return true;
            }
            Player requester = Bukkit.getPlayer(requesterUUID);
            if (requester == null) {
                tpaRequests.remove(requesterUUID);
                player.sendMessage(Component.text("❌ The requester is no longer online!", NamedTextColor.RED));
                return true;
            }
            tpaRequests.remove(requesterUUID);
            player.sendMessage(Component.text("✅ Teleport request accepted! " + requester.getName() + " will arrive in 5 seconds.", NamedTextColor.GREEN));
            requester.sendMessage(Component.text("✅ " + player.getName() + " accepted your request! Teleporting in 5 seconds...", NamedTextColor.GREEN));
            performTpaCountdown(requester, player);
            return true;
        }

        // --- /pay <amount> <player> ---
        if (command.getName().equalsIgnoreCase("pay")) {
            if (args.length < 2) {
                player.sendMessage(Component.text("❌ Usage: /pay <amount> <player>", NamedTextColor.RED));
                return true;
            }
            long amount;
            try {
                amount = parseAmountWithSuffix(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("❌ Invalid amount format! E.g. 500, 10k, 1.5m, 1b", NamedTextColor.RED));
                return true;
            }
            if (amount <= 0) {
                player.sendMessage(Component.text("❌ Amount must be greater than 0!", NamedTextColor.RED));
                return true;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(Component.text("❌ Player not found!", NamedTextColor.RED));
                return true;
            }
            if (target.equals(player)) {
                player.sendMessage(Component.text("❌ You cannot pay yourself!", NamedTextColor.RED));
                return true;
            }
            UUID uuid = player.getUniqueId();
            long balance = erpiesMap.getOrDefault(uuid, 0L);
            if (balance < amount) {
                player.sendMessage(Component.text("❌ You don't have enough Erpies! Balance: " + balance, NamedTextColor.RED));
                return true;
            }
            erpiesMap.put(uuid, balance - amount);
            erpiesMap.put(target.getUniqueId(), erpiesMap.getOrDefault(target.getUniqueId(), 0L) + amount);
            player.sendMessage(Component.text("💸 Paid " + amount + " Erpies to " + target.getName() + "!", NamedTextColor.GREEN));
            target.sendMessage(Component.text("💰 " + player.getName() + " paid you " + amount + " Erpies!", NamedTextColor.GREEN));
            return true;
        }

        // --- /stash (OP) ---
        if (command.getName().equalsIgnoreCase("stash")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }
            spawnStash(player);
            return true;
        }

        // --- /erpies <player> <amount> (OP) ---
        if (command.getName().equalsIgnoreCase("erpies")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(Component.text("❌ Usage: /erpies <player> <amount>", NamedTextColor.RED));
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(Component.text("❌ Player not found!", NamedTextColor.RED));
                return true;
            }
            long amount;
            try {
                amount = parseAmountWithSuffix(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("❌ Invalid amount format! E.g. 500, 10k, 1.5m, 1b", NamedTextColor.RED));
                return true;
            }
            UUID targetUUID = target.getUniqueId();
            erpiesMap.put(targetUUID, amount);
            player.sendMessage(Component.text("✅ Set " + target.getName() + "'s Erpies to " + amount, NamedTextColor.GREEN));
            return true;
        }

        // --- /echokeys <player> <reset|remove|add> <amount> (OP) ---
        if (command.getName().equalsIgnoreCase("echokeys")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 3) {
                player.sendMessage(Component.text("❌ Usage: /echokeys <player> <reset|remove|add> <amount>", NamedTextColor.RED));
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(Component.text("❌ Player not found!", NamedTextColor.RED));
                return true;
            }
            String action = args[1].toLowerCase();
            int amount;
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("❌ Invalid amount!", NamedTextColor.RED));
                return true;
            }
            UUID targetUUID = target.getUniqueId();
            int current = echoKeysMap.getOrDefault(targetUUID, 0);
            switch (action) {
                case "reset" -> {
                    echoKeysMap.put(targetUUID, amount);
                    player.sendMessage(Component.text("✅ Set " + target.getName() + "'s Echo keys to " + amount, NamedTextColor.GREEN));
                }
                case "remove" -> {
                    echoKeysMap.put(targetUUID, Math.max(0, current - amount));
                    player.sendMessage(Component.text("✅ Removed " + amount + " Echo keys from " + target.getName() + ". New balance: " + echoKeysMap.get(targetUUID), NamedTextColor.GREEN));
                }
                case "add" -> {
                    echoKeysMap.put(targetUUID, current + amount);
                    player.sendMessage(Component.text("✅ Added " + amount + " Echo keys to " + target.getName() + ". New balance: " + echoKeysMap.get(targetUUID), NamedTextColor.GREEN));
                }
                default -> player.sendMessage(Component.text("❌ Unknown action! Use: reset, remove, or add", NamedTextColor.RED));
            }
            return true;
        }

        // --- /crimsonkeys <player> <reset|remove|add> <amount> (OP) ---
        if (command.getName().equalsIgnoreCase("crimsonkeys")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 3) {
                player.sendMessage(Component.text("❌ Usage: /crimsonkeys <player> <reset|remove|add> <amount>", NamedTextColor.RED));
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(Component.text("❌ Player not found!", NamedTextColor.RED));
                return true;
            }
            String action = args[1].toLowerCase();
            int amount;
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("❌ Invalid amount!", NamedTextColor.RED));
                return true;
            }
            UUID targetUUID = target.getUniqueId();
            int current = crimsonKeysMap.getOrDefault(targetUUID, 0);
            switch (action) {
                case "reset" -> {
                    crimsonKeysMap.put(targetUUID, amount);
                    player.sendMessage(Component.text("✅ Set " + target.getName() + "'s crimson keys to " + amount, NamedTextColor.GREEN));
                }
                case "remove" -> {
                    crimsonKeysMap.put(targetUUID, Math.max(0, current - amount));
                    player.sendMessage(Component.text("✅ Removed " + amount + " crimson keys from " + target.getName() + ". New balance: " + crimsonKeysMap.get(targetUUID), NamedTextColor.GREEN));
                }
                case "add" -> {
                    crimsonKeysMap.put(targetUUID, current + amount);
                    player.sendMessage(Component.text("✅ Added " + amount + " crimson keys to " + target.getName() + ". New balance: " + crimsonKeysMap.get(targetUUID), NamedTextColor.GREEN));
                }
                default -> player.sendMessage(Component.text("❌ Unknown action! Use: reset, remove, or add", NamedTextColor.RED));
            }
            return true;
        }

        // --- /adminroom (OP) ---
        if (command.getName().equalsIgnoreCase("adminroom")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }
            teleportToAdminRoom(player);
            return true;
        }

        // --- /erpitem (OP Only) ---
        if (command.getName().equalsIgnoreCase("erpitem")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 1) {
                player.sendMessage(Component.text("❌ Usage: /erpitem <pickaxe|axe|bow|stick|crate|sword|pickaxe_lerp|mace|echo_sword|gateway|echo_crate|crimson_crate|key_crate> [player]", NamedTextColor.RED));
                return true;
            }

            Player targetPlayer = player;
            if (args.length >= 2) {
                targetPlayer = Bukkit.getPlayer(args[1]);
                if (targetPlayer == null) {
                    player.sendMessage(Component.text("❌ Player not found!", NamedTextColor.RED));
                    return true;
                }
            }

            String itemType = args[0].toLowerCase();
            ItemStack item = null;

            switch (itemType) {
                case "pickaxe" -> item = createEchoPickaxe();
                case "axe" -> item = createEchoAxe();
                case "bow" -> item = createEchoBow();
                case "stick" -> item = createKnockbackStick();
                case "crate" -> item = createShopCrate();
                case "sword" -> item = createSwordDerp();
                case "pickaxe_lerp" -> item = createPickaxeLerp();
                case "mace" -> item = createMaceMerp();
                case "echo_sword" -> item = createEchoSword();
                case "gateway" -> item = createEndGatewayItem();
                case "echo_crate" -> item = createEchoCrate();
                case "crimson_crate" -> item = createCrimsonCrate();
                case "key_crate" -> item = createKeyCrate();
                default -> {
                    player.sendMessage(Component.text("❌ Unknown item type! Use: pickaxe, axe, bow, stick, crate, sword, pickaxe_lerp, mace, echo_sword, gateway, echo_crate, crimson_crate, key_crate", NamedTextColor.RED));
                    return true;
                }
            }

            if (item != null) {
                targetPlayer.getInventory().addItem(item);
                player.sendMessage(Component.text("✅ Gave " + itemType + " to " + targetPlayer.getName(), NamedTextColor.GREEN));
            }
            return true;
        }

        // --- /sethome ---
        if (command.getName().equalsIgnoreCase("sethome")) {
            openSetHomeGui(player);
            return true;
        }

        // --- /home ---
        if (command.getName().equalsIgnoreCase("home")) {
            openHomeGui(player);
            return true;
        }

        // --- /afk ---
        if (command.getName().equalsIgnoreCase("afk")) {
            teleportToAfkZone(player);
            return true;
        }

        // --- /dupe @s <amount> ---
        if (command.getName().equalsIgnoreCase("dupe")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }
            int amount = 1;
            if (args.length >= 2) {
                try {
                    amount = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("❌ Invalid amount! Usage: /dupe @s <amount>", NamedTextColor.RED));
                    return true;
                }
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("@s")) {
                    amount = 1;
                } else {
                    try {
                        amount = Integer.parseInt(args[0]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(Component.text("❌ Invalid amount! Usage: /dupe <amount>", NamedTextColor.RED));
                        return true;
                    }
                }
            }

            if (amount <= 0) {
                player.sendMessage(Component.text("❌ Amount must be greater than 0!", NamedTextColor.RED));
                return true;
            }

            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType() == Material.AIR) {
                player.sendMessage(Component.text("❌ You must be holding an item to duplicate it!", NamedTextColor.RED));
                return true;
            }

            ItemStack dupe = hand.clone();
            dupe.setAmount(1);
            for (int i = 0; i < amount; i++) {
                HashMap<Integer, ItemStack> left = player.getInventory().addItem(dupe.clone());
                for (ItemStack remaining : left.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), remaining);
                }
            }

            player.sendMessage(Component.text("✨ Successfully duplicated your held item " + amount + " times!", NamedTextColor.GREEN));
            return true;
        }

        // --- /viewhome <playername> ---
        if (command.getName().equalsIgnoreCase("viewhome")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 1) {
                player.sendMessage(Component.text("❌ Usage: /viewhome <playername>", NamedTextColor.RED));
                return true;
            }

            String targetName = args[0];
            UUID targetUUID = null;
            Player targetPlayer = Bukkit.getPlayer(targetName);
            if (targetPlayer != null) {
                targetUUID = targetPlayer.getUniqueId();
                targetName = targetPlayer.getName();
            } else {
                org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
                if (op != null && (op.hasPlayedBefore() || op.getName() != null)) {
                    targetUUID = op.getUniqueId();
                    if (op.getName() != null) targetName = op.getName();
                }
            }

            if (targetUUID == null) {
                player.sendMessage(Component.text("❌ Player not found!", NamedTextColor.RED));
                return true;
            }

            Location[] homes = playerHomes.get(targetUUID);
            if (homes == null) {
                homes = new Location[5];
                String path = "players." + targetUUID.toString() + ".";
                for (int i = 0; i < 5; i++) {
                    String homePath = path + "homes." + i;
                    if (getConfig().contains(homePath)) {
                        String worldName = getConfig().getString(homePath + ".world");
                        double x = getConfig().getDouble(homePath + ".x");
                        double y = getConfig().getDouble(homePath + ".y");
                        double z = getConfig().getDouble(homePath + ".z");
                        float pitch = (float) getConfig().getDouble(homePath + ".pitch");
                        float yaw = (float) getConfig().getDouble(homePath + ".yaw");
                        World w = Bukkit.getWorld(worldName);
                        if (w != null) {
                            homes[i] = new Location(w, x, y, z, yaw, pitch);
                        }
                    }
                }
            }

            Inventory inv = Bukkit.createInventory(null, 27, Component.text(targetName + "'s Homes"));
            for (int i = 0; i < 5; i++) {
                Location loc = homes[i];
                int slot = 11 + i;
                if (loc != null) {
                    String locStr = String.format("%.0f, %.0f, %.0f (%s)", loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName());
                    inv.setItem(slot, createGuiItem(Material.GREEN_WOOL, "Home " + (i + 1), NamedTextColor.GREEN, "Location: " + locStr + " | Click to teleport there"));
                } else {
                    inv.setItem(slot, createGuiItem(Material.GRAY_WOOL, "Home " + (i + 1) + " Not Set", NamedTextColor.GRAY, "This home point has not been saved"));
                }
            }
            player.openInventory(inv);
            return true;
        }
        // --- /admin <add|remove> <player> (.RedToppat208 Only) ---
        if (command.getName().equalsIgnoreCase("admin")) {
            if (!player.getName().equalsIgnoreCase(".Redtoppat208") && !player.getName().equalsIgnoreCase(".RedToppat208")) {
                player.sendMessage(Component.text("❌ Only .RedToppat208 can use this command!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(Component.text("❌ Usage: /admin <add|remove> <playername>", NamedTextColor.RED));
                return true;
            }

            String action = args[0].toLowerCase();
            String targetName = args[1];

            org.bukkit.OfflinePlayer target = null;
            Player online = Bukkit.getPlayer(targetName);
            if (online != null) {
                target = online;
            } else {
                target = Bukkit.getOfflinePlayer(targetName);
            }

            if (target == null || (!target.hasPlayedBefore() && Bukkit.getPlayer(targetName) == null)) {
                player.sendMessage(Component.text("❌ Player not found!", NamedTextColor.RED));
                return true;
            }

            if (action.equals("add")) {
                target.setOp(true);
                player.sendMessage(Component.text("✅ Added operator status for " + target.getName(), NamedTextColor.GREEN));
                if (target.isOnline()) {
                    ((Player) target).sendMessage(Component.text("👑 You are now a server operator!", NamedTextColor.GOLD));
                }
            } else if (action.equals("remove")) {
                target.setOp(false);
                player.sendMessage(Component.text("❌ Removed operator status for " + target.getName(), NamedTextColor.RED));
                if (target.isOnline()) {
                    ((Player) target).sendMessage(Component.text("❌ Your operator status has been removed.", NamedTextColor.RED));
                }
            } else {
                player.sendMessage(Component.text("❌ Invalid action! Use 'add' or 'remove'.", NamedTextColor.RED));
            }
            return true;
        }
        // --- /setspawn <1-5> (.Redtoppat208 Only, in 'spawn' dimension) ---
        if (command.getName().equalsIgnoreCase("setspawn")) {
            if (!player.getName().equalsIgnoreCase(".Redtoppat208") && !player.getName().equalsIgnoreCase(".RedToppat208")) {
                player.sendMessage(Component.text("❌ Only .RedToppat208 can use this command!", NamedTextColor.RED));
                return true;
            }
            if (!player.getWorld().getName().equalsIgnoreCase("spawn")) {
                player.sendMessage(Component.text("❌ This command can only be used in the spawn dimension!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 1) {
                player.sendMessage(Component.text("❌ Usage: /setspawn <1-5>", NamedTextColor.RED));
                return true;
            }
            try {
                int index = Integer.parseInt(args[0]);
                if (index < 1 || index > 5) {
                    player.sendMessage(Component.text("❌ Invalid spawnpoint number! Use 1 to 5.", NamedTextColor.RED));
                    return true;
                }
                Location loc = player.getLocation();
                customSpawnPoints[index - 1] = loc;

                String spawnPath = "spawnpoints." + (index - 1);
                getConfig().set(spawnPath + ".world", loc.getWorld().getName());
                getConfig().set(spawnPath + ".x", loc.getX());
                getConfig().set(spawnPath + ".y", loc.getY());
                getConfig().set(spawnPath + ".z", loc.getZ());
                getConfig().set(spawnPath + ".pitch", (double) loc.getPitch());
                getConfig().set(spawnPath + ".yaw", (double) loc.getYaw());
                saveConfig();

                player.sendMessage(Component.text("✅ Spawnpoint " + index + " set to your current location!", NamedTextColor.GREEN));
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("❌ Invalid number! Usage: /setspawn <1-5>", NamedTextColor.RED));
            }
            return true;
        }
        // --- /copy <x1> <y1> <z1> <x2> <y2> <z2> (OP only) ---
        if (command.getName().equalsIgnoreCase("copy")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 6) {
                player.sendMessage(Component.text("❌ Usage: /copy <x1> <y1> <z1> <x2> <y2> <z2>", NamedTextColor.RED));
                return true;
            }
            try {
                Location ploc = player.getLocation();
                int x1 = (int) Math.round(parseCoordinate(args[0], ploc.getX()));
                int y1 = (int) Math.round(parseCoordinate(args[1], ploc.getY()));
                int z1 = (int) Math.round(parseCoordinate(args[2], ploc.getZ()));
                int x2 = (int) Math.round(parseCoordinate(args[3], ploc.getX()));
                int y2 = (int) Math.round(parseCoordinate(args[4], ploc.getY()));
                int z2 = (int) Math.round(parseCoordinate(args[5], ploc.getZ()));

                int minX = Math.min(x1, x2);
                int maxX = Math.max(x1, x2);
                int minY = Math.min(y1, y2);
                int maxY = Math.max(y1, y2);
                int minZ = Math.min(z1, z2);
                int maxZ = Math.max(z1, z2);

                int volume = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
                if (volume > 500000) {
                    player.sendMessage(Component.text("❌ Area is too large! Maximum volume is 500,000 blocks.", NamedTextColor.RED));
                    return true;
                }

                List<CopiedBlock> clipboard = new ArrayList<>();
                World world = player.getWorld();

                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            Block block = world.getBlockAt(x, y, z);
                            if (block.getType() != Material.AIR) {
                                clipboard.add(new CopiedBlock(x - x1, y - y1, z - z1, block.getType(), block.getBlockData().clone()));
                            }
                        }
                    }
                }

                playerClipboards.put(player.getUniqueId(), clipboard);
                player.sendMessage(Component.text("✅ Successfully copied " + volume + " blocks (" + clipboard.size() + " non-air) relative to point 1.", NamedTextColor.GREEN));
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("❌ Invalid coordinates! Use numbers or ~ for relative position.", NamedTextColor.RED));
            }
            return true;
        }

        // --- /paste (OP only) ---
        if (command.getName().equalsIgnoreCase("paste")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }
            List<CopiedBlock> clipboard = playerClipboards.get(player.getUniqueId());
            if (clipboard == null || clipboard.isEmpty()) {
                player.sendMessage(Component.text("❌ You don't have anything copied! Use /copy first.", NamedTextColor.RED));
                return true;
            }

            Location ploc = player.getLocation();
            int refX = ploc.getBlockX();
            int refY = ploc.getBlockY();
            int refZ = ploc.getBlockZ();
            World world = player.getWorld();

            for (CopiedBlock cb : clipboard) {
                int targetX = refX + cb.offsetX;
                int targetY = refY + cb.offsetY;
                int targetZ = refZ + cb.offsetZ;
                Block block = world.getBlockAt(targetX, targetY, targetZ);
                block.setType(cb.material, false);
                block.setBlockData(cb.blockData, false);
            }

            player.sendMessage(Component.text("✅ Successfully pasted " + clipboard.size() + " blocks relative to your position.", NamedTextColor.GREEN));
            return true;
        }

        // --- /dtp <dimension> (OP only) ---
        if (command.getName().equalsIgnoreCase("dtp")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 1) {
                player.sendMessage(Component.text("❌ Usage: /dtp <overworld|nether|end|afk|spawn>", NamedTextColor.RED));
                return true;
            }

            String dimName = args[0].toLowerCase();
            World targetWorld = null;
            String cleanName = "";

            switch (dimName) {
                case "overworld" -> {
                    targetWorld = Bukkit.getWorlds().get(0);
                    cleanName = "Overworld";
                }
                case "nether" -> {
                    targetWorld = Bukkit.getWorld("world_nether");
                    if (targetWorld == null) {
                        targetWorld = Bukkit.getWorlds().stream()
                                .filter(w -> w.getEnvironment() == World.Environment.NETHER)
                                .findFirst().orElse(null);
                    }
                    cleanName = "Nether";
                }
                case "end" -> {
                    targetWorld = Bukkit.getWorld("world_the_end");
                    if (targetWorld == null) {
                        targetWorld = Bukkit.getWorlds().stream()
                                .filter(w -> w.getEnvironment() == World.Environment.THE_END)
                                .findFirst().orElse(null);
                    }
                    cleanName = "End";
                }
                case "afk" -> {
                    targetWorld = Bukkit.getWorld("afk_zone");
                    cleanName = "AFK Zone";
                }
                case "spawn" -> {
                    targetWorld = Bukkit.getWorld("spawn");
                    cleanName = "Spawn";
                }
                default -> {
                    targetWorld = Bukkit.getWorld(args[0]);
                    cleanName = args[0];
                }
            }

            if (targetWorld == null) {
                player.sendMessage(Component.text("❌ Dimension not found: " + args[0], NamedTextColor.RED));
                return true;
            }

            Location dest = targetWorld.getSpawnLocation();
            player.teleport(dest);
            player.sendMessage(Component.text("🚀 Teleported to the " + cleanName + " dimension!", NamedTextColor.GREEN));
            return true;
        }

        // --- /rules ---
        if (command.getName().equalsIgnoreCase("rules")) {
            ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
            org.bukkit.inventory.meta.BookMeta meta = (org.bukkit.inventory.meta.BookMeta) book.getItemMeta();
            if (meta != null) {
                meta.title(Component.text("Server Rules", NamedTextColor.RED));
                meta.author(Component.text("Staff"));

                Component pageContent = Component.text("📜 ")
                    .append(Component.text("Server Rules", NamedTextColor.BLACK, net.kyori.adventure.text.format.TextDecoration.BOLD))
                    .append(Component.newline())
                    .append(Component.text("-------------------", NamedTextColor.BLACK))
                    .append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text("Rule 1: ", NamedTextColor.BLACK, net.kyori.adventure.text.format.TextDecoration.BOLD))
                    .append(Component.text("no xray", NamedTextColor.BLACK))
                    .append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text("Rule 2: ", NamedTextColor.BLACK, net.kyori.adventure.text.format.TextDecoration.BOLD))
                    .append(Component.text("no exploits and toolbox", NamedTextColor.BLACK))
                    .append(Component.newline())
                    .append(Component.newline())
                    .append(Component.text("Rule 3: ", NamedTextColor.BLACK, net.kyori.adventure.text.format.TextDecoration.BOLD))
                    .append(Component.text("no hacking", NamedTextColor.BLACK));

                meta.addPages(pageContent);
                book.setItemMeta(meta);
            }
            player.openBook(book);
            return true;
        }

        // --- /item <load|save> <name> (OP) ---
        if (command.getName().equalsIgnoreCase("item")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(Component.text("❌ Usage: /item <load|save> <name>", NamedTextColor.RED));
                return true;
            }
            String action = args[0].toLowerCase();
            String itemName = args[1].toLowerCase();

            if (action.equals("save")) {
                ItemStack item = player.getInventory().getItemInMainHand();
                if (item.getType() == Material.AIR) {
                    player.sendMessage(Component.text("❌ You must hold an item in your main hand to save it!", NamedTextColor.RED));
                    return true;
                }
                getConfig().set("saveditems." + itemName, item);
                saveConfig();
                player.sendMessage(Component.text("✅ Item saved as '" + itemName + "'!", NamedTextColor.GREEN));
            } else if (action.equals("load")) {
                if (!getConfig().contains("saveditems." + itemName)) {
                    player.sendMessage(Component.text("❌ No saved item found with the name '" + itemName + "'!", NamedTextColor.RED));
                    return true;
                }
                ItemStack item = getConfig().getItemStack("saveditems." + itemName);
                if (item != null) {
                    player.getInventory().addItem(item.clone());
                    player.sendMessage(Component.text("✅ Loaded item '" + itemName + "' into your inventory!", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("❌ Failed to load item!", NamedTextColor.RED));
                }
            } else {
                player.sendMessage(Component.text("❌ Unknown action! Use: load or save", NamedTextColor.RED));
            }
            return true;
        }

        // --- /gm ---
        if (command.getName().equalsIgnoreCase("gm")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 1) {
                player.sendMessage(Component.text("❌ Usage: /gm (s/c/a/sp)", NamedTextColor.RED));
                return true;
            }
            String mode = args[0].toLowerCase();
            GameMode gm;
            switch (mode) {
                case "s" -> gm = GameMode.SURVIVAL;
                case "c" -> gm = GameMode.CREATIVE;
                case "a" -> gm = GameMode.ADVENTURE;
                case "sp" -> gm = GameMode.SPECTATOR;
                default -> {
                    player.sendMessage(Component.text("❌ Unknown gamemode! Use: s, c, a, sp", NamedTextColor.RED));
                    return true;
                }
            }
            player.setGameMode(gm);
            player.sendMessage(Component.text("🎮 Gamemode set to " + gm.name().toLowerCase() + "!", NamedTextColor.GREEN));
            return true;
        }

        // --- /xray ---
        if (command.getName().equalsIgnoreCase("xray")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }
            if (xrayPlayers.contains(player.getUniqueId())) {
                player.sendMessage(Component.text("ℹ️ X-ray is already enabled!", NamedTextColor.YELLOW));
                return true;
            }
            xrayPlayers.add(player.getUniqueId());
            player.sendMessage(Component.text("👁️ X-ray enabled! Use /unxray to stop.", NamedTextColor.GREEN));
            return true;
        }

        // --- /unxray ---
        if (command.getName().equalsIgnoreCase("unxray")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }
            if (!xrayPlayers.contains(player.getUniqueId())) {
                player.sendMessage(Component.text("ℹ️ X-ray is not enabled!", NamedTextColor.YELLOW));
                return true;
            }
            xrayPlayers.remove(player.getUniqueId());
            restoreBlocksForPlayer(player);
            player.sendMessage(Component.text("👁️ X-ray disabled. Restoring blocks...", NamedTextColor.GREEN));
            return true;
        }

        // --- /keys (keys/echo/crimson) (add/remove/reset) (amount) (playername) ---
        if (command.getName().equalsIgnoreCase("keys")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 4) {
                player.sendMessage(Component.text("❌ Usage: /keys (keys/echo/crimson) (add/remove/reset) (amount) (playername)", NamedTextColor.RED));
                return true;
            }
            String keyType = args[0].toLowerCase();
            String action = args[1].toLowerCase();
            int amount;
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("❌ Invalid amount!", NamedTextColor.RED));
                return true;
            }
            Player target = Bukkit.getPlayer(args[3]);
            if (target == null) {
                player.sendMessage(Component.text("❌ Player not found!", NamedTextColor.RED));
                return true;
            }
            UUID targetUUID = target.getUniqueId();
            
            HashMap<UUID, Integer> targetMap;
            String keyLabel;
            if (keyType.equals("keys")) {
                targetMap = regularKeysMap;
                keyLabel = "Regular keys";
            } else if (keyType.equals("echo")) {
                targetMap = echoKeysMap;
                keyLabel = "Echo keys";
            } else if (keyType.equals("crimson")) {
                targetMap = crimsonKeysMap;
                keyLabel = "Crimson keys";
            } else {
                player.sendMessage(Component.text("❌ Invalid key type! Use: keys, echo, or crimson", NamedTextColor.RED));
                return true;
            }

            int current = targetMap.getOrDefault(targetUUID, 0);
            switch (action) {
                case "reset" -> {
                    targetMap.put(targetUUID, amount);
                    player.sendMessage(Component.text("✅ Set " + target.getName() + "'s " + keyLabel + " to " + amount, NamedTextColor.GREEN));
                }
                case "remove" -> {
                    targetMap.put(targetUUID, Math.max(0, current - amount));
                    player.sendMessage(Component.text("✅ Removed " + amount + " " + keyLabel + " from " + target.getName() + ". New balance: " + targetMap.get(targetUUID), NamedTextColor.GREEN));
                }
                case "add" -> {
                    targetMap.put(targetUUID, current + amount);
                    player.sendMessage(Component.text("✅ Added " + amount + " " + keyLabel + " to " + target.getName() + ". New balance: " + targetMap.get(targetUUID), NamedTextColor.GREEN));
                }
                default -> player.sendMessage(Component.text("❌ Unknown action! Use: reset, remove, or add", NamedTextColor.RED));
            }
            return true;
        }

        // --- /setting ---
        if (command.getName().equalsIgnoreCase("setting")) {
            openSettingsGui(player);
            return true;
        }

        return false;
    }

    // --- Spawn Protection & Cabin Generator ---
    private Location getSpawnLocation() {
        World world = Bukkit.getWorlds().get(0);
        return new Location(world, 0, 126, 0);
    }

    private Location getRandomSpawnPoint() {
        List<Location> activeSpawns = new ArrayList<>();
        for (Location loc : customSpawnPoints) {
            if (loc != null) activeSpawns.add(loc);
        }
        if (!activeSpawns.isEmpty()) {
            return activeSpawns.get(random.nextInt(activeSpawns.size()));
        }

        World world = Bukkit.getWorlds().get(0);
        // 4 spawnpoints arranged in a cross, each facing (0, 127, 0)
        // Pitch: atan(-1/9) ≈ -6.34° (looking slightly up toward y=127 from y=126 at distance 9)
        float pitch = (float) Math.toDegrees(Math.atan(-1.0 / 9.0));
        Location[] spots = new Location[] {
            new Location(world,  0.5, 126,  -9.5,   0f, pitch), // North face, facing south (+Z)
            new Location(world, -9.5, 126,   0.5, -90f, pitch), // West face, facing east (+X)
            new Location(world,  0.5, 126,   9.5, 180f, pitch), // South face, facing north (-Z)
            new Location(world,  9.5, 126,   0.5,  90f, pitch)  // East face, facing west (-X)
        };
        return spots[random.nextInt(spots.length)];
    }

    private boolean isInSpawnRadius(Location loc) {
        Location spawn = getSpawnLocation();
        if (!loc.getWorld().equals(spawn.getWorld())) return false;
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        return x >= -68 && x <= 50 &&
               y >= 78  && y <= 188 &&
               z >= -49 && z <= 63;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location loc = block.getLocation();

        if (shopCrates.containsKey(loc)) {
            ShopCrateData data = shopCrates.get(loc);
            if (player.getGameMode() == GameMode.SURVIVAL && !player.getUniqueId().equals(data.owner) && !player.isOp()) {
                event.setCancelled(true);
                player.sendMessage(Component.text("❌ Only the shop owner or an administrator can break this Shop Crate!", NamedTextColor.RED));
                return;
            }

            event.setCancelled(true);
            deleteCrateHologram(data);
            shopCrates.remove(loc);

            ItemStack crateToDrop = null;
            if (data.crateType.equals("echo")) {
                crateToDrop = createEchoCrate();
            } else if (data.crateType.equals("crimson")) {
                crateToDrop = createCrimsonCrate();
            } else if (data.crateType.equals("key")) {
                crateToDrop = createKeyCrate();
            } else {
                crateToDrop = createShopCrate();
            }
            loc.getWorld().dropItemNaturally(loc, crateToDrop);
            if (data.items != null) {
                for (ItemStack item : data.items) {
                    if (item != null && item.getType() != Material.AIR) {
                        loc.getWorld().dropItemNaturally(loc, item);
                    }
                }
            }

            block.setType(Material.AIR);
            player.sendMessage(Component.text("📦 Shop Crate removed successfully.", NamedTextColor.GREEN));
            return;
        }

        if (loc.getWorld().getName().equals("afk_zone") && !player.isOp()) {
            event.setCancelled(true);
            player.sendMessage(Component.text("❌ You cannot break blocks in the AFK zone!", NamedTextColor.RED));
            return;
        }

        if (loc.getWorld().getName().equalsIgnoreCase("spawn") && player.getGameMode() == GameMode.SURVIVAL) {
            event.setCancelled(true);
            player.sendMessage(Component.text("❌ You cannot break blocks in the Spawn world!", NamedTextColor.RED));
            return;
        }

        if (player.getGameMode() == GameMode.SURVIVAL) {
            if (isInSpawnRadius(loc)) {
                event.setCancelled(true);
                player.sendMessage(Component.text("❌ You cannot break blocks at spawn!", NamedTextColor.RED));
                return;
            }
        }

        if (breakingCustom) return;

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null || !tool.hasItemMeta()) return;

        ItemMeta meta = tool.getItemMeta();
        String customItem = meta.getPersistentDataContainer().get(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING);
        if (customItem == null) return;

        if (customItem.equals("echo_pickaxe")) {
            event.setCancelled(true);
            breakingCustom = true;
            try {
                handleEchoPickaxeBreak(player, event.getBlock(), tool);
            } finally {
                breakingCustom = false;
            }
        } else if (customItem.equals("echo_axe")) {
            Material type = event.getBlock().getType();
            String name = type.name();
            if (name.contains("LOG") || name.contains("WOOD")) {
                event.setCancelled(true);
                breakingCustom = true;
                try {
                    handleEchoAxeBreak(player, event.getBlock(), tool);
                } finally {
                    breakingCustom = false;
                }
            }
        } else if (customItem.equals("pickaxe_lerp")) {
            Material type = event.getBlock().getType();
            String name = type.name();
            if (name.contains("ORE")) {
                event.setCancelled(true);
                breakingCustom = true;
                try {
                    handlePickaxeLerpBreak(player, event.getBlock(), tool);
                } finally {
                    breakingCustom = false;
                }
            }
        }
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        ItemStack bow = event.getBow();
        if (bow == null || !bow.hasItemMeta()) return;

        ItemMeta meta = bow.getItemMeta();
        String customItem = meta.getPersistentDataContainer().get(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING);
        if (customItem != null && customItem.equals("echo_bow") && event.getProjectile() instanceof Arrow arrow) {
            arrow.getPersistentDataContainer().set(new NamespacedKey(this, "echo_arrow"), PersistentDataType.STRING, "true");
            
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!arrow.isValid() || arrow.isOnGround() || arrow.isDead()) {
                        cancel();
                        return;
                    }
                    arrow.getWorld().spawnParticle(Particle.SONIC_BOOM, arrow.getLocation(), 1, 0, 0, 0, 0);
                }
            }.runTaskTimer(this, 0L, 1L);
        }
    }

    @EventHandler
    public void onArrowHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Arrow arrow && event.getEntity() instanceof org.bukkit.entity.LivingEntity victim) {
            if (arrow.getPersistentDataContainer().has(new NamespacedKey(this, "echo_arrow"), PersistentDataType.STRING)) {
                victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
                victim.getWorld().spawnParticle(Particle.SONIC_BOOM, victim.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0);
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlock().getWorld().getName().equals("afk_zone") && !event.getPlayer().isOp()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("❌ You cannot place blocks in the AFK zone!", NamedTextColor.RED));
            return;
        }

        if (event.getBlock().getWorld().getName().equalsIgnoreCase("spawn") && event.getPlayer().getGameMode() == GameMode.SURVIVAL) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("❌ You cannot place blocks in the Spawn world!", NamedTextColor.RED));
            return;
        }

        if (event.getPlayer().getGameMode() == GameMode.SURVIVAL) {
            if (isInSpawnRadius(event.getBlock().getLocation())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Component.text("❌ You cannot place blocks at spawn!", NamedTextColor.RED));
                return;
            }
        }
        ItemStack item = event.getItemInHand();
        if (item != null && item.hasItemMeta()) {
            String customItem = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING);
            if (customItem != null) {
                if (customItem.equals("shop_crate") || customItem.equals("echo_crate") || customItem.equals("crimson_crate") || customItem.equals("key_crate")) {
                    Location loc = event.getBlock().getLocation();
                    ShopCrateData data = new ShopCrateData(
                        loc,
                        null,
                        0,
                        "",
                        event.getPlayer().getUniqueId(),
                        event.getPlayer().getName(),
                        false
                    );
                    if (customItem.equals("echo_crate")) {
                        data.crateType = "echo";
                        event.getPlayer().sendMessage(Component.text("📦 You placed an Echo Crate! Right-click it in Creative Mode to set it up.", NamedTextColor.YELLOW));
                    } else if (customItem.equals("crimson_crate")) {
                        data.crateType = "crimson";
                        event.getPlayer().sendMessage(Component.text("📦 You placed a Crimson Crate! Right-click it in Creative Mode to set it up.", NamedTextColor.YELLOW));
                    } else if (customItem.equals("key_crate")) {
                        data.crateType = "key";
                        event.getPlayer().sendMessage(Component.text("📦 You placed a Key Crate! Right-click it in Creative Mode to set it up.", NamedTextColor.YELLOW));
                    } else {
                        event.getPlayer().sendMessage(Component.text("📦 You placed a Shop Crate! Right-click it in Creative Mode to set it up.", NamedTextColor.YELLOW));
                    }
                    shopCrates.put(loc, data);
                } else if (customItem.equals("end_gateway")) {
                    Location loc = event.getBlock().getLocation();
                    Bukkit.getScheduler().runTask(this, () -> {
                        loc.getBlock().setType(Material.END_GATEWAY);
                    });
                    event.getPlayer().sendMessage(Component.text("🌌 You placed an End Gateway portal!", NamedTextColor.LIGHT_PURPLE));
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }

        if (attacker != null) {
            if (attacker.getGameMode() == GameMode.SURVIVAL) {
                if (attacker.getWorld().getName().equalsIgnoreCase("spawn")) {
                    event.setCancelled(true);
                    attacker.sendMessage(Component.text("❌ PvP and damage are disabled in the Spawn world!", NamedTextColor.RED));
                    return;
                }
                if (isInSpawnRadius(event.getEntity().getLocation())) {
                    event.setCancelled(true);
                    attacker.sendMessage(Component.text("❌ PvP and damage are disabled at spawn!", NamedTextColor.RED));
                    return;
                }
            }
        }

        if (event.getEntity() instanceof Player victim && attacker != null) {
            if (attacker.getWorld().getName().equals("afk_zone") && !attacker.isOp()) {
                event.setCancelled(true);
                attacker.sendMessage(Component.text("❌ PvP is disabled in the AFK zone!", NamedTextColor.RED));
                return;
            }

            ItemStack weapon = attacker.getInventory().getItemInMainHand();
            if (weapon != null && weapon.hasItemMeta()) {
                String customItem = weapon.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING);
                if (customItem != null && customItem.equals("echo_sword")) {
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
                    victim.getWorld().spawnParticle(Particle.SONIC_BOOM, victim.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0);
                }
            }

            if (!event.isCancelled() && !victim.equals(attacker)) {
                UUID victimUUID = victim.getUniqueId();
                UUID attackerUUID = attacker.getUniqueId();

                combatTagTicks.put(victimUUID, 20);
                combatTagTicks.put(attackerUUID, 20);

                victim.sendActionBar(Component.text("combat 20s", NamedTextColor.RED));
                attacker.sendActionBar(Component.text("combat 20s", NamedTextColor.RED));
            }
        }
    }

    // --- Sell GUI handler ---
    @EventHandler
    public void onSellClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals("Drop items here to Sell")) return;
        
        Player player = (Player) event.getPlayer();
        Inventory inv = event.getInventory();
        long totalPayout = 0L;

        for (ItemStack item : inv.getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;

            int valueMultiplier = item.getAmount();
            int baseValue = getItemRarityValue(item.getType());
            totalPayout += ((long) baseValue * valueMultiplier);
        }

        if (totalPayout > 0) {
            UUID uuid = player.getUniqueId();
            erpiesMap.put(uuid, erpiesMap.getOrDefault(uuid, 0L) + totalPayout);
            player.sendMessage(Component.text("💰 Items sold! Received: ", NamedTextColor.GREEN)
                    .append(Component.text(totalPayout + " Erpies", NamedTextColor.WHITE)));
        }
    }

    private int getItemRarityValue(Material material) {
        if (material == null) return 0;
        
        switch (material) {
            // Epic / Legendary (50,000)
            case ELYTRA:
            case DRAGON_EGG:
            case DRAGON_HEAD:
            case NETHER_STAR:
            case BEACON:
                return 50000;

            // Rare / Special (10,000)
            case TOTEM_OF_UNDYING:
            case HEART_OF_THE_SEA:
            case TRIDENT:
            case ENCHANTED_GOLDEN_APPLE:
            case NETHERITE_INGOT:
                return 10000;

            // Diamond / Emerald / Valuable items (1,000)
            case DIAMOND:
            case EMERALD:
            case NETHERITE_SCRAP:
            case ANCIENT_DEBRIS:
            case WITHER_SKELETON_SKULL:
            case SHULKER_SHELL:
            case SHULKER_BOX:
                return 1000;

            // Semi-precious / Mid-tier (100)
            case GOLD_INGOT:
            case IRON_INGOT:
            case LAPIS_LAZULI:
            case REDSTONE_BLOCK:
            case ENDER_PEARL:
            case OBSIDIAN:
            case BLAZE_ROD:
            case GHAST_TEAR:
            case SLIME_BALL:
            case SADDLE:
            case GOLDEN_APPLE:
                return 100;

            // Uncommon blocks/items (10)
            case COAL:
            case COPPER_INGOT:
            case REDSTONE:
            case QUARTZ:
            case NETHER_QUARTZ_ORE:
            case GLOWSTONE_DUST:
            case GUNPOWDER:
            case LEATHER:
            case BOOK:
            case IRON_ORE:
            case GOLD_ORE:
            case DIAMOND_ORE:
            case EMERALD_ORE:
            case COPPER_ORE:
            case LAPIS_ORE:
            case DEEPSLATE_IRON_ORE:
            case DEEPSLATE_GOLD_ORE:
            case DEEPSLATE_DIAMOND_ORE:
            case DEEPSLATE_EMERALD_ORE:
            case DEEPSLATE_LAPIS_ORE:
            case NETHER_GOLD_ORE:
                return 10;

            // Common blocks / junk (1)
            case DIRT:
            case COBBLESTONE:
            case STONE:
            case GRAVEL:
            case SAND:
            case NETHERRACK:
            case GRASS_BLOCK:
            case DEEPSLATE:
            case TUFF:
            case ANDESITE:
            case DIORITE:
            case GRANITE:
            case DIRT_PATH:
            case COARSE_DIRT:
            case MYCELIUM:
            case PODZOL:
            case OAK_LOG:
            case SPRUCE_LOG:
            case BIRCH_LOG:
            case JUNGLE_LOG:
            case ACACIA_LOG:
            case DARK_OAK_LOG:
            case MANGROVE_LOG:
            case CHERRY_LOG:
            case BAMBOO_BLOCK:
            case OAK_PLANKS:
            case SPRUCE_PLANKS:
            case BIRCH_PLANKS:
            case JUNGLE_PLANKS:
            case ACACIA_PLANKS:
            case DARK_OAK_PLANKS:
            case MANGROVE_PLANKS:
            case CHERRY_PLANKS:
            case BAMBOO_PLANKS:
            case WHEAT:
            case WHEAT_SEEDS:
            case ROTTEN_FLESH:
            case BONE:
            case STRING:
            case FEATHER:
            case EGG:
                return 1;
        }

        // Fallback checks by name pattern
        String name = material.name();
        if (name.contains("NETHERITE")) {
            return 10000;
        } else if (name.contains("DIAMOND") || name.contains("EMERALD") || name.contains("SHULKER")) {
            return 1000;
        } else if (name.contains("GOLD") || name.contains("IRON") || name.contains("LAPIS") || name.contains("ENDER") || name.contains("OBSIDIAN") || name.contains("SLIME")) {
            return 100;
        } else if (name.contains("COAL") || name.contains("COPPER") || name.contains("REDSTONE") || name.contains("QUARTZ") || name.contains("ORE") || name.contains("GLOWSTONE")) {
            return 10;
        }

        return 1;
    }

    // --- Shop System ---
    private void openMainMenu(Player player) {
        Inventory shop = Bukkit.createInventory(null, 27, Component.text("The Erp SMP - Shop"));

        shop.setItem(11, createGuiItem(Material.ENDER_EYE, "End", NamedTextColor.LIGHT_PURPLE, "Click to browse End category"));
        shop.setItem(13, createGuiItem(Material.DIAMOND_SWORD, "PVP", NamedTextColor.RED, "Click to browse Combat category"));
        shop.setItem(15, createGuiItem(Material.COOKED_BEEF, "Food", NamedTextColor.GREEN, "Click to browse Provisions category"));

        player.openInventory(shop);
    }

    private void openDerpShop(Player player) {
        Inventory shop = Bukkit.createInventory(null, 27, Component.text("Derp Shop - Keys"));
        UUID uuid = player.getUniqueId();

        int regCount = regularKeysMap.getOrDefault(uuid, 0);
        int crimCount = crimsonKeysMap.getOrDefault(uuid, 0);
        int echoCount = echoKeysMap.getOrDefault(uuid, 0);

        shop.setItem(11, createGuiItem(Material.TRIPWIRE_HOOK, "Regular Key", NamedTextColor.YELLOW, "Cost: 10 Derpies | Owned: " + regCount));
        shop.setItem(13, createGuiItem(Material.REDSTONE, "Crimson Key", NamedTextColor.RED, "Cost: 25 Derpies | Owned: " + crimCount));
        shop.setItem(15, createGuiItem(Material.ECHO_SHARD, "Echo Key", NamedTextColor.AQUA, "Cost: 50 Derpies | Owned: " + echoCount));

        player.openInventory(shop);
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.contains("Shop") && !title.contains("Auction") && !title.contains("Bounty")
                && !title.equals("Random Teleport") && !title.equals("List an Item")
                && !title.equals("Set Home Points") && !title.equals("Teleport Home") && !title.equals("Settings")
                && !title.equals("Setup Crate Shop") && !title.equals("Buy from Shop")
                && !title.equals("Order Board") && !title.equals("Order Board - Your Orders")
                && !title.endsWith("'s Homes")) return;

        // Do not cancel clicks in "List an Item" GUI or "Setup Crate Shop" (slot 4) because players need to place/take items.
        if (!title.equals("List an Item") && !title.equals("Setup Crate Shop")) {
            event.setCancelled(true);
        }

        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();

        if (title.equals("Setup Crate Shop")) {
            // Allow all clicks to place items in any of the 9 slots
            return;
        }

        if (title.endsWith("'s Homes")) {
            event.setCancelled(true);
            int rawSlot = event.getRawSlot();
            if (rawSlot >= 11 && rawSlot <= 15) {
                ItemStack item = event.getCurrentItem();
                if (item != null && item.getType() == Material.GREEN_WOOL) {
                    String targetName = title.substring(0, title.indexOf("'s Homes"));
                    UUID targetUUID = null;
                    Player targetPlayer = Bukkit.getPlayer(targetName);
                    if (targetPlayer != null) {
                        targetUUID = targetPlayer.getUniqueId();
                    } else {
                        org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(targetName);
                        if (op != null) {
                            targetUUID = op.getUniqueId();
                        }
                    }

                    if (targetUUID != null) {
                        Location[] homes = playerHomes.get(targetUUID);
                        if (homes == null) {
                            homes = new Location[5];
                            String path = "players." + targetUUID.toString() + ".";
                            int homeIdx = rawSlot - 11;
                            String homePath = path + "homes." + homeIdx;
                            if (getConfig().contains(homePath)) {
                                String worldName = getConfig().getString(homePath + ".world");
                                double x = getConfig().getDouble(homePath + ".x");
                                double y = getConfig().getDouble(homePath + ".y");
                                double z = getConfig().getDouble(homePath + ".z");
                                float pitch = (float) getConfig().getDouble(homePath + ".pitch");
                                float yaw = (float) getConfig().getDouble(homePath + ".yaw");
                                World w = Bukkit.getWorld(worldName);
                                if (w != null) {
                                    Location dest = new Location(w, x, y, z, yaw, pitch);
                                    player.closeInventory();
                                    player.teleport(dest);
                                    player.sendMessage(Component.text("🚀 Teleported to " + targetName + "'s Home " + (homeIdx + 1) + "!", NamedTextColor.GREEN));
                                }
                            }
                        } else {
                            int homeIdx = rawSlot - 11;
                            Location dest = homes[homeIdx];
                            if (dest != null) {
                                player.closeInventory();
                                player.teleport(dest);
                                player.sendMessage(Component.text("🚀 Teleported to " + targetName + "'s Home " + (homeIdx + 1) + "!", NamedTextColor.GREEN));
                            }
                        }
                    }
                }
            }
            return;
        }

        if (title.equals("Buy from Shop")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot >= 0 && slot < 9) {
                ItemStack clicked = event.getCurrentItem();
                if (clicked == null || clicked.getType() == Material.AIR) return;

                Location crateLoc = activeCratePurchase.get(uuid);
                if (crateLoc != null && shopCrates.containsKey(crateLoc)) {
                    ShopCrateData data = shopCrates.get(crateLoc);
                    if (data != null && data.active && data.items != null && data.items[slot] != null) {
                        long balance = 0L;
                        if (data.priceType.equalsIgnoreCase("keys")) {
                            balance = keysMap.getOrDefault(uuid, 0);
                        } else if (data.priceType.equalsIgnoreCase("erpies")) {
                            balance = erpiesMap.getOrDefault(uuid, 0L);
                        } else if (data.priceType.equalsIgnoreCase("derpies")) {
                            balance = derpiesMap.getOrDefault(uuid, 0L);
                        } else if (data.priceType.equalsIgnoreCase("Echo keys")) {
                            balance = echoKeysMap.getOrDefault(uuid, 0);
                        } else if (data.priceType.equalsIgnoreCase("crimson keys")) {
                            balance = crimsonKeysMap.getOrDefault(uuid, 0);
                        }

                        if (balance < data.price) {
                            player.sendMessage(Component.text("❌ You don't have enough " + data.priceType + "! Balance: " + balance, NamedTextColor.RED));
                            return;
                        }

                        if (data.priceType.equalsIgnoreCase("keys")) {
                            keysMap.put(uuid, (int) (balance - data.price));
                        } else if (data.priceType.equalsIgnoreCase("erpies")) {
                            erpiesMap.put(uuid, balance - data.price);
                        } else if (data.priceType.equalsIgnoreCase("derpies")) {
                            derpiesMap.put(uuid, balance - data.price);
                        } else if (data.priceType.equalsIgnoreCase("Echo keys")) {
                            echoKeysMap.put(uuid, (int) (balance - data.price));
                        } else if (data.priceType.equalsIgnoreCase("crimson keys")) {
                            crimsonKeysMap.put(uuid, (int) (balance - data.price));
                        }

                        addPlayerCurrency(data.owner, data.priceType, data.price);

                        ItemStack bought = data.items[slot].clone();
                        HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(bought);
                        for (ItemStack left : remaining.values()) {
                            player.getWorld().dropItemNaturally(player.getLocation(), left);
                        }

                        player.sendMessage(Component.text("🛍️ Purchase successful! Paid " + formatValue(data.price) + " " + data.priceType + ".", NamedTextColor.GREEN));
                    }
                }
            }
            return;
        }
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // A. Set Home Points GUI
        if (title.equals("Set Home Points")) {
            int rawSlot = event.getRawSlot();
            Location[] homes = playerHomes.computeIfAbsent(uuid, k -> new Location[5]);
            if (rawSlot >= 11 && rawSlot <= 15) {
                int homeIdx = rawSlot - 11;
                homes[homeIdx] = player.getLocation();
                player.sendMessage(Component.text("✅ Home " + (homeIdx + 1) + " set to your current location!", NamedTextColor.GREEN));
                openSetHomeGui(player);
            } else if (rawSlot >= 20 && rawSlot <= 24) {
                int homeIdx = rawSlot - 20;
                if (homes[homeIdx] != null) {
                    homes[homeIdx] = null;
                    player.sendMessage(Component.text("❌ Home " + (homeIdx + 1) + " removed!", NamedTextColor.RED));
                }
                openSetHomeGui(player);
            }
            return;
        }

        // B. Teleport Home GUI
        if (title.equals("Teleport Home")) {
            int rawSlot = event.getRawSlot();
            if (rawSlot >= 11 && rawSlot <= 15) {
                int homeIdx = rawSlot - 11;
                Location[] homes = playerHomes.get(uuid);
                if (homes != null && homes[homeIdx] != null) {
                    player.closeInventory();
                    performHomeCountdown(player, homes[homeIdx], homeIdx + 1);
                } else {
                    player.sendMessage(Component.text("❌ This home point is not set!", NamedTextColor.RED));
                }
            }
            return;
        }

        // C. Settings GUI
        if (title.equals("Settings")) {
            int rawSlot = event.getRawSlot();
            if (rawSlot == 11) {
                boolean current = chatSpamDisabled.getOrDefault(uuid, false);
                chatSpamDisabled.put(uuid, !current);
                player.sendMessage(Component.text("⚙️ Chat Spam disabled: " + (!current ? "ON" : "OFF"), NamedTextColor.YELLOW));
                openSettingsGui(player);
            } else if (rawSlot == 15) {
                boolean current = tpaDisabled.getOrDefault(uuid, false);
                tpaDisabled.put(uuid, !current);
                player.sendMessage(Component.text("⚙️ Auto-reject TPA requests: " + (!current ? "ON" : "OFF"), NamedTextColor.YELLOW));
                openSettingsGui(player);
            }
            return;
        }

        // 0. RTP GUI
        if (title.equals("Random Teleport")) {
            player.closeInventory();
            String dimension = null;
            if (clicked.getType() == Material.GRASS_BLOCK) dimension = "overworld";
            else if (clicked.getType() == Material.NETHERRACK) dimension = "nether";
            else if (clicked.getType() == Material.END_STONE) dimension = "end";
            if (dimension != null) performRtp(player, dimension);
            return;
        }

        // 1. Bounty Hunter GUI
        if (title.equals("Bounty Hunter")) {
            int rawSlot = event.getRawSlot();
            if (rawSlot == 3 || rawSlot == 5) {
                ItemStack targetHead = null;
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && item.getType() == Material.PLAYER_HEAD) {
                        targetHead = item;
                        break;
                    }
                }

                if (targetHead == null) {
                    player.sendMessage(Component.text("❌ You do not have any Player Heads to trade!", NamedTextColor.RED));
                    player.closeInventory();
                    return;
                }

                targetHead.setAmount(targetHead.getAmount() - 1);

                if (rawSlot == 3) {
                    erpiesMap.put(uuid, erpiesMap.getOrDefault(uuid, 0L) + 1000L);
                    player.sendMessage(Component.text("💰 Traded 1 Player Head for 1000 Erpies!", NamedTextColor.GREEN));
                } else {
                    derpiesMap.put(uuid, derpiesMap.getOrDefault(uuid, 0L) + 50L);
                    player.sendMessage(Component.text("💎 Traded 1 Player Head for 50 Derpies!", NamedTextColor.GREEN));
                }

                player.closeInventory();
            }
            return;
        }

        // 2. Main Shop GUI
        if (title.equals("The Erp SMP - Shop")) {
            if (clicked.getType() == Material.ENDER_EYE) openEndMenu(player);
            else if (clicked.getType() == Material.DIAMOND_SWORD) openPvpMenu(player);
            else if (clicked.getType() == Material.COOKED_BEEF) openFoodMenu(player);
            return;
        }

        // 3. Derpshop GUI
        if (title.equals("Derp Shop - Keys")) {
            int derpCost = -1;
            if (clicked.getType() == Material.TRIPWIRE_HOOK) derpCost = 10;
            else if (clicked.getType() == Material.REDSTONE) derpCost = 25;
            else if (clicked.getType() == Material.ECHO_SHARD) derpCost = 50;

            if (derpCost == -1) return;

            long playerDerpies = derpiesMap.getOrDefault(uuid, 0L);
            if (playerDerpies < derpCost) {
                player.sendMessage(Component.text("❌ You don't have enough Derpies!", NamedTextColor.RED));
                return;
            }

            derpiesMap.put(uuid, playerDerpies - derpCost);
            keysMap.put(uuid, keysMap.getOrDefault(uuid, 0) + 1);

            if (clicked.getType() == Material.TRIPWIRE_HOOK) {
                regularKeysMap.put(uuid, regularKeysMap.getOrDefault(uuid, 0) + 1);
            } else if (clicked.getType() == Material.REDSTONE) {
                crimsonKeysMap.put(uuid, crimsonKeysMap.getOrDefault(uuid, 0) + 1);
                // Give physical Crimson Key item
                HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(createCrimsonKey());
                for (ItemStack left : remaining.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), left);
                }
            } else if (clicked.getType() == Material.ECHO_SHARD) {
                echoKeysMap.put(uuid, echoKeysMap.getOrDefault(uuid, 0) + 1);
                // Give physical Echo Key item
                HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(createEchoKey());
                for (ItemStack left : remaining.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), left);
                }
            }

            player.sendMessage(Component.text("🔑 Successfully purchased 1x " + clicked.getItemMeta().getDisplayName() + "!", NamedTextColor.GREEN));
            openDerpShop(player);
            return;
        }

        // 4. Main Auction GUI
        if (title.equals("Auction House")) {
            int rawSlot = event.getRawSlot();
            if (rawSlot == 45) {
                openAuctionGui(player, null);
                return;
            }
            if (rawSlot == 46) {
                openSignInput(player, SignAction.SEARCH, null, "search here");
                return;
            }
            if (rawSlot == 47) {
                openMyListings(player);
                return;
            }
            if (rawSlot == 48) {
                player.closeInventory();
                Inventory hopper = Bukkit.createInventory(null, org.bukkit.event.inventory.InventoryType.HOPPER, Component.text("List an Item"));
                player.openInventory(hopper);
                return;
            }

            if (rawSlot < 45) {
                int slotIndex = rawSlot;
                AuctionListing clickedListing = null;
                int current = 0;
                for (AuctionListing listing : listings) {
                    if (current == slotIndex) {
                        clickedListing = listing;
                        break;
                    }
                    current++;
                }

                if (clickedListing != null) {
                    if (clickedListing.seller.equals(uuid)) {
                        player.sendMessage(Component.text("❌ You cannot buy your own item!", NamedTextColor.RED));
                        return;
                    }

                    long playerMoney = erpiesMap.getOrDefault(uuid, 0L);
                    if (playerMoney < clickedListing.price) {
                        player.sendMessage(Component.text("❌ You don't have enough Erpies!", NamedTextColor.RED));
                        return;
                    }

                    erpiesMap.put(uuid, playerMoney - clickedListing.price);
                    erpiesMap.put(clickedListing.seller, erpiesMap.getOrDefault(clickedListing.seller, 0L) + clickedListing.price);

                    listings.remove(clickedListing);
                    player.getInventory().addItem(clickedListing.item);
                    player.sendMessage(Component.text("🛍️ Successfully purchased " + clickedListing.item.getType().name() + "!", NamedTextColor.GREEN));
                    
                    Player sellerPlayer = Bukkit.getPlayer(clickedListing.seller);
                    if (sellerPlayer != null) {
                        sellerPlayer.sendMessage(Component.text("💰 Your listed item was sold! Received: " + clickedListing.price + " Erpies", NamedTextColor.GREEN));
                    }

                    openAuctionGui(player, null);
                }
            }
            return;
        }

        // 5. Your Listings GUI
        if (title.equals("Auction House - Your Listings")) {
            if (event.getRawSlot() == 49) {
                openAuctionGui(player, null);
                return;
            }
            if (event.getRawSlot() == 48) {
                player.closeInventory();
                Inventory listChest = Bukkit.createInventory(null, 9, Component.text("List an Item"));
                player.openInventory(listChest);
                return;
            }
            if (event.getRawSlot() < 45) {
                int index = event.getRawSlot();
                int current = 0;
                AuctionListing toRemove = null;
                for (AuctionListing listing : listings) {
                    if (listing.seller.equals(uuid)) {
                        if (current == index) {
                            toRemove = listing;
                            break;
                        }
                        current++;
                    }
                }
                if (toRemove != null) {
                    listings.remove(toRemove);
                    player.getInventory().addItem(toRemove.item);
                    player.sendMessage(Component.text("❌ Listing cancelled. Item returned to inventory.", NamedTextColor.GREEN));
                    openMyListings(player);
                }
            }
            return;
        }

        // 6. Order Board GUI
        if (title.equals("Order Board")) {
            int rawSlot = event.getRawSlot();
            if (rawSlot == 45) {
                openOrdersGui(player, null);
                return;
            }
            if (rawSlot == 46) {
                openSignInput(player, SignAction.SEARCH, null, "search here");
                return;
            }
            if (rawSlot == 47) {
                openMyOrders(player);
                return;
            }
            if (rawSlot == 48) {
                // Post a new order — sign input for item name
                openSignInput(player, SignAction.ORDER_ITEM, null, "item name");
                return;
            }
            if (rawSlot < 45) {
                // Fulfill an order
                int current = 0;
                OrderRequest target = null;
                for (OrderRequest order : orders) {
                    if (!order.buyer.equals(uuid)) { // can't fulfill your own
                        if (current == rawSlot) {
                            target = order;
                            break;
                        }
                        current++;
                    }
                }
                if (target == null) return;

                // Check if player has the item
                Material mat = Material.matchMaterial(target.itemName);
                if (mat == null) return;
                int needed = target.quantity;
                int inInv = 0;
                for (ItemStack it : player.getInventory().getContents()) {
                    if (it != null && it.getType() == mat) inInv += it.getAmount();
                }
                if (inInv < needed) {
                    player.sendMessage(Component.text("❌ You need " + needed + "x " + target.itemName + " to fulfill this order! You have " + inInv + ".", NamedTextColor.RED));
                    return;
                }

                // Remove items from fulfiller's inventory
                int toRemove = needed;
                for (ItemStack it : player.getInventory().getContents()) {
                    if (it != null && it.getType() == mat && toRemove > 0) {
                        int take = Math.min(it.getAmount(), toRemove);
                        it.setAmount(it.getAmount() - take);
                        toRemove -= take;
                    }
                }

                // Pay fulfiller
                erpiesMap.put(uuid, erpiesMap.getOrDefault(uuid, 0L) + target.price);

                // Give item to buyer
                Player buyerPlayer = Bukkit.getPlayer(target.buyer);
                if (buyerPlayer != null) {
                    HashMap<Integer, ItemStack> rem = buyerPlayer.getInventory().addItem(new ItemStack(mat, needed));
                    for (ItemStack left : rem.values()) buyerPlayer.getWorld().dropItemNaturally(buyerPlayer.getLocation(), left);
                    buyerPlayer.sendMessage(Component.text("📦 Your order for " + needed + "x " + target.itemName + " was fulfilled by " + player.getName() + "!", NamedTextColor.GREEN));
                }

                orders.remove(target);
                player.sendMessage(Component.text("✅ Order fulfilled! You received " + target.price + " Erpies.", NamedTextColor.GREEN));
                openOrdersGui(player, null);
            }
            return;
        }

        // 7. My Orders GUI
        if (title.equals("Order Board - Your Orders")) {
            if (event.getRawSlot() == 49) {
                openOrdersGui(player, null);
                return;
            }
            if (event.getRawSlot() < 45) {
                int index = event.getRawSlot();
                int current = 0;
                OrderRequest toCancel = null;
                for (OrderRequest order : orders) {
                    if (order.buyer.equals(uuid)) {
                        if (current == index) {
                            toCancel = order;
                            break;
                        }
                        current++;
                    }
                }
                if (toCancel != null) {
                    orders.remove(toCancel);
                    // Refund erpies
                    erpiesMap.put(uuid, erpiesMap.getOrDefault(uuid, 0L) + toCancel.price);
                    player.sendMessage(Component.text("❌ Order cancelled. " + toCancel.price + " Erpies refunded.", NamedTextColor.YELLOW));
                    openMyOrders(player);
                }
            }
            return;
        }

        // 8. Normal Categories Shop submenus
        int cost = getPrice(clicked.getType(), title);
        if (cost == -1) return;

        long playerMoney = erpiesMap.getOrDefault(uuid, 0L);
        if (playerMoney < cost) {
            player.sendMessage(Component.text("❌ You don't have enough Erpies!", NamedTextColor.RED));
            return;
        }

        erpiesMap.put(uuid, playerMoney - cost);
        player.getInventory().addItem(new ItemStack(clicked.getType(), 1));
        player.sendMessage(Component.text("🛍️ Successfully purchased 1x " + clicked.getType().name() + "!", NamedTextColor.GREEN));
        
        openMainMenu(player);
    }

    private void openEndMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Shop - End Items"));
        inv.setItem(10, createGuiItem(Material.SHULKER_BOX, "Shulker Box", NamedTextColor.WHITE, "Cost: 800 Erpies"));
        inv.setItem(12, createGuiItem(Material.ENDER_PEARL, "Ender Pearl", NamedTextColor.AQUA, "Cost: 75 Erpies"));
        inv.setItem(14, createGuiItem(Material.ENDER_CHEST, "Ender Chest", NamedTextColor.DARK_PURPLE, "Cost: 1200 Erpies"));
        inv.setItem(16, createGuiItem(Material.CHORUS_FRUIT, "Chorus Fruit", NamedTextColor.LIGHT_PURPLE, "Cost: 50 Erpies"));
        player.openInventory(inv);
    }

    private void openPvpMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Shop - PvP Combat"));
        inv.setItem(11, createGuiItem(Material.TOTEM_OF_UNDYING, "Totem of Undying", NamedTextColor.GOLD, "Cost: 1500 Erpies"));
        inv.setItem(12, createGuiItem(Material.WIND_CHARGE, "Wind Charge", NamedTextColor.GRAY, "Cost: 75 Erpies"));
        inv.setItem(13, createGuiItem(Material.END_CRYSTAL, "End Crystal", NamedTextColor.LIGHT_PURPLE, "Cost: 500 Erpies"));
        inv.setItem(14, createGuiItem(Material.OBSIDIAN, "Obsidian", NamedTextColor.DARK_GRAY, "Cost: 500 Erpies"));
        inv.setItem(15, createGuiItem(Material.ENCHANTED_GOLDEN_APPLE, "Enchanted Golden Apple", NamedTextColor.DARK_PURPLE, "Cost: 500 Erpies"));
        player.openInventory(inv);
    }

    private void openFoodMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Shop - Food & Provisions"));
        inv.setItem(10, createGuiItem(Material.BREAD, "Bread", NamedTextColor.YELLOW, "Cost: 25 Erpies"));
        inv.setItem(12, createGuiItem(Material.COOKED_BEEF, "Steak", NamedTextColor.RED, "Cost: 100 Erpies"));
        inv.setItem(14, createGuiItem(Material.COOKED_PORKCHOP, "Porkchop", NamedTextColor.GOLD, "Cost: 50 Erpies"));
        inv.setItem(16, createGuiItem(Material.GOLDEN_CARROT, "Golden Carrot", NamedTextColor.GOLD, "Cost: 125 Erpies"));
        player.openInventory(inv);
    }

    private int getPrice(Material material, String title) {
        if (title.contains("End Items")) {
            if (material == Material.SHULKER_BOX) return 800;
            if (material == Material.ENDER_CHEST) return 1200;
            if (material == Material.ENDER_PEARL) return 75;
            if (material == Material.CHORUS_FRUIT) return 50;
        } else if (title.contains("PvP Combat")) {
            if (material == Material.TOTEM_OF_UNDYING) return 1500;
            if (material == Material.WIND_CHARGE) return 75;
            if (material == Material.END_CRYSTAL) return 500;
            if (material == Material.OBSIDIAN) return 500;
            if (material == Material.ENCHANTED_GOLDEN_APPLE) return 500;
        } else if (title.contains("Food")) {
            if (material == Material.COOKED_BEEF) return 100;
            if (material == Material.COOKED_PORKCHOP) return 50;
            if (material == Material.BREAD) return 25;
            if (material == Material.GOLDEN_CARROT) return 125;
        }
        return -1;
    }

    private ItemStack createGuiItem(Material material, String name, NamedTextColor color, String description) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, color));
            meta.lore(List.of(Component.text(description, NamedTextColor.DARK_GRAY)));
            item.setItemMeta(meta);
        }
        return item;
    }

    // --- Order Board ---
    private void openOrdersGui(Player player, String query) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Order Board"));
        int slot = 0;
        for (OrderRequest order : orders) {
            if (order.buyer.equals(player.getUniqueId())) continue; // skip own orders
            Material mat = Material.matchMaterial(order.itemName);
            if (mat == null) mat = Material.PAPER;
            if (query != null && !order.itemName.toLowerCase().contains(query.toLowerCase())) continue;

            ItemStack display = new ItemStack(mat);
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(order.itemName, NamedTextColor.YELLOW));
                meta.lore(List.of(
                    Component.text("Buyer: " + order.buyerName, NamedTextColor.GRAY),
                    Component.text("Wants: " + order.quantity + "x " + order.itemName, NamedTextColor.WHITE),
                    Component.text("Paying: " + order.price + " Erpies", NamedTextColor.GOLD),
                    Component.text("Click to fulfill (needs item in inv)", NamedTextColor.GREEN)
                ));
                display.setItemMeta(meta);
            }
            inv.setItem(slot++, display);
            if (slot >= 45) break;
        }
        inv.setItem(45, createGuiItem(Material.DIAMOND, "Refresh", NamedTextColor.AQUA, "Click to refresh"));
        inv.setItem(46, createGuiItem(Material.OAK_SIGN, "Search", NamedTextColor.YELLOW, "Search orders by item name"));
        inv.setItem(47, createGuiItem(Material.CHEST, "Your Orders", NamedTextColor.GREEN, "View and cancel your buy orders"));
        inv.setItem(48, createGuiItem(Material.WRITABLE_BOOK, "Post Order", NamedTextColor.GOLD, "Request to buy an item (costs Erpies upfront)"));
        player.openInventory(inv);
    }

    private void openMyOrders(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Order Board - Your Orders"));
        UUID uuid = player.getUniqueId();
        int slot = 0;
        for (OrderRequest order : orders) {
            if (!order.buyer.equals(uuid)) continue;
            Material mat = Material.matchMaterial(order.itemName);
            if (mat == null) mat = Material.PAPER;
            ItemStack display = new ItemStack(mat);
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(order.itemName, NamedTextColor.YELLOW));
                meta.lore(List.of(
                    Component.text("Wants: " + order.quantity + "x " + order.itemName, NamedTextColor.WHITE),
                    Component.text("Paying: " + order.price + " Erpies", NamedTextColor.GOLD),
                    Component.text("Click to cancel (refunds Erpies)", NamedTextColor.RED)
                ));
                display.setItemMeta(meta);
            }
            inv.setItem(slot++, display);
            if (slot >= 45) break;
        }
        inv.setItem(49, createGuiItem(Material.BARRIER, "Back to Order Board", NamedTextColor.RED, "Return to main page"));
        player.openInventory(inv);
    }

    // --- Auction House ---
    private void openAuctionGui(Player player, String query) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Auction House"));
        
        int slot = 0;
        for (AuctionListing listing : listings) {
            if (query != null && !listing.item.getType().name().toLowerCase().contains(query.toLowerCase())) {
                continue;
            }
            ItemStack item = listing.item.clone();
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<Component> lore = meta.lore();
                if (lore == null) lore = new ArrayList<>();
                lore.add(Component.text("Price: " + listing.price + " Erpies", NamedTextColor.GOLD));
                lore.add(Component.text("Seller: " + listing.sellerName, NamedTextColor.GRAY));
                lore.add(Component.text("Click to buy", NamedTextColor.GREEN));
                meta.lore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
            if (slot >= 45) break;
        }

        inv.setItem(45, createGuiItem(Material.DIAMOND, "Refresh", NamedTextColor.AQUA, "Click to refresh page"));
        inv.setItem(46, createGuiItem(Material.OAK_SIGN, "Search", NamedTextColor.YELLOW, "Click to search items"));
        inv.setItem(47, createGuiItem(Material.CHEST, "Your listed items", NamedTextColor.GREEN, "Click to view your listings"));
        inv.setItem(48, createGuiItem(Material.HOPPER, "List", NamedTextColor.GOLD, "Click to list an item for sale"));

        player.openInventory(inv);
    }

    private void openMyListings(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Auction House - Your Listings"));
        UUID uuid = player.getUniqueId();
        int slot = 0;
        for (AuctionListing listing : listings) {
            if (listing.seller.equals(uuid)) {
                ItemStack item = listing.item.clone();
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    List<Component> lore = meta.lore();
                    if (lore == null) lore = new ArrayList<>();
                    lore.add(Component.text("Price: " + listing.price + " Erpies", NamedTextColor.YELLOW));
                    lore.add(Component.text("Click to cancel listing", NamedTextColor.RED));
                    meta.lore(lore);
                    item.setItemMeta(meta);
                }
                inv.setItem(slot++, item);
                if (slot >= 45) break;
            }
        }
        inv.setItem(48, createGuiItem(Material.CHEST, "List", NamedTextColor.GOLD, "Click to list an item"));
        inv.setItem(49, createGuiItem(Material.BARRIER, "Back to Auction", NamedTextColor.RED, "Return to main page"));
        player.openInventory(inv);
    }

    private void openSignInput(Player player, SignAction action, ItemStack item, String promptText) {
        Location loc = player.getLocation().getBlock().getLocation().add(0, 3, 0);
        org.bukkit.block.data.BlockData original = loc.getBlock().getBlockData();

        loc.getBlock().setType(Material.OAK_SIGN);
        org.bukkit.block.Sign sign = (org.bukkit.block.Sign) loc.getBlock().getState();
        sign.getSide(org.bukkit.block.sign.Side.FRONT).line(0, Component.text(promptText));
        sign.update();

        pendingSigns.put(player.getUniqueId(), new PendingSignInput(loc, original, action, item));
        player.closeInventory();
        player.openSign(sign, org.bukkit.block.sign.Side.FRONT);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (pendingSigns.containsKey(uuid)) {
            PendingSignInput pending = pendingSigns.remove(uuid);
            pending.loc.getBlock().setBlockData(pending.originalData);

            String input = "";
            for (int i = 0; i < 4; i++) {
                String line = PlainTextComponentSerializer.plainText().serialize(event.line(i)).trim();
                if (line.equalsIgnoreCase("search here") || line.equalsIgnoreCase("list the price") || line.isEmpty()) {
                    continue;
                }
                input = line;
                break;
            }
            if (input.isEmpty()) {
                String line0 = PlainTextComponentSerializer.plainText().serialize(event.line(0)).trim();
                if (!line0.equalsIgnoreCase("search here") && !line0.equalsIgnoreCase("list the price")) {
                    input = line0;
                }
            }

            if (pending.action == SignAction.SEARCH) {
                if (input.isEmpty()) {
                    player.sendMessage(Component.text("❌ Search cancelled.", NamedTextColor.RED));
                    Bukkit.getScheduler().runTask(this, () -> openAuctionGui(player, null));
                } else {
                    final String query = input;
                    player.sendMessage(Component.text("🔍 Searching for: " + query, NamedTextColor.GREEN));
                    Bukkit.getScheduler().runTask(this, () -> openAuctionGui(player, query));
                }
            } else if (pending.action == SignAction.ORDER_ITEM) {
                if (input.isEmpty()) {
                    player.sendMessage(Component.text("❌ Order cancelled.", NamedTextColor.RED));
                } else {
                    Material mat = Material.matchMaterial(input.toUpperCase().replace(" ", "_"));
                    if (mat == null) {
                        player.sendMessage(Component.text("❌ Unknown item: '" + input + "'. Use the Minecraft item name (e.g. diamond, oak_log).", NamedTextColor.RED));
                    } else {
                        pendingOrderItemName.put(uuid, mat.name());
                        Bukkit.getScheduler().runTask(this, () -> openSignInput(player, SignAction.ORDER_PRICE, null, "price (erpies)"));
                    }
                }
            } else if (pending.action == SignAction.ORDER_PRICE) {
                String itemName = pendingOrderItemName.remove(uuid);
                if (itemName == null || input.isEmpty()) {
                    player.sendMessage(Component.text("❌ Order cancelled.", NamedTextColor.RED));
                } else {
                    try {
                        long price = parseAmountWithSuffix(input);
                        if (price <= 0) {
                            player.sendMessage(Component.text("❌ Price must be greater than 0!", NamedTextColor.RED));
                        } else if (erpiesMap.getOrDefault(uuid, 0L) < price) {
                            player.sendMessage(Component.text("❌ You don't have enough Erpies! Need: " + price + ", Have: " + erpiesMap.getOrDefault(uuid, 0L), NamedTextColor.RED));
                        } else {
                            erpiesMap.put(uuid, erpiesMap.getOrDefault(uuid, 0L) - price);
                            orders.add(new OrderRequest(uuid, player.getName(), itemName, 1, price));
                            player.sendMessage(Component.text("✅ Buy order posted for 1x " + itemName + " at " + price + " Erpies!", NamedTextColor.GREEN));
                            Bukkit.getScheduler().runTask(this, () -> openOrdersGui(player, null));
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(Component.text("❌ Invalid price! Use a number (e.g. 500, 1k).", NamedTextColor.RED));
                    }
                }
            } else if (pending.action == SignAction.LIST_PRICE) {
                if (input.isEmpty()) {
                    player.sendMessage(Component.text("❌ Listing cancelled. Item returned.", NamedTextColor.RED));
                    player.getInventory().addItem(pending.item);
                } else {
                    try {
                        long price = parseAmountWithSuffix(input);
                        if (price <= 0) {
                            player.sendMessage(Component.text("❌ Price must be greater than 0! Item returned.", NamedTextColor.RED));
                            player.getInventory().addItem(pending.item);
                        } else {
                            listings.add(new AuctionListing(uuid, player.getName(), pending.item, price));
                            player.sendMessage(Component.text("✅ Item successfully listed for " + price + " Erpies!", NamedTextColor.GREEN));
                            Bukkit.getScheduler().runTask(this, () -> openAuctionGui(player, null));
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(Component.text("❌ Invalid price number! Item returned.", NamedTextColor.RED));
                        player.getInventory().addItem(pending.item);
                    }
                }
            } else if (pending.action == SignAction.SET_CRATE_PRICE) {
                Location crateLoc = activeCrateSetup.remove(uuid);
                ItemStack[] crateItems = pendingCrateItemsArray.remove(uuid);
                if (crateLoc != null && crateItems != null && shopCrates.containsKey(crateLoc)) {
                    if (input.isEmpty()) {
                        player.sendMessage(Component.text("❌ Setup cancelled. Items returned.", NamedTextColor.RED));
                        for (ItemStack item : crateItems) {
                            if (item != null) player.getInventory().addItem(item);
                        }
                    } else {
                        try {
                            ShopCrateData data = shopCrates.get(crateLoc);
                            String targetCurrency = null;
                            if (data != null) {
                                if (data.crateType.equals("echo")) targetCurrency = "Echo keys";
                                else if (data.crateType.equals("crimson")) targetCurrency = "crimson keys";
                                else if (data.crateType.equals("key")) targetCurrency = "keys";
                            }

                            ShopPrice priceObj = null;
                            if (targetCurrency != null) {
                                priceObj = parseCustomCratePrice(input, targetCurrency);
                            } else {
                                priceObj = parseShopPrice(input);
                            }

                            if (priceObj == null) {
                                if (targetCurrency != null) {
                                    player.sendMessage(Component.text("❌ Invalid price format! E.g. 50, 100k, 10m. Items returned.", NamedTextColor.RED));
                                } else {
                                    player.sendMessage(Component.text("❌ Invalid price format! E.g. 100kkeys, 50derpies, 1000erpies. Items returned.", NamedTextColor.RED));
                                }
                                for (ItemStack item : crateItems) {
                                    if (item != null) player.getInventory().addItem(item);
                                }
                            } else {
                                ShopCrateData newData = new ShopCrateData(
                                    crateLoc,
                                    crateItems,
                                    priceObj.price,
                                    priceObj.currency,
                                    player.getUniqueId(),
                                    player.getName(),
                                    true
                                );
                                if (data != null) {
                                    newData.hologramId = data.hologramId;
                                    newData.crateType = data.crateType;
                                }
                                shopCrates.put(crateLoc, newData);
                                updateCrateHologram(newData);
                                saveShopCrates();
                                player.sendMessage(Component.text("✅ Shop Crate configured successfully!", NamedTextColor.GREEN));
                            }
                        } catch (Exception e) {
                            player.sendMessage(Component.text("❌ Error setting price. Items returned.", NamedTextColor.RED));
                            for (ItemStack item : crateItems) {
                                if (item != null) player.getInventory().addItem(item);
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Random Teleport ---
    private void openRtpGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Random Teleport"));
        inv.setItem(11, createGuiItem(Material.GRASS_BLOCK, "Overworld", NamedTextColor.GREEN, "Teleport to a random Overworld location"));
        inv.setItem(13, createGuiItem(Material.NETHERRACK, "Nether", NamedTextColor.RED, "Teleport to a random Nether location"));
        inv.setItem(15, createGuiItem(Material.END_STONE, "End", NamedTextColor.YELLOW, "Teleport to a random End location"));
        player.openInventory(inv);
    }

    private void performRtp(Player player, String dimension) {
        final Location startLoc = player.getLocation().clone();
        // 5-second countdown using title
        new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                if (player.getLocation().getBlockX() != startLoc.getBlockX() ||
                    player.getLocation().getBlockY() != startLoc.getBlockY() ||
                    player.getLocation().getBlockZ() != startLoc.getBlockZ()) {
                    cancel();
                    player.sendTitle("§cTeleport Cancelled", "§7You moved!", 0, 20, 10);
                    player.sendMessage(Component.text("❌ Teleport cancelled because you moved!", NamedTextColor.RED));
                    return;
                }
                if (countdown > 0) {
                    player.sendTitle(
                        "§b Teleporting in...",
                        "§f" + countdown + " second" + (countdown == 1 ? "" : "s"),
                        0, 25, 5
                    );
                    countdown--;
                } else {
                    cancel();
                    player.sendTitle(
                        "§a Teleporting!",
                        "",
                        0, 20, 10
                    );
                    doRtpTeleport(player, dimension);
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private void doRtpTeleport(Player player, String dimension) {
        World target;
        int rangeMin, rangeMax;

        switch (dimension) {
            case "nether" -> {
                target = Bukkit.getWorld("world_nether");
                if (target == null) target = Bukkit.getWorlds().stream()
                        .filter(w -> w.getEnvironment() == World.Environment.NETHER)
                        .findFirst().orElse(null);
                rangeMin = -500; rangeMax = 500;
            }
            case "end" -> {
                target = Bukkit.getWorld("world_the_end");
                if (target == null) target = Bukkit.getWorlds().stream()
                        .filter(w -> w.getEnvironment() == World.Environment.THE_END)
                        .findFirst().orElse(null);
                rangeMin = 100; rangeMax = 600;
            }
            default -> {
                target = Bukkit.getWorlds().get(0);
                rangeMin = -5000; rangeMax = 5000;
            }
        }

        if (target == null) {
            player.sendMessage(net.kyori.adventure.text.Component.text("❌ That dimension does not exist on this server!", net.kyori.adventure.text.format.NamedTextColor.RED));
            return;
        }

        final World finalWorld = target;
        final int fMin = rangeMin;
        final int fMax = rangeMax;

        // Find safe location asynchronously, teleport on main thread
        new BukkitRunnable() {
            @Override
            public void run() {
                int attempts = 0;
                Location safe = null;
                while (attempts < 100) {
                    int x = random.nextInt(fMax - fMin) + fMin;
                    int z = random.nextInt(fMax - fMin) + fMin;
                    int y = finalWorld.getHighestBlockYAt(x, z);

                    if (finalWorld.getEnvironment() == World.Environment.NETHER) {
                        boolean foundY = false;
                        for (int testY = 120; testY > 30; testY--) {
                            Block footBlock = finalWorld.getBlockAt(x, testY, z);
                            Block headBlock = finalWorld.getBlockAt(x, testY + 1, z);
                            Block standBlock = finalWorld.getBlockAt(x, testY - 1, z);
                            if (footBlock.getType() == Material.AIR &&
                                headBlock.getType() == Material.AIR &&
                                standBlock.getType().isSolid() &&
                                standBlock.getType() != Material.LAVA &&
                                standBlock.getType() != Material.FIRE) {
                                y = testY;
                                foundY = true;
                                break;
                            }
                        }
                        if (!foundY) {
                            attempts++;
                            continue;
                        }
                    } else if (finalWorld.getEnvironment() == World.Environment.THE_END) {
                        if (y <= 10) {
                            attempts++;
                            continue;
                        }
                        Block standBlock = finalWorld.getBlockAt(x, y - 1, z);
                        if (!standBlock.getType().isSolid()) {
                            attempts++;
                            continue;
                        }
                    } else {
                        // Overworld: avoid water, lava, and voids
                        if (y <= 50) {
                            attempts++;
                            continue;
                        }
                        Block standBlock = finalWorld.getBlockAt(x, y - 1, z);
                        if (standBlock.getType() == Material.WATER || standBlock.getType() == Material.LAVA || !standBlock.getType().isSolid()) {
                            attempts++;
                            continue;
                        }
                    }

                    Location loc = new Location(finalWorld, x + 0.5, y, z + 0.5);
                    safe = loc;
                    break;
                }

                if (safe == null) {
                    if (finalWorld.getEnvironment() == World.Environment.NETHER) {
                        safe = new Location(finalWorld, 0.5, 64, 0.5);
                    } else if (finalWorld.getEnvironment() == World.Environment.THE_END) {
                        safe = new Location(finalWorld, 100.5, 49, 0.5);
                    } else {
                        safe = finalWorld.getSpawnLocation();
                    }
                }

                final Location dest = safe;
                new BukkitRunnable() {
                    @Override public void run() {
                        player.teleport(dest);
                        player.sendMessage(net.kyori.adventure.text.Component.text("🌍 Teleported to a random location in the " + dimension + "!", net.kyori.adventure.text.format.NamedTextColor.GREEN));
                    }
                }.runTask(CustomScoreboard.this);
            }
        }.runTaskAsynchronously(this);
    }

    // --- TPA System ---
    private void performTpaCountdown(Player requester, Player target) {
        final Location startLoc = requester.getLocation().clone();
        new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                if (!requester.isOnline() || !target.isOnline()) {
                    cancel();
                    if (requester.isOnline()) requester.sendMessage(Component.text("❌ Teleport cancelled!", NamedTextColor.RED));
                    return;
                }
                if (requester.getLocation().getBlockX() != startLoc.getBlockX() ||
                    requester.getLocation().getBlockY() != startLoc.getBlockY() ||
                    requester.getLocation().getBlockZ() != startLoc.getBlockZ()) {
                    cancel();
                    requester.sendTitle("§cTeleport Cancelled", "§7You moved!", 0, 20, 10);
                    requester.sendMessage(Component.text("❌ Teleport cancelled because you moved!", NamedTextColor.RED));
                    return;
                }
                if (countdown > 0) {
                    requester.sendTitle(
                        "§bTeleporting in...",
                        "§f" + countdown + " second" + (countdown == 1 ? "" : "s"),
                        0, 25, 5
                    );
                    countdown--;
                } else {
                    cancel();
                    requester.teleport(target.getLocation());
                    requester.sendTitle("§aTeleported!", "", 0, 20, 10);
                    requester.sendMessage(Component.text("✅ Teleported to " + target.getName() + "!", NamedTextColor.GREEN));
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    // --- Stash (OP) ---
    private void spawnStash(Player player) {
        Location loc = player.getLocation().getBlock().getLocation();
        loc.getBlock().setType(Material.SHULKER_BOX);
        org.bukkit.block.ShulkerBox box = (org.bukkit.block.ShulkerBox) loc.getBlock().getState();
        box.getInventory().addItem(new ItemStack(Material.ELYTRA));
        box.update();
        player.sendMessage(Component.text("📦 Spawned a shulker with elytra at your location!", NamedTextColor.GREEN));
    }

    // --- Admin Room (OP) ---
    private void teleportToAdminRoom(Player player) {
        World adminWorld = Bukkit.getWorld("admin_room");
        if (adminWorld == null) {
            WorldCreator creator = new WorldCreator("admin_room");
            creator.type(WorldType.FLAT);
            creator.generateStructures(false);
            adminWorld = Bukkit.createWorld(creator);
        }
        if (adminWorld != null) {
            Location spawn = new Location(adminWorld, 0.5, adminWorld.getHighestBlockYAt(0, 0) + 1, 0.5);
            player.teleport(spawn);
            player.sendMessage(Component.text("🏠 Welcome to the Admin Room!", NamedTextColor.GOLD));
        } else {
            player.sendMessage(Component.text("❌ Failed to create admin room!", NamedTextColor.RED));
        }
    }

    // --- Hopper Listing Close Handler ---
    @EventHandler
    public void onListHopperClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals("List an Item")) return;
        Player player = (Player) event.getPlayer();
        Inventory inv = event.getInventory();

        ItemStack toList = null;
        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                if (toList == null) {
                    toList = item.clone();
                } else {
                    // Return extra items
                    player.getInventory().addItem(item.clone());
                }
            }
        }
        inv.clear();

        if (toList != null) {
            pendingListItems.put(player.getUniqueId(), toList);
            final ItemStack listItem = toList;
            Bukkit.getScheduler().runTask(this, () -> openSignInput(player, SignAction.LIST_PRICE, listItem, "list the price"));
        }
    }

    @EventHandler
    public void onGuiClose(InventoryCloseEvent event) {
        String title = event.getView().getTitle();
        if (title.equals("Setup Crate Shop")) {
            Player player = (Player) event.getPlayer();
            UUID uuid = player.getUniqueId();
            Location loc = activeCrateSetup.get(uuid);
            if (loc != null && shopCrates.containsKey(loc)) {
                ItemStack[] items = new ItemStack[9];
                boolean hasItem = false;
                for (int i = 0; i < 9; i++) {
                    ItemStack item = event.getInventory().getItem(i);
                    if (item != null && item.getType() != Material.AIR) {
                        items[i] = item.clone();
                        hasItem = true;
                        event.getInventory().setItem(i, null);
                    }
                }
                if (hasItem) {
                    pendingCrateItemsArray.put(uuid, items);
                    Bukkit.getScheduler().runTask(this, () -> {
                        openSignInput(player, SignAction.SET_CRATE_PRICE, null, "Enter Price");
                    });
                } else {
                    activeCrateSetup.remove(uuid);
                    player.sendMessage(Component.text("❌ Setup cancelled: No items placed to sell.", NamedTextColor.RED));
                }
            }
            return;
        }
        if (title.equals("Buy from Shop")) {
            Player player = (Player) event.getPlayer();
            activeCratePurchase.remove(player.getUniqueId());
            return;
        }
        if (title.contains("Shop") || title.contains("Auction") || title.contains("Bounty") 
                || title.equals("Random Teleport") || title.equals("Set Home Points") 
                || title.equals("Teleport Home") || title.equals("Settings")
                || title.endsWith("'s Homes")) {
            Player player = (Player) event.getPlayer();
            player.setItemOnCursor(null);
            player.updateInventory();
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        String title = player.getOpenInventory().getTitle();
        if (title.contains("Shop") || title.contains("Auction") || title.contains("Bounty") 
                || title.equals("Random Teleport") || title.equals("Set Home Points") 
                || title.equals("Teleport Home") || title.equals("Settings")
                || title.endsWith("'s Homes")) {
            event.setCancelled(true);
            event.getItemDrop().remove();
            player.sendMessage(Component.text("❌ You cannot drop items while in a menu!", NamedTextColor.RED));
        }
    }

    // --- Bounty Hunter ---
    private void openBountyHunter(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("Bounty Hunter"));
        inv.setItem(3, createGuiItem(Material.DIAMOND, "Trade Head for Erpies", NamedTextColor.AQUA, "Cost: 1 Player Head | Receive: 1000 Erpies"));
        inv.setItem(5, createGuiItem(Material.AMETHYST_SHARD, "Trade Head for Derpies", NamedTextColor.LIGHT_PURPLE, "Cost: 1 Player Head | Receive: 50 Derpies"));
        player.openInventory(inv);
    }

    // --- Scoreboard ---
    public void updateScoreboard(Player player) {
        Scoreboard board = player.getScoreboard();
        if (board == Bukkit.getScoreboardManager().getMainScoreboard()) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(board);
        }

        updateNameplateTeams(board);

        Objective oldObj = board.getObjective("smp_board");
        if (oldObj != null) oldObj.unregister();

        Component title = Component.text("play.", NamedTextColor.BLUE)
                .append(Component.text("theerpsmp", NamedTextColor.GREEN))
                .append(Component.text(".net", NamedTextColor.GOLD));

        Objective obj = board.registerNewObjective("smp_board", Criteria.DUMMY, title);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.numberFormat(io.papermc.paper.scoreboard.numbers.NumberFormat.blank());

        UUID uuid = player.getUniqueId();

        addScoreboardRow(board, obj, "HoursPlayed", NamedTextColor.AQUA, String.valueOf(hoursPlayedMap.getOrDefault(uuid, 0)), 6, "§1");
        addScoreboardRow(board, obj, "Erpies", NamedTextColor.GREEN, formatValue(erpiesMap.getOrDefault(uuid, 0L)), 5, "§2");
        addScoreboardRow(board, obj, "Derpies", NamedTextColor.LIGHT_PURPLE, formatValue(derpiesMap.getOrDefault(uuid, 0L)), 4, "§3");
        addScoreboardRow(board, obj, "Keys", NamedTextColor.BLUE, String.valueOf(keysMap.getOrDefault(uuid, 0)), 3, "§4");
        addScoreboardRow(board, obj, "Kills", NamedTextColor.DARK_GREEN, String.valueOf(killsMap.getOrDefault(uuid, 0)), 2, "§5");
        addScoreboardRow(board, obj, "Deaths", NamedTextColor.RED, String.valueOf(deathsMap.getOrDefault(uuid, 0)), 1, "§6");
    }

    private String formatValue(long value) {
        if (value >= 1_000_000_000_000L) {
            return (value % 1_000_000_000_000L == 0) ? (value / 1_000_000_000_000L) + "T" : String.format("%.1fT", value / 1_000_000_000_000.0);
        }
        if (value >= 1_000_000_000L) {
            return (value % 1_000_000_000L == 0) ? (value / 1_000_000_000L) + "B" : String.format("%.1fB", value / 1_000_000_000.0);
        }
        if (value >= 1_000_000L) {
            return (value % 1_000_000L == 0) ? (value / 1_000_000L) + "M" : String.format("%.1fM", value / 1_000_000.0);
        }
        if (value >= 1000L) {
            return (value % 1000L == 0) ? (value / 1000L) + "k" : String.format("%.1fk", value / 1000.0);
        }
        return String.valueOf(value);
    }

    private void addScoreboardRow(Scoreboard board, Objective obj, String label, NamedTextColor labelColor, String valueStr, int scoreIndex, String placeholderId) {
        Component rowText = Component.text(label + ": ", labelColor)
                .append(Component.text(valueStr, NamedTextColor.WHITE));

        String teamName = "row_" + scoreIndex;
        Team team = board.getTeam(teamName);
        if (team == null) team = board.registerNewTeam(teamName);

        team.addEntry(placeholderId);
        team.prefix(rowText);

        Score score = obj.getScore(placeholderId);
        score.setScore(scoreIndex);
        score.numberFormat(io.papermc.paper.scoreboard.numbers.NumberFormat.blank());
    }

    private ItemStack createEchoPickaxe() {
        ItemStack pick = new ItemStack(Material.NETHERITE_PICKAXE);
        ItemMeta meta = pick.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Echo Pickaxe", NamedTextColor.AQUA));
            meta.lore(List.of(
                Component.text("Mined blocks burst with sonic echoes.", NamedTextColor.GRAY),
                Component.text("Mines a 3x3 hole forward,", NamedTextColor.GRAY),
                Component.text("or a 1x3 hole downwards.", NamedTextColor.GRAY)
            ));
            meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "echo_pickaxe");
            pick.setItemMeta(meta);
        }
        return pick;
    }

    private ItemStack createEchoAxe() {
        ItemStack axe = new ItemStack(Material.NETHERITE_AXE);
        ItemMeta meta = axe.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Echo Axe", NamedTextColor.AQUA));
            meta.lore(List.of(
                Component.text("Cuts down entire trees with a single chop.", NamedTextColor.GRAY)
            ));
            meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "echo_axe");
            axe.setItemMeta(meta);
        }
        return axe;
    }

    private ItemStack createEchoBow() {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta meta = bow.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Echo Bow", NamedTextColor.AQUA));
            meta.lore(List.of(
                Component.text("Arrows trail sonic explosions and blind targets.", NamedTextColor.GRAY)
            ));
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addEnchant(Enchantment.INFINITY, 1, true);
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "echo_bow");
            bow.setItemMeta(meta);
        }
        return bow;
    }

    private ItemStack createKnockbackStick() {
        ItemStack stick = new ItemStack(Material.STICK);
        ItemMeta meta = stick.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Knockback Stick", NamedTextColor.RED));
            meta.lore(List.of(
                Component.text("Sends targets into orbit.", NamedTextColor.GRAY)
            ));
            meta.addEnchant(Enchantment.KNOCKBACK, 100, true);
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "knockback_stick");
            stick.setItemMeta(meta);
        }
        return stick;
    }

    private void handleEchoPickaxeBreak(Player player, Block centerBlock, ItemStack tool) {
        Location centerLoc = centerBlock.getLocation();
        if (isInSpawnRadius(centerLoc)) return;

        BlockFace face = player.getTargetBlockFace(6);
        float pitch = player.getLocation().getPitch();
        
        List<Block> blocksToBreak = new ArrayList<>();
        blocksToBreak.add(centerBlock);

        if (pitch > 55 || pitch < -55 || face == BlockFace.UP || face == BlockFace.DOWN) {
            BlockFace playerDirection = player.getFacing();
            blocksToBreak.add(centerBlock.getRelative(playerDirection));
            blocksToBreak.add(centerBlock.getRelative(playerDirection.getOppositeFace()));
        } else {
            int dx1 = 0, dz1 = 0;
            int dx2 = 0, dz2 = 0;
            int dy1 = 0, dy2 = 0;

            if (face == BlockFace.NORTH || face == BlockFace.SOUTH) {
                dx1 = 1; dy2 = 1;
            } else if (face == BlockFace.EAST || face == BlockFace.WEST) {
                dz1 = 1; dy2 = 1;
            } else {
                dx1 = 1; dy2 = 1;
            }

            for (int h = -1; h <= 1; h++) {
                for (int v = -1; v <= 1; v++) {
                    if (h == 0 && v == 0) continue;
                    Block b = centerBlock.getRelative(h * dx1 + v * dx2, h * dy1 + v * dy2, h * dz1 + v * dz2);
                    blocksToBreak.add(b);
                }
            }
        }

        for (Block b : blocksToBreak) {
            if (b.getType() == Material.AIR || b.getType() == Material.BEDROCK || b.getType() == Material.BARRIER) continue;
            if (isInSpawnRadius(b.getLocation())) continue;
            b.breakNaturally(tool);
            b.getWorld().spawnParticle(Particle.SONIC_BOOM, b.getLocation().add(0.5, 0.5, 0.5), 1, 0, 0, 0, 0);
        }
    }

    private void handleEchoAxeBreak(Player player, Block startBlock, ItemStack tool) {
        Material startType = startBlock.getType();
        List<Block> logsToBreak = new ArrayList<>();
        List<Block> queue = new ArrayList<>();
        queue.add(startBlock);
        logsToBreak.add(startBlock);

        int maxLogs = 256;
        int index = 0;

        while (index < queue.size() && logsToBreak.size() < maxLogs) {
            Block current = queue.get(index++);
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;
                        Block relative = current.getRelative(x, y, z);
                        if (relative.getType() == startType && !logsToBreak.contains(relative)) {
                            if (isInSpawnRadius(relative.getLocation())) continue;
                            logsToBreak.add(relative);
                            queue.add(relative);
                        }
                    }
                }
            }
        }

        for (Block b : logsToBreak) {
            b.breakNaturally(tool);
            b.getWorld().spawnParticle(Particle.GUST, b.getLocation().add(0.5, 0.5, 0.5), 1, 0, 0, 0, 0);
        }
    }

    private void openSetHomeGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Set Home Points"));
        UUID uuid = player.getUniqueId();
        Location[] homes = playerHomes.computeIfAbsent(uuid, k -> new Location[5]);

        for (int i = 0; i < 5; i++) {
            Location loc = homes[i];
            int setSlot = 11 + i;
            int removeSlot = 20 + i;

            if (loc != null) {
                String locStr = String.format("%.0f, %.0f, %.0f (%s)", loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName());
                inv.setItem(setSlot, createGuiItem(Material.GREEN_WOOL, "Set Home " + (i + 1), NamedTextColor.GREEN, "Saved: " + locStr + " | Click to overwrite"));
                inv.setItem(removeSlot, createGuiItem(Material.BARRIER, "Remove Home " + (i + 1), NamedTextColor.RED, "Click to delete this home point"));
            } else {
                inv.setItem(setSlot, createGuiItem(Material.GRAY_WOOL, "Set Home " + (i + 1), NamedTextColor.GRAY, "Not set. Click to save current location here"));
                inv.setItem(removeSlot, createGuiItem(Material.RED_STAINED_GLASS_PANE, "No Home " + (i + 1) + " Set", NamedTextColor.RED, "No saved home point to remove"));
            }
        }

        player.openInventory(inv);
    }

    private void openHomeGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Teleport Home"));
        UUID uuid = player.getUniqueId();
        Location[] homes = playerHomes.computeIfAbsent(uuid, k -> new Location[5]);

        for (int i = 0; i < 5; i++) {
            Location loc = homes[i];
            int slot = 11 + i;

            if (loc != null) {
                String locStr = String.format("%.0f, %.0f, %.0f (%s)", loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName());
                inv.setItem(slot, createGuiItem(Material.GREEN_WOOL, "Teleport to Home " + (i + 1), NamedTextColor.GREEN, "Location: " + locStr + " | Click to teleport"));
            } else {
                inv.setItem(slot, createGuiItem(Material.GRAY_WOOL, "Home " + (i + 1) + " Not Set", NamedTextColor.GRAY, "Use /sethome to set this home point"));
            }
        }

        player.openInventory(inv);
    }

    private void performHomeCountdown(Player player, Location dest, int homeNumber) {
        final Location startLoc = player.getLocation().clone();
        new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                if (player.getLocation().getBlockX() != startLoc.getBlockX() ||
                    player.getLocation().getBlockY() != startLoc.getBlockY() ||
                    player.getLocation().getBlockZ() != startLoc.getBlockZ()) {
                    cancel();
                    player.sendTitle("§cTeleport Cancelled", "§7You moved!", 0, 20, 10);
                    player.sendMessage(Component.text("❌ Teleport cancelled because you moved!", NamedTextColor.RED));
                    return;
                }
                if (countdown > 0) {
                    player.sendTitle(
                        "§bTeleporting in...",
                        "§f" + countdown + " second" + (countdown == 1 ? "" : "s"),
                        0, 25, 5
                    );
                    countdown--;
                } else {
                    cancel();
                    player.teleport(dest);
                    player.sendTitle("§aWelcome Home!", "", 0, 20, 10);
                    player.sendMessage(Component.text("🏠 Teleported to Home " + homeNumber + "!", NamedTextColor.GREEN));
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private void performTeleportCountdown(Player player, Location dest, String destinationName) {
        final Location startLoc = player.getLocation().clone();
        new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                if (player.getLocation().getBlockX() != startLoc.getBlockX() ||
                    player.getLocation().getBlockY() != startLoc.getBlockY() ||
                    player.getLocation().getBlockZ() != startLoc.getBlockZ()) {
                    cancel();
                    player.sendTitle("§cTeleport Cancelled", "§7You moved!", 0, 20, 10);
                    player.sendMessage(Component.text("❌ Teleport cancelled because you moved!", NamedTextColor.RED));
                    return;
                }
                if (countdown > 0) {
                    player.sendTitle(
                        "§bTeleporting in...",
                        "§f" + countdown + " second" + (countdown == 1 ? "" : "s"),
                        0, 25, 5
                    );
                    countdown--;
                } else {
                    cancel();
                    player.teleport(dest);
                    player.sendTitle("§aTeleported!", "", 0, 20, 10);
                    player.sendMessage(Component.text("✅ Teleported to " + destinationName + "!", NamedTextColor.GREEN));
                    if (destinationName.equals("AFK Zone")) {
                        player.sendMessage(Component.text("💤 Welcome to the AFK Zone! You will earn 1 Derpy per minute.", NamedTextColor.LIGHT_PURPLE));
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private void teleportToAfkZone(Player player) {
        World afkWorld = Bukkit.getWorld("afk_zone");
        if (afkWorld == null) {
            WorldCreator creator = new WorldCreator("afk_zone");
            creator.type(WorldType.FLAT);
            creator.generateStructures(false);
            afkWorld = Bukkit.createWorld(creator);
        }
        if (afkWorld != null) {
            Location spawn = new Location(afkWorld, 0.5, afkWorld.getHighestBlockYAt(0, 0) + 1, 0.5);
            performTeleportCountdown(player, spawn, "AFK Zone");
        } else {
            player.sendMessage(Component.text("❌ Failed to create AFK zone!", NamedTextColor.RED));
        }
    }

    private void openSettingsGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Settings"));
        UUID uuid = player.getUniqueId();

        boolean spam = chatSpamDisabled.getOrDefault(uuid, false);
        if (spam) {
            inv.setItem(11, createGuiItem(Material.RED_WOOL, "Disable Chat Spam: ON", NamedTextColor.RED, "Click to toggle (Currently quiet)"));
        } else {
            inv.setItem(11, createGuiItem(Material.GREEN_WOOL, "Disable Chat Spam: OFF", NamedTextColor.GREEN, "Click to toggle (Currently shows alerts)"));
        }

        boolean tpa = tpaDisabled.getOrDefault(uuid, false);
        if (tpa) {
            inv.setItem(15, createGuiItem(Material.RED_WOOL, "Disable TPA Requests: ON", NamedTextColor.RED, "Click to toggle (Auto-rejects TPA)"));
        } else {
            inv.setItem(15, createGuiItem(Material.GREEN_WOOL, "Disable TPA Requests: OFF", NamedTextColor.GREEN, "Click to toggle (Accepts incoming TPA)"));
        }

        player.openInventory(inv);
    }

    // --- Shop Crate Helpers & Handlers ---

    public static class ShopPrice {
        public final long price;
        public final String currency;

        public ShopPrice(long price, String currency) {
            this.price = price;
            this.currency = currency;
        }
    }

    private ShopPrice parseShopPrice(String input) {
        if (input == null) return null;
        String clean = input.replaceAll("[()\\s]", "").toLowerCase();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^(\\d+)(k|m|b|t)?(keys|erpies|derpies|echokeys|crimsonkeys)$");
        java.util.regex.Matcher matcher = pattern.matcher(clean);
        if (!matcher.matches()) return null;
        try {
            long base = Long.parseLong(matcher.group(1));
            String suffix = matcher.group(2);
            String currency = matcher.group(3);
            long price = base;
            if (suffix != null) {
                if (suffix.equals("k")) {
                    price = base * 1000L;
                } else if (suffix.equals("m")) {
                    price = base * 1000000L;
                } else if (suffix.equals("b")) {
                    price = base * 1000000000L;
                } else if (suffix.equals("t")) {
                    price = base * 1000000000000L;
                }
            }
            if (price <= 0) return null;
            
            // Normalize currency naming
            if (currency.equals("echokeys")) {
                currency = "Echo keys";
            } else if (currency.equals("crimsonkeys")) {
                currency = "crimson keys";
            }
            
            return new ShopPrice(price, currency);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private ItemStack createShopCrate() {
        ItemStack chest = new ItemStack(Material.CHEST);
        ItemMeta meta = chest.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Shop Crate", NamedTextColor.GOLD));
            meta.lore(List.of(
                Component.text("Place to start a custom shop.", NamedTextColor.GRAY),
                Component.text("Can only be configured in Creative.", NamedTextColor.GRAY),
                Component.text("Cannot be obtained in Survival.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "shop_crate");
            chest.setItemMeta(meta);
        }
        return chest;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CHEST) return;
        Location loc = block.getLocation();
        if (!shopCrates.containsKey(loc)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        ShopCrateData data = shopCrates.get(loc);

        if (player.getGameMode() == GameMode.CREATIVE) {
            openCrateSetupGui(player, data);
        } else {
            boolean hasItems = false;
            if (data.items != null) {
                for (ItemStack it : data.items) {
                    if (it != null && it.getType() != Material.AIR) {
                        hasItems = true;
                        break;
                    }
                }
            }
            if (!data.active || !hasItems) {
                player.sendMessage(Component.text("❌ This shop is not yet active!", NamedTextColor.RED));
                return;
            }
            openCratePurchaseGui(player, data);
        }
    }

    private void openCrateSetupGui(Player player, ShopCrateData data) {
        activeCrateSetup.put(player.getUniqueId(), data.loc);
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("Setup Crate Shop"));
        if (data.items != null) {
            for (int i = 0; i < 9; i++) {
                if (data.items[i] != null) {
                    inv.setItem(i, data.items[i].clone());
                }
            }
        }
        player.openInventory(inv);
    }

    private void openCratePurchaseGui(Player player, ShopCrateData data) {
        activeCratePurchase.put(player.getUniqueId(), data.loc);
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("Buy from Shop"));
        for (int i = 0; i < 9; i++) {
            if (data.items[i] != null && data.items[i].getType() != Material.AIR) {
                ItemStack display = data.items[i].clone();
                ItemMeta meta = display.getItemMeta();
                if (meta != null) {
                    List<Component> lore = meta.lore();
                    if (lore == null) lore = new java.util.ArrayList<>();
                    lore.add(Component.text("Price: " + formatValue(data.price) + " " + data.priceType, NamedTextColor.GOLD));
                    lore.add(Component.text("Click to purchase", NamedTextColor.GREEN));
                    meta.lore(lore);
                    display.setItemMeta(meta);
                }
                inv.setItem(i, display);
            }
        }
        player.openInventory(inv);
    }

    private void addPlayerCurrency(UUID uuid, String type, long amount) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            if (type.equalsIgnoreCase("keys")) {
                keysMap.put(uuid, keysMap.getOrDefault(uuid, 0) + (int) amount);
            } else if (type.equalsIgnoreCase("erpies")) {
                erpiesMap.put(uuid, erpiesMap.getOrDefault(uuid, 0L) + amount);
            } else if (type.equalsIgnoreCase("derpies")) {
                derpiesMap.put(uuid, derpiesMap.getOrDefault(uuid, 0L) + amount);
            } else if (type.equalsIgnoreCase("Echo keys") || type.equalsIgnoreCase("echokeys")) {
                echoKeysMap.put(uuid, echoKeysMap.getOrDefault(uuid, 0) + (int) amount);
            } else if (type.equalsIgnoreCase("crimson keys") || type.equalsIgnoreCase("crimsonkeys")) {
                crimsonKeysMap.put(uuid, crimsonKeysMap.getOrDefault(uuid, 0) + (int) amount);
            }
            online.sendMessage(Component.text("💰 You received " + amount + " " + type + " from your shop sale!", NamedTextColor.GREEN));
        } else {
            String path = "players." + uuid.toString() + ".";
            String configKey = type;
            if (type.equalsIgnoreCase("Echo keys") || type.equalsIgnoreCase("echokeys")) {
                configKey = "echoKeys";
            } else if (type.equalsIgnoreCase("crimson keys") || type.equalsIgnoreCase("crimsonkeys")) {
                configKey = "crimsonKeys";
            }
            long current = getConfig().getLong(path + configKey, 0L);
            getConfig().set(path + configKey, current + amount);
            saveConfig();
        }
    }

    private void updateCrateHologram(ShopCrateData data) {
        deleteCrateHologram(data);
        if (!data.active || data.items == null) return;
        
        StringBuilder itemLines = new StringBuilder();
        int count = 0;
        for (ItemStack item : data.items) {
            if (item != null && item.getType() != Material.AIR) {
                String name = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                    ? PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName())
                    : item.getType().name().replace("_", " ").toLowerCase();
                name = capitalize(name);
                itemLines.append(name).append(" x").append(item.getAmount()).append(", ");
                count++;
                if (count >= 3) break;
            }
        }
        String itemsStr = itemLines.toString();
        if (itemsStr.endsWith(", ")) {
            itemsStr = itemsStr.substring(0, itemsStr.length() - 2);
        }
        if (count > 3) {
            itemsStr += " & more...";
        }
        final String finalItemsStr = itemsStr;

        Location holoLoc = data.loc.clone().add(0.5, 1.2, 0.5);
        TextDisplay display = holoLoc.getWorld().spawn(holoLoc, TextDisplay.class, entity -> {
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setBackgroundColor(Color.fromARGB(100, 0, 0, 0));
            entity.setShadowed(true);
            Component line1 = Component.text("🛒 " + data.ownerName + "'s Crate Shop", NamedTextColor.GOLD);
            Component line2 = Component.text("Selling: " + finalItemsStr, NamedTextColor.WHITE);
            Component line3 = Component.text("Price: ", NamedTextColor.YELLOW)
                .append(Component.text(formatValue(data.price) + " " + data.priceType, NamedTextColor.GREEN));
            Component text = line1.append(Component.newline()).append(line2).append(Component.newline()).append(line3);
            
            if (!data.crateType.equals("normal")) {
                NamedTextColor currencyColor = NamedTextColor.YELLOW;
                String currencyName = "Keys";
                if (data.crateType.equals("echo")) {
                    currencyColor = NamedTextColor.AQUA;
                    currencyName = "Echo keys";
                } else if (data.crateType.equals("crimson")) {
                    currencyColor = NamedTextColor.RED;
                    currencyName = "Crimson keys";
                } else if (data.crateType.equals("key")) {
                    currencyColor = NamedTextColor.BLUE;
                    currencyName = "Keys";
                }
                Component line4 = Component.text(currencyName + ": ", currencyColor)
                    .append(Component.text("- -", NamedTextColor.WHITE));
                text = text.append(Component.newline()).append(line4);
            }
            
            entity.text(text);
        });
        data.hologramId = display.getUniqueId();
    }

    private void deleteCrateHologram(ShopCrateData data) {
        if (data.hologramId != null) {
            org.bukkit.entity.Entity entity = Bukkit.getEntity(data.hologramId);
            if (entity != null) {
                entity.remove();
            }
            data.hologramId = null;
        }
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        String[] words = str.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private void loadShopCrates() {
        shopCrates.clear();
        if (!getConfig().contains("shopcrates")) return;
        org.bukkit.configuration.ConfigurationSection sec = getConfig().getConfigurationSection("shopcrates");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            String path = "shopcrates." + key;
            String worldName = getConfig().getString(path + ".world");
            double x = getConfig().getDouble(path + ".x");
            double y = getConfig().getDouble(path + ".y");
            double z = getConfig().getDouble(path + ".z");
            World w = Bukkit.getWorld(worldName);
            if (w == null) continue;
            Location loc = new Location(w, x, y, z);
            
            ItemStack[] items = new ItemStack[9];
            if (getConfig().contains(path + ".items")) {
                List<?> list = getConfig().getList(path + ".items");
                if (list != null) {
                    for (int i = 0; i < Math.min(9, list.size()); i++) {
                        if (list.get(i) instanceof ItemStack) {
                            items[i] = (ItemStack) list.get(i);
                        }
                    }
                }
            } else {
                ItemStack single = getConfig().getItemStack(path + ".item");
                items[4] = single;
            }
            
            int price = getConfig().getInt(path + ".price");
            String priceType = getConfig().getString(path + ".priceType");
            String ownerUUIDStr = getConfig().getString(path + ".owner");
            UUID owner = ownerUUIDStr != null ? UUID.fromString(ownerUUIDStr) : null;
            String ownerName = getConfig().getString(path + ".ownerName");
            boolean active = getConfig().getBoolean(path + ".active");
            String holoUUIDStr = getConfig().getString(path + ".hologramId");
            UUID hologramId = holoUUIDStr != null ? UUID.fromString(holoUUIDStr) : null;

            ShopCrateData data = new ShopCrateData(loc, items, price, priceType, owner, ownerName, active);
            data.hologramId = hologramId;
            data.crateType = getConfig().getString(path + ".crateType", "normal");
            shopCrates.put(loc, data);

            if (active) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        updateCrateHologram(data);
                    }
                }.runTaskLater(this, 20L);
            }
        }
    }

    private void saveShopCrates() {
        getConfig().set("shopcrates", null);
        for (var entry : shopCrates.entrySet()) {
            Location loc = entry.getKey();
            ShopCrateData data = entry.getValue();
            String key = loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
            String path = "shopcrates." + key;
            getConfig().set(path + ".world", loc.getWorld().getName());
            getConfig().set(path + ".x", loc.getX());
            getConfig().set(path + ".y", loc.getY());
            getConfig().set(path + ".z", loc.getZ());
            
            List<ItemStack> itemList = java.util.Arrays.asList(data.items);
            getConfig().set(path + ".items", itemList);
            
            getConfig().set(path + ".price", data.price);
            getConfig().set(path + ".priceType", data.priceType);
            getConfig().set(path + ".owner", data.owner != null ? data.owner.toString() : null);
            getConfig().set(path + ".ownerName", data.ownerName);
            getConfig().set(path + ".active", data.active);
            getConfig().set(path + ".hologramId", data.hologramId != null ? data.hologramId.toString() : null);
            getConfig().set(path + ".crateType", data.crateType);
        }
        saveConfig();
    }

    private ItemStack createSwordDerp() {
        ItemStack sword = new ItemStack(Material.WOODEN_SWORD);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Sword o' Derp", NamedTextColor.GOLD));
            meta.lore(List.of(Component.text("You Just Got Derped", NamedTextColor.DARK_PURPLE)));
            if (meta instanceof org.bukkit.inventory.meta.Damageable dmg) {
                dmg.setDamage(Material.WOODEN_SWORD.getMaxDurability() - 1);
            }
            meta.addEnchant(Enchantment.KNOCKBACK, 100000000, true);
            meta.addEnchant(Enchantment.SHARPNESS, 1000, true);
            meta.addEnchant(Enchantment.FIRE_ASPECT, 100000000, true);
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "sword_derp");
            sword.setItemMeta(meta);
        }
        return sword;
    }

    private ItemStack createPickaxeLerp() {
        ItemStack pick = new ItemStack(Material.NETHERITE_PICKAXE);
        ItemMeta meta = pick.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Pickaxe o' Lerp", NamedTextColor.AQUA));
            meta.lore(List.of(Component.text("Mines the entire vein of ores.", NamedTextColor.GRAY)));
            meta.addEnchant(Enchantment.EFFICIENCY, 10000, true);
            meta.addEnchant(Enchantment.FORTUNE, 1000, true);
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "pickaxe_lerp");
            pick.setItemMeta(meta);
        }
        return pick;
    }

    private ItemStack createMaceMerp() {
        ItemStack mace = new ItemStack(Material.MACE);
        ItemMeta meta = mace.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Mace o' Merp", NamedTextColor.LIGHT_PURPLE));
            meta.lore(List.of(Component.text("Merp! Feel the wind burst.", NamedTextColor.GRAY)));
            meta.addEnchant(Enchantment.WIND_BURST, 3, true);
            meta.addEnchant(Enchantment.DENSITY, 1000000, true);
            meta.addEnchant(Enchantment.BREACH, 100000000, true);
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "mace_merp");
            mace.setItemMeta(meta);
        }
        return mace;
    }

    private void handlePickaxeLerpBreak(Player player, Block startBlock, ItemStack tool) {
        Material startType = startBlock.getType();
        List<Block> oresToBreak = new ArrayList<>();
        List<Block> queue = new ArrayList<>();
        queue.add(startBlock);
        oresToBreak.add(startBlock);

        int maxOres = 256;
        int index = 0;

        while (index < queue.size() && oresToBreak.size() < maxOres) {
            Block current = queue.get(index++);
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) continue;
                        Block relative = current.getRelative(x, y, z);
                        if (relative.getType() == startType && !oresToBreak.contains(relative)) {
                            if (isInSpawnRadius(relative.getLocation())) continue;
                            oresToBreak.add(relative);
                            queue.add(relative);
                        }
                    }
                }
            }
        }

        for (Block b : oresToBreak) {
            b.breakNaturally(tool);
            b.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, b.getLocation().add(0.5, 0.5, 0.5), 1, 0, 0, 0, 0);
        }
    }

    private ItemStack createEchoSword() {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Echo Sword", NamedTextColor.AQUA));
            meta.lore(List.of(Component.text("Strikes targets with blinding sonic echoes.", NamedTextColor.GRAY)));
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
            meta.addEnchant(Enchantment.SHARPNESS, 5, true);
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "echo_sword");
            sword.setItemMeta(meta);
        }
        return sword;
    }

    private ItemStack createEndGatewayItem() {
        ItemStack item = new ItemStack(Material.END_PORTAL_FRAME);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("End Gateway Portal", NamedTextColor.LIGHT_PURPLE));
            meta.lore(List.of(Component.text("Spawns an End Gateway portal when placed.", NamedTextColor.GRAY)));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "end_gateway");
            item.setItemMeta(meta);
        }
        return item;
    }

    private void updateNameplateTeams(Scoreboard board) {
        Team ownerTeam = getOrCreateTeam(board, "np_owner", Component.text("[Owner] ", NamedTextColor.RED), NamedTextColor.RED);
        Team coOwnerTeam = getOrCreateTeam(board, "np_coowner", Component.text("[Co-Owner] ", NamedTextColor.BLUE), NamedTextColor.BLUE);
        Team adminDerpTeam = getOrCreateTeam(board, "np_adminderp", Component.text("[admin o' derp] ", NamedTextColor.DARK_PURPLE), NamedTextColor.DARK_PURPLE);
        Team adminTeam = getOrCreateTeam(board, "np_admin", Component.text("[admin] ", NamedTextColor.LIGHT_PURPLE), NamedTextColor.LIGHT_PURPLE);

        for (Team t : List.of(ownerTeam, coOwnerTeam, adminDerpTeam, adminTeam)) {
            for (String entry : new java.util.ArrayList<>(t.getEntries())) {
                t.removeEntry(entry);
            }
        }

        for (Player online : Bukkit.getOnlinePlayers()) {
            String name = online.getName();
            if (name.equalsIgnoreCase(".Redtoppat208") || name.equalsIgnoreCase(".RedToppat208")) {
                ownerTeam.addEntry(name);
            } else if (name.equalsIgnoreCase(".Boreas4052") || name.equalsIgnoreCase(".Boreas4052")) {
                coOwnerTeam.addEntry(name);
            } else if (name.equalsIgnoreCase(".Ironwarden7425") || name.equalsIgnoreCase(".IronWarden7425")) {
                adminDerpTeam.addEntry(name);
            } else if (online.isOp()) {
                adminTeam.addEntry(name);
            }
        }
    }

    private Team getOrCreateTeam(Scoreboard board, String teamName, Component prefix, NamedTextColor color) {
        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }
        team.prefix(prefix);
        team.color(color);
        return team;
    }

    private ItemStack createEchoCrate() {
        ItemStack chest = new ItemStack(Material.CHEST);
        ItemMeta meta = chest.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Echo Crate", NamedTextColor.AQUA));
            meta.lore(List.of(
                Component.text("Place to start an Echo keys shop.", NamedTextColor.GRAY),
                Component.text("Shows your Echo keys balance above.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "echo_crate");
            chest.setItemMeta(meta);
        }
        return chest;
    }

    private ItemStack createCrimsonCrate() {
        ItemStack chest = new ItemStack(Material.CHEST);
        ItemMeta meta = chest.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Crimson Crate", NamedTextColor.RED));
            meta.lore(List.of(
                Component.text("Place to start a Crimson keys shop.", NamedTextColor.GRAY),
                Component.text("Shows your Crimson keys balance above.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "crimson_crate");
            chest.setItemMeta(meta);
        }
        return chest;
    }

    private ItemStack createKeyCrate() {
        ItemStack chest = new ItemStack(Material.CHEST);
        ItemMeta meta = chest.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Key Crate", NamedTextColor.BLUE));
            meta.lore(List.of(
                Component.text("Place to start a Keys shop.", NamedTextColor.GRAY),
                Component.text("Shows your Keys balance above.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "key_crate");
            chest.setItemMeta(meta);
        }
        return chest;
    }

    private ItemStack createEchoKey() {
        ItemStack key = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = key.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Echo Key", NamedTextColor.AQUA));
            meta.lore(List.of(
                Component.text("A key imbued with echo energy.", NamedTextColor.GRAY),
                Component.text("Used to open Echo Crates.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "echo_key");
            key.setItemMeta(meta);
        }
        return key;
    }

    private ItemStack createCrimsonKey() {
        ItemStack key = new ItemStack(Material.REDSTONE);
        ItemMeta meta = key.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Crimson Key", NamedTextColor.RED));
            meta.lore(List.of(
                Component.text("A key forged in crimson flame.", NamedTextColor.GRAY),
                Component.text("Used to open Crimson Crates.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "crimson_key");
            key.setItemMeta(meta);
        }
        return key;
    }

    private ShopPrice parseCustomCratePrice(String input, String targetCurrency) {
        if (input == null) return null;
        String clean = input.replaceAll("[()\\s]", "").toLowerCase();
        
        String cleanCurrency = targetCurrency.replaceAll("\\s", "").toLowerCase();
        
        java.util.regex.Pattern patternWithCurrency = java.util.regex.Pattern.compile("^(\\d+)(k|m|b|t)?" + cleanCurrency + "$");
        java.util.regex.Matcher matcherWith = patternWithCurrency.matcher(clean);
        if (matcherWith.matches()) {
            return parseBaseAndSuffix(matcherWith.group(1), matcherWith.group(2), targetCurrency);
        }
        
        java.util.regex.Pattern patternNumberOnly = java.util.regex.Pattern.compile("^(\\d+)(k|m|b|t)?$");
        java.util.regex.Matcher matcherNum = patternNumberOnly.matcher(clean);
        if (matcherNum.matches()) {
            return parseBaseAndSuffix(matcherNum.group(1), matcherNum.group(2), targetCurrency);
        }
        
        return null;
    }

    private ShopPrice parseBaseAndSuffix(String baseStr, String suffix, String targetCurrency) {
        try {
            long base = Long.parseLong(baseStr);
            long price = base;
            if (suffix != null) {
                if (suffix.equals("k")) {
                    price = base * 1000L;
                } else if (suffix.equals("m")) {
                    price = base * 1000000L;
                } else if (suffix.equals("b")) {
                    price = base * 1000000000L;
                } else if (suffix.equals("t")) {
                    price = base * 1000000000000L;
                }
            }
            if (price <= 0) return null;
            return new ShopPrice(price, targetCurrency);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void updateCustomCrateHolograms() {
        for (ShopCrateData data : shopCrates.values()) {
            if (!data.active || data.crateType.equals("normal") || data.hologramId == null) continue;
            
            org.bukkit.entity.Entity entity = Bukkit.getEntity(data.hologramId);
            if (entity instanceof TextDisplay display) {
                Player nearest = null;
                double nearestDist = 10.0;
                for (Player p : data.loc.getWorld().getPlayers()) {
                    double dist = p.getLocation().distance(data.loc);
                    if (dist < nearestDist) {
                        nearest = p;
                        nearestDist = dist;
                    }
                }
                
                String balanceStr = "- -";
                if (nearest != null) {
                    UUID uuid = nearest.getUniqueId();
                    if (data.crateType.equals("echo")) {
                        balanceStr = String.valueOf(echoKeysMap.getOrDefault(uuid, 0));
                    } else if (data.crateType.equals("crimson")) {
                        balanceStr = String.valueOf(crimsonKeysMap.getOrDefault(uuid, 0));
                    } else if (data.crateType.equals("key")) {
                        balanceStr = String.valueOf(keysMap.getOrDefault(uuid, 0));
                    }
                }
                
                StringBuilder itemLines = new StringBuilder();
                int count = 0;
                for (ItemStack item : data.items) {
                    if (item != null && item.getType() != Material.AIR) {
                        String name = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                            ? PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName())
                            : item.getType().name().replace("_", " ").toLowerCase();
                        name = capitalize(name);
                        itemLines.append(name).append(" x").append(item.getAmount()).append(", ");
                        count++;
                        if (count >= 3) break;
                    }
                }
                String itemsStr = itemLines.toString();
                if (itemsStr.endsWith(", ")) {
                    itemsStr = itemsStr.substring(0, itemsStr.length() - 2);
                }
                if (count > 3) {
                    itemsStr += " & more...";
                }
                final String finalItemsStr = itemsStr;
                
                NamedTextColor currencyColor = NamedTextColor.YELLOW;
                String currencyName = "Keys";
                if (data.crateType.equals("echo")) {
                    currencyColor = NamedTextColor.AQUA;
                    currencyName = "Echo keys";
                } else if (data.crateType.equals("crimson")) {
                    currencyColor = NamedTextColor.RED;
                    currencyName = "Crimson keys";
                } else if (data.crateType.equals("key")) {
                    currencyColor = NamedTextColor.BLUE;
                    currencyName = "Keys";
                }
                
                Component line1 = Component.text("🛒 " + data.ownerName + "'s Crate Shop", NamedTextColor.GOLD);
                Component line2 = Component.text("Selling: " + finalItemsStr, NamedTextColor.WHITE);
                Component line3 = Component.text("Price: ", NamedTextColor.YELLOW)
                    .append(Component.text(formatValue(data.price) + " " + data.priceType, NamedTextColor.GREEN));
                Component line4 = Component.text(currencyName + ": ", currencyColor)
                    .append(Component.text(balanceStr, NamedTextColor.WHITE));
                
                Component text = line1.append(Component.newline())
                    .append(line2).append(Component.newline())
                    .append(line3).append(Component.newline())
                    .append(line4);
                display.text(text);
            }
        }
    }

    @EventHandler
    public void onCommandSend(PlayerCommandSendEvent event) {
        event.getCommands().removeIf(cmd -> cmd.startsWith("erpscoreboard:"));
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().toLowerCase();

        // Block teleport commands while in combat
        if (combatTagTicks.containsKey(event.getPlayer().getUniqueId())) {
            String[] parts = message.split(" ");
            String cmd = parts[0].replaceAll("^/", "");
            if (cmd.contains(":")) cmd = cmd.substring(cmd.indexOf(':') + 1);
            if (cmd.startsWith("tp") || cmd.equals("spawn") || cmd.equals("rtp") || cmd.equals("home") || cmd.equals("afk") || cmd.equals("warp")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Component.text("❌ You cannot use teleport commands while in combat!", NamedTextColor.RED));
                return;
            }
        }

        // Block /op, /deop, /ban for everyone except .Redtoppat208
        if (!event.getPlayer().getName().equals(".Redtoppat208")) {
            String[] parts = message.split(" ");
            String cmd = parts[0].replaceAll("^/", "");
            // Strip namespace prefix (e.g. minecraft:op -> op)
            if (cmd.contains(":")) cmd = cmd.substring(cmd.indexOf(':') + 1);
            if (cmd.equals("op") || cmd.equals("deop") || cmd.equals("ban") || cmd.equals("ban-ip")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Component.text("❌ You are not allowed to use that command!", NamedTextColor.RED));
                return;
            }
        }

        if (message.startsWith("/erpscoreboard:")) {
            event.setCancelled(true);
            String rawCmd = message.substring("/erpscoreboard:".length()).split(" ")[0];
            event.getPlayer().sendMessage(Component.text("❌ Namespace commands are disabled. Please use /" + rawCmd + " instead.", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        String command = event.getCommand().toLowerCase();
        if (command.startsWith("erpscoreboard:")) {
            event.setCancelled(true);
            event.getSender().sendMessage("❌ Namespace commands are disabled.");
        }
    }

    private long parseAmountWithSuffix(String input) throws NumberFormatException {
        if (input == null) throw new NumberFormatException("Null input");
        String clean = input.replaceAll("[()\\s]", "").toLowerCase();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("^(\\d+(\\.\\d+)?)(k|m|b|t)?$");
        java.util.regex.Matcher matcher = pattern.matcher(clean);
        if (!matcher.matches()) {
            throw new NumberFormatException("Invalid format");
        }
        double base = Double.parseDouble(matcher.group(1));
        String suffix = matcher.group(3);
        double multiplier = 1.0;
        if (suffix != null) {
            switch (suffix) {
                case "k" -> multiplier = 1000.0;
                case "m" -> multiplier = 1000000.0;
                case "b" -> multiplier = 1000000000.0;
                case "t" -> multiplier = 1000000000000.0;
            }
        }
        double result = base * multiplier;
        if (result > Long.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        if (result < 0) {
            return 0L;
        }
        return (long) result;
    }

    private boolean isCommonBlock(Material type) {
        String name = type.name();
        return name.contains("STONE") || name.contains("DEEPSLATE") || name.contains("DIRT") || 
               name.contains("GRASS") || name.contains("GRAVEL") || name.contains("SAND") || 
               name.contains("NETHERRACK") || name.contains("TUFF") || name.contains("ANDESITE") || 
               name.contains("DIORITE") || name.contains("GRANITE") || name.contains("COBBLESTONE") || 
               name.contains("TERRACOTTA") || name.contains("BASALT") || name.contains("BLACKSTONE") ||
               name.equals("CLAY") || name.equals("OBSIDIAN");
    }

    private void restoreBlocksForPlayer(Player player) {
        Location loc = player.getLocation();
        World world = loc.getWorld();
        int px = loc.getBlockX();
        int py = loc.getBlockY();
        int pz = loc.getBlockZ();
        for (int dx = -16; dx <= 16; dx++) {
            for (int dy = -16; dy <= 16; dy++) {
                for (int dz = -16; dz <= 16; dz++) {
                    Block block = world.getBlockAt(px + dx, py + dy, pz + dz);
                    player.sendBlockChange(block.getLocation(), block.getBlockData());
                }
            }
        }
    }

    private double parseCoordinate(String arg, double current) {
        if (arg.startsWith("~")) {
            if (arg.length() == 1) return current;
            return current + Double.parseDouble(arg.substring(1));
        }
        return Double.parseDouble(arg);
    }
}