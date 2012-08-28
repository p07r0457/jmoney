package net.sf.jmoney.email.handlers;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.security.Security;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.internet.MimeMultipart;

import net.sf.jmoney.email.IMailReader;
import net.sf.jmoney.isolation.TransactionManager;
import net.sf.jmoney.model2.DatastoreManager;
import net.sf.jmoney.resources.Messages;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.sun.mail.pop3.POP3SSLStore;
import com.sun.mail.pop3.POP3Store;
import com.sun.mail.util.BASE64DecoderStream;
/**
 * Reads mail, looking for e-mail messages that have information that
 * can be imported into JMoney.
 */
public class EmailImportHandler extends AbstractHandler {

	private Session mailSession;
	private Store store;
	private Folder folder;
	private Message[] messages;
	private FetchProfile fetchProfile;
	private Properties properties;
	private String username;
	private String password;

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		Shell shell = HandlerUtil.getActiveShellChecked(event);
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);

		DatastoreManager sessionManager = (DatastoreManager)window.getActivePage().getInput();
		if (sessionManager == null) {
			MessageDialog.openWarning(
					shell,
					Messages.CloseSessionAction_WarningTitle,
					Messages.CloseSessionAction_WarningMessage);
		} else {
		// Load the extensions
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint extensionPoint = registry.getExtensionPoint("net.sf.jmoney.mail.mailimporter");
		IExtension[] extensions = extensionPoint.getExtensions();

		try {


			List<IMailReader> importers = new ArrayList<IMailReader>();

			for (int i = 0; i < extensions.length; i++) {
				IConfigurationElement[] elements = extensions[i].getConfigurationElements();
				for (int j = 0; j < elements.length; j++) {
					if (elements[j].getName().equals("mail")) {
						String name = elements[j].getAttribute("name");
						String description = "";
						IConfigurationElement[] descriptionElement = elements[j].getChildren("description");
						if (descriptionElement.length == 1) {
							description = descriptionElement[0].getValue();
						}
						IMailReader importer = (IMailReader)elements[j].createExecutableExtension("class");
						importers.add(importer);
					}
				}
			}

			String host = "pop.gmail.com";
			username = "<gmail username>";
			password = "<gmail password>";
			int port = 995;
			
			properties = new Properties();
//					properties.setProperty("mail.pop3.host", host);
			properties.setProperty("mail.pop3.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			properties.setProperty("mail.pop3.socketFactory.fallback", "false");
			properties.setProperty("mail.pop3.port", Integer.toString(port));
			properties.setProperty("mail.pop3.socketFactory.port", Integer.toString(port));
			properties.setProperty("mail.pop3.ssl", "true");
			Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());

			mailSession = Session.getInstance(properties, null);

			URLName urln = new URLName("pop3", host, port, null, username, password);
			store = new POP3SSLStore(mailSession, urln);

			Message[] messages  = readAllMessages(window, host);
			transform(sessionManager, messages, importers);

		} catch (CoreException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		}
		
		return null;
	}

	public Message[] readAllMessages(IWorkbenchWindow window, String host) {

		try {
			store.connect(host, username, password);
		} catch (MessagingException ex) {
			MessageDialog.openInformation(
					window.getShell(),
					"Mail client Error",
					" couldn't connect to mail server ,please make sure you entered to right(username,password). Or make sure of your mail client connect configuration under props/config.properties file.");
		}
		try {

			folder = store.getDefaultFolder();
			folder = folder.getFolder("INBOX");
			folder.open(Folder.READ_ONLY);
			System.out.println("Message Count Found " + folder.getMessageCount());
			System.out.println("New Message Count " + folder.getNewMessageCount());
			System.out.println("=========================================");
			Message[] newmessages = folder.getMessages();
			FetchProfile fp = new FetchProfile();
			fp.add(FetchProfile.Item.ENVELOPE);
			folder.fetch(newmessages, fp);
			messages = newmessages;
			return messages;
		} catch (MessagingException ex) {
			MessageDialog.openInformation(
					window.getShell(),
					"Mail client Error",
					"Reading the messages INBOX failed -" + ex);
		}



		return messages;

	}

	public void transform(DatastoreManager datastoreManager, Message[] messages, List<IMailReader> importers) {

		for (int i = 0; i < messages.length; i++) {
			try {
				Address[] addressList = messages[i].getFrom();
				for (int j = 0; j < addressList.length; j++) {
					System.out.println("From address #" + j + " : " + addressList[j].toString());
				}

				System.out.println("Receive date :" + messages[i].getSentDate());

				String contentString = null;
				System.out.println(messages[i].getContentType());
				if (messages[i].isMimeType("text/plain")) {
					contentString = messages[i].getContent().toString();
				} else if (messages[i].isMimeType("text/html")) {
					contentString = messages[i].getContent().toString();
				} else {
					contentString = handleMultipart(messages[i]);
				}

				/*
				 * Create a transaction to be used to import the mail message.  This allows the data to
				 * be more efficiently written to the back-end datastore and it also groups
				 * the entire import as a single change for undo/redo purposes.
				 */
				TransactionManager transactionManager = new TransactionManager(datastoreManager);
				net.sf.jmoney.model2.Session sessionInTransaction = transactionManager.getSession();
				
				boolean anyProcessed = false;
				if (contentString != null) {
					for (IMailReader reader : importers) {
						boolean processed = reader.processEmail(sessionInTransaction, messages[i].getSentDate(), contentString);
						anyProcessed |= processed;
					}
				}

				/*
				 * We don't know whether any extensions have actually done anything, so
				 * we commit the transaction anyway.  This will be a null operation if
				 * no changes were made.  Note the the 'anyProcessed' flag cannot be tested
				 * because this flag is used to indicate the the mail is 'done' and should
				 * be deleted.  An extension may make some changes to the datastore but not
				 * think the message should be deleted. 
				 */
				String transactionDescription = MessageFormat.format("Import Mail {0}", messages[i].getSubject());
				transactionManager.commit(transactionDescription);									

				if (anyProcessed) {
					// TODO delete the e-mail
				}
			} catch (MessagingException ex) {
				System.out.println("Messages transformation failed :" + ex);
			} catch (IOException ex) {
				System.out.println("Messages I/O transformation failed : " + ex);
			}
		}
	}

	public String handleMultipart(Message msg) {

		String content = null;
		try {
			String disposition;
			BodyPart part;
			Multipart mp = (Multipart) msg.getContent();

			int mpCount = mp.getCount();
			for (int m = 0; m < mpCount; m++) {
				part = mp.getBodyPart(m);

				disposition = part.getDisposition();
				Object x = part.getContent();
				if (!(x instanceof String)) {
					System.out.println("here");
				}
				if (x instanceof BASE64DecoderStream) {
					BASE64DecoderStream stream = (BASE64DecoderStream)x;
					
				} else if (x instanceof MimeMultipart) {
					MimeMultipart stream = (MimeMultipart)x;
					System.out.println(stream.getContentType() + stream.getCount());
					for (int i = 0; i < stream.getCount(); i++) {
						BodyPart p = stream.getBodyPart(i);
						Object z = p.getContent();
						System.out.println(z);
					}
				} else {
				if (disposition != null && disposition.equals(Part.INLINE)) {
					content = part.getContent().toString();
				} else {
					content = part.getContent().toString();
				}
				}
			}
		} catch (IOException ex) {
			System.out.println("Messages - Parts - Input/output transformation failed :" + ex);
		} catch (MessagingException ex) {
			System.out.println("Messages - Parts - transformation failed :" + ex);
		}
		return content;
	}
}
