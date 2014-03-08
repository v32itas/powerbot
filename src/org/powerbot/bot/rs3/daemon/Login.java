package org.powerbot.bot.rs3.daemon;

import java.awt.Rectangle;

import org.powerbot.misc.GameAccounts;
import org.powerbot.misc.Tracker;
import org.powerbot.script.PollingScript;
import org.powerbot.bot.script.InternalScript;
import org.powerbot.script.Filter;
import org.powerbot.script.rs3.tools.ClientContext;
import org.powerbot.script.rs3.tools.Game;
import org.powerbot.script.rs3.tools.Lobby;
import org.powerbot.script.Random;
import org.powerbot.script.rs3.tools.Component;

/**
 */
public class Login extends PollingScript<ClientContext> implements InternalScript {
	private static final int WIDGET = 596;
	private static final int WIDGET_LOGIN_ERROR = 57;
	private static final int WIDGET_LOGIN_TRY_AGAIN = 84;
	private static final int WIDGET_LOGIN_USERNAME_TEXT = 90;
	private static final int WIDGET_LOGIN_PASSWORD_TEXT = 93;

	public static final String LOGIN_USER_PROPERTY = "login.account.username";

	private boolean isValid() {
		if (ctx.property("login.disable").equals("true")) {
			return false;
		}

		final int state = ctx.game.getClientState();
		return state == -1 || state == Game.INDEX_LOGIN_SCREEN ||
				state == Game.INDEX_LOBBY_SCREEN ||
				state == Game.INDEX_LOGGING_IN;
	}

	@Override
	public void poll() {
		if (!isValid()) {
			priority.set(0);
			return;
		}
		priority.set(4);

		final GameAccounts.Account account = GameAccounts.getInstance().get(ctx.property(LOGIN_USER_PROPERTY));
		final int state = ctx.game.getClientState();

		if (state == Game.INDEX_LOBBY_SCREEN) {
			int world = -1;
			final String w = ctx.property("login.world", "-1");
			try {
				world = Integer.parseInt(w);
			} catch (final NumberFormatException ignored) {
			}

			final Component child = ctx.widgets.get(906, 517); // post email validation continue button
			if (child.isVisible()) {
				child.click();
				return;
			}

			if (world > 0) {
				final Lobby.World world_wrapper;
				if ((world_wrapper = ctx.lobby.getWorld(world)) != null) {
					if (!ctx.lobby.enterGame(world_wrapper) && account != null) {
						final Lobby.World[] worlds = ctx.lobby.getWorlds(new Filter<Lobby.World>() {
							@Override
							public boolean accept(final Lobby.World world) {
								return world.isMembers() == account.member;
							}
						});
						if (worlds.length > 0) {
							ctx.properties.put("login.world", Integer.toString(worlds[Random.nextInt(0, worlds.length)].getNumber()));
						}
					}
					return;
				}
			}
			ctx.lobby.enterGame();
			return;
		}

		if (account != null && (state == Game.INDEX_LOGIN_SCREEN || state == Game.INDEX_LOGGING_IN)) {
			final Component error = ctx.widgets.get(WIDGET, WIDGET_LOGIN_ERROR);
			if (error.isVisible()) {
				final String pre = "scripts/0/login/", txt = error.getText().toLowerCase();
				boolean stop = false;

				if (txt.contains("your ban will be lifted in")) {
					Tracker.getInstance().trackPage(pre + "ban", txt);
					stop = true;
				} else if (txt.contains("account has been disabled")) {
					Tracker.getInstance().trackPage(pre + "disabled", txt);
					stop = true;
				} else if (txt.contains("password") || txt.contains("ended")) {
					stop = true;
				}

				if (stop) {
					ctx.controller.stop();
					return;
				}

				ctx.widgets.get(WIDGET, WIDGET_LOGIN_TRY_AGAIN).click();
				return;
			}

			final String username = account.toString();
			final String password = account.getPassword();
			String text;
			text = getUsernameText();
			if (!text.equalsIgnoreCase(username)) {
				if (!clickLoginInterface(ctx.widgets.get(WIDGET, WIDGET_LOGIN_USERNAME_TEXT))) {
					return;
				}
				try {
					Thread.sleep(600);
				} catch (final InterruptedException ignored) {
				}

				final int length = text.length();
				if (length > 0) {
					final StringBuilder b = new StringBuilder(length);
					for (int i = 0; i < length; i++) {
						b.append('\b');
					}
					ctx.keyboard.send(b.toString());
					return;
				}

				ctx.keyboard.send(username);
				try {
					Thread.sleep(1000);
				} catch (final InterruptedException ignored) {
				}
				return;
			}

			text = getPasswordText();
			if (text.length() != password.length()) {
				if (!clickLoginInterface(ctx.widgets.get(WIDGET, WIDGET_LOGIN_PASSWORD_TEXT))) {
					return;
				}
				try {
					Thread.sleep(600);
				} catch (final InterruptedException ignored) {
				}
				final int length = text.length();
				if (length > 0) {
					final StringBuilder b = new StringBuilder(length);
					for (int i = 0; i < length; i++) {
						b.append('\b');
					}
					ctx.keyboard.send(b.toString());
					return;
				}
				ctx.keyboard.send(password);
				return;
			}

			ctx.keyboard.send("\n");
			try {
				Thread.sleep(1200);
			} catch (final InterruptedException ignored) {
			}
		}
	}

	private boolean clickLoginInterface(final Component i) {
		if (!i.isValid()) {
			return false;
		}
		final Rectangle pos = i.getBoundingRect();
		if (pos.x == -1 || pos.y == -1 || pos.width == -1 || pos.height == -1) {
			return false;
		}
		final int dy = (int) (pos.getHeight() - 4) / 2;
		final int maxRandomX = (int) (pos.getMaxX() - pos.getCenterX());
		final int midx = (int) pos.getCenterX();
		final int h = (int) pos.getHeight();
		final int midy = (int) (pos.getMinY() + (h == 0 ? 27 : h) / 2);
		if (i.getIndex() == WIDGET_LOGIN_PASSWORD_TEXT) {
			return ctx.mouse.click(getPasswordX(i), midy + Random.nextInt(-dy, dy), true);
		}
		return ctx.mouse.click(midx + Random.nextInt(1, maxRandomX), midy + Random.nextInt(-dy, dy), true);
	}

	private int getPasswordX(final Component a) {
		int x = 0;
		final Rectangle pos = a.getBoundingRect();
		final int dx = (int) (pos.getWidth() - 4) / 2;
		final int midx = (int) (pos.getMinX() + pos.getWidth() / 2);
		if (pos.x == -1 || pos.y == -1 || pos.width == -1 || pos.height == -1) {
			return 0;
		}
		for (int i = 0; i < ctx.widgets.get(WIDGET, WIDGET_LOGIN_PASSWORD_TEXT).getText().length(); i++) {
			x += 11;
		}
		if (x > 44) {
			return (int) (pos.getMinX() + x + 15);
		} else {
			return midx + Random.nextInt(-dx, dx);
		}
	}

	private String getUsernameText() {
		return ctx.widgets.get(WIDGET, WIDGET_LOGIN_USERNAME_TEXT).getText().toLowerCase();
	}

	public String getPasswordText() {
		return ctx.widgets.get(WIDGET, WIDGET_LOGIN_PASSWORD_TEXT).getText();
	}
}