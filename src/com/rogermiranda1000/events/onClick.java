package com.rogermiranda1000.events;

import com.rogermiranda1000.mineit.MineIt;
import com.rogermiranda1000.mineit.Mines;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class onClick implements Listener {
    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();
        Inventory inventory = e.getInventory();

        if(!e.getView().getTitle()/*inventory.getName()*/.equalsIgnoreCase("§6§lMineIt") && !e.getView().getTitle()/*inventory.getName()*/.contains("§cEdit mine")) return;
        e.setCancelled(true);
        if(!player.hasPermission("mineit.open")) {
            player.closeInventory();
            player.sendMessage(MineIt.prefix+"You can't use this menu.");
            return;
        }
        if(clicked==null || clicked.getType()==null) return;
        if((clicked.equals(MineIt.item2) || clicked.equals(MineIt.editar) || clicked.equals(MineIt.crear)) && !player.hasPermission("mineit.create")) {
            player.sendMessage(MineIt.prefix+"You can't use this action.");
            return;
        }

        if(clicked.equals(MineIt.item2)) {
            player.closeInventory();
            MineIt.instance.getLogger().info("Giving Mine creator to "+player.getName()+"...");
            player.getInventory().addItem(MineIt.item);
        }
        else if(clicked.equals(MineIt.crear)) {
            player.closeInventory();
            player.sendMessage(MineIt.prefix+"Under construction, use "+ ChatColor.AQUA+"/mineit create [name]"+ChatColor.RED+" instead.");
        }
        else if(clicked.equals(MineIt.editar)) editMine(player);
        else if(clicked.equals(MineIt.anvil)) {
            player.closeInventory();
            if(e.getView().getTitle()/*inventory.getName()*/.contains("§cEdit mine") && isEditing(inventory)) editMine(player);
            else player.openInventory(MineIt.inv);
            return;
        }
        else if(clicked.equals(MineIt.redstone)) {
            if(!player.hasPermission("mineit.remove")) {
                player.sendMessage(MineIt.prefix + "You don't have the permissions to do that.");
                return;
            }

            Mines mine = getMine(e.getView().getTitle().substring(14));
            if(mine==null) return;

            MineIt.instance.minas.remove(mine);
            try {
                File f = new File(MineIt.instance.getDataFolder(), mine.name + ".yml");
                if (f.exists()) f.delete();
            }
            catch (Exception ex) {}
            player.sendMessage(MineIt.clearPrefix+"Mine '"+mine.name+"' removed.");
            player.closeInventory();
            return;
        }
        else if(clicked.getType()==Material.FURNACE) {
            Mines mine = getMine(e.getView().getTitle().substring(14));
            if(mine==null) return;

            if(mine.start) player.sendMessage(MineIt.clearPrefix+"Mine '"+mine.name+"' stopped.");
            else player.sendMessage(MineIt.clearPrefix+"Starting mine '"+mine.name+"'...");
            mine.start = !mine.start;
            inventory.setItem(16, MineIt.instance.watch(mine));
            return;
        }
        else if(e.getView().getTitle()/*inventory.getName()*/.equals("§cEdit mine") && clicked.getType()==Material.STONE && !isEditing(inventory)) {
            Mines mine = getMine(clicked.getItemMeta().getDisplayName());
            if(mine==null) return;

            player.closeInventory();
            MineIt.instance.edintingMine(player, mine);
            return;
        }

        if(e.getView().getTitle()/*inventory.getName()*/.contains("§cEdit mine") && isEditing(inventory)) {
            for(int x = 0; x<inventory.getSize()-9; x++) {
                if(inventory.getItem(x)==null) continue;
                //if(player.getItemOnCursor().getType()==Material.AIR) break;
                if(!inventory.getItem(x).equals(clicked)) continue;
                /*if(inventory.getItem(x).hasItemMeta() && inventory.getItem(x).getItemMeta().hasDisplayName() &&
                        inventory.getItem(x).getItemMeta().getDisplayName().equalsIgnoreCase("-")) break;*/

                e.setCancelled(true);

                ItemStack item = new ItemStack(player.getItemOnCursor().getType());
                //if(item==null) return;
                if(item.getType()!=Material.AIR && !item.getType().isBlock()) return;

                Mines mine = getMine(e.getView().getTitle().substring(14));
                if(mine==null) return;

                if(((int)x/9)%2==1) {
                    if(item.getType()==Material.AIR) return;

                    List<String> s = new ArrayList<String>();
                    s.addAll(Arrays.asList(mine.stages));
                    if(!s.contains(item.getType().name())) {
                        player.sendMessage(MineIt.prefix+item.getType().name().toLowerCase()+" stage doesn't exists in this mine!");
                        return;
                    }

                    for(int w = 0; w<s.size(); w++) {
                        String st = s.get(w);
                        if(!item.getType().name().equalsIgnoreCase(st)) continue;

                        int z = (((int)(((int)((x-9)/9))/2))*9) + ((x-9)%9);

                        if(z==0 || z-2>=mine.stageGo.length) return;

                        mine.stageGo[z-2] = w;

                        ItemMeta m = item.getItemMeta();
                        List<String> str = new ArrayList<String>();
                        str.add("On break, go to stage " + String.valueOf(w+1));
                        m.setLore(str);
                        item.setItemMeta(m);
                        inventory.setItem(x, item);

                        return;
                    }

                    return;
                }
                int z = (((int)(((int)(x/9))/2))*9) + (x%9);

                if(item.getType()==Material.AIR) {
                    List<String> s = new ArrayList<String>();
                    s.addAll(Arrays.asList(mine.stages));
                    s.remove(inventory.getItem(x).getType().name());
                    if(s.size()==0) {
                        player.sendMessage(MineIt.prefix+"There can't be a null mine.");
                        return;
                    }
                    mine.stages = s.toArray(new String[s.size()]);
                    if(MineIt.instance.limit) MineIt.instance.updateStages(mine);

                    player.closeInventory();
                    MineIt.instance.edintingMine(player, mine);
                    return;
                }

                //item.setAmount(1);
                ItemMeta m = item.getItemMeta();
                List<String> s = new ArrayList<String>();
                s.addAll(Arrays.asList(mine.stages));
                if(s.contains(item.getType().name())) {
                    player.sendMessage(MineIt.prefix+"There's already a "+item.getType().name().toLowerCase()+" stage!");
                    return;
                }

                if (inventory.getItem(x).hasItemMeta() && inventory.getItem(x).getItemMeta().hasLore()) {
                    m.setLore(inventory.getItem(x).getItemMeta().getLore());
                    item.setItemMeta(m);

                    for (int y = 0; y<s.size(); y++) {
                        if(s.get(y).equalsIgnoreCase(inventory.getItem(x).getType().name())) {
                            s.set(y, item.getType().name());
                            break;
                        }
                    }
                    mine.stages = s.toArray(new String[s.size()]);
                    if(MineIt.instance.limit) MineIt.instance.updateStages(mine);
                    inventory.setItem(x, item);
                    return;
                }

                s.add(item.getType().name());
                mine.stages = s.toArray(new String[s.size()]);
                if (!inventory.getItem(x).hasItemMeta() || !inventory.getItem(x).getItemMeta().hasLore())
                    mine.stageGo = IntStream.range(1, mine.stages.length-1).toArray();
                if(MineIt.instance.limit) MineIt.instance.updateStages(mine);

                List<String> st = new ArrayList<String>();
                st.add("Stage "+String.valueOf(mine.stages.length));
                if(MineIt.instance.limit) st.add("Limit setted to "+String.valueOf(mine.stageLimit[z])+" blocks");
                m.setLore(st);
                item.setItemMeta(m);
                inventory.setItem(x, item);

                item = new ItemStack(Material.getMaterial(mine.stages[z-1]));
                m = item.getItemMeta();
                st = new ArrayList<String>();
                st.add("On break, go to stage " + String.valueOf(z));
                m.setLore(st);
                item.setItemMeta(m);
                inventory.setItem(x+9, item);

                if((x+1)%9==0) {
                    player.closeInventory();
                    MineIt.instance.edintingMine(player, mine);
                }
                return;
            }
            e.setCancelled(false);
        }
    }

    Mines getMine(String name) {
        for(Mines mine: MineIt.instance.minas) {
            if (!mine.name.equalsIgnoreCase(name)) continue;
            return mine;
        }
        return null;
    }

    void editMine(Player player) {
        player.closeInventory();
        int l = (int) (MineIt.instance.minas.size()/9);
        if(MineIt.instance.minas.size()%9>0) l++;
        if(l==0) {
            Inventory i = Bukkit.createInventory(null, 18, "§cEdit mine");
            ItemStack none = new ItemStack(Material.COBBLESTONE);
            ItemMeta m = none.getItemMeta();
            m.setDisplayName("-");
            none.setItemMeta(m);
            for(int x=0; x<9; x++) i.setItem(x, none);
            i.setItem(9, MineIt.anvil);
            player.openInventory(i);
            return;
        }
        l++;
        if(l>6) {
            player.sendMessage(MineIt.prefix+"Error, too many mines. Please remove some mines.");
            return;
        }

        Inventory i = Bukkit.createInventory(null, l*9, "§cEdit mine");
        int pos=0;
        for (Mines mine: MineIt.instance.minas) {
            ItemStack mina = new ItemStack(Material.STONE);
            ItemMeta meta = mina.getItemMeta();
            meta.setDisplayName(mine.name);
            meta.setLore(Arrays.asList(mine.stages));
            mina.setItemMeta(meta);

            i.setItem(pos++, mina);
        }
        i.setItem((l-1)*9, MineIt.anvil);

        player.openInventory(i);
    }

    boolean isEditing(Inventory i) {
        if(!i.getItem(0).hasItemMeta()) return false;
        if(!i.getItem(0).getItemMeta().hasLore()) return false;
        return i.getItem(0).getItemMeta().getLore().get(0).equalsIgnoreCase("Stage 1");
    }
}
