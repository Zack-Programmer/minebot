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
package net.famzangl.minecraft.minebot.ai.path;

import net.famzangl.minecraft.minebot.ai.AIHelper;
import net.famzangl.minecraft.minebot.ai.command.AIChatController;
import net.famzangl.minecraft.minebot.ai.path.world.BlockSet;
import net.famzangl.minecraft.minebot.ai.path.world.BlockSets;
import net.famzangl.minecraft.minebot.ai.path.world.WorldData;
import net.famzangl.minecraft.minebot.ai.task.AITask;
import net.famzangl.minecraft.minebot.ai.task.DestroyInRangeTask;
import net.famzangl.minecraft.minebot.ai.task.DestroyLogInRange;
import net.famzangl.minecraft.minebot.ai.task.RunOnceTask;
import net.famzangl.minecraft.minebot.ai.task.TaskOperations;
import net.famzangl.minecraft.minebot.ai.task.WaitTask;
import net.famzangl.minecraft.minebot.ai.task.error.StringTaskError;
import net.famzangl.minecraft.minebot.ai.task.move.HorizontalMoveTask;
import net.famzangl.minecraft.minebot.ai.task.move.JumpMoveTask;
import net.famzangl.minecraft.minebot.ai.task.place.PlantSaplingTask;
import net.famzangl.minecraft.minebot.ai.utils.BlockCounter;
import net.famzangl.minecraft.minebot.ai.utils.BlockCuboid;
import net.famzangl.minecraft.minebot.build.block.WoodType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Comparator;
import java.util.stream.Stream;

/**
 * This searches for trees (vertical rows of logs), walks to the bottom most and
 * then destroys them.
 * <p>
 * There is special case handling for the big, 2x2 trees. If one of those is
 * found, a staircase all the way up is build. After reaching the top, that
 * staircase is walked downwards destroying all logs above the player on every
 * layer.
 * 
 * @author Michael Zangl
 *
 */
public class TreePathFinder extends MovePathFinder {
	private static final Logger LOGGER = LogManager.getLogger(TreePathFinder.class);
	
	private static class PrefaceBarrier extends AITask {

		@Override
		public boolean isFinished(AIHelper aiHelper) {
			return true;
		}

		@Override
		public void runTick(AIHelper aiHelper, TaskOperations taskOperations) {
		}

		@Override
		public String toString() {
			return "PrefaceBarrier{}";
		}
	}

	private class LargeTreeState {
		/**
		 * This is an intended preface stopper.
		 * 
		 * @author Michael Zangl
		 */
		private class TopReachedTask extends RunOnceTask {

			@Override
			protected void runOnce(AIHelper aiHelper, TaskOperations taskOperations) {
				topReached = true;
			}


			@Override
			public String toString() {
				return "TopReachedTask{" + topReached + "}";
			}
		}

		/**
		 * TODO: Increase for {@link WoodType#SPRUCE}
		 */
		private int treeTopOffset = 1;
		private int singleTreeSideMax = 4;
		private int minX;
		private int minZ;

		/**
		 * The height at which we started. We need to dig down at least to here
		 * when digging down again.
		 */
		private int playerStartY;

		/**
		 * Lowest log for the tree.
		 */
		private int minY;

		/**
		 * How high we should dig. This is the first block that is no trunk any
		 * more.
		 */
		private int topY;

		/**
		 * An offset (0..3) to compute the stairs.
		 */
		private int stairOffset;
		private boolean topReached;
		private WoodType woodType;

		public LargeTreeState(int minX, int minY, int minZ) {
			this.minX = minX;
			this.playerStartY = minY;
			this.minZ = minZ;
		}

		public LargeTreeState(BlockPos p) {
			this(p.getX(), p.getY(), p.getZ());
		}

		/**
		 * The position we need to stand on a stair.
		 * 
		 * @param y
		 * @return
		 */
		private BlockPos getPosition(int y) {
			return new BlockPos(getRelativeX(y) + minX, y, getRelativeZ(y)
					+ minZ);
		}

		private int getRelativeX(int y) {
			return ((y + stairOffset) >> 1) & 1;
		}

		private int getRelativeZ(int y) {
			return ((y + stairOffset + 1) >> 1) & 1;
		}

		public void scanTreeHeight(WorldData world, BlockPos ignoredPlayerPos) {
			minY = topY = playerStartY;
			setYOffsetByPosition(ignoredPlayerPos);
			for (int y = playerStartY; y < 255; y++) {
				BlockPos pos = getPosition(y);
				if (BlockSets.LOGS.isAt(world, pos.add(0, 3, 0))) {
					topY = y + 3;
				} else if (!allowedGroundBlocks.isAt(world, pos.add(0, -1, 0))) {
					break;
				}
			}

			for (int y = playerStartY; y > 0; y--) {
				int trunkBlocks = countTrunkBlocks(world, y);
				BlockPos pos = getPosition(y);
				if (trunkBlocks < 1 || !allowedGroundBlocks.isAt(world, pos.add(0, -1, 0))) {
					break;
				}
				minY = y;
			}
			scanTreeType(world);
		}

