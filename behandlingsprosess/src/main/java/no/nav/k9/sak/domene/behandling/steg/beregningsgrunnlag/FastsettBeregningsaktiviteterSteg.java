package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.weld.exceptions.UnsupportedOperationException;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.KalkulusResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.resultat.SamletKalkulusResultat;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@FagsakYtelseTypeRef
@BehandlingStegRef(kode = "FASTSETT_STP_BER")
@BehandlingTypeRef
@ApplicationScoped
public class FastsettBeregningsaktiviteterSteg implements BeregningsgrunnlagSteg {

    private BeregningTjeneste kalkulusTjeneste;
    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private Instance<BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?>> ytelseGrunnlagMapper;

    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;

    protected FastsettBeregningsaktiviteterSteg() {
        // for CDI proxy
    }

    @Inject
    public FastsettBeregningsaktiviteterSteg(BeregningTjeneste kalkulusTjeneste,
                                             SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                             @Any Instance<BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?>> ytelseGrunnlagMapper,
                                             BehandlingRepository behandlingRepository,
                                             BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste) {

        this.kalkulusTjeneste = kalkulusTjeneste;
        this.ytelseGrunnlagMapper = ytelseGrunnlagMapper;
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);

        var perioderTilVurdering = beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(ref, true);

        var perioderTilBeregning = new ArrayList<DatoIntervallEntitet>();
        for (DatoIntervallEntitet periode : perioderTilVurdering) {
            if (periodeErUtenforFagsaksIntervall(periode, behandling.getFagsak().getPeriode())) {
                avslåVilkår(kontekst, Avslagsårsak.INGEN_BEREGNINGSREGLER_TILGJENGELIG_I_LØSNINGEN, periode);
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

    private List<AksjonspunktResultat> utførBeregningForPeriode(BehandlingskontrollKontekst kontekst, BehandlingReferanse ref, List<DatoIntervallEntitet> vilkårsperioder) {
        var mapper = getYtelsesspesifikkMapper(ref.getFagsakYtelseType());

        Map<LocalDate, YtelsespesifiktGrunnlagDto> stpTilYtelseGrunnlag = vilkårsperioder.stream()
            .collect(Collectors.toMap(v -> v.getFomDato(), v -> mapper.lagYtelsespesifiktGrunnlag(ref, v), (e1, e2) -> e1, LinkedHashMap::new));

        var resultat = kalkulusTjeneste.startBeregning(ref, stpTilYtelseGrunnlag);
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

    private void avslå(BehandlingskontrollKontekst kontekst, List<DatoIntervallEntitet> vilkårsperioder, SamletKalkulusResultat resultat, KalkulusResultat kalkulusResultat,
                       UUID bgReferanse) {
        var stp = resultat.getStp(bgReferanse);
        var vilkårsperiode = vilkårsperioder.stream().filter(p -> Objects.equals(stp, p.getFomDato())).findFirst()
            .orElseThrow(() -> new IllegalStateException("Finner ikke vilkårsperiode for stp [" + stp + "] for bgReferanse [" + bgReferanse + "]"));
        beregningsgrunnlagVilkårTjeneste.lagreAvslåttVilkårresultat(kontekst, vilkårsperiode, Objects.requireNonNull(kalkulusResultat.getAvslagsårsak(), "mangler avslagsårsak: " + kalkulusResultat));
    }

    private void avslåVilkår(BehandlingskontrollKontekst kontekst,
                             Avslagsårsak avslagsårsak, DatoIntervallEntitet vilkårsPeriode) {
        beregningsgrunnlagVilkårTjeneste.lagreAvslåttVilkårresultat(kontekst, vilkårsPeriode, avslagsårsak);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (!BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING.equals(tilSteg)) {
            Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
            var ref = BehandlingReferanse.fra(behandling);
            NavigableSet<DatoIntervallEntitet> perioderTilVurdering = beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(ref, false);
            deaktiverResultatOgSettPeriodeTilVurdering(ref, kontekst, perioderTilVurdering);
        }
    }

    private void deaktiverResultatOgSettPeriodeTilVurdering(BehandlingReferanse ref, BehandlingskontrollKontekst kontekst, NavigableSet<DatoIntervallEntitet> perioder) {
        if (perioder.isEmpty()) {
            return;
        }
        beregningsgrunnlagVilkårTjeneste.ryddVedtaksresultatOgVilkår(kontekst, perioder);
        List<LocalDate> skjæringstidspunkter = perioder.stream().map(DatoIntervallEntitet::getFomDato).collect(Collectors.toList());
        kalkulusTjeneste.deaktiverBeregningsgrunnlag(ref, skjæringstidspunkter);
    }

    public BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?> getYtelsesspesifikkMapper(FagsakYtelseType ytelseType) {
        String ytelseTypeKode = ytelseType.getKode();
        var mapper = FagsakYtelseTypeRef.Lookup.find(ytelseGrunnlagMapper, ytelseTypeKode).orElseThrow(
            () -> new UnsupportedOperationException("Har ikke " + BeregningsgrunnlagYtelsespesifiktGrunnlagMapper.class.getName() + " mapper for ytelsetype=" + ytelseTypeKode));
        return mapper;
    }
}
