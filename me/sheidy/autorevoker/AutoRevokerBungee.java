package me.sheidy.autorevoker;

import me.leoko.advancedban.bungee.event.PunishmentEvent;
import me.leoko.advancedban.bungee.event.RevokePunishmentEvent;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class AutoRevokerBungee extends Plugin implements Listener {

    private AutoRevoker autoRevoker;

    @Override
    public void onEnable() {
        autoRevoker = new AutoRevoker(getLogger());
        autoRevoker.start();
        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getPluginManager().registerCommand(this, new ARSCommand());
    }

    @Override
    public void onDisable() {
        autoRevoker.interrupt();
        autoRevoker = null;
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

    private class ARSCommand extends Command {

        public ARSCommand() {
            super("ars", "autorevoker.status", "autorevoker");
        }

        @Override
        @SuppressWarnings("deprecation")
        public void execute(CommandSender sender, String[] args) {
            sender.sendMessage("AutoRevoker > " + autoRevoker.getStatus());
        }        
    }
}
