package org.chadate.testPlugin.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
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

    private final Map<UUID, Integer> mailboxPage = new HashMap<>();
    private final Map<UUID, Integer> adminMailPage = new HashMap<>();

    private static final int ITEMS_PER_PAGE = 45;

    public MailGUI(TestPlugin plugin) {
        this.mailManager = plugin.getMailManager();
    }

    public void openMailbox(Player player) {
        mailboxPage.putIfAbsent(player.getUniqueId(), 0);
        
        List<ItemStack> allItems = buildMailItemList(player.getName(), false);

        int totalPages = (int) Math.ceil((double) allItems.size() / ITEMS_PER_PAGE);
        if (totalPages == 0)
            totalPages = 1;
        int page = mailboxPage.get(player.getUniqueId());
        if (page >= totalPages) {
            page = totalPages - 1;
            mailboxPage.put(player.getUniqueId(), page);
        }

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("我的邮箱 [" + (page + 1) + "/" + totalPages + "]", NamedTextColor.DARK_PURPLE,
                        TextDecoration.BOLD));

        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            inv.setItem(i - startIndex, allItems.get(i));
        }

        addPageNavigetionButtons(inv, page, totalPages);

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
        if (!admin.hasPermission("testplugin.admin")) {
            admin.sendMessage(Component.text("你没有权限使用此功能！", NamedTextColor.RED));
            return;
        }

        adminViewing.put(admin.getUniqueId(), targetPlayerName);
        adminMailPage.putIfAbsent(admin.getUniqueId(), 0);

        List<ItemStack> allDisplayItems = buildMailItemList(targetPlayerName, true);

        int totalPages = Math.max(1, (int) Math.ceil((double) allDisplayItems.size() / ITEMS_PER_PAGE));
        int currentPage = adminMailPage.get(admin.getUniqueId());

        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1);
            adminMailPage.put(admin.getUniqueId(), currentPage);
        }

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text("管理: " + targetPlayerName + " 的邮箱 [" + (currentPage + 1) + "/" + totalPages + "]",
                        NamedTextColor.RED, TextDecoration.BOLD));

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allDisplayItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            inv.setItem(i - startIndex, allDisplayItems.get(i));
        }

        addPageNavigetionButtons(inv, currentPage, totalPages);

        admin.openInventory(inv);
    }

    private ItemStack createItemDisplayForMail(ItemStack originalItem, Mail mail, int itemIndex, boolean isAdmin) {
        ItemStack displayItem = originalItem.clone();

        ItemMeta meta = displayItem.getItemMeta();
        if (meta != null) {

            List<Component> lore = meta.hasLore() ? new ArrayList<>(Objects.requireNonNull(meta.lore()))
                    : new ArrayList<>();

            lore.add(Component.empty());

            lore.add(Component.text("✉ ", NamedTextColor.AQUA)
                    .append(Component.text("发件人: ", NamedTextColor.GRAY))
                    .append(Component.text(mail.getSender(), NamedTextColor.GOLD)
                            .decoration(TextDecoration.BOLD, true)));

            lore.add(Component.text("⏰ ", NamedTextColor.GREEN)
                    .append(Component.text("时间: ", NamedTextColor.GRAY))
                    .append(Component.text(mail.getSendTime(), NamedTextColor.WHITE)));

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
        boolean hasShowMessage = false;
        for (ItemStack item : items) {
            if (item != null && item.getType() != Material.AIR) {
                HashMap<Integer, ItemStack> notAdded = player.getInventory().addItem(item);
                if (!notAdded.isEmpty()) {
                    if (!hasShowMessage) {
                        player.sendMessage(Component.text("背包已满！部分物品掉落在地上", NamedTextColor.RED));
                        hasShowMessage = true;
                    }
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

        if (title.contains("我的邮箱")) {
            event.setCancelled(true);
            handleMailboxClick(player, event);
        } else if (title.contains("管理:") && title.contains("的邮箱")) {
            event.setCancelled(true);
            handleAdminClick(player, event);
        }
    }

    private void handleMailboxClick(Player player, InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        int clickedSlot = event.getSlot();

        if (clickedSlot == 45) {
            int currentPage = mailboxPage.getOrDefault(player.getUniqueId(), 0);
            if (currentPage > 0) {
                mailboxPage.put(player.getUniqueId(), currentPage - 1);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                openMailbox(player);
            }
            return;
        }

        if (clickedSlot == 53) {
            List<Mail> mails = mailManager.getPlayerMails(player.getName());
            int totalItems = 0;
            for (Mail mail : mails) {
                for (ItemStack item : mail.getItems()) {
                    if (item != null && item.getType() != Material.AIR) {
                        totalItems++;
                    }
                }
            }
            int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE));
            int currentPage = mailboxPage.getOrDefault(player.getUniqueId(), 0);

            if (currentPage < totalPages - 1) {
                mailboxPage.put(player.getUniqueId(), currentPage + 1);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                openMailbox(player);
            }
            return;
        }

        if (clickedSlot == 49) {
            List<Mail> allMails = mailManager.getPlayerMails(player.getName());
            if (allMails.isEmpty()) {
                player.sendMessage(Component.text("✗ 没有可领取的邮件", NamedTextColor.RED));
                return;
            }

            int totalClaimedItems = 0;
            List<ItemStack> allItems = new ArrayList<>();
            
            for (Mail mail : allMails) {
                List<ItemStack> items = mailManager.claimMail(player.getName(), mail.getId());
                if (items != null && !items.isEmpty()) {
                    allItems.addAll(items);
                    totalClaimedItems += items.size();
                }
            }

            if (totalClaimedItems > 0) {
                giveItemsToPlayer(player, allItems);
                
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
                player.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
                player.sendMessage(Component.text("✓ 已领取所有邮件，共 " + totalClaimedItems + " 个物品", NamedTextColor.GOLD));
                
                refreshMailboxGUI(player, event.getInventory());
            } else {
                player.sendMessage(Component.text("✗ 没有可领取的物品", NamedTextColor.RED));
            }
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
                player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 15, 0.5, 0.5, 0.5,
                        0.1);
                player.sendMessage(
                        Component.text("✓ 已领取来自 " + targetMail.getSender() + " 的整封邮件", NamedTextColor.GREEN));

                refreshMailboxGUI(player, event.getInventory());
            }
        } else if (event.getClick() == ClickType.LEFT) {
            ItemStack removedItem = mailManager.removeItemFromMail(player.getName(), mailId, itemIndex);

            if (removedItem != null) {
                ItemStack cleanItem = cleanItemLore(removedItem);
                giveItemsToPlayer(player, List.of(cleanItem));

                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.0f);
                player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3,
                        0.05);
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

        int clickedSlot = event.getSlot();

        if (clickedSlot == 45) {
            int currentPage = adminMailPage.getOrDefault(admin.getUniqueId(), 0);
            if (currentPage > 0) {
                adminMailPage.put(admin.getUniqueId(), currentPage - 1);
                admin.playSound(admin.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                openAdminMailGUI(admin, targetPlayer);
            }
            return;
        }

        if (clickedSlot == 53) {
            List<Mail> mails = mailManager.getPlayerMails(targetPlayer);
            int totalItems = 0;
            for (Mail mail : mails) {
                for (ItemStack item : mail.getItems()) {
                    if (item != null && item.getType() != Material.AIR) {
                        totalItems++;
                    }
                }
            }
            int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE));
            int currentPage = adminMailPage.getOrDefault(admin.getUniqueId(), 0);

            if (currentPage < totalPages - 1) {
                adminMailPage.put(admin.getUniqueId(), currentPage + 1);
                admin.playSound(admin.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
                openAdminMailGUI(admin, targetPlayer);
            }
            return;
        }

        if (clickedSlot == 49) {
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
        } else if (event.getClick() == ClickType.LEFT) {
            List<ItemStack> items = targetMail.getItems();
            if (itemIndex < items.size()) {
                ItemStack item = items.get(itemIndex);
                if (item != null && item.getType() != Material.AIR) {
                    ItemStack cleanItem = cleanItemLore(item);
                    admin.getInventory().addItem(cleanItem);
                    admin.playSound(admin.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.2f);
                    admin.spawnParticle(Particle.HAPPY_VILLAGER, admin.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.05);
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
            Bukkit.getScheduler().runTaskLater(mailManager.getPlugin(), () -> {
                if (player.getOpenInventory().getTopInventory().getHolder() == null) {
                    String currentTitle = player.getOpenInventory().title().toString();
                    if (!currentTitle.contains("管理:")) {
                        adminViewing.remove(playerId);
                        adminMailPage.remove(playerId);
                    }
                }
            }, 1L);
        }
    }

    private void refreshMailboxGUI(Player player, Inventory inv) {
        inv.clear();

        List<ItemStack> allDisplayItems = buildMailItemList(player.getName(), false);

        int totalPages = Math.max(1, (int) Math.ceil((double) allDisplayItems.size() / ITEMS_PER_PAGE));
        int currentPage = mailboxPage.getOrDefault(player.getUniqueId(), 0);
        
        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1);
            mailboxPage.put(player.getUniqueId(), currentPage);
        }

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allDisplayItems.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            inv.setItem(i - startIndex, allDisplayItems.get(i));
        }

        addPageNavigetionButtons(inv, currentPage, totalPages);
    }

    private void refreshAdminMailGUI(Player admin, String targetPlayerName, Inventory inv) {
        inv.clear();

        List<ItemStack> allDisplayItems = buildMailItemList(targetPlayerName, true);

        int totalPages = Math.max(1, (int) Math.ceil((double) allDisplayItems.size() / ITEMS_PER_PAGE));
        int currentPage = adminMailPage.getOrDefault(admin.getUniqueId(), 0);

        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1);
            adminMailPage.put(admin.getUniqueId(), currentPage);
        }

        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allDisplayItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            inv.setItem(i - startIndex, allDisplayItems.get(i));
        }

        addPageNavigetionButtons(inv, currentPage, totalPages);
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

            Component clickableMessage = Component.text("➤ ", NamedTextColor.GREEN)
                    .append(Component.text("[点击查看邮箱]", NamedTextColor.AQUA)
                            .decoration(TextDecoration.BOLD, true)
                            .clickEvent(ClickEvent.runCommand("/mailbox"))
                            .hoverEvent(HoverEvent.showText(Component.text("点击打开邮箱", NamedTextColor.GREEN))))
                    .append(Component.text(" 或输入 ", NamedTextColor.GRAY))
                    .append(Component.text("/mailbox", NamedTextColor.WHITE));

            recipientPlayer.sendMessage(clickableMessage);
        }
    }

    private void addPageNavigetionButtons(Inventory inv, int mailboxPage, int totalPages) {
        if (mailboxPage > 0) {
            ItemStack prevPage = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevPage.getItemMeta();
            if (prevMeta != null) {
                prevMeta.displayName(Component.text("◀ 上一页", NamedTextColor.YELLOW, TextDecoration.BOLD));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("点击查看第 " + mailboxPage + " 页", NamedTextColor.GRAY));
                prevMeta.lore(lore);
                prevPage.setItemMeta(prevMeta);
            }
            inv.setItem(45, prevPage);
        } else {
            ItemStack disabled = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta disabledMeta = disabled.getItemMeta();
            if (disabledMeta != null) {
                disabledMeta.displayName(Component.text("已是第一页", NamedTextColor.DARK_GRAY));
                disabled.setItemMeta(disabledMeta);
            }
            inv.setItem(45, disabled);
        }

        ItemStack pageInfo = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = pageInfo.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(Component.text("第 " + (mailboxPage + 1) + " / " + totalPages + " 页",
                    NamedTextColor.GOLD, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("点击左右箭头进行翻页", NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("✨ 点击书本 ✨", NamedTextColor.YELLOW, TextDecoration.BOLD));
            lore.add(Component.text("一键领取所有邮件", NamedTextColor.GREEN));
            infoMeta.lore(lore);
            pageInfo.setItemMeta(infoMeta);
        }
        inv.setItem(49, pageInfo);

        if (mailboxPage < totalPages - 1) {
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextPage.getItemMeta();
            if (nextMeta != null) {
                nextMeta.displayName(Component.text("下一页 ▶", NamedTextColor.YELLOW, TextDecoration.BOLD));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("点击查看第 " + (mailboxPage + 2) + " 页", NamedTextColor.GRAY));
                nextMeta.lore(lore);
                nextPage.setItemMeta(nextMeta);
            }
            inv.setItem(53, nextPage);
        } else {
            ItemStack disabled = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta disabledMeta = disabled.getItemMeta();
            if (disabledMeta != null) {
                disabledMeta.displayName(Component.text("已是最后一页", NamedTextColor.DARK_GRAY));
                disabled.setItemMeta(disabledMeta);
            }
            inv.setItem(53, disabled);
        }
    }

    /**
     * 构建邮件物品显示列表（通用方法）
     * @param playerName 玩家名称
     * @param isAdmin 是否为管理员模式
     * @return 显示物品列表
     */
    private List<ItemStack> buildMailItemList(String playerName, boolean isAdmin) {
        List<Mail> mails = mailManager.getPlayerMails(playerName);
        List<ItemStack> allDisplayItems = new ArrayList<>();
        
        for (Mail mail : mails) {
            List<ItemStack> items = mail.getItems();
            for (int i = 0; i < items.size(); i++) {
                ItemStack item = items.get(i);
                if (item != null && item.getType() != Material.AIR) {
                    ItemStack displayItem = createItemDisplayForMail(item, mail, i, isAdmin);
                    allDisplayItems.add(displayItem);
                }
            }
        }
        
        return allDisplayItems;
    }

    /**
     * 清理物品的邮件相关lore（通用方法）
     * @param item 原始物品
     * @return 清理后的物品
     */
    private ItemStack cleanItemLore(ItemStack item) {
        ItemStack cleanItem = item.clone();
        ItemMeta meta = cleanItem.getItemMeta();
        
        if (meta != null && meta.hasLore()) {
            List<Component> lore = new ArrayList<>(Objects.requireNonNull(meta.lore()));
            
            int separatorIndex = -1;
            for (int i = 0; i < lore.size(); i++) {
                String text = ((net.kyori.adventure.text.TextComponent) lore.get(i)).content();
                if (text.isEmpty() && i > 0) {
                    if (i + 1 < lore.size()) {
                        String nextText = ((net.kyori.adventure.text.TextComponent) lore.get(i + 1)).content();
                        if (nextText.contains("✉") || nextText.contains("发件人")) {
                            separatorIndex = i;
                            break;
                        }
                    }
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
        
        return cleanItem;
    }
}