		private void scanTreeType(WorldData world) {
			BlockPos corner1 = new BlockPos(minX, minY, minZ);
			BlockPos corner2 = new BlockPos(minX + 1, topY + 2, minZ + 1);
			BlockCuboid<?> trunk = new BlockCuboid<>(corner1, corner2);
			BlockSet[] sets = Stream.of(WoodType.values())
					.map(WoodType::getLogBlocks)
					.toArray(BlockSet[]::new);
			int[] counts = BlockCounter.countBlocks(world, trunk, sets);
			woodType = Stream.of(WoodType.values()).max(Comparator.comparing(type -> counts[type.ordinal()])).orElse(WoodType.JUNGLE);
			
			if (woodType == WoodType.DARK_OAK) {
				singleTreeSideMax = 2;
				treeTopOffset = 2;
			} else if (woodType == WoodType.SPRUCE) {
				singleTreeSideMax = 1;
				treeTopOffset = 3;
			}
		}

		private int countTrunkBlocks(WorldData world, int topY) {
			BlockPos corner = new BlockPos(minX, topY, minZ);
			BlockCuboid<?> trunk = new BlockCuboid<>(corner, corner.add(1, 0, 1));
			int trunkBlocks = BlockCounter.countBlocks(world, trunk, logs)[0];
			return trunkBlocks;
		}

		public int getTreeHeightAbovePlayer() {
			return topY - playerStartY;
		}

		public int getTreeHeight() {
			return topY - minY;
		}

		/**
		 * Sets the height offset so that currentPos is a stair.
		 * 
		 * @param currentPos
		 *            The position.
		 */
		public void setYOffsetByPosition(BlockPos currentPos) {
			for (stairOffset = 0; stairOffset < 4; stairOffset++) {
				if (getPosition(currentPos.getY()).equals(currentPos)) {
					return;
				}
			}
			throw new IllegalArgumentException(currentPos
					+ " is not on the trunk.");
		}

		public void addTasks(AIHelper aiHelper) {
			BlockPos pos = aiHelper.getPlayerPosition();
			if (!isValidPlayerPosition(pos)) {
				// TODO: Print a warning?
				System.err.println("Illegal start position " + pos + " for "
						+ this);
				return;
			}

			// dig up until the top.
			BlockPos lastPos = pos;
			if (!topReached) {
				for (int y = pos.getY() + 1; y < topY - treeTopOffset; y++) {
					BlockPos digTo = getPosition(y);
					if (!BlockSets.safeSideAndCeilingAround(world,
							digTo.add(0, 1, 0))
							|| !BlockSets.safeSideAround(world, digTo)
							|| !BlockSets.SAFE_GROUND.isAt(world,
									digTo.add(0, -1, 0))) {
						break;
					}
					addTask(new JumpMoveTask(digTo, lastPos.getX(),
							lastPos.getZ()));
					lastPos = digTo;
				}
			}
			addTask(new TopReachedTask());
			// now destroy all
			for (int y = lastPos.getY(); y >= minY; y--) {
				BlockPos digTo = getPosition(y);
				addTask(new HorizontalMoveTask(digTo));
				addTask(new PrefaceBarrier());
				// Destroy all logs above y.
				addTask(new DestroyLogInRange(new BlockCuboid<>(
						new BlockPos(minX - singleTreeSideMax, y, minZ
								- singleTreeSideMax), new BlockPos(minX + 1
								+ singleTreeSideMax, y + 4, minZ + 1
								+ singleTreeSideMax))));
			}

			// plant saplings
			if (replant) {
				addReplantTasks(aiHelper);
			}
		}

		private void addReplantTasks(AIHelper aiHelper) {
			for (int i = 0; i < 4; i++) {
				BlockPos floorBase = getPosition(minY + i).add(0, -i, 0);
				if (!BlockSets.SAFE_GROUND.isAt(world, floorBase.add(0, -1, 0))
						|| !BlockSets.safeSideAround(world, floorBase)
						|| !BlockSets.safeSideAndCeilingAround(world,
								floorBase.add(0, 1, 0))) {
					return;
				}
			}

			for (int i = 0; i < 4; i++) {
				BlockPos floorBase = getPosition(minY + i).add(0, -i, 0);
				if (i != 0) {
					addTask(new HorizontalMoveTask(floorBase));
				}
				addTask(new PlantSaplingTask(floorBase, type));
			}
		}

		private boolean isValidPlayerPosition(BlockPos pos) {
			return !(pos.getY() < playerStartY || pos.getY() >= topY || !getPosition(
					pos.getY()).equals(pos));
		}

		@Override
		public String toString() {
			return "LargeTreeState [minX=" + minX + ", minZ=" + minZ
					+ ", minY=" + playerStartY + ", topY=" + topY
					+ ", stairOffset=" + stairOffset + "]";
		}

	}

	public class SwitchToLargeTreeTask extends RunOnceTask {
		private LargeTreeState state;

		public SwitchToLargeTreeTask(LargeTreeState state) {
			this.state = state;
		}

