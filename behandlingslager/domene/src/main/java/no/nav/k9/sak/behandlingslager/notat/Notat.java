package no.nav.k9.sak.behandlingslager.notat;

import java.util.UUID;

public interface Notat {
    String getNotatTekst();

    //TODO delete?
    Long getId();

    UUID getUuid();

    void skjul(boolean skjul);
}
