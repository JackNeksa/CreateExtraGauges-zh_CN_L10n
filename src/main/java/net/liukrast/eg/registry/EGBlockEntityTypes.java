package net.liukrast.eg.registry;

import com.simibubi.create.content.redstone.displayLink.LinkBulbRenderer;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.liukrast.eg.EGConstants;
import net.liukrast.eg.content.logistics.LinkedLeverBlockEntity;
import net.liukrast.eg.content.logistics.DisplayCollectorBlockEntity;
import net.liukrast.eg.content.logistics.IntSelectorBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.ApiStatus;

@SuppressWarnings("DataFlowIssue")
public class EGBlockEntityTypes {
    private EGBlockEntityTypes() {}
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, EGConstants.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IntSelectorBlockEntity>> INT_SELECTOR = BLOCK_ENTITY_TYPES.register("integer_selector", () -> BlockEntityType.Builder.of(IntSelectorBlockEntity::new, EGBlocks.INT_SELECTOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DisplayCollectorBlockEntity>> DISPLAY_COLLECTOR = BLOCK_ENTITY_TYPES.register("display_collector", () -> BlockEntityType.Builder.of(DisplayCollectorBlockEntity::new, EGBlocks.DISPLAY_COLLECTOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LinkedLeverBlockEntity>> LINKED_LEVER = BLOCK_ENTITY_TYPES.register("linked_lever", () -> BlockEntityType.Builder.of(LinkedLeverBlockEntity::new, EGBlocks.LINKED_LEVER.get(), EGBlocks.LINKED_BUTTON.get()).build(null));

    @ApiStatus.Internal
    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(DISPLAY_COLLECTOR.get(), LinkBulbRenderer::new);
        event.registerBlockEntityRenderer(LINKED_LEVER.get(), SmartBlockEntityRenderer::new);
    }
}
