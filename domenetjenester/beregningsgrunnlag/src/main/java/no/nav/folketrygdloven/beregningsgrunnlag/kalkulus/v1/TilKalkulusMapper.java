package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.FinnPGITilgjengeligPåVedtakstidspunktet;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingerRelevantForBeregning;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningAktiviteter.OpptjeningPeriode;
import no.nav.folketrygdloven.kalkulus.felles.v1.Aktør;
import no.nav.folketrygdloven.kalkulus.felles.v1.AktørIdPersonident;
import no.nav.folketrygdloven.kalkulus.felles.v1.InternArbeidsforholdRefDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.JournalpostId;
import no.nav.folketrygdloven.kalkulus.felles.v1.Organisasjon;
import no.nav.folketrygdloven.kalkulus.felles.v1.Periode;
import no.nav.folketrygdloven.kalkulus.iay.IayProsent;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.AktivitetsAvtaleDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidsforholdInformasjonDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.ArbeidsforholdOverstyringDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.PermisjonDto;
import no.nav.folketrygdloven.kalkulus.iay.arbeid.v1.YrkesaktivitetDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.InntekterDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.InntektsmeldingDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.InntektsmeldingerDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.NaturalYtelseDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.RefusjonDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.UtbetalingDto;
import no.nav.folketrygdloven.kalkulus.iay.inntekt.v1.UtbetalingsPostDto;
import no.nav.folketrygdloven.kalkulus.iay.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.AnvistAndel;
import no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelseAnvistDto;
import no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelseDto;
import no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelseFordelingDto;
import no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelseGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.iay.ytelse.v1.YtelserDto;
import no.nav.folketrygdloven.kalkulus.kodeverk.ArbeidType;
import no.nav.folketrygdloven.kalkulus.kodeverk.ArbeidsforholdHandlingType;
import no.nav.folketrygdloven.kalkulus.kodeverk.Arbeidskategori;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektPeriodeType;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektYtelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.Inntektskategori;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektskildeType;
import no.nav.folketrygdloven.kalkulus.kodeverk.InntektspostType;
import no.nav.folketrygdloven.kalkulus.kodeverk.LønnsinntektBeskrivelse;
import no.nav.folketrygdloven.kalkulus.kodeverk.MidlertidigInaktivType;
import no.nav.folketrygdloven.kalkulus.kodeverk.NaturalYtelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.OpptjeningAktivitetType;
import no.nav.folketrygdloven.kalkulus.kodeverk.PermisjonsbeskrivelseType;
import no.nav.folketrygdloven.kalkulus.kodeverk.VirksomhetType;
import no.nav.folketrygdloven.kalkulus.kodeverk.YtelseType;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittArbeidsforholdDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittEgenNæringDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittFrilansDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittFrilansInntekt;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OppgittOpptjeningDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OpptjeningAktiviteterDto;
import no.nav.folketrygdloven.kalkulus.opptjening.v1.OpptjeningPeriodeDto;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.k9.sak.domene.iay.modell.Inntekt;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektFilter;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.Inntektspost;
import no.nav.k9.sak.domene.iay.modell.OppgittArbeidsforhold;
import no.nav.k9.sak.domene.iay.modell.OppgittEgenNæring;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilans;
import no.nav.k9.sak.domene.iay.modell.OppgittFrilansoppdrag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.Permisjon;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.domene.iay.modell.Ytelse;
import no.nav.k9.sak.domene.iay.modell.YtelseAnvist;
import no.nav.k9.sak.domene.iay.modell.YtelseAnvistAndel;
import no.nav.k9.sak.domene.iay.modell.YtelseFilter;
import no.nav.k9.sak.domene.iay.modell.YtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.YtelseStørrelse;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Mapper fra k9-format til kalkulus-format, benytter kontrakt v1 fra kalkulus
 */
public class TilKalkulusMapper {

    public static final String KODEVERDI_UNDEFINED = "-";

