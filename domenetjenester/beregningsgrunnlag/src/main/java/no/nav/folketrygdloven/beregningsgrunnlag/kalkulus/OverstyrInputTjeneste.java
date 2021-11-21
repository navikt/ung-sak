package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import static no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1.TilKalkulusMapper.mapTilAktør;
import static no.nav.k9.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulus.beregning.v1.KravperioderPrArbeidsforhold;
import no.nav.folketrygdloven.kalkulus.beregning.v1.PerioderForKrav;
import no.nav.folketrygdloven.kalkulus.beregning.v1.Refusjonsperiode;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Aktør;
import no.nav.folketrygdloven.kalkulus.felles.v1.BeløpDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.JournalpostId;
import no.nav.folketrygdloven.kalkulus.felles.v1.KalkulatorInputDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Organisasjon;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.AktivitetsAvtaleDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.YrkesaktivitetDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.InntekterDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.InntektsmeldingDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.InntektsmeldingerDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.UtbetalingDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.UtbetalingsPostDto;
import no.nav.folketrygdloven.kalkulus.iay.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelseAnvistDto;
import no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelseDto;
import no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelserDto;
import no.nav.folketrygdloven.kalkulus.kodeverk.ArbeidType;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektskildeType;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektspostType;
import no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType;
import no.nav.folketrygdloven.kalkulus.kodeverk.RelatertYtelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.TemaUnderkategori;
import no.nav.folketrygdloven.kalkulus.kodeverk.VirksomhetType;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittEgenNæringDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittFrilansDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittOpptjeningDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OpptjeningAktiviteterDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OpptjeningPeriodeDto;
import no.nav.folketrygdloven.kalkulus.request.v1.HentGrunnbeløpRequest;
import no.nav.folketrygdloven.kalkulus.response.v1.Grunnbeløp;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputAktivitetOverstyring;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputOverstyringPeriode;

class OverstyrInputTjeneste {

    public static final String FRILANS_ORGNR = "871400172";
    private final VilkårResultatRepository vilkårResultatRepository;
    private final KalkulusRestKlient kalkulusRestKlient;
    private final Instance<BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?>> ytelseGrunnlagMapper;


