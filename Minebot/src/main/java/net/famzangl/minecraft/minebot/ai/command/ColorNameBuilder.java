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

import net.famzangl.minecraft.minebot.ai.AIHelper;
import net.minecraft.item.DyeColor;

import java.util.ArrayList;
import java.util.Collection;

public class ColorNameBuilder extends ParameterBuilder {

	private final static class ColorArgumentDefinition extends
			ArgumentDefinition {
		public ColorArgumentDefinition(String description) {
			super("Color", description);
		}

		@Override
		public boolean couldEvaluateAgainst(String string) {
			for (DyeColor v : DyeColor.values()) {
				if (v.getName().equalsIgnoreCase(string)) {
					return true;
				}
			}
			return false;
		}

		@Override
		public void getTabCompleteOptions(String currentStart,
				Collection<String> addTo) {
			for (final DyeColor color : DyeColor.values()) {
				if (color.getName().toLowerCase().startsWith(currentStart.toLowerCase())) {
					addTo.add(color.getName());
				}
			}
		}
	}

	public ColorNameBuilder(AICommandParameter annot) {
		super(annot);
	}

	@Override
	public void addArguments(ArrayList<ArgumentDefinition> list) {
		list.add(new ColorArgumentDefinition(annot.description()));
	}

	@Override
	public Object getParameter(AIHelper helper, String[] arguments) {
		for (DyeColor v : DyeColor.values()) {
			if (v.getName().equalsIgnoreCase(arguments[0])) {
				return v;
			}
		}
		throw new IllegalArgumentException("Unknown color: " + arguments[0]);
	}
	
	@Override
	protected Class<?> getRequiredParameterClass() {
		return DyeColor.class;
	}

}
