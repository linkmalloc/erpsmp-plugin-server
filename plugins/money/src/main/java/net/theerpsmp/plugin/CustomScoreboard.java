package net.theerpsmp.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.bukkit.event.block.SignChangeEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
    private final HashMap<UUID, Integer> erpiesMap = new HashMap<>();
    private final HashMap<UUID, Integer> derpiesMap = new HashMap<>();
    private final HashMap<UUID, Integer> keysMap = new HashMap<>();
    private final HashMap<UUID, Integer> killsMap = new HashMap<>();
    private final HashMap<UUID, Integer> deathsMap = new HashMap<>();

    // Key tracking for /derpshop
    private final HashMap<UUID, Integer> regularKeysMap = new HashMap<>();
    private final HashMap<UUID, Integer> crimsonKeysMap = new HashMap<>();
    private final HashMap<UUID, Integer> echoKeysMap = new HashMap<>();

    // Auction variables
    private final List<AuctionListing> listings = new ArrayList<>();
    private final HashMap<UUID, PendingSignInput> pendingSigns = new HashMap<>();

    // Duel match tracking
    private final HashMap<UUID, UUID> pendingInvites = new HashMap<>();
    private final HashMap<UUID, DuelMatch> activeMatches = new HashMap<>();
    private final boolean[] occupiedArenas = new boolean[10];

    private final Random random = new Random();

    public enum SignAction { SEARCH, LIST_PRICE }

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

    public static class AuctionListing {
        public final UUID id;
        public final UUID seller;
        public final String sellerName;
        public final ItemStack item;
        public final int price;

        public AuctionListing(UUID seller, String sellerName, ItemStack item, int price) {
            this.id = UUID.randomUUID();
            this.seller = seller;
            this.sellerName = sellerName;
            this.item = item;
            this.price = price;
        }
    }

    public static class DuelMatch {
        public final int arenaId;
        public final UUID p1;
        public final UUID p2;
        public final Location p1Restore;
        public final Location p2Restore;
        public final ItemStack[] p1Inv;
        public final ItemStack[] p2Inv;

        public DuelMatch(int arenaId, UUID p1, UUID p2, Location p1Restore, Location p2Restore, ItemStack[] p1Inv, ItemStack[] p2Inv) {
            this.arenaId = arenaId;
            this.p1 = p1;
            this.p2 = p2;
            this.p1Restore = p1Restore;
            this.p2Restore = p2Restore;
            this.p1Inv = p1Inv;
            this.p2Inv = p2Inv;
        }
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        
        if (getCommand("sell") != null) getCommand("sell").setExecutor(this);
        if (getCommand("shop") != null) getCommand("shop").setExecutor(this);
        if (getCommand("derpshop") != null) getCommand("derpshop").setExecutor(this);
        if (getCommand("spawn") != null) getCommand("spawn").setExecutor(this);
        if (getCommand("auction") != null) getCommand("auction").setExecutor(this);
        if (getCommand("duel") != null) getCommand("duel").setExecutor(this);
        if (getCommand("dual") != null) getCommand("dual").setExecutor(this);
        if (getCommand("bh") != null) getCommand("bh").setExecutor(this);
        if (getCommand("rtp") != null) getCommand("rtp").setExecutor(this);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateScoreboard(player);
            }
        }, 20L, 20L);

        // Hourly reward tracking tasks
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                hoursPlayedMap.put(uuid, hoursPlayedMap.getOrDefault(uuid, 0) + 1);
                keysMap.put(uuid, keysMap.getOrDefault(uuid, 0) + 1);
                player.sendMessage(Component.text("🎉 You received 1 Hour Played and 1 Key reward!", NamedTextColor.GOLD));
            }
        }, 72000L, 72000L);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        hoursPlayedMap.putIfAbsent(uuid, 0);
        erpiesMap.putIfAbsent(uuid, 100);
        derpiesMap.putIfAbsent(uuid, 10);
        keysMap.putIfAbsent(uuid, 0);
        killsMap.putIfAbsent(uuid, 0);
        deathsMap.putIfAbsent(uuid, 0);

        regularKeysMap.putIfAbsent(uuid, 0);
        crimsonKeysMap.putIfAbsent(uuid, 0);
        echoKeysMap.putIfAbsent(uuid, 0);

        updateScoreboard(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (activeMatches.containsKey(uuid)) {
            DuelMatch match = activeMatches.get(uuid);
            Player winner = (match.p1.equals(uuid)) ? Bukkit.getPlayer(match.p2) : Bukkit.getPlayer(match.p1);
            endDuel(match, winner, player);
        }

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
        }

        if (activeMatches.containsKey(victimUUID)) {
            event.setKeepInventory(true);
            event.getDrops().clear();
            
            DuelMatch match = activeMatches.get(victimUUID);
            Player winner = (match.p1.equals(victimUUID)) ? Bukkit.getPlayer(match.p2) : Bukkit.getPlayer(match.p1);
            endDuel(match, winner, victim);
        }
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
            Location spawnLoc = getSpawnLocation();
            buildCabinIfNeeded(spawnLoc);
            player.teleport(spawnLoc);
            player.sendMessage(Component.text("🚀 Teleported to spawn protection cabin!", NamedTextColor.GREEN));
            return true;
        }

        if (command.getName().equalsIgnoreCase("auction")) {
            openAuctionGui(player, null);
            return true;
        }

        if (command.getName().equalsIgnoreCase("bh")) {
            openBountyHunter(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("rtp")) {
            openRtpGui(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("duel") || command.getName().equalsIgnoreCase("dual")) {
            if (args.length == 0) {
                player.sendMessage(Component.text("⚔️ Usage: /duel <player> or /duel accept <player>", NamedTextColor.RED));
                return true;
            }

            if (args[0].equalsIgnoreCase("accept")) {
                if (args.length < 2) {
                    player.sendMessage(Component.text("⚔️ Usage: /duel accept <player>", NamedTextColor.RED));
                    return true;
                }
                Player challenger = Bukkit.getPlayer(args[1]);
                if (challenger == null) {
                    player.sendMessage(Component.text("❌ Challenger not found!", NamedTextColor.RED));
                    return true;
                }

                UUID challengerUUID = challenger.getUniqueId();
                if (pendingInvites.containsKey(challengerUUID) && pendingInvites.get(challengerUUID).equals(player.getUniqueId())) {
                    pendingInvites.remove(challengerUUID);
                    startDuel(challenger, player);
                } else {
                    player.sendMessage(Component.text("❌ No pending duel request from this player!", NamedTextColor.RED));
                }
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(Component.text("❌ Player not found!", NamedTextColor.RED));
                return true;
            }
            if (target.equals(player)) {
                player.sendMessage(Component.text("❌ You cannot duel yourself!", NamedTextColor.RED));
                return true;
            }

            pendingInvites.put(player.getUniqueId(), target.getUniqueId());
            player.sendMessage(Component.text("⚔️ Duel request sent to " + target.getName() + "!", NamedTextColor.GREEN));
            target.sendMessage(Component.text("⚔️ " + player.getName() + " has challenged you to a duel!", NamedTextColor.YELLOW)
                    .append(Component.text("\nType ", NamedTextColor.GOLD))
                    .append(Component.text("/duel accept " + player.getName(), NamedTextColor.GREEN))
                    .append(Component.text(" to accept!", NamedTextColor.GOLD)));
            return true;
        }

        return false;
    }

    // --- Spawn Protection & Cabin Generator ---
    private Location getSpawnLocation() {
        World world = Bukkit.getWorlds().get(0);
        return new Location(world, 0.5, 100, 0.5);
    }

    private void buildCabinIfNeeded(Location center) {
        Location check = center.clone().add(0, 1, 0);
        if (check.getBlock().getType() != Material.AIR) return;

        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY() - 1;
        int cz = center.getBlockZ();

        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                world.getBlockAt(cx + x, cy, cz + z).setType(Material.OAK_PLANKS);
            }
        }

        for (int y = 0; y < 4; y++) {
            for (int x = -3; x <= 3; x++) {
                for (int z = -3; z <= 3; z++) {
                    if (Math.abs(x) == 3 || Math.abs(z) == 3) {
                        Material wallMat = (x == -3 && z == 0) ? Material.AIR : Material.OAK_PLANKS;
                        if (y == 3) wallMat = Material.OAK_PLANKS;
                        world.getBlockAt(cx + x, cy + 1 + y, cz + z).setType(wallMat);
                    } else {
                        world.getBlockAt(cx + x, cy + 1 + y, cz + z).setType(Material.AIR);
                    }
                }
            }
        }

        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                world.getBlockAt(cx + x, cy + 5, cz + z).setType(Material.SPRUCE_SLAB);
            }
        }

        world.getBlockAt(cx - 2, cy + 1, cz - 2).setType(Material.CAMPFIRE);
        world.getBlockAt(cx + 2, cy + 1, cz - 2).setType(Material.CAMPFIRE);
    }

    private boolean isInSpawnRadius(Location loc) {
        Location spawn = getSpawnLocation();
        if (!loc.getWorld().equals(spawn.getWorld())) return false;
        return loc.distanceSquared(spawn) <= 2500;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.SURVIVAL) {
            if (isInSpawnRadius(event.getBlock().getLocation())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Component.text("❌ You cannot break blocks at spawn!", NamedTextColor.RED));
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.SURVIVAL) {
            if (isInSpawnRadius(event.getBlock().getLocation())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Component.text("❌ You cannot place blocks at spawn!", NamedTextColor.RED));
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker) {
            if (attacker.getGameMode() == GameMode.SURVIVAL) {
                if (isInSpawnRadius(event.getEntity().getLocation())) {
                    if (activeMatches.containsKey(attacker.getUniqueId())) {
                        return;
                    }
                    event.setCancelled(true);
                    attacker.sendMessage(Component.text("❌ PvP and damage are disabled at spawn!", NamedTextColor.RED));
                }
            }
        }
    }

    // --- Sell GUI handler ---
    @EventHandler
    public void onSellClose(InventoryCloseEvent event) {
        if (!event.getView().getTitle().equals("Drop items here to Sell")) return;
        
        Player player = (Player) event.getPlayer();
        Inventory inv = event.getInventory();
        int totalPayout = 0;

        for (ItemStack item : inv.getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;

            int valueMultiplier = item.getAmount();
            int baseValue = getItemRarityValue(item.getType());
            totalPayout += (baseValue * valueMultiplier);
        }

        if (totalPayout > 0) {
            UUID uuid = player.getUniqueId();
            erpiesMap.put(uuid, erpiesMap.getOrDefault(uuid, 0) + totalPayout);
            player.sendMessage(Component.text("💰 Items sold! Received: ", NamedTextColor.GREEN)
                    .append(Component.text(totalPayout + " Erpies", NamedTextColor.WHITE)));
        }
    }

    private int getItemRarityValue(Material material) {
        String name = material.name();
        if (name.contains("DIAMOND") || name.contains("NETHERITE") || name.contains("EMERALD") || name.equals("BEACON") || name.equals("ENCHANTED_GOLDEN_APPLE")) {
            return random.nextInt(500) + 500;
        } else if (name.contains("GOLD") || name.contains("IRON") || name.contains("REDSTONE") || name.contains("ENDER")) {
            return random.nextInt(200) + 100;
        }
        return random.nextInt(24) + 1;
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
        if (!title.contains("Shop") && !title.contains("Auction") && !title.contains("Bounty") && !title.equals("Random Teleport")) return;

        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

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
                    erpiesMap.put(uuid, erpiesMap.getOrDefault(uuid, 0) + 1000);
                    player.sendMessage(Component.text("💰 Traded 1 Player Head for 1000 Erpies!", NamedTextColor.GREEN));
                } else {
                    derpiesMap.put(uuid, derpiesMap.getOrDefault(uuid, 0) + 50);
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

            int playerDerpies = derpiesMap.getOrDefault(uuid, 0);
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
            } else if (clicked.getType() == Material.ECHO_SHARD) {
                echoKeysMap.put(uuid, echoKeysMap.getOrDefault(uuid, 0) + 1);
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
                ItemStack cursorItem = event.getCursor();
                if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                    event.setCursor(new ItemStack(Material.AIR));
                    openSignInput(player, SignAction.LIST_PRICE, cursorItem.clone(), "list the price");
                } else {
                    openMyListings(player);
                }
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

                    int playerMoney = erpiesMap.getOrDefault(uuid, 0);
                    if (playerMoney < clickedListing.price) {
                        player.sendMessage(Component.text("❌ You don't have enough Erpies!", NamedTextColor.RED));
                        return;
                    }

                    erpiesMap.put(uuid, playerMoney - clickedListing.price);
                    erpiesMap.put(clickedListing.seller, erpiesMap.getOrDefault(clickedListing.seller, 0) + clickedListing.price);

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

        // 6. Normal Categories Shop submenus
        int cost = getPrice(clicked.getType(), title);
        if (cost == -1) return;

        int playerMoney = erpiesMap.getOrDefault(uuid, 0);
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
        inv.setItem(47, createGuiItem(Material.CHEST, "Your listed items", NamedTextColor.GREEN, "Place cursor item here to list it"));

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
            } else if (pending.action == SignAction.LIST_PRICE) {
                if (input.isEmpty()) {
                    player.sendMessage(Component.text("❌ Listing cancelled. Item returned.", NamedTextColor.RED));
                    player.getInventory().addItem(pending.item);
                } else {
                    try {
                        int price = Integer.parseInt(input);
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
        // 5-second countdown using title
        new BukkitRunnable() {
            int countdown = 5;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
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
                while (attempts < 20) {
                    int x = random.nextInt(fMax - fMin) + fMin;
                    int z = random.nextInt(fMax - fMin) + fMin;
                    int y = finalWorld.getHighestBlockYAt(x, z) + 1;
                    Location loc = new Location(finalWorld, x + 0.5, y, z + 0.5);
                    if (!loc.getBlock().getType().isSolid() && !loc.clone().subtract(0, 1, 0).getBlock().getType().isAir()) {
                        safe = loc;
                        break;
                    }
                    attempts++;
                }
                if (safe == null) {
                    int x = random.nextInt(fMax - fMin) + fMin;
                    int z = random.nextInt(fMax - fMin) + fMin;
                    safe = new Location(finalWorld, x + 0.5, finalWorld.getHighestBlockYAt(x, z) + 1, z + 0.5);
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

    // --- Bounty Hunter ---
    private void openBountyHunter(Player player) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text("Bounty Hunter"));
        inv.setItem(3, createGuiItem(Material.DIAMOND, "Trade Head for Erpies", NamedTextColor.AQUA, "Receive 1000 Erpies"));
        inv.setItem(5, createGuiItem(Material.AMETHYST_SHARD, "Trade Head for Derpies", NamedTextColor.LIGHT_PURPLE, "Receive 50 Derpies"));
        player.openInventory(inv);
    }

    // --- Duel System ---
    private Location getArenaCenter(int id) {
        World world = Bukkit.getWorlds().get(0);
        return new Location(world, 10000 + (id * 200), 100, 10000);
    }

    private void rebuildArena(int id) {
        Location center = getArenaCenter(id);
        World world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        for (int x = -15; x <= 15; x++) {
            for (int z = -15; z <= 15; z++) {
                world.getBlockAt(cx + x, cy - 1, cz + z).setType(Material.GRASS_BLOCK);
                for (int y = 0; y < 10; y++) {
                    world.getBlockAt(cx + x, cy + y, cz + z).setType(Material.AIR);
                }
            }
        }

        for (int x = -16; x <= 16; x++) {
            for (int z = -16; z <= 16; z++) {
                if (Math.abs(x) == 16 || Math.abs(z) == 16) {
                    for (int y = 0; y < 4; y++) {
                        world.getBlockAt(cx + x, cy + y, cz + z).setType(Material.GLASS);
                    }
                }
            }
        }
    }

    private void startDuel(Player p1, Player p2) {
        int freeArena = -1;
        for (int i = 0; i < 10; i++) {
            if (!occupiedArenas[i]) {
                freeArena = i;
                break;
            }
        }

        if (freeArena == -1) {
            p1.sendMessage(Component.text("❌ All duel arenas are currently in use! Please try again later.", NamedTextColor.RED));
            p2.sendMessage(Component.text("❌ All duel arenas are currently in use! Please try again later.", NamedTextColor.RED));
            return;
        }

        occupiedArenas[freeArena] = true;
        rebuildArena(freeArena);

        Location center = getArenaCenter(freeArena);
        Location spawn1 = center.clone().add(10, 0, 0);
        Location spawn2 = center.clone().add(-10, 0, 0);

        DuelMatch match = new DuelMatch(
                freeArena,
                p1.getUniqueId(),
                p2.getUniqueId(),
                p1.getLocation(),
                p2.getLocation(),
                p1.getInventory().getContents(),
                p2.getInventory().getContents()
        );

        activeMatches.put(p1.getUniqueId(), match);
        activeMatches.put(p2.getUniqueId(), match);

        p1.teleport(spawn1);
        p2.teleport(spawn2);

        p1.setHealth(p1.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        p2.setHealth(p2.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
        p1.setFoodLevel(20);
        p2.setFoodLevel(20);

        p1.sendMessage(Component.text("⚔️ The duel has begun!", NamedTextColor.GREEN));
        p2.sendMessage(Component.text("⚔️ The duel has begun!", NamedTextColor.GREEN));
    }

    private void endDuel(DuelMatch match, Player winner, Player loser) {
        activeMatches.remove(match.p1);
        activeMatches.remove(match.p2);
        occupiedArenas[match.arenaId] = false;

        if (winner != null) {
            winner.sendMessage(Component.text("🏆 You won the duel!", NamedTextColor.GOLD));
            
            UUID wUUID = winner.getUniqueId();
            derpiesMap.put(wUUID, derpiesMap.getOrDefault(wUUID, 0) + 10);

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta skullMeta = skull.getItemMeta();
            if (skullMeta instanceof org.bukkit.inventory.meta.SkullMeta sm) {
                sm.setOwningPlayer(loser);
                sm.displayName(Component.text(loser.getName() + "'s Head", NamedTextColor.YELLOW));
                skull.setItemMeta(sm);
            }
            winner.getInventory().addItem(skull);
            winner.sendMessage(Component.text("🎁 Received " + loser.getName() + "'s Head & 10 Derpies!", NamedTextColor.GREEN));
        }

        if (loser != null) {
            loser.sendMessage(Component.text("💀 You lost the duel!", NamedTextColor.RED));
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            Player p1 = Bukkit.getPlayer(match.p1);
            Player p2 = Bukkit.getPlayer(match.p2);

            if (p1 != null) {
                p1.teleport(match.p1Restore);
                p1.getInventory().setContents(match.p1Inv);
                p1.setHealth(p1.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
                p1.setFoodLevel(20);
                p1.setFireTicks(0);
            }
            if (p2 != null) {
                p2.teleport(match.p2Restore);
                p2.getInventory().setContents(match.p2Inv);
                p2.setHealth(p2.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
                p2.setFoodLevel(20);
                p2.setFireTicks(0);
            }
            
            rebuildArena(match.arenaId);
        }, 10L);
    }

    // --- Scoreboard ---
    public void updateScoreboard(Player player) {
        Scoreboard board = player.getScoreboard();
        if (board == Bukkit.getScoreboardManager().getMainScoreboard()) {
            board = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(board);
        }

        Objective oldObj = board.getObjective("smp_board");
        if (oldObj != null) oldObj.unregister();

        Component title = Component.text("play.", NamedTextColor.BLUE)
                .append(Component.text("theerpsmp", NamedTextColor.GREEN))
                .append(Component.text(".net", NamedTextColor.GOLD));

        Objective obj = board.registerNewObjective("smp_board", Criteria.DUMMY, title);
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.numberFormat(io.papermc.paper.scoreboard.numbers.NumberFormat.blank());

        UUID uuid = player.getUniqueId();

        addScoreboardRow(board, obj, "HoursPlayed", NamedTextColor.AQUA, hoursPlayedMap.getOrDefault(uuid, 0), 6, "§1");
        addScoreboardRow(board, obj, "Erpies", NamedTextColor.GREEN, erpiesMap.getOrDefault(uuid, 0), 5, "§2");
        addScoreboardRow(board, obj, "Derpies", NamedTextColor.LIGHT_PURPLE, derpiesMap.getOrDefault(uuid, 0), 4, "§3");
        addScoreboardRow(board, obj, "Keys", NamedTextColor.BLUE, keysMap.getOrDefault(uuid, 0), 3, "§4");
        addScoreboardRow(board, obj, "Kills", NamedTextColor.DARK_GREEN, killsMap.getOrDefault(uuid, 0), 2, "§5");
        addScoreboardRow(board, obj, "Deaths", NamedTextColor.RED, deathsMap.getOrDefault(uuid, 0), 1, "§6");
    }

    private void addScoreboardRow(Scoreboard board, Objective obj, String label, NamedTextColor labelColor, int value, int scoreIndex, String placeholderId) {
        String suffix = (value == 0) ? "" : ":";
        Component rowText = Component.text(label + ": ", labelColor)
                .append(Component.text(value, NamedTextColor.WHITE))
                .append(Component.text(suffix, labelColor));

        String teamName = "row_" + scoreIndex;
        Team team = board.getTeam(teamName);
        if (team == null) team = board.registerNewTeam(teamName);

        team.addEntry(placeholderId);
        team.prefix(rowText);

        Score score = obj.getScore(placeholderId);
        score.setScore(scoreIndex);
    }
}