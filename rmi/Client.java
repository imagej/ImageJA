package rmi;

import java.io.FileInputStream;
import java.io.ObjectInputStream;

public class Client {

	private Client() {}

	public static void main(String[] args) {

		String file = args.length < 1 ? Server.getStubPath() : args[0];
		try {
			FileInputStream in = new FileInputStream(file);
			Hello hello =
				(Hello)new ObjectInputStream(in).readObject();
			in.close();

			String response = hello.sayHello();
			System.out.println("response: " + response);
		} catch (Exception e) {
			System.err.println("Client exception: " + e.toString());
			e.printStackTrace();
		}
	}
}

