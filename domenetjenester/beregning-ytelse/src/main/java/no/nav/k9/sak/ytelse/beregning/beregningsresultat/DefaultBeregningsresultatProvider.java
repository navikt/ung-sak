package no.nav.k9.sak.ytelse.beregning.beregningsresultat;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;

@ApplicationScoped
@FagsakYtelseTypeRef
@BehandlingTypeRef
public class DefaultBeregningsresultatProvider implements BeregningsresultatProvider {

    private BeregningsresultatRepository beregningsresultatRepository;

    public DefaultBeregningsresultatProvider() {
        // CDI
    }

    @Inject
    public DefaultBeregningsresultatProvider(BehandlingRepositoryProvider repositoryProvider) {
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
    }

    @Override
    public Optional<BeregningsresultatEntitet> hentBeregningsresultat(Long behandlingId) {
        return beregningsresultatRepository.hentBeregningsresultatAggregat(behandlingId)
            .map(BehandlingBeregningsresultatEntitet::getBgBeregningsresultat);
    }

    @Override
    public Optional<BeregningsresultatEntitet> hentUtbetBeregningsresultat(Long behandlingId) {
        Optional<BehandlingBeregningsresultatEntitet> aggregat = beregningsresultatRepository.hentBeregningsresultatAggregat(behandlingId);
        Optional<BeregningsresultatEntitet> utbet = aggregat
            .map(BehandlingBeregningsresultatEntitet::getUtbetBeregningsresultat);

        return utbet.isPresent() ? utbet : aggregat.map(BehandlingBeregningsresultatEntitet::getBgBeregningsresultat);
    }
}
