package no.nav.foreldrepenger.behandling.steg.beregnytelse.fp;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.ytelse.beregning.FinnEndringsdatoBeregningsresultatTjeneste;
import no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk.BRAndelSammenligning;
import no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk.BeregningsresultatTidslinjetjeneste;
import no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk.HindreTilbaketrekkNårAlleredeUtbetalt;
import no.nav.foreldrepenger.ytelse.beregning.tilbaketrekk.KopierFeriepenger;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.time.LocalDate;
import java.util.Optional;

@BehandlingStegRef(kode = "BERYT_OPPDRAG")
@BehandlingTypeRef("BT-004")
@FagsakYtelseTypeRef("FP")

@ApplicationScoped
public class HindreTilbaketrekkSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private HindreTilbaketrekkNårAlleredeUtbetalt hindreTilbaketrekkNårAlleredeUtbetalt;
    private BeregningsresultatTidslinjetjeneste beregningsresultatTidslinjetjeneste;
    private Instance<FinnEndringsdatoBeregningsresultatTjeneste> finnEndringsdatoBeregningsresultatTjenesteInstances;

    HindreTilbaketrekkSteg() {
        // for CDI proxy
    }

    @Inject
    public HindreTilbaketrekkSteg(BehandlingRepositoryProvider repositoryProvider,
                                  @Any Instance<FinnEndringsdatoBeregningsresultatTjeneste> finnEndringsdatoBeregningsresultatTjenesteInstances,
                                  HindreTilbaketrekkNårAlleredeUtbetalt hindreTilbaketrekkNårAlleredeUtbetalt,
                                  BeregningsresultatTidslinjetjeneste beregningsresultatTidslinjetjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.hindreTilbaketrekkNårAlleredeUtbetalt = hindreTilbaketrekkNårAlleredeUtbetalt;
        this.beregningsresultatTidslinjetjeneste = beregningsresultatTidslinjetjeneste;
        this.finnEndringsdatoBeregningsresultatTjenesteInstances = finnEndringsdatoBeregningsresultatTjenesteInstances;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        BehandlingBeregningsresultatEntitet aggregatTY = beregningsresultatRepository.hentBeregningsresultatAggregat(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mangler beregningsresultat for behandling " + behandlingId));

        if (aggregatTY.skalHindreTilbaketrekk().orElse(false)) {
            BeregningsresultatEntitet revurderingTY = aggregatTY.getBgBeregningsresultatFP();
            LocalDateTimeline<BRAndelSammenligning> brAndelTidslinje = beregningsresultatTidslinjetjeneste.lagTidslinjeForRevurdering(BehandlingReferanse.fra(behandling));
            BeregningsresultatEntitet utbetBR = hindreTilbaketrekkNårAlleredeUtbetalt.reberegn(revurderingTY, brAndelTidslinje);

            KopierFeriepenger.kopier(behandlingId, revurderingTY, utbetBR);

            FinnEndringsdatoBeregningsresultatTjeneste finnEndringsdatoBeregningsresultatTjeneste = FagsakYtelseTypeRef.Lookup.find(finnEndringsdatoBeregningsresultatTjenesteInstances, behandling.getFagsakYtelseType())
                .orElseThrow(() -> new IllegalStateException("Finner ikke implementasjon for FinnEndringsdatoBeregningsresultatTjeneste for behandling " + behandling.getId()));

            Optional<LocalDate> endringsDato = finnEndringsdatoBeregningsresultatTjeneste.finnEndringsdato(behandling, utbetBR);
            endringsDato.ifPresent(endringsdato -> BeregningsresultatEntitet.builder(utbetBR).medEndringsdato(endringsdato));

            beregningsresultatRepository.lagreUtbetBeregningsresultat(behandling, utbetBR);
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
