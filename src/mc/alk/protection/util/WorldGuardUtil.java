package mc.alk.protection.util;

import java.io.File;
import java.io.IOException;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.sk89q.minecraft.util.commands.CommandContext;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalConfiguration;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.bukkit.selections.CuboidSelection;
import com.sk89q.worldedit.bukkit.selections.Selection;
import com.sk89q.worldedit.data.DataException;
import com.sk89q.worldedit.schematic.SchematicFormat;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

public class WorldGuardUtil {
	public static WorldEditPlugin wep;
	public static WorldGuardPlugin wgp;


	public static boolean wgRegionConflict(Chunk c, Player p) {
		final Location l1 = c.getBlock(0, 0, 0).getLocation(), l2 = c.getBlock(15, 255, 15).getLocation();
		final Selection sel = new CuboidSelection(l1.getWorld(),l1,l2);
		final RegionManager mgr = wgp.getGlobalRegionManager().get(sel.getWorld());

		final BlockVector min = sel.getNativeMinimumPoint().toBlockVector();
		final BlockVector max = sel.getNativeMaximumPoint().toBlockVector();
		final ProtectedRegion region = new ProtectedCuboidRegion("temp_util_region", min, max);

		final LocalPlayer wgplayer = wgp.wrapPlayer(p);
		final ApplicableRegionSet ars = mgr.getApplicableRegions(region);
		return (!ars.canBuild(wgplayer));
	}

	public static boolean hasWorldGuard() {
		return wgp != null && wep != null;
	}


	public static boolean pasteSchematic(Player p, Location loc, String schematic){
		CommandContext cc = null;
		String args[] = {"load", schematic};
		final WorldEdit we = wep.getWorldEdit();
		final LocalSession session = wep.getSession(p);
		final BukkitPlayer lPlayer = wep.wrapPlayer(p);

		EditSession editSession = session.createEditSession(lPlayer);
		Vector pos = new Vector(loc.getBlockX(),loc.getBlockY(),loc.getBlockZ());
		try {
			cc = new CommandContext(args);
			return loadAndPaste(cc, we, session, lPlayer,editSession,pos);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	/**
	 * This is just copied and pasted from world edit source, with small changes to also paste
	 * @param args
	 * @param we
	 * @param session
	 * @param player
	 * @param editSession
	 */
	public static boolean loadAndPaste(CommandContext args, WorldEdit we,
			LocalSession session, com.sk89q.worldedit.LocalPlayer player, EditSession editSession, Vector pos) {

		LocalConfiguration config = we.getConfiguration();

		String filename = args.getString(0);
		File dir = we.getWorkingDirectoryFile(config.saveDir);
		File f = null;
		try {
			f = we.getSafeOpenFile(player, dir, filename, "schematic",new String[] {"schematic"});
			String filePath = f.getCanonicalPath();
			String dirPath = dir.getCanonicalPath();

			if (!filePath.substring(0, dirPath.length()).equals(dirPath)) {
				player.printError("Schematic could not read or it does not exist.");
				return false;
			} 
			SchematicFormat format = SchematicFormat.getFormat(f);
	        if (format == null) {
	            player.printError("Unknown schematic format for file" + f);
	            return false;
	        }

            if (!filePath.substring(0, dirPath.length()).equals(dirPath)) {
                player.printError("Schematic could not read or it does not exist.");
            } else {
                session.setClipboard(format.load(f));
                WorldEdit.logger.info(player.getName() + " loaded " + filePath);
                player.print(filePath + " loaded");
            }
            session.getClipboard().paste(editSession, pos, false, true);
            WorldEdit.logger.info(player.getName() + " pasted schematic" + filePath +"  at " + pos);            
		} catch (DataException e) {
			player.printError("Load error: " + e.getMessage());
		} catch (IOException e) {
			player.printError("Schematic could not read or it does not exist: " + e.getMessage());
		} catch (Exception e){
			player.printError("Error : " + e.getMessage());
		}
		return true;
	}

}
