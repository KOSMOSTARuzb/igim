package com.kosmostar.instancemanager.gui.tabs;

import net.minecraft.client.gui.components.AbstractWidget;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface TabFooterProvider {
    /**
     * Returns a list of widgets (buttons, input fields, etc.) to show in the footer for this tab.
     * Return an empty list if no custom widgets are needed.
     */
    default List<AbstractWidget> createFooterWidgets(){
        return List.of();
    };

    @Nullable
    default Integer doneButtonSize() {
        return null;
    }

    default boolean renderBottomLine(){return true;}
}