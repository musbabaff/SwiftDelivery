package com.nettyforge.swiftdelivery;

import com.nettyforge.swiftdelivery.command.CommandHandler;
import com.nettyforge.swiftdelivery.command.DeliverCommand;
import com.nettyforge.swiftdelivery.config.ConfigManager;
import com.nettyforge.swiftdelivery.config.ConfigManagerImpl;
import com.nettyforge.swiftdelivery.gui.DeliveryGUI;
import com.nettyforge.swiftdelivery.gui.ProfileGUI;
import com.nettyforge.swiftdelivery.placeholder.PlaceholderEngine;
import com.nettyforge.swiftdelivery.placeholder.PlaceholderEngineImpl;
import com.nettyforge.swiftdelivery.reward.PendingRewardStore;
import com.nettyforge.swiftdelivery.reward.PendingRewardStoreImpl;
import com.nettyforge.swiftdelivery.reward.RewardService;
import com.nettyforge.swiftdelivery.reward.RewardServiceImpl;
import com.nettyforge.swiftdelivery.service.CategoryService;
import com.nettyforge.swiftdelivery.service.CategoryServiceImpl;
import com.nettyforge.swiftdelivery.data.*;
import com.nettyforge.swiftdelivery.service.DeliveryService;
import com.nettyforge.swiftdelivery.service.DeliveryServiceImpl;
import com.nettyforge.swiftdelivery.service.MessageService;
import com.nettyforge.swiftdelivery.service.MessageServiceImpl;
import com.nettyforge.swiftdelivery.service.SchedulerService;
import com.nettyforge.swiftdelivery.service.SchedulerServiceImpl;
import com.nettyforge.swiftdelivery.util.LoggingService;
import com.nettyforge.swiftdelivery.webhook.WebhookService;
import com.nettyforge.swiftdelivery.webhook.WebhookServiceImpl;
import com.nettyforge.swiftdelivery.hook.item.*;
import com.nettyforge.swiftdelivery.hook.economy.VaultHook;
import com.nettyforge.swiftdelivery.hook.papi.SwiftDeliveryExpansion;
import com.nettyforge.swiftdelivery.command.MailCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.block.Chest;
import org.bukkit.plugin.java.JavaPlugin;

import com.nettyforge.swiftdelivery.hook.AdventureHook;

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;

/**
 * Main plugin class for SwiftDelivery.
 */
public class SwiftDeliveryPlugin extends JavaPlugin implements Listener {

    private ConfigManager configManager;
    private PlaceholderEngine placeholderEngine;
    private CategoryService categoryService;
    private SchedulerService schedulerService;
    private DeliveryService deliveryService;
    private RewardService rewardService;
    private WebhookService webhookService;
    private MessageService messageService;
    private PendingRewardStore pendingRewardStore;
    private CommandHandler commandHandler;
    private DeliverCommand deliverCommand;
    private MailCommand mailCommand;
    
    private AdventureHook adventureHook;
    private DeliveryGUI deliveryGUI;
    private ProfileGUI profileGUI;
    private VaultHook vaultHook;
    private ScheduledExecutorService executorService;
    private ItemFactory itemFactory;

    public ItemFactory getItemFactory() {
        return itemFactory;
    }

    private DatabaseManager databaseManager;

