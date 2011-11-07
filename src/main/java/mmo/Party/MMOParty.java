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
import mmo.Core.MMO;
import mmo.Core.MMOPlugin;
import mmo.Core.util.EnumBitSet;
import mmo.Core.MMOMinecraft;
import mmo.Core.PartyAPI.Party;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.util.config.Configuration;
import org.getspout.spoutapi.gui.Container;
import org.getspout.spoutapi.gui.ContainerType;
import org.getspout.spoutapi.gui.GenericContainer;
import org.getspout.spoutapi.player.SpoutPlayer;

public class MMOParty extends MMOPlugin {

	static final PartyAPI partyapi = PartyAPI.instance;
	private int updateTask;
	/**
	 * Config options
	 */
	static String config_ui_align = "TOP_LEFT";
	static int config_ui_left = 3;
	static int config_ui_top = 3;
	static int config_ui_maxwidth = 160;
	static int config_max_party_size = 6;
	static boolean config_always_show = true;
	static boolean config_no_party_pvp = true;
	static boolean config_no_party_pvp_quiet = false;
	static boolean config_show_pets = true;
	static boolean config_leave_on_quit = false;

	@Override
	public EnumBitSet mmoSupport(EnumBitSet support) {
		support.set(Support.MMO_PLAYER);
		return support;
	}

	@Override
	public void onEnable() {
		super.onEnable();
		PartyAPI.plugin = this;
		MMOMinecraft.addAPI(partyapi);

		pm.registerEvent(Type.CUSTOM_EVENT, new PartyDamage(this), Priority.Highest, this);
		pm.registerEvent(Type.CUSTOM_EVENT, new PartyChannel(), Priority.Normal, this);

		updateTask = getServer().getScheduler().scheduleSyncRepeatingTask(this,
				new Runnable() {

					@Override
					public void run() {
						PartyAPI.updateAll();
					}
				}, 20, 20);
	}

	@Override
	public void loadConfiguration(Configuration cfg) {
		config_ui_align = cfg.getString("ui.default.align", config_ui_align);
		config_ui_left = cfg.getInt("ui.default.left", config_ui_left);
		config_ui_top = cfg.getInt("ui.default.top", config_ui_top);
		config_ui_maxwidth = cfg.getInt("ui.default.width", config_ui_maxwidth);
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
		super.onDisable();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			return false;
		}
		Player player = (Player) sender;
		if (command.getName().equalsIgnoreCase("party")) {
			PartyAPI party = partyapi.find(player);
			boolean isParty = party == null ? false : party.isParty();
			boolean isLeader = party == null ? true : party.isLeader(player);
			if (args.length == 0) {
				//<editor-fold defaultstate="collapsed" desc="/party">
				if (MMOMinecraft.mmoChat) {
					MMOMinecraft.getChat().doChat("Party", player, "");
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
				if (isParty && MMOMinecraft.mmoChat) {
					sendMessage(player, "/party <message>");
				}
				//</editor-fold>
			} else if (args[0].equalsIgnoreCase("status")) {
				//<editor-fold defaultstate="collapsed" desc="/party status">
				if (!isParty) {
					List<Party> invites = partyapi.findInvites(player);
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
					List<Party> invites = partyapi.findInvites(player);
					if (args.length > 1) {
						party = partyapi.find(args[1]);
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
				List<Party> invites = partyapi.findInvites(player);
				if (args.length > 1) {
					party = partyapi.find(args[1]);
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
				if (MMOMinecraft.mmoChat) {
					String output = "";
					for (String word : args) {
						output += word + " ";
					}
					MMOMinecraft.getChat().doChat("Party", player, output.trim());
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
	public void onPlayerJoin(Player player) {
		PartyAPI party = partyapi.find(player);
		if (party.isParty()) {
			party.update();
		} else {
			List<Party> invites = partyapi.findInvites(player);
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
	}

	@Override
	public void onPlayerQuit(Player player) {
		PartyAPI.containers.remove(player);
		PartyAPI party = partyapi.find(player);
		if (party != null) {
			if (!party.isParty() && !party.hasInvites()) {
				PartyAPI.delete(party);
			} else if (config_leave_on_quit) {
				party.leave(player);
			} else {
				party.update();
			}
		}
	}

	@Override
	public void onSpoutCraftPlayer(SpoutPlayer player) {
		Container container = getContainer(player, config_ui_align, config_ui_left, config_ui_top);
		Container members = new GenericContainer();
		container.setLayout(ContainerType.HORIZONTAL).addChildren(members, new GenericContainer()).setWidth(config_ui_maxwidth);
		PartyAPI.containers.put(player, members);
		PartyAPI.updateAll(player);
	}
}
