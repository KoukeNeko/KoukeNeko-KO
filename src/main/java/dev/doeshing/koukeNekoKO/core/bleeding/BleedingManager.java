package dev.doeshing.koukeNekoKO.core.bleeding;

import dev.doeshing.koukeNekoKO.KoukeNekoKO;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
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
        Component bleedingMessage = formatText(bleedingMessageRaw);
        player.sendMessage(bleedingMessage);

        // 顯示標題
        if (plugin.getConfig().getBoolean("bleeding.effects.title", true)) {
            String titleRaw = plugin.getConfig().getString("bleeding.effects.title-text", "&c你正在流血！");
            String subtitleRaw = plugin.getConfig().getString("bleeding.effects.subtitle-text", "&e需要隊友救援！");
            
            Component title = formatText(titleRaw);
            Component subtitle = formatText(subtitleRaw);
            

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

        // 播放音效
        if (plugin.getConfig().getBoolean("bleeding.effects.sound", true)) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.5f);
        }

        // 設定玩家狀態
        player.setInvulnerable(true);
        
        // 讓玩家看起來像是倒在地上
        // 保存原始位置以便救援後恢復
        data.setOriginalLocation(player.getLocation().clone());
        
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
        
        // 限制移動
        if (!canMove) {
            // 固定玩家位置
            player.setWalkSpeed(0);
            player.setFlySpeed(0);
        } else {
            // 即使可以移動，也減慢速度模擬受傷
            player.setWalkSpeed(0.05f); // 正常速度的1/4
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

        // 從線上玩家獲取Player物件
        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            bleedingPlayers.remove(playerId);
            return;
        }

        // 恢復玩家狀態
        restorePlayerState(player);
        
        // 恢復玩家原始位置和視角
        if (data.getOriginalLocation() != null) {
            player.teleport(data.getOriginalLocation());
        }

        if (rescued) {
            // 如果被救援，恢復一半的生命值
            double maxHealth = Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getValue();
            player.setHealth(Math.max(1.0, maxHealth / 2));

            // 顯示被救援訊息
            String rescuerName = data.getLastRescuer() != null ? 
                Objects.requireNonNull(plugin.getServer().getPlayer(data.getLastRescuer())).getName() : "某人";
                
            String rescuedMessageRaw = plugin.getConfig().getString("bleeding.messages.bleeding-rescued", "&a你已被成功救援！")
                .replace("{player}", rescuerName);
            Component rescuedMessage = formatText(rescuedMessageRaw);
            player.sendMessage(rescuedMessage);

        } else {
            // 如果沒有被救援，處理死亡
            String deathMessageRaw = plugin.getConfig().getString("bleeding.messages.bleeding-death", "&c你因失血過多而死亡！");
            Component deathMessage = formatText(deathMessageRaw);
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
            Component cannotRescueSelf = formatText(cannotRescueSelfRaw);
            rescuer.sendMessage(cannotRescueSelf);
            return;
        }

        BleedingPlayerData data = bleedingPlayers.get(targetId);

        // 記錄最後一個救援者
        data.setLastRescuer(rescuer.getUniqueId());

        // 減少所需點選次數
        int remainingClicks = data.decrementRemainingClicks();

        // 向救援者顯示資訊
        String rescuerMessageRaw = plugin.getConfig().getString("bleeding.messages.rescuer-start", "&a你正在救援 {player}！還需 {clicks} 次點選。")
            .replace("{player}", target.getName())
            .replace("{clicks}", String.valueOf(remainingClicks));
        Component rescuerMessage = formatText(rescuerMessageRaw);
        rescuer.sendMessage(rescuerMessage);

        // 向瀕死玩家顯示資訊
        String targetMessageRaw = plugin.getConfig().getString("bleeding.messages.bleeding-rescue", "&a玩家 {player} 正在救援你！還需 {clicks} 次點選。")
            .replace("{player}", rescuer.getName())
            .replace("{clicks}", String.valueOf(remainingClicks));
        Component targetMessage = formatText(targetMessageRaw);
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
            Component successMessage = formatText(successMessageRaw);
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
