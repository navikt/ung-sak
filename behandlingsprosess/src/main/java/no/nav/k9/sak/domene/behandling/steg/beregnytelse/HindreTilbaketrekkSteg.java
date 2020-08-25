package no.nav.k9.sak.domene.behandling.steg.beregnytelse;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.ytelse.beregning.tilbaketrekk.BRAndelSammenligning;
import no.nav.k9.sak.ytelse.beregning.tilbaketrekk.BeregningsresultatTidslinjetjeneste;
import no.nav.k9.sak.ytelse.beregning.tilbaketrekk.HindreTilbaketrekkNårAlleredeUtbetalt;
import no.nav.k9.sak.ytelse.beregning.tilbaketrekk.KopierFeriepenger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@BehandlingStegRef(kode = "BERYT_OPPDRAG")
@BehandlingTypeRef("BT-004")
@FagsakYtelseTypeRef
@ApplicationScoped
public class HindreTilbaketrekkSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private HindreTilbaketrekkNårAlleredeUtbetalt hindreTilbaketrekkNårAlleredeUtbetalt;
    private BeregningsresultatTidslinjetjeneste beregningsresultatTidslinjetjeneste;

    HindreTilbaketrekkSteg() {
        // for CDI proxy
    }

    @Inject
    public HindreTilbaketrekkSteg(BehandlingRepositoryProvider repositoryProvider,
                                  HindreTilbaketrekkNårAlleredeUtbetalt hindreTilbaketrekkNårAlleredeUtbetalt,
                                  BeregningsresultatTidslinjetjeneste beregningsresultatTidslinjetjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.hindreTilbaketrekkNårAlleredeUtbetalt = hindreTilbaketrekkNårAlleredeUtbetalt;
        this.beregningsresultatTidslinjetjeneste = beregningsresultatTidslinjetjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        BehandlingBeregningsresultatEntitet aggregatTY = beregningsresultatRepository.hentBeregningsresultatAggregat(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mangler beregningsresultat for behandling " + behandlingId));

        if (aggregatTY.skalHindreTilbaketrekk().orElse(false)) {
            BeregningsresultatEntitet revurderingTY = aggregatTY.getBgBeregningsresultat();
            LocalDateTimeline<BRAndelSammenligning> brAndelTidslinje = beregningsresultatTidslinjetjeneste.lagTidslinjeForRevurdering(BehandlingReferanse.fra(behandling));
            BeregningsresultatEntitet utbetBR = hindreTilbaketrekkNårAlleredeUtbetalt.reberegn(revurderingTY, brAndelTidslinje);

            KopierFeriepenger.kopier(behandlingId, revurderingTY, utbetBR);

            beregningsresultatRepository.lagreUtbetBeregningsresultat(behandling, utbetBR);
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
