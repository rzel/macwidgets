package com.explodingpixels.macwidgets;

import com.explodingpixels.widgets.TreeUtils;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of an OS X Source List. For a full descrption of what a Source List is, see the
 * <a href="http://developer.apple.com/documentation/UserExperience/Conceptual/AppleHIGuidelines/XHIGWindows/chapter_18_section_4.html#//apple_ref/doc/uid/20000961-CHDDIGDE">Source Lists</a>
 * section of Apple's Human Interface Guidelines.
 * <p/>
 * This component provides the two basic sytles of Source List: focusble and non-focusable.
 * As the name implies, focusable Source Lists and recieve keyboard focus, and thus can be navigated
 * using the arrow keys. Non-focusable, cannot receive keyboard focus, and thus cannot be
 * navigated via the arrow keys. The two styles of {@code SourceList} are pictured below:
 * <br>
 * <table>
 * <tr><td align="center"><img src="../../../../graphics/iTunesSourceList.png"></td>
 * <td align="center"><img src="../../../../graphics/MailSourceList.png"></td></tr>
 * <tr><td align="center"><font size="2" face="arial"><b>Focusable SourceList<b></font></td>
 * <td align="center"><font size="2" face="arial"><b>Non-focusable SourceList<b></font></td></tr>
 * </table>
 * <br>
 * Here's how to create a simple {@code SourceList} with one item:
 * <pre>
 * SourceListModel model = new SourceListModel();
 * SourceListCategory category = new SourceListCategory("Category");
 * model.addCategory(category);
 * model.addItemToCategory(new SourceListItem("Item"), category);
 * SourceList sourceList = new SourceList(model);
 * </pre>
 * <p>
 * To install a selection listener on the {@code SourceList}, add a
 * {@link SourceListSelectionListener}.
 * </p>
 * <p/>
 * To install a context-menu provider, call
 * {@link #setSourceListContextMenuProvider(SourceListContextMenuProvider)} with an implementation
 * of {@link SourceListContextMenuProvider}.
 */
public class SourceList {

    private final SourceListModel fModel;

    private final SourceListModelListener fModelListener = createSourceListModelListener();

    private final List<SourceListSelectionListener> fSourceListSelectionListeners =
            new ArrayList<SourceListSelectionListener>();

    private DefaultMutableTreeNode fRoot = new DefaultMutableTreeNode("root");

    private DefaultTreeModel fTreeModel = new DefaultTreeModel(fRoot);

    private JTree fTree = MacWidgetFactory.createSourceList(fTreeModel);

    private JScrollPane fScrollPane = MacWidgetFactory.createSourceListScrollPane(fTree);

    private final JPanel fComponent = new JPanel(new BorderLayout());

    private TreeSelectionListener fTreeSelectionListener = createTreeSelectionListener();

    private MouseListener fMouseListener = createMouseListener();

    private SourceListControlBar fSourceListControlBar;

    private SourceListContextMenuProvider fContextMenuProvider =
            new EmptySourceListContextMenuProvider();

    private List<SourceListClickListener> fSourceListClickListeners =
            new ArrayList<SourceListClickListener>();

    /**
     * Creates a {@code SourceList} with an empty {@link SourceListModel}.
     */
    public SourceList() {
        this(new SourceListModel());
    }

    /**
     * Creates a {@code SourceList} with the given {@link SourceListModel}.
     *
     * @param model the {@code SourceListModel} to use.
     */
    public SourceList(SourceListModel model) {
        if (model == null) {
            throw new IllegalArgumentException("Groups cannot be null.");
        }

        fModel = model;
        fModel.addSourceListModelListener(fModelListener);

        initUi();

        // add each category and its sub-items to backing JTree.
        for (int i = 0; i < model.getCategories().size(); i++) {
            doAddCategory(model.getCategories().get(i), i);
        }
    }

    private void initUi() {
        fComponent.add(fScrollPane, BorderLayout.CENTER);
        fTree.addTreeSelectionListener(fTreeSelectionListener);
        fTree.addMouseListener(fMouseListener);
    }

    /**
     * Installs the given {@link SourceListControlBar} at the base of this {@code SourceList}. This
     * method can be called only once, and should generally be called during creation of the
     * {@code SourceList}.
     *
     * @param sourceListControlBar the {@link SourceListControlBar} to add.
     * @throws IllegalStateException    if a {@code SourceListControlBar} has already been installed
     *                                  on this {@code SourceList}.
     * @throws IllegalArgumentException if the {@code SourceListControlBar} is null.
     */
    public void installSourceListControlBar(SourceListControlBar sourceListControlBar) {
        if (fSourceListControlBar != null) {
            throw new IllegalStateException("A SourceListControlBar has already been installed on" +
                    " this SourceList.");
        }
        if (sourceListControlBar == null) {
            throw new IllegalArgumentException("SourceListControlBar cannot be null.");
        }
        fSourceListControlBar = sourceListControlBar;
        fComponent.add(fSourceListControlBar.getComponent(), BorderLayout.SOUTH);
    }

    /**
     * Sets the {@link SourceListContextMenuProvider} to use for this {@code SourceList}.
     *
     * @param contextMenuProvider the {@link SourceListContextMenuProvider} to use for this
     *                            {@code SourceList}.
     * @throws IllegalArgumentException if the {@code SourceListContextMenuProvider} is null.
     */
    public void setSourceListContextMenuProvider(SourceListContextMenuProvider contextMenuProvider) {
        if (contextMenuProvider == null) {
            throw new IllegalArgumentException("SourceListContextMenuProvider cannot be null.");
        }
        fContextMenuProvider = contextMenuProvider;
    }

    /**
     * Uninstalls any listeners that this {@code SourceList} installed on creation, thereby allowing
     * it to be garbage collected.
     */
    public void dispose() {
        fModel.removeSourceListModelListener(fModelListener);
    }

    /**
     * Gets the selected {@link SourceListItem}.
     *
     * @return the selected {@code SourceListItem}.
     */
    public SourceListItem getSelectedItem() {
        SourceListItem selectedItem = null;
        if (fTree.getSelectionPath() != null
                && fTree.getSelectionPath().getLastPathComponent() != null) {
            DefaultMutableTreeNode selectedNode =
                    (DefaultMutableTreeNode) fTree.getSelectionPath().getLastPathComponent();
            assert selectedNode.getUserObject() instanceof SourceListItem
                    : "Only SourceListItems can be selected.";
            selectedItem = (SourceListItem) selectedNode.getUserObject();
        }
        return selectedItem;
    }

    /**
     * Selects the given {@link SourceListItem} in the list.
     *
     * @param item the item to select.
     * @throws IllegalArgumentException if the given item is not in the list.
     */
    public void setSelectedItem(SourceListItem item) {
        getModel().checkItemIsInModel(item);
        DefaultMutableTreeNode treeNode = getNodeForObject(fRoot, item);
        fTree.setSelectionPath(new TreePath(treeNode.getPath()));
    }

    /**
     * Sets whether this {@code SourceList} can have focus. When focusable and this
     * {@code SourceList} has focus, the keyboard can be used for navigation.
     *
     * @param focusable true if this {@code SourceList} should be focusable.
     */
    public void setFocusable(boolean focusable) {
        fTree.setFocusable(focusable);
    }

    /**
     * Installs iApp style scroll bars on this {@code SourceList}.
     *
     * @see IAppWidgetFactory#makeIAppScrollPane
     */
    public void useIAppStyleScrollBars() {
        IAppWidgetFactory.makeIAppScrollPane(fScrollPane);
    }

    private static DefaultMutableTreeNode getNodeForObject(DefaultMutableTreeNode parentNode,
                                                           Object userObject) {
        if (parentNode.getUserObject().equals(userObject)) {
            return parentNode;
        } else if (parentNode.children().hasMoreElements()) {
            for (int i = 0; i < parentNode.getChildCount(); i++) {
                DefaultMutableTreeNode childNode =
                        (DefaultMutableTreeNode) parentNode.getChildAt(i);
                DefaultMutableTreeNode retVal =
                        getNodeForObject(childNode, userObject);
                if (retVal != null) {
                    return retVal;
                }
            }
        } else {
            return null;
        }

        return null;
    }

    /**
     * Gets the user interface component representing this {@code SourceList}. The returned
     * {@link JComponent} should be added to a container that will be displayed.
     *
     * @return the user interface component representing this {@code SourceList}.
     */
    public JComponent getComponent() {
        return fComponent;
    }

    /**
     * Gets the {@link SourceListModel} backing this {@code SourceList}.
     *
     * @return the {@code SourceListModel} backing this {@code SourceList}.
     */
    public SourceListModel getModel() {
        return fModel;
    }

    private void doAddCategory(SourceListCategory category, int index) {
        DefaultMutableTreeNode categoryNode = new DefaultMutableTreeNode(category);
        fTreeModel.insertNodeInto(categoryNode, fRoot, index);
        // add each of the categories child items to the tree.
        for (int i = 0; i < category.getItems().size(); i++) {
            doAddItemToCategory(category.getItems().get(i), category, i);
        }

        TreeUtils.expandPathOnEdt(fTree, new TreePath(categoryNode.getPath()));
    }

    private void doRemoveCategory(SourceListCategory category) {
        DefaultMutableTreeNode categoryNode = getNodeForObject(fRoot, category);
        checkNodeNotNull(categoryNode);
        fTreeModel.removeNodeFromParent(categoryNode);
    }

    private void doAddItemToCategory(SourceListItem itemToAdd, SourceListCategory category, int index) {
        DefaultMutableTreeNode categoryNode = getNodeForObject(fRoot, category);
        checkNodeNotNull(categoryNode);
        doAddItemToNode(itemToAdd, categoryNode, index);
    }

    private void doRemoveItemFromCategory(SourceListItem itemToRemove, SourceListCategory category) {
        DefaultMutableTreeNode categoryNode = getNodeForObject(fRoot, category);
        checkNodeNotNull(categoryNode);
        DefaultMutableTreeNode itemNode = getNodeForObject(categoryNode, itemToRemove);
        checkNodeNotNull(itemNode);
        fTreeModel.removeNodeFromParent(itemNode);
    }

    private void doAddItemToItem(SourceListItem itemToAdd, SourceListItem parentItem, int index) {
        DefaultMutableTreeNode parentItemNode = getNodeForObject(fRoot, parentItem);
        checkNodeNotNull(parentItemNode);
        doAddItemToNode(itemToAdd, parentItemNode, index);
    }

    private void doRemoveItemFromItem(SourceListItem itemToRemove, SourceListItem parentItem) {
        DefaultMutableTreeNode parentNode = getNodeForObject(fRoot, parentItem);
        checkNodeNotNull(parentNode);
        DefaultMutableTreeNode itemNode = getNodeForObject(parentNode, itemToRemove);
        checkNodeNotNull(itemNode);
        fTreeModel.removeNodeFromParent(itemNode);
    }

    private void doAddItemToNode(SourceListItem itemToAdd, DefaultMutableTreeNode parentNode, int index) {
        DefaultMutableTreeNode itemNode = new DefaultMutableTreeNode(itemToAdd);
        fTreeModel.insertNodeInto(itemNode, parentNode, index);
        // add each of the newly added item's children nodes.
        for (int i = 0; i < itemToAdd.getChildItems().size(); i++) {
            doAddItemToItem(itemToAdd.getChildItems().get(i), itemToAdd, i);
        }
    }

    private void doShowContextMenu(MouseEvent event) {
        // grab the item or category under the mouse events point if there is
        // there is an item or category under this point.
        Object itemOrCategory = getItemOrCategoryUnderPoint(event.getPoint());

        // if there was no item under the click, then call the generic contribution method.
        // else if there was a SourceListItem under the click, call the corresponding contribution
        //         method.
        // else if there was a SourceListCategory under the click, call the corresponding contribution
        //         method.
        JPopupMenu popup = null;
        if (itemOrCategory == null) {
            popup = fContextMenuProvider.createContextMenu();
        } else if (itemOrCategory instanceof SourceListItem) {
            popup = fContextMenuProvider.createContextMenu((SourceListItem) itemOrCategory);
        } else if (itemOrCategory instanceof SourceListCategory) {
            popup = fContextMenuProvider.createContextMenu((SourceListCategory) itemOrCategory);
        }

        // only show the context-menu if menu items have been added to it.
        if (popup != null && popup.getComponentCount() > 0) {
            popup.show(fTree, event.getX(), event.getY());
        }
    }

    private void doSourceListClicked(MouseEvent event) {
        // grab the item or category under the mouse events point if there is
        // there is an item or category under this point.
        Object itemOrCategory = getItemOrCategoryUnderPoint(event.getPoint());

        SourceListClickListener.Button button =
                SourceListClickListener.Button.getButton(event.getButton());
        int clickCount = event.getClickCount();

        if (itemOrCategory == null) {
            // do nothing.
        } else if (itemOrCategory instanceof SourceListItem) {
            fireSourceListItemClicked((SourceListItem) itemOrCategory, button, clickCount);
        } else if (itemOrCategory instanceof SourceListCategory) {
            fireSourceListCategoryClicked((SourceListCategory) itemOrCategory, button, clickCount);
        }
    }

    private Object getItemOrCategoryUnderPoint(Point point) {
        // grab the path under the given point.
        TreePath path = fTree.getPathForLocation(point.x, point.y);
        // if there is a tree item under that point, cast it to a DefaultMutableTreeNode and grab
        // the user object which will either be a SourceListItem or SourceListCategory.
        return path == null
                ? null : ((DefaultMutableTreeNode) path.getLastPathComponent()).getUserObject();
    }

    private TreeSelectionListener createTreeSelectionListener() {
        return new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                fireSourceListItemSelected(getSelectedItem());
            }
        };
    }

    private SourceListModelListener createSourceListModelListener() {
        return new SourceListModelListener() {
            public void categoryAdded(SourceListCategory category, int index) {
                doAddCategory(category, index);
            }

            public void categoryRemoved(SourceListCategory category) {
                doRemoveCategory(category);
            }

            public void itemAddedToCategory(SourceListItem item, SourceListCategory category, int index) {
                doAddItemToCategory(item, category, index);
            }

            public void itemRemovedFromCategory(SourceListItem item, SourceListCategory category) {
                doRemoveItemFromCategory(item, category);
            }

            public void itemAddedToItem(SourceListItem item, SourceListItem parentItem, int index) {
                doAddItemToItem(item, parentItem, index);
            }

            public void itemRemovedFromItem(SourceListItem item, SourceListItem parentItem) {
                doRemoveItemFromItem(item, parentItem);
            }
        };
    }

    private MouseListener createMouseListener() {
        return new MouseAdapter() {
            // TODO there is an interesting point of contention here: should
            // TODO the context menu always be shown as if it were on a Mac (on
            // TODO mouse press) or based on the platform on which it is running.
            // TODO always doing the same thing would actually be harder,
            // TODO because we wouldn't be able to rely on the isPopupTrigger
            // TODO method and there is no way to determine when the 
            // TODO popup-menu-trigger button is.
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    doShowContextMenu(e);
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    doShowContextMenu(e);
                }
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                doSourceListClicked(e);
            }
        };
    }

    // SourceListClickListener support. ///////////////////////////////////////    

    private void fireSourceListItemClicked(
            SourceListItem item, SourceListClickListener.Button button,
            int clickCount) {
        for (SourceListClickListener listener : fSourceListClickListeners) {
            listener.sourceListItemClicked(item, button, clickCount);
        }
    }

    private void fireSourceListCategoryClicked(
            SourceListCategory category, SourceListClickListener.Button button,
            int clickCount) {
        for (SourceListClickListener listener : fSourceListClickListeners) {
            listener.sourceListCategoryClicked(category, button, clickCount);
        }
    }

    /**
     * Adds the {@link SourceListClickListener} to the list of listeners.
     *
     * @param listener the {@code SourceListClickListener} to add.
     */
    public void addSourceListClickListener(SourceListClickListener listener) {
        fSourceListClickListeners.add(listener);
    }

    /**
     * Removes the {@link SourceListClickListener} to the list of listeners.
     *
     * @param listener the {@code SourceListClickListener} to remove.
     */
    public void removeSourceListClickListener(SourceListClickListener listener) {
        fSourceListClickListeners.remove(listener);
    }

    // SourceListSelectionListener support. ///////////////////////////////////

    private void fireSourceListItemSelected(SourceListItem item) {
        for (SourceListSelectionListener listener : fSourceListSelectionListeners) {
            listener.sourceListItemSelected(item);
        }
    }

    /**
     * Adds the {@link SourceListSelectionListener} to the list of listeners.
     *
     * @param listener the {@code SourceListSelectionListener} to add.
     */
    public void addSourceListSelectionListener(SourceListSelectionListener listener) {
        fSourceListSelectionListeners.add(listener);
    }

    /**
     * Removes the {@link SourceListSelectionListener} from the list of listeners.
     *
     * @param listener the {@code SourceListSelectionListener} to remove.
     */
    public void removeSourceListSelectionListener(SourceListSelectionListener listener) {
        fSourceListSelectionListeners.remove(listener);
    }

    // Utility methods. ///////////////////////////////////////////////////////////////////////////

    private static void checkNodeNotNull(MutableTreeNode node) {
        if (node == null) {
            throw new IllegalArgumentException("The given SourceListCategory " +
                    "does not exist in this SourceList.");
        }
    }

    // EmptySourceListContextMenuProvider implementation. ///////////////////////////////////////

    private static class EmptySourceListContextMenuProvider implements SourceListContextMenuProvider {
        public JPopupMenu createContextMenu() {
            return null;
        }

        public JPopupMenu createContextMenu(SourceListItem item) {
            return null;
        }

        public JPopupMenu createContextMenu(SourceListCategory category) {
            return null;
        }
    }
}