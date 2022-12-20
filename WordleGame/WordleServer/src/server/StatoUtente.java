package server;

// classe enum contenente i vari stati possibili nel quale un utente, ad un dato momento nel programma, può trovarsi
public enum StatoUtente {
    LOGGATO, // Stato in cui l'utente è effettivamente loggato all'interno del gioco. In questo stato può giocare
    // a Wordle, visualizzare le sue statistiche personali, ricevere le notifiche dal Server in merito
    // alle partite vinte dagli altri giocatori, e soprattutto effettuare il logout
    NONLOGGATO, // in questo stato l'utente non è ancora loggato all'interno del gioco, di conseguenza, per accedere
    // a tutte le funzionalità del gioco, dovrà effettuare il login, oppure registrarsi, per poi
    // successivamente loggarsi.
    INTERRUPTED, // stato in cui l'utente non è più accettato all'interno del gioco, e di conseguenza deve essere
    // disconnesso. Questa situazione si verifica in 2 situazioni:
    // 1) quando l'utente non riesce a effettuare il login entro il numero di tentativi stabiliti;
    // 2) quando l'utente decide di disconnettersi dal gioco
    INGAME // stato in cui l'utente è considerato a tutti gli effetti nel flusso del videogioco. In questo stato,
    // L'utente può inviare al Server la propria guessedWord, per un numero di volte pari a 12.
    // Il server, mediante appositi controlli (spiegati nel dettaglio più avanti nel codice), verificherà
    // la validità della guessed word, e ne controllerà la verosimiglianza con la secret word.
}