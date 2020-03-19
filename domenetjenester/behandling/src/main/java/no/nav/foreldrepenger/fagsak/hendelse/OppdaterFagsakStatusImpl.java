package no.nav.foreldrepenger.fagsak.hendelse;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;

@ApplicationScoped
@FagsakYtelseTypeRef
public class OppdaterFagsakStatusImpl implements OppdaterFagsakStatus {

    private BehandlingRepository behandlingRepository;
    private OppdaterFagsakStatusFelles oppdaterFagsakStatusFelles;

    /**
     */
    @Inject
    public OppdaterFagsakStatusImpl(BehandlingRepositoryProvider repositoryProvider,
                                    OppdaterFagsakStatusFelles oppdaterFagsakStatusFelles) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.oppdaterFagsakStatusFelles = oppdaterFagsakStatusFelles;
    }

    OppdaterFagsakStatusImpl() {
        // CDI
    }

    @Override
    public void oppdaterFagsakNårBehandlingEndret(Behandling behandling) {
        oppdaterFagsak(behandling);
    }

    @Override
    public void avsluttFagsakUtenAktiveBehandlinger(Fagsak fagsak) {
        avsluttFagsakNårAlleBehandlingerErLukket(fagsak, null);
    }

    private void oppdaterFagsak(Behandling behandling) {

        if (Objects.equals(BehandlingStatus.AVSLUTTET, behandling.getStatus())) {
            avsluttFagsakNårAlleBehandlingerErLukket(behandling.getFagsak(), behandling);
        } else {
            // hvis en Behandling har noe annen status, setter Fagsak til Under behandling
            oppdaterFagsakStatusFelles.oppdaterFagsakStatus(behandling.getFagsak(), behandling, FagsakStatus.UNDER_BEHANDLING);
        }
    }

    private void avsluttFagsakNårAlleBehandlingerErLukket(Fagsak fagsak, Behandling behandling) {
        List<Behandling> alleÅpneBehandlinger = behandlingRepository.hentBehandlingerSomIkkeErAvsluttetForFagsakId(fagsak.getId());

        if (behandling != null) {
            alleÅpneBehandlinger.remove(behandling);
        }

        // ingen andre behandlinger er åpne
        if (alleÅpneBehandlinger.isEmpty()) {
            if (behandling == null || ingenLøpendeYtelsesvedtak(behandling)) {
                oppdaterFagsakStatusFelles.oppdaterFagsakStatus(fagsak, behandling, FagsakStatus.AVSLUTTET);
            } else {
                oppdaterFagsakStatusFelles.oppdaterFagsakStatus(fagsak, behandling, FagsakStatus.LØPENDE);
            }
        }
    }

    public boolean ingenLøpendeYtelsesvedtak(Behandling behandling) {
        if (oppdaterFagsakStatusFelles.ingenLøpendeYtelsesvedtak(behandling)) {
            return true;
        }
        Optional<Behandling> sisteInnvilgedeBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandling.getFagsakId());

        if (sisteInnvilgedeBehandling.isPresent()) {
            // FIXME K9 trengs logikk her?
            return false;
        }
        return true;
    }

}
