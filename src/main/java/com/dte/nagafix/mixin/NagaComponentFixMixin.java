package com.dte.nagafix.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;

@Mixin(targets = "dev.onyxstudios.cca.api.v3.component.ComponentKey", remap = false)
public class NagaComponentFixMixin {

    @Inject(method = "get", at = @At("HEAD"), cancellable = true)
    private void preventNagaCcaCrash(Object provider, CallbackInfoReturnable<Object> cir) {
        if (provider instanceof Entity entity) {
            // Безопасно проверяем, является ли сущность боссом Нагой или её сегментом
            if (entity.getClass().getSimpleName().contains("Naga")) {
                try {
                    // Находим базовый интерфейс Component
                    Class<?> componentClass = Class.forName("dev.onyxstudios.cca.api.v3.component.Component");

                    // Находим внутренний класс Apoli, к которому он пытается привести типы (PowerHolderComponent)
                    Class<?> powerHolderClass = Class.forName("io.github.apace100.apoli.component.PowerHolderComponent");

                    // Создаем динамический прокси, который ОДНОВРЕМЕННО реализует оба интерфейса
                    Object emptyComponent = java.lang.reflect.Proxy.newProxyInstance(
                            powerHolderClass.getClassLoader(),
                            new Class<?>[]{componentClass, powerHolderClass},
                            (proxy, method, args) -> {
                                // Если Apoli запрашивает список сил через методы getPowers() или getPowersFromSource(),
                                // мы возвращаем пустой список, чтобы он думал, что у Наги просто нет эффектов origins.
                                if (method.getReturnType().equals(List.class)) {
                                    return Collections.emptyList();
                                }
                                // Для логических методов (например, hasPower) возвращаем false
                                if (method.getReturnType().equals(boolean.class)) {
                                    return false;
                                }
                                // Для остальных методов (readFromNbt, writeToNbt, modify) возвращаем null / ничего
                                return null;
                            }
                    );

                    // Успешно возвращаем заглушку, которая теперь не вызывает ClassCastException
                    cir.setReturnValue(emptyComponent);

                } catch (ClassNotFoundException e) {
                    // Если класс не найден, просто возвращаем null в качестве запасного варианта
                    cir.setReturnValue(null);
                }
            }
        }
    }
}
