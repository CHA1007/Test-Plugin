package org.chadate.testPlugin.listeners;


import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.chadate.testPlugin.TestPlugin;
import org.chadate.testPlugin.manager.MailManager;

public class PlayerListener implements Listener {

    private final MailManager mailManager;

    public PlayerListener(TestPlugin plugin) {
        this.mailManager = plugin.getMailManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        player.getServer().getScheduler().runTaskLater(
                mailManager.getPlugin(),
                () -> checkAndNotifyMail(player),
                20L);
    }

    private void checkAndNotifyMail(Player player) {
        int mailCount = mailManager.getMailCount(player.getName());

        if (mailCount > 0) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);

            player.sendMessage(Component.empty());

            player.sendMessage(Component.text("✉ 邮件提醒", NamedTextColor.GOLD)
                    .decoration(TextDecoration.BOLD, true));

            player.sendMessage(Component.text("➤ 你有 ", NamedTextColor.AQUA)
                    .append(Component.text(String.valueOf(mailCount), NamedTextColor.YELLOW)
                            .decoration(TextDecoration.BOLD, true))
                    .append(Component.text(" 封未读邮件！", NamedTextColor.YELLOW)));

            Component clickableMessage = Component.text("➤ ", NamedTextColor.GREEN)
                    .append(Component.text("[点击查看邮箱]", NamedTextColor.AQUA)
                            .decoration(TextDecoration.BOLD, true)
                            .clickEvent(ClickEvent.runCommand("/mailbox"))
                            .hoverEvent(HoverEvent.showText(Component.text("点击打开邮箱", NamedTextColor.GREEN))))
                    .append(Component.text(" 或输入 ", NamedTextColor.GRAY))
                    .append(Component.text("/mailbox", NamedTextColor.WHITE));

            player.sendMessage(clickableMessage);
            player.sendMessage(Component.empty());
        }
    }
}
