package com.salvaging;

import com.google.inject.Provides;
import javax.inject.Inject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.util.*;

@Slf4j
@PluginDescriptor(
	name = "Shipwreck Salvaging"
)
public class SalvagingPlugin extends Plugin
{
	@Inject private Client client;
    @Inject OverlayManager overlayManager;
    @Inject
    Notifier notifier;

	@Inject private SalvagingConfig config;
    @Inject private SalvagingOverlay overlay;

    @Getter
    private boolean playerSalvaging = false;
    private boolean playerSorting = false;

    @Getter
    private final Set<GameObject> wrecks = new HashSet<>();

    @Getter
    private boolean isOnBoat = false;

    @Getter
    private boolean cargoFull = false;

    @Getter
    private final HashMap<Actor, Boolean> crewSalvaging = new HashMap<>();
    private final HashMap<Actor, Integer> crewIdleTicks = new HashMap<>();

    @Getter
    private final Set<Actor> crewmates = new HashSet<>();

    private boolean playerSalvageTracker = false;
    private boolean playerSortTracker = false;
    private boolean crewSalvageTracker = false;
    private boolean fullCargoTracker = false;

	@Override
	protected void startUp()
	{
        overlayManager.add(overlay);
		log.debug("Salvaging plugin started!");
	}

	@Override
	protected void shutDown()
	{
        overlayManager.remove(overlay);
		log.debug("Salvaging plugin stopped!");
	}

    @Subscribe
    public void onGameStateChanged(GameStateChanged state) {
        if (state.getGameState() == GameState.LOGGING_IN || state.getGameState() == GameState.HOPPING) { //Reset vars
            crewSalvaging.clear();
            crewIdleTicks.clear();
            crewmates.clear();
            playerSalvageTracker = false;
            playerSortTracker = false;
            crewSalvageTracker = false;
            fullCargoTracker = false;
        }
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged chg) {
        if (chg.getVarbitId() == VarbitID.SAILING_BOARDED_BOAT) {
            isOnBoat = chg.getValue()==1;
        }
    }

    @Subscribe
    public void onWorldViewUnloaded(WorldViewUnloaded e) {
        if (e.getWorldView().isTopLevel()) {
            wrecks.clear();
        }
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned event) {
        NPC npc = event.getNpc();
        if (!isCrewmateName(npc.getName()) || npc.getWorldView() != client.getLocalPlayer().getWorldView()) {
            return;
        }

        if (!crewmates.contains(npc)) {
            log.debug("Found Crewmate: {}", npc.getName());
            crewmates.add(npc);
            crewSalvaging.put(npc, false);
            crewIdleTicks.put(npc, 0);
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned event) {
        NPC npc = event.getNpc();
        crewmates.remove(npc);
        crewSalvaging.remove(npc);
        crewIdleTicks.remove(npc);
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned obj) {
        int objId = obj.getGameObject().getId();
        if (isObjectIdShipwreck(objId)) {
            if (!wrecks.contains(obj.getGameObject())) {
                log.debug("Adding Shipwreck");
                wrecks.add(obj.getGameObject());
            }
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned obj) {
        wrecks.remove(obj.getGameObject());
        //if (obj.getGameObject().getRenderable().getModel())
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        for (Actor crew : crewmates) {
            int currentIdle = crewIdleTicks.get(crew);
            if (!crewSalvaging.get(crew) && currentIdle < 10) {//if not salvaging, cap at 10 ticks
                crewIdleTicks.replace(crew, currentIdle + 1);
                handleSalvageUpdate();
            }
        }
    }

    @Subscribe
    public void onAnimationChanged(AnimationChanged animationChanged) {
        final Actor actor = animationChanged.getActor();
        final int anim = actor.getAnimation();

        if (actor.getWorldView().isTopLevel()) {
            return;
        }

        if (actor == client.getLocalPlayer()) {
            playerSalvaging = isAnimationSalvaging(anim);
            playerSorting = (anim == 13599);
        } else if (crewmates.contains(actor)) {
            if (isAnimationSalvaging(anim)) {
                if (cargoFull) {
                    cargoFull = false;
                }
                crewSalvaging.replace(actor, true);
            } else {
                crewSalvaging.replace(actor, false);
                crewIdleTicks.replace(actor, 0);
            }
        }
        handleSalvageUpdate();
    }

    @Subscribe
    public void onChatMessage(ChatMessage msg) {
        if (msg.getType() == ChatMessageType.MESBOX) {
            if (msg.getMessage().matches("^Your inventory is too full to hold any more \\w* salvage.")) {
                log.debug("Inv full on salvage");
            }
        } else if (msg.getType() == ChatMessageType.SPAM) {
            if (msg.getMessage().equals("Your crewmate on the salvaging hook cannot salvage as the cargo hold is full.")) {
                cargoFull = true;
                log.debug("Cargo full on salvage");
                handleSalvageUpdate();
            }
        }
    }

    @Provides
    SalvagingConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SalvagingConfig.class);
	}

    private void handleSalvageUpdate() {
        if (isPlayerSalvaging() != playerSalvageTracker) { //we have an update
            if (!isPlayerSalvaging()) { //we are no longer salvaging
                notifier.notify(config.playerStopSalvageNotif(), "Salvaging: Player stopped salvaging");
            }
            playerSalvageTracker = isPlayerSalvaging();
        }

        boolean crewSalvage = getCrewSalvaging().values().stream().anyMatch(b -> b);
        if (crewSalvage != crewSalvageTracker) { //we have an update
            if (crewSalvage) { //Crew have started salvaging
                if(maxCrewIdleTicks() > 3) {
                    notifier.notify(config.crewStartSalvageNotif(), "Salvaging: Crew started salvaging");
                    crewSalvageTracker = true;
                }
            } else { //Crew have stopped salvaging
                if(maxCrewIdleTicks() > 3) {
                    notifier.notify(config.crewStopSalvageNotif(), "Salvaging: Crew stopped salvaging");
                    crewSalvageTracker = false;
                }
            }
        }

        if (playerSorting != playerSortTracker) {
            if (!playerSorting) { //We are no longer sorting
                notifier.notify(config.playerStopSortNotif(), "Salvaging: Player stopped sorting salvage");
            }
            playerSortTracker = playerSorting;
        }

        if(cargoFull != fullCargoTracker) {
            if (cargoFull) {
                notifier.notify(config.cargoFullNotif(), "Salvaging: Full cargo");
            }
            fullCargoTracker = cargoFull;
        }
    }

    private boolean isAnimationSalvaging(int anim) {
        return anim == 13576 || anim == 13577 || anim == 13584 || anim == 13583;
    }

    private boolean isObjectIdShipwreck(int objectId) {
        return (objectId >= 60464 && objectId <= 60479);
    }

    private boolean isCrewmateName(String name) {
        String[] names = {
                "Jobless Jim", "Ex-Captain Siad", "Adventurer Ada",
                "Cabin Boy Jenkins", "Oarswoman Olga", "Jittery Jim",
                "Bosun Zarah", "Jolly Jim", "Spotter Virginia", "Sailor Jakob"};
        return Arrays.asList(names).contains(name);
    }

    private int maxCrewIdleTicks() {
        int max = 0;
        for (int i : crewIdleTicks.values()) {
            if (i > max && i != 10) {
                max = i;
            }
        }
        return max;
    }
}
