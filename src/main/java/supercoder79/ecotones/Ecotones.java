package supercoder79.ecotones;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.event.server.ServerTickCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import supercoder79.ecotones.api.BiomeRegistries;
import supercoder79.ecotones.blocks.EcotonesBlocks;
import supercoder79.ecotones.client.particle.EcotonesParticles;
import supercoder79.ecotones.client.sound.Sounds;
import supercoder79.ecotones.command.GetDataAtCommand;
import supercoder79.ecotones.compat.TerrestriaCompat;
import supercoder79.ecotones.compat.TraverseCompat;
import supercoder79.ecotones.items.EcotonesItems;
import supercoder79.ecotones.util.EcotonesBlockPlacers;
import supercoder79.ecotones.world.EcotonesWorldType;
import supercoder79.ecotones.world.biome.EcotonesBiomes;
import supercoder79.ecotones.world.biome.special.BlessedSpringsBiome;
import supercoder79.ecotones.world.decorator.EcotonesDecorators;
import supercoder79.ecotones.world.features.EcotonesFeatures;
import supercoder79.ecotones.world.features.foliage.EcotonesFoliagePlacers;
import supercoder79.ecotones.world.generation.EcotonesBiomeSource;
import supercoder79.ecotones.world.generation.EcotonesChunkGenerator;
import supercoder79.ecotones.world.surface.EcotonesSurfaces;
import supercoder79.ecotones.world.treedecorator.EcotonesTreeDecorators;

import java.util.List;

public class Ecotones implements ModInitializer {
	private static EcotonesWorldType worldType;

	@Override
	public void onInitialize() {
		Sounds.init();

		EcotonesParticles.init();
		EcotonesBlockPlacers.init();
		EcotonesFoliagePlacers.init();
		EcotonesTreeDecorators.init();

        EcotonesBlocks.init();
		EcotonesItems.init();
		EcotonesDecorators.init();
		EcotonesFeatures.init();
		EcotonesSurfaces.init();

		EcotonesBiomes.init();

		// mod compat
		if (FabricLoader.getInstance().isModLoaded("traverse")) {
			TraverseCompat.init();
		}
		if (FabricLoader.getInstance().isModLoaded("terrestria")) {
			TerrestriaCompat.init();
		}

		BiomeRegistries.compile();

		GetDataAtCommand.init();

		//this is stupid but whatever
		int ecotonesBiomes = 0;
		for (Identifier id : Registry.BIOME.getIds()) {
			if (id.getNamespace().contains("ecotones")) {
				ecotonesBiomes++;
			}
		}

		System.out.println("[ecotones] Registering " + ecotonesBiomes + " ecotones biomes!");

		Registry.register(Registry.BIOME_SOURCE, new Identifier("ecotones", "ecotones"), EcotonesBiomeSource.CODEC);
		Registry.register(Registry.CHUNK_GENERATOR, new Identifier("ecotones", "ecotones"), EcotonesChunkGenerator.CODEC);

		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			worldType = new EcotonesWorldType();
		}

		RegistryEntryAddedCallback.event(Registry.BIOME).register((idx, id, biome) -> {
			// Iterate through, search for biomes
			for (List<Identifier> identifiers : BiomeRegistries.DEFERRED_REGISTERS.keySet()) {
				// If the current list contains the biome, check if the others are as well
				if (identifiers.contains(id)) {
					// Ensure all needed biomes are loaded
					boolean run = true;
					for (Identifier identifier : identifiers) {
						if (!Registry.BIOME.containsId(identifier)) {
							run = false;
							break;
						}
					}

					// If everything is good, then run
					if (run) {
						BiomeRegistries.DEFERRED_REGISTERS.get(identifiers).run();
					}
				}
			}
		});

		ServerTickCallback.EVENT.register(server -> {
			if (server.getTicks() % 300 == 0) {
				for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
					if (player.world.getBiome(new BlockPos(player.getPos())) == BlessedSpringsBiome.INSTANCE) {
						player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, player.getRandom().nextInt(200) + 60, 0, false, false, true));
					}
				}
			}
		});
	}
}
