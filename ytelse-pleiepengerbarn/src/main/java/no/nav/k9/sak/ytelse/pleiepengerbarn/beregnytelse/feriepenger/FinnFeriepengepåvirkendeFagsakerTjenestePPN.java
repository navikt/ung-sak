package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse.feriepenger;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;

@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@ApplicationScoped
public class FinnFeriepengepåvirkendeFagsakerTjenestePPN implements FinnFeriepengepåvirkendeFagsakerTjeneste {

    private FagsakRepository fagsakRepository;

    @Inject
    public FinnFeriepengepåvirkendeFagsakerTjenestePPN(FagsakRepository fagsakRepository) {
        this.fagsakRepository = fagsakRepository;
    }

    @Override
    public Set<Fagsak> finnSakerSomPåvirkerFeriepengerFor(Fagsak fagsak) {
        LocalDate fom = fagsak.getPeriode().getFomDato().withMonth(1).withDayOfMonth(1);
        LocalDate tom = fagsak.getPeriode().getTomDato();
        List<Fagsak> fagsakerForPleietrengende = fagsakRepository.finnFagsakRelatertTil(FagsakYtelseType.PPN, null, fagsak.getPleietrengendeAktørId(), null, fom, tom);

        return fagsakerForPleietrengende.stream()
            .filter(s -> !fagsak.equals(s))
            .collect(Collectors.toSet());
    }
}