		@Override
		protected void runOnce(AIHelper aiHelper, TaskOperations taskOperations) {
			if (state != null
					&& !state.isValidPlayerPosition(aiHelper.getPlayerPosition())) {
				taskOperations.desync(new StringTaskError("Attempted to start a large tree taks, but player is in a tree."));
				largeTree = null;
			} else {
				LOGGER.debug("Starting with large tree {}", largeTree);
				largeTree = state;
			}
		}

		@Override
		public String toString() {
			return "SwitchToLargeTreeTask{" + "state=" + state + '}';
		}
	}

	private final WoodType type;
	private final BlockSet logs;
	private final boolean replant;
	/**
	 * If this is set, we are cutting a large tree. We don't use the pathfinding
	 * in this case.
	 */
	private LargeTreeState largeTree;

	public TreePathFinder(WoodType type, boolean replant) {
		this.type = type;
		this.replant = replant;
		this.logs = type == null ? BlockSets.LOGS : type.getLogBlocks();
		shortFootBlocks = BlockSet.builder().add(shortFootBlocks).add(BlockSets.LEAVES).build();
		shortHeadBlocks = BlockSet.builder().add(shortHeadBlocks).add(BlockSets.LEAVES).build();
	}

	private static final int TREE_HEIGHT = 7;

	/**
	 * This is a special override that allows us to addd tasks for the large
	 * tree without requireing to pathfind. FIXME: Find a nicer, uniform
	 * solution.
	 * 
	 * @return
	 */
	public boolean addTasksForLargeTree(AIHelper aiHelper) {
		if (largeTree != null) {
			LOGGER.debug("Adding tasks for lage tree {}", largeTree);
			largeTree.addTasks(aiHelper);
			addTask(new SwitchToLargeTreeTask(null));
			return true;
		}
		// Attempt to start a new large tree at our position.
		world = aiHelper.getWorld();
		return handleLargeTree(aiHelper.getPlayerPosition());
	}

	@Override
	protected float rateDestination(int distance, int x, int y, int z) {
		int points = 0;
		if (isLog(x, y, z)) {
			points++;
		}
		if (isLog(x, y + 1, z)) {
			points++;
		}
		for (int i = 2; i < TREE_HEIGHT; i++) {
			if (!BlockSets.safeSideAndCeilingAround(world, x, y + i, z)) {
				break;
			} else if (isLog(x, y + i, z)) {
				points++;
			}
		}

		return points == 0 ? -1 : distance + (TREE_HEIGHT + 5 - points) * 2;
	}

	private boolean isLog(int x, int y, int z) {
		return logs.isAt(world, x, y, z);
	}
	
	

	@Override
	protected void addTasksForTarget(BlockPos currentPos) {
		if (handleLargeTree(currentPos)) {
			// This adds one last task to the task queue.
			return;
		}

		int mineAbove = 0;

		for (int i = 2; i < TREE_HEIGHT; i++) {
			if (isLog(currentPos.getX(), currentPos.getY() + i,
					currentPos.getZ())) {
				mineAbove = i;
			}
		}
		int max = 0;
		for (int i = 2; i <= mineAbove; i++) {
			BlockPos pos = currentPos.add(0, i, 0);
			if (!BlockSets.safeSideAndCeilingAround(world, pos)) {
				break;
			}
			if (!BlockSets.AIR.isAt(world, pos)) {
				max = i;
			}
		}
		if (max > 0) {
			addTask(new DestroyInRangeTask(new BlockCuboid<>(currentPos.add(0, 2,
					0), currentPos.add(0, max, 0))));
		}

		if (replant) {
			addTask(new PlantSaplingTask(currentPos, type));
		}
		addTask(new WaitTask(mineAbove * 2));
	}

	private boolean handleLargeTree(BlockPos currentPos) {
		for (BlockPos pos : new BlockPos[] { currentPos,
				currentPos.add(0, 0, -1), currentPos.add(-1, 0, 0),
				currentPos.add(-1, 0, -1), }) {
			LargeTreeState state = new LargeTreeState(pos);
			state.scanTreeHeight(world, currentPos);
			if (state.getTreeHeight() > 6 || state.getTreeHeightAbovePlayer() > 4) {
				// we are in a large tree that should be handled by this special algorithm.
				state.setYOffsetByPosition(currentPos);
				LOGGER.debug("Found a tree that is large (4x4) and tall at {}, using special handling.", currentPos);
				addTask(new SwitchToLargeTreeTask(state));
				return true;
			}
		}
		return false;
	}
	
	@Override
	protected boolean runSearch(BlockPos playerPosition) {
		if (addTasksForLargeTree(helper)) {
			return true;
		}
		if (BlockSets.AIR.isAt(world, playerPosition.offset(Direction.DOWN))
				&& allowedGroundBlocks.isAt(world, playerPosition.offset(Direction.DOWN, 2))) {
			// Bug: Desync during jump. TODO: Port this to all other pathfinders, find a common way to handle it safely.
			playerPosition = playerPosition.offset(Direction.DOWN);
		}
		return super.runSearch(playerPosition);
	}

	@Override
	protected void noPathFound() {
		super.noPathFound();
		if (statsVisited < 50) {
			AIChatController.addChatLine("No more more trees found. The area the bot can walk to is pretty small.");
		}
	}
}
