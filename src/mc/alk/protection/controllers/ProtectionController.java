	package mc.alk.protection.controllers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.milkbowl.vault.chat.Chat;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Polygonal2DSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.databases.ProtectionDatabaseException;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion.CircularInheritanceException;

public class ProtectionController {
	public WorldGuardPlugin wgp;
	Chat chat;
	public WorldEditPlugin wep;
	public enum Owner {OWNER,MEMBER}
	HashMap<World, ProtectedRegion> defaultRegions = new HashMap<World, ProtectedRegion>();
	public class ProtectionInfo{
		public int nProts;
		public int area;
		public ProtectionInfo(int prots, int area){
			this.nProts = prots; this.area = area;
		}
	}
	public class ProtectionList{
		public ProtectionInfo pi;
		public HashMap<Owner,List<String>> protList;
	}
	public int getAllowedProtections(Player p) {
		return chat.getPlayerInfoInteger(p.getWorld().getName(), p.getName(), "nRegions", 0);
	}

	public ProtectionInfo getProtectionsInfo(Player p) {
		final LocalPlayer wgplayer = wgp.wrapPlayer(p);
		RegionManager mgr = wgp.getGlobalRegionManager().get(p.getWorld());
		Map<String, ProtectedRegion> regions = mgr.getRegions();
		int count=0,area=0;
		String prefix = p.getName()+"-";
		for (ProtectedRegion pr : regions.values()) {
			if (!pr.getId().startsWith(prefix))
				continue;
			if (pr.isOwner(wgplayer)){
				count++;
				area+= pr.volume();
			}
		}
		return new ProtectionInfo(count, area);
	}

	public ProtectionList getProtectionList(Player p) {
		final LocalPlayer wgplayer = wgp.wrapPlayer(p);
		RegionManager mgr = wgp.getGlobalRegionManager().get(p.getWorld());
		Map<String, ProtectedRegion> regions = mgr.getRegions();

		HashMap<Owner,List<String>> map = new HashMap<Owner,List<String>>();
		List<String> owner = new ArrayList<String>();
		List<String> member = new ArrayList<String>();
		map.put(Owner.OWNER, owner);
		map.put(Owner.MEMBER, member);
		int count =0, area=0;
		String prefix = p.getName()+"-";

		for (ProtectedRegion pr : regions.values()) {
			if (pr.isOwner(wgplayer)){
				if (!pr.getId().startsWith(prefix))
					continue;
				area+=pr.volume();
				count++;
				owner.add(pr.getId());
			}
			else if (pr.isMember(wgplayer)){
				member.add(pr.getId());}
		}
		ProtectionList pl = new ProtectionList();
		pl.pi = new ProtectionInfo(count,area);
		pl.protList = map;
		return pl;
	}

	public int getAllowedAreaPerRegion(Player p) {
		return chat.getPlayerInfoInteger(p.getWorld().getName(), p.getName(), "allowedPerRegionArea", 0);
	}

	public int getAllowedVolume(Player p) {
		return chat.getPlayerInfoInteger(p.getWorld().getName(), p.getName(), "allowedTotalArea", 0);
	}

	public boolean addDefaultRegion(World w, String id) {
		RegionManager mgr = wgp.getGlobalRegionManager().get(w);
		ProtectedRegion region = mgr.getRegion(id);
		if (region != null) {
			defaultRegions.put(w, region);
			return false;
		}

		region = new ProtectedCuboidRegion(id, new BlockVector(0,0,0),new BlockVector(0,0,0));
		try {
			wgp.getRegionManager(w).addRegion(region);
			mgr.save();
//			region.setFlag(DefaultFlag.CHEST_ACCESS,State.ALLOW);
//			region.setFlag(DefaultFlag.PVP,State.ALLOW);
			defaultRegions.put(w, region);
		} catch (ProtectionDatabaseException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public ProtectedRegion getRegion(Selection sel, String id){

		// Detect the type of region from WorldEdit
		if (sel instanceof Polygonal2DSelection) {
			Polygonal2DSelection polySel = (Polygonal2DSelection) sel;
			int minY = polySel.getNativeMinimumPoint().getBlockY();
			int maxY = polySel.getNativeMaximumPoint().getBlockY();
			return new ProtectedPolygonalRegion(id, polySel.getNativePoints(), minY, maxY);
		} else if (sel instanceof CuboidSelection) {
			BlockVector min = sel.getNativeMinimumPoint().toBlockVector();
			BlockVector max = sel.getNativeMaximumPoint().toBlockVector();
			return new ProtectedCuboidRegion(id, min, max);
		} else {
			return null;
		}
	}

	public boolean addRegion(Player p, Selection sel, String id) throws Exception {
		RegionManager mgr = wgp.getGlobalRegionManager().get(sel.getWorld());

		if (mgr.hasRegion(id)) {
			return false;
		}

		ProtectedRegion region = getRegion(sel,id);
		if (region == null)
			return false;

		final LocalPlayer wgplayer = wgp.wrapPlayer(p);
		ApplicableRegionSet ars = mgr.getApplicableRegions(region);
		if (!ars.canBuild(wgplayer)){
			throw new Exception("You can't build in all areas of this region");
		}
		/// Add our owner
		try {
			DefaultDomain owners = new DefaultDomain();
			owners.addPlayer(wgplayer);
			region.setOwners(owners);
			wgp.getRegionManager(p.getWorld()).addRegion(region);
			region.setParent(defaultRegions.get(p.getWorld()));
			mgr.save();
		} catch (ProtectionDatabaseException e) {
			e.printStackTrace();
			return false;
		} catch (CircularInheritanceException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean delete(World w, ProtectedRegion region) {
		RegionManager mgr = wgp.getRegionManager(w);
		try {
			mgr.removeRegion(region.getId());
			mgr.save();
		} catch (ProtectionDatabaseException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean share(World w, ProtectedRegion region, Player p2) {
		RegionManager mgr = wgp.getRegionManager(w);
		try {
			DefaultDomain dd = region.getMembers();
			dd.addPlayer(p2.getName());
			region.setMembers(dd);
			mgr.save();
		} catch (ProtectionDatabaseException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}


	public boolean unshare(World w, ProtectedRegion region, Player p2) {
		RegionManager mgr = wgp.getRegionManager(w);
		try {
			DefaultDomain dd = region.getMembers();
			dd.removePlayer(p2.getName());
			region.setMembers(dd);
			mgr.save();
		} catch (ProtectionDatabaseException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public ProtectedRegion getRegion(World w, String id) {
		return wgp.getRegionManager(w).getRegion(id);
	}

	public boolean ownsRegion(Player p, ProtectedRegion region) {
		final LocalPlayer wgplayer = wgp.wrapPlayer(p);
		return region.isOwner(wgplayer);
	}

	public WorldEditPlugin getWorldEdit() {
		return wep;
	}

	public WorldGuardPlugin getWorldGuard() {
		return wgp;
	}

	public Set<String> getPlayersInSelection(Selection sel) {
		ProtectedRegion region = getRegion(sel,"a_temp_region");
		if (region == null)
			return null;
		HashSet<String> players = new HashSet<String>();
		RegionManager mgr = wgp.getRegionManager(sel.getWorld());
		ApplicableRegionSet ars = mgr.getApplicableRegions(region);
		for (ProtectedRegion pr: ars){
			DefaultDomain dd = pr.getOwners();
			players.addAll(dd.getPlayers());
		}
		return players;
	}

	public void setPermission(Chat provider) {
		this.chat = provider;
	}


}
