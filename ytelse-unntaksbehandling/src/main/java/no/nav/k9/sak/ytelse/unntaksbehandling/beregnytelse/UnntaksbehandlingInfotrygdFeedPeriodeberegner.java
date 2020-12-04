package no.nav.k9.sak.ytelse.unntaksbehandling.beregnytelse;

import static java.util.Optional.ofNullable;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedPeriode;
import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedPeriodeberegner;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.typer.Saksnummer;

//TODO Hva er rett fagsakytelsetype?
@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class UnntaksbehandlingInfotrygdFeedPeriodeberegner implements InfotrygdFeedPeriodeberegner {
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;

    @SuppressWarnings("unused")
    UnntaksbehandlingInfotrygdFeedPeriodeberegner() {
        // for CDI
    }

    @Inject
    public UnntaksbehandlingInfotrygdFeedPeriodeberegner(FagsakRepository fagsakRepository, BehandlingRepository behandlingRepository, BeregningsresultatRepository beregningsresultatRepository) {
        this.fagsakRepository = fagsakRepository;
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
    }

    @Override
    public InfotrygdFeedPeriode finnInnvilgetPeriode(Saksnummer saksnummer) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElseThrow();
        var sisteBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()).orElseThrow();
        Optional<BehandlingBeregningsresultatEntitet> beregningsresultatAggregat = beregningsresultatRepository.hentBeregningsresultatAggregat(sisteBehandling.getId());

        return beregningsresultatAggregat
            .map(
                behandlingBeregningsresultatEntitet ->
                    ofNullable(behandlingBeregningsresultatEntitet.getOverstyrtBeregningsresultat())
                        .orElse(behandlingBeregningsresultatEntitet.getBgBeregningsresultat())
            )
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder)
            .map(perioder -> {
                LocalDate fraOgMed = perioder.stream().map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom).min(LocalDate::compareTo).orElseThrow();
                LocalDate tilOgMed = perioder.stream().map(BeregningsresultatPeriode::getBeregningsresultatPeriodeTom).max(LocalDate::compareTo).orElseThrow();
                return new InfotrygdFeedPeriode(fraOgMed, tilOgMed);
            })
            .orElse(InfotrygdFeedPeriode.annullert());
    }

    @Override
    public String getInfotrygdYtelseKode() {
        //TODO Hva er rett infotrygdytelsekode?
        return "OM";
    }
}
