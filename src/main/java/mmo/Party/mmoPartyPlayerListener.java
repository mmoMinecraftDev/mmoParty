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

import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerKickEvent;

public class mmoPartyPlayerListener extends PlayerListener {

	@Override
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		Party party = Party.find(player);
		if (party == null) {
			//ToDo: Catch this Leak
			new Party(player.getName());
		} else {
			List<Party> invites = Party.findInvites(player);
			if (!invites.isEmpty()) {
				String output = "Invitations from: ";
				boolean first = true;
				for (Party invite : invites) {
					if (!first) {
						output += ", ";
					}
					output += mmoParty.mmo.name(invite.getLeader());
					first = false;
				}
				mmoParty.mmo.sendMessage(player, output);
			}
		}
		Party.update(player);
	}

	@Override
	public void onPlayerQuit(PlayerQuitEvent event) {
		Party party = Party.find(event.getPlayer());
		if (party != null && !party.isParty() && !party.hasInvites()) {
			Party.delete(party);
		}
	}

	@Override
	public void onPlayerKick(PlayerKickEvent event) {
		Party party = Party.find(event.getPlayer());
		if (party != null && !party.isParty() && !party.hasInvites()) {
			Party.delete(party);
		}
	}
}
