package dev.doeshing.koukeNekoKO.commands;

import dev.doeshing.koukeNekoKO.KoukeNekoKO;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GiveUpCommand implements CommandExecutor {

    private final KoukeNekoKO plugin;

    public GiveUpCommand(KoukeNekoKO plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        // 只有玩家可以使用此指令
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("此指令只能由玩家使用！", NamedTextColor.RED));
            return true;
        }

        // 檢查玩家是否處於瀕死狀態
        if (!plugin.getBleedingManager().isPlayerBleeding(player.getUniqueId())) {
            player.sendMessage(Component.text("你現在沒有處於瀕死狀態！", NamedTextColor.RED));
            return true;
        }

        // 顯示放棄急救的確認訊息
        String giveUpMessageRaw = plugin.getConfig().getString("bleeding.messages.give-up", "&c你已放棄急救，即將死亡...");
        Component giveUpMessage = plugin.getBleedingManager().formatText(giveUpMessageRaw);
        player.sendMessage(giveUpMessage);
        
        // 播放死亡前的音效
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_DEATH, 0.5f, 1.5f);
        
        // 執行結束瀕死狀態（不是被救援）
        plugin.getBleedingManager().endBleeding(player.getUniqueId(), false);

        return true;
    }
}
