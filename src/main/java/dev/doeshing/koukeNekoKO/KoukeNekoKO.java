package dev.doeshing.koukeNekoKO;

import dev.doeshing.koukeNekoKO.commands.ReloadCommand;
import dev.doeshing.koukeNekoKO.core.CommandSystem;
import dev.doeshing.koukeNekoKO.core.bleeding.BleedingListener;
import dev.doeshing.koukeNekoKO.core.bleeding.BleedingManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class KoukeNekoKO extends JavaPlugin {
    private BleedingManager bleedingManager;

    @Override
    public void onEnable() {
        // 若 config.yml 尚未存在則複製預設配置
        saveDefaultConfig();

        // 初始化瀕死管理器
        bleedingManager = new BleedingManager(this);

        // 註冊事件監聽器
        getServer().getPluginManager().registerEvents(new BleedingListener(this, bleedingManager), this);

        // 使用 CommandSystem 註冊 reload 指令
        CommandSystem commandSystem = new CommandSystem(this);
        commandSystem.registerCommand("koukenekokoreload", new ReloadCommand(this), "koukeneko.ko.admin", "過載外掛設定", "/koukenekokoreload", "knkoreload");

        // 輸出啟動資訊
        getLogger().info("KoukeNeko-KO 外掛已啟動！");
        getLogger().info("瀕死持續時間: " + getConfig().getInt("bleeding.duration", 60) + " 秒");
        getLogger().info("救援點選次數: " + getConfig().getInt("bleeding.revival-clicks", 5) + " 次");
    }

    @Override
    public void onDisable() {
        // 清理所有瀕死玩家狀態
        if (bleedingManager != null) {
            bleedingManager.clearAllBleedingPlayers();
        }

        // 輸出關閉資訊
        getLogger().info("KoukeNeko-KO 外掛已關閉！");
    }

    /**
     * 獲取瀕死管理器例項
     * @return 瀕死管理器
     */
    public BleedingManager getBleedingManager() {
        return bleedingManager;
    }
}
