package dev.headswap.mixin;

import dev.headswap.render.HeadRenderPolicy;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.HeadFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Unico punto de integracion con el renderizador vanilla.
 *
 * HeadFeatureRenderer#render empieza con:
 *     ItemStack itemStack = livingEntity.getEquippedStack(EquipmentSlot.HEAD);
 *     if (itemStack.isEmpty()) return;
 * Sustituimos ese ItemStack local justo tras asignarse:
 *   - EMPTY  -> vanilla sale del render (cabeza oculta)
 *   - otro   -> vanilla dibuja ese skull con TODA su logica (posicion, escala, rotacion, bebes, etc.)
 * No se modifica ninguna otra parte del renderizado del jugador.
 */
@Mixin(HeadFeatureRenderer.class)
public abstract class HeadFeatureRendererMixin {

    @ModifyVariable(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/LivingEntity;FFFFFF)V",
            at = @At(value = "STORE", ordinal = 0),
            ordinal = 0
    )
    private ItemStack headswap$swapHeadStack(ItemStack original,
                                             MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                             LivingEntity entity,
                                             float limbAngle, float limbDistance, float tickDelta,
                                             float animationProgress, float headYaw, float headPitch) {
        if (entity instanceof PlayerEntity player) {
            return HeadRenderPolicy.apply(player, original);
        }
        return original;
    }
}
