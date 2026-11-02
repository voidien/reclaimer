package com.voidreclaimer.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.asteroid.AsteroidChunkGenerator;
import com.voidreclaimer.earth.RuinedEarth;
import com.voidreclaimer.space.BarrenChunkGenerator;
import com.voidreclaimer.space.ModDimensions;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** What a chunk keeps when its generator takes the short way: its one biome, in every section, and any decoration a datapack asks for. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BarrenChunkGameTests {
    private static final int CHUNK_X = 21_000;
    /** A section's biomes with one entry on the wire: bits, the entry and an empty storage. A section that grew its palette needs more. */
    private static final int ONE_BIOME_BYTES = 4;

    private BarrenChunkGameTests() {}

    private static void checksEverySection(GameTestHelper helper, ResourceKey<Level> dimension, int chunkZ) {
        ServerLevel level = helper.getLevel().getServer().getLevel(dimension);
        ResourceKey<Biome> biome = dimension == ModDimensions.DEEP_SPACE ? ModDimensions.DEEP_SPACE_BIOME : RuinedEarth.BIOME;
        LevelChunk chunk = level.getChunk(CHUNK_X, chunkZ);
        LevelChunkSection[] sections = chunk.getSections();
        for (int i = 0; i < sections.length; i++) {
            Holder<Biome> found = sections[i].getNoiseBiome(i % 4, 3 - i % 4, 1);
            helper.assertTrue(found.is(biome), "section " + i + " of " + dimension.location() + " is " + found.getRegisteredName());
            int size = sections[i].getBiomes().getSerializedSize();
            helper.assertTrue(size <= ONE_BIOME_BYTES, "section " + i + " of " + dimension.location() + " takes " + size + " bytes for one biome");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void everySectionOfADeepSpaceChunkHasItsOneBiome(GameTestHelper helper) {
        checksEverySection(helper, ModDimensions.DEEP_SPACE, 21_000);
    }

    @GameTest(template = "empty")
    public static void everySectionOfARuinedEarthChunkHasItsOneBiome(GameTestHelper helper) {
        checksEverySection(helper, RuinedEarth.DIMENSION, 21_004);
    }

    @GameTest(template = "empty")
    public static void aSavedChunkReadsBackWithItsOneBiome(GameTestHelper helper) {
        ServerLevel level = TestPlayers.deepSpace(helper);
        LevelChunk chunk = level.getChunk(CHUNK_X, 21_008);
        RegionStorageInfo storage = new RegionStorageInfo(level.getServer().getWorldData().getLevelName(), level.dimension(), "chunk");
        CompoundTag saved = ChunkSerializer.write(level, chunk);
        LevelChunkSection[] sections = ChunkSerializer.read(level, level.getPoiManager(), storage, chunk.getPos(), saved).getSections();
        helper.assertTrue(sections.length == chunk.getSections().length, "read back " + sections.length + " sections");
        for (int i = 0; i < sections.length; i++) {
            Holder<Biome> found = sections[i].getNoiseBiome(1, i % 4, 2);
            helper.assertTrue(found.is(ModDimensions.DEEP_SPACE_BIOME), "section " + i + " read back as " + found.getRegisteredName());
        }
        helper.succeed();
    }

    /** A section that starts with one biome takes a Terraformer's rewrite like any other, half a chunk of it, and saves that. */
    @GameTest(template = "empty")
    public static void aFreshSectionTakesAPartRewriteOfItsBiomes(GameTestHelper helper) {
        ServerLevel level = TestPlayers.deepSpace(helper);
        LevelChunk chunk = level.getChunk(CHUNK_X, 21_012);
        Holder<Biome> plains = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS);
        int top = chunk.getSectionsCount() - 1;
        chunk.getSection(top).fillBiomesFromNoise((x, y, z, sampler) -> (x & 3) < 2 ? plains : chunk.getSection(0).getNoiseBiome(0, 0, 0), null,
                QuartPos.fromBlock(chunk.getPos().getMinBlockX()), QuartPos.fromSection(chunk.getSectionYFromSectionIndex(top)), QuartPos.fromBlock(chunk.getPos().getMinBlockZ()));
        RegionStorageInfo storage = new RegionStorageInfo(level.getServer().getWorldData().getLevelName(), level.dimension(), "chunk");
        LevelChunkSection back = ChunkSerializer.read(level, level.getPoiManager(), storage, chunk.getPos(), ChunkSerializer.write(level, chunk)).getSection(top);
        for (LevelChunkSection section : new LevelChunkSection[] {chunk.getSection(top), back}) {
            helper.assertTrue(section.getNoiseBiome(0, 1, 2).is(Biomes.PLAINS), "the rewritten half is " + section.getNoiseBiome(0, 1, 2).getRegisteredName());
            helper.assertTrue(section.getNoiseBiome(3, 1, 2).is(ModDimensions.DEEP_SPACE_BIOME), "the half left alone is " + section.getNoiseBiome(3, 1, 2).getRegisteredName());
        }
        helper.succeed();
    }

    private static BarrenChunkGenerator generator(GameTestHelper helper, String settings) {
        RegistryOps<com.google.gson.JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
        return new AsteroidChunkGenerator(FlatLevelGeneratorSettings.CODEC.parse(ops, JsonParser.parseString(settings)).getOrThrow());
    }

    @GameTest(template = "empty")
    public static void onlyADimensionWithSomethingToPlaceIsDecorated(GameTestHelper helper) {
        String shape = "{\"biome\":\"" + Biomes.THE_VOID.location() + "\",\"layers\":[],\"lakes\":%s,\"features\":false,\"structure_overrides\":%s}";
        helper.assertFalse(generator(helper, shape.formatted("false", "[]")).decorates(), "nothing to place");
        helper.assertTrue(generator(helper, shape.formatted("true", "[]")).decorates(), "lakes are features");
        helper.assertTrue(generator(helper, shape.formatted("false", "[\"minecraft:villages\"]")).decorates(), "a structure set");
        helper.assertFalse(((BarrenChunkGenerator) TestPlayers.deepSpace(helper).getChunkSource().getGenerator()).decorates(), "deep space as shipped");
        helper.succeed();
    }
}
