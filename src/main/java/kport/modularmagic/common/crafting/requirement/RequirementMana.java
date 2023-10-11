package kport.modularmagic.common.crafting.requirement;

import com.google.common.collect.Lists;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftCheck;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.lib.RegistriesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.util.ResultChance;
import kport.modularmagic.common.crafting.requirement.types.ModularMagicRequirements;
import kport.modularmagic.common.crafting.requirement.types.RequirementTypeMana;
import kport.modularmagic.common.integration.jei.component.JEIComponentMana;
import kport.modularmagic.common.integration.jei.ingredient.Mana;
import kport.modularmagic.common.tile.TileManaProvider;

import javax.annotation.Nonnull;
import java.util.List;

public class RequirementMana extends ComponentRequirement<Mana, RequirementTypeMana> {

    public int manaAmount;

    public RequirementMana(IOType actionType, int manaAmount) {
        super((RequirementTypeMana) RegistriesMM.REQUIREMENT_TYPE_REGISTRY.getValue(ModularMagicRequirements.KEY_REQUIREMENT_MANA), actionType);
        this.manaAmount = manaAmount;
    }

    @Override
    public boolean isValidComponent(ProcessingComponent<?> component, RecipeCraftingContext ctx) {
        return component.getComponent().getContainerProvider() instanceof TileManaProvider;
    }

    @Override
    public boolean startCrafting(ProcessingComponent component, RecipeCraftingContext context, ResultChance chance) {
        if (!canStartCrafting(component, context, Lists.newArrayList()).isSuccess())
            return false;
        TileManaProvider provider = (TileManaProvider) component.getComponent().getContainerProvider();
        provider.recieveMana(-manaAmount);
        return true;
    }

    @Nonnull
    @Override
    public CraftCheck finishCrafting(ProcessingComponent component, RecipeCraftingContext context, ResultChance chance) {
        if (getActionType() == IOType.OUTPUT) {
            TileManaProvider provider = (TileManaProvider) component.getComponent().getContainerProvider();
            if (provider instanceof TileManaProvider.Output)
                provider.recieveMana(manaAmount);
        }
        return CraftCheck.success();
    }

    @Nonnull
    @Override
    public CraftCheck canStartCrafting(ProcessingComponent component, RecipeCraftingContext context, List restrictions) {
        if (getActionType() == IOType.INPUT) {
            TileManaProvider provider = (TileManaProvider) component.getComponent().getContainerProvider();
            return provider.getCurrentMana() >= this.manaAmount ? CraftCheck.success() : CraftCheck.failure("error.modularmachinery.requirement.mana.less");
        }
        return CraftCheck.success();
    }

    @Nonnull
    @Override
    public String getMissingComponentErrorMessage(IOType ioType) {
        return "error.modularmachinery.component.invalid";
    }

    @Override
    public ComponentRequirement deepCopy() {
        return this;
    }

    @Override
    public ComponentRequirement deepCopyModified(List list) {
        return this;
    }

    @Override
    public void startRequirementCheck(ResultChance contextChance, RecipeCraftingContext context) {

    }

    @Override
    public void endRequirementCheck() {

    }

    @Override
    public JEIComponent provideJEIComponent() {
        return new JEIComponentMana(this);
    }
}