    private final InntektsmeldingerRelevantForBeregning imTjeneste;
    private final FinnPGITilgjengeligPåVedtakstidspunktet finnPGITilgjengeligPåVedtakstidspunktet;

    public TilKalkulusMapper(InntektsmeldingerRelevantForBeregning imTjeneste, FinnPGITilgjengeligPåVedtakstidspunktet sigrunFilter) {
        this.imTjeneste = imTjeneste;
        this.finnPGITilgjengeligPåVedtakstidspunktet = sigrunFilter;
    }


    public List<Inntekt> finnRelevanteInntekter(InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                BehandlingReferanse referanse,
                                                LocalDate skjæringstidspunktBeregning) {
        var inntektFilter = new InntektFilter(iayGrunnlag.getAktørInntektFraRegister(referanse.getAktørId())).før(skjæringstidspunktBeregning);
        return new ArrayList<>() {
            {
                addAll(inntektFilter.getAlleInntektSammenligningsgrunnlag());
                addAll(inntektFilter.getAlleInntektBeregningsgrunnlag());
                addAll(finnPGITilgjengeligPåVedtakstidspunktet.finnInntekter(referanse, iayGrunnlag, skjæringstidspunktBeregning));

            }
        };
    }

    public static ArbeidsforholdInformasjonDto mapTilArbeidsforholdInformasjonDto(ArbeidsforholdInformasjon arbeidsforholdInformasjon, Collection<Inntektsmelding> sakInntektsmeldinger) {
        List<ArbeidsforholdOverstyringDto> resultat = arbeidsforholdInformasjon.getOverstyringer().stream()
            .map(arbeidsforholdOverstyring -> new ArbeidsforholdOverstyringDto(mapTilAktør(arbeidsforholdOverstyring.getArbeidsgiver()),
                arbeidsforholdOverstyring.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold() ? new InternArbeidsforholdRefDto(arbeidsforholdOverstyring.getArbeidsforholdRef().getReferanse())
                    : null,
                utledHandling(arbeidsforholdOverstyring, sakInntektsmeldinger)))
            .collect(Collectors.toList());

        if (!resultat.isEmpty()) {
            return new ArbeidsforholdInformasjonDto(resultat);
        }
        return null;
    }

    private static ArbeidsforholdHandlingType utledHandling(ArbeidsforholdOverstyring arbeidsforholdOverstyring, Collection<Inntektsmelding> sakInntektsmeldinger) {
        var harMottattInntektsmelding = sakInntektsmeldinger.stream().anyMatch(im -> im.getArbeidsgiver().equals(arbeidsforholdOverstyring.getArbeidsgiver()) && im.getArbeidsforholdRef().gjelderFor(arbeidsforholdOverstyring.getArbeidsforholdRef()));
        var erLagtTilAvSaksbehandler = arbeidsforholdOverstyring.getHandling().equals(no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER);
        return erLagtTilAvSaksbehandler && harMottattInntektsmelding ?
            ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING :
            ArbeidsforholdHandlingType.fraKode(arbeidsforholdOverstyring.getHandling().getKode());
    }

    public static OppgittEgenNæringDto mapOppgittEgenNæring(OppgittEgenNæring oppgittEgenNæring) {
        return new OppgittEgenNæringDto(
            mapPeriode(oppgittEgenNæring.getPeriode()),
            oppgittEgenNæring.getOrgnr() == null ? null : new Organisasjon(oppgittEgenNæring.getOrgnr()),
            VirksomhetType.fraKode(oppgittEgenNæring.getVirksomhetType().getKode()),
            oppgittEgenNæring.getNyoppstartet(),
            oppgittEgenNæring.getVarigEndring(),
            oppgittEgenNæring.getEndringDato(),
            oppgittEgenNæring.getNyIArbeidslivet(),
            oppgittEgenNæring.getBegrunnelse(),
            no.nav.folketrygdloven.kalkulus.felles.v1.Beløp.fra(oppgittEgenNæring.getBruttoInntekt()));
    }

