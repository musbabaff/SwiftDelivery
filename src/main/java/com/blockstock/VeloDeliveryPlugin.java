package com.blockstock;

import com.blockstock.command.CommandHandler;
import com.blockstock.command.DeliverCommand;
import com.blockstock.config.ConfigManager;
import com.blockstock.config.ConfigManagerImpl;
import com.blockstock.gui.DeliveryGUI;
import com.blockstock.placeholder.PlaceholderEngine;
import com.blockstock.placeholder.PlaceholderEngineImpl;
import com.blockstock.reward.PendingRewardStore;
import com.blockstock.reward.PendingRewardStoreImpl;
import com.blockstock.reward.RewardService;
import com.blockstock.reward.RewardServiceImpl;
import com.blockstock.service.CategoryService;
import com.blockstock.service.CategoryServiceImpl;
import com.blockstock.service.DataManager;
import com.blockstock.service.DeliveryService;
import com.blockstock.service.DeliveryServiceImpl;
import com.blockstock.service.MessageService;
import com.blockstock.service.MessageServiceImpl;
import com.blockstock.service.SchedulerService;
import com.blockstock.service.SchedulerServiceImpl;
import com.blockstock.util.LoggingService;
import com.blockstock.webhook.WebhookService;
import com.blockstock.webhook.WebhookServiceImpl;
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

import java.io.File;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;

/**
 * Main plugin class for VeloDelivery.
 */
public class VeloDeliveryPlugin extends JavaPlugin implements Listener {

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
    private DeliveryGUI deliveryGUI;
    private ScheduledExecutorService executorService;

    private DataManager dataManager;

    private String currentLanguage = "tr";
    private String latestVersion = null; // GitHub'dan kontrol edilen son sürüm
    private boolean updateAvailable = false;

    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();

