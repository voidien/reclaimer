
    @GameTest(template = "empty", timeoutTicks = 300)
    public static void aMapShowsTheBlocksAroundItAndOnlyResyncsOnAChange(GameTestHelper helper) {
        BlockPos pos = new BlockPos(131_800, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            BlockPos block = pos.offset(10, 0, 4);
            level.setBlock(block, Blocks.RED_CONCRETE.defaultBlockState(), 3);
            projector.setMap();
            int radius = projector.mapRadius();
            int at = HoloMapImage.pixel(4, radius) * HoloMapImage.SIZE + HoloMapImage.pixel(10, radius);
            int red = MapColor.COLOR_RED.getPackedId(MapColor.Brightness.NORMAL) & 0xFF;
            helper.assertTrue((projector.mapImage()[at] & 0xFF) == red, "pixel " + (projector.mapImage()[at] & 0xFF) + ", red is " + red);
            int syncs = projector.syncs();
            projector.refreshMap();
            helper.assertTrue(projector.syncs() == syncs, "the same picture was sent again");
            level.setBlock(block, Blocks.AIR.defaultBlockState(), 3);
            projector.refreshMap();
            helper.assertTrue(projector.mapImage()[at] == 0, "the block is gone but its pixel stayed");
            helper.assertTrue(projector.syncs() == syncs + 1, "the new picture was not sent");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void theMapGoesWithTheBlocksUpdateNotItsSave(GameTestHelper helper) {
        BlockPos pos = new BlockPos(131_900, 80, 0);
        NaniteGameTests.inLoadedArea(helper, pos, 2, () -> {
            ServerLevel level = TestPlayers.deepSpace(helper);
            HoloProjectorBlockEntity projector = place(level, pos);
            projector.setMap();
            HolderLookup.Provider registries = level.registryAccess();
            helper.assertTrue(projector.getUpdateTag(registries).contains("map"), "a client would get no picture");
            helper.assertTrue(!projector.saveWithFullMetadata(registries).contains("map"), "the picture went into the save");
            projector.setLine(0, "text", HoloDisplay.NO_COLOR);
            helper.assertTrue(!projector.getUpdateTag(registries).contains("map"), "text still sends a picture");
            helper.succeed();
        });
    }
