package no.nav.k9.sak.ytelse.unntaksbehandling.beregnytelse;

import static java.lang.String.format;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static java.util.Optional.ofNullable;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedPeriode;
import no.nav.foreldrepenger.domene.vedtak.infotrygdfeed.InfotrygdFeedPeriodeberegner;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.typer.Saksnummer;

//TODO Hva er rett fagsakytelsetype?
@FagsakYtelseTypeRef("OMP")  // Når denne skal erstatte alle: så blir argument borte her
@ApplicationScoped
public class UnntaksbehandlingInfotrygdFeedPeriodeberegner implements InfotrygdFeedPeriodeberegner {
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;

    private static final Map<FagsakYtelseType, String> ytelseTypeTilInfotrygdYtelseKode = ofEntries(
        entry(OMSORGSPENGER, "OM"),
        entry(PLEIEPENGER_SYKT_BARN, "PN")
    );

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
                        .orElse(behandlingBeregningsresultatEntitet.getBgBeregningsresultat()) // maskinelt beregnet
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
    public String getInfotrygdYtelseKode(Saksnummer saksnummer) {
        FagsakYtelseType ytelseType = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElseThrow().getYtelseType();

        return ofNullable(ytelseTypeTilInfotrygdYtelseKode.get(ytelseType))
            .orElseThrow(() -> new UnsupportedOperationException(format("Kan ikke utlede infotrygdytelsekode for ytelsetype %s, siden mapping for dette mangler", ytelseType)));
    }
}