        getLogger().info("");
        getLogger().info("  VeloDelivery v" + getDescription().getVersion());
        getLogger().info("  Teslimat Etkinlik Sistemi");
        getLogger().info("");

        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }

            saveDefaultConfigs();
            executorService = Executors.newScheduledThreadPool(2);

            dataManager = new DataManager(this);

            initializeServices();
            loadConfigurations();
            registerListeners();
            resumeActiveEvents();
            loadSavedEvents();

            deliveryGUI.loadItemsConfig(getDataFolder());

            long loadTime = System.currentTimeMillis() - startTime;
            getLogger().info("Basariyla yuklendi! (" + loadTime + "ms)");

            // GitHub'dan güncelleme kontrolü (async)
            checkForUpdates();

        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable VeloDelivery", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("VeloDelivery kapatiliyor...");

        if (deliveryService != null && dataManager != null) {
            var activeEvents = deliveryService.getAllActiveEvents();
            if (!activeEvents.isEmpty()) {
                dataManager.saveAllActiveEvents(activeEvents);
            }
            dataManager.saveDataSync();
        }

        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }

        getLogger().info("Kapatildi!");
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

        File langFolder = new File(getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }
        saveResourceIfNotExists("lang/tr.yml");
        saveResourceIfNotExists("lang/en.yml");
        saveResourceIfNotExists("lang/de.yml");
        saveResourceIfNotExists("lang/es.yml");
        saveResourceIfNotExists("lang/fr.yml");
        saveResourceIfNotExists("lang/ru.yml");
        saveResourceIfNotExists("lang/zh.yml");
        saveResourceIfNotExists("lang/pt.yml");
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

        SchedulerServiceImpl schedulerImpl = new SchedulerServiceImpl(executorService);
        schedulerImpl.setEventStartCallback(this::handleEventStart);
        schedulerImpl.setEventEndCallback(this::handleEventEnd);
        schedulerService = schedulerImpl;

        webhookService = new WebhookServiceImpl(placeholderEngine);
        rewardService = new RewardServiceImpl(pendingRewardStore, placeholderEngine);

        getLogger().info("Servisler hazir.");
    }

    private void handleEventStart(String deliveryName) {
        if (deliveryService != null) {
            deliveryService.startEvent(deliveryName).ifPresent(event -> {
                String itemTR = deliveryGUI.getItemDisplayName(event.getResolvedItem());
                String catTR = deliveryGUI.getCategoryDisplayName(event.getResolvedCategory());
                String delTR = deliveryGUI.getDeliveryDisplayName(deliveryName);

                // Console log
                getLogger().info("[ETKINLIK BASLADI] " + delTR + " | Kategori: " + catTR + " | Esya: " + itemTR);

                // Broadcast
                String msg = "§e§lV§6elo§e§lD§6elivery §8» §a" + delTR + " §7basladi! §eIstenen: §f" + itemTR;
                Bukkit.broadcastMessage(msg);

                // Title
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendTitle("§a§l" + delTR.toUpperCase(), "§7Istenen: §e" + itemTR, 10, 70, 20);
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
            getLogger().info("[ETKINLIK BITTI] " + delTR + " | Kazananlar: " + winners.size());

            // Broadcast
            String msg = "§e§lV§6elo§e§lD§6elivery §8» §c" + delTR + " §7sona erdi!";
            Bukkit.broadcastMessage(msg);

            if (!winners.isEmpty()) {
                Bukkit.broadcastMessage("§e§lV§6elo§e§lD§6elivery §8» §6Kazananlar:");
                int rank = 1;
                for (var winner : winners) {
                    String color = rank == 1 ? "§6" : rank == 2 ? "§f" : rank == 3 ? "§c" : "§7";
                    Bukkit.broadcastMessage("  " + color + rank + ". §f" + winner.playerName() + " §8- §e"
                            + winner.deliveryCount() + " §7teslimat");
                    rank++;
                    if (rank > 3)
                        break;
                }
            }

            // Title
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendTitle("§c§lETKINLIK BITTI!", "§7" + delTR, 10, 70, 20);
            }

            // Webhook - kazananlarla birlikte
            sendWebhookWithWinners(deliveryName, itemTR, catTR, winners);
        }
    }

    private void sendWebhookWithWinners(String deliveryName, String itemTR, String categoryTR,
            java.util.List<com.blockstock.model.Winner> winners) {
        try {
            File configFile = new File(getDataFolder(), "config.yml");
            if (!configFile.exists()) {
                getLogger().warning("[WEBHOOK] config.yml bulunamadi: " + configFile.getAbsolutePath());
                return;
            }

            var config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
            boolean enabled = config.getBoolean("webhook.enabled", false);
            String url = config.getString("webhook.url", "");

            getLogger().info("[WEBHOOK-END] enabled=" + enabled + ", url="
                    + (url != null ? url.substring(0, Math.min(url.length(), 30)) + "..." : "null"));

            if (!enabled || url == null || url.isEmpty() || url.contains("YOUR_WEBHOOK")) {
                getLogger().info(
                        "[WEBHOOK-END] Atlanıyor: enabled=" + enabled + ", url_empty=" + (url == null || url.isEmpty())
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
                            .append("** - ").append(winner.deliveryCount()).append(" teslimat");
                    rank++;
                    if (rank > 5)
                        break;
                }
            } else {
                winnersText.append("\\n\\n*Kimse katılmadı*");
            }

            String desc = "**Eşya:** " + itemTR + "\\n**Kategori:** " + categoryTR + winnersText;
            String json = "{\"embeds\":[{\"title\":\"🏆 " + delTR + " Bitti!\",\"description\":\"" + desc
                    + "\",\"color\":16766720,\"footer\":{\"text\":\"VeloDelivery\"}}]}";

            // Async webhook gönder
            final String finalUrl = url;
            final String finalJson = json;
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                sendWebhookRequest(finalUrl, finalJson, "END");
            });
        } catch (Exception e) {
            getLogger().warning("[WEBHOOK] Hata: " + e.getMessage());
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
                getLogger().warning("[WEBHOOK] config.yml bulunamadi: " + configFile.getAbsolutePath());
                return;
            }

            var config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
            boolean enabled = config.getBoolean("webhook.enabled", false);
            String url = config.getString("webhook.url", "");

            getLogger().info("[WEBHOOK-START] enabled=" + enabled + ", url="
                    + (url != null ? url.substring(0, Math.min(url.length(), 30)) + "..." : "null"));

            if (!enabled || url == null || url.isEmpty() || url.contains("YOUR_WEBHOOK")) {
                getLogger().info("[WEBHOOK-START] Atlanıyor: enabled=" + enabled + ", url_empty="
                        + (url == null || url.isEmpty()) + ", placeholder="
                        + (url != null && url.contains("YOUR_WEBHOOK")));
                return;
            }

            String delTR = deliveryGUI.getDeliveryDisplayName(deliveryName);
            String title = "📦 " + delTR + " Başladı!";
            String desc = "**Teslim Edilecek:** " + itemTR + "\\n**Kategori:** " + categoryTR;

            String json = "{\"embeds\":[{\"title\":\"" + title + "\",\"description\":\"" + desc
                    + "\",\"color\":65280,\"footer\":{\"text\":\"VeloDelivery\"}}]}";

            // Async webhook gönder
            final String finalUrl = url;
            final String finalJson = json;
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                sendWebhookRequest(finalUrl, finalJson, "START");
            });
        } catch (Exception e) {
            getLogger().warning("[WEBHOOK] Hata: " + e.getMessage());
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
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 VeloDelivery/1.0");
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
                getLogger().info("[WEBHOOK] Gonderildi (" + tag + ")");
            } else {
                getLogger().warning("[WEBHOOK] Hata: " + responseCode);
            }

            connection.disconnect();

        } catch (java.net.SocketTimeoutException e) {
            getLogger().warning("[WEBHOOK] Zaman asimi - Discord'a ulasilamiyor");
        } catch (java.io.IOException e) {
            getLogger().warning("[WEBHOOK] Baglanti hatasi: " + e.getMessage());
        } catch (Exception e) {
            getLogger().warning("[WEBHOOK] Hata: " + e.getMessage());
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
                getLogger().info("Dil ayari: " + currentLanguage);
            }
        } catch (Exception e) {
            currentLanguage = "tr";
            getLogger().warning("Dil ayari yuklenemedi, varsayilan: tr");
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

        deliveryGUI = new DeliveryGUI(configManager, deliveryService);
        deliveryGUI.setLanguageSupplier(this::getCurrentLanguage);
        deliveryGUI.setDataFolder(getDataFolder());
        loadGUISettings();

        deliverCommand = new DeliverCommand(
                this::checkPermission,
                this::sendMessageToPlayer,
                deliveryGUI);

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
                getLogger().info("[VeloDelivery] Manuel etkinlik süresi doldu: " + deliveryName);
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
                getLogger().warning("[WEBHOOK TEST] config.yml bulunamadi!");
                return;
            }

            var config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(configFile);
            String url = config.getString("webhook.url", "");

            getLogger().info("[WEBHOOK TEST] URL uzunluk: " + url.length());

            if (url.isEmpty() || url.contains("YOUR_WEBHOOK")) {
                getLogger().warning("[WEBHOOK TEST] Webhook URL ayarlanmamis!");
                return;
            }

            String json = "{\"embeds\":[{\"title\":\"🧪 VeloDelivery Test\",\"description\":\"Webhook baglantisi basarili!\\n\\nBu bir test mesajidir.\",\"color\":65280,\"footer\":{\"text\":\"VeloDelivery v1.0.0\"}}]}";

            getLogger().info("[WEBHOOK TEST] Gonderiliyor...");

            final String finalUrl = url;
            final String finalJson = json;
            Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
                sendWebhookRequest(finalUrl, finalJson, "TEST");
            });
        } catch (Exception e) {
            getLogger().warning("[WEBHOOK TEST] Hata: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * GitHub'dan en son sürümü kontrol eder ve güncel değilse bildirim oluşturur.
     */
    private void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                String apiUrl = "https://api.github.com/repos/musbabaff/VeloDelivery/releases/latest";
                java.net.URI uri = java.net.URI.create(apiUrl);
                java.net.URL url = uri.toURL();
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "VeloDelivery/" + getDescription().getVersion());
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
                                getLogger().info("[GUNCELLEME] Yeni surum mevcut: v" + remoteVersion +
                                        " (Mevcut: v" + currentVersion + ")");
                                getLogger().info("[GUNCELLEME] https://github.com/musbabaff/VeloDelivery/releases");
                            } else {
                                getLogger().info("[GUNCELLEME] Guncel surum kullaniliyor (v" + currentVersion + ")");
                            }
                        }
                    }
                }
                connection.disconnect();
            } catch (Exception e) {
                // Güncelleme kontrolü başarısız - sessizce geç
                getLogger().fine("[GUNCELLEME] Kontrol basarisiz: " + e.getMessage());
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

        getLogger().info("Tum ayarlar yeniden yuklendi. Dil: " + currentLanguage);
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
            getLogger().warning("GUI ayarlari yuklenemedi: " + e.getMessage());
        }
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void resumeActiveEvents() {
        if (schedulerService != null) {
            schedulerService.resumeActiveEvents();
        }

        if (configManager != null && configManager.getDeliveryConfig() != null) {
            var enabledDeliveries = configManager.getDeliveryConfig().getEnabledDeliveries();
            if (!enabledDeliveries.isEmpty()) {
                getLogger().info("Zamanlanmis teslimatlar:");
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
        if (dataManager == null || deliveryService == null)
            return;

        var savedEvents = dataManager.loadActiveEvents();
        if (savedEvents.isEmpty())
            return;

        getLogger().info("Kaydedilmis etkinlikler yukleniyor...");

        for (var savedData : savedEvents) {
            try {
                // Etkinlik hala geçerli mi kontrol et (bitiş zamanı geçmemiş mi)
                if (savedData.endTime() != null && savedData.endTime().isBefore(java.time.ZonedDateTime.now())) {
                    getLogger().info("  - " + savedData.deliveryName() + " (suresi dolmus, atlanıyor)");
                    dataManager.removeActiveEvent(savedData.deliveryName());
                    continue;
                }

                // ActiveEvent oluştur ve servise ekle
                var activeEvent = savedData.toActiveEvent();
                ((DeliveryServiceImpl) deliveryService).restoreEvent(activeEvent);

                getLogger().info("  + " + savedData.deliveryName() + " (yuklendi, " +
                        savedData.playerDeliveries().size() + " oyuncu verisi)");

            } catch (Exception e) {
                getLogger().warning("  x " + savedData.deliveryName() + ": " + e.getMessage());
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (rewardService != null && rewardService.hasPendingRewards(player.getUniqueId())) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                deliverPendingRewardsToPlayer(player);
            }, 20L);
        }

        // Güncelleme bildirimi (sadece OP oyuncular)
        if (updateAvailable && latestVersion != null && player.isOp()) {
            Bukkit.getScheduler().runTaskLater(this, () -> {
                player.sendMessage("");
                player.sendMessage("§e§lV§6elo§e§lD§6elivery §8» §aYeni sürüm mevcut!");
                player.sendMessage(
                        "§7  Mevcut: §cv" + getDescription().getVersion() + " §8» §7Yeni: §av" + latestVersion);
                player.sendMessage("§7  §ehttps://github.com/musbabaff/VeloDelivery/releases");
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
            player.sendMessage("§e§lV§6elo§e§lD§6elivery §8» §cBu sandığa erişim izniniz yok!");
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
            player.sendMessage("§e§lV§6elo§e§lD§6elivery §8» §cBu bir sandık değil!");
            return;
        }

        event.setCancelled(true);

        String deliveryName = deliveryGUI.getChestSelectionDelivery(player.getUniqueId());
        deliveryGUI.cancelChestSelection(player.getUniqueId());

        var activeEvent = deliveryService.getActiveEvent(deliveryName).orElse(null);
        if (activeEvent == null) {
            player.sendMessage("§e§lV§6elo§e§lD§6elivery §8» §cTeslimat artık aktif değil!");
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
        if (player.hasPermission("velodelivery.bypass.protection")) {
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
            getLogger().warning("Koruma kontrolü hatası: " + e.getMessage());
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
            player.sendMessage("§e§lV§6elo§e§lD§6elivery §8» §7Bu teslimat henüz başlamadı!");
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
            player.sendMessage("§e§lV§6elo§e§lD§6elivery §8» §cTeslimat artik aktif degil!");
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
            player.sendMessage("§e§lV§6elo§e§lD§6elivery §8» §aBir sandığa sağ tıkla! §7(10 saniye)");
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

    private void deliverFromInventory(Player player, com.blockstock.service.ActiveEvent activeEvent) {
        String requiredItem = activeEvent.getResolvedItem();
        org.bukkit.Material material;
        try {
            material = org.bukkit.Material.valueOf(requiredItem.toUpperCase());
        } catch (Exception e) {
            player.sendMessage("§e§lV§6elo§e§lD§6elivery §8» §cGeçersiz eşya: §f" + requiredItem);
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
            player.sendMessage("§e§lV§6elo§e§lD§6elivery §8» §cEnvanterinde §e" + itemName + " §cyok!");
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

        // Veriyi kaydet ve oyuncu ismini cache'le
        if (dataManager != null) {
            dataManager.saveActiveEvent(activeEvent);
            dataManager.updatePlayerStats(player.getUniqueId(), player.getName(), delivered);
        }
        deliveryGUI.cachePlayerName(player.getUniqueId(), player.getName());

        int total = activeEvent.getPlayerDeliveries().getOrDefault(player.getUniqueId(), 0);
        int rank = calculateRank(activeEvent, player.getUniqueId());
        String itemName = deliveryGUI.getItemDisplayName(requiredItem);

        player.sendTitle("§a§lTESLİM EDİLDİ!", "§e" + delivered + " §7adet §f" + itemName, 10, 50, 10);
        player.sendMessage(
                "§e§lV§6elo§e§lD§6elivery §8» §a" + delivered + " §7adet §e" + itemName + " §7teslim edildi!");
        player.sendMessage("§e§lV§6elo§e§lD§6elivery §8» §7Toplam: §f" + total + " §8| §7Sıra: §e#" + rank);

        try {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } catch (Exception ignored) {
        }
    }

    private void deliverFromRealChestInventory(Player player, org.bukkit.inventory.Inventory chestInv,
            com.blockstock.service.ActiveEvent activeEvent) {
        String requiredItem = activeEvent.getResolvedItem();
        org.bukkit.Material material;
        try {
            material = org.bukkit.Material.valueOf(requiredItem.toUpperCase());
        } catch (Exception e) {
            player.sendMessage("§e§lV§6elo§e§lD§6elivery §8» §cGeçersiz eşya: §f" + requiredItem);
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
            player.sendMessage("§e§lV§6elo§e§lD§6elivery §8» §cSandıkta §e" + itemName + " §cyok!");
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

        // Veriyi kaydet ve oyuncu ismini cache'le
        if (dataManager != null) {
            dataManager.saveActiveEvent(activeEvent);
            dataManager.updatePlayerStats(player.getUniqueId(), player.getName(), delivered);
        }
        deliveryGUI.cachePlayerName(player.getUniqueId(), player.getName());

        int total = activeEvent.getPlayerDeliveries().getOrDefault(player.getUniqueId(), 0);
        int rank = calculateRank(activeEvent, player.getUniqueId());
        String itemName = deliveryGUI.getItemDisplayName(requiredItem);

        player.sendTitle("§a§lTESLİM EDİLDİ!", "§e" + delivered + " §7adet §f" + itemName, 10, 50, 10);
        player.sendMessage("§e§lV§6elo§e§lD§6elivery §8» §a" + delivered + " §7adet §e" + itemName
                + " §7sandıktan teslim edildi!");
        player.sendMessage("§e§lV§6elo§e§lD§6elivery §8» §7Toplam: §f" + total + " §8| §7Sıra: §e#" + rank);

        try {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } catch (Exception ignored) {
        }
    }

    private int calculateRank(com.blockstock.service.ActiveEvent event, UUID playerUuid) {
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
            case "velodelivery" -> commandHandler.handleCommand(senderName, args);
            case "teslimat" -> {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cBu komut sadece oyuncular kullanabilir.");
                    yield true;
                }
                yield deliverCommand.handleTeslimatCommand(senderName, senderUuid, args);
            }
            case "teslim" -> {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cBu komut sadece oyuncular kullanabilir.");
                    yield true;
                }
                yield deliverCommand.handleTeslimCommand(senderName, senderUuid, args);
            }
            default -> false;
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String senderName = sender instanceof Player ? sender.getName() : "CONSOLE";
        String cmdName = command.getName().toLowerCase();

        return switch (cmdName) {
            case "velodelivery" -> commandHandler.handleTabComplete(senderName, args);
            case "teslimat" -> deliverCommand.handleTeslimatTabComplete(senderName, args);
            case "teslim" -> deliverCommand.handleTeslimTabComplete(senderName, args);
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
            getLogger().info(message);
            return;
        }
        Player player = Bukkit.getPlayer(playerName);
        if (player != null && player.isOnline()) {
            player.sendMessage(message);
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