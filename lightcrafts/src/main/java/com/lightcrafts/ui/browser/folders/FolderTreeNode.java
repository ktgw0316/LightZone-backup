/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2018-     Masahiro Kitagawa */

package com.lightcrafts.ui.browser.folders;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.utils.directory.DirectoryMonitor;
import com.lightcrafts.utils.file.FileUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * A TreeNode representing a folder.
 */
class FolderTreeNode implements TreeNode {

    private final static FileSystemView FileSystemView =
        Platform.getPlatform().getFileSystemView();

    @NotNull
    private List<FolderTreeNode> children = new ArrayList<>();

    @Getter
    private FolderTreeNode parent;

    @Getter
    private NodeFileIndex index;  // Maintain the tree-wide index

    @Getter
    private DirectoryMonitor directoryMonitor;   // Update the watched folder list

    private File file;            // Determines the icon, node identity

    private File resolvedFile;    // For getFile(), indexing

    private Icon icon;    // FileSystemView icon of "file" (not "resolvedFile")

    private Boolean isLeaf;     // Cached isLeaf(), for efficiency

    private String name;        // Cached toString(), for efficiency

    private boolean isDropTarget;   // Gets a highlight in the cell renderer

    static FolderTreeNode createRoot() {
        NodeFileIndex index = new NodeFileIndex();
        DirectoryMonitor monitor = Platform.getPlatform().getDirectoryMonitor();
        if (Platform.isMac()) {
            return new MacOSXRootTreeNode(index, monitor);
        } else {
            File[] roots = FileSystemView.getRoots();
            return new FolderTreeNode(roots[0], null, index, monitor);
        }
    }

    FolderTreeNode(File file,
        FolderTreeNode parent,
        NodeFileIndex index,
        DirectoryMonitor directoryMonitor) {
        this.file = file;
        this.parent = parent;
        this.index = index;
        this.directoryMonitor = directoryMonitor;
        resolvedFile = FileUtil.resolveAliasFile(this.file);
        try {
            resolvedFile = resolvedFile.getCanonicalFile();
        }
        catch (IOException e) {
            // Accept the non-canonical instance
            e.printStackTrace();
        }
        index.add(this);
        directoryMonitor.addDirectory(resolvedFile);
    }

    void setIsDropTarget(boolean isTarget) {
        isDropTarget = isTarget;
    }

    boolean isDropTarget() {
        return isDropTarget;
    }

    @Override
    public Enumeration<FolderTreeNode> children() {
        final Iterator<FolderTreeNode> i = getChildren().iterator();
        return new Enumeration<FolderTreeNode>() {
            @Override
            public boolean hasMoreElements() {
                return i.hasNext();
            }

            @Override
            public FolderTreeNode nextElement() {
                return i.next();
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (! (o instanceof FolderTreeNode)) {
            return false;
        }
        FolderTreeNode node = (FolderTreeNode) o;
        return file.equals(node.file);
    }

    @Override
    public boolean getAllowsChildren() {
        return !isLeaf();
    }

    @Override
    public TreeNode getChildAt(int index) {
        return getChildren().get(index);
    }

    @Override
    public int getChildCount() {
        return getChildren().size();
    }

    @Override
    public int getIndex(TreeNode node) {
        assert node instanceof FolderTreeNode;
        return getChildren().indexOf(node);
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }

    @Override
    public boolean isLeaf() {
        if (isLeaf == null) {
            isLeaf = isLeaf(file);
        }
        return isLeaf;
    }

    /**
     * Returns the name of the folder this node represents as it would be
     * displayed in the native file browser of the OS.
     */
    @Override
    public String toString() {
        if (name == null) {
            name = Platform.getPlatform().getDisplayNameOf(file);
        }
        return name;
    }

    /**
     * Gets the child node having the given name.  This is needed for
     * reconstructing tree paths from their serialized form.
     */
    FolderTreeNode getChildByName(String name) {
        return getChildren().stream()
                .filter(child -> name.equals(child.toString()))
                .findFirst()
                .orElse(null);
    }

    File getFile() {
        return resolvedFile;
    }

    /**
     * Gets the icon of the folder this node represents as it would be
     * displayed in the native file browser of the OS.
     *
     * @return Returns said icon.
     */
    Icon getIcon() {
        if ((icon == null) && (file != null)) {
            try {
                icon = FileSystemView.getSystemIcon(file);
            }
            catch (Throwable t) {
                // ignore
            }
        }
        return icon;
    }

    TreePath getTreePath() {
        ArrayList<TreeNode> path = new ArrayList<>();
        //
        // Construct the TreePath in reverse, i.e., from the current node back
        // to the root, because it's easier.
        //
        for (TreeNode node = this; node != null; node = node.getParent()) {
            path.add(0, node);
        }
        return new TreePath(path.toArray());
    }

    /**
     * Checks whether the file this node represents has a special icon, for
     * the Windows tree cell renderer.
     */
    boolean hasSpecialIcon() {
        return  FileSystemView.isDrive(file)
                || FileSystemView.isFileSystem(file)
                || FileSystemView.isRoot(file);
    }

    // Compute (or recompute) the children of this node.  Useful in the TreeNode
    // methods, and also when folder modifications are detected.
    synchronized void updateChildren() {
        children.clear();
        final File[] files = FileSystemView.getFiles(resolvedFile, true);
        assert files != null;
        Arrays.stream(files)
                .sorted()
                .filter(File::isDirectory)
                .forEach(it -> children.add(new FolderTreeNode(it, this, index, directoryMonitor)));
        children.sort(FolderTreeNodeComparator.INSTANCE);
    }

    List<FolderTreeNode> getChildren() {
        if (children.isEmpty()) {
            updateChildren();
        }
        return children;
    }

    /**
     * Checks whether the given File is a leaf node.  For our purposes, a leaf
     * node must be a file and not an alias to a folder.
     */
    private static boolean isLeaf(File file) {
        if (FileSystemView.isDrive(file)
                || FileSystemView.isFileSystemRoot(file)
                || FileSystemView.isFloppyDrive(file)
                || FileSystemView.isRoot(file)) {
            return false;
        }
        file = FileUtil.isFolder(file);
        return file == null;
/*
        if (file == null)
            return true;
        if (file instanceof SmartFolder) {
            //
            // Always consider a SmartFolder to be a non-leaf node because
            // determining whether it contains subfolders via listFiles() may
            // be expensive.
            //
            return false;
        }
        return !FileUtil.containsAtLeastOne(file, FolderFilter.INSTANCE);
*/
    }
}
/* vim:set et sw=4 ts=4: */
