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

import java.util.ArrayList;
import java.util.List;
import mmo.Chat.Chat;
import mmo.Core.MMO;
import mmo.Core.MMOListener;
import mmo.Core.events.MMODamageEvent;
import mmo.Core.MMOPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.config.Configuration;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.event.spout.SpoutCraftEnableEvent;
import org.getspout.spoutapi.event.spout.SpoutListener;
import org.getspout.spoutapi.gui.GenericContainer;
import org.getspout.spoutapi.player.SpoutPlayer;

public class MMOParty extends MMOPlugin {

	private int updateTask;
	/**
	 * Config options
	 */
	String config_ui_align = "TOP_LEFT";
	int config_ui_left = 3;
	int config_ui_top = 3;
	int config_max_party_size = 6;
	boolean config_always_show = true;
	boolean config_no_party_pvp = true;
	boolean config_no_party_pvp_quiet = false;
	boolean config_show_pets = true;
	boolean config_leave_on_quit = false;

	@Override
	public void onEnable() {
		super.onEnable();
		MMO.mmoParty = true;

		getDatabase().find(PartyDB.class);//.findRowCount();

		mmoPartyPlayerListener ppl = new mmoPartyPlayerListener();
		pm.registerEvent(Type.PLAYER_JOIN, ppl, Priority.Monitor, this);
		pm.registerEvent(Type.PLAYER_QUIT, ppl, Priority.Monitor, this);
		pm.registerEvent(Type.PLAYER_KICK, ppl, Priority.Monitor, this);

		pm.registerEvent(Type.CUSTOM_EVENT, new mmoPartyEntityListener(), Priority.Highest, this);
		pm.registerEvent(Type.CUSTOM_EVENT, new mmoSpoutListener(), Priority.Normal, this);
		pm.registerEvent(Type.CUSTOM_EVENT, new ChannelParty(), Priority.Normal, this);

		Party.plugin = this;
		Party.load();

		for (Player player : getServer().getOnlinePlayers()) {
			if (Party.find(player) == null) {
				//ToDo: Catch this Leak
				new Party(player.getName());
			}
			SpoutPlayer splayer = SpoutManager.getPlayer(player);
			if (splayer.isSpoutCraftEnabled()) {
				GenericContainer container = getContainer();
				Party.containers.put(player, container);
				splayer.getMainScreen().attachWidget(this, container);
				Party.update(player);
			}
		}

		updateTask = getServer().getScheduler().scheduleSyncRepeatingTask(this,
				  new Runnable() {

					  @Override
					  public void run() {
						  Party.updateAll();
					  }
				  }, 20, 20);
	}

