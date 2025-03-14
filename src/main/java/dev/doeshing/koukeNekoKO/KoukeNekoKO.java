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
        // 若 config.yml 尚未存在则複製預設配置
        saveDefaultConfig();
        
        // 初始化濒死管理器
        bleedingManager = new BleedingManager(this);
        
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new BleedingListener(this, bleedingManager), this);

        // 使用 CommandSystem 註冊 reload 指令
        CommandSystem commandSystem = new CommandSystem(this);
        commandSystem.registerCommand("koukenekokoreload", new ReloadCommand(this), "koukeneko.ko.admin", "重載插件設定", "/koukenekokoreload", "knkoreload");
        
        // 输出启动信息
        getLogger().info("KoukeNeko-KO 插件已启动！");
        getLogger().info("瀕死持續時間: " + getConfig().getInt("bleeding.duration", 60) + " 秒");
        getLogger().info("救援點擊次數: " + getConfig().getInt("bleeding.revival-clicks", 5) + " 次");
    }

    @Override
    public void onDisable() {
        // 清理所有濒死玩家状态
        if (bleedingManager != null) {
            bleedingManager.clearAllBleedingPlayers();
        }
        
        // 输出关闭信息
        getLogger().info("KoukeNeko-KO 插件已关闭！");
    }
    
    /**
     * 获取濒死管理器实例
     * @return 濒死管理器
     */
    public BleedingManager getBleedingManager() {
        return bleedingManager;
    }
}
