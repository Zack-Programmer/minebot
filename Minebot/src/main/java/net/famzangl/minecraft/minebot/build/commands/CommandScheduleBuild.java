/*******************************************************************************
 * This file is part of Minebot.
 *
 * Minebot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Minebot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Minebot.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package net.famzangl.minecraft.minebot.build.commands;

import net.famzangl.minecraft.minebot.ai.AIHelper;
import net.famzangl.minecraft.minebot.ai.command.AICommand;
import net.famzangl.minecraft.minebot.ai.command.AICommandInvocation;
import net.famzangl.minecraft.minebot.ai.command.AICommandParameter;
import net.famzangl.minecraft.minebot.ai.command.AICommandParameter.BlockFilter;
import net.famzangl.minecraft.minebot.ai.command.CommandEvaluationException;
import net.famzangl.minecraft.minebot.ai.command.ParameterType;
import net.famzangl.minecraft.minebot.ai.path.world.BlockSet;
import net.famzangl.minecraft.minebot.ai.strategy.AIStrategy;
import net.famzangl.minecraft.minebot.ai.strategy.RunOnceStrategy;
import net.famzangl.minecraft.minebot.build.blockbuild.BlockBuildTask;
import net.famzangl.minecraft.minebot.build.blockbuild.BuildNormalStairsTask;
import net.famzangl.minecraft.minebot.build.blockbuild.BuildTask;
import net.famzangl.minecraft.minebot.build.blockbuild.FenceBuildTask;
import net.famzangl.minecraft.minebot.build.blockbuild.LogBuildTask;
import net.famzangl.minecraft.minebot.build.blockbuild.SlabBuildTask;
import net.famzangl.minecraft.minebot.build.blockbuild.StandingSignBuildTask;
import net.famzangl.minecraft.minebot.build.blockbuild.StandingSignBuildTask.SignDirection;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

@AICommand(helpText = "Schedules a build task.", name = "minebuild")
public class CommandScheduleBuild {

	public static final BlockSet SIMPLE_WHITELIST = BlockSet.builder().add(BlockBuildTask.BLOCKS)
			.add(FenceBuildTask.BLOCKS)
			.add(LogBuildTask.NORMAL_LOGS)
			.add(SlabBuildTask.BLOCKS).build();

	private static final class ScheduleTaskStrategy extends RunOnceStrategy {
		private final BuildTask task;

		private ScheduleTaskStrategy(BuildTask task) {
			this.task = task;
		}

		@Override
		protected void singleRun(AIHelper helper) {
			helper.buildManager.addTask(task);
		}
	}

	public static final class RunSimpleFilter extends BlockFilter {
		@Override
		public boolean matches(BlockState b) {
			return SIMPLE_WHITELIST.contains(b);
		}
	}

	@AICommandInvocation()
	public static AIStrategy runSimple(
			AIHelper helper,
			@AICommandParameter(type = ParameterType.FIXED, fixedName = "schedule", description = "") String nameArg,
			@AICommandParameter(type = ParameterType.POSITION, description = "Where to place it (relative is to your current pos)") BlockPos forPosition,
			@AICommandParameter(type = ParameterType.BLOCK_STATE, description = "The block", blockFilter = RunSimpleFilter.class) BlockState blockToPlace) {
		final BuildTask task;
		if (BlockBuildTask.BLOCKS.contains(blockToPlace)) {
			task = new BlockBuildTask(forPosition, blockToPlace);
		} else if (LogBuildTask.NORMAL_LOGS.contains(blockToPlace)) {
			task = new LogBuildTask(forPosition, blockToPlace);
		} else if (FenceBuildTask.BLOCKS.contains(blockToPlace)) {
			task = new FenceBuildTask(forPosition, blockToPlace);
		} else if (SlabBuildTask.BLOCKS.contains(blockToPlace)) {
			task = new SlabBuildTask(forPosition, blockToPlace);
		} else {
			throw new CommandEvaluationException("Cannot build " + blockToPlace);
		}
		return new ScheduleTaskStrategy(task);
	}


	// public static final class RunColoredFilter extends BlockFilter {
	// @Override
	// public boolean matches(Block b) {
	// return ColoredCubeBuildTask.BLOCKS.contains(b);
	// }
	// }

	// TODO: This is the task of BLOCK_NAME
	// @AICommandInvocation()
	// public static AIStrategy run(
	// AIHelper helper,
	// @AICommandParameter(type = ParameterType.FIXED, fixedName = "schedule",
	// description = "") String nameArg,
	// @AICommandParameter(type = ParameterType.POSITION, description =
	// "Where to place it (relative is to your current pos)") BlockPos
	// forPosition,
	// @AICommandParameter(type = ParameterType.BLOCK_NAME, description =
	// "The block", blockFilter = RunColoredFilter.class)
	// BlockState blockToPlace,
	// @AICommandParameter(type = ParameterType.COLOR, description =
	// "The color") DyeColor color) {
	// if (ColoredCubeBuildTask.BLOCKS.contains(blockToPlace)) {
	// addTask(helper, new ColoredCubeBuildTask(forPosition, blockToPlace,
	// color));
	// } else {
	// throw new CommandEvaluationException("Cannot build " + blockToPlace);
	// }
	// return null;
	// }

	// public static final class WoodBlockFilter extends BlockFilter {
	// @Override
	// public boolean matches(BlockState b) {
	// return WoodBuildTask.BLOCKS.contains(b);
	// }
	// }

	// @AICommandInvocation()
	// public static AIStrategy run(
	// AIHelper helper,
	// @AICommandParameter(type = ParameterType.FIXED, fixedName = "schedule",
	// description = "") String nameArg,
	// @AICommandParameter(type = ParameterType.POSITION, description =
	// "Where to place it (relative is to your current pos)") BlockPos
	// forPosition,
	// @AICommandParameter(type = ParameterType.BLOCK_NAME, description =
	// "The block type to place", blockFilter = WoodBlockFilter.class) Block
	// blockToPlace,
	// @AICommandParameter(type = ParameterType.ENUM, description =
	// "The wood subtype to place") WoodType woodType) {
	// if (WoodBuildTask.BLOCKS.contains(blockToPlace)) {
	// addTask(helper, new WoodBuildTask(forPosition, woodType));
	// } else {
	// throw new CommandEvaluationException("Cannot build " + blockToPlace);
	// }
	// return null;
	// }

	public static final class LogBlockFilter extends BlockFilter {
		@Override
		public boolean matches(BlockState b) {
			return LogBuildTask.BLOCKS.contains(b);
		}
	}

	// @AICommandInvocation()
	// public static AIStrategy run(
	// AIHelper helper,
	// @AICommandParameter(type = ParameterType.FIXED, fixedName = "schedule",
	// description = "") String nameArg,
	// @AICommandParameter(type = ParameterType.POSITION, description =
	// "Where to place it (relative is to your current pos)") BlockPos
	// forPosition,
	// @AICommandParameter(type = ParameterType.BLOCK_NAME, description =
	// "The block", blockFilter = LogBlockFilter.class) Block blockToPlace,
	// @AICommandParameter(type = ParameterType.ENUM, description =
	// "The type of wood logs") WoodType woodType,
	// @AICommandParameter(type = ParameterType.ENUM, description =
	// "The direction the log is facing") Direction direction) {
	// if (LogBuildTask.BLOCKS.contains(blockToPlace)) {
	// addTask(helper, new LogBuildTask(forPosition, woodType, direction));
	// } else {
	// throw new CommandEvaluationException("Cannot build " + blockToPlace);
	// }
	// return null;
	// }

	public static final class StairsBlockFilter extends BlockFilter {
		@Override
		public boolean matches(BlockState b) {
			return BuildNormalStairsTask.BLOCKS.contains(b);
		}
	}

	// TODO: Merge with Factory

//	@AICommandInvocation()
//	public static AIStrategy run(AIHelper helper,
//			@AICommandParameter(type = ParameterType.FIXED, fixedName = "schedule", description = "") String nameArg,
//			@AICommandParameter(type = ParameterType.POSITION, description = "Where to place it (relative is to your current pos)") BlockPos forPosition,
//			@AICommandParameter(type = ParameterType.BLOCK_NAME, description = "The block", blockFilter = StairsBlockFilter.class) BlockState blockToPlace,
//			@AICommandParameter(type = ParameterType.ENUM, description = "The direction the stairs face") Direction direction,
//			@AICommandParameter(type = ParameterType.ENUM, description = "Upper for inverted stairs", optional = true) EnumHalf half) {
//		if (BuildNormalStairsTask.BLOCKS.contains(blockToPlace)) {
//			addTask(helper,
//					new BuildNormalStairsTask(forPosition, blockToPlace.getBlock(), direction, half == null ? EnumHalf.BOTTOM : half));
//		} else {
//			throw new CommandEvaluationException("Cannot build " + blockToPlace);
//		}
//		return null;
//	}

	public static final class SignBlockFilter extends BlockFilter {
		@Override
		public boolean matches(BlockState b) {
			return StandingSignBuildTask.BLOCKS.contains(b);
		}
	}

	@AICommandInvocation()
	public static AIStrategy run(
			AIHelper helper,
			@AICommandParameter(type = ParameterType.FIXED, fixedName = "schedule", description = "") String nameArg,
			@AICommandParameter(type = ParameterType.POSITION, description = "Where to place it (relative is to your current pos)") BlockPos forPosition,
			@AICommandParameter(type = ParameterType.BLOCK_STATE, description = "The block", blockFilter = SignBlockFilter.class) BlockState blockToPlace,
			@AICommandParameter(type = ParameterType.ENUM, description = "The direction of the sign") SignDirection direction,
			@AICommandParameter(type = ParameterType.STRING, description = "The first line. Replace space with §.", optional = true) String text1,
			@AICommandParameter(type = ParameterType.STRING, description = "The second line. Replace space with §.", optional = true) String text2,
			@AICommandParameter(type = ParameterType.STRING, description = "The third line. Replace space with §.", optional = true) String text3,
			@AICommandParameter(type = ParameterType.STRING, description = "The fourth line. Replace space with §.", optional = true) String text4) {
		if (StandingSignBuildTask.BLOCKS.contains(blockToPlace)) {
			StandingSignBuildTask task = new StandingSignBuildTask(forPosition, direction,
					new String[] { text1, text2, text3, text4 });
			return new ScheduleTaskStrategy(task);
		} else {
			throw new CommandEvaluationException("Cannot build " + blockToPlace);
		}
	}
}
