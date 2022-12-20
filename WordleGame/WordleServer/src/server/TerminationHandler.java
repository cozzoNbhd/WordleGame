package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

// Thread il cui unico scopo è quello di restare in attesa di un segnale CTRL-C. Non appena il segnale viene
// catturato dal thread, la Socket nella quale il Server è in attesa di richieste di nuovi client viene chiusa,
// in modo tale da non permettere più a nessun nuovo client di collegarsi.
// Successivamente, si resta in attesa che tutti i client già connessi al gioco terminino la loro attività all'
// interno del Server, per poi arrestare definitivamente il Thread pool. Come ultima operazione, il Thread invoca
// la funzione della classe Sistema "aggiornaStato". Il metodo aggiornaStato è molto importante, perché eventuali
// modifiche allo stato del Sistema verranno propagate al file di sistema, permettendo di non perdere alcun nuovo
// dato e mantere uno stato persistente.
public class TerminationHandler extends Thread {
    private int maxDelay;
    private ExecutorService pool;
    private ServerSocket serverSocket;

    public TerminationHandler(int maxDelay, ExecutorService pool, ServerSocket serverSocket) {
        this.maxDelay = maxDelay;
        this.pool = pool;
        this.serverSocket = serverSocket;
    }

    public void run() {

        System.out.println("[SERVER] Avvio terminazione...");

        // Chiudo la ServerSocket in modo tale da non accettare piu' nuove richieste.
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.printf("[SERVER] Errore: %s\n", e.getMessage());
        }

        // Faccio terminare il pool di thread.
        pool.shutdown();

        try {
            if (!pool.awaitTermination(maxDelay, TimeUnit.MILLISECONDS))
                pool.shutdownNow();
        } catch (InterruptedException e) {
            pool.shutdownNow();
        }

        try {
            Sistema.aggiornaLista();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("[SERVER] Terminato.");
    }

}