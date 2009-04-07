package rmi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.lang.reflect.Method;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/*
 * No need for extra security, as the stub (and its serialization) contain
 * a hard-to-guess hash code.
 */

public class OtherInstance {
	interface ImageJInstance extends Remote {
		void sendArgument(String arg) throws RemoteException;
	}

	static class Implementation implements ImageJInstance {
		int counter = 0;

		public void sendArgument(String arg) {
			System.err.println("got argument " + arg);
		}
	}

	public static String getStubPath() {
		return System.getProperty("java.io.tmpdir") + "/ImageJ-"
			+ System.getProperty("user.name") + ".stub";
	}

	static boolean verbose;

	public static void makeFilePrivate(String path) {
		try {
			// File.setReadable() is Java 6
			Class[] types = { boolean.class, boolean.class };
			Method m = File.class.getMethod("setReadable", types);
			Object[] arguments = { Boolean.FALSE, Boolean.FALSE };
			m.invoke(new File(path), arguments);
			arguments = new Object[] { Boolean.TRUE, Boolean.TRUE };
			m.invoke(new File(path), arguments);
			return;
		} catch (Exception e) {
			if (verbose)
				System.err.println("Java < 6 detected,"
					+ " trying chmod 0600 " + path);
		}
		try {
			String[] command = {
				"chmod", "0600", path
			};
			Runtime.getRuntime().exec(command);
		} catch (Exception e) {
			if (verbose)
				System.err.println("Even chmod failed.");
		}
	}

	public static boolean client(String[] args) {
		if (args.length == 0)
			args = new String[] { "dummy" };
		String file = getStubPath();
		try {
			FileInputStream in = new FileInputStream(file);
			ImageJInstance instance = (ImageJInstance)
				new ObjectInputStream(in).readObject();
			in.close();

			for (int i = 0; i < args.length; i++)
				instance.sendArgument(args[i]);
			return true;
		} catch (Exception e) {
			if (verbose) {
				System.err.println("Client exception: " + e);
				e.printStackTrace();
			}
		}
		return false;
	}

	public static void main(String[] args) {
		if (client(args))
			return;

		try {
			Implementation obj = new Implementation();
			ImageJInstance stub = (ImageJInstance)
				UnicastRemoteObject.exportObject(obj, 0);

			// Write serialized object
			String path = getStubPath();
			FileOutputStream out = new FileOutputStream(path);
			makeFilePrivate(path);
			new ObjectOutputStream(out).writeObject(stub);
			out.close();

			if (verbose)
				System.err.println("Server ready");
		} catch (Exception e) {
			if (verbose) {
				System.err.println("Server exception: " + e);
				e.printStackTrace();
			}
		}
	}
}
