/**
* ShowCaseStandalone
* Copyright (C) 2014 Kellerkindt <copyright at kellerkindt.com>
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*/
package com.kellerkindt.scs.listeners;

import com.kellerkindt.scs.ShowCaseStandalone;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.kellerkindt.scs.SCSConfiguration;
import com.kellerkindt.scs.events.ShowCaseCreateEvent;

/**
 *
 * @author michael <michael at kellerkindt.com>
 */
public class ResidenceListener implements Listener {

    protected ShowCaseStandalone scs;
    protected SCSConfiguration   config;
    
    public ResidenceListener (ShowCaseStandalone scs) {
        this.scs    = scs;
        this.config = scs.getConfiguration();
    }

    @EventHandler (priority=EventPriority.NORMAL, ignoreCancelled=true)
    public void onShowCaseCreateEvent (ShowCaseCreateEvent event) {
        if (config.isDebuggingShopCreation()) {
            scs.getLogger().info("Entered ResidenceListener::onShowCaseCreateEvent");
        }
        
        Location location = event.getShop().getLocation();
        Player   player   = event.getPlayer();
        
        // try to get the residence
        ClaimedResidence residence = Residence.getResidenceManager().getByLoc(location);

        if (config.isDebuggingShopCreation()) {
            scs.getLogger().info("ClaimedResidence="+residence+", for location="+location);
        }
        
        if (residence != null) {
            
            boolean hasFlag = residence.getPermissions().playerHas(player.getName(), config.getResidenceFlag(), false);
            boolean isOwner = player.getName().equals(residence.getOwner());

            if (config.isDebuggingShopCreation()) {
                scs.getLogger().info("hasFlag="+hasFlag+", isOwner="+isOwner);
            }
            
            if (!hasFlag && !(config.getResidenceAllowOwner() && isOwner)) {
                // no permissions
                event.setCancelled(true);

                if (config.isDebuggingShopCreation()) {
                    scs.getLogger().info("ShowCaseCreateEvent cancelled");
                }
            }
        }

        if (config.isDebuggingShopCreation()) {
            scs.getLogger().info("Leaving ResidenceListener::onShowCaseCreateEvent");
        }
    }
    
}
