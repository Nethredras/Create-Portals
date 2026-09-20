package net.nethredras.create_portals.sound;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.DeferredSoundType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.nethredras.create_portals.CreatePortals;

import java.util.function.Supplier;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, CreatePortals.MOD_ID);

    public static final Supplier<SoundEvent> PORTAL_DENIED = registerSoundEvent("portal_denied");
    public static final Supplier<SoundEvent> PORTAL_ENTER1 = registerSoundEvent("portal_enter1");
    public static final Supplier<SoundEvent> PORTAL_ENTER2 = registerSoundEvent("portal_enter2");
    public static final Supplier<SoundEvent> PORTAL_FIRED1 = registerSoundEvent("portal_fired1");
    public static final Supplier<SoundEvent> PORTAL_FIRED2 = registerSoundEvent("portal_fired2");

    // Portal block
    public static final Supplier<SoundEvent> PORTAL_PLACED = registerSoundEvent("portal_placed");
    public static final DeferredSoundType PORTAL_BLOCK_SOUNDS = new DeferredSoundType(1f, 1f,
            ModSounds.PORTAL_PLACED, ModSounds.PORTAL_PLACED, ModSounds.PORTAL_PLACED, ModSounds.PORTAL_PLACED, ModSounds.PORTAL_PLACED);


    private static Supplier<SoundEvent> registerSoundEvent(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CreatePortals.MOD_ID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
