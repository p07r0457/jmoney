package net.sf.jmoney.views;

import java.util.ArrayList;
import java.util.Vector;

import net.sf.jmoney.IBookkeepingPageListener;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.graphics.Image;

class TreeNode implements IAdaptable {
    private String name;

    private Image image;

    private TreeNode parent;

    private String parentId;

    protected ArrayList children = null;

    private Vector pageListeners = new Vector();

    public TreeNode(String name, Image image, TreeNode parent) {
        this.name = name;
        this.image = image;
        this.parent = parent;
    }

    public TreeNode(String name, Image image, String parentId) {
        this.name = name;
        this.image = image;
        this.parentId = parentId;
    }

    public String getName() {
        return name;
    }

    public TreeNode getParent() {
        return parent;
    }

    public String toString() {
        return getName();
    }

    public Object getAdapter(Class key) {
        return null;
    }

    public Image getImage() {
        return image;
    }

    public void addChild(Object child) {
        if (children == null) {
            children = new ArrayList();
        }
        children.add(child);
    }

    public void removeChild(Object child) {
        children.remove(child);
    }

    public Object[] getChildren() {
        if (children == null) {
            return new Object[0];
        } else {
            return children.toArray(new Object[children.size()]);
        }
    }

    public boolean hasChildren() {
        return children != null && children.size() > 0;
    }

    /**
     * @return
     */
    public Object getParentId() {
        return parentId;
    }

    /**
     * @param parentNode
     */
    public void setParent(TreeNode parent) {
        this.parent = parent;
    }

    /**
     * @param pageListener
     */
    public void addPageListener(IBookkeepingPageListener pageListener) {
        pageListeners.add(pageListener);
    }

    /**
     * @return An array of objects that implement the IBookkeepingPageListener
     *         interface. The returned value is never null but the Vector may be
     *         empty if there are no listeners for this node.
     */
    public Vector getPageListeners() {
        return pageListeners;
    }
}

