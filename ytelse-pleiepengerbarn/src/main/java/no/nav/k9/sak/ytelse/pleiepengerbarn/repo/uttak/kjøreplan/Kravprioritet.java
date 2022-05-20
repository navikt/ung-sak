package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.kjøreplan;

import java.time.LocalDateTime;
import java.util.UUID;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;

public final class Kravprioritet implements Comparable<Kravprioritet> {
    private final Fagsak fagsak;

    private final JournalpostId journalpostId;
    private final Behandling aktuellBehandling;
    private final LocalDateTime tidspunktForKrav;

    public Kravprioritet(Fagsak fagsak, JournalpostId journalpostId, Behandling aktuellBehandling, LocalDateTime tidspunktForKrav) {
        this.fagsak = fagsak;
        this.journalpostId = journalpostId;
        this.aktuellBehandling = aktuellBehandling;
        this.tidspunktForKrav = tidspunktForKrav;
    }

    public Saksnummer getSaksnummer() {
        return fagsak.getSaksnummer();
    }

    public Fagsak getFagsak() {
        return fagsak;
    }

    /**
     * Gir siste gjeldende behandling der kravet inngår.
     * <p>
     * Dette er den åpne behandlingen for søker, og siste besluttede
     * behandling for andre søkere.
     */
    public Behandling getAktuellBehandling() {
        return aktuellBehandling;
    }

    public UUID getAktuellBehandlingUuid() {
        return aktuellBehandling.getUuid();
    }

    public LocalDateTime getTidspunktForKrav() {
        return tidspunktForKrav;
    }

    public boolean erVedtatt() {
        return aktuellBehandling.erSaksbehandlingAvsluttet();
    }

    public int compareTo(Kravprioritet other) {
        final int result = tidspunktForKrav.compareTo(other.tidspunktForKrav);
        if (result == 0) {
            return fagsak.getSaksnummer().compareTo(other.getFagsak().getSaksnummer());
        }
        return result;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

}
