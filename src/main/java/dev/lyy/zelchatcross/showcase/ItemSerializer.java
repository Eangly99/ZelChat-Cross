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
 */
public final class ItemSerializer {

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
        if (base64 == null || base64.isEmpty()) {
            return new ItemStack(Material.AIR);
        }
        byte[] bytes = Base64.getDecoder().decode(base64);
        try {
            return ItemStack.deserializeBytes(bytes);
        } catch (Throwable fallback) {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                 BukkitObjectInputStream ois = new BukkitObjectInputStream(bais)) {
                return (ItemStack) ois.readObject();
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
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(items.length);
            for (ItemStack item : items) {
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
        if (base64 == null || base64.isEmpty()) {
            return new ItemStack[0];
        }
        byte[] raw = Base64.getDecoder().decode(base64);
        try (ByteArrayInputStream bais = new ByteArrayInputStream(raw);
             DataInputStream dis = new DataInputStream(bais)) {
            int length = dis.readInt();
            ItemStack[] items = new ItemStack[length];
            for (int i = 0; i < length; i++) {
                int byteLen = dis.readInt();
                if (byteLen == 0) {
                    items[i] = new ItemStack(Material.AIR);
                } else {
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
        } catch (Exception e) {
            return new ItemStack[0];
        }
    }
}
