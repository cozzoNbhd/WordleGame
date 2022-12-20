package client;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Properties;
import java.util.Scanner;

public class WordleClientMain {

    // Percorso del file di configurazione del client.
    public static final String configFile = "./src/client/client.properties";

    // Nome host e porta del server.
    public static String hostname;

    // Numero della porta utilizzata dal client per connettersi al Server
    public static int port;

    // Socket e relativi stream di input/output.
    public static Scanner sc = new Scanner(System.in);

    // Socket utilizzata per comunicare con la socket aperta dal Server riservata unicamente alle comunicazioni con il client corrente.
    public static Socket socket;

    // Array list contenente le notifiche che il Thread UDPMultiCast riceverà dal Server.
    public static ArrayList<String> notifiche;

    // Canale nel quale verranno ricevuti i messaggi dal Server
    private static BufferedReader in;

    // Canale nel quale verranno inviati i messaggi al Server
    private static PrintWriter out;


    public static void main(String[] args) throws UnknownHostException {

        boolean done;
        String msg;


        try {

            // Leggo il file di configurazione.
            readConfig();

            // Apro la socket
            socket = new Socket(hostname, port);

            // Inizializzo i canali di comunicazione con il Server
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Il client entra in un ciclo infinito nel quale:
            // 1) Riceve un messaggio dal Server;
            // 2) Lo stampa a video
            // 3) Il base al contenuto del messaggio, si decide se far digitare da tastiera all'utente un comando oppure no.

            while (true) {

                msg = in.readLine();

                System.out.println(msg);

                if (Objects.equals(msg, "Benvenuto!")) continue;
                if (Objects.equals(msg, "Digita '1' per effettuare il login")) continue;
                if (Objects.equals(msg, "Digita '2' per registrarti")) continue;
                if (Objects.equals(msg, "Registrazione avvenuta con successo!")) continue;
                if (Objects.equals(msg, "Username o password invalidi!")) continue;
                if (Objects.equals(msg, "Hai esaurito i tentativi, mi dispiace!")) break;
                if (Objects.equals(msg, "Il nome inserito NON e' valido!")) continue;
                if (Objects.equals(msg, "Il cognome inserito non e' valido!")) continue;
                if (Objects.equals(msg, "L'username inserito NON e' sintatticamente valido!")) continue;
                if (Objects.equals(msg, "L'username e' gia' stato utilizzato! Registrazione fallita!")) continue;
                if (Objects.equals(msg, "La password deve contenere almeno 1 carattere minuscolo, 1 maiuscolo, 1 cifra, 1 carattere speciale e una lunghezza tra 8 e 20 caratteri"))
                    continue;
                if (Objects.equals(msg, "Username o password non validi!")) continue;
                if (Objects.equals(msg, "Alla prossima!")) break;
                if (Objects.equals(msg, "Username e password corrette!")) { // se riceve il messaggio username e password corrette,
                    // capisce che deve invocare il metodo menu, perché il Server è passato alla fase di menu
                    menu(socket, in, out);
                    continue;
                }

                String msgRisposta;

                msgRisposta = sc.nextLine();

                out.println(msgRisposta);
            }
        } catch (IOException ex) {
            System.out.println("Impossibile raggiungere " + hostname + " alla porta " + port);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ex){}
            }
        }

    }

    private static void menu(Socket socket, BufferedReader in, PrintWriter out) throws IOException {

        String msg;

        Thread receiver = new Thread(new UDPMultiCastClient());

        receiver.start();

        // Il client entra in un ciclo infinito nel quale:
        // 1) Riceve un messaggio dal Server;
        // 2) Lo stampa a video
        // 3) Il base al contenuto del messaggio, si decide se far digitare da tastiera all'utente un comando oppure no.

        while (true) {

            msg = in.readLine();
            System.out.println(msg);

            if (msg.contains("Cosa vuoi fare oggi?")) continue;
            if (Objects.equals(msg, "1: Gioca a WORDLE!")) continue;
            if (Objects.equals(msg, "2: Visualizza statistiche personali")) continue;
            if (Objects.equals(msg, "3: Mostra notifiche")) continue;

            if (Objects.equals(msg, "--------------------")) continue;
            if (msg.contains("Statistiche di ")) continue;
            if (msg.contains("Numero partite giocate:")) continue;
            if (msg.contains("Numero partite vinte: ")) continue;
            if (msg.contains("Percentuale di partite vinte: ")) continue;
            if (msg.contains("Lunghezza dell'ultima streak di vittorie: ")) continue;
            if (msg.contains("Lunghezza della massima streak di vittorie: ")) continue;
            if (Objects.equals(msg, "Ecco la tua guess distribution:")) continue;
            if (msg.contains("\tVittoria n. ")) continue;

            if (msg.contains("Alla prossima")) break;

            String msgRisposta;
            msgRisposta = sc.nextLine();
            out.println(msgRisposta);

            // se riceve il messaggio 1, capisce che deve invocare il metodo playWordle, perché il Server è passato alla fase di gioco
            if (msgRisposta.equals("1"))
                playWORDLE(socket, in, out);

            // se riceve il messaggio 3, capisce che deve invocare il metodo showMeSharing, perché il Server è passato alla fase di invio
            // Statistiche
            if (msgRisposta.equals("3"))
                showMeSharing();

        }

        try {
            receiver.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // metodo che si occupa di stampare il contenuto dell'array list notifiche.
    private static void showMeSharing() {
        System.out.println("--------------------");
        for (int i = 0; i < notifiche.size(); i++) {
            System.out.println(notifiche.get(i));
            if (i != notifiche.size() - 1) System.out.println("--------------------");
        }
        System.out.println("--------------------");
    }

    private static void playWORDLE(Socket socket, BufferedReader in, PrintWriter out) throws IOException {

        String msg;

        // Il client entra in un ciclo infinito nel quale:
        // 1) Riceve un messaggio dal Server;
        // 2) Lo stampa a video
        // 3) Il base al contenuto del messaggio, si decide se far digitare da tastiera all'utente un comando oppure no.

        while (true) {

            msg = in.readLine();
            System.out.println(msg);

            if (Objects.equals(msg, "Prova a indovinare la parola di oggi! Digita 3 in qualunque momento per tornare al menu")) continue;
            if (msg.contains("Hai ancora a disposizione")) continue;
            if (Objects.equals(msg, "La parola deve contenere solo caratteri!")) continue;
            if (Objects.equals(msg, "La parola deve avere una lunghezza di 10 caratteri!")) continue;
            if (Objects.equals(msg, "La parola digitata non esiste nel vocabolario!")) continue;
            if (msg.length() == 10 && (msg.contains("?") || msg.contains("x"))) continue;
            if (Objects.equals(msg, "Mi dispiace! Hai esaurito i tentativi a tua disposizione, attendi la prossima parola!")) break;
            if (Objects.equals(msg, "Hai indovinato la parola! Bravissimo!")) continue;
            if (Objects.equals(msg, "Hai gia' indovinato la parola di oggi, attendi la prossima parola!")) break;

            String msgRisposta;
            msgRisposta = sc.nextLine();

            out.println(msgRisposta);

            if (Objects.equals(msg, "Vuoi condividere i risultati della partita? [Y/N]")) break;

            if (Objects.equals(msgRisposta, "3")) break;

        }

    }

    // metodo per leggere dal file di configurazione client.properties i vari parametri necessari al corretto funzionamento del client
    public static void readConfig() throws FileNotFoundException, IOException {
        InputStream input = new FileInputStream(configFile);
        Properties prop = new Properties();
        prop.load(input);
        hostname = prop.getProperty("hostname");
        port = Integer.parseInt(prop.getProperty("port"));
        input.close();
    }

}