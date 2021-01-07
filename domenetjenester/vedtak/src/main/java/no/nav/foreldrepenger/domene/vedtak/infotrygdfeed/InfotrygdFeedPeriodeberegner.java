package no.nav.foreldrepenger.domene.vedtak.infotrygdfeed;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.typer.Saksnummer;


@ApplicationScoped
public class InfotrygdFeedPeriodeberegner {
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;

    @SuppressWarnings("unused")
    InfotrygdFeedPeriodeberegner() {
        // for CDI
    }

    @Inject
    public InfotrygdFeedPeriodeberegner(FagsakRepository fagsakRepository, BehandlingRepository behandlingRepository, BeregningsresultatRepository beregningsresultatRepository) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
    }

    InfotrygdFeedPeriode finnInnvilgetPeriode(Saksnummer saksnummer) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElseThrow();
        var sisteBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()).orElseThrow();
        Optional<BehandlingBeregningsresultatEntitet> beregningsresultatAggregat = beregningsresultatRepository.hentBeregningsresultatAggregat(sisteBehandling.getId());

        return beregningsresultatAggregat
            .map(BehandlingBeregningsresultatEntitet::getBgBeregningsresultat)
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder)
            .filter(perioder -> !perioder.isEmpty())
            .map(perioder -> {
                LocalDate fraOgMed = perioder.stream().map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom).min(LocalDate::compareTo).orElseThrow();
                LocalDate tilOgMed = perioder.stream().map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom).max(LocalDate::compareTo).orElseThrow();
                return new InfotrygdFeedPeriode(fraOgMed, tilOgMed);
            })
            .orElse(InfotrygdFeedPeriode.annullert());
    }
}