    public static OppgittArbeidsforholdDto mapArbeidsforhold(OppgittArbeidsforhold arb) {
        return new OppgittArbeidsforholdDto(mapPeriode(arb.getPeriode()), no.nav.folketrygdloven.kalkulus.felles.v1.Beløp.fra(arb.getInntekt()));
    }

    public static Function<OppgittFrilansoppdrag, OppgittFrilansInntekt> mapFrilansOppdrag() {
        return frilansoppdrag -> new OppgittFrilansInntekt(mapPeriode(frilansoppdrag.getPeriode()), no.nav.folketrygdloven.kalkulus.felles.v1.Beløp.fra(frilansoppdrag.getInntekt()));
    }

    private static InntektsmeldingerDto mapTilDto(InntektsmeldingerRelevantForBeregning imTjeneste,
                                                  Collection<Inntektsmelding> sakInntektsmeldinger,
                                                  DatoIntervallEntitet vilkårsPeriode,
                                                  BehandlingReferanse referanse) {
        // TODO: Skal vi ta hensyn til endringer i refusjonskrav så må dette konstrueres fra alle inntektsmeldingene som overlapper med perioden
        // Da denne informasjonen ikke er periodisert for IM for OMP så må det mappes fra inntektsmeldingene i kronologisk rekkefølge
        var inntektsmeldinger = imTjeneste.begrensSakInntektsmeldinger(referanse, sakInntektsmeldinger, vilkårsPeriode);
        List<Inntektsmelding> inntektsmeldingerForPerioden = imTjeneste.utledInntektsmeldingerSomGjelderForPeriode(inntektsmeldinger, vilkårsPeriode);

        List<InntektsmeldingDto> inntektsmeldingDtoer = inntektsmeldingerForPerioden.stream().map(inntektsmelding -> {
            Aktør aktør = mapTilAktør(inntektsmelding.getArbeidsgiver());
            var beløpDto = no.nav.folketrygdloven.kalkulus.felles.v1.Beløp.fra((inntektsmelding.getInntektBeløp().getVerdi()));
            var naturalYtelseDtos = inntektsmelding.getNaturalYtelser().stream().map(naturalYtelse -> new NaturalYtelseDto(
                mapPeriode(naturalYtelse.getPeriode()),
                no.nav.folketrygdloven.kalkulus.felles.v1.Beløp.fra(naturalYtelse.getBeloepPerMnd().getVerdi()),
                NaturalYtelseType.fraKode(naturalYtelse.getType().getKode()))).collect(Collectors.toList());

            var refusjonDtos = inntektsmelding.getEndringerRefusjon().stream().map(refusjon -> new RefusjonDto(
                no.nav.folketrygdloven.kalkulus.felles.v1.Beløp.fra(refusjon.getRefusjonsbeløp().getVerdi()),
                refusjon.getFom())).collect(Collectors.toList());

            var internArbeidsforholdRefDto = inntektsmelding.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold()
                ? new InternArbeidsforholdRefDto(inntektsmelding.getArbeidsforholdRef().getReferanse())
                : null;
            var startDato = inntektsmelding.getStartDatoPermisjon().isPresent() ? inntektsmelding.getStartDatoPermisjon().get() : null;
            var refusjon = inntektsmelding.getRefusjonOpphører();
            var beløpDto1 = inntektsmelding.getRefusjonBeløpPerMnd() != null ? no.nav.folketrygdloven.kalkulus.felles.v1.Beløp.fra(inntektsmelding.getRefusjonBeløpPerMnd().getVerdi()) : null;

            var journalpostId = new JournalpostId(inntektsmelding.getJournalpostId().getJournalpostId().getVerdi());
            return new InntektsmeldingDto(aktør, beløpDto,
                naturalYtelseDtos,
                refusjonDtos,
                internArbeidsforholdRefDto,
                startDato,
                refusjon,
                beløpDto1,
                journalpostId);
        }).collect(Collectors.toList());

        return inntektsmeldingDtoer.isEmpty() ? null : new InntektsmeldingerDto(inntektsmeldingDtoer);
    }

