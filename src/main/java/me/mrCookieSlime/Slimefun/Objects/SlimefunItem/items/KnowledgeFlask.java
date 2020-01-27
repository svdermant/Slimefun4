package me.mrCookieSlime.Slimefun.Objects.SlimefunItem.items;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.thebusybiscuit.cscorelib2.item.CustomItem;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.Category;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SimpleSlimefunItem;
import me.mrCookieSlime.Slimefun.Objects.handlers.ItemUseHandler;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;

public class KnowledgeFlask extends SimpleSlimefunItem<ItemUseHandler> {

	public KnowledgeFlask(Category category, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe, ItemStack recipeOutput) {
		super(category, item, recipeType, recipe, recipeOutput);
	}

	@Override
	public ItemUseHandler getItemHandler() {
		return e -> {
			Player p = e.getPlayer();
			if (p.getLevel() >= 1 && (!e.getClickedBlock().isPresent() || !(e.getClickedBlock().get().getState() instanceof Container))) {
				p.setLevel(p.getLevel() - 1);
				p.getInventory().addItem(new CustomItem(Material.EXPERIENCE_BOTTLE, "&aFlask of Knowledge"));
					
				p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 0.5F);
					
				ItemStack item = e.getItem();
				item.setAmount(item.getAmount() - 1);
				e.cancel();
			}
		};
	}

}
