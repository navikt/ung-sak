package no.nav.k9.sak.ytelse.unntaksbehandling.beregnytelse;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.ytelse.beregning.beregningsresultat.BeregningsresultatProvider;

@ApplicationScoped
@FagsakYtelseTypeRef
@BehandlingTypeRef("BT-010")
class UnntaksbehandlingBeregningsresultatProvider implements BeregningsresultatProvider {

    private BeregningsresultatRepository beregningsresultatRepository;

    public UnntaksbehandlingBeregningsresultatProvider() {
        // CDI
    }

    @Inject
    public UnntaksbehandlingBeregningsresultatProvider(BehandlingRepositoryProvider repositoryProvider) {
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
    }

    @Override
    public Optional<BeregningsresultatEntitet> hentBeregningsresultat(Long behandlingId) {
        Optional<BehandlingBeregningsresultatEntitet> aggregat = beregningsresultatRepository.hentBeregningsresultatAggregat(behandlingId);
        Optional<BeregningsresultatEntitet> overstyrt = aggregat
            .map(BehandlingBeregningsresultatEntitet::getOverstyrtBeregningsresultat);

        return overstyrt.isPresent() ? overstyrt : aggregat.map(BehandlingBeregningsresultatEntitet::getBgBeregningsresultat);
    }

    @Override
    public Optional<BeregningsresultatEntitet> hentUtbetBeregningsresultat(Long behandlingId) {
        Optional<BehandlingBeregningsresultatEntitet> aggregat = beregningsresultatRepository.hentBeregningsresultatAggregat(behandlingId);
        Optional<BeregningsresultatEntitet> utbet = aggregat
            .map(BehandlingBeregningsresultatEntitet::getUtbetBeregningsresultat);

        return utbet.isPresent() ? utbet : hentBeregningsresultat(behandlingId);
    }
}