    private static Periode mapPeriode(DatoIntervallEntitet periode) {
        return new Periode(periode.getFomDato(), periode.getTomDato());
    }

    public static Aktør mapTilAktør(Arbeidsgiver arbeidsgiver) {
        if (arbeidsgiver == null) {
            return null;
        }
        return arbeidsgiver.getErVirksomhet() ? new Organisasjon(arbeidsgiver.getOrgnr()) : new AktørIdPersonident(arbeidsgiver.getAktørId().getId());
    }

    public static YtelserDto mapYtelseDto(List<Ytelse> alleYtelser) {
        List<YtelseDto> ytelserDto = alleYtelser.stream().map(ytelse -> new YtelseDto(
                mapBeløp(ytelse.getYtelseGrunnlag().flatMap(YtelseGrunnlag::getVedtaksDagsats)),
                mapYtelseAnvist(ytelse.getYtelseAnvist()),
                mapYtelseType(ytelse.getYtelseType()),
                mapPeriode(ytelse.getPeriode()),
                mapYtelseGrunnlag(ytelse.getYtelseGrunnlag())))
            .collect(Collectors.toList());

        if (!ytelserDto.isEmpty()) {
            return new YtelserDto(ytelserDto);
        }
        return null;
    }

    private static YtelseType mapYtelseType(FagsakYtelseType fraYtelseType) {
        return switch (fraYtelseType) {
            case null -> null;
            case DAGPENGER -> YtelseType.DAGPENGER;
            case FRISINN -> YtelseType.FRISINN;
            case SYKEPENGER -> YtelseType.SYKEPENGER;
            case PLEIEPENGER_SYKT_BARN -> YtelseType.PLEIEPENGER_SYKT_BARN;
            case PLEIEPENGER_NÆRSTÅENDE -> YtelseType.PLEIEPENGER_NÆRSTÅENDE;
            case OMSORGSPENGER -> YtelseType.OMSORGSPENGER;
            case OMSORGSPENGER_KS -> YtelseType.OMSORGSPENGER;
            case OMSORGSPENGER_MA -> YtelseType.OMSORGSPENGER;
            case OMSORGSPENGER_AO -> YtelseType.OMSORGSPENGER;
            case OPPLÆRINGSPENGER -> YtelseType.OPPLÆRINGSPENGER;
            case ARBEIDSAVKLARINGSPENGER -> YtelseType.ARBEIDSAVKLARINGSPENGER;
            case ENGANGSTØNAD -> YtelseType.ENGANGSTØNAD;
            case FORELDREPENGER -> YtelseType.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> YtelseType.SVANGERSKAPSPENGER;
            case ENSLIG_FORSØRGER -> YtelseType.ENSLIG_FORSØRGER;
            case OBSOLETE, UDEFINERT -> YtelseType.UDEFINERT;
        };
    }

    private static YtelseGrunnlagDto mapYtelseGrunnlag(Optional<YtelseGrunnlag> ytelseGrunnlag) {
        return ytelseGrunnlag.map(yg -> new YtelseGrunnlagDto(Arbeidskategori.fraKode(yg.getArbeidskategori().map(no.nav.k9.kodeverk.arbeidsforhold.Arbeidskategori::getKode).orElse(null)), mapYtelseFordeling(yg.getYtelseStørrelse()))).orElse(null);
    }

    private static List<YtelseFordelingDto> mapYtelseFordeling(List<YtelseStørrelse> ytelseStørrelse) {
        return ytelseStørrelse.stream()
            .map(ys -> new YtelseFordelingDto(mapVirksomhet(ys), mapInntektPeriodeType(ys), ys.getBeløp().getVerdi(), ys.getErRefusjon()))
            .collect(Collectors.toList());
    }

