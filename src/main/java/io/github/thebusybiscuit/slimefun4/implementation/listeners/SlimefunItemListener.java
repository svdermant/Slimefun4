package io.github.thebusybiscuit.slimefun4.implementation.listeners;

import java.util.List;
import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import me.mrCookieSlime.CSCoreLibPlugin.events.ItemUseEvent;
import me.mrCookieSlime.Slimefun.SlimefunPlugin;
import me.mrCookieSlime.Slimefun.Lists.SlimefunItems;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.Juice;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.MultiTool;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockUseHandler;
import me.mrCookieSlime.Slimefun.Objects.handlers.ItemConsumptionHandler;
import me.mrCookieSlime.Slimefun.Objects.handlers.ItemDropHandler;
import me.mrCookieSlime.Slimefun.Objects.handlers.ItemHandler;
import me.mrCookieSlime.Slimefun.Objects.handlers.ItemInteractionHandler;
import me.mrCookieSlime.Slimefun.Objects.handlers.ItemUseHandler;
import me.mrCookieSlime.Slimefun.Setup.SlimefunManager;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.Slimefun;
import me.mrCookieSlime.Slimefun.api.energy.ItemEnergy;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.UniversalBlockMenu;
import me.mrCookieSlime.Slimefun.utils.Utilities;

public class SlimefunItemListener implements Listener {

	private final Utilities utilities;

	public SlimefunItemListener(SlimefunPlugin plugin) {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		utilities = SlimefunPlugin.getUtilities();
	}
	
