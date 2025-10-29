package org.chadate.testPlugin.manager;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.chadate.testPlugin.TestPlugin;
import org.chadate.testPlugin.model.Mail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MailManager {
    private final TestPlugin plugin;
    private final File mailFolder;
    private final Map<String, List<Mail>> mailCache;

    public MailManager(TestPlugin plugin){
        this.plugin = plugin;
        this.mailFolder = new File(plugin.getDataFolder(), "mails");
        this.mailCache = new HashMap<>();

        if (!mailFolder.exists()) {
            if (!mailFolder.mkdirs()){
                plugin.getLogger().severe("无法创建邮件文件夹: " + mailFolder.getAbsolutePath());
            }
        }
    }

    public void sendMail(String sender, String recipient, List<ItemStack> items, String message) {
        if (items == null || items.isEmpty()) {
            return;
        }

        // 创建邮件
        Mail mail = new Mail(sender, recipient, items, message);

        // 添加到缓存
        mailCache.computeIfAbsent(recipient.toLowerCase(), k -> new ArrayList<>()).add(mail);

        // 保存到文件
        savePlayerMails(recipient);
    }

    public List<Mail> getPlayerMails(String playerName) {
        String name = playerName.toLowerCase();

        // 如果缓存中没有，从文件加载
        if (!mailCache.containsKey(name)) {
            loadPlayerMails(name);
        }

        return new ArrayList<>(mailCache.getOrDefault(name, new ArrayList<>()));
    }

    public List<ItemStack> claimMail(String playerName, String mailId) {
        String name = playerName.toLowerCase();
        List<Mail> mails = mailCache.get(name);

        if (mails == null) {
            return null;
        }

        Mail targetMail = null;
        for (Mail mail : mails) {
            if (mail.getId().equals(mailId)) {
                targetMail = mail;
                break;
            }
        }

        if (targetMail == null) {
            return null;
        }

        // 获取物品
        List<ItemStack> items = targetMail.getItems();

        // 删除邮件
        mails.remove(targetMail);
        savePlayerMails(playerName);

        return items;
    }

    private void loadPlayerMails(String playerName) {
        String name = playerName.toLowerCase();
        File file = new File(mailFolder, name + ".yml");

        if (!file.exists()) {
            mailCache.put(name, new ArrayList<>());
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<?> mailList = config.getList("mails");

        if (mailList == null || mailList.isEmpty()) {
            mailCache.put(name, new ArrayList<>());
            return;
        }

        List<Mail> mails = new ArrayList<>();
        for (Object obj : mailList) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mailData = (Map<String, Object>) obj;
                mails.add(new Mail(mailData));
            }
        }

        mailCache.put(name, mails);
    }

    private void savePlayerMails(String playerName) {
        String name = playerName.toLowerCase();
        File file = new File(mailFolder, name + ".yml");

        List<Mail> mails = mailCache.get(name);
        if (mails == null || mails.isEmpty()) {
            // 如果没有邮件，删除文件
            if (file.exists()) {
                if (!file.delete()){
                    plugin.getLogger().severe("无法删除玩家邮件文件: " + file.getAbsolutePath());
                }
            }
            return;
        }

        YamlConfiguration config = new YamlConfiguration();
        List<Map<String, Object>> serializedMails = mails.stream()
                .map(Mail::serialize)
                .collect(Collectors.toList());

        config.set("mails", serializedMails);

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("无法保存玩家邮件: " + playerName);
        }
    }

    public void saveAll() {
        for (String playerName : mailCache.keySet()) {
            savePlayerMails(playerName);
        }
    }

    public int getMailCount(String playerName) {
        return getPlayerMails(playerName).size();
    }

    public ItemStack removeItemFromMail(String playerName, String mailId, int itemIndex) {
        String name = playerName.toLowerCase();
        List<Mail> mails = mailCache.get(name);

        if (mails == null) {
            return null;
        }

        // 查找目标邮件
        Mail targetMail = null;
        for (Mail mail : mails) {
            if (mail.getId().equals(mailId)) {
                targetMail = mail;
                break;
            }
        }

        if (targetMail == null) {
            return null;
        }

        // 直接从 Mail 对象中移除物品
        ItemStack removedItem = targetMail.removeItem(itemIndex);

        if (removedItem == null) {
            return null;
        }

        // 如果邮件没有物品了，删除整封邮件
        if (targetMail.isEmpty()) {
            mails.remove(targetMail);
        }

        // 保存更改
        savePlayerMails(playerName);

        return removedItem;
    }

    public Mail getMail(String playerName, String mailId) {
        List<Mail> mails = getPlayerMails(playerName);
        for (Mail mail : mails) {
            if (mail.getId().equals(mailId)) {
                return mail;
            }
        }
        return null;
    }

    public TestPlugin getPlugin() {
        return plugin;
    }
}