    private static InntektPeriodeType mapInntektPeriodeType(YtelseStørrelse ytelseStørrelse) {
        return switch (ytelseStørrelse.getHyppighet()) {
            case null -> null;
            case DAGLIG -> InntektPeriodeType.DAGLIG;
            case UKENTLIG -> InntektPeriodeType.UKENTLIG;
            case BIUKENTLIG -> InntektPeriodeType.BIUKENTLIG;
            case MÅNEDLIG -> InntektPeriodeType.MÅNEDLIG;
            case ÅRLIG -> InntektPeriodeType.ÅRLIG;
            case FASTSATT25PAVVIK -> InntektPeriodeType.FASTSATT25PAVVIK;
            case PREMIEGRUNNLAG -> InntektPeriodeType.PREMIEGRUNNLAG;
            case UDEFINERT -> InntektPeriodeType.UDEFINERT;
        };
    }

    private static Organisasjon mapVirksomhet(YtelseStørrelse ys) {
        return ys.getVirksomhet().map(orgNummer -> new Organisasjon(orgNummer.getOrgNummer())).orElse(null);
    }

    private static no.nav.folketrygdloven.kalkulus.felles.v1.Beløp mapBeløp(Optional<Beløp> beløp) {
        return beløp.map(value -> no.nav.folketrygdloven.kalkulus.felles.v1.Beløp.fra((value.getVerdi()))).orElse(null);
    }

    private static Set<YtelseAnvistDto> mapYtelseAnvist(Collection<YtelseAnvist> ytelseAnvist) {
        return ytelseAnvist.stream().map(ya -> {
            var beløpDto = mapBeløp(ya.getBeløp());
            var dagsatsDto = mapBeløp(ya.getDagsats());
            var utbetalingsgrad = ya.getUtbetalingsgradProsent().isPresent() ? IayProsent.fra(ya.getUtbetalingsgradProsent().get().getVerdi()) : null;
            return new YtelseAnvistDto(new Periode(
                ya.getAnvistFOM(), ya.getAnvistTOM()),
                beløpDto,
                dagsatsDto,
                utbetalingsgrad,
                mapAndeler(ya.getYtelseAnvistAndeler()));
        }).collect(Collectors.toSet());
    }

    private static List<AnvistAndel> mapAndeler(Set<YtelseAnvistAndel> ytelseAnvistAndeler) {
        return ytelseAnvistAndeler == null ? null : ytelseAnvistAndeler.stream()
            .map(TilKalkulusMapper::mapAndel)
            .toList();
    }

    private static AnvistAndel mapAndel(YtelseAnvistAndel a) {
        return new AnvistAndel(
            a.getArbeidsgiver().map(TilKalkulusMapper::mapTilAktør).orElse(null),
            a.getArbeidsforholdRef().getReferanse() == null ? null : new InternArbeidsforholdRefDto(a.getArbeidsforholdRef().getReferanse()),
            a.getDagsats() == null ? null : no.nav.folketrygdloven.kalkulus.felles.v1.Beløp.fra(a.getDagsats().getVerdi()),
            a.getUtbetalingsgradProsent() == null ? null : IayProsent.fra(a.getUtbetalingsgradProsent().getVerdi()),
            a.getRefusjonsgradProsent() == null ? null : IayProsent.fra(a.getRefusjonsgradProsent().getVerdi()),
            a.getInntektskategori() == null ?
                Inntektskategori.UDEFINERT : Inntektskategori.fraKode(a.getInntektskategori().getKode())
        );
    }

    public static InntekterDto mapInntektDto(List<Inntekt> alleInntektBeregningsgrunnlag) {

        List<UtbetalingDto> utbetalingDtoer = alleInntektBeregningsgrunnlag.stream().map(TilKalkulusMapper::mapTilDto).collect(Collectors.toList());
        if (!utbetalingDtoer.isEmpty()) {
            return new InntekterDto(utbetalingDtoer);
        }
        return null;
    }

