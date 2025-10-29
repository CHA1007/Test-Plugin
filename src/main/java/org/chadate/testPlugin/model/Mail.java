package org.chadate.testPlugin.model;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Mail implements ConfigurationSerializable {
    private final String id;
    private final String sender;
    private final List<ItemStack> items;
    private final String recipient;
    private final String sendTime;
    private final String message;

    public Mail(String sender, String recipient, List<ItemStack> items, String message) {
        this.id = UUID.randomUUID().toString();
        this.sender = sender;
        this.recipient = recipient;
        this.items = new ArrayList<>(items);
        this.sendTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        this.message = message;
    }

    @SuppressWarnings("unchecked")
    public Mail(Map<String, Object> map) {
        this.id = (String) map.get("id");
        this.sender = (String) map.get("sender");
        this.recipient = (String) map.get("recipient");
        this.items = (List<ItemStack>) map.get("items");
        this.sendTime = (String) map.get("sendTime");
        this.message = (String) map.get("message");
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("sender", sender);
        map.put("recipient", recipient);
        map.put("items", items);
        map.put("sendTime", sendTime);
        if (message != null){
            map.put("message", message);
        }
        return map;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getSender() {
        return sender;
    }

    public List<ItemStack> getItems() {
        return new ArrayList<>(items);
    }

    public String getSendTime() {
        return sendTime;
    }

    public String getMessage() {
        return message;
    }

    public boolean hasMessage() {
        return message != null && !message.isEmpty();
    }

    public ItemStack removeItem(int index) {
        if (index < 0 || index >= items.size()) {
            return null;
        }
        return items.remove(index);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
