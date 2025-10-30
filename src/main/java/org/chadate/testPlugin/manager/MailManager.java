package org.chadate.testPlugin.manager;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.chadate.testPlugin.TestPlugin;
import org.chadate.testPlugin.model.Mail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MailManager {
    private final TestPlugin plugin;
    private final File mailFolder;
    private final Map<String, List<Mail>> mailCache;

    public MailManager(TestPlugin plugin){
        this.plugin = plugin;
        this.mailFolder = new File(plugin.getDataFolder(), "mails");
        this.mailCache = new ConcurrentHashMap<>();

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

        Mail mail = new Mail(sender, recipient, items, message);

        mailCache.computeIfAbsent(recipient.toLowerCase(), k -> new ArrayList<>()).add(mail);

        savePlayerMails(recipient);
    }

    public List<Mail> getPlayerMails(String playerName) {
        String name = playerName.toLowerCase();

        if (!mailCache.containsKey(name)) {
            loadPlayerMailsSync(name);
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

        List<ItemStack> items = targetMail.getItems();

        mails.remove(targetMail);
        savePlayerMails(playerName);

        return items;
    }

    private void loadPlayerMailsSync(String playerName) {
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
        List<Mail> mails = mailCache.get(name);
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(mailFolder, name + ".yml");
            
            if (mails == null || mails.isEmpty()) {
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
                e.printStackTrace();
            }
        });
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

        ItemStack removedItem = targetMail.removeItem(itemIndex);

        if (removedItem == null) {
            return null;
        }

        if (targetMail.isEmpty()) {
            mails.remove(targetMail);
        }

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
