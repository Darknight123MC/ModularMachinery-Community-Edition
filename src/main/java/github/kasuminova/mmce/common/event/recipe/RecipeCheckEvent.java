package github.kasuminova.mmce.common.event.recipe;

import crafttweaker.annotations.ZenRegister;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import stanhebben.zenscript.annotations.ZenSetter;

@ZenRegister
@ZenClass("mods.modularmachinery.RecipeCheckEvent")
public class RecipeCheckEvent extends RecipeEvent {
    private boolean isFailure = false;
    private String failureReason = null;

    public RecipeCheckEvent(TileMultiblockMachineController controller, RecipeCraftingContext context) {
        super(controller, null, context);
    }

    @ZenMethod
    public void setFailed(String reason) {
        this.isFailure = true;
        this.failureReason = reason;
        setCanceled(true);
    }

    @ZenMethod
    @ZenSetter("parallelism")
    public void setParallelism(int parallelism) {
        getContext().setParallelism(Math.min(getActiveRecipe().getParallelism(), parallelism));
    }

    public boolean isFailure() {
        return isFailure;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
