package server;// Thread il cui scopo è di invocare, allo scadere di un Timer impostato a 30 secondi, il metodo aggiornaLista().
// Utilizzando questo thread, riusciamo a garantire la quasi totale persistenza all'interno del sistema, dal momento
// che il contenuto della lista giocatori verrà scritto sul file ogni 30 secondi.

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

// Thread il cui scopo è eseguire una riscrittura sul file di stato "Stato.json" ogni 30 secondi.
public class SalvataggioAutomatico implements Runnable {
    public void run() {
        final Timer timer = new Timer();
        timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            Sistema.aggiornaLista();
                            System.out.println("Salvataggio automatico effettuato!");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                },
                Date.from(Instant.now()), Duration.ofSeconds(30).toMillis()
        );
    }
}