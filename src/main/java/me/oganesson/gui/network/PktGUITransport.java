package me.oganesson.gui.network;

import java.util.UUID;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

public class PktGUITransport implements IMessage, IMessageHandler<PktGUITransport, IMessage> {
	private UUID uuid;

	public PktGUITransport() {}

	public PktGUITransport(UUID uuid) {
		this.uuid = uuid;
	}

	@Override
        public void toBytes(ByteBuf buf) {
            buf.writeLong(this.uuid.getMostSignificantBits());
            buf.writeLong(this.uuid.getLeastSignificantBits());
        }

        @Override
        public void fromBytes(ByteBuf buf) {
            final long most = buf.readLong();
            final long least = buf.readLong();
            this.uuid = new UUID(most, least);
        }

        @Override
        public IMessage onMessage(PktGUITransport pkt , MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                EntityPlayerSP player = Minecraft.getMinecraft().player;
                if (player.getPersistentID() == pkt.uuid) return new PktGUITransport(pkt.uuid);
            }
            return null;
        }
}
