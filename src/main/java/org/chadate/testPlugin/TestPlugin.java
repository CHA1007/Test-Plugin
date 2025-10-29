package org.chadate.testPlugin;

import org.bukkit.plugin.java.JavaPlugin;
import org.chadate.testPlugin.commands.MailCommands;
import org.chadate.testPlugin.gui.MailGUI;
import org.chadate.testPlugin.listeners.PlayerListener;
import org.chadate.testPlugin.manager.MailManager;

import java.util.Objects;

public final class TestPlugin extends JavaPlugin {

    private MailManager mailManager;
    private MailGUI mailGUI;

    @Override
    public void onEnable() {

        mailManager = new MailManager(this);
        mailGUI = new MailGUI(this);

        MailCommands mailCommands = new MailCommands(this);
        Objects.requireNonNull(getCommand("mailbox")).setExecutor(mailCommands);
        Objects.requireNonNull(getCommand("mailbox")).setTabCompleter(mailCommands);
        Objects.requireNonNull(getCommand("sendmail")).setExecutor(mailCommands);
        Objects.requireNonNull(getCommand("sendmail")).setTabCompleter(mailCommands);
        Objects.requireNonNull(getCommand("adminmail")).setExecutor(mailCommands);
        Objects.requireNonNull(getCommand("adminmail")).setTabCompleter(mailCommands);

        getServer().getPluginManager().registerEvents(mailGUI, this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

    }

    @Override
    public void onDisable() {

        if (mailManager != null) {
            mailManager.saveAll();
        }
    }

    public MailManager getMailManager() {
        return mailManager;
    }

    public MailGUI getMailGUI() {
        return mailGUI;
    }
}
