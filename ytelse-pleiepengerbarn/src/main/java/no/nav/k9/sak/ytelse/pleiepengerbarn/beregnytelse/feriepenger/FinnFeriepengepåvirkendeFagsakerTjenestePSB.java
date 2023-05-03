package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse.feriepenger;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;

@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@ApplicationScoped
public class FinnFeriepengepåvirkendeFagsakerTjenestePSB implements FinnFeriepengepåvirkendeFagsakerTjeneste {

    private FagsakRepository fagsakRepository;

    @Inject
    public FinnFeriepengepåvirkendeFagsakerTjenestePSB(FagsakRepository fagsakRepository) {
        this.fagsakRepository = fagsakRepository;
    }

    @Override
    public Set<Fagsak> finnSakerSomPåvirkerFeriepengerFor(BehandlingReferanse behandlingReferanse) {
        List<Fagsak> psbFagsakerPleietrengende = fagsakRepository.finnFagsakRelatertTil(FagsakYtelseType.PSB, behandlingReferanse.getAktørId(), behandlingReferanse.getPleietrengendeAktørId(), null, null, null);
        List<Fagsak> oppFagsakerPleietrengende = fagsakRepository.finnFagsakRelatertTil(FagsakYtelseType.OPPLÆRINGSPENGER, behandlingReferanse.getAktørId(), behandlingReferanse.getPleietrengendeAktørId(), null, null, null);

        return Stream.concat(psbFagsakerPleietrengende.stream(), oppFagsakerPleietrengende.stream())
            .filter(s -> !s.getSaksnummer().equals(behandlingReferanse.getSaksnummer()))
            .collect(Collectors.toSet());
    }
}
