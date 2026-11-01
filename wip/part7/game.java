
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aShipMiniatureIsSavedAndTheLastWriterWins(GameTestHelper helper) {
        BlockPos pos = new BlockPos(131_700, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            projector.setShip();
            helper.assertTrue(projector.display().mode() == HoloDisplay.Mode.SHIP, "mode " + projector.display().mode());
            HolderLookup.Provider registries = level.registryAccess();
            CompoundTag saved = projector.saveWithFullMetadata(registries);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            HoloProjectorBlockEntity restored = place(level, pos);
            restored.loadWithComponents(saved, registries);
            helper.assertTrue(restored.display().mode() == HoloDisplay.Mode.SHIP, "mode after a load " + restored.display().mode());
            restored.setLine(0, "text", HoloDisplay.NO_COLOR);
            helper.assertTrue(restored.display().mode() == HoloDisplay.Mode.TEXT, "text did not replace the ship");
            helper.succeed();
        });
    }
