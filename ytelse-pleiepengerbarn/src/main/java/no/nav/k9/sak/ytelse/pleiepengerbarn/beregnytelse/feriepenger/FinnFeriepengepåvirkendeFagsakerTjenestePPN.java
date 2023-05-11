package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse.feriepenger;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.ytelse.beregning.regler.feriepenger.SaksnummerOgSisteBehandling;

@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@ApplicationScoped
public class FinnFeriepengepåvirkendeFagsakerTjenestePPN implements FinnFeriepengepåvirkendeFagsakerTjeneste {

    private FagsakRepository fagsakRepository;

    private HentFeriepengeAndelerTjeneste hentFeriepengeAndelerTjeneste;

    FinnFeriepengepåvirkendeFagsakerTjenestePPN() {
        //for CDI proxy
    }

    @Inject
    public FinnFeriepengepåvirkendeFagsakerTjenestePPN(FagsakRepository fagsakRepository, HentFeriepengeAndelerTjeneste hentFeriepengeAndelerTjeneste) {
        this.fagsakRepository = fagsakRepository;
        this.hentFeriepengeAndelerTjeneste = hentFeriepengeAndelerTjeneste;
    }

    @Override
    public LocalDateTimeline<Set<SaksnummerOgSisteBehandling>> finnPåvirkedeSaker(BehandlingReferanse behandling) {
        Set<Fagsak> påvirkendeFagsaker = finnSakerSomPåvirkerFeriepengerFor(behandling);
        return hentFeriepengeAndelerTjeneste.finnAndelerSomKanGiFeriepenger(påvirkendeFagsaker);
    }

    private Set<Fagsak> finnSakerSomPåvirkerFeriepengerFor(BehandlingReferanse behandlingReferanse) {
        List<Fagsak> fagsakerForPleietrengende = fagsakRepository.finnFagsakRelatertTil(FagsakYtelseType.PPN, null, behandlingReferanse.getPleietrengendeAktørId(), null, null, null);

        return fagsakerForPleietrengende.stream()
            .filter(s -> !s.getSaksnummer().equals(behandlingReferanse.getSaksnummer()))
            .collect(Collectors.toSet());
    }
}
