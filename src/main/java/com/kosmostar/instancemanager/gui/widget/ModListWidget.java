package com.kosmostar.instancemanager.gui.widget;


import com.kosmostar.instancemanager.gui.tabs.ModsTab;
import com.kosmostar.instancemanager.mod.Mod;
import com.kosmostar.instancemanager.mod.ModManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ObjectSelectionList;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class ModListWidget extends ObjectSelectionList<ModListEntry> {

    private final ModsTab parentTab;
    private final Map<Mod, List<Mod>> allMods = new LinkedHashMap<>();
    private final Set<String> expandedParentIds = new HashSet<>();
    private String currentQuery = "";


    public ModListWidget(Minecraft minecraft, int width, int height, int y, int itemHeight, ModsTab parentTab) {
        super(minecraft, width, height, y, itemHeight);
        this.parentTab = parentTab;
    }

    public void setMods(List<Mod> mods) {
        this.clearEntries();
        this.allMods.clear();

        for (Mod mod : mods) {
            Mod topParent = ModManager.getTopParent(mod);
            allMods.computeIfAbsent(topParent, _ -> new ArrayList<>());
            if (mod != topParent) {
                allMods.get(topParent).add(mod);
            }
        }

        filter("");
    }


    public void filter(String query) {
        this.currentQuery = query;
        Mod previouslySelected = this.getSelected() != null ? this.getSelected().getModInfo() : null;

        this.clearEntries();
        this.setSelected(null);

        String lowercaseQuery = query.toLowerCase(Locale.ROOT).trim();
        ModListEntry entryToSelect = null;

        for (Map.Entry<Mod, List<Mod>> entry : allMods.entrySet()) {
            Mod parent = entry.getKey();
            List<Mod> children = entry.getValue();

            if (lowercaseQuery.isEmpty()
                    || parent.getName().toLowerCase(Locale.ROOT).contains(lowercaseQuery)
                    || parent.getId().toLowerCase(Locale.ROOT).contains(lowercaseQuery)) {

                ModListEntry parentEntry = new ModListEntry(parent, this::toggleExpand, this::setSelected);
                this.addEntry(parentEntry);

                boolean isExpanded = expandedParentIds.contains(parent.getId());
                if (!children.isEmpty()) {
                    parentEntry.setExpanded(isExpanded);
                }

                if (previouslySelected != null && parent.getId().equals(previouslySelected.getId())) {
                    entryToSelect = parentEntry;
                }

                if (isExpanded) {
                    for (int i = 0; i < children.size(); i++) {
                        Mod child = children.get(i);
                        ModListEntry childEntry = new ModListEntry(child, this::setSelected);
                        childEntry.setIsBottomChild(i == children.size() - 1);
                        this.addEntry(childEntry);
                        if (previouslySelected != null && child.getId().equals(previouslySelected.getId())) {
                            entryToSelect = childEntry;
                        }
                    }
                }
            }
        }


        if (entryToSelect != null) {
            this.setSelected(entryToSelect);
        } else if (!this.children().isEmpty()) {
            this.setSelected(this.children().getFirst());
        }
    }

    private void toggleExpand(ModListEntry entry) {
        String modId = entry.getModInfo().getId();
        if (expandedParentIds.contains(modId)) {
            expandedParentIds.remove(modId);
        } else {
            expandedParentIds.add(modId);
        }

        filter(this.currentQuery);
    }

    @Override
    public void setSelected(@Nullable ModListEntry entry) {
        super.setSelected(entry);
        if (entry != null) {
            this.parentTab.updateSelectedMod(entry.getModInfo());
        }
    }

    @Override
    public int getRowWidth() {
        return Math.max(1, this.width - 16);
    }

    @Override
    public int getRowLeft() {
        return this.getX() + 4;
    }
}