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
import java.util.logging.Level;
import java.util.logging.Logger;
import mmo.Core.mmo;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;

public class mmoParty extends JavaPlugin {

	private static Logger log;
	protected static Server server;
	protected static PluginManager pm;
	protected static PluginDescriptionFile description;
	protected static mmo mmo;
	private int updateTask;

	@Override
	public void onEnable() {
		server = getServer();
		pm = server.getPluginManager();
		log = Logger.getLogger("Minecraft");
		description = getDescription();

		log("loading " + description.getFullName());

		Party.mmo = mmo = mmo.create(this);
		mmo.mmoParty = true;
		mmo.setPluginName("Party");
		mmo.setX(mmo.cfg.getInt("ui.default.left", 3));
		mmo.setY(mmo.cfg.getInt("ui.default.top", 3));

		// Default values
		mmo.cfg.getBoolean("auto_update", true);
		mmo.cfg.getInt("max_party_size", 6);
		mmo.cfg.getBoolean("always_show", true);
		mmo.cfg.getBoolean("no_party_pvp", true);
		mmo.cfg.getBoolean("show_pets", true);
		mmo.cfg.save();

		mmoPartyPlayerListener ppl = new mmoPartyPlayerListener();
		pm.registerEvent(Type.PLAYER_JOIN, ppl, Priority.Monitor, this);
		pm.registerEvent(Type.PLAYER_QUIT, ppl, Priority.Monitor, this);
		pm.registerEvent(Type.PLAYER_KICK, ppl, Priority.Monitor, this);

		mmoPartyEntityListener pel = new mmoPartyEntityListener();
		pm.registerEvent(Type.ENTITY_DAMAGE, pel, Priority.Highest, this);
		pm.registerEvent(Type.PROJECTILE_HIT, pel, Priority.Highest, this); // craftbukkit 1000

		Party.load();

		for (Player player : server.getOnlinePlayers()) {
			if (Party.find(player) == null) {
				new Party(player.getName());
			}
		}

		updateTask = server.getScheduler().scheduleSyncRepeatingTask(this,
			new Runnable() {

				@Override
				public void run() {
					Party.updateAll();
				}
			}, 20, 20);
	}

	@Override
	public void onDisable() {
		server.getScheduler().cancelTask(updateTask);
		Party.save();
		Party.clear();
		log("Disabled " + description.getFullName());
		mmo.autoUpdate();
		mmo.mmoParty = false;
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
			if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
				//<editor-fold defaultstate="collapsed" desc="/party OR /party help">
				mmo.sendMessage(player, "Party commands:");
				mmo.sendMessage(player, "/party status");
				if (isLeader) {
					mmo.sendMessage(player, "/party invite <player>");
				}
				if (!isParty) {
					mmo.sendMessage(player, "/party accept [<leader>]");
				}
				if (!isParty) {
					mmo.sendMessage(player, "/party decline [<leader>]");
				}
				if (isParty) {
					mmo.sendMessage(player, "/party leave");
				}
				if (isParty && isLeader) {
					mmo.sendMessage(player, "/party promote <player>");
				}
				if (isParty && isLeader) {
					mmo.sendMessage(player, "/party kick <player>");
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
							output += mmo.name(invite.getLeader());
							first = false;
						}
					}
					mmo.sendMessage(player, output + ".");
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
					mmo.sendMessage(player, "Who do you want to invite?");
				}
				//</editor-fold>
			} else if (args[0].equalsIgnoreCase("promote")) {
				//<editor-fold defaultstate="collapsed" desc="/party promote <player>">
				if (!isParty) {
					mmo.sendMessage(player, "You are not in a party.");
				} else if (args.length > 1) {
					party.promote(player, args[1]);
				} else {
					mmo.sendMessage(player, "Who do you want to promote?");
				}
				//</editor-fold>
			} else if (args[0].equalsIgnoreCase("accept")) {
				//<editor-fold defaultstate="collapsed" desc="/party accept [<leader>]">
				if (isParty) {
					mmo.sendMessage(player, "You are already in a party.");
				} else {
					List<Party> invites = Party.findInvites(player);
					if (args.length > 1) {
						party = Party.find(args[1]);
						if (party == null) {
							mmo.sendMessage(player, "Unable to find that party.");
						} else {
							party.accept(player);
						}
					} else {
						if (invites.isEmpty()) {
							mmo.sendMessage(player, "No invitations to accept.");
						} else if (invites.size() == 1) {
							invites.get(0).accept(player);
						} else {
							mmo.sendMessage(player, "Accept which invitation? (/party status for list)");
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
						mmo.sendMessage(player, "Unable to find that party.");
					} else {
						party.decline(player);
					}
				} else {
					if (invites.isEmpty()) {
						mmo.sendMessage(player, "No invitations to decline.");
					} else if (invites.size() == 1) {
						invites.get(0).decline(player);
					} else {
						mmo.sendMessage(player, "Decline which invitation? (/party status for list)");
					}
				}
				//</editor-fold>
			} else if (args[0].equalsIgnoreCase("leave")) {
				//<editor-fold defaultstate="collapsed" desc="/party leave">
				if (!isParty) {
					mmo.sendMessage(player, "You are not in a party.");
				} else {
					party.leave(player);
				}
				//</editor-fold>
			} else if (args[0].equalsIgnoreCase("kick")) {
				//<editor-fold defaultstate="collapsed" desc="/party kick <player>">
				if (!isParty) {
					mmo.sendMessage(player, "You are not in a party.");
				} else if (args.length > 1) {
					party.kick(player, args[1]);
				} else {
					mmo.sendMessage(player, "Who do you want to kick?");
				}
				//</editor-fold>
			} else {
				//<editor-fold defaultstate="collapsed" desc="unknown">
				return false;
				//</editor-fold>
			}
			return true;
		}
		return false;
	}

	protected static void log(String text) {
		log.log(Level.INFO, "[" + description.getName() + "] " + text);
	}
}
