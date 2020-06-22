package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.Grunnbeløp;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningAktiviteter.OpptjeningPeriode;
import no.nav.folketrygdloven.kalkulus.beregning.v1.GrunnbeløpDto;
import no.nav.folketrygdloven.kalkulus.beregning.v1.RefusjonskravDatoDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Aktør;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.BeløpDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.Organisasjon;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.AktivitetsAvtaleDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidsforholdInformasjonDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidsforholdOverstyringDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.YrkesaktivitetDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.InntekterDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.InntektsmeldingDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.InntektsmeldingerDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.NaturalYtelseDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.RefusjonDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.UtbetalingDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.UtbetalingsPostDto;
import no.nav.folketrygdloven.kalkulus.iay.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelseAnvistDto;
import no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelseDto;
import no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelserDto;
import no.nav.folketrygdloven.kalkulus.kodeverk.ArbeidType;
import no.nav.folketrygdloven.kalkulus.kodeverk.ArbeidsforholdHandlingType;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektskildeType;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektspostType;
import no.nav.folketrygdloven.kalkulus.kodeverk.NaturalYtelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType;
import no.nav.folketrygdloven.kalkulus.kodeverk.RelatertYtelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.TemaUnderkategori;
import no.nav.folketrygdloven.kalkulus.kodeverk.VirksomhetType;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittEgenNæringDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittFrilansDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittFrilansInntekt;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittOpptjeningDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OpptjeningAktiviteterDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OpptjeningPeriodeDto;
import no.nav.k9.sak.domene.arbeidsforhold.impl.SakInntektsmeldinger;
import no.nav.k9.sak.domene.iay.inntektsmelding.InntektsmeldingErNyereVurderer;
import no.nav.k9.sak.domene.iay.modell.AktørArbeid;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.k9.sak.domene.iay.modell.Inntekt;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektFilter;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.k9.sak.domene.iay.modell.Inntektspost;
import no.nav.k9.sak.domene.iay.modell.OppgittArbeidsforhold;
import no.nav.k9.sak.domene.iay.modell.OppgittEgenNæring;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilans;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.PeriodeAndel;
import no.nav.k9.sak.domene.iay.modell.RefusjonskravDato;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.Ytelse;
import no.nav.k9.sak.domene.iay.modell.YtelseAnvist;
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.domene.iay.modell.YtelseGrunnlag;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;

/**
 * Mapper fra k9-format til kalkulus-format, benytter kontrakt v1 fra kalkulus
 */
public class TilKalkulusMapper {

    public static final String KODEVERDI_UNDEFINED = "-";

    public static InntektArbeidYtelseGrunnlagDto mapTilDto(InntektArbeidYtelseGrunnlag grunnlag,
                                                           SakInntektsmeldinger sakInntektsmeldinger,
                                                           AktørId aktørId,
                                                           DatoIntervallEntitet vilkårsPeriode,
                                                           OppgittOpptjening oppgittOpptjening) {

        var skjæringstidspunktBeregning = vilkårsPeriode.getFomDato();
        var inntektFilter = new InntektFilter(grunnlag.getAktørInntektFraRegister(aktørId)).før(skjæringstidspunktBeregning);
        var ytelseFilter = new YtelseFilter(grunnlag.getAktørYtelseFraRegister(aktørId));
        Optional<AktørArbeid> arbeid = grunnlag.getAktørArbeidFraRegister(aktørId);

        var inntektsmeldinger = grunnlag.getInntektsmeldinger();
        var yrkesaktiviteterForBeregning = arbeid.map(AktørArbeid::hentAlleYrkesaktiviteter).orElse(Collections.emptyList());
        var alleRelevanteInntekter = finnRelevanteInntekter(inntektFilter);
        var inntektArbeidYtelseGrunnlagDto = new InntektArbeidYtelseGrunnlagDto();

        inntektArbeidYtelseGrunnlagDto.medArbeidDto(mapArbeidDto(yrkesaktiviteterForBeregning));
        inntektArbeidYtelseGrunnlagDto.medInntekterDto(mapInntektDto(alleRelevanteInntekter));
        inntektArbeidYtelseGrunnlagDto.medYtelserDto(mapYtelseDto(ytelseFilter.getAlleYtelser()));
        inntektArbeidYtelseGrunnlagDto.medInntektsmeldingerDto(mapTilDto(inntektsmeldinger, sakInntektsmeldinger, vilkårsPeriode));
        inntektArbeidYtelseGrunnlagDto.medArbeidsforholdInformasjonDto(mapTilArbeidsforholdInformasjonDto(grunnlag.getArbeidsforholdInformasjon()));
        inntektArbeidYtelseGrunnlagDto.medOppgittOpptjeningDto(mapTilOppgittOpptjeningDto(oppgittOpptjening));
        inntektArbeidYtelseGrunnlagDto.medArbeidsforholdInformasjonDto(mapTilArbeidsforholdInformasjonDto(grunnlag.getArbeidsforholdInformasjon()));

        return inntektArbeidYtelseGrunnlagDto;
    }

