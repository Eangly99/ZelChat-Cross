package dev.lyy.zelchatcross.showcase;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Base64;

/**
 * High-performance, Paper/Folia-safe serializer and deserializer for ItemStacks and item arrays.
 * Implements strict payload bounds checking to prevent memory exhaustion and DoS attacks.
 */
public final class ItemSerializer {

    private static final int MAX_ARRAY_LENGTH = 100;
    private static final int MAX_ITEM_BYTES = 1_000_000; // 1MB max per item
    private static final int MAX_BASE64_LENGTH = 10_000_000; // 10MB max total payload

    private ItemSerializer() {}

    /**
     * Serializes a single ItemStack to Base64.
     */
    public static String toBase64(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return "";
        }
        try {
            byte[] bytes = item.serializeAsBytes();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Throwable fallback) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                 BukkitObjectOutputStream oos = new BukkitObjectOutputStream(baos)) {
                oos.writeObject(item);
                oos.flush();
                return Base64.getEncoder().encodeToString(baos.toByteArray());
            } catch (Exception e) {
                return "";
            }
        }
    }

    /**
     * Deserializes a single ItemStack from Base64.
     */
    public static ItemStack fromBase64(String base64) {
        if (base64 == null || base64.isEmpty() || base64.length() > MAX_BASE64_LENGTH) {
            return new ItemStack(Material.AIR);
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(base64);
            if (bytes.length > MAX_ITEM_BYTES) {
                return new ItemStack(Material.AIR);
            }
            return ItemStack.deserializeBytes(bytes);
        } catch (Throwable fallback) {
            try {
                byte[] bytes = Base64.getDecoder().decode(base64);
                try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                     BukkitObjectInputStream ois = new BukkitObjectInputStream(bais)) {
                    return (ItemStack) ois.readObject();
                }
            } catch (Exception e) {
                return new ItemStack(Material.AIR);
            }
        }
    }

    /**
     * Serializes an array of ItemStacks to Base64.
     */
    public static String itemArrayToBase64(ItemStack[] items) {
        if (items == null || items.length == 0) {
            return "";
        }
        int len = Math.min(items.length, MAX_ARRAY_LENGTH);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(len);
            for (int i = 0; i < len; i++) {
                ItemStack item = items[i];
                if (item == null || item.getType() == Material.AIR) {
                    dos.writeInt(0);
                } else {
                    byte[] itemBytes;
                    try {
                        itemBytes = item.serializeAsBytes();
                    } catch (Throwable t) {
                        try (ByteArrayOutputStream itemBaos = new ByteArrayOutputStream();
                             BukkitObjectOutputStream boos = new BukkitObjectOutputStream(itemBaos)) {
                            boos.writeObject(item);
                            boos.flush();
                            itemBytes = itemBaos.toByteArray();
                        }
                    }
                    dos.writeInt(itemBytes.length);
                    dos.write(itemBytes);
                }
            }
            dos.flush();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Deserializes an array of ItemStacks from Base64.
     */
    public static ItemStack[] itemArrayFromBase64(String base64) {
        if (base64 == null || base64.isEmpty() || base64.length() > MAX_BASE64_LENGTH) {
            return new ItemStack[0];
        }
        try {
            byte[] raw = Base64.getDecoder().decode(base64);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(raw);
                 DataInputStream dis = new DataInputStream(bais)) {
                int length = dis.readInt();
                if (length < 0 || length > MAX_ARRAY_LENGTH) {
                    return new ItemStack[0];
                }
                ItemStack[] items = new ItemStack[length];
                for (int i = 0; i < length; i++) {
                    int byteLen = dis.readInt();
                    if (byteLen == 0) {
                        items[i] = new ItemStack(Material.AIR);
                    } else {
                        if (byteLen < 0 || byteLen > MAX_ITEM_BYTES) {
                            return new ItemStack[0];
                        }
                        byte[] itemBytes = new byte[byteLen];
                        dis.readFully(itemBytes);
                        try {
                            items[i] = ItemStack.deserializeBytes(itemBytes);
                        } catch (Throwable t) {
                            try (ByteArrayInputStream ibais = new ByteArrayInputStream(itemBytes);
                                 BukkitObjectInputStream bois = new BukkitObjectInputStream(ibais)) {
                                items[i] = (ItemStack) bois.readObject();
                            }
                        }
                    }
                }
                return items;
            }
        } catch (Exception e) {
            return new ItemStack[0];
        }
    }
}
