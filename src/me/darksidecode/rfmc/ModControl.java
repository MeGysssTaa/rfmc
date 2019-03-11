/*
 * Copyright 2019 DarksideCode
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.darksidecode.rfmc;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.LoaderState;
import net.minecraftforge.fml.common.ModContainer;

import java.util.*;

/**
 * An utility class that allows one to control Forge/FML mods right at runtime with ease.
 */
public final class ModControl {

    private ModControl() {}

    /**
     * An additional optional map of mod IDs to mod controllers specific to them.
     * This may be useful to create a better compatibility with some certain mods.
     */
    private static final Map<String, ModController> modSpecificControllers = new HashMap<>();

    /**
     * List of mod-specific FML event listeners that have been disabled together with the mod.
     * Used to re-register them (re-subscribe the mod to events) when enabling mods back.
     *
     * IMPORTANT NOTE: when RFMC registers the disabled event listeners back, the listeners
     *                 are being registered BY THE CALLER MOD. This means that if you call
     *                 disableMod("someModId") and then enableMod("someModId"), then events
     *                 that had previously belonged to mod "someModId" now belong to your mod.
     *                 Although that "someModId" will still continue to function properly (at
     *                 least it should), this would normally lead to you being no longer able
     *                 to control that mod. In order to solve the issue, event handlers are
     *                 never removed from this map after being put, and are furthermore used
     *                 by the disableMod(...) method as a list of the desired mod's listeners.
     *
     *                 TODO: create a better solution? Would probably be better if we spoofed
     *                       the register(...) caller to Forge somehow...
     */
    private static final Map<ModContainer, List<Object>> disabledListeners = new HashMap<>();

    private static RFMCReflections reflections;

    /**
     * Attempts to find a mod with the specific mod ID and returns it in case it is found, or
     *
     * @throws NullPointerException if any of the specified parameters is null or empty/spaces-only..
     * @throws NoSuchElementException otherwise.
     *
     * @param modId ID to search the mod by.
     *
     * @return a ModContainer object holding a mod with the specified mod ID.
     */
    public static ModContainer getModById(String modId) {
        if ((modId == null) || (modId.trim().isEmpty()))
            throw new NullPointerException("modId cannot be null, empty, or spaces-only");
        
        return Loader.instance().getModList().stream().filter(m -> m.getModId().equals(modId)).
                findFirst().orElseThrow(() -> new NoSuchElementException("unknown mod id: " + modId));
    }

    /**
     * Returns the current (the last) state of mod with the specified ID.
     *
     * @throws NullPointerException if any of the specified parameters is null or empty/spaces-only..
     * @throws NoSuchElementException if there are no mods with the specified mod ID.
     *
     * @param modId ID to search the mod by.
     * @return the last known state of mod with the specified ID.
     */
    public static LoaderState.ModState getCurrentState(String modId) {
        if ((modId == null) || (modId.trim().isEmpty()))
            throw new NullPointerException("modId cannot be null, empty, or spaces-only");
        
        Collection<LoaderState.ModState> modStates = loadReflections().getModStates().get(modId);

        if (modStates == null)
            throw new NoSuchElementException("mod not loaded: id: " + modId);

        if (modStates.isEmpty())
            throw new IllegalStateException("mod states list for " +
                    "mod id: " + modId + " is empty - was the mod properly loaded?");

        Iterator<LoaderState.ModState> it = modStates.iterator();
        LoaderState.ModState lastState = null;

        while (it.hasNext())
            lastState = it.next();
        return lastState;
    }

    /**
     * Checks whether mod with the specified ID is currently enabled.
     *
     * @throws NullPointerException if any of the specified parameters is null or empty/spaces-only..
     * @throws NoSuchElementException if there are no mods with the specified mod ID.
     *
     * @param modId ID to search the mod by.
     * @return true if and only if the current (the last) state of mod with the specified ID is AVAILABLE.
     */
    public static boolean isModEnabled(String modId) {
        return getCurrentState(modId) == LoaderState.ModState.AVAILABLE;
    }

    /**
     * Checks whether mod with the specified ID is currently disabled.
     *
     * @throws NullPointerException if any of the specified parameters is null or empty/spaces-only..
     * @throws NoSuchElementException if there are no mods with the specified mod ID.
     *
     * @param modId ID to search the mod by.
     * @return true if and only if the current (the last) state of mod with the specified ID is DISABLED.
     */
    public static boolean isModDisabled(String modId) {
        return getCurrentState(modId) == LoaderState.ModState.DISABLED;
    }

