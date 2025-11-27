package com.salvaging;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SalvagingPluginTest
{
	public static void main(String[] args) throws Exception
	{
        //noinspection unchecked
        ExternalPluginManager.loadBuiltin(SalvagingPlugin.class);
		RuneLite.main(args);
	}
}