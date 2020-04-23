package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static no.nav.k9.sak.behandlingskontroll.transisjoner.FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT;

import java.time.LocalDate;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.weld.exceptions.UnsupportedOperationException;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@FagsakYtelseTypeRef
@BehandlingStegRef(kode = "FASTSETT_STP_BER")
@BehandlingTypeRef
@ApplicationScoped
public class FastsettBeregningsaktiviteterSteg implements BeregningsgrunnlagSteg {

    //FIXME(k9) hvor langt tilbake skal k9 se etter arbeid med FL og SN
    public static final int ANTALL_ARBEIDSDAGER = 100;
    private Instance<KalkulusTjeneste> kalkulusTjeneste;
    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private Instance<BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?>> ytelseGrunnlagMapper;
    private Instance<UtledBeregningSkjæringstidspunktForBehandlingTjeneste> utledStpTjenester;
    private BeregningInfotrygdsakTjeneste beregningInfotrygdsakTjeneste;
    private BehandletPeriodeTjeneste behandletPeriodeTjeneste;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;

    protected FastsettBeregningsaktiviteterSteg() {
        // for CDI proxy
    }

    @Inject
    public FastsettBeregningsaktiviteterSteg(@Any Instance<KalkulusTjeneste> kalkulusTjeneste,
                                             SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                             @Any Instance<BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?>> ytelseGrunnlagMapper,
                                             BehandlingRepository behandlingRepository,
                                             @Any Instance<UtledBeregningSkjæringstidspunktForBehandlingTjeneste> utledStpTjenester,
                                             BeregningInfotrygdsakTjeneste beregningInfotrygdsakTjeneste,
                                             BehandletPeriodeTjeneste behandletPeriodeTjeneste,
                                             BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste) {

        this.kalkulusTjeneste = kalkulusTjeneste;
        this.ytelseGrunnlagMapper = ytelseGrunnlagMapper;
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.utledStpTjenester = utledStpTjenester;
        this.beregningInfotrygdsakTjeneste = beregningInfotrygdsakTjeneste;
        this.behandletPeriodeTjeneste = behandletPeriodeTjeneste;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);

        LocalDate stp = utledSkjæringstidspunkt(ref);

        //FIXME(k9)(NB! midlertidig løsning!! k9 skal etterhvert behandle OMSORGSPENGER for FL og SN
        DatoIntervallEntitet inntektsperioden = DatoIntervallEntitet.tilOgMedMinusArbeidsdager(stp, ANTALL_ARBEIDSDAGER);
        boolean sendtTilInfotrygd = beregningInfotrygdsakTjeneste.vurderOgOppdaterSakSomBehandlesAvInfotrygd(ref, kontekst, inntektsperioden);
        if (!sendtTilInfotrygd) {
            return BehandleStegResultat.fremoverført(FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT);
        } else {
            return utførBeregning(kontekst, ref);
        }
    }

    private BehandleStegResultat utførBeregning(BehandlingskontrollKontekst kontekst, BehandlingReferanse ref) {
        var mapper = getYtelsesspesifikkMapper(ref.getFagsakYtelseType());
        var ytelseGrunnlag = mapper.lagYtelsespesifiktGrunnlag(ref);
        var kalkulusResultat = FagsakYtelseTypeRef.Lookup.find(kalkulusTjeneste, ref.getFagsakYtelseType())
            .orElseThrow(() -> new IllegalArgumentException("Fant ikke kalkulustjeneste"))
            .startBeregning(ref, ytelseGrunnlag);
        Boolean vilkårOppfylt = kalkulusResultat.getVilkårOppfylt();
        if (vilkårOppfylt != null && !vilkårOppfylt) {
            return avslåVilkår(kontekst, ref);
        } else {
            return BehandleStegResultat.utførtMedAksjonspunktResultater(kalkulusResultat.getBeregningAksjonspunktResultat().stream().map(BeregningResultatMapper::map).collect(Collectors.toList()));
        }
    }

    private BehandleStegResultat avslåVilkår(BehandlingskontrollKontekst kontekst, BehandlingReferanse ref) {
        var vilkårsPeriode = behandletPeriodeTjeneste.utledPeriode(ref);
        var orginalVilkårsPeriode = behandletPeriodeTjeneste.utledOrginalVilkårsPeriode(ref);
        beregningsgrunnlagVilkårTjeneste.lagreVilkårresultat(kontekst, false, vilkårsPeriode, orginalVilkårsPeriode);
        return BehandleStegResultat.fremoverført(FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT);
    }

    private LocalDate utledSkjæringstidspunkt(BehandlingReferanse ref) {
        String ytelseTypeKode = ref.getFagsakYtelseType().getKode();
        var mapper = FagsakYtelseTypeRef.Lookup.find(utledStpTjenester, ytelseTypeKode).orElseThrow(
            () -> new UnsupportedOperationException("Har ikke " + UtledBeregningSkjæringstidspunktForBehandlingTjeneste.class.getName() + " mapper for ytelsetype=" + ytelseTypeKode));
        return mapper.utled(ref);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (!BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING.equals(tilSteg)) {
            Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
            FagsakYtelseTypeRef.Lookup.find(kalkulusTjeneste, behandling.getFagsakYtelseType())
                .orElseThrow(() -> new IllegalArgumentException("Fant ikke kalkulustjeneste"))
                .deaktiverBeregningsgrunnlag(kontekst.getBehandlingId());
        }
    }

    public BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?> getYtelsesspesifikkMapper(FagsakYtelseType ytelseType) {
        String ytelseTypeKode = ytelseType.getKode();
        var mapper = FagsakYtelseTypeRef.Lookup.find(ytelseGrunnlagMapper, ytelseTypeKode).orElseThrow(
            () -> new UnsupportedOperationException("Har ikke " + BeregningsgrunnlagYtelsespesifiktGrunnlagMapper.class.getName() + " mapper for ytelsetype=" + ytelseTypeKode));
        return mapper;
    }
}
