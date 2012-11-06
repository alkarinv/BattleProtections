package mc.alk.protection.executors;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import mc.alk.protection.controllers.ProtectionController;
import mc.alk.protection.controllers.ProtectionController.Owner;
import mc.alk.protection.controllers.ProtectionController.ProtectionInfo;
import mc.alk.protection.controllers.ProtectionController.ProtectionList;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.alk.controllers.MC;
import com.alk.executors.CustomCommandExecutor;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class ProtectionExecutor extends CustomCommandExecutor {
	ProtectionController pc;
	MC mc;

	public ProtectionExecutor(ProtectionController pc, MC mc) {
		super();
		this.pc = pc;
		this.mc = mc;
	}


	@Override
	protected void showHelp(CommandSender sender, Command command) {
		MC.sendMessage(sender, "&2/protect add <region>");
		MC.sendMessage(sender, "&2/protect delete <region>");
		MC.sendMessage(sender, "&2/protect share <region> <player>");
		MC.sendMessage(sender, "&2/protect unshare <region> <player>");
		MC.sendMessage(sender, "&2/protect list");		
	}

	@MCCommand(cmds={"list"},inGame=true)
	public boolean regionList(Player sender){
		ProtectionList pl = pc.getProtectionList(sender);
		Map<Owner,List<String>> regions = pl.protList;
		int nProts = pc.getAllowedProtections(sender);
		int nVolume = pc.getAllowedVolume(sender);
		/// \u221E is infinity
		MC.sendMessage(sender, "&2Owner Regions: &6" + pl.pi.nProts +
				"/" + nProts + "&2 Volume: &6" + pl.pi.area+"/"+ nVolume+
				"&2 Member Regions: &6" + regions.get(Owner.MEMBER).size());

		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String region: regions.get(Owner.OWNER)){
			if (!first) sb.append(", ");
			first = false;
			int index = region.lastIndexOf("-");
			if (index != -1)
				region = region.substring(index+1, region.length());
			sb.append(region);
		}
		if (!regions.get(Owner.OWNER).isEmpty())
			MC.sendMessage(sender, "&2Owner: &6" + sb.toString());
		first = true;
		for (String region: regions.get(Owner.MEMBER)){
			if (!first) sb.append(", ");
			first = false;
			int index = region.lastIndexOf("-");
			if (index != -1)
				region = region.substring(index+1, region.length());
			sb.append(region);
		}
		if (!regions.get(Owner.MEMBER).isEmpty()){
			MC.sendMessage(sender, "&2Member: &6" + sb.toString());
		}
		return true;
	}

	@MCCommand(cmds={"share"},inGame=true, alphanum={1}, usage="prot share <region> <player>")
	public boolean regionShare(Player p, String regionid, Player p2){
		World w = p.getWorld();
		String id = p.getName()+"-"+regionid;
		ProtectedRegion region = pc.getRegion(w, (String) id);
		if (region == null){
			return sendMessage(p, ChatColor.RED + "That region doesnt exist");}
		if (!pc.ownsRegion(p,region)){
			return sendMessage(p, ChatColor.RED + "You don't own this region");}
		if (pc.share(w, region,p2))
			return sendMessage(p, ChatColor.GREEN + "Region &6"+regionid+ChatColor.GREEN+" shared with &6"+p2.getDisplayName());
		else
			return sendMessage(p, ChatColor.RED + "Error sharing region.");
	}

	@MCCommand(cmds={"unshare","remove"},inGame=true, alphanum={1}, usage="prot unshare <region> <player>")
	public boolean regionUnshare(Player p, String regionid, Player p2){
		World w = p.getWorld();
		String id = p.getName()+"-"+regionid;
		ProtectedRegion region = pc.getRegion(w, (String) id);
		if (region == null){
			return sendMessage(p, ChatColor.RED + "That region doesnt exist");}
		if (!pc.ownsRegion(p,region)){
			return sendMessage(p, ChatColor.RED + "You don't own this region");}
		if (pc.unshare(w, region,p2))
			return sendMessage(p, ChatColor.GREEN + "Region &6"+regionid+ChatColor.GREEN+" unshared with &6"+p2.getDisplayName());
		else
			return sendMessage(p, ChatColor.RED + "Error unsharing region.");
	}

	@MCCommand(cmds={"add"},inGame=true, alphanum={1}, usage="prot add <region>")
	public boolean regionAdd(Player p, String regionid){
		pc.addDefaultRegion(p.getWorld(), "PlayerDefault");
		int nProts = pc.getAllowedProtections(p);
		if (nProts == 0){
			return mc.sendMsg(p, ChatColor.RED+"You have no available protection regions");}
		ProtectionInfo pi = pc.getProtectionsInfo(p);
		if (pi.nProts >= nProts){
			mc.sendMsg(p, ChatColor.RED+"You are using all &6"+nProts+"&c of your protections");
			return mc.sendMsg(p, ChatColor.GOLD+"/protection list: &e to see them");
		}
		String id = p.getName()+"-"+regionid; 
		World w = p.getWorld();
		if (pc.getRegion(w, id) != null){
			return sendMessage(p, ChatColor.RED + "You already have a region called &6" +regionid+"&c");}

		WorldEditPlugin wep = pc.getWorldEdit();
		Selection sel = wep.getSelection(p);
		if (sel == null)
			return sendMessage(p, ChatColor.RED + "Please select the protection area first.");
		int area = sel.getArea();
		int allowed = pc.getAllowedAreaPerRegion(p);
		if (area > allowed){
			return sendMessage(p, ChatColor.RED + "You can only protect &6"+allowed+"&c blocks. You selected &6"+area);}

		int totalAllowed = pc.getAllowedVolume(p);
		if (pi.area+area >= totalAllowed){
			return mc.sendMsg(p, ChatColor.RED+"This selection would take you to &6"+(pi.area+area)+"/" + totalAllowed);
		}
		try{
			if (pc.addRegion(p,sel,id))
				return sendMessage(p, ChatColor.GREEN + "Region &6"+regionid+ChatColor.GREEN+" added");
			else 
				return sendMessage(p, ChatColor.RED + "Error adding region. Contact an Administrator");
		} catch (Exception e){
			return sendMessage(p, e.getMessage());
		}
	}

	@MCCommand(cmds={"listSelectionPlayers"},op=true,inGame=true, alphanum={1}, 
			usage="prot listSelectionPlayers: list players having regions in a selected area")
	public boolean regionListPlayersInSelection(Player p){

		WorldEditPlugin wep = pc.getWorldEdit();
		Selection sel = wep.getSelection(p);
		if (sel == null)
			return sendMessage(p, ChatColor.RED + "Please select the protection area first.");
		Collection<String> players = pc.getPlayersInSelection(sel);
		if (players == null || players.isEmpty()){
			return sendMessage(p, "&2There are no players that have regions in this selection");}
		
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String s: players){
			if (!first) sb.append(", ");
			sb.append(s);
			if (first) first = false;
		}
		return sendMessage(p, "&2The following players have selections in this region " + sb.toString());
	}
	
	@MCCommand(cmds={"delete","del"},inGame=true)
	public boolean regionDelete(Player p, String regionid){
		World w = p.getWorld();
		String id = p.getName()+"-"+regionid; 
		ProtectedRegion region = pc.getRegion(w, (String) id);
		if (region == null){
			return sendMessage(p, ChatColor.RED + "That region doesnt exist");}
		if (!pc.ownsRegion(p,region)){
			return sendMessage(p, ChatColor.RED + "You don't own this region");}
		if (pc.delete(w, region))
			return sendMessage(p, ChatColor.GREEN + "Region &6"+regionid+ChatColor.GREEN+" deleted");
		else
			return sendMessage(p, ChatColor.RED + "Error deleting region.");
	}

}
