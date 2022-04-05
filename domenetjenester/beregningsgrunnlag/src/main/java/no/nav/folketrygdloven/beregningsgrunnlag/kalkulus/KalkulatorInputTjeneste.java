package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

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
@FagsakYtelseTypeRef
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
                                                              List<BeregnInput> beregnInput) {
        var opptjeningsvilkår = vilkårResultatRepository.hent(behandlingReferanse.getBehandlingId()).getVilkår(VilkårType.OPPTJENINGSVILKÅRET);
        var mapper = getYtelsesspesifikkMapper(behandlingReferanse.getFagsakYtelseType());
        return beregnInput.stream()
            .collect(Collectors.toMap(
                BeregnInput::getBgReferanse,
                mapInputDto(behandlingReferanse, iayGrunnlag, sakInntektsmeldinger, opptjeningsvilkår, mapper)));
    }

    private Function<BeregnInput, KalkulatorInputDto> mapInputDto(BehandlingReferanse behandlingReferanse, InntektArbeidYtelseGrunnlag iayGrunnlag, Collection<Inntektsmelding> sakInntektsmeldinger, Optional<Vilkår> opptjeningsvilkår, BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?> mapper) {
        return input -> {
            var vilkårsMerknad = finnVilkårmerknadForOpptjening(opptjeningsvilkår, input);
            var vilkårsperiode = input.getVilkårsperiode();
            var ytelsesGrunnlag = mapper.lagYtelsespesifiktGrunnlag(behandlingReferanse, vilkårsperiode);
            return byggDto(
                behandlingReferanse,
                iayGrunnlag,
                sakInntektsmeldinger,
                ytelsesGrunnlag,
                vilkårsperiode,
                vilkårsMerknad);
        };
    }

    /** Finner vilkårmerknad for opptjeningsvilkåret
     *  Brukes til å merke saker som skal beregnes som inaktiv § 8-47
     *
     * @param opptjeningsvilkår Opptjeningvilkår
     * @param i input
     * @return Vilkårutfallmerknad
     */
    private VilkårUtfallMerknad finnVilkårmerknadForOpptjening(Optional<Vilkår> opptjeningsvilkår, BeregnInput i) {
        VilkårUtfallMerknad vilkårsMerknad = null;
        if (opptjeningsvilkår.isPresent()) {
            vilkårsMerknad = opptjeningsvilkår.get().finnPeriodeForSkjæringstidspunkt(i.getSkjæringstidspunkt()).getMerknad();
        }
        return vilkårsMerknad;
    }

    /** Mapper inputdto for beregning
     * @param referanse Behandlingreferanse
     * @param iayGrunnlag   IAY-grunnlag
     * @param sakInntektsmeldinger  Inntektsmeldinger for saken
     * @param ytelseGrunnlag Ytelsesspesifikt grunnlag
     * @param vilkårsperiode Vilkårsperioden
     * @param vilkårsMerknad Vilkårutfallmerknad fra opptjening (for inaktiv § 8-47)
     * @return Input-dto
     */
    public KalkulatorInputDto byggDto(BehandlingReferanse referanse,
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
            throw new IllegalStateException("Forventer opptjening for vilkårsperiode: " + vilkårsperiode + ", iayGrunnlag.opptjening=" + oppgittOpptjening);
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
        return FagsakYtelseTypeRef.Lookup.find(ytelseGrunnlagMapper, ytelseType).orElseThrow(
            () -> new UnsupportedOperationException("Har ikke " + BeregningsgrunnlagYtelsespesifiktGrunnlagMapper.class.getName() + " mapper for ytelsetype=" + ytelseType));
    }
}
