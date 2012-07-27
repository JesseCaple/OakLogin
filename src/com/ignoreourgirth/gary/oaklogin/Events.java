/*******************************************************************************
 * Copyright (c) 2012 GaryMthrfkinOak (Jesse Caple).
 * 
 * This is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or any later version.
 * 
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this software.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.ignoreourgirth.gary.oaklogin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;

import com.ignoreourgirth.gary.oakcorelib.OakCoreLib;

public class Events implements Listener {
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		player.sendMessage("§2§nWelcome to the §lIoG §r§2§nMinecraft server                              ");
		event.setJoinMessage(null);
		OakLogin.displayGlobalMessage(player, "§7:: " + player.getName() + " has connected.");
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		event.setQuitMessage(null);
		OakLogin.displayGlobalMessage(player, "§7:: " + player.getName() + " has disconnected.");
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onKick(PlayerKickEvent event) {
		Player player = event.getPlayer();
		event.setLeaveMessage(null);
		OakLogin.displayGlobalMessage(player, "§7:: " + player.getName() + " has disconnected.");
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerDeathEvent(PlayerDeathEvent event) {
		Player player = event.getEntity();
		Player murderer = player.getKiller();
		if (murderer != null) {
			OakLogin.displayLocalMessage(player, murderer, "§4§l" + murderer.getName() + " killed " + player.getName(), 500);
			player.sendMessage("§4 -- You were killed by " + murderer.getName() + " -- ");
			murderer.sendMessage("§4§oYou killed " + player.getName());
		} else {
			OakLogin.displayLocalMessage(player, null, "§7§o" + event.getDeathMessage() + ".", 100);
		}
		event.setDeathMessage(null);
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerLogin(PlayerLoginEvent event) {
		try {
			Player eventPlayer = event.getPlayer();
			
			PreparedStatement statement = OakCoreLib.getDB().prepareStatement("SELECT userid FROM iog_website.bb_userfield WHERE field5=?");
			statement.setString(1, eventPlayer.getName());
			ResultSet result = statement.executeQuery();
			if (!result.next()) {
				event.disallow(Result.KICK_OTHER, "Please register your mc username with our forum.");
				return;
			}
			int userID = result.getInt(1);
			if (result.next()) {
				event.disallow(Result.KICK_OTHER, "This name is linked to multiple accounts. Contact an admin immediately!");
				return;
			}
			result.close();
			statement.close();
			
			statement = OakCoreLib.getDB().prepareStatement("SELECT usergroupid, membergroupids FROM iog_website.bb_user WHERE userid=?");
			statement.setInt(1, userID);
			result = statement.executeQuery();
			if (!result.next()) {
				event.disallow(Result.KICK_OTHER, OakLogin.dbErrorMessage);
				return;
			}
			int primaryGroupID = result.getInt(1);
			String memberIDList = result.getString(2);
			if (primaryGroupID == 8) {
				event.disallow(Result.KICK_OTHER, "Can not connect. Your account has been banned.");
				return;
			} else if (primaryGroupID == 3) {
				event.disallow(Result.KICK_OTHER, "Please check your inbox for a forum verification email.");
				return;
			}
			result.close();
			statement.close();
			
			statement = OakCoreLib.getDB().prepareStatement("UPDATE iog_website.bb_user SET mc_username=? WHERE userid=?");
			statement.setString(1, eventPlayer.getName());
			statement.setInt(2, userID);
			statement.executeUpdate();
			statement.close();
			
			boolean containsRegisteredGroup = false;
			ArrayList<String> permissionGroups = new ArrayList<String>();
			permissionGroups.add(OakLogin.getGroupName(primaryGroupID).toLowerCase());	
			for (String nextGroupID : memberIDList.split(",")) {
				if (!nextGroupID.isEmpty()) {
					int nextID = Integer.parseInt(nextGroupID);
					if (nextID == 2) {containsRegisteredGroup = true;}
					permissionGroups.add(OakLogin.getGroupName(nextID));
				}
			}
			if (!containsRegisteredGroup) {permissionGroups.add(OakLogin.getGroupName(2));}
			OakLogin.setPermissions(eventPlayer, permissionGroups);
			
		} catch (SQLException ex) {
			OakLogin.log.log(Level.SEVERE, ex.getMessage());
			event.disallow(Result.KICK_OTHER, OakLogin.dbErrorMessage);
		}
	}
	
}
