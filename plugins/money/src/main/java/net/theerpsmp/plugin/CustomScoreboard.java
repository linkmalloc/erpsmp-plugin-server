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
import org.bukkit.WorldBorder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
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

    private final HashMap<UUID, Integer> timePlayedMap = new HashMap<>();
    private final HashMap<UUID, Long> erpiesMap = new HashMap<>();
    private final HashMap<UUID, Long> derpiesMap = new HashMap<>();
    private final HashMap<UUID, Integer> keysMap = new HashMap<>();
    private final HashMap<UUID, Integer> killsMap = new HashMap<>();
    private final HashMap<UUID, Integer> deathsMap = new HashMap<>();

    // Key tracking for /derpshop
    private final HashMap<UUID, Integer> regularKeysMap = new HashMap<>();
    private final HashMap<UUID, Integer> crimsonKeysMap = new HashMap<>();
    private final HashMap<UUID, Integer> echoKeysMap = new HashMap<>();
    private final HashMap<UUID, Integer> endKeysMap = new HashMap<>();
    private final HashMap<UUID, Integer> amethystKeysMap = new HashMap<>();
    private final HashMap<UUID, Boolean> hasErpPlusMap = new HashMap<>();
    private final HashMap<UUID, Boolean> hasErpProMap = new HashMap<>();
    private final HashMap<UUID, Boolean> hasErpProMaxMap = new HashMap<>();

    // Bank tracking maps
    private final HashMap<UUID, Long> bankErpiesMap = new HashMap<>();
    private final HashMap<UUID, Long> bankDerpiesMap = new HashMap<>();
    private final HashMap<UUID, Long> lastInterestTimeMap = new HashMap<>();
    private final HashMap<UUID, List<ItemStack>> bankItemsMap = new HashMap<>();

    // Apocalypse mini-game tracking maps
    private final HashMap<UUID, String> playerApocalypseDifficulty = new HashMap<>();
    private final HashMap<UUID, Integer> apocalypseZombieKillsMap = new HashMap<>();
    private final HashMap<UUID, Long> apocalypseLongestSurvivalTimeMap = new HashMap<>();
    private final HashMap<UUID, Long> apocalypseStartTimeMap = new HashMap<>();
    private final HashMap<UUID, Integer> apocalypseMaxWavesSurvivedMap = new HashMap<>();
    private final HashMap<UUID, Long> lastHordeSpawnTime = new HashMap<>();
    private final HashMap<UUID, Integer> playerApocalypseWaveMap = new HashMap<>();
    private final HashMap<UUID, java.util.Set<UUID>> playerActiveApocalypseZombies = new HashMap<>();

    // Auction variables
    private final List<AuctionListing> listings = new ArrayList<>();
    private final List<OrderRequest> orders = new ArrayList<>();
    private final HashMap<UUID, PendingSignInput> pendingSigns = new HashMap<>();
    private final HashMap<UUID, ItemStack> pendingListItems = new HashMap<>();
    private final HashMap<UUID, String> pendingOrderItemName = new HashMap<>(); // stores item name between ORDER_ITEM and ORDER_PRICE signs
    private boolean breakingCustom = false;

    // Combat tag system
    private final HashMap<UUID, Integer> combatTagTicks = new HashMap<>();
    private final java.util.Set<UUID> dualNightVisionPlayers = new java.util.HashSet<>();

    // Wand point selection system
    private final HashMap<UUID, Location> wandPoint1 = new HashMap<>();
    private final HashMap<UUID, Location> wandPoint2 = new HashMap<>();

    // Command Chest fields
    private final HashMap<Location, String> commandChests = new HashMap<>();
    private final HashMap<UUID, Location> activeCommandChestSetup = new HashMap<>();

    // Divine Flame fields
    private final HashMap<UUID, Long> chargingDivineFlame = new HashMap<>();

    // Duel Chest fields
    private final List<UUID> duelQueue = new ArrayList<>();
    private final HashMap<UUID, Integer> duelPlayerPage = new HashMap<>();
    private final HashMap<UUID, String> duelPlayerSearchQuery = new HashMap<>();
    private final HashMap<UUID, UUID> pendingDirectDuelChallenge = new HashMap<>();
    private final java.util.Set<UUID> bypassCommandChestOpCheck = new java.util.HashSet<>();

    // Team System variables
    public static class TeamData {
        public String name;
        public UUID leader;
        public String leaderName;
        public List<UUID> members = new ArrayList<>();
        public List<String> rules = new ArrayList<>();
        public Location teamHome;

        public TeamData(String name, UUID leader, String leaderName) {
            this.name = name;
            this.leader = leader;
            this.leaderName = leaderName;
            this.members.add(leader);
            this.rules.add("1. Respect all team members.");
            this.rules.add("2. Work together.");
        }
    }
    private final HashMap<String, TeamData> teams = new HashMap<>();
    private final HashMap<UUID, String> playerTeams = new HashMap<>();
    private final HashMap<UUID, String> pendingPlayerSearch = new HashMap<>();
    private final HashMap<UUID, String> pendingTeamInvites = new HashMap<>();
    private final HashMap<UUID, UUID> pendingMemberKicks = new HashMap<>();
    private final HashMap<UUID, UUID> pendingDuelInvites = new HashMap<>();

    private static class UndoBlock {
        final Location location;
        final Material material;
        final org.bukkit.block.data.BlockData blockData;
        UndoBlock(Location location, Material material, org.bukkit.block.data.BlockData blockData) {
            this.location = location;
            this.material = material;
            this.blockData = blockData.clone();
        }
    }

    private final HashMap<UUID, java.util.Stack<List<UndoBlock>>> wandUndoHistory = new HashMap<>();
    private final HashMap<UUID, java.util.Stack<List<UndoBlock>>> wandRedoHistory = new HashMap<>();

    private void recordWandAction(Player player, Location p1, Location p2) {
        if (p1 == null || p2 == null) return;
        int minX = Math.min(p1.getBlockX(), p2.getBlockX());
        int maxX = Math.max(p1.getBlockX(), p2.getBlockX());
        int minY = Math.min(p1.getBlockY(), p2.getBlockY());
        int maxY = Math.max(p1.getBlockY(), p2.getBlockY());
        int minZ = Math.min(p1.getBlockZ(), p2.getBlockZ());
        int maxZ = Math.max(p1.getBlockZ(), p2.getBlockZ());

        List<UndoBlock> blocks = new ArrayList<>();
        World world = p1.getWorld();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block b = world.getBlockAt(x, y, z);
                    blocks.add(new UndoBlock(b.getLocation(), b.getType(), b.getBlockData()));
                }
            }
        }
        wandUndoHistory.computeIfAbsent(player.getUniqueId(), k -> new java.util.Stack<>()).push(blocks);
        java.util.Stack<List<UndoBlock>> redoStack = wandRedoHistory.get(player.getUniqueId());
        if (redoStack != null) redoStack.clear();
    }

    private void recordWandActionForMove(Player player, Location srcMin, Location srcMax, Location destMin, Location destMax) {
        List<UndoBlock> blocks = new ArrayList<>();
        World world = srcMin.getWorld();
        
        // Record source region
        int sMinX = Math.min(srcMin.getBlockX(), srcMax.getBlockX());
        int sMaxX = Math.max(srcMin.getBlockX(), srcMax.getBlockX());
        int sMinY = Math.min(srcMin.getBlockY(), srcMax.getBlockY());
        int sMaxY = Math.max(srcMin.getBlockY(), srcMax.getBlockY());
        int sMinZ = Math.min(srcMin.getBlockZ(), srcMax.getBlockZ());
        int sMaxZ = Math.max(srcMin.getBlockZ(), srcMax.getBlockZ());
        for (int x = sMinX; x <= sMaxX; x++) {
            for (int y = sMinY; y <= sMaxY; y++) {
                for (int z = sMinZ; z <= sMaxZ; z++) {
                    Block b = world.getBlockAt(x, y, z);
                    blocks.add(new UndoBlock(b.getLocation(), b.getType(), b.getBlockData()));
                }
            }
        }

        // Record destination region
        int dMinX = Math.min(destMin.getBlockX(), destMax.getBlockX());
        int dMaxX = Math.max(destMin.getBlockX(), destMax.getBlockX());
        int dMinY = Math.min(destMin.getBlockY(), destMax.getBlockY());
        int dMaxY = Math.max(destMin.getBlockY(), destMax.getBlockY());
        int dMinZ = Math.min(destMin.getBlockZ(), destMax.getBlockZ());
        int dMaxZ = Math.max(destMin.getBlockZ(), destMax.getBlockZ());
        for (int x = dMinX; x <= dMaxX; x++) {
            for (int y = dMinY; y <= dMaxY; y++) {
                for (int z = dMinZ; z <= dMaxZ; z++) {
                    Block b = world.getBlockAt(x, y, z);
                    blocks.add(new UndoBlock(b.getLocation(), b.getType(), b.getBlockData()));
                }
            }
        }
        wandUndoHistory.computeIfAbsent(player.getUniqueId(), k -> new java.util.Stack<>()).push(blocks);
        java.util.Stack<List<UndoBlock>> redoStack = wandRedoHistory.get(player.getUniqueId());
        if (redoStack != null) redoStack.clear();
    }


    // Nametag and achievements system
    private final HashMap<UUID, String> activeNametags = new HashMap<>();
    private final HashMap<UUID, Boolean> killedAdminMap = new HashMap<>();
    private final HashMap<UUID, Boolean> killedDragonMap = new HashMap<>();
    private final HashMap<UUID, java.util.Set<String>> manuallyUnlockedNametags = new HashMap<>();

    private static final java.util.Set<Material> ALL_FOODS = java.util.Set.of(
        Material.APPLE, Material.BAKED_POTATO, Material.BREAD, Material.CARROT, 
        Material.COOKED_BEEF, Material.COOKED_CHICKEN, Material.COOKED_COD, 
        Material.COOKED_MUTTON, Material.COOKED_PORKCHOP, Material.COOKED_RABBIT, 
        Material.COOKED_SALMON, Material.COOKIE, Material.MELON_SLICE, 
        Material.PUMPKIN_PIE, Material.SWEET_BERRIES
    );

    // Stats for new nametags
    private final HashMap<UUID, Integer> oresMinedMap = new HashMap<>();
    private final HashMap<UUID, Integer> invisibleKillsMap = new HashMap<>();
    private final HashMap<UUID, Integer> blocksPlacedMap = new HashMap<>();
    private final HashMap<UUID, Integer> starvationDeathsMap = new HashMap<>();
    private final HashMap<UUID, HashMap<Material, Integer>> foodsEatenMap = new HashMap<>();

    // Lunge spear cooldown tracking
    private final HashMap<UUID, Long> lastLungeTime = new HashMap<>();


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

    // Floating Text tracking
    private static class BlockBackup {
        public final Material material;
        public final org.bukkit.block.data.BlockData data;
        public BlockBackup(Material material, org.bukkit.block.data.BlockData data) {
            this.material = material;
            this.data = data;
        }
    }
    private final HashMap<UUID, Location> activeFloatingTextPlacement = new HashMap<>();
    private final HashMap<UUID, BlockBackup> originalBlockState = new HashMap<>();
    private final HashMap<Location, BlockBackup> duelBlockChanges = new HashMap<>();
    private final HashMap<UUID, String> activeFloatingTextContent = new HashMap<>();
    private final HashMap<UUID, Boolean> chatSpamDisabled = new HashMap<>();
    private final HashMap<UUID, Boolean> tpaDisabled = new HashMap<>();
    private final HashMap<UUID, Boolean> voiceChatEnabled = new HashMap<>();
    private final HashMap<UUID, Boolean> musicDisabled = new HashMap<>();
    private final HashMap<UUID, String> editingGlobalBook = new HashMap<>();
    private List<String> serverRules = new ArrayList<>();
    private List<String> serverCredits = new ArrayList<>();
    private final List<UUID> xrayPlayers = new ArrayList<>();
    private final Location[] customSpawnPoints = new Location[5];
    private final HashMap<String, Location> warps = new HashMap<>();
    private final HashMap<UUID, String[]> playerHomeNames = new HashMap<>();
    private final HashMap<UUID, Boolean> renameModeActive = new HashMap<>();
    private final HashMap<UUID, Boolean> deleteModeActive = new HashMap<>();
    private final HashMap<UUID, Integer> renamingHomeIndex = new HashMap<>();

    // Shop Crates
    private final HashMap<Location, ShopCrateData> shopCrates = new HashMap<>();
    private final HashMap<UUID, Location> activeCrateSetup = new HashMap<>();
    private final HashMap<UUID, Location> activeCratePurchase = new HashMap<>();
    private final HashMap<UUID, ItemStack[]> pendingCrateItemsArray = new HashMap<>();

    // Custom Generators
    public static class GeneratorData {
        public final Location loc;
        public final String type;
        public final Inventory inventory;

        public GeneratorData(Location loc, String type, Inventory inventory) {
            this.loc = loc;
            this.type = type;
            this.inventory = inventory;
        }
    }
    private final HashMap<Location, GeneratorData> generators = new HashMap<>();

    // Shopping Cart state
    private final HashMap<UUID, Material> cartItem = new HashMap<>();
    private final HashMap<UUID, Integer> cartQuantity = new HashMap<>();
    private final HashMap<UUID, Integer> cartUnitPrice = new HashMap<>();
    private final HashMap<UUID, String> cartCategory = new HashMap<>();

    private final Random random = new Random();

    public enum SignAction { SEARCH, LIST_PRICE, SET_CRATE_PRICE, ORDER_ITEM, ORDER_PRICE, TEAM_SEARCH, BANK_DEPOSIT, BANK_WITHDRAW, SET_COMMAND_CHEST, DUEL_PLAYER_SEARCH, HOME_SEARCH, HOME_RENAME }

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

        // Load saved warps from configuration file
        if (getConfig().contains("warps")) {
            org.bukkit.configuration.ConfigurationSection section = getConfig().getConfigurationSection("warps");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    String path = "warps." + key;
                    String worldName = getConfig().getString(path + ".world");
                    double x = getConfig().getDouble(path + ".x");
                    double y = getConfig().getDouble(path + ".y");
                    double z = getConfig().getDouble(path + ".z");
                    float pitch = (float) getConfig().getDouble(path + ".pitch");
                    float yaw = (float) getConfig().getDouble(path + ".yaw");
                    World w = Bukkit.getWorld(worldName);
                    if (w != null) {
                        warps.put(key.toLowerCase(), new Location(w, x, y, z, yaw, pitch));
                    }
                }
            }
        }
        
        if (getCommand("warp") != null) getCommand("warp").setExecutor(this);
        if (getCommand("setwarp") != null) getCommand("setwarp").setExecutor(this);
        if (getCommand("store") != null) getCommand("store").setExecutor(this);
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
        if (getCommand("bank") != null) getCommand("bank").setExecutor(this);
        if (getCommand("dupe") != null) getCommand("dupe").setExecutor(this);
        if (getCommand("viewhome") != null) getCommand("viewhome").setExecutor(this);
        if (getCommand("admin") != null) getCommand("admin").setExecutor(this);
        if (getCommand("rules") != null) getCommand("rules").setExecutor(this);
        if (getCommand("item") != null) getCommand("item").setExecutor(this);
        if (getCommand("gm") != null) getCommand("gm").setExecutor(this);
        if (getCommand("gamemode") != null) getCommand("gamemode").setExecutor(this);
        if (getCommand("xray") != null) getCommand("xray").setExecutor(this);
        if (getCommand("unxray") != null) getCommand("unxray").setExecutor(this);
        if (getCommand("keys") != null) getCommand("keys").setExecutor(this);
        if (getCommand("copy") != null) getCommand("copy").setExecutor(this);
        if (getCommand("paste") != null) getCommand("paste").setExecutor(this);
        if (getCommand("dtp") != null) getCommand("dtp").setExecutor(this);
        if (getCommand("nametag") != null) getCommand("nametag").setExecutor(this);
        if (getCommand("addnametag") != null) getCommand("addnametag").setExecutor(this);
        if (getCommand("erpscoreboard") != null) getCommand("erpscoreboard").setExecutor(this);
        if (getCommand("cut") != null) getCommand("cut").setExecutor(this);
        if (getCommand("alwaysday") != null) getCommand("alwaysday").setExecutor(this);
        if (getCommand("maketeam") != null) getCommand("maketeam").setExecutor(this);
        if (getCommand("team") != null) getCommand("team").setExecutor(this);
        if (getCommand("requesteam") != null) getCommand("requesteam").setExecutor(this);
        if (getCommand("teamaccept") != null) getCommand("teamaccept").setExecutor(this);
        if (getCommand("dual") != null) getCommand("dual").setExecutor(this);
        if (getCommand("dualaccept") != null) getCommand("dualaccept").setExecutor(this);
        if (getCommand("say") != null) getCommand("say").setExecutor(this);
        if (getCommand("dualchest") != null) getCommand("dualchest").setExecutor(this);
        if (getCommand("apocalypse") != null) getCommand("apocalypse").setExecutor(this);
        if (getCommand("rank") != null) getCommand("rank").setExecutor(this);
        if (getCommand("setrank") != null) getCommand("setrank").setExecutor(this);
        loadAdminToken();
        startWebhookServer();

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                timePlayedMap.put(uuid, timePlayedMap.getOrDefault(uuid, 0) + 1);
                updateScoreboard(player);

                if (player.getWorld().getName().equalsIgnoreCase("duel")) {
                    if (!player.hasPotionEffect(PotionEffectType.NIGHT_VISION) || !dualNightVisionPlayers.contains(uuid)) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, PotionEffect.INFINITE_DURATION, 0, false, false, false));
                        dualNightVisionPlayers.add(uuid);
                    }
                } else {
                    if (dualNightVisionPlayers.contains(uuid)) {
                        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                        dualNightVisionPlayers.remove(uuid);
                    }
                }


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
                keysMap.put(uuid, keysMap.getOrDefault(uuid, 0) + 1);
                if (!chatSpamDisabled.getOrDefault(uuid, false)) {
                    player.sendMessage(Component.text("🎉 You received 1 Key reward for playing for an hour!", NamedTextColor.GOLD));
                }
            }
        }, 72000L, 72000L);

        // AFK Zone & Rank minute reward tracking task
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                boolean isAfk = player.getWorld().getName().equals("afk_zone") || player.getWorld().getName().equals("afk");
                
                long derpiesEarned = 0;
                String source = "";

                if (hasErpProMaxMap.getOrDefault(uuid, false)) { // Erp+++
                    if (isAfk) {
                        derpiesEarned = 10;
                        source = "Erp+++ (AFK Zone)";
                    } else {
                        derpiesEarned = 5;
                        source = "Erp+++ (Passive)";
                    }
                } else if (hasErpProMap.getOrDefault(uuid, false)) { // Erp++
                    if (isAfk) {
                        derpiesEarned = 2;
                        source = "Erp++ (AFK Zone)";
                    } else {
                        derpiesEarned = 1;
                        source = "Erp++ (Passive)";
                    }
                } else if (hasErpPlusMap.getOrDefault(uuid, false)) { // Erp+
                    derpiesEarned = 1;
                    source = isAfk ? "Erp+ (AFK)" : "Erp+ (Passive)";
                } else { // Normal Player
                    if (isAfk) {
                        derpiesEarned = 1;
                        source = "AFK Zone";
                    }
                }

                if (derpiesEarned > 0) {
                    derpiesMap.put(uuid, derpiesMap.getOrDefault(uuid, 0L) + derpiesEarned);
                    if (!chatSpamDisabled.getOrDefault(uuid, false)) {
                        player.sendMessage(Component.text("🎁 You received " + derpiesEarned + " Derpies (" + source + ")!", NamedTextColor.LIGHT_PURPLE));
                    }
                    updateScoreboard(player);
                    savePlayerData(player);
                }
            }
        }, 1200L, 1200L);

        // Generator minute production task
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (GeneratorData gen : generators.values()) {
                Inventory inv = gen.inventory;
                ItemStack toAdd = null;
                if (gen.type.equals("food_generator")) {
                    toAdd = new ItemStack(Material.COOKED_BEEF);
                } else if (gen.type.equals("ore_generator")) {
                    toAdd = new ItemStack(Material.DIAMOND);
                } else if (gen.type.equals("tools_generator")) {
                    toAdd = new ItemStack(getRandomToolOrArmorMaterial());
                }

                if (toAdd != null) {
                    inv.addItem(toAdd);
                    saveGenerators();
                }
            }
        }, 1200L, 1200L);

        loadShopCrates();
        loadTeams();
        loadCommandChests();
        loadGenerators();

        if (getConfig().contains("server.rules")) {
            serverRules = getConfig().getStringList("server.rules");
        } else {
            serverRules.add("&cServer Rules\n-------------------\n\nRule 1: no xray\n\nRule 2: no exploits and toolbox\n\nRule 3: no hacking");
            getConfig().set("server.rules", serverRules);
            saveConfig();
        }

        if (getConfig().contains("server.credits")) {
            serverCredits = getConfig().getStringList("server.credits");
        } else {
            serverCredits.add("&eCredits\n-------------------\n\nServer Creator: .Redtoppat208\nPlugin Developer: Antigravity");
            getConfig().set("server.credits", serverCredits);
            saveConfig();
        }
    }

    @Override
    public void onDisable() {
        stopWebhookServer();
        for (Player player : Bukkit.getOnlinePlayers()) {
            savePlayerData(player);
        }
        saveShopCrates();
        saveTeams();
        saveCommandChests();
        saveGenerators();
    }

    private void loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        String path = "players." + uuid.toString() + ".";
        
        int seconds = getConfig().getInt(path + "timePlayed", -1);
        if (seconds == -1) {
            seconds = getConfig().getInt(path + "hoursPlayed", 0) * 3600;
        }
        timePlayedMap.put(uuid, seconds);
        erpiesMap.put(uuid, getConfig().getLong(path + "erpies", 0L));
        derpiesMap.put(uuid, getConfig().getLong(path + "derpies", 0L));
        keysMap.put(uuid, getConfig().getInt(path + "keys", 0));
        killsMap.put(uuid, getConfig().getInt(path + "kills", 0));
        deathsMap.put(uuid, getConfig().getInt(path + "deaths", 0));
        
        regularKeysMap.put(uuid, getConfig().getInt(path + "regularKeys", 0));
        crimsonKeysMap.put(uuid, getConfig().getInt(path + "crimsonKeys", 0));
        echoKeysMap.put(uuid, getConfig().getInt(path + "echoKeys", 0));
        endKeysMap.put(uuid, getConfig().getInt(path + "endKeys", 0));
        amethystKeysMap.put(uuid, getConfig().getInt(path + "amethystKeys", 0));
        hasErpPlusMap.put(uuid, getConfig().getBoolean(path + "hasErpPlus", false));
        hasErpProMap.put(uuid, getConfig().getBoolean(path + "hasErpPro", false));
        hasErpProMaxMap.put(uuid, getConfig().getBoolean(path + "hasErpProMax", false));

        bankErpiesMap.put(uuid, getConfig().getLong(path + "bankErpies", 0L));
        bankDerpiesMap.put(uuid, getConfig().getLong(path + "bankDerpies", 0L));
        lastInterestTimeMap.put(uuid, getConfig().getLong(path + "lastInterestTime", 0L));
        
        List<ItemStack> bankItems = new ArrayList<>();
        if (getConfig().contains(path + "bankItems")) {
            List<?> list = getConfig().getList(path + "bankItems");
            if (list != null) {
                for (Object obj : list) {
                    if (obj instanceof ItemStack) {
                        bankItems.add((ItemStack) obj);
                    }
                }
            }
        }
        bankItemsMap.put(uuid, bankItems);

        chatSpamDisabled.put(uuid, getConfig().getBoolean(path + "chatSpamDisabled", false));
        tpaDisabled.put(uuid, getConfig().getBoolean(path + "tpaDisabled", false));
        voiceChatEnabled.put(uuid, getConfig().getBoolean(path + "voiceChatEnabled", false));
        musicDisabled.put(uuid, getConfig().getBoolean(path + "musicDisabled", false));

        activeNametags.put(uuid, getConfig().getString(path + "activeNametag", ""));
        killedAdminMap.put(uuid, getConfig().getBoolean(path + "killedAdmin", false));
        killedDragonMap.put(uuid, getConfig().getBoolean(path + "killedDragon", false));
        java.util.List<String> unlocked = getConfig().getStringList(path + "manuallyUnlockedNametags");
        manuallyUnlockedNametags.put(uuid, new java.util.HashSet<>(unlocked));

        oresMinedMap.put(uuid, getConfig().getInt(path + "oresMined", 0));
        invisibleKillsMap.put(uuid, getConfig().getInt(path + "invisibleKills", 0));
        blocksPlacedMap.put(uuid, getConfig().getInt(path + "blocksPlaced", 0));
        starvationDeathsMap.put(uuid, getConfig().getInt(path + "starvationDeaths", 0));
        apocalypseZombieKillsMap.put(uuid, getConfig().getInt(path + "apocalypseZombieKills", 0));
        apocalypseLongestSurvivalTimeMap.put(uuid, getConfig().getLong(path + "apocalypseLongestSurvival", 0L));
        apocalypseMaxWavesSurvivedMap.put(uuid, getConfig().getInt(path + "apocalypseMaxWavesSurvived", 0));

        HashMap<Material, Integer> foods = new HashMap<>();
        if (getConfig().contains(path + "foodsEaten")) {
            org.bukkit.configuration.ConfigurationSection foodSec = getConfig().getConfigurationSection(path + "foodsEaten");
            if (foodSec != null) {
                for (String key : foodSec.getKeys(false)) {
                    try {
                        Material mat = Material.valueOf(key);
                        int count = foodSec.getInt(key);
                        foods.put(mat, count);
                    } catch (Exception e) {}
                }
            }
        }
        foodsEatenMap.put(uuid, foods);

        Location[] homes = new Location[36];
        for (int i = 0; i < 36; i++) {
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

        String[] homeNames = new String[36];
        for (int i = 0; i < 36; i++) {
            String homePath = path + "homes." + i;
            if (getConfig().contains(homePath + ".name")) {
                homeNames[i] = getConfig().getString(homePath + ".name");
            } else {
                homeNames[i] = "Home " + (i + 1);
            }
        }
        playerHomeNames.put(uuid, homeNames);
    }

    private void savePlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        String path = "players." + uuid.toString() + ".";
        
        getConfig().set(path + "timePlayed", timePlayedMap.getOrDefault(uuid, 0));
        getConfig().set(path + "erpies", erpiesMap.getOrDefault(uuid, 0L));
        getConfig().set(path + "derpies", derpiesMap.getOrDefault(uuid, 0L));
        getConfig().set(path + "keys", keysMap.getOrDefault(uuid, 0));
        getConfig().set(path + "kills", killsMap.getOrDefault(uuid, 0));
        getConfig().set(path + "deaths", deathsMap.getOrDefault(uuid, 0));
        
        getConfig().set(path + "regularKeys", regularKeysMap.getOrDefault(uuid, 0));
        getConfig().set(path + "crimsonKeys", crimsonKeysMap.getOrDefault(uuid, 0));
        getConfig().set(path + "echoKeys", echoKeysMap.getOrDefault(uuid, 0));
        getConfig().set(path + "endKeys", endKeysMap.getOrDefault(uuid, 0));
        getConfig().set(path + "amethystKeys", amethystKeysMap.getOrDefault(uuid, 0));
        getConfig().set(path + "hasErpPlus", hasErpPlusMap.getOrDefault(uuid, false));
        getConfig().set(path + "hasErpPro", hasErpProMap.getOrDefault(uuid, false));
        getConfig().set(path + "hasErpProMax", hasErpProMaxMap.getOrDefault(uuid, false));

        getConfig().set(path + "bankErpies", bankErpiesMap.getOrDefault(uuid, 0L));
        getConfig().set(path + "bankDerpies", bankDerpiesMap.getOrDefault(uuid, 0L));
        getConfig().set(path + "lastInterestTime", lastInterestTimeMap.getOrDefault(uuid, 0L));
        getConfig().set(path + "bankItems", bankItemsMap.getOrDefault(uuid, new ArrayList<>()));

        getConfig().set(path + "chatSpamDisabled", chatSpamDisabled.getOrDefault(uuid, false));
        getConfig().set(path + "tpaDisabled", tpaDisabled.getOrDefault(uuid, false));
        getConfig().set(path + "voiceChatEnabled", voiceChatEnabled.getOrDefault(uuid, false));
        getConfig().set(path + "musicDisabled", musicDisabled.getOrDefault(uuid, false));

        getConfig().set(path + "activeNametag", activeNametags.getOrDefault(uuid, ""));
        getConfig().set(path + "killedAdmin", killedAdminMap.getOrDefault(uuid, false));
        getConfig().set(path + "killedDragon", killedDragonMap.getOrDefault(uuid, false));
        java.util.Set<String> unlockedSet = manuallyUnlockedNametags.get(uuid);
        if (unlockedSet != null) {
            getConfig().set(path + "manuallyUnlockedNametags", new java.util.ArrayList<>(unlockedSet));
        } else {
            getConfig().set(path + "manuallyUnlockedNametags", null);
        }

        getConfig().set(path + "oresMined", oresMinedMap.getOrDefault(uuid, 0));
        getConfig().set(path + "invisibleKills", invisibleKillsMap.getOrDefault(uuid, 0));
        getConfig().set(path + "blocksPlaced", blocksPlacedMap.getOrDefault(uuid, 0));
        getConfig().set(path + "starvationDeaths", starvationDeathsMap.getOrDefault(uuid, 0));
        getConfig().set(path + "apocalypseZombieKills", apocalypseZombieKillsMap.getOrDefault(uuid, 0));
        getConfig().set(path + "apocalypseLongestSurvival", apocalypseLongestSurvivalTimeMap.getOrDefault(uuid, 0L));
        getConfig().set(path + "apocalypseMaxWavesSurvived", apocalypseMaxWavesSurvivedMap.getOrDefault(uuid, 0));

        HashMap<Material, Integer> foods = foodsEatenMap.get(uuid);
        if (foods != null) {
            for (var entry : foods.entrySet()) {
                getConfig().set(path + "foodsEaten." + entry.getKey().name(), entry.getValue());
            }
        }

        Location[] homes = playerHomes.get(uuid);
        if (homes != null) {
            for (int i = 0; i < 36; i++) {
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

        String[] homeNames = playerHomeNames.get(uuid);
        if (homeNames != null) {
            for (int i = 0; i < 36; i++) {
                String homePath = path + "homes." + i;
                if (homeNames[i] != null) {
                    getConfig().set(homePath + ".name", homeNames[i]);
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
        if (!player.hasPlayedBefore()) {
            giveStarterGear(player);
            player.teleport(getRandomSpawnPoint());
        }
        // Play custom spawn/afk music only for the joining player
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                String worldName = player.getWorld().getName();
                if (worldName.equalsIgnoreCase("spawn") || worldName.equalsIgnoreCase("afk") || worldName.equalsIgnoreCase("afk_zone")) {
                    playLobbyMusic(player);
                }
            }
        }, 20L);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        giveStarterGear(player);
        if (!event.isBedSpawn() && !event.isAnchorSpawn()) {
            event.setRespawnLocation(getRandomSpawnPoint());
        }
        // Play custom spawn/afk music on respawn only for this player
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                String worldName = player.getWorld().getName();
                if (worldName.equalsIgnoreCase("spawn") || worldName.equalsIgnoreCase("afk") || worldName.equalsIgnoreCase("afk_zone")) {
                    playLobbyMusic(player);
                }
            }
        }, 10L);
    }

    private void giveStarterGear(Player player) {
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        inv.setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
        inv.setChestplate(new ItemStack(Material.CHAINMAIL_CHESTPLATE));
        inv.setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
        inv.setBoots(new ItemStack(Material.CHAINMAIL_BOOTS));
        inv.addItem(new ItemStack(Material.IRON_SWORD));
        inv.addItem(new ItemStack(Material.BREAD, 20));
    }

    private void checkAndTrackMinedOre(Player player, Block block) {
        if (block.getType().name().endsWith("_ORE")) {
            UUID uuid = player.getUniqueId();
            oresMinedMap.put(uuid, oresMinedMap.getOrDefault(uuid, 0) + 1);
        }
    }

    private int getFattyProgress(Player player) {
        UUID uuid = player.getUniqueId();
        HashMap<Material, Integer> foods = foodsEatenMap.getOrDefault(uuid, new HashMap<>());
        int count = 0;
        for (Material food : ALL_FOODS) {
            if (foods.getOrDefault(food, 0) >= 300) {
                count++;
            }
        }
        return count;
    }

    @EventHandler
    public void onPlayerTeleport(org.bukkit.event.player.PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (event.getFrom().getWorld() != null && event.getTo().getWorld() != null) {
            String fromWorld = event.getFrom().getWorld().getName();
            String toWorld = event.getTo().getWorld().getName();
            boolean fromLobby = fromWorld.equalsIgnoreCase("spawn") || fromWorld.equalsIgnoreCase("afk") || fromWorld.equalsIgnoreCase("afk_zone");
            boolean toLobby = toWorld.equalsIgnoreCase("spawn") || toWorld.equalsIgnoreCase("afk") || toWorld.equalsIgnoreCase("afk_zone");
            if (fromLobby && !toLobby) {
                stopLobbyMusic(player);
            }
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(org.bukkit.event.player.PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String fromWorld = event.getFrom().getName();
        boolean fromLobby = fromWorld.equalsIgnoreCase("spawn") || fromWorld.equalsIgnoreCase("afk") || fromWorld.equalsIgnoreCase("afk_zone");
        String toWorld = player.getWorld().getName();
        boolean toLobby = toWorld.equalsIgnoreCase("spawn") || toWorld.equalsIgnoreCase("afk") || toWorld.equalsIgnoreCase("afk_zone");
        if (fromLobby && !toLobby) {
            stopLobbyMusic(player);
        }
        if (fromWorld.equalsIgnoreCase("apocalypse")) {
            endApocalypseRun(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (player.getWorld().getName().equalsIgnoreCase("apocalypse")) {
            endApocalypseRun(player);
        }

        combatTagTicks.remove(uuid);
        duelQueue.remove(uuid);

        savePlayerData(player);

        timePlayedMap.remove(uuid);
        dualNightVisionPlayers.remove(uuid);
        erpiesMap.remove(uuid);
        derpiesMap.remove(uuid);
        keysMap.remove(uuid);
        killsMap.remove(uuid);
        deathsMap.remove(uuid);

        regularKeysMap.remove(uuid);
        crimsonKeysMap.remove(uuid);
        echoKeysMap.remove(uuid);
        endKeysMap.remove(uuid);
        amethystKeysMap.remove(uuid);
        hasErpPlusMap.remove(uuid);
        hasErpProMap.remove(uuid);
        hasErpProMaxMap.remove(uuid);

        bankErpiesMap.remove(uuid);
        bankDerpiesMap.remove(uuid);
        lastInterestTimeMap.remove(uuid);
        bankItemsMap.remove(uuid);

        activeNametags.remove(uuid);
        killedAdminMap.remove(uuid);
        killedDragonMap.remove(uuid);
        oresMinedMap.remove(uuid);
        invisibleKillsMap.remove(uuid);
        blocksPlacedMap.remove(uuid);
        starvationDeathsMap.remove(uuid);
        foodsEatenMap.remove(uuid);
        lastLungeTime.remove(uuid);

        String teamName = "np_" + (player.getName().length() > 13 ? player.getName().substring(0, 13) : player.getName());
        for (Player online : Bukkit.getOnlinePlayers()) {
            Team t = online.getScoreboard().getTeam(teamName);
            if (t != null) t.unregister();
        }

        PendingSignInput signPending = pendingSigns.remove(uuid);
        if (signPending != null) {
            signPending.loc.getBlock().setBlockData(signPending.originalData);
            if (signPending.item != null) {
                player.getInventory().addItem(signPending.item);
            }
        }

        if (activeFloatingTextPlacement.containsKey(uuid)) {
            Location loc = activeFloatingTextPlacement.remove(uuid);
            BlockBackup backup = originalBlockState.remove(uuid);
            if (backup != null) {
                loc.getBlock().setType(backup.material, false);
                loc.getBlock().setBlockData(backup.data, false);
            }
            activeFloatingTextContent.remove(uuid);
        }
    }

    @EventHandler
    public void onPlayerKill(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        UUID victimUUID = victim.getUniqueId();
        deathsMap.put(victimUUID, deathsMap.getOrDefault(victimUUID, 0) + 1);

        if (victim.getWorld().getName().equalsIgnoreCase("apocalypse")) {
            endApocalypseRun(victim);
        }

        if (victim.getLastDamageCause() != null && victim.getLastDamageCause().getCause() == org.bukkit.event.entity.EntityDamageEvent.DamageCause.STARVATION) {
            starvationDeathsMap.put(victimUUID, starvationDeathsMap.getOrDefault(victimUUID, 0) + 1);
        }

        Player killer = victim.getKiller();
        if (killer != null) {
            UUID killerUUID = killer.getUniqueId();
            killsMap.put(killerUUID, killsMap.getOrDefault(killerUUID, 0) + 1);

            if (killer.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                invisibleKillsMap.put(killerUUID, invisibleKillsMap.getOrDefault(killerUUID, 0) + 1);
            }

            if (victim.isOp() || victim.getName().equalsIgnoreCase(".Redtoppat208") || victim.getName().equalsIgnoreCase(".Boreas4052")) {
                killedAdminMap.put(killerUUID, true);
            }

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
        if (command.getName().equalsIgnoreCase("setrank")) {
            if (sender instanceof Player) {
                sender.sendMessage(Component.text("❌ This command can only be executed from the Server Console.", NamedTextColor.RED));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("Usage: /setrank <playername> <erp+|erp++|erp+++|none>");
                return true;
            }
            String targetName = args[0];
            String rankArg = args[1].toLowerCase();
            Player target = Bukkit.getPlayer(targetName);
            if (target == null) {
                sender.sendMessage("Error: Player '" + targetName + "' is not online.");
                return true;
            }
            UUID targetUuid = target.getUniqueId();
            hasErpPlusMap.put(targetUuid, false);
            hasErpProMap.put(targetUuid, false);
            hasErpProMaxMap.put(targetUuid, false);
            String rankLabel = "None";
            switch (rankArg) {
                case "erp+":
                    hasErpPlusMap.put(targetUuid, true);
                    rankLabel = "Erp+";
                    break;
                case "erp++":
                    hasErpProMap.put(targetUuid, true);
                    rankLabel = "Erp++";
                    break;
                case "erp+++":
                    hasErpProMaxMap.put(targetUuid, true);
                    rankLabel = "Erp+++";
                    break;
                case "none":
                    break;
                default:
                    sender.sendMessage("Error: Invalid rank. Use: erp+, erp++, erp+++, or none");
                    return true;
            }
            savePlayerData(target);
            updateScoreboard(target);
            sender.sendMessage("Success: Set " + target.getName() + "'s rank to " + rankLabel);
            target.sendMessage(Component.text("🌟 Your rank has been updated to " + rankLabel + "!", NamedTextColor.GOLD));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this system command!");
            return true;
        }

        if (command.getName().equalsIgnoreCase("setwarp")) {
            if (!player.getName().equalsIgnoreCase(".Redtoppat208") && !player.getName().equalsIgnoreCase(".RedToppat208")) {
                player.sendMessage(Component.text("❌ Only .RedToppat208 can use this command!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 1) {
                player.sendMessage(Component.text("❌ Usage: /setwarp <name>", NamedTextColor.RED));
                return true;
            }
            String warpName = args[0].toLowerCase();
            Location loc = player.getLocation();
            warps.put(warpName, loc);
            
            String path = "warps." + warpName;
            getConfig().set(path + ".world", loc.getWorld().getName());
            getConfig().set(path + ".x", loc.getX());
            getConfig().set(path + ".y", loc.getY());
            getConfig().set(path + ".z", loc.getZ());
            getConfig().set(path + ".pitch", (double) loc.getPitch());
            getConfig().set(path + ".yaw", (double) loc.getYaw());
            saveConfig();
            
            player.sendMessage(Component.text("✅ Warp '" + args[0] + "' set at your location!", NamedTextColor.GREEN));
            return true;
        }

        if (command.getName().equalsIgnoreCase("warp")) {
            if (args.length < 1) {
                player.sendMessage(Component.text("❌ Usage: /warp <name>", NamedTextColor.RED));
                return true;
            }
            String warpName = args[0].toLowerCase();
            if (!warps.containsKey(warpName)) {
                player.sendMessage(Component.text("❌ Warp '" + args[0] + "' does not exist!", NamedTextColor.RED));
                return true;
            }
            player.teleport(warps.get(warpName));
            player.sendMessage(Component.text("✨ Teleported to warp '" + args[0] + "'!", NamedTextColor.GREEN));
            return true;
        }

        if (command.getName().equalsIgnoreCase("store")) {
            player.sendMessage(Component.text("\"theerpsmp.net\" Copy this and paste in you preferred browser to open the store", NamedTextColor.YELLOW));
            return true;
        }

        if (command.getName().equalsIgnoreCase("say")) {
            if (!player.getName().equalsIgnoreCase(".Redtoppat208")) {
                player.sendMessage(Component.text("❌ You do not have permission to use this command!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 1) {
                player.sendMessage(Component.text("❌ Usage: /say <message>", NamedTextColor.RED));
                return true;
            }
            String rawMessage = String.join(" ", args);
            String formattedMessage = rawMessage.replace("&", "§");
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.sendTitle(formattedMessage, "", 10, 70, 20);
            }
            return true;
        }

        // /rank <erp+|erp++|erp+++> <playername>  — restricted to trusted admins
        if (command.getName().equalsIgnoreCase("rank")) {
            String senderName = player.getName();
            boolean isTrusted = senderName.equals(".Redtoppat208") || senderName.equals(".Boreas4052");
            if (!isTrusted) {
                player.sendMessage(Component.text("❌ You do not have permission to use this command.", NamedTextColor.RED));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(Component.text("❌ Usage: /rank <erp+|erp++|erp+++> <playername>", NamedTextColor.RED));
                return true;
            }
            String rankArg = args[0].toLowerCase();
            String targetName = args[1];
            Player target = Bukkit.getPlayer(targetName);
            if (target == null) {
                player.sendMessage(Component.text("❌ Player '" + targetName + "' is not online.", NamedTextColor.RED));
                return true;
            }
            UUID targetUuid = target.getUniqueId();
            // Clear all ranks first, then apply the chosen one
            hasErpPlusMap.put(targetUuid, false);
            hasErpProMap.put(targetUuid, false);
            hasErpProMaxMap.put(targetUuid, false);
            String rankLabel;
            switch (rankArg) {
                case "erp+":
                    hasErpPlusMap.put(targetUuid, true);
                    rankLabel = "Erp+";
                    break;
                case "erp++":
                    hasErpProMap.put(targetUuid, true);
                    rankLabel = "Erp++";
                    break;
                case "erp+++":
                    hasErpProMaxMap.put(targetUuid, true);
                    rankLabel = "Erp+++";
                    break;
                default:
                    player.sendMessage(Component.text("❌ Invalid rank. Use: erp+, erp++, or erp+++", NamedTextColor.RED));
                    return true;
            }
            savePlayerData(target);
            updateScoreboard(target);
            player.sendMessage(Component.text("✅ Set " + target.getName() + "'s rank to " + rankLabel + "!", NamedTextColor.GREEN));
            target.sendMessage(Component.text("🌟 You have been given the " + rankLabel + " rank!", NamedTextColor.GOLD));
            return true;
        }

        if (command.getName().equalsIgnoreCase("maketeam")) {
            UUID uuid = player.getUniqueId();
            if (playerTeams.containsKey(uuid)) {
                player.sendMessage(Component.text("❌ You are already in a team: " + teams.get(playerTeams.get(uuid)).name, NamedTextColor.RED));
                return true;
            }
            if (args.length < 1) {
                player.sendMessage(Component.text("❌ Usage: /maketeam <teamname>", NamedTextColor.RED));
                return true;
            }
            String teamName = args[0];
            String lowercaseName = teamName.toLowerCase();
            if (teams.containsKey(lowercaseName)) {
                player.sendMessage(Component.text("❌ A team with that name already exists!", NamedTextColor.RED));
                return true;
            }
            if (!teamName.matches("^[a-zA-Z0-9_]{3,16}$")) {
                player.sendMessage(Component.text("❌ Team name must be alphanumeric, between 3 and 16 characters, and can include underscores.", NamedTextColor.RED));
                return true;
            }
            
            TeamData data = new TeamData(teamName, uuid, player.getName());
            teams.put(lowercaseName, data);
            playerTeams.put(uuid, lowercaseName);
            saveTeams();
            player.sendMessage(Component.text("🎉 Team \"" + teamName + "\" created successfully!", NamedTextColor.GREEN));
            return true;
        }

        if (command.getName().equalsIgnoreCase("team")) {
            openTeamGui(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("requesteam")) {
            UUID uuid = player.getUniqueId();
            String teamNameLower = playerTeams.get(uuid);
            if (teamNameLower == null) {
                player.sendMessage(Component.text("❌ You are not in a team!", NamedTextColor.RED));
                return true;
            }
            TeamData data = teams.get(teamNameLower);
            if (!uuid.equals(data.leader)) {
                player.sendMessage(Component.text("❌ Only the team leader can invite players!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 1) {
                player.sendMessage(Component.text("❌ Usage: /requesteam <playername>", NamedTextColor.RED));
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                player.sendMessage(Component.text("❌ Player not found or is offline.", NamedTextColor.RED));
                return true;
            }
            UUID targetUUID = target.getUniqueId();
            if (playerTeams.containsKey(targetUUID)) {
                player.sendMessage(Component.text("❌ That player is already in a team!", NamedTextColor.RED));
                return true;
            }
            pendingTeamInvites.put(targetUUID, teamNameLower);
            player.sendMessage(Component.text("✉️ Invitation sent to " + target.getName() + "!", NamedTextColor.GREEN));
            target.sendMessage(Component.text("✉️ You have been invited to join the team \"" + data.name + "\"!", NamedTextColor.GOLD));
            target.sendMessage(Component.text("👉 Type /teamaccept to join the team (expires in 60s).", NamedTextColor.YELLOW));
            
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (pendingTeamInvites.containsKey(targetUUID) && pendingTeamInvites.get(targetUUID).equals(teamNameLower)) {
                    pendingTeamInvites.remove(targetUUID);
                    if (target.isOnline()) {
                        target.sendMessage(Component.text("⏳ Team invitation to \"" + data.name + "\" has expired.", NamedTextColor.GRAY));
                    }
                    if (player.isOnline()) {
                        player.sendMessage(Component.text("⏳ Invitation to " + target.getName() + " has expired.", NamedTextColor.GRAY));
                    }
                }
            }, 1200L);
            return true;
        }

        if (command.getName().equalsIgnoreCase("teamaccept")) {
            UUID uuid = player.getUniqueId();
            String teamNameLower = pendingTeamInvites.remove(uuid);
            if (teamNameLower == null) {
                player.sendMessage(Component.text("❌ You don't have any pending team invitations!", NamedTextColor.RED));
                return true;
            }
            TeamData data = teams.get(teamNameLower);
            if (data == null) {
                player.sendMessage(Component.text("❌ That team no longer exists.", NamedTextColor.RED));
                return true;
            }
            if (playerTeams.containsKey(uuid)) {
                player.sendMessage(Component.text("❌ You are already in a team!", NamedTextColor.RED));
                return true;
            }
            data.members.add(uuid);
            playerTeams.put(uuid, teamNameLower);
            saveTeams();
            
            player.sendMessage(Component.text("🎉 You joined the team \"" + data.name + "\"!", NamedTextColor.GREEN));
            for (UUID memberUUID : data.members) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null && member.isOnline() && !memberUUID.equals(uuid)) {
                    member.sendMessage(Component.text("👋 " + player.getName() + " has joined the team!", NamedTextColor.GREEN));
                }
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("dual")) {
            if (args.length < 1) {
                player.sendMessage(Component.text("❌ Usage: /dual <playername>", NamedTextColor.RED));
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                player.sendMessage(Component.text("❌ Player not found or is offline.", NamedTextColor.RED));
                return true;
            }
            if (target.getUniqueId().equals(player.getUniqueId())) {
                player.sendMessage(Component.text("❌ You cannot duel yourself!", NamedTextColor.RED));
                return true;
            }
            UUID targetUUID = target.getUniqueId();
            pendingDuelInvites.put(targetUUID, player.getUniqueId());
            player.sendMessage(Component.text("⚔️ Duel challenge sent to " + target.getName() + "!", NamedTextColor.GREEN));
            target.sendMessage(Component.text("⚔️ " + player.getName() + " has challenged you to a duel!", NamedTextColor.GOLD));
            target.sendMessage(Component.text("👉 Type /dualaccept to accept (expires in 60s).", NamedTextColor.YELLOW));
            
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (pendingDuelInvites.containsKey(targetUUID) && pendingDuelInvites.get(targetUUID).equals(player.getUniqueId())) {
                    pendingDuelInvites.remove(targetUUID);
                    if (target.isOnline()) {
                        target.sendMessage(Component.text("⏳ Duel challenge from " + player.getName() + " has expired.", NamedTextColor.GRAY));
                    }
                    if (player.isOnline()) {
                        player.sendMessage(Component.text("⏳ Duel challenge to " + target.getName() + " has expired.", NamedTextColor.GRAY));
                    }
                }
            }, 1200L);
            return true;
        }

        if (command.getName().equalsIgnoreCase("dualaccept")) {
            UUID uuid = player.getUniqueId();
            UUID challengerUUID = pendingDuelInvites.remove(uuid);
            if (challengerUUID == null) {
                player.sendMessage(Component.text("❌ You don't have any pending duel challenges!", NamedTextColor.RED));
                return true;
            }
            Player challenger = Bukkit.getPlayer(challengerUUID);
            if (challenger == null || !challenger.isOnline()) {
                player.sendMessage(Component.text("❌ The challenger is no longer online.", NamedTextColor.RED));
                return true;
            }
            
            World dualWorld = Bukkit.getWorld("duel");
            if (dualWorld == null) {
                WorldCreator creator = new WorldCreator("duel");
                creator.environment(World.Environment.NORMAL);
                dualWorld = Bukkit.createWorld(creator);
            }
            if (dualWorld != null) {
                dualWorld.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
                dualWorld.setTime(6000L);

                org.bukkit.WorldBorder border = dualWorld.getWorldBorder();
                border.setCenter(0.0, 0.0);
                border.setSize(50.0);

                cleanupDuelArena(dualWorld);

                double y1 = dualWorld.getHighestBlockYAt(-15, 0);
                double y2 = dualWorld.getHighestBlockYAt(15, 0);

                Location loc1 = new Location(dualWorld, -15.5, y1 + 1.0, 0.5, -90f, 0f);
                Location loc2 = new Location(dualWorld, 15.5, y2 + 1.0, 0.5, 90f, 0f);
                challenger.teleport(loc1);
                player.teleport(loc2);
                
                challenger.sendMessage(Component.text("⚔️ Duel started! Good luck!", NamedTextColor.GOLD));
                player.sendMessage(Component.text("⚔️ Duel started! Good luck!", NamedTextColor.GOLD));
            } else {
                player.sendMessage(Component.text("❌ Failed to load the duel arena.", NamedTextColor.RED));
                challenger.sendMessage(Component.text("❌ Failed to load the duel arena.", NamedTextColor.RED));
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("dualchest")) {
            if (player.isOp() || bypassCommandChestOpCheck.contains(player.getUniqueId())) {
                openDuelChestGui(player);
            } else {
                player.sendMessage(Component.text("❌ You do not have permission to run this command!", NamedTextColor.RED));
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("alwaysday")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(Component.text("❌ Usage: /alwaysday (dimension) (true/false)", NamedTextColor.RED));
                return true;
            }
            String dimName = args[0];
            World world = Bukkit.getWorld(dimName);
            if (world == null) {
                if (dimName.equalsIgnoreCase("overworld")) {
                    world = Bukkit.getWorlds().stream()
                            .filter(w -> w.getEnvironment() == World.Environment.NORMAL)
                            .findFirst().orElse(null);
                } else if (dimName.equalsIgnoreCase("nether")) {
                    world = Bukkit.getWorlds().stream()
                            .filter(w -> w.getEnvironment() == World.Environment.NETHER)
                            .findFirst().orElse(null);
                } else if (dimName.equalsIgnoreCase("end")) {
                    world = Bukkit.getWorlds().stream()
                            .filter(w -> w.getEnvironment() == World.Environment.THE_END)
                            .findFirst().orElse(null);
                }
            }
            if (world == null) {
                player.sendMessage(Component.text("❌ Dimension not found: '" + dimName + "'", NamedTextColor.RED));
                return true;
            }
            boolean enable = Boolean.parseBoolean(args[1]);
            if (enable) {
                world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
                world.setTime(6000L);
                player.sendMessage(Component.text("☀️ Always Day enabled for dimension '" + world.getName() + "'. Time locked to noon.", NamedTextColor.GREEN));
            } else {
                world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, true);
                player.sendMessage(Component.text("🌙 Always Day disabled for dimension '" + world.getName() + "'. Daylight cycle resumed.", NamedTextColor.YELLOW));
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("addnametag")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You do not have permission to use this command!", NamedTextColor.RED));
                return true;
            }
            if (args.length == 0) {
                player.sendMessage(Component.text("❌ Usage: /addnametag (nametag/all) [player]", NamedTextColor.RED));
                return true;
            }
            Player target = player;
            String tagArg = "";
            
            Player possibleTarget = Bukkit.getPlayer(args[args.length - 1]);
            if (possibleTarget != null) {
                target = possibleTarget;
                if (args.length > 1) {
                    tagArg = String.join(" ", java.util.Arrays.copyOfRange(args, 0, args.length - 1));
                } else {
                    player.sendMessage(Component.text("❌ Usage: /addnametag (nametag/all) [player]", NamedTextColor.RED));
                    return true;
                }
            } else {
                tagArg = String.join(" ", args);
            }
            
            String matchedTag = null;
            String[] availableTags = {"Berry Lover", "Combat Master", "Admin killer", "Richie Boi", "Dragon Slayer", "The Miner", "Silent Assassin", "The Builder", "Fatty", "Skin and Bones"};
            for (String t : availableTags) {
                if (t.equalsIgnoreCase(tagArg) || t.replace(" ", "").equalsIgnoreCase(tagArg)) {
                    matchedTag = t;
                    break;
                }
            }
            
            UUID targetUUID = target.getUniqueId();
            manuallyUnlockedNametags.computeIfAbsent(targetUUID, k -> new java.util.HashSet<>());
            if (tagArg.equalsIgnoreCase("all")) {
                manuallyUnlockedNametags.get(targetUUID).addAll(java.util.Arrays.asList(availableTags));
                player.sendMessage(Component.text("✅ Unlocked all nametags for " + target.getName(), NamedTextColor.GREEN));
                target.sendMessage(Component.text("🎉 All nametags have been unlocked for you!", NamedTextColor.GOLD));
            } else if (matchedTag != null) {
                manuallyUnlockedNametags.get(targetUUID).add(matchedTag);
                player.sendMessage(Component.text("✅ Unlocked nametag '" + matchedTag + "' for " + target.getName(), NamedTextColor.GREEN));
                target.sendMessage(Component.text("🎉 Nametag '" + matchedTag + "' has been unlocked for you!", NamedTextColor.GOLD));
            } else {
                player.sendMessage(Component.text("❌ Unknown nametag! Available: Berry Lover, Combat Master, Admin killer, Richie Boi, Dragon Slayer, The Miner, Silent Assassin, The Builder, Fatty, Skin and Bones", NamedTextColor.RED));
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("erpscoreboard")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You do not have permission to use this command!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(Component.text("❌ Usage: /erpscoreboard (t/e/d/k/ki/de) (add/remove/set/reset) (amount) [player]", NamedTextColor.RED));
                return true;
            }
            String statArg = args[0].toLowerCase();
            String opArg = args[1].toLowerCase();
            long amount = 0;
            Player target = player;
            int nextArgIdx = 2;
            
            if (!opArg.equals("reset")) {
                if (args.length < 3) {
                    player.sendMessage(Component.text("❌ Usage: /erpscoreboard <t/e/d/k/ki/de> <add/remove/set> <amount> [player]", NamedTextColor.RED));
                    return true;
                }
                try {
                    amount = Long.parseLong(args[2]);
                    nextArgIdx = 3;
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("❌ Invalid amount format!", NamedTextColor.RED));
                    return true;
                }
            }
            
            if (args.length > nextArgIdx) {
                target = Bukkit.getPlayer(args[nextArgIdx]);
                if (target == null) {
                    player.sendMessage(Component.text("❌ Target player not found!", NamedTextColor.RED));
                    return true;
                }
            }
            
            UUID targetUUID = target.getUniqueId();
            long currentValue = 0;
            switch (statArg) {
                case "t", "h" -> currentValue = timePlayedMap.getOrDefault(targetUUID, 0);
                case "e" -> currentValue = erpiesMap.getOrDefault(targetUUID, 0L);
                case "d" -> currentValue = derpiesMap.getOrDefault(targetUUID, 0L);
                case "k" -> currentValue = keysMap.getOrDefault(targetUUID, 0);
                case "ki" -> currentValue = killsMap.getOrDefault(targetUUID, 0);
                case "de" -> currentValue = deathsMap.getOrDefault(targetUUID, 0);
                default -> {
                    player.sendMessage(Component.text("❌ Unknown stat '" + statArg + "'! Use: t, e, d, k, ki, de", NamedTextColor.RED));
                    return true;
                }
            }

            long newValue = currentValue;
            switch (opArg) {
                case "add" -> newValue = currentValue + amount;
                case "remove" -> newValue = currentValue - amount;
                case "set" -> newValue = amount;
                case "reset" -> newValue = 0;
                default -> {
                    player.sendMessage(Component.text("❌ Unknown operation '" + opArg + "'! Use: add, remove, set, reset", NamedTextColor.RED));
                    return true;
                }
            }
            
            if (statArg.equals("t") || statArg.equals("h") || statArg.equals("k") || statArg.equals("ki") || statArg.equals("de")) {
                if (newValue < 0) newValue = 0;
                if (newValue > Integer.MAX_VALUE) newValue = Integer.MAX_VALUE;
                int intVal = (int) newValue;
                switch (statArg) {
                    case "t", "h" -> timePlayedMap.put(targetUUID, intVal);
                    case "k" -> keysMap.put(targetUUID, intVal);
                    case "ki" -> killsMap.put(targetUUID, intVal);
                    case "de" -> deathsMap.put(targetUUID, intVal);
                }
            } else {
                if (newValue < 0) newValue = 0;
                switch (statArg) {
                    case "e" -> erpiesMap.put(targetUUID, newValue);
                    case "d" -> derpiesMap.put(targetUUID, newValue);
                }
            }

            updateScoreboard(target);
            player.sendMessage(Component.text("✅ Successfully updated " + statArg + " for " + target.getName() + " to " + newValue, NamedTextColor.GREEN));
            target.sendMessage(Component.text("⚙️ Your scoreboard stat (" + statArg + ") has been updated to " + newValue, NamedTextColor.GOLD));
            savePlayerData(target);
            return true;
        }

        if (command.getName().equalsIgnoreCase("sell")) {
            Inventory sellInv = Bukkit.createInventory(null, 27, Component.text("Drop items here to Sell"));
            player.openInventory(sellInv);
            return true;
        }

        if (command.getName().equalsIgnoreCase("apocalypse")) {
            openApocalypseGui(player);
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
            if (!player.isOp() && !pName.equalsIgnoreCase(".Redtoppat208") && !pName.equalsIgnoreCase(".Boreas4052")) {
                player.sendMessage(Component.text("❌ You do not have permission to use this command!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(Component.text("❌ Usage: /player (ban/unban) (playername) (optional:number of days of ban)", NamedTextColor.RED));
                return true;
            }
            String action = args[0].toLowerCase();
            String targetName = args[1];

            if (action.equals("ban")) {
                org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
                if (offlineTarget.isOp()) {
                    player.sendMessage(Component.text("❌ You cannot ban another Operator!", NamedTextColor.RED));
                    return true;
                }

                java.util.Date expiration = null;
                if (args.length >= 3) {
                    try {
                        double days = Double.parseDouble(args[2]);
                        long ms = (long) (days * 24 * 60 * 60 * 1000);
                        expiration = new java.util.Date(System.currentTimeMillis() + ms);
                    } catch (NumberFormatException e) {
                        player.sendMessage(Component.text("❌ Invalid number of days!", NamedTextColor.RED));
                        return true;
                    }
                }

                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(targetName, "Banned by administrator", expiration, null);
                Player target = Bukkit.getPlayer(targetName);
                if (target != null) {
                    target.kick(Component.text("You have been banned from the server."));
                }
                if (expiration != null) {
                    player.sendMessage(Component.text("✅ Banned player " + targetName + " for " + args[2] + " days", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("✅ Permanently banned player " + targetName, NamedTextColor.GREEN));
                }
            } else if (action.equals("unban")) {
                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).pardon(targetName);
                player.sendMessage(Component.text("✅ Unbanned player " + targetName, NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("❌ Usage: /player (ban/unban) (playername) (optional:number of days of ban)", NamedTextColor.RED));
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("bh")) {
            openBountyHunter(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("nametag")) {
            openNametagMainMenu(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("bank")) {
            openBankGui(player);
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
            if (args.length == 0 || !args[0].equals("05132014!Cc")) {
                player.sendMessage(Component.text("❌ Incorrect password!", NamedTextColor.RED));
                return true;
            }
            args = java.util.Arrays.copyOfRange(args, 1, args.length);

            if (args.length < 1) {
                openCustomItemsAdminPanel(player);
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
                case "shovel", "echo_shovel" -> item = createEchoShovel();
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
                case "end_crate" -> item = createEndCrate();
                case "amethyst_crate" -> item = createAmethystCrate();
                case "orbital_strike" -> item = createOrbitalStrike();
                case "wand" -> item = createWand();
                case "lunge_spear" -> item = createLungeSpear();
                case "echo_key" -> item = createEchoKey();
                case "crimson_key" -> item = createCrimsonKey();
                case "end_key" -> item = createEndKey();
                case "amethyst_key" -> item = createAmethystKey();
                case "npc_egg" -> item = createNpcEgg();
                case "floating_text" -> item = createFloatingTextItem();
                case "command_chest" -> item = createCommandChest();
                case "divine_flame" -> item = createDivineFlame();
                case "food_generator" -> item = createFoodGeneratorItem();
                case "ore_generator" -> item = createOreGeneratorItem();
                case "tools_generator" -> item = createToolsGeneratorItem();
                default -> {
                    player.sendMessage(Component.text("❌ Unknown item type! Use: pickaxe, shovel, axe, bow, stick, crate, sword, pickaxe_lerp, mace, echo_sword, gateway, echo_crate, crimson_crate, key_crate, end_crate, amethyst_crate, orbital_strike, wand, lunge_spear, echo_key, crimson_key, end_key, amethyst_key, npc_egg, floating_text, command_chest, divine_flame, food_generator, ore_generator, tools_generator", NamedTextColor.RED));
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
            openUnifiedHomeGui(player);
            return true;
        }

        // --- /home ---
        if (command.getName().equalsIgnoreCase("home")) {
            openUnifiedHomeGui(player);
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
        // --- /cut (OP only) ---
        if (command.getName().equalsIgnoreCase("cut")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }

            UUID uuid = player.getUniqueId();
            Location p1 = wandPoint1.get(uuid);
            Location p2 = wandPoint2.get(uuid);
            if (p1 == null || p2 == null) {
                player.sendMessage(Component.text("❌ Select two points with the Wand first!", NamedTextColor.RED));
                return true;
            }

            int x1 = p1.getBlockX();
            int y1 = p1.getBlockY();
            int z1 = p1.getBlockZ();
            int x2 = p2.getBlockX();
            int y2 = p2.getBlockY();
            int z2 = p2.getBlockZ();

            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            int minY = Math.min(y1, y2);
            int maxY = Math.max(y1, y2);
            int minZ = Math.min(z1, z2);
            int maxZ = Math.max(z1, z2);

            World world = player.getWorld();
            int blocksDestroyed = 0;

            // Record undo history
            recordWandAction(player, p1, p2);

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        Block block = world.getBlockAt(x, y, z);
                        if (block.getType() != Material.AIR) {
                            block.setType(Material.AIR, false);
                            blocksDestroyed++;
                        }
                    }
                }
            }

            player.sendMessage(Component.text("✅ Successfully cut " + blocksDestroyed + " blocks.", NamedTextColor.GREEN));
            return true;
        }

        // --- /wandfill (OP only) ---
        if (command.getName().equalsIgnoreCase("wandfill")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 1) {
                player.sendMessage(Component.text("❌ Usage: /wandfill (material) (optional:hollow/replace) (if replace:material)", NamedTextColor.RED));
                return true;
            }

            UUID uuid = player.getUniqueId();
            Location p1 = wandPoint1.get(uuid);
            Location p2 = wandPoint2.get(uuid);
            if (p1 == null || p2 == null) {
                player.sendMessage(Component.text("❌ Select two points with the Wand first!", NamedTextColor.RED));
                return true;
            }

            Material material = Material.matchMaterial(args[0]);
            if (material == null || !material.isBlock()) {
                player.sendMessage(Component.text("❌ Invalid block material: " + args[0], NamedTextColor.RED));
                return true;
            }

            boolean hollow = false;
            boolean replaceMode = false;
            Material replaceMaterial = null;

            if (args.length > 1) {
                if (args[1].equalsIgnoreCase("hollow")) {
                    hollow = true;
                } else if (args[1].equalsIgnoreCase("replace")) {
                    if (args.length < 3) {
                        player.sendMessage(Component.text("❌ Usage: /wandfill (material) replace (replaceMaterial)", NamedTextColor.RED));
                        return true;
                    }
                    replaceMode = true;
                    replaceMaterial = Material.matchMaterial(args[2]);
                    if (replaceMaterial == null || !replaceMaterial.isBlock()) {
                        player.sendMessage(Component.text("❌ Invalid replace material: " + args[2], NamedTextColor.RED));
                        return true;
                    }
                } else {
                    player.sendMessage(Component.text("❌ Usage: /wandfill (material) [hollow|replace] [if replace: material]", NamedTextColor.RED));
                    return true;
                }
            }

            int x1 = p1.getBlockX();
            int y1 = p1.getBlockY();
            int z1 = p1.getBlockZ();
            int x2 = p2.getBlockX();
            int y2 = p2.getBlockY();
            int z2 = p2.getBlockZ();

            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            int minY = Math.min(y1, y2);
            int maxY = Math.max(y1, y2);
            int minZ = Math.min(z1, z2);
            int maxZ = Math.max(z1, z2);

            World world = player.getWorld();
            int blocksChanged = 0;

            // Record undo history
            recordWandAction(player, p1, p2);

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        Block block = world.getBlockAt(x, y, z);
                        if (replaceMode) {
                            if (block.getType() == replaceMaterial) {
                                block.setType(material, false);
                                blocksChanged++;
                            }
                        } else if (hollow) {
                            if (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ) {
                                if (block.getType() != material) {
                                    block.setType(material, false);
                                    blocksChanged++;
                                }
                            } else {
                                if (block.getType() != Material.AIR) {
                                    block.setType(Material.AIR, false);
                                    blocksChanged++;
                                }
                            }
                        } else {
                            if (block.getType() != material) {
                                block.setType(material, false);
                                blocksChanged++;
                            }
                        }
                    }
                }
            }

            String msg = "✅ Successfully filled " + blocksChanged + " blocks with " + material.name();
            if (replaceMode) {
                msg += " (replaced " + replaceMaterial.name() + ").";
            } else if (hollow) {
                msg += " (hollow).";
            } else {
                msg += ".";
            }
            player.sendMessage(Component.text(msg, NamedTextColor.GREEN));
            return true;
        }

        // --- /move (OP only) ---
        if (command.getName().equalsIgnoreCase("move")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(Component.text("❌ Usage: /move (up/down/north/south/east/west) (amount of blocks)", NamedTextColor.RED));
                return true;
            }

            UUID uuid = player.getUniqueId();
            Location p1 = wandPoint1.get(uuid);
            Location p2 = wandPoint2.get(uuid);
            if (p1 == null || p2 == null) {
                player.sendMessage(Component.text("❌ Select two points with the Wand first!", NamedTextColor.RED));
                return true;
            }

            String dir = args[0].toLowerCase();
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
                if (amount <= 0) {
                    player.sendMessage(Component.text("❌ Amount must be a positive integer!", NamedTextColor.RED));
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("❌ Invalid amount of blocks!", NamedTextColor.RED));
                return true;
            }

            int dx = 0, dy = 0, dz = 0;
            switch (dir) {
                case "up" -> dy = amount;
                case "down" -> dy = -amount;
                case "north" -> dz = -amount;
                case "south" -> dz = amount;
                case "east" -> dx = amount;
                case "west" -> dx = -amount;
                default -> {
                    player.sendMessage(Component.text("❌ Invalid direction! Use: up, down, north, south, east, west", NamedTextColor.RED));
                    return true;
                }
            }

            int x1 = p1.getBlockX();
            int y1 = p1.getBlockY();
            int z1 = p1.getBlockZ();
            int x2 = p2.getBlockX();
            int y2 = p2.getBlockY();
            int z2 = p2.getBlockZ();

            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            int minY = Math.min(y1, y2);
            int maxY = Math.max(y1, y2);
            int minZ = Math.min(z1, z2);
            int maxZ = Math.max(z1, z2);

            World world = player.getWorld();

            // Record undo history for move
            recordWandActionForMove(player, p1, p2, p1.clone().add(dx, dy, dz), p2.clone().add(dx, dy, dz));

            // 1. Copy region
            List<CopiedBlock> tempBuffer = new ArrayList<>();
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        Block block = world.getBlockAt(x, y, z);
                        tempBuffer.add(new CopiedBlock(x - minX, y - minY, z - minZ, block.getType(), block.getBlockData().clone()));
                    }
                }
            }

            // 2. Clear region
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        world.getBlockAt(x, y, z).setType(Material.AIR, false);
                    }
                }
            }

            // 3. Paste at new coordinates
            int targetMinX = minX + dx;
            int targetMinY = minY + dy;
            int targetMinZ = minZ + dz;
            for (CopiedBlock cb : tempBuffer) {
                int targetX = targetMinX + cb.offsetX;
                int targetY = targetMinY + cb.offsetY;
                int targetZ = targetMinZ + cb.offsetZ;
                Block block = world.getBlockAt(targetX, targetY, targetZ);
                block.setType(cb.material, false);
                block.setBlockData(cb.blockData, false);
            }

            // 4. Move selection points
            wandPoint1.put(uuid, p1.clone().add(dx, dy, dz));
            wandPoint2.put(uuid, p2.clone().add(dx, dy, dz));

            player.sendMessage(Component.text("✅ Successfully moved selection " + amount + " blocks " + dir + ".", NamedTextColor.GREEN));
            return true;
        }

        // --- /wandcircle (OP only) ---
        if (command.getName().equalsIgnoreCase("wandcircle")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 1) {
                player.sendMessage(Component.text("❌ Usage: /wandcircle (material) (optional:semicircle)", NamedTextColor.RED));
                return true;
            }

            UUID uuid = player.getUniqueId();
            Location p1 = wandPoint1.get(uuid);
            Location p2 = wandPoint2.get(uuid);
            if (p1 == null || p2 == null) {
                player.sendMessage(Component.text("❌ Select two points with the Wand first!", NamedTextColor.RED));
                return true;
            }

            Material material = Material.matchMaterial(args[0]);
            if (material == null || !material.isBlock()) {
                player.sendMessage(Component.text("❌ Invalid block material: " + args[0], NamedTextColor.RED));
                return true;
            }

            boolean semi = args.length > 1 && args[1].equalsIgnoreCase("semicircle");

            int x1 = p1.getBlockX();
            int y1 = p1.getBlockY();
            int z1 = p1.getBlockZ();
            int x2 = p2.getBlockX();
            int y2 = p2.getBlockY();
            int z2 = p2.getBlockZ();

            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            int minY = Math.min(y1, y2);
            int maxY = Math.max(y1, y2);
            int minZ = Math.min(z1, z2);
            int maxZ = Math.max(z1, z2);

            int sizeX = maxX - minX + 1;
            int sizeY = maxY - minY + 1;
            int sizeZ = maxZ - minZ + 1;

            World world = player.getWorld();

            // Record undo history
            recordWandAction(player, p1, p2);

            // Determine plane of the circle/semicircle
            // Planes:
            // 0: X-Z plane (horizontal)
            // 1: X-Y plane (vertical, arch along X axis, extruded along Z)
            // 2: Y-Z plane (vertical, arch along Z axis, extruded along X)
            int plane = 0;
            if (semi) {
                if (sizeX >= sizeZ) {
                    plane = 1;
                } else {
                    plane = 2;
                }
            } else {
                if (sizeY == 1) {
                    plane = 0;
                } else if (sizeX == 1) {
                    plane = 2;
                } else if (sizeZ == 1) {
                    plane = 1;
                } else {
                    plane = 0; // Default to horizontal circle wall
                }
            }

            int blocksChanged = 0;
            java.util.Set<String> blocksToPlace = new java.util.HashSet<>();

            if (plane == 0) {
                double centerX = (minX + maxX) / 2.0;
                double centerZ = (minZ + maxZ) / 2.0;
                double rx = (maxX - minX) / 2.0;
                double rz = (maxZ - minZ) / 2.0;

                if (rx > 0 && rz > 0) {
                    for (int x = minX; x <= maxX; x++) {
                        double dx = (x - centerX) / rx;
                        double val = 1.0 - dx * dx;
                        if (val >= 0) {
                            double dz = Math.sqrt(val) * rz;
                            int zA = (int) Math.round(centerZ + dz);
                            int zB = (int) Math.round(centerZ - dz);
                            blocksToPlace.add(x + "," + zA);
                            blocksToPlace.add(x + "," + zB);
                        }
                    }
                    for (int z = minZ; z <= maxZ; z++) {
                        double dz = (z - centerZ) / rz;
                        double val = 1.0 - dz * dz;
                        if (val >= 0) {
                            double dx = Math.sqrt(val) * rx;
                            int xA = (int) Math.round(centerX + dx);
                            int xB = (int) Math.round(centerX - dx);
                            blocksToPlace.add(xA + "," + z);
                            blocksToPlace.add(xB + "," + z);
                        }
                    }
                } else {
                    for (int x = minX; x <= maxX; x++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            blocksToPlace.add(x + "," + z);
                        }
                    }
                }

                for (int y = minY; y <= maxY; y++) {
                    for (String coord : blocksToPlace) {
                        String[] parts = coord.split(",");
                        int x = Integer.parseInt(parts[0]);
                        int z = Integer.parseInt(parts[1]);
                        Block b = world.getBlockAt(x, y, z);
                        if (b.getType() != material) {
                            b.setType(material, false);
                            blocksChanged++;
                        }
                    }
                }

            } else if (plane == 1) {
                double centerX = (minX + maxX) / 2.0;
                double centerY = semi ? minY : (minY + maxY) / 2.0;
                double rx = (maxX - minX) / 2.0;
                double ry = semi ? (maxY - minY) : (maxY - minY) / 2.0;

                if (rx > 0 && ry > 0) {
                    for (int x = minX; x <= maxX; x++) {
                        double dx = (x - centerX) / rx;
                        double val = 1.0 - dx * dx;
                        if (val >= 0) {
                            double dy = Math.sqrt(val) * ry;
                            int yA = (int) Math.round(centerY + dy);
                            int yB = (int) Math.round(centerY - dy);
                            if (!semi || yA >= minY) blocksToPlace.add(x + "," + yA);
                            if (!semi && yB >= minY) blocksToPlace.add(x + "," + yB);
                        }
                    }
                    for (int y = minY; y <= maxY; y++) {
                        double dy = (y - centerY) / ry;
                        double val = 1.0 - dy * dy;
                        if (val >= 0) {
                            double dx = Math.sqrt(val) * rx;
                            int xA = (int) Math.round(centerX + dx);
                            int xB = (int) Math.round(centerX - dx);
                            blocksToPlace.add(xA + "," + y);
                            blocksToPlace.add(xB + "," + y);
                        }
                    }
                } else {
                    for (int x = minX; x <= maxX; x++) {
                        for (int y = minY; y <= maxY; y++) {
                            blocksToPlace.add(x + "," + y);
                        }
                    }
                }

                for (int z = minZ; z <= maxZ; z++) {
                    for (String coord : blocksToPlace) {
                        String[] parts = coord.split(",");
                        int x = Integer.parseInt(parts[0]);
                        int y = Integer.parseInt(parts[1]);
                        Block b = world.getBlockAt(x, y, z);
                        if (b.getType() != material) {
                            b.setType(material, false);
                            blocksChanged++;
                        }
                    }
                }

            } else {
                double centerZ = (minZ + maxZ) / 2.0;
                double centerY = semi ? minY : (minY + maxY) / 2.0;
                double rz = (maxZ - minZ) / 2.0;
                double ry = semi ? (maxY - minY) : (maxY - minY) / 2.0;

                if (rz > 0 && ry > 0) {
                    for (int z = minZ; z <= maxZ; z++) {
                        double dz = (z - centerZ) / rz;
                        double val = 1.0 - dz * dz;
                        if (val >= 0) {
                            double dy = Math.sqrt(val) * ry;
                            int yA = (int) Math.round(centerY + dy);
                            int yB = (int) Math.round(centerY - dy);
                            if (!semi || yA >= minY) blocksToPlace.add(z + "," + yA);
                            if (!semi && yB >= minY) blocksToPlace.add(z + "," + yB);
                        }
                    }
                    for (int y = minY; y <= maxY; y++) {
                        double dy = (y - centerY) / ry;
                        double val = 1.0 - dy * dy;
                        if (val >= 0) {
                            double dz = Math.sqrt(val) * rz;
                            int zA = (int) Math.round(centerZ + dz);
                            int zB = (int) Math.round(centerZ - dz);
                            blocksToPlace.add(zA + "," + y);
                            blocksToPlace.add(zB + "," + y);
                        }
                    }
                } else {
                    for (int z = minZ; z <= maxZ; z++) {
                        for (int y = minY; y <= maxY; y++) {
                            blocksToPlace.add(z + "," + y);
                        }
                    }
                }

                for (int x = minX; x <= maxX; x++) {
                    for (String coord : blocksToPlace) {
                        String[] parts = coord.split(",");
                        int z = Integer.parseInt(parts[0]);
                        int y = Integer.parseInt(parts[1]);
                        Block b = world.getBlockAt(x, y, z);
                        if (b.getType() != material) {
                            b.setType(material, false);
                            blocksChanged++;
                        }
                    }
                }
            }

            player.sendMessage(Component.text("✅ Successfully drew hollow " + (semi ? "semicircle" : "circle") + " (" + blocksChanged + " blocks placed).", NamedTextColor.GREEN));
            return true;
        }

        // --- /wandundo (OP only) ---
        if (command.getName().equalsIgnoreCase("wandundo")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }
            UUID uuid = player.getUniqueId();
            java.util.Stack<List<UndoBlock>> history = wandUndoHistory.get(uuid);
            if (history == null || history.isEmpty()) {
                player.sendMessage(Component.text("❌ Nothing to undo!", NamedTextColor.RED));
                return true;
            }
            List<UndoBlock> previousState = history.pop();
            
            // Record redo history (current state before undoing)
            List<UndoBlock> redoState = new ArrayList<>();
            for (UndoBlock ub : previousState) {
                Block b = ub.location.getBlock();
                redoState.add(new UndoBlock(b.getLocation(), b.getType(), b.getBlockData()));
            }
            wandRedoHistory.computeIfAbsent(uuid, k -> new java.util.Stack<>()).push(redoState);

            int restored = 0;
            for (UndoBlock ub : previousState) {
                Block b = ub.location.getBlock();
                b.setType(ub.material, false);
                b.setBlockData(ub.blockData, false);
                restored++;
            }
            player.sendMessage(Component.text("✅ Successfully undid wand action (" + restored + " blocks restored).", NamedTextColor.GREEN));
            return true;
        }

        // --- /wandredo (OP only) ---
        if (command.getName().equalsIgnoreCase("wandredo")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }
            UUID uuid = player.getUniqueId();
            java.util.Stack<List<UndoBlock>> redoHistory = wandRedoHistory.get(uuid);
            if (redoHistory == null || redoHistory.isEmpty()) {
                player.sendMessage(Component.text("❌ Nothing to redo!", NamedTextColor.RED));
                return true;
            }
            List<UndoBlock> redoState = redoHistory.pop();

            // Record undo history (current state before redoing)
            List<UndoBlock> undoState = new ArrayList<>();
            for (UndoBlock ub : redoState) {
                Block b = ub.location.getBlock();
                undoState.add(new UndoBlock(b.getLocation(), b.getType(), b.getBlockData()));
            }
            wandUndoHistory.computeIfAbsent(uuid, k -> new java.util.Stack<>()).push(undoState);

            int restored = 0;
            for (UndoBlock ub : redoState) {
                Block b = ub.location.getBlock();
                b.setType(ub.material, false);
                b.setBlockData(ub.blockData, false);
                restored++;
            }
            player.sendMessage(Component.text("✅ Successfully redid wand action (" + restored + " blocks restored).", NamedTextColor.GREEN));
            return true;
        }

        // --- /wandstructure (OP only) ---
        if (command.getName().equalsIgnoreCase("wandstructure")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(Component.text("❌ Usage: /wandstructure (save/load) (name)", NamedTextColor.RED));
                return true;
            }

            String action = args[0].toLowerCase();
            String structName = args[1].toLowerCase();
            UUID uuid = player.getUniqueId();

            if (action.equals("save")) {
                Location p1 = wandPoint1.get(uuid);
                Location p2 = wandPoint2.get(uuid);
                if (p1 == null || p2 == null) {
                    player.sendMessage(Component.text("❌ Select two points with the Wand first!", NamedTextColor.RED));
                    return true;
                }

                int x1 = p1.getBlockX();
                int y1 = p1.getBlockY();
                int z1 = p1.getBlockZ();
                int x2 = p2.getBlockX();
                int y2 = p2.getBlockY();
                int z2 = p2.getBlockZ();

                int minX = Math.min(x1, x2);
                int maxX = Math.max(x1, x2);
                int minY = Math.min(y1, y2);
                int maxY = Math.max(y1, y2);
                int minZ = Math.min(z1, z2);
                int maxZ = Math.max(z1, z2);

                World world = player.getWorld();
                List<String> serializedBlocks = new ArrayList<>();

                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            Block block = world.getBlockAt(x, y, z);
                            if (block.getType() != Material.AIR) {
                                // Save offset from Point 1
                                int ox = x - x1;
                                int oy = y - y1;
                                int oz = z - z1;
                                String serialized = ox + ";" + oy + ";" + oz + ";" + block.getType().name() + ";" + block.getBlockData().getAsString();
                                serializedBlocks.add(serialized);
                            }
                        }
                    }
                }

                getConfig().set("wandstructures." + structName, serializedBlocks);
                saveConfig();
                player.sendMessage(Component.text("✅ Structure saved as '" + structName + "' with " + serializedBlocks.size() + " non-air blocks!", NamedTextColor.GREEN));
                return true;

            } else if (action.equals("load")) {
                if (!getConfig().contains("wandstructures." + structName)) {
                    player.sendMessage(Component.text("❌ No structure found with name: " + structName, NamedTextColor.RED));
                    return true;
                }

                Location refLoc = wandPoint1.get(uuid);
                if (refLoc == null) {
                    refLoc = player.getLocation();
                }

                List<String> serializedBlocks = getConfig().getStringList("wandstructures." + structName);
                if (serializedBlocks == null || serializedBlocks.isEmpty()) {
                    player.sendMessage(Component.text("❌ Structure is empty or corrupt!", NamedTextColor.RED));
                    return true;
                }

                World world = refLoc.getWorld();
                int refX = refLoc.getBlockX();
                int refY = refLoc.getBlockY();
                int refZ = refLoc.getBlockZ();
                int blocksPlaced = 0;
                List<UndoBlock> undoBlocks = new ArrayList<>();

                for (String line : serializedBlocks) {
                    String[] parts = line.split(";", 5);
                    if (parts.length < 5) continue;
                    try {
                        int ox = Integer.parseInt(parts[0]);
                        int oy = Integer.parseInt(parts[1]);
                        int oz = Integer.parseInt(parts[2]);
                        Material mat = Material.valueOf(parts[3]);
                        String blockDataStr = parts[4];

                        int tx = refX + ox;
                        int ty = refY + oy;
                        int tz = refZ + oz;

                        Block block = world.getBlockAt(tx, ty, tz);
                        undoBlocks.add(new UndoBlock(block.getLocation(), block.getType(), block.getBlockData()));
                        block.setType(mat, false);
                        block.setBlockData(Bukkit.createBlockData(blockDataStr), false);
                        blocksPlaced++;
                    } catch (Exception e) {
                        // Ignore corrupt block entries
                    }
                }

                if (!undoBlocks.isEmpty()) {
                    wandUndoHistory.computeIfAbsent(player.getUniqueId(), k -> new java.util.Stack<>()).push(undoBlocks);
                    java.util.Stack<List<UndoBlock>> redoStack = wandRedoHistory.get(player.getUniqueId());
                    if (redoStack != null) redoStack.clear();
                }

                player.sendMessage(Component.text("✅ Structure '" + structName + "' loaded successfully relative to " + (wandPoint1.containsKey(uuid) ? "Point 1" : "your position") + " (" + blocksPlaced + " blocks placed).", NamedTextColor.GREEN));
                return true;

            } else {
                player.sendMessage(Component.text("❌ Unknown action! Use: save or load", NamedTextColor.RED));
                return true;
            }
        }

        // --- /copy (OP only) ---
        if (command.getName().equalsIgnoreCase("copy")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }

            int x1, y1, z1, x2, y2, z2;
            UUID uuid = player.getUniqueId();

            if (args.length < 6) {
                Location p1 = wandPoint1.get(uuid);
                Location p2 = wandPoint2.get(uuid);
                if (p1 == null || p2 == null) {
                    player.sendMessage(Component.text("❌ Usage: /copy <x1> <y1> <z1> <x2> <y2> <z2> OR select two points with the Wand first!", NamedTextColor.RED));
                    return true;
                }
                x1 = p1.getBlockX();
                y1 = p1.getBlockY();
                z1 = p1.getBlockZ();
                x2 = p2.getBlockX();
                y2 = p2.getBlockY();
                z2 = p2.getBlockZ();
            } else {
                try {
                    Location ploc = player.getLocation();
                    x1 = (int) Math.round(parseCoordinate(args[0], ploc.getX()));
                    y1 = (int) Math.round(parseCoordinate(args[1], ploc.getY()));
                    z1 = (int) Math.round(parseCoordinate(args[2], ploc.getZ()));
                    x2 = (int) Math.round(parseCoordinate(args[3], ploc.getX()));
                    y2 = (int) Math.round(parseCoordinate(args[4], ploc.getY()));
                    z2 = (int) Math.round(parseCoordinate(args[5], ploc.getZ()));
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("❌ Invalid coordinates! Use numbers or ~ for relative position.", NamedTextColor.RED));
                    return true;
                }
            }

            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            int minY = Math.min(y1, y2);
            int maxY = Math.max(y1, y2);
            int minZ = Math.min(z1, z2);
            int maxZ = Math.max(z1, z2);

            int volume = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);

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
                    targetWorld = Bukkit.getWorld("afk") != null ? Bukkit.getWorld("afk") : Bukkit.getWorld("afk_zone");
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
                for (String page : serverRules) {
                    meta.addPages(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(page));
                }
                book.setItemMeta(meta);
            }
            player.openBook(book);
            return true;
        }

        // --- /credits ---
        if (command.getName().equalsIgnoreCase("credits")) {
            ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
            org.bukkit.inventory.meta.BookMeta meta = (org.bukkit.inventory.meta.BookMeta) book.getItemMeta();
            if (meta != null) {
                meta.title(Component.text("Server Credits", NamedTextColor.GOLD));
                meta.author(Component.text("Staff"));
                for (String page : serverCredits) {
                    meta.addPages(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(page));
                }
                book.setItemMeta(meta);
            }
            player.openBook(book);
            return true;
        }

        // --- /edit (rules/credits) ---
        if (command.getName().equalsIgnoreCase("edit")) {
            String pName = player.getName();
            if (!pName.equalsIgnoreCase(".Redtoppat208") && !pName.equalsIgnoreCase("Redtoppat208")) {
                player.sendMessage(Component.text("❌ Only player .Redtoppat208 can use this command!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 1) {
                player.sendMessage(Component.text("❌ Usage: /edit (rules/credits)", NamedTextColor.RED));
                return true;
            }
            String targetType = args[0].toLowerCase();
            if (!targetType.equals("rules") && !targetType.equals("credits")) {
                player.sendMessage(Component.text("❌ Usage: /edit (rules/credits)", NamedTextColor.RED));
                return true;
            }

            ItemStack book = new ItemStack(Material.WRITABLE_BOOK);
            org.bukkit.inventory.meta.BookMeta meta = (org.bukkit.inventory.meta.BookMeta) book.getItemMeta();
            if (meta != null) {
                List<String> pages = targetType.equals("rules") ? serverRules : serverCredits;
                meta.setPages(pages);
                book.setItemMeta(meta);
            }
            editingGlobalBook.put(player.getUniqueId(), targetType);
            player.getInventory().addItem(book);
            player.sendMessage(Component.text("📖 A book has been added to your inventory. Open it to edit the " + targetType + ", then Sign/Done to save!", NamedTextColor.YELLOW));
            return true;
        }

        // --- /item <load|save> <name> (OP only) ---
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
                getConfig().set("saveditems." + itemName + ".item", item);

                // Save shulker contents explicitly
                if (item.getItemMeta() instanceof org.bukkit.inventory.meta.BlockStateMeta bsm) {
                    if (bsm.getBlockState() instanceof org.bukkit.block.ShulkerBox shulker) {
                        ItemStack[] contents = shulker.getInventory().getContents();
                        getConfig().set("saveditems." + itemName + ".contents", contents);
                    }
                } else {
                    getConfig().set("saveditems." + itemName + ".contents", null);
                }
                saveConfig();
                player.sendMessage(Component.text("✅ Item saved as '" + itemName + "'!", NamedTextColor.GREEN));
            } else if (action.equals("load")) {
                ItemStack item = null;
                boolean legacy = false;

                if (getConfig().contains("saveditems." + itemName + ".item")) {
                    item = getConfig().getItemStack("saveditems." + itemName + ".item");
                } else if (getConfig().contains("saveditems." + itemName)) {
                    // Fallback to legacy format
                    if (getConfig().isItemStack("saveditems." + itemName)) {
                        item = getConfig().getItemStack("saveditems." + itemName);
                        legacy = true;
                    }
                }

                if (item == null) {
                    player.sendMessage(Component.text("❌ No saved item found with the name '" + itemName + "'!", NamedTextColor.RED));
                    return true;
                }

                // Restore shulker contents explicitly if not legacy
                if (!legacy && item.getItemMeta() instanceof org.bukkit.inventory.meta.BlockStateMeta bsm) {
                    if (bsm.getBlockState() instanceof org.bukkit.block.ShulkerBox shulker) {
                        List<?> rawList = getConfig().getList("saveditems." + itemName + ".contents");
                        if (rawList != null) {
                            ItemStack[] contents = new ItemStack[shulker.getInventory().getSize()];
                            for (int i = 0; i < Math.min(contents.length, rawList.size()); i++) {
                                Object obj = rawList.get(i);
                                if (obj instanceof ItemStack) {
                                    contents[i] = (ItemStack) obj;
                                }
                            }
                            shulker.getInventory().setContents(contents);
                            bsm.setBlockState(shulker);
                            item.setItemMeta(bsm);
                        }
                    }
                }

                player.getInventory().addItem(item.clone());
                player.sendMessage(Component.text("✅ Loaded item '" + itemName + "' into your inventory!", NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("❌ Unknown action! Use: load or save", NamedTextColor.RED));
            }
            return true;
        }

        // --- /offhand ---
        if (command.getName().equalsIgnoreCase("offhand")) {
            org.bukkit.inventory.PlayerInventory inv = player.getInventory();
            ItemStack main = inv.getItemInMainHand();
            ItemStack off = inv.getItemInOffHand();
            inv.setItemInMainHand(off);
            inv.setItemInOffHand(main);
            player.sendMessage(Component.text("🔄 Swapped main hand and offhand items!", NamedTextColor.GREEN));
            return true;
        }

        // --- /enchantitem ---
        if (command.getName().equalsIgnoreCase("enchantitem")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(Component.text("❌ Usage: /enchantitem (enchantment) (level)", NamedTextColor.RED));
                return true;
            }

            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType() == Material.AIR) {
                player.sendMessage(Component.text("❌ You must hold an item in your main hand to enchant it!", NamedTextColor.RED));
                return true;
            }

            String enchantName = args[0].toLowerCase();
            int level;
            try {
                level = Integer.parseInt(args[1]);
                if (level < 0) {
                    player.sendMessage(Component.text("❌ Level must be non-negative!", NamedTextColor.RED));
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("❌ Invalid level!", NamedTextColor.RED));
                return true;
            }

            NamespacedKey key;
            if (enchantName.contains(":")) {
                String[] parts = enchantName.split(":");
                key = new NamespacedKey(parts[0], parts[1]);
            } else {
                key = NamespacedKey.minecraft(enchantName);
            }

            Enchantment enchant = Enchantment.getByKey(key);
            if (enchant == null) {
                try {
                    enchant = org.bukkit.Registry.ENCHANTMENT.get(key);
                } catch (NoClassDefFoundError | NoSuchFieldError | Exception ex) {}
            }

            if (enchant == null) {
                for (Enchantment e : org.bukkit.Registry.ENCHANTMENT) {
                    if (e.getKey().getKey().equalsIgnoreCase(enchantName)) {
                        enchant = e;
                        break;
                    }
                }
            }

            if (enchant == null) {
                player.sendMessage(Component.text("❌ Unknown enchantment: " + enchantName, NamedTextColor.RED));
                return true;
            }

            if (level == 0) {
                item.removeEnchantment(enchant);
                player.sendMessage(Component.text("✅ Removed " + enchant.getKey().getKey() + " from your item!", NamedTextColor.GREEN));
            } else {
                item.addUnsafeEnchantment(enchant, level);
                player.sendMessage(Component.text("✅ Successfully enchanted item with " + enchant.getKey().getKey() + " Level " + level + "!", NamedTextColor.GREEN));
            }
            return true;
        }

        // --- /gm and /gamemode ---
        if (command.getName().equalsIgnoreCase("gm") || command.getName().equalsIgnoreCase("gamemode")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You don't have permission!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 1) {
                player.sendMessage(Component.text("❌ Usage: /" + command.getName().toLowerCase() + " (s/c/a/spectator)", NamedTextColor.RED));
                return true;
            }
            String mode = args[0].toLowerCase();
            GameMode gm;
            switch (mode) {
                case "s", "survival" -> gm = GameMode.SURVIVAL;
                case "c", "creative" -> gm = GameMode.CREATIVE;
                case "a", "adventure" -> gm = GameMode.ADVENTURE;
                case "sp", "spectator" -> gm = GameMode.SPECTATOR;
                default -> {
                    player.sendMessage(Component.text("❌ Unknown gamemode! Use: s, c, a, spectator", NamedTextColor.RED));
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
                player.sendMessage(Component.text("❌ Usage: /keys (keys/echo/crimson/end/amethyst) (add/remove/reset) (amount) (playername)", NamedTextColor.RED));
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
            } else if (keyType.equals("end")) {
                targetMap = endKeysMap;
                keyLabel = "End keys";
            } else if (keyType.equals("amethyst")) {
                targetMap = amethystKeysMap;
                keyLabel = "Amethyst keys";
            } else {
                player.sendMessage(Component.text("❌ Invalid key type! Use: keys, echo, crimson, end, or amethyst", NamedTextColor.RED));
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
        Player player = event.getPlayer();
        Block block = event.getBlock();

        // Wand selection Point 1
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand != null && mainHand.hasItemMeta()) {
            String customType = mainHand.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING);
            if (customType != null && customType.equals("wand")) {
                if (player.isOp()) {
                    event.setCancelled(true);
                    setWandPoint(player, block.getLocation(), 1);
                    return;
                }
            }
        }

        if (event.isCancelled()) return;
        Location loc = block.getLocation();

        if (generators.containsKey(loc)) {
            event.setCancelled(true);
            GeneratorData data = generators.get(loc);

            ItemStack genItem = null;
            if (data.type.equals("food_generator")) {
                genItem = createFoodGeneratorItem();
            } else if (data.type.equals("ore_generator")) {
                genItem = createOreGeneratorItem();
            } else if (data.type.equals("tools_generator")) {
                genItem = createToolsGeneratorItem();
            }

            if (genItem != null) {
                loc.getWorld().dropItemNaturally(loc, genItem);
            }

            for (ItemStack itemStack : data.inventory.getContents()) {
                if (itemStack != null && itemStack.getType() != Material.AIR) {
                    loc.getWorld().dropItemNaturally(loc, itemStack);
                }
            }

            generators.remove(loc);
            saveGenerators();

            block.setType(Material.AIR);
            player.sendMessage(Component.text("⚡ Generator removed successfully.", NamedTextColor.GREEN));
            return;
        }

        if (commandChests.containsKey(loc)) {
            if (player.getGameMode() == GameMode.SURVIVAL) {
                event.setCancelled(true);
                player.sendMessage(Component.text("❌ Command Chests cannot be broken in Survival mode!", NamedTextColor.RED));
                return;
            }
            commandChests.remove(loc);
            saveCommandChests();
            block.setType(Material.AIR);
            loc.getWorld().dropItemNaturally(loc, createCommandChest());
            player.sendMessage(Component.text("✅ Command Chest removed successfully.", NamedTextColor.GREEN));
            return;
        }

        if (shopCrates.containsKey(loc)) {
            ShopCrateData data = shopCrates.get(loc);
            if (player.getGameMode() == GameMode.SURVIVAL) {
                event.setCancelled(true);
                player.sendMessage(Component.text("❌ Shop Crates cannot be broken in Survival mode!", NamedTextColor.RED));
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
            } else if (data.crateType.equals("end")) {
                crateToDrop = createEndCrate();
            } else if (data.crateType.equals("amethyst")) {
                crateToDrop = createAmethystCrate();
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

        if ((loc.getWorld().getName().equals("afk_zone") || loc.getWorld().getName().equals("afk")) && player.getGameMode() == GameMode.SURVIVAL) {
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
        } else if (customItem.equals("echo_shovel")) {
            event.setCancelled(true);
            breakingCustom = true;
            try {
                handleEchoShovelBreak(player, event.getBlock(), tool);
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
        if (!event.isCancelled()) {
            checkAndTrackMinedOre(player, block);
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
        if ((event.getBlock().getWorld().getName().equals("afk_zone") || event.getBlock().getWorld().getName().equals("afk")) && event.getPlayer().getGameMode() == GameMode.SURVIVAL) {
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
                if (customItem.equals("shop_crate") || customItem.equals("echo_crate") || customItem.equals("crimson_crate") || customItem.equals("key_crate") || customItem.equals("end_crate") || customItem.equals("amethyst_crate")) {
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
                    } else if (customItem.equals("end_crate")) {
                        data.crateType = "end";
                        event.getPlayer().sendMessage(Component.text("📦 You placed an End Crate! Right-click it in Creative Mode to set it up.", NamedTextColor.YELLOW));
                    } else if (customItem.equals("amethyst_crate")) {
                        data.crateType = "amethyst";
                        event.getPlayer().sendMessage(Component.text("📦 You placed an Amethyst Crate! Right-click it in Creative Mode to set it up.", NamedTextColor.YELLOW));
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
                } else if (customItem.equals("command_chest")) {
                    Location loc = event.getBlock().getLocation();
                    activeCommandChestSetup.put(event.getPlayer().getUniqueId(), loc);
                    Bukkit.getScheduler().runTask(this, () -> {
                        openSignInput(event.getPlayer(), SignAction.SET_COMMAND_CHEST, null, "Enter Command");
                    });
                } else if (customItem.equals("food_generator") || customItem.equals("ore_generator") || customItem.equals("tools_generator")) {
                    Location loc = event.getBlock().getLocation();
                    String title = capitalize(customItem.replace("_", " "));
                    Inventory genInv = Bukkit.createInventory(null, 27, Component.text(title));
                    generators.put(loc, new GeneratorData(loc, customItem, genInv));
                    saveGenerators();
                    event.getPlayer().sendMessage(Component.text("⚡ You placed a " + title + "! Left-click it to open its inventory.", NamedTextColor.GREEN));
                }
            }
        }
        if (!event.isCancelled()) {
            UUID uuid = event.getPlayer().getUniqueId();
            blocksPlacedMap.put(uuid, blocksPlacedMap.getOrDefault(uuid, 0) + 1);
        }
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item != null && item.getType().isEdible()) {
            UUID uuid = player.getUniqueId();
            Material mat = item.getType();
            HashMap<Material, Integer> playerFoods = foodsEatenMap.computeIfAbsent(uuid, k -> new HashMap<>());
            playerFoods.put(mat, playerFoods.getOrDefault(mat, 0) + 1);
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
            
            ItemStack weapon = attacker.getInventory().getItemInMainHand();
            if (weapon != null && weapon.hasItemMeta()) {
                String customItem = weapon.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING);
                if (customItem != null && customItem.equals("divine_flame")) {
                    event.getEntity().setFireTicks(160);
                }
            }
        }

        if (event.getEntity() instanceof Player victim && attacker != null) {
            if ((attacker.getWorld().getName().equals("afk_zone") || attacker.getWorld().getName().equals("afk")) && attacker.getGameMode() == GameMode.SURVIVAL) {
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
            // Shop items matching shop prices
            case SHULKER_BOX: return 800;
            case ENDER_CHEST: return 1200;
            case ENDER_PEARL: return 75;
            case CHORUS_FRUIT: return 50;
            case TOTEM_OF_UNDYING: return 1500;
            case WIND_CHARGE: return 75;
            case END_CRYSTAL: return 500;
            case OBSIDIAN: return 500;
            case ENCHANTED_GOLDEN_APPLE: return 500;
            case COOKED_BEEF: return 100;
            case COOKED_PORKCHOP: return 50;
            case BREAD: return 25;
            case GOLDEN_CARROT: return 125;

            // Epic / Legendary (50,000)
            case ELYTRA:
            case DRAGON_EGG:
            case DRAGON_HEAD:
            case NETHER_STAR:
            case BEACON:
                return 50000;

            // Rare / Special (10,000)
            case HEART_OF_THE_SEA:
            case TRIDENT:
            case NETHERITE_INGOT:
                return 10000;

            // Diamond / Emerald / Valuable items (1,000)
            case DIAMOND:
            case EMERALD:
            case NETHERITE_SCRAP:
            case ANCIENT_DEBRIS:
            case WITHER_SKELETON_SKULL:
            case SHULKER_SHELL:
                return 1000;

            // Semi-precious / Mid-tier (100)
            case GOLD_INGOT:
            case IRON_INGOT:
            case LAPIS_LAZULI:
            case REDSTONE_BLOCK:
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

    // --- Nametag System ---
    private void openNametagMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Nametag Menu"));
        inv.setItem(11, createGuiItem(Material.RED_WOOL, "Clear Nametag", NamedTextColor.RED, "Click to remove your current tag"));
        inv.setItem(15, createGuiItem(Material.NAME_TAG, "Add Nametag", NamedTextColor.GREEN, "Click to browse and select your unlocked tags"));
        player.openInventory(inv);
    }

    private void openNametagAddMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 36, Component.text("Add Nametag"));
        UUID uuid = player.getUniqueId();

        // 1. Berry Lover
        boolean hasBerry = hasBerryLoverUnlocked(player);
        inv.setItem(10, createGuiItem(
            Material.SWEET_BERRIES, 
            "Berry Lover", 
            NamedTextColor.LIGHT_PURPLE, 
            "Requirement: Fill inventory with Sweet/Glow Berries",
            hasBerry ? "§aStatus: UNLOCKED" : "§cStatus: LOCKED"
        ));

        // 2. Combat Master
        int kills = killsMap.getOrDefault(uuid, 0);
        boolean hasCombat = kills >= 30;
        inv.setItem(11, createGuiItem(
            Material.DIAMOND_SWORD, 
            "Combat Master", 
            NamedTextColor.RED, 
            "Requirement: Kill 30 players",
            "Progress: " + kills + "/30",
            hasCombat ? "§aStatus: UNLOCKED" : "§cStatus: LOCKED"
        ));

        // 3. Admin killer
        boolean hasAdminKiller = killedAdminMap.getOrDefault(uuid, false);
        ItemStack adminKillerItem = createGuiItem(
            Material.NETHER_STAR,
            "Admin killer",
            NamedTextColor.WHITE,
            "Requirement: Kill 1 Admin/Operator (Secret Tag)",
            hasAdminKiller ? "§aStatus: UNLOCKED" : "§cStatus: LOCKED"
        );
        ItemMeta meta = adminKillerItem.getItemMeta();
        if (meta != null) {
            meta.displayName(createRainbowComponent("[Admin killer]"));
            adminKillerItem.setItemMeta(meta);
        }
        inv.setItem(12, adminKillerItem);

        // 4. Richie Boi
        long balance = erpiesMap.getOrDefault(uuid, 0L);
        boolean hasRichie = balance >= 1000000L;
        inv.setItem(13, createGuiItem(
            Material.EMERALD_BLOCK, 
            "Richie Boi", 
            NamedTextColor.GREEN, 
            "Requirement: Make at least 1M Erpies",
            "Progress: " + formatValue(balance) + " / 1M",
            hasRichie ? "§aStatus: UNLOCKED" : "§cStatus: LOCKED"
        ));

        // 5. Dragon Slayer
        boolean hasDragon = killedDragonMap.getOrDefault(uuid, false);
        inv.setItem(14, createGuiItem(
            Material.DRAGON_HEAD, 
            "Dragon Slayer", 
            NamedTextColor.DARK_PURPLE, 
            "Requirement: Kill the Ender Dragon once",
            hasDragon ? "§aStatus: UNLOCKED" : "§cStatus: LOCKED"
        ));

        // 6. The Miner
        int ores = oresMinedMap.getOrDefault(uuid, 0);
        boolean hasMiner = ores >= 100;
        inv.setItem(15, createGuiItem(
            Material.DIAMOND_ORE,
            "The Miner",
            NamedTextColor.BLUE,
            "Requirement: Mine at least 100 ores",
            "Progress: " + ores + "/100",
            hasMiner ? "§aStatus: UNLOCKED" : "§cStatus: LOCKED"
        ));

        // 7. Silent Assassin
        int invisKills = invisibleKillsMap.getOrDefault(uuid, 0);
        boolean hasAssassin = invisKills >= 30;
        inv.setItem(16, createGuiItem(
            Material.POTION,
            "Silent Assassin",
            NamedTextColor.RED,
            "Requirement: Kill 30 players with Invisibility active",
            "Progress: " + invisKills + "/30",
            hasAssassin ? "§aStatus: UNLOCKED" : "§cStatus: LOCKED"
        ));

        // 8. The Builder
        int blocks = blocksPlacedMap.getOrDefault(uuid, 0);
        boolean hasBuilder = blocks >= 3000;
        inv.setItem(20, createGuiItem(
            Material.BRICKS,
            "The Builder",
            NamedTextColor.YELLOW,
            "Requirement: Place 3000 blocks",
            "Progress: " + blocks + "/3000",
            hasBuilder ? "§aStatus: UNLOCKED" : "§cStatus: LOCKED"
        ));

        // 9. Fatty
        int fattyProg = getFattyProgress(player);
        boolean hasFatty = fattyProg == ALL_FOODS.size();
        inv.setItem(22, createGuiItem(
            Material.COOKED_BEEF,
            "Fatty",
            NamedTextColor.GREEN,
            "Requirement: Eat 300 of every common food type",
            "Progress: " + fattyProg + "/" + ALL_FOODS.size() + " foods completed",
            hasFatty ? "§aStatus: UNLOCKED" : "§cStatus: LOCKED"
        ));

        // 10. Skin and Bones
        int starvation = starvationDeathsMap.getOrDefault(uuid, 0);
        boolean hasSkin = starvation >= 10;
        inv.setItem(24, createGuiItem(
            Material.BONE,
            "Skin and Bones",
            NamedTextColor.WHITE,
            "Requirement: Die to starvation 10 times",
            "Progress: " + starvation + "/10",
            hasSkin ? "§aStatus: UNLOCKED" : "§cStatus: LOCKED"
        ));

        inv.setItem(31, createGuiItem(Material.ARROW, "Back to Menu", NamedTextColor.YELLOW, "Click to go back"));

        player.openInventory(inv);
    }

    @EventHandler
    public void onNametagInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (!title.equals("Nametag Menu") && !title.equals("Add Nametag")) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        UUID uuid = player.getUniqueId();

        if (title.equals("Nametag Menu")) {
            if (event.getRawSlot() == 11) {
                // Clear tag
                activeNametags.put(uuid, "");
                player.sendMessage(Component.text("✅ Your active nametag has been cleared!", NamedTextColor.GREEN));
                for (Player online : Bukkit.getOnlinePlayers()) {
                    updateNameplateTeams(online.getScoreboard());
                }
                player.closeInventory();
            } else if (event.getRawSlot() == 15) {
                openNametagAddMenu(player);
            }
        } else if (title.equals("Add Nametag")) {
            if (event.getRawSlot() == 31) {
                openNametagMainMenu(player);
                return;
            }

            String selectedTag = null;
            if (event.getRawSlot() == 10) selectedTag = "Berry Lover";
            else if (event.getRawSlot() == 11) selectedTag = "Combat Master";
            else if (event.getRawSlot() == 12) selectedTag = "Admin killer";
            else if (event.getRawSlot() == 13) selectedTag = "Richie Boi";
            else if (event.getRawSlot() == 14) selectedTag = "Dragon Slayer";
            else if (event.getRawSlot() == 15) selectedTag = "The Miner";
            else if (event.getRawSlot() == 16) selectedTag = "Silent Assassin";
            else if (event.getRawSlot() == 20) selectedTag = "The Builder";
            else if (event.getRawSlot() == 22) selectedTag = "Fatty";
            else if (event.getRawSlot() == 24) selectedTag = "Skin and Bones";

            if (selectedTag != null) {
                if (isNametagUnlocked(player, selectedTag)) {
                    activeNametags.put(uuid, selectedTag);
                    player.sendMessage(Component.text("✅ Applied nametag: " + selectedTag + "!", NamedTextColor.GREEN));
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        updateNameplateTeams(online.getScoreboard());
                    }
                    player.closeInventory();
                } else {
                    player.sendMessage(Component.text("❌ You have not unlocked this nametag yet!", NamedTextColor.RED));
                }
            }
        }
    }

    // --- Apocalypse System ---
    private void openApocalypseGui(Player player) {
        UUID uuid = player.getUniqueId();
        String currentDiff = playerApocalypseDifficulty.getOrDefault(uuid, "None");
        
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Apocalypse Menu"));
        ItemStack pane = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.GRAY);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, pane);
        }

        String easyLore = currentDiff.equalsIgnoreCase("easy") ? "§aStatus: SELECTED" : "§cStatus: NOT SELECTED";
        inv.setItem(11, createGuiItem(Material.GREEN_WOOL, "Easy Mode", NamedTextColor.GREEN, "Sets difficulty to Easy", easyLore));

        String normalLore = currentDiff.equalsIgnoreCase("normal") ? "§aStatus: SELECTED" : "§cStatus: NOT SELECTED";
        inv.setItem(12, createGuiItem(Material.LIGHT_BLUE_WOOL, "Normal Mode", NamedTextColor.BLUE, "Sets difficulty to Normal", normalLore));

        String hardLore = currentDiff.equalsIgnoreCase("hard") ? "§aStatus: SELECTED" : "§cStatus: NOT SELECTED";
        inv.setItem(13, createGuiItem(Material.ORANGE_WOOL, "Hard Mode", NamedTextColor.GOLD, "Sets difficulty to Hard", hardLore));

        String apocLore = currentDiff.equalsIgnoreCase("apocalypse") ? "§aStatus: SELECTED" : "§cStatus: NOT SELECTED";
        inv.setItem(14, createGuiItem(Material.RED_WOOL, "Apocalypse Mode", NamedTextColor.RED, "Sets difficulty to Apocalypse (Hardest)", apocLore));

        inv.setItem(29, createGuiItem(Material.NETHER_STAR, "Start", NamedTextColor.DARK_RED, 
            "Teleport to the Apocalypse world", 
            "§aSelected Difficulty: " + capitalize(currentDiff)));

        int zombieKills = apocalypseZombieKillsMap.getOrDefault(uuid, 0);
        long longestSurvival = apocalypseLongestSurvivalTimeMap.getOrDefault(uuid, 0L);
        int maxWaves = apocalypseMaxWavesSurvivedMap.getOrDefault(uuid, 0);
        String formatTime = formatTimePlayed((int) longestSurvival);
        inv.setItem(33, createGuiItem(Material.BOOK, "Stats", NamedTextColor.BLUE, 
            "Your Apocalypse Records:",
            "Zombie Kills: " + zombieKills,
            "Longest Survival: " + formatTime,
            "Max Waves Survived: " + maxWaves));

        player.openInventory(inv);
    }

    private void openApocalypseConfirmGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Confirm Start?"));
        ItemStack pane = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.GRAY);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, pane);
        }
        inv.setItem(11, createGuiItem(Material.GREEN_WOOL, "Confirm", NamedTextColor.GREEN, "Click to start the game!"));
        inv.setItem(15, createGuiItem(Material.RED_WOOL, "Cancel", NamedTextColor.RED, "Go back to difficulty selection"));
        player.openInventory(inv);
    }

    private void endApocalypseRun(Player player) {
        UUID uuid = player.getUniqueId();
        
        java.util.Set<UUID> active = playerActiveApocalypseZombies.remove(uuid);
        if (active != null) {
            for (UUID zUuid : active) {
                org.bukkit.entity.Entity ent = Bukkit.getEntity(zUuid);
                if (ent != null) ent.remove();
            }
        }

        Long start = apocalypseStartTimeMap.remove(uuid);
        Integer currentWaveObj = playerApocalypseWaveMap.remove(uuid);
        int wavesSurvived = currentWaveObj != null ? currentWaveObj - 1 : 0;
        if (wavesSurvived < 0) wavesSurvived = 0;

        if (start != null) {
            long elapsedSeconds = (System.currentTimeMillis() - start) / 1000L;
            long currentRecord = apocalypseLongestSurvivalTimeMap.getOrDefault(uuid, 0L);
            int maxWaves = apocalypseMaxWavesSurvivedMap.getOrDefault(uuid, 0);
            
            player.sendMessage(Component.text("💀 Apocalypse run ended!", NamedTextColor.RED));
            player.sendMessage(Component.text("⏱️ You survived for: " + formatTimePlayed((int) elapsedSeconds), NamedTextColor.YELLOW));
            player.sendMessage(Component.text("🧟 Waves survived: " + wavesSurvived, NamedTextColor.YELLOW));
            
            if (elapsedSeconds > currentRecord) {
                apocalypseLongestSurvivalTimeMap.put(uuid, elapsedSeconds);
                player.sendMessage(Component.text("🏆 NEW RECORD! Your longest survival is now: " + formatTimePlayed((int) elapsedSeconds), NamedTextColor.GREEN));
                player.sendTitle("§aNEW RECORD!", "§7Survived: " + formatTimePlayed((int) elapsedSeconds), 10, 40, 10);
            }
            if (wavesSurvived > maxWaves) {
                apocalypseMaxWavesSurvivedMap.put(uuid, wavesSurvived);
                player.sendMessage(Component.text("🏆 NEW RECORD! Max waves survived: " + wavesSurvived, NamedTextColor.GREEN));
            }
            savePlayerData(player);
        }
    }


    private void spawnNextApocalypseWave(Player player) {
        UUID uuid = player.getUniqueId();
        int wave = playerApocalypseWaveMap.getOrDefault(uuid, 1);
        String difficulty = playerApocalypseDifficulty.getOrDefault(uuid, "normal");
        
        player.sendTitle("§cWAVE " + wave, "§7Zombies are coming...", 10, 40, 10);
        player.sendMessage(Component.text("🚨 Wave " + wave + " has started! 100 zombies spawned.", NamedTextColor.RED));

        java.util.Set<UUID> activeZombies = playerActiveApocalypseZombies.computeIfAbsent(uuid, k -> new java.util.HashSet<>());
        activeZombies.clear(); // Safe clean-up
        
        Location loc = player.getLocation();
        
        for (int i = 0; i < 100; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = 10 + random.nextInt(16); // 10 to 25 blocks away
            double dx = Math.cos(angle) * radius;
            double dz = Math.sin(angle) * radius;
            Location spawnLoc = loc.clone().add(dx, 0, dz);
            spawnLoc.setY(spawnLoc.getWorld().getHighestBlockYAt(spawnLoc.getBlockX(), spawnLoc.getBlockZ()) + 1);
            
            org.bukkit.entity.Zombie zombie = spawnLoc.getWorld().spawn(spawnLoc, org.bukkit.entity.Zombie.class);
            zombie.setTarget(player);
            
            equipZombieForWave(zombie, wave);
            applyDifficultyAttributes(zombie, difficulty);
            
            activeZombies.add(zombie.getUniqueId());
        }
    }

    private void equipZombieForWave(org.bukkit.entity.Zombie zombie, int wave) {
        org.bukkit.inventory.EntityEquipment equip = zombie.getEquipment();
        if (equip == null) return;

        equip.setHelmetDropChance(0.0f);
        equip.setChestplateDropChance(0.0f);
        equip.setLeggingsDropChance(0.0f);
        equip.setBootsDropChance(0.0f);
        equip.setItemInMainHandDropChance(0.0f);

        double rand = random.nextDouble();

        if (wave == 1) {
            if (rand < 0.15) {
                zombie.setBaby(true);
            }
        } else if (wave == 2) {
            if (rand < 0.30) {
                Material helmet = random.nextBoolean() ? Material.LEATHER_HELMET : Material.CHAINMAIL_HELMET;
                Material chest = random.nextBoolean() ? Material.LEATHER_CHESTPLATE : Material.CHAINMAIL_CHESTPLATE;
                Material legs = random.nextBoolean() ? Material.LEATHER_LEGGINGS : Material.CHAINMAIL_LEGGINGS;
                Material boots = random.nextBoolean() ? Material.LEATHER_BOOTS : Material.CHAINMAIL_BOOTS;
                equip.setHelmet(new ItemStack(helmet));
                equip.setChestplate(new ItemStack(chest));
                equip.setLeggings(new ItemStack(legs));
                equip.setBoots(new ItemStack(boots));
            }
        } else if (wave == 3) {
            if (rand < 0.40) {
                boolean isGold = random.nextBoolean();
                Material helmet = isGold ? Material.GOLDEN_HELMET : Material.CHAINMAIL_HELMET;
                Material chest = isGold ? Material.GOLDEN_CHESTPLATE : Material.CHAINMAIL_CHESTPLATE;
                Material legs = isGold ? Material.GOLDEN_LEGGINGS : Material.CHAINMAIL_LEGGINGS;
                Material boots = isGold ? Material.GOLDEN_BOOTS : Material.CHAINMAIL_BOOTS;
                Material weapon = isGold ? Material.GOLDEN_SWORD : Material.STONE_SWORD;
                equip.setHelmet(new ItemStack(helmet));
                equip.setChestplate(new ItemStack(chest));
                equip.setLeggings(new ItemStack(legs));
                equip.setBoots(new ItemStack(boots));
                equip.setItemInMainHand(new ItemStack(weapon));
            }
        } else if (wave >= 4 && wave <= 10) {
            if (rand < 0.80) {
                boolean isIron = random.nextDouble() < 0.25;
                Material helmet = isIron ? Material.IRON_HELMET : Material.GOLDEN_HELMET;
                Material chest = isIron ? Material.IRON_CHESTPLATE : Material.GOLDEN_CHESTPLATE;
                Material legs = isIron ? Material.IRON_LEGGINGS : Material.GOLDEN_LEGGINGS;
                Material boots = isIron ? Material.IRON_BOOTS : Material.GOLDEN_BOOTS;
                Material weapon = isIron ? Material.IRON_SWORD : Material.GOLDEN_SWORD;
                equip.setHelmet(new ItemStack(helmet));
                equip.setChestplate(new ItemStack(chest));
                equip.setLeggings(new ItemStack(legs));
                equip.setBoots(new ItemStack(boots));
                equip.setItemInMainHand(new ItemStack(weapon));
            }
        } else if (wave >= 11) {
            boolean isNetherite = random.nextDouble() < 0.30;
            Material helmet = isNetherite ? Material.NETHERITE_HELMET : Material.DIAMOND_HELMET;
            Material chest = isNetherite ? Material.NETHERITE_CHESTPLATE : Material.DIAMOND_CHESTPLATE;
            Material legs = isNetherite ? Material.NETHERITE_LEGGINGS : Material.DIAMOND_LEGGINGS;
            Material boots = isNetherite ? Material.NETHERITE_BOOTS : Material.DIAMOND_BOOTS;
            Material weapon = isNetherite ? Material.NETHERITE_SWORD : Material.DIAMOND_SWORD;
            equip.setHelmet(new ItemStack(helmet));
            equip.setChestplate(new ItemStack(chest));
            equip.setLeggings(new ItemStack(legs));
            equip.setBoots(new ItemStack(boots));
            equip.setItemInMainHand(new ItemStack(weapon));
        }
    }

    private void applyDifficultyAttributes(org.bukkit.entity.Zombie zombie, String difficulty) {
        double healthMult = 1.0;
        double dmgMult = 1.0;
        double speedMult = 1.0;

        if (difficulty.equalsIgnoreCase("easy")) {
            healthMult = 0.75;
            dmgMult = 0.5;
            speedMult = 0.8;
        } else if (difficulty.equalsIgnoreCase("hard")) {
            healthMult = 2.0;
            dmgMult = 2.0;
            speedMult = 1.1;
        } else if (difficulty.equalsIgnoreCase("apocalypse")) {
            healthMult = 5.0;
            dmgMult = 5.0;
            speedMult = 1.2;
        }

        var maxHealth = zombie.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(maxHealth.getBaseValue() * healthMult);
            zombie.setHealth(maxHealth.getValue());
        }

        var attackDmg = zombie.getAttribute(org.bukkit.attribute.Attribute.ATTACK_DAMAGE);
        if (attackDmg != null) {
            attackDmg.setBaseValue(attackDmg.getBaseValue() * dmgMult);
        }

        var speed = zombie.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(speed.getBaseValue() * speedMult);
        }
    }

    private int calculateZombieDerpiesPayout(org.bukkit.entity.Zombie zombie) {
        var equip = zombie.getEquipment();
        if (equip == null) return 10;
        
        Material[] items = {
            equip.getHelmet() != null ? equip.getHelmet().getType() : Material.AIR,
            equip.getChestplate() != null ? equip.getChestplate().getType() : Material.AIR,
            equip.getLeggings() != null ? equip.getLeggings().getType() : Material.AIR,
            equip.getBoots() != null ? equip.getBoots().getType() : Material.AIR,
            equip.getItemInMainHand() != null ? equip.getItemInMainHand().getType() : Material.AIR
        };
        
        int maxPayout = 10;
        for (Material type : items) {
            if (type.name().contains("NETHERITE") || type.name().contains("DIAMOND")) {
                maxPayout = Math.max(maxPayout, 30);
            } else if (type.name().contains("IRON")) {
                maxPayout = Math.max(maxPayout, 25);
            } else if (type.name().contains("GOLD")) {
                maxPayout = Math.max(maxPayout, 20);
            } else if (type.name().contains("CHAINMAIL") || type.name().contains("LEATHER")) {
                maxPayout = Math.max(maxPayout, 15);
            }
        }
        return maxPayout;
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

        shop.setItem(10, createGuiItem(Material.TRIPWIRE_HOOK, "Regular Key", NamedTextColor.YELLOW, "Cost: 100 Derpies | Owned: " + regCount));

        shop.setItem(19, createGuiItem(Material.SPAWNER, "Food Generator", NamedTextColor.GOLD, "Cost: 2000 Derpies", "Generates steak every minute when placed.", "Right-click placed block to open inventory."));
        shop.setItem(20, createGuiItem(Material.SPAWNER, "Ore Generator", NamedTextColor.AQUA, "Cost: 2000 Derpies", "Generates diamonds every minute when placed.", "Right-click placed block to open inventory."));
        shop.setItem(21, createGuiItem(Material.SPAWNER, "Tools Generator", NamedTextColor.LIGHT_PURPLE, "Cost: 2000 Derpies", "Generates tools & armor (except netherite) every minute when placed.", "Right-click placed block to open inventory."));

        player.openInventory(shop);
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.contains("Shop") && !title.contains("Auction") && !title.contains("Bounty")
                && !title.equals("Random Teleport") && !title.equals("List an Item")
                && !title.equals("Homes Menu") && !title.equals("Settings")
                && !title.equals("Setup Crate Shop") && !title.equals("Buy from Shop")
                && !title.equals("Order Board") && !title.equals("Order Board - Your Orders")
                && !title.endsWith("'s Homes") && !title.startsWith("Team: ") && !title.startsWith("Kick: ")
                && !title.equals("Bank") && !title.equals("Deposit Items") && !title.equals("Withdraw Items") && !title.equals("Bank Stats")
                && !title.equals("Duel Menu") && !title.startsWith("Select Player to Duel") && !title.startsWith("Challenge ")
                && !title.equals("Apocalypse Menu") && !title.equals("Confirm Start?")) return;

        if (title.equals("Apocalypse Menu")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            Player p = (Player) event.getWhoClicked();
            UUID uuid = p.getUniqueId();
            
            if (slot == 11) {
                playerApocalypseDifficulty.put(uuid, "easy");
                openApocalypseGui(p);
            } else if (slot == 12) {
                playerApocalypseDifficulty.put(uuid, "normal");
                openApocalypseGui(p);
            } else if (slot == 13) {
                playerApocalypseDifficulty.put(uuid, "hard");
                openApocalypseGui(p);
            } else if (slot == 14) {
                playerApocalypseDifficulty.put(uuid, "apocalypse");
                openApocalypseGui(p);
            } else if (slot == 33) {
                int zombieKills = apocalypseZombieKillsMap.getOrDefault(uuid, 0);
                long longestSurvival = apocalypseLongestSurvivalTimeMap.getOrDefault(uuid, 0L);
                p.closeInventory();
                p.sendMessage(Component.text("🧟 Apocalypse Stats:", NamedTextColor.GOLD));
                p.sendMessage(Component.text("- Zombie Kills: " + zombieKills, NamedTextColor.YELLOW));
                p.sendMessage(Component.text("- Longest Survival: " + formatTimePlayed((int) longestSurvival), NamedTextColor.YELLOW));
            } else if (slot == 29) {
                String difficulty = playerApocalypseDifficulty.getOrDefault(uuid, "None");
                if (difficulty.equalsIgnoreCase("None")) {
                    p.sendMessage(Component.text("❌ Please choose a difficulty first!", NamedTextColor.RED));
                    return;
                }
                openApocalypseConfirmGui(p);
            }
            return;
        }

        if (title.equals("Confirm Start?")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            Player p = (Player) event.getWhoClicked();
            UUID uuid = p.getUniqueId();
            
            if (slot == 11) { // Confirm
                String difficulty = playerApocalypseDifficulty.getOrDefault(uuid, "None");
                if (difficulty.equalsIgnoreCase("None")) {
                    p.sendMessage(Component.text("❌ Please choose a difficulty first!", NamedTextColor.RED));
                    p.closeInventory();
                    return;
                }
                
                World apocWorld = Bukkit.getWorld("apocalypse");
                if (apocWorld == null) {
                    p.sendMessage(Component.text("🌀 Initializing the Apocalypse world, please wait...", NamedTextColor.YELLOW));
                    WorldCreator creator = new WorldCreator("apocalypse");
                    creator.environment(World.Environment.NORMAL);
                    apocWorld = Bukkit.createWorld(creator);
                }
                
                if (apocWorld == null) {
                    p.sendMessage(Component.text("❌ Error loading the Apocalypse world!", NamedTextColor.RED));
                    p.closeInventory();
                    return;
                }

                apocWorld.setTime(18000L);
                apocWorld.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);

                // Set 64x64 barrier (WorldBorder)
                Location spawnLoc = apocWorld.getSpawnLocation();
                WorldBorder border = apocWorld.getWorldBorder();
                border.setCenter(spawnLoc.getX(), spawnLoc.getZ());
                border.setSize(64.0);
                
                if (difficulty.equalsIgnoreCase("easy")) {
                    apocWorld.setDifficulty(org.bukkit.Difficulty.EASY);
                } else {
                    apocWorld.setDifficulty(org.bukkit.Difficulty.HARD);
                }
                
                p.closeInventory();
                


                // Clear any existing active zombies for this player first
                var active = playerActiveApocalypseZombies.get(uuid);
                if (active != null) {
                    for (UUID zUuid : active) {
                        org.bukkit.entity.Entity ent = Bukkit.getEntity(zUuid);
                        if (ent != null) ent.remove();
                    }
                    active.clear();
                }
                
                apocalypseStartTimeMap.put(uuid, System.currentTimeMillis());
                playerApocalypseWaveMap.put(uuid, 1);
                
                p.teleport(spawnLoc);
                p.sendTitle("§4APOCALYPSE STARTED", "§7Difficulty: " + capitalize(difficulty), 10, 40, 10);
                p.sendMessage(Component.text("💀 You have entered the Apocalypse! Good luck...", NamedTextColor.RED));
                
                // Spawn Wave 1
                spawnNextApocalypseWave(p);
            } else if (slot == 15) { // Cancel
                p.closeInventory();
                openApocalypseGui(p);
            }
            return;
        }

        if (title.startsWith("Kick: ")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            Player p = (Player) event.getWhoClicked();
            UUID leaderUUID = p.getUniqueId();
            UUID targetUUID = pendingMemberKicks.get(leaderUUID);
            if (slot == 11) {
                p.closeInventory();
                if (targetUUID != null) {
                    String teamLower = playerTeams.get(leaderUUID);
                    if (teamLower != null) {
                        TeamData data = teams.get(teamLower);
                        if (data != null && leaderUUID.equals(data.leader)) {
                            data.members.remove(targetUUID);
                            playerTeams.remove(targetUUID);
                            saveTeams();
                            p.sendMessage(Component.text("✅ Member kicked successfully.", NamedTextColor.GREEN));
                            
                            Player kickedPlayer = Bukkit.getPlayer(targetUUID);
                            if (kickedPlayer != null && kickedPlayer.isOnline()) {
                                kickedPlayer.sendMessage(Component.text("❌ You have been kicked from the team \"" + data.name + "\".", NamedTextColor.RED));
                            }
                        }
                    }
                }
                pendingMemberKicks.remove(leaderUUID);
                openTeamGui(p);
            } else if (slot == 15) {
                p.closeInventory();
                pendingMemberKicks.remove(leaderUUID);
                openTeamGui(p);
            }
            return;
        }

        if (title.startsWith("Team: ")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            Player p = (Player) event.getWhoClicked();
            if (slot == 48) {
                p.closeInventory();
                p.sendMessage(Component.text("🔍 Please type the player name you want to search for in chat!", NamedTextColor.YELLOW));
                pendingPlayerSearch.put(p.getUniqueId(), playerTeams.get(p.getUniqueId()));
            } else if (slot == 50) {
                p.closeInventory();
                openTeamRulesBook(p);
            } else if (slot >= 0 && slot <= 44) {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.getType() == Material.PLAYER_HEAD) {
                    String teamLower = playerTeams.get(p.getUniqueId());
                    if (teamLower != null) {
                        TeamData data = teams.get(teamLower);
                        if (data != null && p.getUniqueId().equals(data.leader)) {
                            org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) clickedItem.getItemMeta();
                            if (skullMeta != null && skullMeta.getOwningPlayer() != null) {
                                UUID clickedUUID = skullMeta.getOwningPlayer().getUniqueId();
                                if (!clickedUUID.equals(data.leader)) {
                                    pendingMemberKicks.put(p.getUniqueId(), clickedUUID);
                                    p.closeInventory();
                                    openKickConfirmationGui(p, skullMeta.getOwningPlayer().getName() != null ? skullMeta.getOwningPlayer().getName() : "Unknown");
                                }
                            }
                        }
                    }
                }
            }
            return;
        }

        // Do not cancel clicks in "List an Item" GUI, "Setup Crate Shop", or "Deposit Items" because players need to place/take items.
        if (!title.equals("List an Item") && !title.equals("Setup Crate Shop") && !title.equals("Deposit Items")) {
            event.setCancelled(true);
        }

        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();

        if (title.equals("Deposit Items")) {
            return;
        }

        if (title.equals("Setup Crate Shop")) {
            // Allow all clicks to place items in any of the 9 slots
            return;
        }

        if (title.equals("Bank")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 11) {
                Inventory depositInv = Bukkit.createInventory(null, 54, Component.text("Deposit Items"));
                player.openInventory(depositInv);
            } else if (slot == 13) {
                openSignInput(player, SignAction.BANK_DEPOSIT, null, "Deposit: 100 erpies");
            } else if (slot == 15) {
                openBankWithdrawGui(player);
            } else if (slot == 17) {
                openBankStatsGui(player);
            }
            return;
        }

        if (title.equals("Bank Stats")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 22) {
                openBankGui(player);
            }
            return;
        }

        if (title.equals("Withdraw Items")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 53) {
                openBankGui(player);
                return;
            }
            if (slot == 49) {
                openSignInput(player, SignAction.BANK_WITHDRAW, null, "Withdraw: 100 erpies");
                return;
            }
            if (slot >= 0 && slot < 45) {
                List<ItemStack> bankItems = bankItemsMap.getOrDefault(uuid, new ArrayList<>());
                List<ItemStack> compactItems = new ArrayList<>();
                for (ItemStack item : bankItems) {
                    if (item != null && item.getType() != Material.AIR) {
                        compactItems.add(item);
                    }
                }
                
                if (slot < compactItems.size()) {
                    ItemStack toWithdraw = compactItems.get(slot);
                    HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(toWithdraw.clone());
                    if (remaining.isEmpty()) {
                        compactItems.remove(slot);
                    } else {
                        int withdrawn = toWithdraw.getAmount() - remaining.get(0).getAmount();
                        if (withdrawn > 0) {
                            toWithdraw.setAmount(remaining.get(0).getAmount());
                        } else {
                            player.sendMessage(Component.text("❌ Your inventory is full!", NamedTextColor.RED));
                            return;
                        }
                    }
                    bankItemsMap.put(uuid, compactItems);
                    savePlayerData(player);
                    openBankWithdrawGui(player);
                }
            }
            return;
        }

        if (title.equals("Duel Menu")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 11) {
                player.closeInventory();
                UUID pUuid = player.getUniqueId();
                if (duelQueue.contains(pUuid)) {
                    player.sendMessage(Component.text("❌ You are already in the duel queue!", NamedTextColor.RED));
                    return;
                }
                int activeFighters = 0;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getWorld().getName().equalsIgnoreCase("duel")) {
                        activeFighters++;
                    }
                }
                if (activeFighters >= 2) {
                    player.sendMessage(Component.text("❌ There is still a dual waiting in queue", NamedTextColor.RED));
                    return;
                }
                duelQueue.add(pUuid);
                player.sendMessage(Component.text("✅ You joined the duel queue! (" + duelQueue.size() + " players queued)", NamedTextColor.GREEN));
                checkAndStartQueuedDuel();
            } else if (slot == 13) {
                player.closeInventory();
                UUID pUuid = player.getUniqueId();
                if (duelQueue.remove(pUuid)) {
                    player.sendMessage(Component.text("✅ You left the duel queue.", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("❌ You were not in the duel queue.", NamedTextColor.RED));
                }
            } else if (slot == 15) {
                openDirectDuelSelectorGui(player, 0, null);
            }
            return;
        }

        if (title.startsWith("Select Player to Duel")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            int page = duelPlayerPage.getOrDefault(uuid, 0);
            String search = duelPlayerSearchQuery.get(uuid);

            if (slot == 45) {
                if (page > 0) {
                    openDirectDuelSelectorGui(player, page - 1, search);
                }
                return;
            }
            if (slot == 49) {
                player.closeInventory();
                openSignInput(player, SignAction.DUEL_PLAYER_SEARCH, null, "Search Name");
                return;
            }
            if (slot == 53) {
                List<Player> targetPlayers = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.equals(player)) continue;
                    if (search != null && !p.getName().toLowerCase().contains(search.toLowerCase())) continue;
                    targetPlayers.add(p);
                }
                if ((page + 1) * 45 < targetPlayers.size()) {
                    openDirectDuelSelectorGui(player, page + 1, search);
                }
                return;
            }

            if (slot >= 0 && slot < 45) {
                ItemStack item = event.getCurrentItem();
                if (item != null && item.getType() == Material.PLAYER_HEAD) {
                    String name = PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName()).trim();
                    name = name.replaceAll("§[0-9a-fk-orxX]", "");
                    Player target = Bukkit.getPlayer(name);
                    if (target != null && target.isOnline()) {
                        openDirectDuelConfirmationGui(player, target);
                    } else {
                        player.sendMessage(Component.text("❌ Player not found or offline.", NamedTextColor.RED));
                    }
                }
            }
            return;
        }

        if (title.startsWith("Challenge ")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            UUID targetUuid = pendingDirectDuelChallenge.remove(uuid);
            player.closeInventory();
            if (slot == 11) {
                if (targetUuid != null) {
                    Player target = Bukkit.getPlayer(targetUuid);
                    if (target != null && target.isOnline()) {
                        pendingDuelInvites.put(targetUuid, uuid);
                        player.sendMessage(Component.text("⚔️ Duel challenge sent to " + target.getName() + "!", NamedTextColor.GREEN));
                        target.sendMessage(Component.text("⚔️ " + player.getName() + " has challenged you to a duel!", NamedTextColor.GOLD));
                        target.sendMessage(Component.text("👉 Type /dualaccept to accept (expires in 60s).", NamedTextColor.YELLOW));
                        
                        Bukkit.getScheduler().runTaskLater(this, () -> {
                            if (pendingDuelInvites.containsKey(targetUuid) && pendingDuelInvites.get(targetUuid).equals(uuid)) {
                                pendingDuelInvites.remove(targetUuid);
                                if (target.isOnline()) {
                                    target.sendMessage(Component.text("⏳ Duel challenge from " + player.getName() + " has expired.", NamedTextColor.GRAY));
                                }
                                if (player.isOnline()) {
                                    player.sendMessage(Component.text("⏳ Duel challenge to " + target.getName() + " has expired.", NamedTextColor.GRAY));
                                }
                            }
                        }, 1200L);
                    } else {
                        player.sendMessage(Component.text("❌ Player not found or offline.", NamedTextColor.RED));
                    }
                }
            } else if (slot == 15) {
                player.sendMessage(Component.text("❌ Challenge cancelled.", NamedTextColor.RED));
            }
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
                        } else if (data.priceType.equalsIgnoreCase("End keys")) {
                            balance = endKeysMap.getOrDefault(uuid, 0);
                        } else if (data.priceType.equalsIgnoreCase("amethyst keys")) {
                            balance = amethystKeysMap.getOrDefault(uuid, 0);
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
                        } else if (data.priceType.equalsIgnoreCase("End keys")) {
                            endKeysMap.put(uuid, (int) (balance - data.price));
                        } else if (data.priceType.equalsIgnoreCase("amethyst keys")) {
                            amethystKeysMap.put(uuid, (int) (balance - data.price));
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
        // A. Homes Menu GUI
        if (title.equals("Homes Menu")) {
            event.setCancelled(true);
            int rawSlot = event.getRawSlot();
            Location[] homes = playerHomes.computeIfAbsent(uuid, k -> new Location[36]);
            String[] homeNames = playerHomeNames.computeIfAbsent(uuid, k -> {
                String[] names = new String[36];
                for (int i = 0; i < 36; i++) {
                    names[i] = "Home " + (i + 1);
                }
                return names;
            });

            boolean renameMode = renameModeActive.getOrDefault(uuid, false);
            boolean deleteMode = deleteModeActive.getOrDefault(uuid, false);

            if (rawSlot >= 0 && rawSlot <= 35) {
                int limit = getPlayerMaxHomes(player);
                if (rawSlot >= limit) {
                    player.sendMessage(Component.text("❌ This home slot is locked! Upgrade your store rank to unlock.", NamedTextColor.RED));
                    return;
                }
                int homeIdx = rawSlot;
                if (deleteMode) {
                    // Remove home
                    homes[homeIdx] = null;
                    homeNames[homeIdx] = "Home " + (homeIdx + 1);
                    deleteModeActive.put(uuid, false);
                    player.sendMessage(Component.text("❌ " + homeNames[homeIdx] + " removed!", NamedTextColor.RED));
                    savePlayerData(player);
                    openUnifiedHomeGui(player);
                } else if (renameMode) {
                    // Open rename sign
                    renameModeActive.put(uuid, false);
                    renamingHomeIndex.put(uuid, homeIdx);
                    Bukkit.getScheduler().runTask(this, () -> openSignInput(player, SignAction.HOME_RENAME, null, "Rename Home Name"));
                } else {
                    // Normal click: Teleport or Set
                    if (homes[homeIdx] != null) {
                        player.closeInventory();
                        performHomeCountdown(player, homes[homeIdx], homeIdx + 1);
                    } else {
                        homes[homeIdx] = player.getLocation();
                        player.sendMessage(Component.text("✅ " + homeNames[homeIdx] + " set to your current location!", NamedTextColor.GREEN));
                        savePlayerData(player);
                        openUnifiedHomeGui(player);
                    }
                }
            } else if (rawSlot == 47) {
                // Search home
                renameModeActive.put(uuid, false);
                deleteModeActive.put(uuid, false);
                Bukkit.getScheduler().runTask(this, () -> openSignInput(player, SignAction.HOME_SEARCH, null, "Search Name"));
            } else if (rawSlot == 49) {
                // Rename home toggle
                renameModeActive.put(uuid, !renameMode);
                deleteModeActive.put(uuid, false);
                if (!renameMode) {
                    player.sendMessage(Component.text("⚙️ Rename mode active! Click the home button above that you want to rename.", NamedTextColor.YELLOW));
                }
                openUnifiedHomeGui(player);
            } else if (rawSlot == 51) {
                // Remove home toggle
                deleteModeActive.put(uuid, !deleteMode);
                renameModeActive.put(uuid, false);
                if (!deleteMode) {
                    player.sendMessage(Component.text("❌ Delete mode active! Click the home button above that you want to remove.", NamedTextColor.RED));
                }
                openUnifiedHomeGui(player);
            } else if (rawSlot == 53) {
                // Team Home teleport
                player.closeInventory();
                String teamNameClick = playerTeams.get(uuid);
                if (teamNameClick == null) {
                    player.sendMessage(Component.text("❌ You are not in a team!", NamedTextColor.RED));
                    return;
                }
                TeamData tdClick = teams.get(teamNameClick);
                if (tdClick == null || tdClick.teamHome == null) {
                    player.sendMessage(Component.text("❌ Your team does not have a home set yet!", NamedTextColor.RED));
                    return;
                }
                performHomeCountdown(player, tdClick.teamHome, -1);
                player.sendMessage(Component.text("🏠 Teleporting to Team Home: " + tdClick.name, NamedTextColor.AQUA));
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
                savePlayerData(player);
                openSettingsGui(player);

            } else if (rawSlot == 15) {
                boolean current = tpaDisabled.getOrDefault(uuid, false);
                tpaDisabled.put(uuid, !current);
                player.sendMessage(Component.text("⚙️ Auto-reject TPA requests: " + (!current ? "ON" : "OFF"), NamedTextColor.YELLOW));
                savePlayerData(player);
                openSettingsGui(player);
            }
            return;
        }

        if (title.equals("Custom Items Admin Panel")) {
            event.setCancelled(true);
            if (clicked != null && clicked.getType() != Material.AIR) {
                ItemStack clone = clicked.clone();
                if (event.isShiftClick()) {
                    clone.setAmount(clone.getMaxStackSize());
                } else {
                    clone.setAmount(1);
                }
                
                java.util.HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(clone);
                for (ItemStack drop : remaining.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.5f);
                player.sendMessage(Component.text("✨ Obtained " + (clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName() ? PlainTextComponentSerializer.plainText().serialize(clicked.getItemMeta().displayName()) : clicked.getType().name()), NamedTextColor.GREEN));
            }
            return;
        }

        if (title.equals("Choose Text Color")) {
            event.setCancelled(true);
            player.closeInventory();
            
            Location loc = activeFloatingTextPlacement.remove(uuid);
            String text = activeFloatingTextContent.remove(uuid);
            originalBlockState.remove(uuid);
            
            if (loc == null || text == null) return;
            
            NamedTextColor color = NamedTextColor.WHITE;
            boolean rainbow = false;
            
            switch (clicked.getType()) {
                case RED_WOOL -> color = NamedTextColor.RED;
                case ORANGE_WOOL -> color = NamedTextColor.GOLD;
                case YELLOW_WOOL -> color = NamedTextColor.YELLOW;
                case GREEN_WOOL -> color = NamedTextColor.GREEN;
                case LIGHT_BLUE_WOOL -> color = NamedTextColor.AQUA;
                case BLUE_WOOL -> color = NamedTextColor.BLUE;
                case PURPLE_WOOL -> color = NamedTextColor.LIGHT_PURPLE;
                case WHITE_WOOL -> color = NamedTextColor.WHITE;
                case BLACK_WOOL -> rainbow = true;
                default -> {}
            }
            
            Component textComp;
            if (rainbow) {
                textComp = createRainbowComponent(text);
            } else {
                Component parsed = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(text);
                textComp = parsed.colorIfAbsent(color).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD);
            }
            
            org.bukkit.entity.TextDisplay textDisplay = loc.getWorld().spawn(loc.clone().add(0, 0.5, 0), org.bukkit.entity.TextDisplay.class);
            textDisplay.text(textComp);
            textDisplay.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
            textDisplay.setInvulnerable(true);
            textDisplay.setSeeThrough(false);
            textDisplay.setShadowed(true);
            textDisplay.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            
            textDisplay.getPersistentDataContainer().set(new NamespacedKey(this, "is_floating_text"), PersistentDataType.BOOLEAN, true);
            textDisplay.getPersistentDataContainer().set(new NamespacedKey(this, "floating_text_placer"), PersistentDataType.STRING, uuid.toString());
            
            player.playSound(loc, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.sendMessage(Component.text("✅ Spawned floating text!", NamedTextColor.GREEN));
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

        if (title.equals("Shop - Confirm Purchase")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            Material mat = cartItem.get(uuid);
            if (mat == null) return;
            int qty = cartQuantity.getOrDefault(uuid, 1);
            int unitPrice = cartUnitPrice.getOrDefault(uuid, 0);

            if (slot == 9) {
                player.sendMessage(Component.text("❌ Purchase cancelled.", NamedTextColor.RED));
                returnToCategory(player);
            } else if (slot == 10) {
                qty = Math.max(1, qty - 1);
                cartQuantity.put(uuid, qty);
                openCartGui(player);
            } else if (slot == 11) {
                qty = Math.max(1, qty - 5);
                cartQuantity.put(uuid, qty);
                openCartGui(player);
            } else if (slot == 12) {
                qty = Math.max(1, qty - 10);
                cartQuantity.put(uuid, qty);
                openCartGui(player);
            } else if (slot == 15) {
                qty = Math.min(640, qty + 1);
                cartQuantity.put(uuid, qty);
                openCartGui(player);
            } else if (slot == 16) {
                qty = Math.min(640, qty + 5);
                cartQuantity.put(uuid, qty);
                openCartGui(player);
            } else if (slot == 17) {
                qty = Math.min(640, qty + 10);
                cartQuantity.put(uuid, qty);
                openCartGui(player);
            } else if (slot == 14) {
                int totalCost = unitPrice * qty;
                long playerMoney = erpiesMap.getOrDefault(uuid, 0L);
                if (playerMoney < totalCost) {
                    player.sendMessage(Component.text("❌ You don't have enough Erpies! Cost: " + totalCost + " Erpies.", NamedTextColor.RED));
                    return;
                }

                erpiesMap.put(uuid, playerMoney - totalCost);
                int remainingToGive = qty;
                while (remainingToGive > 0) {
                    int giveAmount = Math.min(remainingToGive, mat.getMaxStackSize());
                    HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(new ItemStack(mat, giveAmount));
                    for (ItemStack left : remaining.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), left);
                    }
                    remainingToGive -= giveAmount;
                }

                player.sendMessage(Component.text("🛍️ Successfully purchased " + qty + "x " + capitalize(mat.name().replace("_", " ")) + " for " + totalCost + " Erpies!", NamedTextColor.GREEN));
                cartItem.remove(uuid);
                cartQuantity.remove(uuid);
                cartUnitPrice.remove(uuid);
                returnToCategory(player);
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
            int rawSlot = event.getRawSlot();

            long derpCost = -1;
            if (rawSlot == 10) derpCost = 100;
            else if (rawSlot == 19) derpCost = 2000;
            else if (rawSlot == 20) derpCost = 2000;
            else if (rawSlot == 21) derpCost = 2000;

            if (derpCost == -1) return;

            long playerDerpies = derpiesMap.getOrDefault(uuid, 0L);
            if (playerDerpies < derpCost) {
                player.sendMessage(Component.text("❌ You don't have enough Derpies!", NamedTextColor.RED));
                return;
            }

            derpiesMap.put(uuid, playerDerpies - derpCost);
            if (rawSlot == 10) {
                keysMap.put(uuid, keysMap.getOrDefault(uuid, 0) + 1);
            }

            if (rawSlot == 10) {
                regularKeysMap.put(uuid, regularKeysMap.getOrDefault(uuid, 0) + 1);
            } else if (rawSlot == 19) {
                HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(createFoodGeneratorItem());
                for (ItemStack left : remaining.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), left);
                }
            } else if (rawSlot == 20) {
                HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(createOreGeneratorItem());
                for (ItemStack left : remaining.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), left);
                }
            } else if (rawSlot == 21) {
                HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(createToolsGeneratorItem());
                for (ItemStack left : remaining.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), left);
                }
            }

            player.sendMessage(Component.text("🛍️ Successfully purchased 1x " + clicked.getItemMeta().getDisplayName() + "!", NamedTextColor.GREEN));
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

        cartItem.put(uuid, clicked.getType());
        cartQuantity.put(uuid, 1);
        cartUnitPrice.put(uuid, cost);
        cartCategory.put(uuid, title);
        openCartGui(player);
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

    private void applyInterest(UUID uuid) {
        long last = lastInterestTimeMap.getOrDefault(uuid, 0L);
        if (last == 0L) return;

        long now = System.currentTimeMillis();
        long diffMs = now - last;
        long periodMs = 24L * 60 * 60 * 1000L;
        if (diffMs >= periodMs) {
            long periods = diffMs / periodMs;
            long erpies = bankErpiesMap.getOrDefault(uuid, 0L);
            long derpies = bankDerpiesMap.getOrDefault(uuid, 0L);
            if (erpies > 0 || derpies > 0) {
                for (int i = 0; i < periods; i++) {
                    erpies = (long) (erpies * 1.05);
                    derpies = (long) (derpies * 1.05);
                }
                bankErpiesMap.put(uuid, erpies);
                bankDerpiesMap.put(uuid, derpies);
            }
            lastInterestTimeMap.put(uuid, last + (periods * periodMs));
        }
    }

    private void openBankGui(Player player) {
        UUID uuid = player.getUniqueId();
        applyInterest(uuid);

        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Bank"));
        ItemStack pane = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.GRAY);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, pane);
        }

        inv.setItem(11, createGuiItem(Material.CHEST, "Deposit Items", NamedTextColor.GREEN, "Click to deposit items"));
        inv.setItem(13, createGuiItem(Material.OAK_SIGN, "Deposit Money", NamedTextColor.GOLD, "Click to deposit Erpies/Derpies"));
        inv.setItem(15, createGuiItem(Material.ENDER_CHEST, "Withdraw", NamedTextColor.AQUA, "Click to withdraw items/money"));
        inv.setItem(17, createGuiItem(Material.BOOK, "Stats", NamedTextColor.LIGHT_PURPLE, "Click to view your bank stats"));

        player.openInventory(inv);
    }

    private void openBankStatsGui(Player player) {
        UUID uuid = player.getUniqueId();
        applyInterest(uuid);

        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Bank Stats"));
        ItemStack pane = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.GRAY);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, pane);
        }

        long erpies = bankErpiesMap.getOrDefault(uuid, 0L);
        long derpies = bankDerpiesMap.getOrDefault(uuid, 0L);

        inv.setItem(11, createGuiItem(Material.GOLD_BLOCK, "Deposited Erpies", NamedTextColor.GOLD, "Amount: " + erpies + " Erpies", "Interest Rate: +5% every 24 hours"));
        inv.setItem(15, createGuiItem(Material.DIAMOND_BLOCK, "Deposited Derpies", NamedTextColor.AQUA, "Amount: " + derpies + " Derpies", "Interest Rate: +5% every 24 hours"));
        inv.setItem(22, createGuiItem(Material.ARROW, "Back to Bank", NamedTextColor.YELLOW, "Click to go back"));

        player.openInventory(inv);
    }

    private void openBankWithdrawGui(Player player) {
        UUID uuid = player.getUniqueId();
        applyInterest(uuid);

        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Withdraw Items"));
        List<ItemStack> items = bankItemsMap.getOrDefault(uuid, new ArrayList<>());

        int idx = 0;
        for (ItemStack item : items) {
            if (idx >= 45) break;
            if (item != null && item.getType() != Material.AIR) {
                inv.setItem(idx++, item.clone());
            }
        }

        ItemStack pane = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.GRAY);
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, pane);
        }

        inv.setItem(49, createGuiItem(Material.OAK_SIGN, "Withdraw Money", NamedTextColor.GOLD, "Click to withdraw Erpies or Derpies"));
        inv.setItem(53, createGuiItem(Material.ARROW, "Back to Bank", NamedTextColor.YELLOW, "Click to go back"));

        player.openInventory(inv);
    }

    private ItemStack createGuiItem(Material material, String name, NamedTextColor color, String... descriptionLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, color));
            List<Component> lore = new java.util.ArrayList<>();
            for (String line : descriptionLines) {
                if (line.startsWith("§a")) {
                    lore.add(Component.text(line.substring(2), NamedTextColor.GREEN));
                } else if (line.startsWith("§c")) {
                    lore.add(Component.text(line.substring(2), NamedTextColor.RED));
                } else {
                    lore.add(Component.text(line, NamedTextColor.DARK_GRAY));
                }
            }
            meta.lore(lore);
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

        if (activeFloatingTextPlacement.containsKey(uuid)) {
            Location loc = activeFloatingTextPlacement.get(uuid);
            
            List<Component> lines = event.lines();
            List<String> textLines = new ArrayList<>();
            for (Component line : lines) {
                String str = PlainTextComponentSerializer.plainText().serialize(line).trim();
                if (!str.isEmpty()) textLines.add(str);
            }
            String joinedText = String.join("\n", textLines);
            
            BlockBackup backup = originalBlockState.remove(uuid);
            if (backup != null) {
                loc.getBlock().setType(backup.material, false);
                loc.getBlock().setBlockData(backup.data, false);
            } else {
                loc.getBlock().setType(Material.AIR, false);
            }
            
            if (joinedText.isEmpty()) {
                player.sendMessage(Component.text("❌ Floating text cannot be empty!", NamedTextColor.RED));
                activeFloatingTextPlacement.remove(uuid);
                
                ItemStack returned = createFloatingTextItem();
                java.util.HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(returned);
                for (ItemStack drop : remaining.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
                return;
            }
            
            activeFloatingTextContent.put(uuid, joinedText);
            openColorSelectionGui(player);
            return;
        }

        if (pendingSigns.containsKey(uuid)) {
            PendingSignInput pending = pendingSigns.remove(uuid);
            pending.loc.getBlock().setBlockData(pending.originalData);

            String input = "";
            for (int i = 0; i < 4; i++) {
                String line = PlainTextComponentSerializer.plainText().serialize(event.line(i)).trim();
                if (line.equalsIgnoreCase("search here") || line.equalsIgnoreCase("list the price") || line.isEmpty()
                        || line.equalsIgnoreCase("Enter Command") || line.equalsIgnoreCase("Search Name") || line.equalsIgnoreCase("Rename Home Name") || line.equalsIgnoreCase("Rename Home Title") || line.toLowerCase().startsWith("deposit:")
                        || line.toLowerCase().startsWith("withdraw:") || line.toLowerCase().startsWith("price (")
                        || line.toLowerCase().startsWith("price:") || line.toLowerCase().startsWith("enter")) {
                    continue;
                }
                input = line;
                break;
            }
            if (input.isEmpty()) {
                String line0 = PlainTextComponentSerializer.plainText().serialize(event.line(0)).trim();
                if (!line0.equalsIgnoreCase("search here") && !line0.equalsIgnoreCase("list the price")
                        && !line0.equalsIgnoreCase("Enter Command") && !line0.equalsIgnoreCase("Search Name") && !line0.equalsIgnoreCase("Rename Home Name") && !line0.equalsIgnoreCase("Rename Home Title") && !line0.toLowerCase().startsWith("deposit:")
                        && !line0.toLowerCase().startsWith("withdraw:") && !line0.toLowerCase().startsWith("price (")
                        && !line0.toLowerCase().startsWith("price:") && !line0.toLowerCase().startsWith("enter")) {
                    input = line0;
                }
            }

            if (pending.action == SignAction.HOME_SEARCH) {
                if (input.isEmpty()) {
                    player.sendMessage(Component.text("❌ Search cancelled.", NamedTextColor.RED));
                } else {
                    String query = input.toLowerCase();
                    Location[] homes = playerHomes.get(uuid);
                    String[] homeNames = playerHomeNames.get(uuid);
                    
                    int foundIdx = -1;
                    if (homes != null && homeNames != null) {
                        for (int i = 0; i < 5; i++) {
                            if (homes[i] != null && homeNames[i] != null && homeNames[i].toLowerCase().contains(query)) {
                                foundIdx = i;
                                break;
                            }
                        }
                    }
                    
                    if (foundIdx != -1) {
                        final Location targetLoc = homes[foundIdx];
                        final int finalIdx = foundIdx;
                        player.sendMessage(Component.text("🔍 Found home: " + homeNames[foundIdx] + "! Teleporting...", NamedTextColor.GREEN));
                        Bukkit.getScheduler().runTask(this, () -> performHomeCountdown(player, targetLoc, finalIdx + 1));
                        return;
                    } else {
                        player.sendMessage(Component.text("❌ No home matching '" + input + "' was found!", NamedTextColor.RED));
                    }
                }
                Bukkit.getScheduler().runTask(this, () -> openUnifiedHomeGui(player));
                return;
            }

            if (pending.action == SignAction.HOME_RENAME) {
                Integer homeIdxObj = renamingHomeIndex.remove(uuid);
                if (homeIdxObj != null) {
                    int homeIdx = homeIdxObj;
                    if (input.isEmpty()) {
                        player.sendMessage(Component.text("❌ Rename cancelled.", NamedTextColor.RED));
                    } else {
                        String[] homeNames = playerHomeNames.computeIfAbsent(uuid, k -> new String[]{"Home 1", "Home 2", "Home 3", "Home 4", "Home 5"});
                        homeNames[homeIdx] = input;
                        player.sendMessage(Component.text("✅ Renamed Home " + (homeIdx + 1) + " to '" + input + "'!", NamedTextColor.GREEN));
                        savePlayerData(player);
                    }
                }
                Bukkit.getScheduler().runTask(this, () -> openUnifiedHomeGui(player));
                return;
            }

            if (pending.action == SignAction.BANK_DEPOSIT) {
                if (input.isEmpty()) {
                    player.sendMessage(Component.text("❌ Deposit cancelled.", NamedTextColor.RED));
                } else {
                    try {
                        String inputLower = input.toLowerCase().replaceAll("[()\\s]", "");
                        String amountStr = "";
                        String currencyStr = "";

                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^([0-9.]+(?:[kmbt])?)(.*)$").matcher(inputLower);
                        if (m.matches()) {
                            amountStr = m.group(1);
                            currencyStr = m.group(2);
                        } else {
                            java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("^(erpies|derpies|erpie|derpie|erp|derp|e|d)([0-9.]+(?:[kmbt])?)$").matcher(inputLower);
                            if (m2.matches()) {
                                currencyStr = m2.group(1);
                                amountStr = m2.group(2);
                            }
                        }

                        long amount = parseAmountWithSuffix(amountStr);
                        String currency = null;
                        if (currencyStr.contains("derp") || currencyStr.equals("d")) {
                            currency = "derpies";
                        } else if (currencyStr.contains("erp") || currencyStr.equals("e")) {
                            currency = "erpies";
                        }

                        if (amount <= 0 || currency == null) {
                            player.sendMessage(Component.text("❌ Invalid format. Please enter like '100 erpies' or '1m derpies'!", NamedTextColor.RED));
                        } else {
                            long playerBal = 0;
                            if (currency.equals("erpies")) {
                                playerBal = erpiesMap.getOrDefault(uuid, 0L);
                            } else {
                                playerBal = derpiesMap.getOrDefault(uuid, 0L);
                            }

                            if (playerBal < amount) {
                                player.sendMessage(Component.text("❌ You don't have enough money! Your balance: " + playerBal + " " + currency, NamedTextColor.RED));
                            } else {
                                applyInterest(uuid);
                                if (currency.equals("erpies")) {
                                    erpiesMap.put(uuid, playerBal - amount);
                                    bankErpiesMap.put(uuid, bankErpiesMap.getOrDefault(uuid, 0L) + amount);
                                } else {
                                    derpiesMap.put(uuid, playerBal - amount);
                                    bankDerpiesMap.put(uuid, bankDerpiesMap.getOrDefault(uuid, 0L) + amount);
                                }
                                if (lastInterestTimeMap.getOrDefault(uuid, 0L) == 0L) {
                                    lastInterestTimeMap.put(uuid, System.currentTimeMillis());
                                }
                                player.sendMessage(Component.text("✅ Deposited " + amount + " " + currency + " into the bank! It will gain 5% interest daily.", NamedTextColor.GREEN));
                                updateScoreboard(player);
                                savePlayerData(player);
                            }
                        }
                    } catch (Exception e) {
                        player.sendMessage(Component.text("❌ Invalid input format! Use: (amount erpies/derpies), e.g. 100 erpies or 1m derpies.", NamedTextColor.RED));
                    }
                }
                Bukkit.getScheduler().runTask(this, () -> openBankGui(player));
            } else if (pending.action == SignAction.BANK_WITHDRAW) {
                if (input.isEmpty()) {
                    player.sendMessage(Component.text("❌ Withdrawal cancelled.", NamedTextColor.RED));
                } else {
                    try {
                        String inputLower = input.toLowerCase().replaceAll("[()\\s]", "");
                        String amountStr = "";
                        String currencyStr = "";

                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^([0-9.]+(?:[kmbt])?)(.*)$").matcher(inputLower);
                        if (m.matches()) {
                            amountStr = m.group(1);
                            currencyStr = m.group(2);
                        } else {
                            java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("^(erpies|derpies|erpie|derpie|erp|derp|e|d)([0-9.]+(?:[kmbt])?)$").matcher(inputLower);
                            if (m2.matches()) {
                                currencyStr = m2.group(1);
                                amountStr = m2.group(2);
                            }
                        }

                        long amount = parseAmountWithSuffix(amountStr);
                        String currency = null;
                        if (currencyStr.contains("derp") || currencyStr.equals("d")) {
                            currency = "derpies";
                        } else if (currencyStr.contains("erp") || currencyStr.equals("e")) {
                            currency = "erpies";
                        }

                        if (amount <= 0 || currency == null) {
                            player.sendMessage(Component.text("❌ Invalid format. Please enter like '100 erpies' or '1m derpies'!", NamedTextColor.RED));
                        } else {
                            applyInterest(uuid);
                            long bankBal = 0;
                            if (currency.equals("erpies")) {
                                bankBal = bankErpiesMap.getOrDefault(uuid, 0L);
                            } else {
                                bankBal = bankDerpiesMap.getOrDefault(uuid, 0L);
                            }

                            if (bankBal < amount) {
                                player.sendMessage(Component.text("❌ You do not have enough deposited! Deposited balance: " + bankBal + " " + currency, NamedTextColor.RED));
                            } else {
                                if (currency.equals("erpies")) {
                                    bankErpiesMap.put(uuid, bankBal - amount);
                                    erpiesMap.put(uuid, erpiesMap.getOrDefault(uuid, 0L) + amount);
                                } else {
                                    bankDerpiesMap.put(uuid, bankBal - amount);
                                    derpiesMap.put(uuid, derpiesMap.getOrDefault(uuid, 0L) + amount);
                                }
                                player.sendMessage(Component.text("✅ Withdrew " + amount + " " + currency + " from the bank!", NamedTextColor.GREEN));
                                updateScoreboard(player);
                                savePlayerData(player);
                            }
                        }
                    } catch (Exception e) {
                        player.sendMessage(Component.text("❌ Invalid input format! Use: (amount erpies/derpies), e.g. 100 erpies or 1m derpies.", NamedTextColor.RED));
                    }
                }
                Bukkit.getScheduler().runTask(this, () -> openBankWithdrawGui(player));
            } else if (pending.action == SignAction.SET_COMMAND_CHEST) {
                Location chestLoc = activeCommandChestSetup.remove(uuid);
                if (chestLoc != null) {
                    if (input.isEmpty()) {
                        player.sendMessage(Component.text("❌ Setup cancelled. Command chest removed.", NamedTextColor.RED));
                        chestLoc.getBlock().setType(Material.AIR);
                    } else {
                        String command = input.trim();
                        if (!command.startsWith("/")) {
                            command = "/" + command;
                        }
                        commandChests.put(chestLoc, command);
                        saveCommandChests();
                        player.sendMessage(Component.text("✅ Command Chest bound to command: " + command, NamedTextColor.GREEN));
                    }
                }
            } else if (pending.action == SignAction.DUEL_PLAYER_SEARCH) {
                if (input.isEmpty()) {
                    player.sendMessage(Component.text("❌ Search cancelled.", NamedTextColor.RED));
                    Bukkit.getScheduler().runTask(this, () -> openDirectDuelSelectorGui(player, 0, null));
                } else {
                    final String query = input;
                    player.sendMessage(Component.text("🔍 Searching for: " + query, NamedTextColor.GREEN));
                    Bukkit.getScheduler().runTask(this, () -> openDirectDuelSelectorGui(player, 0, query));
                }
            } else if (pending.action == SignAction.SEARCH) {
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
                                else if (data.crateType.equals("end")) targetCurrency = "End keys";
                                else if (data.crateType.equals("amethyst")) targetCurrency = "amethyst keys";
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
                rangeMin = 1000; rangeMax = 5000;
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
                    int x, z;
                    if (finalWorld.getEnvironment() == World.Environment.THE_END) {
                        double angle = random.nextDouble() * 2 * Math.PI;
                        double radius = fMin + random.nextInt(fMax - fMin);
                        x = (int) (Math.cos(angle) * radius);
                        z = (int) (Math.sin(angle) * radius);
                    } else {
                        x = random.nextInt(fMax - fMin) + fMin;
                        z = random.nextInt(fMax - fMin) + fMin;
                    }
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
                        safe = new Location(finalWorld, 1000.5, 60, 0.5);
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
        if (title.equals("Deposit Items")) {
            Player player = (Player) event.getPlayer();
            UUID uuid = player.getUniqueId();
            List<ItemStack> bankItems = bankItemsMap.computeIfAbsent(uuid, k -> new ArrayList<>());
            int depositedCount = 0;
            for (ItemStack item : event.getInventory().getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    bankItems.add(item.clone());
                    depositedCount += item.getAmount();
                }
            }
            event.getInventory().clear();
            if (depositedCount > 0) {
                player.sendMessage(Component.text("✅ Successfully deposited " + depositedCount + " items into your bank!", NamedTextColor.GREEN));
                savePlayerData(player);
            }
            return;
        }
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
                || title.equals("Random Teleport") || title.equals("Homes Menu") || title.equals("Settings")
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
                || title.equals("Random Teleport") || title.equals("Homes Menu") || title.equals("Settings")
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

        addScoreboardRow(board, obj, "TimePlayed", NamedTextColor.AQUA, formatTimePlayed(timePlayedMap.getOrDefault(uuid, 0)), 6, "§1");
        addScoreboardRow(board, obj, "Erpies", NamedTextColor.GREEN, formatValue(erpiesMap.getOrDefault(uuid, 0L)), 5, "§2");
        addScoreboardRow(board, obj, "Derpies", NamedTextColor.LIGHT_PURPLE, formatValue(derpiesMap.getOrDefault(uuid, 0L)), 4, "§3");
        addScoreboardRow(board, obj, "Keys", NamedTextColor.BLUE, String.valueOf(keysMap.getOrDefault(uuid, 0)), 3, "§4");
        addScoreboardRow(board, obj, "Kills", NamedTextColor.DARK_GREEN, String.valueOf(killsMap.getOrDefault(uuid, 0)), 2, "§5");
        addScoreboardRow(board, obj, "Deaths", NamedTextColor.RED, String.valueOf(deathsMap.getOrDefault(uuid, 0)), 1, "§6");
    }

    private String formatTimePlayed(int totalSeconds) {
        if (totalSeconds < 60) {
            return totalSeconds + "s";
        }
        int days = totalSeconds / 86400;
        int hours = (totalSeconds % 86400) / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m " + seconds + "s";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m " + seconds + "s";
        }
        return minutes + "m " + seconds + "s";
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

    private ItemStack createEchoShovel() {
        ItemStack shovel = new ItemStack(Material.NETHERITE_SHOVEL);
        ItemMeta meta = shovel.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Echo Shovel", NamedTextColor.AQUA));
            meta.lore(List.of(
                Component.text("Mines a 3x3 hole forward/downward.", NamedTextColor.GRAY),
                Component.text("Creates a 3x3 path when making grass paths.", NamedTextColor.GRAY)
            ));
            meta.addEnchant(Enchantment.EFFICIENCY, 3, true);
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "echo_shovel");
            shovel.setItemMeta(meta);
        }
        return shovel;
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

    private ItemStack createOrbitalStrike() {
        ItemStack rod = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = rod.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Orbital Strike", NamedTextColor.RED));
            meta.lore(List.of(
                Component.text("Launches a massive TNT strike where you look.", NamedTextColor.DARK_RED),
                Component.text("Cooldown: 2 mins", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "orbital_strike");
            rod.setItemMeta(meta);
        }
        return rod;
    }

    private ItemStack createLungeSpear() {
        ItemStack trident = new ItemStack(Material.TRIDENT);
        ItemMeta meta = trident.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Lunge Spear", NamedTextColor.GOLD));
            meta.lore(List.of(
                Component.text("Right-click to lunge forward.", NamedTextColor.YELLOW),
                Component.text("Cooldown: 5s. Switches reset cooldown.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "lunge_spear");
            trident.setItemMeta(meta);
        }
        return trident;
    }

    private ItemStack createWand() {
        ItemStack axe = new ItemStack(Material.WOODEN_AXE);
        ItemMeta meta = axe.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Wand", NamedTextColor.GOLD));
            meta.lore(List.of(
                Component.text("World editing selection wand.", NamedTextColor.YELLOW),
                Component.text("Left-click: Set Point 1", NamedTextColor.GRAY),
                Component.text("Right-click: Set Point 2", NamedTextColor.GRAY),
                Component.text("Run /copy to copy selected region.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "wand");
            axe.setItemMeta(meta);
        }
        return axe;
    }

    private ItemStack createNpcEgg() {
        ItemStack egg = new ItemStack(Material.VILLAGER_SPAWN_EGG);
        ItemMeta meta = egg.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("NPC Spawn Egg", NamedTextColor.LIGHT_PURPLE, net.kyori.adventure.text.format.TextDecoration.BOLD));
            meta.lore(List.of(
                Component.text("Spawn a customizable NPC.", NamedTextColor.YELLOW),
                Component.text("1. Rename this egg in an anvil to a player's name.", NamedTextColor.GRAY),
                Component.text("2. Right-click a block to spawn the NPC.", NamedTextColor.GRAY),
                Component.text("3. Shift+Right-click the NPC to pick it up.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "npc_egg");
            egg.setItemMeta(meta);
        }
        return egg;
    }

    private ItemStack createFloatingTextItem() {
        ItemStack sign = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = sign.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Floating Text", NamedTextColor.LIGHT_PURPLE, net.kyori.adventure.text.format.TextDecoration.BOLD));
            meta.lore(List.of(
                Component.text("Place to create floating text.", NamedTextColor.YELLOW),
                Component.text("1. Place on the ground or a wall.", NamedTextColor.GRAY),
                Component.text("2. Type text in the Oak Sign editor.", NamedTextColor.GRAY),
                Component.text("3. Choose a color in the GUI.", NamedTextColor.GRAY),
                Component.text("4. Shift+Left-click the block to remove.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "floating_text");
            sign.setItemMeta(meta);
        }
        return sign;
    }

    private ItemStack createCommandChest() {
        ItemStack chest = new ItemStack(Material.CHEST);
        ItemMeta meta = chest.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Command Chest", NamedTextColor.GOLD, net.kyori.adventure.text.format.TextDecoration.BOLD));
            meta.lore(List.of(
                Component.text("Place to bind a command chest UI.", NamedTextColor.YELLOW),
                Component.text("1. Place on the ground.", NamedTextColor.GRAY),
                Component.text("2. Type a command (e.g. /rtp) in the Sign UI.", NamedTextColor.GRAY),
                Component.text("3. Right-click to open that command's UI.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "command_chest");
            chest.setItemMeta(meta);
        }
        return chest;
    }

    private void loadCommandChests() {
        commandChests.clear();
        if (!getConfig().contains("commandchests")) return;
        org.bukkit.configuration.ConfigurationSection sec = getConfig().getConfigurationSection("commandchests");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            String path = "commandchests." + key;
            String worldName = getConfig().getString(path + ".world");
            double x = getConfig().getDouble(path + ".x");
            double y = getConfig().getDouble(path + ".y");
            double z = getConfig().getDouble(path + ".z");
            World w = Bukkit.getWorld(worldName);
            if (w == null) continue;
            Location loc = new Location(w, x, y, z);
            String command = getConfig().getString(path + ".command");
            commandChests.put(loc, command);
        }
    }

    private void saveCommandChests() {
        getConfig().set("commandchests", null);
        int i = 0;
        for (var entry : commandChests.entrySet()) {
            Location loc = entry.getKey();
            String command = entry.getValue();
            String path = "commandchests.c" + (i++);
            getConfig().set(path + ".world", loc.getWorld().getName());
            getConfig().set(path + ".x", loc.getX());
            getConfig().set(path + ".y", loc.getY());
            getConfig().set(path + ".z", loc.getZ());
            getConfig().set(path + ".command", command);
        }
        saveConfig();
    }

    private ItemStack createDivineFlame() {
        ItemStack powder = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = powder.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Divine Flame", NamedTextColor.RED, net.kyori.adventure.text.format.TextDecoration.BOLD));
            meta.lore(List.of(
                Component.text("Concentrated essence of celestial fire.", NamedTextColor.YELLOW),
                Component.text("1. Left-click an entity to set them on fire.", NamedTextColor.GRAY),
                Component.text("2. Crouch + hold Right-click for 3s to charge a fireball.", NamedTextColor.GRAY),
                Component.text("3. Unleashes a massive flame tornado on impact.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "divine_flame");
            powder.setItemMeta(meta);
        }
        return powder;
    }

    private void shootDivineFlame(Player player) {
        player.sendMessage(Component.text("💥 Divine Flame unleashed!", NamedTextColor.RED));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0f, 1.0f);
        
        Location startLoc = player.getEyeLocation();
        org.bukkit.util.Vector direction = startLoc.getDirection().normalize().multiply(1.0);

        new org.bukkit.scheduler.BukkitRunnable() {
            Location currentLoc = startLoc.clone();
            int flightTicks = 0;

            @Override
            public void run() {
                if (flightTicks >= 60) {
                    createFlameTornado(currentLoc, player);
                    cancel();
                    return;
                }

                currentLoc.add(direction);
                currentLoc.getWorld().spawnParticle(Particle.FLAME, currentLoc, 5, 0.1, 0.1, 0.1, 0.02);
                currentLoc.getWorld().spawnParticle(Particle.LAVA, currentLoc, 1, 0.0, 0.0, 0.0, 0.0);

                if (currentLoc.getBlock().getType().isSolid()) {
                    createFlameTornado(currentLoc, player);
                    cancel();
                    return;
                }

                java.util.Collection<org.bukkit.entity.LivingEntity> nearby = currentLoc.getNearbyLivingEntities(1.2);
                for (org.bukkit.entity.LivingEntity entity : nearby) {
                    if (!entity.equals(player)) {
                        createFlameTornado(currentLoc, player);
                        cancel();
                        return;
                    }
                }

                flightTicks++;
            }
        }.runTaskTimer(CustomScoreboard.this, 0L, 1L);
    }

    private void createFlameTornado(Location loc, Player shooter) {
        loc.getWorld().playSound(loc, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);
        loc.getWorld().playSound(loc, org.bukkit.Sound.ITEM_FIRECHARGE_USE, 1.5f, 0.5f);

        java.util.Collection<org.bukkit.entity.LivingEntity> targets = loc.getNearbyLivingEntities(5.0);
        for (org.bukkit.entity.LivingEntity entity : targets) {
            if (!entity.equals(shooter)) {
                entity.damage(30.0, shooter);
                entity.setFireTicks(200);
            }
        }

        new org.bukkit.scheduler.BukkitRunnable() {
            int step = 0;
            @Override
            public void run() {
                if (step >= 15) {
                    cancel();
                    return;
                }
                
                for (int y = 0; y < 15; y++) {
                    double radius = 0.5 + (y * 0.2) + (step * 0.05);
                    double angle = (step * 0.5) + (y * 0.3);
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    Location particleLoc = loc.clone().add(x, y * 0.4, z);
                    loc.getWorld().spawnParticle(Particle.FLAME, particleLoc, 2, 0.05, 0.05, 0.05, 0.01);
                    if (y % 3 == 0) {
                        loc.getWorld().spawnParticle(Particle.SMALL_FLAME, particleLoc, 1, 0.0, 0.0, 0.0, 0.0);
                    }
                }
                
                step++;
            }
        }.runTaskTimer(CustomScoreboard.this, 0L, 2L);
    }

    private void openColorSelectionGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("Choose Text Color"));
        
        inv.setItem(0, createGuiItem(Material.RED_WOOL, "Red", NamedTextColor.RED, "Click to select Red"));
        inv.setItem(1, createGuiItem(Material.ORANGE_WOOL, "Gold/Orange", NamedTextColor.GOLD, "Click to select Gold"));
        inv.setItem(2, createGuiItem(Material.YELLOW_WOOL, "Yellow", NamedTextColor.YELLOW, "Click to select Yellow"));
        inv.setItem(3, createGuiItem(Material.GREEN_WOOL, "Green", NamedTextColor.GREEN, "Click to select Green"));
        inv.setItem(4, createGuiItem(Material.LIGHT_BLUE_WOOL, "Aqua/Cyan", NamedTextColor.AQUA, "Click to select Aqua"));
        inv.setItem(5, createGuiItem(Material.BLUE_WOOL, "Blue", NamedTextColor.BLUE, "Click to select Blue"));
        inv.setItem(6, createGuiItem(Material.PURPLE_WOOL, "Purple", NamedTextColor.LIGHT_PURPLE, "Click to select Purple"));
        inv.setItem(7, createGuiItem(Material.WHITE_WOOL, "White", NamedTextColor.WHITE, "Click to select White"));
        inv.setItem(8, createGuiItem(Material.BLACK_WOOL, "Rainbow", NamedTextColor.DARK_GRAY, "Click to select Rainbow color"));
        
        player.openInventory(inv);
    }

    private void openCustomItemsAdminPanel(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Custom Items Admin Panel"));
        
        // Row 1 & 2: Tools & Weapons
        inv.setItem(0, createEchoPickaxe());
        inv.setItem(1, createEchoAxe());
        inv.setItem(2, createEchoBow());
        inv.setItem(3, createKnockbackStick());
        inv.setItem(4, createSwordDerp());
        inv.setItem(5, createPickaxeLerp());
        inv.setItem(6, createMaceMerp());
        inv.setItem(7, createEchoSword());
        inv.setItem(8, createLungeSpear());
        inv.setItem(9, createEchoShovel());
        
        // Row 3: Crates
        inv.setItem(18, createShopCrate());
        inv.setItem(19, createEchoCrate());
        inv.setItem(20, createCrimsonCrate());
        inv.setItem(21, createKeyCrate());
        inv.setItem(22, createEndCrate());
        inv.setItem(23, createAmethystCrate());
        
        // Row 4: Keys
        inv.setItem(27, createEchoKey());
        inv.setItem(28, createCrimsonKey());
        inv.setItem(29, createEndKey());
        inv.setItem(30, createAmethystKey());
        
        // Row 5: Utility & Fun
        inv.setItem(36, createEndGatewayItem());
        inv.setItem(37, createOrbitalStrike());
        inv.setItem(38, createWand());
        inv.setItem(39, createNpcEgg());
        inv.setItem(40, createFloatingTextItem());
        inv.setItem(41, createFoodGeneratorItem());
        inv.setItem(42, createOreGeneratorItem());
        inv.setItem(43, createToolsGeneratorItem());
        
        player.openInventory(inv);
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
            checkAndTrackMinedOre(player, b);
            b.breakNaturally(tool);
            b.getWorld().spawnParticle(Particle.SONIC_BOOM, b.getLocation().add(0.5, 0.5, 0.5), 1, 0, 0, 0, 0);
        }
    }

    private void handleEchoShovelBreak(Player player, Block centerBlock, ItemStack tool) {
        Location centerLoc = centerBlock.getLocation();
        if (isInSpawnRadius(centerLoc)) return;

        BlockFace face = player.getTargetBlockFace(6);
        if (face == null) face = BlockFace.UP;

        List<Block> blocksToBreak = new ArrayList<>();
        blocksToBreak.add(centerBlock);

        int dx1 = 0, dz1 = 0;
        int dx2 = 0, dz2 = 0;
        int dy1 = 0, dy2 = 0;

        if (face == BlockFace.UP || face == BlockFace.DOWN) {
            dx1 = 1;
            dz2 = 1;
        } else if (face == BlockFace.NORTH || face == BlockFace.SOUTH) {
            dx1 = 1;
            dy2 = 1;
        } else if (face == BlockFace.EAST || face == BlockFace.WEST) {
            dz1 = 1;
            dy2 = 1;
        } else {
            dx1 = 1;
            dz2 = 1;
        }

        for (int h = -1; h <= 1; h++) {
            for (int v = -1; v <= 1; v++) {
                if (h == 0 && v == 0) continue;
                Block b = centerBlock.getRelative(h * dx1 + v * dx2, h * dy1 + v * dy2, h * dz1 + v * dz2);
                blocksToBreak.add(b);
            }
        }

        for (Block b : blocksToBreak) {
            if (b.getType() == Material.AIR || b.getType() == Material.BEDROCK || b.getType() == Material.BARRIER) continue;
            if (isInSpawnRadius(b.getLocation())) continue;
            checkAndTrackMinedOre(player, b);
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
        }
    }

    private int getPlayerMaxHomes(Player player) {
        UUID uuid = player.getUniqueId();
        if (hasErpProMaxMap.getOrDefault(uuid, false)) {
            return 35;
        }
        if (hasErpProMap.getOrDefault(uuid, false)) {
            return 23;
        }
        if (hasErpPlusMap.getOrDefault(uuid, false)) {
            return 14;
        }
        return 5;
    }

    private void openUnifiedHomeGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Homes Menu"));
        UUID uuid = player.getUniqueId();
        Location[] homes = playerHomes.computeIfAbsent(uuid, k -> new Location[36]);
        String[] homeNames = playerHomeNames.computeIfAbsent(uuid, k -> {
            String[] names = new String[36];
            for (int i = 0; i < 36; i++) {
                names[i] = "Home " + (i + 1);
            }
            return names;
        });

        // Fill with decorative background gray stained glass pane
        ItemStack pane = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.GRAY);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, pane);
        }

        // Check if rename/delete mode is active to show in lore
        boolean renameMode = renameModeActive.getOrDefault(uuid, false);
        boolean deleteMode = deleteModeActive.getOrDefault(uuid, false);
        String modeLore = " ";
        if (renameMode) {
            modeLore = "§e[RENAME MODE ACTIVE - Click to rename]";
        } else if (deleteMode) {
            modeLore = "§c[REMOVE MODE ACTIVE - Click to remove]";
        } else {
            modeLore = "§7Left-click: Teleport | Click if empty: Set Location";
        }

        // Slots 0 to 35: Homes / Locked Slots
        int limit = getPlayerMaxHomes(player);
        for (int i = 0; i < 36; i++) {
            if (i < limit) {
                Location loc = homes[i];
                String name = homeNames[i] != null ? homeNames[i] : "Home " + (i + 1);
                if (loc != null) {
                    String locStr = String.format("%.0f, %.0f, %.0f (%s)", loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName());
                    inv.setItem(i, createGuiItem(Material.RED_BED, name, NamedTextColor.GREEN, 
                        "Location: " + locStr, 
                        modeLore));
                } else {
                    inv.setItem(i, createGuiItem(Material.BLACK_BED, name + " (Not Set)", NamedTextColor.GRAY, 
                        "No location saved here.", 
                        renameMode || deleteMode ? modeLore : "§7Click to save your current location"));
                }
            } else {
                inv.setItem(i, createGuiItem(Material.RED_STAINED_GLASS_PANE, "§cLocked Home Slot", NamedTextColor.RED, 
                    "§7Requires a higher store rank to unlock.", 
                    "§7Purchase at play.theerpsmp.net"));
            }
        }

        // Bottom row: Search (47), Rename (49), Remove (51), Team Home (53)
        inv.setItem(47, createGuiItem(Material.COMPASS, "Search Home", NamedTextColor.YELLOW,
            "Click to search for a named home point."));
        inv.setItem(49, createGuiItem(Material.NAME_TAG, "Rename Home", NamedTextColor.GOLD,
            renameMode ? "§aStatus: ACTIVE" : "§cStatus: INACTIVE",
            "Click here, then click a home above to rename it."));
        inv.setItem(51, createGuiItem(Material.BARRIER, "Remove Home", NamedTextColor.RED,
            deleteMode ? "§aStatus: ACTIVE" : "§cStatus: INACTIVE",
            "Click here, then click a home above to delete it."));

        // Slot 53 — Team Home
        String teamNameForGui = playerTeams.get(uuid);
        if (teamNameForGui != null) {
            TeamData teamDataForGui = teams.get(teamNameForGui);
            if (teamDataForGui != null && teamDataForGui.teamHome != null) {
                Location th = teamDataForGui.teamHome;
                String thLoc = String.format("%.0f, %.0f, %.0f (%s)", th.getX(), th.getY(), th.getZ(), th.getWorld().getName());
                inv.setItem(53, createGuiItem(Material.BEACON, "Team Home", NamedTextColor.AQUA,
                    "Team: §b" + teamDataForGui.name,
                    "Location: " + thLoc,
                    "§7Click to teleport to your team's home."));
            } else {
                inv.setItem(53, createGuiItem(Material.BEACON, "Team Home", NamedTextColor.GRAY,
                    "Team: §7" + (teamDataForGui != null ? teamDataForGui.name : "?"),
                    "§cNo team home set yet.",
                    "§7Ask your team leader to set one with /team."));
            }
        } else {
            inv.setItem(53, createGuiItem(Material.BEACON, "Team Home", NamedTextColor.DARK_GRAY,
                "§cYou are not in a team.",
                "§7Join or create a team to use this."));
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
                    if (homeNumber == -1) {
                        player.sendMessage(Component.text("🏠 Teleported to Team Home!", NamedTextColor.AQUA));
                    } else {
                        player.sendMessage(Component.text("🏠 Teleported to Home " + homeNumber + "!", NamedTextColor.GREEN));
                    }
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
                    if (destinationName.equalsIgnoreCase("Spawn") || destinationName.equalsIgnoreCase("AFK Zone")) {
                        playLobbyMusic(player);
                    } else {
                        stopLobbyMusic(player);
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private void teleportToAfkZone(Player player) {
        World afkWorld = Bukkit.getWorld("afk");
        if (afkWorld == null) {
            WorldCreator creator = new WorldCreator("afk");
            creator.type(WorldType.FLAT);
            creator.generateStructures(false);
            afkWorld = Bukkit.createWorld(creator);
        }
        if (afkWorld != null) {
            Location spawn = new Location(afkWorld, 6.5, -60.0, 4.5);
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

    private void setWandPoint(Player player, Location loc, int pointNum) {
        UUID uuid = player.getUniqueId();
        if (pointNum == 1) {
            wandPoint1.put(uuid, loc);
            player.sendMessage(Component.text("📍 Point 1 set to: (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")", NamedTextColor.GREEN));
        } else {
            wandPoint2.put(uuid, loc);
            player.sendMessage(Component.text("📍 Point 2 set to: (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")", NamedTextColor.GREEN));
        }
    }

    @EventHandler
    public void onCustomItemInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        if (item.hasItemMeta()) {
            String customType = item.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING);
            if (customType != null) {
                if (customType.equals("divine_flame")) {
                    if (player.isSneaking()) {
                        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                            event.setCancelled(true);
                            UUID pUuid = player.getUniqueId();
                            if (!chargingDivineFlame.containsKey(pUuid)) {
                                chargingDivineFlame.put(pUuid, System.currentTimeMillis());
                                player.sendMessage(Component.text("🔥 You begin focusing the Divine Flame...", NamedTextColor.GOLD));
                                player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_FIRECHARGE_USE, 1.0f, 0.5f);
                                
                                new org.bukkit.scheduler.BukkitRunnable() {
                                    int ticks = 0;
                                    @Override
                                    public void run() {
                                        if (!player.isOnline()) {
                                            chargingDivineFlame.remove(pUuid);
                                            cancel();
                                            return;
                                        }
                                        ItemStack hand = player.getInventory().getItemInMainHand();
                                        String currentCustom = null;
                                        if (hand != null && hand.hasItemMeta()) {
                                            currentCustom = hand.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(CustomScoreboard.this, "custom_item"), PersistentDataType.STRING);
                                        }
                                        
                                        if (!player.isSneaking() || currentCustom == null || !currentCustom.equals("divine_flame")) {
                                            player.sendMessage(Component.text("❌ Focus lost!", NamedTextColor.RED));
                                            chargingDivineFlame.remove(pUuid);
                                            cancel();
                                            return;
                                        }
                                        
                                        try {
                                            player.startUsingItem(org.bukkit.inventory.EquipmentSlot.HAND);
                                        } catch (Exception e) {}

                                        Location eye = player.getEyeLocation();
                                        player.getWorld().spawnParticle(Particle.FLAME, eye.add(player.getLocation().getDirection().multiply(0.5)), 3, 0.1, 0.1, 0.1, 0.05);
                                        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_FIRE_AMBIENT, 0.5f, 1.0f + (ticks * 0.05f));

                                        ticks += 2;
                                        if (ticks >= 60) {
                                            chargingDivineFlame.remove(pUuid);
                                            cancel();
                                            shootDivineFlame(player);
                                        }
                                    }
                                }.runTaskTimer(CustomScoreboard.this, 0L, 2L);
                            }
                        }
                    }
                }
                if (customType.equals("orbital_strike")) {
                    if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                        event.setCancelled(true);

                        String worldName = player.getWorld().getName();
                        if (worldName.equalsIgnoreCase("spawn") || worldName.equalsIgnoreCase("afk") || worldName.equalsIgnoreCase("afk_zone")) {
                            player.sendMessage(Component.text("❌ You cannot use the Orbital Strike in this zone!", NamedTextColor.RED));
                            return;
                        }

                        Block targetBlock = player.getTargetBlockExact(120);
                        Location targetLoc;
                        if (targetBlock != null) {
                            targetLoc = targetBlock.getLocation();
                        } else {
                            targetLoc = player.getEyeLocation().add(player.getLocation().getDirection().multiply(50));
                        }

                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

                        final Location center = targetLoc.clone();
                        new org.bukkit.scheduler.BukkitRunnable() {
                            int step = 0;
                            final int maxSteps = 50; // 50 runs, every 2 ticks = 100 ticks (5 seconds)

                            @Override
                            public void run() {
                                if (step >= maxSteps) {
                                    cancel();
                                    return;
                                }

                                // Radius grows from 1.5 to 15.0 blocks
                                double radius = 1.5 + (step * (15.0 - 1.5) / (maxSteps - 1));
                                
                                // Spawn 4 TNT blocks per step in a circle pattern, rotated slightly based on step
                                double angleOffset = step * 0.2; // Radians offset
                                for (int i = 0; i < 4; i++) {
                                    double angle = angleOffset + (i * Math.PI / 2.0);
                                    double dx = radius * Math.cos(angle);
                                    double dz = radius * Math.sin(angle);
                                    
                                    Location tntLoc = center.clone().add(dx, 5.0, dz);
                                    org.bukkit.entity.TNTPrimed tnt = tntLoc.getWorld().spawn(tntLoc, org.bukkit.entity.TNTPrimed.class);
                                    tnt.setSource(player);
                                }

                                step++;
                            }
                        }.runTaskTimer(this, 0L, 2L);
                    }
                } else if (customType.equals("lunge_spear")) {
                    if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                        event.setCancelled(true);
                        if (player.hasCooldown(Material.TRIDENT)) {
                            player.sendMessage(Component.text("❌ Lunge Spear is on cooldown!", NamedTextColor.RED));
                            return;
                        }
                        
                        org.bukkit.util.Vector direction = player.getLocation().getDirection();
                        player.setVelocity(direction.multiply(1.5).setY(0.4));
                        
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WIND_CHARGE_THROW, 1.0f, 1.0f);
                        player.setCooldown(Material.TRIDENT, 100); // 5 seconds cooldown
                        lastLungeTime.put(player.getUniqueId(), System.currentTimeMillis());
                    }
                } else if (customType.equals("wand")) {
                    if (!player.isOp()) {
                        player.sendMessage(Component.text("❌ Only operators can use the wand!", NamedTextColor.RED));
                        return;
                    }
                    if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                        event.setCancelled(true);
                        Block clicked = event.getClickedBlock();
                        if (clicked != null) {
                            setWandPoint(player, clicked.getLocation(), 1);
                        }
                    } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                        event.setCancelled(true);
                        Block clicked = event.getClickedBlock();
                        if (clicked != null) {
                            setWandPoint(player, clicked.getLocation(), 2);
                        }
                    }
                } else if (customType.equals("npc_egg")) {
                    if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                        event.setCancelled(true);
                        
                        String targetName = "";
                        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                            Component displayNameComponent = item.getItemMeta().displayName();
                            if (displayNameComponent != null) {
                                targetName = PlainTextComponentSerializer.plainText().serialize(displayNameComponent).trim();
                            }
                        }
                        
                        if (targetName.isEmpty() || targetName.equalsIgnoreCase("NPC Spawn Egg")) {
                            player.sendMessage(Component.text("❌ Rename this egg in an anvil to a player's name first!", NamedTextColor.RED));
                            return;
                        }
                        
                        if (!targetName.matches("^[a-zA-Z0-9_.*-]{3,20}$")) {
                            player.sendMessage(Component.text("❌ Invalid player name: '" + targetName + "'. Name must be 3-20 characters long.", NamedTextColor.RED));
                            return;
                        }
                        
                        Block clickedBlock = event.getClickedBlock();
                        org.bukkit.block.BlockFace clickedFace = event.getBlockFace();
                        if (clickedBlock == null || clickedFace == null) return;
                        
                        Location spawnLoc = clickedBlock.getRelative(clickedFace).getLocation().add(0.5, 0.0, 0.5);
                        org.bukkit.util.Vector direction = player.getLocation().toVector().subtract(spawnLoc.toVector());
                        direction.setY(0);
                        if (direction.lengthSquared() > 0) {
                            spawnLoc.setDirection(direction);
                        }
                        
                        ArmorStand npc = spawnLoc.getWorld().spawn(spawnLoc, ArmorStand.class);
                        npc.setBasePlate(false);
                        npc.setArms(true);
                        npc.setInvulnerable(true);
                        npc.setGravity(true);
                        npc.setCustomNameVisible(true);
                        npc.customName(Component.text(targetName, NamedTextColor.YELLOW, net.kyori.adventure.text.format.TextDecoration.BOLD));
                        
                        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
                        if (skullMeta != null) {
                            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(targetName));
                            skull.setItemMeta(skullMeta);
                        }
                        npc.getEquipment().setHelmet(skull);
                        
                        NamespacedKey npcKey = new NamespacedKey(this, "is_npc");
                        NamespacedKey ownerKey = new NamespacedKey(this, "npc_owner");
                        NamespacedKey placerKey = new NamespacedKey(this, "npc_placer");
                        npc.getPersistentDataContainer().set(npcKey, PersistentDataType.BOOLEAN, true);
                        npc.getPersistentDataContainer().set(ownerKey, PersistentDataType.STRING, targetName);
                        npc.getPersistentDataContainer().set(placerKey, PersistentDataType.STRING, player.getUniqueId().toString());
                        
                        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                            item.setAmount(item.getAmount() - 1);
                        }
                        
                        player.playSound(spawnLoc, org.bukkit.Sound.ENTITY_CHICKEN_EGG, 1.0f, 1.0f);
                        player.sendMessage(Component.text("✅ Spawned NPC of player " + targetName + "!", NamedTextColor.GREEN));
                    }
                } else if (customType.equals("floating_text")) {
                    if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                        event.setCancelled(true);
                        
                        Block clickedBlock = event.getClickedBlock();
                        org.bukkit.block.BlockFace clickedFace = event.getBlockFace();
                        if (clickedBlock == null || clickedFace == null) return;
                        
                        Block targetBlock = clickedBlock.getRelative(clickedFace);
                        if (targetBlock.getType() != Material.AIR) {
                            player.sendMessage(Component.text("❌ You can only place floating text in the air!", NamedTextColor.RED));
                            return;
                        }
                        
                        Material originalType = targetBlock.getType();
                        org.bukkit.block.data.BlockData originalData = targetBlock.getBlockData();
                        
                        targetBlock.setType(Material.OAK_SIGN, false);
                        
                        if (targetBlock.getState() instanceof org.bukkit.block.Sign sign) {
                            player.openSign(sign);
                            activeFloatingTextPlacement.put(player.getUniqueId(), targetBlock.getLocation());
                            originalBlockState.put(player.getUniqueId(), new BlockBackup(originalType, originalData));
                            
                            if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                                item.setAmount(item.getAmount() - 1);
                            }
                        } else {
                            targetBlock.setType(originalType, false);
                            targetBlock.setBlockData(originalData, false);
                            player.sendMessage(Component.text("❌ Failed to initialize floating text input sign!", NamedTextColor.RED));
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onItemHeldChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (lastLungeTime.containsKey(uuid)) {
            long diff = System.currentTimeMillis() - lastLungeTime.get(uuid);
            if (diff <= 500) {
                player.setCooldown(Material.TRIDENT, 0);
                player.sendMessage(Component.text("⚡ Lunge cooldown reset!", NamedTextColor.GOLD));
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            if (block != null && block.getType() == Material.SPAWNER) {
                Location loc = block.getLocation();
                if (generators.containsKey(loc)) {
                    event.setCancelled(true);
                    Player player = event.getPlayer();
                    if (!player.isSneaking()) {
                        GeneratorData data = generators.get(loc);
                        player.openInventory(data.inventory);
                        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
                    }
                    return;
                }
            }

            ItemStack item = event.getItem();
            if (block != null && item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                String customItem = meta.getPersistentDataContainer().get(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING);
                if (customItem != null && customItem.equals("echo_shovel")) {
                    Material type = block.getType();
                    if (type == Material.GRASS_BLOCK || type == Material.DIRT || type == Material.COARSE_DIRT || type == Material.PODZOL || type == Material.ROOTED_DIRT) {
                        Player player = event.getPlayer();
                        Location loc = block.getLocation();
                        if (isInSpawnRadius(loc)) return;
                        
                        event.setCancelled(true);
                        boolean playSound = false;
                        for (int dx = -1; dx <= 1; dx++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                Block b = block.getRelative(dx, 0, dz);
                                if (isInSpawnRadius(b.getLocation())) continue;
                                Material bType = b.getType();
                                if (bType == Material.GRASS_BLOCK || bType == Material.DIRT || bType == Material.COARSE_DIRT || bType == Material.PODZOL || bType == Material.ROOTED_DIRT) {
                                    Block above = b.getRelative(BlockFace.UP);
                                    if (above.getType().isAir() || !above.getType().isSolid()) {
                                        b.setType(Material.DIRT_PATH);
                                        playSound = true;
                                    }
                                }
                            }
                        }
                        if (playSound) {
                            block.getWorld().playSound(block.getLocation(), org.bukkit.Sound.ITEM_SHOVEL_FLATTEN, 1.0f, 1.0f);
                            if (player.getGameMode() != GameMode.CREATIVE) {
                                org.bukkit.inventory.meta.Damageable toolMeta = (org.bukkit.inventory.meta.Damageable) item.getItemMeta();
                                if (toolMeta != null) {
                                    int unbreakingLevel = item.getEnchantmentLevel(Enchantment.UNBREAKING);
                                    if (random.nextInt(unbreakingLevel + 1) == 0) {
                                        toolMeta.setDamage(toolMeta.getDamage() + 1);
                                        item.setItemMeta(toolMeta);
                                        if (toolMeta.getDamage() >= item.getType().getMaxDurability()) {
                                            player.getInventory().setItemInMainHand(null);
                                            player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                                        }
                                    }
                                }
                            }
                        }
                        return;
                    }
                }
            }
        }

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        Location loc = block.getLocation();

        if (block.getType() != Material.CHEST) return;

        if (commandChests.containsKey(loc)) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            String command = commandChests.get(loc);
            String cmdToRun = command.startsWith("/") ? command.substring(1) : command;
            bypassCommandChestOpCheck.add(player.getUniqueId());
            try {
                player.performCommand(cmdToRun);
            } finally {
                bypassCommandChestOpCheck.remove(player.getUniqueId());
            }
            return;
        }

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

    @EventHandler
    public void onBedrockNetherRoofInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (player.getWorld().getEnvironment() != World.Environment.NETHER) return;
        
        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null || clickedBlock.getY() < 127) return;
        
        // Check if player is a Bedrock player
        boolean isBedrock = player.getName().startsWith(".") || player.getUniqueId().toString().startsWith("00000000-0000-0000-");
        if (!isBedrock) return;

        ItemStack mainItem = player.getInventory().getItemInMainHand();
        if (mainItem == null || !mainItem.getType().isBlock()) return;

        BlockFace face = event.getBlockFace();
        Block targetBlock = clickedBlock.getRelative(face);

        // Ensure the target block is air or replaceable
        if (targetBlock.getType() != Material.AIR && !targetBlock.isReplaceable()) return;

        // Create and call block place event
        BlockPlaceEvent placeEvent = new BlockPlaceEvent(
            targetBlock,
            targetBlock.getState(),
            clickedBlock,
            mainItem,
            player,
            true,
            org.bukkit.inventory.EquipmentSlot.HAND
        );
        Bukkit.getPluginManager().callEvent(placeEvent);
        if (placeEvent.isCancelled() || !placeEvent.canBuild()) return;

        // Place the block
        Material blockType = mainItem.getType();
        targetBlock.setType(blockType);
        
        // Play placement sound
        targetBlock.getWorld().playSound(targetBlock.getLocation(), targetBlock.getBlockData().getSoundGroup().getPlaceSound(), 1.0f, 1.0f);

        // Consume block from hand in survival
        if (player.getGameMode() == GameMode.SURVIVAL) {
            mainItem.setAmount(mainItem.getAmount() - 1);
        }
        
        event.setCancelled(true);
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
            } else if (type.equalsIgnoreCase("End keys") || type.equalsIgnoreCase("endkeys")) {
                endKeysMap.put(uuid, endKeysMap.getOrDefault(uuid, 0) + (int) amount);
            } else if (type.equalsIgnoreCase("amethyst keys") || type.equalsIgnoreCase("amethystkeys")) {
                amethystKeysMap.put(uuid, amethystKeysMap.getOrDefault(uuid, 0) + (int) amount);
            }
            online.sendMessage(Component.text("💰 You received " + amount + " " + type + " from your shop sale!", NamedTextColor.GREEN));
        } else {
            String path = "players." + uuid.toString() + ".";
            String configKey = type;
            if (type.equalsIgnoreCase("Echo keys") || type.equalsIgnoreCase("echokeys")) {
                configKey = "echoKeys";
            } else if (type.equalsIgnoreCase("crimson keys") || type.equalsIgnoreCase("crimsonkeys")) {
                configKey = "crimsonKeys";
            } else if (type.equalsIgnoreCase("End keys") || type.equalsIgnoreCase("endkeys")) {
                configKey = "endKeys";
            } else if (type.equalsIgnoreCase("amethyst keys") || type.equalsIgnoreCase("amethystkeys")) {
                configKey = "amethystKeys";
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
                } else if (data.crateType.equals("end")) {
                    currencyColor = NamedTextColor.LIGHT_PURPLE;
                    currencyName = "End keys";
                } else if (data.crateType.equals("amethyst")) {
                    currencyColor = NamedTextColor.DARK_PURPLE;
                    currencyName = "Amethyst keys";
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

    private void loadTeams() {
        teams.clear();
        playerTeams.clear();
        if (!getConfig().contains("teams")) return;
        org.bukkit.configuration.ConfigurationSection sec = getConfig().getConfigurationSection("teams");
        if (sec == null) return;
        for (String lowercaseName : sec.getKeys(false)) {
            String path = "teams." + lowercaseName;
            String name = getConfig().getString(path + ".name");
            String leaderUUIDStr = getConfig().getString(path + ".leader");
            if (leaderUUIDStr == null) continue;
            UUID leader = UUID.fromString(leaderUUIDStr);
            String leaderName = getConfig().getString(path + ".leaderName");
            
            TeamData data = new TeamData(name, leader, leaderName);
            data.members.clear();
            if (getConfig().contains(path + ".members")) {
                List<String> memberUUIDs = getConfig().getStringList(path + ".members");
                for (String mStr : memberUUIDs) {
                    try {
                        UUID mUUID = UUID.fromString(mStr);
                        data.members.add(mUUID);
                        playerTeams.put(mUUID, lowercaseName);
                    } catch (Exception ignored) {}
                }
            }
            if (getConfig().contains(path + ".rules")) {
                data.rules = getConfig().getStringList(path + ".rules");
            }
            if (getConfig().contains(path + ".home")) {
                String worldName = getConfig().getString(path + ".home.world");
                double x = getConfig().getDouble(path + ".home.x");
                double y = getConfig().getDouble(path + ".home.y");
                double z = getConfig().getDouble(path + ".home.z");
                float pitch = (float) getConfig().getDouble(path + ".home.pitch");
                float yaw = (float) getConfig().getDouble(path + ".home.yaw");
                World w = Bukkit.getWorld(worldName);
                if (w != null) {
                    data.teamHome = new Location(w, x, y, z, yaw, pitch);
                }
            }
            teams.put(lowercaseName, data);
        }
    }

    private void saveTeams() {
        getConfig().set("teams", null);
        for (var entry : teams.entrySet()) {
            String lowercaseName = entry.getKey();
            TeamData data = entry.getValue();
            String path = "teams." + lowercaseName;
            getConfig().set(path + ".name", data.name);
            getConfig().set(path + ".leader", data.leader.toString());
            getConfig().set(path + ".leaderName", data.leaderName);
            
            List<String> memberStrings = new ArrayList<>();
            for (UUID uuid : data.members) {
                memberStrings.add(uuid.toString());
            }
            getConfig().set(path + ".members", memberStrings);
            getConfig().set(path + ".rules", data.rules);
            if (data.teamHome != null) {
                getConfig().set(path + ".home.world", data.teamHome.getWorld().getName());
                getConfig().set(path + ".home.x", data.teamHome.getX());
                getConfig().set(path + ".home.y", data.teamHome.getY());
                getConfig().set(path + ".home.z", data.teamHome.getZ());
                getConfig().set(path + ".home.pitch", data.teamHome.getPitch());
                getConfig().set(path + ".home.yaw", data.teamHome.getYaw());
            }
        }
        saveConfig();
        for (Player online : Bukkit.getOnlinePlayers()) {
            updateNameplateTeams(online.getScoreboard());
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
            checkAndTrackMinedOre(player, b);
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
        Player boardOwner = null;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getScoreboard() == board) {
                boardOwner = p;
                break;
            }
        }
        String ownerTeamNameLower = boardOwner != null ? playerTeams.get(boardOwner.getUniqueId()) : null;

        for (Player online : Bukkit.getOnlinePlayers()) {
            String name = online.getName();
            String teamName = "np_" + (name.length() > 13 ? name.substring(0, 13) : name);

            Team team = board.getTeam(teamName);
            if (team == null) {
                team = board.registerNewTeam(teamName);
            }
            if (!team.hasEntry(name)) {
                team.addEntry(name);
            }

            Component prefix = Component.empty();

            String onlineTeamNameLower = playerTeams.get(online.getUniqueId());
            if (onlineTeamNameLower != null) {
                TeamData teamData = teams.get(onlineTeamNameLower);
                if (teamData != null) {
                    if (online.getUniqueId().equals(teamData.leader)) {
                        prefix = prefix.append(Component.text("(^Team Leader of " + teamData.name + "^) ", NamedTextColor.RED));
                    } else if (ownerTeamNameLower != null && ownerTeamNameLower.equalsIgnoreCase(onlineTeamNameLower)) {
                        prefix = prefix.append(Component.text("(^Member of " + teamData.name + "^) ", NamedTextColor.GREEN));
                    }
                }
            }

            UUID uuid = online.getUniqueId();
            String activeTag = activeNametags.getOrDefault(uuid, "");
            if (!activeTag.isEmpty()) {
                Component tagComp = switch (activeTag) {
                    case "Berry Lover" -> Component.text("[Berry Lover] ", NamedTextColor.LIGHT_PURPLE);
                    case "Combat Master" -> Component.text("[Combat Master] ", NamedTextColor.RED);
                    case "Admin killer" -> createRainbowComponent("[Admin killer] ");
                    case "Richie Boi" -> Component.text("[Richie Boi] ", NamedTextColor.GREEN);
                    case "Dragon Slayer" -> Component.text("[Dragon Slayer] ", NamedTextColor.DARK_PURPLE);
                    case "The Miner" -> Component.text("[The Miner] ", NamedTextColor.BLUE);
                    case "Silent Assassin" -> Component.text("[Silent Assassin] ", NamedTextColor.RED);
                    case "The Builder" -> Component.text("[The Builder] ", NamedTextColor.YELLOW);
                    case "Fatty" -> Component.text("[", NamedTextColor.GRAY).append(Component.text("Fat", NamedTextColor.GREEN)).append(Component.text("ty", net.kyori.adventure.text.format.TextColor.color(0x8b, 0x5a, 0x2b))).append(Component.text("] ", NamedTextColor.GRAY));
                    case "Skin and Bones" -> Component.text("[Skin and Bones] ", NamedTextColor.WHITE);
                    default -> Component.empty();
                };
                prefix = prefix.append(tagComp);
            }

            if (name.equalsIgnoreCase(".Redtoppat208") || name.equalsIgnoreCase(".RedToppat208")) {
                prefix = prefix.append(Component.text("[Owner o' Merp] ", NamedTextColor.RED));
                team.color(NamedTextColor.RED);
            } else if (name.equalsIgnoreCase(".Boreas4052") || name.equalsIgnoreCase(".Boreas4052")) {
                prefix = prefix.append(Component.text("[Co-Owner o' Lerp] ", NamedTextColor.BLUE));
                team.color(NamedTextColor.BLUE);
            } else if (name.equalsIgnoreCase(".Ironwarden7425") || name.equalsIgnoreCase(".IronWarden7425")) {
                prefix = prefix.append(Component.text("[Admin o' Derp] ", NamedTextColor.DARK_PURPLE));
                team.color(NamedTextColor.DARK_PURPLE);
            } else if (name.equalsIgnoreCase(".AlberTogofound") || name.equalsIgnoreCase("AlberTogofound")) {
                prefix = prefix.append(Component.text("[Albert!! the pizza lover] ", NamedTextColor.YELLOW));
                team.color(NamedTextColor.YELLOW);
            } else if (online.isOp()) {
                prefix = prefix.append(Component.text("[admin] ", NamedTextColor.LIGHT_PURPLE));
                team.color(NamedTextColor.LIGHT_PURPLE);
            } else if (hasErpProMaxMap.getOrDefault(uuid, false)) {
                prefix = prefix.append(createGoldGradientComponent("[Erp+++] "));
                team.color(NamedTextColor.GOLD);
            } else if (hasErpProMap.getOrDefault(uuid, false)) {
                prefix = prefix.append(Component.text("[Erp++] ", NamedTextColor.GRAY));
                team.color(NamedTextColor.GRAY);
            } else if (hasErpPlusMap.getOrDefault(uuid, false)) {
                prefix = prefix.append(Component.text("[Erp+] ", NamedTextColor.GOLD));
                team.color(NamedTextColor.WHITE);
            } else {
                team.color(NamedTextColor.WHITE);
            }

            team.prefix(prefix);
            if (voiceChatEnabled.getOrDefault(uuid, false)) {
                team.suffix(Component.text(" 🎤", NamedTextColor.AQUA));
            } else {
                team.suffix(Component.empty());
            }
        }
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

    private ItemStack createEndCrate() {
        ItemStack chest = new ItemStack(Material.CHEST);
        ItemMeta meta = chest.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("End Crate", NamedTextColor.LIGHT_PURPLE));
            meta.lore(List.of(
                Component.text("Place to start an End keys shop.", NamedTextColor.GRAY),
                Component.text("Shows your End keys balance above.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "end_crate");
            chest.setItemMeta(meta);
        }
        return chest;
    }

    private ItemStack createAmethystCrate() {
        ItemStack chest = new ItemStack(Material.CHEST);
        ItemMeta meta = chest.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Amethyst Crate", NamedTextColor.DARK_PURPLE));
            meta.lore(List.of(
                Component.text("Place to start an Amethyst keys shop.", NamedTextColor.GRAY),
                Component.text("Shows your Amethyst keys balance above.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "amethyst_crate");
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

    private ItemStack createEndKey() {
        ItemStack key = new ItemStack(Material.ENDER_EYE);
        ItemMeta meta = key.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("End Key", NamedTextColor.LIGHT_PURPLE));
            meta.lore(List.of(
                Component.text("A key vibrating with ender energy.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "end_key");
            key.setItemMeta(meta);
        }
        return key;
    }

    private ItemStack createAmethystKey() {
        ItemStack key = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = key.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Amethyst Key", NamedTextColor.DARK_PURPLE));
            meta.lore(List.of(
                Component.text("A shiny key forged from amethyst crystal.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "amethyst_key");
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
                    } else if (data.crateType.equals("end")) {
                        balanceStr = String.valueOf(endKeysMap.getOrDefault(uuid, 0));
                    } else if (data.crateType.equals("amethyst")) {
                        balanceStr = String.valueOf(amethystKeysMap.getOrDefault(uuid, 0));
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
                } else if (data.crateType.equals("end")) {
                    currencyColor = NamedTextColor.LIGHT_PURPLE;
                    currencyName = "End keys";
                } else if (data.crateType.equals("amethyst")) {
                    currencyColor = NamedTextColor.DARK_PURPLE;
                    currencyName = "Amethyst keys";
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
        Player player = event.getPlayer();
        if (player.getWorld().getName().equalsIgnoreCase("duel")) {
            String message = event.getMessage().toLowerCase().trim();
            if (message.startsWith("/tp") || message.startsWith("/spawn") || message.startsWith("/home")
                    || message.startsWith("/rtp") || message.startsWith("/dtp") || message.startsWith("/warp")
                    || message.startsWith("/tpa") || message.startsWith("/tpaccept") || message.startsWith("/back")) {
                event.setCancelled(true);
                player.sendMessage(Component.text("❌ You cannot teleport out of the duel arena!", NamedTextColor.RED));
                return;
            }
        }
        String rawMessage = event.getMessage();
        if (rawMessage.startsWith("//fill ") || rawMessage.equalsIgnoreCase("//fill")) {
            event.setCancelled(true);
            player = event.getPlayer();
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ You do not have permission to use this command!", NamedTextColor.RED));
                return;
            }

            UUID uuid = player.getUniqueId();
            Location p1 = wandPoint1.get(uuid);
            Location p2 = wandPoint2.get(uuid);
            if (p1 == null || p2 == null) {
                player.sendMessage(Component.text("❌ Select two points with the Wand first!", NamedTextColor.RED));
                return;
            }

            String[] parts = rawMessage.split(" ");
            if (parts.length < 2) {
                player.sendMessage(Component.text("❌ Usage: //fill <material> [hollow]", NamedTextColor.RED));
                return;
            }

            String matName = parts[1].toUpperCase();
            Material material = Material.matchMaterial(matName);
            if (material == null || !material.isBlock()) {
                player.sendMessage(Component.text("❌ Invalid block material!", NamedTextColor.RED));
                return;
            }

            boolean hollow = false;
            if (parts.length >= 3 && parts[2].equalsIgnoreCase("hollow")) {
                hollow = true;
            }

            int x1 = p1.getBlockX();
            int y1 = p1.getBlockY();
            int z1 = p1.getBlockZ();
            int x2 = p2.getBlockX();
            int y2 = p2.getBlockY();
            int z2 = p2.getBlockZ();

            int minX = Math.min(x1, x2);
            int maxX = Math.max(x1, x2);
            int minY = Math.min(y1, y2);
            int maxY = Math.max(y1, y2);
            int minZ = Math.min(z1, z2);
            int maxZ = Math.max(z1, z2);

            World world = player.getWorld();
            int blocksChanged = 0;

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        if (hollow) {
                            boolean isOuter = (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ);
                            if (!isOuter) {
                                Block block = world.getBlockAt(x, y, z);
                                if (block.getType() != Material.AIR) {
                                    block.setType(Material.AIR, false);
                                    blocksChanged++;
                                }
                                continue;
                            }
                        }

                        Block block = world.getBlockAt(x, y, z);
                        if (block.getType() != material) {
                            block.setType(material, false);
                            blocksChanged++;
                        }
                    }
                }
            }

            player.sendMessage(Component.text("✅ Successfully filled " + blocksChanged + " blocks with " + material.name().toLowerCase() + (hollow ? " (hollow)" : "") + ".", NamedTextColor.GREEN));
            return;
        }

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

    @EventHandler
    public void onEntityDeath(org.bukkit.event.entity.EntityDeathEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.EnderDragon) {
            Player killer = event.getEntity().getKiller();
            if (killer != null) {
                killedDragonMap.put(killer.getUniqueId(), true);
                killer.sendMessage(Component.text("🐉 You have unlocked the Dragon Slayer nametag!", NamedTextColor.LIGHT_PURPLE));
            }
        }

        if (event.getEntity() instanceof org.bukkit.entity.Zombie) {
            org.bukkit.entity.Zombie zombie = (org.bukkit.entity.Zombie) event.getEntity();
            UUID zombieUuid = zombie.getUniqueId();
            
            UUID waveOwnerUuid = null;
            for (var entry : playerActiveApocalypseZombies.entrySet()) {
                if (entry.getValue().contains(zombieUuid)) {
                    waveOwnerUuid = entry.getKey();
                    entry.getValue().remove(zombieUuid);
                    break;
                }
            }
            
            if (waveOwnerUuid != null) {
                Player killer = zombie.getKiller();
                if (killer != null) {
                    UUID killerUUID = killer.getUniqueId();
                    apocalypseZombieKillsMap.put(killerUUID, apocalypseZombieKillsMap.getOrDefault(killerUUID, 0) + 1);
                    
                    int payout = calculateZombieDerpiesPayout(zombie);
                    derpiesMap.put(killerUUID, derpiesMap.getOrDefault(killerUUID, 0L) + payout);
                    killer.sendMessage(Component.text("💰 +" + payout + " Derpies for killing an Apocalypse Zombie!", NamedTextColor.LIGHT_PURPLE));
                    updateScoreboard(killer);
                    savePlayerData(killer);
                }
                
                java.util.Set<UUID> active = playerActiveApocalypseZombies.get(waveOwnerUuid);
                if (active == null || active.isEmpty()) {
                    Player owner = Bukkit.getPlayer(waveOwnerUuid);
                    if (owner != null && owner.isOnline()) {
                        int currentWave = playerApocalypseWaveMap.getOrDefault(waveOwnerUuid, 1);
                        owner.sendTitle("§6WAVE " + currentWave + " COMPLETED!", "§7Next wave starts in 5 seconds...", 10, 40, 10);
                        owner.sendMessage(Component.text("🎉 Wave " + currentWave + " completed! Preparing next wave...", NamedTextColor.GREEN));
                        
                        playerApocalypseWaveMap.put(waveOwnerUuid, currentWave + 1);
                        
                        final Player finalOwner = owner;
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (finalOwner.isOnline() && finalOwner.getWorld().getName().equalsIgnoreCase("apocalypse")) {
                                    spawnNextApocalypseWave(finalOwner);
                                }
                            }
                        }.runTaskLater(this, 100L);
                    }
                }
            }
        }
    }

    private Component createRainbowComponent(String text) {
        NamedTextColor[] colors = {
            NamedTextColor.RED, NamedTextColor.GOLD, NamedTextColor.YELLOW, 
            NamedTextColor.GREEN, NamedTextColor.AQUA, NamedTextColor.BLUE, 
            NamedTextColor.LIGHT_PURPLE
        };
        Component comp = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            NamedTextColor color = colors[i % colors.length];
            comp = comp.append(Component.text(String.valueOf(c), color));
        }
        return comp;
    }

    private Component createGoldGradientComponent(String text) {
        Component comp = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            NamedTextColor color = (i % 2 == 0) ? NamedTextColor.GOLD : NamedTextColor.YELLOW;
            comp = comp.append(Component.text(String.valueOf(c), color));
        }
        return comp;
    }

    private boolean hasBerryLoverUnlocked(Player player) {
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null) return false;
            Material type = item.getType();
            if (type != Material.SWEET_BERRIES && type != Material.GLOW_BERRIES) {
                return false;
            }
        }
        return true;
    }

    private boolean isNametagUnlocked(Player player, String tagName) {
        UUID uuid = player.getUniqueId();
        if (manuallyUnlockedNametags.getOrDefault(uuid, java.util.Collections.emptySet()).contains(tagName)) {
            return true;
        }
        return switch (tagName) {
            case "Berry Lover" -> hasBerryLoverUnlocked(player);
            case "Combat Master" -> killsMap.getOrDefault(uuid, 0) >= 30;
            case "Admin killer" -> killedAdminMap.getOrDefault(uuid, false);
            case "Richie Boi" -> erpiesMap.getOrDefault(uuid, 0L) >= 1000000L;
            case "Dragon Slayer" -> killedDragonMap.getOrDefault(uuid, false);
            case "The Miner" -> oresMinedMap.getOrDefault(uuid, 0) >= 100;
            case "Silent Assassin" -> invisibleKillsMap.getOrDefault(uuid, 0) >= 30;
            case "The Builder" -> blocksPlacedMap.getOrDefault(uuid, 0) >= 3000;
            case "Fatty" -> getFattyProgress(player) == ALL_FOODS.size();
            case "Skin and Bones" -> starvationDeathsMap.getOrDefault(uuid, 0) >= 10;
            default -> false;
        };
    }

    @EventHandler
    public void onNpcInteract(org.bukkit.event.player.PlayerInteractAtEntityEvent event) {
        org.bukkit.entity.Entity entity = event.getRightClicked();
        if (entity instanceof ArmorStand npc) {
            NamespacedKey npcKey = new NamespacedKey(this, "is_npc");
            if (npc.getPersistentDataContainer().has(npcKey, PersistentDataType.BOOLEAN)) {
                event.setCancelled(true);
                Player player = event.getPlayer();
                
                String placerUuidStr = npc.getPersistentDataContainer().get(new NamespacedKey(this, "npc_placer"), PersistentDataType.STRING);
                boolean canModify = player.isOp() || (placerUuidStr != null && placerUuidStr.equals(player.getUniqueId().toString()));
                if (!canModify) {
                    player.sendMessage(Component.text("❌ You do not have permission to modify this NPC!", NamedTextColor.RED));
                    return;
                }
                
                String targetName = npc.getPersistentDataContainer().get(new NamespacedKey(this, "npc_owner"), PersistentDataType.STRING);
                if (targetName == null) targetName = "Unknown";
                
                if (player.isSneaking()) {
                    if (npc.getEquipment() != null) {
                        for (ItemStack eq : npc.getEquipment().getArmorContents()) {
                            if (eq != null && eq.getType() != Material.AIR && eq.getType() != Material.PLAYER_HEAD) {
                                npc.getWorld().dropItemNaturally(npc.getLocation(), eq);
                            }
                        }
                        ItemStack mainHand = npc.getEquipment().getItemInMainHand();
                        if (mainHand != null && mainHand.getType() != Material.AIR) {
                            npc.getWorld().dropItemNaturally(npc.getLocation(), mainHand);
                        }
                        ItemStack offHand = npc.getEquipment().getItemInOffHand();
                        if (offHand != null && offHand.getType() != Material.AIR) {
                            npc.getWorld().dropItemNaturally(npc.getLocation(), offHand);
                        }
                    }
                    
                    npc.remove();
                    
                    ItemStack egg = createNpcEgg();
                    ItemMeta meta = egg.getItemMeta();
                    if (meta != null) {
                        meta.displayName(Component.text(targetName, NamedTextColor.LIGHT_PURPLE, net.kyori.adventure.text.format.TextDecoration.BOLD));
                        egg.setItemMeta(meta);
                    }
                    
                    java.util.HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(egg);
                    for (ItemStack drop : remaining.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                    
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
                    player.sendMessage(Component.text("✅ Despawned NPC and returned spawn egg for " + targetName + "!", NamedTextColor.GREEN));
                } else {
                    ItemStack handItem = player.getInventory().getItemInMainHand();
                    if (handItem != null && handItem.getType() != Material.AIR) {
                        Material type = handItem.getType();
                        ItemStack equipItem = handItem.clone();
                        equipItem.setAmount(1);
                        
                        boolean equipped = false;
                        if (type.name().endsWith("_CHESTPLATE") || type == Material.ELYTRA) {
                            npc.getEquipment().setChestplate(equipItem);
                            equipped = true;
                        } else if (type.name().endsWith("_LEGGINGS")) {
                            npc.getEquipment().setLeggings(equipItem);
                            equipped = true;
                        } else if (type.name().endsWith("_BOOTS")) {
                            npc.getEquipment().setBoots(equipItem);
                            equipped = true;
                        } else {
                            if (npc.getEquipment().getItemInMainHand().getType() == Material.AIR) {
                                npc.getEquipment().setItemInMainHand(equipItem);
                                equipped = true;
                            } else if (npc.getEquipment().getItemInOffHand().getType() == Material.AIR) {
                                npc.getEquipment().setItemInOffHand(equipItem);
                                equipped = true;
                            } else {
                                npc.getEquipment().setItemInMainHand(equipItem);
                                equipped = true;
                            }
                        }
                        
                        if (equipped) {
                            if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                                handItem.setAmount(handItem.getAmount() - 1);
                            }
                            player.playSound(npc.getLocation(), org.bukkit.Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.0f);
                            player.sendMessage(Component.text("✅ Equipped NPC with " + type.name().toLowerCase().replace("_", " "), NamedTextColor.GREEN));
                        }
                    } else {
                        player.sendMessage(Component.text("💡 Right-click this NPC with armor or a weapon to equip it, or Shift+Right-click to pick it up.", NamedTextColor.AQUA));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onNpcDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getEntity() instanceof ArmorStand npc) {
            NamespacedKey npcKey = new NamespacedKey(this, "is_npc");
            if (npc.getPersistentDataContainer().has(npcKey, PersistentDataType.BOOLEAN)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onMobSpawn(org.bukkit.event.entity.CreatureSpawnEvent event) {
        String worldName = event.getLocation().getWorld().getName();
        if (worldName.equalsIgnoreCase("afk") || worldName.equalsIgnoreCase("afk_zone") || worldName.equalsIgnoreCase("spawn")) {
            if (event.getSpawnReason() != org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onFloatingTextRemove(org.bukkit.event.player.PlayerInteractEvent event) {
        if (event.getAction() == org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            if (player.isSneaking()) {
                Block block = event.getClickedBlock();
                if (block != null) {
                    Location center = block.getLocation().add(0.5, 0.5, 0.5);
                    java.util.Collection<org.bukkit.entity.TextDisplay> displays = center.getNearbyEntitiesByType(org.bukkit.entity.TextDisplay.class, 1.5);
                    for (org.bukkit.entity.TextDisplay display : displays) {
                        NamespacedKey key = new NamespacedKey(this, "is_floating_text");
                        if (display.getPersistentDataContainer().has(key, PersistentDataType.BOOLEAN)) {
                            String placerUuidStr = display.getPersistentDataContainer().get(new NamespacedKey(this, "floating_text_placer"), PersistentDataType.STRING);
                            boolean canRemove = player.isOp() || (placerUuidStr != null && placerUuidStr.equals(player.getUniqueId().toString()));
                            if (canRemove) {
                                display.remove();
                                event.setCancelled(true);
                                
                                player.getWorld().dropItemNaturally(display.getLocation(), createFloatingTextItem());
                                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                                player.sendMessage(Component.text("✅ Removed floating text.", NamedTextColor.GREEN));
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onAfkDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            String worldName = player.getWorld().getName();
            if (worldName.equalsIgnoreCase("afk") || worldName.equalsIgnoreCase("afk_zone")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onAfkHunger(org.bukkit.event.entity.FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            String worldName = player.getWorld().getName();
            if (worldName.equalsIgnoreCase("afk") || worldName.equalsIgnoreCase("afk_zone")) {
                event.setCancelled(true);
                player.setFoodLevel(20);
            }
        }
    }

    @EventHandler
    public void onPlayerWorldChange(org.bukkit.event.player.PlayerChangedWorldEvent event) {
        handleWorldMusic(event.getPlayer(), event.getPlayer().getWorld());
    }

    @EventHandler
    public void onPlayerJoinMusic(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                handleWorldMusic(player, player.getWorld());
            }
        }, 40L);
    }

    private void handleWorldMusic(Player player, World world) {
        String worldName = world.getName();
        String spawnWorldName = Bukkit.getWorlds().isEmpty() ? "world" : Bukkit.getWorlds().get(0).getName();
        
        if (worldName.equalsIgnoreCase(spawnWorldName) || worldName.equalsIgnoreCase("spawn")
                || worldName.equalsIgnoreCase("afk") || worldName.equalsIgnoreCase("afk_zone")) {
            playLobbyMusic(player);
        } else {
            stopLobbyMusic(player);
        }
    }

    private void playLobbyMusic(Player player) {
        // Disabled custom lobby music
    }

    private void stopLobbyMusic(Player player) {
        // Disabled custom lobby music
    }

    private void openTeamGui(Player player) {
        UUID uuid = player.getUniqueId();
        String teamNameLower = playerTeams.get(uuid);
        if (teamNameLower == null) {
            player.sendMessage(Component.text("❌ You are not in a team! Use /maketeam <name> to create one.", NamedTextColor.RED));
            return;
        }
        TeamData data = teams.get(teamNameLower);
        if (data == null) {
            player.sendMessage(Component.text("❌ Team not found.", NamedTextColor.RED));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Team: " + data.name));

        // Fill background of bottom row
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        if (paneMeta != null) {
            paneMeta.displayName(Component.empty());
            pane.setItemMeta(paneMeta);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, pane);
        }

        // Search button (Slot 48)
        ItemStack searchBtn = new ItemStack(Material.COMPASS);
        ItemMeta searchMeta = searchBtn.getItemMeta();
        if (searchMeta != null) {
            searchMeta.displayName(Component.text("Search for Player", NamedTextColor.YELLOW));
            searchMeta.lore(List.of(Component.text("Click to search for a player on the server.", NamedTextColor.GRAY)));
            searchBtn.setItemMeta(searchMeta);
        }
        inv.setItem(48, searchBtn);

        // Rules button (Slot 50)
        ItemStack rulesBtn = new ItemStack(Material.BOOK);
        ItemMeta rulesMeta = rulesBtn.getItemMeta();
        if (rulesMeta != null) {
            rulesMeta.displayName(Component.text("Team Rules", NamedTextColor.YELLOW));
            rulesMeta.lore(List.of(
                Component.text("Click to view team rules.", NamedTextColor.GRAY),
                Component.text(player.getUniqueId().equals(data.leader) ? "⚡ You are the leader (Click to edit)" : "📖 Click to read", NamedTextColor.GOLD)
            ));
            rulesBtn.setItemMeta(rulesMeta);
        }
        inv.setItem(50, rulesBtn);

        // Fill members (slots 0 to 44)
        int slot = 0;
        for (UUID memberUUID : data.members) {
            if (slot >= 45) break;
            org.bukkit.OfflinePlayer member = Bukkit.getOfflinePlayer(memberUUID);
            String name = member.getName() != null ? member.getName() : "Unknown Player";
            String role = memberUUID.equals(data.leader) ? "Leader" : "Member";
            boolean online = member.isOnline();
            inv.setItem(slot++, getPlayerHead(memberUUID, name, role, online));
        }

        player.openInventory(inv);
    }

    private ItemStack getPlayerHead(UUID uuid, String name, String role, boolean online) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) skull.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
            meta.displayName(Component.text(name, NamedTextColor.YELLOW));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Role: " + role, NamedTextColor.GRAY));
            lore.add(Component.text("Status: " + (online ? "● Online" : "○ Offline"), online ? NamedTextColor.GREEN : NamedTextColor.RED));
            meta.lore(lore);
            skull.setItemMeta(meta);
        }
        return skull;
    }

    @EventHandler
    public void onPlayerChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        if (pendingPlayerSearch.containsKey(uuid)) {
            event.setCancelled(true);
            String searchName = event.getMessage().trim();
            pendingPlayerSearch.remove(uuid);

            Bukkit.getScheduler().runTask(this, () -> {
                org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(searchName);
                if (target.hasPlayedBefore() || target.isOnline()) {
                    String targetTeamLower = playerTeams.get(target.getUniqueId());
                    String targetName = target.getName() != null ? target.getName() : searchName;
                    if (targetTeamLower != null) {
                        TeamData targetTeam = teams.get(targetTeamLower);
                        player.sendMessage(Component.text("🔍 Search result: " + targetName + " is in team \"" + targetTeam.name + "\".", NamedTextColor.GREEN));
                    } else {
                        player.sendMessage(Component.text("🔍 Search result: " + targetName + " is not currently in any team.", NamedTextColor.YELLOW));
                    }
                } else {
                    player.sendMessage(Component.text("❌ Player \"" + searchName + "\" has never played on this server before.", NamedTextColor.RED));
                }
            });
        }
    }

    private void openTeamRulesBook(Player player) {
        String teamLower = playerTeams.get(player.getUniqueId());
        if (teamLower == null) return;
        TeamData data = teams.get(teamLower);
        if (data == null) return;

        boolean isLeader = player.getUniqueId().equals(data.leader);

        ItemStack book = new ItemStack(isLeader ? Material.WRITABLE_BOOK : Material.WRITTEN_BOOK);
        org.bukkit.inventory.meta.BookMeta meta = (org.bukkit.inventory.meta.BookMeta) book.getItemMeta();
        if (meta != null) {
            if (isLeader) {
                meta.setPages(data.rules);
            } else {
                meta.title(Component.text(data.name + " Rules"));
                meta.author(Component.text(data.leaderName));
                meta.setPages(data.rules);
            }
            book.setItemMeta(meta);
        }

        if (isLeader) {
            player.getInventory().addItem(book);
            player.sendMessage(Component.text("📖 A 'Book and Quill' has been added to your inventory. Open it to edit the team rules, then Sign/Done to save!", NamedTextColor.YELLOW));
        } else {
            player.openBook(book);
        }
    }

    @EventHandler
    public void onBookEdit(org.bukkit.event.player.PlayerEditBookEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (editingGlobalBook.containsKey(uuid)) {
            if (!event.isSigning()) {
                return;
            }
            String type = editingGlobalBook.remove(uuid);
            org.bukkit.inventory.meta.BookMeta newMeta = event.getNewBookMeta();
            List<String> pages = new ArrayList<>(newMeta.getPages());
            if (type.equals("rules")) {
                serverRules = pages;
                getConfig().set("server.rules", serverRules);
                saveConfig();
                player.sendMessage(Component.text("✅ Server rules updated successfully!", NamedTextColor.GREEN));
            } else if (type.equals("credits")) {
                serverCredits = pages;
                getConfig().set("server.credits", serverCredits);
                saveConfig();
                player.sendMessage(Component.text("✅ Server credits updated successfully!", NamedTextColor.GREEN));
            }
            Bukkit.getScheduler().runTaskLater(this, () -> {
                player.getInventory().remove(Material.WRITABLE_BOOK);
                player.getInventory().remove(Material.WRITTEN_BOOK);
            }, 1L);
            return;
        }

        String teamLower = playerTeams.get(uuid);
        if (teamLower == null) return;
        TeamData data = teams.get(teamLower);
        if (data == null) return;

        if (!uuid.equals(data.leader)) return;

        org.bukkit.inventory.meta.BookMeta newMeta = event.getNewBookMeta();
        data.rules = new ArrayList<>(newMeta.getPages());
        saveTeams();

        player.sendMessage(Component.text("✅ Team rules updated successfully!", NamedTextColor.GREEN));

        Bukkit.getScheduler().runTaskLater(this, () -> {
            player.getInventory().remove(Material.WRITABLE_BOOK);
            player.getInventory().remove(Material.WRITTEN_BOOK);
        }, 1L);
    }

    private void openKickConfirmationGui(Player leader, String targetName) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Kick: " + targetName));

        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        if (paneMeta != null) {
            paneMeta.displayName(Component.empty());
            pane.setItemMeta(paneMeta);
        }
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, pane);
        }

        ItemStack confirm = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.displayName(Component.text("Confirm Kick", NamedTextColor.GREEN));
            confirmMeta.lore(List.of(Component.text("Kicks " + targetName + " from the team.", NamedTextColor.GRAY)));
            confirm.setItemMeta(confirmMeta);
        }
        inv.setItem(11, confirm);

        ItemStack cancel = new ItemStack(Material.RED_CONCRETE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.displayName(Component.text("Cancel", NamedTextColor.RED));
            cancelMeta.lore(List.of(Component.text("Return to the team menu.", NamedTextColor.GRAY)));
            cancel.setItemMeta(cancelMeta);
        }
        inv.setItem(15, cancel);

        leader.openInventory(inv);
    }

    private void cleanupDuelArena(World world) {
        if (world == null) return;
        
        for (org.bukkit.entity.Entity entity : world.getEntities()) {
            if (entity instanceof org.bukkit.entity.Item) {
                entity.remove();
            }
            if (entity instanceof org.bukkit.entity.EnderCrystal) {
                entity.remove();
            }
        }

        for (var entry : duelBlockChanges.entrySet()) {
            Location loc = entry.getKey();
            BlockBackup backup = entry.getValue();
            Block block = loc.getBlock();
            block.setType(backup.material, false);
            block.setBlockData(backup.data, false);
        }
        duelBlockChanges.clear();
    }

    @EventHandler
    public void onDuelBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block.getWorld().getName().equalsIgnoreCase("duel")) {
            Location loc = block.getLocation();
            if (!duelBlockChanges.containsKey(loc)) {
                org.bukkit.block.BlockState state = event.getBlockReplacedState();
                duelBlockChanges.put(loc, new BlockBackup(state.getType(), state.getBlockData()));
            }
        }
    }

    @EventHandler
    public void onDuelBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getWorld().getName().equalsIgnoreCase("duel")) {
            Location loc = block.getLocation();
            if (!duelBlockChanges.containsKey(loc)) {
                duelBlockChanges.put(loc, new BlockBackup(block.getType(), block.getBlockData()));
            }
        }
    }

    @EventHandler
    public void onDuelExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        if (event.getLocation().getWorld().getName().equalsIgnoreCase("duel")) {
            for (Block block : event.blockList()) {
                Location loc = block.getLocation();
                if (!duelBlockChanges.containsKey(loc)) {
                    duelBlockChanges.put(loc, new BlockBackup(block.getType(), block.getBlockData()));
                }
            }
        }
    }

    @EventHandler
    public void onDuelBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        if (event.getBlock().getWorld().getName().equalsIgnoreCase("duel")) {
            for (Block block : event.blockList()) {
                Location loc = block.getLocation();
                if (!duelBlockChanges.containsKey(loc)) {
                    duelBlockChanges.put(loc, new BlockBackup(block.getType(), block.getBlockData()));
                }
            }
        }
    }

    @EventHandler
    public void onDuelBlockBurn(org.bukkit.event.block.BlockBurnEvent event) {
        Block block = event.getBlock();
        if (block.getWorld().getName().equalsIgnoreCase("duel")) {
            Location loc = block.getLocation();
            if (!duelBlockChanges.containsKey(loc)) {
                duelBlockChanges.put(loc, new BlockBackup(block.getType(), block.getBlockData()));
            }
        }
    }

    @EventHandler
    public void onCrystalPlace(org.bukkit.event.player.PlayerInteractEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        if (worldName.equalsIgnoreCase("spawn")) {
            if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
                return;
            }
            if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                ItemStack item = event.getItem();
                if (item != null && item.getType() == Material.END_CRYSTAL) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("❌ You cannot place End Crystals in the Spawn dimension!", NamedTextColor.RED));
                    return;
                }
            }
        }
        if (worldName.equalsIgnoreCase("duel")) {
            if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                ItemStack item = event.getItem();
                if (item != null && item.getType() == Material.END_CRYSTAL) {
                    Block clickedBlock = event.getClickedBlock();
                    if (clickedBlock != null && clickedBlock.getType() != Material.OBSIDIAN) {
                        event.setCancelled(true);
                        player.sendMessage(Component.text("❌ You can only place End Crystals on Obsidian inside this arena!", NamedTextColor.RED));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBucketEmpty(org.bukkit.event.player.PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            return;
        }
        if (player.getWorld().getName().equalsIgnoreCase("spawn")) {
            Material bucket = event.getBucket();
            if (bucket == Material.WATER_BUCKET || bucket == Material.LAVA_BUCKET) {
                event.setCancelled(true);
                player.sendMessage(Component.text("❌ You cannot place water or lava in the Spawn dimension!", NamedTextColor.RED));
            }
        }
    }

    @EventHandler
    public void onDuelDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player deceased = event.getEntity();
        if (deceased.getWorld().getName().equalsIgnoreCase("duel")) {
            Player killer = deceased.getKiller();
            if (killer != null && killer.isOnline() && killer.getWorld().getName().equalsIgnoreCase("duel")) {
                killer.sendTitle("§a§lVICTORY!", "§fYou won the duel!", 10, 40, 10);
                killer.sendMessage(Component.text("🏆 You won the duel! You have 1 minute to collect the loot before being teleported back to spawn.", NamedTextColor.GOLD));
                
                final Player finalKiller = killer;
                new org.bukkit.scheduler.BukkitRunnable() {
                    int timer = 60;
                    
                    @Override
                    public void run() {
                        if (!finalKiller.isOnline() || !finalKiller.getWorld().getName().equalsIgnoreCase("duel")) {
                            cleanupDuelArena(deceased.getWorld());
                            checkAndStartQueuedDuel();
                            cancel();
                            return;
                        }
                        if (timer <= 0) {
                            cleanupDuelArena(deceased.getWorld());
                            checkAndStartQueuedDuel();
                            cancel();
                            World spawnWorld = Bukkit.getWorld("spawn");
                            Location spawnLoc = spawnWorld != null ? spawnWorld.getSpawnLocation() : Bukkit.getWorlds().get(0).getSpawnLocation();
                            finalKiller.teleport(spawnLoc);
                            finalKiller.sendMessage(Component.text("🏠 Teleported back to spawn!", NamedTextColor.GREEN));
                            return;
                        }
                        if (timer == 30 || timer == 15 || timer == 10 || timer <= 5) {
                            finalKiller.sendMessage(Component.text("⏳ Teleporting back to spawn in " + timer + " seconds...", NamedTextColor.YELLOW));
                        }
                        timer--;
                    }
                }.runTaskTimer(this, 20L, 20L);
            } else {
                cleanupDuelArena(deceased.getWorld());
                checkAndStartQueuedDuel();
            }
        }
    }



    private void openDuelChestGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Duel Menu"));
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        if (paneMeta != null) {
            paneMeta.displayName(Component.text(" "));
            pane.setItemMeta(paneMeta);
        }
        for (int i = 0; i < 27; i++) {
            if (i != 11 && i != 13 && i != 15) {
                inv.setItem(i, pane);
            }
        }

        ItemStack join = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta joinMeta = join.getItemMeta();
        if (joinMeta != null) {
            joinMeta.displayName(Component.text("Join Queue", NamedTextColor.GREEN, net.kyori.adventure.text.format.TextDecoration.BOLD));
            joinMeta.lore(List.of(
                Component.text("Queue up for a 1v1 duel.", NamedTextColor.GRAY),
                Component.text("Players in queue: " + duelQueue.size(), NamedTextColor.YELLOW)
            ));
            join.setItemMeta(joinMeta);
        }
        inv.setItem(11, join);

        ItemStack cancel = new ItemStack(Material.RED_CONCRETE);
        ItemMeta cancelMeta = cancel.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.displayName(Component.text("Cancel Queue", NamedTextColor.RED, net.kyori.adventure.text.format.TextDecoration.BOLD));
            cancelMeta.lore(List.of(Component.text("Leave the matchmaking queue.", NamedTextColor.GRAY)));
            cancel.setItemMeta(cancelMeta);
        }
        inv.setItem(13, cancel);

        ItemStack duelPlayer = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta dpMeta = duelPlayer.getItemMeta();
        if (dpMeta != null) {
            dpMeta.displayName(Component.text("Duel Player", NamedTextColor.GOLD, net.kyori.adventure.text.format.TextDecoration.BOLD));
            dpMeta.lore(List.of(Component.text("Directly challenge an online player.", NamedTextColor.GRAY)));
            duelPlayer.setItemMeta(dpMeta);
        }
        inv.setItem(15, duelPlayer);

        player.openInventory(inv);
    }

    private void openDirectDuelSelectorGui(Player player, int page, String searchQuery) {
        UUID uuid = player.getUniqueId();
        duelPlayerPage.put(uuid, page);
        if (searchQuery != null) {
            duelPlayerSearchQuery.put(uuid, searchQuery);
        } else {
            duelPlayerSearchQuery.remove(uuid);
        }

        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Select Player to Duel" + (searchQuery != null ? " (Search)" : "")));

        List<Player> targetPlayers = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(player)) continue;
            if (searchQuery != null && !p.getName().toLowerCase().contains(searchQuery.toLowerCase())) continue;
            targetPlayers.add(p);
        }

        int startIndex = page * 45;
        int endIndex = Math.min(startIndex + 45, targetPlayers.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            Player target = targetPlayers.get(i);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
            if (skullMeta != null) {
                skullMeta.setOwningPlayer(target);
                skullMeta.displayName(Component.text(target.getName(), NamedTextColor.YELLOW, net.kyori.adventure.text.format.TextDecoration.BOLD));
                skullMeta.lore(List.of(Component.text("Click to challenge to a duel!", NamedTextColor.GRAY)));
                head.setItemMeta(skullMeta);
            }
            inv.setItem(slot++, head);
        }

        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        if (paneMeta != null) {
            paneMeta.displayName(Component.text(" "));
            pane.setItemMeta(paneMeta);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, pane);
        }

        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            if (prevMeta != null) {
                prevMeta.displayName(Component.text("Previous Page", NamedTextColor.YELLOW));
                prev.setItemMeta(prevMeta);
            }
            inv.setItem(45, prev);
        }

        ItemStack search = new ItemStack(Material.OAK_SIGN);
        ItemMeta searchMeta = search.getItemMeta();
        if (searchMeta != null) {
            searchMeta.displayName(Component.text("Search Player", NamedTextColor.GREEN));
            searchMeta.lore(List.of(Component.text("Type name to filter list.", NamedTextColor.GRAY)));
            search.setItemMeta(searchMeta);
        }
        inv.setItem(49, search);

        if (endIndex < targetPlayers.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            if (nextMeta != null) {
                nextMeta.displayName(Component.text("Next Page", NamedTextColor.YELLOW));
                next.setItemMeta(nextMeta);
            }
            inv.setItem(53, next);
        }

        player.openInventory(inv);
    }

    private void openDirectDuelConfirmationGui(Player player, Player target) {
        pendingDirectDuelChallenge.put(player.getUniqueId(), target.getUniqueId());
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Challenge " + target.getName() + "?"));

        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        if (paneMeta != null) {
            paneMeta.displayName(Component.text(" "));
            pane.setItemMeta(paneMeta);
        }
        for (int i = 0; i < 27; i++) {
            if (i != 11 && i != 15) {
                inv.setItem(i, pane);
            }
        }

        ItemStack confirm = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta confMeta = confirm.getItemMeta();
        if (confMeta != null) {
            confMeta.displayName(Component.text("Confirm Duel", NamedTextColor.GREEN, net.kyori.adventure.text.format.TextDecoration.BOLD));
            confirm.setItemMeta(confMeta);
        }
        inv.setItem(11, confirm);

        ItemStack cancel = new ItemStack(Material.RED_CONCRETE);
        ItemMeta cancMeta = cancel.getItemMeta();
        if (cancMeta != null) {
            cancMeta.displayName(Component.text("Cancel", NamedTextColor.RED, net.kyori.adventure.text.format.TextDecoration.BOLD));
            cancel.setItemMeta(cancMeta);
        }
        inv.setItem(15, cancel);

        player.openInventory(inv);
    }

    private void checkAndStartQueuedDuel() {
        World dualWorld = Bukkit.getWorld("duel");
        if (dualWorld == null) {
            WorldCreator creator = new WorldCreator("duel");
            creator.environment(World.Environment.NORMAL);
            dualWorld = Bukkit.createWorld(creator);
        }
        if (dualWorld == null) return;

        int activeFighters = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().getName().equalsIgnoreCase("duel")) {
                activeFighters++;
            }
        }

        if (activeFighters >= 2) {
            return;
        }

        List<Player> playersToDuel = new ArrayList<>();
        java.util.Iterator<UUID> iter = duelQueue.iterator();
        while (iter.hasNext() && playersToDuel.size() < 2) {
            UUID id = iter.next();
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                playersToDuel.add(p);
            }
            iter.remove();
        }

        if (playersToDuel.size() < 2) {
            for (Player p : playersToDuel) {
                if (!duelQueue.contains(p.getUniqueId())) {
                    duelQueue.add(0, p.getUniqueId());
                }
            }
            return;
        }

        Player p1 = playersToDuel.get(0);
        Player p2 = playersToDuel.get(1);

        p1.sendMessage(Component.text("⚔️ Match found! Teleporting to duel...", NamedTextColor.GREEN));
        p2.sendMessage(Component.text("⚔️ Match found! Teleporting to duel...", NamedTextColor.GREEN));

        dualWorld.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
        dualWorld.setTime(6000L);

        org.bukkit.WorldBorder border = dualWorld.getWorldBorder();
        border.setCenter(0.0, 0.0);
        border.setSize(50.0);

        cleanupDuelArena(dualWorld);

        double y1 = dualWorld.getHighestBlockYAt(-15, 0);
        double y2 = dualWorld.getHighestBlockYAt(15, 0);

        Location loc1 = new Location(dualWorld, -15.5, y1 + 1.0, 0.5, -90f, 0f);
        Location loc2 = new Location(dualWorld, 15.5, y2 + 1.0, 0.5, 90f, 0f);

        p1.teleport(loc1);
        p2.teleport(loc2);

        p1.sendMessage(Component.text("⚔️ Duel started! Good luck!", NamedTextColor.GOLD));
        p2.sendMessage(Component.text("⚔️ Duel started! Good luck!", NamedTextColor.GOLD));
    }

    @EventHandler
    public void onEnderPearl(org.bukkit.event.player.PlayerTeleportEvent event) {
        if (event.getCause() == org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            if (event.getFrom().getWorld().getName().equalsIgnoreCase("duel")) {
                Location to = event.getTo();
                if (to == null || !to.getWorld().getName().equalsIgnoreCase("duel")) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(Component.text("❌ You cannot ender pearl out of the duel arena!", NamedTextColor.RED));
                    return;
                }
                double limit = 25.0; // 50x50 centered at 0, 0
                if (Math.abs(to.getX()) > limit || Math.abs(to.getZ()) > limit) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(Component.text("❌ You cannot ender pearl outside the arena!", NamedTextColor.RED));
                }
            }
        }
    }

    private ItemStack createFoodGeneratorItem() {
        ItemStack item = new ItemStack(Material.SPAWNER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Food Generator", NamedTextColor.GOLD, net.kyori.adventure.text.format.TextDecoration.BOLD));
            meta.lore(List.of(
                Component.text("Generates steak every minute.", NamedTextColor.YELLOW),
                Component.text("1. Place on the ground.", NamedTextColor.GRAY),
                Component.text("2. Left-click placed block to open inventory.", NamedTextColor.GRAY),
                Component.text("3. Sneak + left-click block to break.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "food_generator");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createOreGeneratorItem() {
        ItemStack item = new ItemStack(Material.SPAWNER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Ore Generator", NamedTextColor.AQUA, net.kyori.adventure.text.format.TextDecoration.BOLD));
            meta.lore(List.of(
                Component.text("Generates diamonds every minute.", NamedTextColor.YELLOW),
                Component.text("1. Place on the ground.", NamedTextColor.GRAY),
                Component.text("2. Left-click placed block to open inventory.", NamedTextColor.GRAY),
                Component.text("3. Sneak + left-click block to break.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "ore_generator");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createToolsGeneratorItem() {
        ItemStack item = new ItemStack(Material.SPAWNER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Tools Generator", NamedTextColor.LIGHT_PURPLE, net.kyori.adventure.text.format.TextDecoration.BOLD));
            meta.lore(List.of(
                Component.text("Generates tools & armor (except netherite) every minute.", NamedTextColor.YELLOW),
                Component.text("1. Place on the ground.", NamedTextColor.GRAY),
                Component.text("2. Left-click placed block to open inventory.", NamedTextColor.GRAY),
                Component.text("3. Sneak + left-click block to break.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "tools_generator");
            item.setItemMeta(meta);
        }
        return item;
    }

    private Material getRandomToolOrArmorMaterial() {
        Material[] materials = {
            Material.DIAMOND_SWORD, Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE, Material.DIAMOND_SHOVEL, Material.DIAMOND_HOE,
            Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
            Material.IRON_SWORD, Material.IRON_PICKAXE, Material.IRON_AXE, Material.IRON_SHOVEL, Material.IRON_HOE,
            Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
            Material.GOLDEN_SWORD, Material.GOLDEN_PICKAXE, Material.GOLDEN_AXE, Material.GOLDEN_SHOVEL, Material.GOLDEN_HOE,
            Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
            Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
            Material.STONE_SWORD, Material.STONE_PICKAXE, Material.STONE_AXE, Material.STONE_SHOVEL, Material.STONE_HOE,
            Material.WOODEN_SWORD, Material.WOODEN_PICKAXE, Material.WOODEN_AXE, Material.WOODEN_SHOVEL, Material.WOODEN_HOE,
            Material.BOW, Material.CROSSBOW, Material.SHIELD, Material.TRIDENT, Material.SHEARS, Material.FLINT_AND_STEEL
        };
        return materials[random.nextInt(materials.length)];
    }

    private void saveGenerators() {
        getConfig().set("generators", null);
        for (var entry : generators.entrySet()) {
            Location loc = entry.getKey();
            GeneratorData data = entry.getValue();
            String key = loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
            String path = "generators." + key;
            getConfig().set(path + ".world", loc.getWorld().getName());
            getConfig().set(path + ".x", loc.getX());
            getConfig().set(path + ".y", loc.getY());
            getConfig().set(path + ".z", loc.getZ());
            getConfig().set(path + ".type", data.type);
            
            List<ItemStack> itemList = new ArrayList<>();
            for (ItemStack item : data.inventory.getContents()) {
                itemList.add(item != null ? item : new ItemStack(Material.AIR));
            }
            getConfig().set(path + ".items", itemList);
        }
        saveConfig();
    }

    private void loadGenerators() {
        generators.clear();
        if (!getConfig().contains("generators")) return;
        org.bukkit.configuration.ConfigurationSection sec = getConfig().getConfigurationSection("generators");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            String path = "generators." + key;
            String worldName = getConfig().getString(path + ".world");
            double x = getConfig().getDouble(path + ".x");
            double y = getConfig().getDouble(path + ".y");
            double z = getConfig().getDouble(path + ".z");
            World w = Bukkit.getWorld(worldName);
            if (w == null) continue;
            Location loc = new Location(w, x, y, z);
            String type = getConfig().getString(path + ".type");
            
            String title = capitalize(type.replace("_", " "));
            Inventory inventory = Bukkit.createInventory(null, 27, Component.text(title));
            if (getConfig().contains(path + ".items")) {
                List<?> list = getConfig().getList(path + ".items");
                if (list != null) {
                    for (int i = 0; i < Math.min(27, list.size()); i++) {
                        if (list.get(i) instanceof ItemStack) {
                            inventory.setItem(i, (ItemStack) list.get(i));
                        }
                    }
                }
            }
            generators.put(loc, new GeneratorData(loc, type, inventory));
        }
    }

    @EventHandler
    public void onGeneratorInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        if (title.equals("Food Generator") || title.equals("Ore Generator") || title.equals("Tools Generator")) {
            saveGenerators();
        }
    }

    private void openCartGui(Player player) {
        UUID uuid = player.getUniqueId();
        Material mat = cartItem.get(uuid);
        if (mat == null) return;
        int qty = cartQuantity.getOrDefault(uuid, 1);
        int unitPrice = cartUnitPrice.getOrDefault(uuid, 0);
        int totalPrice = unitPrice * qty;

        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Shop - Confirm Purchase"));
        ItemStack pane = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.GRAY);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, pane);
        }

        inv.setItem(9, createGuiItem(Material.RED_WOOL, "Cancel", NamedTextColor.RED, "Cancel the order and return"));
        inv.setItem(10, createGuiItem(Material.RED_DYE, "Remove 1", NamedTextColor.RED, "Remove 1 from cart"));
        inv.setItem(11, createGuiItem(Material.RED_DYE, "Remove 5", NamedTextColor.RED, "Remove 5 from cart"));
        inv.setItem(12, createGuiItem(Material.RED_DYE, "Remove 10", NamedTextColor.RED, "Remove 10 from cart"));

        ItemStack itemShow = new ItemStack(mat, Math.min(qty, mat.getMaxStackSize()));
        ItemMeta meta = itemShow.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(qty + "x " + capitalize(mat.name().replace("_", " ")), NamedTextColor.YELLOW, net.kyori.adventure.text.format.TextDecoration.BOLD));
            meta.lore(List.of(
                Component.text("Unit Price: " + unitPrice + " Erpies", NamedTextColor.GRAY),
                Component.text("Total Cost: " + totalPrice + " Erpies", NamedTextColor.GOLD)
            ));
            itemShow.setItemMeta(meta);
        }
        inv.setItem(13, itemShow);

        inv.setItem(14, createGuiItem(Material.GREEN_WOOL, "Purchase", NamedTextColor.GREEN, "Pay " + totalPrice + " Erpies and receive items"));
        inv.setItem(15, createGuiItem(Material.GREEN_DYE, "Add 1", NamedTextColor.GREEN, "Add 1 to cart"));
        inv.setItem(16, createGuiItem(Material.GREEN_DYE, "Add 5", NamedTextColor.GREEN, "Add 5 to cart"));
        inv.setItem(17, createGuiItem(Material.GREEN_DYE, "Add 10", NamedTextColor.GREEN, "Add 10 to cart"));

        player.openInventory(inv);
    }

    private void returnToCategory(Player player) {
        UUID uuid = player.getUniqueId();
        String category = cartCategory.remove(uuid);
        if (category == null) {
            openMainMenu(player);
            return;
        }
        if (category.contains("End Items")) {
            openEndMenu(player);
        } else if (category.contains("PvP Combat")) {
            openPvpMenu(player);
        } else if (category.contains("Food")) {
            openFoodMenu(player);
        } else {
            openMainMenu(player);
        }
    }


    private String adminTokenForPoller = "";

    private void loadAdminToken() {
        java.io.File file = new java.io.File(".secret/tokens.properties");
        if (file.exists()) {
            try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
                java.util.Properties props = new java.util.Properties();
                props.load(fis);
                adminTokenForPoller = props.getProperty("ADMIN_TOKEN", "").trim();
            } catch (Exception e) {
                getLogger().warning("Failed to load .secret/tokens.properties: " + e.getMessage());
            }
        } else {
            getLogger().warning(".secret/tokens.properties file not found at " + file.getAbsolutePath());
        }
    }

    private com.sun.net.httpserver.HttpServer webhookServer = null;

    private void startWebhookServer() {
        if (adminTokenForPoller == null || adminTokenForPoller.isEmpty()) {
            getLogger().warning("Webhook server did not start because ADMIN_TOKEN was empty or missing.");
            return;
        }
        try {
            webhookServer = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress(8081), 0);
            
            webhookServer.createContext("/webhook", exchange -> {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    exchange.sendResponseHeaders(405, -1);
                    exchange.close();
                    return;
                }

                String token = exchange.getRequestHeaders().getFirst("X-Admin-Token");
                if (token == null || !token.equals(adminTokenForPoller)) {
                    exchange.sendResponseHeaders(401, -1);
                    exchange.close();
                    return;
                }

                try {
                    java.io.InputStream is = exchange.getRequestBody();
                    String body = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    com.google.gson.JsonObject payload = gson.fromJson(body, com.google.gson.JsonObject.class);
                    
                    if (payload != null && payload.has("username")) {
                        String username = payload.get("username").getAsString();
                        com.google.gson.JsonArray items = payload.has("items") ? payload.getAsJsonArray("items") : new com.google.gson.JsonArray();
                        
                        List<String> cmds = new ArrayList<>();
                        for (com.google.gson.JsonElement itemEl : items) {
                            if (!itemEl.isJsonObject()) continue;
                            com.google.gson.JsonObject itemObj = itemEl.getAsJsonObject();
                            if (!itemObj.has("id")) continue;
                            String itemId = itemObj.get("id").getAsString();
                            int qty = itemObj.has("quantity") ? itemObj.get("quantity").getAsInt() : 1;

                            if ("erpie".equals(itemId)) {
                                cmds.add("setrank " + username + " erp+");
                            } else if ("erpiepro".equals(itemId)) {
                                cmds.add("setrank " + username + " erp++");
                            } else if ("erpiepromaxx".equals(itemId)) {
                                cmds.add("setrank " + username + " erp+++");
                            } else if ("echokey".equals(itemId)) {
                                cmds.add("echokeys " + username + " add " + qty);
                            } else if ("crimsonkey".equals(itemId)) {
                                cmds.add("crimsonkeys " + username + " add " + qty);
                            } else if ("endkey".equals(itemId)) {
                                cmds.add("keys end add " + qty + " " + username);
                            } else if ("amethystkey".equals(itemId)) {
                                cmds.add("keys amethyst add " + qty + " " + username);
                            } else if ("basickey".equals(itemId)) {
                                cmds.add("keys basic add " + qty + " " + username);
                            }
                        }

                        Bukkit.getScheduler().runTask(this, () -> {
                            for (String cmd : cmds) {
                                getLogger().info("Webhook executing auto-delivery command: " + cmd);
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                            }
                        });
                    }

                    String response = "{\"success\":true}";
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.length());
                    java.io.OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                } catch (Exception parseErr) {
                    getLogger().warning("Error processing webhook body: " + parseErr.getMessage());
                    exchange.sendResponseHeaders(400, -1);
                    exchange.close();
                }
            });

            webhookServer.setExecutor(java.util.concurrent.Executors.newSingleThreadExecutor());
            webhookServer.start();
            getLogger().info("🚀 Webhook server successfully listening on port 8081");
        } catch (Exception e) {
            getLogger().warning("Failed to start webhook server: " + e.getMessage());
        }
    }

    private void stopWebhookServer() {
        if (webhookServer != null) {
            webhookServer.stop(1);
            getLogger().info("Stopped Webhook server.");
        }
    }
}