    /**
     * Enables mod with the specified ID and re-registers its disabled listeners, if any.
     * If there is a mod-specific controller bound to the specified mod ID, the enableMod()
     * method of that controller is called first.
     *
     * The current (the last) state of mod with the specified ID is normally AVAILABLE
     * if this operation succeeds at least partially.
     *
     * @see ModControl#modSpecificControllers
     * @see ModController#enableMod()
     *
     * @throws NullPointerException if any of the specified parameters is null or empty/spaces-only..
     * @throws NoSuchElementException if there are no mods with the specified mod ID.
     * @throws IllegalStateException if mod with the specified ID is already enabled.
     *
     * @param modId ID to search the mod by.
     */
    public static void enableMod(String modId) {
        if (isModEnabled(modId))
            throw new IllegalStateException("mod id: " + modId + " is already enabled");

        ModController modSpecificController = modSpecificControllers.get(modId);

        if (modSpecificController != null)
            modSpecificController.enableMod();

        RFMCReflections reflections = loadReflections();
        reflections.getModStates().put(modId, LoaderState.ModState.AVAILABLE);

        ModContainer mod = getModById(modId);
        List<Object> listenersList = disabledListeners.get(mod);

        if (listenersList == null)
            throw new IllegalStateException("disabled listeners list is null for mod id: "
                    + modId + " - was the mod properly loaded? is it properly operated?");

        listenersList.forEach(MinecraftForge.EVENT_BUS::register);
        System.out.println("Silently enabled mod " + mod.getModId() + ". "
                + listenersList.size() + " event listeners have been renewed.");
    }

    /**
     * Disables mod with the specified ID and unregisters all its event listeners, if any.
     * If there is a mod-specific controller bound to the specified mod ID, the disableMod()
     * method of that controller is called first.
     *
     * The current (the last) state of mod with the specified ID is normally DISABLED
     * if this operation succeeds at least partially.
     *
     * @see ModControl#modSpecificControllers
     * @see ModController#disableMod()
     *
     * @throws NullPointerException if any of the specified parameters is null or empty/spaces-only..
     * @throws NoSuchElementException if there are no mods with the specified mod ID.
     * @throws IllegalStateException if mod with the specified ID is already disabled.
     *
     * @param modId ID to search the mod by.
     */
    public static void disableMod(String modId) {
        if (isModDisabled(modId))
            throw new IllegalStateException("mod id: " + modId + " is already disabled");

        ModController modSpecificController = modSpecificControllers.get(modId);

        if (modSpecificController != null)
            modSpecificController.disableMod();

        RFMCReflections reflections = loadReflections();
        reflections.getModStates().put(modId, LoaderState.ModState.DISABLED);

        ModContainer mod = getModById(modId);
        List<Object> listenersList = new ArrayList<>();

        for (Object listener : reflections.getListeners().keySet()) {
            if (reflections.getListenerOwners().get(listener) == mod) {
                System.out.println("Unregistered listener "
                        + listener + " that belonged to mod " + mod.getModId());
                MinecraftForge.EVENT_BUS.unregister(listener);
                listenersList.add(listener); // to allow further subscription renewal
            }
        }

        List<Object> previouslyDisabled = disabledListeners.get(mod);

        if (previouslyDisabled == null)
            disabledListeners.put(mod, listenersList); // to allow further subscription renewal
        else {
            // Why do this? See javadoc to field disabledListeners.
            // For the same reason, we don't want to update the disabledListeners list for
            // this mod anymore, since that will simply reset/clear the list forever.
            for (Object listener : previouslyDisabled) {
                System.out.println("Unregistered listener "
                        + listener + " that is known to have belonged to mod " + mod.getModId());
                MinecraftForge.EVENT_BUS.unregister(listener);
            }
        }

        System.out.println("Silently disabled mod " + mod.getModId() + ". " + (listenersList.size()
                + ((previouslyDisabled != null) ? previouslyDisabled.size() : 0))
                + " event listeners have been temporarily unregistered.");
    }

    /**
     * May be used externally at mod or client initialization to save time at run.
     * However, calling this is not required - it's also done automatically if skipped.
     */
    public static RFMCReflections loadReflections() {
        if (reflections == null) {
            try {
                reflections = new RFMCReflections();
            } catch (Exception ex) {
                throw new RuntimeException("failed to load " +
                        "RFMC reflections - RFMC will not work without them!", ex);
            }
        }

        return reflections;
    }

    /**
     * Returns the complete list of all mods that have mod-specific controllers bound to
     * them, as well as the controllers themselves.
     *
     * @see ModControl#modSpecificControllers
     */
    public static Map<String, ModController> getModSpecificControllers() {
        return modSpecificControllers;
    }

    /**
     * Assigns the specified mod controller to mod with the specified ID.
     *
     * @throws NullPointerException if any of the specified parameters is null or empty/spaces-only..
     * @throws IllegalStateException if there is already a mod controller bound to mod with the specified ID.
     *
     * @param modId ID of the mod to bind the specified mod-specific controller to.
     * @param controller controller to use with mod with the specified ID.
     */
    public static void registerModSpecificController(String modId, ModController controller) {
        if ((modId == null) || (modId.trim().isEmpty()))
            throw new NullPointerException("modId cannot be null, empty, or spaces-only");

        if (controller == null)
            throw new NullPointerException("controller cannot be null");

        if (modSpecificControllers.containsKey(modId))
            throw new IllegalStateException("there is already a mod-specific controller bound " +
                    "to mod id: " + modId + ": " + modSpecificControllers.get(modId).getClass().getName());

        modSpecificControllers.put(modId, controller);
    }

}
