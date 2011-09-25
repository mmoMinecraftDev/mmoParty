/*
 * This file is part of mmoMinecraft (https://github.com/mmoMinecraftDev).
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
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package mmo.Party;

import mmo.CoreAPI.MMOListener;
import mmo.DamageAPI.MMODamageEvent;
import org.bukkit.entity.Player;

public class PartyDamage extends MMOListener {

	MMOParty plugin;

	PartyDamage(MMOParty plugin) {
		this.plugin = plugin;
	}

	@Override
	public void onMMOPVPDamage(MMODamageEvent event) {
		if (MMOParty.config_no_party_pvp && Party.isSameParty((Player) event.getAttacker(), (Player) event.getDefender())) {
			if (MMOParty.config_no_party_pvp_quiet) {
				plugin.sendMessage((Player) event.getAttacker(), "Can't attack your own party!");
			}
			event.setCancelled(true);
		}
	}
}