	@EventHandler
	public void onRightClick(PlayerInteractEvent e) {
		if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			io.github.thebusybiscuit.slimefun4.api.events.ItemUseEvent event = new io.github.thebusybiscuit.slimefun4.api.events.ItemUseEvent(e);
			Bukkit.getPluginManager().callEvent(event);
			
			boolean itemUsed = e.getHand() == EquipmentSlot.HAND;
			
			if (event.useItem() != Result.DENY) {
				Optional<SlimefunItem> optional = event.getSlimefunItem();
				
				if (optional.isPresent() && Slimefun.hasUnlocked(e.getPlayer(), optional.get(), true)) {
					optional.get().callItemHandler(ItemUseHandler.class, handler -> handler.onRightClick(event));
					itemUsed = true;
				}
			}
			
			if (!itemUsed && event.useBlock() != Result.DENY) {
				Optional<SlimefunItem> optional = event.getSlimefunBlock();

				if (optional.isPresent() && Slimefun.hasUnlocked(e.getPlayer(), optional.get(), true)) {
					optional.get().callItemHandler(BlockUseHandler.class, handler -> handler.onRightClick(event));
				}
			}
			
			if (e.useInteractedBlock() != Result.DENY) {
				e.setUseInteractedBlock(event.useBlock());
			}
			
			if (e.useItemInHand() != Result.DENY) {
				e.setUseItemInHand(event.useItem());
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onRightClick(ItemUseEvent e) {
		if (e.getParentEvent() != null && e.getParentEvent().getHand() != EquipmentSlot.HAND) {
			return;
		}

		Player p = e.getPlayer();
		ItemStack item = e.getItem();

		if (!SlimefunManager.isItemSimilar(item, SlimefunItems.DEBUG_FISH, true)) {
			SlimefunItem slimefunItem = SlimefunItem.getByItem(item);

			if (slimefunItem != null) {
				if (Slimefun.hasUnlocked(p, slimefunItem, true)) {
					slimefunItem.callItemHandler(ItemInteractionHandler.class, handler ->
						handler.onRightClick(e, p, item)
					);

					if (slimefunItem instanceof MultiTool) {
						e.setCancelled(true);

						List<Integer> modes = ((MultiTool) slimefunItem).getModes();
						int index = utilities.mode.getOrDefault(p.getUniqueId(), 0);

						if (!p.isSneaking()) {
							float charge = ItemEnergy.getStoredEnergy(item);
							float cost = 0.3F;

							if (charge >= cost) {
								p.getEquipment().setItemInMainHand(ItemEnergy.chargeItem(item, -cost));
								Bukkit.getPluginManager().callEvent(new ItemUseEvent(e.getParentEvent(), SlimefunItem.getByID((String) Slimefun.getItemValue(slimefunItem.getID(), "mode." + modes.get(index) + ".item")).getItem().clone(), e.getClickedBlock()));
							}
						}
						else {
							index++;
							if (index == modes.size()) index = 0;

							SlimefunItem selectedItem = SlimefunItem.getByID((String) Slimefun.getItemValue(slimefunItem.getID(), "mode." + modes.get(index) + ".item"));
							String itemName = selectedItem != null ? selectedItem.getItemName(): "Unknown";
							SlimefunPlugin.getLocal().sendMessage(p, "messages.mode-change", true, msg -> msg.replace("%device%", "Multi Tool").replace("%mode%", ChatColor.stripColor(itemName)));
							utilities.mode.put(p.getUniqueId(), index);
						}
					}
					else if (SlimefunManager.isItemSimilar(item, SlimefunItems.HEAVY_CREAM, true)) e.setCancelled(true);
				}
				else {
					e.setCancelled(true);
				}
			}
			else {
				for (ItemHandler handler : SlimefunItem.getHandlers(ItemInteractionHandler.class)) {
					if (((ItemInteractionHandler) handler).onRightClick(e, p, item)) return;
				}
			}
		}


		if (e.getClickedBlock() != null && BlockStorage.hasBlockInfo(e.getClickedBlock())) {
			String id = BlockStorage.checkID(e.getClickedBlock());

			if (BlockMenuPreset.isInventory(id) && !canPlaceCargoNodes(p, item, e.getClickedBlock().getRelative(e.getParentEvent().getBlockFace())) && (!p.isSneaking() || item == null || item.getType() == Material.AIR)) {
				e.setCancelled(true);

				if (BlockStorage.hasUniversalInventory(id)) {
					UniversalBlockMenu menu = BlockStorage.getUniversalInventory(id);

					if (menu.canOpen(e.getClickedBlock(), p)) {
						menu.open(p);
					}
					else {
						SlimefunPlugin.getLocal().sendMessage(p, "inventory.no-access", true);
					}
				}
				else if (BlockStorage.getStorage(e.getClickedBlock().getWorld()).hasInventory(e.getClickedBlock().getLocation())) {
					BlockMenu menu = BlockStorage.getInventory(e.getClickedBlock().getLocation());

					if (menu.canOpen(e.getClickedBlock(), p)) {
						menu.open(p);
					}
					else {
						SlimefunPlugin.getLocal().sendMessage(p, "inventory.no-access", true);
					}
				}
			}
		}
	}

	private boolean canPlaceCargoNodes(Player p, ItemStack item, Block b) {
		if (canPlaceBlock(p, b) && SlimefunManager.isItemSimilar(item, SlimefunItems.CARGO_INPUT, true)) return true;
		else if (canPlaceBlock(p, b) && SlimefunManager.isItemSimilar(item, SlimefunItems.CARGO_OUTPUT, true)) return true;
		else if (canPlaceBlock(p, b) && SlimefunManager.isItemSimilar(item, SlimefunItems.CARGO_OUTPUT_ADVANCED, true)) return true;
		else if (canPlaceBlock(p, b) && SlimefunManager.isItemSimilar(item, SlimefunItems.CT_IMPORT_BUS, true)) return true;
		else if (canPlaceBlock(p, b) && SlimefunManager.isItemSimilar(item, SlimefunItems.CT_EXPORT_BUS, true)) return true;
		else return false;
	}

	private boolean canPlaceBlock(Player p, Block relative) {
		return p.isSneaking() && relative.getType() == Material.AIR;
	}

	@EventHandler
	public void onEat(PlayerItemConsumeEvent e) {
		Player p = e.getPlayer();
		ItemStack item = e.getItem();
		SlimefunItem sfItem = SlimefunItem.getByItem(item);

		if (sfItem != null) {
			if (Slimefun.hasUnlocked(p, sfItem, true)) {
				if (sfItem instanceof Juice) {
					// Fix for Saturation on potions is no longer working
					for (PotionEffect effect : ((PotionMeta) item.getItemMeta()).getCustomEffects()) {
						if (effect.getType().equals(PotionEffectType.SATURATION)) {
							p.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, effect.getDuration(), effect.getAmplifier()));
							break;
						}
					}

					// Determine from which hand the juice is being drunk, and its amount
					int mode = 0;
					if (SlimefunManager.isItemSimilar(item, p.getInventory().getItemInMainHand(), true)) {
						if (p.getInventory().getItemInMainHand().getAmount() == 1) {
							mode = 0;
						}
						else {
							mode = 2;
						}
					}
					else if (SlimefunManager.isItemSimilar(item, p.getInventory().getItemInOffHand(), true)) {
						if (p.getInventory().getItemInOffHand().getAmount() == 1) {
							mode = 1;
						}
						else {
							mode = 2;
						}
					}

					// Remove the glass bottle once drunk
					final int m = mode;

					Slimefun.runSync(() -> {
						if (m == 0) p.getEquipment().getItemInMainHand().setAmount(0);
						else if (m == 1) p.getEquipment().getItemInOffHand().setAmount(0);
						else if (m == 2) p.getInventory().removeItem(new ItemStack(Material.GLASS_BOTTLE, 1));
					}, 0L);
				}
				else {
					sfItem.callItemHandler(ItemConsumptionHandler.class, handler ->
						handler.onConsume(e, p, item)
					);
				}
			}
			else {
				e.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onIronGolemHeal(PlayerInteractEntityEvent e) {
		if (e.getRightClicked() instanceof IronGolem) {
			PlayerInventory inv = e.getPlayer().getInventory();
			ItemStack item = null;

			if (e.getHand() == EquipmentSlot.HAND) {
				item = inv.getItemInMainHand();
			}
			else if (e.getHand() == EquipmentSlot.OFF_HAND) {
				item = inv.getItemInOffHand();
			}

			if (item != null && item.getType() == Material.IRON_INGOT && SlimefunItem.getByItem(item) != null) {
				e.setCancelled(true);
				SlimefunPlugin.getLocal().sendMessage(e.getPlayer(), "messages.no-iron-golem-heal");

				// This is just there to update the Inventory...
				// Somehow cancelling it isn't enough.
				if (e.getHand() == EquipmentSlot.HAND) {
					inv.setItemInMainHand(item);
				}
				else if (e.getHand() == EquipmentSlot.OFF_HAND) {
					inv.setItemInOffHand(item);
				}
			}
		}
	}

	@EventHandler
	public void onItemDrop(PlayerDropItemEvent e) {
		for (ItemHandler handler : SlimefunItem.getHandlers(ItemDropHandler.class)) {
			if (((ItemDropHandler) handler).onItemDrop(e, e.getPlayer(), e.getItemDrop())) return;
		}
	}

}
