package server;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

// Thread il cui è scopo è invocare, allo scadere di un Timer impostato a 24 ore, il metodo estraiSW() e il metodo
// ripristinaTentativi().Il thread, inoltre, dopo aver generato la nuova secret word, invia a tutti gli utenti un messaggio multicast,
// in cui semplicemente viene notificata l'avvenuta generazione della nuova secret word e il conseguente reset di tutti i tentativi.
public class Estrattore implements Runnable {
    public void run() {
        final Timer timer = new Timer();
        timer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            System.out.println("La nuova parola estratta e': " + Sistema.estraiSW());
                            Sistema.ripristinaTentativi();
                            Sistema.ripristaSuggerimenti();
                            WordleServerMain.sendUDPMessage("ATTENZIONE! Nuova parola estratta! Numero di tentativi ripristinati!", "230.0.0.0", 4321);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                },
                Date.from(Instant.now()), Duration.ofDays(1).toMillis()
                // Duration.ofSeconds(60).toMillis()
        );
    }
}