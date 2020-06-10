package bmbremaster.client;

import java.io.IOException;
import java.net.UnknownHostException;

import bmbremaster.server.Msg;

public class Run {

	public static void main(String[] args) throws UnknownHostException, IOException, ClassNotFoundException {
		Client client = new Client();
		client.connect("localhost", 6666);
		client.sendMessage(new Msg("Dominik"));
		System.out.println(client.getMessage().getTextMessage());
		client.disconnect();
	}

}
