package dev.doeshing.koukeNekoKO.commands;

import dev.doeshing.koukeNekoKO.KoukeNekoKO;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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

        // 使用 Adventure API 發送訊息
        String prefix = plugin.getConfig().getString("prefix", "&8[&6KoukeNeko-KO&8] &r");
        Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + "&a設定文件已重新載入！");
        sender.sendMessage(message);
        return true;
    }
}