	@Override
	public void loadConfiguration(Configuration cfg) {
		config_ui_align = cfg.getString("ui.default.align", config_ui_align);
		config_ui_left = cfg.getInt("ui.default.left", config_ui_left);
		config_ui_top = cfg.getInt("ui.default.top", config_ui_top);
		config_max_party_size = cfg.getInt("max_party_size", config_max_party_size);
		config_always_show = cfg.getBoolean("always_show", config_always_show);
		config_no_party_pvp = cfg.getBoolean("no_party_pvp", config_no_party_pvp);
		config_no_party_pvp_quiet = cfg.getBoolean("no_party_pvp_quiet", config_no_party_pvp_quiet);
		config_show_pets = cfg.getBoolean("show_pets", config_show_pets);
		config_leave_on_quit = cfg.getBoolean("leave_on_quit", config_leave_on_quit);
	}

	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTask(updateTask);
		Party.save();
		Party.clear();
//		mmo.autoUpdate();
		MMO.mmoParty = false;
		super.onDisable();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			return false;
		}
		Player player = (Player) sender;
		if (command.getName().equalsIgnoreCase("party")) {
			Party party = Party.find(player);
			boolean isParty = party == null ? false : party.isParty();
			boolean isLeader = party == null ? true : party.isLeader(player);
			if (args.length == 0) {
				//<editor-fold defaultstate="collapsed" desc="/party">
				if (MMO.mmoChat) {
					Chat.doChat("Party", player, "");
				} else {
					return false;
				}
				//</editor-fold>
			} else if (args[0].equalsIgnoreCase("help")) {
				//<editor-fold defaultstate="collapsed" desc="/party OR /party help">
				sendMessage(player, "Party commands:");
				sendMessage(player, "/party status");
				if (isLeader) {
					sendMessage(player, "/party invite <player>");
				}
				if (!isParty) {
					sendMessage(player, "/party accept [<leader>]");
				}
				if (!isParty) {
					sendMessage(player, "/party decline [<leader>]");
				}
				if (isParty) {
					sendMessage(player, "/party leave");
				}
				if (isParty && isLeader) {
					sendMessage(player, "/party promote <player>");
				}
				if (isParty && isLeader) {
					sendMessage(player, "/party kick <player>");
				}
				if (isParty && MMO.mmoChat) {
					sendMessage(player, "/party <message>");
				}
				//</editor-fold>
			} else if (args[0].equalsIgnoreCase("status")) {
				//<editor-fold defaultstate="collapsed" desc="/party status">
				if (!isParty) {
					List<Party> invites = Party.findInvites(player);
					String output = "You are not in a party, and have ";
					if (invites.isEmpty()) {
						output += "no party invites";
					} else {
						output += "been invited by: ";
						boolean first = true;
						for (Party invite : invites) {
							if (!first) {
								output += ", ";
							}
							output += MMO.name(invite.getLeader());
							first = false;
						}
					}
					sendMessage(player, output + ".");
				} else {
					party.status(player);
					party.update();
				}
				//</editor-fold>
			} else if (args[0].equalsIgnoreCase("invite")) {
				//<editor-fold defaultstate="collapsed" desc="/party invite <player>">
				if (args.length > 1) {
					if (party == null) {
						party = new Party(player.getName());
					}
					party.invite(player, args[1]);
				} else {
					sendMessage(player, "Who do you want to invite?");
				}
				//</editor-fold>
			} else if (args[0].equalsIgnoreCase("promote")) {
				//<editor-fold defaultstate="collapsed" desc="/party promote <player>">
				if (!isParty) {
					sendMessage(player, "You are not in a party.");
				} else if (args.length > 1) {
					party.promote(player, args[1]);
				} else {
					sendMessage(player, "Who do you want to promote?");
				}
				//</editor-fold>
			} else if (args[0].equalsIgnoreCase("accept")) {
				//<editor-fold defaultstate="collapsed" desc="/party accept [<leader>]">
				if (isParty) {
					sendMessage(player, "You are already in a party.");
				} else {
					List<Party> invites = Party.findInvites(player);
					if (args.length > 1) {
						party = Party.find(args[1]);
						if (party == null) {
							sendMessage(player, "Unable to find that party.");
						} else {
							party.accept(player);
						}
					} else {
						if (invites.isEmpty()) {
							sendMessage(player, "No invitations to accept.");
						} else if (invites.size() == 1) {
							invites.get(0).accept(player);
						} else {
							sendMessage(player, "Accept which invitation? (/party status for list)");
						}
					}
				}
				//</editor-fold>
			} else if (args[0].equalsIgnoreCase("decline")) {
				//<editor-fold defaultstate="collapsed" desc="/party decline <leader>">
				List<Party> invites = Party.findInvites(player);
				if (args.length > 1) {
					party = Party.find(args[1]);
					if (party == null) {
						sendMessage(player, "Unable to find that party.");
					} else {
						party.decline(player);
					}
				} else {
					if (invites.isEmpty()) {
						sendMessage(player, "No invitations to decline.");
					} else if (invites.size() == 1) {
						invites.get(0).decline(player);
					} else {
						sendMessage(player, "Decline which invitation? (/party status for list)");
					}
				}
				//</editor-fold>
			} else if (args[0].equalsIgnoreCase("leave")) {
				//<editor-fold defaultstate="collapsed" desc="/party leave">
				if (!isParty) {
					sendMessage(player, "You are not in a party.");
				} else {
					party.leave(player);
				}
				//</editor-fold>
			} else if (args[0].equalsIgnoreCase("kick")) {
				//<editor-fold defaultstate="collapsed" desc="/party kick <player>">
				if (!isParty) {
					sendMessage(player, "You are not in a party.");
				} else if (args.length > 1) {
					party.kick(player, args[1]);
				} else {
					sendMessage(player, "Who do you want to kick?");
				}
				//</editor-fold>
			} else {
				//<editor-fold defaultstate="collapsed" desc="/party <message>">
				if (MMO.mmoChat) {
					String output = "";
					for (String word : args) {
						output += word + " ";
					}
					Chat.doChat("Party", player, output.trim());
				} else {
					return false;
				}
				//</editor-fold>
			}
			return true;
		}
		return false;
	}

	@Override
	public List<Class<?>> getDatabaseClasses() {
		List<Class<?>> list = new ArrayList<Class<?>>();
		list.add(PartyDB.class);
		return list;
	}

	private class mmoSpoutListener extends SpoutListener {

		@Override
		public void onSpoutCraftEnable(SpoutCraftEnableEvent event) {
			SpoutPlayer player = SpoutManager.getPlayer(event.getPlayer());
			GenericContainer container = getContainer();
			Party.containers.put(player, container);
			player.getMainScreen().attachWidget(plugin, container);
			Party.update(player);
		}
	}

	private class mmoPartyEntityListener extends MMOListener {

		@Override
		public void onMMOPVPDamage(MMODamageEvent event) {
			if (config_no_party_pvp && Party.isSameParty((Player) event.getAttacker(), (Player) event.getDefender())) {
				if (config_no_party_pvp_quiet) {
					plugin.sendMessage((Player) event.getAttacker(), "Can't attack your own party!");
				}
				event.setCancelled(true);
			}
		}
	}

	private class mmoPartyPlayerListener extends PlayerListener {

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
						output += MMO.name(invite.getLeader());
						first = false;
					}
					plugin.sendMessage(player, output);
				}
			}
			Party.update(player);
		}

		public void PlayerQuit(Player player) {
			Party.containers.remove(player);
			Party party = Party.find(player);
			if (party != null) {
				if (!party.isParty() && !party.hasInvites()) {
					Party.delete(party);
				} else if (config_leave_on_quit) {
					party.leave(player);
				} else {
					party.update();
				}
			}
		}

		@Override
		public void onPlayerQuit(PlayerQuitEvent event) {
			PlayerQuit(event.getPlayer());
		}

		@Override
		public void onPlayerKick(PlayerKickEvent event) {
			PlayerQuit(event.getPlayer());
		}
	}
}
