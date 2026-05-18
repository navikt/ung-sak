package no.nav.ung.ytelse.ungdomsprogramytelsen.hendelsehåndtering;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandling.revurdering.ÅrsakOgPerioder;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.hendelsemottak.tjenester.FagsakerTilVurderingUtleder;
import no.nav.ung.sak.hendelsemottak.tjenester.FinnFagsakerForAktørTjeneste;
import no.nav.ung.sak.hendelsemottak.tjenester.HendelseTypeRef;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;

import java.util.List;
import java.util.Map;

/**
 * @deprecated Bakoverkompatibel håndtering av hendelser med gammelt typenavn {@code UNGDOMSPROGRAM_UTVIDET_KVOTE}.
 *             Delegerer til {@link UngdomsprogramForlengetPeriodeFagsakTilVurderingUtleder}.
 *             Fjernes etter at alle produsenter har migrert til {@code UNGDOMSPROGRAM_FORLENGET_PERIODE}.
 */
@Deprecated
@ApplicationScoped
@HendelseTypeRef("UNGDOMSPROGRAM_UTVIDET_KVOTE")
public class UngdomsprogramUtvidetKvoteFagsakTilVurderingUtleder implements FagsakerTilVurderingUtleder {

    private UngdomsprogramForlengetPeriodeFagsakTilVurderingUtleder delegate;

    public UngdomsprogramUtvidetKvoteFagsakTilVurderingUtleder() {
        // For CDI
    }

    @Inject
    public UngdomsprogramUtvidetKvoteFagsakTilVurderingUtleder(BehandlingRepository behandlingRepository,
                                                               UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
                                                               FinnFagsakerForAktørTjeneste finnFagsakerForAktørTjeneste) {
        this.delegate = new UngdomsprogramForlengetPeriodeFagsakTilVurderingUtleder(
            behandlingRepository, ungdomsprogramPeriodeRepository, finnFagsakerForAktørTjeneste);
    }

    @Override
    public Map<Fagsak, List<ÅrsakOgPerioder>> finnFagsakerTilVurdering(Hendelse hendelse) {
        return delegate.finnFagsakerTilVurdering(hendelse);
    }
}

