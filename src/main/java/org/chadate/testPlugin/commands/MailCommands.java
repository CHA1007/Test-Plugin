package org.chadate.testPlugin.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.chadate.testPlugin.TestPlugin;
import org.chadate.testPlugin.gui.MailGUI;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MailCommands implements CommandExecutor, TabCompleter {

    private final TestPlugin plugin;
    private final MailGUI mailGUI;

    public MailCommands(TestPlugin plugin) {
        this.plugin = plugin;
        this.mailGUI = plugin.getMailGUI();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("此命令只能由玩家执行！", NamedTextColor.RED));
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "mailbox" -> handleMailboxCommand(player);
            case "sendmail" -> handleSendMailCommand(player, args);
            case "adminmail" -> handleAdminMailCommand(player, args);
        }

        return true;
    }

    private void handleMailboxCommand(Player player) {
        int mailCount = plugin.getMailManager().getMailCount(player.getName());

        if (mailCount == 0) {
            player.sendMessage(Component.text("你当前没有邮件", NamedTextColor.YELLOW));
            return;
        }

        mailGUI.openMailbox(player);
    }

    private void handleSendMailCommand(Player sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(Component.text("用法: /sendmail <玩家名> [留言]", NamedTextColor.RED));
            return;
        }

        String recipientName = args[0];

        if (recipientName.equalsIgnoreCase(sender.getName())) {
            sender.sendMessage(Component.text("不能给自己发送邮件！", NamedTextColor.RED));
            return;
        }

        String message = null;
        if (args.length > 1) {
            message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        }

        mailGUI.openSendMailGUI(sender, recipientName, message);
    }

    private void handleAdminMailCommand(Player admin, String[] args) {
        if (!admin.hasPermission("testplugin.admin")) {
            admin.sendMessage(Component.text("你没有权限使用此命令！", NamedTextColor.RED));
            return;
        }

        if (args.length < 1) {
            admin.sendMessage(Component.text("用法: /adminmail <玩家名>", NamedTextColor.RED));
            return;
        }

        String targetPlayer = args[0];
        int mailCount = plugin.getMailManager().getMailCount(targetPlayer);

        if (mailCount == 0) {
            admin.sendMessage(Component.text("玩家 " + targetPlayer + " 没有邮件", NamedTextColor.YELLOW));
            return;
        }

        mailGUI.openAdminMailGUI(admin, targetPlayer);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {

        List<String> completions = new ArrayList<>();

        if ((command.getName().equalsIgnoreCase("sendmail") ||
                command.getName().equalsIgnoreCase("adminmail")) && args.length == 1) {

            String input = args[0].toLowerCase();
            completions = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}
