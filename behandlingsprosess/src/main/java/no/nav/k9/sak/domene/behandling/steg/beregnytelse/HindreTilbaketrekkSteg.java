package no.nav.k9.sak.domene.behandling.steg.beregnytelse;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.BehandlingType;
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
import no.nav.k9.sak.ytelse.beregning.beregningsresultat.BeregningsresultatProvider;
import no.nav.k9.sak.ytelse.beregning.tilbaketrekk.BRAndelSammenligning;
import no.nav.k9.sak.ytelse.beregning.tilbaketrekk.BeregningsresultatTidslinjetjeneste;
import no.nav.k9.sak.ytelse.beregning.tilbaketrekk.HindreTilbaketrekkNårAlleredeUtbetalt;
import no.nav.k9.sak.ytelse.beregning.tilbaketrekk.KopierFeriepenger;

@BehandlingStegRef(kode = "BERYT_OPPDRAG")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class HindreTilbaketrekkSteg implements BehandlingSteg {
    private static Logger log = LoggerFactory.getLogger(HindreTilbaketrekkSteg.class);
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private HindreTilbaketrekkNårAlleredeUtbetalt hindreTilbaketrekkNårAlleredeUtbetalt;
    private BeregningsresultatTidslinjetjeneste beregningsresultatTidslinjetjeneste;
    private Instance<BeregningsresultatProvider> beregningsresultatProvidere;

    HindreTilbaketrekkSteg() {
        // for CDI proxy
    }

    @Inject
    public HindreTilbaketrekkSteg(BehandlingRepositoryProvider repositoryProvider,
                                  HindreTilbaketrekkNårAlleredeUtbetalt hindreTilbaketrekkNårAlleredeUtbetalt,
                                  BeregningsresultatTidslinjetjeneste beregningsresultatTidslinjetjeneste,
                                  @Any Instance<BeregningsresultatProvider> beregningsresultatProvidere) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.hindreTilbaketrekkNårAlleredeUtbetalt = hindreTilbaketrekkNårAlleredeUtbetalt;
        this.beregningsresultatTidslinjetjeneste = beregningsresultatTidslinjetjeneste;
        this.beregningsresultatProvidere = beregningsresultatProvidere;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        BeregningsresultatProvider beregningsresultatProvider = getBeregningsresultatProvider(behandling);

        BehandlingBeregningsresultatEntitet aggregatTY = beregningsresultatRepository.hentBeregningsresultatAggregat(behandlingId)
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mangler beregningsresultat for behandling " + behandlingId));

        if (aggregatTY.skalHindreTilbaketrekk().orElse(false)) {
            BeregningsresultatEntitet revurderingTY = beregningsresultatProvider.hentBeregningsresultat(behandlingId).orElseThrow();
            LocalDateTimeline<BRAndelSammenligning> brAndelTidslinje = beregningsresultatTidslinjetjeneste.lagTidslinjeForRevurdering(BehandlingReferanse.fra(behandling));
            BeregningsresultatEntitet utbetBR = hindreTilbaketrekkNårAlleredeUtbetalt.reberegn(revurderingTY, brAndelTidslinje);

            KopierFeriepenger.kopierFraTil(behandlingId, revurderingTY, utbetBR);

            log.info("Skal forhindre tilbaketrekk, nytt utbetal beregningsresultat");
            beregningsresultatRepository.lagreUtbetBeregningsresultat(behandling, utbetBR);
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private BeregningsresultatProvider getBeregningsresultatProvider(Behandling behandling) {
        if (BehandlingType.FØRSTEGANGSSØKNAD.equals(behandling.getType())) {
            throw new IllegalArgumentException("Tilbaketrekk ikke støttet for førstegangsbehandling");
        }
        return BehandlingTypeRef.Lookup.find(BeregningsresultatProvider.class, beregningsresultatProvidere, behandling.getFagsakYtelseType(), behandling.getType())
            .orElseThrow(() ->  new UnsupportedOperationException("BeregningsresultatProvider ikke implementert for ytelse [" + behandling.getFagsakYtelseType() + "], behandlingtype [" + behandling.getType() + "]"));
    }

}
