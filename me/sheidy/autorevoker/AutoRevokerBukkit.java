package me.sheidy.autorevoker;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import me.leoko.advancedban.bukkit.event.PunishmentEvent;
import me.leoko.advancedban.bukkit.event.RevokePunishmentEvent;

public class AutoRevokerBukkit extends JavaPlugin implements Listener, CommandExecutor {

    private AutoRevoker autoRevoker;

    @Override
    public void onEnable() {
        autoRevoker = new AutoRevoker(getLogger());
        autoRevoker.start();
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("ars").setExecutor(this);
    }

    @Override
    public void onDisable() {
        autoRevoker.interrupt();
        autoRevoker = null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        sender.sendMessage("AutoRevoker > " + autoRevoker.getStatus());
        return true;
    }

    @EventHandler
    public void onPunishment(PunishmentEvent event) {
        if (event.getPunishment().getType().isTemp()) {
            autoRevoker.addPunishment(event.getPunishment());
        }
    }

    @EventHandler
    public void onRevokePunishment(RevokePunishmentEvent event) {
        if (event.getPunishment().getType().isTemp()) {
            autoRevoker.removePunishment(event.getPunishment());
        }
    }
}
