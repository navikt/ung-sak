package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.BgRef;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulatorInputTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.LagFortsettRequest;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.KalkulatorInputDto;
import no.nav.folketrygdloven.kalkulus.kodeverk.StegType;
import no.nav.folketrygdloven.kalkulus.kodeverk.YtelseTyperKalkulusStøtterKontrakt;
import no.nav.folketrygdloven.kalkulus.request.v1.FortsettBeregningListeRequest;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@FagsakYtelseTypeRef("PSB")
@FagsakYtelseTypeRef("PPN")
@BehandlingTypeRef
@BehandlingStegRef(kode = "VURDER_REF_BERGRUNN")
@ApplicationScoped
class PleiepengerVurderRefusjonLagFortsettRequest implements LagFortsettRequest {

    private PleiepengerGrunnlagMapper ytelsesspesifiktGrunnlagMapper;
    private VilkårResultatRepository vilkårResultatRepository;
    private KalkulatorInputTjeneste kalkulatorInputTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    public PleiepengerVurderRefusjonLagFortsettRequest() {
    }

    @Inject
    public PleiepengerVurderRefusjonLagFortsettRequest(@Any PleiepengerGrunnlagMapper ytelsesspesifiktGrunnlagMapper,
                                                       VilkårResultatRepository vilkårResultatRepository,
                                                       @FagsakYtelseTypeRef("*") KalkulatorInputTjeneste kalkulatorInputTjeneste,
                                                       InntektArbeidYtelseTjeneste iayTjeneste) {
        this.ytelsesspesifiktGrunnlagMapper = ytelsesspesifiktGrunnlagMapper;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.kalkulatorInputTjeneste = kalkulatorInputTjeneste;
        this.iayTjeneste = iayTjeneste;
    }

    @Override
    public FortsettBeregningListeRequest lagRequest(BehandlingReferanse referanse,
                                                    Collection<BgRef> bgReferanser,
                                                    BehandlingStegType stegType) {
        var bgRefs = BgRef.getRefs(bgReferanser);
        var ytelseType = YtelseTyperKalkulusStøtterKontrakt.fraKode(referanse.getFagsakYtelseType().getKode());
        return new FortsettBeregningListeRequest(
            referanse.getSaksnummer().getVerdi(),
            bgRefs,
            lagInputMap(bgReferanser, referanse),
            ytelseType,
            new StegType(stegType.getKode()));
    }

    private Map<UUID, KalkulatorInputDto> lagInputMap(Collection<BgRef> bgReferanser, BehandlingReferanse referanse) {
        var iayGrunnlag = iayTjeneste.hentGrunnlag(referanse.getBehandlingId());
        var sakInntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(referanse.getSaksnummer());

        Map<UUID, LocalDate> stpMap = bgReferanser.stream().collect(Collectors.toMap(BgRef::getRef, BgRef::getStp));
        return getReferanseTilInputMap(referanse, iayGrunnlag, sakInntektsmeldinger, stpMap);
    }

    private Map<UUID, KalkulatorInputDto> getReferanseTilInputMap(BehandlingReferanse behandlingReferanse,
                                                                  InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                  Collection<Inntektsmelding> sakInntektsmeldinger,
                                                                  Map<UUID, LocalDate> referanseSkjæringstidspunktMap) {
        Vilkår vilkår = vilkårResultatRepository.hent(behandlingReferanse.getBehandlingId()).getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow();

        var opptjeningsvilkår = vilkårResultatRepository.hent(behandlingReferanse.getBehandlingId()).getVilkår(VilkårType.OPPTJENINGSVILKÅRET);
        return referanseSkjæringstidspunktMap.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByValue())
            .map(entry -> {
                UUID bgReferanse = entry.getKey();
                var vilkårPeriode = vilkår.finnPeriodeForSkjæringstidspunkt(entry.getValue());
                VilkårUtfallMerknad vilkårsMerknad = null;
                if (opptjeningsvilkår.isPresent()) {
                    vilkårsMerknad = opptjeningsvilkår.get().finnPeriodeForSkjæringstidspunkt(vilkårPeriode.getSkjæringstidspunkt()).getMerknad();
                }
                var ytelsesGrunnlag = ytelsesspesifiktGrunnlagMapper.lagYtelsespesifiktGrunnlag(behandlingReferanse, vilkårPeriode.getPeriode());
                KalkulatorInputDto kalkulatorInputDto = kalkulatorInputTjeneste.byggDto(
                    behandlingReferanse,
                    bgReferanse,
                    iayGrunnlag,
                    sakInntektsmeldinger,
                    ytelsesGrunnlag,
                    vilkårPeriode.getPeriode(),
                    vilkårsMerknad);
                return new AbstractMap.SimpleEntry<>(bgReferanse,
                    kalkulatorInputDto);
            }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

}
