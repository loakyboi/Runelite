package com.flippingutilities;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class FlippingPluginTest
{

	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(FlippingPlugin.class);
		RuneLite.main(args);

	}
}