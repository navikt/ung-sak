package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittEgenNæringDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittFrilansDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittOpptjeningDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OpptjeningAktiviteterDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OpptjeningPeriodeDto;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.k9.sak.domene.iay.modell.Inntekt;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektFilter;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.k9.sak.domene.iay.modell.Inntektspost;
import no.nav.k9.sak.domene.iay.modell.OppgittEgenNæring;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilans;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.RefusjonskravDato;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
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

    public static InntektArbeidYtelseGrunnlagDto mapTilDto(InntektArbeidYtelseGrunnlag grunnlag, AktørId aktørId, LocalDate skjæringstidspunktBeregning) {
        var yrkesaktivitetFilter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId));
        var inntektFilter = new InntektFilter(grunnlag.getAktørInntektFraRegister(aktørId)).før(skjæringstidspunktBeregning);
        var ytelseFilter = new YtelseFilter(grunnlag.getAktørYtelseFraRegister(aktørId));


        var inntektsmeldinger = grunnlag.getInntektsmeldinger();
        var yrkesaktiviteterForBeregning = yrkesaktivitetFilter.getYrkesaktiviteterForBeregning();
        var alleInntektBeregningsgrunnlag = inntektFilter.getAlleInntektBeregningsgrunnlag();
        var inntektArbeidYtelseGrunnlagDto = new InntektArbeidYtelseGrunnlagDto();

        inntektArbeidYtelseGrunnlagDto.medArbeidDto(mapArbeidDto(yrkesaktiviteterForBeregning));
        inntektArbeidYtelseGrunnlagDto.medInntekterDto(mapInntektDto(alleInntektBeregningsgrunnlag));
        inntektArbeidYtelseGrunnlagDto.medYtelserDto(mapYtelseDto(ytelseFilter.getAlleYtelser()));
        inntektArbeidYtelseGrunnlagDto.medInntektsmeldingerDto(mapTilDto(inntektsmeldinger));
        inntektArbeidYtelseGrunnlagDto.medArbeidsforholdInformasjonDto(mapTilArbeidsforholdInformasjonDto(grunnlag.getArbeidsforholdInformasjon()));
        inntektArbeidYtelseGrunnlagDto.medOppgittOpptjeningDto(mapTilOppgittOpptjeingDto(grunnlag.getOppgittOpptjening()));
        inntektArbeidYtelseGrunnlagDto.medArbeidsforholdInformasjonDto(mapTilArbeidsforholdInformasjonDto(grunnlag.getArbeidsforholdInformasjon()));

        return inntektArbeidYtelseGrunnlagDto;
    }

    private static ArbeidsforholdInformasjonDto mapTilArbeidsforholdInformasjonDto(Optional<ArbeidsforholdInformasjon> arbeidsforholdInformasjonOpt) {
        if (arbeidsforholdInformasjonOpt.isEmpty()) {
            return null;
        }
        ArbeidsforholdInformasjon arbeidsforholdInformasjon = arbeidsforholdInformasjonOpt.get();
        List<ArbeidsforholdOverstyringDto> resultat = arbeidsforholdInformasjon.getOverstyringer().stream().map(arbeidsforholdOverstyring -> new ArbeidsforholdOverstyringDto(mapTilAktør(arbeidsforholdOverstyring.getArbeidsgiver()),
            new InternArbeidsforholdRefDto(arbeidsforholdOverstyring.getArbeidsforholdRef().getReferanse()),
            new ArbeidsforholdHandlingType(arbeidsforholdOverstyring.getHandling().getKode()))).collect(Collectors.toList());

        if (!resultat.isEmpty()) {
            return new ArbeidsforholdInformasjonDto(resultat);
        }
        return null;
    }

    private static OppgittOpptjeningDto mapTilOppgittOpptjeingDto(Optional<OppgittOpptjening> oppgittOpptjening) {
        return oppgittOpptjening.map(oo -> new OppgittOpptjeningDto(
            oo.getFrilans().map(TilKalkulusMapper::mapOppgittFrilans).orElse(null),
            mapOppgittEgenNæringListe(oo.getEgenNæring())))
            .orElse(null);
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
            oppgittEgenNæring.getNærRelasjon(),
            oppgittEgenNæring.getNyIArbeidslivet(),
            oppgittEgenNæring.getBruttoInntekt()
        );
    }

    private static OppgittFrilansDto mapOppgittFrilans(OppgittFrilans oppgittFrilans) {
        return new OppgittFrilansDto(oppgittFrilans.getHarInntektFraFosterhjem(), oppgittFrilans.getErNyoppstartet(), oppgittFrilans.getHarNærRelasjon());
    }

    private static InntektsmeldingerDto mapTilDto(Optional<InntektsmeldingAggregat> inntektsmeldingerOpt) {
        if (inntektsmeldingerOpt.isEmpty()) {
            return null;
        }
        InntektsmeldingAggregat inntektsmeldingAggregat = inntektsmeldingerOpt.get();
        List<Inntektsmelding> inntektsmeldingerSomSkalBrukes = inntektsmeldingAggregat.getInntektsmeldingerSomSkalBrukes();

        List<InntektsmeldingDto> inntektsmeldingDtoer = inntektsmeldingerSomSkalBrukes.stream().map(inntektsmelding -> {
            Aktør aktør = mapTilAktør(inntektsmelding.getArbeidsgiver());
            var beløpDto = new BeløpDto(inntektsmelding.getInntektBeløp().getVerdi());
            var naturalYtelseDtos = inntektsmelding.getNaturalYtelser().stream().map(naturalYtelse -> new NaturalYtelseDto(
                mapPeriode(naturalYtelse.getPeriode()),
                new BeløpDto(naturalYtelse.getBeloepPerMnd().getVerdi()),
                new NaturalYtelseType(naturalYtelse.getType().getKode()))).collect(Collectors.toList());

            var refusjonDtos = inntektsmelding.getEndringerRefusjon().stream().map(refusjon -> new RefusjonDto(
                new BeløpDto(refusjon.getRefusjonsbeløp().getVerdi()),
                refusjon.getFom())).collect(Collectors.toList());

            var internArbeidsforholdRefDto = inntektsmelding.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold() ? new InternArbeidsforholdRefDto(inntektsmelding.getArbeidsforholdRef().getReferanse()) : null;
            var startDato = inntektsmelding.getStartDatoPermisjon().isPresent() ? inntektsmelding.getStartDatoPermisjon().get() : null;
            var refusjon = inntektsmelding.getRefusjonOpphører();
            var beløpDto1 = inntektsmelding.getRefusjonBeløpPerMnd() != null ? new BeløpDto(inntektsmelding.getRefusjonBeløpPerMnd().getVerdi()) : null;

            return new InntektsmeldingDto(aktør, beløpDto, naturalYtelseDtos, refusjonDtos, internArbeidsforholdRefDto, startDato, refusjon, beløpDto1);
        }).collect(Collectors.toList());

        return inntektsmeldingDtoer.isEmpty() ? null : new InntektsmeldingerDto(inntektsmeldingDtoer);
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
            new TemaUnderkategori(ytelse.getBehandlingsTema().getKode())))
            .collect(Collectors.toList());

        if (!ytelserDto.isEmpty()) {
            return new YtelserDto(ytelserDto);
        }
        return null;
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
            }
        ).collect(Collectors.toSet());
    }

    private static InntekterDto mapInntektDto(List<Inntekt> alleInntektBeregningsgrunnlag) {
        List<UtbetalingDto> utbetalingDtoer = alleInntektBeregningsgrunnlag.stream().map(TilKalkulusMapper::mapTilDto).collect(Collectors.toList());
        if (utbetalingDtoer.isEmpty()) {
            return new InntekterDto(utbetalingDtoer);
        }
        return null;
    }

    private static UtbetalingDto mapTilDto(Inntekt inntekt) {
        return new UtbetalingDto(new InntektskildeType(inntekt.getInntektsKilde().getKode()),
            inntekt.getAlleInntektsposter().stream().map(TilKalkulusMapper::mapTilDto).collect(Collectors.toList())
        );
    }

    private static UtbetalingsPostDto mapTilDto(Inntektspost inntektspost) {
        return new UtbetalingsPostDto(
            mapPeriode(inntektspost.getPeriode()),
            new InntektspostType(inntektspost.getInntektspostType().getKode()),
            inntektspost.getBeløp().getVerdi()
        );
    }

    private static ArbeidDto mapArbeidDto(Collection<Yrkesaktivitet> yrkesaktiviteterForBeregning) {
        List<YrkesaktivitetDto> yrkesaktivitetDtoer = yrkesaktiviteterForBeregning.stream().map(TilKalkulusMapper::mapTilDto).collect(Collectors.toList());
        if (!yrkesaktivitetDtoer.isEmpty()) {
            return new ArbeidDto(yrkesaktivitetDtoer);
        }
        return null;
    }

    private static YrkesaktivitetDto mapTilDto(Yrkesaktivitet yrkesaktivitet) {
        List<AktivitetsAvtaleDto> aktivitetsAvtaleDtos = yrkesaktivitet.getAlleAktivitetsAvtaler().stream().map(aktivitetsAvtale ->
            new AktivitetsAvtaleDto(mapPeriode(aktivitetsAvtale.getPeriode()),
                aktivitetsAvtale.getSisteLønnsendringsdato(),
                aktivitetsAvtale.getProsentsats() != null ? aktivitetsAvtale.getProsentsats().getVerdi() : null)

        ).collect(Collectors.toList());

        return new YrkesaktivitetDto(
            mapTilAktør(yrkesaktivitet.getArbeidsgiver()),
            yrkesaktivitet.getArbeidsforholdRef() != null ? new InternArbeidsforholdRefDto(yrkesaktivitet.getArbeidsforholdRef().getReferanse()) : null,
            new ArbeidType(yrkesaktivitet.getArbeidType().getKode()),
            aktivitetsAvtaleDtos,
            yrkesaktivitet.getNavnArbeidsgiverUtland()
        );
    }


    public static OpptjeningAktiviteterDto mapTilDto(OpptjeningAktiviteter opptjeningAktiviteter) {
        return new OpptjeningAktiviteterDto(opptjeningAktiviteter.getOpptjeningPerioder().stream().map(opptjeningPeriode -> new OpptjeningPeriodeDto(
            new OpptjeningAktivitetType(opptjeningPeriode.getOpptjeningAktivitetType().getKode()),
            new Periode(opptjeningPeriode.getPeriode().getFom(), opptjeningPeriode.getPeriode().getTom()),
            mapTilDto(opptjeningPeriode),
            opptjeningPeriode.getArbeidsforholdId() != null ? new InternArbeidsforholdRefDto(opptjeningPeriode.getArbeidsforholdId().getReferanse()) : null))
            .collect(Collectors.toList()));
    }

    public static List<RefusjonskravDatoDto> mapTilDto(List<RefusjonskravDato> refusjonskravDatoes) {
        return refusjonskravDatoes.stream().map(refusjonskravDato ->
            new RefusjonskravDatoDto(mapTilAktør(
                refusjonskravDato.getArbeidsgiver()),
                refusjonskravDato.getFørsteDagMedRefusjonskrav(),
                refusjonskravDato.getFørsteInnsendingAvRefusjonskrav(),
                refusjonskravDato.getHarRefusjonFraStart())
        ).collect(Collectors.toList());
    }

    private static Aktør mapTilDto(OpptjeningPeriode periode) {
        var orgNummer = periode.getArbeidsgiverOrgNummer() != null ? new Organisasjon(periode.getArbeidsgiverOrgNummer()) : null;
        if (orgNummer != null) {
            return orgNummer;
        }
        return periode.getArbeidsgiverAktørId() != null ? new AktørIdPersonident(periode.getArbeidsgiverAktørId()) : null;
    }

    public static List<GrunnbeløpDto> mapGrunnbeløp(List<Grunnbeløp> mapGrunnbeløpSatser) {
        return mapGrunnbeløpSatser.stream().map(grunnbeløp ->
            new GrunnbeløpDto(
                new Periode(grunnbeløp.getFom(), grunnbeløp.getTom()),
                BigDecimal.valueOf(grunnbeløp.getGSnitt()),
                BigDecimal.valueOf(grunnbeløp.getGVerdi())))
            .collect(Collectors.toList());
    }
}
