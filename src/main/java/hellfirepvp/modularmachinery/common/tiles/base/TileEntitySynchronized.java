/*******************************************************************************
 * HellFirePvP / Modular Machinery 2019
 *
 * This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.
 * The source code is available on github: https://github.com/HellFirePvP/ModularMachinery
 * For further details, see the License file there.
 ******************************************************************************/

package hellfirepvp.modularmachinery.common.tiles.base;

import hellfirepvp.modularmachinery.ModularMachinery;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nonnull;

/**
 * This class is part of the Modular Machinery Mod
 * The complete source code for this mod can be found on github.
 * Class: TileEntitySynchronized
 * Created by HellFirePvP
 * Date: 28.06.2017 / 17:15
 */
public class TileEntitySynchronized extends TileEntity {
    private boolean inUpdateTask = false;
    private boolean inMarkTask = false;

    private long lastUpdateTick = 0;

    public final void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        readCustomNBT(compound);
    }

    public void readCustomNBT(NBTTagCompound compound) {
    }

    public void readNetNBT(NBTTagCompound compound) {
    }

    @Nonnull
    public final NBTTagCompound writeToNBT(@Nonnull NBTTagCompound compound) {
        NBTTagCompound writeToNBT = super.writeToNBT(compound);
        writeCustomNBT(writeToNBT);
        return writeToNBT;
    }

    public void writeCustomNBT(NBTTagCompound compound) {
    }

    public void writeNetNBT(NBTTagCompound compound) {
    }

    @Nonnull
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound compound = new NBTTagCompound();
        super.writeToNBT(compound);
        writeCustomNBT(compound);
        writeNetNBT(compound);
        return new SPacketUpdateTileEntity(getPos(), 255, compound);
    }

    @Nonnull
    @Override
    public NBTTagCompound getUpdateTag() {
        NBTTagCompound compound = new NBTTagCompound();
        super.writeToNBT(compound);
        writeCustomNBT(compound);
        return compound;
    }

    public final void onDataPacket(@Nonnull NetworkManager manager, @Nonnull SPacketUpdateTileEntity packet) {
        super.onDataPacket(manager, packet);
        readCustomNBT(packet.getNbtCompound());
        readNetNBT(packet.getNbtCompound());
    }

    public void markNoUpdate() {
        markDirty();
        inMarkTask = false;
        lastUpdateTick = getWorld().getTotalWorldTime();
    }

    public void markForUpdate() {
        markNoUpdate();
        notifyUpdate();
    }

    public void notifyUpdate() {
        IBlockState state = world.getBlockState(pos);

        world.notifyBlockUpdate(pos, state, state, 3);
        inUpdateTask = false;
    }

    public void markNoUpdateSync() {
        if (inMarkTask) {
            return;
        }
        ModularMachinery.EXECUTE_MANAGER.addTEMarkNoUpdateTask(this);
        inMarkTask = true;
    }

    /**
     * <p>markForUpdate 的同步实现，防止意料之外的 CME。</p>
     * <p>*** 只能保证 MMCE 自身对世界的线程安全 ***</p>
     */
    public void markForUpdateSync() {
        if (this instanceof SelectiveUpdateTileEntity) {
            markNoUpdateSync();
            return;
        }
        if (inUpdateTask) {
            return;
        }
        ModularMachinery.EXECUTE_MANAGER.addTEUpdateTask(this);
        inUpdateTask = true;
        inMarkTask = true;
    }

    public boolean isInUpdateTask() {
        return inUpdateTask;
    }

    public long getLastUpdateTick() {
        return lastUpdateTick;
    }
}
