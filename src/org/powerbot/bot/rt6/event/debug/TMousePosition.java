package org.powerbot.bot.rt6.event.debug;

import java.awt.Graphics;
import java.awt.Point;

import org.powerbot.script.TextPaintListener;
import org.powerbot.script.rt6.ClientContext;

import static org.powerbot.bot.rt6.event.debug.DebugHelper.drawLine;

public class TMousePosition implements TextPaintListener {
	private final ClientContext ctx;

	public TMousePosition(final ClientContext ctx) {
		this.ctx = ctx;
	}

	public int draw(int idx, final Graphics render) {
		final Point p = ctx.mouse.getLocation();
		drawLine(render, idx++, "Mouse position: " + (int) p.getX() + "," + (int) p.getY());
		return idx;
	}
}