    private static UtbetalingDto mapTilDto(Inntekt inntekt) {
        UtbetalingDto utbetalingDto = new UtbetalingDto(InntektskildeType.fraKode(inntekt.getInntektsKilde().getKode()),
            inntekt.getAlleInntektsposter().stream().map(TilKalkulusMapper::mapTilDto).collect(Collectors.toList()));
        if (inntekt.getArbeidsgiver() != null) {
            return utbetalingDto.medArbeidsgiver(mapTilAktør(inntekt.getArbeidsgiver()));
        }
        return utbetalingDto;
    }

    private static UtbetalingsPostDto mapTilDto(Inntektspost inntektspost) {
        var utbetalingsPostDto = new UtbetalingsPostDto(
            mapPeriode(inntektspost.getPeriode()),
            InntektspostType.fraKode(inntektspost.getInntektspostType().getKode()),
            no.nav.folketrygdloven.kalkulus.felles.v1.Beløp.fra(inntektspost.getBeløp().getVerdi()));
        if (inntektspost.getInntektYtelseType() != null) {
            var ytelseType = inntektspost.getInntektYtelseType();
            utbetalingsPostDto.setInntektYtelseType(InntektYtelseType.valueOf(ytelseType.name()));
        }
        if (inntektspost.getLønnsinntektBeskrivelse() != null) {
            utbetalingsPostDto.setLønnsinntektBeskrivelse(mapLønnsinntektBeskrivelse(inntektspost));
        }
        return utbetalingsPostDto;
    }

    private static LønnsinntektBeskrivelse mapLønnsinntektBeskrivelse(Inntektspost inntektspost) {
        return switch (inntektspost.getLønnsinntektBeskrivelse()) {
            case KOMMUNAL_OMSORGSLOENN_OG_FOSTERHJEMSGODTGJOERELSE -> LønnsinntektBeskrivelse.KOMMUNAL_OMSORGSLOENN_OG_FOSTERHJEMSGODTGJOERELSE;
            case UDEFINERT -> LønnsinntektBeskrivelse.UDEFINERT;
        };
    }

    public static ArbeidDto mapArbeidDto(Collection<Yrkesaktivitet> yrkesaktiviteterForBeregning, DatoIntervallEntitet vilkårsPeriode) {
        List<YrkesaktivitetDto> yrkesaktivitetDtoer = yrkesaktiviteterForBeregning.stream().map(ya -> TilKalkulusMapper.mapTilDto(ya, vilkårsPeriode)).collect(Collectors.toList());
        if (!yrkesaktivitetDtoer.isEmpty()) {
            return new ArbeidDto(yrkesaktivitetDtoer);
        }
        return null;
    }

    private static YrkesaktivitetDto mapTilDto(Yrkesaktivitet yrkesaktivitet, DatoIntervallEntitet vilkårsPeriode) {
        List<AktivitetsAvtaleDto> aktivitetsAvtaleDtos = yrkesaktivitet.getAlleAktivitetsAvtaler().stream().map(aktivitetsAvtale -> new AktivitetsAvtaleDto(mapPeriode(aktivitetsAvtale.getPeriode()),
            aktivitetsAvtale.getSisteLønnsendringsdato(),
            aktivitetsAvtale.getProsentsats() != null ? IayProsent.fra(aktivitetsAvtale.getProsentsats().getVerdi()) : null)
        ).collect(Collectors.toList());

        String arbeidsforholdRef = yrkesaktivitet.getArbeidsforholdRef().getReferanse();
        List<PermisjonDto> permisjoner = yrkesaktivitet.getPermisjon().stream()
            .filter(p -> !gjelderSøktYtelse(p, vilkårsPeriode))
            .map(TilKalkulusMapper::mapTilPermisjonDto)
            .collect(Collectors.toList());
        return new YrkesaktivitetDto(
            mapTilAktør(yrkesaktivitet.getArbeidsgiver()),
            arbeidsforholdRef != null ? new InternArbeidsforholdRefDto(arbeidsforholdRef) : null,
            ArbeidType.fraKode(yrkesaktivitet.getArbeidType().getKode()),
            aktivitetsAvtaleDtos,
            permisjoner);
    }

