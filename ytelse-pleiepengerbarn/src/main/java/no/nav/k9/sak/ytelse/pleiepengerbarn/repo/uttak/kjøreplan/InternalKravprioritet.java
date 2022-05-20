package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.kjøreplan;

import java.time.LocalDateTime;
import java.util.Objects;

import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;

final class InternalKravprioritet implements Comparable<InternalKravprioritet> {
    private final Long fagsak;
    private final Saksnummer saksnummer;

    private final JournalpostId journalpostId;
    private final Long aktuellBehandling;
    private BehandlingStatus behandlingStatus;
    private final LocalDateTime tidspunktForKrav;

    public InternalKravprioritet(Long fagsak, Saksnummer saksnummer, JournalpostId journalpostId, Long aktuellBehandling, BehandlingStatus behandlingStatus, LocalDateTime tidspunktForKrav) {
        this.fagsak = fagsak;
        this.saksnummer = saksnummer;
        this.journalpostId = journalpostId;
        this.aktuellBehandling = aktuellBehandling;
        this.behandlingStatus = behandlingStatus;
        this.tidspunktForKrav = tidspunktForKrav;
    }

    public Long getFagsak() {
        return fagsak;
    }

    /**
     * Gir siste gjeldende behandling der kravet inngår.
     * <p>
     * Dette er den åpne behandlingen for søker, og siste besluttede
     * behandling for andre søkere.
     */
    public Long getAktuellBehandling() {
        return aktuellBehandling;
    }

    public LocalDateTime getTidspunktForKrav() {
        return tidspunktForKrav;
    }

    public boolean erVedtatt() {
        return behandlingStatus.erFerdigbehandletStatus();
    }

    public boolean erUbehandlet() {
        return !behandlingStatus.erFerdigbehandletStatus();
    }

    public int compareTo(InternalKravprioritet other) {
        final int result = tidspunktForKrav.compareTo(other.tidspunktForKrav);
        if (result == 0) {
            return saksnummer.compareTo(other.saksnummer);
        }
        return result;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InternalKravprioritet that = (InternalKravprioritet) o;
        return Objects.equals(fagsak, that.fagsak) && Objects.equals(saksnummer, that.saksnummer) && Objects.equals(journalpostId, that.journalpostId) && Objects.equals(aktuellBehandling, that.aktuellBehandling) && Objects.equals(tidspunktForKrav, that.tidspunktForKrav);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fagsak, saksnummer, journalpostId, aktuellBehandling, tidspunktForKrav);
    }

    @Override
    public String toString() {
        return "InternalKravprioritet{" +
            "fagsak=" + fagsak +
            ", saksnummer=" + saksnummer +
            ", journalpostId=" + journalpostId +
            ", aktuellBehandling=" + aktuellBehandling +
            ", tidspunktForKrav=" + tidspunktForKrav +
            '}';
    }
}
