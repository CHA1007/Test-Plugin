package org.chadate.testPlugin.gui;


import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.chadate.testPlugin.TestPlugin;
import org.chadate.testPlugin.manager.MailManager;
import org.chadate.testPlugin.model.Mail;

import java.util.*;

public class MailGUI implements Listener {

    private final MailManager mailManager;

    private final Map<UUID, String> sendingMails = new HashMap<>();

    private final Map<UUID, String> mailMessages = new HashMap<>();

    private final Map<UUID, String> adminViewing = new HashMap<>();

    public MailGUI(TestPlugin plugin) {
        this.mailManager = plugin.getMailManager();
    }

    public void openMailbox(Player player) {
        List<Mail> mails = mailManager.getPlayerMails(player.getName());

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("我的邮箱", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));

        int slot = 0;
        for (Mail mail : mails) {
            List<ItemStack> items = mail.getItems();
            for (int i = 0; i < items.size() && slot < 54; i++) {
                ItemStack item = items.get(i);
                if (item != null && item.getType() != Material.AIR) {
                    ItemStack displayItem = createItemDisplayForMail(item, mail, i, false);
                    inv.setItem(slot, displayItem);
                    slot++;
                }
            }
        }

        // 播放粒子效果
        player.spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 2, 0), 30, 0.5, 0.5, 0.5, 0.1);

        player.openInventory(inv);
    }

    public void openSendMailGUI(Player sender, String recipientName, String message) {
        sendingMails.put(sender.getUniqueId(), recipientName);
        if (message != null && !message.isEmpty()) {
            mailMessages.put(sender.getUniqueId(), message);
        }

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("发送邮件给: " + recipientName, NamedTextColor.GREEN, TextDecoration.BOLD));

        sender.openInventory(inv);
    }

    public void openAdminMailGUI(Player admin, String targetPlayerName) {
        if (!admin.hasPermission("item mail.admin")) {
            admin.sendMessage(Component.text("你没有权限使用此功能！", NamedTextColor.RED));
            return;
        }

        adminViewing.put(admin.getUniqueId(), targetPlayerName);
        List<Mail> mails = mailManager.getPlayerMails(targetPlayerName);

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("管理: " + targetPlayerName + " 的邮箱", NamedTextColor.RED, TextDecoration.BOLD));

        int slot = 0;
        for (Mail mail : mails) {
            List<ItemStack> items = mail.getItems();
            for (int i = 0; i < items.size() && slot < 54; i++) {
                ItemStack item = items.get(i);
                if (item != null && item.getType() != Material.AIR) {
                    ItemStack displayItem = createItemDisplayForMail(item, mail, i, true);
                    inv.setItem(slot, displayItem);
                    slot++;
                }
            }
        }

        admin.openInventory(inv);
    }

    private ItemStack createItemDisplayForMail(ItemStack originalItem, Mail mail, int itemIndex, boolean isAdmin) {
        ItemStack displayItem = originalItem.clone();

        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {

            // 获取原有lore或创建新的
            List<Component> lore = meta.hasLore() ? new ArrayList<>(Objects.requireNonNull(meta.lore())) : new ArrayList<>();

            // 添加邮件信息 - 简洁版
            lore.add(Component.empty());

            // 发送者信息
            lore.add(Component.text("✉ ", NamedTextColor.AQUA)
                    .append(Component.text("发件人: ", NamedTextColor.GRAY))
                    .append(Component.text(mail.getSender(), NamedTextColor.GOLD)
                            .decoration(TextDecoration.BOLD, true)));

            // 时间信息
            lore.add(Component.text("⏰ ", NamedTextColor.GREEN)
                    .append(Component.text("时间: ", NamedTextColor.GRAY))
                    .append(Component.text(mail.getSendTime(), NamedTextColor.WHITE)));

            // 显示留言
            if (mail.hasMessage()) {
                lore.add(Component.text("✎ ", NamedTextColor.LIGHT_PURPLE)
                        .append(Component.text("留言: ", NamedTextColor.GRAY))
                        .append(Component.text(mail.getMessage(), NamedTextColor.LIGHT_PURPLE)
                                .decoration(TextDecoration.ITALIC, true)));
            }

            lore.add(Component.empty());

            if (isAdmin) {
                lore.add(Component.text("⚡ ", NamedTextColor.GREEN)
                        .append(Component.text("左键", NamedTextColor.WHITE)
                                .decoration(TextDecoration.BOLD, true))
                        .append(Component.text(" → 领取副本", NamedTextColor.GREEN)));
                lore.add(Component.text("✖ ", NamedTextColor.RED)
                        .append(Component.text("Shift+左键", NamedTextColor.WHITE)
                                .decoration(TextDecoration.BOLD, true))
                        .append(Component.text(" → 删除物品", NamedTextColor.RED)));
            } else {
                lore.add(Component.text("★ ", NamedTextColor.YELLOW)
                        .append(Component.text("左键", NamedTextColor.WHITE)
                                .decoration(TextDecoration.BOLD, true))
                        .append(Component.text(" → 领取物品", NamedTextColor.YELLOW)));
                lore.add(Component.text("✦ ", NamedTextColor.AQUA)
                        .append(Component.text("Shift+左键", NamedTextColor.WHITE)
                                .decoration(TextDecoration.BOLD, true))
                        .append(Component.text(" → 领取全部", NamedTextColor.AQUA)));
            }

            meta.lore(lore);

            NamespacedKey mailIdKey = new NamespacedKey(mailManager.getPlugin(), "mail_id");
            NamespacedKey itemIndexKey = new NamespacedKey(mailManager.getPlugin(), "item_index");
            meta.getPersistentDataContainer().set(mailIdKey, PersistentDataType.STRING, mail.getId());
            meta.getPersistentDataContainer().set(itemIndexKey, PersistentDataType.INTEGER, itemIndex);

            displayItem.setItemMeta(meta);
        }

        return displayItem;
    }

    private void giveItemsToPlayer(Player player, List<ItemStack> items) {
        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                HashMap<Integer, ItemStack> notAdded = player.getInventory().addItem(item);
                if (!notAdded.isEmpty()) {
                    player.sendMessage(Component.text("背包已满！部分物品掉落在地上", NamedTextColor.RED));
                    for (ItemStack drop : notAdded.values()) {
                        player.getWorld().dropItem(player.getLocation(), drop);
                    }
                }
            }
        }
    }

    private String extractMailId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }

        NamespacedKey mailIdKey = new NamespacedKey(mailManager.getPlugin(), "mail_id");
        return meta.getPersistentDataContainer().get(mailIdKey, PersistentDataType.STRING);
    }

    private int extractItemIndex(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return -1;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return -1;
        }

        NamespacedKey itemIndexKey = new NamespacedKey(mailManager.getPlugin(), "item_index");
        Integer index = meta.getPersistentDataContainer().get(itemIndexKey, PersistentDataType.INTEGER);
        return index != null ? index : -1;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = event.getView().title().toString();

        // 处理邮箱界面点击
        if (title.contains("我的邮箱")) {
            event.setCancelled(true);
            handleMailboxClick(player, event);
        }
        // 处理管理员界面点击
        else if (title.contains("管理:") && title.contains("的邮箱")) {
            event.setCancelled(true);
            handleAdminClick(player, event);
        }
    }

    private void handleMailboxClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        String mailId = extractMailId(clicked);
        int itemIndex = extractItemIndex(clicked);

        if (mailId == null || itemIndex < 0) {
            return;
        }

        Mail targetMail = mailManager.getMail(player.getName(), mailId);
        if (targetMail == null) {
            return;
        }

        if (event.getClick() == ClickType.SHIFT_LEFT) {
            List<ItemStack> items = mailManager.claimMail(player.getName(), mailId);
            if (items != null) {
                giveItemsToPlayer(player, items);

                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.0f);
                player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5, 0.1);
                player.sendMessage(
                        Component.text("✓ 已领取来自 " + targetMail.getSender() + " 的整封邮件", NamedTextColor.GREEN));

                refreshMailboxGUI(player, event.getInventory());
            }
        }
        else if (event.getClick() == ClickType.LEFT) {
            ItemStack removedItem = mailManager.removeItemFromMail(player.getName(), mailId, itemIndex);

            if (removedItem != null) {

                ItemStack cleanItem = removedItem.clone();
                ItemMeta meta = cleanItem.getItemMeta();
                if (meta != null && meta.hasLore()) {
                    List<Component> lore = new ArrayList<>(Objects.requireNonNull(meta.lore()));

                    int separatorIndex = -1;
                    for (int i = 0; i < lore.size(); i++) {
                        String text = ((net.kyori.adventure.text.TextComponent) lore.get(i)).content();
                        if (text.contains("━")) {
                            separatorIndex = i;
                            break;
                        }
                    }
                    if (separatorIndex >= 0) {
                        lore = lore.subList(0, separatorIndex);
                        if (lore.isEmpty()) {
                            meta.lore(null);
                        } else {
                            meta.lore(lore);
                        }
                    }
                    cleanItem.setItemMeta(meta);
                }

                giveItemsToPlayer(player, List.of(cleanItem));

                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.0f);
                player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
                player.sendMessage(Component.text("✓ 已领取物品", NamedTextColor.GREEN));

                refreshMailboxGUI(player, event.getInventory());
            } else {
                player.sendMessage(Component.text("✗ 领取失败，请重试", NamedTextColor.RED));
            }
        }
    }

    private void handleAdminClick(Player admin, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        String targetPlayer = adminViewing.get(admin.getUniqueId());
        if (targetPlayer == null) {
            return;
        }

        String mailId = extractMailId(clicked);
        int itemIndex = extractItemIndex(clicked);

        if (mailId == null || itemIndex < 0) {
            return;
        }

        Mail targetMail = mailManager.getMail(targetPlayer, mailId);
        if (targetMail == null) {
            return;
        }

        if (event.getClick() == ClickType.SHIFT_LEFT) {
            ItemStack removedItem = mailManager.removeItemFromMail(targetPlayer, mailId, itemIndex);
            if (removedItem != null) {
                admin.sendMessage(Component.text("✓ 已删除物品", NamedTextColor.GREEN));
                refreshAdminMailGUI(admin, targetPlayer, event.getInventory());
            } else {
                admin.sendMessage(Component.text("✗ 删除失败", NamedTextColor.RED));
            }
        }
        else if (event.getClick() == ClickType.LEFT) {
            List<ItemStack> items = targetMail.getItems();
            if (itemIndex < items.size()) {
                ItemStack item = items.get(itemIndex);
                if (item != null && item.getType() != Material.AIR) {
                    ItemStack cleanItem = item.clone();
                    ItemMeta meta = cleanItem.getItemMeta();
                    if (meta != null && meta.hasLore()) {
                        List<Component> lore = new ArrayList<>(Objects.requireNonNull(meta.lore()));
                        int separatorIndex = -1;
                        for (int i = 0; i < lore.size(); i++) {
                            String text = ((net.kyori.adventure.text.TextComponent) lore.get(i)).content();
                            if (text.contains("━")) {
                                separatorIndex = i;
                                break;
                            }
                        }
                        if (separatorIndex >= 0) {
                            lore = lore.subList(0, separatorIndex);
                            if (lore.isEmpty()) {
                                meta.lore(null);
                            } else {
                                meta.lore(lore);
                            }
                        }
                        cleanItem.setItemMeta(meta);
                    }

                    admin.getInventory().addItem(cleanItem);
                    admin.sendMessage(Component.text("✓ 已领取物品副本", NamedTextColor.GREEN));
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        UUID playerId = player.getUniqueId();
        String title = event.getView().title().toString();

        if (title.contains("发送邮件给:")) {
            String recipient = sendingMails.remove(playerId);
            String message = mailMessages.remove(playerId);
            if (recipient != null) {
                handleSendMail(player, recipient, event.getInventory(), message);
            }
        }

        if (title.contains("管理:")) {
            adminViewing.remove(playerId);
        }
    }

    private void refreshMailboxGUI(Player player, Inventory inv) {
        inv.clear();

        List<Mail> mails = mailManager.getPlayerMails(player.getName());
        int slot = 0;
        for (Mail mail : mails) {
            List<ItemStack> items = mail.getItems();
            for (int i = 0; i < items.size() && slot < 54; i++) {
                ItemStack item = items.get(i);
                if (item != null && item.getType() != Material.AIR) {
                    ItemStack displayItem = createItemDisplayForMail(item, mail, i, false);
                    inv.setItem(slot, displayItem);
                    slot++;
                }
            }
        }
    }

    private void refreshAdminMailGUI(Player admin, String targetPlayerName, Inventory inv) {
        inv.clear();

        List<Mail> mails = mailManager.getPlayerMails(targetPlayerName);
        int slot = 0;
        for (Mail mail : mails) {
            List<ItemStack> items = mail.getItems();
            for (int i = 0; i < items.size() && slot < 54; i++) {
                ItemStack item = items.get(i);
                if (item != null && item.getType() != Material.AIR) {
                    ItemStack displayItem = createItemDisplayForMail(item, mail, i, true);
                    inv.setItem(slot, displayItem);
                    slot++;
                }
            }
        }
    }

    private void handleSendMail(Player sender, String recipient, Inventory inv, String message) {
        List<ItemStack> items = new ArrayList<>();

        for (ItemStack item : inv.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                items.add(item.clone());
            }
        }

        if (items.isEmpty()) {
            sender.sendMessage(Component.text("✗ 未放置任何物品，邮件未发送", NamedTextColor.RED));
            return;
        }

        mailManager.sendMail(sender.getName(), recipient, items, message);

        sender.playSound(sender.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
        sender.spawnParticle(Particle.END_ROD, sender.getLocation().add(0, 1.5, 0), 20, 0.3, 0.5, 0.3, 0.05);
        sender.sendMessage(Component.text("✓ 邮件已成功发送给 " + recipient, NamedTextColor.GREEN));

        Player recipientPlayer = Bukkit.getPlayer(recipient);
        if (recipientPlayer != null && recipientPlayer.isOnline()) {
            recipientPlayer.playSound(recipientPlayer.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
            recipientPlayer.sendMessage(Component.text("✉ 你收到了来自 " + sender.getName() + " 的新邮件！", NamedTextColor.GOLD));
        }
    }
}
