# RFMC
## Runtime Forge Mod Control

An easy-to-use and lightweight library **(not a mod!)** that allows one to control Minecraft Forge/FML mods right at runtime. There is no proper API in Forge to do so, and thus RFMC uses nothing but black magic (reflections) to do its job.


## Usage
Just copy the `src/me/...` directory and clone it into your project's source folder. The only dependency is Forge. I personally used `1.8.9-11.15.1.2318`, but hope RFMC also works for other versions, or at least for the newer ones.


**Done?** Now you can use code like this to control Forge mods:

```java
private static final String KEYSTROKES_MOD_ID = "keystrokesmod";

public void toogleKeystrokesMod() {
    if (ModControl.isModEnabled(KEYSTROKES_MOD_ID)) {
        // Since now, the mod will be displayed as disabled in the Forge mods list,
        // and all its event handlers are unregistered.
        System.out.println("The mod was enabled. Disabling it!");
        ModControl.disableMod(KEYSTROKES_MOD_ID);
    } else {
        // Since now, the mod will be displayed as enabled in the Forge mods list,
        // and all its event handlers that were disabled previously are registered again.
        System.out.println("The mod was disabled. Enabling it!");
        ModControl.enableMod(KEYSTROKES_MOD_ID);
    }
}
```


The default implementation disables mods by switching their state to `DISABLED` and unregistering all their `MinecraftForge.EVENT_BUS` event handlers. The process of enabling simply does the opposite things, with new mod state being set to `AVAILABLE`.


If one or more of your mods require something more to be disabled, or are even courteous enough to provide some API for runtime control, you can always add mod-specific controllers as follows:
```java
public class CustomModController implements ModController {
    
    public CustomModController() {
        ModControl.registerModSpecificController("custom-mod-id", this);
    }
    
    public void enableMod() {
        CustomModAPI.enableProperlyKThx();
    }
    
    public void disableMod() {
        CustomModAPI.disableProperlyKThx();
    }
    
}
```

Mod-specific controllers are called right before the basic implementation which was described above.


### Contributing
Any pull requests are always welcome, for example, if you want to add a publicly available mod-specific controller or do something else which may be cool for people using RFMC.


Issues and bugs are not really welcome, obviously. However, if you believe something is wrong with the code and want me to fix it, please **open a proper issue** describing your problem in details.


Have fun using RFMC!


### License
Apache License 2.0
