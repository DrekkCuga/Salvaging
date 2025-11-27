package com.salvaging;

import net.runelite.client.config.*;

@ConfigGroup("salvaging")
public interface SalvagingConfig extends Config
{
    @ConfigItem(
            keyName = "crewStopSalvageNotif",
            name = "Crew Stop Salvage Notification",
            description = "Send notification when all crewmates stop salvaging"
    )
    default Notification crewStopSalvageNotif() {
        return Notification.ON;
    }

    @ConfigItem(
            keyName = "crewStartSalvageNotif",
            name = "Crew Start Salvage Notification",
            description = "Send notification when any crewmates start salvaging"
    )
    default Notification crewStartSalvageNotif() {
        return Notification.ON;
    }

    @ConfigItem(
            keyName = "playerStopSalvageNotif",
            name = "Player Stop Salvage Notification",
            description = "Send notification when the player stops salvaging"
    )
    default Notification playerStopSalvageNotif() {
        return Notification.ON;
    }

    @ConfigItem(
            keyName = "playerStopSortNotif",
            name = "Player Stop Sorting Notification",
            description = "Send notification when the player stops sorting salvage"
    )
    default Notification playerStopSortNotif() {
        return Notification.ON;
    }

    @ConfigItem(
            keyName = "cargoFullNotif",
            name = "Full Cargo Notification",
            description = "Send notification when ships cargo is full"
    )
    default Notification cargoFullNotif() {
        return Notification.ON;
    }
}
