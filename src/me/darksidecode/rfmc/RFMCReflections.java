package me.darksidecode.rfmc;

import com.google.common.collect.Multimap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.LoadController;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.LoaderState;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.IEventListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides simpler access and caching for our use of FML-related reflections.
 */
public class RFMCReflections {

    private final Multimap<String, LoaderState.ModState> modStates;
    private final ConcurrentHashMap<Object, ArrayList<IEventListener>> listeners;
    private final Map<Object, ModContainer> listenerOwners;

    /**
     * Instantiate a new RFMCReflections object, load and cache the necessary reflection data.
     * Only for internal use - there should normally be no need in this class externally.
     */
    public RFMCReflections() throws Exception {
        // loadController
        Loader loader = Loader.instance();
        Class loaderClass = loader.getClass();
        Field modCtrlField = loaderClass.getDeclaredField("modController");

        modCtrlField.setAccessible(true);

        LoadController loadController = (LoadController) modCtrlField.get(loader);
        Class loadCtrlClass = loadController.getClass();

        // modStates
        Field modStatesField = loadCtrlClass.getDeclaredField("modStates");
        modStatesField.setAccessible(true);
        modStates = autocast(modStatesField.get(loadController));

        // Primary Forge event bus's class. Used to obtain listeners and listenerOwners.
        Class eventBusClass = MinecraftForge.EVENT_BUS.getClass();

        // listeners
        Field listenersField = eventBusClass.getDeclaredField("listeners");
        listenersField.setAccessible(true);
        listeners = autocast(listenersField.get(MinecraftForge.EVENT_BUS));

        // listenerOwners
        Field listenerOwnersField = eventBusClass.getDeclaredField("listenerOwners");
        listenerOwnersField.setAccessible(true);
        listenerOwners = autocast(listenerOwnersField.get(MinecraftForge.EVENT_BUS));
    }

    public Multimap<String, LoaderState.ModState> getModStates() {
        return modStates;
    }

    public ConcurrentHashMap<Object, ArrayList<IEventListener>> getListeners() {
        return listeners;
    }

    public Map<Object, ModContainer> getListenerOwners() {
        return listenerOwners;
    }

    /**
     * Used to avoid extremely cumbersome notations like
     *     ConcurrentHashMap<Object, ArrayList<IEventListener>> map = (ConcurrentHashMap<Object, ArrayList<IEventListener>>) sth.
     * Allows us to just do something like
     *     ConcurrentHashMap<Object, ArrayList<IEventListener>> map = autocast(sth).
     */
    private <T> T autocast(Object o) {
        return (T) o;
    }

}
