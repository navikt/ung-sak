package no.nav.k9.sak.web.app.tjenester.brukerdialog;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.log.mdc.MdcExtendedLogContext;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.typer.AktørId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.Optional;

@Dependent
public class BrukerdialogTjeneste {
    private static final MdcExtendedLogContext LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess");

    private static final Logger logger = LoggerFactory.getLogger(BrukerdialogRestTjeneste.class);

    private final FagsakRepository fagsakRepository;

    private final BehandlingRepository behandlingRepository;

    @Inject
    public BrukerdialogTjeneste(
        FagsakRepository fagsakRepository,
        BehandlingRepository behandlingRepository

    ) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
    }

    public HarGyldigOmsorgsdagerVedtakDto harGyldigOmsorgsdagerVedtak(AktørId aktørId, AktørId pleietrengendeAktørId) {
        //FIXME spør via k9-aarskvantum når tjenesten der er klar, for å få med Infotrygd-vedtak
        //FIXME håndter hvis bruker har fått avslag etter innvilgelse
        //FIXME ta med alle typer omsorgsdagervedtak eller rename metode
        Optional<Behandling> sistBehandlingMedInnvilgetVedtak =
            fagsakRepository.finnFagsakRelatertTil(
                    FagsakYtelseType.OMSORGSPENGER_KS,
                    aktørId,
                    pleietrengendeAktørId,
                    null,
                    null,
                    null
                )
                .stream()
                .map(fagsak -> behandlingRepository.finnSisteInnvilgetBehandling(fagsak.getId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(Comparator.comparing(Behandling::getAvsluttetDato));

        if (sistBehandlingMedInnvilgetVedtak.isPresent()) {
            Behandling behandling = sistBehandlingMedInnvilgetVedtak.get();

            return new HarGyldigOmsorgsdagerVedtakDto(
                true,
                behandling.getFagsak().getSaksnummer(),
                behandling.getAvsluttetDato().toLocalDate()
            );
        } else {
            return new HarGyldigOmsorgsdagerVedtakDto(
                false, null, null
            );
        }
    }
}
