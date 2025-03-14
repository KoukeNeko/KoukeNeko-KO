package dev.doeshing.koukeNekoKO.commands;

import dev.doeshing.koukeNekoKO.KoukeNekoKO;
import dev.doeshing.koukeNekoKO.core.bleeding.BleedingPlayerData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
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
            String prefix = plugin.getConfig().getString("prefix", "&8[&6KoukeNeko-KO&8] &r");
            Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + "&c此指令只能由玩家使用！");
            sender.sendMessage(message);
            return true;
        }

        // 檢查玩家是否處於瀕死狀態
        if (!plugin.getBleedingManager().isPlayerBleeding(player.getUniqueId())) {
            String prefix = plugin.getConfig().getString("prefix", "&8[&6KoukeNeko-KO&8] &r");
            Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + "&c你現在沒有處於瀕死狀態！");
            player.sendMessage(message);
            return true;
        }

        // 顯示放棄急救的確認訊息
        String giveUpMessageRaw = plugin.getConfig().getString("bleeding.messages.give-up", "&c你已放棄急救，即將死亡...");
        String prefix = plugin.getConfig().getString("prefix", "&8[&6KoukeNeko-KO&8] &r");
        Component giveUpMessage = LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + giveUpMessageRaw);
        player.sendMessage(giveUpMessage);
        
        // 播放死亡前的音效
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_WITHER_DEATH, 0.5f, 1.5f);
        
        // 先清除瀕死狀態但不觸發死亡事件
        player.setMetadata("processingDeath", new org.bukkit.metadata.FixedMetadataValue(plugin, true));
        
        try {
            // 安全地移除瀕死狀態
            plugin.getBleedingManager().removeBleedingState(player.getUniqueId());
            
            // 然後手動設置死亡 - 這會觸發死亡事件但玩家已經不在瀕死狀態
            player.setHealth(0);
        } finally {
            // 確保移除標記
            player.removeMetadata("processingDeath", plugin);
        }

        return true;
    }
}
