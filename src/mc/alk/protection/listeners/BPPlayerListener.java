package mc.alk.protection.listeners;

import mc.alk.protection.controllers.ProtectionController;

import org.bukkit.event.Listener;


/**
 * 
 * @author alkarin
 *
 */
public class BPPlayerListener implements Listener  {
	ProtectionController pc;
	
	public BPPlayerListener(ProtectionController pc){
		this.pc = pc;
	}
//	
//	@EventHandler
//	 public void onPlayerInteract(PlayerInteractEvent event) {
//    	if (event.isCancelled()) return;
//        final Block clickedBlock = event.getClickedBlock();
//        if (clickedBlock == null) return; /// This can happen, minecraft is a strange beast
//        final Player player = event.getPlayer();
//        if (player.getItemInHand().getType() != Material.WOOD_AXE){
//        	return;
//        }
//        
////        if (!pc.canBuild(player.getLocation())){
////        	
////        }
//	}
}