    private static boolean gjelderSøktYtelse(Permisjon p, DatoIntervallEntitet vilkårsPeriode) {
        return no.nav.k9.kodeverk.arbeidsforhold.PermisjonsbeskrivelseType.K9_VELFERDSPERMISJON.contains(p.getPermisjonsbeskrivelseType()) &&
            p.getProsentsats().getVerdi().compareTo(BigDecimal.valueOf(100)) >= 0 && p.getPeriode().overlapper(vilkårsPeriode);
    }

    public static OpptjeningAktiviteterDto mapTilDto(Optional<OpptjeningAktiviteter> opptjeningAktiviteter, VilkårUtfallMerknad vilkårsMerknad) {
        return new OpptjeningAktiviteterDto(
            opptjeningAktiviteter.map(TilKalkulusMapper::mapTilKalkulusOpptjeningPerioder).orElse(Collections.emptyList())
            , finnMidlertidigInaktivType(vilkårsMerknad));
    }

    private static List<OpptjeningPeriodeDto> mapTilKalkulusOpptjeningPerioder(OpptjeningAktiviteter opptjeningAktiviteter) {
        return opptjeningAktiviteter.getOpptjeningPerioder()
            .stream()
            .map(opptjeningPeriode -> new OpptjeningPeriodeDto(
                OpptjeningAktivitetType.fraKode(opptjeningPeriode.getOpptjeningAktivitetType().getKode()),
                new Periode(opptjeningPeriode.getPeriode().getFom(), opptjeningPeriode.getPeriode().getTom()),
                mapTilDto(opptjeningPeriode),
                opptjeningPeriode.getArbeidsforholdId() != null && opptjeningPeriode.getArbeidsforholdId().getReferanse() != null
                    ? new InternArbeidsforholdRefDto(opptjeningPeriode.getArbeidsforholdId().getReferanse())
                    : null))
            .collect(Collectors.toList());
    }

    private static MidlertidigInaktivType finnMidlertidigInaktivType(VilkårUtfallMerknad vilkårsMerknad) {
        if (vilkårsMerknad == null) {
            return null;
        }
        return switch (vilkårsMerknad) {
            case VM_7847_A -> MidlertidigInaktivType.A;
            case VM_7847_B -> MidlertidigInaktivType.B;
            default -> null;
        };
    }

    private static PermisjonDto mapTilPermisjonDto(Permisjon permisjon) {
        return new PermisjonDto(
            new Periode(permisjon.getFraOgMed(), permisjon.getTilOgMed()),
            IayProsent.fra(permisjon.getProsentsats().getVerdi()),
            PermisjonsbeskrivelseType.fraKode(permisjon.getPermisjonsbeskrivelseType().getKode())
        );
    }

    private static Aktør mapTilDto(OpptjeningPeriode periode) {
        var orgNummer = periode.getArbeidsgiverOrgNummer() != null ? new Organisasjon(periode.getArbeidsgiverOrgNummer()) : null;
        if (orgNummer != null) {
            return orgNummer;
        }
        return periode.getArbeidsgiverAktørId() != null ? new AktørIdPersonident(periode.getArbeidsgiverAktørId()) : null;
    }

