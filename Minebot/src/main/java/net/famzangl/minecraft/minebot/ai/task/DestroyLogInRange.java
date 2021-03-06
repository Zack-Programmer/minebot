package net.famzangl.minecraft.minebot.ai.task;

import net.famzangl.minecraft.minebot.ai.AIHelper;
import net.famzangl.minecraft.minebot.ai.path.world.BlockSet;
import net.famzangl.minecraft.minebot.ai.path.world.BlockSets;
import net.famzangl.minecraft.minebot.ai.path.world.WorldData;
import net.famzangl.minecraft.minebot.ai.utils.BlockArea;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

/**
 * This task attemtps to destroy any log in a given area.
 * 
 * @author michael
 *
 */
public class DestroyLogInRange extends DestroyInRangeTask {

	private static final BlockSet LEAVES_OR_LOGS = BlockSet.builder().add(BlockSets.LEAVES).add(BlockSets.LOGS).add(Blocks.VINE).build();

	public DestroyLogInRange(BlockArea<WorldData> range) {
		super(range);
	}

	@Override
	protected boolean noDestructionRequired(WorldData world, int x, int y, int z) {
		if (super.noDestructionRequired(world, x, y, z)) {
			return true;
		} else {
			return !BlockSets.LOGS.isAt(world, x, y, z);
		}
	}

	protected boolean isAcceptedFacingPos(AIHelper aiHelper, BlockPos n, BlockPos pos) {
		return (LEAVES_OR_LOGS.isAt(
						aiHelper.getWorld(), pos) && BlockSets.LOGS.isAt(aiHelper.getWorld(), n));
	}
}
