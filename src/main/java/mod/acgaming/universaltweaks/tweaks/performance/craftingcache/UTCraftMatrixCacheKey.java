package mod.acgaming.universaltweaks.tweaks.performance.craftingcache;

import java.util.Arrays;
import java.util.Objects;

import net.minecraft.init.Items;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

// Courtesy of EverNife
public class UTCraftMatrixCacheKey
{
    final int hashCode;
    final World world;
    final Item[] items;
    final int[] damages;

    public UTCraftMatrixCacheKey(InventoryCrafting craftMatrix, World worldIn)
    {
        world = worldIn;

        int sizeInventory = craftMatrix.getSizeInventory();
        items = new Item[sizeInventory];
        int[] maybeDamages = new int[sizeInventory];
        boolean nonzeroDamage = false;
        for (int i = 0; i < sizeInventory; i++)
        {
            ItemStack stack = craftMatrix.getStackInSlot(i);
            Item item = stack.getItem();
            items[i] = item;
            int itemDamage = stack.getItemDamage();
            maybeDamages[i] = item != Items.AIR ? itemDamage : 0;
            nonzeroDamage = itemDamage != 0 || nonzeroDamage;
        }

        if (nonzeroDamage) {
            damages = maybeDamages;
        } else {
            damages = null;
        }

        hashCode = Objects.hash(world, Arrays.hashCode(items), Arrays.hashCode(damages));
    }

    @Override
    public int hashCode()
    {
        return hashCode;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof UTCraftMatrixCacheKey)) return false;
        UTCraftMatrixCacheKey that = (UTCraftMatrixCacheKey) o;
        return Objects.equals(world, that.world) && Arrays.equals(items, that.items) && Arrays.equals(damages, that.damages);
    }
}