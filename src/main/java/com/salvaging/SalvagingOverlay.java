package com.salvaging;

import net.runelite.api.MenuAction;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;

import java.awt.*;

import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

public class SalvagingOverlay extends OverlayPanel {
    private final SalvagingPlugin plugin;

    @Inject
    private SalvagingOverlay(SalvagingPlugin plugin) {
        super(plugin);
        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
        this.plugin = plugin;
        addMenuEntry(MenuAction.RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Salvaging overlay");
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if ((plugin.isOnBoat()) && plugin.getWrecks().isEmpty()) {
            return super.render(graphics);
        }
        boolean playerSalvaging = plugin.isPlayerSalvaging();

        String player = playerSalvaging ? "Yes":"No";
        Color playerColor = playerSalvaging ? Color.GREEN:Color.RED;

        int crewCount = plugin.getCrewmates().size();
        int crewSalvaging = (int) plugin.getCrewSalvaging().values().stream().filter(b -> b).count();
        String crewSalvage = crewSalvaging + "/" + crewCount;
        Color crewSalvageColor = (crewSalvaging > 0) ? Color.GREEN:Color.RED;

        panelComponent.getChildren().add(TitleComponent.builder().text("Salvaging").color(Color.WHITE).build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Player Salvaging:").leftColor(playerColor)
                .right(player).rightColor(playerColor)
                .build());
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Crew Salvaging:").leftColor(crewSalvageColor)
                .right(crewSalvage).rightColor(crewSalvageColor)
                .build());

        if (plugin.isCargoFull()) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Cargo:").leftColor(Color.RED)
                    .right("FULL").rightColor(Color.RED)
                    .build());
        }


        panelComponent.setPreferredSize(new Dimension(graphics.getFontMetrics().stringWidth("Player Salvaging: Yes") + 10, 0));

        return super.render(graphics);
    }
}
