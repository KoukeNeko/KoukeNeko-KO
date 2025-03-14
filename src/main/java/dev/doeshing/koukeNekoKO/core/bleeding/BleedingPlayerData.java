package dev.doeshing.koukeNekoKO.core.bleeding;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/**
 * 儲存瀕死玩家的資料
 */
public class BleedingPlayerData {
    private final Location location; // 玩家瀕死發生的原始位置
    private Location originalLocation; // 儲存完整的原始位置和視角
    private final int totalClicksNeeded;
    private int remainingClicks;
    private BukkitTask timerTask;
    private UUID lastRescuer;

    /**
     * 建立一個瀕死玩家資料
     * @param location 玩家瀕死位置
     * @param clicksNeeded 救援所需的點選次數
     */
    public BleedingPlayerData(Location location, int clicksNeeded) {
        this.location = location;
        this.totalClicksNeeded = clicksNeeded;
        this.remainingClicks = clicksNeeded;
    }

    /**
     * 獲取玩家瀕死位置
     * @return 瀕死位置
     */
    public Location getLocation() {
        return location;
    }

    /**
     * 獲取總共需要的點選次數
     * @return 總點選次數
     */
    public int getTotalClicksNeeded() {
        return totalClicksNeeded;
    }

    /**
     * 獲取剩餘需要的點選次數
     * @return 剩餘點選次數
     */
    public int getRemainingClicks() {
        return remainingClicks;
    }

    /**
     * 減少剩餘點選次數並回傳新值
     * @return 減少後的剩餘點選次數
     */
    public int decrementRemainingClicks() {
        return --remainingClicks;
    }

    /**
     * 獲取瀕死計時任務
     * @return 計時任務
     */
    public BukkitTask getTimerTask() {
        return timerTask;
    }

    /**
     * 設定瀕死計時任務
     * @param timerTask 計時任務
     */
    public void setTimerTask(BukkitTask timerTask) {
        this.timerTask = timerTask;
    }

    /**
     * 獲取最後一個救援者的UUID
     * @return 最後救援者UUID
     */
    public UUID getLastRescuer() {
        return lastRescuer;
    }

    /**
     * 設定最後一個救援者的UUID
     * @param lastRescuer 救援者UUID
     */
    public void setLastRescuer(UUID lastRescuer) {
        this.lastRescuer = lastRescuer;
    }

    /**
     * 獲取玩家原始位置和視角
     * @return 原始位置和視角
     */
    public Location getOriginalLocation() {
        return originalLocation;
    }

    /**
     * 設定玩家原始位置和視角
     * @param originalLocation 原始位置和視角
     */
    public void setOriginalLocation(Location originalLocation) {
        this.originalLocation = originalLocation;
    }
}
