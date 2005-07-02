/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2004 Nigel Westbury <westbury@users.sourceforge.net>
*
*
*  This program is free software; you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation; either version 2 of the License, or
*  (at your option) any later version.
*
*  This program is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU General Public License for more details.
*
*  You should have received a copy of the GNU General Public License
*  along with this program; if not, write to the Free Software
*  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
*
*/

package net.sf.jmoney.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import net.sf.jmoney.JMoneyPlugin;
import net.sf.jmoney.model2.PageEntry;
import net.sf.jmoney.model2.PropertySet;
import net.sf.jmoney.model2.PropertySetNotFoundException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

/*
 * The content provider class is responsible for
 * providing objects to the view. It can wrap
 * existing objects in adapters or simply return
 * objects as-is. These objects may be sensitive
 * to the current input of the view, or ignore
 * it and always show the same content 
 * (like Task List, for example).
 */
public class TreeNode implements IAdaptable {
	/** String (the full id of the node) to TreeNode */
	private static Map idToNodeMap = new HashMap();
	private static TreeNode invisibleRoot;

	// TODO: generalize this code
	private static AccountsNode accountsRootNode;

	private String id;
	private String label;
	private Image image = null;
	private ImageDescriptor imageDescriptor;
	private TreeNode parent;
	private String parentId;
	private int position;
	protected ArrayList children = null;
	
	/** Element: PageEntry */
	private Vector pageFactories = new Vector();

	/**
	 * Initialize the navigation tree nodes.
	 * <P>
	 * The initialization of the navigation nodes depends on
	 * the property sets.  Therefore PropertySet.init must be
	 * called before this method. 
	 */
	public static void init() {
		// Load the extensions
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint("net.sf.jmoney.pages");
		IExtension[] extensions = extensionPoint.getExtensions();
		for (int i = extensions.length-1; i>=0; i--) {
			IConfigurationElement[] elements =
				extensions[i].getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				if (elements[j].getName().equals("node")) {
					
					String label = elements[j].getAttribute("label");
					String icon = elements[j].getAttribute("icon");
					String id = elements[j].getAttribute("id");
					String parentNodeId = elements[j].getAttribute("parent");
					String position = elements[j].getAttribute("position");
					
					if (id != null && id.length() != 0) {
						String fullNodeId = extensions[i].getNamespace() + '.' + id;
						ImageDescriptor descriptor = null;
						if (icon != null) {
							// Try getting the image from this plug-in.
							descriptor = JMoneyPlugin.imageDescriptorFromPlugin(extensions[i].getNamespace(), icon); 
							if (descriptor == null) {
								// try getting the image from the JMoney plug-in. 
								descriptor = JMoneyPlugin.imageDescriptorFromPlugin("net.sf.jmoney", icon);
							}
						}
						
						int positionNumber = 800;
						if (position != null) {
							positionNumber = Integer.parseInt(position);
						}
						
						TreeNode node = new TreeNode(fullNodeId, label, descriptor, parentNodeId, positionNumber);
						idToNodeMap.put(fullNodeId, node);
					}
				}
			}
		}
		
		// Set each node's parent.  If no node exists
		// with the given parent node id then the node
		// is placed at the root.

		invisibleRoot = new TreeNode("root", "", null, "", 0);

		for (Iterator iter = idToNodeMap.values().iterator(); iter.hasNext(); ) {
			TreeNode treeNode = (TreeNode)iter.next();
			TreeNode parentNode;
			if (treeNode.getParentId() != null) {
				parentNode = (TreeNode)idToNodeMap.get(treeNode.getParentId());
				if (parentNode == null) {
					parentNode = invisibleRoot;
				}
			} else {
				parentNode = invisibleRoot;
			}
			treeNode.setParent(parentNode);
			parentNode.addChild(treeNode);
		}	
		
