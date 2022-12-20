package server;

import java.util.ArrayList;

// Classe contenente tutte le informazioni importanti di un Giocatore.
public class Giocatore {

    public int idUtente; // identificativo univoco che contraddistingue il giocatore all'interno della Concurrent Hash Map
    public String nome;
    public String cognome;
    public String username;
    public String password;
    public String suggerimenti; // Stringa dinamica contenente tutti i suggerimenti che il Server ha di volta in volta inviato al
    // Client durante il gioco. Solo alla fine del gioco, se l'utente indovina la secret word, verranno
    // inviati, su richiesta dell'utente, tutti i vari suggerimenti ricevuti nel corso della partita
    public int numPartiteGiocate;
    public int numVittorie;
    public double rateoVittoria;
    public int numTentativiRimasti;
    public int numTentativiEffettuati;
    public ArrayList<Integer> guessDistribution;
    public int lunghezzaUltimaStreak;
    public int lunghezzaMaxStreak;
    public int lunghezzaStreakAttuale;
    public boolean indovinata;
    public boolean haGiocatoOggi;

    public Giocatore(int idUtente, String nome, String cognome, String username, String password) {
        this.idUtente = idUtente;
        this.nome = nome;
        this.cognome = cognome;
        this.username = username;
        this.password = WordleServerMain.MD5(password);
        this.suggerimenti = ""; // Inizializzo i suggerimenti dell'utente a "". La stringa verrà resettata ad ogni nuova estrazione
        this.numPartiteGiocate = 0;
        this.numTentativiEffettuati = 0;
        this.numVittorie = 0;
        this.rateoVittoria = 0.0;
        this.lunghezzaUltimaStreak = 0;
        this.lunghezzaMaxStreak = 0;
        this.guessDistribution = new ArrayList<>();
        this.numTentativiRimasti = WordleServerMain.numeroTentativiSW;
        this.indovinata = false;
        this.haGiocatoOggi = false;
        this.lunghezzaStreakAttuale = 0;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getNumTentativiRimasti() {
        return numTentativiRimasti;
    }

}