    private static List<Inntekt> finnRelevanteInntekter(InntektFilter inntektFilter) {
        return new ArrayList<>() {
            {
                addAll(inntektFilter.getAlleInntektSammenligningsgrunnlag());
                addAll(inntektFilter.getAlleInntektBeregningsgrunnlag());
            }
        };
    }

    private static ArbeidsforholdInformasjonDto mapTilArbeidsforholdInformasjonDto(Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjonOpt) {
        if (arbeidsforholdInformasjonOpt.isEmpty()) {
            return null;
        }
        ArbeidsforholdInformasjon arbeidsforholdInformasjon = arbeidsforholdInformasjonOpt.get();
        List<ArbeidsforholdOverstyringDto> resultat = arbeidsforholdInformasjon.getOverstyringer().stream()
            .map(arbeidsforholdOverstyring -> new ArbeidsforholdOverstyringDto(mapTilAktør(arbeidsforholdOverstyring.getArbeidsgiver()),
                new InternArbeidsforholdRefDto(arbeidsforholdOverstyring.getArbeidsforholdRef().getReferanse()),
                new ArbeidsforholdHandlingType(arbeidsforholdOverstyring.getHandling().getKode())))
            .collect(Collectors.toList());

        if (!resultat.isEmpty()) {
            return new ArbeidsforholdInformasjonDto(resultat);
        }
        return null;
    }

    private static OppgittOpptjeningDto mapTilOppgittOpptjeningDto(OppgittOpptjening oppgittOpptjening) {
        if (oppgittOpptjening != null) {
            return new OppgittOpptjeningDto(
                oppgittOpptjening.getFrilans().map(TilKalkulusMapper::mapOppgittFrilans).orElse(null),
                mapOppgittEgenNæringListe(oppgittOpptjening.getEgenNæring()),
                mapOppgittArbeidsforholdDto(oppgittOpptjening.getOppgittArbeidsforhold()));
        }
        return null;
    }

    private static List<OppgittEgenNæringDto> mapOppgittEgenNæringListe(List<OppgittEgenNæring> egenNæring) {
        return egenNæring == null ? null : egenNæring.stream().map(TilKalkulusMapper::mapOppgittEgenNæring).collect(Collectors.toList());
    }

    private static OppgittEgenNæringDto mapOppgittEgenNæring(OppgittEgenNæring oppgittEgenNæring) {
        return new OppgittEgenNæringDto(
            mapPeriode(oppgittEgenNæring.getPeriode()),
            oppgittEgenNæring.getOrgnr() == null ? null : new Organisasjon(oppgittEgenNæring.getOrgnr()),
            new VirksomhetType(oppgittEgenNæring.getVirksomhetType().getKode()),
            oppgittEgenNæring.getNyoppstartet(),
            oppgittEgenNæring.getVarigEndring(),
            oppgittEgenNæring.getEndringDato(),
            oppgittEgenNæring.getNærRelasjon(),
            oppgittEgenNæring.getNyIArbeidslivet(),
            oppgittEgenNæring.getBruttoInntekt());
    }

