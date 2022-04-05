package no.nav.k9.sak.ytelse.omsorgspenger.henleggelse;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.behandling.steg.iverksettevedtak.HenleggelsePostopsTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumDeaktiveringTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef(OMSORGSPENGER)
class OMPHenleggelsePostopsTjeneste implements HenleggelsePostopsTjeneste {

    private ÅrskvantumDeaktiveringTjeneste årskvantumDeaktiveringTjeneste;
    private ProsessTaskTjeneste prosessTaskRepository;

    public OMPHenleggelsePostopsTjeneste() {
        // For CDI
    }

    @Inject
    OMPHenleggelsePostopsTjeneste(ÅrskvantumDeaktiveringTjeneste årskvantumDeaktiveringTjeneste, ProsessTaskTjeneste prosessTaskRepository) {
        this.årskvantumDeaktiveringTjeneste = årskvantumDeaktiveringTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
    }

    @Override
    public void utfør(Behandling behandling) {
        årskvantumDeaktiveringTjeneste.meldFraDersomDeaktivering(behandling).ifPresent(task -> prosessTaskRepository.lagre(task));
    }
}
