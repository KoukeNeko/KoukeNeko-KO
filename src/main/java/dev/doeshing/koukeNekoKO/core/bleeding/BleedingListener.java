package dev.doeshing.koukeNekoKO.core.bleeding;

import dev.doeshing.koukeNekoKO.KoukeNekoKO;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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

        // 嘗試救援目標玩家
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
        if (bleedingManager.isPlayerBleeding(player.getUniqueId())) {
            bleedingManager.endBleeding(player.getUniqueId(), false);
        }
    }
}
