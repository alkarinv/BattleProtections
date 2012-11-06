package mc.alk.protection;

import mc.alk.protection.controllers.ProtectionController;
import mc.alk.protection.executors.ProtectionExecutor;
import mc.alk.protection.listeners.BPPlayerListener;
import net.milkbowl.vault.Vault;
import net.milkbowl.vault.chat.Chat;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.alk.controllers.MC;
import com.alk.util.Log;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

public class Protections extends JavaPlugin{
	private static Protections plugin;
	static private String pluginname;
	static private String version;
	ProtectionController protectionController = new ProtectionController();

	@Override
	public void onEnable() {
		plugin = this;
		PluginDescriptionFile pdfFile = this.getDescription();
		pluginname = pdfFile.getName();
		version = pdfFile.getVersion();
		if (!loadVault()){
			Log.err(pluginname + " needs Vault to function.  Disabling");
			return;
		}

		if (!loadWorldGuardPlugin()){
			Log.err(pluginname + " needs WorldEdit and Worldguard to function. Disabling");
			return;
		}
		getCommand("protect").setExecutor(new ProtectionExecutor(protectionController,new MC()));
		Bukkit.getPluginManager().registerEvents(new BPPlayerListener(protectionController), this);
		Log.info(pluginname +":" + version +" enabled!");
	}

	public static Protections getSelf() {return plugin;}

	private boolean loadVault(){
		Vault vp = (Vault) Bukkit.getServer().getPluginManager().getPlugin("Vault");
		if (vp != null) {
			RegisteredServiceProvider<Chat> chatProvider = Bukkit.getServicesManager().getRegistration(Chat.class);
			if (chatProvider==null || chatProvider.getProvider() == null){
				return false;
			} else {
				protectionController.setPermission(chatProvider.getProvider());
			}
		}
		return true;
	}
	private boolean loadWorldGuardPlugin() {
		Plugin plugin= Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");

		if (plugin == null) {
			Log.err(getVersion()+" WorldEdit not detected!");
			return false;
		}
		protectionController.wep = ((WorldEditPlugin) plugin);


		if (protectionController.wgp != null) {
			return true;
		}
		plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");

		if (plugin == null) {
			System.out.println(getVersion()+ " WorldGuard not detected!");
			return false;
		}
		protectionController.wgp = ((WorldGuardPlugin) plugin);
		return true;
	}

	public static String getVersion() {return "[" + pluginname + " v" + version +"]";}

}
