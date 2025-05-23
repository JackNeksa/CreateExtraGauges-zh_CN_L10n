package net.liukrast.eg.content.logistics.board;

import com.simibubi.create.content.logistics.factoryBoard.*;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.data.IntAttached;
import net.liukrast.eg.api.logistics.board.PanelConnections;
import net.liukrast.eg.api.registry.PanelType;
import net.liukrast.eg.registry.RegisterItems;
import net.liukrast.eg.registry.RegisterPartialModels;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

public class IntPanelBehaviour extends ScrollOptionPanelBehaviour<IntOperationMode> {
    public IntPanelBehaviour(PanelType<?> type, FactoryPanelBlockEntity be, FactoryPanelBlock.PanelSlot slot) {
        super(type, be, slot, IntOperationMode.class);
    }

    @Override
    public void addConnections(PanelConnectionBuilder builder) {
        builder.put(PanelConnections.INTEGER, () -> count);
        builder.put(PanelConnections.REDSTONE, () -> Mth.clamp(count, 0, 15));
    }

    @Override
    public void easyWrite(CompoundTag nbt, boolean clientPacket) {
        super.easyWrite(nbt, clientPacket);
        nbt.putInt("Count", count);
    }

    @Override
    public void easyRead(CompoundTag nbt, boolean clientPacket) {
        super.easyRead(nbt, clientPacket);
        count = nbt.getInt("Count");
    }

    @Override
    public Item getItem() {
        return RegisterItems.INT_GAUGE.get();
    }

    @Override
    public PartialModel getModel(FactoryPanelBlock.PanelState panelState, FactoryPanelBlock.PanelType panelType) {
        return RegisterPartialModels.INT_PANEL;
    }

    @Override
    public void checkForRedstoneInput() {
        if(!active)
            return;
        List<Integer> countList = new ArrayList<>();
        for(FactoryPanelConnection connection : targetedByLinks.values()) {
            if(!getWorld().isLoaded(connection.from.pos())) return;
            FactoryPanelSupportBehaviour linkAt = linkAt(getWorld(), connection);
            if(linkAt == null) return;
            if(!linkAt.isOutput()) continue;
            //TODO: Replace instanceof redstone link with a better way of connection
            if(linkAt.shouldPanelBePowered() && linkAt.blockEntity instanceof RedstoneLinkBlockEntity redstoneLink) {
                countList.add(redstoneLink.getReceivedSignal());
            } else countList.add(linkAt.shouldPanelBePowered() ? 1 : 0);
        }
        for(FactoryPanelConnection connection : targetedBy.values()) {
            if(!getWorld().isLoaded(connection.from.pos())) return;
            FactoryPanelBehaviour at = at(getWorld(), connection);
            if(at == null) return;
            var opt = PanelConnections.getConnectionValue(at, PanelConnections.INTEGER);
            if(opt.isEmpty()) continue;
            countList.add(opt.get());
        }

        int result = get().test(countList.stream());

        //End logical mode
        if(result == count)
            return;

        count = result;
        blockEntity.notifyUpdate();
        /*
        for(FactoryPanelPosition panelPos : targeting) {
            if(!getWorld().isLoaded(panelPos.pos()))
                return;
            FactoryPanelBehaviour behaviour = FactoryPanelBehaviour.at(getWorld(), panelPos);
            if(behaviour == null) continue;
            behaviour.checkForRedstoneInput();
        } TODO: Update instantly?
        */
        notifyRedstoneOutputs();
    }

    @Override
    public IntAttached<MutableComponent> getDisplayLinkComponent() {
        int count = getConnectionValue(PanelConnections.INTEGER).orElse(0);
        return IntAttached.with(count, Component.empty());
    }
}
