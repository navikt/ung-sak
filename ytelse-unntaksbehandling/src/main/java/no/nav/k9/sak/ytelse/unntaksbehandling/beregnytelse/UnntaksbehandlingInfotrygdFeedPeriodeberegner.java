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
        // Ansvar:
        // Beregne periode (finn første og siste dato) som skal sendes til infotrygd for sjekk mot andre ytelser


        // Vi skal sende vilkårsperiodens fom - tom som vår periode ved unntaksbehandling
        // Vi må finne ut hvilken behandling vi jobber på.

        var fagsak = fagsakRepository.hentSakGittSaksnummer(saksnummer).orElseThrow();
        // behandling er et inkrement - siste behandling inneholder "alle tidligere behandlinger"
        var sisteBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()).orElseThrow();

        // benytter ikke årskvantumTjeneste her, men baserer oss direkte på tilkjent ytelse.

        // kan returnere empty
        Optional<BehandlingBeregningsresultatEntitet> beregningsresultatAggregat = beregningsresultatRepository.hentBeregningsresultatAggregat(sisteBehandling.getId());

        /*
          Hvis det ikke finnes noen BeregningsresultatPeriode'er her så kan det skyldes:
           1. Finnes ikke noe BehandlingBeregningsresultatEntitet (beregningsresultatAggregat)
           2. Finnes ikke noe overstyrt- eller bg-beregningsresultat på BehandlingBeregningsresultatEntitet

            Dersom dette ikke finnes så velger jeg forløpig å lage en InfotrygdFeedPeriode.annullert()
         */

        var infotrygdFeedPeriode = beregningsresultatAggregat
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

        return infotrygdFeedPeriode;
    }

    @Override
    public String getInfotrygdYtelseKode() {
        //TODO Hva er rett infotrygdytelsekode?
        return "OM";
    }
}