    public InntektArbeidYtelseGrunnlagDto mapTilDto(InntektArbeidYtelseGrunnlag grunnlag,
                                                    Collection<Inntektsmelding> sakInntektsmeldinger,
                                                    AktørId aktørId,
                                                    DatoIntervallEntitet vilkårsPeriode,
                                                    OppgittOpptjening oppgittOpptjening,
                                                    BehandlingReferanse referanse) {

        var skjæringstidspunktBeregning = vilkårsPeriode.getFomDato();
        var ytelseFilter = new YtelseFilter(grunnlag.getAktørYtelseFraRegister(aktørId));
        var yrkesaktivitetFilter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getAktørArbeidFraRegister(aktørId));

        var yrkesaktiviteterForBeregning = new ArrayList<>(yrkesaktivitetFilter.getYrkesaktiviteter());
        yrkesaktiviteterForBeregning.addAll(yrkesaktivitetFilter.getFrilansOppdrag());
        var alleRelevanteInntekter = finnRelevanteInntekter(grunnlag, referanse, skjæringstidspunktBeregning);
        var inntektArbeidYtelseGrunnlagDto = new InntektArbeidYtelseGrunnlagDto();

        inntektArbeidYtelseGrunnlagDto.medArbeidDto(mapArbeidDto(yrkesaktiviteterForBeregning, vilkårsPeriode));
        inntektArbeidYtelseGrunnlagDto.medInntekterDto(mapInntektDto(alleRelevanteInntekter));
        inntektArbeidYtelseGrunnlagDto.medYtelserDto(mapYtelseDto(ytelseFilter.getAlleYtelser()));
        inntektArbeidYtelseGrunnlagDto.medInntektsmeldingerDto(mapTilDto(imTjeneste, sakInntektsmeldinger, vilkårsPeriode, referanse));
        inntektArbeidYtelseGrunnlagDto.medArbeidsforholdInformasjonDto(grunnlag.getArbeidsforholdInformasjon().map(arbeidsforholdInformasjon ->
            mapTilArbeidsforholdInformasjonDto(arbeidsforholdInformasjon, sakInntektsmeldinger)).orElse(null));
        inntektArbeidYtelseGrunnlagDto.medOppgittOpptjeningDto(mapTilOppgittOpptjeningDto(oppgittOpptjening));

        return inntektArbeidYtelseGrunnlagDto;
    }

    public OppgittOpptjeningDto mapTilOppgittOpptjeningDto(OppgittOpptjening oppgittOpptjening) {
        if (oppgittOpptjening != null) {
            return new OppgittOpptjeningDto(
                oppgittOpptjening.getJournalpostId() != null ? new JournalpostId(oppgittOpptjening.getJournalpostId().getVerdi()) : null,
                oppgittOpptjening.getFrilans().map(this::mapOppgittFrilansOppdragListe).orElse(null),
                mapOppgittEgenNæringListe(oppgittOpptjening.getEgenNæring()),
                mapOppgittArbeidsforholdDto(oppgittOpptjening.getOppgittArbeidsforhold()));
        }
        return null;
    }

    public List<OppgittEgenNæringDto> mapOppgittEgenNæringListe(List<OppgittEgenNæring> egenNæring) {
        return egenNæring == null ? null : egenNæring.stream().map(TilKalkulusMapper::mapOppgittEgenNæring).collect(Collectors.toList());
    }

    private List<OppgittArbeidsforholdDto> mapOppgittArbeidsforholdDto(List<OppgittArbeidsforhold> arbeidsforhold) {
        if (arbeidsforhold == null) {
            return null;
        }
        return arbeidsforhold.stream().map(TilKalkulusMapper::mapArbeidsforhold).collect(Collectors.toList());
    }

    private OppgittFrilansDto mapOppgittFrilansOppdragListe(OppgittFrilans oppgittFrilans) {
        List<OppgittFrilansInntekt> oppdrag = oppgittFrilans.getFrilansoppdrag()
            .stream()
            .filter(o -> o.getInntekt() != null)
            .map(mapFrilansOppdrag())
            .collect(Collectors.toList());
        return new OppgittFrilansDto(Boolean.TRUE.equals(oppgittFrilans.getErNyoppstartet()), oppdrag);
    }
}
