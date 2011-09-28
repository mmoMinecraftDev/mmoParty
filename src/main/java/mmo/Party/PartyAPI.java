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

import mmo.Core.util.ArrayListString;
import mmo.Core.MMO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mmo.Core.PartyAPI.Party;
import mmo.Core.gui.GenericLivingEntity;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.getspout.spoutapi.gui.Container;
import org.getspout.spoutapi.gui.Widget;

public class PartyAPI implements Party {

	/**
	 * Singleton instance - only for finding parties
	 */
	public final static PartyAPI instance = new PartyAPI();
	// ...and now the class...
	protected static MMOParty plugin;
	/**
	 * All the active parties, use "party.remove()" where needed.
	 */
	private static final ArrayList<PartyAPI> parties = new ArrayList<PartyAPI>();
	/**
	 * The names of all members of this party.
	 */
	private final ArrayListString members = new ArrayListString();
	/**
	 * The names of all players invited to this party.
	 */
	private final ArrayListString invites = new ArrayListString();
	/**
	 * The name of the party leader.
	 */
	protected String leader;
	/**
	 * A map of player containers, each container is their party bar
	 */
	protected static HashMap<Player, Container> containers = new HashMap<Player, Container>();

	/**
	 * Constructor.
	 */
	private PartyAPI() {
		this("", "", "");
	}

	/**
	 * Constructor.
	 * @param names The player names to add
	 */
	private PartyAPI(String leader) {
		this(leader, leader, "");
	}

	/**
	 * Constructor.
	 * @param names The player names to add
	 */
	private PartyAPI(String leader, String names) {
		this(leader, names, "");
	}

	/**
	 * Constructor.
	 * @param names The player names to add
	 * @param invites The player names to invite
	 */
	private PartyAPI(String leader, String names, String invite) {
		this.leader = leader;
		if (!names.equals("")) {
			members.addAll(Arrays.asList(names.split(",")));
		}
		if (!invite.equals("")) {
			invites.addAll(Arrays.asList(invite.split(",")));
		}
	}

	/**
	 * Load all parties and invites.
	 */
	protected static void loadAll() {
		try {
			for (PartyDB row : plugin.getDatabase().find(PartyDB.class).setAutofetch(true).findList()) {
				parties.add(new PartyAPI(row.getLeader(), row.getMembers(), row.getInvites())); // Make sure we can store the new party
			}
		} catch (Exception e) {
		}
	}

	/**
	 * Save all parties and invites.
	 */
	protected static void saveAll() {
		for (PartyAPI party : parties) {
			party.save();
		}
	}

	/**
	 * Save the party.
	 */
	protected void save() {
		PartyDB row = plugin.getDatabase().find(PartyDB.class).where().ieq("leader", leader).findUnique();
		if (isParty() || !invites.isEmpty()) {
			if (row == null) {
				row = new PartyDB();
				row.setLeader(leader);
			}
			row.setMembers(getMemberNames());
			row.setInvites(getInviteNames());
			plugin.getDatabase().save(row);
		} else if (row != null) {
			plugin.getDatabase().delete(row);
		}
	}

	/**
	 * Delete a Party from the global list.
	 * @param party The Party to remove
	 */
	public static void delete(PartyAPI party) {
		if (party != null && parties.contains(party)) {
			parties.remove(parties.indexOf(party));
		}
	}

	@Override
	public PartyAPI find(Player player) {
		return find(player.getName());
	}

	@Override
	public PartyAPI find(String player) {
		for (PartyAPI party : parties) {
			if (party.members.contains(player)) {
				return party;
			}
		}
		PartyAPI party = new PartyAPI(player);
		parties.add(party); // Make sure we can store the new party
		return party;
	}

