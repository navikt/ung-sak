package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1.KravperioderMapper;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1.TilKalkulusMapper;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.KalkulatorInputDto;
import no.nav.folketrygdloven.kalkulus.iay.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@ApplicationScoped
@FagsakYtelseTypeRef("*")
public class KalkulatorInputTjeneste {

    private Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste;
    private Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning;
    private Instance<BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?>> ytelseGrunnlagMapper;
    private VilkårResultatRepository vilkårResultatRepository;
    private boolean skalMappeAlleKrav;

    @Inject
    public KalkulatorInputTjeneste(@Any Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste,
                                   @Any Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning,
                                   @Any Instance<BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?>> ytelseGrunnlagMapper,
                                   VilkårResultatRepository vilkårResultatRepository,
                                   @KonfigVerdi(value = "MAP_ALLE_KRAV", defaultVerdi = "false") boolean skalMappeAlleKrav
    ) {
        this.opptjeningForBeregningTjeneste = Objects.requireNonNull(opptjeningForBeregningTjeneste, "opptjeningForBeregningTjeneste");
        this.inntektsmeldingerRelevantForBeregning = inntektsmeldingerRelevantForBeregning;
        this.ytelseGrunnlagMapper = ytelseGrunnlagMapper;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.skalMappeAlleKrav = skalMappeAlleKrav;
    }

    protected KalkulatorInputTjeneste() {
        // for CDI proxy
    }

    public Map<UUID, KalkulatorInputDto> byggInputPrReferanse(BehandlingReferanse behandlingReferanse,
                                                              InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                              Collection<Inntektsmelding> sakInntektsmeldinger,
                                                              Map<UUID, LocalDate> referanseTilStp) {
        Vilkår vilkår = vilkårResultatRepository.hent(behandlingReferanse.getBehandlingId()).getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow();

        var opptjeningsvilkår = vilkårResultatRepository.hent(behandlingReferanse.getBehandlingId()).getVilkår(VilkårType.OPPTJENINGSVILKÅRET);
        var mapper = getYtelsesspesifikkMapper(behandlingReferanse.getFagsakYtelseType());
        return referanseTilStp.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByValue())
            .map(i -> {
                UUID bgReferanse = i.getKey();
                var vilkårPeriode = vilkår.finnPeriodeForSkjæringstidspunkt(i.getValue());
                VilkårUtfallMerknad vilkårsMerknad = null;
                if (opptjeningsvilkår.isPresent()) {
                    vilkårsMerknad = opptjeningsvilkår.get().finnPeriodeForSkjæringstidspunkt(vilkårPeriode.getSkjæringstidspunkt()).getMerknad();
                }
                var ytelsesGrunnlag = mapper.lagYtelsespesifiktGrunnlag(behandlingReferanse, vilkårPeriode.getPeriode());
                KalkulatorInputDto kalkulatorInputDto = byggDto(
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

    public KalkulatorInputDto byggDto(BehandlingReferanse referanse,
                                      UUID bgReferanse,
                                      InntektArbeidYtelseGrunnlag iayGrunnlag,
                                      Collection<Inntektsmelding> sakInntektsmeldinger,
                                      YtelsespesifiktGrunnlagDto ytelseGrunnlag,
                                      DatoIntervallEntitet vilkårsperiode,
                                      VilkårUtfallMerknad vilkårsMerknad) {
        var stp = finnSkjæringstidspunkt(vilkårsperiode);

        OpptjeningForBeregningTjeneste tjeneste = finnOpptjeningForBeregningTjeneste(referanse);
        var imTjeneste = finnInntektsmeldingForBeregningTjeneste(referanse);

        var oppgittOpptjening = tjeneste.finnOppgittOpptjening(referanse, iayGrunnlag, stp).orElse(null);
        var grunnlagDto = mapIAYTilKalkulus(referanse, vilkårsperiode, iayGrunnlag, sakInntektsmeldinger, oppgittOpptjening, imTjeneste);
        var opptjeningAktiviteter = tjeneste.hentEksaktOpptjeningForBeregning(referanse, iayGrunnlag, vilkårsperiode);

        if (opptjeningAktiviteter.isEmpty()) {
            throw new IllegalStateException("Forventer opptjening for vilkårsperiode: " + vilkårsperiode + ", bgReferanse=" + bgReferanse + ", iayGrunnlag.opptjening=" + oppgittOpptjening);
        }

        var opptjeningAktiviteterDto = TilKalkulusMapper.mapTilDto(opptjeningAktiviteter.get(), vilkårsMerknad);

        KalkulatorInputDto kalkulatorInputDto = new KalkulatorInputDto(grunnlagDto, opptjeningAktiviteterDto, stp);

        kalkulatorInputDto.medYtelsespesifiktGrunnlag(ytelseGrunnlag);

        if (skalMappeAlleKrav) {
            kalkulatorInputDto.medRefusjonsperioderPrInntektsmelding(KravperioderMapper.map(
                referanse,
                vilkårsperiode,
                sakInntektsmeldinger,
                imTjeneste,
                grunnlagDto));
        }

        return kalkulatorInputDto;
    }


    protected InntektArbeidYtelseGrunnlagDto mapIAYTilKalkulus(BehandlingReferanse referanse,
                                                               DatoIntervallEntitet vilkårsperiode,
                                                               InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag,
                                                               Collection<Inntektsmelding> inntektsmeldinger,
                                                               OppgittOpptjening oppgittOpptjening,
                                                               InntektsmeldingerRelevantForBeregning imTjeneste) {
        return new TilKalkulusMapper().mapTilDto(inntektArbeidYtelseGrunnlag, inntektsmeldinger, referanse.getAktørId(), vilkårsperiode, oppgittOpptjening, imTjeneste, referanse);
    }


    protected LocalDate finnSkjæringstidspunkt(DatoIntervallEntitet vilkårsperiode) {
        return vilkårsperiode.getFomDato();
    }

    private OpptjeningForBeregningTjeneste finnOpptjeningForBeregningTjeneste(BehandlingReferanse referanse) {
        FagsakYtelseType ytelseType = referanse.getFagsakYtelseType();
        return FagsakYtelseTypeRef.Lookup.find(opptjeningForBeregningTjeneste, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + OpptjeningForBeregningTjeneste.class.getSimpleName() + " for ytelseType=" + ytelseType));
    }

    private InntektsmeldingerRelevantForBeregning finnInntektsmeldingForBeregningTjeneste(BehandlingReferanse referanse) {
        FagsakYtelseType ytelseType = referanse.getFagsakYtelseType();
        return FagsakYtelseTypeRef.Lookup.find(inntektsmeldingerRelevantForBeregning, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + InntektsmeldingerRelevantForBeregning.class.getSimpleName() + " for ytelseType=" + ytelseType));
    }

    public BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?> getYtelsesspesifikkMapper(FagsakYtelseType ytelseType) {
        var ytelseTypeKode = ytelseType.getKode();
        return FagsakYtelseTypeRef.Lookup.find(ytelseGrunnlagMapper, ytelseTypeKode).orElseThrow(
            () -> new UnsupportedOperationException("Har ikke " + BeregningsgrunnlagYtelsespesifiktGrunnlagMapper.class.getName() + " mapper for ytelsetype=" + ytelseTypeKode));
    }
}
