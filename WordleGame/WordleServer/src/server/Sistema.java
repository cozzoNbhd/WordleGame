package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.*;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;

// Classe statica contente tutte le proprietà e i metodi definiti "critici" all'interno del Server.
// All'interno della classe possiamo trovare la stringa contente la secret word giornaliera, che verrà
// periodicamente aggiornata, la struttura dati Concurrent Hash Map contenente le associazioni
// String (username) -> Persona (indirizzo dell'oggetto persona corrispondente all'username.
// La concurrent Hash map è molto importante perché rappresenta a tutti gli effetti lo stato "dinamico" del
// Sistema: eventuali cambiamenti (ad es: alle statistiche di un giocatore) verranno prima prorogate nella struttura
// dati, e vedremo che successivamente esisterà un thread il cui unico scopo è di effettuare un salvataggio automatico
// all'interno del sistema.
// Successivamente, troviamo il riferimento al numero di giocatori registrati nel nostro gioco, e semplicemente
// corrisponde alla dimensione della concurrent hash map.
// Inoltre, è importante che la concurrent Hash Map sia definita "concurrent" e non "Hash Map", in quanto è necessario
// Prevedere il caso di 2 thread che simultaneamente vogliono scrivere al suo interno.
// Oltre a ciò, è contenuto un oggetto di tipo File, riferito al percorso contenente tutte le parole del nostro
// vocabolario, un oggetto Gson che serve ad eseguire i vari metodi di serializzazione e deserializzazione di un
// oggetto da e verso un file, nonché il riferimento al tipo che l'oggetto gson dovrà serializzare/deserializzare.

public class Sistema {

    public static volatile String secretWord;

    public static volatile ConcurrentHashMap<String, Giocatore> listaUtenti;

    public static volatile int numGiocatori;

    public static File file = new File("./src/server/words.txt");

    public static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static Type type = new TypeToken<ConcurrentHashMap<String, Giocatore>>() {}.getType();

    // Il metodo ripristinaStato() è un metodo speciale che viene eseguito ad ogni nuova esecuzione del
    // Server. Serve per riuscire a ricostruire lo stato del gioco risalente all'ultima volta che il Server
    // è stato interrotto. Se ciò non avvenisse, ad ogni nuova esecuzione non potremo riuscire a memorizzare
    // la lista degli utenti registrati nel gioco.
    // Il file di stato, chiamato "Stato.json", è un file in cui è fondamentalmente presente il contenuto della
    // concurrent hash map ad un dato momento nel tempo. Il contenuto è tanto più aggiornato possibile (ogni 30
    // secondi il contenuto viene aggiornato da un apposito Thread)
    public static void ripristinaStato() throws IOException {
        // Come prima cosa, eseguo la de-serializzazione dal file di stato e memorizzo il contenuto risultante
        // nella concurrent hashmap "lista utenti" (inizialmente vuota).
        JsonReader reader = new JsonReader(new FileReader("./src/server/Stato.json"));
        listaUtenti = gson.fromJson(reader, type);
        // se, dopo aver eseguito la de-serializzazione, il contenuto di lista utenti è comunque null, vuol dire
        // che nessun utente è ancora registrato, e quindi la lista utenti viene inizializzata come oggetto vuoto.
        if (listaUtenti == null) {
            listaUtenti = new ConcurrentHashMap<>();
            numGiocatori = 0; // naturalmente il numero di giocatori dev'essere uguale a 0, perché nessun
            // utente è ancora registrato!
        } else { // altrimenti, all'interno di lista utenti è già stato caricato correttamente la lista
            // degli utenti già registrati, e non ci resta altro che inizializzare numGiocatori alla dimensione
            // di lista utenti.
            numGiocatori = listaUtenti.size();
        }
        reader.close();
    }


    // il metodo aggiornaLista, come già anticipato precedentemente, consente di scrivere all'interno del file
    // di stato per prorogare le ultime modifiche presenti nella Concurrent Hash map.
    // IL contenuto del file viene sovrascritto ad ogni invocazione del metodo.
    public static synchronized void aggiornaLista() throws IOException {
        JsonWriter writer = new JsonWriter(new FileWriter("./src/server/Stato.json", false));
        Sistema.gson.toJson(Sistema.listaUtenti, Sistema.type, writer);
        writer.flush();
        writer.close();
    }

    // Metodo che consente di estrarre una nuova secretWord, prendendola dal file di testo contenente tutte le
    // parole del nostro vocabolario.

    public static String estraiSW() throws IOException {
        // viene inizialmente creato un random access file in lettura
        final RandomAccessFile f = new RandomAccessFile(file, "r");
        // dopodiché, viene generato randomicamente una locazione da cui andare a leggere la nuova parola
        final long randomLocation = (long) (Math.random() * f.length());
        // con il metodo seek spostiamo il puntatore del file all'indirizzo della locazione random.
        f.seek(randomLocation);
        // a questo punto, leggiamo la riga successiva e il contenuto letto sarà la nuova secretWord
        f.readLine();
        String randomWord = f.readLine();
        f.close();
        secretWord = randomWord; // modifichiamo infine il contenuto di secretWord con la nuova parola.
        return secretWord;
    }

    // Il metodo ripristinaTentativi() viene invocato quando una nuova secret Word viene estratta, questo perché,
    // per le regole del gioco, ogni giocatore ha di nuovo a disposizione tutti i tentativi.
    // Inoltre, il flag "indovinata" (per stabilire se un giocatore ha indovinato la parola del giorno) e il flag
    // "haGiocatoOggi" (per stabilire se un giocatore ha effettuato almeno un tentativo) vengono settati a "false"

    public static void ripristinaTentativi() {
        for (String username: Sistema.listaUtenti.keySet()) {
            Sistema.listaUtenti.get(username).numTentativiRimasti = 12;
            Sistema.listaUtenti.get(username).numTentativiEffettuati = 0;
            Sistema.listaUtenti.get(username).indovinata = false;
            Sistema.listaUtenti.get(username).haGiocatoOggi = false;
        }
    }

    public static void ripristaSuggerimenti() {
        for (String username: Sistema.listaUtenti.keySet()) {
            Sistema.listaUtenti.get(username).suggerimenti = "";
        }
    }
}