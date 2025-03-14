package dev.doeshing.koukeNekoKO.commands;

import dev.doeshing.koukeNekoKO.KoukeNekoKO;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadCommand implements CommandExecutor {

    private final KoukeNekoKO plugin;

    public ReloadCommand(KoukeNekoKO plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        // 重新載入配置文件
        plugin.reloadConfig();

        // 重新插件讀取後的操作
        // plugin.reloadConfig();

        sender.sendMessage("§a設定文件已重新載入！");
        return true;
    }
}

