package com.blockstock.gui;

import com.blockstock.config.ConfigManager;
import com.blockstock.service.ActiveEvent;
import com.blockstock.service.DeliveryService;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class DeliveryGUI {
    private static final Logger logger = Logger.getLogger("VeloDelivery");
    private final ConfigManager configManager;
    private final DeliveryService deliveryService;

    private final Map<String, String> itemDisplayNames = new ConcurrentHashMap<>();
    private final Map<String, Double> itemPrices = new ConcurrentHashMap<>();
    private final Map<String, String> categoryDisplayNames = new ConcurrentHashMap<>();
    private final Map<String, String> deliveryDisplayNames = new ConcurrentHashMap<>();
    private final Map<String, String> categoryHeads = new ConcurrentHashMap<>();
    private final Map<UUID, String> chestSelectionMode = new ConcurrentHashMap<>();
    private final Map<UUID, Long> chestSelectionTimeout = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerNameCache = new ConcurrentHashMap<>();

    // Dil desteği
    private Supplier<String> languageSupplier;
    private File dataFolder;
    private YamlConfiguration langConfig;

    // Head Textures
    private static final String HEAD_DELIVERY = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGFhMTg3ZmVkZTg4ZGUwMDJjYmQ5MzA1NzVlYjdiYTQ4ZDNiMWEwNmQ5NjFiZGM1MzU4MDA3NTBhZjc2NDkyNiJ9fX0=";
    private static final String HEAD_CHEST = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDVjNmRjMmJiZjUxYzM2Y2ZjNzcxNDU4NWE2YTU2ODNlZjJiMTRkNDdkOGZmNzE0NjU0YTg5M2Y1ZGE2MjIifX19";
    private static final String HEAD_INVENTORY = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWQ5Y2M1OGFkMjVhMWFiMTZkMzZiYjVkNmQ0OTNjOGY1ODkxMjMyNTEyYjVjNDg0YzY2Y2ZkM2I0YjZhYzRiNCJ9fX0=";
    private static final String HEAD_TOP = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjdkYzNlMjlhMDkyM2U1MmVjZWU2YjRjOWQ1MzNhNzllNzRiYjZiZWQ1NDFiNDk1YTEzYWJkMzU5NjI3NjUzIn19fQ==";
    private static final String HEAD_HELP = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmFkYzA0OGE3Y2U3OGY3ZGFkNzJhMDdkYTI3ZDg1YzA5MTY4ODFlNTUyMmVlZWQxZTNkYWYyMTdhMzhjMWEifX19";
    private static final String HEAD_BACK = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmQ2OWUwNmU1ZGFkZmQ4NGU1ZjNkMWMyMTA2M2YyNTUzYjJmYTk0NWVlMWQ0ZDcxNTJmZGM1NDI1YmMxMmE5In19fQ==";
    private static final String HEAD_WAITING = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmNlZjlhYTE0ZTg4NDc3M2VhYzEzNGE0ZWU4OTcyMDYzZjQ2NmRlNjc4MzYzY2Y3YjFhMjFhODViNyJ9fX0=";

    // SmallCaps
    private static final String NORMAL = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String SMALL = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀsᴛᴜᴠᴡxʏᴢ";

    // Dinamik başlıklar - dil dosyasından yüklenir
    public static final String MAIN_TITLE_TR = "ᴛᴇsʟɪᴍᴀᴛ ᴍᴇɴᴜ̈sᴜ̈";
    public static final String MAIN_TITLE_EN = "ᴅᴇʟɪᴠᴇʀʏ ᴍᴇɴᴜ";
    public static final String DELIVERY_PREFIX_TR = "ᴛᴇsʟɪᴍᴀᴛ: ";
    public static final String DELIVERY_PREFIX_EN = "ᴅᴇʟɪᴠᴇʀʏ: ";
    public static final String LEADERBOARD_TITLE_TR = "sɪʀᴀʟᴀᴍᴀ";
    public static final String LEADERBOARD_TITLE_EN = "ʟᴇᴀᴅᴇʀʙᴏᴀʀᴅ";

    // Uyumluluk için varsayılan değerler
    public static final String MAIN_TITLE = MAIN_TITLE_TR;
    public static final String DELIVERY_PREFIX = DELIVERY_PREFIX_TR;
    public static final String LEADERBOARD_TITLE = LEADERBOARD_TITLE_TR;

    public DeliveryGUI(ConfigManager cm, DeliveryService ds) {
        this.configManager = cm;
        this.deliveryService = ds;
        initDefaults();
    }

    private void initDefaults() {
        categoryDisplayNames.put("farm", "Çiftlik");
        categoryDisplayNames.put("ore", "Maden");
        categoryDisplayNames.put("block", "Blok");
        categoryDisplayNames.put("food", "Yiyecek");
        categoryDisplayNames.put("wood", "Odun");
        categoryDisplayNames.put("rare", "Nadir");
        categoryDisplayNames.put("nether", "Nether");
        categoryDisplayNames.put("mob", "Mob Drop");
        categoryDisplayNames.put("end", "End");
        categoryDisplayNames.put("dye", "Boya");
        categoryDisplayNames.put("tool", "Alet");
        categoryDisplayNames.put("armor", "Zırh");
        categoryDisplayNames.put("combat", "Savaş");
        categoryDisplayNames.put("potion", "İksir");
        categoryDisplayNames.put("decoration", "Dekorasyon");
        categoryDisplayNames.put("redstone", "Kızıltaş");
        categoryDisplayNames.put("misc", "Çeşitli");
    }

    /**
     * Dil sağlayıcısını ayarlar
     */
    public void setLanguageSupplier(Supplier<String> supplier) {
        this.languageSupplier = supplier;
    }

    /**
     * Data folder'ı ayarlar (dil dosyalarını okumak için)
     */
    public void setDataFolder(File folder) {
        this.dataFolder = folder;
        reloadLanguage();
    }

    /**
     * Dil dosyasını yeniden yükler
     */
    public void reloadLanguage() {
        if (dataFolder == null)
            return;
        String lang = getCurrentLanguage();
        File langFile = new File(dataFolder, "lang/" + lang + ".yml");
        if (langFile.exists()) {
            langConfig = YamlConfiguration.loadConfiguration(langFile);
        }
    }

    /**
     * Mevcut dili döndürür
     */
    private String getCurrentLanguage() {
        if (languageSupplier != null) {
            return languageSupplier.get();
        }
        return "tr";
    }

    /**
     * Dil dosyasından mesaj alır
     */
    public String getLang(String path, String defaultValue) {
        if (langConfig != null) {
            return langConfig.getString(path, defaultValue);
        }
        return defaultValue;
    }

    /**
     * Mevcut dile göre ana menü başlığını döndürür
     */
    public String getMainTitle() {
        return "en".equals(getCurrentLanguage()) ? MAIN_TITLE_EN : MAIN_TITLE_TR;
    }

    /**
     * Mevcut dile göre teslimat prefix'ini döndürür
     */
    public String getDeliveryPrefix() {
        return "en".equals(getCurrentLanguage()) ? DELIVERY_PREFIX_EN : DELIVERY_PREFIX_TR;
    }

    /**
     * Mevcut dile göre sıralama başlığını döndürür
     */
    public String getLeaderboardTitle() {
        return "en".equals(getCurrentLanguage()) ? LEADERBOARD_TITLE_EN : LEADERBOARD_TITLE_TR;
    }

    public void loadItemsConfig(File dataFolder) {
        File itemsFile = new File(dataFolder, "items.yml");
        if (!itemsFile.exists()) {
            logger.warning("items.yml bulunamadi: " + itemsFile.getAbsolutePath());
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(itemsFile);
        var itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null) {
            logger.warning("items.yml'de 'items' section bulunamadi!");
            return;
        }
        int loadedCount = 0;
        for (String key : itemsSection.getKeys(false)) {
            String displayName = itemsSection.getString(key + ".display-name");
            double price = itemsSection.getDouble(key + ".price", 0);
            if (displayName != null) {
                itemDisplayNames.put(key.toUpperCase(), displayName);
                loadedCount++;
            }
            if (price > 0)
                itemPrices.put(key.toUpperCase(), price);
        }
        logger.info(loadedCount + " esya yuklendi (items.yml)");
    }

    public static String sc(String text) {
        if (text == null)
            return "";
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            int idx = NORMAL.indexOf(c);
            sb.append(idx >= 0 ? SMALL.charAt(idx) : c);
        }
        return sb.toString();
    }

    public String getItemDisplayName(String itemName) {
        if (itemName == null)
            return "Bilinmiyor";
        return itemDisplayNames.getOrDefault(itemName.toUpperCase(), itemName.replace("_", " "));
    }

    public double getItemPrice(String itemName) {
        return itemPrices.getOrDefault(itemName.toUpperCase(), 0.0);
    }

    public String getCategoryDisplayName(String catName) {
        return categoryDisplayNames.getOrDefault(catName.toLowerCase(), catName);
    }

    public String getDeliveryDisplayName(String deliveryName) {
        return deliveryDisplayNames.getOrDefault(deliveryName.toLowerCase(), deliveryName);
    }

    public void setCategoryDisplayName(String cat, String name) {
        categoryDisplayNames.put(cat.toLowerCase(), name);
    }

    public void setDeliveryDisplayName(String del, String name) {
        deliveryDisplayNames.put(del.toLowerCase(), name);
    }

    public void setCategoryHead(String cat, String texture) {
        categoryHeads.put(cat.toLowerCase(), texture);
    }

    public void setItemDisplayName(String item, String name) {
        itemDisplayNames.put(item.toUpperCase(), name);
    }

    public void setChestCapacity(int capacity) {
    }

    public void cachePlayerName(UUID id, String name) {
        if (id != null && name != null)
            playerNameCache.put(id, name);
    }

    public void startChestSelection(Player player, String deliveryName) {
        chestSelectionMode.put(player.getUniqueId(), deliveryName);
        chestSelectionTimeout.put(player.getUniqueId(), System.currentTimeMillis() + 10000);
    }

    public boolean isInChestSelectionMode(UUID id) {
        Long timeout = chestSelectionTimeout.get(id);
        if (timeout == null)
            return false;
        if (System.currentTimeMillis() > timeout) {
            cancelChestSelection(id);
            return false;
        }
        return chestSelectionMode.containsKey(id);
    }

    public String getChestSelectionDelivery(UUID id) {
        return chestSelectionMode.get(id);
    }

    public void cancelChestSelection(UUID id) {
        chestSelectionMode.remove(id);
        chestSelectionTimeout.remove(id);
    }

    public static boolean isDeliveryGUI(String title) {
        return title.contains(MAIN_TITLE_TR) || title.contains(MAIN_TITLE_EN) ||
                title.contains(DELIVERY_PREFIX_TR) || title.contains(DELIVERY_PREFIX_EN) ||
                title.contains(LEADERBOARD_TITLE_TR) || title.contains(LEADERBOARD_TITLE_EN);
    }

    public static boolean isMainMenu(String title) {
        return title.contains(MAIN_TITLE_TR) || title.contains(MAIN_TITLE_EN);
    }

    public static boolean isDeliveryMenu(String title) {
        return title.contains(DELIVERY_PREFIX_TR) || title.contains(DELIVERY_PREFIX_EN);
    }

    public static boolean isLeaderboardMenu(String title) {
        return title.contains(LEADERBOARD_TITLE_TR) || title.contains(LEADERBOARD_TITLE_EN);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ANA MENÜ
    // ═══════════════════════════════════════════════════════════════════════════

    public void openMainMenu(Player player) {
        boolean isEnglish = "en".equals(getCurrentLanguage());
        Inventory inv = Bukkit.createInventory(null, 36, getMainTitle());

        // Arka plan - siyah cam paneller
        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 36; i++)
            inv.setItem(i, bg);

        // Dekoratif kenar (üst ve alt satır gri cam)
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(27 + i, border);
        }

        // Başlık ikonu (slot 4 - üst orta)
        String titleName = sc("VeloDelivery");
        String titleLore = isEnglish ? sc("delivery event system") : sc("teslimat etkinlik sistemi");
        inv.setItem(4, createHead(HEAD_DELIVERY, "§e§l" + titleName, Arrays.asList(
                "", "§7" + titleLore, "")));

        // Aktif teslimatlar - satır 1 (slot 9-17)
        var events = deliveryService.getAllActiveEvents();
        int[] centeredSlots = getCenteredSlots(events.size(), 1);
        for (int i = 0; i < events.size() && i < centeredSlots.length; i++) {
            inv.setItem(centeredSlots[i], createDeliveryItem(player, events.get(i)));
        }

        // Bekleyen teslimatlar - satır 2 (slot 18-26)
        var allDeliveries = configManager.getDeliveryConfig().getEnabledDeliveries();
        List<com.blockstock.model.DeliveryDefinition> waitingList = new java.util.ArrayList<>();
        for (var delivery : allDeliveries) {
            boolean isActive = events.stream().anyMatch(e -> e.getDeliveryName().equals(delivery.name()));
            if (!isActive)
                waitingList.add(delivery);
        }
        int[] waitingCenteredSlots = getCenteredSlots(Math.min(waitingList.size(), 5), 2);
        for (int i = 0; i < waitingList.size() && i < waitingCenteredSlots.length; i++) {
            inv.setItem(waitingCenteredSlots[i], createWaitingDeliveryItem(waitingList.get(i)));
        }

        // Alt butonlar - satır 3 (simetrik: slot 30 ve 32)
        String leaderboardName = isEnglish ? sc("leaderboard") : sc("siralama");
        String leaderboardLore = isEnglish ? sc("see top deliverers") : sc("en cok teslim edenleri gor");
        String clickText = isEnglish ? sc("click") : sc("tikla");
        inv.setItem(30, createHead(HEAD_TOP, "§b" + leaderboardName, Arrays.asList(
                "", "§7" + leaderboardLore, "", "§a" + clickText)));

        String helpName = isEnglish ? sc("help") : sc("yardim");
        String helpLore = isEnglish ? sc("learn about the system") : sc("teslimat sistemini ogren");
        inv.setItem(32, createHead(HEAD_HELP, "§f" + helpName, Arrays.asList(
                "", "§7" + helpLore, "", "§a" + clickText)));

        player.openInventory(inv);
    }

    /**
     * Belirtilen eleman sayısını verilen satıra ortalayarak slotları döndürür.
     * satır 0-5 arasında (6 satırlık 54-slot envanter)
     */
    private int[] getCenteredSlots(int count, int row) {
        if (count <= 0)
            return new int[0];
        count = Math.min(count, 5);
        int rowStart = row * 9;
        return switch (count) {
            case 1 -> new int[] { rowStart + 4 };
            case 2 -> new int[] { rowStart + 3, rowStart + 5 };
            case 3 -> new int[] { rowStart + 2, rowStart + 4, rowStart + 6 };
            case 4 -> new int[] { rowStart + 1, rowStart + 3, rowStart + 5, rowStart + 7 };
            case 5 -> new int[] { rowStart + 2, rowStart + 3, rowStart + 4, rowStart + 5, rowStart + 6 };
            default -> new int[0];
        };
    }

    /**
     * Click handler'dan erişim için public wrapper.
     */
    public int[] getCenteredSlotsPublic(int count, int row) {
        return getCenteredSlots(count, row);
    }

    private ItemStack createWaitingDeliveryItem(com.blockstock.model.DeliveryDefinition delivery) {
        boolean isEnglish = "en".equals(getCurrentLanguage());
        String deliveryName = getDeliveryDisplayName(delivery.name());
        String schedule = delivery.schedule().start();
        String nextTime = parseScheduleToReadable(schedule);

        String waitingText = isEnglish ? sc("waiting") : sc("bekliyor");
        String startText = isEnglish ? sc("start") : sc("baslangic");
        String notStartedText = isEnglish ? sc("not started yet") : sc("henuz baslamadi");

        return createHead(HEAD_WAITING, "§7" + deliveryName, Arrays.asList(
                "",
                "§8" + waitingText,
                "",
                "§7" + startText + " §e" + nextTime,
                "",
                "§8" + notStartedText));
    }

    private String parseScheduleToReadable(String schedule) {
        if (schedule == null || schedule.isEmpty()) {
            return "en".equals(getCurrentLanguage()) ? "Unknown" : "Bilinmiyor";
        }
        String lower = schedule.toLowerCase();
        boolean isEnglish = "en".equals(getCurrentLanguage());

        Map<String, String> daysTR = Map.of(
                "every day", "Her gün", "every monday", "Her Pazartesi",
                "every tuesday", "Her Salı", "every wednesday", "Her Çarşamba",
                "every thursday", "Her Perşembe", "every friday", "Her Cuma",
                "every saturday", "Her Cumartesi", "every sunday", "Her Pazar");
        Map<String, String> daysEN = Map.of(
                "every day", "Every day", "every monday", "Every Monday",
                "every tuesday", "Every Tuesday", "every wednesday", "Every Wednesday",
                "every thursday", "Every Thursday", "every friday", "Every Friday",
                "every saturday", "Every Saturday", "every sunday", "Every Sunday");

        Map<String, String> days = isEnglish ? daysEN : daysTR;
        for (var entry : days.entrySet()) {
            if (lower.contains(entry.getKey())) {
                String time = schedule.replaceAll("(?i)" + entry.getKey().replace(" ", "\\s+") + "\\s*", "").trim();
                return entry.getValue() + " " + time;
            }
        }
        return schedule;
    }

    private ItemStack createDeliveryItem(Player player, ActiveEvent event) {
        boolean isEnglish = "en".equals(getCurrentLanguage());
        String itemName = getItemDisplayName(event.getResolvedItem());
        String deliveryName = getDeliveryDisplayName(event.getDeliveryName());
        int playerCount = event.getPlayerDeliveries().getOrDefault(player.getUniqueId(), 0);
        int totalCount = event.getPlayerDeliveries().values().stream().mapToInt(Integer::intValue).sum();
        int rank = calculateRank(event, player.getUniqueId());

        String itemText = isEnglish ? sc("item") : sc("esya");
        String categoryText = isEnglish ? sc("category") : sc("kategori");
        String totalText = isEnglish ? sc("total") : sc("toplam");
        String yoursText = isEnglish ? sc("yours") : sc("senin");
        String rankText = isEnglish ? sc("rank") : sc("sira");
        String clickDeliverText = isEnglish ? sc("click to deliver") : sc("tikla ve teslim et");

        return createHead(HEAD_DELIVERY, "§a" + deliveryName, Arrays.asList(
                "",
                "§7" + itemText + " §f" + itemName,
                "§7" + categoryText + " §f" + getCategoryDisplayName(event.getResolvedCategory()),
                "",
                "§7" + totalText + " §e" + totalCount,
                "§7" + yoursText + " §a" + playerCount,
                "§7" + rankText + " §e#" + (rank > 0 ? rank : "-"),
                "",
                "§a" + clickDeliverText));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // TESLİMAT DETAY MENÜSÜ
    // ═══════════════════════════════════════════════════════════════════════════

    public void openDeliveryMenu(Player player, ActiveEvent event) {
        boolean isEnglish = "en".equals(getCurrentLanguage());
        String deliveryName = getDeliveryDisplayName(event.getDeliveryName());
        Inventory inv = Bukkit.createInventory(null, 45, getDeliveryPrefix() + deliveryName);

        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; i++)
            inv.setItem(i, bg);

        // Dekoratif kenar
        ItemStack border = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border);
            inv.setItem(36 + i, border);
        }

        String itemName = getItemDisplayName(event.getResolvedItem());
        int playerCount = event.getPlayerDeliveries().getOrDefault(player.getUniqueId(), 0);
        int totalCount = event.getPlayerDeliveries().values().stream().mapToInt(Integer::intValue).sum();
        int rank = calculateRank(event, player.getUniqueId());

        // Dil string'leri
        String requiredItemText = isEnglish ? sc("required item") : sc("istenen esya");
        String categoryText = isEnglish ? sc("category") : sc("kategori");
        String totalDeliveriesText = isEnglish ? sc("total deliveries") : sc("toplam teslimat");
        String yourDeliveriesText = isEnglish ? sc("your deliveries") : sc("senin teslimat");
        String yourRankText = isEnglish ? sc("your rank") : sc("siran");
        String deliverFromInvText = isEnglish ? sc("deliver from inventory") : sc("envanterden teslim et");
        String allItemsText = isEnglish ? sc("all your") : sc("envanterindeki tum");
        String itemsDeliverText = isEnglish ? sc("items will be delivered") : sc("esyalarini teslim et");
        String deliverFromChestText = isEnglish ? sc("deliver from chest") : sc("sandiktan teslim et");
        String rightClickChestText = isEnglish ? sc("right click a chest") : sc("bir sandiga sag tikla");
        String insideText = isEnglish ? sc("inside") : sc("icindeki");
        String timeLeftText = isEnglish ? sc("10 seconds to select") : sc("10 saniye suren var");
        String clickText = isEnglish ? sc("click") : sc("tikla");
        String backText = isEnglish ? sc("back") : sc("geri");
        String returnMainText = isEnglish ? sc("return to main menu") : sc("ana menuye don");

        // Bilgi (slot 13 - üst orta)
        inv.setItem(13, createHead(HEAD_DELIVERY, "§e" + deliveryName, Arrays.asList(
                "",
                "§7" + requiredItemText,
                "§f  " + itemName,
                "",
                "§7" + categoryText + " §f" + getCategoryDisplayName(event.getResolvedCategory()),
                "§7" + totalDeliveriesText + " §e" + totalCount,
                "",
                "§7" + yourDeliveriesText + " §a" + playerCount,
                "§7" + yourRankText + " §e#" + (rank > 0 ? rank : "-"))));

        // Envanterden teslim et (slot 20 - sol orta)
        inv.setItem(20, createHead(HEAD_INVENTORY, "§a" + deliverFromInvText, Arrays.asList(
                "",
                "§7" + allItemsText,
                "§f" + itemName + " §7" + itemsDeliverText,
                "",
                "§a" + clickText)));

        // Sandıktan teslim et (slot 24 - sağ orta)
        inv.setItem(24, createHead(HEAD_CHEST, "§6" + deliverFromChestText, Arrays.asList(
                "",
                "§7" + rightClickChestText,
                "§7" + insideText + " §f" + itemName,
                "§7" + itemsDeliverText,
                "",
                "§e" + timeLeftText,
                "",
                "§a" + clickText)));

        // Geri butonu (slot 40 - alt orta)
        inv.setItem(40, createHead(HEAD_BACK, "§c" + backText, Arrays.asList(
                "",
                "§7" + returnMainText)));

        player.openInventory(inv);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SIRALAMA MENÜSÜ
    // ═══════════════════════════════════════════════════════════════════════════

    public void openLeaderboard(Player player) {
        boolean isEnglish = "en".equals(getCurrentLanguage());
        Inventory inv = Bukkit.createInventory(null, 54, getLeaderboardTitle());

        ItemStack bg = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++)
            inv.setItem(i, bg);

        var events = deliveryService.getAllActiveEvents();
        if (events.isEmpty()) {
            String noActiveText = isEnglish ? sc("no active delivery") : sc("aktif teslimat yok");
            String waitingText = isEnglish ? sc("waiting for delivery event") : sc("siralama icin teslimat bekleniyor");
            inv.setItem(22, createHead(HEAD_HELP, "§7" + noActiveText, Arrays.asList(
                    "",
                    "§8" + waitingText)));
        } else {
            ActiveEvent event = events.get(0);
            var deliveries = event.getPlayerDeliveries();

            List<Map.Entry<UUID, Integer>> sorted = deliveries.entrySet().stream()
                    .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                    .limit(10)
                    .toList();

            int[] slots = { 13, 21, 23, 29, 30, 31, 32, 33, 38, 42 };
            String[] colors = { "§6", "§f", "§c", "§7", "§7", "§7", "§7", "§7", "§7", "§7" };

            String deliveriesText = isEnglish ? sc("deliveries") : sc("teslimat");
            for (int i = 0; i < sorted.size() && i < slots.length; i++) {
                var entry = sorted.get(i);
                String name = getPlayerName(entry.getKey());

                inv.setItem(slots[i], createPlayerHead(entry.getKey(),
                        colors[i] + "#" + (i + 1) + " " + name, Arrays.asList(
                                "",
                                "§7" + deliveriesText + " §e" + entry.getValue())));
            }
        }

        String backText = isEnglish ? sc("back") : sc("geri");
        String returnMainText = isEnglish ? sc("return to main menu") : sc("ana menuye don");
        inv.setItem(49, createHead(HEAD_BACK, "§c" + backText, Arrays.asList(
                "",
                "§7" + returnMainText)));

        player.openInventory(inv);
    }

    private String getPlayerName(UUID playerId) {
        Player onlinePlayer = Bukkit.getPlayer(playerId);
        if (onlinePlayer != null) {
            playerNameCache.put(playerId, onlinePlayer.getName());
            return onlinePlayer.getName();
        }
        String cachedName = playerNameCache.get(playerId);
        if (cachedName != null)
            return cachedName;
        try {
            org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
            String name = offlinePlayer.getName();
            if (name != null) {
                playerNameCache.put(playerId, name);
                return name;
            }
        } catch (Exception ignored) {
        }
        return "en".equals(getCurrentLanguage()) ? "Player" : "Oyuncu";
    }

    private int calculateRank(ActiveEvent event, UUID playerUuid) {
        int playerCount = event.getPlayerDeliveries().getOrDefault(playerUuid, 0);
        if (playerCount == 0)
            return 0;
        int rank = 1;
        for (int count : event.getPlayerDeliveries().values()) {
            if (count > playerCount)
                rank++;
        }
        return rank;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ITEM OLUŞTURMA - 1.16.5+ Uyumlu
    // ═══════════════════════════════════════════════════════════════════════════

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createHead(String texture, String name, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            // Custom texture uygula
            applyTexture(meta, texture);
            head.setItemMeta(meta);
        }
        return head;
    }

    private ItemStack createPlayerHead(UUID playerId, String name, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            try {
                org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerId);
                meta.setOwningPlayer(offlinePlayer);
            } catch (Exception ignored) {
            }
            head.setItemMeta(meta);
        }
        return head;
    }

    /**
     * Custom texture ile head oluşturma - 1.16.5 - 1.21.4 arası tüm sürümler
     * Yöntem 1: Bukkit PlayerProfile API (Paper 1.18.1+ / Spigot 1.20.2+)
     * Yöntem 2: GameProfile reflection (1.16.5 - 1.20.4)
     */
    private void applyTexture(SkullMeta meta, String texture) {
        // Yöntem 1: Bukkit PlayerProfile API - explicit class names ile
        try {
            Class<?> playerProfileClass = Class.forName("org.bukkit.profile.PlayerProfile");
            Class<?> playerTexturesClass = Class.forName("org.bukkit.profile.PlayerTextures");

            // Bukkit.createPlayerProfile(UUID, String)
            java.lang.reflect.Method createProfile = Bukkit.class.getMethod("createPlayerProfile",
                    UUID.class, String.class);
            Object profile = createProfile.invoke(null, UUID.randomUUID(), "VeloDelivery");

            // profile.getTextures()
            Object textures = playerProfileClass.getMethod("getTextures").invoke(profile);

            // Base64'ten skin URL çıkar
            String decoded = new String(java.util.Base64.getDecoder().decode(texture));
            String urlStr = decoded.split("\"url\"\\s*:\\s*\"")[1].split("\"")[0];
            java.net.URL skinUrl = java.net.URI.create(urlStr).toURL();

            // textures.setSkin(URL)
            playerTexturesClass.getMethod("setSkin", java.net.URL.class).invoke(textures, skinUrl);

            // profile.setTextures(PlayerTextures)
            playerProfileClass.getMethod("setTextures", playerTexturesClass).invoke(profile, textures);

            // meta.setOwnerProfile(PlayerProfile)
            meta.getClass().getMethod("setOwnerProfile", playerProfileClass).invoke(meta, profile);
            return; // Başarılı!
        } catch (Exception ignored) {
            // PlayerProfile API mevcut değil, yöntem 2'ye geç
        }

        // Yöntem 2: GameProfile reflection (1.16.5 - 1.20.4)
        try {
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");

            Object gameProfile = gameProfileClass.getConstructor(UUID.class, String.class)
                    .newInstance(UUID.randomUUID(), "VeloDelivery");

            Object properties = gameProfileClass.getMethod("getProperties").invoke(gameProfile);

            // Property constructor: bazı sürümlerde 2, bazılarında 3 parametre
            Object property;
            try {
                property = propertyClass.getConstructor(String.class, String.class)
                        .newInstance("textures", texture);
            } catch (NoSuchMethodException e) {
                property = propertyClass.getConstructor(String.class, String.class, String.class)
                        .newInstance("textures", texture, null);
            }

            properties.getClass().getMethod("put", Object.class, Object.class)
                    .invoke(properties, "textures", property);

            // Profile'ı meta'ya set et
            java.lang.reflect.Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);

            // 1.20.5+: profile field tipi ResolvableProfile olabilir
            if (profileField.getType().equals(gameProfileClass)) {
                // Eski sürümler: direkt GameProfile set et
                profileField.set(meta, gameProfile);
            } else {
                // 1.20.5+: ResolvableProfile oluştur GameProfile'dan
                try {
                    Class<?> resolvableClass = profileField.getType();
                    // ResolvableProfile(GameProfile) constructor
                    Object resolvable = resolvableClass.getConstructor(gameProfileClass)
                            .newInstance(gameProfile);
                    profileField.set(meta, resolvable);
                } catch (Exception e2) {
                    // Son çare: setProfile metodu dene
                    try {
                        java.lang.reflect.Method setProfile = meta.getClass()
                                .getDeclaredMethod("setProfile", gameProfileClass);
                        setProfile.setAccessible(true);
                        setProfile.invoke(meta, gameProfile);
                    } catch (Exception e3) {
                        logger.warning("Head texture fallback hatasi: " + e3.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Head texture uygulama hatasi: " + e.getMessage());
        }
    }
}
