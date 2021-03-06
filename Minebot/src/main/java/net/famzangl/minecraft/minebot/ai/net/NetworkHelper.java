package net.famzangl.minecraft.minebot.ai.net;

import net.minecraft.entity.Entity;

import java.util.List;

public interface NetworkHelper {

	void resetFishState();

	boolean fishIsCaptured(Entity expectedPos);

	void addChunkChangeListener(ChunkListener l);

	void removeChunkChangeListener(ChunkListener l);

	/**
	 * Gets a list of chat messages received since game start.
	 * 
	 * @return The list of chat messages.
	 */
	public List<PersistentChat> getChatMessages();
}
