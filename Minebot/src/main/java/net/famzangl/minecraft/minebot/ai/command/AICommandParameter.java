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
package net.famzangl.minecraft.minebot.ai.command;

import net.famzangl.minecraft.minebot.ai.path.world.BlockSet;
import net.famzangl.minecraft.minebot.ai.path.world.BlockSets;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface AICommandParameter {
	/**
	 * Subclasses need to have an empty constructor.
	 * @author michael
	 *
	 */
	public static abstract class BlockFilter {
		public abstract boolean matches(BlockState block);
	}
	
	public static class AnyBlockFilter extends BlockFilter {
		@Override
		public boolean matches(BlockState b) {
			return true;
		}
	}
	
	public static class SurvivalBlockFilter extends BlockFilter {
		private static final BlockSet NON_SURVIVAL_BLOCKS = BlockSet.builder().add(Blocks.COMMAND_BLOCK).add(BlockSets.AIR).build();

		@Override
		public boolean matches(BlockState b) {
			return !NON_SURVIVAL_BLOCKS.contains(b);
		}
	}
	
	ParameterType type();

	String description();

	String fixedName() default "";
	
	boolean optional() default false;

	Class<? extends BlockFilter> blockFilter() default SurvivalBlockFilter.class;

	String relativeToSettingsFile() default "";
}
