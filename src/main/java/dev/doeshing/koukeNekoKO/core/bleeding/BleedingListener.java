package dev.doeshing.koukeNekoKO.core.bleeding;

import dev.doeshing.koukeNekoKO.KoukeNekoKO;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class BleedingListener implements Listener {
    private final KoukeNekoKO plugin;
    private final BleedingManager bleedingManager;

    public BleedingListener(KoukeNekoKO plugin, BleedingManager bleedingManager) {
        this.plugin = plugin;
        this.bleedingManager = bleedingManager;
    }

    /**
     * 監聽玩家受到致命傷害的事件，啟動瀕死階段
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerFatalDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();

        // 僅處理玩家實體
        if (!(entity instanceof Player player)) {
            return;
        }

        // 如果玩家已經處於瀕死狀態，不再處理
        if (bleedingManager.isPlayerBleeding(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // 計算傷害後的生命值
        double healthAfterDamage = player.getHealth() - event.getFinalDamage();

        // 如果傷害會導致玩家死亡，啟動瀕死階段
        if (healthAfterDamage <= 0.0) {
            // 取消原始傷害事件
            event.setCancelled(true);

            // 設定玩家生命值為1（防止死亡）
            player.setHealth(1.0);

            // 啟動瀕死狀態
            bleedingManager.startBleeding(player);
        }
    }

    /**
     * 監聽玩家右鍵點選其他玩家的事件，嘗試救援
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        // 檢查是否是點選玩家
        if (!(event.getRightClicked() instanceof Player target)) {
            return;
        }

        Player rescuer = event.getPlayer();

        // 檢查是否啟用右鍵救援
        boolean rightClickEnabled = plugin.getConfig().getBoolean("bleeding.rescue-methods.right-click", true);
        
        // 如果右鍵救援被禁用，則直接返回
        if (!rightClickEnabled) {
            return;
        }

        // 嘗試救援目標玩家
        if (bleedingManager.isPlayerBleeding(target.getUniqueId())) {
            event.setCancelled(true);
            bleedingManager.attemptRescue(rescuer, target);
        }
    }

    /**
     * 監聽玩家左鍵點擊其他玩家的事件，嘗試救援
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerAttackEntity(EntityDamageByEntityEvent event) {
        // 檢查是否是攻擊玩家，並且攻擊者也是玩家
        if (!(event.getEntity() instanceof Player target) || !(event.getDamager() instanceof Player rescuer)) {
            return;
        }
        
        // 檢查是否啟用左鍵救援
        boolean leftClickEnabled = plugin.getConfig().getBoolean("bleeding.rescue-methods.left-click", true);
        
        // 如果左鍵救援被禁用，則直接返回
        if (!leftClickEnabled) {
            return;
        }
        
        // 如果目標玩家處於瀕死狀態，取消傷害並嘗試救援
        if (bleedingManager.isPlayerBleeding(target.getUniqueId())) {
            event.setCancelled(true);
            bleedingManager.attemptRescue(rescuer, target);
        }
    }

    /**
     * 監聽玩家退出遊戲事件，處理瀕死玩家的退出
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // 如果退出的玩家正處於瀕死狀態，直接處理死亡
        if (bleedingManager.isPlayerBleeding(player.getUniqueId())) {
            bleedingManager.endBleeding(player.getUniqueId(), false);
        }
    }

    /**
     * 最低優先順序監聽玩家死亡事件，以確保我們的插件已經處理了瀕死邏輯
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        // 玩家死亡後，確保移除瀕死狀態（防止狀態殘留）
        Player player = event.getEntity();
        
        // 如果有處理死亡標記，跳過處理以避免無限遞迴
        if (player.hasMetadata("processingDeath")) {
            return;
        }
        
        if (bleedingManager.isPlayerBleeding(player.getUniqueId())) {
            // 設置正在處理死亡的標記
            player.setMetadata("processingDeath", new FixedMetadataValue(plugin, true));
            
            try {
                // 從瀕死管理器中移除玩家（不嘗試殺死玩家，因為玩家已經死了）
                bleedingManager.removeBleedingState(player.getUniqueId());
            } finally {
                // 確保一定會移除標記
                player.removeMetadata("processingDeath", plugin);
            }
        }
    }
    
    /**
     * 監聽玩家恢復生命值的事件，防止瀕死玩家自動回血
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerRegainHealth(EntityRegainHealthEvent event) {
        Entity entity = event.getEntity();
        
        // 僅處理玩家實體
        if (!(entity instanceof Player player)) {
            return;
        }
        
        // 如果玩家正處於瀕死狀態，取消任何生命值恢復
        if (bleedingManager.isPlayerBleeding(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