		// Set the list of pages for each node.
		for (int i = extensions.length-1; i>=0; i--) {
			IConfigurationElement[] elements =
				extensions[i].getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				if (elements[j].getName().equals("pages")) {
					// TODO: remove plug-in as bad if the id is not unique.
					String id = elements[j].getAttribute("id");
					String pageId = extensions[i].getNamespace() + '.' + id;
					String nodeId = elements[j].getAttribute("node");
					if (nodeId != null && nodeId.length() != 0) {
						TreeNode node = (TreeNode)idToNodeMap.get(nodeId);
						if (node != null) {
							node.addPage(pageId, elements[j]);
						} else {
							// No node found with given id, so the
							// page listener is dropped.
							// TODO Log missing node.
						}
					} else {
						// No 'node' attribute so see if we have
						// an 'extendable-property-set' attribute.
						// (This means the page should be supplied if
						// the node represents an object that contains
						// the given property set).
						String propertySetId = elements[j].getAttribute("extendable-property-set");
						if (propertySetId != null) {
							try {
								PropertySet pagePropertySet = PropertySet.getPropertySet(propertySetId);
								PageEntry pageEntry = new PageEntry(pageId, elements[j]);  
								
								for (Iterator iter = pagePropertySet.getDerivedPropertySetIterator(); iter.hasNext(); ) {
									PropertySet derivedPropertySet = (PropertySet)iter.next();
									derivedPropertySet.addPage(pageEntry);
								}
							} catch (PropertySetNotFoundException e1) {
								// This is a plug-in error.
								// TODO implement properly.
								e1.printStackTrace();
							}
						}
					}
				}
			}
		}

		// Special case node
		accountsRootNode = new AccountsNode(JMoneyPlugin.getResourceString("NavigationTreeModel.accounts"), JMoneyPlugin.createImageDescriptor("icons/accounts.gif"), invisibleRoot);
		invisibleRoot.addChild(accountsRootNode);

		// If a node has no child nodes and no page listeners
		// then the node is removed.  This allows nodes to be
		// created by the framework or the more general plug-ins
		// that have no functionality provided by the plug-in that
		// created the node but that can be extended by other
		// plug-ins.  By doing this, rather than expecting plug-ins
		// to create their own nodes, it is more likely that
		// different plug-in developers will share nodes, and
		// thus avoiding hundreds of root nodes in the navigation
		// tree, each with a single tab view. 

		// TODO: implement this
	}
	
	public static TreeNode getInvisibleRoot() {
		return invisibleRoot;
	}

	// At some point this node will be generalized and this method
	// can be removed.  For time being, this node is a special case node.
	public static AccountsNode getAccountsRootNode() {
		return accountsRootNode;
	}
	
	/**
	 * @param nodeId the full id of a node
	 * @return the node, or null if no node with the given id exists
	 */
	public static TreeNode getTreeNode(String nodeId) {
		return (TreeNode)idToNodeMap.get(nodeId);
	}
	
	public TreeNode(String id, String label, ImageDescriptor imageDescriptor, String parentId, int position) {
		this.id = id;
		this.label = label;
		this.imageDescriptor = imageDescriptor;
		this.parentId = parentId;
		this.position = position;
	}

	/**
	 * @return
	 */
	public String getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}
	public TreeNode getParent() {
		return parent;
	}
	int getPosition() {
		return position;
	}
	public String toString() {
		return getLabel();
	}
	public Object getAdapter(Class key) {
		return null;
	}
	public Image getImage() {
		if (image == null && imageDescriptor != null) {
			image = imageDescriptor.createImage();
		}
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
	public Object [] getChildren() {
		if (children == null) {
			return new Object[0];
		} else {
			return children.toArray();
		}
	}
	public boolean hasChildren() {
		return children != null && children.size()>0;
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
	public void addPage(String pageId, IConfigurationElement pageElement) {
		PageEntry pageEntry = new PageEntry(pageId, pageElement);  
		pageFactories.add(pageEntry);
	}

	/**
	 * @return An array of objects that implement the IBookkeepingPage
	 * 		interface.  The returned value is never null but the Vector may
	 * 		be empty if there are no pages for this node.
	 */
	public Vector getPageFactories() {
		return pageFactories;
	}
}


