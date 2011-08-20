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

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityListener;

public class mmoPartyEntityListener extends EntityListener {

	@Override
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.isCancelled()) {
			return;
		}
		if (mmoParty.mmo.cfg.getBoolean("no_party_pvp", true)) {
			Player attacker = null, defender = null;
			if (event.getEntity() instanceof Player) {
				defender = (Player) event.getEntity();
			} else if (event.getEntity() instanceof Tameable) {
				Tameable pet = (Tameable) event.getEntity();
				if (pet.isTamed() && pet.getOwner() instanceof Player) {
					defender = (Player) pet.getOwner();
				}
			}
			if (defender != null) {
				if (event.getCause() == DamageCause.ENTITY_ATTACK) {
					EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
					if (e.getDamager() instanceof Player) {
						attacker = (Player) e.getDamager();
					} else if (e.getDamager() instanceof Tameable) {
						Tameable pet = (Tameable) e.getDamager();
						if (pet.isTamed() && pet.getOwner() instanceof Player) {
							defender = (Player) pet.getOwner();
						}
					}
				}
			} else if (event.getCause() == DamageCause.PROJECTILE) {
				EntityDamageByEntityEvent e = (EntityDamageByEntityEvent) event;
				Projectile arrow = (Projectile) e.getDamager();
				if (arrow.getShooter() instanceof Player) {
					attacker = (Player) arrow.getShooter();
				}
			}
			if (attacker != null && Party.isSameParty(attacker, defender)) {
				mmoParty.mmo.sendMessage(attacker, "Can't attack your own party!");
				event.setCancelled(true);
			}
		}
	}
}
