package net.liukrast.eg.content.logistics.board;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import net.liukrast.eg.api.registry.PanelType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public abstract class NumericalScrollPanelBehaviour extends ScrollPanelBehaviour {
    public NumericalScrollPanelBehaviour(Component label, PanelType<?> type, FactoryPanelBlockEntity be, FactoryPanelBlock.PanelSlot slot) {
        super(label, type, be, slot);
        withFormatter(String::valueOf);
    }

    @Override
    public ValueSettings getValueSettings() {
        return new ValueSettings(value < 0 ? 1 : 0, Math.abs(value));
    }

    public MutableComponent formatSettings(ValueSettings settings) {
        return CreateLang.number(settings.value())
                .component();
    }

    @Override
    public String getClipboardKey() {
        return "Numerical";
    }
}
