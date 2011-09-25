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

import java.util.HashSet;

import mmo.ChatAPI.MMOChatEvent;
import mmo.CoreAPI.MMOListener;
import org.bukkit.entity.Player;

public class PartyChannel extends MMOListener {

	@Override
	public void onMMOChat(MMOChatEvent event) {
		if (event.hasFilter("Party")) {
			Player from = event.getPlayer();
			Party party = Party.find(from);
			HashSet<Player> recipients = (HashSet<Player>) event.getRecipients();
			for (Player to : new HashSet<Player>(recipients)) {
				if (party == null || party != Party.find(to)) {
					recipients.remove(to);
				}
			}
		}
	}
}
