package net.theerpsmp.plugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.io.File;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.Comparator;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import org.bukkit.Sound;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
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
import org.bukkit.inventory.EquipmentSlot;
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

    private final Object dbLock = new Object();

    public Connection getConnection() {
        try {
            File dataFolder = getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File dbFile = new File(dataFolder, "playerdata.db");
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        } catch (Exception e) {
            getLogger().severe("Database connection error: " + e.getMessage());
            return null;
        }
    }

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
    private final HashMap<UUID, Boolean> hasVipMap = new HashMap<>();

    private long nextKeyallTime = 0;
    private final HashMap<UUID, java.util.Set<UUID>> hiddenEntitiesMap = new HashMap<>();

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
    private final HashMap<UUID, String> pendingOrderItemName = new HashMap<>(); // stores item name between order steps
    private final HashMap<UUID, Integer> pendingOrderQuantity = new HashMap<>(); // stores chosen quantity (max 1M)
    private final HashMap<UUID, String> pendingOrderSearchQuery = new HashMap<>(); // stores active item search filter
    private final HashMap<UUID, String> orderBoardSearchQuery = new HashMap<>(); // stores active order board filter
    private final HashMap<UUID, Integer> pendingOrderPage = new HashMap<>(); // stores active Choose Item page
    private boolean breakingCustom = false;

    // Combat tag system
    private final HashMap<UUID, Integer> combatTagTicks = new HashMap<>();
    private final java.util.Set<UUID> dualNightVisionPlayers = new java.util.HashSet<>();

    // Wand point selection system
    private final HashMap<UUID, Location> wandPoint1 = new HashMap<>();
    private final HashMap<UUID, Location> wandPoint2 = new HashMap<>();

    // Custom Protection fields
    private String protectionWorld = null;
    private double protectionMinX = 0;
    private double protectionMinY = 0;
    private double protectionMinZ = 0;
    private double protectionMaxX = 0;
    private double protectionMaxY = 0;
    private double protectionMaxZ = 0;
    private boolean protectionEnabled = false;

    // Command Chest fields
    private final HashMap<Location, String> commandChests = new HashMap<>();
    private final HashMap<UUID, Location> activeCommandChestSetup = new HashMap<>();
    private final HashMap<UUID, Inventory> customEnderChests = new HashMap<>();

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
        public List<UUID> requests = new ArrayList<>();
        public List<ItemStack[]> vaultPages = new ArrayList<>();

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
    private final HashMap<UUID, Integer> playerVaultPage = new HashMap<>();
    private final HashMap<UUID, Integer> playerAllTeamsPage = new HashMap<>();
    private final HashMap<UUID, Integer> playerTeamRequestPage = new HashMap<>();
    private final HashMap<UUID, String> playerPasswords = new HashMap<>();
    private final java.util.Set<UUID> loggedInPlayers = new java.util.HashSet<>();
    private final java.util.Set<UUID> openedWithdrawViaCommand = new java.util.HashSet<>();
    private final HashMap<UUID, Integer> erpItemPage = new HashMap<>();
    private final HashMap<UUID, org.bukkit.scheduler.BukkitTask> pendingWarpTeleports = new HashMap<>();
    private final HashMap<UUID, Location> pendingWarpStartLocations = new HashMap<>();

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
    private final HashMap<UUID, java.util.Set<String>> activeNametags = new HashMap<>();
    private final HashMap<UUID, List<org.bukkit.entity.TextDisplay>> playerTagDisplays = new HashMap<>();
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
    private static final UUID RED_TOPPAT_UUID = UUID.fromString("00000000-0000-0000-0009-01f06c518376");
    private static final UUID MERP208_UUID = UUID.fromString("1ba04e2c-6696-3083-8059-c94736a7d303");
    private static final UUID BOREAS_UUID = UUID.fromString("00000000-0000-0000-0009-01f9fff22f06");

    private static boolean isRedToppat(UUID uuid) {
        return uuid != null && (uuid.equals(RED_TOPPAT_UUID) || uuid.equals(MERP208_UUID));
    }

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
    private final HashMap<UUID, UUID> tpahereRequests = new HashMap<>();

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
    private final HashMap<UUID, Boolean> activeFloatingTextIsLeaderboard = new HashMap<>();
    private final HashMap<UUID, Boolean> chatSpamDisabled = new HashMap<>();
    private final HashMap<UUID, Boolean> tpaDisabled = new HashMap<>();
    private final HashMap<UUID, Boolean> voiceChatEnabled = new HashMap<>();
    private final HashMap<UUID, Boolean> musicDisabled = new HashMap<>();
    private final HashMap<UUID, Boolean> starterLootDisabled = new HashMap<>();
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
    private final HashMap<UUID, Boolean> openedWithSethome = new HashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<UUID, Long> goatyArmorProtectionUntil = new java.util.concurrent.ConcurrentHashMap<>();

    // Shop Crates
    private final HashMap<Location, ShopCrateData> shopCrates = new HashMap<>();
    private final HashMap<UUID, Location> activeCrateSetup = new HashMap<>();
    private final HashMap<UUID, Location> activeCratePurchase = new HashMap<>();
    private final HashMap<UUID, ItemStack[]> pendingCrateItemsArray = new HashMap<>();
    private final HashMap<UUID, Location> activeMobGeneratorSetup = new HashMap<>();

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

    public enum SignAction { SEARCH, LIST_PRICE, SET_CRATE_PRICE, ORDER_ITEM, ORDER_PRICE, TEAM_SEARCH, BANK_DEPOSIT, BANK_WITHDRAW, SET_COMMAND_CHEST, DUEL_PLAYER_SEARCH, HOME_SEARCH, HOME_RENAME, WITHDRAW_ERPIES_ONLY, WITHDRAW_DERPIES_ONLY, DEPOSIT_MONEY_ONLY, ORDER_QUANTITY, ORDER_SEARCH, ORDER_BOARD_SEARCH }

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
            this(UUID.randomUUID(), buyer, buyerName, itemName, quantity, price);
        }

        public OrderRequest(UUID id, UUID buyer, String buyerName, String itemName, int quantity, long price) {
            this.id = id != null ? id : UUID.randomUUID();
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
        initDatabase();
        migrateYamlToDatabase();
        loadOrdersFromDatabase();
        getServer().getPluginManager().registerEvents(this, this);

        // Load data for all currently online players (e.g. after reload or plugin update)
        for (Player online : Bukkit.getOnlinePlayers()) {
            loadPlayerData(online.getUniqueId());
        }

        // Ensure "spawn" world is loaded/created flat
        World spawnWorld = Bukkit.getWorld("spawn");
        if (spawnWorld == null) {
            WorldCreator creator = new WorldCreator("spawn");
            creator.type(WorldType.FLAT);
            creator.generateStructures(false);
            spawnWorld = Bukkit.createWorld(creator);
        }

        // Ensure "echo_valley" world is loaded/created
        World echoValleyWorld = Bukkit.getWorld("echo_valley");
        if (echoValleyWorld == null) {
            WorldCreator creator = new WorldCreator("echo_valley");
            creator.environment(World.Environment.NORMAL);
            echoValleyWorld = Bukkit.createWorld(creator);
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

        // Load custom protection settings
        if (getConfig().contains("protection.enabled")) {
            protectionEnabled = getConfig().getBoolean("protection.enabled");
            protectionWorld = getConfig().getString("protection.world");
            protectionMinX = getConfig().getDouble("protection.minX");
            protectionMinY = getConfig().getDouble("protection.minY");
            protectionMinZ = getConfig().getDouble("protection.minZ");
            protectionMaxX = getConfig().getDouble("protection.maxX");
            protectionMaxY = getConfig().getDouble("protection.maxY");
            protectionMaxZ = getConfig().getDouble("protection.maxZ");
        }

        // Ensure duel arena worlds have rain and locked settings
        for (World world : Bukkit.getWorlds()) {
            if (isDuelWorld(world)) {
                applyDuelWorldSettings(world);
            }
        }
        
        if (getCommand("warp") != null) getCommand("warp").setExecutor(this);
        if (getCommand("setwarp") != null) getCommand("setwarp").setExecutor(this);
        if (getCommand("setprotection") != null) getCommand("setprotection").setExecutor(this);
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
        if (getCommand("tpahere") != null) getCommand("tpahere").setExecutor(this);
        if (getCommand("tpaccept") != null) getCommand("tpaccept").setExecutor(this);
        if (getCommand("pay") != null) getCommand("pay").setExecutor(this);
        if (getCommand("stash") != null) getCommand("stash").setExecutor(this);
        if (getCommand("erpies") != null) getCommand("erpies").setExecutor(this);
        if (getCommand("currency") != null) getCommand("currency").setExecutor(this);
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
        if (getCommand("register") != null) getCommand("register").setExecutor(this);
        if (getCommand("login") != null) getCommand("login").setExecutor(this);
        if (getCommand("passwordreset") != null) getCommand("passwordreset").setExecutor(this);
        if (getCommand("deposit") != null) getCommand("deposit").setExecutor(this);
        if (getCommand("withdraw") != null) getCommand("withdraw").setExecutor(this);
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
        if (getCommand("rank") != null) getCommand("rank").setExecutor(this);
        if (getCommand("setrank") != null) getCommand("setrank").setExecutor(this);
        if (getCommand("stats") != null) getCommand("stats").setExecutor(this);
        loadAdminToken();
        startWebhookServer();

        // Clean up orphaned player tag displays on startup
        for (World world : Bukkit.getWorlds()) {
            for (org.bukkit.entity.TextDisplay display : world.getEntitiesByClass(org.bukkit.entity.TextDisplay.class)) {
                if (display.getPersistentDataContainer().has(new NamespacedKey(this, "is_player_tag"), PersistentDataType.BOOLEAN)) {
                    display.remove();
                }
            }
        }

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                timePlayedMap.put(uuid, timePlayedMap.getOrDefault(uuid, 0) + 1);
                updateScoreboard(player);

                // Ensure floating nametags match spectator and invisibility state
                boolean hideTags = shouldHideNametag(player);
                boolean hasDisplays = playerTagDisplays.containsKey(uuid);
                if (hideTags && hasDisplays) {
                    updatePlayerFloatingTags(player);
                } else if (!hideTags && !hasDisplays && hasAnyFloatingTags(player)) {
                    updatePlayerFloatingTags(player);
                }

                if (isDuelWorld(player.getWorld())) {
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
            for (World world : Bukkit.getWorlds()) {
                if (isDuelWorld(world)) {
                    if (!world.hasStorm()) {
                        applyDuelWorldSettings(world);
                    }
                }
            }
            updateCustomCrateHolograms();
            updateLeaderboardFloatingTexts();

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

        // Hourly reward tracking tasks (checks nextKeyallTime every second)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (System.currentTimeMillis() >= nextKeyallTime) {
                nextKeyallTime = System.currentTimeMillis() + (60 * 60 * 1000);
                getConfig().set("nextKeyallTime", nextKeyallTime);
                saveConfig();

                for (Player player : Bukkit.getOnlinePlayers()) {
                    UUID uuid = player.getUniqueId();
                    keysMap.put(uuid, keysMap.getOrDefault(uuid, 0) + 1);
                    if (!chatSpamDisabled.getOrDefault(uuid, false)) {
                        player.sendMessage(Component.text("🎉 You received 1 Key reward for playing for an hour!", NamedTextColor.GOLD));
                    }
                    updateScoreboard(player);
                }
            }
        }, 20L, 20L);

        // AFK Zone & Rank minute reward tracking task
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                boolean isAfk = player.getWorld().getName().equals("afk_zone") || player.getWorld().getName().equals("afk");
                
                long derpiesEarned = 0;
                String source = "";

                if (hasErpProMaxMap.getOrDefault(uuid, false)) { // Erp+ Pro Max
                    if (isAfk) {
                        derpiesEarned = 10;
                        source = "Erp+ Pro Max (AFK Zone)";
                    } else {
                        derpiesEarned = 5;
                        source = "Erp+ Pro Max (Passive)";
                    }
                } else if (hasErpProMap.getOrDefault(uuid, false)) { // Erp+ Pro
                    if (isAfk) {
                        derpiesEarned = 2;
                        source = "Erp+ Pro (AFK Zone)";
                    } else {
                        derpiesEarned = 1;
                        source = "Erp+ Pro (Passive)";
                    }
                } else if (hasErpPlusMap.getOrDefault(uuid, false)) { // Erp+
                    derpiesEarned = 1;
                    source = isAfk ? "Erp+ (AFK)" : "Erp+ (Passive)";
                } else if (hasVipMap.getOrDefault(uuid, false)) { // VIP
                    if (isAfk) {
                        derpiesEarned = 1;
                        source = "VIP (AFK)";
                    } else {
                        derpiesEarned = ((System.currentTimeMillis() / 60000) % 2 == 0) ? 1 : 0;
                        source = "VIP (Passive)";
                    }
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
                }
            }
        }, 1200L, 1200L);

        // Mob Generator 5-second production & Hopper pulling task
        final int[] ticks = {0};
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            ticks[0] += 20;

            // Every 5 seconds (100 ticks), spawn mob generator items
            if (ticks[0] >= 100) {
                ticks[0] = 0;
                for (GeneratorData gen : generators.values()) {
                    ItemStack toAdd = null;
                    if (gen.type.equals("iron_golem_generator")) {
                        toAdd = new ItemStack(Material.IRON_INGOT);
                    } else if (gen.type.equals("snow_golem_generator")) {
                        toAdd = new ItemStack(Material.SNOWBALL);
                    } else if (gen.type.equals("creeper_generator")) {
                        toAdd = new ItemStack(Material.GUNPOWDER);
                    } else if (gen.type.equals("skeleton_generator")) {
                        toAdd = new ItemStack(Material.BONE);
                    } else if (gen.type.equals("witch_generator")) {
                        toAdd = new ItemStack(Material.POTION);
                        org.bukkit.inventory.meta.PotionMeta pm = (org.bukkit.inventory.meta.PotionMeta) toAdd.getItemMeta();
                        if (pm != null) {
                            org.bukkit.potion.PotionType[] types = org.bukkit.potion.PotionType.values();
                            org.bukkit.potion.PotionType randomType = types[random.nextInt(types.length)];
                            try {
                                pm.setBasePotionType(randomType);
                            } catch (NoSuchMethodError e) {
                                try {
                                    java.lang.reflect.Constructor<?> dataConst = org.bukkit.potion.PotionData.class.getConstructor(org.bukkit.potion.PotionType.class);
                                    pm.setBasePotionData((org.bukkit.potion.PotionData) dataConst.newInstance(randomType));
                                } catch (Exception ex) {
                                    // Fallback
                                }
                            }
                            toAdd.setItemMeta(pm);
                        }
                    }

                    if (toAdd != null) {
                        gen.inventory.addItem(toAdd);
                    }
                }
            }

            // Every 1 second (20 ticks), check for hoppers below ANY generator
            for (GeneratorData gen : generators.values()) {
                Location loc = gen.loc;
                Block blockBelow = loc.getBlock().getRelative(org.bukkit.block.BlockFace.DOWN);
                if (blockBelow.getType() == Material.HOPPER) {
                    org.bukkit.block.BlockState state = blockBelow.getState();
                    if (state instanceof org.bukkit.block.Hopper) {
                        org.bukkit.block.Hopper hopper = (org.bukkit.block.Hopper) state;
                        Inventory hopperInv = hopper.getInventory();
                        Inventory genInv = gen.inventory;

                        // Find first item in generator inventory
                        for (int i = 0; i < genInv.getSize(); i++) {
                            ItemStack item = genInv.getItem(i);
                            if (item != null && item.getType() != Material.AIR) {
                                ItemStack toMove = item.clone();
                                toMove.setAmount(1);
                                HashMap<Integer, ItemStack> remaining = hopperInv.addItem(toMove);
                                if (remaining.isEmpty()) {
                                    item.setAmount(item.getAmount() - 1);
                                    genInv.setItem(i, item.getAmount() <= 0 ? null : item);
                                    break; // move 1 item per generator per tick
                                }
                            }
                        }
                    }
                }
            }
        }, 20L, 20L);

        // Periodic auto-save generators task (every 5 minutes) to avoid synchronous disk write lag
        Bukkit.getScheduler().runTaskTimer(this, this::saveGenerators, 6000L, 6000L);

        // Floating text display line-of-sight culling task (runs every 10 ticks / 0.5 seconds)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.isOnline()) continue;
                World world = player.getWorld();
                Location eyeLoc = player.getEyeLocation();
                
                for (org.bukkit.entity.TextDisplay display : world.getEntitiesByClass(org.bukkit.entity.TextDisplay.class)) {
                    boolean isPlayerTag = display.getPersistentDataContainer().has(new NamespacedKey(this, "is_player_tag"), PersistentDataType.BOOLEAN);
                    if (display.getVehicle() == null || isPlayerTag) {
                        double distSq = display.getLocation().distanceSquared(eyeLoc);
                        boolean canSee = false;
                        if (distSq <= 1600.0) { // Within 40 blocks
                            canSee = player.hasLineOfSight(display);
                        }
                        
                        // Spectator mode visibility culling: viewer cannot see tags
                        if (player.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                            canSee = false;
                        }
                        
                        if (isPlayerTag) {
                            String ownerUUIDStr = display.getPersistentDataContainer().get(new NamespacedKey(this, "player_tag_owner"), PersistentDataType.STRING);
                            if (ownerUUIDStr != null) {
                                try {
                                    UUID ownerUUID = UUID.fromString(ownerUUIDStr);
                                    if (player.getUniqueId().equals(ownerUUID)) {
                                        canSee = false;
                                    } else {
                                        Player owner = Bukkit.getPlayer(ownerUUID);
                                        // If owner is offline or in spectator mode, tag is hidden
                                        if (owner == null || !owner.isOnline() || owner.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                                            canSee = false;
                                        }
                                    }
                                } catch (Exception ignored) {
                                    canSee = false;
                                }
                            }
                        }
                        
                        updateDisplayVisibility(player, display, canSee);
                    }
                }
            }
        }, 20L, 10L);

        // Periodic cleanup of orphaned player overhead tag displays (runs every 5 seconds)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (World world : Bukkit.getWorlds()) {
                for (org.bukkit.entity.TextDisplay display : world.getEntitiesByClass(org.bukkit.entity.TextDisplay.class)) {
                    if (display.getPersistentDataContainer().has(new NamespacedKey(this, "is_player_tag"), PersistentDataType.BOOLEAN)) {
                        String ownerUUIDStr = display.getPersistentDataContainer().get(new NamespacedKey(this, "player_tag_owner"), PersistentDataType.STRING);
                        if (ownerUUIDStr != null) {
                            try {
                                UUID ownerUUID = UUID.fromString(ownerUUIDStr);
                                Player owner = Bukkit.getPlayer(ownerUUID);
                                if (owner == null || !owner.isOnline()) {
                                    display.remove();
                                }
                            } catch (Exception e) {
                                display.remove();
                            }
                        } else {
                            display.remove();
                        }
                    }
                }
            }
        }, 100L, 100L);

        loadShopCrates();
        loadTeams();
        loadCommandChests();
        loadGenerators();

        if (getConfig().contains("nextKeyallTime")) {
            nextKeyallTime = getConfig().getLong("nextKeyallTime");
            if (nextKeyallTime <= System.currentTimeMillis()) {
                nextKeyallTime = System.currentTimeMillis() + (60 * 60 * 1000);
                getConfig().set("nextKeyallTime", nextKeyallTime);
                saveConfig();
            }
        } else {
            nextKeyallTime = System.currentTimeMillis() + (60 * 60 * 1000);
            getConfig().set("nextKeyallTime", nextKeyallTime);
            saveConfig();
        }

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
        saveAllOrdersSync();

        // Clean up all spawned floating nametags
        for (List<org.bukkit.entity.TextDisplay> displays : playerTagDisplays.values()) {
            for (org.bukkit.entity.TextDisplay td : displays) {
                if (td.isValid()) td.remove();
            }
        }
        playerTagDisplays.clear();
    }

    public Inventory getCustomEnderChest(Player player) {
        UUID uuid = player.getUniqueId();
        if (customEnderChests.containsKey(uuid)) {
            return customEnderChests.get(uuid);
        }
        
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Ender Chest", NamedTextColor.DARK_PURPLE));
        String path = "players." + uuid.toString() + ".enderChest";
        if (getConfig().contains(path)) {
            org.bukkit.configuration.ConfigurationSection sec = getConfig().getConfigurationSection(path);
            if (sec != null) {
                for (String key : sec.getKeys(false)) {
                    try {
                        int slot = Integer.parseInt(key);
                        if (slot >= 0 && slot < 54) {
                            ItemStack item = sec.getItemStack(key);
                            inv.setItem(slot, item);
                        }
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            }
        }
        customEnderChests.put(uuid, inv);
        return inv;
    }

    public static String serializeItemList(List<ItemStack> items) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("items", items);
        return config.saveToString();
    }

    public static List<ItemStack> deserializeItemList(String data) {
        List<ItemStack> items = new ArrayList<>();
        if (data == null || data.isEmpty()) return items;
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(data);
            List<?> list = config.getList("items");
            if (list != null) {
                for (Object obj : list) {
                    if (obj instanceof ItemStack) {
                        items.add((ItemStack) obj);
                    } else {
                        items.add(null);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }

    private void initDatabase() {
        synchronized (dbLock) {
            try (Connection conn = getConnection()) {
                if (conn == null) return;
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("PRAGMA journal_mode=WAL;");
                    stmt.execute("PRAGMA busy_timeout=5000;");
                    stmt.execute("CREATE TABLE IF NOT EXISTS player_stats (" +
                         "uuid TEXT PRIMARY KEY, " +
                         "lastKnownName TEXT, " +
                         "timePlayed INTEGER, " +
                         "erpies INTEGER, " +
                         "derpies INTEGER, " +
                         "keys INTEGER, " +
                         "kills INTEGER, " +
                         "deaths INTEGER, " +
                         "regularKeys INTEGER, " +
                         "crimsonKeys INTEGER, " +
                         "echoKeys INTEGER, " +
                         "endKeys INTEGER, " +
                         "amethystKeys INTEGER, " +
                         "hasErpPlus INTEGER, " +
                         "hasErpPro INTEGER, " +
                         "hasErpProMax INTEGER, " +
                         "hasVip INTEGER, " +
                         "bankErpies INTEGER, " +
                         "bankDerpies INTEGER, " +
                         "lastInterestTime INTEGER, " +
                         "chatSpamDisabled INTEGER, " +
                         "tpaDisabled INTEGER, " +
                         "voiceChatEnabled INTEGER, " +
                         "musicDisabled INTEGER, " +
                         "starterLootDisabled INTEGER, " +
                         "activeNametagsList TEXT, " +
                         "killedAdmin INTEGER, " +
                         "killedDragon INTEGER, " +
                         "manuallyUnlockedNametags TEXT, " +
                         "oresMined INTEGER, " +
                         "invisibleKills INTEGER, " +
                         "blocksPlaced INTEGER, " +
                         "starvationDeaths INTEGER, " +
                         "apocalypseZombieKills INTEGER, " +
                         "apocalypseLongestSurvival INTEGER, " +
                         "apocalypseMaxWavesSurvived INTEGER, " +
                         "password TEXT, " +
                         "foodsEaten TEXT, " +
                         "enderChest TEXT, " +
                         "bankItems TEXT" +
                         ");");
            
            stmt.execute("CREATE TABLE IF NOT EXISTS player_homes (" +
                         "uuid TEXT, " +
                         "slot INTEGER, " +
                         "world TEXT, " +
                         "x REAL, " +
                         "y REAL, " +
                         "z REAL, " +
                         "pitch REAL, " +
                         "yaw REAL, " +
                         "name TEXT, " +
                         "PRIMARY KEY (uuid, slot)" +
                         ");");
                    stmt.execute("CREATE TABLE IF NOT EXISTS player_orders (" +
                                 "id TEXT PRIMARY KEY, " +
                                 "buyer TEXT, " +
                                 "buyerName TEXT, " +
                                 "itemName TEXT, " +
                                 "quantity INTEGER, " +
                                 "price INTEGER" +
                                 ");");
                    stmt.execute("CREATE TABLE IF NOT EXISTS player_deliveries (" +
                                 "id TEXT PRIMARY KEY, " +
                                 "uuid TEXT, " +
                                 "itemName TEXT, " +
                                 "quantity INTEGER" +
                                 ");");
            
                    getLogger().info("[Database] SQLite database initialized successfully.");
                }
            } catch (Exception e) {
                getLogger().severe("[Database] Failed to initialize SQLite database: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void loadOrdersFromDatabase() {
        synchronized (dbLock) {
            orders.clear();
            try (Connection conn = getConnection()) {
                if (conn == null) return;
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT id, buyer, buyerName, itemName, quantity, price FROM player_orders")) {
                    while (rs.next()) {
                        try {
                            UUID id = UUID.fromString(rs.getString("id"));
                            UUID buyer = UUID.fromString(rs.getString("buyer"));
                            String buyerName = rs.getString("buyerName");
                            String itemName = rs.getString("itemName");
                            int quantity = rs.getInt("quantity");
                            long price = rs.getLong("price");
                            orders.add(new OrderRequest(id, buyer, buyerName, itemName, quantity, price));
                        } catch (Exception ex) {
                            getLogger().warning("[Orders] Error reading order from DB: " + ex.getMessage());
                        }
                    }
                    getLogger().info("[Orders] Loaded " + orders.size() + " active order(s) from database.");
                }
            } catch (Exception e) {
                getLogger().severe("[Orders] Failed to load orders from database: " + e.getMessage());
            }
        }
    }

    private void saveOrderToDatabase(OrderRequest order) {
        if (order == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            synchronized (dbLock) {
                try (Connection conn = getConnection()) {
                    if (conn == null) return;
                    String sql = "REPLACE INTO player_orders (id, buyer, buyerName, itemName, quantity, price) VALUES (?, ?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, order.id.toString());
                        ps.setString(2, order.buyer.toString());
                        ps.setString(3, order.buyerName);
                        ps.setString(4, order.itemName);
                        ps.setInt(5, order.quantity);
                        ps.setLong(6, order.price);
                        ps.executeUpdate();
                    }
                } catch (Exception e) {
                    getLogger().severe("[Orders] Failed to save order to database: " + e.getMessage());
                }
            }
        });
    }

    private void updateOrderInDatabase(OrderRequest order) {
        if (order == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            synchronized (dbLock) {
                try (Connection conn = getConnection()) {
                    if (conn == null) return;
                    String sql = "UPDATE player_orders SET quantity = ?, price = ? WHERE id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setInt(1, order.quantity);
                        ps.setLong(2, order.price);
                        ps.setString(3, order.id.toString());
                        ps.executeUpdate();
                    }
                } catch (Exception e) {
                    getLogger().severe("[Orders] Failed to update order in database: " + e.getMessage());
                }
            }
        });
    }

    private void deleteOrderFromDatabase(UUID orderId) {
        if (orderId == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            synchronized (dbLock) {
                try (Connection conn = getConnection()) {
                    if (conn == null) return;
                    String sql = "DELETE FROM player_orders WHERE id = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, orderId.toString());
                        ps.executeUpdate();
                    }
                } catch (Exception e) {
                    getLogger().severe("[Orders] Failed to delete order from database: " + e.getMessage());
                }
            }
        });
    }

    private void saveAllOrdersSync() {
        synchronized (dbLock) {
            try (Connection conn = getConnection()) {
                if (conn == null) return;
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("DELETE FROM player_orders;");
                }
                String sql = "INSERT INTO player_orders (id, buyer, buyerName, itemName, quantity, price) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    for (OrderRequest order : orders) {
                        ps.setString(1, order.id.toString());
                        ps.setString(2, order.buyer.toString());
                        ps.setString(3, order.buyerName);
                        ps.setString(4, order.itemName);
                        ps.setInt(5, order.quantity);
                        ps.setLong(6, order.price);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            } catch (Exception e) {
                getLogger().severe("[Orders] Failed to sync orders on disable: " + e.getMessage());
            }
        }
    }

    private void saveOfflineDelivery(UUID uuid, String itemName, int quantity) {
        if (uuid == null || itemName == null || quantity <= 0) return;
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            synchronized (dbLock) {
                try (Connection conn = getConnection()) {
                    if (conn == null) return;
                    String sql = "INSERT INTO player_deliveries (id, uuid, itemName, quantity) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, UUID.randomUUID().toString());
                        ps.setString(2, uuid.toString());
                        ps.setString(3, itemName);
                        ps.setInt(4, quantity);
                        ps.executeUpdate();
                    }
                } catch (Exception e) {
                    getLogger().severe("[Orders] Failed to save offline delivery: " + e.getMessage());
                }
            }
        });
    }

    private void deliverOfflineItems(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            synchronized (dbLock) {
                try (Connection conn = getConnection()) {
                    if (conn == null) return;
                    List<String[]> deliveries = new ArrayList<>();
                    List<String> idsToDelete = new ArrayList<>();
                    String sql = "SELECT id, itemName, quantity FROM player_deliveries WHERE uuid = ?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, uuid.toString());
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                idsToDelete.add(rs.getString("id"));
                                deliveries.add(new String[]{rs.getString("itemName"), String.valueOf(rs.getInt("quantity"))});
                            }
                        }
                    }
                    if (!deliveries.isEmpty()) {
                        String delSql = "DELETE FROM player_deliveries WHERE id = ?";
                        try (PreparedStatement ps = conn.prepareStatement(delSql)) {
                            for (String id : idsToDelete) {
                                ps.setString(1, id);
                                ps.addBatch();
                            }
                            ps.executeBatch();
                        }
                        Bukkit.getScheduler().runTask(this, () -> {
                            if (!player.isOnline()) return;
                            for (String[] entry : deliveries) {
                                Material mat = Material.matchMaterial(entry[0]);
                                if (mat == null) mat = Material.PAPER;
                                int qty = Integer.parseInt(entry[1]);
                                int remaining = qty;
                                int maxStack = mat.getMaxStackSize();
                                while (remaining > 0) {
                                    int stackQty = Math.min(remaining, maxStack);
                                    HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(new ItemStack(mat, stackQty));
                                    for (ItemStack drop : overflow.values()) {
                                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                                    }
                                    remaining -= stackQty;
                                }
                                player.sendMessage(Component.text("📦 §a[Orders] You received §e" + String.format("%,d", qty) + "x " + formatItemDisplayName(mat) + " §afrom your fulfilled buy order while you were away!", NamedTextColor.GREEN));
                            }
                            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                        });
                    }
                } catch (Exception e) {
                    getLogger().severe("[Orders] Failed to load offline deliveries: " + e.getMessage());
                }
            }
        });
    }

    private void migrateYamlToDatabase() {
        if (!getConfig().contains("players")) {
            return;
        }
        org.bukkit.configuration.ConfigurationSection section = getConfig().getConfigurationSection("players");
        if (section == null) return;
        
        getLogger().info("[Database] Found legacy player data in config.yml. Starting auto-migration to SQLite...");
        
        int count = 0;
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            
            String insertStats = "REPLACE INTO player_stats (uuid, lastKnownName, timePlayed, erpies, derpies, keys, kills, deaths, " +
                                 "regularKeys, crimsonKeys, echoKeys, endKeys, amethystKeys, hasErpPlus, hasErpPro, hasErpProMax, hasVip, " +
                                 "bankErpies, bankDerpies, lastInterestTime, chatSpamDisabled, tpaDisabled, voiceChatEnabled, musicDisabled, " +
                                 "starterLootDisabled, activeNametagsList, killedAdmin, killedDragon, manuallyUnlockedNametags, " +
                                 "oresMined, invisibleKills, blocksPlaced, starvationDeaths, apocalypseZombieKills, " +
                                 "apocalypseLongestSurvival, apocalypseMaxWavesSurvived, password, foodsEaten, enderChest, bankItems) " +
                                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
            
            String insertHome = "REPLACE INTO player_homes (uuid, slot, world, x, y, z, pitch, yaw, name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
            
            try (PreparedStatement psStats = conn.prepareStatement(insertStats);
                 PreparedStatement psHome = conn.prepareStatement(insertHome)) {
                
                for (String uuidStr : section.getKeys(false)) {
                    try {
                        String path = "players." + uuidStr + ".";
                        
                        psStats.setString(1, uuidStr);
                        psStats.setString(2, getConfig().getString(path + "lastKnownName"));
                        
                        int seconds = getConfig().getInt(path + "timePlayed", -1);
                        if (seconds == -1) {
                            seconds = getConfig().getInt(path + "hoursPlayed", 0) * 3600;
                        }
                        psStats.setInt(3, seconds);
                        psStats.setLong(4, getConfig().getLong(path + "erpies", 0L));
                        psStats.setLong(5, getConfig().getLong(path + "derpies", 0L));
                        psStats.setInt(6, getConfig().getInt(path + "keys", 0));
                        psStats.setInt(7, getConfig().getInt(path + "kills", 0));
                        psStats.setInt(8, getConfig().getInt(path + "deaths", 0));
                        
                        psStats.setInt(9, getConfig().getInt(path + "regularKeys", 0));
                        psStats.setInt(10, getConfig().getInt(path + "crimsonKeys", 0));
                        psStats.setInt(11, getConfig().getInt(path + "echoKeys", 0));
                        psStats.setInt(12, getConfig().getInt(path + "endKeys", 0));
                        psStats.setInt(13, getConfig().getInt(path + "amethystKeys", 0));
                        psStats.setInt(14, getConfig().getBoolean(path + "hasErpPlus", false) ? 1 : 0);
                        psStats.setInt(15, getConfig().getBoolean(path + "hasErpPro", false) ? 1 : 0);
                        psStats.setInt(16, getConfig().getBoolean(path + "hasErpProMax", false) ? 1 : 0);
                        psStats.setInt(17, getConfig().getBoolean(path + "hasVip", false) ? 1 : 0);
                        
                        psStats.setLong(18, getConfig().getLong(path + "bankErpies", 0L));
                        psStats.setLong(19, getConfig().getLong(path + "bankDerpies", 0L));
                        psStats.setLong(20, getConfig().getLong(path + "lastInterestTime", 0L));
                        
                        psStats.setInt(21, getConfig().getBoolean(path + "chatSpamDisabled", false) ? 1 : 0);
                        psStats.setInt(22, getConfig().getBoolean(path + "tpaDisabled", false) ? 1 : 0);
                        psStats.setInt(23, getConfig().getBoolean(path + "voiceChatEnabled", false) ? 1 : 0);
                        psStats.setInt(24, getConfig().getBoolean(path + "musicDisabled", false) ? 1 : 0);
                        psStats.setInt(25, getConfig().getBoolean(path + "starterLootDisabled", false) ? 1 : 0);
                        
                        List<String> activeList = getConfig().getStringList(path + "activeNametagsList");
                        if (activeList.isEmpty() && getConfig().contains(path + "activeNametag")) {
                            String legacy = getConfig().getString(path + "activeNametag", "");
                            if (!legacy.isEmpty()) activeList.add(legacy);
                        }
                        psStats.setString(26, String.join(",", activeList));
                        
                        psStats.setInt(27, getConfig().getBoolean(path + "killedAdmin", false) ? 1 : 0);
                        psStats.setInt(28, getConfig().getBoolean(path + "killedDragon", false) ? 1 : 0);
                        
                        List<String> unlockedList = getConfig().getStringList(path + "manuallyUnlockedNametags");
                        psStats.setString(29, String.join(",", unlockedList));
                        
                        psStats.setInt(30, getConfig().getInt(path + "oresMined", 0));
                        psStats.setInt(31, getConfig().getInt(path + "invisibleKills", 0));
                        psStats.setInt(32, getConfig().getInt(path + "blocksPlaced", 0));
                        psStats.setInt(33, getConfig().getInt(path + "starvationDeaths", 0));
                        psStats.setInt(34, getConfig().getInt(path + "apocalypseZombieKills", 0));
                        psStats.setLong(35, getConfig().getLong(path + "apocalypseLongestSurvival", 0L));
                        psStats.setInt(36, getConfig().getInt(path + "apocalypseMaxWavesSurvived", 0));
                        psStats.setString(37, getConfig().getString(path + "password", ""));
                        
                        YamlConfiguration foodSection = new YamlConfiguration();
                        if (getConfig().contains(path + "foodsEaten")) {
                            org.bukkit.configuration.ConfigurationSection foods = getConfig().getConfigurationSection(path + "foodsEaten");
                            if (foods != null) {
                                for (String foodKey : foods.getKeys(false)) {
                                    foodSection.set(foodKey, foods.getInt(foodKey));
                                }
                            }
                        }
                        psStats.setString(38, foodSection.saveToString());
                        
                        List<ItemStack> enderItems = new ArrayList<>();
                        if (getConfig().contains(path + "enderChest")) {
                            org.bukkit.configuration.ConfigurationSection ecSec = getConfig().getConfigurationSection(path + "enderChest");
                            if (ecSec != null) {
                                for (int i = 0; i < 54; i++) {
                                    if (ecSec.contains(String.valueOf(i))) {
                                        enderItems.add(ecSec.getItemStack(String.valueOf(i)));
                                    } else {
                                        enderItems.add(null);
                                    }
                                }
                            }
                        }
                        psStats.setString(39, serializeItemList(enderItems));
                        
                        List<ItemStack> bankItems = new ArrayList<>();
                        if (getConfig().contains(path + "bankItems")) {
                            List<?> list = getConfig().getList(path + "bankItems");
                            if (list != null) {
                                for (Object obj : list) {
                                    if (obj instanceof ItemStack) bankItems.add((ItemStack) obj);
                                }
                            }
                        }
                        psStats.setString(40, serializeItemList(bankItems));
                        psStats.executeUpdate();
                        
                        for (int i = 0; i < 54; i++) {
                            String homePath = path + "homes." + i;
                            if (getConfig().contains(homePath + ".world")) {
                                psHome.setString(1, uuidStr);
                                psHome.setInt(2, i);
                                psHome.setString(3, getConfig().getString(homePath + ".world"));
                                psHome.setDouble(4, getConfig().getDouble(homePath + ".x"));
                                psHome.setDouble(5, getConfig().getDouble(homePath + ".y"));
                                psHome.setDouble(6, getConfig().getDouble(homePath + ".z"));
                                psHome.setFloat(7, (float) getConfig().getDouble(homePath + ".pitch"));
                                psHome.setFloat(8, (float) getConfig().getDouble(homePath + ".yaw"));
                                psHome.setString(9, getConfig().getString(homePath + ".name"));
                                psHome.executeUpdate();
                            }
                        }
                        count++;
                    } catch (Exception ex) {
                        getLogger().severe("Error migrating player: " + uuidStr);
                        ex.printStackTrace();
                    }
                }
                conn.commit();
                
                getConfig().set("players", null);
                saveConfig();
                
                getLogger().info("[Database] Successfully migrated " + count + " players to SQLite. config.yml cleaned up.");
            } catch (Exception ex) {
                conn.rollback();
                getLogger().severe("[Database] Rollback! Error during migration: " + ex.getMessage());
                ex.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadPlayerData(Player player) {
        loadPlayerData(player.getUniqueId());
        
        UUID uuid = player.getUniqueId();
        String name = player.getName();
        if (name != null && name.startsWith(".")) {
            String cleanName = name.substring(1);
            org.bukkit.OfflinePlayer javaPlayer = Bukkit.getOfflinePlayer(cleanName);
            if (javaPlayer != null && javaPlayer.getUniqueId() != null) {
                UUID javaUuid = javaPlayer.getUniqueId();
                
                boolean hasPlus = hasErpPlusMap.getOrDefault(javaUuid, false);
                boolean hasPro = hasErpProMap.getOrDefault(javaUuid, false);
                boolean hasProMax = hasErpProMaxMap.getOrDefault(javaUuid, false);
                boolean hasVip = hasVipMap.getOrDefault(javaUuid, false);

                // If not loaded in maps, check DB directly
                if (!hasPlus && !hasPro && !hasProMax && !hasVip) {
                    try (Connection conn = getConnection();
                         PreparedStatement psJava = conn.prepareStatement("SELECT hasErpPlus, hasErpPro, hasErpProMax, hasVip FROM player_stats WHERE uuid = ?")) {
                        psJava.setString(1, javaUuid.toString());
                        try (ResultSet rsJava = psJava.executeQuery()) {
                            if (rsJava.next()) {
                                if (rsJava.getInt("hasErpPlus") == 1) hasPlus = true;
                                if (rsJava.getInt("hasErpPro") == 1) hasPro = true;
                                if (rsJava.getInt("hasErpProMax") == 1) hasProMax = true;
                                if (rsJava.getInt("hasVip") == 1) hasVip = true;
                            }
                        }
                    } catch (Exception ignored) {}
                }

                if (hasPlus) hasErpPlusMap.put(uuid, true);
                if (hasPro) hasErpProMap.put(uuid, true);
                if (hasProMax) hasErpProMaxMap.put(uuid, true);
                if (hasVip) hasVipMap.put(uuid, true);
            }
        }
    }

    private void loadPlayerData(UUID uuid) {
        try (Connection conn = getConnection();
             PreparedStatement psStats = conn.prepareStatement("SELECT * FROM player_stats WHERE uuid = ?");
             PreparedStatement psHomes = conn.prepareStatement("SELECT * FROM player_homes WHERE uuid = ?")) {
            
            psStats.setString(1, uuid.toString());
            try (ResultSet rs = psStats.executeQuery()) {
                if (rs.next()) {
                    timePlayedMap.put(uuid, rs.getInt("timePlayed"));
                    erpiesMap.put(uuid, rs.getLong("erpies"));
                    derpiesMap.put(uuid, rs.getLong("derpies"));
                    keysMap.put(uuid, rs.getInt("keys"));
                    killsMap.put(uuid, rs.getInt("kills"));
                    deathsMap.put(uuid, rs.getInt("deaths"));
                    
                    regularKeysMap.put(uuid, rs.getInt("regularKeys"));
                    crimsonKeysMap.put(uuid, rs.getInt("crimsonKeys"));
                    echoKeysMap.put(uuid, rs.getInt("echoKeys"));
                    endKeysMap.put(uuid, rs.getInt("endKeys"));
                    amethystKeysMap.put(uuid, rs.getInt("amethystKeys"));
                    
                    hasErpPlusMap.put(uuid, rs.getInt("hasErpPlus") == 1);
                    hasErpProMap.put(uuid, rs.getInt("hasErpPro") == 1);
                    hasErpProMaxMap.put(uuid, rs.getInt("hasErpProMax") == 1);
                    hasVipMap.put(uuid, rs.getInt("hasVip") == 1);
                    
                    bankErpiesMap.put(uuid, rs.getLong("bankErpies"));
                    bankDerpiesMap.put(uuid, rs.getLong("bankDerpies"));
                    lastInterestTimeMap.put(uuid, rs.getLong("lastInterestTime"));
                    
                    chatSpamDisabled.put(uuid, rs.getInt("chatSpamDisabled") == 1);
                    tpaDisabled.put(uuid, rs.getInt("tpaDisabled") == 1);
                    voiceChatEnabled.put(uuid, rs.getInt("voiceChatEnabled") == 1);
                    musicDisabled.put(uuid, rs.getInt("musicDisabled") == 1);
                    starterLootDisabled.put(uuid, rs.getInt("starterLootDisabled") == 1);
                    
                    killedAdminMap.put(uuid, rs.getInt("killedAdmin") == 1);
                    killedDragonMap.put(uuid, rs.getInt("killedDragon") == 1);
                    
                    oresMinedMap.put(uuid, rs.getInt("oresMined"));
                    invisibleKillsMap.put(uuid, rs.getInt("invisibleKills"));
                    blocksPlacedMap.put(uuid, rs.getInt("blocksPlaced"));
                    starvationDeathsMap.put(uuid, rs.getInt("starvationDeaths"));
                    apocalypseZombieKillsMap.put(uuid, rs.getInt("apocalypseZombieKills"));
                    apocalypseLongestSurvivalTimeMap.put(uuid, rs.getLong("apocalypseLongestSurvival"));
                    apocalypseMaxWavesSurvivedMap.put(uuid, rs.getInt("apocalypseMaxWavesSurvived"));
                    playerPasswords.put(uuid, rs.getString("password"));
                    
                    String activeTagsStr = rs.getString("activeNametagsList");
                    java.util.Set<String> activeSet = new java.util.HashSet<>();
                    if (activeTagsStr != null && !activeTagsStr.isEmpty()) {
                        for (String s : activeTagsStr.split(",")) activeSet.add(s);
                    }
                    activeNametags.put(uuid, activeSet);
                    
                    String unlockedTagsStr = rs.getString("manuallyUnlockedNametags");
                    java.util.Set<String> unlockedSet = new java.util.HashSet<>();
                    if (unlockedTagsStr != null && !unlockedTagsStr.isEmpty()) {
                        for (String s : unlockedTagsStr.split(",")) unlockedSet.add(s);
                    }
                    manuallyUnlockedNametags.put(uuid, unlockedSet);
                    
                    HashMap<Material, Integer> foods = new HashMap<>();
                    String foodsStr = rs.getString("foodsEaten");
                    if (foodsStr != null && !foodsStr.isEmpty()) {
                        YamlConfiguration foodConfig = new YamlConfiguration();
                        foodConfig.loadFromString(foodsStr);
                        for (String key : foodConfig.getKeys(false)) {
                            try {
                                foods.put(Material.valueOf(key), foodConfig.getInt(key));
                            } catch (Exception ignored) {}
                        }
                    }
                    foodsEatenMap.put(uuid, foods);
                    
                    String ecStr = rs.getString("enderChest");
                    List<ItemStack> enderItems = deserializeItemList(ecStr);
                    Inventory enderInv = Bukkit.createInventory(null, 54, Component.text("Ender Chest"));
                    for (int i = 0; i < Math.min(54, enderItems.size()); i++) {
                        if (enderItems.get(i) != null) enderInv.setItem(i, enderItems.get(i));
                    }
                    customEnderChests.put(uuid, enderInv);
                    
                    String bankStr = rs.getString("bankItems");
                    bankItemsMap.put(uuid, deserializeItemList(bankStr));
                } else {
                    timePlayedMap.put(uuid, 0);
                    erpiesMap.put(uuid, 0L);
                    derpiesMap.put(uuid, 0L);
                    keysMap.put(uuid, 0);
                    killsMap.put(uuid, 0);
                    deathsMap.put(uuid, 0);
                    regularKeysMap.put(uuid, 0);
                    crimsonKeysMap.put(uuid, 0);
                    echoKeysMap.put(uuid, 0);
                    endKeysMap.put(uuid, 0);
                    amethystKeysMap.put(uuid, 0);
                    hasErpPlusMap.put(uuid, false);
                    hasErpProMap.put(uuid, false);
                    hasErpProMaxMap.put(uuid, false);
                    hasVipMap.put(uuid, false);
                    bankErpiesMap.put(uuid, 0L);
                    bankDerpiesMap.put(uuid, 0L);
                    lastInterestTimeMap.put(uuid, 0L);
                    chatSpamDisabled.put(uuid, false);
                    tpaDisabled.put(uuid, false);
                    voiceChatEnabled.put(uuid, false);
                    musicDisabled.put(uuid, false);
                    starterLootDisabled.put(uuid, false);
                    killedAdminMap.put(uuid, false);
                    killedDragonMap.put(uuid, false);
                    oresMinedMap.put(uuid, 0);
                    invisibleKillsMap.put(uuid, 0);
                    blocksPlacedMap.put(uuid, 0);
                    starvationDeathsMap.put(uuid, 0);
                    apocalypseZombieKillsMap.put(uuid, 0);
                    apocalypseLongestSurvivalTimeMap.put(uuid, 0L);
                    apocalypseMaxWavesSurvivedMap.put(uuid, 0);
                    playerPasswords.put(uuid, "");
                    activeNametags.put(uuid, new java.util.HashSet<>());
                    manuallyUnlockedNametags.put(uuid, new java.util.HashSet<>());
                    foodsEatenMap.put(uuid, new HashMap<>());
                    
                    Inventory enderInv = Bukkit.createInventory(null, 54, Component.text("Ender Chest"));
                    customEnderChests.put(uuid, enderInv);
                    bankItemsMap.put(uuid, new ArrayList<>());
                }
            }
            
            loadPlayerHomesFromDb(uuid);
        } catch (Exception e) {
            getLogger().severe("Failed to load player data for UUID: " + uuid);
            e.printStackTrace();
        }
    }

    private void loadPlayerHomesFromDb(UUID uuid) {
        synchronized (dbLock) {
            Location[] homes = new Location[54];
            String[] homeNames = new String[54];
            for (int i = 0; i < 54; i++) {
                homeNames[i] = "Home " + (i + 1);
            }
            try (Connection conn = getConnection()) {
                if (conn == null) return;
                try (PreparedStatement psHomes = conn.prepareStatement("SELECT * FROM player_homes WHERE uuid = ?")) {
                    psHomes.setString(1, uuid.toString());
                    try (ResultSet rs = psHomes.executeQuery()) {
                        while (rs.next()) {
                            int slot = rs.getInt("slot");
                            if (slot >= 0 && slot < 54) {
                                String worldName = rs.getString("world");
                                double x = rs.getDouble("x");
                                double y = rs.getDouble("y");
                                double z = rs.getDouble("z");
                                float pitch = rs.getFloat("pitch");
                                float yaw = rs.getFloat("yaw");
                                String homeName = rs.getString("name");
                                
                                World w = Bukkit.getWorld(worldName);
                                if (w != null) {
                                    homes[slot] = new Location(w, x, y, z, yaw, pitch);
                                    if (homeName != null && !homeName.trim().isEmpty()) {
                                        homeNames[slot] = homeName;
                                    }
                                }
                            }
                        }
                    }
                }
                playerHomes.put(uuid, homes);
                playerHomeNames.put(uuid, homeNames);
            } catch (Exception e) {
                getLogger().severe("Failed to load player homes for UUID: " + uuid);
                e.printStackTrace();
            }
        }
    }

    private Location[] getPlayerHomes(UUID uuid) {
        if (!playerHomes.containsKey(uuid)) {
            loadPlayerHomesFromDb(uuid);
        }
        return playerHomes.computeIfAbsent(uuid, k -> new Location[54]);
    }

    private String[] getPlayerHomeNames(UUID uuid) {
        if (!playerHomeNames.containsKey(uuid)) {
            loadPlayerHomesFromDb(uuid);
        }
        return playerHomeNames.computeIfAbsent(uuid, k -> {
            String[] names = new String[54];
            for (int i = 0; i < 54; i++) {
                names[i] = "Home " + (i + 1);
            }
            return names;
        });
    }

    private void savePlayerData(Player player) {
        savePlayerData(player.getUniqueId());
    }

    private void savePlayerData(UUID uuid) {
        OfflinePlayer offline = Bukkit.getOfflinePlayer(uuid);
        String name = offline != null ? offline.getName() : null;
        
        int timePlayed = timePlayedMap.getOrDefault(uuid, 0);
        long erpies = erpiesMap.getOrDefault(uuid, 0L);
        long derpies = derpiesMap.getOrDefault(uuid, 0L);
        int keys = keysMap.getOrDefault(uuid, 0);
        int kills = killsMap.getOrDefault(uuid, 0);
        int deaths = deathsMap.getOrDefault(uuid, 0);
        
        int regularKeys = regularKeysMap.getOrDefault(uuid, 0);
        int crimsonKeys = crimsonKeysMap.getOrDefault(uuid, 0);
        int echoKeys = echoKeysMap.getOrDefault(uuid, 0);
        int endKeys = endKeysMap.getOrDefault(uuid, 0);
        int amethystKeys = amethystKeysMap.getOrDefault(uuid, 0);
        
        boolean hasErpPlus = hasErpPlusMap.getOrDefault(uuid, false);
        boolean hasErpPro = hasErpProMap.getOrDefault(uuid, false);
        boolean hasErpProMax = hasErpProMaxMap.getOrDefault(uuid, false);
        boolean hasVip = hasVipMap.getOrDefault(uuid, false);
        
        long bankErpies = bankErpiesMap.getOrDefault(uuid, 0L);
        long bankDerpies = bankDerpiesMap.getOrDefault(uuid, 0L);
        long lastInterestTime = lastInterestTimeMap.getOrDefault(uuid, 0L);
        
        boolean chatSpam = chatSpamDisabled.getOrDefault(uuid, false);
        boolean tpa = tpaDisabled.getOrDefault(uuid, false);
        boolean voice = voiceChatEnabled.getOrDefault(uuid, false);
        boolean music = musicDisabled.getOrDefault(uuid, false);
        boolean starter = starterLootDisabled.getOrDefault(uuid, false);
        
        boolean admin = killedAdminMap.getOrDefault(uuid, false);
        boolean dragon = killedDragonMap.getOrDefault(uuid, false);
        
        int ores = oresMinedMap.getOrDefault(uuid, 0);
        int invKills = invisibleKillsMap.getOrDefault(uuid, 0);
        int blocks = blocksPlacedMap.getOrDefault(uuid, 0);
        int starvation = starvationDeathsMap.getOrDefault(uuid, 0);
        int apocZombies = apocalypseZombieKillsMap.getOrDefault(uuid, 0);
        long apocLongest = apocalypseLongestSurvivalTimeMap.getOrDefault(uuid, 0L);
        int apocMaxWaves = apocalypseMaxWavesSurvivedMap.getOrDefault(uuid, 0);
        String pass = playerPasswords.getOrDefault(uuid, "");
        
        List<String> activeList = activeNametags.containsKey(uuid) ? new ArrayList<>(activeNametags.get(uuid)) : new ArrayList<>();
        List<String> unlockedList = manuallyUnlockedNametags.containsKey(uuid) ? new ArrayList<>(manuallyUnlockedNametags.get(uuid)) : new ArrayList<>();
        
        HashMap<Material, Integer> foodsEaten = foodsEatenMap.containsKey(uuid) ? new HashMap<>(foodsEatenMap.get(uuid)) : new HashMap<>();
        
        YamlConfiguration foodConfig = new YamlConfiguration();
        for (var entry : foodsEaten.entrySet()) {
            foodConfig.set(entry.getKey().name(), entry.getValue());
        }
        String foodsEatenStr = foodConfig.saveToString();
        
        List<ItemStack> enderItems = new ArrayList<>();
        Inventory enderInv = customEnderChests.get(uuid);
        if (enderInv != null) {
            for (int i = 0; i < 54; i++) {
                ItemStack item = enderInv.getItem(i);
                enderItems.add(item != null ? item.clone() : null);
            }
        }
        String enderChestStr = serializeItemList(enderItems);
        
        List<ItemStack> bankItems = new ArrayList<>();
        List<ItemStack> rawBankItems = bankItemsMap.get(uuid);
        if (rawBankItems != null) {
            for (ItemStack item : rawBankItems) {
                bankItems.add(item != null ? item.clone() : null);
            }
        }
        String bankItemsStr = serializeItemList(bankItems);
        
        final boolean hasHomesLoaded = playerHomes.containsKey(uuid);
        Location[] homes = playerHomes.get(uuid);
        String[] homeNames = playerHomeNames.get(uuid);
        final Location[] homesCopy = (hasHomesLoaded && homes != null) ? homes.clone() : null;
        final String[] homeNamesCopy = (hasHomesLoaded && homeNames != null) ? homeNames.clone() : null;
        
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            synchronized (dbLock) {
                String insertStats = "REPLACE INTO player_stats (uuid, lastKnownName, timePlayed, erpies, derpies, keys, kills, deaths, " +
                                     "regularKeys, crimsonKeys, echoKeys, endKeys, amethystKeys, hasErpPlus, hasErpPro, hasErpProMax, hasVip, " +
                                     "bankErpies, bankDerpies, lastInterestTime, chatSpamDisabled, tpaDisabled, voiceChatEnabled, musicDisabled, " +
                                     "starterLootDisabled, activeNametagsList, killedAdmin, killedDragon, manuallyUnlockedNametags, " +
                                     "oresMined, invisibleKills, blocksPlaced, starvationDeaths, apocalypseZombieKills, " +
                                     "apocalypseLongestSurvival, apocalypseMaxWavesSurvived, password, foodsEaten, enderChest, bankItems) " +
                                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
                
                try (Connection conn = getConnection()) {
                    if (conn == null) return;
                    conn.setAutoCommit(false);
                    try {
                        try (PreparedStatement psStats = conn.prepareStatement(insertStats)) {
                            psStats.setString(1, uuid.toString());
                            psStats.setString(2, name);
                            psStats.setInt(3, timePlayed);
                            psStats.setLong(4, erpies);
                            psStats.setLong(5, derpies);
                            psStats.setInt(6, keys);
                            psStats.setInt(7, kills);
                            psStats.setInt(8, deaths);
                            
                            psStats.setInt(9, regularKeys);
                            psStats.setInt(10, crimsonKeys);
                            psStats.setInt(11, echoKeys);
                            psStats.setInt(12, endKeys);
                            psStats.setInt(13, amethystKeys);
                            psStats.setInt(14, hasErpPlus ? 1 : 0);
                            psStats.setInt(15, hasErpPro ? 1 : 0);
                            psStats.setInt(16, hasErpProMax ? 1 : 0);
                            psStats.setInt(17, hasVip ? 1 : 0);
                            
                            psStats.setLong(18, bankErpies);
                            psStats.setLong(19, bankDerpies);
                            psStats.setLong(20, lastInterestTime);
                            
                            psStats.setInt(21, chatSpam ? 1 : 0);
                            psStats.setInt(22, tpa ? 1 : 0);
                            psStats.setInt(23, voice ? 1 : 0);
                            psStats.setInt(24, music ? 1 : 0);
                            psStats.setInt(25, starter ? 1 : 0);
                            
                            psStats.setString(26, String.join(",", activeList));
                            
                            psStats.setInt(27, admin ? 1 : 0);
                            psStats.setInt(28, dragon ? 1 : 0);
                            
                            psStats.setString(29, String.join(",", unlockedList));
                            
                            psStats.setInt(30, ores);
                            psStats.setInt(31, invKills);
                            psStats.setInt(32, blocks);
                            psStats.setInt(33, starvation);
                            psStats.setInt(34, apocZombies);
                            psStats.setLong(35, apocLongest);
                            psStats.setInt(36, apocMaxWaves);
                            psStats.setString(37, pass);
                            psStats.setString(38, foodsEatenStr);
                            psStats.setString(39, enderChestStr);
                            psStats.setString(40, bankItemsStr);
                            psStats.executeUpdate();
                        }
                        
                        if (hasHomesLoaded && homesCopy != null) {
                            try (PreparedStatement psHomeDelete = conn.prepareStatement("DELETE FROM player_homes WHERE uuid = ?");
                                 PreparedStatement psHomeInsert = conn.prepareStatement("REPLACE INTO player_homes (uuid, slot, world, x, y, z, pitch, yaw, name) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);")) {
                                psHomeDelete.setString(1, uuid.toString());
                                psHomeDelete.executeUpdate();
                                
                                for (int i = 0; i < 54; i++) {
                                    if (homesCopy[i] != null && homesCopy[i].getWorld() != null) {
                                        psHomeInsert.setString(1, uuid.toString());
                                        psHomeInsert.setInt(2, i);
                                        psHomeInsert.setString(3, homesCopy[i].getWorld().getName());
                                        psHomeInsert.setDouble(4, homesCopy[i].getX());
                                        psHomeInsert.setDouble(5, homesCopy[i].getY());
                                        psHomeInsert.setDouble(6, homesCopy[i].getZ());
                                        psHomeInsert.setFloat(7, homesCopy[i].getPitch());
                                        psHomeInsert.setFloat(8, homesCopy[i].getYaw());
                                        psHomeInsert.setString(9, (homeNamesCopy != null && homeNamesCopy[i] != null) ? homeNamesCopy[i] : null);
                                        psHomeInsert.executeUpdate();
                                    }
                                }
                            }
                        }
                        conn.commit();
                    } catch (Exception ex) {
                        conn.rollback();
                        throw ex;
                    } finally {
                        conn.setAutoCommit(true);
                    }
                } catch (Exception e) {
                    getLogger().severe("Failed to save player data asynchronously for UUID: " + uuid);
                    e.printStackTrace();
                }
            }
        });
    }

    private void unloadPlayerData(UUID uuid) {
        Player online = Bukkit.getPlayer(uuid);
        if (online != null && online.isOnline()) {
            return;
        }
        timePlayedMap.remove(uuid);
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
        hasVipMap.remove(uuid);
        bankErpiesMap.remove(uuid);
        bankDerpiesMap.remove(uuid);
        lastInterestTimeMap.remove(uuid);
        bankItemsMap.remove(uuid);
        chatSpamDisabled.remove(uuid);
        tpaDisabled.remove(uuid);
        voiceChatEnabled.remove(uuid);
        musicDisabled.remove(uuid);
        starterLootDisabled.remove(uuid);
        activeNametags.remove(uuid);
        killedAdminMap.remove(uuid);
        killedDragonMap.remove(uuid);
        manuallyUnlockedNametags.remove(uuid);
        oresMinedMap.remove(uuid);
        invisibleKillsMap.remove(uuid);
        blocksPlacedMap.remove(uuid);
        starvationDeathsMap.remove(uuid);
        apocalypseZombieKillsMap.remove(uuid);
        apocalypseLongestSurvivalTimeMap.remove(uuid);
        apocalypseMaxWavesSurvivedMap.remove(uuid);
        playerPasswords.remove(uuid);
        foodsEatenMap.remove(uuid);
        playerHomes.remove(uuid);
        playerHomeNames.remove(uuid);
        customEnderChests.remove(uuid);
    }

    private boolean isBedrockPlayer(Player player) {
        if (player == null) return false;
        String name = player.getName();
        if (name.startsWith(".") || name.startsWith("*") || name.startsWith("_") || name.toLowerCase().startsWith("bedrock")) return true;
        UUID uuid = player.getUniqueId();
        if (uuid.toString().startsWith("00000000-0000-0000-") || uuid.getMostSignificantBits() == 0L) return true;
        
        // Floodgate API check via reflection
        try {
            Class<?> floodgateApiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object floodgateApiInstance = floodgateApiClass.getMethod("getInstance").invoke(null);
            Object isFloodgatePlayer = floodgateApiClass.getMethod("isFloodgatePlayer", UUID.class).invoke(floodgateApiInstance, uuid);
            if (isFloodgatePlayer instanceof Boolean && (Boolean) isFloodgatePlayer) {
                return true;
            }
        } catch (Exception ignored) {}
        
        // Geyser API check via reflection
        try {
            Class<?> geyserApiClass = Class.forName("org.geysermc.geyser.api.GeyserApi");
            Object geyserApiInstance = geyserApiClass.getMethod("api").invoke(null);
            Object isBedrockGeyser = geyserApiClass.getMethod("isBedrockPlayer", UUID.class).invoke(geyserApiInstance, uuid);
            if (isBedrockGeyser instanceof Boolean && (Boolean) isBedrockGeyser) {
                return true;
            }
        } catch (Exception ignored) {}

        return false;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        broadcastLiveEvent("join", "fa-user-plus", "<span class=\"highlight-name\">" + player.getName() + "</span> joined ErpSMP — Welcome! 🎉");
        loadPlayerData(player);
        updateScoreboard(player);
        updatePlayerFloatingTags(player);
        deliverOfflineItems(player);
        if (!player.hasPlayedBefore()) {
            giveStarterGear(player);
            player.teleport(getRandomSpawnPoint());
        }

        if (isBedrockPlayer(player)) {
            loggedInPlayers.add(player.getUniqueId());
            try {
                org.bukkit.permissions.PermissionAttachment attachment = player.addAttachment(this);
                attachment.setPermission("vulcan.bypass", true);
                attachment.setPermission("matrix.bypass", true);
                attachment.setPermission("grim.bypass", true);
                attachment.setPermission("grimac.bypass", true);
                attachment.setPermission("spartan.bypass", true);
                attachment.setPermission("nocheatplus.bypass", true);
                attachment.setPermission("nocheatplus.shortcut.safeactive", true);
            } catch (Exception ignored) {}
        } else {
            player.sendMessage(Component.text(""));
            player.sendMessage(Component.text("🔒 To start playing do ", NamedTextColor.YELLOW)
                .append(Component.text("/register", NamedTextColor.GREEN, net.kyori.adventure.text.format.TextDecoration.BOLD))
                .append(Component.text(" to make a password or ", NamedTextColor.YELLOW))
                .append(Component.text("/login", NamedTextColor.GREEN, net.kyori.adventure.text.format.TextDecoration.BOLD))
                .append(Component.text(" if you already have one", NamedTextColor.YELLOW)));
            player.sendMessage(Component.text(""));
            if (!loggedInPlayers.contains(player.getUniqueId())) {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, Integer.MAX_VALUE, 1, false, false));
                
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (player.isOnline() && !loggedInPlayers.contains(player.getUniqueId())) {
                        String pwd = playerPasswords.getOrDefault(player.getUniqueId(), "");
                        if (pwd.isEmpty()) {
                            player.sendMessage(Component.text("🔒 Please register using /register <password> <confirmPassword>", NamedTextColor.RED));
                        } else {
                            player.sendMessage(Component.text("🔒 Please log in using /login <password>", NamedTextColor.RED));
                        }
                    }
                }, 10L);
            }
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
                updatePlayerFloatingTags(player);
                String worldName = player.getWorld().getName();
                if (worldName.equalsIgnoreCase("spawn") || worldName.equalsIgnoreCase("afk") || worldName.equalsIgnoreCase("afk_zone")) {
                    playLobbyMusic(player);
                }
            }
        }, 10L);
    }

    private boolean isStarterLoot(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        NamespacedKey key = new NamespacedKey(this, "starter_loot");
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    private void markAsStarterLoot(ItemStack item) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            NamespacedKey key = new NamespacedKey(this, "starter_loot");
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            
            List<Component> lore = meta.lore();
            if (lore == null) lore = new ArrayList<>();
            lore.add(Component.text("⚠️ Starter Loot (Cannot be sold)", NamedTextColor.RED));
            meta.lore(lore);
            
            item.setItemMeta(meta);
        }
    }

    private void giveStarterGear(Player player) {
        if (starterLootDisabled.getOrDefault(player.getUniqueId(), false)) {
            return;
        }
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        
        ItemStack helmet = new ItemStack(Material.CHAINMAIL_HELMET);
        ItemStack chest = new ItemStack(Material.CHAINMAIL_CHESTPLATE);
        ItemStack leggings = new ItemStack(Material.CHAINMAIL_LEGGINGS);
        ItemStack boots = new ItemStack(Material.CHAINMAIL_BOOTS);
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        ItemStack bread = new ItemStack(Material.BREAD, 20);
        
        markAsStarterLoot(helmet);
        markAsStarterLoot(chest);
        markAsStarterLoot(leggings);
        markAsStarterLoot(boots);
        markAsStarterLoot(sword);
        markAsStarterLoot(bread);
        
        inv.setHelmet(helmet);
        inv.setChestplate(chest);
        inv.setLeggings(leggings);
        inv.setBoots(boots);
        inv.addItem(sword);
        inv.addItem(bread);
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
        UUID uuid = player.getUniqueId();
        
        // Remove old tag displays immediately before teleport occurs to prevent client ghost entities
        List<org.bukkit.entity.TextDisplay> old = playerTagDisplays.remove(uuid);
        if (old != null) {
            for (org.bukkit.entity.TextDisplay td : old) {
                if (td.isValid()) td.remove();
            }
        }

        if (event.getCause() == org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.END_GATEWAY) {
            Location from = event.getFrom();
            if (from.getWorld() != null) {
                Block b = from.getBlock();
                if (isEchoPortalGateway(b)) {
                    event.setCancelled(true);
                    if (player.getWorld().getName().equals("echo_valley")) {
                        teleportFromEchoValley(player);
                    } else {
                        teleportToEchoValley(player);
                    }
                    return;
                }
            }
        }
        if (event.getFrom().getWorld() != null && event.getTo().getWorld() != null) {
            String fromWorld = event.getFrom().getWorld().getName();
            String toWorld = event.getTo().getWorld().getName();
            boolean fromLobby = fromWorld.equalsIgnoreCase("spawn") || fromWorld.equalsIgnoreCase("afk") || fromWorld.equalsIgnoreCase("afk_zone");
            boolean toLobby = toWorld.equalsIgnoreCase("spawn") || toWorld.equalsIgnoreCase("afk") || toWorld.equalsIgnoreCase("afk_zone");
            if (fromLobby && !toLobby) {
                stopLobbyMusic(player);
            }
        }
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                updatePlayerFloatingTags(player);
            }
        }, 5L);
    }

    @EventHandler
    public void onPlayerChangedWorld(org.bukkit.event.player.PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        
        // Remove old tag displays immediately
        List<org.bukkit.entity.TextDisplay> old = playerTagDisplays.remove(uuid);
        if (old != null) {
            for (org.bukkit.entity.TextDisplay td : old) {
                if (td.isValid()) td.remove();
            }
        }

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
        updatePlayerFloatingTags(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        broadcastLiveEvent("join", "fa-door-open", "<span class=\"highlight-name\">" + player.getName() + "</span> left the game.");
        UUID uuid = player.getUniqueId();

        if (player.getWorld().getName().equalsIgnoreCase("apocalypse")) {
            endApocalypseRun(player);
        }

        // Clean up spawned displays
        List<org.bukkit.entity.TextDisplay> displays = playerTagDisplays.remove(uuid);
        if (displays != null) {
            for (org.bukkit.entity.TextDisplay td : displays) {
                if (td.isValid()) td.remove();
            }
        }

        hiddenEntitiesMap.remove(uuid);
        combatTagTicks.remove(uuid);
        duelQueue.remove(uuid);
        org.bukkit.scheduler.BukkitTask warpTask = pendingWarpTeleports.remove(uuid);
        if (warpTask != null) warpTask.cancel();
        pendingWarpStartLocations.remove(uuid);

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
        hasVipMap.remove(uuid);

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
        playerPasswords.remove(uuid);
        loggedInPlayers.remove(uuid);

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
            activeFloatingTextIsLeaderboard.remove(uuid);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerGameModeChange(org.bukkit.event.player.PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTask(this, () -> {
            if (player.isOnline()) {
                updatePlayerFloatingTags(player);
                for (Player online : Bukkit.getOnlinePlayers()) {
                    updateScoreboard(online);
                }
            }
        });
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPotionEffect(org.bukkit.event.entity.EntityPotionEffectEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getModifiedType().equals(PotionEffectType.INVISIBILITY)) {
                Bukkit.getScheduler().runTask(this, () -> {
                    if (player.isOnline()) {
                        updatePlayerFloatingTags(player);
                        for (Player online : Bukkit.getOnlinePlayers()) {
                            updateScoreboard(online);
                        }
                    }
                });
            }
        }
    }

    @EventHandler
    public void onPlayerKill(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        net.kyori.adventure.text.Component deathMessageComp = event.deathMessage();
        String deathMsg = "";
        if (deathMessageComp != null) {
            deathMsg = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(deathMessageComp);
        } else {
            deathMsg = victim.getName() + " died.";
        }
        String formattedMsg = deathMsg.replace(victim.getName(), "<span class=\"highlight-name\">" + victim.getName() + "</span>");
        Player eventKiller = victim.getKiller();
        if (eventKiller != null) {
            formattedMsg = formattedMsg.replace(eventKiller.getName(), "<span class=\"highlight-name\">" + eventKiller.getName() + "</span>");
        }
        broadcastLiveEvent("kill", "fa-skull", formattedMsg);

        UUID victimUUID = victim.getUniqueId();
        deathsMap.put(victimUUID, deathsMap.getOrDefault(victimUUID, 0) + 1);

        // Remove floating nametag displays on death
        List<org.bukkit.entity.TextDisplay> victimOldTags = playerTagDisplays.remove(victimUUID);
        if (victimOldTags != null) {
            for (org.bukkit.entity.TextDisplay td : victimOldTags) {
                if (td.isValid()) td.remove();
            }
        }

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

            if (victim.isOp() || isRedToppat(victim.getUniqueId()) || victim.getUniqueId().equals(BOREAS_UUID)) {
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

    private void broadcastLiveEvent(String cls, String icon, String text) {
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(2))
                    .build();

                java.util.Map<String, String> data = new java.util.HashMap<>();
                data.put("cls", cls);
                data.put("icon", icon);
                data.put("text", text);

                StringBuilder jsonBuilder = new StringBuilder("{");
                int index = 0;
                for (java.util.Map.Entry<String, String> entry : data.entrySet()) {
                    if (index > 0) jsonBuilder.append(",");
                    jsonBuilder.append("\"").append(entry.getKey()).append("\":\"")
                               .append(entry.getValue().replace("\"", "\\\"")).append("\"");
                    index++;
                }
                jsonBuilder.append("}");
                String json = jsonBuilder.toString();

                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/api/publish"))
                    .timeout(java.time.Duration.ofSeconds(2))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer ErpAdmin$2026!Secure")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(json))
                    .build();

                client.send(request, java.net.http.HttpResponse.BodyHandlers.discarding());
            } catch (Exception ignored) {}
        });
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("setrank")) {
            if (sender instanceof Player) {
                sender.sendMessage(Component.text("❌ This command can only be executed from the Server Console.", NamedTextColor.RED));
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("Usage: /setrank <playername> <erp+|erp++|erp+++|vip|none|reset> (erp++ matches Erp+ Pro, erp+++ matches Erp+ Pro Max)");
                return true;
            }
            String targetName = args[0];
            String rankArg = args[1].toLowerCase();
            Player target = Bukkit.getPlayer(targetName);
            UUID targetUuid;
            String targetDisplayName;
            boolean isOnline = (target != null);
            if (isOnline) {
                targetUuid = target.getUniqueId();
                targetDisplayName = target.getName();
            } else {
                org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
                if (offlineTarget == null || offlineTarget.getUniqueId() == null) {
                    sender.sendMessage("Error: Player '" + targetName + "' was never found.");
                    return true;
                }
                targetUuid = offlineTarget.getUniqueId();
                targetDisplayName = offlineTarget.getName() != null ? offlineTarget.getName() : targetName;
                
                // Load existing data for offline player to prevent overwriting other stats/homes!
                loadPlayerData(targetUuid);
            }
            
            int currentWeight = getRankWeight(targetUuid);
            int newWeight = getRankWeightByName(rankArg);
            if (!rankArg.equals("none") && !rankArg.equals("reset") && newWeight <= currentWeight) {
                sender.sendMessage("Error: " + targetDisplayName + " already has " + (currentWeight == newWeight ? "this rank" : "a higher rank") + "!");
                if (!isOnline) {
                    unloadPlayerData(targetUuid);
                }
                return true;
            }
            UUID counterpartUuid = null;
            String tName = targetDisplayName;
            if (tName != null) {
                if (tName.startsWith(".")) {
                    org.bukkit.OfflinePlayer javaPlayer = Bukkit.getOfflinePlayer(tName.substring(1));
                    if (javaPlayer != null) counterpartUuid = javaPlayer.getUniqueId();
                } else {
                    org.bukkit.OfflinePlayer bedrockPlayer = Bukkit.getOfflinePlayer("." + tName);
                    if (bedrockPlayer != null) counterpartUuid = bedrockPlayer.getUniqueId();
                }
            }

            hasErpPlusMap.put(targetUuid, false);
            hasErpProMap.put(targetUuid, false);
            hasErpProMaxMap.put(targetUuid, false);
            hasVipMap.put(targetUuid, false);
            if (counterpartUuid != null) {
                hasErpPlusMap.put(counterpartUuid, false);
                hasErpProMap.put(counterpartUuid, false);
                hasErpProMaxMap.put(counterpartUuid, false);
                hasVipMap.put(counterpartUuid, false);
            }

            String rankLabel = "None";
            switch (rankArg) {
                case "e+":
                case "erp+":
                    hasErpPlusMap.put(targetUuid, true);
                    if (counterpartUuid != null) hasErpPlusMap.put(counterpartUuid, true);
                    rankLabel = "Erp+";
                    break;
                case "e+p":
                case "erp++":
                    hasErpProMap.put(targetUuid, true);
                    if (counterpartUuid != null) hasErpProMap.put(counterpartUuid, true);
                    rankLabel = "Erp+ Pro";
                    break;
                case "e+pm":
                case "erp+++":
                    hasErpProMaxMap.put(targetUuid, true);
                    if (counterpartUuid != null) hasErpProMaxMap.put(counterpartUuid, true);
                    rankLabel = "Erp+ Pro Max";
                    break;
                case "vip":
                    hasVipMap.put(targetUuid, true);
                    if (counterpartUuid != null) hasVipMap.put(counterpartUuid, true);
                    rankLabel = "VIP";
                    break;
                case "none":
                case "reset":
                    break;
                default:
                    sender.sendMessage("Error: Invalid rank. Use: erp+, erp++, erp+++, vip, none, or reset");
                    if (!isOnline) {
                        unloadPlayerData(targetUuid);
                    }
                    return true;
            }
            savePlayerData(targetUuid);
            if (counterpartUuid != null) {
                savePlayerData(counterpartUuid);
            }
            if (isOnline) {
                updateScoreboard(target);
                sender.sendMessage("Success: Set " + targetDisplayName + "'s rank to " + rankLabel);
                target.sendMessage(Component.text("🌟 Your rank has been updated to " + rankLabel + "!", NamedTextColor.GOLD));
            } else {
                sender.sendMessage("Success: Set offline player " + targetDisplayName + "'s rank to " + rankLabel);
                unloadPlayerData(targetUuid);
            }
            return true;
        }

        final Player player;
        if (sender instanceof Player) {
            player = (Player) sender;
        } else {
            player = (Player) java.lang.reflect.Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, methodArgs) -> {
                    if (method.getName().equals("sendMessage")) {
                        if (methodArgs[0] instanceof net.kyori.adventure.text.Component) {
                            sender.sendMessage((net.kyori.adventure.text.Component) methodArgs[0]);
                        } else {
                            sender.sendMessage(String.valueOf(methodArgs[0]));
                        }
                        return null;
                    }
                    if (method.getName().equals("isOp")) {
                        return true;
                    }
                    if (method.getName().equals("getName")) {
                        return "Console";
                    }
                    if (method.getName().equals("getUniqueId")) {
                        return new java.util.UUID(0L, 0L);
                    }
                    if (method.getName().equals("hasPermission")) {
                        return true;
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType.equals(boolean.class)) return true;
                    if (returnType.equals(int.class)) return 0;
                    if (returnType.equals(long.class)) return 0L;
                    if (returnType.equals(double.class)) return 0.0;
                    if (returnType.equals(float.class)) return 0.0f;
                    return null;
                }
            );
        }

        if (command.getName().equalsIgnoreCase("setwarp")) {
            if (!isRedToppat(player.getUniqueId())) {
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
            final String displayWarpName = args[0];
            String warpName = displayWarpName.toLowerCase();
            if (!warps.containsKey(warpName)) {
                player.sendMessage(Component.text("❌ Warp '" + displayWarpName + "' does not exist!", NamedTextColor.RED));
                return true;
            }

            UUID uuid = player.getUniqueId();
            if (combatTagTicks.containsKey(uuid)) {
                player.sendMessage(Component.text("❌ You cannot warp while in combat!", NamedTextColor.RED));
                return true;
            }

            org.bukkit.scheduler.BukkitTask oldTask = pendingWarpTeleports.remove(uuid);
            if (oldTask != null) {
                oldTask.cancel();
            }

            pendingWarpStartLocations.put(uuid, player.getLocation());
            player.sendMessage(Component.text("⏳ Warp teleporting in 5 seconds... Do not move!", NamedTextColor.YELLOW));

            org.bukkit.scheduler.BukkitTask task = Bukkit.getScheduler().runTaskLater(this, () -> {
                pendingWarpTeleports.remove(uuid);
                pendingWarpStartLocations.remove(uuid);
                if (player.isOnline()) {
                    teleportationSync(player, warps.get(warpName), "✨ Teleported to warp '" + displayWarpName + "'!");
                }
            }, 100L); // 100 ticks = 5 seconds

            pendingWarpTeleports.put(uuid, task);
            return true;
        }

        if (command.getName().equalsIgnoreCase("store")) {
            player.sendMessage(Component.text("\"theerpsmp.net\" Copy this and paste in you preferred browser to open the store", NamedTextColor.YELLOW));
            return true;
        }

        if (command.getName().equalsIgnoreCase("say")) {
            if (!isRedToppat(player.getUniqueId())) {
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

        // /rank 05132014!Cc (e+/e+p/e+pm/vip) (set/reset) player
        if (command.getName().equalsIgnoreCase("rank")) {
            if (args.length < 3) {
                player.sendMessage(Component.text("❌ Usage: /rank 05132014!Cc <e+/e+p/e+pm/vip> <set/reset> <player>", NamedTextColor.RED));
                return true;
            }

            String password = args[0];
            if (!password.equals("05132014!Cc") && !password.equals("05132014")) {
                player.sendMessage(Component.text("❌ Incorrect password!", NamedTextColor.RED));
                return true;
            }

            String rankArg;
            String action;
            String targetName;

            if (args.length == 3) {
                if (args[1].equalsIgnoreCase("reset")) {
                    rankArg = "none";
                    action = "reset";
                    targetName = args[2];
                } else if (args[2].equalsIgnoreCase("reset")) {
                    rankArg = "none";
                    action = "reset";
                    targetName = args[1];
                } else {
                    player.sendMessage(Component.text("❌ Usage: /rank 05132014!Cc <e+/e+p/e+pm/vip> <set/reset> <player>", NamedTextColor.RED));
                    return true;
                }
            } else {
                rankArg = args[1].toLowerCase().trim().replace(" ", "");
                action = args[2].toLowerCase().trim();
                targetName = args[3];

                if (!action.equals("set") && !action.equals("reset")) {
                    player.sendMessage(Component.text("❌ Invalid action! Use 'set' or 'reset'.", NamedTextColor.RED));
                    return true;
                }
                if (action.equals("reset")) {
                    rankArg = "none";
                }
            }

            String normalizedRank;
            if (rankArg.equals("e+pm") || rankArg.equals("e+++") || rankArg.equals("erp+++") || rankArg.equals("erppromax") || rankArg.equals("erp+promax") || rankArg.equals("erpiepromaxx") || rankArg.equals("erppromaxx")) {
                normalizedRank = "erp+++";
            } else if (rankArg.equals("e+p") || rankArg.equals("e++") || rankArg.equals("erp++") || rankArg.equals("erppro") || rankArg.equals("erp+pro") || rankArg.equals("erpiepro")) {
                normalizedRank = "erp++";
            } else if (rankArg.equals("e+") || rankArg.equals("e") || rankArg.equals("erp+") || rankArg.equals("erpplus") || rankArg.equals("erp+plus") || rankArg.equals("erp") || rankArg.equals("erpie")) {
                normalizedRank = "erp+";
            } else if (rankArg.equals("vip")) {
                normalizedRank = "vip";
            } else if (rankArg.equals("none") || rankArg.equals("reset")) {
                normalizedRank = "reset";
            } else {
                player.sendMessage(Component.text("❌ Invalid rank! Use: e+, e+p, e+pm, or vip", NamedTextColor.RED));
                return true;
            }

            Player target = Bukkit.getPlayerExact(targetName);
            if (target == null) target = Bukkit.getPlayer(targetName);
            if (target == null) {
                String alt = targetName.startsWith(".") ? targetName.substring(1) : "." + targetName;
                target = Bukkit.getPlayer(alt);
            }

            UUID targetUuid = null;
            String targetDisplayName = targetName;
            boolean isOnline = (target != null);

            if (isOnline) {
                targetUuid = target.getUniqueId();
                targetDisplayName = target.getName();
            } else {
                synchronized (dbLock) {
                    try (Connection conn = getConnection()) {
                        if (conn != null) {
                            String alt = targetName.startsWith(".") ? targetName.substring(1) : "." + targetName;
                            String query = "SELECT uuid, lastKnownName FROM player_stats WHERE lower(lastKnownName) = lower(?) OR lower(lastKnownName) = lower(?) OR uuid = ? ORDER BY CASE WHEN lower(lastKnownName) = lower(?) THEN 0 ELSE 1 END LIMIT 1";
                            try (PreparedStatement ps = conn.prepareStatement(query)) {
                                ps.setString(1, targetName);
                                ps.setString(2, alt);
                                ps.setString(3, targetName);
                                ps.setString(4, targetName);
                                try (ResultSet rs = ps.executeQuery()) {
                                    if (rs.next()) {
                                        targetUuid = UUID.fromString(rs.getString("uuid"));
                                        if (rs.getString("lastKnownName") != null) {
                                            targetDisplayName = rs.getString("lastKnownName");
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }

                if (targetUuid == null) {
                    org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
                    if (offlineTarget == null || (!offlineTarget.hasPlayedBefore() && offlineTarget.getName() == null)) {
                        String alt = targetName.startsWith(".") ? targetName.substring(1) : "." + targetName;
                        offlineTarget = Bukkit.getOfflinePlayer(alt);
                    }
                    if (offlineTarget != null && (offlineTarget.hasPlayedBefore() || offlineTarget.getName() != null)) {
                        targetUuid = offlineTarget.getUniqueId();
                        if (offlineTarget.getName() != null) targetDisplayName = offlineTarget.getName();
                    }
                }

                if (targetUuid == null) {
                    player.sendMessage(Component.text("❌ Player '" + targetName + "' was never found.", NamedTextColor.RED));
                    return true;
                }

                loadPlayerData(targetUuid);
            }

            UUID counterpartUuid = null;
            if (targetDisplayName != null) {
                if (targetDisplayName.startsWith(".")) {
                    org.bukkit.OfflinePlayer javaPlayer = Bukkit.getOfflinePlayer(targetDisplayName.substring(1));
                    if (javaPlayer != null && (javaPlayer.hasPlayedBefore() || javaPlayer.getName() != null)) counterpartUuid = javaPlayer.getUniqueId();
                } else {
                    org.bukkit.OfflinePlayer bedrockPlayer = Bukkit.getOfflinePlayer("." + targetDisplayName);
                    if (bedrockPlayer != null && (bedrockPlayer.hasPlayedBefore() || bedrockPlayer.getName() != null)) counterpartUuid = bedrockPlayer.getUniqueId();
                }
            }

            // Clear all ranks first
            hasErpPlusMap.put(targetUuid, false);
            hasErpProMap.put(targetUuid, false);
            hasErpProMaxMap.put(targetUuid, false);
            hasVipMap.put(targetUuid, false);
            if (counterpartUuid != null) {
                hasErpPlusMap.put(counterpartUuid, false);
                hasErpProMap.put(counterpartUuid, false);
                hasErpProMaxMap.put(counterpartUuid, false);
                hasVipMap.put(counterpartUuid, false);
            }

            String rankLabel;
            switch (normalizedRank) {
                case "erp+":
                    hasErpPlusMap.put(targetUuid, true);
                    if (counterpartUuid != null) hasErpPlusMap.put(counterpartUuid, true);
                    rankLabel = "Erp+";
                    break;
                case "erp++":
                    hasErpProMap.put(targetUuid, true);
                    if (counterpartUuid != null) hasErpProMap.put(counterpartUuid, true);
                    rankLabel = "Erp+ Pro";
                    break;
                case "erp+++":
                    hasErpProMaxMap.put(targetUuid, true);
                    if (counterpartUuid != null) hasErpProMaxMap.put(counterpartUuid, true);
                    rankLabel = "Erp+ Pro Max";
                    break;
                case "vip":
                    hasVipMap.put(targetUuid, true);
                    if (counterpartUuid != null) hasVipMap.put(counterpartUuid, true);
                    rankLabel = "VIP";
                    break;
                case "reset":
                default:
                    rankLabel = "None";
                    break;
            }

            savePlayerData(targetUuid);
            if (counterpartUuid != null) {
                savePlayerData(counterpartUuid);
            }

            if (isOnline) {
                updateScoreboard(target);
                player.sendMessage(Component.text("✅ Set " + targetDisplayName + "'s rank to " + rankLabel + "!", NamedTextColor.GREEN));
                if (rankLabel.equals("None")) {
                    target.sendMessage(Component.text("ℹ️ Your rank has been reset to None.", NamedTextColor.YELLOW));
                } else {
                    target.sendMessage(Component.text("🌟 Your rank has been set to " + rankLabel + "!", NamedTextColor.GOLD));
                }
            } else {
                player.sendMessage(Component.text("✅ Set offline player " + targetDisplayName + "'s rank to " + rankLabel + "!", NamedTextColor.GREEN));
                unloadPlayerData(targetUuid);
            }
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
            updatePlayerFloatingTags(player);
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
            updatePlayerFloatingTags(player);
            
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
            
            String worldName = "duel_" + challenger.getName() + "_" + player.getName();
            
            // First, make sure the "duel" template folder exists on disk and copy it
            File templateDir = new File(Bukkit.getWorldContainer(), "duel");
            File targetDir = new File(Bukkit.getWorldContainer(), worldName);
            if (templateDir.exists()) {
                copyWorldDirectory(templateDir, targetDir);
            }
            
            World dualWorld = Bukkit.getWorld(worldName);
            if (dualWorld == null) {
                WorldCreator creator = new WorldCreator(worldName);
                creator.environment(World.Environment.NORMAL);
                dualWorld = Bukkit.createWorld(creator);
            }
            if (dualWorld != null) {
                applyDuelWorldSettings(dualWorld);

                org.bukkit.WorldBorder border = dualWorld.getWorldBorder();
                border.setCenter(0.0, 0.0);
                border.setSize(50.0);

                cleanupDuelArena(dualWorld);

                double y1 = dualWorld.getHighestBlockYAt(-15, 0);
                double y2 = dualWorld.getHighestBlockYAt(15, 0);

                Location loc1 = new Location(dualWorld, -15.5, y1 + 1.0, 0.5, -90f, 0f);
                Location loc2 = new Location(dualWorld, 15.5, y2 + 1.0, 0.5, 90f, 0f);
                teleportationSync(challenger, loc1, "⚔️ Duel started! Good luck!");
                teleportationSync(player, loc2, "⚔️ Duel started! Good luck!");
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
            if (args.length > 0) {
                if (args[0].equalsIgnoreCase("hand")) {
                    ItemStack handItem = player.getInventory().getItemInMainHand();
                    if (handItem == null || handItem.getType() == Material.AIR) {
                        player.sendMessage(Component.text("❌ You are not holding any item to sell!", NamedTextColor.RED));
                        return true;
                    }
                    if (isStarterLoot(handItem)) {
                        player.sendMessage(Component.text("❌ You cannot sell starter loot!", NamedTextColor.RED));
                        return true;
                    }
                    long total = getItemSellPrice(handItem);
                    int amount = handItem.getAmount();
                    player.getInventory().setItemInMainHand(null);
                    UUID uuid = player.getUniqueId();
                    erpiesMap.put(uuid, erpiesMap.getOrDefault(uuid, 0L) + total);
                    player.sendMessage(Component.text("💰 Sold " + amount + "x " + handItem.getType().name() + " for ", NamedTextColor.GREEN)
                            .append(Component.text(String.format("%,d", total) + " Erpies", NamedTextColor.WHITE)));
                    return true;
                } else if (args[0].equalsIgnoreCase("price") || args[0].equalsIgnoreCase("check") || args[0].equalsIgnoreCase("value")) {
                    ItemStack handItem = player.getInventory().getItemInMainHand();
                    if (handItem == null || handItem.getType() == Material.AIR) {
                        player.sendMessage(Component.text("❌ You are not holding any item!", NamedTextColor.RED));
                        return true;
                    }
                    long totalValue = getItemSellPrice(handItem);
                    if (isShulkerWithContents(handItem)) {
                        player.sendMessage(Component.text("ℹ️ " + handItem.getType().name() + " sells for ", NamedTextColor.YELLOW)
                                .append(Component.text(String.format("%,d", totalValue) + " Erpies (shulker + contents)", NamedTextColor.WHITE)));
                    } else if (handItem.getAmount() > 1) {
                        long unitPrice = getItemRarityValue(handItem.getType());
                        player.sendMessage(Component.text("ℹ️ " + handItem.getType().name() + " sells for ", NamedTextColor.YELLOW)
                                .append(Component.text(String.format("%,d", unitPrice) + " Erpies each (" + String.format("%,d", totalValue) + " total)", NamedTextColor.WHITE)));
                    } else {
                        player.sendMessage(Component.text("ℹ️ " + handItem.getType().name() + " sells for ", NamedTextColor.YELLOW)
                                .append(Component.text(String.format("%,d", totalValue) + " Erpies each", NamedTextColor.WHITE)));
                    }
                    return true;
                }
            }

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

        if (command.getName().equalsIgnoreCase("event")) {
            UUID uuid = player.getUniqueId();
            if (combatTagTicks.containsKey(uuid)) {
                player.sendMessage(Component.text("❌ You cannot teleport to the event while in combat!", NamedTextColor.RED));
                return true;
            }
            Location eventLoc = warps.get("event");
            if (eventLoc == null) {
                World mainWorld = Bukkit.getWorld("world");
                if (mainWorld == null && !Bukkit.getWorlds().isEmpty()) {
                    mainWorld = Bukkit.getWorlds().get(0);
                }
                if (mainWorld != null) {
                    eventLoc = new Location(mainWorld, 35.5, 68.0, 4.5);
                }
            }
            if (eventLoc == null) {
                player.sendMessage(Component.text("❌ Event warp location could not be determined!", NamedTextColor.RED));
                return true;
            }
            performTeleportCountdown(player, eventLoc, "Event Area");
            return true;
        }

        if (command.getName().equalsIgnoreCase("auction")) {
            String query = args.length > 0 ? String.join(" ", args) : null;
            openAuctionGui(player, query);
            return true;
        }

        if (command.getName().equalsIgnoreCase("orders")) {
            if (args.length > 0 && (args[0].equalsIgnoreCase("my") || args[0].equalsIgnoreCase("mine"))) {
                openMyOrders(player);
                return true;
            }
            String query = args.length > 0 ? String.join(" ", args) : null;
            openOrdersGui(player, query);
            return true;
        }

        if (command.getName().equalsIgnoreCase("player")) {
            String pName = player.getName();
            if (!player.isOp() && !isRedToppat(player.getUniqueId()) && !player.getUniqueId().equals(BOREAS_UUID)) {
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
                String reason = "Banned by administrator";
                Double days = null;

                if (args.length >= 3) {
                    try {
                        days = Double.parseDouble(args[2]);
                        long ms = (long) (days * 24 * 60 * 60 * 1000);
                        expiration = new java.util.Date(System.currentTimeMillis() + ms);
                        
                        if (args.length >= 4) {
                            reason = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
                        }
                    } catch (NumberFormatException e) {
                        reason = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                    }
                }

                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(targetName, reason, expiration, null);
                Player target = Bukkit.getPlayer(targetName);
                if (target != null) {
                    target.kick(Component.text("You have been banned from the server.\nReason: " + reason));
                }
                
                if (expiration != null) {
                    player.sendMessage(Component.text("✅ Banned player " + targetName + " for " + days + " days. Reason: " + reason, NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("✅ Permanently banned player " + targetName + ". Reason: " + reason, NamedTextColor.GREEN));
                }
            } else if (action.equals("unban")) {
                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).pardon(targetName);
                player.sendMessage(Component.text("✅ Unbanned player " + targetName, NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("❌ Usage: /player (ban/unban) (playername) [days] [reason]", NamedTextColor.RED));
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
            // Clear any old tpahere requests between them to prevent conflicts
            tpahereRequests.remove(player.getUniqueId());
            player.sendMessage(Component.text("📨 Teleport request sent to " + target.getName() + "!", NamedTextColor.GREEN));
            target.sendMessage(Component.text("📨 " + player.getName() + " wants to teleport to you!", NamedTextColor.YELLOW)
                    .append(Component.text("\nType ", NamedTextColor.GOLD))
                    .append(Component.text("/tpaccept", NamedTextColor.GREEN))
                    .append(Component.text(" to accept!", NamedTextColor.GOLD)));
            return true;
        }

        // --- /tpahere <player> ---
        if (command.getName().equalsIgnoreCase("tpahere")) {
            if (args.length == 0) {
                player.sendMessage(Component.text("❌ Usage: /tpahere <player>", NamedTextColor.RED));
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(Component.text("❌ Player not found!", NamedTextColor.RED));
                return true;
            }
            if (target.equals(player)) {
                player.sendMessage(Component.text("❌ You cannot teleport yourself to yourself!", NamedTextColor.RED));
                return true;
            }
            if (tpaDisabled.getOrDefault(target.getUniqueId(), false)) {
                player.sendMessage(Component.text("❌ " + target.getName() + " has disabled TPA requests!", NamedTextColor.RED));
                return true;
            }
            tpahereRequests.put(player.getUniqueId(), target.getUniqueId());
            // Clear any old tpa requests between them to prevent conflicts
            tpaRequests.remove(player.getUniqueId());
            player.sendMessage(Component.text("📨 Teleport-here request sent to " + target.getName() + "!", NamedTextColor.GREEN));
            target.sendMessage(Component.text("📨 " + player.getName() + " wants you to teleport to them!", NamedTextColor.YELLOW)
                    .append(Component.text("\nType ", NamedTextColor.GOLD))
                    .append(Component.text("/tpaccept", NamedTextColor.GREEN))
                    .append(Component.text(" to accept!", NamedTextColor.GOLD)));
            return true;
        }

        // --- /tpaccept ---
        if (command.getName().equalsIgnoreCase("tpaccept")) {
            UUID accepterUUID = player.getUniqueId();
            UUID requesterUUID = null;
            boolean isTpaHere = false;

            // Check regular TPA requests first
            for (var entry : tpaRequests.entrySet()) {
                if (entry.getValue().equals(accepterUUID)) {
                    requesterUUID = entry.getKey();
                    break;
                }
            }

            // Check TPAHere requests if no regular TPA found
            if (requesterUUID == null) {
                for (var entry : tpahereRequests.entrySet()) {
                    if (entry.getValue().equals(accepterUUID)) {
                        requesterUUID = entry.getKey();
                        isTpaHere = true;
                        break;
                    }
                }
            }

            if (requesterUUID == null) {
                player.sendMessage(Component.text("❌ You have no pending teleport requests!", NamedTextColor.RED));
                return true;
            }

            Player requester = Bukkit.getPlayer(requesterUUID);
            if (requester == null) {
                if (isTpaHere) {
                    tpahereRequests.remove(requesterUUID);
                } else {
                    tpaRequests.remove(requesterUUID);
                }
                player.sendMessage(Component.text("❌ The requester is no longer online!", NamedTextColor.RED));
                return true;
            }

            if (isTpaHere) {
                tpahereRequests.remove(requesterUUID);
                if (combatTagTicks.containsKey(accepterUUID)) {
                    player.sendMessage(Component.text("❌ You cannot teleport while in combat!", NamedTextColor.RED));
                    requester.sendMessage(Component.text("❌ " + player.getName() + " cannot teleport while in combat!", NamedTextColor.RED));
                    return true;
                }
                player.sendMessage(Component.text("✅ Teleport request accepted! You will teleport to " + requester.getName() + " in 5 seconds.", NamedTextColor.GREEN));
                requester.sendMessage(Component.text("✅ " + player.getName() + " accepted your request! Teleporting in 5 seconds...", NamedTextColor.GREEN));
                performTpaCountdown(player, requester);
            } else {
                tpaRequests.remove(requesterUUID);
                if (combatTagTicks.containsKey(requesterUUID)) {
                    player.sendMessage(Component.text("❌ " + requester.getName() + " cannot teleport while in combat!", NamedTextColor.RED));
                    requester.sendMessage(Component.text("❌ You cannot teleport while in combat!", NamedTextColor.RED));
                    return true;
                }
                player.sendMessage(Component.text("✅ Teleport request accepted! " + requester.getName() + " will arrive in 5 seconds.", NamedTextColor.GREEN));
                requester.sendMessage(Component.text("✅ " + player.getName() + " accepted your request! Teleporting in 5 seconds...", NamedTextColor.GREEN));
                performTpaCountdown(requester, player);
            }
            return true;
        }

        // --- /pay <player> <amount> (also supports /pay <amount> <player>) ---
        if (command.getName().equalsIgnoreCase("pay")) {
            if (args.length < 2) {
                player.sendMessage(Component.text("❌ Usage: /pay <player> <amount>", NamedTextColor.RED));
                return true;
            }

            String targetArg;
            String amountArg;

            // Determine argument positions: whether /pay <player> <amount> or /pay <amount> <player>
            boolean firstIsAmount = false;
            try {
                parseAmountWithSuffix(args[0]);
                firstIsAmount = true;
            } catch (NumberFormatException ignored) {}

            boolean secondIsAmount = false;
            try {
                parseAmountWithSuffix(args[1]);
                secondIsAmount = true;
            } catch (NumberFormatException ignored) {}

            if (secondIsAmount && !firstIsAmount) {
                targetArg = args[0];
                amountArg = args[1];
            } else if (firstIsAmount && !secondIsAmount) {
                amountArg = args[0];
                targetArg = args[1];
            } else if (secondIsAmount) {
                // Both can be numbers (e.g. player named '123' or numeric string)
                targetArg = args[0];
                amountArg = args[1];
            } else {
                player.sendMessage(Component.text("❌ Invalid amount format! E.g. 500, 10k, 1.5m, 1b", NamedTextColor.RED));
                return true;
            }

            long amount;
            try {
                amount = parseAmountWithSuffix(amountArg);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("❌ Invalid amount format! E.g. 500, 10k, 1.5m, 1b", NamedTextColor.RED));
                return true;
            }

            if (amount <= 0) {
                player.sendMessage(Component.text("❌ Amount must be greater than 0!", NamedTextColor.RED));
                return true;
            }

            // Locate target player (supporting Bedrock dot prefix and case-insensitivity)
            Player target = Bukkit.getPlayer(targetArg);
            if (target == null) {
                target = Bukkit.getPlayerExact(targetArg);
            }
            if (target == null && !targetArg.startsWith(".")) {
                target = Bukkit.getPlayer("." + targetArg);
            }
            if (target == null && targetArg.startsWith(".")) {
                target = Bukkit.getPlayer(targetArg.substring(1));
            }
            if (target == null) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    String pName = p.getName();
                    if (pName.equalsIgnoreCase(targetArg) ||
                        pName.replaceFirst("^\\.", "").equalsIgnoreCase(targetArg.replaceFirst("^\\.", "")) ||
                        pName.toLowerCase().startsWith(targetArg.toLowerCase()) ||
                        pName.replaceFirst("^\\.", "").toLowerCase().startsWith(targetArg.replaceFirst("^\\.", "").toLowerCase())) {
                        target = p;
                        break;
                    }
                }
            }

            UUID uuid = player.getUniqueId();
            long balance = erpiesMap.getOrDefault(uuid, 0L);
            if (balance < amount) {
                player.sendMessage(Component.text("❌ You don't have enough Erpies! Balance: " + String.format("%,d", balance) + " Erpies", NamedTextColor.RED));
                return true;
            }

            if (target != null) {
                if (target.equals(player) || target.getUniqueId().equals(uuid)) {
                    player.sendMessage(Component.text("❌ You cannot pay yourself!", NamedTextColor.RED));
                    return true;
                }

                UUID targetUUID = target.getUniqueId();
                erpiesMap.put(uuid, balance - amount);
                erpiesMap.put(targetUUID, erpiesMap.getOrDefault(targetUUID, 0L) + amount);

                player.sendMessage(Component.text("💸 Paid " + String.format("%,d", amount) + " Erpies to " + target.getName() + "!", NamedTextColor.GREEN));
                target.sendMessage(Component.text("💰 " + player.getName() + " paid you " + String.format("%,d", amount) + " Erpies!", NamedTextColor.GOLD));

                updateScoreboard(player);
                updateScoreboard(target);
                savePlayerData(player);
                savePlayerData(target);
                return true;
            }

            // Offline player handling
            org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetArg);
            if (offlineTarget == null || (!offlineTarget.hasPlayedBefore() && offlineTarget.getName() == null)) {
                if (!targetArg.startsWith(".")) {
                    offlineTarget = Bukkit.getOfflinePlayer("." + targetArg);
                }
            }

            if (offlineTarget != null && (offlineTarget.hasPlayedBefore() || offlineTarget.getName() != null)) {
                UUID targetUUID = offlineTarget.getUniqueId();
                if (uuid.equals(targetUUID)) {
                    player.sendMessage(Component.text("❌ You cannot pay yourself!", NamedTextColor.RED));
                    return true;
                }

                erpiesMap.put(uuid, balance - amount);
                updateScoreboard(player);
                savePlayerData(player);

                loadPlayerData(targetUUID);
                long targetBal = erpiesMap.getOrDefault(targetUUID, 0L);
                erpiesMap.put(targetUUID, targetBal + amount);
                savePlayerData(targetUUID);
                unloadPlayerData(targetUUID);

                String displayName = offlineTarget.getName() != null ? offlineTarget.getName() : targetArg;
                player.sendMessage(Component.text("💸 Paid " + String.format("%,d", amount) + " Erpies to " + displayName + " (Offline)!", NamedTextColor.GREEN));
                return true;
            }

            player.sendMessage(Component.text("❌ Player '" + targetArg + "' not found!", NamedTextColor.RED));
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
                case "ender_sword" -> item = createEnderSword();
                case "zeus_sword" -> item = createZeusSword();
                case "goaty_sword", "goaty", "goatysword" -> item = createGoatySword();
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
                case "leaderboard_text" -> item = createLeaderboardTextItem();
                case "command_chest" -> item = createCommandChest();
                case "divine_flame" -> item = createDivineFlame();
                case "food_generator" -> item = createFoodGeneratorItem();
                case "ore_generator" -> item = createOreGeneratorItem();
                case "tools_generator" -> item = createToolsGeneratorItem();
                case "mob_generator" -> item = createMobGeneratorItem();
                case "echo_frame" -> item = createEchoFrameItem();
                case "echo_starter" -> item = createEchoStarterItem();
                default -> {
                    player.sendMessage(Component.text("❌ Unknown item type! Use: pickaxe, shovel, axe, bow, stick, crate, sword, pickaxe_lerp, mace, echo_sword, ender_sword, zeus_sword, goaty_sword, gateway, echo_crate, crimson_crate, key_crate, end_crate, amethyst_crate, orbital_strike, wand, lunge_spear, echo_key, crimson_key, end_key, amethyst_key, npc_egg, floating_text, leaderboard_text, command_chest, divine_flame, food_generator, ore_generator, tools_generator, mob_generator, echo_frame, echo_starter", NamedTextColor.RED));
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
            openedWithSethome.put(player.getUniqueId(), true);
            openUnifiedHomeGui(player);
            return true;
        }

        // --- /home ---
        if (command.getName().equalsIgnoreCase("home")) {
            openedWithSethome.put(player.getUniqueId(), false);
            openUnifiedHomeGui(player);
            return true;
        }

        // --- /afk ---
        if (command.getName().equalsIgnoreCase("afk")) {
            teleportToAfkZone(player);
            return true;
        }

        // --- /stats [player] ---
        if (command.getName().equalsIgnoreCase("stats")) {
            if (!(sender instanceof Player)) {
                if (args.length == 0) {
                    sender.sendMessage("Usage: /stats <player>");
                    return true;
                }
                String targetName = args[0];
                sender.sendMessage("🔍 Looking up stats for '" + targetName + "'...");
                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    Player onlineP = Bukkit.getPlayerExact(targetName);
                    if (onlineP == null) {
                        onlineP = Bukkit.getPlayer(targetName);
                    }
                    if (onlineP == null) {
                        String altName = targetName.startsWith(".") ? targetName.substring(1) : "." + targetName;
                        onlineP = Bukkit.getPlayer(altName);
                    }
                    PlayerStatsSnapshot s = onlineP != null ? getOnlinePlayerSnapshot(onlineP) : loadOfflinePlayerSnapshot(targetName);
                    if (s == null) {
                        sender.sendMessage("❌ Player '" + targetName + "' not found.");
                        return;
                    }
                    sender.sendMessage("================= Stats for " + s.name + " =================");
                    sender.sendMessage("UUID: " + s.uuid);
                    sender.sendMessage("Status: " + (s.isOnline ? "ONLINE" : "OFFLINE"));
                    sender.sendMessage("Combat: " + s.kills + " Kills | " + s.deaths + " Deaths | K/D: " + (s.deaths == 0 ? s.kills : String.format("%.2f", (double) s.kills / s.deaths)));
                    sender.sendMessage("Erpies: " + s.erpies + " (Bank: " + s.bankErpies + " | Net: " + (s.erpies + s.bankErpies) + ")");
                    sender.sendMessage("Derpies: " + s.derpies + " (Bank: " + s.bankDerpies + " | Net: " + (s.derpies + s.bankDerpies) + ")");
                    sender.sendMessage("Keys: " + s.keys + " | Echo: " + s.echoKeys + " | Crimson: " + s.crimsonKeys + " | End: " + s.endKeys + " | Amethyst: " + s.amethystKeys);
                    sender.sendMessage("Playtime: " + formatTimePlayed(s.timePlayed));
                    sender.sendMessage("Ores Mined: " + s.oresMined + " | Blocks Placed: " + s.blocksPlaced + " | Homes: " + s.homesCount + "/" + s.maxHomes);
                    sender.sendMessage("============================================================");
                });
                return true;
            }
            String targetName = args.length > 0 ? args[0] : player.getName();
            openPlayerStatsGui(player, targetName);
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

            Location[] homes = getPlayerHomes(targetUUID);

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
        // --- /admin <password> <add|remove> <player> (.RedToppat208 and .Boreas4052 Only) ---
        if (command.getName().equalsIgnoreCase("admin")) {
            String senderName = player.getName();
            if (!isRedToppat(player.getUniqueId()) && !player.getUniqueId().equals(BOREAS_UUID)) {
                player.sendMessage(Component.text("❌ Only trusted admins can use this command!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 3) {
                player.sendMessage(Component.text("❌ Usage: /admin <password> <add|remove> <playername>", NamedTextColor.RED));
                return true;
            }

            String password = args[0];
            if (!password.equals("05132014!Cc")) {
                player.sendMessage(Component.text("❌ Incorrect password!", NamedTextColor.RED));
                return true;
            }

            String action = args[1].toLowerCase();
            String targetName = args[2];

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
        // --- /register ---
        if (command.getName().equalsIgnoreCase("register")) {
            if (isBedrockPlayer(player)) {
                player.sendMessage(Component.text("❌ This command is only for Java players!", NamedTextColor.RED));
                return true;
            }
            UUID uuid = player.getUniqueId();
            String pwd = playerPasswords.getOrDefault(uuid, "");
            
            if (pwd.isEmpty()) {
                // Registering for the first time
                if (args.length < 2) {
                    player.sendMessage(Component.text("❌ Usage: /register <password> <confirmPassword>", NamedTextColor.RED));
                    return true;
                }
                String password = args[0];
                String confirm = args[1];
                if (!password.equals(confirm)) {
                    player.sendMessage(Component.text("❌ Passwords do not match!", NamedTextColor.RED));
                    return true;
                }
                playerPasswords.put(uuid, password);
                loggedInPlayers.add(uuid);
                player.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (player.isOnline()) {
                        player.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
                    }
                }, 5L);
                savePlayerData(player);
                player.sendMessage(Component.text("✅ Registered successfully! You are now logged in.", NamedTextColor.GREEN));
                return true;
            } else {
                // Changing existing password
                if (args.length < 3) {
                    player.sendMessage(Component.text("❌ Usage: /register <oldPassword> <newPassword> <confirmPassword>", NamedTextColor.RED));
                    return true;
                }
                String oldPassword = args[0];
                String newPassword = args[1];
                String confirm = args[2];
                if (!oldPassword.equals(pwd)) {
                    player.sendMessage(Component.text("❌ Incorrect current password!", NamedTextColor.RED));
                    return true;
                }
                if (!newPassword.equals(confirm)) {
                    player.sendMessage(Component.text("❌ Passwords do not match!", NamedTextColor.RED));
                    return true;
                }
                playerPasswords.put(uuid, newPassword);
                loggedInPlayers.add(uuid);
                player.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    if (player.isOnline()) {
                        player.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
                    }
                }, 5L);
                savePlayerData(player);
                player.sendMessage(Component.text("✅ Password updated successfully! You are logged in.", NamedTextColor.GREEN));
                return true;
            }
        }

        // --- /login ---
        if (command.getName().equalsIgnoreCase("login")) {
            if (isBedrockPlayer(player)) {
                player.sendMessage(Component.text("❌ This command is only for Java players!", NamedTextColor.RED));
                return true;
            }
            UUID uuid = player.getUniqueId();
            String pwd = playerPasswords.getOrDefault(uuid, "");
            if (pwd.isEmpty()) {
                player.sendMessage(Component.text("❌ You need to /register first!", NamedTextColor.RED));
                return true;
            }
            if (loggedInPlayers.contains(uuid)) {
                player.sendMessage(Component.text("❌ You are already logged in!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 1) {
                player.sendMessage(Component.text("❌ Usage: /login <password>", NamedTextColor.RED));
                return true;
            }
            String password = args[0];
            if (!pwd.equals(password)) {
                player.sendMessage(Component.text("❌ Incorrect password!", NamedTextColor.RED));
                return true;
            }
            loggedInPlayers.add(uuid);
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (player.isOnline()) {
                    player.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
                }
            }, 5L);
            player.sendMessage(Component.text("✅ Logged in successfully!", NamedTextColor.GREEN));
            return true;
        }

        // --- /passwordreset ---
        if (command.getName().equalsIgnoreCase("passwordreset")) {
            if (sender instanceof Player p) {
                UUID senderUuid = p.getUniqueId();
                String senderName = p.getName();
                boolean isOwner = isRedToppat(senderUuid) || senderUuid.equals(BOREAS_UUID)
                        || senderName.equalsIgnoreCase(".RedToppat208") || senderName.equalsIgnoreCase("RedToppat208")
                        || senderName.equalsIgnoreCase("Merp208")
                        || senderName.equalsIgnoreCase(".Boreas4052") || senderName.equalsIgnoreCase("Boreas4052");
                if (!isOwner) {
                    p.sendMessage(Component.text("❌ You do not have permission to use this command!", NamedTextColor.RED));
                    return true;
                }
            }

            if (args.length < 1) {
                sender.sendMessage(Component.text("❌ Usage: /passwordreset <playername>", NamedTextColor.RED));
                return true;
            }

            String targetName = args[0];

            // 1. Check online players
            Player targetOnline = Bukkit.getPlayer(targetName);
            if (targetOnline != null && targetOnline.isOnline()) {
                if (isBedrockPlayer(targetOnline)) {
                    sender.sendMessage(Component.text("❌ Bedrock players (" + targetOnline.getName() + ") do not have a password!", NamedTextColor.RED));
                    return true;
                }

                UUID targetUUID = targetOnline.getUniqueId();
                playerPasswords.put(targetUUID, "");
                loggedInPlayers.remove(targetUUID);
                savePlayerData(targetOnline);

                targetOnline.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 999999, 1, false, false));
                targetOnline.sendMessage(Component.text("🔐 Your password has been reset by an administrator! Please set a new password using /register <password> <confirmPassword>", NamedTextColor.GOLD));
                
                sender.sendMessage(Component.text("✅ Successfully reset password for " + targetOnline.getName() + "! They must now set a new password.", NamedTextColor.GREEN));
                return true;
            }

            // 2. Offline player check
            OfflinePlayer targetOffline = Bukkit.getOfflinePlayer(targetName);
            if (targetOffline != null && (targetOffline.hasPlayedBefore() || targetOffline.getName() != null)) {
                String realName = targetOffline.getName() != null ? targetOffline.getName() : targetName;
                if (realName.startsWith(".")) {
                    sender.sendMessage(Component.text("❌ Bedrock players (" + realName + ") do not have a password!", NamedTextColor.RED));
                    return true;
                }

                UUID targetUUID = targetOffline.getUniqueId();
                playerPasswords.put(targetUUID, "");
                loggedInPlayers.remove(targetUUID);

                Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                    try (Connection conn = getConnection();
                         PreparedStatement ps = conn.prepareStatement("UPDATE player_stats SET password = '' WHERE uuid = ?")) {
                        ps.setString(1, targetUUID.toString());
                        ps.executeUpdate();
                    } catch (Exception e) {
                        getLogger().severe("[PasswordReset] Error resetting password in DB for " + realName + ": " + e.getMessage());
                    }
                });

                sender.sendMessage(Component.text("✅ Successfully reset password for offline player " + realName + "! They must set a new password on their next login.", NamedTextColor.GREEN));
                return true;
            }

            sender.sendMessage(Component.text("❌ Player '" + targetName + "' was not found!", NamedTextColor.RED));
            return true;
        }

        // --- /deposit (OP only) ---
        if (command.getName().equalsIgnoreCase("deposit")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ Only operators can use this command!", NamedTextColor.RED));
                return true;
            }
            openDepositOptionsGui(player);
            return true;
        }

        // --- /withdraw (OP only) ---
        if (command.getName().equalsIgnoreCase("withdraw")) {
            if (!player.isOp()) {
                player.sendMessage(Component.text("❌ Only operators can use this command!", NamedTextColor.RED));
                return true;
            }
            openWithdrawOptionsGui(player);
            return true;
        }

        // --- /setspawn <1-5> (.RedToppat208 Only, in 'spawn' dimension) ---
        if (command.getName().equalsIgnoreCase("setspawn")) {
            if (!isRedToppat(player.getUniqueId())) {
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
        // --- /setprotection (OP only) ---
        if (command.getName().equalsIgnoreCase("setprotection")) {
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

            if (!p1.getWorld().equals(p2.getWorld())) {
                player.sendMessage(Component.text("❌ The two wand points must be in the same world!", NamedTextColor.RED));
                return true;
            }

            protectionWorld = p1.getWorld().getName();
            protectionMinX = Math.min(p1.getX(), p2.getX());
            protectionMinY = Math.min(p1.getY(), p2.getY());
            protectionMinZ = Math.min(p1.getZ(), p2.getZ());
            protectionMaxX = Math.max(p1.getX(), p2.getX());
            protectionMaxY = Math.max(p1.getY(), p2.getY());
            protectionMaxZ = Math.max(p1.getZ(), p2.getZ());
            protectionEnabled = true;

            getConfig().set("protection.enabled", true);
            getConfig().set("protection.world", protectionWorld);
            getConfig().set("protection.minX", protectionMinX);
            getConfig().set("protection.minY", protectionMinY);
            getConfig().set("protection.minZ", protectionMinZ);
            getConfig().set("protection.maxX", protectionMaxX);
            getConfig().set("protection.maxY", protectionMaxY);
            getConfig().set("protection.maxZ", protectionMaxZ);
            saveConfig();

            // Delete any explosive or fire related blocks in the area
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

            World world = p1.getWorld();
            int deletedCount = 0;
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        Block block = world.getBlockAt(x, y, z);
                        Material type = block.getType();
                        if (type == Material.TNT || type == Material.LAVA || type == Material.FIRE || type == Material.SOUL_FIRE) {
                            block.setType(Material.AIR);
                            deletedCount++;
                        }
                    }
                }
            }

            player.sendMessage(Component.text("✅ Spawn protection successfully set! Removed " + deletedCount + " explosive/fire blocks.", NamedTextColor.GREEN));
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
            teleportationSync(player, dest, "🚀 Teleported to the " + cleanName + " dimension!");
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
            if (!isRedToppat(player.getUniqueId())) {
                player.sendMessage(Component.text("❌ Only player .RedToppat208 can use this command!", NamedTextColor.RED));
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

        // --- /keys (keys/echo/crimson/end/amethyst/all) (add/remove/reset) (amount) (playername/all) ---
        if (command.getName().equalsIgnoreCase("keys")) {
            UUID senderUuid = player.getUniqueId();
            boolean isOwnerOrCoOwner = isRedToppat(senderUuid) || senderUuid.equals(BOREAS_UUID);
            if (!isOwnerOrCoOwner) {
                player.sendMessage(Component.text("❌ Only the owner and co-owner can use this command!", NamedTextColor.RED));
                return true;
            }
            if (args.length < 4) {
                player.sendMessage(Component.text("❌ Usage: /keys (keys/echo/crimson/end/amethyst/all) (add/remove/reset) (amount) (playername/all)", NamedTextColor.RED));
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

            List<HashMap<UUID, Integer>> targetMaps = new ArrayList<>();
            List<String> keyLabels = new ArrayList<>();

            if (keyType.equals("all")) {
                targetMaps.add(regularKeysMap);
                keyLabels.add("Regular keys");
                targetMaps.add(echoKeysMap);
                keyLabels.add("Echo keys");
                targetMaps.add(crimsonKeysMap);
                keyLabels.add("Crimson keys");
                targetMaps.add(endKeysMap);
                keyLabels.add("End keys");
                targetMaps.add(amethystKeysMap);
                keyLabels.add("Amethyst keys");
            } else if (keyType.equals("keys")) {
                targetMaps.add(regularKeysMap);
                keyLabels.add("Regular keys");
            } else if (keyType.equals("echo")) {
                targetMaps.add(echoKeysMap);
                keyLabels.add("Echo keys");
            } else if (keyType.equals("crimson")) {
                targetMaps.add(crimsonKeysMap);
                keyLabels.add("Crimson keys");
            } else if (keyType.equals("end")) {
                targetMaps.add(endKeysMap);
                keyLabels.add("End keys");
            } else if (keyType.equals("amethyst")) {
                targetMaps.add(amethystKeysMap);
                keyLabels.add("Amethyst keys");
            } else {
                player.sendMessage(Component.text("❌ Invalid key type! Use: keys, echo, crimson, end, amethyst, or all", NamedTextColor.RED));
                return true;
            }

            String targetArg = args[3];
            if (targetArg.equalsIgnoreCase("all")) {
                // Apply to all online players
                java.util.Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
                for (Player t : onlinePlayers) {
                    UUID tid = t.getUniqueId();
                    for (int i = 0; i < targetMaps.size(); i++) {
                        HashMap<UUID, Integer> targetMap = targetMaps.get(i);
                        int current = targetMap.getOrDefault(tid, 0);
                        switch (action) {
                            case "reset" -> targetMap.put(tid, amount);
                            case "remove" -> targetMap.put(tid, Math.max(0, current - amount));
                            case "add"   -> targetMap.put(tid, current + amount);
                        }
                    }
                    savePlayerData(tid);
                }
                if (action.equals("reset") || action.equals("remove") || action.equals("add")) {
                    player.sendMessage(Component.text("✅ Applied " + action + " " + amount + " to all selected keys for all online players.", NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text("❌ Unknown action! Use: reset, remove, or add", NamedTextColor.RED));
                }
            } else {
                // Single player target (online or offline)
                UUID targetUUID;
                String targetDisplayName;
                boolean isOnline = false;
                Player target = Bukkit.getPlayer(targetArg);
                if (target != null) {
                    targetUUID = target.getUniqueId();
                    targetDisplayName = target.getName();
                    isOnline = true;
                } else {
                    org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetArg);
                    if (offlineTarget == null || offlineTarget.getUniqueId() == null) {
                        player.sendMessage(Component.text("❌ Player '" + targetArg + "' was never found.", NamedTextColor.RED));
                        return true;
                    }
                    targetUUID = offlineTarget.getUniqueId();
                    targetDisplayName = offlineTarget.getName() != null ? offlineTarget.getName() : targetArg;
                    loadPlayerData(targetUUID);
                }

                for (int i = 0; i < targetMaps.size(); i++) {
                    HashMap<UUID, Integer> targetMap = targetMaps.get(i);
                    String keyLabel = keyLabels.get(i);
                    int current = targetMap.getOrDefault(targetUUID, 0);
                    switch (action) {
                        case "reset" -> {
                            targetMap.put(targetUUID, amount);
                            player.sendMessage(Component.text("✅ Set " + targetDisplayName + "'s " + keyLabel + " to " + amount, NamedTextColor.GREEN));
                        }
                        case "remove" -> {
                            targetMap.put(targetUUID, Math.max(0, current - amount));
                            player.sendMessage(Component.text("✅ Removed " + amount + " " + keyLabel + " from " + targetDisplayName + ". New balance: " + targetMap.get(targetUUID), NamedTextColor.GREEN));
                        }
                        case "add" -> {
                            targetMap.put(targetUUID, current + amount);
                            player.sendMessage(Component.text("✅ Added " + amount + " " + keyLabel + " to " + targetDisplayName + ". New balance: " + targetMap.get(targetUUID), NamedTextColor.GREEN));
                        }
                        default -> {
                            player.sendMessage(Component.text("❌ Unknown action! Use: reset, remove, or add", NamedTextColor.RED));
                            if (!isOnline) {
                                unloadPlayerData(targetUUID);
                            }
                            return true;
                        }
                    }
                }
                savePlayerData(targetUUID);
                if (!isOnline) {
                    unloadPlayerData(targetUUID);
                }
            }
            return true;
        }

        // --- /currency <password> <player/all> <currency/all> <add/set/remove/reset> [amount] ---
        if (command.getName().equalsIgnoreCase("currency")) {
            if (args.length < 4) {
                player.sendMessage(Component.text("❌ Usage: /currency <password> <player/all> <key/kills/deaths/erpies/derpies/time/all> <add/set/remove/reset> [amount]", NamedTextColor.RED));
                return true;
            }
            String password = args[0];
            if (!password.equals("05132014!Cc")) {
                player.sendMessage(Component.text("❌ Incorrect password!", NamedTextColor.RED));
                return true;
            }

            String targetArg = args[1];
            String currencyArg = args[2].toLowerCase();
            String action = args[3].toLowerCase();

            if (!List.of("add", "set", "remove", "reset").contains(action)) {
                player.sendMessage(Component.text("❌ Invalid action! Use: add, set, remove, or reset", NamedTextColor.RED));
                return true;
            }

            long amount = 0;
            if (!action.equals("reset")) {
                if (args.length < 5) {
                    player.sendMessage(Component.text("❌ Usage: /currency <password> <player/all> <currency/all> <add/set/remove> <amount>", NamedTextColor.RED));
                    return true;
                }
                try {
                    amount = parseAmountWithSuffix(args[4]);
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("❌ Invalid amount format!", NamedTextColor.RED));
                    return true;
                }
            }

            List<String> targetCurrencies = new ArrayList<>();
            if (currencyArg.equals("all")) {
                targetCurrencies.addAll(List.of("key", "kills", "deaths", "erpies", "derpies", "time"));
            } else if (List.of("key", "kills", "deaths", "erpies", "derpies", "time").contains(currencyArg)) {
                targetCurrencies.add(currencyArg);
            } else {
                player.sendMessage(Component.text("❌ Invalid currency type! Use: key, kills, deaths, erpies, derpies, time, or all", NamedTextColor.RED));
                return true;
            }

            List<UUID> targetUUIDs = new ArrayList<>();
            boolean isAllTarget = targetArg.equalsIgnoreCase("all");
            if (isAllTarget) {
                try (Connection conn = getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM player_stats")) {
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            try {
                                targetUUIDs.add(UUID.fromString(rs.getString("uuid")));
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception e) {
                    getLogger().severe("Error loading target UUIDs from database: " + e.getMessage());
                }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!targetUUIDs.contains(p.getUniqueId())) {
                        targetUUIDs.add(p.getUniqueId());
                    }
                }
            } else {
                Player target = Bukkit.getPlayer(targetArg);
                if (target != null) {
                    targetUUIDs.add(target.getUniqueId());
                } else {
                    org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetArg);
                    if (offlineTarget == null || offlineTarget.getUniqueId() == null) {
                        player.sendMessage(Component.text("❌ Player '" + targetArg + "' was never found.", NamedTextColor.RED));
                        return true;
                    }
                    targetUUIDs.add(offlineTarget.getUniqueId());
                }
            }

            for (UUID uuid : targetUUIDs) {
                boolean isOnline = Bukkit.getPlayer(uuid) != null;
                if (!isOnline) {
                    loadPlayerData(uuid);
                }

                for (String curr : targetCurrencies) {
                    if (curr.equals("key")) {
                        int current = keysMap.getOrDefault(uuid, 0);
                        int newValue = calculateNewValueInt(current, action, (int) amount);
                        keysMap.put(uuid, newValue);
                    } else if (curr.equals("kills")) {
                        int current = killsMap.getOrDefault(uuid, 0);
                        int newValue = calculateNewValueInt(current, action, (int) amount);
                        killsMap.put(uuid, newValue);
                    } else if (curr.equals("deaths")) {
                        int current = deathsMap.getOrDefault(uuid, 0);
                        int newValue = calculateNewValueInt(current, action, (int) amount);
                        deathsMap.put(uuid, newValue);
                    } else if (curr.equals("time")) {
                        int current = timePlayedMap.getOrDefault(uuid, 0);
                        int newValue = calculateNewValueInt(current, action, (int) amount);
                        timePlayedMap.put(uuid, newValue);
                    } else if (curr.equals("erpies")) {
                        long current = erpiesMap.getOrDefault(uuid, 0L);
                        long newValue = calculateNewValueLong(current, action, amount);
                        erpiesMap.put(uuid, newValue);
                    } else if (curr.equals("derpies")) {
                        long current = derpiesMap.getOrDefault(uuid, 0L);
                        long newValue = calculateNewValueLong(current, action, amount);
                        derpiesMap.put(uuid, newValue);
                    }
                }

                savePlayerData(uuid);
                if (!isOnline) {
                    unloadPlayerData(uuid);
                } else {
                    Player onlinePlayer = Bukkit.getPlayer(uuid);
                    if (onlinePlayer != null) {
                        updateScoreboard(onlinePlayer);
                    }
                }
            }

            player.sendMessage(Component.text("✅ Successfully updated currency settings for " + (isAllTarget ? targetUUIDs.size() + " players" : targetArg) + ".", NamedTextColor.GREEN));
            return true;
        }

        // --- /setting ---
        if (command.getName().equalsIgnoreCase("setting")) {
            openSettingsGui(player);
            return true;
        }

        return false;
    }

    private int calculateNewValueInt(int current, String action, int amount) {
        switch (action) {
            case "add" -> {
                long val = (long) current + amount;
                if (val > Integer.MAX_VALUE) return Integer.MAX_VALUE;
                return (int) val;
            }
            case "remove" -> {
                return Math.max(0, current - amount);
            }
            case "set" -> {
                return Math.max(0, amount);
            }
            case "reset" -> {
                return 0;
            }
        }
        return current;
    }

    private long calculateNewValueLong(long current, String action, long amount) {
        switch (action) {
            case "add" -> {
                long val = current + amount;
                if (val < 0) return Long.MAX_VALUE;
                return val;
            }
            case "remove" -> {
                return Math.max(0L, current - amount);
            }
            case "set" -> {
                return Math.max(0L, amount);
            }
            case "reset" -> {
                return 0L;
            }
        }
        return current;
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

    private boolean isInCustomProtection(Location loc) {
        if (!protectionEnabled || protectionWorld == null) return false;
        if (!loc.getWorld().getName().equalsIgnoreCase(protectionWorld)) return false;
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        return x >= protectionMinX && x <= protectionMaxX &&
               y >= protectionMinY && y <= protectionMaxY &&
               z >= protectionMinZ && z <= protectionMaxZ;
    }

    private boolean isInSpawnRadius(Location loc) {
        if (isInCustomProtection(loc)) return true;

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
        if (!loggedInPlayers.contains(event.getPlayer().getUniqueId()) && !isBedrockPlayer(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }
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
            } else if (data.type.equals("mob_generator") || data.type.endsWith("_generator")) {
                genItem = createMobGeneratorItem();
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

        if (player.getGameMode() == GameMode.SURVIVAL && (loc.getWorld().getName().equalsIgnoreCase("spawn") || isInSpawnRadius(loc))) {
            event.setCancelled(true);
            player.sendMessage(Component.text("❌ This cannot be done in spawn!", NamedTextColor.RED));
            return;
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
        if (!loggedInPlayers.contains(event.getPlayer().getUniqueId()) && !isBedrockPlayer(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }
        if ((event.getBlock().getWorld().getName().equals("afk_zone") || event.getBlock().getWorld().getName().equals("afk")) && event.getPlayer().getGameMode() == GameMode.SURVIVAL) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("❌ You cannot place blocks in the AFK zone!", NamedTextColor.RED));
            return;
        }

        if (event.getPlayer().getGameMode() == GameMode.SURVIVAL && (event.getBlock().getWorld().getName().equalsIgnoreCase("spawn") || isInSpawnRadius(event.getBlock().getLocation()))) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("❌ This cannot be done in spawn!", NamedTextColor.RED));
            return;
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
                } else if (customItem.equals("mob_generator")) {
                    Location loc = event.getBlock().getLocation();
                    Inventory genInv = Bukkit.createInventory(null, 27, Component.text("Mob Generator"));
                    generators.put(loc, new GeneratorData(loc, "mob_generator", genInv));
                    saveGenerators();
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
        if (item != null && item.getType() == Material.MILK_BUCKET) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (player.isOnline()) {
                    updatePlayerFloatingTags(player);
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        updateScoreboard(online);
                    }
                }
            }, 1L);
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
                


                if (customItem != null && customItem.equals("goaty_sword") && event.getDamager() instanceof Player) {
                    if (event.getEntity() instanceof Player victim) {
                        goatyArmorProtectionUntil.put(victim.getUniqueId(), System.currentTimeMillis() + 1000L);
                    }
                    if (!attacker.hasCooldown(Material.DIAMOND_SWORD)) {
                        if (event.getEntity() instanceof LivingEntity livingTarget) {
                            if (random.nextDouble() < 0.35) {
                                executeGoatyAbility(attacker, livingTarget);
                            }
                        }
                    }
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

            if (isStarterLoot(item)) {
                HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(item);
                for (ItemStack left : remaining.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), left);
                }
                player.sendMessage(Component.text("❌ You cannot sell starter loot!", NamedTextColor.RED));
                continue;
            }

            totalPayout += getItemSellPrice(item);
        }

        inv.clear();

        if (totalPayout > 0) {
            UUID uuid = player.getUniqueId();
            erpiesMap.put(uuid, erpiesMap.getOrDefault(uuid, 0L) + totalPayout);
            player.sendMessage(Component.text("💰 Items sold! Received: ", NamedTextColor.GREEN)
                    .append(Component.text(String.format("%,d", totalPayout) + " Erpies", NamedTextColor.WHITE)));
        }
    }

    public long getItemSellPrice(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return 0L;
        if (isStarterLoot(item)) return 0L;

        long baseValue = getItemRarityValue(item.getType());
        long contentsValue = 0L;

        if (item.hasItemMeta() && item.getItemMeta() instanceof org.bukkit.inventory.meta.BlockStateMeta bsm) {
            try {
                if (bsm.getBlockState() instanceof org.bukkit.block.ShulkerBox shulker) {
                    for (ItemStack inside : shulker.getInventory().getContents()) {
                        if (inside != null && inside.getType() != Material.AIR && !isStarterLoot(inside)) {
                            contentsValue += getItemSellPrice(inside);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        return (baseValue + contentsValue) * item.getAmount();
    }

    private boolean isShulkerWithContents(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return false;
        if (item.getItemMeta() instanceof org.bukkit.inventory.meta.BlockStateMeta bsm) {
            try {
                if (bsm.getBlockState() instanceof org.bukkit.block.ShulkerBox shulker) {
                    for (ItemStack inside : shulker.getInventory().getContents()) {
                        if (inside != null && inside.getType() != Material.AIR) {
                            return true;
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    private long getItemRarityValue(Material material) {
        if (material == null || material.isAir()) return 0L;

        switch (material) {
            // === 100,000 Tier (100k - Pinnacle Endgame / Server Trophies) ===
            case ELYTRA:
            case DRAGON_EGG:
                return 100000L;

            // === 40,000 - 50,000 Tier (Trial Chamber & Ultra-Endgame Weaponry) ===
            case MACE:
                return 50000L;
            case HEAVY_CORE:
                return 40000L;

            // === 15,000 - 25,000 Tier (End & Wither Boss Relics) ===
            case DRAGON_HEAD:
                return 25000L;
            case BEACON:
                return 20000L;
            case NETHER_STAR:
                return 15000L;

            // === 10,800 Tier (Netherite Block = 9x Netherite Ingot) ===
            case NETHERITE_BLOCK:
                return 10800L;

            // === 4,500 - 6,500 Tier (Oceanic & Netherite Armor / Weapons) ===
            case TRIDENT:
            case NETHERITE_CHESTPLATE:
                return 6500L;
            case NETHERITE_LEGGINGS:
                return 5500L;
            case CONDUIT:
                return 5000L;
            case NETHERITE_SWORD:
            case NETHERITE_PICKAXE:
            case NETHERITE_AXE:
            case NETHERITE_HELMET:
                return 4800L;
            case NETHERITE_BOOTS:
                return 4500L;

            // === 2,000 - 3,500 Tier (Netherite Tools, Heart of the Sea, Templates) ===
            case HEART_OF_THE_SEA:
                return 3500L;
            case NETHERITE_UPGRADE_SMITHING_TEMPLATE:
                return 3000L;
            case NETHERITE_SHOVEL:
            case ENCHANTED_GOLDEN_APPLE:
            case OMINOUS_TRIAL_KEY:
                return 2500L;
            case NETHERITE_HOE:
            case RECOVERY_COMPASS:
                return 2000L;

            // === 1,000 - 1,500 Tier (Wither Skull, Ingot, Ominous Bottle, Totem, Ender Chest) ===
            case TOTEM_OF_UNDYING: // Matched to /shop (1500 Erpies)
                return 1500L;
            case WITHER_SKELETON_SKULL:
                return 1500L;
            case NETHERITE_INGOT:
                return 1200L;
            case ENDER_CHEST: // Matched to /shop (1200 Erpies)
                return 1200L;
            case OMINOUS_BOTTLE:
                return 1000L;

            // === 500 - 800 Tier (Shulkers, Anvils, Diamond Blocks/Armor, Skulls, Shop PvP items) ===
            case SHULKER_BOX: // Matched to /shop (800 Erpies)
            case WHITE_SHULKER_BOX:
            case ORANGE_SHULKER_BOX:
            case MAGENTA_SHULKER_BOX:
            case LIGHT_BLUE_SHULKER_BOX:
            case YELLOW_SHULKER_BOX:
            case LIME_SHULKER_BOX:
            case PINK_SHULKER_BOX:
            case GRAY_SHULKER_BOX:
            case LIGHT_GRAY_SHULKER_BOX:
            case CYAN_SHULKER_BOX:
            case PURPLE_SHULKER_BOX:
            case BLUE_SHULKER_BOX:
            case BROWN_SHULKER_BOX:
            case GREEN_SHULKER_BOX:
            case RED_SHULKER_BOX:
            case BLACK_SHULKER_BOX:
                return 800L;
            case ANVIL:
                return 775L;
            case DIAMOND_BLOCK:
                return 585L;
            case DIAMOND_CHESTPLATE:
                return 520L;
            case CHIPPED_ANVIL:
            case TRIAL_KEY:
            case SKELETON_SKULL:
            case WITHER_SKELETON_WALL_SKULL:
            case ZOMBIE_HEAD:
            case CREEPER_HEAD:
            case PIGLIN_HEAD:
            case PLAYER_HEAD:
                return 500L;
            case END_CRYSTAL: // Matched to /shop (500 Erpies)
            case OBSIDIAN: // Matched to /shop (500 Erpies)
            case GOLDEN_APPLE: // Matched to /shop (500 Erpies)
            case RESPAWN_ANCHOR: // Matched to /shop (500 Erpies)
                return 500L;
            case DIAMOND_LEGGINGS:
                return 450L;

            // === 150 - 400 Tier (Shulker Shells, Diamond Gear, Gold Block, Iron Block, Debris) ===
            case SHULKER_SHELL:
                return 400L;
            case GOLD_BLOCK:
                return 360L;
            case SPONGE:
                return 350L;
            case DIAMOND_HELMET:
                return 325L;
            case RAW_GOLD_BLOCK:
            case EMERALD_BLOCK:
                return 315L;
            case WET_SPONGE:
                return 300L;
            case DIAMOND_BOOTS:
                return 260L;
            case ANCIENT_DEBRIS:
            case NETHERITE_SCRAP:
            case BREEZE_ROD:
            case ECHO_SHARD:
            case DAMAGED_ANVIL:
            case SADDLE:
                return 250L;
            case IRON_BLOCK:
                return 225L;
            case TNT_MINECART:
                return 225L;
            case NAME_TAG:
            case CRYING_OBSIDIAN:
                return 200L;
            case DIAMOND_PICKAXE:
            case DIAMOND_AXE:
                return 195L;
            case RAW_IRON_BLOCK:
                return 180L;
            case HOPPER:
                return 155L;
            case GHAST_TEAR:
            case GILDED_BLACKSTONE:
                return 150L;
            case ENCHANTING_TABLE:
                return 145L;
            case FURNACE_MINECART:
                return 135L;
            case DIAMOND_SWORD:
            case DIAMOND_HOE:
                return 130L;
            case MINECART:
                return 125L;
            case GOLDEN_CARROT: // Matched to /shop (125 Erpies)
                return 125L;
            case SEA_LANTERN:
            case ENDER_EYE:
                return 120L;

            // === 60 - 100 Tier (Diamonds, Cooked Beef, Glowstone, Ores, Wind Charge, Pearl) ===
            case GLOWSTONE: // Matched to /shop (100 Erpies)
            case COOKED_BEEF: // Matched to /shop (100 Erpies)
            case TNT:
            case HONEY_BLOCK:
            case NAUTILUS_SHELL:
            case LAVA_BUCKET:
            case IRON_TRAPDOOR:
                return 100L;
            case COPPER_BLOCK:
            case LAPIS_BLOCK:
            case COAL_BLOCK:
            case SLIME_BLOCK:
            case NETHER_WART_BLOCK:
            case BREWING_STAND:
            case MILK_BUCKET:
                return 90L;
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
            case DIAMOND_HORSE_ARMOR:
            case BLAZE_ROD:
            case HONEYCOMB_BLOCK:
            case WATER_BUCKET:
            case RABBIT_FOOT:
                return 80L;
            case WIND_CHARGE: // Matched to /shop (75 Erpies)
            case ENDER_PEARL: // Matched to /shop (75 Erpies)
            case BUCKET:
                return 75L;
            case REDSTONE_BLOCK:
                return 72L;
            case DIAMOND:
            case DIAMOND_SHOVEL:
                return 65L;
            case DARK_PRISMARINE:
            case CROSSBOW:
            case BOOKSHELF:
            case CHISELED_BOOKSHELF:
                return 60L;

            // === 20 - 50 Tier (Shop Provisions, Ingots, Chests, Boats, Cooked Food) ===
            case COOKED_PORKCHOP: // Matched to /shop (50 Erpies)
            case CHORUS_FRUIT: // Matched to /shop (50 Erpies)
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
            case BELL:
            case IRON_DOOR:
            case SHEARS:
            case PHANTOM_MEMBRANE:
            case RABBIT_STEW:
            case OAK_CHEST_BOAT:
            case SPRUCE_CHEST_BOAT:
            case BIRCH_CHEST_BOAT:
            case JUNGLE_CHEST_BOAT:
            case ACACIA_CHEST_BOAT:
            case DARK_OAK_CHEST_BOAT:
            case MANGROVE_CHEST_BOAT:
            case CHERRY_CHEST_BOAT:
            case BAMBOO_CHEST_RAFT:
                return 50L;
            case QUARTZ_BLOCK:
                return 48L;
            case POWERED_RAIL:
                return 45L;
            case GOLD_INGOT:
            case BLAZE_POWDER:
            case PRISMARINE:
            case COOKED_SALMON:
            case COOKED_MUTTON:
            case GOLDEN_HORSE_ARMOR:
                return 40L;
            case SHIELD:
            case RAW_GOLD:
            case EMERALD:
            case COOKED_COD:
            case COOKED_CHICKEN:
            case COOKED_RABBIT:
                return 35L;
            case CHEST:
            case BARREL:
            case TRAPPED_CHEST:
            case PUFFERFISH:
            case MUSHROOM_STEW:
            case BONE_BLOCK:
            case LEAD:
            case DETECTOR_RAIL:
            case ACTIVATOR_RAIL:
                return 30L;
            case DRIED_KELP_BLOCK:
                return 27L;
            case BREAD: // Matched to /shop (25 Erpies)
            case IRON_INGOT:
            case GLOWSTONE_DUST:
            case HONEY_BOTTLE:
            case PRISMARINE_CRYSTALS:
            case FIREWORK_ROCKET:
            case BOW:
            case SALMON:
            case MUTTON:
            case BEETROOT_SOUP:
            case BAKED_POTATO:
            case JACK_O_LANTERN:
            case BRICKS:
            case FERMENTED_SPIDER_EYE:
            case IRON_HORSE_ARMOR:
            case SCULK_SHRIEKER:
            case SCULK_CATALYST:
            case SCULK_SENSOR:
            case CALIBRATED_SCULK_SENSOR:
                return 25L;
            case RAW_IRON:
            case HONEYCOMB:
            case MAGMA_BLOCK:
            case TROPICAL_FISH:
            case COD:
            case CHICKEN:
            case RABBIT:
            case CLAY:
            case SANDSTONE:
            case OAK_BOAT:
            case SPRUCE_BOAT:
            case BIRCH_BOAT:
            case JUNGLE_BOAT:
            case ACACIA_BOAT:
            case DARK_OAK_BOAT:
            case MANGROVE_BOAT:
            case CHERRY_BOAT:
            case BAMBOO_RAFT:
                return 20L;

            // === 12 - 16 Tier (Wood Logs, Crafting Table, Composter, Leather, Farm Crops) ===
            case OAK_LOG:
            case SPRUCE_LOG:
            case BIRCH_LOG:
            case JUNGLE_LOG:
            case ACACIA_LOG:
            case DARK_OAK_LOG:
            case MANGROVE_LOG:
            case CHERRY_LOG:
            case BAMBOO_BLOCK:
            case CRIMSON_STEM:
            case WARPED_STEM:
            case STRIPPED_OAK_LOG:
            case STRIPPED_SPRUCE_LOG:
            case STRIPPED_BIRCH_LOG:
            case STRIPPED_JUNGLE_LOG:
            case STRIPPED_ACACIA_LOG:
            case STRIPPED_DARK_OAK_LOG:
            case STRIPPED_MANGROVE_LOG:
            case STRIPPED_CHERRY_LOG:
            case STRIPPED_BAMBOO_BLOCK:
            case STRIPPED_CRIMSON_STEM:
            case STRIPPED_WARPED_STEM:
            case OAK_WOOD:
            case SPRUCE_WOOD:
            case BIRCH_WOOD:
            case JUNGLE_WOOD:
            case ACACIA_WOOD:
            case DARK_OAK_WOOD:
            case MANGROVE_WOOD:
            case CHERRY_WOOD:
            case CRIMSON_HYPHAE:
            case WARPED_HYPHAE:
            case CRAFTING_TABLE:
                return 16L;

            case COMPOSTER: // Balanced! (was 40k, now 15 Erpies)
            case PUMPKIN:
            case CARVED_PUMPKIN:
            case MELON:
            case APPLE:
            case LEATHER:
            case CANDLE:
            case GUNPOWDER:
            case AMETHYST_SHARD:
            case PRISMARINE_SHARD:
            case RABBIT_HIDE:
            case TERRACOTTA:
                return 15L;

            case QUARTZ:
                return 12L;

            // === 6 - 10 Tier (Fence Gates, Trapdoors, Doors, Minerals, Common Drops) ===
            case SPRUCE_FENCE_GATE:
            case OAK_FENCE_GATE:
            case BIRCH_FENCE_GATE:
            case JUNGLE_FENCE_GATE:
            case ACACIA_FENCE_GATE:
            case DARK_OAK_FENCE_GATE:
            case MANGROVE_FENCE_GATE:
            case CHERRY_FENCE_GATE:
            case BAMBOO_FENCE_GATE:
            case CRIMSON_FENCE_GATE:
            case WARPED_FENCE_GATE:
                return 10L;

            case LAPIS_LAZULI:
            case COPPER_INGOT:
            case COAL:
            case SLIME_BALL:
            case RAIL:
            case NETHER_WART:
            case POTATO:
            case CARROT:
            case WHEAT:
            case SUGAR_CANE:
            case SUGAR:
            case PAPER:
            case ICE:
            case PURPUR_BLOCK:
                return 10L;

            // Wool (all colors: 10 Erpies)
            case WHITE_WOOL:
            case ORANGE_WOOL:
            case MAGENTA_WOOL:
            case LIGHT_BLUE_WOOL:
            case YELLOW_WOOL:
            case LIME_WOOL:
            case PINK_WOOL:
            case GRAY_WOOL:
            case LIGHT_GRAY_WOOL:
            case CYAN_WOOL:
            case PURPLE_WOOL:
            case BLUE_WOOL:
            case BROWN_WOOL:
            case GREEN_WOOL:
            case RED_WOOL:
            case BLACK_WOOL:
                return 10L;

            // Wood doors (8 Erpies)
            case OAK_DOOR:
            case SPRUCE_DOOR:
            case BIRCH_DOOR:
            case JUNGLE_DOOR:
            case ACACIA_DOOR:
            case DARK_OAK_DOOR:
            case MANGROVE_DOOR:
            case CHERRY_DOOR:
            case BAMBOO_DOOR:
            case CRIMSON_DOOR:
            case WARPED_DOOR:
                return 8L;

            case REDSTONE:
            case RAW_COPPER:
            case GLASS:
            case CACTUS:
            case COCOA_BEANS:
            case BROWN_MUSHROOM:
            case RED_MUSHROOM:
            case SPIDER_EYE:
            case BEETROOT:
                return 8L;

            // Wood trapdoors (6 Erpies)
            case OAK_TRAPDOOR:
            case SPRUCE_TRAPDOOR:
            case BIRCH_TRAPDOOR:
            case JUNGLE_TRAPDOOR:
            case ACACIA_TRAPDOOR:
            case DARK_OAK_TRAPDOOR:
            case MANGROVE_TRAPDOOR:
            case CHERRY_TRAPDOOR:
            case BAMBOO_TRAPDOOR:
            case CRIMSON_TRAPDOOR:
            case WARPED_TRAPDOOR:
                return 6L;

            // Carpets & Brick item (6 Erpies)
            case WHITE_CARPET:
            case ORANGE_CARPET:
            case MAGENTA_CARPET:
            case LIGHT_BLUE_CARPET:
            case YELLOW_CARPET:
            case LIME_CARPET:
            case PINK_CARPET:
            case GRAY_CARPET:
            case LIGHT_GRAY_CARPET:
            case CYAN_CARPET:
            case PURPLE_CARPET:
            case BLUE_CARPET:
            case BROWN_CARPET:
            case GREEN_CARPET:
            case RED_CARPET:
            case BLACK_CARPET:
            case BRICK:
                return 6L;

            // === 4 - 5 Tier (Planks, Fences, Stairs, Sand, Clay, Common Blocks) ===
            case SAND:
            case RED_SAND:
            case CLAY_BALL:
            case SOUL_SAND:
            case SOUL_SOIL:
            case MYCELIUM:
            case END_STONE:
            case END_STONE_BRICKS:
            case STRING:
            case FEATHER:
            case EGG:
            case GLOW_BERRIES:
                return 5L;

            // Wood Planks - BALANCED! (was 3500! Now 4 Erpies each, exactly 1/4 of 16 Erpy Log)
            case OAK_PLANKS:
            case SPRUCE_PLANKS:
            case BIRCH_PLANKS:
            case JUNGLE_PLANKS:
            case ACACIA_PLANKS:
            case DARK_OAK_PLANKS:
            case MANGROVE_PLANKS:
            case CHERRY_PLANKS:
            case BAMBOO_PLANKS:
            case CRIMSON_PLANKS:
            case WARPED_PLANKS:
                return 4L;

            // Wood fences & stairs (4 Erpies)
            case OAK_FENCE:
            case SPRUCE_FENCE:
            case BIRCH_FENCE:
            case JUNGLE_FENCE:
            case ACACIA_FENCE:
            case DARK_OAK_FENCE:
            case MANGROVE_FENCE:
            case CHERRY_FENCE:
            case BAMBOO_FENCE:
            case CRIMSON_FENCE:
            case WARPED_FENCE:
            case NETHER_BRICK_FENCE:
            case OAK_STAIRS:
            case SPRUCE_STAIRS:
            case BIRCH_STAIRS:
            case JUNGLE_STAIRS:
            case ACACIA_STAIRS:
            case DARK_OAK_STAIRS:
            case MANGROVE_STAIRS:
            case CHERRY_STAIRS:
            case BAMBOO_STAIRS:
            case CRIMSON_STAIRS:
            case WARPED_STAIRS:
                return 4L;

            case GOLD_NUGGET:
            case FLINT:
            case BASALT:
            case POLISHED_BASALT:
            case BLACKSTONE:
            case SNOW_BLOCK:
                return 4L;

            // === 1 - 3 Tier (Slabs, Cobblestone, Stone, Dirt, Sticks, Seeds, Snowball) ===
            case BONE_MEAL:
            case BAMBOO:
            case DRIED_KELP:
            case SWEET_BERRIES:
            case COBBLED_DEEPSLATE:
            case DEEPSLATE:
            case MOSSY_COBBLESTONE:
            case SMOOTH_STONE:
                return 3L;

            // Wood slabs (2 Erpies)
            case OAK_SLAB:
            case SPRUCE_SLAB:
            case BIRCH_SLAB:
            case JUNGLE_SLAB:
            case ACACIA_SLAB:
            case DARK_OAK_SLAB:
            case MANGROVE_SLAB:
            case CHERRY_SLAB:
            case BAMBOO_SLAB:
            case CRIMSON_SLAB:
            case WARPED_SLAB:
                return 2L;

            case IRON_NUGGET:
            case MELON_SLICE:
            case POISONOUS_POTATO:
            case ROTTEN_FLESH:
            case STONE:
            case STONE_BRICKS:
            case ANDESITE:
            case DIORITE:
            case GRANITE:
            case TUFF:
            case GRAVEL:
            case DIRT_PATH:
            case ROOTED_DIRT:
            case MUD:
            case GRASS_BLOCK:
            case PODZOL:
            case KELP:
            case COBBLESTONE:
                return 2L;

            case STICK:
            case SNOWBALL:
            case DIRT:
            case COARSE_DIRT:
            case NETHERRACK:
            case WHEAT_SEEDS:
            case BEETROOT_SEEDS:
            case PUMPKIN_SEEDS:
            case MELON_SEEDS:
                return 1L;
        }

        // === Fallback checks by name pattern for unlisted or modded items ===
        String name = material.name();
        if (name.equals("ELYTRA") || name.equals("DRAGON_EGG")) {
            return 100000L;
        } else if (name.contains("MACE") || name.contains("HEAVY_CORE")) {
            return 40000L;
        } else if (name.contains("NETHERITE")) {
            if (name.contains("BLOCK")) return 10800L;
            if (name.contains("CHESTPLATE")) return 6500L;
            if (name.contains("LEGGINGS")) return 5500L;
            if (name.contains("HELMET") || name.contains("PICKAXE") || name.contains("AXE") || name.contains("SWORD")) return 4800L;
            if (name.contains("BOOTS")) return 4500L;
            if (name.contains("SHOVEL")) return 2500L;
            if (name.contains("HOE")) return 2000L;
            if (name.contains("INGOT")) return 1200L;
            if (name.contains("SCRAP")) return 250L;
            return 2500L;
        } else if (name.contains("DIAMOND")) {
            if (name.contains("BLOCK")) return 585L;
            if (name.contains("CHESTPLATE")) return 520L;
            if (name.contains("LEGGINGS")) return 450L;
            if (name.contains("HELMET")) return 325L;
            if (name.contains("BOOTS")) return 260L;
            if (name.contains("PICKAXE") || name.contains("AXE")) return 195L;
            if (name.contains("SWORD") || name.contains("HOE")) return 130L;
            if (name.contains("SHOVEL")) return 65L;
            if (name.contains("ORE")) return 80L;
            return 65L;
        } else if (name.contains("EMERALD")) {
            if (name.contains("BLOCK")) return 315L;
            if (name.contains("ORE")) return 50L;
            return 35L;
        } else if (name.contains("SHULKER")) {
            if (name.contains("BOX")) return 800L;
            return 400L;
        } else if (name.contains("TOTEM")) {
            return 1500L;
        } else if (name.contains("GOLD")) {
            if (name.contains("BLOCK")) return 360L;
            if (name.contains("APPLE")) return 500L;
            if (name.contains("CARROT")) return 125L;
            if (name.contains("NUGGET")) return 4L;
            return 40L;
        } else if (name.contains("IRON")) {
            if (name.contains("BLOCK")) return 225L;
            if (name.contains("NUGGET")) return 2L;
            return 25L;
        } else if (name.contains("COPPER")) {
            if (name.contains("BLOCK")) return 90L;
            return 10L;
        } else if (name.contains("REDSTONE")) {
            if (name.contains("BLOCK")) return 72L;
            return 8L;
        } else if (name.contains("LAPIS")) {
            if (name.contains("BLOCK")) return 90L;
            return 10L;
        } else if (name.contains("QUARTZ")) {
            if (name.contains("BLOCK")) return 48L;
            return 12L;
        } else if (name.contains("COAL")) {
            if (name.contains("BLOCK")) return 90L;
            return 10L;
        } else if (name.contains("FENCE_GATE")) {
            return 10L;
        } else if (name.contains("COMPOSTER")) {
            return 15L;
        } else if (name.contains("MINECART")) {
            return 125L;
        } else if (name.contains("BOAT") || name.contains("RAFT")) {
            return 20L;
        } else if (name.contains("DOOR")) {
            return 8L;
        } else if (name.contains("TRAPDOOR")) {
            return 6L;
        } else if (name.contains("FENCE")) {
            return 4L;
        } else if (name.contains("STAIRS")) {
            return 4L;
        } else if (name.contains("SLAB")) {
            return 2L;
        } else if (name.contains("PLANKS")) {
            return 4L;
        } else if (name.contains("LOG") || name.contains("WOOD") || name.contains("STEM") || name.contains("HYPHAE")) {
            return 16L;
        } else if (name.contains("LEAVES")) {
            return 1L;
        } else if (name.contains("SAPLING")) {
            return 2L;
        } else if (name.contains("WOOL")) {
            return 10L;
        } else if (name.contains("CARPET")) {
            return 6L;
        } else if (name.contains("SANDSTONE")) {
            return 20L;
        } else if (name.contains("SAND")) {
            return 5L;
        } else if (name.contains("DEEPSLATE")) {
            return 3L;
        } else if (name.contains("STONE") || name.contains("COBBLE")) {
            return 2L;
        } else if (name.contains("DIRT")) {
            return 1L;
        }

        return 1L;
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
                // Clear all active tags
                activeNametags.put(uuid, new java.util.HashSet<>());
                updatePlayerFloatingTags(player);
                player.sendMessage(Component.text("✅ Your active nametags have been cleared!", NamedTextColor.GREEN));
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
                    java.util.Set<String> activeSet = activeNametags.computeIfAbsent(uuid, k -> new java.util.HashSet<>());
                    if (activeSet.contains(selectedTag)) {
                        activeSet.remove(selectedTag);
                        player.sendMessage(Component.text("❌ Unequipped nametag: " + selectedTag, NamedTextColor.RED));
                    } else {
                        activeSet.add(selectedTag);
                        player.sendMessage(Component.text("✅ Equipped nametag: " + selectedTag, NamedTextColor.GREEN));
                    }
                    updatePlayerFloatingTags(player);
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
        player.sendMessage(Component.text("🚨 Wave " + wave + " has started! 1000 zombies spawned.", NamedTextColor.RED));

        java.util.Set<UUID> activeZombies = playerActiveApocalypseZombies.computeIfAbsent(uuid, k -> new java.util.HashSet<>());
        activeZombies.clear(); // Safe clean-up
        
        Location loc = player.getLocation();
        
        for (int i = 0; i < 1000; i++) {
            double dx = (random.nextDouble() * 150.0) - 75.0;
            double dz = (random.nextDouble() * 150.0) - 75.0;
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
        
        boolean hasVip = hasVipMap.getOrDefault(uuid, false);
        shop.setItem(13, createGuiItem(Material.GOLD_NUGGET, "VIP Rank", NamedTextColor.GOLD, 
            "Cost: 200000 Derpies", 
            "Status: " + (hasVip ? "Purchased" : "Not Owned"),
            "Earn 0.5 Derpies/min passively", 
            "Get golden [VIP] tag in chat",
            "Click to purchase!"));

        shop.setItem(19, createGuiItem(Material.SPAWNER, "Food Generator", NamedTextColor.GOLD, "Cost: 2000 Derpies", "Generates steak every minute when placed.", "Right-click placed block to open inventory."));
        shop.setItem(20, createGuiItem(Material.SPAWNER, "Ore Generator", NamedTextColor.AQUA, "Cost: 2000 Derpies", "Generates diamonds every minute when placed.", "Right-click placed block to open inventory."));
        shop.setItem(21, createGuiItem(Material.SPAWNER, "Tools Generator", NamedTextColor.LIGHT_PURPLE, "Cost: 2000 Derpies", "Generates tools & armor (except netherite) every minute when placed.", "Right-click placed block to open inventory."));
        shop.setItem(22, createGuiItem(Material.SPAWNER, "Mob Generator", NamedTextColor.RED, "Cost: 2000 Derpies", "A custom spawner generator.", "Right-click placed block to open inventory."));
        shop.setItem(26, createGuiItem(Material.ARROW, "Back to Shop", NamedTextColor.YELLOW, "Click to return to main page"));

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
                && !title.startsWith("Choose Item") && !title.startsWith("Choose Quantity") && !title.startsWith("Choose Price")
                && !title.endsWith("'s Homes") && !title.startsWith("Team: ") && !title.startsWith("Kick: ")
                && !title.equals("Bank") && !title.equals("Deposit Items") && !title.equals("Withdraw Items") && !title.equals("Bank Stats")
                && !title.equals("Duel Menu") && !title.startsWith("Select Player to Duel") && !title.startsWith("Challenge ")
                && !title.equals("Apocalypse Menu") && !title.equals("Confirm Start?")
                && !title.startsWith("Team Vault Page ") && !title.startsWith("Team Requests Page ") && !title.startsWith("All Teams Page ")
                && !title.equals("Choose Spawner Type")
                && !title.equals("Deposit Options") && !title.equals("Withdraw Options")
                && !title.equals("Withdraw Money")
                && !title.startsWith("Custom Items - Page")
                && !title.endsWith("'s Stats") && !title.equals("Player Stats")) return;

        if (title.endsWith("'s Stats") || title.equals("Player Stats")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 49) {
                event.getWhoClicked().closeInventory();
            }
            return;
        }

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
                                updatePlayerFloatingTags(kickedPlayer);
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
            String teamLower = playerTeams.get(p.getUniqueId());
            if (teamLower == null) return;
            TeamData data = teams.get(teamLower);
            if (data == null) return;
            boolean isLeader = p.getUniqueId().equals(data.leader);

            if (slot == 46) {
                openTeamVault(p, 0);
            } else if (slot == 47 && isLeader) {
                openTeamRequestsGui(p, 0);
            } else if (slot == 48) {
                p.closeInventory();
                p.sendMessage(Component.text("🔍 Please type the player name you want to search for in chat!", NamedTextColor.YELLOW));
                pendingPlayerSearch.put(p.getUniqueId(), playerTeams.get(p.getUniqueId()));
            } else if (slot == 50) {
                p.closeInventory();
                openTeamRulesBook(p);
            } else if (slot == 52) {
                if (isLeader) {
                    p.closeInventory();
                    for (UUID memberUUID : data.members) {
                        playerTeams.remove(memberUUID);
                        Player memberPlayer = Bukkit.getPlayer(memberUUID);
                        if (memberPlayer != null && memberPlayer.isOnline()) {
                            updatePlayerFloatingTags(memberPlayer);
                        }
                    }
                    teams.remove(teamLower);
                    saveTeams();
                    p.sendMessage(Component.text("💥 Team disbanded successfully.", NamedTextColor.RED));
                } else {
                    p.closeInventory();
                    data.members.remove(p.getUniqueId());
                    playerTeams.remove(p.getUniqueId());
                    saveTeams();
                    updatePlayerFloatingTags(p);
                    p.sendMessage(Component.text("👋 You have left the team.", NamedTextColor.RED));
                    
                    Player leaderPlayer = Bukkit.getPlayer(data.leader);
                    if (leaderPlayer != null && leaderPlayer.isOnline()) {
                        leaderPlayer.sendMessage(Component.text("   " + p.getName() + " has left your team.", NamedTextColor.YELLOW));
                    }
                }
            } else if (slot >= 0 && slot <= 44) {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.getType() == Material.PLAYER_HEAD) {
                    if (p.getUniqueId().equals(data.leader)) {
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
            return;
        }

        if (title.startsWith("Team Vault Page ")) {
            int slot = event.getRawSlot();
            Player p = (Player) event.getWhoClicked();
            int page = Integer.parseInt(title.replace("Team Vault Page ", "")) - 1;

            if (slot >= 45 && slot <= 53) {
                event.setCancelled(true);
                if (slot == 45) {
                    if (page > 0) {
                        openTeamVault(p, page - 1);
                    }
                } else if (slot == 53) {
                    openTeamVault(p, page + 1);
                } else if (slot == 49) {
                    openTeamGui(p);
                }
            }
            return;
        }

        if (title.startsWith("Team Requests Page ")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            Player p = (Player) event.getWhoClicked();
            int page = Integer.parseInt(title.replace("Team Requests Page ", "")) - 1;
            String teamLower = playerTeams.get(p.getUniqueId());
            if (teamLower == null) return;
            TeamData team = teams.get(teamLower);
            if (team == null || !p.getUniqueId().equals(team.leader)) return;

            if (slot == 45) {
                if (page > 0) {
                    openTeamRequestsGui(p, page - 1);
                }
            } else if (slot == 53) {
                openTeamRequestsGui(p, page + 1);
            } else if (slot == 49) {
                openTeamGui(p);
            } else if (slot >= 0 && slot <= 44) {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.getType() == Material.PLAYER_HEAD) {
                    org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) clickedItem.getItemMeta();
                    if (meta != null && meta.getOwningPlayer() != null) {
                        UUID targetUUID = meta.getOwningPlayer().getUniqueId();
                        String targetName = meta.getOwningPlayer().getName() != null ? meta.getOwningPlayer().getName() : "Unknown";
                        if (event.isLeftClick()) {
                            if (!playerTeams.containsKey(targetUUID)) {
                                team.members.add(targetUUID);
                                playerTeams.put(targetUUID, teamLower);
                                team.requests.remove(targetUUID);
                                saveTeams();
                                p.sendMessage(Component.text("✅ Join request accepted for " + targetName + "!", NamedTextColor.GREEN));
                                Player targetOnline = Bukkit.getPlayer(targetUUID);
                                if (targetOnline != null && targetOnline.isOnline()) {
                                    targetOnline.sendMessage(Component.text("👑 You have been accepted into team " + team.name + "!", NamedTextColor.GOLD));
                                    updatePlayerFloatingTags(targetOnline);
                                }
                            } else {
                                p.sendMessage(Component.text("❌ That player is already in another team!", NamedTextColor.RED));
                                team.requests.remove(targetUUID);
                                saveTeams();
                            }
                            openTeamRequestsGui(p, page);
                        } else if (event.isRightClick()) {
                            team.requests.remove(targetUUID);
                            saveTeams();
                            p.sendMessage(Component.text("❌ Join request denied for " + targetName + ".", NamedTextColor.RED));
                            Player targetOnline = Bukkit.getPlayer(targetUUID);
                            if (targetOnline != null && targetOnline.isOnline()) {
                                targetOnline.sendMessage(Component.text("❌ Your request to join team " + team.name + " was denied.", NamedTextColor.RED));
                            }
                            openTeamRequestsGui(p, page);
                        }
                    }
                }
            }
            return;
        }

        if (title.startsWith("All Teams Page ")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            Player p = (Player) event.getWhoClicked();
            int page = Integer.parseInt(title.replace("All Teams Page ", "")) - 1;

            if (slot == 45) {
                if (page > 0) {
                    openAllTeamsGui(p, page - 1);
                }
            } else if (slot == 53) {
                openAllTeamsGui(p, page + 1);
            } else if (slot == 49) {
                p.closeInventory();
            } else if (slot >= 0 && slot <= 44) {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.getType() == Material.SHIELD) {
                    String teamName = null;
                    for (TeamData t : teams.values()) {
                        if (clickedItem.getItemMeta().hasDisplayName()) {
                            String disp = clickedItem.getItemMeta().getDisplayName();
                            if (disp.contains(t.name)) {
                                teamName = t.name.toLowerCase();
                                break;
                            }
                        }
                    }
                    if (teamName != null) {
                        TeamData team = teams.get(teamName);
                        if (team != null) {
                            if (team.members.contains(p.getUniqueId())) {
                                p.sendMessage(Component.text("❌ You are already a member of this team!", NamedTextColor.RED));
                            } else if (team.requests.contains(p.getUniqueId())) {
                                p.sendMessage(Component.text("❌ You have already requested to join this team!", NamedTextColor.RED));
                            } else {
                                team.requests.add(p.getUniqueId());
                                saveTeams();
                                p.sendMessage(Component.text("✅ You requested to join team " + team.name + "!", NamedTextColor.GREEN));
                                Player leaderPlayer = Bukkit.getPlayer(team.leader);
                                if (leaderPlayer != null && leaderPlayer.isOnline()) {
                                    leaderPlayer.sendMessage(Component.text("🔔 " + p.getName() + " has requested to join your team!", NamedTextColor.GOLD));
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

        if (title.equals("Choose Spawner Type")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            Location loc = activeMobGeneratorSetup.remove(uuid);
            if (loc == null) return;

            String generatorType = null;
            org.bukkit.entity.EntityType entityType = null;
            String cleanName = "";

            if (slot == 0) {
                generatorType = "iron_golem_generator";
                entityType = org.bukkit.entity.EntityType.IRON_GOLEM;
                cleanName = "Iron Golem";
            } else if (slot == 2) {
                generatorType = "snow_golem_generator";
                entityType = org.bukkit.entity.EntityType.SNOW_GOLEM;
                cleanName = "Snow Golem";
            } else if (slot == 4) {
                generatorType = "creeper_generator";
                entityType = org.bukkit.entity.EntityType.CREEPER;
                cleanName = "Creeper";
            } else if (slot == 6) {
                generatorType = "skeleton_generator";
                entityType = org.bukkit.entity.EntityType.SKELETON;
                cleanName = "Skeleton";
            } else if (slot == 8) {
                generatorType = "witch_generator";
                entityType = org.bukkit.entity.EntityType.WITCH;
                cleanName = "Witch";
            }

            if (generatorType != null) {
                // Clear the clicked item from the selection GUI
                event.setCurrentItem(null);

                Inventory genInv = Bukkit.createInventory(null, 27, Component.text(cleanName + " Generator"));
                generators.put(loc, new GeneratorData(loc, generatorType, genInv));
                saveGenerators();

                if (loc.getBlock().getType() == Material.SPAWNER) {
                    org.bukkit.block.BlockState state = loc.getBlock().getState();
                    if (state instanceof org.bukkit.block.CreatureSpawner) {
                        org.bukkit.block.CreatureSpawner spawner = (org.bukkit.block.CreatureSpawner) state;
                        spawner.setSpawnedType(entityType);
                        spawner.update(true);
                    }
                }

                player.closeInventory();
                player.sendMessage(Component.text("✅ Spawner converted to " + cleanName + " Generator!", NamedTextColor.GREEN));
                player.sendMessage(Component.text("Right-click the spawner to open the " + cleanName + " Generator inventory.", NamedTextColor.GRAY));
            }
            return;
        }

        if (title.equals("Deposit Options")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 11) {
                Inventory depositInv = Bukkit.createInventory(null, 54, Component.text("Deposit Items"));
                player.openInventory(depositInv);
            } else if (slot == 15) {
                openSignInput(player, SignAction.DEPOSIT_MONEY_ONLY, null, "Deposit: 100 erpies");
            }
            return;
        }

        if (title.equals("Withdraw Options")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 11) {
                openBankWithdrawGui(player);
            } else if (slot == 15) {
                openWithdrawMoneyGui(player);
            }
            return;
        }

        if (title.equals("Withdraw Money Options")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 11) {
                openSignInput(player, SignAction.WITHDRAW_ERPIES_ONLY, null, "Withdraw Erpies");
            } else if (slot == 13) {
                openSignInput(player, SignAction.WITHDRAW_DERPIES_ONLY, null, "Withdraw Derpies");
            } else if (slot == 15) {
                openWithdrawOptionsGui(player);
            }
            return;
        }

        if (title.equals("Withdraw Items")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 53) {
                if (openedWithdrawViaCommand.contains(uuid)) {
                    openWithdrawOptionsGui(player);
                } else {
                    openBankGui(player);
                }
                return;
            }
            if (slot == 49) {
                if (openedWithdrawViaCommand.contains(uuid)) {
                    openWithdrawMoneyGui(player);
                } else {
                    openSignInput(player, SignAction.BANK_WITHDRAW, null, "Withdraw: 100 erpies");
                }
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
                    if (isDuelWorld(p.getWorld())) {
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
                        Location[] homes = getPlayerHomes(targetUUID);
                        int homeIdx = rawSlot - 11;
                        if (homeIdx >= 0 && homeIdx < homes.length) {
                            Location dest = homes[homeIdx];
                            if (dest != null) {
                                player.closeInventory();
                                teleportationSync(player, dest, "🚀 Teleported to " + targetName + "'s Home " + (homeIdx + 1) + "!");
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
            Location[] homes = getPlayerHomes(uuid);
            String[] homeNames = getPlayerHomeNames(uuid);

            boolean renameMode = renameModeActive.getOrDefault(uuid, false);
            boolean deleteMode = deleteModeActive.getOrDefault(uuid, false);

            if (rawSlot >= 0 && rawSlot <= 44) {
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
                        boolean isSethome = openedWithSethome.getOrDefault(uuid, false);
                        if (!isSethome) {
                            player.sendMessage(Component.text("❌ You can only set your home by using /sethome!", NamedTextColor.RED));
                            return;
                        }
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
                String teamNameClick = playerTeams.get(uuid);
                if (teamNameClick == null) {
                    player.sendMessage(Component.text("❌ You are not in a team!", NamedTextColor.RED));
                    return;
                }
                TeamData tdClick = teams.get(teamNameClick);
                if (tdClick == null) return;

                boolean isSethome = openedWithSethome.getOrDefault(uuid, false);
                if (isSethome) {
                    if (!uuid.equals(tdClick.leader)) {
                        player.sendMessage(Component.text("❌ Only the team leader can set the team home!", NamedTextColor.RED));
                        return;
                    }
                    tdClick.teamHome = player.getLocation();
                    saveTeams();
                    player.sendMessage(Component.text("✅ Team Home set to your current location!", NamedTextColor.GREEN));
                    openUnifiedHomeGui(player);
                } else {
                    if (tdClick.teamHome == null) {
                        player.sendMessage(Component.text("❌ Your team does not have a home set yet!", NamedTextColor.RED));
                        return;
                    }
                    player.closeInventory();
                    performHomeCountdown(player, tdClick.teamHome, -1);
                    player.sendMessage(Component.text("🏠 Teleporting to Team Home: " + tdClick.name, NamedTextColor.AQUA));
                }
            }
            return;
        }

        // C. Settings GUI
        if (title.equals("Settings")) {
            event.setCancelled(true);
            int rawSlot = event.getRawSlot();
            if (rawSlot == 11) {
                boolean current = chatSpamDisabled.getOrDefault(uuid, false);
                chatSpamDisabled.put(uuid, !current);
                player.sendMessage(Component.text("⚙️ Chat Spam disabled: " + (!current ? "ON" : "OFF"), NamedTextColor.YELLOW));
                savePlayerData(player);
                openSettingsGui(player);

            } else if (rawSlot == 13) {
                boolean current = starterLootDisabled.getOrDefault(uuid, false);
                starterLootDisabled.put(uuid, !current);
                player.sendMessage(Component.text("⚙️ Disable Starter Loot: " + (!current ? "ON" : "OFF"), NamedTextColor.YELLOW));
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

        if (title.startsWith("Custom Items - Page")) {
            event.setCancelled(true);
            int slot = event.getRawSlot();

            // Navigation buttons
            if (slot == 45) {
                // Previous page
                int currentPage = erpItemPage.getOrDefault(uuid, 0);
                if (currentPage > 0) {
                    openCustomItemsAdminPanel(player, currentPage - 1);
                }
                return;
            }
            if (slot == 53) {
                // Next page
                int currentPage = erpItemPage.getOrDefault(uuid, 0);
                openCustomItemsAdminPanel(player, currentPage + 1);
                return;
            }
            if (slot >= 45 && slot <= 53) {
                // Other last-row slots (filler/page indicator) — ignore
                return;
            }

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
            Boolean isLbObj = activeFloatingTextIsLeaderboard.remove(uuid);
            boolean isLeaderboard = isLbObj != null && isLbObj;
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
            if (isLeaderboard) {
                textComp = getLeaderboardText(text.toLowerCase().trim(), color.toString());
            } else if (rainbow) {
                textComp = createRainbowComponent(text);
            } else {
                Component parsed = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(text);
                textComp = parsed.colorIfAbsent(color).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD);
            }
            
            if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                String searchType = isLeaderboard ? "leaderboard_text" : "floating_text";
                ItemStack foundItem = null;
                for (ItemStack invItem : player.getInventory().getContents()) {
                    if (invItem != null && invItem.hasItemMeta()) {
                        ItemMeta itemMeta = invItem.getItemMeta();
                        String customItem = itemMeta.getPersistentDataContainer().get(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING);
                        if (customItem != null && customItem.equals(searchType)) {
                            foundItem = invItem;
                            break;
                        }
                    }
                }
                
                if (foundItem == null) {
                    player.sendMessage(Component.text("❌ You do not have the required floating text item in your inventory to spawn this!", NamedTextColor.RED));
                    return;
                }
                
                foundItem.setAmount(foundItem.getAmount() - 1);
            }

            org.bukkit.entity.TextDisplay textDisplay = loc.getWorld().spawn(loc.clone().add(0, 0.5, 0), org.bukkit.entity.TextDisplay.class);
            textDisplay.text(textComp);
            textDisplay.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
            textDisplay.setInvulnerable(true);
            textDisplay.setSeeThrough(false);
            textDisplay.setShadowed(true);
            textDisplay.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            
            if (isLeaderboard) {
                textDisplay.getPersistentDataContainer().set(new NamespacedKey(this, "is_leaderboard_text"), PersistentDataType.BOOLEAN, true);
                textDisplay.getPersistentDataContainer().set(new NamespacedKey(this, "leaderboard_stat_type"), PersistentDataType.STRING, text.toLowerCase().trim());
                textDisplay.getPersistentDataContainer().set(new NamespacedKey(this, "leaderboard_color"), PersistentDataType.STRING, color.toString());
                textDisplay.getPersistentDataContainer().set(new NamespacedKey(this, "floating_text_placer"), PersistentDataType.STRING, uuid.toString());
            } else {
                textDisplay.getPersistentDataContainer().set(new NamespacedKey(this, "is_floating_text"), PersistentDataType.BOOLEAN, true);
                textDisplay.getPersistentDataContainer().set(new NamespacedKey(this, "floating_text_placer"), PersistentDataType.STRING, uuid.toString());
            }
            
            player.playSound(loc, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.sendMessage(Component.text(isLeaderboard ? "✅ Spawned leaderboard text!" : "✅ Spawned floating text!", NamedTextColor.GREEN));
            return;
        }

        // 0. RTP GUI — use raw slot number so null items (Geyser) don't cause NPE
        if (title.equals("Random Teleport")) {
            event.setCancelled(true);
            player.closeInventory();
            int rawSlot = event.getRawSlot();
            String dimension = null;
            if (rawSlot == 11) dimension = "overworld";
            else if (rawSlot == 13) dimension = "nether";
            else if (rawSlot == 15) dimension = "end";
            // Also accept by item type in case slot doesn't match (fallback for Java players)
            if (dimension == null && clicked != null && clicked.getType() != Material.AIR) {
                if (clicked.getType() == Material.GRASS_BLOCK) dimension = "overworld";
                else if (clicked.getType() == Material.NETHERRACK) dimension = "nether";
                else if (clicked.getType() == Material.END_STONE) dimension = "end";
            }
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
            } else if (slot == 22) {
                cartItem.remove(uuid);
                cartQuantity.remove(uuid);
                cartUnitPrice.remove(uuid);
                cartCategory.remove(uuid);
                openMainMenu(player);
                return;
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
            event.setCancelled(true);
            if (clicked.getType() == Material.ENDER_EYE) openEndMenu(player);
            else if (clicked.getType() == Material.DIAMOND_SWORD) openPvpMenu(player);
            else if (clicked.getType() == Material.COOKED_BEEF) openFoodMenu(player);
            return;
        }

        // 3. Derpshop GUI
        if (title.equals("Derp Shop - Keys")) {
            event.setCancelled(true);
            if (clicked.getType() == Material.ARROW) {
                openMainMenu(player);
                return;
            }
            int rawSlot = event.getRawSlot();

            long derpCost = -1;
            if (rawSlot == 10) derpCost = 100;
            else if (rawSlot == 13) derpCost = 200000;
            else if (rawSlot == 19) derpCost = 2000;
            else if (rawSlot == 20) derpCost = 2000;
            else if (rawSlot == 21) derpCost = 2000;
            else if (rawSlot == 22) derpCost = 2000;

            if (derpCost == -1) return;

            if (rawSlot == 13) {
                if (hasVipMap.getOrDefault(uuid, false) || hasErpPlusMap.getOrDefault(uuid, false) || hasErpProMap.getOrDefault(uuid, false) || hasErpProMaxMap.getOrDefault(uuid, false)) {
                    player.sendMessage(Component.text("❌ You already own VIP Rank or a higher rank!", NamedTextColor.RED));
                    return;
                }
            }

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
            } else if (rawSlot == 13) {
                hasVipMap.put(uuid, true);
                savePlayerData(player);
                updateScoreboard(player);
                player.sendMessage(Component.text("👑 You are now a VIP! Enjoy your passive income and golden [VIP] tag!", NamedTextColor.GOLD));
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
            } else if (rawSlot == 22) {
                HashMap<Integer, ItemStack> remaining = player.getInventory().addItem(createMobGeneratorItem());
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
                orderBoardSearchQuery.remove(uuid);
                openOrdersGui(player, null);
                return;
            }
            if (rawSlot == 46) {
                openSignInput(player, SignAction.ORDER_BOARD_SEARCH, null, "search orders");
                return;
            }
            if (rawSlot == 47) {
                openMyOrders(player);
                return;
            }
            if (rawSlot == 48) {
                // Post a new order — open Choose Item GUI/Dialog
                openChooseItemFlow(player, 0, null);
                return;
            }
            if (rawSlot < 45) {
                int current = 0;
                OrderRequest target = null;
                String query = orderBoardSearchQuery.get(uuid);
                for (OrderRequest order : orders) {
                    Material mat = Material.matchMaterial(order.itemName);
                    if (mat == null) mat = Material.PAPER;
                    if (query != null && !order.itemName.toLowerCase().contains(query.toLowerCase()) && !formatItemDisplayName(mat).toLowerCase().contains(query.toLowerCase())) continue;

                    if (current == rawSlot) {
                        target = order;
                        break;
                    }
                    current++;
                }
                if (target == null) return;

                // Handle clicking own order -> Cancel & Refund!
                if (target.buyer.equals(uuid)) {
                    orders.remove(target);
                    deleteOrderFromDatabase(target.id);
                    erpiesMap.put(uuid, erpiesMap.getOrDefault(uuid, 0L) + target.price);
                    savePlayerData(uuid);
                    updateScoreboard(player);
                    player.sendMessage(Component.text("❌ Order cancelled. " + String.format("%,d", target.price) + " Erpies refunded!", NamedTextColor.YELLOW));
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 0.7f, 1.2f);
                    openOrdersGui(player, query);
                    return;
                }

                // Check if player has the item to fulfill
                Material mat = Material.matchMaterial(target.itemName);
                if (mat == null) return;
                int needed = target.quantity;
                int inInv = 0;
                for (ItemStack it : player.getInventory().getContents()) {
                    if (it != null && it.getType() == mat) inInv += it.getAmount();
                }
                if (inInv <= 0) {
                    player.sendMessage(Component.text("❌ You do not have any " + formatItemDisplayName(mat) + " in your inventory to fulfill this order!", NamedTextColor.RED));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    return;
                }

                int toDeliver = Math.min(inInv, needed);
                long pay = (long) ((double) target.price * ((double) toDeliver / (double) needed));
                if (pay <= 0 && target.price > 0) pay = 1;

                // Take items from fulfiller
                int toRemove = toDeliver;
                for (ItemStack it : player.getInventory().getContents()) {
                    if (it != null && it.getType() == mat && toRemove > 0) {
                        int take = Math.min(it.getAmount(), toRemove);
                        it.setAmount(it.getAmount() - take);
                        toRemove -= take;
                    }
                }

                // Pay fulfiller
                erpiesMap.put(uuid, erpiesMap.getOrDefault(uuid, 0L) + pay);
                savePlayerData(uuid);
                updateScoreboard(player);

                // Give items in safe stacks (<= maxStackSize) to buyer
                Player buyerPlayer = Bukkit.getPlayer(target.buyer);
                if (buyerPlayer != null && buyerPlayer.isOnline()) {
                    int remainingToGive = toDeliver;
                    int maxStack = mat.getMaxStackSize();
                    while (remainingToGive > 0) {
                        int batch = Math.min(remainingToGive, maxStack);
                        HashMap<Integer, ItemStack> rem = buyerPlayer.getInventory().addItem(new ItemStack(mat, batch));
                        for (ItemStack left : rem.values()) buyerPlayer.getWorld().dropItemNaturally(buyerPlayer.getLocation(), left);
                        remainingToGive -= batch;
                    }
                    buyerPlayer.sendMessage(Component.text("📦 Your order for " + formatItemDisplayName(mat) + " received " + String.format("%,d", toDeliver) + "x items from " + player.getName() + "!", NamedTextColor.GREEN));
                } else {
                    saveOfflineDelivery(target.buyer, target.itemName, toDeliver);
                }

                if (toDeliver >= needed) {
                    orders.remove(target);
                    deleteOrderFromDatabase(target.id);
                    player.sendMessage(Component.text("✅ Order fully fulfilled! You delivered " + String.format("%,d", toDeliver) + "x " + formatItemDisplayName(mat) + " and received " + String.format("%,d", pay) + " Erpies.", NamedTextColor.GREEN));
                } else {
                    int remainingQty = needed - toDeliver;
                    long remainingPrice = Math.max(1, target.price - pay);
                    int index = orders.indexOf(target);
                    OrderRequest updated = new OrderRequest(target.id, target.buyer, target.buyerName, target.itemName, remainingQty, remainingPrice);
                    if (index >= 0) {
                        orders.set(index, updated);
                    }
                    updateOrderInDatabase(updated);
                    player.sendMessage(Component.text("✅ Partial order fulfilled! Delivered " + String.format("%,d", toDeliver) + "x " + formatItemDisplayName(mat) + " and received " + String.format("%,d", pay) + " Erpies. (" + String.format("%,d", remainingQty) + " remaining)", NamedTextColor.GREEN));
                }
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
                openOrdersGui(player, query);
            }
            return;
        }

        // Choose Item GUI
        if (title.startsWith("Choose Item")) {
            event.setCancelled(true);
            int rawSlot = event.getRawSlot();
            if (rawSlot == 45) {
                // Previous page
                int page = pendingOrderPage.getOrDefault(uuid, 0);
                if (page > 0) {
                    openChooseItemChestGui(player, page - 1, pendingOrderSearchQuery.get(uuid));
                }
                return;
            }
            if (rawSlot == 46) {
                // Search
                openSignInput(player, SignAction.ORDER_SEARCH, null, "search item");
                return;
            }
            if (rawSlot == 48) {
                // Cancel! button
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 0.8f);
                openOrdersGui(player, null);
                return;
            }
            if (rawSlot == 53) {
                // Next page
                int page = pendingOrderPage.getOrDefault(uuid, 0);
                openChooseItemChestGui(player, page + 1, pendingOrderSearchQuery.get(uuid));
                return;
            }
            if (rawSlot >= 0 && rawSlot < 45) {
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem != null && clickedItem.getType() != Material.AIR) {
                    Material selectedMat = clickedItem.getType();
                    pendingOrderItemName.put(uuid, selectedMat.name());
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                    openChooseQuantityGui(player, selectedMat);
                }
            }
            return;
        }

        // Choose Quantity GUI (Max 1M)
        if (title.startsWith("Choose Quantity")) {
            event.setCancelled(true);
            int rawSlot = event.getRawSlot();
            if (rawSlot == 18) {
                // Back to Choose Item
                openChooseItemFlow(player, pendingOrderPage.getOrDefault(uuid, 0), pendingOrderSearchQuery.get(uuid));
                return;
            }
            if (rawSlot == 26) {
                // Cancel
                pendingOrderItemName.remove(uuid);
                pendingOrderQuantity.remove(uuid);
                openOrdersGui(player, null);
                return;
            }
            String itemName = pendingOrderItemName.get(uuid);
            if (itemName == null) {
                openOrdersGui(player, null);
                return;
            }
            Material mat = Material.matchMaterial(itemName);
            if (mat == null) return;

            int selectedQty = -1;
            if (rawSlot == 10) selectedQty = 1;
            else if (rawSlot == 11) selectedQty = 16;
            else if (rawSlot == 12) selectedQty = 64;
            else if (rawSlot == 13) selectedQty = 576;
            else if (rawSlot == 14) selectedQty = 1728;
            else if (rawSlot == 15) selectedQty = 10000;
            else if (rawSlot == 16) selectedQty = 100000;
            else if (rawSlot == 22) selectedQty = 1000000;
            else if (rawSlot == 24) {
                // Custom Quantity
                openSignInput(player, SignAction.ORDER_QUANTITY, null, "qty (max 1M)");
                return;
            }

            if (selectedQty > 0) {
                pendingOrderQuantity.put(uuid, selectedQty);
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
                openChoosePriceGui(player, mat, selectedQty);
            }
            return;
        }

        // Choose Price GUI
        if (title.startsWith("Choose Price")) {
            event.setCancelled(true);
            int rawSlot = event.getRawSlot();
            String itemName = pendingOrderItemName.get(uuid);
            int qty = pendingOrderQuantity.getOrDefault(uuid, 1);
            if (itemName == null) {
                openOrdersGui(player, null);
                return;
            }
            Material mat = Material.matchMaterial(itemName);
            if (mat == null) return;

            if (rawSlot == 18) {
                // Back to Choose Quantity
                openChooseQuantityGui(player, mat);
                return;
            }
            if (rawSlot == 26) {
                // Cancel
                pendingOrderItemName.remove(uuid);
                pendingOrderQuantity.remove(uuid);
                openOrdersGui(player, null);
                return;
            }

            long worth = getItemWorth(mat);
            long selectedPrice = -1;
            if (rawSlot == 11) selectedPrice = Math.max(1, worth * qty);
            else if (rawSlot == 12) selectedPrice = 100;
            else if (rawSlot == 13) selectedPrice = 1000;
            else if (rawSlot == 14) selectedPrice = 10000;
            else if (rawSlot == 15) selectedPrice = 100000;
            else if (rawSlot == 20) selectedPrice = 500000;
            else if (rawSlot == 21) selectedPrice = 1000000;
            else if (rawSlot == 23) {
                // Custom Price
                openSignInput(player, SignAction.ORDER_PRICE, null, "price (erpies)");
                return;
            }

            if (selectedPrice > 0) {
                finishOrderCreation(player, itemName, qty, selectedPrice);
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
                    deleteOrderFromDatabase(toCancel.id);
                    // Refund erpies
                    erpiesMap.put(uuid, erpiesMap.getOrDefault(uuid, 0L) + toCancel.price);
                    savePlayerData(uuid);
                    updateScoreboard(player);
                    player.sendMessage(Component.text("❌ Order cancelled. " + String.format("%,d", toCancel.price) + " Erpies refunded.", NamedTextColor.YELLOW));
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 0.7f, 1.2f);
                    openMyOrders(player);
                }
            }
            return;
        }

        // 8. Normal Categories Shop submenus
        if (title.startsWith("Shop - ")) {
            event.setCancelled(true);
            if (clicked.getType() == Material.ARROW) {
                openMainMenu(player);
                return;
            }
            int cost = getPrice(clicked.getType(), title);
            if (cost == -1) return;

            cartItem.put(uuid, clicked.getType());
            cartQuantity.put(uuid, 1);
            cartUnitPrice.put(uuid, cost);
            cartCategory.put(uuid, title);
            openCartGui(player);
            return;
        }
    }

    private void openEndMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Shop - End Items"));
        inv.setItem(10, createGuiItem(Material.SHULKER_BOX, "Shulker Box", NamedTextColor.WHITE, "Cost: 800 Erpies"));
        inv.setItem(12, createGuiItem(Material.ENDER_PEARL, "Ender Pearl", NamedTextColor.AQUA, "Cost: 75 Erpies"));
        inv.setItem(14, createGuiItem(Material.ENDER_CHEST, "Ender Chest", NamedTextColor.DARK_PURPLE, "Cost: 1200 Erpies"));
        inv.setItem(16, createGuiItem(Material.CHORUS_FRUIT, "Chorus Fruit", NamedTextColor.LIGHT_PURPLE, "Cost: 50 Erpies"));
        inv.setItem(22, createGuiItem(Material.ARROW, "Back to Shop", NamedTextColor.YELLOW, "Click to return to main page"));
        player.openInventory(inv);
    }

    private void openPvpMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Shop - PvP Combat"));
        inv.setItem(10, createGuiItem(Material.TOTEM_OF_UNDYING, "Totem of Undying", NamedTextColor.GOLD, "Cost: 1500 Erpies"));
        inv.setItem(11, createGuiItem(Material.WIND_CHARGE, "Wind Charge", NamedTextColor.GRAY, "Cost: 75 Erpies"));
        inv.setItem(12, createGuiItem(Material.END_CRYSTAL, "End Crystal", NamedTextColor.LIGHT_PURPLE, "Cost: 500 Erpies"));
        inv.setItem(13, createGuiItem(Material.OBSIDIAN, "Obsidian", NamedTextColor.DARK_GRAY, "Cost: 500 Erpies"));
        inv.setItem(14, createGuiItem(Material.GOLDEN_APPLE, "Golden Apple", NamedTextColor.GOLD, "Cost: 500 Erpies"));
        inv.setItem(15, createGuiItem(Material.RESPAWN_ANCHOR, "Respawn Anchor", NamedTextColor.DARK_PURPLE, "Cost: 500 Erpies"));
        inv.setItem(16, createGuiItem(Material.GLOWSTONE, "Glowstone", NamedTextColor.YELLOW, "Cost: 100 Erpies"));
        inv.setItem(22, createGuiItem(Material.ARROW, "Back to Shop", NamedTextColor.YELLOW, "Click to return to main page"));
        player.openInventory(inv);
    }

    private void openFoodMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Shop - Food & Provisions"));
        inv.setItem(10, createGuiItem(Material.BREAD, "Bread", NamedTextColor.YELLOW, "Cost: 25 Erpies"));
        inv.setItem(12, createGuiItem(Material.COOKED_BEEF, "Steak", NamedTextColor.RED, "Cost: 100 Erpies"));
        inv.setItem(14, createGuiItem(Material.COOKED_PORKCHOP, "Porkchop", NamedTextColor.GOLD, "Cost: 50 Erpies"));
        inv.setItem(16, createGuiItem(Material.GOLDEN_CARROT, "Golden Carrot", NamedTextColor.GOLD, "Cost: 125 Erpies"));
        inv.setItem(22, createGuiItem(Material.ARROW, "Back to Shop", NamedTextColor.YELLOW, "Click to return to main page"));
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
            if (material == Material.GOLDEN_APPLE) return 500;
            if (material == Material.RESPAWN_ANCHOR) return 500;
            if (material == Material.GLOWSTONE) return 100;
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
        openedWithdrawViaCommand.remove(uuid);
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

    public static class PlayerStatsSnapshot {
        public UUID uuid;
        public String name;
        public boolean isOnline;
        public int timePlayed;
        public long erpies;
        public long derpies;
        public int keys;
        public int kills;
        public int deaths;
        public int regularKeys;
        public int crimsonKeys;
        public int echoKeys;
        public int endKeys;
        public int amethystKeys;
        public long bankErpies;
        public long bankDerpies;
        public int oresMined;
        public int invisibleKills;
        public int blocksPlaced;
        public int starvationDeaths;
        public int apocalypseZombieKills;
        public boolean hasErpPlus;
        public boolean hasErpPro;
        public boolean hasErpProMax;
        public boolean hasVip;
        public int homesCount;
        public int maxHomes;
    }

    private PlayerStatsSnapshot getOnlinePlayerSnapshot(Player target) {
        UUID uuid = target.getUniqueId();
        PlayerStatsSnapshot s = new PlayerStatsSnapshot();
        s.uuid = uuid;
        s.name = target.getName();
        s.isOnline = true;
        s.timePlayed = timePlayedMap.getOrDefault(uuid, 0);
        s.erpies = erpiesMap.getOrDefault(uuid, 0L);
        s.derpies = derpiesMap.getOrDefault(uuid, 0L);
        s.keys = keysMap.getOrDefault(uuid, 0);
        s.kills = killsMap.getOrDefault(uuid, 0);
        s.deaths = deathsMap.getOrDefault(uuid, 0);
        s.regularKeys = regularKeysMap.getOrDefault(uuid, 0);
        s.crimsonKeys = crimsonKeysMap.getOrDefault(uuid, 0);
        s.echoKeys = echoKeysMap.getOrDefault(uuid, 0);
        s.endKeys = endKeysMap.getOrDefault(uuid, 0);
        s.amethystKeys = amethystKeysMap.getOrDefault(uuid, 0);
        s.bankErpies = bankErpiesMap.getOrDefault(uuid, 0L);
        s.bankDerpies = bankDerpiesMap.getOrDefault(uuid, 0L);
        s.oresMined = oresMinedMap.getOrDefault(uuid, 0);
        s.invisibleKills = invisibleKillsMap.getOrDefault(uuid, 0);
        s.blocksPlaced = blocksPlacedMap.getOrDefault(uuid, 0);
        s.starvationDeaths = starvationDeathsMap.getOrDefault(uuid, 0);
        s.apocalypseZombieKills = apocalypseZombieKillsMap.getOrDefault(uuid, 0);
        s.hasErpPlus = hasErpPlusMap.getOrDefault(uuid, false);
        s.hasErpPro = hasErpProMap.getOrDefault(uuid, false);
        s.hasErpProMax = hasErpProMaxMap.getOrDefault(uuid, false);
        s.hasVip = hasVipMap.getOrDefault(uuid, false);
        
        Location[] homes = playerHomes.get(uuid);
        int hCount = 0;
        if (homes != null) {
            for (Location loc : homes) {
                if (loc != null) hCount++;
            }
        }
        s.homesCount = hCount;
        
        int maxH = 5;
        if (s.hasErpProMax) maxH = 45;
        else if (s.hasErpPro) maxH = 27;
        else if (s.hasErpPlus) maxH = 13;
        s.maxHomes = maxH;
        return s;
    }

    private PlayerStatsSnapshot loadOfflinePlayerSnapshot(String targetSearchName) {
        synchronized (dbLock) {
            try (Connection conn = getConnection()) {
                if (conn == null) return null;
                
                String altName = targetSearchName.startsWith(".") ? targetSearchName.substring(1) : "." + targetSearchName;
                String query = "SELECT * FROM player_stats WHERE lower(lastKnownName) = lower(?) OR lower(lastKnownName) = lower(?) OR uuid = ? ORDER BY CASE WHEN lower(lastKnownName) = lower(?) THEN 0 ELSE 1 END LIMIT 1";
                UUID parsedUuid = null;
                try {
                    parsedUuid = UUID.fromString(targetSearchName);
                } catch (Exception ignored) {}
                
                UUID resolvedUuid = null;
                PlayerStatsSnapshot s = new PlayerStatsSnapshot();
                
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    ps.setString(1, targetSearchName);
                    ps.setString(2, altName);
                    ps.setString(3, parsedUuid != null ? parsedUuid.toString() : targetSearchName);
                    ps.setString(4, targetSearchName);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            resolvedUuid = UUID.fromString(rs.getString("uuid"));
                            s.uuid = resolvedUuid;
                            s.name = rs.getString("lastKnownName");
                            if (s.name == null || s.name.isEmpty()) s.name = targetSearchName;
                            s.isOnline = false;
                            s.timePlayed = rs.getInt("timePlayed");
                            s.erpies = rs.getLong("erpies");
                            s.derpies = rs.getLong("derpies");
                            s.keys = rs.getInt("keys");
                            s.kills = rs.getInt("kills");
                            s.deaths = rs.getInt("deaths");
                            s.regularKeys = rs.getInt("regularKeys");
                            s.crimsonKeys = rs.getInt("crimsonKeys");
                            s.echoKeys = rs.getInt("echoKeys");
                            s.endKeys = rs.getInt("endKeys");
                            s.amethystKeys = rs.getInt("amethystKeys");
                            s.bankErpies = rs.getLong("bankErpies");
                            s.bankDerpies = rs.getLong("bankDerpies");
                            s.oresMined = rs.getInt("oresMined");
                            s.invisibleKills = rs.getInt("invisibleKills");
                            s.blocksPlaced = rs.getInt("blocksPlaced");
                            s.starvationDeaths = rs.getInt("starvationDeaths");
                            s.apocalypseZombieKills = rs.getInt("apocalypseZombieKills");
                            s.hasErpPlus = rs.getInt("hasErpPlus") == 1;
                            s.hasErpPro = rs.getInt("hasErpPro") == 1;
                            s.hasErpProMax = rs.getInt("hasErpProMax") == 1;
                            s.hasVip = rs.getInt("hasVip") == 1;
                        }
                    }
                }
                
                if (resolvedUuid == null) {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(targetSearchName);
                    if (op == null || (!op.hasPlayedBefore() && op.getName() == null)) {
                        op = Bukkit.getOfflinePlayer(altName);
                    }
                    if (op != null && (op.hasPlayedBefore() || op.getName() != null)) {
                        resolvedUuid = op.getUniqueId();
                        String opName = op.getName() != null ? op.getName() : targetSearchName;
                        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM player_stats WHERE uuid = ?")) {
                            ps.setString(1, resolvedUuid.toString());
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    s.uuid = resolvedUuid;
                                    s.name = rs.getString("lastKnownName") != null ? rs.getString("lastKnownName") : opName;
                                    s.isOnline = false;
                                    s.timePlayed = rs.getInt("timePlayed");
                                    s.erpies = rs.getLong("erpies");
                                    s.derpies = rs.getLong("derpies");
                                    s.keys = rs.getInt("keys");
                                    s.kills = rs.getInt("kills");
                                    s.deaths = rs.getInt("deaths");
                                    s.regularKeys = rs.getInt("regularKeys");
                                    s.crimsonKeys = rs.getInt("crimsonKeys");
                                    s.echoKeys = rs.getInt("echoKeys");
                                    s.endKeys = rs.getInt("endKeys");
                                    s.amethystKeys = rs.getInt("amethystKeys");
                                    s.bankErpies = rs.getLong("bankErpies");
                                    s.bankDerpies = rs.getLong("bankDerpies");
                                    s.oresMined = rs.getInt("oresMined");
                                    s.invisibleKills = rs.getInt("invisibleKills");
                                    s.blocksPlaced = rs.getInt("blocksPlaced");
                                    s.starvationDeaths = rs.getInt("starvationDeaths");
                                    s.apocalypseZombieKills = rs.getInt("apocalypseZombieKills");
                                    s.hasErpPlus = rs.getInt("hasErpPlus") == 1;
                                    s.hasErpPro = rs.getInt("hasErpPro") == 1;
                                    s.hasErpProMax = rs.getInt("hasErpProMax") == 1;
                                    s.hasVip = rs.getInt("hasVip") == 1;
                                } else {
                                    return null;
                                }
                            }
                        }
                    } else {
                        return null;
                    }
                }
                
                int homesCount = 0;
                try (PreparedStatement psHomes = conn.prepareStatement("SELECT count(*) FROM player_homes WHERE uuid = ?")) {
                    psHomes.setString(1, s.uuid.toString());
                    try (ResultSet rs = psHomes.executeQuery()) {
                        if (rs.next()) {
                            homesCount = rs.getInt(1);
                        }
                    }
                }
                s.homesCount = homesCount;
                
                int maxH = 5;
                if (s.hasErpProMax) maxH = 45;
                else if (s.hasErpPro) maxH = 27;
                else if (s.hasErpPlus) maxH = 13;
                s.maxHomes = maxH;
                
                return s;
            } catch (Exception e) {
                getLogger().severe("Failed to load offline player stats for " + targetSearchName);
                e.printStackTrace();
                return null;
            }
        }
    }

    private ItemStack createStatsItem(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            List<Component> finalLore = new ArrayList<>();
            for (Component c : lore) {
                finalLore.add(c.decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(finalLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void openStatsGuiWithSnapshot(Player viewer, PlayerStatsSnapshot s) {
        String titleStr = s.name + "'s Stats";
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(titleStr));
        
        ItemStack borderPane = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ", NamedTextColor.DARK_GRAY);
        ItemStack accentPane = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.GRAY);
        
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, borderPane);
        }
        int[] accents = {9, 17, 18, 26, 27, 35, 36, 44};
        for (int slot : accents) {
            inv.setItem(slot, accentPane);
        }
        
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
        if (skullMeta != null) {
            try {
                skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(s.uuid));
            } catch (Exception ignored) {}
            skullMeta.displayName(Component.text(s.name + "'s Profile", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            
            String rankName = "Member";
            NamedTextColor rColor = NamedTextColor.GRAY;
            if (s.hasErpProMax) {
                rankName = "ERP+++ (Pro Max)";
                rColor = NamedTextColor.LIGHT_PURPLE;
            } else if (s.hasErpPro) {
                rankName = "ERP++ (Pro)";
                rColor = NamedTextColor.AQUA;
            } else if (s.hasErpPlus) {
                rankName = "ERP+ (Plus)";
                rColor = NamedTextColor.GREEN;
            } else if (s.hasVip) {
                rankName = "VIP";
                rColor = NamedTextColor.GOLD;
            }
            
            double kd = s.deaths == 0 ? s.kills : ((double) s.kills / (double) s.deaths);
            
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
            lore.add(Component.text("§7Status: ").append(s.isOnline ? Component.text("● Online", NamedTextColor.GREEN) : Component.text("○ Offline", NamedTextColor.GRAY)));
            lore.add(Component.text("§7Rank: ").append(Component.text(rankName, rColor)));
            lore.add(Component.text("§7Playtime: §f" + formatTimePlayed(s.timePlayed)));
            lore.add(Component.text("§7K/D Ratio: §b" + String.format("%.2f", kd)));
            lore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
            
            List<Component> cleanLore = new ArrayList<>();
            for (Component c : lore) cleanLore.add(c.decoration(TextDecoration.ITALIC, false));
            skullMeta.lore(cleanLore);
            skull.setItemMeta(skullMeta);
        }
        inv.setItem(4, skull);
        
        List<Component> combatLore = new ArrayList<>();
        combatLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        combatLore.add(Component.text("§7Player Kills: §c" + s.kills));
        combatLore.add(Component.text("§7Invisible Kills: §6" + s.invisibleKills));
        combatLore.add(Component.text("§7Zombie Kills: §e" + s.apocalypseZombieKills));
        combatLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        inv.setItem(10, createStatsItem(Material.NETHERITE_SWORD, Component.text("⚔ Combat & Kills", NamedTextColor.RED, TextDecoration.BOLD), combatLore));
        
        double kd = s.deaths == 0 ? s.kills : ((double) s.kills / (double) s.deaths);
        List<Component> deathLore = new ArrayList<>();
        deathLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        deathLore.add(Component.text("§7Total Deaths: §c" + s.deaths));
        deathLore.add(Component.text("§7Starvation Deaths: §e" + s.starvationDeaths));
        deathLore.add(Component.text("§7K/D Ratio: §b" + String.format("%.2f", kd)));
        deathLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        inv.setItem(11, createStatsItem(Material.SKELETON_SKULL, Component.text("☠ Deaths & K/D", NamedTextColor.DARK_RED, TextDecoration.BOLD), deathLore));
        
        List<Component> timeLore = new ArrayList<>();
        timeLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        timeLore.add(Component.text("§7Total Playtime: §f" + formatTimePlayed(s.timePlayed)));
        timeLore.add(Component.text("§7Raw Seconds: §7" + s.timePlayed + "s"));
        timeLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        inv.setItem(13, createStatsItem(Material.CLOCK, Component.text("⏱ Time Played", NamedTextColor.YELLOW, TextDecoration.BOLD), timeLore));
        
        String rName = "Member";
        NamedTextColor rColor = NamedTextColor.GRAY;
        if (s.hasErpProMax) {
            rName = "ERP+++ (Pro Max)";
            rColor = NamedTextColor.LIGHT_PURPLE;
        } else if (s.hasErpPro) {
            rName = "ERP++ (Pro)";
            rColor = NamedTextColor.AQUA;
        } else if (s.hasErpPlus) {
            rName = "ERP+ (Plus)";
            rColor = NamedTextColor.GREEN;
        } else if (s.hasVip) {
            rName = "VIP";
            rColor = NamedTextColor.GOLD;
        }
        List<Component> rankLore = new ArrayList<>();
        rankLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        rankLore.add(Component.text("§7Store Rank: ").append(Component.text(rName, rColor)));
        rankLore.add(Component.text("§7VIP Status: ").append(s.hasVip ? Component.text("Active", NamedTextColor.GOLD) : Component.text("None", NamedTextColor.GRAY)));
        rankLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        inv.setItem(15, createStatsItem(Material.NETHER_STAR, Component.text("★ Membership Rank", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD), rankLore));
        
        List<Component> erpiesLore = new ArrayList<>();
        erpiesLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        erpiesLore.add(Component.text("§7Wallet Balance: §a" + formatValue(s.erpies) + " Erpies §8(" + s.erpies + ")"));
        erpiesLore.add(Component.text("§7Bank Balance: §2" + formatValue(s.bankErpies) + " Erpies"));
        erpiesLore.add(Component.text("§7Total Net Worth: §e" + formatValue(s.erpies + s.bankErpies) + " Erpies"));
        erpiesLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        inv.setItem(19, createStatsItem(Material.EMERALD, Component.text("⛃ Erpies Balance", NamedTextColor.GREEN, TextDecoration.BOLD), erpiesLore));
        
        List<Component> derpiesLore = new ArrayList<>();
        derpiesLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        derpiesLore.add(Component.text("§7Wallet Balance: §d" + formatValue(s.derpies) + " Derpies §8(" + s.derpies + ")"));
        derpiesLore.add(Component.text("§7Bank Balance: §5" + formatValue(s.bankDerpies) + " Derpies"));
        derpiesLore.add(Component.text("§7Total Net Worth: §e" + formatValue(s.derpies + s.bankDerpies) + " Derpies"));
        derpiesLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        inv.setItem(21, createStatsItem(Material.AMETHYST_SHARD, Component.text("✦ Derpies Balance", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD), derpiesLore));
        
        List<Component> bankLore = new ArrayList<>();
        bankLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        bankLore.add(Component.text("§7Deposited Erpies: §a" + formatValue(s.bankErpies)));
        bankLore.add(Component.text("§7Deposited Derpies: §d" + formatValue(s.bankDerpies)));
        bankLore.add(Component.text("§7Daily Interest: §e+5% every 24h"));
        bankLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        inv.setItem(25, createStatsItem(Material.ENDER_CHEST, Component.text("🏦 Bank Account", NamedTextColor.GOLD, TextDecoration.BOLD), bankLore));
        
        List<Component> kLore = new ArrayList<>();
        kLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        kLore.add(Component.text("§7Standard Crate Keys: §b" + s.keys));
        kLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        inv.setItem(29, createStatsItem(Material.TRIPWIRE_HOOK, Component.text("🗝 Regular Keys: " + s.keys, NamedTextColor.AQUA, TextDecoration.BOLD), kLore));
        
        List<Component> echoLore = new ArrayList<>();
        echoLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        echoLore.add(Component.text("§7Echo Crate Keys: §3" + s.echoKeys));
        echoLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        inv.setItem(30, createStatsItem(Material.ECHO_SHARD, Component.text("✧ Echo Keys: " + s.echoKeys, NamedTextColor.DARK_AQUA, TextDecoration.BOLD), echoLore));
        
        List<Component> crimLore = new ArrayList<>();
        crimLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        crimLore.add(Component.text("§7Crimson Crate Keys: §c" + s.crimsonKeys));
        crimLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        inv.setItem(31, createStatsItem(Material.CRIMSON_FUNGUS, Component.text("✦ Crimson Keys: " + s.crimsonKeys, NamedTextColor.RED, TextDecoration.BOLD), crimLore));
        
        List<Component> endLore = new ArrayList<>();
        endLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        endLore.add(Component.text("§7End Dimension Keys: §5" + s.endKeys));
        endLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        inv.setItem(32, createStatsItem(Material.ENDER_PEARL, Component.text("✴ End Keys: " + s.endKeys, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD), endLore));
        
        List<Component> ametLore = new ArrayList<>();
        ametLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        ametLore.add(Component.text("§7Amethyst Crate Keys: §d" + s.amethystKeys));
        ametLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        inv.setItem(33, createStatsItem(Material.AMETHYST_CLUSTER, Component.text("💠 Amethyst Keys: " + s.amethystKeys, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD), ametLore));
        
        List<Component> oresLore = new ArrayList<>();
        oresLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        oresLore.add(Component.text("§7Total Ores Mined: §b" + s.oresMined));
        oresLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        inv.setItem(38, createStatsItem(Material.DIAMOND_PICKAXE, Component.text("⛏ Ores Mined", NamedTextColor.AQUA, TextDecoration.BOLD), oresLore));
        
        List<Component> blocksLore = new ArrayList<>();
        blocksLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        blocksLore.add(Component.text("§7Total Blocks Placed: §6" + s.blocksPlaced));
        blocksLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        inv.setItem(40, createStatsItem(Material.BRICKS, Component.text("🧱 Blocks Placed", NamedTextColor.GOLD, TextDecoration.BOLD), blocksLore));
        
        List<Component> homesLore = new ArrayList<>();
        homesLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        homesLore.add(Component.text("§7Saved Homepoints: §a" + s.homesCount + " §7/ §e" + s.maxHomes));
        homesLore.add(Component.text("§8━━━━━━━━━━━━━━━━━━━━━━"));
        inv.setItem(42, createStatsItem(Material.RED_BED, Component.text("🏠 Saved Homes", NamedTextColor.GREEN, TextDecoration.BOLD), homesLore));
        
        List<Component> closeLore = new ArrayList<>();
        closeLore.add(Component.text("§7Click to exit stats view"));
        inv.setItem(49, createStatsItem(Material.BARRIER, Component.text("❌ Close", NamedTextColor.RED, TextDecoration.BOLD), closeLore));
        
        viewer.openInventory(inv);
    }

    private void openPlayerStatsGui(Player viewer, String targetSearchName) {
        if (targetSearchName == null || targetSearchName.trim().isEmpty()) {
            targetSearchName = viewer.getName();
        }
        
        Player onlineTarget = Bukkit.getPlayerExact(targetSearchName);
        if (onlineTarget == null) {
            onlineTarget = Bukkit.getPlayer(targetSearchName);
        }
        if (onlineTarget == null) {
            String altName = targetSearchName.startsWith(".") ? targetSearchName.substring(1) : "." + targetSearchName;
            onlineTarget = Bukkit.getPlayer(altName);
        }
        if (onlineTarget != null) {
            PlayerStatsSnapshot snapshot = getOnlinePlayerSnapshot(onlineTarget);
            openStatsGuiWithSnapshot(viewer, snapshot);
            return;
        }
        
        final String finalTargetName = targetSearchName;
        viewer.sendMessage(Component.text("🔍 Looking up stats for '" + finalTargetName + "'...", NamedTextColor.GRAY));
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            PlayerStatsSnapshot snapshot = loadOfflinePlayerSnapshot(finalTargetName);
            Bukkit.getScheduler().runTask(this, () -> {
                if (!viewer.isOnline()) return;
                if (snapshot == null) {
                    viewer.sendMessage(Component.text("❌ Player '" + finalTargetName + "' not found!", NamedTextColor.RED));
                    return;
                }
                openStatsGuiWithSnapshot(viewer, snapshot);
            });
        });
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

    private void openDepositOptionsGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Deposit Options"));
        ItemStack pane = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.GRAY);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, pane);
        }
        inv.setItem(11, createGuiItem(Material.CHEST, "Deposit Items", NamedTextColor.GREEN, "Click to deposit items"));
        inv.setItem(15, createGuiItem(Material.OAK_SIGN, "Deposit Money", NamedTextColor.GOLD, "Click to deposit Erpies/Derpies"));
        player.openInventory(inv);
    }

    private void openWithdrawOptionsGui(Player player) {
        UUID uuid = player.getUniqueId();
        openedWithdrawViaCommand.add(uuid);
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Withdraw Options"));
        ItemStack pane = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.GRAY);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, pane);
        }
        inv.setItem(11, createGuiItem(Material.ENDER_CHEST, "Withdraw Items", NamedTextColor.AQUA, "Click to withdraw items"));
        inv.setItem(15, createGuiItem(Material.GOLD_INGOT, "Withdraw Money", NamedTextColor.GOLD, "Click to withdraw money"));
        player.openInventory(inv);
    }

    private void openWithdrawMoneyGui(Player player) {
        UUID uuid = player.getUniqueId();
        applyInterest(uuid);
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Withdraw Money Options"));
        ItemStack pane = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.GRAY);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, pane);
        }
        long erpies = bankErpiesMap.getOrDefault(uuid, 0L);
        long derpies = bankDerpiesMap.getOrDefault(uuid, 0L);

        inv.setItem(11, createGuiItem(Material.GOLD_BLOCK, "Withdraw Erpies", NamedTextColor.GOLD, "Deposited: " + erpies + " Erpies", "Click to withdraw"));
        inv.setItem(13, createGuiItem(Material.DIAMOND_BLOCK, "Withdraw Derpies", NamedTextColor.AQUA, "Deposited: " + derpies + " Derpies", "Click to withdraw"));
        inv.setItem(15, createGuiItem(Material.ARROW, "Back to Withdraw Menu", NamedTextColor.YELLOW, "Click to go back"));
        player.openInventory(inv);
    }

    private void openMobGeneratorTypeSelectionGui(Player player, Location loc) {
        activeMobGeneratorSetup.put(player.getUniqueId(), loc);
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("Choose Spawner Type"));
        inv.setItem(0, createGuiItem(Material.IRON_BLOCK, "Iron Golem", NamedTextColor.GRAY, "Click to select Iron Golem"));
        inv.setItem(2, createGuiItem(Material.CARVED_PUMPKIN, "Snow Golem", NamedTextColor.WHITE, "Click to select Snow Golem"));
        inv.setItem(4, createGuiItem(Material.GUNPOWDER, "Creeper", NamedTextColor.GREEN, "Click to select Creeper"));
        inv.setItem(6, createGuiItem(Material.BONE, "Skeleton", NamedTextColor.YELLOW, "Click to select Skeleton"));
        inv.setItem(8, createGuiItem(Material.POTION, "Witch", NamedTextColor.LIGHT_PURPLE, "Click to select Witch"));
        player.openInventory(inv);
    }

    private ItemStack createMobGeneratorItem() {
        ItemStack item = new ItemStack(Material.SPAWNER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Mob Generator", NamedTextColor.RED, net.kyori.adventure.text.format.TextDecoration.BOLD));
            meta.lore(List.of(
                Component.text("A custom spawner generator.", NamedTextColor.YELLOW),
                Component.text("1. Place on the ground.", NamedTextColor.GRAY),
                Component.text("2. Right-click to configure spawner type.", NamedTextColor.GRAY),
                Component.text("3. Sneak + left-click block to break.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "mob_generator");
            item.setItemMeta(meta);
        }
        return item;
    }

    private int getRankWeight(UUID uuid) {
        if (hasErpProMaxMap.getOrDefault(uuid, false)) return 4;
        if (hasErpProMap.getOrDefault(uuid, false)) return 3;
        if (hasErpPlusMap.getOrDefault(uuid, false)) return 2;
        if (hasVipMap.getOrDefault(uuid, false)) return 1;
        return 0;
    }

    private int getRankWeightByName(String rankName) {
        if (rankName == null) return 0;
        String r = rankName.toLowerCase().trim().replace(" ", "");
        if (r.equals("e+pm") || r.equals("e+++") || r.equals("erp+++") || r.equals("erppromax") || r.equals("erp+promax") || r.equals("erpiepromaxx") || r.equals("erppromaxx")) return 4;
        if (r.equals("e+p") || r.equals("e++") || r.equals("erp++") || r.equals("erppro") || r.equals("erp+pro") || r.equals("erpiepro")) return 3;
        if (r.equals("e+") || r.equals("e") || r.equals("erp+") || r.equals("erpplus") || r.equals("erp+plus") || r.equals("erp") || r.equals("erpie")) return 2;
        if (r.equals("vip")) return 1;
        return 0;
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
        UUID uuid = player.getUniqueId();
        orderBoardSearchQuery.put(uuid, query);
        for (OrderRequest order : orders) {
            Material mat = Material.matchMaterial(order.itemName);
            if (mat == null) mat = Material.PAPER;
            if (query != null && !order.itemName.toLowerCase().contains(query.toLowerCase()) && !formatItemDisplayName(mat).toLowerCase().contains(query.toLowerCase())) continue;

            boolean isOwn = order.buyer.equals(uuid);
            ItemStack display = new ItemStack(mat);
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(formatItemDisplayName(mat), isOwn ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Buyer: " + (isOwn ? "You (" + order.buyerName + ")" : order.buyerName), isOwn ? NamedTextColor.GREEN : NamedTextColor.GRAY));
                lore.add(Component.text("Wants: " + String.format("%,d", order.quantity) + "x " + formatItemDisplayName(mat), NamedTextColor.WHITE));
                lore.add(Component.text("Paying: " + String.format("%,d", order.price) + " Erpies", NamedTextColor.GOLD));
                if (isOwn) {
                    lore.add(Component.text("§a✦ YOUR POSTED ORDER ✦"));
                    lore.add(Component.text("§cClick to cancel & refund your Erpies"));
                } else {
                    lore.add(Component.text("§aClick to fulfill (needs item in inv)"));
                }
                meta.lore(lore);
                display.setItemMeta(meta);
            }
            inv.setItem(slot++, display);
            if (slot >= 45) break;
        }
        if (slot == 0) {
            inv.setItem(22, createGuiItem(Material.BARRIER, "No Orders Found", NamedTextColor.GRAY, (query != null ? "No orders match '" + query + "'." : "No orders have been posted yet."), "Click 'Post Order' below to request an item!"));
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
                meta.displayName(Component.text(formatItemDisplayName(mat), NamedTextColor.GREEN));
                meta.lore(List.of(
                    Component.text("Wants: " + String.format("%,d", order.quantity) + "x " + formatItemDisplayName(mat), NamedTextColor.WHITE),
                    Component.text("Paying: " + String.format("%,d", order.price) + " Erpies", NamedTextColor.GOLD),
                    Component.text("§cClick to cancel & refund your Erpies", NamedTextColor.RED)
                ));
                display.setItemMeta(meta);
            }
            inv.setItem(slot++, display);
            if (slot >= 45) break;
        }
        if (slot == 0) {
            inv.setItem(22, createGuiItem(Material.BARRIER, "No Active Orders", NamedTextColor.GRAY, "You don't have any active buy orders.", "Click 'Back to Order Board' and then 'Post Order'!"));
        }
        inv.setItem(49, createGuiItem(Material.BARRIER, "Back to Order Board", NamedTextColor.RED, "Return to main page"));
        player.openInventory(inv);
    }

    // --- Redesigned Order Creation System ---

    public static final List<Material> ORDERABLE_ITEMS = new ArrayList<>();
    static {
        for (Material mat : Material.values()) {
            if (!mat.isItem()) continue;
            if (mat.isAir()) continue;
            if (mat.isLegacy()) continue;
            String name = mat.name();
            if (name.contains("SPAWN_EGG")) continue;
            if (name.contains("COMMAND_BLOCK")) continue;
            if (name.contains("STRUCTURE_")) continue;
            if (name.contains("INFESTED_")) continue;
            if (name.equals("BEDROCK") || name.equals("BARRIER") || name.equals("LIGHT") ||
                name.equals("DEBUG_STICK") || name.equals("KNOWLEDGE_BOOK") || name.equals("JIGSAW") ||
                name.equals("TEST_BLOCK") || name.equals("BUNDLE")) continue;
            ORDERABLE_ITEMS.add(mat);
        }
        ORDERABLE_ITEMS.sort(Comparator.comparing(CustomScoreboard::formatItemDisplayName));
    }

    public static String formatItemDisplayName(Material mat) {
        if (mat == null) return "Unknown";
        if (mat == Material.AMETHYST_BLOCK) return "Block of Amethyst";
        String name = mat.name().toLowerCase();
        String[] parts = name.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            String part = parts[i];
            if (i > 0 && (part.equals("of") || part.equals("with") || part.equals("and") || part.equals("the"))) {
                sb.append(part);
            } else {
                sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
            if (i < parts.length - 1) sb.append(" ");
        }
        return sb.toString();
    }

    public static long getItemWorth(Material mat) {
        if (mat == null) return 10;
        String name = mat.name();

        // Exact screenshot match: Acacia Fence Gate -> $ 12
        if (name.equals("ACACIA_FENCE_GATE")) return 12;

        // Wood products / fence gates / fences / doors / signs / boats
        if (name.endsWith("_FENCE_GATE")) return 12;
        if (name.endsWith("_FENCE")) return 8;
        if (name.endsWith("_DOOR")) return 10;
        if (name.endsWith("_TRAPDOOR")) return 10;
        if (name.endsWith("_BOAT")) return 15;
        if (name.endsWith("_BOAT_WITH_CHEST") || name.endsWith("_CHEST_BOAT")) return 25;
        if (name.endsWith("_BUTTON")) return 2;
        if (name.endsWith("_PRESSURE_PLATE")) return 5;
        if (name.endsWith("_STAIRS")) return 6;
        if (name.endsWith("_SLAB")) return 4;
        if (name.endsWith("_SIGN")) return 8;
        if (name.endsWith("_HANGING_SIGN")) return 12;
        if (name.endsWith("_LOG") || name.endsWith("_WOOD") || name.endsWith("_STEM") || name.endsWith("_HYPHAE")) return 10;
        if (name.endsWith("_PLANKS")) return 3;
        if (name.endsWith("_SAPLING") || name.endsWith("_PROPAGULE")) return 10;
        if (name.endsWith("_LEAVES")) return 5;

        // Netherite items
        if (name.equals("NETHERITE_UPGRADE_SMITHING_TEMPLATE")) return 5000;
        if (name.equals("NETHERITE_BLOCK")) return 27000;
        if (name.equals("NETHERITE_INGOT")) return 3000;
        if (name.equals("NETHERITE_SCRAP")) return 750;
        if (name.equals("ANCIENT_DEBRIS")) return 800;
        if (name.startsWith("NETHERITE_")) return 4000;

        // Diamonds
        if (name.equals("DIAMOND_BLOCK")) return 2700;
        if (name.equals("DIAMOND")) return 300;
        if (name.startsWith("DIAMOND_")) return 600;

        // Gold
        if (name.equals("GOLD_BLOCK")) return 675;
        if (name.equals("GOLD_INGOT")) return 75;
        if (name.equals("GOLD_NUGGET")) return 8;
        if (name.startsWith("GOLDEN_")) return 150;

        // Iron
        if (name.equals("IRON_BLOCK")) return 450;
        if (name.equals("IRON_INGOT")) return 50;
        if (name.equals("IRON_NUGGET")) return 5;
        if (name.startsWith("IRON_")) return 100;

        // Copper
        if (name.equals("COPPER_BLOCK")) return 180;
        if (name.equals("COPPER_INGOT")) return 20;

        // Rare / Special items
        if (name.equals("ELYTRA")) return 10000;
        if (name.equals("NETHER_STAR")) return 8000;
        if (name.equals("BEACON")) return 10000;
        if (name.equals("TOTEM_OF_UNDYING")) return 1500;
        if (name.equals("ENCHANTED_GOLDEN_APPLE")) return 5000;
        if (name.equals("GOLDEN_APPLE")) return 250;
        if (name.equals("SHULKER_BOX") || name.endsWith("_SHULKER_BOX")) return 800;
        if (name.equals("SHULKER_SHELL")) return 400;
        if (name.equals("TRIDENT")) return 3500;
        if (name.equals("HEAVY_CORE")) return 15000;
        if (name.equals("MACE")) return 20000;
        if (name.equals("HEART_OF_THE_SEA")) return 2500;
        if (name.equals("NAUTILUS_SHELL")) return 200;
        if (name.equals("ENDER_PEARL")) return 75;
        if (name.equals("ENDER_EYE")) return 100;
        if (name.equals("END_CRYSTAL")) return 500;
        if (name.equals("RESPAWN_ANCHOR")) return 500;
        if (name.equals("OBSIDIAN") || name.equals("CRYING_OBSIDIAN")) return 50;
        if (name.equals("WIND_CHARGE")) return 75;
        if (name.equals("BREEZE_ROD")) return 250;
        if (name.equals("BLAZE_ROD")) return 50;
        if (name.equals("BLAZE_POWDER")) return 25;
        if (name.equals("GHAST_TEAR")) return 150;
        if (name.equals("WITHER_SKELETON_SKULL")) return 2000;
        if (name.equals("DRAGON_BREATH")) return 100;
        if (name.equals("DRAGON_EGG")) return 50000;
        if (name.equals("EXPERIENCE_BOTTLE")) return 50;
        if (name.equals("NAME_TAG")) return 150;
        if (name.equals("SADDLE")) return 150;
        if (name.equals("LEAD")) return 20;

        // Food & consumables
        if (name.equals("ENCHANTED_GOLDEN_CARROT")) return 500;
        if (name.equals("GOLDEN_CARROT")) return 50;
        if (name.equals("COOKED_BEEF") || name.equals("COOKED_PORKCHOP")) return 20;
        if (name.equals("BREAD")) return 10;
        if (name.equals("APPLE")) return 10;
        if (name.equals("CARROT") || name.equals("POTATO") || name.equals("BAKED_POTATO")) return 5;

        // Amethyst & ores
        if (name.equals("AMETHYST_CLUSTER")) return 60;
        if (name.equals("AMETHYST_SHARD")) return 25;
        if (name.equals("BLOCK_OF_AMETHYST")) return 100;
        if (name.equals("COAL")) return 10;
        if (name.equals("COAL_BLOCK")) return 90;
        if (name.equals("LAPIS_LAZULI")) return 15;
        if (name.equals("LAPIS_BLOCK")) return 135;
        if (name.equals("REDSTONE")) return 15;
        if (name.equals("REDSTONE_BLOCK")) return 135;
        if (name.equals("EMERALD")) return 50;
        if (name.equals("EMERALD_BLOCK")) return 450;

        // Default based on material properties
        if (mat.isEdible()) return 15;
        if (name.contains("RAW_")) return 30;
        if (name.contains("ORE")) return 50;
        if (name.contains("CORAL")) return 40;
        if (name.contains("POTTERY_SHERD")) return 100;
        if (name.contains("ARMOR_TRIM")) return 1000;
        if (name.contains("BANNER_PATTERN")) return 500;
        if (name.contains("MUSIC_DISC")) return 1500;
        if (name.contains("BUCKET")) return 60;

        return 20;
    }

    private void openChooseItemFlow(Player player, int page, String query) {
        if (!isBedrockPlayer(player)) {
            try {
                openChooseItemDialog(player, query);
                return;
            } catch (Throwable ignored) {
                // Fallback to chest GUI if client doesn't support dialogs
            }
        }
        openChooseItemChestGui(player, page, query);
    }

    private void openChooseItemDialog(Player player, String query) {
        List<Material> filtered = new ArrayList<>();
        String q = (query != null) ? query.trim().toLowerCase() : "";
        for (Material mat : ORDERABLE_ITEMS) {
            String dName = formatItemDisplayName(mat);
            if (q.isEmpty() || dName.toLowerCase().contains(q) || mat.name().toLowerCase().contains(q)) {
                filtered.add(mat);
            }
        }

        Dialog dialog = Dialog.create(factory -> {
            var builder = factory.empty();
            builder.base(DialogBase.builder(Component.text("Choose Item ⚠", NamedTextColor.WHITE))
                .inputs(List.of(
                    DialogInput.text("search", Component.text("Search"))
                        .initial(query != null ? query : "")
                        .build()
                ))
                .build());

            List<ActionButton> actions = new ArrayList<>();

            // 1. Search button
            actions.add(ActionButton.create(
                Component.text("Search", NamedTextColor.WHITE),
                Component.text("Click to filter items"),
                100,
                DialogAction.customClick((view, aud) -> {
                    if (aud instanceof Player p) {
                        String inputQuery = view.getText("search");
                        Bukkit.getScheduler().runTask(this, () -> openChooseItemFlow(p, 0, (inputQuery != null && !inputQuery.trim().isEmpty()) ? inputQuery.trim() : null));
                    }
                }, null)
            ));

            // 2. Action buttons for filtered items (first 100)
            int count = 0;
            for (Material mat : filtered) {
                if (count++ >= 100) break;
                String displayName = formatItemDisplayName(mat);
                long worth = getItemWorth(mat);
                Component tooltip = Component.text(displayName, NamedTextColor.WHITE)
                    .append(Component.newline())
                    .append(Component.text("Worth: ", NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text("$ ", NamedTextColor.GREEN))
                    .append(Component.text(worth, NamedTextColor.WHITE));

                actions.add(ActionButton.create(
                    Component.text(displayName, NamedTextColor.WHITE),
                    tooltip,
                    120,
                    DialogAction.customClick((view, aud) -> {
                        if (aud instanceof Player p) {
                            Bukkit.getScheduler().runTask(this, () -> {
                                pendingOrderItemName.put(p.getUniqueId(), mat.name());
                                openChooseQuantityGui(p, mat);
                            });
                        }
                    }, null)
                ));
            }

            ActionButton cancelBtn = ActionButton.create(
                Component.text("Cancel!", NamedTextColor.RED),
                Component.text("Cancel order creation"),
                100,
                DialogAction.customClick((view, aud) -> {
                    if (aud instanceof Player p) {
                        Bukkit.getScheduler().runTask(this, () -> openOrdersGui(p, null));
                    }
                }, null)
            );

            builder.type(DialogType.multiAction(actions, cancelBtn, 4));
        });

        ((net.kyori.adventure.audience.Audience) player).showDialog(dialog);
    }

    private void openChooseItemChestGui(Player player, int page, String query) {
        pendingOrderPage.put(player.getUniqueId(), page);
        pendingOrderSearchQuery.put(player.getUniqueId(), query);

        List<Material> filtered = new ArrayList<>();
        String q = (query != null) ? query.trim().toLowerCase() : "";
        for (Material mat : ORDERABLE_ITEMS) {
            String dName = formatItemDisplayName(mat);
            if (q.isEmpty() || dName.toLowerCase().contains(q) || mat.name().toLowerCase().contains(q)) {
                filtered.add(mat);
            }
        }

        int pageSize = 45;
        int totalItems = filtered.size();
        int maxPages = Math.max(1, (int) Math.ceil((double) totalItems / pageSize));
        int currentPage = Math.max(0, Math.min(page, maxPages - 1));
        pendingOrderPage.put(player.getUniqueId(), currentPage);

        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Choose Item ⚠"));

        int start = currentPage * pageSize;
        int end = Math.min(start + pageSize, totalItems);
        int slot = 0;
        for (int i = start; i < end; i++) {
            Material mat = filtered.get(i);
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String dName = formatItemDisplayName(mat);
                long worth = getItemWorth(mat);
                meta.displayName(Component.text(dName, NamedTextColor.WHITE));
                meta.lore(List.of(
                    Component.text("Worth: ", NamedTextColor.LIGHT_PURPLE)
                        .append(Component.text("$ ", NamedTextColor.GREEN))
                        .append(Component.text(worth, NamedTextColor.WHITE)),
                    Component.text("Click to choose this item", NamedTextColor.YELLOW)
                ));
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
        }

        // Fill row 6 with border and controls
        ItemStack pane = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.GRAY);
        for (int c = 45; c < 54; c++) {
            inv.setItem(c, pane);
        }

        if (currentPage > 0) {
            inv.setItem(45, createGuiItem(Material.ARROW, "Previous Page", NamedTextColor.YELLOW, "Go to page " + currentPage));
        }

        String searchLabel = (query != null && !query.isEmpty()) ? "Search: \"" + query + "\"" : "Search";
        inv.setItem(46, createGuiItem(Material.OAK_SIGN, searchLabel, NamedTextColor.AQUA, "Click to filter items by name"));

        inv.setItem(48, createGuiItem(Material.RED_CONCRETE, "Cancel!", NamedTextColor.RED, "Cancel order creation and return to Order Board"));

        inv.setItem(49, createGuiItem(Material.BOOK, "Page " + (currentPage + 1) + " / " + maxPages, NamedTextColor.GOLD, "Total items: " + totalItems));

        if (currentPage < maxPages - 1) {
            inv.setItem(53, createGuiItem(Material.ARROW, "Next Page", NamedTextColor.YELLOW, "Go to page " + (currentPage + 2)));
        }

        player.openInventory(inv);
    }

    private void openChooseQuantityGui(Player player, Material mat) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Choose Quantity (Max 1M)"));
        ItemStack pane = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.GRAY);
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        String dName = formatItemDisplayName(mat);
        long worth = getItemWorth(mat);

        // Slot 4: Item info
        ItemStack itemDisplay = new ItemStack(mat);
        ItemMeta meta = itemDisplay.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Selected: " + dName, NamedTextColor.GOLD));
            meta.lore(List.of(
                Component.text("Unit Worth: ", NamedTextColor.GRAY)
                    .append(Component.text("$ ", NamedTextColor.GREEN))
                    .append(Component.text(worth, NamedTextColor.WHITE)),
                Component.text("Maximum Limit: ", NamedTextColor.RED)
                    .append(Component.text("1,000,000 (1M)", NamedTextColor.YELLOW)),
                Component.text("Click a preset below or enter custom quantity", NamedTextColor.YELLOW)
            ));
            itemDisplay.setItemMeta(meta);
        }
        inv.setItem(4, itemDisplay);

        // Preset quantity buttons
        inv.setItem(10, createGuiItem(Material.IRON_NUGGET, "1x", NamedTextColor.WHITE, "Order 1 item", "Cost approx: " + worth + " Erpies"));
        inv.setItem(11, createGuiItem(Material.COPPER_INGOT, "16x", NamedTextColor.GOLD, "Order 16 items", "Cost approx: " + (worth * 16) + " Erpies"));
        inv.setItem(12, createGuiItem(Material.GOLD_INGOT, "64x (1 Stack)", NamedTextColor.YELLOW, "Order 64 items (1 full stack)", "Cost approx: " + (worth * 64) + " Erpies"));
        inv.setItem(13, createGuiItem(Material.DIAMOND, "576x (9 Stacks)", NamedTextColor.AQUA, "Order 576 items (9 stacks)", "Cost approx: " + (worth * 576) + " Erpies"));
        inv.setItem(14, createGuiItem(Material.EMERALD, "1,728x (1 Shulker)", NamedTextColor.GREEN, "Order 1,728 items (27 stacks)", "Cost approx: " + (worth * 1728) + " Erpies"));
        inv.setItem(15, createGuiItem(Material.NETHERITE_SCRAP, "10,000x (10k)", NamedTextColor.LIGHT_PURPLE, "Order 10,000 items", "Cost approx: " + (worth * 10000) + " Erpies"));
        inv.setItem(16, createGuiItem(Material.NETHERITE_INGOT, "100,000x (100k)", NamedTextColor.DARK_PURPLE, "Order 100,000 items", "Cost approx: " + (worth * 100000) + " Erpies"));

        inv.setItem(22, createGuiItem(Material.BEACON, "1,000,000x (1M Max)", NamedTextColor.GOLD, "Order 1,000,000 items (Maximum Limit)", "Cost approx: " + (worth * 1000000) + " Erpies"));

        inv.setItem(24, createGuiItem(Material.NAME_TAG, "Custom Quantity", NamedTextColor.YELLOW, "Click to type exact quantity", "Range: 1 to 1,000,000 (1M)"));

        inv.setItem(18, createGuiItem(Material.ARROW, "Back", NamedTextColor.GRAY, "Return to Choose Item"));
        inv.setItem(26, createGuiItem(Material.RED_CONCRETE, "Cancel!", NamedTextColor.RED, "Cancel and return to Order Board"));

        player.openInventory(inv);
    }

    private void openChoosePriceGui(Player player, Material mat, int quantity) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text("Choose Price for Order"));
        ItemStack pane = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.GRAY);
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        String dName = formatItemDisplayName(mat);
        long worth = getItemWorth(mat);
        long estWorth = worth * quantity;
        long playerBal = erpiesMap.getOrDefault(player.getUniqueId(), 0L);

        // Slot 4: Item info
        int stackIcon = Math.min(Math.max(1, quantity), mat.getMaxStackSize());
        ItemStack itemDisplay = new ItemStack(mat, stackIcon);
        ItemMeta meta = itemDisplay.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Order: " + String.format("%,d", quantity) + "x " + dName, NamedTextColor.GOLD));
            meta.lore(List.of(
                Component.text("Unit Worth: ", NamedTextColor.GRAY)
                    .append(Component.text("$ ", NamedTextColor.GREEN))
                    .append(Component.text(worth, NamedTextColor.WHITE)),
                Component.text("Total Est. Worth: ", NamedTextColor.GRAY)
                    .append(Component.text("$ ", NamedTextColor.GREEN))
                    .append(Component.text(String.format("%,d", estWorth), NamedTextColor.WHITE)),
                Component.text("Your Balance: ", NamedTextColor.GRAY)
                    .append(Component.text(String.format("%,d", playerBal) + " Erpies", NamedTextColor.GOLD)),
                Component.text("Select total price to offer sellers:", NamedTextColor.YELLOW)
            ));
            itemDisplay.setItemMeta(meta);
        }
        inv.setItem(4, itemDisplay);

        // Presets
        inv.setItem(11, createGuiItem(Material.EMERALD, "Est. Worth (" + String.format("%,d", estWorth) + " Erpies)", NamedTextColor.GREEN, "Pay market estimated price: " + String.format("%,d", estWorth) + " Erpies"));
        inv.setItem(12, createGuiItem(Material.GOLD_NUGGET, "100 Erpies", NamedTextColor.YELLOW, "Offer total: 100 Erpies"));
        inv.setItem(13, createGuiItem(Material.GOLD_INGOT, "1,000 Erpies (1k)", NamedTextColor.GOLD, "Offer total: 1,000 Erpies"));
        inv.setItem(14, createGuiItem(Material.DIAMOND, "10,000 Erpies (10k)", NamedTextColor.AQUA, "Offer total: 10,000 Erpies"));
        inv.setItem(15, createGuiItem(Material.NETHERITE_INGOT, "100,000 Erpies (100k)", NamedTextColor.LIGHT_PURPLE, "Offer total: 100,000 Erpies"));
        inv.setItem(20, createGuiItem(Material.NETHERITE_BLOCK, "500,000 Erpies (500k)", NamedTextColor.DARK_PURPLE, "Offer total: 500,000 Erpies"));
        inv.setItem(21, createGuiItem(Material.BEACON, "1,000,000 Erpies (1M)", NamedTextColor.GOLD, "Offer total: 1,000,000 Erpies"));

        inv.setItem(23, createGuiItem(Material.NAME_TAG, "Custom Price", NamedTextColor.YELLOW, "Click to type custom price", "Examples: 500, 10k, 1m"));

        inv.setItem(18, createGuiItem(Material.ARROW, "Back", NamedTextColor.GRAY, "Return to Choose Quantity"));
        inv.setItem(26, createGuiItem(Material.RED_CONCRETE, "Cancel!", NamedTextColor.RED, "Cancel and return to Order Board"));

        player.openInventory(inv);
    }

    private void finishOrderCreation(Player player, String itemName, int quantity, long price) {
        UUID uuid = player.getUniqueId();
        long bal = erpiesMap.getOrDefault(uuid, 0L);
        Material mat = Material.matchMaterial(itemName);
        String display = (mat != null) ? formatItemDisplayName(mat) : itemName;

        if (quantity < 1) {
            player.sendMessage(Component.text("❌ Quantity must be at least 1!", NamedTextColor.RED));
            return;
        }
        if (quantity > 1_000_000) {
            player.sendMessage(Component.text("❌ Maximum quantity is 1,000,000 (1M)!", NamedTextColor.RED));
            return;
        }
        if (price <= 0) {
            player.sendMessage(Component.text("❌ Price must be greater than 0!", NamedTextColor.RED));
            return;
        }
        if (bal < price) {
            player.sendMessage(Component.text("❌ You don't have enough Erpies! Need: " + String.format("%,d", price) + ", Have: " + String.format("%,d", bal), NamedTextColor.RED));
            return;
        }

        // Deduct Erpies upfront
        erpiesMap.put(uuid, bal - price);
        savePlayerData(uuid);
        updateScoreboard(player);

        // Add order
        OrderRequest newOrder = new OrderRequest(uuid, player.getName(), itemName, quantity, price);
        orders.add(newOrder);
        saveOrderToDatabase(newOrder);

        // Cleanup pending state
        pendingOrderItemName.remove(uuid);
        pendingOrderQuantity.remove(uuid);

        // Success notification & sound
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        player.sendMessage(Component.text("§a======================================="));
        player.sendMessage(Component.text("§a✅ Buy order successfully posted!"));
        player.sendMessage(Component.text("§fItem: §e" + String.format("%,d", quantity) + "x " + display));
        player.sendMessage(Component.text("§fTotal Paid: §6" + String.format("%,d", price) + " Erpies §7(held in escrow)"));
        player.sendMessage(Component.text("§7Your order is now live on the Order Board!"));
        player.sendMessage(Component.text("§a======================================="));

        openOrdersGui(player, null);
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
            boolean isLeaderboard = activeFloatingTextIsLeaderboard.getOrDefault(uuid, false);
            
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
                activeFloatingTextIsLeaderboard.remove(uuid);
                return;
            }

            if (isLeaderboard) {
                String typeLower = joinedText.toLowerCase().trim();
                if (!typeLower.equals("kills") && !typeLower.equals("erpies") && !typeLower.equals("derpies") && !typeLower.equals("time")) {
                    player.sendMessage(Component.text("❌ Invalid leaderboard type! Use: kills, erpies, derpies, or time.", NamedTextColor.RED));
                    return;
                }
            }
            
            activeFloatingTextPlacement.remove(uuid);
            activeFloatingTextIsLeaderboard.remove(uuid);
            
            if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                String searchType = isLeaderboard ? "leaderboard_text" : "floating_text";
                ItemStack foundItem = null;
                for (ItemStack invItem : player.getInventory().getContents()) {
                    if (invItem != null && invItem.hasItemMeta()) {
                        ItemMeta itemMeta = invItem.getItemMeta();
                        String customItem = itemMeta.getPersistentDataContainer().get(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING);
                        if (customItem != null && customItem.equals(searchType)) {
                            foundItem = invItem;
                            break;
                        }
                    }
                }
                
                if (foundItem == null) {
                    player.sendMessage(Component.text("❌ You do not have the required floating text item in your inventory to spawn this!", NamedTextColor.RED));
                    return;
                }
                
                foundItem.setAmount(foundItem.getAmount() - 1);
            }

            NamedTextColor color = NamedTextColor.WHITE;
            Component textComp;
            if (isLeaderboard) {
                textComp = getLeaderboardText(joinedText.toLowerCase().trim(), color.toString());
            } else {
                Component parsed = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().deserialize(joinedText);
                textComp = parsed.colorIfAbsent(color).decorate(net.kyori.adventure.text.format.TextDecoration.BOLD);
            }
            
            org.bukkit.entity.TextDisplay textDisplay = loc.getWorld().spawn(loc.clone().add(0, 0.5, 0), org.bukkit.entity.TextDisplay.class);
            textDisplay.text(textComp);
            textDisplay.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
            textDisplay.setInvulnerable(true);
            textDisplay.setSeeThrough(false);
            textDisplay.setShadowed(true);
            textDisplay.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            
            if (isLeaderboard) {
                textDisplay.getPersistentDataContainer().set(new NamespacedKey(this, "is_leaderboard_text"), PersistentDataType.BOOLEAN, true);
                textDisplay.getPersistentDataContainer().set(new NamespacedKey(this, "leaderboard_stat_type"), PersistentDataType.STRING, joinedText.toLowerCase().trim());
                textDisplay.getPersistentDataContainer().set(new NamespacedKey(this, "leaderboard_color"), PersistentDataType.STRING, color.toString());
                textDisplay.getPersistentDataContainer().set(new NamespacedKey(this, "floating_text_placer"), PersistentDataType.STRING, uuid.toString());
            } else {
                textDisplay.getPersistentDataContainer().set(new NamespacedKey(this, "is_floating_text"), PersistentDataType.BOOLEAN, true);
                textDisplay.getPersistentDataContainer().set(new NamespacedKey(this, "floating_text_placer"), PersistentDataType.STRING, uuid.toString());
            }
            
            player.playSound(loc, org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
            player.sendMessage(Component.text(isLeaderboard ? "✅ Spawned leaderboard text!" : "✅ Spawned floating text!", NamedTextColor.GREEN));
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
                    Location[] homes = getPlayerHomes(uuid);
                    String[] homeNames = getPlayerHomeNames(uuid);
                    
                    int foundIdx = -1;
                    if (homes != null && homeNames != null) {
                        for (int i = 0; i < 45; i++) {
                            if (i < homes.length && homes[i] != null && i < homeNames.length && homeNames[i] != null && homeNames[i].toLowerCase().contains(query)) {
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
                        String[] homeNames = playerHomeNames.computeIfAbsent(uuid, k -> {
                            String[] names = new String[54];
                            for (int idx = 0; idx < 54; idx++) {
                                names[idx] = "Home " + (idx + 1);
                            }
                            return names;
                        });
                        homeNames[homeIdx] = input;
                        player.sendMessage(Component.text("✅ Renamed Home " + (homeIdx + 1) + " to '" + input + "'!", NamedTextColor.GREEN));
                        savePlayerData(player);
                    }
                }
                Bukkit.getScheduler().runTask(this, () -> openUnifiedHomeGui(player));
                return;
            }

            if (pending.action == SignAction.DEPOSIT_MONEY_ONLY) {
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
                            long playerBal = currency.equals("erpies") ? erpiesMap.getOrDefault(uuid, 0L) : derpiesMap.getOrDefault(uuid, 0L);

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
                Bukkit.getScheduler().runTask(this, () -> openDepositOptionsGui(player));
                return;
            }

            if (pending.action == SignAction.WITHDRAW_ERPIES_ONLY) {
                if (input.isEmpty()) {
                    player.sendMessage(Component.text("❌ Withdrawal cancelled.", NamedTextColor.RED));
                } else {
                    try {
                        String inputClean = input.toLowerCase().replaceAll("[()\\s]", "");
                        long amount = parseAmountWithSuffix(inputClean);
                        if (amount <= 0) {
                            player.sendMessage(Component.text("❌ Invalid amount!", NamedTextColor.RED));
                        } else {
                            applyInterest(uuid);
                            long bankBal = bankErpiesMap.getOrDefault(uuid, 0L);
                            if (bankBal < amount) {
                                player.sendMessage(Component.text("❌ You do not have enough deposited! Deposited balance: " + bankBal + " Erpies", NamedTextColor.RED));
                            } else {
                                bankErpiesMap.put(uuid, bankBal - amount);
                                erpiesMap.put(uuid, erpiesMap.getOrDefault(uuid, 0L) + amount);
                                player.sendMessage(Component.text("✅ Withdrew " + amount + " Erpies from the bank!", NamedTextColor.GREEN));
                                updateScoreboard(player);
                                savePlayerData(player);
                            }
                        }
                    } catch (Exception e) {
                        player.sendMessage(Component.text("❌ Invalid input format! Enter a number, e.g., 100 or 1m", NamedTextColor.RED));
                    }
                }
                Bukkit.getScheduler().runTask(this, () -> openWithdrawMoneyGui(player));
                return;
            }

            if (pending.action == SignAction.WITHDRAW_DERPIES_ONLY) {
                if (input.isEmpty()) {
                    player.sendMessage(Component.text("❌ Withdrawal cancelled.", NamedTextColor.RED));
                } else {
                    try {
                        String inputClean = input.toLowerCase().replaceAll("[()\\s]", "");
                        long amount = parseAmountWithSuffix(inputClean);
                        if (amount <= 0) {
                            player.sendMessage(Component.text("❌ Invalid amount!", NamedTextColor.RED));
                        } else {
                            applyInterest(uuid);
                            long bankBal = bankDerpiesMap.getOrDefault(uuid, 0L);
                            if (bankBal < amount) {
                                player.sendMessage(Component.text("❌ You do not have enough deposited! Deposited balance: " + bankBal + " Derpies", NamedTextColor.RED));
                            } else {
                                bankDerpiesMap.put(uuid, bankBal - amount);
                                derpiesMap.put(uuid, derpiesMap.getOrDefault(uuid, 0L) + amount);
                                player.sendMessage(Component.text("✅ Withdrew " + amount + " Derpies from the bank!", NamedTextColor.GREEN));
                                updateScoreboard(player);
                                savePlayerData(player);
                            }
                        }
                    } catch (Exception e) {
                        player.sendMessage(Component.text("❌ Invalid input format! Enter a number, e.g., 100 or 1m", NamedTextColor.RED));
                    }
                }
                Bukkit.getScheduler().runTask(this, () -> openWithdrawMoneyGui(player));
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
            } else if (pending.action == SignAction.ORDER_BOARD_SEARCH) {
                if (input.isEmpty()) {
                    player.sendMessage(Component.text("❌ Search cancelled.", NamedTextColor.RED));
                    orderBoardSearchQuery.remove(uuid);
                    Bukkit.getScheduler().runTask(this, () -> openOrdersGui(player, null));
                } else {
                    final String query = input.trim();
                    orderBoardSearchQuery.put(uuid, query);
                    player.sendMessage(Component.text("🔍 Searching orders for: " + query, NamedTextColor.GREEN));
                    Bukkit.getScheduler().runTask(this, () -> openOrdersGui(player, query));
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
                        Bukkit.getScheduler().runTask(this, () -> openChooseQuantityGui(player, mat));
                    }
                }
            } else if (pending.action == SignAction.ORDER_QUANTITY) {
                String itemName = pendingOrderItemName.get(uuid);
                if (itemName == null || input.isEmpty()) {
                    player.sendMessage(Component.text("❌ Order cancelled.", NamedTextColor.RED));
                } else {
                    Material mat = Material.matchMaterial(itemName);
                    try {
                        long rawQty = parseAmountWithSuffix(input);
                        if (rawQty < 1) {
                            player.sendMessage(Component.text("❌ Quantity must be at least 1!", NamedTextColor.RED));
                            if (mat != null) Bukkit.getScheduler().runTask(this, () -> openChooseQuantityGui(player, mat));
                        } else if (rawQty > 1_000_000) {
                            player.sendMessage(Component.text("❌ Maximum quantity is 1,000,000 (1M)!", NamedTextColor.RED));
                            if (mat != null) Bukkit.getScheduler().runTask(this, () -> openChooseQuantityGui(player, mat));
                        } else {
                            int qty = (int) rawQty;
                            pendingOrderQuantity.put(uuid, qty);
                            if (mat != null) Bukkit.getScheduler().runTask(this, () -> openChoosePriceGui(player, mat, qty));
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(Component.text("❌ Invalid quantity! Use a number (e.g. 64, 1k, 1m).", NamedTextColor.RED));
                        if (mat != null) Bukkit.getScheduler().runTask(this, () -> openChooseQuantityGui(player, mat));
                    }
                }
            } else if (pending.action == SignAction.ORDER_PRICE) {
                String itemName = pendingOrderItemName.get(uuid);
                int qty = pendingOrderQuantity.getOrDefault(uuid, 1);
                if (itemName == null || input.isEmpty()) {
                    player.sendMessage(Component.text("❌ Order cancelled.", NamedTextColor.RED));
                } else {
                    try {
                        long price = parseAmountWithSuffix(input);
                        Bukkit.getScheduler().runTask(this, () -> finishOrderCreation(player, itemName, qty, price));
                    } catch (NumberFormatException e) {
                        player.sendMessage(Component.text("❌ Invalid price! Use a number (e.g. 500, 10k, 1m).", NamedTextColor.RED));
                        Material mat = Material.matchMaterial(itemName);
                        if (mat != null) Bukkit.getScheduler().runTask(this, () -> openChoosePriceGui(player, mat, qty));
                    }
                }
            } else if (pending.action == SignAction.ORDER_SEARCH) {
                String query = input.trim();
                pendingOrderSearchQuery.put(uuid, query.isEmpty() ? null : query);
                pendingOrderPage.put(uuid, 0);
                Bukkit.getScheduler().runTask(this, () -> openChooseItemFlow(player, 0, query.isEmpty() ? null : query));
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

    private boolean isAfkWorld(World world) {
        if (world == null) return false;
        String name = world.getName();
        return name.equalsIgnoreCase("afk") || name.equalsIgnoreCase("afk_zone") || name.equalsIgnoreCase("spawn");
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
                if (!isAfkWorld(player.getWorld()) && (
                    player.getLocation().getBlockX() != startLoc.getBlockX() ||
                    player.getLocation().getBlockY() != startLoc.getBlockY() ||
                    player.getLocation().getBlockZ() != startLoc.getBlockZ())) {
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
                    player.sendTitle("§a Teleporting!", "", 0, 20, 10);
                    doRtpTeleport(player, dimension);
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private void doRtpTeleport(Player player, String dimension) {
        World target = null;
        int rangeMin, rangeMax;

        if (dimension.equalsIgnoreCase("nether")) {
            target = Bukkit.getWorld("world_nether");
            if (target == null) {
                target = Bukkit.getWorlds().stream()
                        .filter(w -> w.getEnvironment() == World.Environment.NETHER)
                        .findFirst().orElse(null);
            }
            rangeMin = -500; rangeMax = 500;
        } else if (dimension.equalsIgnoreCase("end")) {
            target = Bukkit.getWorld("world_the_end");
            if (target == null) {
                target = Bukkit.getWorlds().stream()
                        .filter(w -> w.getEnvironment() == World.Environment.THE_END)
                        .findFirst().orElse(null);
            }
            rangeMin = 1000; rangeMax = 5000;
        } else {
            // Overworld
            target = Bukkit.getWorld("world");
            if (target == null) {
                target = Bukkit.getWorlds().stream()
                        .filter(w -> w.getEnvironment() == World.Environment.NORMAL 
                            && !w.getName().equalsIgnoreCase("spawn") 
                            && !w.getName().equalsIgnoreCase("duel") 
                            && !w.getName().equalsIgnoreCase("afk") 
                            && !w.getName().equalsIgnoreCase("afk_zone") 
                            && !w.getName().equalsIgnoreCase("echo_valley"))
                        .findFirst().orElse(null);
            }
            if (target == null && !Bukkit.getWorlds().isEmpty()) {
                target = Bukkit.getWorlds().get(0);
            }
            rangeMin = -5000; rangeMax = 5000;
        }

        if (target == null) {
            player.sendMessage(net.kyori.adventure.text.Component.text("❌ That dimension does not exist on this server!", net.kyori.adventure.text.format.NamedTextColor.RED));
            return;
        }

        final World finalWorld = target;
        final int fMin = rangeMin;
        final int fMax = rangeMax;

        findRtpLocation(player, finalWorld, dimension, fMin, fMax, 0);
    }

    private void findRtpLocation(Player player, World finalWorld, String dimension, int fMin, int fMax, int attempts) {
        if (!player.isOnline()) return;

        if (attempts >= 30) {
            Location fallback;
            if (finalWorld.getEnvironment() == World.Environment.NETHER) {
                fallback = new Location(finalWorld, 0.5, 64, 0.5);
            } else if (finalWorld.getEnvironment() == World.Environment.THE_END) {
                fallback = new Location(finalWorld, 1000.5, 60, 0.5);
            } else {
                fallback = finalWorld.getSpawnLocation();
            }
            teleportationSync(player, fallback, "🌍 Teleported to a random location in the " + dimension + "!");
            return;
        }

        int x, z;
        if (finalWorld.getEnvironment() == World.Environment.THE_END) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = fMin + random.nextInt(Math.max(1, fMax - fMin));
            x = (int) (Math.cos(angle) * radius);
            z = (int) (Math.sin(angle) * radius);
        } else {
            x = random.nextInt(Math.max(1, fMax - fMin)) + fMin;
            z = random.nextInt(Math.max(1, fMax - fMin)) + fMin;
        }

        final int targetX = x;
        final int targetZ = z;

        finalWorld.getChunkAtAsync(targetX >> 4, targetZ >> 4).thenAccept(chunk -> {
            Bukkit.getScheduler().runTask(this, () -> {
                if (!player.isOnline()) return;

                int y = finalWorld.getHighestBlockYAt(targetX, targetZ);
                boolean safeFound = false;
                int safeY = y;

                if (finalWorld.getEnvironment() == World.Environment.NETHER) {
                    for (int testY = 120; testY > 30; testY--) {
                        Block footBlock = finalWorld.getBlockAt(targetX, testY, targetZ);
                        Block headBlock = finalWorld.getBlockAt(targetX, testY + 1, targetZ);
                        Block standBlock = finalWorld.getBlockAt(targetX, testY - 1, targetZ);
                        if (footBlock.getType() == Material.AIR &&
                            headBlock.getType() == Material.AIR &&
                            standBlock.getType().isSolid() &&
                            standBlock.getType() != Material.LAVA &&
                            standBlock.getType() != Material.FIRE &&
                            standBlock.getType() != Material.SOUL_FIRE) {
                            safeY = testY;
                            safeFound = true;
                            break;
                        }
                    }
                } else if (finalWorld.getEnvironment() == World.Environment.THE_END) {
                    if (y > 10) {
                        Block standBlock = finalWorld.getBlockAt(targetX, y - 1, targetZ);
                        if (standBlock.getType().isSolid()) {
                            safeFound = true;
                        }
                    }
                } else {
                    // Overworld
                    if (y > 50) {
                        Block standBlock = finalWorld.getBlockAt(targetX, y - 1, targetZ);
                        if (standBlock.getType().isSolid() && standBlock.getType() != Material.LAVA) {
                            safeFound = true;
                        }
                    }
                }

                if (safeFound) {
                    Location dest = new Location(finalWorld, targetX + 0.5, safeY, targetZ + 0.5);
                    teleportationSync(player, dest, "🌍 Teleported to a random location in the " + dimension + "!");
                } else {
                    findRtpLocation(player, finalWorld, dimension, fMin, fMax, attempts + 1);
                }
            });
        }).exceptionally(ex -> {
            Bukkit.getScheduler().runTask(this, () -> {
                if (player.isOnline()) {
                    findRtpLocation(player, finalWorld, dimension, fMin, fMax, attempts + 1);
                }
            });
            return null;
        });
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
                if (!isAfkWorld(requester.getWorld()) && (
                    requester.getLocation().getBlockX() != startLoc.getBlockX() ||
                    requester.getLocation().getBlockY() != startLoc.getBlockY() ||
                    requester.getLocation().getBlockZ() != startLoc.getBlockZ())) {
                    cancel();
                    requester.sendTitle("§cTeleport Cancelled", "§7You moved!", 0, 20, 10);
                    requester.sendMessage(Component.text("❌ Teleport cancelled because you moved!", NamedTextColor.RED));
                    return;
                }
                if (combatTagTicks.containsKey(requester.getUniqueId())) {
                    cancel();
                    requester.sendTitle("§cTeleport Cancelled", "§7Entered combat!", 0, 20, 10);
                    requester.sendMessage(Component.text("❌ Teleport cancelled because you entered combat!", NamedTextColor.RED));
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
                    teleportationSync(requester, target.getLocation(), "✅ Teleported to " + target.getName() + "!");
                    requester.sendTitle("§aTeleported!", "", 0, 20, 10);
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    // --- Stash (OP) ---
    private void spawnStash(Player player) {
        Location mid = player.getLocation().getBlock().getLocation();
        Location left = mid.clone().add(-1, 0, 0);
        Location right = mid.clone().add(1, 0, 0);

        mid.getBlock().setType(Material.SHULKER_BOX);
        left.getBlock().setType(Material.SPAWNER);
        right.getBlock().setType(Material.SPAWNER);

        org.bukkit.block.ShulkerBox box = (org.bukkit.block.ShulkerBox) mid.getBlock().getState();
        box.getInventory().addItem(new ItemStack(Material.NETHERITE_INGOT, 64));
        box.update();

        generators.put(left, new GeneratorData(left, "ore_generator", Bukkit.createInventory(null, 27, Component.text("Ore Generator"))));
        generators.put(right, new GeneratorData(right, "tools_generator", Bukkit.createInventory(null, 27, Component.text("Tools Generator"))));
        saveGenerators();

        player.sendMessage(Component.text("📦 Spawned an Ore Generator, a Shulker Box with 64 Netherite, and a Tools Generator!", NamedTextColor.GREEN));
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
            teleportationSync(player, spawn, "🏠 Welcome to the Admin Room!");
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
        if (title.equals("Ender Chest")) {
            Player player = (Player) event.getPlayer();
            savePlayerData(player);
            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ENDER_CHEST_CLOSE, 1.0f, 1.0f);
            return;
        }
        if (title.startsWith("Team Vault Page ")) {
            Player player = (Player) event.getPlayer();
            UUID uuid = player.getUniqueId();
            String teamLower = playerTeams.get(uuid);
            if (teamLower == null) return;
            TeamData data = teams.get(teamLower);
            if (data == null) return;

            int page = Integer.parseInt(title.replace("Team Vault Page ", "")) - 1;
            ItemStack[] pageItems = new ItemStack[45];
            for (int i = 0; i < 45; i++) {
                pageItems[i] = event.getInventory().getItem(i);
            }
            while (data.vaultPages.size() <= page) {
                data.vaultPages.add(new ItemStack[45]);
            }
            data.vaultPages.set(page, pageItems);
            saveTeams();
            return;
        }
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
        if (!loggedInPlayers.contains(event.getPlayer().getUniqueId()) && !isBedrockPlayer(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.SURVIVAL && (player.getWorld().getName().equalsIgnoreCase("spawn") || isInSpawnRadius(event.getItemDrop().getLocation()))) {
            event.setCancelled(true);
            player.sendMessage(Component.text("❌ This cannot be done in spawn!", NamedTextColor.RED));
            return;
        }
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

        // Create gradient title for "ERP SMP" using MiniMessage
        Component title = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                .deserialize("<gradient:#00c6ff:#0072ff><b>ERP SMP</b></gradient>");

        Objective obj = board.registerNewObjective("smp_board", Criteria.DUMMY, title);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.numberFormat(io.papermc.paper.scoreboard.numbers.NumberFormat.blank());

        UUID uuid = player.getUniqueId();

        // Get player team
        String teamName = "None";
        String lowerTeam = playerTeams.get(uuid);
        if (lowerTeam != null) {
            TeamData data = teams.get(lowerTeam);
            if (data != null) {
                teamName = data.name;
            }
        }

        // Add rows matching the user's requested order and screenshot styles
        addScoreboardRow(board, obj, "⛃", "Erpies", NamedTextColor.GREEN, formatValue(erpiesMap.getOrDefault(uuid, 0L)), 7, "§1");
        addScoreboardRow(board, obj, "✦", "Derpies", NamedTextColor.LIGHT_PURPLE, formatValue(derpiesMap.getOrDefault(uuid, 0L)), 6, "§2");
        addScoreboardRow(board, obj, "⚔", "Kills", NamedTextColor.RED, String.valueOf(killsMap.getOrDefault(uuid, 0)), 5, "§3");
        addScoreboardRow(board, obj, "☠", "Deaths", NamedTextColor.GOLD, String.valueOf(deathsMap.getOrDefault(uuid, 0)), 4, "§4");
        addScoreboardRow(board, obj, "⧗", "Keyall", NamedTextColor.BLUE, formatKeyallTime(), 3, "§5");
        addScoreboardRow(board, obj, "⏱", "Playtime", NamedTextColor.YELLOW, formatTimePlayed(timePlayedMap.getOrDefault(uuid, 0)), 2, "§6");
        addScoreboardRow(board, obj, "⛨", "Team", NamedTextColor.AQUA, teamName, 1, "§7");
    }

    private String formatKeyallTime() {
        long diff = nextKeyallTime - System.currentTimeMillis();
        if (diff <= 0) return "0s";
        long totalSeconds = diff / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return minutes + "m " + seconds + "s";
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

    private void addScoreboardRow(Scoreboard board, Objective obj, String icon, String label, NamedTextColor valueColor, String valueStr, int scoreIndex, String placeholderId) {
        Component rowText;
        if (label.equalsIgnoreCase("Team")) {
            rowText = Component.text(icon + " ", valueColor)
                    .append(Component.text(label + " ", valueColor))
                    .append(Component.text(valueStr, NamedTextColor.WHITE));
        } else {
            rowText = Component.text(icon + " ", valueColor)
                    .append(Component.text(label + " ", NamedTextColor.WHITE))
                    .append(Component.text(valueStr, valueColor));
        }

        String teamName = "row_" + scoreIndex;
        Team team = board.getTeam(teamName);
        if (team == null) team = board.registerNewTeam(teamName);

        team.addEntry(placeholderId);
        team.prefix(rowText);

        Score score = obj.getScore(placeholderId);
        score.setScore(scoreIndex);
        score.numberFormat(io.papermc.paper.scoreboard.numbers.NumberFormat.blank());
    }

    private void updateDisplayVisibility(Player player, org.bukkit.entity.TextDisplay display, boolean canSee) {
        UUID playerUUID = player.getUniqueId();
        UUID entityUUID = display.getUniqueId();
        
        java.util.Set<UUID> hidden = hiddenEntitiesMap.computeIfAbsent(playerUUID, k -> new java.util.HashSet<>());
        boolean currentlyHidden = hidden.contains(entityUUID);
        
        if (canSee && currentlyHidden) {
            player.showEntity(this, display);
            hidden.remove(entityUUID);
        } else if (!canSee && !currentlyHidden) {
            player.hideEntity(this, display);
            hidden.add(entityUUID);
        }
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

    private ItemStack createLeaderboardTextItem() {
        ItemStack sign = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = sign.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Leaderboard Text", NamedTextColor.GOLD, net.kyori.adventure.text.format.TextDecoration.BOLD));
            meta.lore(List.of(
                Component.text("Place to create a leaderboard floating text.", NamedTextColor.YELLOW),
                Component.text("1. Place on the ground or a wall.", NamedTextColor.GRAY),
                Component.text("2. Type 'kills', 'erpies', or 'derpies' in the sign.", NamedTextColor.GRAY),
                Component.text("3. Choose a color in the GUI.", NamedTextColor.GRAY),
                Component.text("4. Shift+Left-click the block to remove.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "leaderboard_text");
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

    private List<ItemStack> getAllCustomItems() {
        List<ItemStack> items = new java.util.ArrayList<>();
        items.add(createEchoPickaxe());
        items.add(createEchoShovel());
        items.add(createEchoAxe());
        items.add(createEchoBow());
        items.add(createKnockbackStick());
        items.add(createSwordDerp());
        items.add(createPickaxeLerp());
        items.add(createMaceMerp());
        items.add(createEchoSword());
        items.add(createEnderSword());
        items.add(createZeusSword());
        items.add(createGoatySword());
        items.add(createLungeSpear());
        items.add(createShopCrate());
        items.add(createEchoCrate());
        items.add(createCrimsonCrate());
        items.add(createKeyCrate());
        items.add(createEndCrate());
        items.add(createAmethystCrate());
        items.add(createEchoKey());
        items.add(createCrimsonKey());
        items.add(createEndKey());
        items.add(createAmethystKey());
        items.add(createEndGatewayItem());
        items.add(createOrbitalStrike());
        items.add(createWand());
        items.add(createNpcEgg());
        items.add(createFloatingTextItem());
        items.add(createLeaderboardTextItem());
        items.add(createCommandChest());
        items.add(createDivineFlame());
        items.add(createFoodGeneratorItem());
        items.add(createOreGeneratorItem());
        items.add(createToolsGeneratorItem());
        items.add(createMobGeneratorItem());
        return items;
    }

    private void openCustomItemsAdminPanel(Player player) {
        openCustomItemsAdminPanel(player, 0);
    }

    private void openCustomItemsAdminPanel(Player player, int page) {
        List<ItemStack> allItems = getAllCustomItems();
        int itemsPerPage = 45; // slots 0-44, last row (45-53) for navigation
        int totalPages = (int) Math.ceil((double) allItems.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        erpItemPage.put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Custom Items - Page " + (page + 1) + "/" + totalPages));

        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, allItems.size());
        for (int i = startIndex; i < endIndex; i++) {
            inv.setItem(i - startIndex, allItems.get(i));
        }

        // Fill last row with glass panes
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.displayName(Component.text(" "));
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 45; i <= 53; i++) {
            inv.setItem(i, filler);
        }

        // Previous Page button (slot 45)
        if (page > 0) {
            inv.setItem(45, createGuiItem(Material.ARROW, "◀ Previous Page", NamedTextColor.YELLOW, "Go to page " + page));
        }

        // Page indicator (slot 49)
        inv.setItem(49, createGuiItem(Material.PAPER, "Page " + (page + 1) + " of " + totalPages, NamedTextColor.WHITE, allItems.size() + " total custom items"));

        // Next Page button (slot 53)
        if (page < totalPages - 1) {
            inv.setItem(53, createGuiItem(Material.ARROW, "Next Page ▶", NamedTextColor.YELLOW, "Go to page " + (page + 2)));
        }

        player.openInventory(inv);
    }

    private void handleEchoPickaxeBreak(Player player, Block centerBlock, ItemStack tool) {
        Location centerLoc = centerBlock.getLocation();
        if (isInSpawnRadius(centerLoc)) return;

        BlockFace face = player.getTargetBlockFace(6);
        if (face == null) face = BlockFace.UP;

        List<Block> blocksToBreak = new ArrayList<>();

        if (face == BlockFace.UP || face == BlockFace.DOWN) {
            // Mining up or down: 3x3 horizontal plane based on player facing
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    blocksToBreak.add(centerBlock.getRelative(dx, 0, dz));
                }
            }
        } else if (face == BlockFace.NORTH || face == BlockFace.SOUTH) {
            // Mining into a north/south wall: 3 wide (X) × 3 tall (Y)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    blocksToBreak.add(centerBlock.getRelative(dx, dy, 0));
                }
            }
        } else if (face == BlockFace.EAST || face == BlockFace.WEST) {
            // Mining into an east/west wall: 3 wide (Z) × 3 tall (Y)
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    blocksToBreak.add(centerBlock.getRelative(0, dy, dz));
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
            return 45;
        }
        if (hasErpProMap.getOrDefault(uuid, false)) {
            return 27;
        }
        if (hasErpPlusMap.getOrDefault(uuid, false)) {
            return 13;
        }
        return 5;
    }

    private void openUnifiedHomeGui(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Homes Menu"));
        UUID uuid = player.getUniqueId();
        Location[] homes = getPlayerHomes(uuid);
        String[] homeNames = getPlayerHomeNames(uuid);

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

        // Slots 0 to 44: Homes / Locked Slots
        int limit = getPlayerMaxHomes(player);
        for (int i = 0; i < 45; i++) {
            if (i < limit) {
                Location loc = homes[i];
                String name = homeNames[i] != null ? homeNames[i] : "Home " + (i + 1);
                if (loc != null) {
                    String dimName = "Overworld";
                    if (loc.getWorld() != null) {
                        String wName = loc.getWorld().getName().toLowerCase();
                        if (wName.contains("nether")) dimName = "Nether";
                        else if (wName.contains("end")) dimName = "The End";
                    }
                    inv.setItem(i, createGuiItem(Material.RED_BED, name, NamedTextColor.GREEN, 
                        "§7Status: §aLocation Saved", 
                        "§7Dimension: §b" + dimName, 
                        modeLore));
                } else {
                    boolean isSethome = openedWithSethome.getOrDefault(uuid, false);
                    inv.setItem(i, createGuiItem(Material.BLACK_BED, name + " (Not Set)", NamedTextColor.GRAY, 
                        "No location saved here.", 
                        renameMode || deleteMode ? modeLore : (isSethome ? "§7Click to save your current location" : "§cUse /sethome to set this location")));
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
            boolean isLeader = teamDataForGui != null && uuid.equals(teamDataForGui.leader);
            boolean isSethome = openedWithSethome.getOrDefault(uuid, false);
            String thLore = isSethome ? (isLeader ? "§aClick to set your current location as Team Home" : "§cOnly the team leader can set this") : "§7Click to teleport to your team's home.";
            if (teamDataForGui != null && teamDataForGui.teamHome != null) {
                Location th = teamDataForGui.teamHome;
                String thDim = "Overworld";
                if (th.getWorld() != null) {
                    String wName = th.getWorld().getName().toLowerCase();
                    if (wName.contains("nether")) thDim = "Nether";
                    else if (wName.contains("end")) thDim = "The End";
                }
                inv.setItem(53, createGuiItem(Material.BEACON, "Team Home", NamedTextColor.AQUA,
                    "Team: §b" + teamDataForGui.name,
                    "§7Status: §aLocation Saved",
                    "§7Dimension: §b" + thDim,
                    thLore));
            } else {
                inv.setItem(53, createGuiItem(Material.BEACON, "Team Home", NamedTextColor.GRAY,
                    "Team: §7" + (teamDataForGui != null ? teamDataForGui.name : "?"),
                    "§cNo team home set yet.",
                    thLore));
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
                if (!isAfkWorld(player.getWorld()) && (
                    player.getLocation().getBlockX() != startLoc.getBlockX() ||
                    player.getLocation().getBlockY() != startLoc.getBlockY() ||
                    player.getLocation().getBlockZ() != startLoc.getBlockZ())) {
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
                    String homeMsg = homeNumber == -1
                        ? "🏠 Teleported to Team Home!"
                        : "🏠 Teleported to Home " + homeNumber + "!";
                    player.sendTitle("§aWelcome Home!", "", 0, 20, 10);
                    teleportationSync(player, dest, homeMsg);
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
                if (!isAfkWorld(player.getWorld()) && (
                    player.getLocation().getBlockX() != startLoc.getBlockX() ||
                    player.getLocation().getBlockY() != startLoc.getBlockY() ||
                    player.getLocation().getBlockZ() != startLoc.getBlockZ())) {
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
                    player.sendTitle("§aTeleported!", "", 0, 20, 10);
                    if (destinationName.equals("AFK Zone")) {
                        Bukkit.getScheduler().runTask(CustomScoreboard.this, () -> {
                            if (player.isOnline()) player.sendMessage(Component.text("💤 Welcome to the AFK Zone! You will earn 1 Derpy per minute.", NamedTextColor.LIGHT_PURPLE));
                        });
                    }
                    if (destinationName.equalsIgnoreCase("Spawn") || destinationName.equalsIgnoreCase("AFK Zone")) {
                        teleportationSync(player, dest, "✅ Teleported to " + destinationName + "!");
                        Bukkit.getScheduler().runTask(CustomScoreboard.this, () -> { if (player.isOnline()) playLobbyMusic(player); });
                    } else {
                        teleportationSync(player, dest, "✅ Teleported to " + destinationName + "!");
                        Bukkit.getScheduler().runTask(CustomScoreboard.this, () -> { if (player.isOnline()) stopLobbyMusic(player); });
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

        boolean starter = starterLootDisabled.getOrDefault(uuid, false);
        if (starter) {
            inv.setItem(13, createGuiItem(Material.RED_WOOL, "Disable Starter Loot: ON", NamedTextColor.RED, "Click to toggle (No starter loot given)"));
        } else {
            inv.setItem(13, createGuiItem(Material.GREEN_WOOL, "Disable Starter Loot: OFF", NamedTextColor.GREEN, "Click to toggle (Gives starter loot)"));
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

    private boolean isInteractableBlock(Block block) {
        if (block == null) return false;
        Material type = block.getType();
        if (type.isAir()) return false;

        // Blocks that should NOT count as interactable for combat weapon right-clicks
        if (type == Material.TNT || type == Material.PUMPKIN || type == Material.CARVED_PUMPKIN
                || type == Material.BEEHIVE || type == Material.BEE_NEST) {
            return false;
        }

        // Custom server blocks (Ender Chest, Generators)
        if (type == Material.ENDER_CHEST) return true;
        if (type == Material.SPAWNER && generators.containsKey(block.getLocation())) return true;

        // Standard containers
        if (block.getState() instanceof org.bukkit.block.Container) return true;

        org.bukkit.block.data.BlockData data = block.getBlockData();
        if (data instanceof org.bukkit.block.data.type.Door
                || data instanceof org.bukkit.block.data.type.TrapDoor
                || data instanceof org.bukkit.block.data.type.Gate
                || data instanceof org.bukkit.block.data.type.Switch
                || data instanceof org.bukkit.block.data.type.Bed
                || data instanceof org.bukkit.block.data.type.Lectern
                || data instanceof org.bukkit.block.data.type.Bell
                || data instanceof org.bukkit.block.data.type.RespawnAnchor
                || data instanceof org.bukkit.block.data.type.Cake
                || data instanceof org.bukkit.block.data.type.Comparator
                || data instanceof org.bukkit.block.data.type.Repeater
                || data instanceof org.bukkit.block.data.type.DaylightDetector) {
            return true;
        }

        switch (type) {
            case CRAFTING_TABLE:
            case ANVIL:
            case CHIPPED_ANVIL:
            case DAMAGED_ANVIL:
            case ENCHANTING_TABLE:
            case BEACON:
            case STONECUTTER:
            case CARTOGRAPHY_TABLE:
            case SMITHING_TABLE:
            case GRINDSTONE:
            case LOOM:
            case NOTE_BLOCK:
            case JUKEBOX:
            case COMPOSTER:
            case RESPAWN_ANCHOR:
            case LODESTONE:
            case LEVER:
                return true;
            default:
                try {
                    return type.isInteractable();
                } catch (Exception e) {
                    return false;
                }
        }
    }

    private boolean isInteractableItem(ItemStack item, boolean isBlockClick) {
        if (item == null || item.getType().isAir()) return false;
        Material type = item.getType();

        // 1. Shield (blocking)
        if (type == Material.SHIELD) return true;

        // 2. Edible foods
        if (type.isEdible()) return true;

        // 3. Drinks and potions
        if (type == Material.POTION || type == Material.SPLASH_POTION
                || type == Material.LINGERING_POTION || type == Material.MILK_BUCKET
                || type == Material.HONEY_BOTTLE || type == Material.OMINOUS_BOTTLE) {
            return true;
        }

        // 4. Projectiles, ranged weapons, consumable tools
        if (type == Material.BOW || type == Material.CROSSBOW || type == Material.TRIDENT
                || type == Material.WIND_CHARGE || type == Material.ENDER_PEARL
                || type == Material.ENDER_EYE || type == Material.CHORUS_FRUIT
                || type == Material.EGG || type == Material.SNOWBALL
                || type == Material.EXPERIENCE_BOTTLE || type == Material.FIREWORK_ROCKET
                || type == Material.FISHING_ROD || type == Material.SPYGLASS
                || type == Material.BRUSH || type == Material.FLINT_AND_STEEL
                || type == Material.SHEARS) {
            return true;
        }

        // 5. Buckets
        if (type == Material.BUCKET || type.name().endsWith("_BUCKET")) {
            return true;
        }

        // 6. Spawn Eggs
        if (type.name().endsWith("_SPAWN_EGG")) {
            return true;
        }

        // 7. When clicking a block: placeable blocks and end crystals are interactable (player places them)
        if (isBlockClick) {
            if (type == Material.END_CRYSTAL || type.isBlock()) {
                return true;
            }
        }

        return false;
    }

    private ItemStack getOtherHandItem(Player player, EquipmentSlot hand) {
        if (hand == null || hand == EquipmentSlot.HAND) {
            return player.getInventory().getItemInOffHand();
        } else {
            return player.getInventory().getItemInMainHand();
        }
    }

    private boolean shouldTriggerCustomWeaponAbility(Player player, PlayerInteractEvent event) {
        // 1. If player is sneaking (crouching), custom ability ALWAYS triggers!
        if (player.isSneaking()) {
            return true;
        }

        boolean isBlockClick = (event.getAction() == Action.RIGHT_CLICK_BLOCK);

        // 2. If clicking on an interactable block, let the player interact with the block unless crouching
        if (isBlockClick && isInteractableBlock(event.getClickedBlock())) {
            return false;
        }

        // 3. If the other hand has an interactable item (shield, food, potion, or placeable block on block click),
        // let the other hand item be used unless crouching
        ItemStack otherHandItem = getOtherHandItem(player, event.getHand());
        if (isInteractableItem(otherHandItem, isBlockClick)) {
            return false;
        }

        // 4. Otherwise, the other hand is non-interactable (or empty, e.g. totem of undying)
        // and we are not clicking an interactable block -> activate ability
        return true;
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
                        final List<org.bukkit.entity.TNTPrimed> activeTnts = new java.util.ArrayList<>();

                        // Spawn ALL TNT rings simultaneously overhead
                        for (int step = 0; step < 20; step++) {
                            double radius = 1.0 + (step * 1.5); // grows up to 30 blocks radius
                            int numBlocks = 6 + (int)(radius * 0.8);
                            for (int i = 0; i < numBlocks; i++) {
                                double angle = (i * 2.0 * Math.PI / numBlocks) + (step * 0.05);
                                double dx = radius * Math.cos(angle);
                                double dz = radius * Math.sin(angle);
                                Location tntLoc = center.clone().add(dx, 40.0, dz);
                                org.bukkit.entity.TNTPrimed tnt = tntLoc.getWorld().spawn(tntLoc, org.bukkit.entity.TNTPrimed.class);
                                tnt.setSource(player);
                                tnt.setFuseTicks(100); // 5 seconds fuse
                                tnt.setGravity(true);
                                tnt.setVelocity(new org.bukkit.util.Vector(0, -2.0, 0)); // fast downward fall
                                activeTnts.add(tnt);
                            }
                        }

                        // Maintain fast fall velocity and explode all TNT together at 5 seconds (100 ticks)
                        new org.bukkit.scheduler.BukkitRunnable() {
                            int ticks = 0;

                            @Override
                            public void run() {
                                ticks++;
                                if (ticks >= 100) { // 5 seconds
                                    for (org.bukkit.entity.TNTPrimed tnt : activeTnts) {
                                        if (tnt.isValid()) {
                                            tnt.setFuseTicks(0);
                                        }
                                    }
                                    activeTnts.clear();
                                    cancel();
                                    return;
                                }
                                for (org.bukkit.entity.TNTPrimed tnt : activeTnts) {
                                    if (tnt.isValid() && !tnt.isOnGround()) {
                                        tnt.setVelocity(new org.bukkit.util.Vector(0, -2.0, 0));
                                    }
                                }
                            }
                        }.runTaskTimer(CustomScoreboard.this, 0L, 1L);
                    }
                } else if (customType.equals("lunge_spear")) {
                    if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                        if (!shouldTriggerCustomWeaponAbility(player, event)) {
                            return;
                        }
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
                } else if (customType.equals("floating_text") || customType.equals("leaderboard_text")) {
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
                            activeFloatingTextIsLeaderboard.put(player.getUniqueId(), customType.equals("leaderboard_text"));
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
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack item = event.getItem();
            if (item != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                String customItem = meta.getPersistentDataContainer().get(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING);
                if (customItem != null && customItem.equals("ender_sword")) {
                    Player player = event.getPlayer();
                    if (!shouldTriggerCustomWeaponAbility(player, event)) {
                        return;
                    }
                    event.setCancelled(true);
                    
                    if (player.hasCooldown(Material.NETHERITE_SWORD)) {
                        player.sendMessage(Component.text("❌ Ender Sword is on cooldown!", NamedTextColor.RED));
                        return;
                    }
                    
                    org.bukkit.entity.EnderPearl pearl = player.launchProjectile(org.bukkit.entity.EnderPearl.class);
                    ItemStack representation = new ItemStack(Material.NETHERITE_SWORD);
                    pearl.setItem(representation);
                    
                    player.setCooldown(Material.NETHERITE_SWORD, 60); // 3 seconds cooldown
                    player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDER_PEARL_THROW, 1.0f, 1.0f);
                    return;
                }
                
                if (customItem != null && customItem.equals("zeus_sword")) {
                    Player player = event.getPlayer();
                    if (!shouldTriggerCustomWeaponAbility(player, event)) {
                        return;
                    }
                    event.setCancelled(true);
                    
                    if (player.hasCooldown(Material.NETHERITE_SWORD)) {
                        player.sendMessage(Component.text("❌ Zeus Sword is on cooldown!", NamedTextColor.RED));
                        return;
                    }
                    
                    Block targetBlock = player.getTargetBlockExact(100);
                    if (targetBlock != null && targetBlock.getType() != Material.AIR) {
                        Location strikeLoc = targetBlock.getLocation();
                        strikeLoc.getWorld().strikeLightning(strikeLoc);
                        player.setCooldown(Material.NETHERITE_SWORD, 60); // 3 seconds cooldown
                    } else {
                        player.sendMessage(Component.text("❌ No target block in sight!", NamedTextColor.RED));
                    }
                    return;
                }

                if (customItem != null && customItem.equals("goaty_sword")) {
                    Player player = event.getPlayer();
                    if (!shouldTriggerCustomWeaponAbility(player, event)) {
                        return;
                    }
                    event.setCancelled(true);

                    if (player.hasCooldown(Material.DIAMOND_SWORD)) {
                        int remainingTicks = player.getCooldown(Material.DIAMOND_SWORD);
                        int remainingSec = (int) Math.ceil(remainingTicks / 20.0);
                        player.sendMessage(Component.text("❌ The Goaty Sword is on cooldown! (" + remainingSec + "s left)", NamedTextColor.RED));
                        return;
                    }

                    org.bukkit.util.RayTraceResult result = player.getWorld().rayTraceEntities(
                        player.getEyeLocation(),
                        player.getLocation().getDirection(),
                        15.0,
                        0.8,
                        entity -> entity instanceof LivingEntity && !entity.equals(player)
                    );

                    LivingEntity target = null;
                    if (result != null && result.getHitEntity() instanceof LivingEntity living) {
                        target = living;
                    } else {
                        for (org.bukkit.entity.Entity nearby : player.getNearbyEntities(4.0, 4.0, 4.0)) {
                            if (nearby instanceof LivingEntity living && !nearby.equals(player)) {
                                org.bukkit.util.Vector toTarget = living.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                                if (player.getLocation().getDirection().dot(toTarget) > 0.5) {
                                    target = living;
                                    break;
                                }
                            }
                        }
                    }

                    if (target != null) {
                        executeGoatyAbility(player, target);
                    } else {
                        player.sendMessage(Component.text("❌ No target in sight! Aim at an entity within 15 blocks.", NamedTextColor.RED));
                    }
                    return;
                }
            }
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block block = event.getClickedBlock();
            ItemStack itemInHand = event.getItem();
            if (block != null && block.getType() == Material.REINFORCED_DEEPSLATE && itemInHand != null && itemInHand.hasItemMeta()) {
                ItemMeta meta = itemInHand.getItemMeta();
                String customItem = meta.getPersistentDataContainer().get(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING);
                if (customItem != null && customItem.equals("echo_starter")) {
                    Player player = event.getPlayer();
                    event.setCancelled(true);
                    activateEchoPortal(player, block);
                    return;
                }
            }
            if (block != null && block.getType() == Material.ENDER_CHEST) {
                Player player = event.getPlayer();
                if (player.isSneaking() && player.getInventory().getItemInMainHand().getType() != Material.AIR) {
                    return;
                }
                event.setCancelled(true);
                player.openInventory(getCustomEnderChest(player));
                player.playSound(block.getLocation(), org.bukkit.Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 1.0f);
                return;
            }
            if (block != null && block.getType() == Material.SPAWNER) {
                Location loc = block.getLocation();
                if (generators.containsKey(loc)) {
                    event.setCancelled(true);
                    Player player = event.getPlayer();
                    if (!player.isSneaking()) {
                        GeneratorData data = generators.get(loc);
                        if (data.type.equals("mob_generator")) {
                            openMobGeneratorTypeSelectionGui(player, loc);
                        } else {
                            player.openInventory(data.inventory);
                            player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 0.5f, 1.0f);
                        }
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
            if (player.getUniqueId().equals(data.owner)) {
                openCrateSetupGui(player, data);
            } else {
                player.sendMessage(Component.text("❌ Only the person who placed this crate can edit it!", NamedTextColor.RED));
            }
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
        if (!isBedrockPlayer(player)) return;

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        BlockFace face = event.getBlockFace();
        Block targetBlock = clickedBlock.getRelative(face);

        // Only handle if the placed block would be above the bedrock roof (Y >= 128)
        if (targetBlock.getY() < 128) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        org.bukkit.inventory.EquipmentSlot slot = org.bukkit.inventory.EquipmentSlot.HAND;
        if (item == null || !item.getType().isBlock()) {
            item = player.getInventory().getItemInOffHand();
            slot = org.bukkit.inventory.EquipmentSlot.OFF_HAND;
        }
        if (item == null || !item.getType().isBlock()) return;

        // Ensure the target block is air or replaceable
        if (targetBlock.getType() != Material.AIR && !targetBlock.isReplaceable()) return;

        // Create and call block place event
        BlockPlaceEvent placeEvent = new BlockPlaceEvent(
            targetBlock,
            targetBlock.getState(),
            clickedBlock,
            item,
            player,
            true,
            slot
        );
        Bukkit.getPluginManager().callEvent(placeEvent);
        if (placeEvent.isCancelled() || !placeEvent.canBuild()) return;

        // Place the block
        Material blockType = item.getType();
        targetBlock.setType(blockType);
        
        // Play placement sound
        targetBlock.getWorld().playSound(targetBlock.getLocation(), targetBlock.getBlockData().getSoundGroup().getPlaceSound(), 1.0f, 1.0f);

        // Consume block from hand in survival
        if (player.getGameMode() == GameMode.SURVIVAL) {
            item.setAmount(item.getAmount() - 1);
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
            entity.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            entity.setSeeThrough(false);
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
            if (getConfig().contains(path + ".requests")) {
                List<String> reqStrings = getConfig().getStringList(path + ".requests");
                for (String rStr : reqStrings) {
                    try {
                        data.requests.add(UUID.fromString(rStr));
                    } catch (Exception ignored) {}
                }
            }
            if (getConfig().contains(path + ".vault")) {
                org.bukkit.configuration.ConfigurationSection vaultSec = getConfig().getConfigurationSection(path + ".vault");
                if (vaultSec != null) {
                    for (String pageKey : vaultSec.getKeys(false)) {
                        try {
                            int pageIdx = Integer.parseInt(pageKey.replace("page_", ""));
                            ItemStack[] pageItems = new ItemStack[45];
                            org.bukkit.configuration.ConfigurationSection pageSec = vaultSec.getConfigurationSection(pageKey);
                            if (pageSec != null) {
                                for (String slotKey : pageSec.getKeys(false)) {
                                    int slot = Integer.parseInt(slotKey);
                                    pageItems[slot] = pageSec.getItemStack(slotKey);
                                }
                            }
                            while (data.vaultPages.size() <= pageIdx) {
                                data.vaultPages.add(new ItemStack[45]);
                            }
                            data.vaultPages.set(pageIdx, pageItems);
                        } catch (Exception ignored) {}
                    }
                }
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
            
            List<String> reqStrings = new ArrayList<>();
            for (UUID rUuid : data.requests) {
                reqStrings.add(rUuid.toString());
            }
            getConfig().set(path + ".requests", reqStrings);

            if (data.vaultPages != null) {
                for (int p = 0; p < data.vaultPages.size(); p++) {
                    ItemStack[] pageItems = data.vaultPages.get(p);
                    if (pageItems == null) continue;
                    for (int s = 0; s < 45; s++) {
                        if (pageItems[s] != null) {
                            getConfig().set(path + ".vault.page_" + p + "." + s, pageItems[s]);
                        }
                    }
                }
            }

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

    private ItemStack createEnderSword() {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Ender Sword", NamedTextColor.LIGHT_PURPLE).decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true));
            meta.lore(List.of(
                Component.text("Right-click to throw a teleporting ender pearl.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "ender_sword");
            sword.setItemMeta(meta);
        }
        return sword;
    }

    private ItemStack createZeusSword() {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Zeus Sword", NamedTextColor.GOLD).decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true));
            meta.lore(List.of(
                Component.text("Right-click to strike lightning where you look.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "zeus_sword");
            sword.setItemMeta(meta);
        }
        return sword;
    }

    private ItemStack createGoatySword() {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("The Goaty Sword", NamedTextColor.GOLD).decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true));
            meta.lore(List.of(
                Component.text("Right-click or strike to unleash the Goaty Ability.", NamedTextColor.YELLOW),
                Component.text("Deals 1-10 hearts of random damage.", NamedTextColor.GRAY),
                Component.text("Totems can still save players from death.", NamedTextColor.GRAY),
                Component.text("Does not damage or break armor.", NamedTextColor.GRAY),
                Component.text("Cooldown: 10s", NamedTextColor.DARK_GRAY)
            ));
            meta.addEnchant(Enchantment.SHARPNESS, 5, true);
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.setUnbreakable(true);
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "goaty_sword");
            sword.setItemMeta(meta);
        }
        return sword;
    }

    private void executeGoatyAbility(Player attacker, LivingEntity target) {
        if (target == null || target.isDead()) return;

        if (attacker.getGameMode() == GameMode.SURVIVAL) {
            if (attacker.getWorld().getName().equalsIgnoreCase("spawn") || isInSpawnRadius(target.getLocation())) {
                attacker.sendMessage(Component.text("❌ PvP and damage are disabled at spawn!", NamedTextColor.RED));
                return;
            }
            if (attacker.getWorld().getName().equalsIgnoreCase("afk_zone") || attacker.getWorld().getName().equalsIgnoreCase("afk")) {
                attacker.sendMessage(Component.text("❌ PvP is disabled in the AFK zone!", NamedTextColor.RED));
                return;
            }
        }

        int hearts = 1 + random.nextInt(10); // 1 to 10 hearts
        double damage = hearts * 2.0;

        attacker.getWorld().playSound(attacker.getLocation(), org.bukkit.Sound.ENTITY_GOAT_SCREAMING_PREPARE_RAM, 1.2f, 1.0f);
        target.getWorld().playSound(target.getLocation(), org.bukkit.Sound.ENTITY_GOAT_SCREAMING_RAM_IMPACT, 1.2f, 1.0f);

        // Spawn cloud particle trail from attacker to target
        Location start = attacker.getEyeLocation();
        Location end = target.getLocation().add(0, 1.0, 0);
        org.bukkit.util.Vector dir = end.toVector().subtract(start.toVector());
        double dist = dir.length();
        if (dist > 0.1) {
            dir.normalize();
            for (double d = 0.5; d < dist; d += 0.8) {
                Location pLoc = start.clone().add(dir.clone().multiply(d));
                attacker.getWorld().spawnParticle(Particle.CLOUD, pLoc, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }

        target.getWorld().spawnParticle(Particle.CRIT, end, 20, 0.4, 0.4, 0.4, 0.2);
        target.getWorld().spawnParticle(Particle.CLOUD, end, 10, 0.3, 0.3, 0.3, 0.1);

        double currentHealth = target.getHealth();

        if (target instanceof Player victim) {
            goatyArmorProtectionUntil.put(victim.getUniqueId(), System.currentTimeMillis() + 1000L);

            ItemStack[] armorBefore = victim.getInventory().getArmorContents();
            ItemStack[] armorClones = new ItemStack[armorBefore.length];
            for (int i = 0; i < armorBefore.length; i++) {
                if (armorBefore[i] != null) {
                    armorClones[i] = armorBefore[i].clone();
                }
            }

            if (currentHealth - damage <= 0) {
                // Lethal damage: trigger totem pop / death without breaking armor
                target.damage(Math.max(currentHealth * 5.0, 50.0), attacker);

                if (victim.isValid() && !victim.isDead()) {
                    // Victim survived (e.g. totem popped) -> guarantee armor is intact with original durability
                    ItemStack[] currentArmor = victim.getInventory().getArmorContents();
                    for (int i = 0; i < armorClones.length; i++) {
                        if (armorClones[i] != null) {
                            if (currentArmor[i] == null || currentArmor[i].getType().isAir()) {
                                currentArmor[i] = armorClones[i];
                            } else if (currentArmor[i].getItemMeta() instanceof org.bukkit.inventory.meta.Damageable currentDmg &&
                                       armorClones[i].getItemMeta() instanceof org.bukkit.inventory.meta.Damageable cloneDmg) {
                                currentDmg.setDamage(cloneDmg.getDamage());
                                currentArmor[i].setItemMeta(currentDmg);
                            }
                        }
                    }
                    victim.getInventory().setArmorContents(currentArmor);
                }
            } else {
                // Non-lethal true damage
                target.setHealth(Math.max(0.1, currentHealth - damage));
                target.playHurtAnimation(0);
                target.getWorld().playSound(target.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);

                if (!victim.equals(attacker)) {
                    UUID victimUUID = victim.getUniqueId();
                    UUID attackerUUID = attacker.getUniqueId();
                    combatTagTicks.put(victimUUID, 20);
                    combatTagTicks.put(attackerUUID, 20);
                    victim.sendActionBar(Component.text("combat 20s", NamedTextColor.RED));
                    attacker.sendActionBar(Component.text("combat 20s", NamedTextColor.RED));
                }
            }
        } else {
            // Target is a mob / non-player
            if (currentHealth - damage <= 0) {
                target.damage(Math.max(currentHealth * 5.0, 50.0), attacker);
            } else {
                target.setHealth(Math.max(0.1, currentHealth - damage));
                target.playHurtAnimation(0);
            }
        }

        attacker.sendActionBar(Component.text("🐐 The Goaty Sword dealt " + hearts + " hearts! (" + (int) damage + " DMG)", NamedTextColor.GOLD));
        if (target instanceof Player victim) {
            victim.sendMessage(Component.text("🐐 You were struck by The Goaty Sword for " + hearts + " hearts!", NamedTextColor.RED));
        }

        attacker.setCooldown(Material.DIAMOND_SWORD, 200); // 10 seconds cooldown
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onPlayerItemDamage(org.bukkit.event.player.PlayerItemDamageEvent event) {
        Long until = goatyArmorProtectionUntil.get(event.getPlayer().getUniqueId());
        if (until != null && System.currentTimeMillis() < until) {
            if (isArmor(event.getItem().getType())) {
                event.setDamage(0);
                event.setCancelled(true);
            }
        }
    }

    private boolean isArmor(Material material) {
        if (material == null) return false;
        String name = material.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || 
               name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") || 
               material == Material.ELYTRA || material == Material.TURTLE_HELMET ||
               material == Material.SHIELD;
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

    private ItemStack createEchoFrameItem() {
        ItemStack item = new ItemStack(Material.REINFORCED_DEEPSLATE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Echo Frame", NamedTextColor.DARK_PURPLE).decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true));
            meta.lore(List.of(
                Component.text("Build a vertical rectangular frame of any size,", NamedTextColor.GRAY),
                Component.text("then right-click it with an Echo Starter to activate.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "echo_frame");
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createEchoStarterItem() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Echo Starter", NamedTextColor.AQUA).decoration(net.kyori.adventure.text.format.TextDecoration.BOLD, true));
            meta.lore(List.of(
                Component.text("Right-click on an Echo Frame to activate the Echo Portal.", NamedTextColor.GRAY)
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this, "custom_item"), PersistentDataType.STRING, "echo_starter");
            item.setItemMeta(meta);
        }
        return item;
    }

    private void activateEchoPortal(Player player, Block clickedBlock) {
        Location clickedLoc = clickedBlock.getLocation();
        
        // 1. Try X-Y plane (constant Z)
        PortalResult result = checkPortalPlane(clickedBlock, true);
        if (result == null) {
            // 2. Try Z-Y plane (constant X)
            result = checkPortalPlane(clickedBlock, false);
        }
        
        if (result != null) {
            result.spawnGateways();
            player.sendMessage(Component.text("🌀 The Echo Portal has been activated!", NamedTextColor.DARK_PURPLE));
            clickedBlock.getWorld().playSound(clickedLoc, org.bukkit.Sound.BLOCK_PORTAL_TRIGGER, 1.0f, 1.0f);
        } else {
            player.sendMessage(Component.text("❌ Invalid portal frame configuration!", NamedTextColor.RED));
        }
    }

    private static class PortalResult {
        World world;
        int minCoord;
        int maxCoord;
        int minY;
        int maxY;
        int constantCoord;
        boolean isXY;

        public PortalResult(World world, int minCoord, int maxCoord, int minY, int maxY, int constantCoord, boolean isXY) {
            this.world = world;
            this.minCoord = minCoord;
            this.maxCoord = maxCoord;
            this.minY = minY;
            this.maxY = maxY;
            this.constantCoord = constantCoord;
            this.isXY = isXY;
        }

        public void spawnGateways() {
            for (int coord = minCoord; coord <= maxCoord; coord++) {
                for (int y = minY; y <= maxY; y++) {
                    Block b;
                    if (isXY) {
                        b = world.getBlockAt(coord, y, constantCoord);
                    } else {
                        b = world.getBlockAt(constantCoord, y, coord);
                    }
                    if (b.getType().isAir() || !b.getType().isSolid()) {
                        b.setType(Material.END_GATEWAY);
                    }
                }
            }
        }
    }

    private PortalResult checkPortalPlane(Block clickedBlock, boolean isXY) {
        World world = clickedBlock.getWorld();
        int constantCoord = isXY ? clickedBlock.getZ() : clickedBlock.getX();
        int clickCoord = isXY ? clickedBlock.getX() : clickedBlock.getZ();
        int clickY = clickedBlock.getY();

        int[][] dirs = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}
        };

        for (int[] dir : dirs) {
            int startC = clickCoord + dir[0];
            int startY = clickY + dir[1];

            Block startBlock = getBlockAtPlane(world, startC, startY, constantCoord, isXY);
            if (!isReplaceable(startBlock)) continue;

            int minC = startC;
            while (minC >= startC - 21) {
                Block b = getBlockAtPlane(world, minC, startY, constantCoord, isXY);
                if (b.getType() == Material.REINFORCED_DEEPSLATE) {
                    minC = minC + 1;
                    break;
                }
                if (!isReplaceable(b)) {
                    minC = -999;
                    break;
                }
                minC--;
            }
            if (minC == -999 || minC < startC - 20) continue;

            int maxC = startC;
            while (maxC <= startC + 21) {
                Block b = getBlockAtPlane(world, maxC, startY, constantCoord, isXY);
                if (b.getType() == Material.REINFORCED_DEEPSLATE) {
                    maxC = maxC - 1;
                    break;
                }
                if (!isReplaceable(b)) {
                    maxC = -999;
                    break;
                }
                maxC++;
            }
            if (maxC == -999 || maxC > startC + 20 || maxC < minC) continue;

            int minY = startY;
            while (minY >= startY - 21) {
                Block b = getBlockAtPlane(world, startC, minY, constantCoord, isXY);
                if (b.getType() == Material.REINFORCED_DEEPSLATE) {
                    minY = minY + 1;
                    break;
                }
                if (!isReplaceable(b)) {
                    minY = -999;
                    break;
                }
                minY--;
            }
            if (minY == -999 || minY < startY - 20) continue;

            int maxY = startY;
            while (maxY <= startY + 21) {
                Block b = getBlockAtPlane(world, startC, maxY, constantCoord, isXY);
                if (b.getType() == Material.REINFORCED_DEEPSLATE) {
                    maxY = maxY - 1;
                    break;
                }
                if (!isReplaceable(b)) {
                    maxY = -999;
                    break;
                }
                maxY++;
            }
            if (maxY == -999 || maxY > startY + 20 || maxY < minY) continue;

            boolean valid = true;

            for (int c = minC; c <= maxC; c++) {
                Block bottom = getBlockAtPlane(world, c, minY - 1, constantCoord, isXY);
                Block top = getBlockAtPlane(world, c, maxY + 1, constantCoord, isXY);
                if (bottom.getType() != Material.REINFORCED_DEEPSLATE || top.getType() != Material.REINFORCED_DEEPSLATE) {
                    valid = false;
                    break;
                }
            }
            if (!valid) continue;

            for (int y = minY; y <= maxY; y++) {
                Block left = getBlockAtPlane(world, minC - 1, y, constantCoord, isXY);
                Block right = getBlockAtPlane(world, maxC + 1, y, constantCoord, isXY);
                if (left.getType() != Material.REINFORCED_DEEPSLATE || right.getType() != Material.REINFORCED_DEEPSLATE) {
                    valid = false;
                    break;
                }
            }
            if (!valid) continue;

            for (int c = minC; c <= maxC; c++) {
                for (int y = minY; y <= maxY; y++) {
                    Block inner = getBlockAtPlane(world, c, y, constantCoord, isXY);
                    if (!isReplaceable(inner)) {
                        valid = false;
                        break;
                    }
                }
                if (!valid) break;
            }

            if (valid) {
                return new PortalResult(world, minC, maxC, minY, maxY, constantCoord, isXY);
            }
        }
        return null;
    }

    private Block getBlockAtPlane(World world, int c, int y, int constantCoord, boolean isXY) {
        if (isXY) {
            return world.getBlockAt(c, y, constantCoord);
        } else {
            return world.getBlockAt(constantCoord, y, c);
        }
    }

    private boolean isReplaceable(Block b) {
        Material t = b.getType();
        return t == Material.AIR || t == Material.CAVE_AIR || t == Material.VOID_AIR || t == Material.WATER || t == Material.SEAGRASS || t == Material.TALL_SEAGRASS || t == Material.SHORT_GRASS || t == Material.FERN || t == Material.TALL_GRASS || t == Material.LARGE_FERN || t == Material.END_GATEWAY;
    }

    private boolean isEchoPortalGateway(Block block) {
        if (block.getType() != Material.END_GATEWAY) return false;
        
        boolean hasReinforcedX = false;
        for (int dx = -1; dx >= -22; dx--) {
            Block b = block.getRelative(dx, 0, 0);
            if (b.getType() == Material.REINFORCED_DEEPSLATE) {
                hasReinforcedX = true;
                break;
            }
            if (b.getType() != Material.AIR && b.getType() != Material.END_GATEWAY) break;
        }
        
        if (hasReinforcedX) return true;
        
        boolean hasReinforcedZ = false;
        for (int dz = -1; dz >= -22; dz--) {
            Block b = block.getRelative(0, 0, dz);
            if (b.getType() == Material.REINFORCED_DEEPSLATE) {
                hasReinforcedZ = true;
                break;
            }
            if (b.getType() != Material.AIR && b.getType() != Material.END_GATEWAY) break;
        }
        
        return hasReinforcedZ;
    }

    private void teleportToEchoValley(Player player) {
        World echoValley = Bukkit.getWorld("echo_valley");
        if (echoValley == null) {
            WorldCreator creator = new WorldCreator("echo_valley");
            creator.environment(World.Environment.NORMAL);
            echoValley = Bukkit.createWorld(creator);
        }

        if (echoValley != null) {
            Location spawn = echoValley.getSpawnLocation();
            int safeY = echoValley.getHighestBlockYAt(spawn.getBlockX(), spawn.getBlockZ());
            Location dest = new Location(echoValley, spawn.getX() + 0.5, safeY + 1, spawn.getZ() + 0.5);
            player.teleport(dest);
            player.sendMessage(Component.text("🌀 Teleported to the Echo Valley!", NamedTextColor.DARK_PURPLE));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        } else {
            player.sendMessage(Component.text("❌ Echo Valley dimension is not available!", NamedTextColor.RED));
        }
    }

    private void teleportFromEchoValley(Player player) {
        World overworld = Bukkit.getWorlds().get(0);
        Location spawn = overworld.getSpawnLocation();
        int safeY = overworld.getHighestBlockYAt(spawn.getBlockX(), spawn.getBlockZ());
        Location dest = new Location(overworld, spawn.getX() + 0.5, safeY + 1, spawn.getZ() + 0.5);
        player.teleport(dest);
        player.sendMessage(Component.text("🌀 Teleported back to the Overworld!", NamedTextColor.DARK_PURPLE));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
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

            team.setOption(Team.Option.NAME_TAG_VISIBILITY,
                    shouldHideNametag(online) ? Team.OptionStatus.NEVER : Team.OptionStatus.ALWAYS);

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

            if (isRedToppat(uuid)) {
                prefix = prefix.append(Component.text("[Owner o' Merp] ", NamedTextColor.RED));
                team.color(NamedTextColor.RED);
            } else if (uuid.equals(BOREAS_UUID)) {
                prefix = prefix.append(Component.text("[Co-Owner o' Lerp] ", NamedTextColor.BLUE));
                team.color(NamedTextColor.BLUE);
            } else if (name.equalsIgnoreCase(".Ironwarden7425") || name.equalsIgnoreCase(".IronWarden7425")) {
                prefix = prefix.append(Component.text("[Admin o' Derp] ", NamedTextColor.DARK_PURPLE));
                team.color(NamedTextColor.DARK_PURPLE);
            } else if (name.equalsIgnoreCase(".AlberTogotfound") || name.equalsIgnoreCase("AlberTogofound")) {
                prefix = prefix.append(Component.text("[Albert!! the pizza lover] ", NamedTextColor.YELLOW));
                team.color(NamedTextColor.YELLOW);
            } else if (online.isOp()) {
                prefix = prefix.append(Component.text("[admin] ", NamedTextColor.LIGHT_PURPLE));
                team.color(NamedTextColor.LIGHT_PURPLE);
            } else if (hasErpProMaxMap.getOrDefault(uuid, false)) {
                prefix = prefix.append(createGoldGradientComponent("[Erp+ Pro Max] "));
                team.color(NamedTextColor.GOLD);
            } else if (hasErpProMap.getOrDefault(uuid, false)) {
                prefix = prefix.append(Component.text("[Erp+ Pro] ", NamedTextColor.GRAY));
                team.color(NamedTextColor.GRAY);
            } else if (hasErpPlusMap.getOrDefault(uuid, false)) {
                prefix = prefix.append(Component.text("[Erp+] ", NamedTextColor.GOLD));
                team.color(NamedTextColor.WHITE);
            } else if (hasVipMap.getOrDefault(uuid, false)) {
                prefix = prefix.append(Component.text("[VIP] ", NamedTextColor.GOLD));
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

    private boolean shouldHideNametag(Player player) {
        if (player == null || !player.isOnline()) return true;
        if (player.getGameMode() == GameMode.SPECTATOR) return true;
        if (player.hasPotionEffect(PotionEffectType.INVISIBILITY) || player.isInvisible()) return true;
        return false;
    }

    private boolean hasAnyFloatingTags(Player player) {
        if (player == null) return false;
        UUID uuid = player.getUniqueId();
        if (isRedToppat(uuid) || uuid.equals(BOREAS_UUID)) return true;
        String teamNameLower = playerTeams.get(uuid);
        if (teamNameLower != null && teams.containsKey(teamNameLower)) return true;
        java.util.Set<String> activeTags = activeNametags.get(uuid);
        return activeTags != null && !activeTags.isEmpty();
    }

    private void updatePlayerFloatingTags(Player player) {
        UUID uuid = player.getUniqueId();
        
        // 1. Remove old displays
        List<org.bukkit.entity.TextDisplay> old = playerTagDisplays.remove(uuid);
        if (old != null) {
            java.util.Set<UUID> hidden = hiddenEntitiesMap.get(uuid);
            for (org.bukkit.entity.TextDisplay td : old) {
                if (td.isValid()) td.remove();
                if (hidden != null) hidden.remove(td.getUniqueId());
            }
        }

        // Hide floating nametags if player is in spectator mode or invisible!
        if (shouldHideNametag(player)) {
            return;
        }
        
        // 2. Build list of all tag components to display
        List<Component> allTags = new java.util.ArrayList<>();

        // 2a. Admin tags
        if (isRedToppat(uuid)) {
            allTags.add(Component.text("Owner o' Merp", NamedTextColor.GOLD, net.kyori.adventure.text.format.TextDecoration.BOLD));
        } else if (uuid.equals(BOREAS_UUID)) {
            allTags.add(Component.text("Co-Owner o' Lerp", NamedTextColor.GOLD, net.kyori.adventure.text.format.TextDecoration.BOLD));
        }

        // 2b. Team tag - "Leader of <team>" or "Member of <team>"
        String teamNameLower = playerTeams.get(uuid);
        if (teamNameLower != null) {
            TeamData teamData = teams.get(teamNameLower);
            if (teamData != null) {
                if (teamData.leader.equals(uuid)) {
                    allTags.add(Component.text("Leader of ", NamedTextColor.YELLOW)
                        .append(Component.text(teamData.name, NamedTextColor.GREEN, net.kyori.adventure.text.format.TextDecoration.BOLD)));
                } else {
                    allTags.add(Component.text("Member of ", NamedTextColor.GRAY)
                        .append(Component.text(teamData.name, NamedTextColor.GREEN, net.kyori.adventure.text.format.TextDecoration.BOLD)));
                }
            }
        }

        // 2c. Custom nametags from the nametag menu
        java.util.Set<String> activeTags = activeNametags.getOrDefault(uuid, java.util.Collections.emptySet());
        List<String> sortedTags = new java.util.ArrayList<>(activeTags);
        java.util.Collections.sort(sortedTags);
        
        for (String tag : sortedTags) {
            Component tagComp = switch (tag) {
                case "Berry Lover" -> Component.text("[Berry Lover]", NamedTextColor.LIGHT_PURPLE);
                case "Combat Master" -> Component.text("[Combat Master]", NamedTextColor.RED);
                case "Admin killer" -> createRainbowComponent("[Admin killer]");
                case "Richie Boi" -> Component.text("[Richie Boi]", NamedTextColor.GREEN);
                case "Dragon Slayer" -> Component.text("[Dragon Slayer]", NamedTextColor.DARK_PURPLE);
                case "The Miner" -> Component.text("[The Miner]", NamedTextColor.BLUE);
                case "Silent Assassin" -> Component.text("[Silent Assassin]", NamedTextColor.RED);
                case "The Builder" -> Component.text("[The Builder]", NamedTextColor.YELLOW);
                case "Fatty" -> Component.text("[", NamedTextColor.GRAY).append(Component.text("Fat", NamedTextColor.GREEN)).append(Component.text("ty", net.kyori.adventure.text.format.TextColor.color(0x8b, 0x5a, 0x2b))).append(Component.text("]", NamedTextColor.GRAY));
                case "Skin and Bones" -> Component.text("[Skin and Bones]", NamedTextColor.WHITE);
                default -> Component.empty();
            };
            if (!tagComp.equals(Component.empty())) {
                allTags.add(tagComp);
            }
        }

        if (allTags.isEmpty()) {
            return;
        }
        
        // 3. Create text displays stacked on top of each other
        List<org.bukkit.entity.TextDisplay> displays = new java.util.ArrayList<>();
        org.bukkit.entity.Entity currentVehicle = player;
        
        for (Component tagComp : allTags) {
            Location spawnLoc = player.getLocation().add(0, 2.0, 0);
            org.bukkit.entity.TextDisplay display = player.getWorld().spawn(spawnLoc, org.bukkit.entity.TextDisplay.class);
            display.text(tagComp);
            display.setBillboard(org.bukkit.entity.Display.Billboard.CENTER);
            display.setPersistent(false);
            display.setGravity(false);
            display.setSeeThrough(false);
            display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
            
            // Mark it as a player overhead tag with its owner's UUID
            display.getPersistentDataContainer().set(new NamespacedKey(this, "is_player_tag"), PersistentDataType.BOOLEAN, true);
            display.getPersistentDataContainer().set(new NamespacedKey(this, "player_tag_owner"), PersistentDataType.STRING, uuid.toString());

            currentVehicle.addPassenger(display);
            currentVehicle = display;
            
            // Hide the tag display from the owner player so it doesn't block their first-person crosshair
            updateDisplayVisibility(player, display, false);
            
            displays.add(display);
        }
        
        playerTagDisplays.put(uuid, displays);
    }

    private int getPlayerLeaderboardRank(UUID uuid, String statType) {
        java.util.List<java.util.Map.Entry<UUID, Long>> list = new ArrayList<>();
        java.util.Set<UUID> allUuids = new java.util.HashSet<>();
        org.bukkit.configuration.ConfigurationSection playersSec = getConfig().getConfigurationSection("players");
        if (playersSec != null) {
            for (String key : playersSec.getKeys(false)) {
                try {
                    allUuids.add(UUID.fromString(key));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        if (statType.equalsIgnoreCase("kills")) {
            allUuids.addAll(killsMap.keySet());
        } else if (statType.equalsIgnoreCase("erpies")) {
            allUuids.addAll(erpiesMap.keySet());
        } else if (statType.equalsIgnoreCase("derpies")) {
            allUuids.addAll(derpiesMap.keySet());
        }

        for (UUID u : allUuids) {
            long val = 0;
            String key = u.toString();
            if (statType.equalsIgnoreCase("kills")) {
                if (killsMap.containsKey(u)) {
                    val = killsMap.get(u).longValue();
                } else {
                    val = getConfig().getLong("players." + key + ".kills", 0L);
                }
            } else if (statType.equalsIgnoreCase("erpies")) {
                if (erpiesMap.containsKey(u)) {
                    val = erpiesMap.get(u);
                } else {
                    val = getConfig().getLong("players." + key + ".erpies", 0L);
                }
            } else if (statType.equalsIgnoreCase("derpies")) {
                if (derpiesMap.containsKey(u)) {
                    val = derpiesMap.get(u);
                } else {
                    val = getConfig().getLong("players." + key + ".derpies", 0L);
                }
            }
            if (val > 0) {
                list.add(new java.util.AbstractMap.SimpleEntry<>(u, val));
            }
        }

        list.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getKey().equals(uuid)) {
                return i + 1;
            }
        }
        return -1;
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
        String preprocessMsg = event.getMessage().toLowerCase().trim();
        String[] preprocessParts = preprocessMsg.split(" ");
        String preprocessCmd = preprocessParts[0].replaceAll("^/", "");
        if (preprocessCmd.contains(":")) preprocessCmd = preprocessCmd.substring(preprocessCmd.indexOf(':') + 1);

        if (preprocessCmd.equals("op") || preprocessCmd.equals("deop") || preprocessCmd.equals("ban")) {
            event.setCancelled(true);
            player.sendMessage(Component.text("❌ This command has been disabled by the administrator!", NamedTextColor.RED));
            return;
        }
        if (!loggedInPlayers.contains(player.getUniqueId()) && !isBedrockPlayer(player)) {
            String message = event.getMessage().toLowerCase().trim();
            String[] parts = message.split(" ");
            String cmd = parts[0].replaceAll("^/", "");
            if (cmd.contains(":")) cmd = cmd.substring(cmd.indexOf(':') + 1);
            
            boolean isAuthCmd = cmd.equals("login") || cmd.equals("register");
            boolean isAllowedOpCmd = player.isOp() && (cmd.equals("player") || cmd.equals("kick") || cmd.equals("gm") || cmd.equals("gamemode"));
            
            if (!isAuthCmd && !isAllowedOpCmd) {
                event.setCancelled(true);
                player.sendMessage(Component.text("❌ You must log in or register first before using commands!", NamedTextColor.RED));
                return;
            }
        }
        if (isDuelWorld(player.getWorld())) {
            String message = event.getMessage().toLowerCase().trim();
            if (message.startsWith("/tp") || message.startsWith("/spawn") || message.startsWith("/home")
                    || message.startsWith("/rtp") || message.startsWith("/dtp") || message.startsWith("/warp")
                    || message.startsWith("/tpa") || message.startsWith("/tpaccept") || message.startsWith("/back")
                    || message.startsWith("/tpahere")) {
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

        // Block /op, /deop for everyone except .RedToppat208
        UUID senderUuid = event.getPlayer().getUniqueId();
        String[] cmdParts = message.split(" ");
        String baseCmd = cmdParts[0].replaceAll("^/", "");
        if (baseCmd.contains(":")) baseCmd = baseCmd.substring(baseCmd.indexOf(':') + 1);

        if (baseCmd.equals("op") || baseCmd.equals("deop")) {
            if (!isRedToppat(senderUuid)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Component.text("❌ You are not allowed to use that command!", NamedTextColor.RED));
                return;
            }
        }

        // Ban/unban/kick protection: only RedToppat and Boreas can ban/unban/kick operators
        if ((baseCmd.equals("ban") || baseCmd.equals("unban") || baseCmd.equals("ban-ip") || baseCmd.equals("pardon") || baseCmd.equals("kick")) && cmdParts.length >= 2) {
            if (!isRedToppat(senderUuid) && !senderUuid.equals(BOREAS_UUID)) {
                String targetName = cmdParts[1];
                Player targetPlayer = Bukkit.getPlayer(targetName);
                if (targetPlayer != null && targetPlayer.isOp()) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(Component.text("❌ Only the server owners can ban/kick other operators!", NamedTextColor.RED));
                    return;
                }
                org.bukkit.OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
                if (offlineTarget != null && offlineTarget.isOp()) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(Component.text("❌ Only the server owners can ban/unban other operators!", NamedTextColor.RED));
                    return;
                }
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
        String clean = input.replaceAll("[,()\\s]", "").toLowerCase();
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
                        NamespacedKey lbKey = new NamespacedKey(this, "is_leaderboard_text");
                        boolean hasNormal = display.getPersistentDataContainer().has(key, PersistentDataType.BOOLEAN);
                        boolean hasLeaderboard = display.getPersistentDataContainer().has(lbKey, PersistentDataType.BOOLEAN);
                        if (hasNormal || hasLeaderboard) {
                            String placerUuidStr = display.getPersistentDataContainer().get(new NamespacedKey(this, "floating_text_placer"), PersistentDataType.STRING);
                            boolean canRemove = player.isOp() || (placerUuidStr != null && placerUuidStr.equals(player.getUniqueId().toString()));
                            if (canRemove) {
                                display.remove();
                                event.setCancelled(true);
                                
                                ItemStack toDrop = hasLeaderboard ? createLeaderboardTextItem() : createFloatingTextItem();
                                player.getWorld().dropItemNaturally(display.getLocation(), toDrop);
                                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                                player.sendMessage(Component.text(hasLeaderboard ? "✅ Removed leaderboard text." : "✅ Removed floating text.", NamedTextColor.GREEN));
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private Component getLeaderboardText(String statType, String colorName) {
        NamedTextColor color = NamedTextColor.WHITE;
        try {
            if (colorName != null) {
                color = NamedTextColor.NAMES.value(colorName.toLowerCase());
            }
        } catch (Exception e) {}
        if (color == null) color = NamedTextColor.WHITE;

        String title = "";
        String columnName = "";
        if (statType.equalsIgnoreCase("kills")) {
            title = "🏆 KILLS LEADERBOARD 🏆";
            columnName = "kills";
        } else if (statType.equalsIgnoreCase("erpies")) {
            title = "🪙 ERPIES LEADERBOARD 🪙";
            columnName = "erpies";
        } else if (statType.equalsIgnoreCase("derpies")) {
            title = "💎 DERPIES LEADERBOARD 💎";
            columnName = "derpies";
        } else if (statType.equalsIgnoreCase("time")) {
            title = "⏱️ TIME PLAYED LEADERBOARD ⏱️";
            columnName = "timePlayed";
        }

        if (columnName.isEmpty()) {
            return Component.text("Invalid leaderboard type.", NamedTextColor.RED);
        }

        java.util.Map<UUID, Long> mergedStats = new java.util.HashMap<>();
        java.util.Map<UUID, String> names = new java.util.HashMap<>();

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT uuid, lastKnownName, " + columnName + " FROM player_stats")) {
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String uuidStr = rs.getString("uuid");
                    String lastKnownName = rs.getString("lastKnownName");
                    long val = rs.getLong(columnName);
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        mergedStats.put(uuid, val);
                        if (lastKnownName != null) {
                            names.put(uuid, lastKnownName);
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (Exception e) {
            getLogger().severe("Error loading leaderboard from database: " + e.getMessage());
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID uuid = p.getUniqueId();
            names.put(uuid, p.getName());
            
            long val = 0;
            if (statType.equalsIgnoreCase("kills")) {
                val = killsMap.getOrDefault(uuid, 0);
            } else if (statType.equalsIgnoreCase("erpies")) {
                val = erpiesMap.getOrDefault(uuid, 0L);
            } else if (statType.equalsIgnoreCase("derpies")) {
                val = derpiesMap.getOrDefault(uuid, 0L);
            } else if (statType.equalsIgnoreCase("time")) {
                val = timePlayedMap.getOrDefault(uuid, 0);
            }
            mergedStats.put(uuid, val);
        }

        java.util.List<java.util.Map.Entry<UUID, Long>> list = new ArrayList<>(mergedStats.entrySet());
        list.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        Component comp = Component.text(title, color, net.kyori.adventure.text.format.TextDecoration.BOLD);
        int rank = 1;
        for (java.util.Map.Entry<UUID, Long> entry : list) {
            if (rank > 20) break;
            UUID uuid = entry.getKey();
            String name = names.get(uuid);
            if (name == null) {
                org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                name = op.getName();
            }
            if (name == null) {
                name = "Unknown";
            }
            
            String valStr;
            if (statType.equalsIgnoreCase("time")) {
                long hours = entry.getValue() / 3600;
                long minutes = (entry.getValue() % 3600) / 60;
                valStr = hours + "h " + minutes + "m";
            } else {
                valStr = formatValue(entry.getValue());
            }

            Component line = Component.text("\n" + rank + ". " + name + ": ", NamedTextColor.GRAY)
                .append(Component.text(valStr, NamedTextColor.GOLD));
            comp = comp.append(line);
            rank++;
        }
        if (rank == 1) {
            comp = comp.append(Component.text("\nNo data yet.", NamedTextColor.GRAY));
        }
        return comp;
    }

    private int leaderboardUpdateTicks = 0;
    private void updateLeaderboardFloatingTexts() {
        leaderboardUpdateTicks++;
        if (leaderboardUpdateTicks % 10 != 0) { // Every 10 seconds (ticks run every 1 second)
            return;
        }
        for (World world : Bukkit.getWorlds()) {
            for (org.bukkit.entity.TextDisplay display : world.getEntitiesByClass(org.bukkit.entity.TextDisplay.class)) {
                // Ensure no existing display is visible through walls
                display.setSeeThrough(false);
                if (display.getPersistentDataContainer().has(new NamespacedKey(this, "is_leaderboard_text"), PersistentDataType.BOOLEAN)) {
                    String statType = display.getPersistentDataContainer().get(new NamespacedKey(this, "leaderboard_stat_type"), PersistentDataType.STRING);
                    String colorName = display.getPersistentDataContainer().get(new NamespacedKey(this, "leaderboard_color"), PersistentDataType.STRING);
                    if (statType != null) {
                        display.text(getLeaderboardText(statType, colorName));
                    }
                }
            }
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            updatePlayerFloatingTags(p);
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
    public void onApocalypseZombieDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (event.getEntity() instanceof org.bukkit.entity.Zombie) {
            if (event.getEntity().getWorld().getName().equalsIgnoreCase("apocalypse")) {
                org.bukkit.event.entity.EntityDamageEvent.DamageCause cause = event.getCause();
                if (cause == org.bukkit.event.entity.EntityDamageEvent.DamageCause.LAVA ||
                    cause == org.bukkit.event.entity.EntityDamageEvent.DamageCause.FIRE ||
                    cause == org.bukkit.event.entity.EntityDamageEvent.DamageCause.FIRE_TICK ||
                    cause == org.bukkit.event.entity.EntityDamageEvent.DamageCause.HOT_FLOOR) {
                    event.setCancelled(true);
                }
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
            openAllTeamsGui(player, 0);
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

        // Team Vault (Slot 46)
        inv.setItem(46, createGuiItem(Material.CHEST, "Team Vault", NamedTextColor.YELLOW, "Click to open the shared team vault."));

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

        // Team Requests (Slot 47) and Disband/Leave (Slot 52)
        boolean isLeader = player.getUniqueId().equals(data.leader);
        if (isLeader) {
            inv.setItem(47, createGuiItem(Material.PAPER, "Team Requests", NamedTextColor.GOLD, "Click to view join requests."));
            inv.setItem(52, createGuiItem(Material.BARRIER, "Disband Team", NamedTextColor.RED, "Click to disband the team."));
        } else {
            inv.setItem(52, createGuiItem(Material.BARRIER, "Leave Team", NamedTextColor.RED, "Click to leave the team."));
        }

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

    private void openTeamVault(Player player, int page) {
        if (page < 0) return;
        String teamLower = playerTeams.get(player.getUniqueId());
        if (teamLower == null) return;
        TeamData data = teams.get(teamLower);
        if (data == null) return;

        playerVaultPage.put(player.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Team Vault Page " + (page + 1)));

        // Load items
        while (data.vaultPages.size() <= page) {
            data.vaultPages.add(new ItemStack[45]);
        }
        ItemStack[] pageItems = data.vaultPages.get(page);
        for (int i = 0; i < 45; i++) {
            if (pageItems[i] != null) {
                inv.setItem(i, pageItems[i]);
            }
        }

        // Fill bottom row with gray glass panes
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        if (paneMeta != null) {
            paneMeta.displayName(Component.empty());
            pane.setItemMeta(paneMeta);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, pane);
        }

        // Buttons
        ItemStack prev = createGuiItem(Material.ARROW, "Previous Page", NamedTextColor.GREEN, "Click to go to page " + page);
        ItemStack next = createGuiItem(Material.ARROW, "Next Page", NamedTextColor.GREEN, "Click to go to page " + (page + 2));
        ItemStack back = createGuiItem(Material.BOOK, "Back to Menu", NamedTextColor.YELLOW, "Click to return to the Team menu");

        inv.setItem(45, prev);
        inv.setItem(49, back);
        inv.setItem(53, next);

        player.openInventory(inv);
    }

    private void openTeamRequestsGui(Player player, int page) {
        if (page < 0) return;
        String teamLower = playerTeams.get(player.getUniqueId());
        if (teamLower == null) return;
        TeamData data = teams.get(teamLower);
        if (data == null || !player.getUniqueId().equals(data.leader)) return;

        playerTeamRequestPage.put(player.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("Team Requests Page " + (page + 1)));

        // Bottom row background
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        if (paneMeta != null) {
            paneMeta.displayName(Component.empty());
            pane.setItemMeta(paneMeta);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, pane);
        }

        // Requests list
        List<UUID> requests = data.requests;
        int start = page * 45;
        int end = Math.min(start + 45, requests.size());

        int slot = 0;
        for (int i = start; i < end; i++) {
            UUID reqUUID = requests.get(i);
            org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(reqUUID);
            String name = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";
            
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            org.bukkit.inventory.meta.SkullMeta meta = (org.bukkit.inventory.meta.SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(offlinePlayer);
                meta.displayName(Component.text(name, NamedTextColor.YELLOW));
                meta.lore(List.of(
                    Component.text("Left-Click: Accept Request", NamedTextColor.GREEN),
                    Component.text("Right-Click: Deny Request", NamedTextColor.RED)
                ));
                head.setItemMeta(meta);
            }
            inv.setItem(slot++, head);
        }

        // Control buttons
        ItemStack prev = createGuiItem(Material.ARROW, "Previous Page", NamedTextColor.GREEN, "Click to go to page " + page);
        ItemStack next = createGuiItem(Material.ARROW, "Next Page", NamedTextColor.GREEN, "Click to go to page " + (page + 2));
        ItemStack back = createGuiItem(Material.BOOK, "Back to Menu", NamedTextColor.YELLOW, "Click to return to the Team menu");

        inv.setItem(45, prev);
        inv.setItem(49, back);
        inv.setItem(53, next);

        player.openInventory(inv);
    }

    private void openAllTeamsGui(Player player, int page) {
        if (page < 0) return;
        playerAllTeamsPage.put(player.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(null, 54, Component.text("All Teams Page " + (page + 1)));

        // Bottom row background
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        if (paneMeta != null) {
            paneMeta.displayName(Component.empty());
            pane.setItemMeta(paneMeta);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, pane);
        }

        // Teams list
        List<TeamData> allTeams = new ArrayList<>(teams.values());
        int start = page * 45;
        int end = Math.min(start + 45, allTeams.size());

        int slot = 0;
        for (int i = start; i < end; i++) {
            TeamData team = allTeams.get(i);
            ItemStack teamItem = createGuiItem(Material.SHIELD, "Team: " + team.name, NamedTextColor.AQUA,
                "Leader: " + team.leaderName,
                "Members: " + team.members.size(),
                "§7Click to request to join!");
            inv.setItem(slot++, teamItem);
        }

        // Control buttons
        ItemStack prev = createGuiItem(Material.ARROW, "Previous Page", NamedTextColor.GREEN, "Click to go to page " + page);
        ItemStack next = createGuiItem(Material.ARROW, "Next Page", NamedTextColor.GREEN, "Click to go to page " + (page + 2));
        ItemStack back = createGuiItem(Material.BOOK, "Back to Menu", NamedTextColor.YELLOW, "Click to close menu");

        inv.setItem(45, prev);
        inv.setItem(49, back);
        inv.setItem(53, next);

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

    private boolean isDuelWorld(World world) {
        if (world == null) return false;
        return isDuelWorldName(world.getName());
    }

    private boolean isDuelWorldName(String name) {
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.equals("duel") || lower.startsWith("duel_");
    }

    private void applyDuelWorldSettings(World world) {
        if (world == null || !isDuelWorld(world)) return;
        world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setTime(6000L);
        world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
        world.setStorm(true);
        world.setWeatherDuration(Integer.MAX_VALUE);
    }

    private void copyWorldDirectory(File source, File target) {
        try {
            if (source.isDirectory()) {
                if (!target.exists()) {
                    target.mkdirs();
                }
                String[] children = source.list();
                if (children != null) {
                    for (String child : children) {
                        if (child.equals("uid.dat") || child.equals("session.lock")) {
                            continue;
                        }
                        copyWorldDirectory(new File(source, child), new File(target, child));
                    }
                }
            } else {
                java.nio.file.Files.copy(source.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            getLogger().severe("Error copying world directory: " + e.getMessage());
        }
    }

    private void deleteDirectory(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (!java.nio.file.Files.isSymbolicLink(f.toPath())) {
                    deleteDirectory(f);
                }
            }
        }
        file.delete();
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

        // Restore block changes for this world only
        java.util.Iterator<java.util.Map.Entry<Location, BlockBackup>> iterator = duelBlockChanges.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Location loc = entry.getKey();
            if (loc.getWorld() != null && loc.getWorld().equals(world)) {
                BlockBackup backup = entry.getValue();
                Block block = loc.getBlock();
                block.setType(backup.material, false);
                block.setBlockData(backup.data, false);
                iterator.remove();
            }
        }

        // If it's a dynamically created duel world, teleport remaining players out and delete it
        if (world.getName().toLowerCase().startsWith("duel_")) {
            World spawnWorld = Bukkit.getWorld("spawn");
            Location spawnLoc = spawnWorld != null ? spawnWorld.getSpawnLocation() : Bukkit.getWorlds().get(0).getSpawnLocation();
            
            for (Player p : world.getPlayers()) {
                p.teleport(spawnLoc);
                p.sendMessage(Component.text("🏠 Teleported back to spawn!", NamedTextColor.GREEN));
            }
            
            String worldName = world.getName();
            Bukkit.unloadWorld(world, false);
            
            // Delete folder in a delayed task to ensure OS file handles are completely closed
            Bukkit.getScheduler().runTaskLater(this, () -> {
                File worldFolder = new File(Bukkit.getWorldContainer(), worldName);
                if (worldFolder.exists()) {
                    deleteDirectory(worldFolder);
                    getLogger().info("Successfully deleted temporary duel world: " + worldName);
                }
            }, 100L); // 5 seconds delay
        }
    }

    @EventHandler
    public void onDuelWorldLoad(WorldLoadEvent event) {
        if (isDuelWorld(event.getWorld())) {
            applyDuelWorldSettings(event.getWorld());
        }
    }

    @EventHandler
    public void onDuelWorldInit(WorldInitEvent event) {
        if (isDuelWorld(event.getWorld())) {
            applyDuelWorldSettings(event.getWorld());
        }
    }

    @EventHandler
    public void onDuelWeatherChange(WeatherChangeEvent event) {
        if (isDuelWorld(event.getWorld())) {
            if (!event.toWeatherState()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDuelBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (isDuelWorld(block.getWorld())) {
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
        if (isDuelWorld(block.getWorld())) {
            Location loc = block.getLocation();
            if (!duelBlockChanges.containsKey(loc)) {
                duelBlockChanges.put(loc, new BlockBackup(block.getType(), block.getBlockData()));
            }
        }
    }

    @EventHandler
    public void onDuelExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        if (isDuelWorld(event.getLocation().getWorld())) {
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
        if (isDuelWorld(event.getBlock().getWorld())) {
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
        if (isDuelWorld(block.getWorld())) {
            Location loc = block.getLocation();
            if (!duelBlockChanges.containsKey(loc)) {
                duelBlockChanges.put(loc, new BlockBackup(block.getType(), block.getBlockData()));
            }
        }
    }

    @EventHandler
    public void onProtectionExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        event.blockList().removeIf(block -> isInCustomProtection(block.getLocation()));
    }

    @EventHandler
    public void onProtectionBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        event.blockList().removeIf(block -> isInCustomProtection(block.getLocation()));
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
        if (player.getGameMode() != org.bukkit.GameMode.CREATIVE && event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            if (clickedBlock != null && isInCustomProtection(clickedBlock.getLocation())) {
                ItemStack item = event.getItem();
                if (item != null && item.getType() == Material.END_CRYSTAL) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("❌ You cannot place End Crystals here!", NamedTextColor.RED));
                    return;
                }
            }
        }
        if (isDuelWorldName(worldName)) {
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
                return;
            }
        }
        if (isInCustomProtection(event.getBlock().getLocation())) {
            Material bucket = event.getBucket();
            if (bucket == Material.LAVA_BUCKET) {
                event.setCancelled(true);
                player.sendMessage(Component.text("❌ You cannot place lava here!", NamedTextColor.RED));
            }
        }
    }

    @EventHandler
    public void onDuelDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        Player deceased = event.getEntity();
        if (isDuelWorld(deceased.getWorld())) {
            Player killer = deceased.getKiller();
            if (killer != null && killer.isOnline() && isDuelWorld(killer.getWorld())) {
                broadcastLiveEvent("duel", "fa-trophy", "<span class=\"highlight-name\">" + killer.getName() + "</span> won a duel against <span class=\"highlight-name\">" + deceased.getName() + "</span>! 🏆");
                killer.sendTitle("§a§lVICTORY!", "§fYou won the duel!", 10, 40, 10);
                killer.sendMessage(Component.text("🏆 You won the duel! You have 1 minute to collect the loot before being teleported back to spawn.", NamedTextColor.GOLD));
                
                final Player finalKiller = killer;
                new org.bukkit.scheduler.BukkitRunnable() {
                    int timer = 60;
                    
                    @Override
                    public void run() {
                        if (!finalKiller.isOnline() || !isDuelWorld(finalKiller.getWorld())) {
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

        String worldName = "duel_" + p1.getName() + "_" + p2.getName();

        // First, make sure the "duel" template folder exists on disk and copy it
        File templateDir = new File(Bukkit.getWorldContainer(), "duel");
        File targetDir = new File(Bukkit.getWorldContainer(), worldName);
        if (templateDir.exists()) {
            copyWorldDirectory(templateDir, targetDir);
        }

        World dualWorld = Bukkit.getWorld(worldName);
        if (dualWorld == null) {
            WorldCreator creator = new WorldCreator(worldName);
            creator.environment(World.Environment.NORMAL);
            dualWorld = Bukkit.createWorld(creator);
        }

        if (dualWorld != null) {
            applyDuelWorldSettings(dualWorld);

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
    }

    @EventHandler
    public void onEnderPearl(org.bukkit.event.player.PlayerTeleportEvent event) {
        if (event.getCause() == org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            if (isDuelWorld(event.getFrom().getWorld())) {
                Location to = event.getTo();
                if (to == null || !isDuelWorld(to.getWorld())) {
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
        org.bukkit.configuration.file.YamlConfiguration tempConfig = new org.bukkit.configuration.file.YamlConfiguration();
        for (var entry : generators.entrySet()) {
            Location loc = entry.getKey();
            GeneratorData data = entry.getValue();
            String key = loc.getWorld().getName() + "_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ();
            String path = "generators." + key;
            tempConfig.set(path + ".world", loc.getWorld().getName());
            tempConfig.set(path + ".x", loc.getX());
            tempConfig.set(path + ".y", loc.getY());
            tempConfig.set(path + ".z", loc.getZ());
            tempConfig.set(path + ".type", data.type);
            
            List<ItemStack> itemList = new ArrayList<>();
            for (ItemStack item : data.inventory.getContents()) {
                itemList.add(item != null ? item.clone() : new ItemStack(Material.AIR));
            }
            tempConfig.set(path + ".items", itemList);
        }

        java.io.File generatorsFile = new java.io.File(getDataFolder(), "generators.yml");
        String yamlString = tempConfig.saveToString();
        
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                java.nio.file.Files.writeString(generatorsFile.toPath(), yamlString);
            } catch (Exception e) {
                getLogger().severe("[GeneratorSave] Failed to save generators.yml asynchronously: " + e.getMessage());
            }
        });
    }

    private void loadGenerators() {
        generators.clear();
        java.io.File generatorsFile = new java.io.File(getDataFolder(), "generators.yml");
        org.bukkit.configuration.file.YamlConfiguration genConfig;
        
        if (generatorsFile.exists()) {
            genConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(generatorsFile);
        } else {
            // Fallback to legacy config.yml if generators.yml doesn't exist yet
            genConfig = (org.bukkit.configuration.file.YamlConfiguration) getConfig();
        }
        
        if (!genConfig.contains("generators")) return;
        org.bukkit.configuration.ConfigurationSection sec = genConfig.getConfigurationSection("generators");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            String path = "generators." + key;
            String worldName = genConfig.getString(path + ".world");
            double x = genConfig.getDouble(path + ".x");
            double y = genConfig.getDouble(path + ".y");
            double z = genConfig.getDouble(path + ".z");
            World w = Bukkit.getWorld(worldName);
            if (w == null) continue;
            Location loc = new Location(w, x, y, z);
            String type = genConfig.getString(path + ".type");
            
            String title = capitalize(type.replace("_", " "));
            Inventory inventory = Bukkit.createInventory(null, 27, Component.text(title));
            if (genConfig.contains(path + ".items")) {
                List<?> list = genConfig.getList(path + ".items");
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
        if (title.equals("Food Generator") || title.equals("Ore Generator") || title.equals("Tools Generator") || title.endsWith("Generator")) {
            saveGenerators();
        }
    }

    @EventHandler
    public void onSpawnerSpawn(org.bukkit.event.entity.SpawnerSpawnEvent event) {
        if (generators.containsKey(event.getSpawner().getLocation())) {
            event.setCancelled(true);
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
        inv.setItem(22, createGuiItem(Material.ARROW, "Back to Shop", NamedTextColor.YELLOW, "Click to return to main page"));

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

    @EventHandler
    public void onPlayerMove(org.bukkit.event.player.PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (!loggedInPlayers.contains(uuid) && !isBedrockPlayer(player)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                event.setTo(from);
            }
            return;
        }

        if (pendingWarpTeleports.containsKey(uuid)) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
                org.bukkit.scheduler.BukkitTask task = pendingWarpTeleports.remove(uuid);
                if (task != null) {
                    task.cancel();
                }
                pendingWarpStartLocations.remove(uuid);
                player.sendMessage(Component.text("❌ Warp cancelled because you moved!", NamedTextColor.RED));
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(org.bukkit.event.entity.EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker) {
            if (!loggedInPlayers.contains(attacker.getUniqueId()) && !isBedrockPlayer(attacker)) {
                event.setCancelled(true);
                return;
            }
            if (attacker.getGameMode() == GameMode.SURVIVAL && (attacker.getWorld().getName().equalsIgnoreCase("spawn") || isInSpawnRadius(event.getEntity().getLocation()))) {
                event.setCancelled(true);
                attacker.sendMessage(Component.text("❌ This cannot be done in spawn!", NamedTextColor.RED));
                return;
            }
        }
        if (event.getEntity() instanceof Player victim) {
            if (!loggedInPlayers.contains(victim.getUniqueId()) && !isBedrockPlayer(victim)) {
                event.setCancelled(true);
                return;
            }
        }
    }



    @EventHandler
    public void onPlayerPickupItem(org.bukkit.event.entity.EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (!loggedInPlayers.contains(player.getUniqueId()) && !isBedrockPlayer(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerChatAuth(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        if (!loggedInPlayers.contains(event.getPlayer().getUniqueId()) && !isBedrockPlayer(event.getPlayer())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Component.text("❌ You must log in or register first before chatting!", NamedTextColor.RED));
        }
    }

    private boolean isUnbreakableMaterial(Material material) {
        if (material == null) return true;
        switch (material) {
            case AIR:
            case CAVE_AIR:
            case VOID_AIR:
            case BEDROCK:
            case BARRIER:
            case END_PORTAL:
            case END_PORTAL_FRAME:
            case END_GATEWAY:
            case NETHER_PORTAL:
            case COMMAND_BLOCK:
            case CHAIN_COMMAND_BLOCK:
            case REPEATING_COMMAND_BLOCK:
            case STRUCTURE_BLOCK:
            case JIGSAW:
            case LIGHT:
                return true;
            default:
                return false;
        }
    }

    private boolean isProtectedLocation(Location loc) {
        if (loc == null) return true;
        String worldName = loc.getWorld().getName();
        if (worldName.equalsIgnoreCase("afk_zone") || worldName.equalsIgnoreCase("afk")) {
            return true;
        }
        if (worldName.equalsIgnoreCase("spawn")) {
            return true;
        }
        if (isInSpawnRadius(loc)) {
            return true;
        }
        if (generators.containsKey(loc)) {
            return true;
        }
        if (commandChests.containsKey(loc)) {
            return true;
        }
        if (shopCrates.containsKey(loc)) {
            return true;
        }
        return false;
    }


    private Location findNearestSafeLocation(Location start) {
        World world = start.getWorld();
        int startX = start.getBlockX();
        int startY = start.getBlockY();
        int startZ = start.getBlockZ();

        // Search in increasing radius from 0 to 5 blocks
        for (int r = 0; r <= 5; r++) {
            for (int y = -3; y <= 3; y++) {
                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {
                        if (Math.abs(x) != r && Math.abs(z) != r && Math.abs(y) != r) continue;
                        
                        Location testLoc = new Location(world, startX + x, startY + y, startZ + z);
                        if (isSafeLocation(testLoc)) {
                            testLoc.add(0.5, 0.1, 0.5);
                            testLoc.setYaw(start.getYaw());
                            testLoc.setPitch(start.getPitch());
                            return testLoc;
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isSafeLocation(Location loc) {
        Block feet = loc.getBlock();
        Block head = feet.getRelative(BlockFace.UP);
        Block ground = feet.getRelative(BlockFace.DOWN);

        if (ground.getType().isAir() || !ground.getType().isSolid()) return false;
        if (!feet.getType().isAir() && feet.getType().isSolid()) return false;
        if (!head.getType().isAir() && head.getType().isSolid()) return false;
        
        if (feet.getType() == Material.LAVA || feet.getType() == Material.FIRE || feet.getType() == Material.SOUL_FIRE) return false;
        if (head.getType() == Material.LAVA || head.getType() == Material.FIRE || head.getType() == Material.SOUL_FIRE) return false;

        return true;
    }

    @EventHandler
    public void onEntityPlace(org.bukkit.event.entity.EntityPlaceEvent event) {
        Player player = event.getPlayer();
        if (player != null && player.getGameMode() == GameMode.SURVIVAL) {
            if (player.getWorld().getName().equalsIgnoreCase("spawn") || isInSpawnRadius(event.getEntity().getLocation())) {
                event.setCancelled(true);
                player.sendMessage(Component.text("❌ This cannot be done in spawn!", NamedTextColor.RED));
            }
        }
    }

    @EventHandler
    public void onHangingPlace(org.bukkit.event.hanging.HangingPlaceEvent event) {
        Player player = event.getPlayer();
        if (player != null && player.getGameMode() == GameMode.SURVIVAL) {
            if (player.getWorld().getName().equalsIgnoreCase("spawn") || isInSpawnRadius(event.getEntity().getLocation())) {
                event.setCancelled(true);
                player.sendMessage(Component.text("❌ This cannot be done in spawn!", NamedTextColor.RED));
            }
        }
    }

    @EventHandler
    public void onHangingBreak(org.bukkit.event.hanging.HangingBreakByEntityEvent event) {
        if (event.getRemover() instanceof Player player) {
            if (player.getGameMode() == GameMode.SURVIVAL) {
                if (player.getWorld().getName().equalsIgnoreCase("spawn") || isInSpawnRadius(event.getEntity().getLocation())) {
                    event.setCancelled(true);
                    player.sendMessage(Component.text("❌ This cannot be done in spawn!", NamedTextColor.RED));
                }
            }
        }
    }

    @EventHandler
    public void onEntityPlaceInteract(PlayerInteractEvent event) {
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
            Player player = event.getPlayer();
            if (player.getGameMode() == GameMode.SURVIVAL && (player.getWorld().getName().equalsIgnoreCase("spawn") || isInSpawnRadius(event.getClickedBlock().getLocation()))) {
                ItemStack item = event.getItem();
                if (item != null) {
                    Material type = item.getType();
                    String name = type.name();
                    if (type == Material.ARMOR_STAND || type == Material.END_CRYSTAL || type == Material.TNT || 
                        type == Material.MINECART || type == Material.CHEST_MINECART || type == Material.TNT_MINECART || 
                        type == Material.HOPPER_MINECART || type == Material.FURNACE_MINECART || 
                        name.contains("BOAT") || name.contains("RAFT") || name.endsWith("_SPAWN_EGG")) {
                        event.setCancelled(true);
                        player.sendMessage(Component.text("❌ This cannot be done in spawn!", NamedTextColor.RED));
                    }
                }
            }
        }
    }

    private Location findLandLocation(Location start) {
        Location safe = findNearestSafeLocation(start);
        if (safe != null) return safe;

        World world = start.getWorld();
        int startX = start.getBlockX();
        int startZ = start.getBlockZ();
        int startY = start.getBlockY();

        // Scan downwards
        for (int dy = 0; dy < 100; dy++) {
            int y = startY - dy;
            if (y < world.getMinHeight() + 5) break;
            Location testLoc = new Location(world, startX, y, startZ);
            if (isSafeLocation(testLoc)) {
                testLoc.add(0.5, 0.1, 0.5);
                testLoc.setYaw(start.getYaw());
                testLoc.setPitch(start.getPitch());
                return testLoc;
            }
        }

        // Scan upwards
        for (int dy = 1; dy < 100; dy++) {
            int y = startY + dy;
            if (y > world.getMaxHeight() - 5) break;
            Location testLoc = new Location(world, startX, y, startZ);
            if (isSafeLocation(testLoc)) {
                testLoc.add(0.5, 0.1, 0.5);
                testLoc.setYaw(start.getYaw());
                testLoc.setPitch(start.getPitch());
                return testLoc;
            }
        }
        return null;
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.NORMAL)
    public void onPlayerTeleportSafetyAdjust(org.bukkit.event.player.PlayerTeleportEvent event) {
        // Skip PLUGIN-caused teleports (e.g. /rtp, /home, /tpa) — those destinations are
        // already validated by the caller, and overriding them here would break safe-location delivery.
        if (event.getCause() == org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN) return;
        Location to = event.getTo();
        if (to != null && to.getWorld() != null) {
            World.Environment env = to.getWorld().getEnvironment();
            if (env == World.Environment.NETHER || env == World.Environment.THE_END) {
                if (!isSafeLocation(to)) {
                    Location safeLoc = findLandLocation(to);
                    if (safeLoc != null) {
                        event.setTo(safeLoc);
                    }
                }
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
    public void onPlayerTeleportSafetyBypass(org.bukkit.event.player.PlayerTeleportEvent event) {
        if (event.isCancelled()) {
            Location to = event.getTo();
            if (to != null && to.getWorld() != null) {
                Block block = to.getBlock();
                Block below = block.getRelative(BlockFace.DOWN);
                Material type = block.getType();
                Material belowType = below.getType();

                getLogger().info("[TeleportBypass] Intercepted cancelled teleport for " + event.getPlayer().getName() + " to " + to.getWorld().getName() + " " + to.getBlockX() + "," + to.getBlockY() + "," + to.getBlockZ() + ". Cause: " + event.getCause());

                // Uncancel teleport unconditionally unless destination is inside lava or fire.
                // Void (Y below minHeight) and unloaded chunks are allowed.
                if (type != Material.LAVA && type != Material.FIRE && type != Material.SOUL_FIRE &&
                    belowType != Material.LAVA && belowType != Material.FIRE && belowType != Material.SOUL_FIRE) {
                    event.setCancelled(false);
                    getLogger().info("[TeleportBypass] Force-uncancelled teleport to " + to.getWorld().getName() + " at Y=" + to.getY());
                } else {
                    getLogger().info("[TeleportBypass] Kept teleport cancelled because destination is lava or fire.");
                }
            }
        }
    }

    public void teleportationSync(Player player, Location dest, String successMessage) {
        if (player == null || !player.isOnline() || dest == null || dest.getWorld() == null) return;

        World world = dest.getWorld();
        getLogger().info("[TeleportSync] Teleporting player " + player.getName() + " to " + world.getName() + " " + dest.getBlockX() + "," + dest.getBlockY() + "," + dest.getBlockZ());

        // Remove stacked passenger tag displays first to prevent passenger-teleport bugs in Spigot
        List<org.bukkit.entity.TextDisplay> old = playerTagDisplays.remove(player.getUniqueId());
        if (old != null) {
            for (org.bukkit.entity.TextDisplay td : old) {
                if (td.isValid()) td.remove();
            }
        }

        boolean success = false;
        try {
            success = player.teleport(dest);
        } catch (Exception e) {
            getLogger().severe("[TeleportSync] Exception during teleport for " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
        getLogger().info("[TeleportSync] Teleport result for " + player.getName() + ": " + success);
        if (success) {
            // Re-create overhead tags at the new destination
            updatePlayerFloatingTags(player);
            if (successMessage != null && !successMessage.isEmpty()) {
                player.sendMessage(net.kyori.adventure.text.Component.text(successMessage, net.kyori.adventure.text.format.NamedTextColor.GREEN));
            }
        } else {
            // Re-create tags if teleport failed so they aren't lost
            updatePlayerFloatingTags(player);
            player.sendMessage(net.kyori.adventure.text.Component.text("❌ Teleportation failed! Please try again.", net.kyori.adventure.text.format.NamedTextColor.RED));
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = false)
    public void onBedrockBlockBreakMonitor(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (isBedrockPlayer(player)) {
            Location loc = event.getBlock().getLocation();
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (player.isOnline()) {
                    player.sendBlockChange(loc, loc.getBlock().getBlockData());
                }
            }, 1L);
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (player.isOnline()) {
                    player.sendBlockChange(loc, loc.getBlock().getBlockData());
                }
            }, 3L);
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.MONITOR, ignoreCancelled = false)
    public void onBedrockBlockPlaceMonitor(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (isBedrockPlayer(player)) {
            Location loc = event.getBlock().getLocation();
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (player.isOnline()) {
                    player.sendBlockChange(loc, loc.getBlock().getBlockData());
                }
            }, 1L);
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (player.isOnline()) {
                    player.sendBlockChange(loc, loc.getBlock().getBlockData());
                }
            }, 3L);
        }
    }
}