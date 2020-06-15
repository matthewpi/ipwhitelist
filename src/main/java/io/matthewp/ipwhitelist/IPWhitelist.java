package io.matthewp.ipwhitelist;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class manages the IPWhitelist plugin.
 */
public final class IPWhitelist extends JavaPlugin implements Listener {
    // allowedIPs holds the list of allowed ips.
    @Getter @Setter private List<String> allowedIPs = new ArrayList<>();

    /**
     * This method is called whenever the plugin is enabled.
     */
    @Override
    @SneakyThrows
    public void onEnable() {
        // Load the configuration.
        Config.init(this);

        // Register event listeners.
        this.getServer().getPluginManager().registerEvents(this, this);

        // Run repeating task to update "allowedIPs" every Config#REFRESH_DELAY seconds.
        this.getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            final List<String> allowedIPs = new ArrayList<>();

            for (String ip : Config.ALLOWED_IPS.stringList()) {
                // Check if the IP ends with a "." (this is how we know we should look it up as a DNS record)
                if (ip.endsWith(".")) {
                    ip = ip.substring(0, ip.length() - 1);

                    try {
                        // Lookup the string as a DNS record.
                        final InetAddress address = InetAddress.getByName(ip);

                        // Add the address if it doesn't exist already.
                        if (!allowedIPs.contains(address.getHostAddress())) {
                            allowedIPs.add(address.getHostAddress());
                        }
                    } catch (final UnknownHostException ex) {
                        this.getLogger().warning("UnknownHostException while looking up " + ip);
                    }

                    continue;
                }

                // Add the address if it doesn't exist already.
                if (!allowedIPs.contains(ip)) {
                    allowedIPs.add(ip);
                }
            }

            // Update the actual allowed ips list.
            this.setAllowedIPs(allowedIPs);

            // Log the new allowed ip list only if debug logs are enabled.
            if (Config.DEBUG.booleanValue()) {
                this.getLogger().info("Refreshing allowed IPs: " + Arrays.toString(this.getAllowedIPs().toArray()));
            }
        }, 0L, Config.REFRESH_DELAY.intValue() * 20L);
    }

    /**
     * Called whenever a player logins into the server.
     * Used to verify that the player is connecting from a trusted proxy.
     *
     * @param e {@link PlayerLoginEvent}
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerJoin(final PlayerLoginEvent e) {
        // Gets the player's original address before ip forwarding,
        // this will show us the proxy ip if the player is connecting through a proxy.
        final InetAddress address = e.getRealAddress();

        // Log that the player is joining and their ip only if debug logs are enabled.
        if (Config.DEBUG.booleanValue()) {
            this.getLogger().info(e.getPlayer().getName() + " is joining with IP: " + address.getHostAddress());
        }

        // Check if the player's address is allowed.
        if (this.getAllowedIPs().contains(address.getHostAddress())) {
            return;
        }

        // Disallow the player as they are not connecting from an allowed ip.
        e.disallow(
                PlayerLoginEvent.Result.KICK_WHITELIST,
                ChatColor.translateAlternateColorCodes(
                        '&',
                        Config.KICK_MESSAGE.toString()
                )
        );
    }
}
