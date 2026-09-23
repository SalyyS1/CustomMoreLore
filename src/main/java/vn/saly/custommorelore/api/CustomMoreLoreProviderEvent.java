package vn.saly.custommorelore.api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Fired when an extension provider is about to be registered. */
public final class CustomMoreLoreProviderEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final FragmentProvider provider;
    private boolean cancelled;
    public CustomMoreLoreProviderEvent(FragmentProvider provider) { this.provider = provider; }
    public FragmentProvider provider() { return provider; }
    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
