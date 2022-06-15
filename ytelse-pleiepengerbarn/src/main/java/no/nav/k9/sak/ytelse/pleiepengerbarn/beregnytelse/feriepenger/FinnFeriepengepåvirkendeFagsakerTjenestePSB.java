package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse.feriepenger;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;

@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@ApplicationScoped
public class FinnFeriepengepåvirkendeFagsakerTjenestePSB implements FinnFeriepengepåvirkendeFagsakerTjeneste {

    private FagsakRepository fagsakRepository;

    @Inject
    public FinnFeriepengepåvirkendeFagsakerTjenestePSB(FagsakRepository fagsakRepository) {
        this.fagsakRepository = fagsakRepository;
    }

    @Override
    public Set<Fagsak> finnSakerSomPåvirkerFeriepengerFor(Fagsak fagsak) {
        LocalDate fom = fagsak.getPeriode().getFomDato().withMonth(1).withDayOfMonth(1);
        LocalDate tom = fagsak.getPeriode().getTomDato();
        List<Fagsak> psbFagsakerPleietrengende = fagsakRepository.finnFagsakRelatertTil(FagsakYtelseType.PSB, null, fagsak.getPleietrengendeAktørId(), null, fom, tom);
        List<Fagsak> oppFagsakerPleietrengende = fagsakRepository.finnFagsakRelatertTil(FagsakYtelseType.OPPLÆRINGSPENGER, null, fagsak.getPleietrengendeAktørId(), null, fom, tom);

        return Stream.concat(psbFagsakerPleietrengende.stream(), oppFagsakerPleietrengende.stream())
            .filter(s -> !fagsak.equals(s))
            .collect(Collectors.toSet());
    }
}
