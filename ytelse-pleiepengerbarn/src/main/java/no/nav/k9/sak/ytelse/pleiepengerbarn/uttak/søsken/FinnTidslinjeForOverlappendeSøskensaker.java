package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.søsken;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

@Dependent
public class FinnTidslinjeForOverlappendeSøskensaker {

    private static final AktørId ALLE_PLEIETRENGENDE = null;
    private static final AktørId ALLE_RELATERTE_PERSONER = null;
    private static final LocalDate ALLE_FAGSAK_FOM_DATOER = null;
    private static final LocalDate ALLE_FAGSAK_TOM_DATOER = null;
    private final FagsakRepository fagsakRepository;
    private final FinnAktuellTidslinjeForFagsak finnAktuellTidslinjeForFagsak;

    @Inject
    public FinnTidslinjeForOverlappendeSøskensaker(FagsakRepository fagsakRepository,
                                                   FinnAktuellTidslinjeForFagsak finnAktuellTidslinjeForFagsak) {
        this.fagsakRepository = fagsakRepository;
        this.finnAktuellTidslinjeForFagsak = finnAktuellTidslinjeForFagsak;
    }

    public LocalDateTimeline<Set<Saksnummer>> finnTidslinje(AktørId aktørId, FagsakYtelseType fagsakYtelseType) {
        var aktuelleOverlappendeFagsaker = finnAktuelleFagsakerForBruker(aktørId, fagsakYtelseType);
        var tidslinjeForFagsaker = finnTidslinjeForFagsaker(aktuelleOverlappendeFagsaker);
        return tidslinjeForFagsaker.filterValue(v -> v.size() > 1);
    }

    private List<Fagsak> finnAktuelleFagsakerForBruker(AktørId aktørId, FagsakYtelseType fagsakYtelseType) {
        return fagsakRepository.finnFagsakRelatertTil(fagsakYtelseType, aktørId, ALLE_PLEIETRENGENDE, ALLE_RELATERTE_PERSONER, ALLE_FAGSAK_FOM_DATOER, ALLE_FAGSAK_TOM_DATOER);
    }

    private LocalDateTimeline<Set<Saksnummer>> finnTidslinjeForFagsaker(List<Fagsak> aktuelleOverlappendeFagsaker) {
        LocalDateTimeline<Set<Saksnummer>> tidslinjeForFagsaker = LocalDateTimeline.empty();
        for (var fagsak : aktuelleOverlappendeFagsaker) {
            var tidslinjeForFagsak = finnAktuellTidslinjeForFagsak.finnTidslinje(fagsak);
            tidslinjeForFagsaker = tidslinjeForFagsaker.crossJoin(tidslinjeForFagsak.mapValue(v -> Set.of(fagsak.getSaksnummer())), StandardCombinators::union);
        }
        return tidslinjeForFagsaker;
    }


}