    private String currentLanguage = "tr";
    private String latestVersion = null; // GitHub'dan kontrol edilen son sürüm
    private boolean updateAvailable = false;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        getLogger().info("");
        getLogger().info("  SwiftDelivery v" + getDescription().getVersion());
        getLogger().info("  Delivery Event System");
        getLogger().info("");

        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }

            saveDefaultConfigs();
            executorService = Executors.newScheduledThreadPool(2);

            setupDatabase();
            
            this.adventureHook = new AdventureHook(this);

            initializeServices();
            loadConfigurations();
            registerListeners();
            resumeActiveEvents();
            loadSavedEvents();

            deliveryGUI.loadItemsConfig(getDataFolder());

            long loadTime = System.currentTimeMillis() - startTime;
            getLogger().info("Successfully loaded! (" + loadTime + "ms)");

            if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new SwiftDeliveryExpansion(this, databaseManager).register();
                getLogger().info("PlaceholderAPI hook enabled.");
            }

            // GitHub'dan güncelleme kontrolü (async)
            checkForUpdates();

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable SwiftDelivery", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    private void setupDatabase() {
        File configFile = new File(getDataFolder(), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        String type = config.getString("database.type", "sqlite").toLowerCase();

        if (type.equals("mysql")) {
            String host = config.getString("database.mysql.host", "localhost");
            int port = config.getInt("database.mysql.port", 3306);
            String db = config.getString("database.mysql.database", "swiftdelivery");
            String user = config.getString("database.mysql.username", "root");
            String pass = config.getString("database.mysql.password", "");
            boolean ssl = config.getBoolean("database.mysql.use-ssl", false);
            int pool = config.getInt("database.mysql.pool-size", 10);
            
            databaseManager = new MySQLProvider(this, host, port, db, user, pass, ssl, pool);
        } else {
            databaseManager = new SQLiteProvider(this);
        }

        if (!databaseManager.connect()) {
            getLogger().severe("Could not connect to the database! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("SwiftDelivery is shutting down...");

        if (deliveryService != null && databaseManager != null) {
            var activeEvents = deliveryService.getAllActiveEvents();
            if (!activeEvents.isEmpty()) {
                for (var event : activeEvents) {
                    databaseManager.saveActiveEvent(event).join(); // block on disable
                }
            }
            databaseManager.disconnect();
        }

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        if (adventureHook != null) {
            adventureHook.disable();
        }

        getLogger().info("Shut down!");
    }

    private void saveDefaultConfigs() {
        saveResourceIfNotExists("config.yml");
        saveResourceIfNotExists("categories.yml");
        saveResourceIfNotExists("deliveries.yml");

        // items.yml'yi HER ZAMAN güncelle (yeni eşyalar için)
        File itemsFile = new File(getDataFolder(), "items.yml");
        if (itemsFile.exists()) {
            itemsFile.delete();
        }
        saveResource("items.yml", false);

        File langFolder = new File(getDataFolder(), "locales");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }
        saveResourceIfNotExists("locales/tr.yml");
        saveResourceIfNotExists("locales/en.yml");
        saveResourceIfNotExists("locales/de.yml");
        saveResourceIfNotExists("locales/es.yml");
        saveResourceIfNotExists("locales/fr.yml");
        saveResourceIfNotExists("locales/ru.yml");
        saveResourceIfNotExists("locales/zh.yml");
        saveResourceIfNotExists("locales/pt.yml");
    }

    private void saveResourceIfNotExists(String resourcePath) {
        File file = new File(getDataFolder(), resourcePath);
        if (!file.exists()) {
            saveResource(resourcePath, false);
        }
    }

    private void initializeServices() {
        configManager = new ConfigManagerImpl(getDataFolder().getAbsolutePath());
        placeholderEngine = new PlaceholderEngineImpl();
        new LoggingService(getLogger(), placeholderEngine);
        pendingRewardStore = new PendingRewardStoreImpl();

        itemFactory = new ItemFactory(getLogger());
        if (getServer().getPluginManager().getPlugin("Nexo") != null) {
            itemFactory.registerProvider("NEXO", new NexoProvider(getLogger()));
        }
        if (getServer().getPluginManager().getPlugin("Oraxen") != null) {
            itemFactory.registerProvider("ORAXEN", new OraxenProvider(getLogger()));
        }
        if (getServer().getPluginManager().getPlugin("ItemsAdder") != null) {
            itemFactory.registerProvider("ITEMSADDER", new ItemsAdderProvider(getLogger()));
        }

        SchedulerServiceImpl schedulerImpl = new SchedulerServiceImpl(executorService);
        schedulerImpl.setEventStartCallback(this::handleEventStart);
        schedulerImpl.setEventEndCallback(this::handleEventEnd);
        schedulerService = schedulerImpl;

        webhookService = new WebhookServiceImpl(placeholderEngine, configManager);
        rewardService = new RewardServiceImpl(pendingRewardStore, placeholderEngine);

        getLogger().info("Services ready.");
    }

    private void handleEventStart(String deliveryName) {
        if (deliveryService != null) {
            deliveryService.startEvent(deliveryName).ifPresent(event -> {
                String itemTR = deliveryGUI.getItemDisplayName(event.getResolvedItem());
                String catTR = deliveryGUI.getCategoryDisplayName(event.getResolvedCategory());
                String delTR = deliveryGUI.getDeliveryDisplayName(deliveryName);

                // Console log
                getLogger().info("[EVENT STARTED] " + delTR + " | Category: " + catTR + " | Item: " + itemTR);

                // Broadcast
                String msg = "§e§lS§6wift§e§lD§6elivery §8» §a" + delTR + " §7started! §eRequired: §f" + itemTR;
                Bukkit.broadcastMessage(msg);

                // Title
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendTitle("§a§l" + delTR.toUpperCase(), "§7Required: §e" + itemTR, 10, 70, 20);
                    try {
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    } catch (Exception ignored) {
                    }
                }

                // Webhook
                sendWebhook(deliveryName, "start", itemTR, catTR);
            });
        }
    }

    private void handleEventEnd(String deliveryName) {
        if (deliveryService != null) {
            var activeEvent = deliveryService.getActiveEvent(deliveryName);
            String item = activeEvent.map(e -> e.getResolvedItem()).orElse("unknown");
            String category = activeEvent.map(e -> e.getResolvedCategory()).orElse("unknown");
            String itemTR = deliveryGUI.getItemDisplayName(item);
            String catTR = deliveryGUI.getCategoryDisplayName(category);
            String delTR = deliveryGUI.getDeliveryDisplayName(deliveryName);

            var winners = deliveryService.endEvent(deliveryName);

            // Console log
            getLogger().info("[EVENT ENDED] " + delTR + " | Winners: " + winners.size());

            // Broadcast
            String msg = "§e§lS§6wift§e§lD§6elivery §8» §c" + delTR + " §7ended!";
            Bukkit.broadcastMessage(msg);

            if (!winners.isEmpty()) {
                Bukkit.broadcastMessage("§e§lS§6wift§e§lD§6elivery §8» §6Winners:");
                int rank = 1;
                for (var winner : winners) {
                    String color = rank == 1 ? "§6" : rank == 2 ? "§f" : rank == 3 ? "§c" : "§7";
                    Bukkit.broadcastMessage("  " + color + rank + ". §f" + winner.playerName() + " §8- §e"
                            + winner.deliveryCount() + " §7deliveries");
                    rank++;
                    if (rank > 3)
                        break;
                }
            }

            // Title
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendTitle("§c§lEVENT ENDED!", "§7" + delTR, 10, 70, 20);
            }

            // Webhook - kazananlarla birlikte
            sendWebhookWithWinners(deliveryName, itemTR, catTR, winners);
        }
    }

    private void sendWebhookWithWinners(String deliveryName, String itemTR, String categoryTR,
            java.util.List<com.nettyforge.swiftdelivery.model.Winner> winners) {
        try {
            File configFile = new File(getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                getLogger().warning("[WEBHOOK] config.yml not found: " + configFile.getAbsolutePath());
                return;
            }

            var config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
            boolean enabled = config.getBoolean("webhook.enabled", false);
            String url = config.getString("webhook.url", "");

            getLogger().info("[WEBHOOK-END] enabled=" + enabled + ", url="
                    + (url != null ? url.substring(0, Math.min(url.length(), 30)) + "..." : "null"));

            if (!enabled || url == null || url.isEmpty() || url.contains("YOUR_WEBHOOK")) {
                getLogger().info(
                        "[WEBHOOK-END] Skipping: enabled=" + enabled + ", url_empty=" + (url == null || url.isEmpty())
                                + ", placeholder=" + (url != null && url.contains("YOUR_WEBHOOK")));
                return;
            }

            String delTR = deliveryGUI.getDeliveryDisplayName(deliveryName);

            // Kazananlar listesi oluştur
            StringBuilder winnersText = new StringBuilder();
            if (!winners.isEmpty()) {
                winnersText.append("\\n\\n**🏆 Kazananlar:**");
                int rank = 1;
                for (var winner : winners) {
                    String medal = rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : rank + ".";
                    winnersText.append("\\n").append(medal).append(" **").append(winner.playerName())
                            .append("** - ").append(winner.deliveryCount()).append(" deliveries");
                    rank++;
                    if (rank > 5)
                        break;
                }
            } else {
                winnersText.append("\\n\\n*Kimse katılmadı*");
            }

            String desc = "**Item:** " + itemTR + "\\n**Category:** " + categoryTR + winnersText;
            String json = "{\"embeds\":[{\"title\":\"🏆 " + delTR + " Bitti!\",\"description\":\"" + desc
                    + "\",\"color\":16766720,\"footer\":{\"text\":\"SwiftDelivery\"}}]}";

            // Async webhook gönder
            final String finalUrl = url;
            final String finalJson = json;
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                sendWebhookRequest(finalUrl, finalJson, "END");
            });
        } catch (Exception e) {
            getLogger().warning("[WEBHOOK] Error: " + e.getMessage());
        }
    }

    /**
     * Etkinlik başlangıcı için webhook gönderir
     */
    private void sendWebhook(String deliveryName, String type, String itemTR, String categoryTR) {
        try {
            // Global webhook URL'yi al
            File configFile = new File(getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                getLogger().warning("[WEBHOOK] config.yml not found: " + configFile.getAbsolutePath());
                return;
            }

            var config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
            boolean enabled = config.getBoolean("webhook.enabled", false);
            String url = config.getString("webhook.url", "");

            getLogger().info("[WEBHOOK-START] enabled=" + enabled + ", url="
                    + (url != null ? url.substring(0, Math.min(url.length(), 30)) + "..." : "null"));

            if (!enabled || url == null || url.isEmpty() || url.contains("YOUR_WEBHOOK")) {
                getLogger().info("[WEBHOOK-START] Skipping: enabled=" + enabled + ", url_empty="
                        + (url == null || url.isEmpty()) + ", placeholder="
                        + (url != null && url.contains("YOUR_WEBHOOK")));
                return;
            }

            String delTR = deliveryGUI.getDeliveryDisplayName(deliveryName);
            String title = "📦 " + delTR + " Started!";
            String desc = "**Required Item:** " + itemTR + "\\n**Category:** " + categoryTR;

            String json = "{\"embeds\":[{\"title\":\"" + title + "\",\"description\":\"" + desc
                    + "\",\"color\":65280,\"footer\":{\"text\":\"SwiftDelivery\"}}]}";

            // Async webhook gönder
            final String finalUrl = url;
            final String finalJson = json;
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                sendWebhookRequest(finalUrl, finalJson, "START");
            });
        } catch (Exception e) {
            getLogger().warning("[WEBHOOK] Error: " + e.getMessage());
        }
    }

    /**
     * Discord Webhook gönderici - Tüm sunucularda çalışır
     * TLSv1.2 ve TLSv1.3 destekler, sistem proxy ayarlarını kullanır
     */
    private void sendWebhookRequest(String urlString, String jsonPayload, String tag) {
        try {
            // URL doğrula
            java.net.URI uri = java.net.URI.create(urlString);
            java.net.URL url = uri.toURL();

            // Sistem varsayılan SSL context'ini kullan (en güvenilir yöntem)
            javax.net.ssl.HttpsURLConnection connection = (javax.net.ssl.HttpsURLConnection) url.openConnection();

            // TLS 1.2/1.3 zorla
            try {
                javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLSv1.2");
                sslContext.init(null, null, null);
                connection.setSSLSocketFactory(sslContext.getSocketFactory());
            } catch (Exception e) {
                // Varsayılan SSL kullan
            }

            // Bağlantı ayarları
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 SwiftDelivery/1.0");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(30000);

            // Bağlan
            connection.connect();

            // JSON gönder
            try (java.io.OutputStream os = connection.getOutputStream();
                    java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(os,
                            java.nio.charset.StandardCharsets.UTF_8)) {
                writer.write(jsonPayload);
                writer.flush();
            }

            // Yanıt al
            int responseCode = connection.getResponseCode();

            if (responseCode == 200 || responseCode == 204 || responseCode == 201) {
                if (configManager != null && configManager.isDebug()) {
                    getLogger().info("[WEBHOOK] Sent (" + tag + ")");
                }
            } else {
                getLogger().warning("[WEBHOOK] Error: " + responseCode);
            }

            connection.disconnect();

        } catch (java.net.SocketTimeoutException e) {
            getLogger().warning("[WEBHOOK] Timeout - Cannot reach Discord");
        } catch (java.io.IOException e) {
            getLogger().warning("[WEBHOOK] Connection error: " + e.getMessage());
        } catch (Exception e) {
            getLogger().warning("[WEBHOOK] Error: " + e.getMessage());
        }
    }

    private void loadConfigurations() {
        configManager.loadAll();

        configManager.validate().forEach(error -> getLogger().warning(String.format("[%s] %s - %s: %s",
                error.severity(),
                error.file(),
                error.field() != null ? error.field() : "genel",
                error.message())));

        // Dil ayarını yükle
        loadLanguageSetting();

        initializeConfigDependentServices();
    }

    /**
     * config.yml'den dil ayarını yükler
     */
    private void loadLanguageSetting() {
        try {
            File configFile = new File(getDataFolder(), "config.yml");
            if (configFile.exists()) {
                var config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
                String lang = config.getString("general.language", "tr");
                currentLanguage = lang.toLowerCase().trim();
                if (!currentLanguage.equals("tr") && !currentLanguage.equals("en")) {
                    currentLanguage = "tr";
                }
                getLogger().info("Language setting: " + currentLanguage);
            }
        } catch (Exception e) {
            currentLanguage = "tr";
            getLogger().warning("Language setting could not be loaded, default: en");
        }
    }

    /**
     * Mevcut dil ayarını döndürür
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }

    private void initializeConfigDependentServices() {
        categoryService = new CategoryServiceImpl(configManager.getCategoryConfig());

        deliveryService = new DeliveryServiceImpl(
                configManager.getDeliveryConfig(),
                categoryService,
                schedulerService);

        messageService = new MessageServiceImpl(
                configManager.getLanguageConfig(),
                placeholderEngine,
                this::sendMessageToPlayer,
                getLogger());

        commandHandler = new CommandHandler(
                configManager,
                this::checkPermission,
                this::sendMessageToPlayer,
                getLogger());

        deliveryGUI = new DeliveryGUI(configManager, deliveryService, itemFactory);
        deliveryGUI.setLanguageSupplier(this::getCurrentLanguage);
        deliveryGUI.setDataFolder(getDataFolder());
        loadGUISettings();

        profileGUI = new ProfileGUI(databaseManager);
        vaultHook = new VaultHook();

        deliverCommand = new DeliverCommand(
                this::checkPermission,
                this::sendMessageToPlayer,
                deliveryGUI,
                profileGUI);

        mailCommand = new MailCommand(this, databaseManager, vaultHook);

        commandHandler.setDeliveryService(deliveryService);
        commandHandler.setReloadCallback(this::reloadAllSettings);
        commandHandler.setManualEndScheduler(this::scheduleManualEnd);
        commandHandler.setWebhookTester(this::testWebhook);
        deliverCommand.setDeliveryService(deliveryService);
    }

    /**
     * Manuel başlatılan etkinlik için otomatik bitiş zamanlayıcısı
     */
    private void scheduleManualEnd(String deliveryName, Long durationMinutes) {
        long ticks = durationMinutes * 60 * 20; // dakika -> tick (20 tick = 1 saniye)

        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (deliveryService.getActiveEvent(deliveryName).isPresent()) {
                handleEventEnd(deliveryName);
                getLogger().info("[SwiftDelivery] Manual event expired: " + deliveryName);
            }
        }, ticks);
    }

    /**
     * Webhook test fonksiyonu
     */
    private void testWebhook() {
        try {
            File configFile = new File(getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                getLogger().warning("[WEBHOOK TEST] config.yml not found!");
                return;
            }

            var config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
            String url = config.getString("webhook.url", "");

            getLogger().info("[WEBHOOK TEST] URL length: " + url.length());

            if (url.isEmpty() || url.contains("YOUR_WEBHOOK")) {
                getLogger().warning("[WEBHOOK TEST] Webhook URL is not set!");
                return;
            }

            String json = "{\"embeds\":[{\"title\":\"🧪 SwiftDelivery Test\",\"description\":\"Webhook baglantisi basarili!\\n\\nBu bir test mesajidir.\",\"color\":65280,\"footer\":{\"text\":\"SwiftDelivery v1.0.0\"}}]}";

            getLogger().info("[WEBHOOK TEST] Sending...");

            final String finalUrl = url;
            final String finalJson = json;
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                sendWebhookRequest(finalUrl, finalJson, "TEST");
            });
        } catch (Exception e) {
            getLogger().warning("[WEBHOOK TEST] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * GitHub'dan en son sürümü kontrol eder ve güncel değilse bildirim oluşturur.
     */
    private void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                String apiUrl = "https://api.github.com/repos/musbabaff/SwiftDelivery/releases/latest";
                java.net.URI uri = java.net.URI.create(apiUrl);
                java.net.URL url = uri.toURL();
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "SwiftDelivery/" + getDescription().getVersion());
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(connection.getInputStream(),
                                    java.nio.charset.StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }

                        // JSON'dan tag_name çıkar (basit parse)
                        String json2 = response.toString();
                        int tagIndex = json2.indexOf("\"tag_name\"");
                        if (tagIndex != -1) {
                            int valueStart = json2.indexOf("\"", tagIndex + 10) + 1;
                            int valueEnd = json2.indexOf("\"", valueStart);
                            String tagName = json2.substring(valueStart, valueEnd);

                            // "v" prefix'ini kaldır
                            String remoteVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;
                            String currentVersion = getDescription().getVersion();

                            if (!currentVersion.equals(remoteVersion)
                                    && isNewerVersion(remoteVersion, currentVersion)) {
                                latestVersion = remoteVersion;
                                updateAvailable = true;
                                getLogger().info("[UPDATE] New version available: v" + remoteVersion +
                                        " (Mevcut: v" + currentVersion + ")");
                                getLogger().info("[UPDATE] https://github.com/musbabaff/SwiftDelivery/releases");
                            } else {
                                getLogger().info("[UPDATE] Running latest version (v" + currentVersion + ")");
                            }
                        }
                    }
                }
                connection.disconnect();
            } catch (Exception e) {
                // Güncelleme kontrolü başarısız - sessizce geç
                getLogger().fine("[UPDATE] Check failed: " + e.getMessage());
            }
        });
    }

    /**
     * Semantic versioning karşılaştırması (1.0.3 vs 1.0.2 gibi).
     */
    private boolean isNewerVersion(String remote, String current) {
        try {
            String[] remoteParts = remote.split("\\.");
            String[] currentParts = current.split("\\.");
            int length = Math.max(remoteParts.length, currentParts.length);
            for (int i = 0; i < length; i++) {
                int r = i < remoteParts.length ? Integer.parseInt(remoteParts[i]) : 0;
                int c = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                if (r > c)
                    return true;
                if (r < c)
                    return false;
            }
        } catch (NumberFormatException ignored) {
        }
        return false;
    }

    /**
     * Tüm ayarları yeniden yükler (reload komutu için)
     */
    private void reloadAllSettings() {
        // Dil ayarını yeniden yükle
        loadLanguageSetting();

        // items.yml'yi yeniden yükle
        deliveryGUI.loadItemsConfig(getDataFolder());

        // GUI dil dosyasını yeniden yükle
        deliveryGUI.reloadLanguage();

        // GUI ayarlarını yeniden yükle
        loadGUISettings();

        getLogger().info("All settings reloaded. Language: " + currentLanguage);
    }

    private void loadGUISettings() {
        try {
            File configFile = new File(getDataFolder(), "config.yml");
            if (configFile.exists()) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

                // Kategori display names
                ConfigurationSection catDisplaySection = config.getConfigurationSection("category-display-names");
                if (catDisplaySection != null) {
                    for (String key : catDisplaySection.getKeys(false)) {
                        String displayName = catDisplaySection.getString(key);
                        if (displayName != null && !displayName.isEmpty()) {
                            deliveryGUI.setCategoryDisplayName(key, displayName);
                        }
                    }
                }

                // Teslimat display names
                ConfigurationSection delDisplaySection = config.getConfigurationSection("delivery-display-names");
                if (delDisplaySection != null) {
                    for (String key : delDisplaySection.getKeys(false)) {
                        String displayName = delDisplaySection.getString(key);
                        if (displayName != null && !displayName.isEmpty()) {
                            deliveryGUI.setDeliveryDisplayName(key, displayName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            getLogger().warning("GUI settings could not be loaded: " + e.getMessage());
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(profileGUI, this);
    }



    private void resumeActiveEvents() {
        if (schedulerService != null) {
            schedulerService.resumeActiveEvents();
        }

        if (configManager != null && configManager.getDeliveryConfig() != null) {
            var enabledDeliveries = configManager.getDeliveryConfig().getEnabledDeliveries();
            if (!enabledDeliveries.isEmpty()) {
                getLogger().info("Scheduled deliveries:");
                enabledDeliveries.forEach(delivery -> {
                    try {
                        schedulerService.scheduleEvent(delivery);
                        getLogger().info("  + " + delivery.name());
                    } catch (Exception e) {
                        getLogger().warning("  x " + delivery.name() + ": " + e.getMessage());
                    }
                });
            }
        }
    }

    /**
     * Sunucu kapanmadan önce kaydedilmiş aktif etkinlikleri yükler.
     */
    private void loadSavedEvents() {
        if (databaseManager == null || deliveryService == null)
            return;

        getLogger().info("Saved events loading...");

        databaseManager.loadAllActiveEvents().thenAccept(savedEvents -> {
            if (savedEvents.isEmpty()) return;

            Bukkit.getScheduler().runTask(this, () -> {
                for (var savedEvent : savedEvents) {
                    try {
                        // Etkinlik hala geçerli mi kontrol et (bitiş zamanı geçmemiş mi)
                        if (savedEvent.getEndTime() != null && savedEvent.getEndTime().isBefore(java.time.ZonedDateTime.now())) {
                            getLogger().info("  - " + savedEvent.getDeliveryName() + " (expired, skipping)");
                            databaseManager.deleteActiveEvent(savedEvent.getDeliveryName());
                            continue;
                        }

                        // Restore to service
                        ((DeliveryServiceImpl) deliveryService).restoreEvent(savedEvent);

                        getLogger().info("  + " + savedEvent.getDeliveryName() + " (loaded, " +
                                savedEvent.getPlayerDeliveries().size() + " player data)");

                    } catch (Exception e) {
                        getLogger().warning("  x " + savedEvent.getDeliveryName() + ": " + e.getMessage());
                    }
                }
            });
        });
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (rewardService != null && rewardService.hasPendingRewards(player.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                deliverPendingRewardsToPlayer(player);
            }, 20L);
        }

        if (databaseManager != null && player.hasPermission("swiftdelivery.mail")) {
            databaseManager.getMail(player.getUniqueId()).thenAccept(mails -> {
                long unread = mails.stream().filter(m -> !m.claimed()).count();
                if (unread > 0) {
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        player.sendMessage("§e§lS§6wift§e§lD§6elivery §8» §aYou have §f" + unread + " §aunread mail(s)!");
                        player.sendMessage("§7Type §e/delivery profile §7to open your inbox.");
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                    }, 40L);
                }
            }).exceptionally(ex -> {
                getLogger().warning("Failed to load mail for " + player.getName() + ": " + ex.getMessage());
                return null;
            });
        }

        // Güncelleme bildirimi (sadece OP oyuncular)
        if (updateAvailable && latestVersion != null && (player.isOp() || player.hasPermission("swiftdelivery.admin"))) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                player.sendMessage("");
                player.sendMessage("§e§lS§6wift§e§lD§6elivery §8» §aNew version available!");
                player.sendMessage("§7  Current: §cv" + getDescription().getVersion() + " §8» §7New: §av" + latestVersion);
                
                net.md_5.bungee.api.chat.TextComponent link = new net.md_5.bungee.api.chat.TextComponent("§7  §b§nClick here to download.");
                link.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.OPEN_URL, "https://github.com/musbabaff/SwiftDelivery/releases"));
                player.spigot().sendMessage(link);
                
                player.sendMessage("");
            }, 60L); // 3 saniye sonra göster
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;

        String title = event.getView().getTitle();
        if (!DeliveryGUI.isDeliveryGUI(title))
            return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null)
            return;

        int slot = event.getRawSlot();

        if (DeliveryGUI.isMainMenu(title)) {
            handleMainMenuClick(player, slot);
        } else if (DeliveryGUI.isDeliveryMenu(title)) {
            handleDeliveryMenuClick(player, slot, title);
        } else if (DeliveryGUI.isLeaderboardMenu(title)) {
            handleLeaderboardClick(player, slot);
        }
    }

    // Sandık tıklama event'i
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (!deliveryGUI.isInChestSelectionMode(player.getUniqueId()))
            return;

        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)
            return;
        if (event.getClickedBlock() == null)
            return;

        // Koruma kontrolü - oyuncunun bu bloğa erişim hakkı var mı?
        // Bu, claim/ada pluginleri ile uyumlu çalışır (WorldGuard, GriefPrevention,
        // ASkyBlock vb.)
        org.bukkit.block.Block block = event.getClickedBlock();
        if (!canAccessChest(player, block)) {
            player.sendMessage("§e§lS§6wift§e§lD§6elivery §8» §cYou do not have permission to access this chest!");
            deliveryGUI.cancelChestSelection(player.getUniqueId());
            return;
        }

        // Sandık kontrolü - normal ve çift sandık
        org.bukkit.block.BlockState state = event.getClickedBlock().getState();
        org.bukkit.inventory.Inventory chestInventory = null;

        if (state instanceof Chest chest) {
            chestInventory = chest.getInventory();
        } else if (event.getClickedBlock().getType() == org.bukkit.Material.CHEST ||
                event.getClickedBlock().getType() == org.bukkit.Material.TRAPPED_CHEST) {
            // Blok tipi sandık ama state farklı olabilir
            if (state instanceof org.bukkit.block.Container container) {
                chestInventory = container.getInventory();
            }
        }

        if (chestInventory == null) {
            player.sendMessage("§e§lS§6wift§e§lD§6elivery §8» §cThis is not a chest!");
            return;
        }

        event.setCancelled(true);

        String deliveryName = deliveryGUI.getChestSelectionDelivery(player.getUniqueId());
        deliveryGUI.cancelChestSelection(player.getUniqueId());

        var activeEvent = deliveryService.getActiveEvent(deliveryName).orElse(null);
        if (activeEvent == null) {
            player.sendMessage("§e§lS§6wift§e§lD§6elivery §8» §cDelivery is no longer active!");
            return;
        }

        deliverFromRealChestInventory(player, chestInventory, activeEvent);
    }

    /**
     * Oyuncunun sandığa erişim hakkı olup olmadığını kontrol eder.
     * - Tabela kilidi kontrolü (LWC, Lockette, BlockLocker vb.)
     * - Claim/ada koruma kontrolü (WorldGuard, GriefPrevention, ASkyBlock vb.)
     */
    private boolean canAccessChest(Player player, org.bukkit.block.Block block) {
        // Admin bypass
        if (player.hasPermission("SwiftDelivery.bypass.protection")) {
            return true;
        }

        // Tabela kilidi kontrolü - sandığın yanındaki tabelaları kontrol et
        if (hasSignLock(block, player)) {
            return false;
        }

        // PlayerInteractEvent ile koruma kontrolü
        // Bu, WorldGuard, GriefPrevention, ASkyBlock gibi pluginler tarafından dinlenir
        try {
            org.bukkit.event.player.PlayerInteractEvent testEvent = new org.bukkit.event.player.PlayerInteractEvent(
                    player,
                    org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK,
                    player.getInventory().getItemInMainHand(),
                    block,
                    org.bukkit.block.BlockFace.NORTH);

            // Event'i çağır ve iptal edilip edilmediğini kontrol et
            Bukkit.getPluginManager().callEvent(testEvent);

            // Eğer başka bir plugin event'i iptal ettiyse, erişim yok
            if (testEvent.useInteractedBlock() == org.bukkit.event.Event.Result.DENY) {
                return false;
            }
        } catch (Exception e) {
            // Hata durumunda izin ver
            getLogger().warning("Protection check error: " + e.getMessage());
        }

        return true;
    }

    /**
     * Sandığın yanında kilit tabelası olup olmadığını kontrol eder.
     * [Private] veya oyuncu ismi içeren tabelalar kilit olarak kabul edilir.
     */
    private boolean hasSignLock(org.bukkit.block.Block block, Player player) {
        // Sandığın etrafındaki blokları kontrol et
        org.bukkit.block.BlockFace[] faces = {
                org.bukkit.block.BlockFace.NORTH,
                org.bukkit.block.BlockFace.SOUTH,
                org.bukkit.block.BlockFace.EAST,
                org.bukkit.block.BlockFace.WEST,
                org.bukkit.block.BlockFace.UP
        };

        for (org.bukkit.block.BlockFace face : faces) {
            org.bukkit.block.Block adjacent = block.getRelative(face);

            // Tabela mı kontrol et
            if (adjacent.getState() instanceof org.bukkit.block.Sign sign) {
                String[] lines = sign.getLines();

                // [Private] veya [Lock] kontrolü
                if (lines.length > 0) {
                    String firstLine = org.bukkit.ChatColor.stripColor(lines[0]).toLowerCase();
                    if (firstLine.contains("private") || firstLine.contains("lock") ||
                            firstLine.contains("kilit") || firstLine.contains("özel")) {

                        // Sahibi kontrol et (2. satır genellikle sahip ismi)
                        if (lines.length > 1) {
                            String ownerLine = org.bukkit.ChatColor.stripColor(lines[1]);
                            if (!ownerLine.isEmpty() && !ownerLine.equalsIgnoreCase(player.getName())) {
                                // Sahip değil, erişim yok
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false; // Kilit yok
    }

    private void handleMainMenuClick(Player player, int slot) {
        // Sıralama butonu (slot 30)
        if (slot == 30) {
            deliveryGUI.openLeaderboard(player);
            return;
        }

        // Yardım butonu (slot 32)
        if (slot == 32) {
            player.closeInventory();
            player.performCommand("teslimat help");
            return;
        }

        // Aktif teslimatlar - satır 1 (slot 9-17)
        if (slot >= 9 && slot <= 17) {
            var events = deliveryService.getAllActiveEvents();
            int[] centeredSlots = deliveryGUI.getCenteredSlotsPublic(events.size(), 1);
            for (int i = 0; i < centeredSlots.length && i < events.size(); i++) {
                if (centeredSlots[i] == slot) {
                    deliveryGUI.openDeliveryMenu(player, events.get(i));
                    return;
                }
            }
        }

        // Bekleyen teslimatlar - satır 2 (slot 18-26)
        if (slot >= 18 && slot <= 26) {
            player.sendMessage("§e§lS§6wift§e§lD§6elivery §8» §7This delivery has not started yet!");
        }
    }

    private void handleDeliveryMenuClick(Player player, int slot, String title) {
        // Teslimat adını title'dan çıkar
        String deliveryDisplayName = title.replace(DeliveryGUI.DELIVERY_PREFIX, "");

        // Aktif event'i bul
        var activeEvent = deliveryService.getAllActiveEvents().stream()
                .filter(e -> deliveryGUI.getDeliveryDisplayName(e.getDeliveryName()).equals(deliveryDisplayName))
                .findFirst()
                .orElse(null);

        if (activeEvent == null) {
            player.sendMessage("§e§lS§6wift§e§lD§6elivery §8» §cDelivery is no longer active!");
            deliveryGUI.openMainMenu(player);
            return;
        }

        // Envanterden teslim et (slot 20)
        if (slot == 20) {
            player.closeInventory();
            deliverFromInventory(player, activeEvent);
            return;
        }

        // Sandıktan teslim et (slot 24)
        if (slot == 24) {
            player.closeInventory();
            deliveryGUI.startChestSelection(player, activeEvent.getDeliveryName());
            player.sendMessage("§e§lS§6wift§e§lD§6elivery §8» §aRight-click a chest! §7(10 seconds)");
            return;
        }

        // Geri butonu (slot 40)
        if (slot == 40) {
            deliveryGUI.openMainMenu(player);
        }
    }

    private void handleLeaderboardClick(Player player, int slot) {
        if (slot == 49) {
            deliveryGUI.openMainMenu(player);
        }
    }

    private void deliverFromInventory(Player player, com.nettyforge.swiftdelivery.service.ActiveEvent activeEvent) {
        String requiredItem = activeEvent.getResolvedItem();
        org.bukkit.Material material;
        try {
            material = org.bukkit.Material.valueOf(requiredItem.toUpperCase());
        } catch (Exception e) {
            player.sendMessage("§e§lS§6wift§e§lD§6elivery §8» §cInvalid item: §f" + requiredItem);
            return;
        }

        int count = 0;
        for (var item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }

        if (count == 0) {
            String itemName = deliveryGUI.getItemDisplayName(requiredItem);
            player.sendMessage("§e§lS§6wift§e§lD§6elivery §8» §cYou don't have §e" + itemName + " §cin inventory!");
            return;
        }

        // Envanterdeki eşyaları kaldır
        int remaining = count;
        org.bukkit.inventory.ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            var item = contents[i];
            if (item != null && item.getType() == material) {
                int itemAmount = item.getAmount();
                if (itemAmount <= remaining) {
                    remaining -= itemAmount;
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
            }
        }

        int delivered = count - remaining;
        deliveryService.recordDelivery(player.getUniqueId(), activeEvent.getDeliveryName(), delivered);

        // Veriyi senkronize et ve oyuncu ismini cache'le
        if (databaseManager != null) {
            databaseManager.saveActiveEvent(activeEvent);
        }
        deliveryGUI.cachePlayerName(player.getUniqueId(), player.getName());

        int total = activeEvent.getPlayerDeliveries().getOrDefault(player.getUniqueId(), 0);
        int rank = calculateRank(activeEvent, player.getUniqueId());
        String itemName = deliveryGUI.getItemDisplayName(requiredItem);

        player.sendTitle("§a§lDELIVERED!", "§e" + delivered + " §7amount §f" + itemName, 10, 50, 10);
        player.sendMessage(
                "§e§lS§6wift§e§lD§6elivery §8» §a" + delivered + " §7amount §e" + itemName + " §7delivered!");
        player.sendMessage("§e§lS§6wift§e§lD§6elivery §8» §7Total: §f" + total + " §8| §7Rank: §e#" + rank);

        try {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } catch (Exception ignored) {
        }
    }

    private void deliverFromRealChestInventory(Player player, org.bukkit.inventory.Inventory chestInv,
            com.nettyforge.swiftdelivery.service.ActiveEvent activeEvent) {
        String requiredItem = activeEvent.getResolvedItem();
        org.bukkit.Material material;
        try {
            material = org.bukkit.Material.valueOf(requiredItem.toUpperCase());
        } catch (Exception e) {
            player.sendMessage("§e§lS§6wift§e§lD§6elivery §8» §cInvalid item: §f" + requiredItem);
            return;
        }

        int count = 0;

        // Sandıktaki eşyaları say (double chest dahil)
        for (var item : chestInv.getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }

        if (count == 0) {
            String itemName = deliveryGUI.getItemDisplayName(requiredItem);
            player.sendMessage("§e§lS§6wift§e§lD§6elivery §8» §cChest does not contain §e" + itemName + " §cin inventory!");
            return;
        }

        // Sandıktaki eşyaları kaldır
        int remaining = count;
        org.bukkit.inventory.ItemStack[] contents = chestInv.getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            var item = contents[i];
            if (item != null && item.getType() == material) {
                int itemAmount = item.getAmount();
                if (itemAmount <= remaining) {
                    remaining -= itemAmount;
                    chestInv.setItem(i, null);
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
            }
        }

        int delivered = count - remaining;
        deliveryService.recordDelivery(player.getUniqueId(), activeEvent.getDeliveryName(), delivered);

        // Veriyi senkronize et ve oyuncu ismini cache'le
        if (databaseManager != null) {
            databaseManager.saveActiveEvent(activeEvent);
        }
        deliveryGUI.cachePlayerName(player.getUniqueId(), player.getName());

        int total = activeEvent.getPlayerDeliveries().getOrDefault(player.getUniqueId(), 0);
        int rank = calculateRank(activeEvent, player.getUniqueId());
        String itemName = deliveryGUI.getItemDisplayName(requiredItem);

        player.sendTitle("§a§lDELIVERED!", "§e" + delivered + " §7amount §f" + itemName, 10, 50, 10);
        player.sendMessage("§e§lS§6wift§e§lD§6elivery §8» §a" + delivered + " §7amount §e" + itemName
                + " §7delivered from chest!");
        player.sendMessage("§e§lS§6wift§e§lD§6elivery §8» §7Total: §f" + total + " §8| §7Rank: §e#" + rank);

        try {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } catch (Exception ignored) {
        }
    }

    private int calculateRank(com.nettyforge.swiftdelivery.service.ActiveEvent event, UUID playerUuid) {
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

    private void deliverPendingRewardsToPlayer(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        RewardService.PlayerResolver resolver = createPlayerResolver();
        RewardService.CommandExecutor executor = createCommandExecutor();

        int delivered = rewardService.deliverPendingRewards(
                player.getUniqueId(),
                resolver,
                executor,
                null);

        if (delivered > 0) {
            getLogger().info("Delivered " + delivered + " pending reward(s) to " + player.getName());
        }
    }

    private RewardService.PlayerResolver createPlayerResolver() {
        return new RewardService.PlayerResolver() {
            @Override
            public boolean isOnline(UUID uuid) {
                Player player = Bukkit.getPlayer(uuid);
                return player != null && player.isOnline();
            }

            @Override
            public String getName(UUID uuid) {
                Player player = Bukkit.getPlayer(uuid);
                return player != null ? player.getName() : null;
            }

            @Override
            public boolean giveItem(UUID uuid, String item, int amount) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) {
                    return false;
                }

                try {
                    org.bukkit.Material material = org.bukkit.Material.matchMaterial(item);
                    if (material != null) {
                        org.bukkit.inventory.ItemStack itemStack = new org.bukkit.inventory.ItemStack(material, amount);
                        player.getInventory().addItem(itemStack);
                        return true;
                    }
                } catch (Exception e) {
                    getLogger()
                            .warning("Failed to give item " + item + " to " + player.getName() + ": " + e.getMessage());
                }
                return false;
            }
        };
    }

    private RewardService.CommandExecutor createCommandExecutor() {
        return command -> {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            } catch (Exception e) {
                getLogger().warning("Failed to execute reward command: " + command + " - " + e.getMessage());
            }
        };
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String senderName = sender instanceof Player ? sender.getName() : "CONSOLE";
        UUID senderUuid = sender instanceof Player ? ((Player) sender).getUniqueId() : null;

        String cmdName = command.getName().toLowerCase();

        return switch (cmdName) {
            case "SwiftDelivery" -> commandHandler.handleCommand(senderName, args);
            case "delivery" -> {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cThis command can only be used by players.");
                    yield true;
                }
                yield deliverCommand.handleTeslimatCommand(senderName, senderUuid, args);
            }
            case "deliver" -> {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cThis command can only be used by players.");
                    yield true;
                }
                yield deliverCommand.handleTeslimCommand(senderName, senderUuid, args);
            }
            case "swiftmail" -> mailCommand.onCommand(sender, command, label, args);
            default -> false;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String senderName = sender instanceof Player ? sender.getName() : "CONSOLE";
        String cmdName = command.getName().toLowerCase();

        return switch (cmdName) {
            case "SwiftDelivery" -> commandHandler.handleTabComplete(senderName, args);
            case "delivery" -> deliverCommand.handleTeslimatTabComplete(senderName, args);
            case "deliver" -> deliverCommand.handleTeslimTabComplete(senderName, args);
            case "swiftmail" -> null; // let Bukkit handle player name autocomplete
            default -> List.of();
        };
    }

    private boolean checkPermission(String playerName, String permission) {
        if ("CONSOLE".equals(playerName)) {
            return true;
        }
        Player player = Bukkit.getPlayer(playerName);
        return player != null && player.hasPermission(permission);
    }

    private void sendMessageToPlayer(String playerName, String message) {
        if ("CONSOLE".equals(playerName)) {
            getLogger().info(message); // Console can stay legacy or plain
            return;
        }
        Player player = Bukkit.getPlayer(playerName);
        if (player != null && player.isOnline() && adventureHook != null) {
            adventureHook.sendMessage(player, message);
        } else if (player != null && player.isOnline()) { // fallback
            player.sendMessage(message);
        }
    }

    @org.bukkit.event.EventHandler
    public void onDeliveryDeliver(com.nettyforge.swiftdelivery.event.DeliveryDeliverEvent event) {
        if (event.isCancelled() || databaseManager == null) return;
        
        Player player = event.getPlayer();
        int amount = event.getAmount();
        
        // 1 XP per item delivered
        databaseManager.addCourierXp(player.getUniqueId(), amount);
        
        String summary = amount + "x " + event.getItemType() + " to " + event.getDeliveryName();
        // Server receives it, so receiver is represented as a zero UUID
        UUID serverUuid = new UUID(0, 0); 
        databaseManager.logTransaction(UUID.randomUUID().toString(), player.getUniqueId(), serverUuid, summary, "DELIVERY_EVENT");

        // Cosmetics
        org.bukkit.configuration.file.FileConfiguration config = getConfig();
        String soundName = config.getString("events.cosmetics.delivery-sound", "ENTITY_PLAYER_LEVELUP");
        String particleName = config.getString("events.cosmetics.delivery-particle", "VILLAGER_HAPPY");
        String actionBar = config.getString("events.cosmetics.delivery-action-bar", "<green><bold>SUCCESS!</bold></green> <gray>Item Delivered.</gray>");

        try {
            player.playSound(player.getLocation(), org.bukkit.Sound.valueOf(soundName), 1.0f, 1.0f);
        } catch (Exception ignored) {}

        try {
            player.spawnParticle(org.bukkit.Particle.valueOf(particleName), player.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
        } catch (Exception ignored) {}

        if (adventureHook != null) {
            adventureHook.sendActionBar(player, actionBar);
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public PlaceholderEngine getPlaceholderEngine() {
        return placeholderEngine;
    }

    public CategoryService getCategoryService() {
        return categoryService;
    }

    public SchedulerService getSchedulerService() {
        return schedulerService;
    }

    public DeliveryService getDeliveryService() {
        return deliveryService;
    }

    public RewardService getRewardService() {
        return rewardService;
    }

    public WebhookService getWebhookService() {
        return webhookService;
    }

    public MessageService getMessageService() {
        return messageService;
    }
}