package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.KalkulusResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.SamletKalkulusResultat;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilterProvider;

@FagsakYtelseTypeRef
@BehandlingStegRef(value = FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING)
@BehandlingTypeRef
@ApplicationScoped
public class FastsettSkjæringstidspunktSteg implements BeregningsgrunnlagSteg {

    private final Logger logger = LoggerFactory.getLogger(FastsettSkjæringstidspunktSteg.class);

    private BeregningTjeneste kalkulusTjeneste;
    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private VilkårPeriodeFilterProvider periodeFilterProvider;

    protected FastsettSkjæringstidspunktSteg() {
        // for CDI proxy
    }

    @Inject
    public FastsettSkjæringstidspunktSteg(BeregningTjeneste kalkulusTjeneste,
                                          SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                          BehandlingRepository behandlingRepository,
                                          BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                          VilkårPeriodeFilterProvider periodeFilterProvider) {

        this.kalkulusTjeneste = kalkulusTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.periodeFilterProvider = periodeFilterProvider;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);
        var periodeFilter = periodeFilterProvider.getFilter(ref);
        periodeFilter.ignorerAvslåttePerioder().ignorerForlengelseperioder();
        var perioderTilBeregning = new ArrayList<PeriodeTilVurdering>();
        var perioderTilVurdering = beregningsgrunnlagVilkårTjeneste.utledDetaljertPerioderTilVurdering(ref, periodeFilter);

        for (var periode : perioderTilVurdering) {
            if (periodeErUtenforFagsaksIntervall(periode.getPeriode(), behandling.getFagsak().getPeriode())) {
                avslåVilkår(kontekst, Avslagsårsak.INGEN_BEREGNINGSREGLER_TILGJENGELIG_I_LØSNINGEN, periode.getPeriode());
            } else {
                perioderTilBeregning.add(periode);
            }
        }

        if (perioderTilBeregning.isEmpty()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        } else {
            var aksjonspunktResultater = utførBeregningForPeriode(kontekst, ref, perioderTilBeregning);
            return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunktResultater);
        }
    }

    private boolean periodeErUtenforFagsaksIntervall(DatoIntervallEntitet vilkårPeriode, DatoIntervallEntitet fagsakPeriode) {
        return !vilkårPeriode.overlapper(fagsakPeriode);
    }

    private List<AksjonspunktResultat> utførBeregningForPeriode(BehandlingskontrollKontekst kontekst, BehandlingReferanse ref, List<PeriodeTilVurdering> vilkårsperioder) {
        logger.info("Beregner steg {} for perioder {} ", FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING, vilkårsperioder);

        var resultat = kalkulusTjeneste.startBeregning(ref, vilkårsperioder, BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING);
        var aksjonspunktResultater = new ArrayList<AksjonspunktResultat>();
        for (var entry : resultat.getResultater().entrySet()) {
            var kalkulusResultat = entry.getValue();
            Boolean vilkårOppfylt = kalkulusResultat.getVilkårOppfylt();
            if (vilkårOppfylt != null && !vilkårOppfylt) {
                var bgReferanse = entry.getKey();
                avslå(kontekst, vilkårsperioder, resultat, kalkulusResultat, bgReferanse);
            } else {
                aksjonspunktResultater.addAll(aksjonspunkter(kalkulusResultat));
            }
        }

        return Collections.unmodifiableList(aksjonspunktResultater);
    }

    private List<AksjonspunktResultat> aksjonspunkter(KalkulusResultat kalkulusResultat) {
        return kalkulusResultat.getBeregningAksjonspunktResultat().stream().map(BeregningResultatMapper::map).collect(Collectors.toList());
    }

    private void avslå(BehandlingskontrollKontekst kontekst, List<PeriodeTilVurdering> vilkårsperioder, SamletKalkulusResultat resultat, KalkulusResultat kalkulusResultat,
                       UUID bgReferanse) {
        var stp = resultat.getStp(bgReferanse);
        var vilkårsperiode = vilkårsperioder.stream().map(PeriodeTilVurdering::getPeriode).filter(p -> Objects.equals(stp, p.getFomDato())).findFirst()
            .orElseThrow(() -> new IllegalStateException("Finner ikke vilkårsperiode for stp [" + stp + "] for bgReferanse [" + bgReferanse + "]"));
        beregningsgrunnlagVilkårTjeneste.lagreAvslåttVilkårresultat(kontekst, vilkårsperiode, Objects.requireNonNull(kalkulusResultat.getAvslagsårsak(), "mangler avslagsårsak: " + kalkulusResultat));
    }

    private void avslåVilkår(BehandlingskontrollKontekst kontekst,
                             Avslagsårsak avslagsårsak, DatoIntervallEntitet vilkårsPeriode) {
        beregningsgrunnlagVilkårTjeneste.lagreAvslåttVilkårresultat(kontekst, vilkårsPeriode, avslagsårsak);
    }

}
