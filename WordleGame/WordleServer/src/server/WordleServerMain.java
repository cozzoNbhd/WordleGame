package server;

import java.io.*;
import java.net.*;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WordleServerMain {

    // Percorso del file di configurazione del server.
    public static final String configFile = "./src/server/server.properties";

    // Numero massimo di tentativi che un utente può fare per effettuare il log
    public static int numeroTentativiLogin;

    // Numero massimo di tentativi che un utente ha a disposizione per indovinare la secret word
    public static int numeroTentativiSW;

    // Porta di ascolto del server.
    public static int port;

    // Tempo massimo di attesa (in ms) per la terminazione del server.
    public static int maxDelay;

    // Pool di thread per effettuare la gestione di molteplici client
    public static final ExecutorService pool = Executors.newCachedThreadPool();

    // Socket per ricevere le richieste dei client.
    public static ServerSocket serverSocket;

    // funzione hash per memorizzare in sicurezza le password all'interno del file di stato
    public static String MD5(String md5) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(md5.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; ++i)
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1, 3));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
        }
        return null;
    }

    // Funzione che utilizza una regex appositamente scritta per verificare se un utente inserisce un username nel
    // modo corretto. Nello specifico, l'username deve contenere almeno 1 lettera, 1 carattere speciale e deve avere
    // una lunghezza tra i 5 e i 29 caratteri
    public static boolean checkUsername(String user) {
        String regex = "^[A-Za-z]\\w{5,29}$";
        Pattern p = Pattern.compile(regex);
        if (user == null) return false;
        Matcher m = p.matcher(user);
        return m.matches();
    }

    //funzione che utilizza una regex appositamente scritta per verificare se un utente inserisce
    //Nome e cognome nel modo corretto.
    public static boolean checkNome(String nome) {
        String regex = "^[A-Za-z]+$";
        Pattern p = Pattern.compile(regex);
        if (nome == null) return false;
        Matcher m = p.matcher(nome);
        return m.matches();
    }

    // funzione che utilizza una regex appositamente scritta per verificare se un utente inserisce una password nel
    // modo corretto. La passord deve contenere almeno Un carattere maiuscolo, un carattere minuscolo, una cifra e un
    // carattere speciale, e soprattutto deve avere una lunghezza compresa tra gli 8 e i 20 caratteri.
    public static boolean checkPassword(String password) {
        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#&()–[{}]:;',?/*~$^+=<>]).{8,20}$";
        Pattern p = Pattern.compile(regex);
        if (password == null) return false;
        Matcher m = p.matcher(password);
        return m.matches();
    }

    // Funzione ausiliaria utilizzata per verificare se una stringa contiene solo caratteri. Viene utilizzata quando
    // un utente inserisce la guessed word, ossia la parola che vuole proporre al sistema e di cui vuole capire la
    // verosimiglianza con la secret word giornaliera.
    public static boolean contieneSoloCaratteri(String name) {
        char[] chars = name.toCharArray();
        for (char c : chars)
            if (!Character.isLetter(c))
                return false;
        return true;
    }

    // Funzione ausiliaria per verificare se all'interno di un file (nel nostro caso il vocabolario delle parole)
    // esista una determinata parola. Naturalmente, la parola che viene passta a suddetta funzione è stata precedentemente
    // Sottoposta al controllo "contieneSoloCaratteri". Una volta effettuati questi 2 controlli, si può passare a controllare
    // la verosimiglianza della guessed word con la secret word.
    public static boolean isInFile(String source, String book) throws IOException {
        try (FileReader fileInvc = new FileReader(source);
             BufferedReader readervc = new BufferedReader(fileInvc)) {
            String readvc = readervc.readLine();
            while (readvc != null) {
                if (readvc.contains(book))
                    return true;
                readvc = readervc.readLine();
            }
            return false;
        }
    }

    // Funzione per riuscire a inviare un messaggio UDP
    public static void sendUDPMessage(String message, String ipAddress, int port) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        InetAddress group = InetAddress.getByName(ipAddress);
        byte[] msg = message.getBytes();
        DatagramPacket packet = new DatagramPacket(msg, msg.length, group, port);
        socket.send(packet);
        socket.close();
    }

    public static void main(String[] args) throws IOException {

        try {

            // Leggo il file di configurazione.
            readConfig();

            // Apro la ServerSocket e resto in attesa di richieste.
            serverSocket = new ServerSocket(port);

            // Ad ogni nuova esecuzione del Server, viene inizialmente ripristinato lo stato del programma, leggendo dal file "Stato.json"
            // E ripristinando il contenuto della Concurrent Hash map contenente tutte le informazioni relative ai giocatori registrati
            try {
                Sistema.ripristinaStato();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Viene eseguito il Thread estrattore
            pool.execute(new Estrattore());

            // Viene eseguito il Thread Salvataggio Automatico
            pool.execute(new SalvataggioAutomatico());

            // Avvio l'handler di terminazione.
            Runtime.getRuntime().addShutdownHook(new TerminationHandler(maxDelay, pool, serverSocket));

            // Mi metto in attesa di nuove comunicazioni da parte dei vari client
            while (true) {
                Socket socket = null;
                try {
                    socket = serverSocket.accept();
                } catch (SocketException e) {
                    break;
                }
                // Per ogni nuova connessione, delego ad una nuova istanza di GestionClient il compito di occuparsi della gestione
                // del nuovo utente
                pool.execute(new GestioneClient(socket));
            }

        } catch (IOException ex) {
            System.err.println(ex);
        }

    }

    // metodo per leggere dal file di configurazione server.properties i vari parametri necessari al corretto funzionamento del server
    public static void readConfig() throws FileNotFoundException, IOException {
        InputStream input = new FileInputStream(configFile);
        Properties prop = new Properties();
        prop.load(input);
        port = Integer.parseInt(prop.getProperty("port"));
        maxDelay = Integer.parseInt(prop.getProperty("maxDelay"));
        numeroTentativiLogin = Integer.parseInt(prop.getProperty("numeroTentativiLogin"));
        numeroTentativiSW = Integer.parseInt(prop.getProperty("numeroTentativiSW"));
        input.close();
    }

}

