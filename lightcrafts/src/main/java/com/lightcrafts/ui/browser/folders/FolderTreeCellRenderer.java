/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.folders;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.LightZoneSkin;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

/**
 * A tree cell renderer that uses the specialized icons provided by
 * FolderTreeNode.
 */
class FolderTreeCellRenderer extends DefaultTreeCellRenderer {

    static TreeCellRenderer createRenderer() {
        return Platform.isWindows() ?
            new WindowsFolderTreeCellRenderer() :
            new FolderTreeCellRenderer();
    }

    private Font normalFont;

    private FolderTreeCellRenderer() {
        setBackgroundNonSelectionColor(LightZoneSkin.Colors.ToolPanesBackground);
        setTextNonSelectionColor(LightZoneSkin.Colors.ToolPanesForeground);
    }

    @Override
    public Component getTreeCellRendererComponent(
         JTree tree,
         Object value,
         boolean isSelected,
         boolean isExpanded,
         boolean isLeaf,
         int row,
         boolean hasFocus
    ) {
        super.getTreeCellRendererComponent(
            tree, value, isSelected, isExpanded, isLeaf, row, hasFocus
        );
        FolderTreeNode node = (FolderTreeNode) value;
        
        setIcon(node.getIcon());

        if (normalFont == null) {
            normalFont = getFont();
        }
        if (node.isDropTarget()) {
            Font font = normalFont.deriveFont(Font.BOLD);
            setFont(font);
        }
        else {
            setFont(normalFont);
        }
        return this;
    }
}
/* vim:set et sw=4 ts=4: */
