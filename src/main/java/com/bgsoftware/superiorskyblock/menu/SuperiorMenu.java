package com.bgsoftware.superiorskyblock.menu;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.hooks.PlaceholderHook;
import com.bgsoftware.superiorskyblock.utils.reflections.Fields;
import com.bgsoftware.superiorskyblock.utils.items.ItemBuilder;
import com.bgsoftware.superiorskyblock.utils.threads.Executor;
import com.bgsoftware.superiorskyblock.wrappers.SoundWrapper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class SuperiorMenu implements InventoryHolder {

    protected static final SuperiorSkyblockPlugin plugin = SuperiorSkyblockPlugin.getPlugin();

    public static final char[] itemChars = new char[] {
            '!', '@', '#', '$', '%', '^', '&', '*', '-', '_', '+', '=',
            '~', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '>',
            '<', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K',
            'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W',
            'X', 'Y', 'Z'
    };

    private static final Map<String, MenuData> dataMap = new HashMap<>();

    private final String identifier;
    protected final SuperiorPlayer superiorPlayer;

    protected SuperiorMenu previousMenu;
    protected boolean previousMove = true, closeButton = false, nextMove = false;
    private boolean refreshing = false;

    public SuperiorMenu(String identifier, SuperiorPlayer superiorPlayer){
        this.identifier = identifier;
        this.superiorPlayer = superiorPlayer;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void addSound(int slot, SoundWrapper sound) {
        if(sound != null)
            getData().sounds.put(slot, sound);
    }

    public void addCommands(int slot, List<String> commands) {
        if(commands != null && !commands.isEmpty())
            getData().commands.put(slot, commands);
    }

    public void addFillItem(int slot, ItemBuilder itemBuilder){
        if(itemBuilder != null)
            getData().fillItems.put(slot, itemBuilder);
    }

    public void setBackButton(int slot){
        addData("backSlot", slot);
    }

    public void resetData(){
        dataMap.put(identifier, new MenuData());
    }

    public void setTitle(String title){
        getData().title = title;
    }

    public void setRowsSize(int rowsSize){
        getData().rowsSize = rowsSize;
    }

    public void setInventoryType(InventoryType inventoryType){
        getData().inventoryType = inventoryType;
    }

    public void addData(String key, Object value){
        getData().data.put(key, value);
    }

    public Object getData(String key){
        return getData().data.get(key);
    }

    public boolean containsData(String key){
        return getData().data.containsKey(key);
    }

    @Override
    public Inventory getInventory(){
        return buildInventory(null);
    }

    public final void onClick(InventoryClickEvent e){
        if(refreshing)
            return;

        Player player = superiorPlayer.asPlayer();

        if(e.getRawSlot() == getBackSlot()){
            closeButton = true;
            e.getWhoClicked().closeInventory();
        }

        else if(e.getCurrentItem() != null) {
            SoundWrapper sound = getSound(e.getRawSlot());
            if (sound != null)
                sound.playSound(player);

            List<String> commands = getCommands(e.getRawSlot());
            if (commands != null)
                commands.forEach(command ->
                        Bukkit.dispatchCommand(command.startsWith("PLAYER:") ? player : Bukkit.getConsoleSender(),
                                command.replace("PLAYER:", "").replace("%player%", player.getName())));
        }

        onPlayerClick(e);
    }

    protected abstract void onPlayerClick(InventoryClickEvent e);

    public void open(SuperiorMenu previousMenu){
        if(Bukkit.isPrimaryThread()){
            Executor.async(() -> open(previousMenu));
            return;
        }

        Inventory inventory = getInventory();

        Executor.sync(() -> {
            Player player = superiorPlayer.asPlayer();

            if(player == null)
                return;

            SuperiorMenu currentMenu = null;
            InventoryHolder inventoryHolder = player.getOpenInventory().getTopInventory().getHolder();
            if(inventoryHolder instanceof SuperiorMenu) {
                currentMenu = (SuperiorMenu) inventoryHolder;
                currentMenu.nextMove = true;
            }

            if(Arrays.equals( player.getOpenInventory().getTopInventory().getContents(), inventory.getContents()))
                return;

            player.openInventory(inventory);

            refreshing = false;

            this.previousMenu = previousMenu != null ? previousMenu : previousMove ? currentMenu : null;
        });
    }

    public void closeInventory(SuperiorPlayer superiorPlayer){
        Executor.sync(() -> {
            if(!nextMove && !closeButton && plugin.getSettings().onlyBackButton) {
                open(previousMenu);
            }

            else if(previousMenu != null) {
                if (previousMove)
                    previousMenu.open(previousMenu.previousMenu);
                else
                    previousMove = true;
            }

            closeButton = false;
            nextMove = false;
        });
    }

    protected Inventory buildInventory(Function<String, String> titleReplacer){
        MenuData menuData = getData();
        Inventory inventory;

        String title = PlaceholderHook.parse(superiorPlayer, menuData.title);
        if(titleReplacer != null)
            title = titleReplacer.apply(title);

        if(menuData.inventoryType != InventoryType.CHEST){
            inventory = Bukkit.createInventory(this, menuData.inventoryType, title);
        }

        else{
            inventory = Bukkit.createInventory(this, menuData.rowsSize * 9, title);
        }

        if(inventory.getHolder() == null)
            Fields.CRAFT_INVENTORY_INVENTORY.set(inventory, plugin.getNMSAdapter().getCustomHolder(menuData.inventoryType,this, title));

        //noinspection all
        List<Integer> slots = containsData("slots") ? (List<Integer>) getData("slots") : new ArrayList<>();

        for(Map.Entry<Integer, ItemBuilder> itemStackEntry : menuData.fillItems.entrySet()) {
            ItemBuilder itemBuilder = itemStackEntry.getValue().clone();
            if(itemStackEntry.getKey() >= 0)
                inventory.setItem(itemStackEntry.getKey(), slots.contains(itemStackEntry.getKey()) ? itemBuilder.build() : itemBuilder.build(superiorPlayer));
        }

        return inventory;
    }

    private SoundWrapper getSound(int slot){
        return getData().sounds.get(slot);
    }

    private List<String> getCommands(int slot){
        return getData().commands.get(slot);
    }

    private MenuData getData(){
        if(!dataMap.containsKey(identifier)){
            dataMap.put(identifier, new MenuData());
        }

        return dataMap.get(identifier);
    }

    private int getBackSlot() {
        return (Integer) getData("backSlot");
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SuperiorMenu && ((SuperiorMenu) obj).getIdentifier().equals(getIdentifier());
    }

    public static void killMenu(SuperiorPlayer superiorPlayer){
        InventoryHolder inventoryHolder = superiorPlayer.asPlayer().getOpenInventory().getTopInventory().getHolder();
        if(inventoryHolder instanceof SuperiorMenu)
            ((SuperiorMenu) inventoryHolder).previousMove = false;

        superiorPlayer.asPlayer().closeInventory();
    }

    protected static <T extends SuperiorMenu> void refreshMenus(Class<T> menuClazz){
        runActionOnMenus(menuClazz, superiorMenu -> true, ((player, superiorMenu) -> {
            superiorMenu.previousMove = false;
            superiorMenu.open(superiorMenu.previousMenu);
        }));
    }

    protected static <T extends SuperiorMenu> void destroyMenus(Class<T> menuClazz){
        destroyMenus(menuClazz, superiorMenu -> true);
    }

    protected static <T extends SuperiorMenu> void destroyMenus(Class<T> menuClazz, Predicate<T> predicate){
        runActionOnMenus(menuClazz, predicate, ((player, superiorMenu) -> player.closeInventory()));
    }

    private static <T extends SuperiorMenu> void runActionOnMenus(Class<T> menuClazz, Predicate<T> predicate, BiConsumer<Player, SuperiorMenu> callback){
        for(Player player : Bukkit.getOnlinePlayers()){
            try {
                InventoryHolder inventoryHolder = player.getOpenInventory().getTopInventory().getHolder();
                //noinspection unchecked
                if (menuClazz.isInstance(inventoryHolder) && predicate.test((T) inventoryHolder)) {
                    SuperiorMenu superiorMenu = (SuperiorMenu) inventoryHolder;
                    callback.accept(player, superiorMenu);
                }
            }catch(Exception ignored){}
        }
    }

    protected static class MenuData{

        private Map<Integer, SoundWrapper> sounds = new HashMap<>();
        private Map<Integer, List<String>> commands = new HashMap<>();
        private Map<Integer, ItemBuilder> fillItems = new HashMap<>();
        private Map<String, Object> data = new HashMap<>();
        private String title = "";
        private InventoryType inventoryType = InventoryType.CHEST;
        private int rowsSize = 6;

    }

}
