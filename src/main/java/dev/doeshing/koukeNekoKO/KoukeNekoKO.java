package dev.doeshing.koukeNekoKO;

import dev.doeshing.koukeNekoKO.core.CommandSystem;
import org.bukkit.plugin.java.JavaPlugin;

public final class KoukeNekoKO extends JavaPlugin {

    private CommandSystem commandSystem;

    @Override
    public void onEnable() {

        // 使用 CommandSystem 註冊 reload 指令
        commandSystem = new CommandSystem(this);
        commandSystem.registerCommand("koukenekokoreload", new dev.doeshing.koukeNekoKO.commands.ReloadCommand(this), "koukeneko.ko.admin", "重載插件設定", "/koukenekokoreload", "knkoreload");
        // 若 config.yml 尚未存在則複製預設配置
        saveDefaultConfig();

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
