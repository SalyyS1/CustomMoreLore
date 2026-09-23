package vn.saly.custommorelore.model;

/** Capability report used to keep unsupported MMOItems versions fail-soft. */
public record IntegrationCapabilities(boolean mmoItems, boolean mythicLib,
                                      boolean placeholderApi, boolean loreEvent,
                                      boolean primaryThreadHook, boolean fragmentCapture) {
    public boolean nativeHookAvailable() { return mmoItems && loreEvent && primaryThreadHook; }
}
