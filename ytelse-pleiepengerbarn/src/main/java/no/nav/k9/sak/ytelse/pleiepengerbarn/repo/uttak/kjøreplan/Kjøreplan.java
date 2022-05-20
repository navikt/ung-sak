package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.kjøreplan;

import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Saksnummer;

public class Kjøreplan {

    private final long aktuellFagsakId;
    private final Saksnummer aktuellSak;
    private final LocalDateTimeline<Set<AksjonPerFagsak>> kjøreplanPerPeriode;

    public Kjøreplan(long aktuellFagsakId, Saksnummer aktuellSak, LocalDateTimeline<Set<AksjonPerFagsak>> kravprioritetPerPeriode, LocalDateTimeline<List<InternalKravprioritet>> kravprioMedEldsteKrav) {
        this.aktuellFagsakId = aktuellFagsakId;
        this.aktuellSak = aktuellSak;
        this.kjøreplanPerPeriode = kravprioritetPerPeriode;
    }

    public LocalDateTimeline<Set<AksjonPerFagsak>> getPlan() {
        return kjøreplanPerPeriode;
    }

    public boolean skalVentePåAnnenSak(long fagsakId) {
        return kjøreplanPerPeriode.stream().anyMatch(segment -> skalFagsakVente(fagsakId, segment.getValue()));
    }

    private boolean skalFagsakVente(long fagsakId, Set<AksjonPerFagsak> value) {
        return value.stream()
            .filter(it -> Objects.equals(it.getFagsakId(), fagsakId))
            .anyMatch(it -> it.getAksjon() == Aksjon.VENTE_PÅ_ANNEN);
    }

    public NavigableSet<DatoIntervallEntitet> getPerioderSomSkalUtsettes(Long fagsakId) {
        if (skalVentePåAnnenSak(fagsakId)) {
            return new TreeSet<>();
        }
        // Det er viktig at disse periodene kommer ut splittet slik som kravprio er
        return kjøreplanPerPeriode.filterValue(it -> it.stream()
                .anyMatch(at -> at.getAksjon() == Aksjon.UTSETT && Objects.equals(at.getFagsakId(), fagsakId)))
            .toSegments()
            .stream()
            .map(LocalDateSegment::getLocalDateInterval)
            .map(DatoIntervallEntitet::fra)
            .collect(Collectors.toCollection(TreeSet::new));
    }

    public boolean kanAktuellFagsakFortsette() {
        return !skalVentePåAnnenSak(aktuellFagsakId);
    }

    public NavigableSet<DatoIntervallEntitet> perioderSomSkalUtsettesForAktuellFagsak() {
        return getPerioderSomSkalUtsettes(aktuellFagsakId);
    }
}
