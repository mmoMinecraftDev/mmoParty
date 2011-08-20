/*
 * This file is part of mmoMinecraft (http://code.google.com/p/mmo-minecraft/).
 * 
 * mmoMinecraft is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package mmo.Party;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import mmo.Core.GenericLivingEntity;
import mmo.Core.mmo;
import mmo.Core.mmoPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.SpoutPlayer;

public class perPlayer extends mmoPlayer {

	Party party;
	int health;
	int armor;
	ArrayList<GenericLivingEntity> healthBars = new ArrayList<GenericLivingEntity>();
	ArrayList<LivingEntity> pets = new ArrayList<LivingEntity>();
	//<editor-fold defaultstate="collapsed" desc="Common code">
	private static final Map<String, perPlayer> players = new HashMap<String, perPlayer>();

	protected static perPlayer get(Player player) {
		return get(player.getName());
	}

	protected static perPlayer get(String name) {
		perPlayer data = players.get(name);
		if (data == null) {
			data = new perPlayer();
			data.name = name;
			data.setup();
			if (mmo.hasSpout) {
				data.setupSpout();
			}
			players.put(name, data);
		}
		return data;
	}

	protected static void remove(Player player) {
		remove(player.getName());
	}

	protected static void remove(String name) {
		players.remove(name);
	}

	protected static void clear() {
		players.clear();
	}
	//</editor-fold>

	/**
	 * Setup non-Spout data
	 */
	@Override
	protected void setup() {
		super.setup();
		party = Party.find(name);
	}

	/**
	 * Setup Spout related data
	 */
	@Override
	protected void setupSpout() {
		super.setupSpout();
	}

	protected void getContainer() {
		SpoutPlayer player = SpoutManager.getPlayer(mmo.server.getPlayer(name));
		if (player != null) {
			player.getMainScreen().attachWidget(mmoParty.mmo.plugin, container);
		}
	}

	protected GenericLivingEntity getHealthBar(int index) {
		GenericLivingEntity bar;
		getContainer();
		if (healthBars.size() <= index) {
			healthBars.add(bar = new GenericLivingEntity());
			container.addChild(bar);
		} else {
			bar = healthBars.get(index);
		}
		return bar;
	}

	protected void clearHealthBar(int index) {
		while (healthBars.size() > index) {
			container.removeChild(healthBars.get(index));
			healthBars.remove(index);
		}
	}
}