    private static List<OppgittArbeidsforholdDto> mapOppgittArbeidsforholdDto(List<OppgittArbeidsforhold> arbeidsforhold) {
        if (arbeidsforhold == null) {
            return null;
        }
        return arbeidsforhold.stream().map(arb -> new OppgittArbeidsforholdDto(mapPeriode(arb.getPeriode()), arb.getInntekt())).collect(Collectors.toList());
    }

    private static OppgittFrilansDto mapOppgittFrilans(OppgittFrilans oppgittFrilans) {
        List<OppgittFrilansInntekt> oppdrag = oppgittFrilans.getFrilansoppdrag()
            .stream()
            .map(frilansoppdrag -> new OppgittFrilansInntekt(mapPeriode(frilansoppdrag.getPeriode()), frilansoppdrag.getInntekt()))
            .collect(Collectors.toList());

        return new OppgittFrilansDto(oppgittFrilans.getErNyoppstartet() == null ? false : oppgittFrilans.getErNyoppstartet(), oppdrag);
    }

    private static InntektsmeldingerDto mapTilDto(Optional<InntektsmeldingAggregat> inntektsmeldingerOpt, SakInntektsmeldinger sakInntektsmeldinger, DatoIntervallEntitet vilkårsPeriode) {
        if (inntektsmeldingerOpt.isEmpty()) {
            return null;
        }

        // TODO: Skal vi ta hensyn til endringer i refusjonskrav så må dette konstrueres fra alle inntektsmeldingene som overlapper med perioden
        // Da denne informasjonen ikke er periodisert for IM for OMP så må det mappes fra inntektsmeldingene i kronologisk rekkefølge
        List<Inntektsmelding> inntektsmeldingerForPerioden = utledInntektsmeldingerSomGjelderForPeriode(sakInntektsmeldinger, vilkårsPeriode);

        List<InntektsmeldingDto> inntektsmeldingDtoer = inntektsmeldingerForPerioden.stream().map(inntektsmelding -> {
            Aktør aktør = mapTilAktør(inntektsmelding.getArbeidsgiver());
            var beløpDto = new BeløpDto(inntektsmelding.getInntektBeløp().getVerdi());
            var naturalYtelseDtos = inntektsmelding.getNaturalYtelser().stream().map(naturalYtelse -> new NaturalYtelseDto(
                mapPeriode(naturalYtelse.getPeriode()),
                new BeløpDto(naturalYtelse.getBeloepPerMnd().getVerdi()),
                new NaturalYtelseType(naturalYtelse.getType().getKode()))).collect(Collectors.toList());

            var refusjonDtos = inntektsmelding.getEndringerRefusjon().stream().map(refusjon -> new RefusjonDto(
                new BeløpDto(refusjon.getRefusjonsbeløp().getVerdi()),
                refusjon.getFom())).collect(Collectors.toList());

            var internArbeidsforholdRefDto = inntektsmelding.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold()
                ? new InternArbeidsforholdRefDto(inntektsmelding.getArbeidsforholdRef().getReferanse())
                : null;
            var startDato = inntektsmelding.getStartDatoPermisjon().isPresent() ? inntektsmelding.getStartDatoPermisjon().get() : null;
            var refusjon = inntektsmelding.getRefusjonOpphører();
            var beløpDto1 = inntektsmelding.getRefusjonBeløpPerMnd() != null ? new BeløpDto(inntektsmelding.getRefusjonBeløpPerMnd().getVerdi()) : null;

            return new InntektsmeldingDto(aktør, beløpDto, naturalYtelseDtos, refusjonDtos, internArbeidsforholdRefDto, startDato, refusjon, beløpDto1);
        }).collect(Collectors.toList());

        return inntektsmeldingDtoer.isEmpty() ? null : new InntektsmeldingerDto(inntektsmeldingDtoer);
    }

