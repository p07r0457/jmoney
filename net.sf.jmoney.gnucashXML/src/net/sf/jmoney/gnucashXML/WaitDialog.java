/*
*
*  JMoney - A Personal Finance Manager
*  Copyright (c) 2002 Johann Gyger <johann.gyger@switzerland.org>
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

package net.sf.jmoney.gnucashXML;

import java.awt.BorderLayout;
import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import net.sf.jmoney.Constants;
import net.sf.jmoney.JMoneyPlugin;

public class WaitDialog extends JDialog implements Runnable, Constants {

	private JLabel messageLabel = new JLabel();

	private boolean stop = false;

	public WaitDialog(Frame owner) {
		super(owner, JMoneyPlugin.getResourceString("Dialog.Wait.Title"));
		try {
			jbInit();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run() {
		messageLabel.paintImmediately(messageLabel.getVisibleRect());
		while (!stop) {
			try {
				Thread.sleep(300);
			} catch (InterruptedException ie) {
			}
		}
		dispose();
	}

	public void show(String message) {
		messageLabel.setText(message);
		pack();
		setLocationRelativeTo(getParent());
		show();

		stop = false;
		Thread thread =
			new Thread(
				Thread.currentThread().getThreadGroup(),
				this,
				"WaitDialog");
		thread.start();
	}

	public void stop() {
		stop = true;
	}

	private void jbInit() throws Exception {
		messageLabel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));
		messageLabel.setHorizontalAlignment(SwingConstants.CENTER);

		getContentPane().add(messageLabel, BorderLayout.CENTER);
	}
}