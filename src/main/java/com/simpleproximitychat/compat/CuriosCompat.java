package com.simpleproximitychat.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

import java.util.List;
import java.util.function.Predicate;

/** Acceso a Curios. Solo se carga si Curios está instalado. */
public final class CuriosCompat {

    private CuriosCompat() {}

    public static List<ItemStack> findStacks(LivingEntity entity, Predicate<ItemStack> filter) {
        return CuriosApi.getCuriosInventory(entity).resolve()
                .map(handler -> handler.findCurios(filter).stream().map(SlotResult::stack).toList())
                .orElse(List.of());
    }
}