    static List<Inntektsmelding> utledInntektsmeldingerSomGjelderForPeriode(SakInntektsmeldinger sakInntektsmeldinger, DatoIntervallEntitet vilkårsPeriode) {
        var inntektsmeldingene = new ArrayList<Inntektsmelding>();

        var alleInntektsmeldinger = hentInntektsmeldingerSomGjelderForVilkårsperiode(sakInntektsmeldinger, vilkårsPeriode);
        for (Inntektsmelding inntektsmelding : alleInntektsmeldinger) {
            if (harIngenInntektsmeldingerForArbeidsforholdIdentifikatoren(inntektsmeldingene, inntektsmelding)) {
                inntektsmeldingene.add(inntektsmelding);
            } else if (harInntektsmeldingSomMatcherArbeidsforhold(inntektsmeldingene, inntektsmelding)
                && skalErstatteEksisterendeInntektsmelding(inntektsmelding, inntektsmeldingene, vilkårsPeriode)) {
                inntektsmeldingene.removeIf(arbeidsforholdMatcher(inntektsmelding));
                inntektsmeldingene.add(inntektsmelding);
            }
        }

        return inntektsmeldingene;
    }

    private static boolean skalErstatteEksisterendeInntektsmelding(Inntektsmelding inntektsmelding, List<Inntektsmelding> inntektsmeldingene, DatoIntervallEntitet vilkårsPeriode) {
        var datoNærmestSkjæringstidspunktet = finnDatoNærmestSkjæringstidspunktet(inntektsmelding, vilkårsPeriode.getFomDato());
        if (datoNærmestSkjæringstidspunktet.isEmpty()) {
            return false;
        }
        var inntektsmeldingerSomErNærmereEllerNyere = inntektsmeldingene.stream()
            .filter(arbeidsforholdMatcher(inntektsmelding))
            .filter(it -> erNærmereEllerLikeNæreSkjæringtidspunktet(it, datoNærmestSkjæringstidspunktet.get(), vilkårsPeriode.getFomDato())
                && InntektsmeldingErNyereVurderer.erNyere(it, inntektsmelding))
            .collect(Collectors.toList());

        return !inntektsmeldingerSomErNærmereEllerNyere.isEmpty();
    }

    private static Optional<LocalDate> finnDatoNærmestSkjæringstidspunktet(Inntektsmelding inntektsmelding, LocalDate skjæringstidspunkt) {
        return inntektsmelding.getOppgittFravær().stream().map(PeriodeAndel::getFom).min(Comparator.comparingLong(x -> ChronoUnit.DAYS.between(x, skjæringstidspunkt)));
    }

    private static boolean erNærmereEllerLikeNæreSkjæringtidspunktet(Inntektsmelding gammel, LocalDate nyInntektsmeldingDatoNærmestStp, LocalDate skjæringstidspunkt) {
        var næresteDatoFraEksisterende = finnDatoNærmestSkjæringstidspunktet(gammel, skjæringstidspunkt).orElseThrow();
        long distGammel = Math.abs(ChronoUnit.DAYS.between(skjæringstidspunkt, næresteDatoFraEksisterende));
        long distNy = Math.abs(ChronoUnit.DAYS.between(skjæringstidspunkt, nyInntektsmeldingDatoNærmestStp));
        return distNy <= distGammel;
    }

    private static boolean harInntektsmeldingSomMatcherArbeidsforhold(List<Inntektsmelding> inntektsmeldingene, Inntektsmelding inntektsmelding) {
        return inntektsmeldingene.stream().anyMatch(arbeidsforholdMatcher(inntektsmelding));
    }

    private static boolean harIngenInntektsmeldingerForArbeidsforholdIdentifikatoren(List<Inntektsmelding> inntektsmeldingene, Inntektsmelding inntektsmelding) {
        return inntektsmeldingene.stream().noneMatch(arbeidsforholdMatcher(inntektsmelding));
    }

