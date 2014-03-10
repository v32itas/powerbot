package org.powerbot.bot.os.client;

public interface NpcConfig {
	public int getId();

	public int getLevel();

	public String getName();

	public String[] getActions();

	public int[] getConfigs();

	public int getVarpbitIndex();

	public int getVarbit();
}