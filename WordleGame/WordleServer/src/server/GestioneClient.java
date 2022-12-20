package server;

import java.io.*;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

// Thread che incapsula al suo interno tutta la gestione della singola connessione TCP stabilita tra Client e
// Server. Il Server, in ascolto sulla sua socket Server, una volta ricevuta una nuova richiesta di connessione,
// stabilisce una nuova connessione TCP utilizzando una Socket che verrà gestita da un'istanza della classe GestioneClient
// L'oggetto Runnable è eseguito da uno dei Thread all'interno del Cached Thread Pool utilizzato dal Server.
// Il thread, dunque, gestisce tutta l'interazione con il client, dal momento in cui si connette fino al momento in cui
// si disconnette.
public class GestioneClient implements Runnable {

    private Socket connection; // socket relativa alla connessione con il singolo client
    private StatoUtente stato; // variabile che varia a seconda di cosa sta facendo l'utente
    private BufferedReader in; // Canale da cui leggere i comandi inviati dal client
    private PrintWriter out; // Canale in cui scrivere i messaggi da inviare al client
    private Giocatore g; // Riferimento al giocatore con cui stiamo comunicando. Fino a che l'utente non si sarà autenticato,
                         // questa variabile sarà null, ma una volta loggato, alla variabile g verrà associato il riferimento
                         // alle informazioni relative al giocatore all'interno della Concurrent Hash Map

    public GestioneClient(Socket connection) throws IOException {
        this.connection = connection;
        this.stato = StatoUtente.NONLOGGATO; // inizialmente l'utente è da considerarsi non loggato
        this.in = new BufferedReader(new InputStreamReader(this.connection.getInputStream())); // inizializzo il canale di input
        this.out = new PrintWriter(this.connection.getOutputStream(), true); // inizializzo il canale di output
    }