    private static List<Inntektsmelding> hentInntektsmeldingerSomGjelderForVilkårsperiode(SakInntektsmeldinger sakInntektsmeldinger, DatoIntervallEntitet vilkårsPeriode) {
        Comparator<Inntektsmelding> comp = Comparator.comparing(Inntektsmelding::getInnsendingstidspunkt)
            .thenComparing(Comparator.comparing(im -> im.getJournalpostId().getVerdi(), Comparator.nullsFirst(Comparator.naturalOrder())));

        return sakInntektsmeldinger.getAlleInntektsmeldinger()
            .stream()
            .filter(it -> it.getOppgittFravær()
                .stream()
                .anyMatch(at -> vilkårsPeriode.overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(at.getFom(), at.getTom()))))
            .sorted(comp)
            .collect(Collectors.toList());
    }

    private static Predicate<Inntektsmelding> arbeidsforholdMatcher(Inntektsmelding inntektsmelding) {
        return it -> it.getArbeidsgiver().equals(inntektsmelding.getArbeidsgiver()) && it.getArbeidsforholdRef().gjelderFor(inntektsmelding.getArbeidsforholdRef());
    }

    private static Periode mapPeriode(DatoIntervallEntitet periode) {
        return new Periode(periode.getFomDato(), periode.getTomDato());
    }

    public static Aktør mapTilAktør(Arbeidsgiver arbeidsgiver) {
        return arbeidsgiver.getErVirksomhet() ? new Organisasjon(arbeidsgiver.getOrgnr()) : new AktørIdPersonident(arbeidsgiver.getAktørId().getId());
    }

    private static YtelserDto mapYtelseDto(List<Ytelse> alleYtelser) {
        List<YtelseDto> ytelserDto = alleYtelser.stream().map(ytelse -> new YtelseDto(
            mapBeløp(ytelse.getYtelseGrunnlag().flatMap(YtelseGrunnlag::getVedtaksDagsats)),
            mapYtelseAnvist(ytelse.getYtelseAnvist()),
            new RelatertYtelseType(ytelse.getYtelseType().getKode()),
            mapPeriode(ytelse.getPeriode()),
            mapTemaUnderkategori(ytelse)))
            .collect(Collectors.toList());

        if (!ytelserDto.isEmpty()) {
            return new YtelserDto(ytelserDto);
        }
        return null;
    }

    // TODO (OJR): Skal vi mappe dette slik eller tåler Kalkulus UNDEFINED("-")
    private static TemaUnderkategori mapTemaUnderkategori(Ytelse ytelse) {
        if (KODEVERDI_UNDEFINED.equals(ytelse.getBehandlingsTema().getKode())) {
            return null;
        }
        return new TemaUnderkategori(ytelse.getBehandlingsTema().getKode());
    }

    private static BeløpDto mapBeløp(Optional<Beløp> beløp) {
        return beløp.map(value -> new BeløpDto(value.getVerdi())).orElse(null);
    }

    private static Set<YtelseAnvistDto> mapYtelseAnvist(Collection<YtelseAnvist> ytelseAnvist) {
        return ytelseAnvist.stream().map(ya -> {
            BeløpDto beløpDto = mapBeløp(ya.getBeløp());
            BeløpDto dagsatsDto = mapBeløp(ya.getDagsats());
            BigDecimal bigDecimal = ya.getUtbetalingsgradProsent().isPresent() ? ya.getUtbetalingsgradProsent().get().getVerdi() : null;
            return new YtelseAnvistDto(new Periode(
                ya.getAnvistFOM(), ya.getAnvistTOM()),
                beløpDto,
                dagsatsDto,
                bigDecimal);
        }).collect(Collectors.toSet());
    }

    private static InntekterDto mapInntektDto(List<Inntekt> alleInntektBeregningsgrunnlag) {
        List<UtbetalingDto> utbetalingDtoer = alleInntektBeregningsgrunnlag.stream().map(TilKalkulusMapper::mapTilDto).collect(Collectors.toList());
        if (!utbetalingDtoer.isEmpty()) {
            return new InntekterDto(utbetalingDtoer);
        }
        return null;
    }

    private static UtbetalingDto mapTilDto(Inntekt inntekt) {
        UtbetalingDto utbetalingDto = new UtbetalingDto(new InntektskildeType(inntekt.getInntektsKilde().getKode()),
            inntekt.getAlleInntektsposter().stream().map(TilKalkulusMapper::mapTilDto).collect(Collectors.toList()));
        if (inntekt.getArbeidsgiver() != null) {
            return utbetalingDto.medArbeidsgiver(mapTilAktør(inntekt.getArbeidsgiver()));
        }
        return utbetalingDto;
    }

    private static UtbetalingsPostDto mapTilDto(Inntektspost inntektspost) {
        return new UtbetalingsPostDto(
            mapPeriode(inntektspost.getPeriode()),
            new InntektspostType(inntektspost.getInntektspostType().getKode()),
            inntektspost.getBeløp().getVerdi());
    }

    private static ArbeidDto mapArbeidDto(Collection<Yrkesaktivitet> yrkesaktiviteterForBeregning) {
        List<YrkesaktivitetDto> yrkesaktivitetDtoer = yrkesaktiviteterForBeregning.stream().map(TilKalkulusMapper::mapTilDto).collect(Collectors.toList());
        if (!yrkesaktivitetDtoer.isEmpty()) {
            return new ArbeidDto(yrkesaktivitetDtoer);
        }
        return null;
    }

    private static YrkesaktivitetDto mapTilDto(Yrkesaktivitet yrkesaktivitet) {
        List<AktivitetsAvtaleDto> aktivitetsAvtaleDtos = yrkesaktivitet.getAlleAktivitetsAvtaler().stream().map(aktivitetsAvtale -> new AktivitetsAvtaleDto(mapPeriode(aktivitetsAvtale.getPeriode()),
            aktivitetsAvtale.getSisteLønnsendringsdato(),
            aktivitetsAvtale.getProsentsats() != null ? aktivitetsAvtale.getProsentsats().getVerdi() : null)

        ).collect(Collectors.toList());

        String arbeidsforholdRef = yrkesaktivitet.getArbeidsforholdRef().getReferanse();
        return new YrkesaktivitetDto(
            mapTilAktør(yrkesaktivitet.getArbeidsgiver()),
            arbeidsforholdRef != null ? new InternArbeidsforholdRefDto(arbeidsforholdRef) : null,
            new ArbeidType(yrkesaktivitet.getArbeidType().getKode()),
            aktivitetsAvtaleDtos);
    }

    public static OpptjeningAktiviteterDto mapTilDto(OpptjeningAktiviteter opptjeningAktiviteter) {
        return new OpptjeningAktiviteterDto(opptjeningAktiviteter.getOpptjeningPerioder().stream().map(opptjeningPeriode -> new OpptjeningPeriodeDto(
            new OpptjeningAktivitetType(opptjeningPeriode.getOpptjeningAktivitetType().getKode()),
            new Periode(opptjeningPeriode.getPeriode().getFom(), opptjeningPeriode.getPeriode().getTom()),
            mapTilDto(opptjeningPeriode),
            opptjeningPeriode.getArbeidsforholdId() != null && opptjeningPeriode.getArbeidsforholdId().getReferanse() != null
                ? new InternArbeidsforholdRefDto(opptjeningPeriode.getArbeidsforholdId().getReferanse())
                : null))
            .collect(Collectors.toList()));
    }

    public static List<RefusjonskravDatoDto> mapTilDto(List<RefusjonskravDato> refusjonskravDatoes) {
        return refusjonskravDatoes.stream().map(refusjonskravDato -> new RefusjonskravDatoDto(mapTilAktør(
            refusjonskravDato.getArbeidsgiver()),
            refusjonskravDato.getFørsteDagMedRefusjonskrav(),
            refusjonskravDato.getFørsteInnsendingAvRefusjonskrav(),
            refusjonskravDato.getHarRefusjonFraStart())).collect(Collectors.toList());
    }

    private static Aktør mapTilDto(OpptjeningPeriode periode) {
        var orgNummer = periode.getArbeidsgiverOrgNummer() != null ? new Organisasjon(periode.getArbeidsgiverOrgNummer()) : null;
        if (orgNummer != null) {
            return orgNummer;
        }
        return periode.getArbeidsgiverAktørId() != null ? new AktørIdPersonident(periode.getArbeidsgiverAktørId()) : null;
    }

    public static List<GrunnbeløpDto> mapGrunnbeløp(List<Grunnbeløp> mapGrunnbeløpSatser) {
        return mapGrunnbeløpSatser.stream().map(grunnbeløp -> new GrunnbeløpDto(
            new Periode(grunnbeløp.getFom(), grunnbeløp.getTom()),
            BigDecimal.valueOf(grunnbeløp.getGSnitt()),
            BigDecimal.valueOf(grunnbeløp.getGVerdi())))
            .collect(Collectors.toList());
    }
}