	@Override
	public boolean contains(Player player) {
		if (player != null && find(player) == this) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isParty() {
		return (members.size() > 1);
	}

	@Override
	public boolean hasInvites() {
		return !invites.isEmpty();
	}

	@Override
	public List<Party> findInvites(Player player) {
		ArrayList<Party> list = new ArrayList<Party>();
		for (PartyAPI party : parties) {
			if (party.invites.contains(player.getName())) {
				list.add(party);
			}
		}
		return list;
	}

	@Override
	public void declineInvites(Player player) {
		for (PartyAPI party : parties) {
			if (party.invites.contains(player.getName())) {
				party.invites.remove(party.invites.indexOf(player.getName()));
				party.save();
			}
		}
	}

	@Override
	public List<Player> getMembers(String name) {
		return getMembers(plugin.getServer().getPlayer(name));
	}

	@Override
	public List<Player> getMembers(Player player) {
		List<Player> players = getMembers();
		if (player != null && players.contains(player)) {
			players.remove(player);
		}
		return players;
	}

	@Override
	public List<Player> getMembers() {
		ArrayList<Player> players = new ArrayList<Player>();
		for (String name : members) {
			Player player = plugin.getServer().getPlayer(name);
			if (player != null && player.isOnline()) {
				players.add(player);
			}
		}
		return players;
	}

	@Override
	public String getMemberNames() {
		String names = "";
		boolean first = true;
		for (String name : members) {
			names += (first ? "" : ",") + name;
			first = false;
		}
		return names;
	}

	@Override
	public String getInviteNames() {
		String names = "";
		boolean first = true;
		for (String name : invites) {
			names += (first ? "" : ",") + name;
			first = false;
		}
		return names;
	}

	@Override
	public int size() {
		return members.size();
	}

	@Override
	public boolean accept(Player player) {
		if (player == null) {
			return false;
		}
		PartyAPI party = find(player);
		if (party != null && party.isParty()) {
			plugin.sendMessage(player, "You are already in a party.");
			return false;
		}
		if (!invites.contains(player.getName())) {
			plugin.sendMessage(player, "You haven't been invited.");
			return false;
		}
		invites.remove(invites.indexOf(player.getName()));
		if (members.size() >= MMOParty.config_max_party_size) {
			plugin.sendMessage(player, "There isn't any space for you.");
			return false;
		}
		declineInvites(player); // Make sure they have no outstanding invites from anywhere else
		if (party != null) { // Only if they're the only member - and were sending out invites
			PartyAPI.delete(party);
		}
		// Note the order - send to everyone in the party so the new member gets a custom msg
		plugin.sendMessage(getMembers(), "%s has joined the party.", MMO.name(player.getName()));
		plugin.notify(getMembers(), "%s joined", MMO.name(player.getName()));
		plugin.sendMessage(player, "You have joined a party.");
		plugin.notify(player, "Joined %s", MMO.name(leader));
		members.add(player.getName());
		update();
		save();
		return true;
	}

	@Override
	public boolean decline(Player player) {
		if (player != null && invites.contains(player.getName())) {
			invites.remove(invites.indexOf(player.getName()));
			plugin.sendMessage(player, "Declined invitation from %s.", MMO.name(leader));
			save();
			return true;
		}
		return false;
	}

	@Override
	public boolean remove(String name) {
		if (members.contains(name)) {
			members.remove(members.indexOf(name));
			if (members.isEmpty()) {
				PartyAPI.delete(this);
			} else {
				update();
			}
			updateAll(name);
			save();
			return true;
		}
		return false;
	}

	@Override
	public boolean promote(Player leader, String name) {
		if (!isLeader(leader)) {
			plugin.sendMessage(leader, "You are not the party leader.");
			return false;
		}
		if (!isParty()) {
			plugin.sendMessage(leader, "You are not in a party.");
			return false;
		}
		if (!members.contains(name)) {
			plugin.sendMessage(leader, "%s is not in your party.", MMO.name(name));
			return false;
		}
		if (isLeader(name)) {
			plugin.sendMessage(leader, "You are already the party leader.");
			return false;
		}
		this.leader = members.get(name);
		plugin.notify(name, "Promoted to leader");
		plugin.notify(getMembers(name), "%s promoted", MMO.name(this.leader));
		plugin.sendMessage(leader, "Promoted %s to leader.", MMO.name(this.leader));
		plugin.sendMessage(name, "You have been promoted to leader.");
		update();
		save();
		return true;
	}

	@Override
	public boolean leave(Player player) {
		if (remove(player.getName())) {
			plugin.sendMessage(player, "You have left your party.");
			plugin.sendMessage(getMembers(), "%s has left the party.", MMO.name(player.getName()));
			plugin.notify(getMembers(), "%s left", MMO.name(player.getName()));
			if (isLeader(player)) {
				leader = members.get(0);
				plugin.sendMessage(leader, "You are now the party leader");
				plugin.notify(leader, "Promoted to leader");
				plugin.notify(getMembers(leader), "%s is now leader", MMO.name(leader));
			}
			save();
			return true;
		}
		return false;
	}

	@Override
	public boolean kick(Player leader, String name) {
		if (!isLeader(leader)) {
			plugin.sendMessage(leader, "You are not the party leader.");
			return false;
		}
		if (isLeader(name)) {
			plugin.sendMessage(leader, "You cannot kick yourself.");
			return false;
		}
		if (!members.contains(name)) {
			plugin.sendMessage(leader, "%s is not in your party.", MMO.name(name));
			return false;
		}
		name = members.get(name);
		if (!remove(name)) {
			plugin.sendMessage(leader, "Unable to remove them...");
			return false;
		}
		plugin.sendMessage(getMembers(), "%s has been kicked out of the party.", MMO.name(name));
		plugin.sendMessage(name, "You have been kicked from the party.");
		plugin.notify(getMembers(), "%s kicked", MMO.name(name));
		save();
		return true;
	}

	@Override
	public boolean isLeader(Player player) {
		return isLeader(player.getName());
	}

	@Override
	public boolean isLeader(String name) {
		return name != null && leader.equalsIgnoreCase(name);
	}

	@Override
	public String getLeader() {
		return leader;
	}

	@Override
	public boolean invite(Player leader, String name) {
		if (!isLeader(leader)) {
			plugin.sendMessage(leader, "You are not the party leader.");
			return false;
		}
		Player player = plugin.getServer().getPlayer(name);
		if (player == null) {
			plugin.sendMessage(leader, "%s isn't online, is it spelt correctly?", MMO.name(name));
			return false;
		}
		if (player.equals(leader)) {
			plugin.sendMessage(leader, "You cannot invite yourself.");
			return false;
		}
		if (members.size() >= MMOParty.config_max_party_size) {
			plugin.sendMessage(leader, "You don't have space in your party.");
			return false;
		}
		PartyAPI party = find(player);
		if (party != null && party.size() > 1) {
			if (this == party) {
				plugin.sendMessage(leader, "They are already in your party.");
			} else {
				plugin.sendMessage(leader, "They are already in a party.");
			}
			return false;
		}
		if (invites.contains(player.getName())) {
			plugin.sendMessage(leader, "They have already been invited.");
			return false;
		}
		invites.add(player.getName());
		plugin.sendMessage(player, "You have been invited to a join party by %s\nTo accept type: /party accept %s", MMO.name(this.leader), this.leader);
		plugin.sendMessage(leader, "You have invited %s", MMO.name(player.getName()));
		plugin.notify(player, "Invite from %s", MMO.name(leader.getName()));
		save();
		return true;
	}

	/**
	 * Update all party members in Player's party.
	 */
	public static void updateAll(String name) {
		updateAll(plugin.getServer().getPlayer(name));
	}

	/**
	 * Update all party members in Player's party.
	 * @param player The Player to update
	 */
	public static void updateAll(Player player) {
		if (player != null) {
			PartyAPI party = instance.find(player);
			party.update();
		}
	}

	/**
	 * Update all parties.
	 */
	public static void updateAll() {
		for (PartyAPI party : parties) {
			party.update();
		}
	}

	@Override
	public void update() {
		if (MMOParty.hasSpout) {
			for (Player player : getMembers()) {
				update(player);
			}
		}
	}

	@Override
	public void update(Player player) {
		if (MMOParty.hasSpout) {
			Container container = containers.get(player);

			if (container != null) {
				int index = 0;
				Widget[] bars = container.getChildren();
				if (members.size() > 1 || MMOParty.config_always_show) {
					for (String name : members.meFirst(player.getName())) {
						GenericLivingEntity bar;
						if (index >= bars.length) {
							container.addChild(bar = new GenericLivingEntity());
						} else {
							bar = (GenericLivingEntity) bars[index];
						}
						bar.setEntity(name, isLeader(name) ? ChatColor.GREEN + "@" : "");
						bar.setTargets(MMOParty.config_show_pets ? MMO.getPets(plugin.getServer().getPlayer(name)) : null);
						index++;
					}
				}
				while (index < bars.length) {
					container.removeChild(bars[index++]);
				}
				container.updateLayout();
			}
		}
	}

	/**
	 * Get an array of member_name:status strings, used for /party status
	 * @return 
	 */
	private HashMap<String, String> getStatus() {
		HashMap<String, String> status = new HashMap<String, String>();
		Map<String, Tameable> pets = new HashMap<String, Tameable>();
		String output = "";

		if (MMOParty.config_show_pets) {
			for (World world : plugin.getServer().getWorlds()) {
				for (LivingEntity entity : world.getLivingEntities()) {
					if (entity instanceof Tameable && ((Tameable) entity).isTamed() && ((Tameable) entity).getOwner() instanceof Player) {
						String name = ((Player) ((Tameable) entity).getOwner()).getName();
						if (members.contains(name)) {
							pets.put(name, (Tameable) entity);
						}
					}
				}
			}
		}
		for (String member : members) {
			Player player = plugin.getServer().getPlayer(member);
			if (player == null) {
				output = MMO.makeBar(ChatColor.BLACK, 0) + MMO.makeBar(ChatColor.BLACK, 0) + ChatColor.DARK_GRAY + member;
			} else {
				output = MMO.makeBar(ChatColor.RED, MMO.getHealth(player)) + MMO.makeBar(ChatColor.WHITE, MMO.getArmor(player));
				output += (isLeader(member) ? ChatColor.GREEN + "@" : "") + MMO.name(player.getName());
				if (pets.containsKey(member)) {
					Tameable pet = pets.get(member);
					if (player.getName().equals(((Player) pet.getOwner()).getName())) {
						output += "\n" + MMO.makeBar(ChatColor.RED, MMO.getHealth((Entity) pet)) + MMO.makeBar(ChatColor.BLACK, 0);
						output += ChatColor.WHITE + "+ " + ChatColor.AQUA + " " + MMO.getSimpleName((LivingEntity) pet, false);
					}
				}
			}
			status.put(member, output + "\n");
		}
		return status;
	}

	@Override
	public void status(Player player) {
		if (player != null) {
			HashMap<String, String> status = getStatus();
			String name = player.getName();

			String output = status.get(name);
			for (String tmp : members) {
				if (!name.equals(tmp)) {
					output += status.get(tmp);
				}
			}
			plugin.sendMessage(player, "Status:");
			plugin.sendMessage(player, output);
		}
	}
}
