package no.nav.k9.sak.ytelse.omsorgspenger.henleggelse;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.domene.behandling.steg.iverksettevedtak.HenleggelsePostopsTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumDeaktiveringTjeneste;
import no.nav.k9.sak.økonomi.simulering.klient.K9OppdragRestKlient;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER)
class OMPHenleggelsePostopsTjeneste implements HenleggelsePostopsTjeneste {

    private ÅrskvantumDeaktiveringTjeneste årskvantumDeaktiveringTjeneste;
    private K9OppdragRestKlient k9OppdragKlient;
    private ProsessTaskTjeneste prosessTaskRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private BehandlingLåsRepository behandlingLåsRepository;

    public OMPHenleggelsePostopsTjeneste() {
        // For CDI
    }

    @Inject
    OMPHenleggelsePostopsTjeneste(ÅrskvantumDeaktiveringTjeneste årskvantumDeaktiveringTjeneste, K9OppdragRestKlient k9OppdragKlient, ProsessTaskTjeneste prosessTaskRepository, BeregningsresultatRepository beregningsresultatRepository, BehandlingLåsRepository behandlingLåsRepository) {
        this.årskvantumDeaktiveringTjeneste = årskvantumDeaktiveringTjeneste;
        this.k9OppdragKlient = k9OppdragKlient;
        this.prosessTaskRepository = prosessTaskRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.behandlingLåsRepository = behandlingLåsRepository;
    }

    @Override
    public void utfør(Behandling behandling) {
        årskvantumDeaktiveringTjeneste.meldFraDersomDeaktivering(behandling).ifPresent(task -> prosessTaskRepository.lagre(task));
        k9OppdragKlient.kansellerSimulering(behandling.getUuid());
        beregningsresultatRepository.deaktiverBeregningsresultat(behandling.getId(), behandlingLåsRepository.taLås(behandling.getId()));
    }
}