    @Inject
    public OverstyrInputTjeneste(VilkårResultatRepository vilkårResultatRepository,
                                 KalkulusRestKlient kalkulusRestKlient,
                                 @Any Instance<BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?>> ytelseGrunnlagMapper) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.kalkulusRestKlient = kalkulusRestKlient;
        this.ytelseGrunnlagMapper = ytelseGrunnlagMapper;
    }


    Map<UUID, KalkulatorInputDto> getOverstyrtInputMap(BehandlingReferanse behandlingReferanse,
                                                       Map<UUID, LocalDate> referanseSkjæringstidspunktMap,
                                                       Map<UUID, InputOverstyringPeriode> overstyrteInput) {
        Vilkår vilkår = vilkårResultatRepository.hent(behandlingReferanse.getBehandlingId()).getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow();
        var mapper = getYtelsesspesifikkMapper(behandlingReferanse.getFagsakYtelseType());
        return referanseSkjæringstidspunktMap.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByValue())
            .map(entry -> mapTilInput(behandlingReferanse, overstyrteInput, vilkår, mapper, entry.getKey(), entry.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    private AbstractMap.SimpleEntry<UUID, KalkulatorInputDto> mapTilInput(BehandlingReferanse behandlingReferanse,
                                                                          Map<UUID, InputOverstyringPeriode> overstyrteInput,
                                                                          Vilkår vilkår,
                                                                          BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?> mapper,
                                                                          UUID bgReferanse, LocalDate skjæringstidspunkt) {
        var vilkårPeriode = vilkår.finnPeriodeForSkjæringstidspunkt(skjæringstidspunkt);
        var ytelsesGrunnlag = mapper.lagYtelsespesifiktGrunnlag(behandlingReferanse, vilkårPeriode.getPeriode());
        var kalkulatorInputDto = lagOverstyrtInput(overstyrteInput.get(bgReferanse), ytelsesGrunnlag);
        return new AbstractMap.SimpleEntry<>(bgReferanse, kalkulatorInputDto);
    }

    private KalkulatorInputDto lagOverstyrtInput(InputOverstyringPeriode inputOverstyringPeriode, YtelsespesifiktGrunnlagDto ytelsesGrunnlag) {
        KalkulatorInputDto input = new KalkulatorInputDto(
            mapIAYGrunnlag(inputOverstyringPeriode),
            mapOpptjeningsaktiviteter(inputOverstyringPeriode.getAktivitetOverstyringer()),
            inputOverstyringPeriode.getSkjæringstidspunkt());
        input.medRefusjonsperioderPrInntektsmelding(lagRefusjonsperioderPrInntektsmelding(inputOverstyringPeriode));
        input.medYtelsespesifiktGrunnlag(ytelsesGrunnlag);
        return input;
    }

    private List<KravperioderPrArbeidsforhold> lagRefusjonsperioderPrInntektsmelding(InputOverstyringPeriode inputOverstyringPeriode) {
        List<InntektsmeldingDto> inntektsmeldinger = mapInntektsmeldinger(inputOverstyringPeriode).getInntektsmeldinger();
        return inntektsmeldinger.stream()
        .filter(im -> im.getRefusjonBeløpPerMnd() != null && im.getRefusjonBeløpPerMnd().getVerdi() != null && im.getRefusjonBeløpPerMnd().getVerdi().compareTo(BigDecimal.ZERO) > 0)
            .map(im -> new KravperioderPrArbeidsforhold(im.getArbeidsgiver(), im.getArbeidsforholdRef(), List.of(mapKravPerioder(im)), mapKravPerioder(im)))
            .collect(Collectors.toList());
    }

    private PerioderForKrav mapKravPerioder(InntektsmeldingDto im) {
        return new PerioderForKrav(im.getStartDatoPermisjon(), List.of(new Refusjonsperiode(new Periode(
            im.getStartDatoPermisjon(),
            im.getRefusjonOpphører() == null ? TIDENES_ENDE : im.getRefusjonOpphører()),
            im.getRefusjonBeløpPerMnd().getVerdi())));
    }

    private OpptjeningAktiviteterDto mapOpptjeningsaktiviteter(List<InputAktivitetOverstyring> aktivitetOverstyringer) {
        return new OpptjeningAktiviteterDto(aktivitetOverstyringer.stream().map(this::mapAktivitet).collect(Collectors.toList()));
    }

    private OpptjeningPeriodeDto mapAktivitet(InputAktivitetOverstyring a) {
        return new OpptjeningPeriodeDto(
            mapTilOpptjeningAktivitetType(a.getAktivitetStatus()),
            mapPeriode(a.getPeriode()),
            mapTilAktør(a.getArbeidsgiver()),
            null);
    }

    private OpptjeningAktivitetType mapTilOpptjeningAktivitetType(AktivitetStatus aktivitetStatus) {
        return switch (aktivitetStatus) {
            case ARBEIDSTAKER -> OpptjeningAktivitetType.ARBEID;
            case FRILANSER -> OpptjeningAktivitetType.FRILANS;
            case SELVSTENDIG_NÆRINGSDRIVENDE -> OpptjeningAktivitetType.NÆRING;
            case ARBEIDSAVKLARINGSPENGER -> OpptjeningAktivitetType.ARBEIDSAVKLARING;
            case DAGPENGER -> OpptjeningAktivitetType.DAGPENGER;
            default -> OpptjeningAktivitetType.UDEFINERT;
        };
    }

    private InntektArbeidYtelseGrunnlagDto mapIAYGrunnlag(InputOverstyringPeriode inputOverstyringPeriode) {
        InntektArbeidYtelseGrunnlagDto iayGrunnlag = new InntektArbeidYtelseGrunnlagDto();

        iayGrunnlag.medArbeidDto(lagArbeidDto(inputOverstyringPeriode));
        iayGrunnlag.medInntekterDto(lagInntekterDto(inputOverstyringPeriode));
        iayGrunnlag.medYtelserDto(lagYtelserDto(inputOverstyringPeriode));
        iayGrunnlag.medInntektsmeldingerDto(mapInntektsmeldinger(inputOverstyringPeriode));
        iayGrunnlag.medOppgittOpptjeningDto(lagOppgittOpptjening(inputOverstyringPeriode));
        return iayGrunnlag;
    }

    private OppgittOpptjeningDto lagOppgittOpptjening(InputOverstyringPeriode inputOverstyringPeriode) {
        return new OppgittOpptjeningDto(
            finnOppgittFrilans(inputOverstyringPeriode).orElse(null),
            finnOppgittEgenNæring(inputOverstyringPeriode).map(List::of).orElse(Collections.emptyList()),
            List.of());
    }

    private Optional<OppgittEgenNæringDto> finnOppgittEgenNæring(InputOverstyringPeriode inputOverstyringPeriode) {
        Optional<InputAktivitetOverstyring> næring = finnNæring(inputOverstyringPeriode);
        return næring.map(n -> new OppgittEgenNæringDto(mapPeriode(n.getPeriode()),
            null,
            VirksomhetType.ANNEN,
            false,
            false,
            null,
            false,
            null,
            null));
    }

    private Optional<OppgittFrilansDto> finnOppgittFrilans(InputOverstyringPeriode inputOverstyringPeriode) {
        List<InputAktivitetOverstyring> frilans = finnFrilans(inputOverstyringPeriode.getAktivitetOverstyringer());
        if (!frilans.isEmpty()) {
            return Optional.of(new OppgittFrilansDto(false, List.of()));
        }
        return Optional.empty();
    }

    private InntektsmeldingerDto mapInntektsmeldinger(InputOverstyringPeriode inputOverstyringPeriode) {
        List<InputAktivitetOverstyring> arbeidsforhold = finnArbeidsforhold(inputOverstyringPeriode.getAktivitetOverstyringer());
        List<InntektsmeldingDto> inntektsmeldinger = arbeidsforhold.stream()
            .map(a -> mapTilInntektsmelding(a, inputOverstyringPeriode.getSkjæringstidspunkt()))
            .collect(Collectors.toList());
        return new InntektsmeldingerDto(inntektsmeldinger);
    }

    private InntektsmeldingDto mapTilInntektsmelding(InputAktivitetOverstyring a, LocalDate skjæringstidspunkt) {
        return new InntektsmeldingDto(
            mapTilAktør(a.getArbeidsgiver()),
            new BeløpDto(finnMånedsbeløp(a.getInntektPrÅr())),
            List.of(),
            List.of(),
            null,
            skjæringstidspunkt,
            null,
            new BeløpDto(finnMånedsbeløp(a.getRefusjonPrÅr())),
            new JournalpostId("MAPPET_FRA_OVERSTYRING"),
            "MAPPET_FRA_OVERSTYRING"
        );
    }

    private YtelserDto lagYtelserDto(InputOverstyringPeriode inputOverstyringPeriode) {
        List<InputAktivitetOverstyring> meldekortYtelser = finnMeldekortYtelser(inputOverstyringPeriode);
        return new YtelserDto(lagYtelser(meldekortYtelser));
    }

    private List<YtelseDto> lagYtelser(List<InputAktivitetOverstyring> meldekortYtelser) {
        return meldekortYtelser.stream()
            .map(this::mapYtelseForAktivitet)
            .collect(Collectors.toList());
    }

    private YtelseDto mapYtelseForAktivitet(InputAktivitetOverstyring a) {
        BigDecimal dagsats = a.getInntektPrÅr().erNullEllerNulltall() ? BigDecimal.ZERO : a.getInntektPrÅr().getVerdi().divide(BigDecimal.valueOf(260), RoundingMode.HALF_UP);

        var dagsatsBeløp = new BeløpDto(dagsats);
        int arbeidsdagerIPeriode = a.getPeriode().arbeidsdager().size();

        var utbetalt = new BeløpDto(dagsats.multiply(BigDecimal.valueOf(arbeidsdagerIPeriode)));
        return new YtelseDto(
            dagsatsBeløp,
            Set.of(new YtelseAnvistDto(mapPeriode(a.getPeriode()), utbetalt, dagsatsBeløp, BigDecimal.valueOf(200))),
            mapTilYtelseType(a.getAktivitetStatus()),
            mapPeriode(a.getPeriode()),
            TemaUnderkategori.UDEFINERT,
            null);
    }

    private RelatertYtelseType mapTilYtelseType(AktivitetStatus aktivitetStatus) {
        if (aktivitetStatus.equals(AktivitetStatus.DAGPENGER)) {
            return new RelatertYtelseType(FagsakYtelseType.DAGPENGER.getKode());
        } else if (aktivitetStatus.equals(AktivitetStatus.ARBEIDSAVKLARINGSPENGER)) {
            return new RelatertYtelseType(FagsakYtelseType.ARBEIDSAVKLARINGSPENGER.getKode());
        }
        throw new IllegalStateException("Kan ikke mappe " + aktivitetStatus + " til ytelsetype");
    }

    private List<InputAktivitetOverstyring> finnMeldekortYtelser(InputOverstyringPeriode inputOverstyringPeriode) {
        return inputOverstyringPeriode.getAktivitetOverstyringer().stream()
            .filter(a -> a.getAktivitetStatus().equals(AktivitetStatus.DAGPENGER) || a.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSAVKLARINGSPENGER))
            .collect(Collectors.toList());
    }

    private InntekterDto lagInntekterDto(InputOverstyringPeriode inputOverstyringPeriode) {
        return new InntekterDto(lagUtbetalinger(inputOverstyringPeriode));
    }

    private List<UtbetalingDto> lagUtbetalinger(InputOverstyringPeriode inputOverstyringPeriode) {
        List<UtbetalingDto> utbetalinger = lagRegisterinntekterForArbeidsforhold(inputOverstyringPeriode);
        utbetalinger.addAll(lagRegisterinntekterForFrilans(inputOverstyringPeriode));
        lagSigrunInntekterForNæring(inputOverstyringPeriode).ifPresent(utbetalinger::add);
        return utbetalinger;
    }

    private Optional<UtbetalingDto> lagSigrunInntekterForNæring(InputOverstyringPeriode inputOverstyringPeriode) {
        Optional<InputAktivitetOverstyring> næring = finnNæring(inputOverstyringPeriode);
        return næring.map(InputAktivitetOverstyring::getInntektPrÅr)
            .map(inntekt -> new UtbetalingDto(InntektskildeType.SIGRUN, lagTreÅrMedSigrunInntekt(inntekt, inputOverstyringPeriode.getSkjæringstidspunkt())));
    }

    private Optional<InputAktivitetOverstyring> finnNæring(InputOverstyringPeriode inputOverstyringPeriode) {
        return inputOverstyringPeriode.getAktivitetOverstyringer().stream().filter(a -> a.getAktivitetStatus().erSelvstendigNæringsdrivende())
            .findFirst();
    }

    private List<UtbetalingsPostDto> lagTreÅrMedSigrunInntekt(Beløp inntektPrÅr, LocalDate skjæringstidspunkt) {
        var g3 = kalkulusRestKlient.hentGrunnbeløp(new HentGrunnbeløpRequest(skjæringstidspunkt));
        var g2 = kalkulusRestKlient.hentGrunnbeløp(new HentGrunnbeløpRequest(skjæringstidspunkt.minusYears(1)));
        var g1 = kalkulusRestKlient.hentGrunnbeløp(new HentGrunnbeløpRequest(skjæringstidspunkt.minusYears(2)));
        return List.of(
            new UtbetalingsPostDto(finnÅr(skjæringstidspunkt.minusYears(2)), InntektspostType.SELVSTENDIG_NÆRINGSDRIVENDE, finnSkalertInntekt(inntektPrÅr, g1, g3)),
            new UtbetalingsPostDto(finnÅr(skjæringstidspunkt.minusYears(1)), InntektspostType.SELVSTENDIG_NÆRINGSDRIVENDE, finnSkalertInntekt(inntektPrÅr, g2, g3)),
            new UtbetalingsPostDto(finnÅr(skjæringstidspunkt), InntektspostType.SELVSTENDIG_NÆRINGSDRIVENDE, inntektPrÅr.getVerdi())
        );
    }

    private BigDecimal finnSkalertInntekt(Beløp inntektPrÅr, Grunnbeløp gForInntektsår, Grunnbeløp nåværendeG) {
        return inntektPrÅr.erNullEllerNulltall() ? BigDecimal.ZERO :
            inntektPrÅr.getVerdi()
                .multiply(gForInntektsår.getVerdi())
                .divide(nåværendeG.getVerdi(), RoundingMode.HALF_UP);

    }

    private Periode finnÅr(LocalDate dato) {
        return new Periode(dato.withDayOfYear(1), dato.with(TemporalAdjusters.lastDayOfYear()));
    }

    private List<UtbetalingDto> lagRegisterinntekterForFrilans(InputOverstyringPeriode inputOverstyringPeriode) {
        List<InputAktivitetOverstyring> arbeidsforhold = finnFrilans(inputOverstyringPeriode.getAktivitetOverstyringer());
        var utbetalinger = new ArrayList<UtbetalingDto>();
        arbeidsforhold.forEach(a -> {
            var månedsinntekt = finnMånedsbeløp(a.getInntektPrÅr());
            utbetalinger.add(lagLønnForEtÅr(new Organisasjon(FRILANS_ORGNR), månedsinntekt, InntektskildeType.INNTEKT_SAMMENLIGNING, inputOverstyringPeriode.getSkjæringstidspunkt()));
            utbetalinger.add(lagLønnForEtÅr(new Organisasjon(FRILANS_ORGNR), månedsinntekt, InntektskildeType.INNTEKT_BEREGNING, inputOverstyringPeriode.getSkjæringstidspunkt()));
        });
        return utbetalinger;
    }

    private BigDecimal finnMånedsbeløp(Beløp inntektPrÅr) {
        return inntektPrÅr.erNullEllerNulltall() ? BigDecimal.ZERO : inntektPrÅr.getVerdi().divide(BigDecimal.valueOf(12), RoundingMode.HALF_UP);
    }

    private List<UtbetalingDto> lagRegisterinntekterForArbeidsforhold(InputOverstyringPeriode inputOverstyringPeriode) {
        List<InputAktivitetOverstyring> arbeidsforhold = finnArbeidsforhold(inputOverstyringPeriode.getAktivitetOverstyringer());
        var utbetalinger = new ArrayList<UtbetalingDto>();
        arbeidsforhold.forEach(a -> {
            Aktør arbeidsgiver = mapTilAktør(a.getArbeidsgiver());
            var månedsinntekt = finnMånedsbeløp(a.getInntektPrÅr());
            utbetalinger.add(lagLønnForEtÅr(arbeidsgiver, månedsinntekt, InntektskildeType.INNTEKT_SAMMENLIGNING, inputOverstyringPeriode.getSkjæringstidspunkt()));
            utbetalinger.add(lagLønnForEtÅr(arbeidsgiver, månedsinntekt, InntektskildeType.INNTEKT_BEREGNING, inputOverstyringPeriode.getSkjæringstidspunkt()));
        });
        return utbetalinger;
    }

    private UtbetalingDto lagLønnForEtÅr(Aktør arbeidsgiver,
                                         BigDecimal månedsinntekt,
                                         InntektskildeType inntektSammenligning,
                                         LocalDate skjæringstidspunkt) {
        UtbetalingDto utbetalingDto = new UtbetalingDto(inntektSammenligning, lagEttÅrMedInntekt(skjæringstidspunkt, månedsinntekt));
        utbetalingDto.medArbeidsgiver(arbeidsgiver);
        return utbetalingDto;
    }

    private List<UtbetalingsPostDto> lagEttÅrMedInntekt(LocalDate skjæringstidspunkt, BigDecimal månedsinntekt) {
        LocalDate måned = skjæringstidspunkt.minusMonths(13).withDayOfMonth(1);
        var poster = new ArrayList<UtbetalingsPostDto>();
        while (måned.isBefore(skjæringstidspunkt.withDayOfMonth(1))) {
            poster.add(new UtbetalingsPostDto(new Periode(måned, måned.with(TemporalAdjusters.lastDayOfMonth())), InntektspostType.LØNN, månedsinntekt));
        }
        return poster;
    }

    private ArbeidDto lagArbeidDto(InputOverstyringPeriode inputOverstyringPeriode) {
        List<InputAktivitetOverstyring> arbeidsAktiviteter = finnArbeidsforhold(inputOverstyringPeriode.getAktivitetOverstyringer());
        List<InputAktivitetOverstyring> frilansAktiviteter = finnFrilans(inputOverstyringPeriode.getAktivitetOverstyringer());
        return new ArbeidDto(lagYrkesaktiviteter(arbeidsAktiviteter, frilansAktiviteter));
    }

    private List<InputAktivitetOverstyring> finnFrilans(List<InputAktivitetOverstyring> aktivitetOverstyringer) {
        return aktivitetOverstyringer.stream().filter(a -> a.getAktivitetStatus().erFrilanser()).collect(Collectors.toList());
    }

    private List<InputAktivitetOverstyring> finnArbeidsforhold(List<InputAktivitetOverstyring> aktivitetOverstyringer) {
        return aktivitetOverstyringer.stream().filter(a -> a.getAktivitetStatus().erArbeidstaker()).collect(Collectors.toList());
    }

    private List<YrkesaktivitetDto> lagYrkesaktiviteter(List<InputAktivitetOverstyring> arbeidsAktiviteter, List<InputAktivitetOverstyring> frilansAktiviteter) {
        List<YrkesaktivitetDto> yrkesaktiviteter = arbeidsAktiviteter.stream()
            .map(a -> new YrkesaktivitetDto(
                mapTilAktør(a.getArbeidsgiver()),
                null,
                ArbeidType.ORDINÆRT_ARBEIDSFORHOLD,
                List.of(new AktivitetsAvtaleDto(mapPeriode(a.getPeriode()), null, null))))
            .collect(Collectors.toCollection(ArrayList::new));
        yrkesaktiviteter.addAll(frilansAktiviteter.stream()
            .map(a -> new YrkesaktivitetDto(
                new Organisasjon(FRILANS_ORGNR),
                null,
                ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER,
                List.of(new AktivitetsAvtaleDto(mapPeriode(a.getPeriode()), null, null))))
            .collect(Collectors.toList()));
        return yrkesaktiviteter;
    }

    private Periode mapPeriode(DatoIntervallEntitet periode) {
        return periode.getTomDato() == null ? new Periode(periode.getFomDato(), TIDENES_ENDE) : new Periode(periode.getFomDato(), periode.getTomDato());
    }

    public BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<?> getYtelsesspesifikkMapper(FagsakYtelseType ytelseType) {
        var ytelseTypeKode = ytelseType.getKode();
        return FagsakYtelseTypeRef.Lookup.find(ytelseGrunnlagMapper, ytelseTypeKode).orElseThrow(
            () -> new UnsupportedOperationException("Har ikke " + BeregningsgrunnlagYtelsespesifiktGrunnlagMapper.class.getName() + " mapper for ytelsetype=" + ytelseTypeKode));
    }


}