    public void run() {

        String msg; // stringa contenente il messaggio che verrà ricevuto dal client

        String nome, cognome, username, password; // variabili che servono a memorizzare le risposte che il client invierà
        // nella fase di login e register.

        int numero_tentativi_effettuati = 0; // variabile che serve a ricordare il numero di tentativi effettuati dall'utente.
        // Quando il numero raggiungerà il numero di tentativi massimi stabiliti nel file di configurazione, l'utente verrà
        // disconnesso dal gioco.

        try {
            while (this.stato != StatoUtente.INTERRUPTED) { // fin tanto che lo stato dell'utente non è interrotto
                // invio sul canale di comunicazione i vari messaggi di gioco al client.
                this.out.println("Benvenuto!");
                this.out.println("Digita '1' per effettuare il login");
                this.out.println("Digita '2' per registrarti");
                this.out.println("Digita '3' per uscire dal gioco");
                // attendo il messaggio di risposta da parte del client
                msg = this.in.readLine();
                if (msg == null) break;
                switch (msg) { // In base al contenuto della risposta, analizzo tutti i casi:
                    case "1": // In caso l'utente abbia risposto con il comando "1", invio al Client tutte le informazioni
                              // Di cui ha bisogno per effettuare il login
                        this.out.println("Inserisci il tuo username");
                        username = this.in.readLine();
                        this.out.println("Inserisci la password");
                        password = this.in.readLine();
                        // Una volta inseriti username e password, invoco il metodo login dell'oggetto, e verifico se username e password
                        // Rispettano la sintassi corretta e soprattutto se le credenziali corrispondono effettivamente a un utente
                        // Registrato nel Sistema.
                        switch (this.login(username, password)) {
                            case 10: // messaggio d'errore restituito nel caso di username sintatticamente non valido
                                this.out.println("L'username inserito NON e' sintatticamente valido!");
                                break;
                            case 20: // messaggio d'errore restituito nel caso di password sintatticamente non valida
                                this.out.println("La password deve contenere almeno 1 carattere minuscolo, 1 maiuscolo, 1 cifra, 1 carattere speciale e una lunghezza tra 8 e 20 caratteri");
                                break;
                            case 300: // messaggio d'errore restituito nel caso di username e password non validi
                                // in questo caso viene anche incrementato il numero di tentativi effettuati, questo perché i tentativi
                                // effettuati in maniera non sintatticamente corretta non vengono conteggiati.
                                this.out.println("Username o password non validi!");
                                numero_tentativi_effettuati++;
                                break;
                            case 200: // messaggio di verifica in caso username e password coincidano con un utente registrato nel sistema
                                // (Ossia presente nella Concurrent Hash Map)
                                this.out.println("Username e password corrette!");
                                this.stato = StatoUtente.LOGGATO; // In questo caso modifico lo stato dell'utente in loggato
                                this.g = Sistema.listaUtenti.get(username); // Associo alla variabile g il riferimento all'oggetto Giocatore
                                // presente nella Concurrent Hash Map
                                this.menu(); // Vado al menù
                                break; // Una volta che l'utente sarà uscito dal menù, verrà nuovamente visualizzato il messaggio di
                                // benvenuto.
                        }
                        // Se il numero di tentativi effettuati raggiunge il numero massimo di tentativi concessi
                        if (numero_tentativi_effettuati == WordleServerMain.numeroTentativiLogin) {
                            // viene notificato all'utente di aver esaurito i tentativi
                            this.out.println("Hai esaurito i tentativi, mi dispiace!");
                            // viene modificato lo stato dell'utente a Interrupted, questo provocherà la disconnessione immediata dal
                            // Server.
                            this.stato = StatoUtente.INTERRUPTED;
                        }
                        break;
                    case "2": // In caso l'utente abbia risposto con il comando "2", invio al Client tutte le informazioni
                              // Di cui ha bisogno per effettuare la registrazione
                        this.out.println("Qual e' il tuo nome?");
                        nome = this.in.readLine();
                        this.out.println("Qual e' il tuo cognome?");
                        cognome = this.in.readLine();
                        this.out.println("Inserisci un username che ti distingua online:");
                        username = this.in.readLine();
                        this.out.println("Inserisci una password");
                        password = this.in.readLine();
                        // Una volta inseriti nome, cognome, username e password, invoco il metodo register dell'oggetto,
                        // e verifico se nome, cognome, username e password rispettano la rispettiva sintassi stabilita.
                        // In base al messaggio restituito dal metodo, capirò qual è la prossima mossa da fare.
                        switch (this.register(nome, cognome, username, password)) {
                            case 10: // messaggio d'errore restituito nel caso di nome sintatticamente non valido
                                this.out.println("Il nome inserito NON e' valido!");
                                break;
                            case 20: // messaggio d'errore restituito nel caso di cognome sintatticamente non valido
                                this.out.println("Il cognome inserito non e' valido!");
                                break;
                            case 30: // messaggio d'errore restituito nel caso di username sintatticamente non valido
                                this.out.println("L'username inserito NON e' sintatticamente valido!");
                                break;
                            case 40: // messaggio d'errore restituito nel caso username già utilizzato da un altro giocatore
                                this.out.println("L'username e' gia' stato utilizzato! Registrazione fallita!");
                                break;
                            case 50: // messaggio d'errore restituito nel caso di password sintatticamente non valida
                                this.out.println("La password deve contenere almeno 1 carattere minuscolo, 1 maiuscolo, 1 cifra, 1 carattere speciale e una lunghezza tra 8 e 20 caratteri");
                                break;
                            case 200: // messaggio di conferma di avvenuta registrazione.
                                this.out.println("Registrazione avvenuta con successo!");
                                break;
                        }
                        break;
                    case "3": // In caso l'utente avesse risposto con il comando '3', vuol dire che la sua intenzione è quella di uscire
                              // dal gioco, e in tal caso il Server risponde con un saluto e imposta lo stato dell'utente a Interrupted,
                                // causando un'uscita dal while e la conseguente disconnessione del Client.
                        this.out.println("Alla prossima!");
                        this.stato = StatoUtente.INTERRUPTED;
                        break;
                    default: // In caso l'utente inavvertitamente inserisse un altro comando non presente nella lista, verrà semplicamente
                             // Ignorato e verrà ripetuto il messaggio di benvenuto
                        break;
                }
            }

            // Termino la connessione con il Client
            System.out.println("Connessione con " + this.connection.toString() + " terminata");
            this.connection.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void menu() {

        // Questo metodo (privato e non accessibile dall'esterno) viene eseguito solo nel caso in cui l'utente abbia eseguito
        // Correttamente il login. In questa sezione di gioco l'utente ha a disposizione una lista di azioni che può eseguire,
        // Tra cui giocare a Wordle, visualizzare le sue statistiche di gioco, controllare le nuove notifiche inviate dal Server
        // Relative alle partite degli altri giocatori, e disconnettersi.

        try {

            String msg; // stringa contenente il messaggio che verrà ricevuto dal client

            while (this.stato != StatoUtente.NONLOGGATO) { // fin tanto che lo stato dell'utente risulta loggato

                // invio sul canale di comunicazione tutta la lista di comandi che il Client può eseguire

                this.out.println("Ciao " + this.g.getUsername() + "! Cosa vuoi fare oggi?");
                this.out.println("1: Gioca a WORDLE!");
                this.out.println("2: Visualizza statistiche personali");
                this.out.println(("3: Mostra notifiche"));
                this.out.println("4: Esegui il logout");

                // Leggo il messaggio proveniente dal Client
                msg = this.in.readLine();

                switch (msg) {
                    case "1": // Se l'utente ha digitato il comando 1, setto lo stato del giocatore in "in game" (perché sta per iniziare
                        // a giocare) e invoco il metodo playWORDLE(), che gestirà la fase di gioco per il Client.
                        this.stato = StatoUtente.INGAME;
                        this.playWORDLE();
                        break;
                    case "2": // Se l'utente ha digitato il comando 2, invoco il metodo dell'oggetto showMeStats(), aspetto 3 secondi
                              // E invio nuovamente la lista di comandi che il Client può eseguire.
                        this.showMeStats();
                        try {
                            TimeUnit.SECONDS.sleep(3);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case "3": // Se l'utente ha digitato il comando 3, aspetto 3 secondi, tempo che il metodo all'interno del Client
                              // Invi al Client la lista delle notifiche ricevute da quando il Client si è loggato fino a ora,
                              // Dopodiché invio nuovamente la lista di comandi che il Client può eseguire.
                        try {
                            TimeUnit.SECONDS.sleep(3);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case "4": // Se l'utente ha digitato il comando 4, gli invio un saluto, invio il messaggio speciale "STOP" al
                              // Thread del Client che si occupa di ricevere le notifiche (i messaggi Multicast), causando la sua
                              // Terminazione (di fatto il Thread verrà rieseguito a ogni login), ed infine imposto lo stato dell'utente
                              // a "Non loggato". Questo comporterà la fine del ciclo e il ritorno al menù di benvenuto.
                        this.out.println("Alla prossima " + this.g.getUsername() + "!");
                        WordleServerMain.sendUDPMessage("STOP", "230.0.0.0", 4321);
                        this.stato = StatoUtente.NONLOGGATO;
                        break;
                }

            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void playWORDLE() {

        // Questo metodo (privato e non accessibile dall'esterno) viene eseguito solo nel caso in cui l'utente abbia eseguito
        // correttamente il login e abbia digitato il comando 1 nel menù principale.
        // In questa sezione di gioco l'utente può inviare al Server la guessed word, ossia la parola che il Client pensa potrebbe
        // Avvicinarsi (o proprio essere) la secret Word del giorno. L'invio della guessed word è frutto dei suggerimenti inviati dal
        // Server, implementati come nel vero gioco di Wordle, che tenderanno l'utente verso la soluzione del rompicapo.
        // Qualora il giocatore non riuscisse ad indovinare la parola entro 12 tentativi, l'utente non avrà più il diritto di giocare
        // Fino alla prossima estrazione della nuova secret word.
        // Se l'utente, in qualsiasi momento, volesse uscire dal gioco e tornare al menù principale, può sempre digitare il comando "3"

        try {

            // Innanzitutto controllo:
            // 1) Se il numero di tentativi a disposizione per indovinare la parola è == 0 (tentativi esauriti);
            // 2) Oppure se l'utente ha già indovinato la parola del giorno
            if (this.g.numTentativiRimasti <= 0 || this.g.indovinata) {
                // In caso fossi entrato nel blocco perché il numero di tentativi a disposizione è == 0, allora notificherò ciò al client
                // E imposterò il suo stato a loggato. Questo comporterà la fine del ciclo e il ritorno al menù di benvenuto.
                if (this.g.getNumTentativiRimasti() <= 0) {
                    this.out.println("Mi dispiace! Hai esaurito i tentativi a tua disposizione, attendi la prossima parola!");
                    this.stato = StatoUtente.LOGGATO;
                }
                // In caso fossi entrato nel blocco perché l'utente ha già indovinato la parola di oggi, allora notificherò ciò al client
                // E imposterò il suo stato a loggato. Questo comporterà la fine del ciclo e il ritorno al menù di benvenuto.
                if (this.g.indovinata) {
                    this.out.println("Hai gia' indovinato la parola di oggi, attendi la prossima parola!");
                    this.stato = StatoUtente.LOGGATO;
                }
            } else { // In caso contrario, l'utente può inserire la guessedWord
                this.out.println("Prova a indovinare la parola di oggi! Digita 3 in qualunque momento per tornare al menu");
            }


            String msg;


            while (this.stato == StatoUtente.INGAME) { // fin tanto che lo stato dell'utente risulta essere in game

                // Viene innanzitutto notificato all'utente il numero di tentativi che ha ancora a disposizione
                this.out.println("Hai ancora a disposizione " + Sistema.listaUtenti.get(this.g.getUsername()).numTentativiRimasti + " tentativi");

                // Chiedo all'utente di inserire la parola
                this.out.println("Inserisci la parola");

                // Leggo il messaggio da parte del Client
                msg = this.in.readLine();

                switch (msg) {
                    case "3": // Se l'utente ha risposto con il comando 3, lo faccio ritornare al menù principale, mediante il solito
                              // meccanismo già descritto
                        this.stato = StatoUtente.LOGGATO;
                        break;
                    default: // In tutti gli altri casi, memorizzo dentro la stringa response il risultato dell'invocazione del metodo
                             // sendWord, passando come parametro il messaggio ricevuto dal client.
                             // Il metodo sendWord permette di verificare la correttezza e la somiglianza con la secretWord, e, in base
                             // Al tipo di messaggio restituito, verranno eseguite diverse azioni.
                        String response = this.sendWord(msg);
                        this.out.println(response); // Inviamo al Client la risposta
                        // Se il metodo risponde che i tentativi a disposizione sono finiti, vuol dire che l'ultimo che abbiamo effettuato
                        // Era l'ultimo, e di conseguenza verremo riportati al menu principale secondo il solito meccanismo già spiegato.
                        if (Objects.equals(response, "Mi dispiace! Hai esaurito i tentativi a tua disposizione, attendi la prossima parola!")) {
                            this.stato = StatoUtente.LOGGATO;
                        }
                        // Se il metodo risponde che abbiamo indovinato la parola, il campo "indovinata" dell'utente all'interno della
                        // Concurrent Hash Map verrà impostato a true, dopodiché verrà chiesto all'utente se vuole condividere con gli
                        // Altri giocatori i risultati della sua partita.
                        if (Objects.equals(response, "Hai indovinato la parola! Bravissimo!")) {
                            this.g.indovinata = true;
                            this.out.println("Vuoi condividere i risultati della partita? [Y/N]");
                            String msg2 = this.in.readLine();
                            switch (msg2) {
                                case "Y":
                                case "y":
                                    this.share();
                                    break;
                                default:
                                    break;
                            }
                            // Infine, lo stato dell'utente verrà reimpostato a "loggato", facendolo ritornare al menu principale
                            this.stato = StatoUtente.LOGGATO;
                        }

                        break;
                }

            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    // metodo che invia un messaggio UDP al Thread del client che si occupa di ricevere messaggi MultiCastUDP
    private void share() throws IOException {
        WordleServerMain.sendUDPMessage(this.g.getUsername() + " ha indovinato la parola di oggi!" +
                "\nLa sua partita e' andata cosi':\n" + this.g.suggerimenti, "230.0.0.0", 4321);
    }

    // Metodo utilizzato per visualizzare le statistiche del giocatore all'utente.
    private void showMeStats() {
        this.out.println("--------------------");
        this.out.println("Statistiche di " + this.g.getUsername() + ":");
        this.out.println("Numero partite giocate: " + this.g.numPartiteGiocate);
        this.out.println("Numero partite vinte: " + this.g.numVittorie);
        this.out.println("Percentuale di partite vinte: " + this.g.rateoVittoria);
        this.out.println("Lunghezza dell'ultima streak di vittorie: " + this.g.lunghezzaUltimaStreak);
        this.out.println("Lunghezza della massima streak di vittorie: " + this.g.lunghezzaMaxStreak);
        this.out.println("Ecco la tua guess distribution:");
        for (int i = 0; i < this.g.guessDistribution.size(); i++)
            this.out.println("\tVittoria n. " + (i + 1) + ": " + this.g.guessDistribution.get(i) + " tentativi impiegati");
        this.out.println("--------------------");
    }

    // Metodo per verificare la correttezza delle credenziali inserite nella fase di login.
    private int login(String username, String password) throws FileNotFoundException {
        // Se il metodo checkUsername all'intero della classe WordleServerMain restituisce false, vuol dire che
        // l'username è sintatticamente errato
        if (!WordleServerMain.checkUsername(username)) return 10;
        // Se il metodo checkPassword all'intero della classe WordleServerMain restituisce false, vuol dire che
        // la password è sintatticamente errata
        if (!WordleServerMain.checkPassword(password)) return 20;
        // Se, scansionando la Concurrent Hash Map, l'username risultata presente e la password, prima decriptata mediante
        // La funzione M5D coincide con quella già presente cifrata e associata all'username specificato, allora restituisci il
        // Messaggio di okay.
        if (Sistema.listaUtenti.containsKey(username))
            if (Objects.equals((Sistema.listaUtenti.get(username)).getPassword(), WordleServerMain.MD5(password)))
                return 200;
        // In tutti gli altri casi, restituisci 300
        return 300;
    }

    // Metodo per verificare la correttezza delle credenziali inserite nella fase di register.
    private int register(String nome, String cognome, String username, String password) throws IOException {
        // Se il metodo checkNome all'intero della classe WordleServerMain restituisce false, vuol dire che
        // il nome (o il cognome) è sintatticamente errato
        if (!WordleServerMain.checkNome(nome)) return 10;
        if (!WordleServerMain.checkNome(cognome)) return 20;
        // Se il metodo checkUsername all'intero della classe WordleServerMain restituisce false, vuol dire che
        // l'username è sintatticamente errato
        if (!WordleServerMain.checkUsername(username)) return 30;
        // Se il metodo checkPassword all'intero della classe WordleServerMain restituisce false, vuol dire che
        // la password è sintatticamente errata
        if (!WordleServerMain.checkPassword(password)) return 50;
        // Se all'interno della Concurrent Hash Map è già presente l'username digitato dal Client, restituire un errore.
        if (Sistema.listaUtenti.containsKey(username)) return 40;
        // Altrimenti, viene creato un nuovo oggetto Giocatore a cui viene passato il nome, il cognome, l'username e la password
        // del giocatore
        Giocatore g = new Giocatore(++Sistema.numGiocatori, nome, cognome, username, password);
        // Viene inserito all'interno della Concurrent Hash Map il nuovo legame username -> Giocatore
        Sistema.listaUtenti.put(g.getUsername(), g);
        // Infine, aggiorniamo lo stato (Stato.json)
        Sistema.aggiornaLista();
        return 200;
    }

    // Metodo che semplicemente setta a "" il contenuto della variabile suggerimenti relativa al giocatore all'interno
    // Della concurrent Hash Map
    private void ripristinaSuggerimenti() {
        this.g.suggerimenti = "";
    }

    // Metodo che verifica se la parola digitata dall'utente è uguale alla secret word, oppure quanto è simile ad essa.
    private String sendWord(String guessedWord) throws IOException {

        // Innanzitutto controlliamo, mediante la funzione contieneSoloCaratteri() all'interno di WordleServerMain,
        // se la parola digitata dall'utente contiene solo caratteri. In caso contrario restituiamo un messaggio di errore.

        if (!WordleServerMain.contieneSoloCaratteri(guessedWord))
            return "La parola deve contenere solo caratteri!";

        // Dopodiché controlliamo se la lunghezza della guessedWord è uguale alla lunghezza della secretWord.
        // In caso contrario restituiamo un messaggio di errore.
        if (guessedWord.length() != Sistema.secretWord.length())
            return "La parola deve avere una lunghezza di 10 caratteri!";

        // A questo punto possiamo controllare se la parola è presente nel vocabolario. In caso contrario, il tentativo non verrà
        // Conteggiato. Altrimenti.
        if (WordleServerMain.isInFile("./src/server/words.txt", guessedWord)) {
            // Viene contatto il tentativo effettuato
            this.g.numTentativiEffettuati++;
            // Se l'utente non risultava aver giocato ancora, viene aumentato il numero delle partite giocate, viene ricalcolato il suo
            // rateo vittorie/partite giocate e viene settata la variabile haGiocatoOggi a true
            if (!this.g.haGiocatoOggi) {
                this.g.numPartiteGiocate++;
                this.g.rateoVittoria = (double) this.g.numVittorie / this.g.numPartiteGiocate;
                this.g.haGiocatoOggi = true;
            }
            // Se la parola coincide esattamente con la secret Word, vuol dire che l'utente ha indovinato.
            if (Objects.equals(guessedWord, Sistema.secretWord)) {
                // Viene aggiunto come ultimo suggerimento il fatto che l'utente ha indovinato tutte le posizioni
                this.g.suggerimenti += "++++++++++";
                // Viene aumentato il numero di vittorie
                this.g.numVittorie++;
                // Viene aggiunta nella sua guess distribution il numero di tentativi effettuati per indovinare la parola corrente
                this.g.guessDistribution.add(this.g.numTentativiEffettuati);
                // Viene effettuato un controllo sulla precedente lunghezza della streak
                if (++this.g.lunghezzaStreakAttuale > this.g.lunghezzaMaxStreak)
                    this.g.lunghezzaMaxStreak = this.g.lunghezzaStreakAttuale;
                this.g.rateoVittoria = (double) this.g.numVittorie / this.g.numPartiteGiocate;
                return "Hai indovinato la parola! Bravissimo!";
            } else {
                // Altrimenti, viene invocato il metodo calcolaSuggerimenti, che si occuperà di restituire il messaggio contenente
                // Il suggerimento da inviare all'utente.
                String suggerimento = calcolaSuggerimenti(guessedWord, Sistema.secretWord);
                // Una volta ottenuto il nuovo suggerimento, verrà aggiunto alla lista (stringa) dei suggerimenti totali inviati dal
                // Server.
                this.g.suggerimenti += suggerimento + "\n";
                // Viene decrementato il numero di tentativi rimasti al giocatore
                this.g.numTentativiRimasti = this.g.numTentativiRimasti - 1;
                // Se il numero di tentativi rimasti è == 0, sono finiti i tentativi, e allora bisogna azzerare la streak di vittorie
                // Dell'utente
                if (this.g.numTentativiRimasti == 0) {
                    this.g.lunghezzaUltimaStreak = this.g.lunghezzaStreakAttuale;
                    this.g.lunghezzaStreakAttuale = 0;
                    return "Mi dispiace! Hai esaurito i tentativi a tua disposizione, attendi la prossima parola!";
                }
                return suggerimento;
            }
        }

        return "La parola digitata non esiste nel vocabolario!";
    }

    // metodo che si occupa di calcolare il suggerimento, confrontando quanto sono simili la guessedWord con la secretWord
    private String calcolaSuggerimenti(String w1, String w2) {
        assert (w1 != null && w2 != null);
        String res = "";
        for (int i = 0; i < w1.length(); i++) { // == secretWord.length
            if (w1.charAt(i) == w2.charAt(i)) {
                res += "+";
            } else if (w2.contains(w1.charAt(i) + "")) {
                res += "?";
            } else {
                res += "x";
            }
        }
        return res;
    }

}