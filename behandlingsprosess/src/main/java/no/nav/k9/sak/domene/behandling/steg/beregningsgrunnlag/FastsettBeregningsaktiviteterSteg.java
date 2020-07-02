package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.weld.exceptions.UnsupportedOperationException;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
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

        var aksjonspunktResultater = new ArrayList<AksjonspunktResultat>();
        for (DatoIntervallEntitet periode : perioderTilVurdering) {
            if (periodeErUtenforFagsaksIntervall(periode, behandling.getFagsak().getPeriode())) {
                avslåVilkår(kontekst, Avslagsårsak.INGEN_BEREGNINGSREGLER_TILGJENGELIG_I_LØSNINGEN, periode);
            } else {
                aksjonspunktResultater.addAll(utførBeregningForPeriode(kontekst, ref, periode));
            }
        }

        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunktResultater);
    }

    private boolean periodeErUtenforFagsaksIntervall(DatoIntervallEntitet vilkårPeriode, DatoIntervallEntitet fagsakPeriode) {
        return !vilkårPeriode.overlapper(fagsakPeriode);
    }

    private List<AksjonspunktResultat> utførBeregningForPeriode(BehandlingskontrollKontekst kontekst, BehandlingReferanse ref, DatoIntervallEntitet vilkårsperiode) {
        var mapper = getYtelsesspesifikkMapper(ref.getFagsakYtelseType());
        var skjæringstidspunktForPeriode = vilkårsperiode.getFomDato();
        var ytelseGrunnlag = mapper.lagYtelsespesifiktGrunnlag(ref, vilkårsperiode);
        var kalkulusResultat = kalkulusTjeneste.startBeregning(ref, ytelseGrunnlag, skjæringstidspunktForPeriode);
        Boolean vilkårOppfylt = kalkulusResultat.getVilkårOppfylt();
        if (vilkårOppfylt != null && !vilkårOppfylt) {
            return avslåVilkår(kontekst, Objects.requireNonNull(kalkulusResultat.getAvslagsårsak(), "mangler avslagsårsak: " + kalkulusResultat), vilkårsperiode);
        } else {
            return kalkulusResultat.getBeregningAksjonspunktResultat().stream().map(BeregningResultatMapper::map).collect(Collectors.toList());
        }
    }

    private List<AksjonspunktResultat> avslåVilkår(BehandlingskontrollKontekst kontekst,
                                                   Avslagsårsak avslagsårsak, DatoIntervallEntitet vilkårsPeriode) {
        beregningsgrunnlagVilkårTjeneste.lagreAvslåttVilkårresultat(kontekst, vilkårsPeriode, avslagsårsak);
        return List.of();
    }


    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (!BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING.equals(tilSteg)) {
            Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
            var ref = BehandlingReferanse.fra(behandling);
            beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(ref, false)
                .forEach(periode -> deaktiverResultatOgSettPeriodeTilVurdering(ref, kontekst, periode));
        }
    }

    private void deaktiverResultatOgSettPeriodeTilVurdering(BehandlingReferanse ref, BehandlingskontrollKontekst kontekst, DatoIntervallEntitet periode) {
        beregningsgrunnlagVilkårTjeneste.ryddVedtaksresultatOgVilkår(kontekst, periode);
        kalkulusTjeneste.deaktiverBeregningsgrunnlag(ref, periode.getFomDato());
    }

    public BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?> getYtelsesspesifikkMapper(FagsakYtelseType ytelseType) {
        String ytelseTypeKode = ytelseType.getKode();
        var mapper = FagsakYtelseTypeRef.Lookup.find(ytelseGrunnlagMapper, ytelseTypeKode).orElseThrow(
            () -> new UnsupportedOperationException("Har ikke " + BeregningsgrunnlagYtelsespesifiktGrunnlagMapper.class.getName() + " mapper for ytelsetype=" + ytelseTypeKode));
        return mapper;
    }
}
