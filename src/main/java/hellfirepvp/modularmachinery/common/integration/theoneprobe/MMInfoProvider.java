package hellfirepvp.modularmachinery.common.integration.theoneprobe;

import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.client.util.EnergyDisplayUtil;
import hellfirepvp.modularmachinery.common.crafting.ActiveMachineRecipe;
import hellfirepvp.modularmachinery.common.crafting.helper.ComponentRequirement;
import hellfirepvp.modularmachinery.common.crafting.helper.CraftingStatus;
import hellfirepvp.modularmachinery.common.crafting.helper.RecipeCraftingContext;
import hellfirepvp.modularmachinery.common.crafting.requirement.RequirementEnergy;
import hellfirepvp.modularmachinery.common.integration.ModIntegrationTOP;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.RecipeThread;
import hellfirepvp.modularmachinery.common.machine.factory.FactoryRecipeThread;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.tiles.TileFactoryController;
import hellfirepvp.modularmachinery.common.tiles.TileMachineController;
import hellfirepvp.modularmachinery.common.tiles.TileParallelController;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import io.netty.util.internal.ThrowableUtil;
import mcjty.theoneprobe.api.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MMInfoProvider implements IProbeInfoProvider {
    @Override
    public String getID() {
        return "modularmachinery:dynamic_machine_info_provider";
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
        if (!blockState.getBlock().hasTileEntity(blockState)) return;

        //获取方块实体
        TileEntity tileEntity = world.getTileEntity(data.getPos());
        if (tileEntity == null) return;
        //判断是否为机械控制器
        if (tileEntity instanceof TileMultiblockMachineController machineController) {
            try {
                processMultiblockMachineTOP(machineController, probeInfo, player);
            } catch (Exception e) {
                ModularMachinery.log.warn(ThrowableUtil.stackTraceToString(e));
            }
        } else if (tileEntity instanceof TileParallelController parallelController) {
            processParallelControllerTOP(parallelController, probeInfo);
        }
    }

    private static void processParallelControllerTOP(TileParallelController parallelController, IProbeInfo probeInfo) {
        if (!ModIntegrationTOP.showParallelControllerInfo) {
            return;
        }
        TileParallelController.ParallelControllerProvider provider = parallelController.provideComponent();
        probeInfo.text(TextFormatting.AQUA + "{*top.parallelism*}" + TextFormatting.GREEN + provider.getParallelism());
        probeInfo.text(TextFormatting.GOLD + "{*top.max_parallelism*}" + TextFormatting.YELLOW + provider.getMaxParallelism());
    }

    private static void processMultiblockMachineTOP(TileMultiblockMachineController machine, IProbeInfo probeInfo, EntityPlayer player) {
        //是否形成结构
        if (machine.isStructureFormed()) {
            probeInfo.text(TextFormatting.GREEN + "{*top.machine.structure.found*}");
        } else {
            probeInfo.text(TextFormatting.RED + "{*top.machine.structure.none*}");
            return;
        }

        if (machine instanceof TileMachineController) {
            processMachineControllerTOP((TileMachineController) machine, probeInfo, player);
        }
        if (machine instanceof TileFactoryController) {
            processFactoryControllerTOP((TileFactoryController) machine, probeInfo, player);
        }
    }

    // TODO: Really long...
    private static void processFactoryControllerTOP(TileFactoryController factory, IProbeInfo probeInfo, EntityPlayer player) {
        if (factory.isWorking()) {
            probeInfo.text(TextFormatting.GREEN + "{*top.machine.working*}");
        } else {
            return;
        }

        List<FactoryRecipeThread> recipeThreads = factory.getFactoryRecipeThreadList();
        Collection<FactoryRecipeThread> coreRecipeThreads = factory.getCoreRecipeThreads().values();

        final AtomicLong energyConsumeTotal = new AtomicLong();
        final AtomicLong energyGenerateTotal = new AtomicLong();
        collectRequirementEnergy(coreRecipeThreads, energyConsumeTotal, energyGenerateTotal);
        collectRequirementEnergy(recipeThreads, energyConsumeTotal, energyGenerateTotal);
        if (energyConsumeTotal.get() > 0) {
            addEnergyUsageText(probeInfo, player, IOType.INPUT, energyConsumeTotal.get());
        }
        if (energyGenerateTotal.get() > 0) {
            addEnergyUsageText(probeInfo, player, IOType.OUTPUT, energyGenerateTotal.get());
        }

        int maxThreads = factory.getMaxThreads();
        if (maxThreads > 0) {
            probeInfo.text(
                    TextFormatting.GREEN + String.valueOf(recipeThreads.size()) +
                            TextFormatting.AQUA + " {*top.factory.thread.running*}" +
                            TextFormatting.RESET + " / " +
                            TextFormatting.YELLOW + maxThreads +
                            TextFormatting.GOLD + " {*top.factory.thread.max*}"
            );
        }

        int trueMaxParallelism = factory.getMaxParallelism();
        if (factory.getAvailableParallelism() != trueMaxParallelism) {
            probeInfo.text(TextFormatting.AQUA + "{*top.parallelism*}" +
                    TextFormatting.GREEN + ((trueMaxParallelism - factory.getAvailableParallelism()) + 1));
            probeInfo.text(TextFormatting.GOLD + "{*top.max_parallelism*}" +
                    TextFormatting.YELLOW + trueMaxParallelism);
        }

        AtomicInteger i = new AtomicInteger();
        List<FactoryRecipeThread> recipeThreadList = new ArrayList<>();
        recipeThreadList.addAll(coreRecipeThreads);
        recipeThreadList.addAll(recipeThreads);

        recipeThreadList.stream().limit(6).forEach(thread -> {
            ActiveMachineRecipe activeRecipe = thread.getActiveRecipe();
            CraftingStatus status = thread.getStatus();

            int progressBarFilledColor = ModIntegrationTOP.recipeProgressBarFilledColor;
            int progressBarAlternateFilledColor = ModIntegrationTOP.recipeProgressBarAlternateFilledColor;
            int progressBarBorderColor = ModIntegrationTOP.recipeProgressBarBorderColor;

            String threadName;
            if (thread.isCoreThread()) {
                threadName = TextFormatting.BLUE + "{*" + thread.getThreadName() + "*}";
                i.getAndIncrement();
            } else {
                threadName = TextFormatting.AQUA + "{*top.factory.thread*}" + i.getAndIncrement();
            }

            if (status.isCrafting()) {
                probeInfo.text(threadName + ": " + TextFormatting.GREEN + "{*" + status.getUnlocMessage() + "*}");
            } else {
                probeInfo.text(threadName + ": " + TextFormatting.RED + "{*" + status.getUnlocMessage() + "*}");
                progressBarFilledColor = ModIntegrationTOP.failureProgressBarFilledColor;
                progressBarAlternateFilledColor = ModIntegrationTOP.failureProgressBarAlternateFilledColor;
                progressBarBorderColor = ModIntegrationTOP.failureProgressBarBorderColor;
            }

            if (activeRecipe == null) {
                return;
            }

            int tick = activeRecipe.getTick();
            int totalTick = activeRecipe.getTotalTick();
            float progress = (float) (tick * 100) / totalTick;

            String progressStr;
            if (player.isSneaking()) {
                //如：20.5 秒 / 40.0 秒
                //Example: 20.5 Sec / 40.0 Sec
                progressStr = String.format("%.1f s / %.1f s", (float) tick / 20, (float) totalTick / 20);
            } else if (ModIntegrationTOP.showRecipeProgressBarDecimalPoints && totalTick >= 1200) {
                //只有当启用了显示小数点且配方耗时超过 1200 tick 才会显示小数点
                progressStr = String.format("%.2f", progress) + "%";
            } else {
                progressStr = String.format("%.0f", progress) + "%";
            }

            IProbeInfo progressLine = probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER));
            progressLine.text(TextFormatting.AQUA + "{*top.recipe.progress*}:  ");
            progressLine.progress((int) progress, 100, probeInfo.defaultProgressStyle()
                    .prefix(progressStr)
                    .filledColor(progressBarFilledColor)
                    .alternateFilledColor(progressBarAlternateFilledColor)
                    .borderColor(progressBarBorderColor)
                    .backgroundColor(ModIntegrationTOP.recipeProgressBarBackgroundColor)
                    .numberFormat(NumberFormat.NONE));
        });
    }

    private static void collectRequirementEnergy(final Collection<FactoryRecipeThread> recipeThreads, final AtomicLong energyConsumeTotal, final AtomicLong energyGenerateTotal) {
        for (final FactoryRecipeThread thread : recipeThreads) {
            if (thread.isIdle()) {
                continue;
            }
            RequirementEnergy req = getRequirementEnergy(thread, IOType.INPUT);
            if (req != null) {
                long required = getEnergyRequired(thread, req);
                energyConsumeTotal.addAndGet(required);
            }
            req = getRequirementEnergy(thread, IOType.OUTPUT);
            if (req != null) {
                long required = getEnergyRequired(thread, req);
                energyGenerateTotal.addAndGet(required);
            }
        }
    }

    // TODO: Really long...
    private static void processMachineControllerTOP(TileMachineController machine, IProbeInfo probeInfo, EntityPlayer player) {
        //是否在工作
        if (machine.getActiveRecipe() == null || machine.getFoundMachine() == null) {
            probeInfo.text(TextFormatting.RED + "{*" + machine.getControllerStatus().getUnlocMessage() + "*}");
            return;
        }

        int progressBarFilledColor = ModIntegrationTOP.recipeProgressBarFilledColor;
        int progressBarAlternateFilledColor = ModIntegrationTOP.recipeProgressBarAlternateFilledColor;
        int progressBarBorderColor = ModIntegrationTOP.recipeProgressBarBorderColor;

        if (machine.getControllerStatus().isCrafting()) {
            probeInfo.text(TextFormatting.GREEN + "{*top.machine.working*}");
        } else {
            probeInfo.text(TextFormatting.RED + "{*" + machine.getControllerStatus().getUnlocMessage() + "*}");
            progressBarFilledColor = ModIntegrationTOP.failureProgressBarFilledColor;
            progressBarAlternateFilledColor = ModIntegrationTOP.failureProgressBarAlternateFilledColor;
            progressBarBorderColor = ModIntegrationTOP.failureProgressBarBorderColor;
        }

        RecipeThread thread = machine.getRecipeThreadList()[0];
        RequirementEnergy req = getRequirementEnergy(thread, IOType.INPUT);
        if (req != null) {
            long energyUsage = getEnergyRequired(thread, req);
            addEnergyUsageText(probeInfo, player, IOType.INPUT, energyUsage);
        }
        req = getRequirementEnergy(thread, IOType.OUTPUT);
        if (req != null) {
            long energyUsage = getEnergyRequired(thread, req);
            addEnergyUsageText(probeInfo, player, IOType.OUTPUT, energyUsage);
        }

        ActiveMachineRecipe activeRecipe = machine.getActiveRecipe();
        int tick = activeRecipe.getTick();
        int totalTick = activeRecipe.getTotalTick();
        float progress = (float) (tick * 100) / totalTick;

        if (activeRecipe.getParallelism() > 1) {
            probeInfo.text(TextFormatting.AQUA + "{*top.parallelism*}" + TextFormatting.GREEN + activeRecipe.getParallelism());
            probeInfo.text(TextFormatting.GOLD + "{*top.max_parallelism*}" + TextFormatting.YELLOW + activeRecipe.getMaxParallelism());
        }

        String progressStr;
        if (player.isSneaking()) {
            //如：20.5 秒 / 40.0 秒
            //Example: 20.5 Sec / 40.0 Sec
            progressStr = String.format("%.1f s / %.1f s", (float) tick / 20, (float) totalTick / 20);
        } else if (ModIntegrationTOP.showRecipeProgressBarDecimalPoints && totalTick >= 1200) {
            //只有当启用了显示小数点且配方耗时超过 1200 tick 才会显示小数点
            progressStr = String.format("%.2f", progress) + "%";
        } else {
            progressStr = String.format("%.0f", progress) + "%";
        }

        IProbeInfo progressLine = probeInfo.horizontal(probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER));
        progressLine.text("{*top.recipe.progress*}:  ");
        progressLine.progress((int) progress, 100, probeInfo.defaultProgressStyle()
                .prefix(progressStr)
                .filledColor(progressBarFilledColor)
                .alternateFilledColor(progressBarAlternateFilledColor)
                .borderColor(progressBarBorderColor)
                .backgroundColor(ModIntegrationTOP.recipeProgressBarBackgroundColor)
                .numberFormat(NumberFormat.NONE)
        );
    }

    private static long getEnergyRequired(RecipeThread thread, RequirementEnergy energy) {
        RecipeCraftingContext context = thread.getContext();
        if (context == null) {
            return 0;
        }

        long reqPerTick = energy.getRequiredEnergyPerTick();
        int parallelism = energy.getParallelism();
        float durationMul = context.getDurationMultiplier();

        return Math.round((RecipeModifier.applyModifiers(context, energy, (double) reqPerTick, false) *
                durationMul *
                parallelism)
        );
    }

    private static RequirementEnergy getRequirementEnergy(RecipeThread thread, IOType ioType) {
        List<ComponentRequirement<?, ?>> energyRequirements = thread.getContext().getRequirementBy(RequirementTypesMM.REQUIREMENT_ENERGY, ioType);
        if (energyRequirements.isEmpty()) {
            return null;
        }

        return (RequirementEnergy) energyRequirements.get(0);
    }

    private static void addEnergyUsageText(final IProbeInfo probe, final EntityPlayer player, final IOType ioType, final long usagePerTick) {
        if (ioType == IOType.INPUT) {
            probe.text(TextFormatting.AQUA + "{*top.energy.input*}" + TextFormatting.RED +
                    formatEnergyUsage(player, usagePerTick) + " {*" + EnergyDisplayUtil.type.getUnlocalizedFormat() + "*}/t");
        } else if (ioType == IOType.OUTPUT) {
            probe.text(TextFormatting.AQUA + "{*top.energy.output*}" + TextFormatting.RED +
                    formatEnergyUsage(player, usagePerTick) + " {*" + EnergyDisplayUtil.type.getUnlocalizedFormat() + "*}/t");
        }
    }

    private static String formatEnergyUsage(final EntityPlayer player, final long usagePerTick) {
        return player.isSneaking()
                ? MiscUtils.formatNumber(EnergyDisplayUtil.type.formatEnergyForDisplay(usagePerTick))
                : MiscUtils.formatDecimal(EnergyDisplayUtil.type.formatEnergyForDisplay(usagePerTick));
    }

}
