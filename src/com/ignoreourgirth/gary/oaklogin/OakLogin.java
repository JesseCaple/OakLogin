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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.ignoreourgirth.gary.oakcorelib.CommandPreprocessor;
import com.ignoreourgirth.gary.oakcorelib.CommandPreprocessor.OnCommand;
import com.ignoreourgirth.gary.oakcorelib.OakCoreLib;

public class OakLogin extends JavaPlugin  {

	public static final String dbErrorMessage = "A database error has occurred. Please contact an admin.";
	
	public static Logger log;
	public static Server server;
   
	public void onEnable() {
		log = this.getLogger();
        server = this.getServer();
        server.getPluginManager().registerEvents(new Events(), this);
        CommandPreprocessor.addExecutor(this);
	}
	
	public void onDisable() {
		OakLogin.log.log(Level.WARNING, "Permission authority was disabled.");
	}
	
	@OnCommand (value="who", labels="PlayerName")
	public void whoCommand(Player player, String playerName){
		try {
			Player targetPlayer = getServer().getPlayer(playerName);
			if (targetPlayer == null) {
				player.sendMessage("§4User not found.");
				return;
			}
			PreparedStatement statement = OakCoreLib.getDB().prepareStatement("SELECT userid FROM iog_website.bb_userfield WHERE field5=?");
			statement.setString(1, targetPlayer.getName());
			ResultSet result = statement.executeQuery();
			if (!result.next()) {
				player.sendMessage("§4User not found.");
				return;
			}
			int userID = result.getInt(1);
			if (result.next()) {
				player.sendMessage("§4Warning! Multiple accounts with this name.");
				return;
			}
			statement = OakCoreLib.getDB().prepareStatement("SELECT username, usergroupid, membergroupids, joindate FROM iog_website.bb_user WHERE userid=?");
			statement.setInt(1, userID);
			result = statement.executeQuery();
			if (!result.next()) {
				player.sendMessage(dbErrorMessage);
				return;
			}
			String forumName = result.getString(1);
			int primaryGroupID = result.getInt(2);
			String memberIDList = result.getString(3);
			long joinUnixTimestamp = result.getLong(4);
			
			String primaryGroupName = getGroupName(primaryGroupID);
			StringBuilder builder = new StringBuilder();
			for (String nextGroupID : memberIDList.split(",")) {
				if (!nextGroupID.isEmpty()) {
					int nextID = Integer.parseInt(nextGroupID);
					builder.append(getGroupName(nextID));
					builder.append(", ");
				}
			}
			String secondaryGroupNames = builder.toString();
			
			Date actualJoinDate = new Date(1000L * joinUnixTimestamp);
			String readableJoinDate = DateFormat.getDateInstance(DateFormat.LONG).format(actualJoinDate) + " (UTC)";
			
			player.sendMessage(" ");
			player.sendMessage("§3Player§f:§b " + targetPlayer.getName());
			player.sendMessage("§3Forum Name§f:§b " + forumName);
			player.sendMessage("§3Main Group§f:§b " + primaryGroupName);
			if (!secondaryGroupNames.isEmpty()) {
				player.sendMessage("§3Other Groups§f:§b " + secondaryGroupNames);
			}
			player.sendMessage("§3Join Date§f:§b " + readableJoinDate);
			return;
		} catch (SQLException ex) {
			OakLogin.log.log(Level.SEVERE, ex.getMessage());
			player.sendMessage("§4" + dbErrorMessage);
		}
	}
	
	static void setPermissions(Player player, ArrayList<String> groups) {
		String playerName = player.getName();
		String[] oldGroups = OakCoreLib.getPermission().getPlayerGroups(player);
		for (String oldGroupName : oldGroups)
	    {
    		boolean deleteName = true;
	    	for (String newGroupName : groups)
		    {
	    		if (oldGroupName.equalsIgnoreCase(newGroupName)) {deleteName= false;}
		    }
	    	if (deleteName) {
	    		OakLogin.log.info(playerName + " --> -" + oldGroupName);
	    		OakCoreLib.getPermission().playerRemoveGroup(player, oldGroupName.toLowerCase());
	    	}
	    }
	    for (String newGroupName : groups)
	    {
    		boolean addName = true;
	    	for (String oldGroupName : oldGroups)
		    {
	    		if (oldGroupName.equalsIgnoreCase(newGroupName)) {addName= false;}
		    }
	    	if (addName) {
	    		OakLogin.log.info(playerName + " --> +" + newGroupName);
	    		OakCoreLib.getPermission().playerAddGroup(player, newGroupName.toLowerCase());
	    	}
	    }
	}
	
	public static String getGroupName(int id) throws SQLException {
		String returnValue = null;
		PreparedStatement statement = OakCoreLib.getDB().prepareStatement("SELECT title FROM iog_website.bb_usergroup WHERE usergroupid=?");
		statement.setInt(1, id);
		ResultSet result = statement.executeQuery();
		if (result.next()) {
			returnValue = result.getString(1);
		}
		result.close();
		statement.close();
		return returnValue;
	}

	
	public static void displayGlobalMessage(Player originPlayer, String message) {
        for (Player nextPlayer : server.getOnlinePlayers()) {
        	if (nextPlayer != originPlayer) {
        		nextPlayer.sendMessage(message);
        	}
        }
	}
	
	public static void displayLocalMessage(Player originPlayer, Player atacker, String message, int distance) {
        for (Player nextPlayer : getNearbyPlayers(originPlayer, distance)) {
        	if (nextPlayer != originPlayer && nextPlayer != atacker)
        	{
        		nextPlayer.sendMessage(message);
        	}
        }
	}
	
	private static Player[] getNearbyPlayers(Player originPlayer, int distance) {
		Location originLocation = originPlayer.getLocation();
		ArrayList<Player> list = new ArrayList<Player>();
        String originWorld = originPlayer.getWorld().getName();
        for (Player nextPlayer : server.getOnlinePlayers()) {
            Location nextLocation = nextPlayer.getLocation();
            if (originWorld.equals(nextLocation.getWorld().getName())) {
                int dx = originLocation.getBlockX() - nextLocation.getBlockX();
                int dz = originLocation.getBlockZ() - nextLocation.getBlockZ();
                dx = dx * dx;
                dz = dz * dz;
                int d = (int) Math.sqrt(dx + dz);
                if (d <= distance) { list.add(nextPlayer); }
            }
        }
        return list.toArray(new Player[list.size()]);
    }
}
