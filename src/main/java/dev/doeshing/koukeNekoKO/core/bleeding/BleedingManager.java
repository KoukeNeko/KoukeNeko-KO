package dev.doeshing.koukeNekoKO.core.bleeding;

import dev.doeshing.koukeNekoKO.KoukeNekoKO;
import dev.doeshing.koukeNekoKO.core.bleeding.BleedingPlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class BleedingManager {
    private final KoukeNekoKO plugin;
    private final Map<UUID, BleedingPlayerData> bleedingPlayers = new HashMap<>();

    public BleedingManager(KoukeNekoKO plugin) {
        this.plugin = plugin;
    }

    /**
     * 將舊式顏色代碼字串轉換為 Adventure Component
     * @param text 含有 &color 代碼的文字
     * @return 已處理的 Component
     */
    public Component formatText(String text) {
        if (text == null) return Component.empty();
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
    
    /**
     * 將文字字串轉換為 Adventure Component，並自動加上前綴
     * @param text 原始文字
     * @return 已加上前綴和顏色代碼處理的 Component
     */
    public Component formatTextWithPrefix(String text) {
        if (text == null) return Component.empty();
        String prefix = plugin.getConfig().getString("prefix", "&8[&6KoukeNeko-KO&8] &r");
        return formatText(prefix + text);
    }

    /**
     * 讓玩家進入瀕死狀態
     * @param player 要進入瀕死狀態的玩家
     */
    public void startBleeding(Player player) {
        UUID playerId = player.getUniqueId();

        // 如果玩家已經在瀕死狀態，不做處理
        if (isPlayerBleeding(playerId)) {
            return;
        }

        // 獲取瀕死持續時間
        int duration = plugin.getConfig().getInt("bleeding.duration", 60);

        // 獲取是否可以移動
        boolean canMove = plugin.getConfig().getBoolean("bleeding.can-move", false);

        // 建立瀕死資料
        BleedingPlayerData data = new BleedingPlayerData(
                player.getLocation(),
                plugin.getConfig().getInt("bleeding.revival-clicks", 5)
        );

        // 顯示瀕死資訊
        String bleedingMessageRaw = plugin.getConfig().getString("bleeding.messages.bleeding-start", "&c你進入了瀕死狀態！")
                .replace("{time}", String.valueOf(duration));
        Component bleedingMessage = formatTextWithPrefix(bleedingMessageRaw);
        player.sendMessage(bleedingMessage);

        // 顯示標題
        if (plugin.getConfig().getBoolean("bleeding.effects.title", true)) {
            String titleRaw = plugin.getConfig().getString("bleeding.effects.title-text", "&c你正在流血！");
            String subtitleRaw = plugin.getConfig().getString("bleeding.effects.subtitle-text", "&e需要隊友救援！");
            
            Component title = formatText(titleRaw);
            Component subtitle = formatText(subtitleRaw);
            
            // 使用非已棄用的 Title API
            Title titleObj = Title.title(
                title, 
                subtitle, 
                Title.Times.times(
                    Duration.ofMillis(500),  // fade in
                    Duration.ofSeconds(5),   // stay
                    Duration.ofMillis(500)   // fade out
                )
            );
            player.showTitle(titleObj);
        }

        // 創建瀕死狀態顯示的BossBar
        String bossBarTitle = plugin.getConfig().getString("bleeding.messages.bleeding-bossbar", "&c你正在流血! &7- 剩餘時間: {time}秒");
        BossBar bleedingBar = Bukkit.createBossBar(
                ChatColor.translateAlternateColorCodes('&', bossBarTitle.replace("{time}", String.valueOf(duration))),
                BarColor.RED,
                BarStyle.SOLID
        );
        bleedingBar.addPlayer(player);
        bleedingBar.setVisible(true);
        data.setBleedingBossBar(bleedingBar);
        
        // 創建頭頂倒數計時顯示
        // 初始倒數文字
        String countdownText = plugin.getConfig().getString("bleeding.messages.head-countdown", "&c瀕死 &f{time}&c 秒")
                .replace("{time}", String.valueOf(duration));
        
        // 建立一個 TextDisplay 實體
        Location aboveHeadLoc = player.getLocation().add(0, 2.5, 0); // 在玩家頭頂上方
        TextDisplay textDisplay = (TextDisplay) player.getWorld().spawnEntity(aboveHeadLoc, EntityType.TEXT_DISPLAY);
        textDisplay.text(formatText(countdownText));
        textDisplay.setBillboard(Display.Billboard.CENTER); // 始終面向玩家
        textDisplay.setBackgroundColor(org.bukkit.Color.fromARGB(100, 0, 0, 0)); // 半透明黑色背景
        textDisplay.setSeeThrough(false); // 不透視
        textDisplay.setDisplayWidth(1.0f); // 設置適當寬度
        
        // 將 TextDisplay 實體存入玩家數據
        data.setCountdownDisplay(textDisplay);

        // 播放音效
        if (plugin.getConfig().getBoolean("bleeding.effects.sound", true)) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.5f);
        }

        // 設定玩家狀態
        player.setInvulnerable(true);

        
        // 調整玩家視角 - 讓玩家視角朝下，模擬倒地
        Location downLook = player.getLocation().clone();
        downLook.setPitch(90); // 90度視角 (向下看)
        player.teleport(downLook);
        
        // 設定「倒地」效果
        player.setGliding(false); // 確保不在滑翔狀態
        
        // 增加額外的倒地視覺效果
        // 使用失明效果模擬會失去意識的視覺
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.BLINDNESS, 20, 0));
        
        // 使用緩速效果模擬受傷「手腕發抖」
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 2, false, false));
            
        // 使用虛弱效果模擬受重傷
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.WEAKNESS, Integer.MAX_VALUE, 1, false, false));
            
        // 禁用玩家的自然回血
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.HEALTH_BOOST, Integer.MAX_VALUE, 0, false, false, false));
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
            org.bukkit.potion.PotionEffectType.SATURATION, Integer.MAX_VALUE, 0, false, false, false));
        
        // 限制移動
        if (!canMove) {
            // 固定玩家位置
            player.setWalkSpeed(0);
            player.setFlySpeed(0);
        } else {
            // 即使可以移動，也減慢速度模擬受傷
            player.setWalkSpeed(0.01f); // 設置為極慢的速度，但仍能移動
            player.setFlySpeed(0.01f);  // 同樣設置極慢的飛行速度
            
            // 添加緩慢效果加強移動困難感
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 4, false, false, false));
        }

        // 建立瀕死倒計時任務
        BukkitTask timerTask = new BukkitRunnable() {
            int timeLeft = duration;

            @Override
            public void run() {
                // 顯示粒子效果
                if (plugin.getConfig().getBoolean("bleeding.effects.particles", true)) {
                    // 在玩家周圍顯示受傷粒子
                    player.getWorld().spawnParticle(
                        Particle.DAMAGE_INDICATOR, 
                        player.getLocation().add(0, 0.5, 0), 
                        5, 
                        0.5, 0.1, 0.5, 
                        0
                    );
                    
                    // 在玩家附近增加紅色血軸粒子
                    player.getWorld().spawnParticle(
                        Particle.BLOCK_CRUMBLE, 
                        player.getLocation().add(0, 0.2, 0), 
                        15, 
                        0.4, 0.1, 0.4, 
                        0,
                        org.bukkit.Material.REDSTONE_BLOCK.createBlockData()
                    );
                }
                
                // 確保玩家生命值保持在 1 點
                if (player.getHealth() > 1.0) {
                    player.setHealth(1.0);
                }

                // 更新BossBar進度和標題
                BossBar bleedingBar = data.getBleedingBossBar();
                if (bleedingBar != null) {
                    // 更新進度
                    float progress = (float) timeLeft / duration;
                    bleedingBar.setProgress(Math.max(0, Math.min(1, progress)));
                    
                    // 更新標題
                    String bossBarTitle = plugin.getConfig().getString("bleeding.messages.bleeding-bossbar", "&c你正在流血! &7- 剩餘時間: {time}秒");
                    bleedingBar.setTitle(LegacyComponentSerializer.legacyAmpersand().serialize(
                        formatText(bossBarTitle.replace("{time}", String.valueOf(timeLeft)))));
                }
                
                // 每秒更新浮動倒數文字
                if (plugin.getConfig().getBoolean("bleeding.effects.floating-countdown", true)) {
                    // 在玩家頭頂上方顯示倒數計時
                    String countdownText = plugin.getConfig().getString("bleeding.effects.floating-countdown-text", "&c瀕死倒數: &e{time} 秒");
                    countdownText = countdownText.replace("{time}", String.valueOf(timeLeft));
                    player.sendActionBar(formatText(countdownText)); // 在動作欄顯示
                    
                    // 在玩家頭頂上方顯示浮動文字
                    Component text = formatText("&c♥ &4" + timeLeft + "&c ♥");
                    player.displayName(text);
                    player.playerListName(text);
                    player.customName(text);
                    player.setCustomNameVisible(true);
                }
                TextDisplay countdownDisplay = data.getCountdownDisplay();
                if (countdownDisplay != null && !countdownDisplay.isDead()) {
                    // 更新倒數文字
                    String countdownText = plugin.getConfig().getString("bleeding.messages.head-countdown", "&c瀕死 &f{time}&c 秒")
                        .replace("{time}", String.valueOf(timeLeft));
                        
                    countdownDisplay.text(formatText(countdownText));
                    
                    // 更新位置（跟隨玩家）
                    countdownDisplay.teleport(player.getLocation().add(0, 2.5, 0));
                    
                    // 根據剩餘時間變更顏色
                    if (timeLeft <= 5) {
                        // 最後5秒時文字閃爍
                        if (timeLeft % 2 == 0) {
                            countdownDisplay.setBackgroundColor(org.bukkit.Color.fromARGB(180, 128, 0, 0)); // 深紅色
                        } else {
                            countdownDisplay.setBackgroundColor(org.bukkit.Color.fromARGB(150, 0, 0, 0)); // 黑色
                        }
                    }
                }

                timeLeft--;

                // 如果時間到了或玩家離線，終止瀕死狀態並處理死亡
                if (timeLeft <= 0 || !player.isOnline()) {
                    this.cancel();
                    if (player.isOnline()) {
                        endBleeding(playerId, false);
                    } else {
                        bleedingPlayers.remove(playerId);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        // 儲存瀕死資料
        data.setTimerTask(timerTask);
        bleedingPlayers.put(playerId, data);
    }

    /**
     * 結束玩家的瀕死狀態
     * @param playerId 玩家UUID
     * @param rescued 是否被救援
     */
    public void endBleeding(UUID playerId, boolean rescued) {
        BleedingPlayerData data = bleedingPlayers.get(playerId);
        if (data == null) {
            return;
        }

        // 取消計時器任務
        if (data.getTimerTask() != null) {
            data.getTimerTask().cancel();
        }

        // 移除BossBar
        if (data.getBleedingBossBar() != null) {
            data.getBleedingBossBar().removeAll();
        }
        
        // 移除救援者的BossBar
        if (data.getRescuerBossBar() != null) {
            data.getRescuerBossBar().removeAll();
        }
        
        // 移除頭頂倒數計時顯示
        if (data.getCountdownDisplay() != null && !data.getCountdownDisplay().isDead()) {
            data.getCountdownDisplay().remove(); // 移除實體
        }

        // 從線上玩家獲取Player物件
        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            bleedingPlayers.remove(playerId);
            return;
        }

        // 恢復玩家狀態
        restorePlayerState(player);


        if (rescued) {
            // 如果被救援，恢復一半的生命值
            double maxHealth = Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue();
            player.setHealth(Math.max(1.0, maxHealth / 2));

            // 顯示被救援訊息
            String rescuerName = data.getLastRescuer() != null ? 
                Objects.requireNonNull(plugin.getServer().getPlayer(data.getLastRescuer())).getName() : "某人";
                
            String rescuedMessageRaw = plugin.getConfig().getString("bleeding.messages.bleeding-rescued", "&a你已被成功救援！")
                .replace("{player}", rescuerName);
            Component rescuedMessage = formatTextWithPrefix(rescuedMessageRaw);
            player.sendMessage(rescuedMessage);

        } else {
            // 如果沒有被救援，處理死亡
            String deathMessageRaw = plugin.getConfig().getString("bleeding.messages.bleeding-death", "&c你因失血過多而死亡！");
            Component deathMessage = formatTextWithPrefix(deathMessageRaw);
            player.sendMessage(deathMessage);

            // 實際擊殺玩家（會觸發死亡事件）
            player.setHealth(0);
        }

        // 移除瀕死狀態
        bleedingPlayers.remove(playerId);
    }

    /**
     * 嘗試救援一個瀕死玩家
     *
     * @param rescuer 施救者
     * @param target  目標瀕死玩家
     */
    public void attemptRescue(Player rescuer, Player target) {
        UUID targetId = target.getUniqueId();

        // 檢查目標是否處於瀕死狀態
        if (!isPlayerBleeding(targetId)) {
            return;
        }

        // 檢查不能自救
        if (rescuer.getUniqueId().equals(targetId)) {
            String cannotRescueSelfRaw = plugin.getConfig().getString("bleeding.messages.cannot-rescue-self", "&c你不能救援自己！");
            Component cannotRescueSelf = formatTextWithPrefix(cannotRescueSelfRaw);
            rescuer.sendMessage(cannotRescueSelf);
            return;
        }

        BleedingPlayerData data = bleedingPlayers.get(targetId);

        // 記錄最後一個救援者
        data.setLastRescuer(rescuer.getUniqueId());

        // 減少所需點選次數
        int remainingClicks = data.decrementRemainingClicks();
        int totalClicks = data.getTotalClicksNeeded();

        // 建立或更新救援者的BossBar
        BossBar rescuerBar = data.getRescuerBossBar();
        if (rescuerBar == null) {
            String rescuerBarTitle = plugin.getConfig().getString("bleeding.messages.rescuer-bossbar",
                    String.valueOf(Component.text("&a正在救援 {player} &7- &e進度: {progress}/{total}")));
            rescuerBarTitle = rescuerBarTitle
                .replace("{player}", target.getName())
                .replace("{progress}", String.valueOf(totalClicks - remainingClicks))
                .replace("{total}", String.valueOf(totalClicks));
                
            rescuerBar = Bukkit.createBossBar(
                LegacyComponentSerializer.legacyAmpersand().serialize(formatText(rescuerBarTitle)),
                BarColor.GREEN,
                BarStyle.SEGMENTED_10
            );
            rescuerBar.addPlayer(rescuer);
            rescuerBar.setVisible(true);
            data.setRescuerBossBar(rescuerBar);
        } else {
            // 更新救援BossBar
            String rescuerBarTitle = plugin.getConfig().getString("bleeding.messages.rescuer-bossbar", 
                "&a正在救援 {player} &7- &e點擊進度: {progress}/{total}");
            rescuerBarTitle = rescuerBarTitle
                .replace("{player}", target.getName())
                .replace("{progress}", String.valueOf(totalClicks - remainingClicks))
                .replace("{total}", String.valueOf(totalClicks));
                
            rescuerBar.setTitle(LegacyComponentSerializer.legacyAmpersand().serialize(formatText(rescuerBarTitle)));
            rescuerBar.addPlayer(rescuer); // 確保當前救援者可以看到
        }
        
        // 設定進度條
        float progress = (float) (totalClicks - remainingClicks) / totalClicks;
        rescuerBar.setProgress(Math.max(0, Math.min(1, progress)));

        // 向救援者顯示資訊
        String rescuerMessageRaw = plugin.getConfig().getString("bleeding.messages.rescuer-start", "&a你正在救援 {player}！還需 {clicks} 次點選。")
            .replace("{player}", target.getName())
            .replace("{clicks}", String.valueOf(remainingClicks));
        Component rescuerMessage = formatTextWithPrefix(rescuerMessageRaw);
        rescuer.sendMessage(rescuerMessage);

        // 向瀕死玩家顯示資訊
        String targetMessageRaw = plugin.getConfig().getString("bleeding.messages.bleeding-rescue", "&a玩家 {player} 正在救援你！還需 {clicks} 次點選。")
            .replace("{player}", rescuer.getName())
            .replace("{clicks}", String.valueOf(remainingClicks));
        Component targetMessage = formatTextWithPrefix(targetMessageRaw);
        target.sendMessage(targetMessage);

        // 播放救援音效
        target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        rescuer.playSound(rescuer.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        
        // 救援時增加特效
        // 在救援者與瀕死玩家之間顯示綠色治療粒子
        target.getWorld().spawnParticle(
            Particle.HAPPY_VILLAGER, 
            target.getLocation().add(0, 1.0, 0), 
            10, 
            0.5, 0.5, 0.5, 
            0
        );
        
        // 使用經驗球粒子模擬生命力恢復
        target.getWorld().spawnParticle(
            Particle.INSTANT_EFFECT, 
            target.getLocation().add(0, 0.5, 0), 
            15, 
            0.5, 0.2, 0.5, 
            0
        );

        // 如果點選次數達到了，完成救援
        if (remainingClicks <= 0) {
            // 顯示救援成功資訊
            String successMessageRaw = plugin.getConfig().getString("bleeding.messages.rescuer-success", "&a你成功救援了 {player}！")
                .replace("{player}", target.getName());
            Component successMessage = formatTextWithPrefix(successMessageRaw);
            rescuer.sendMessage(successMessage);
            
            // 救援成功時播放特效音效
            target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            rescuer.playSound(rescuer.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            
            // 增加返生特效
            // 炫目的光柱特效
            target.getWorld().spawnParticle(
                Particle.END_ROD, 
                target.getLocation().add(0, 1.0, 0), 
                40, 
                0.5, 1.0, 0.5, 
                0.1
            );
            
            // 增加治療特效粒子
            target.getWorld().spawnParticle(
                Particle.HEART, 
                target.getLocation().add(0, 1.2, 0), 
                10, 
                0.5, 0.5, 0.5, 
                0
            );

            // 結束瀕死狀態（被救援）
            endBleeding(targetId, true);
        }
    }

    /**
     * 檢查玩家是否處於瀕死狀態
     * @param playerId 玩家UUID
     * @return 如果玩家正在瀕死狀態中返回true
     */
    public boolean isPlayerBleeding(UUID playerId) {
        return bleedingPlayers.containsKey(playerId);
    }

    /**
     * 獲取所有處於瀕死狀態的玩家
     * @return 包含所有瀕死玩家資料的對映
     */
    public Map<UUID, BleedingPlayerData> getBleedingPlayers() {
        return bleedingPlayers;
    }

    /**
     * 清除所有瀕死玩家（用於插件停用時）
     */
    public void clearAllBleedingPlayers() {
        // 複製鍵集以避免併發修改異常
        for (UUID playerId : new HashMap<>(bleedingPlayers).keySet()) {
            endBleeding(playerId, false);
        }
        bleedingPlayers.clear();
    }
    
    /**
     * 恢復玩家的正常狀態，清除所有瀕死效果
     * @param player 要恢復狀態的玩家
     */
    private void restorePlayerState(Player player) {
        // 恢復玩家狀態
        player.setInvulnerable(false);
        player.setWalkSpeed(0.2f); // 恢復預設行走速度
        player.setFlySpeed(0.1f);  // 恢復預設飛行速度
        
        // 移除所有瀕死狀態的藥水效果
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.HEALTH_BOOST);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.SATURATION);
    }
    
    /**
     * 安全地移除玩家的瀕死狀態，不會將玩家生命值設為 0
     * 適用於玩家已經死亡的場合
     * 
     * @param playerId 玩家UUID
     */
    public void removeBleedingState(UUID playerId) {
        BleedingPlayerData data = bleedingPlayers.get(playerId);
        if (data == null) {
            return;
        }

        // 取消計時器任務
        if (data.getTimerTask() != null) {
            data.getTimerTask().cancel();
        }

        // 移除BossBar
        if (data.getBleedingBossBar() != null) {
            data.getBleedingBossBar().removeAll();
        }
        
        // 移除救援者的BossBar
        if (data.getRescuerBossBar() != null) {
            data.getRescuerBossBar().removeAll();
        }

        // 移除頭頂倒數計時顯示
        if (data.getCountdownDisplay() != null && !data.getCountdownDisplay().isDead()) {
            data.getCountdownDisplay().remove(); // 移除實體
        }

        // 從線上玩家獲取Player物件
        Player player = plugin.getServer().getPlayer(playerId);
        if (player != null && player.isOnline()) {
            // 恢復玩家狀態
            restorePlayerState(player);
        }

        // 移除瀕死狀態
        bleedingPlayers.remove(playerId);
    }
}
