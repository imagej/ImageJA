package rmi;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class Server implements Hello {

	public Server() {}

	int counter = 0;

	public String sayHello() {
		return "Hello, world (" + (++counter) + ")!";
	}

	public static void main(String args[]) {

		try {
			Server obj = new Server();
			Hello stub = (Hello)
				UnicastRemoteObject.exportObject(obj, 0);

			// Write serialized object
			FileOutputStream out =
				new FileOutputStream("hello.stub");
			new ObjectOutputStream(out).writeObject(stub);
			out.close();

			System.err.println("Server ready");
		} catch (Exception e) {
			System.err.println("Server exception: " + e.toString());
			e.printStackTrace();
		}
	}
}
