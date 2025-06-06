package net.liukrast.eg.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlock;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import net.liukrast.eg.api.logistics.board.AbstractPanelBehaviour;
import net.liukrast.eg.api.logistics.board.PanelBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mixin(FactoryPanelBlock.class)
public abstract class FactoryPanelBlockMixin extends Block {

    public FactoryPanelBlockMixin(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(@NotNull BlockState state, @NotNull HitResult target, @NotNull LevelReader level, @NotNull BlockPos pos, @NotNull Player player) {
        if(level.getBlockEntity(pos) instanceof FactoryPanelBlockEntity be) {
            var location = target.getLocation();
            FactoryPanelBlock.PanelSlot slot = getTargetedSlot(pos, state, location);
            if(be.panels.get(slot) instanceof AbstractPanelBehaviour ab && ab.active) {
                return ab.getItem().getDefaultInstance();
            }
        }
        return super.getCloneItemStack(state, target, level, pos, player);
    }

    @Override
    protected @NotNull List<ItemStack> getDrops(@NotNull BlockState state, LootParams.@NotNull Builder params) {
        return new ArrayList<>();
    }

    @Shadow
    public static FactoryPanelBlock.PanelSlot getTargetedSlot(BlockPos pos, BlockState blockState, Vec3 clickLocation) {
        throw new AssertionError("Mixin injection failed");
    }

    @ModifyExpressionValue(method = "getStateForPlacement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;isClientSide()Z"))
    private boolean getStateForPlacement(boolean original, @Local(argsOnly = true) BlockPlaceContext context, @Local FactoryPanelBlockEntity blockEntity, @Local(ordinal = 1) BlockState state, @Local Vec3 location) {
        if(original) return true;
        if(!(context.getItemInHand().getItem() instanceof PanelBlockItem panelBlockItem)) return false;
        panelBlockItem.applyExtraPlacementData(context, blockEntity, getTargetedSlot(context.getClickedPos(), state, location));
        return true;
    }

    @ModifyArg(method = "lambda$onSneakWrenched$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;placeItemBackInInventory(Lnet/minecraft/world/item/ItemStack;)V"))
    private static ItemStack asStack(ItemStack original, @Local FactoryPanelBehaviour behaviour) {
        if(!(behaviour instanceof AbstractPanelBehaviour abstractPanelBehaviour)) return original;
        return abstractPanelBehaviour.getItem().getDefaultInstance();
    }

    @WrapWithCondition(method = "setPlacedBy", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBlock;withBlockEntityDo(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Ljava/util/function/Consumer;)V"))
    private boolean withBlockEntityDo(FactoryPanelBlock instance, BlockGetter blockGetter, BlockPos pos, Consumer<FactoryPanelBlockEntity> consumer, @Local(argsOnly = true) ItemStack stack, @Local FactoryPanelBlock.PanelSlot initialSlot) {
        if(!(stack.getItem() instanceof PanelBlockItem panelBlockItem)) return true;
        FactoryPanelBlock.class.cast(this).withBlockEntityDo(blockGetter, pos, blockEntity -> panelBlockItem.applyToSlot(blockEntity, initialSlot));
        return false;
    }

    @Inject(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/BlockHitResult;getLocation()Lnet/minecraft/world/phys/Vec3;"), cancellable = true)
    private void useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<ItemInteractionResult> cir) {
        if(!(stack.getItem() instanceof PanelBlockItem panelBlockItem)) return;
        if(level.getBlockEntity(pos) instanceof FactoryPanelBlockEntity panel && panel.restocker) {
            AllSoundEvents.DENY.playOnServer(level, pos);
            player.displayClientMessage(CreateLang.translate("factory_panel.custom_panel_on_restocker")
                    .component(), true);
            cir.setReturnValue(ItemInteractionResult.FAIL);
            return;
        }
        var error = panelBlockItem.isReadyForPlacement(stack, level, pos, player);
        if(error != null) {
            AllSoundEvents.DENY.playOnServer(level, pos);
            player.displayClientMessage(error, true);
            cir.setReturnValue(ItemInteractionResult.FAIL);
        }
    }

    @ModifyExpressionValue(method = "useItemOn", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/entry/BlockEntry;isIn(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean useItemOn(boolean original, @Local(argsOnly = true) ItemStack stack) {
        return original || stack.getItem() instanceof PanelBlockItem;
    }

    @ModifyExpressionValue(method = "useItemOn", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBlockItem;isTuned(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean useItemOn$$1(boolean original, @Local(argsOnly = true) ItemStack stack) {
        return original || stack.getItem() instanceof PanelBlockItem;
    }

    @ModifyExpressionValue(method = "lambda$useItemOn$2", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBlockEntity;addPanel(Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBlock$PanelSlot;Ljava/util/UUID;)Z"))
    private boolean lambda$useItemOn$2(boolean original, @Local(argsOnly = true) ItemStack stack, @Local(argsOnly = true) FactoryPanelBlockEntity blockEntity, @Local(argsOnly = true) FactoryPanelBlock.PanelSlot newSlot) {
        return original || (stack.getItem() instanceof PanelBlockItem blockItem && blockItem.applyToSlot(blockEntity, newSlot));
    }

    @WrapWithCondition(method = "lambda$useItemOn$2", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;displayClientMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private boolean lambda$useItemOn$2(Player instance, Component chatComponent, boolean actionBar, @Local(argsOnly = true) ItemStack stack) {
        if(!(stack.getItem() instanceof PanelBlockItem blockItem)) return true;
        return blockItem.getPlacedMessage() == null;
    }

    @ModifyArg(method = "lambda$useItemOn$2", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;displayClientMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private Component lambda$useItemOn$2(Component message, @Local(argsOnly = true) ItemStack stack) {
        if(!(stack.getItem() instanceof PanelBlockItem blockItem)) return message;
        return blockItem.getPlacedMessage();
    }

    @Unique
    private static ItemStack extra_gauges$stored$itemStack;

    @Inject(method = "lambda$tryDestroySubPanelFirst$3", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBlockEntity;activePanels()I"))
    private static void lambda$tryDestroySubPanelFirst$3(FactoryPanelBlock.PanelSlot destroyedSlot, Player player, Level level, BlockPos pos, FactoryPanelBlockEntity fpbe, CallbackInfoReturnable<InteractionResult> cir) {
        var behaviour = fpbe.panels.get(destroyedSlot);
        if(!(behaviour instanceof AbstractPanelBehaviour panelBehaviour)) extra_gauges$stored$itemStack = null;
        else extra_gauges$stored$itemStack = panelBehaviour.getItem().getDefaultInstance();
    }

    @ModifyArg(method = "lambda$tryDestroySubPanelFirst$3", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBlock;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"))
    private static ItemStack lambda$tryDestroySubPanelFirst$3(ItemStack stack) {
        return extra_gauges$stored$itemStack == null ? stack : extra_gauges$stored$itemStack;
    }

    @ModifyExpressionValue(method = "canBeReplaced", at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/entry/BlockEntry;isIn(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean canBeReplaced(boolean original, @Local(argsOnly = true) BlockPlaceContext context) {
        return original || context.getItemInHand().getItem() instanceof PanelBlockItem;
    }

    @WrapOperation(method = "canBeReplaced", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/logistics/factoryBoard/FactoryPanelBehaviour;isActive()Z"))
    private boolean canBeReplaced(FactoryPanelBehaviour instance, Operation<Boolean> original) {
        if(instance == null) return false;
        return original.call(instance);
    }
}
