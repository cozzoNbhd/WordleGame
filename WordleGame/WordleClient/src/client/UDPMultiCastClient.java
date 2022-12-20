package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;

// Thread appartenete al Client il cui scopo è mettersi in ascolto di eventuali messaggi UDP proveniente dall'indirizzo di rete Multicast,
// A cui si è precedentemente unito.
// Il thread non fa altro che rimanere in ascolto sulla porta 4321, fino a che il messaggio "STOP" non verrà ricevuto.
public class UDPMultiCastClient implements Runnable {
    public void receiveUDPMessage(String ip, int port) throws IOException {
        byte[] buffer = new byte[1024];
        WordleClientMain.notifiche = new ArrayList<>();
        MulticastSocket socket = new MulticastSocket(port);
        InetAddress group = InetAddress.getByName(ip);
        socket.joinGroup(group);
        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            String msg = new String(packet.getData(), packet.getOffset(), packet.getLength());
            if ("STOP".equals(msg)) break;
            WordleClientMain.notifiche.add(msg);
            //System.out.println(msg);
        }
        socket.leaveGroup(group);
        socket.close();
    }
    public void run() {
        try {
            receiveUDPMessage("230.0.0.0", 4321);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}