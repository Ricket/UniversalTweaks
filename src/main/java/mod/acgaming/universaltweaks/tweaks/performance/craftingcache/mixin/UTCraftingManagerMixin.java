package mod.acgaming.universaltweaks.tweaks.performance.craftingcache.mixin;

import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import mod.acgaming.universaltweaks.tweaks.performance.craftingcache.UTCraftMatrixCacheKey;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;

import mod.acgaming.universaltweaks.UniversalTweaks;
import mod.acgaming.universaltweaks.config.UTConfigGeneral;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.LoaderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(CraftingManager.class)
public class UTCraftingManagerMixin
{
    private static final Object2ObjectLinkedOpenHashMap<UTCraftMatrixCacheKey, Optional<IRecipe>> NON_NBT_CRAFT_CACHE = new Object2ObjectLinkedOpenHashMap<>();

    /** Carries the key from before findMatchingRecipe to after; so that we don't compute it twice **/
    private static UTCraftMatrixCacheKey matrixKey;

    @Inject(method = "findMatchingRecipe", at = @At("HEAD"), cancellable = true)
    private static void beforeFindMatchingRecipe(InventoryCrafting craftMatrix, World worldIn, CallbackInfoReturnable<IRecipe> cir)
    {
        // In case of an unexpected behavior (e.g. exception thrown during findMatchingRecipe), reset matrixKey to null
        // every time.
        matrixKey = null;

        if (!isValid(craftMatrix)) {
            return;
        }

        matrixKey = new UTCraftMatrixCacheKey(craftMatrix, worldIn);
        Optional<IRecipe> optionalContent = NON_NBT_CRAFT_CACHE.getAndMoveToFirst(matrixKey);
        if (optionalContent != null) {
            if (UTConfigGeneral.DEBUG.utDebugToggle) UniversalTweaks.LOGGER.debug("UTCraftingManager ::: Find matching recipe - cache hit");
            matrixKey = null;
            cir.setReturnValue(optionalContent.orElse(null));
        } else {
            if (UTConfigGeneral.DEBUG.utDebugToggle) UniversalTweaks.LOGGER.debug("UTCraftingManager ::: Find matching recipe - cache miss");
        }
    }

    @Inject(method = "findMatchingRecipe", at = @At("RETURN"))
    private static void afterFindMatchingRecipe(InventoryCrafting craftMatrix, World worldIn, CallbackInfoReturnable<IRecipe> cir)
    {
        if (matrixKey == null) {
            return;
        }

        if (UTConfigGeneral.DEBUG.utDebugToggle) UniversalTweaks.LOGGER.debug("UTCraftingManager ::: Find matching recipe - loading result into cache");
        NON_NBT_CRAFT_CACHE.putAndMoveToFirst(matrixKey, Optional.ofNullable(cir.getReturnValue()));
        matrixKey = null;
    }

    @Inject(method = "findMatchingResult", at = @At("HEAD"), cancellable = true)
    private static void beforeFindMatchingResult(InventoryCrafting craftMatrix, World worldIn, CallbackInfoReturnable<ItemStack> cir)
    {
        if (!isValid(craftMatrix)) {
            return;
        }

        Optional<IRecipe> optionalContent = NON_NBT_CRAFT_CACHE.getAndMoveToFirst(new UTCraftMatrixCacheKey(craftMatrix, worldIn));
        if (optionalContent != null) {
            if (UTConfigGeneral.DEBUG.utDebugToggle) UniversalTweaks.LOGGER.debug("UTCraftingManager ::: Find matching result - cache hit");
            cir.setReturnValue(optionalContent.isPresent() ? optionalContent.get().getCraftingResult(craftMatrix) : ItemStack.EMPTY);
        } else {
            if (UTConfigGeneral.DEBUG.utDebugToggle) UniversalTweaks.LOGGER.debug("UTCraftingManager ::: Find matching result - cache miss");
        }
    }

    @Inject(method = "getRemainingItems", at = @At("HEAD"), cancellable = true)
    private static void beforeGetRemainingItems(InventoryCrafting craftMatrix, World worldIn, CallbackInfoReturnable<NonNullList<ItemStack>> cir)
    {
        if (!isValid(craftMatrix)) {
            return;
        }

        Optional<IRecipe> optionalContent = NON_NBT_CRAFT_CACHE.getAndMoveToFirst(new UTCraftMatrixCacheKey(craftMatrix, worldIn));
        if (optionalContent != null) {
            if (UTConfigGeneral.DEBUG.utDebugToggle) UniversalTweaks.LOGGER.debug("UTCraftingManager ::: Get remaining items - cache hit");
            if (optionalContent.isPresent()) {
                cir.setReturnValue(optionalContent.get().getRemainingItems(craftMatrix));
            } else {
                NonNullList<ItemStack> nonnulllist = NonNullList.withSize(craftMatrix.getSizeInventory(), ItemStack.EMPTY);
                for (int i = 0; i < nonnulllist.size(); ++i)
                {
                    nonnulllist.set(i, craftMatrix.getStackInSlot(i));
                }
                cir.setReturnValue(nonnulllist);
            }
        } else {
            if (UTConfigGeneral.DEBUG.utDebugToggle) UniversalTweaks.LOGGER.debug("UTCraftingManager ::: Get remaining items - cache miss");
        }
    }

    private static boolean isValid(InventoryCrafting craftMatrix)
    {
        if (!Loader.instance().hasReachedState(LoaderState.SERVER_STARTING)) {
            return false;
        }

        for (int i = 0; i < craftMatrix.getSizeInventory(); i++)
        {
            ItemStack itemStack = craftMatrix.getStackInSlot(i);
            // Skip NBT items
            if (itemStack.hasTagCompound()) return false;

            // Skip IC2C's stacked items
            if (Loader.isModLoaded("ic2-classic-spmod") && itemStack.getCount() > 1) return false;
        }
        return true;
    }
}