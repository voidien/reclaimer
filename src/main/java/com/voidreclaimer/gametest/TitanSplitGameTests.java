package com.voidreclaimer.gametest;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.api.body.Chassis;
import com.voidreclaimer.body.Bodies;
import com.voidreclaimer.body.BodyData;
import com.voidreclaimer.body.BodyRules;
import com.voidreclaimer.body.BodySplit;
import com.voidreclaimer.body.BuiltinBodies;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** A Titan comes apart into a saucer and a drive half, each a body in its own right, and goes back together. */
@GameTestHolder(VoidReclaimer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TitanSplitGameTests {
    private TitanSplitGameTests() {}

    @GameTest(template = "empty")
    public static void aTitanSplitsIntoTwoWorkingBodiesAndJoinsAgain(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        Chassis drive = BuiltinBodies.CHASSIS.get(BuiltinBodies.TITAN_DRIVE);
        BodyData whole = Bodies.preset(BuiltinBodies.TITAN);
        helper.assertTrue(BodySplit.canSplit(whole, drive), "a whole Titan can't split");
        BodySplit.Parts parts = BodySplit.split(whole, drive);
        var saucer = Bodies.preview(parts.rest(), access);
        var half = Bodies.preview(parts.half(), access);
        helper.assertTrue(saucer.chassis().equals(BuiltinBodies.CHASSIS.get(BuiltinBodies.TITAN)), "the saucer is not a Titan any more");
        helper.assertTrue(half.chassis().equals(drive), "the drive half is a " + half.chassis().coreSocket());
        helper.assertTrue(BodyRules.normalize(parts.rest(), saucer.chassis()).returned().isEmpty(), "the saucer lost parts");
        helper.assertTrue(BodyRules.normalize(parts.half(), half.chassis()).returned().isEmpty(), "the drive half lost parts");
        helper.assertTrue(saucer.stats().flight().maxSpeed() > 0.0 && saucer.stats().flight().acceleration() > 0.0, "the saucer can't fly");
        helper.assertTrue(half.stats().flight().maxSpeed() > 0.0 && half.stats().flight().acceleration() > 0.0, "the drive half can't fly");
        helper.assertTrue(half.stats().mass() < Bodies.preview(whole, access).stats().mass(), "the half is as heavy as the whole");
        helper.assertTrue(!BodySplit.canSplit(parts.rest(), drive), "a saucer can split again");
        BodyData back = BodySplit.join(parts.rest(), parts.half());
        helper.assertTrue(back.sections().size() == whole.sections().size() && back.modules().size() == whole.modules().size(), "parts went missing");
        helper.assertTrue(back.equals(whole), "it did not join up as it was");
        helper.succeed();
    }

    /** The chassis file is what the game reads, and the half has to be in it. */
    @GameTest(template = "empty")
    public static void theDriveHalfIsInTheChassisRegistry(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(com.voidreclaimer.api.VoidReclaimerRegistries.CHASSIS);
        Chassis drive = registry.get(BuiltinBodies.TITAN_DRIVE);
        helper.assertTrue(drive != null && drive.halfOf().filter("titan"::equals).isPresent(), "no half in the registry");
        helper.assertTrue(drive.socket("nacelle_l") != null && drive.socket("saucer") == null, "the half has the wrong sockets");
        helper.succeed();
    }
}
