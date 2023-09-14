package no.nav.k9.sak.behandlingslager.notat;

import java.util.UUID;

public interface Notat {
    String getNotatTekst();

    UUID getUuid();

    void skjul(boolean skjul);

    void nyTekst(String tekst);
}
