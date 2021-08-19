package no.nav.k9.sak.ytelse.pleiepengerbarn.beregnytelse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.k9.felles.util.Tuple;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.uttak.UtfallType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.k9.sak.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.kontrakt.beregningsresultat.BeregningsresultatDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.BeregningsresultatMedUtbetaltePeriodeDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.BeregningsresultatPeriodeAndelDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.BeregningsresultatPeriodeDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.UttakDto;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.OrgNummer;
import no.nav.k9.sak.ytelse.beregning.BeregningsresultatMapper;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utfall;
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

@FagsakYtelseTypeRef("PSB")
@ApplicationScoped
public class PSBBeregningsresultatMapper implements BeregningsresultatMapper {

    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private UttakTjeneste uttakKlient;

    PSBBeregningsresultatMapper() {
        // For inject
    }

    @Inject
    public PSBBeregningsresultatMapper(ArbeidsgiverTjeneste arbeidsgiverTjeneste,
                                       InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                       SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                       UttakTjeneste uttakKlient) {
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.uttakKlient = uttakKlient;
    }

    private static LocalDateSegment<UttaksperiodeInfo> toSegment(LukketPeriode periode, UttaksperiodeInfo value) {
        return new LocalDateSegment<>(periode.getFom(), periode.getTom(), value);
    }

    @Override
    public BeregningsresultatDto map(Behandling behandling,
                                     BehandlingBeregningsresultatEntitet beregningsresultatAggregat) {
        final Uttaksplan uttakResultat = uttakKlient.hentUttaksplan(behandling.getUuid(), false);
        var ref = BehandlingReferanse.fra(behandling);
        LocalDate opphørsdato = skjæringstidspunktTjeneste.getOpphørsdato(ref).orElse(null);
        return BeregningsresultatDto.build()
            .medOpphoersdato(opphørsdato)
            .medPerioder(lagPerioder(behandling.getId(), beregningsresultatAggregat.getBgBeregningsresultat(), uttakResultat))
            .medSkalHindreTilbaketrekk(beregningsresultatAggregat.skalHindreTilbaketrekk().orElse(null))
            .create();
    }

    @Override
    public BeregningsresultatMedUtbetaltePeriodeDto mapMedUtbetaltePerioder(Behandling behandling, BehandlingBeregningsresultatEntitet bresAggregat) {
        final Uttaksplan uttakResultat = uttakKlient.hentUttaksplan(behandling.getUuid(), false);
        var ref = BehandlingReferanse.fra(behandling);
        LocalDate opphørsdato = skjæringstidspunktTjeneste.getOpphørsdato(ref).orElse(null);
        return BeregningsresultatMedUtbetaltePeriodeDto.build()
            .medOpphoersdato(opphørsdato)
            .medPerioder(lagPerioder(behandling.getId(), bresAggregat.getBgBeregningsresultat(), uttakResultat))
            .medUtbetaltePerioder(lagPerioder(behandling.getId(), bresAggregat.getUtbetBeregningsresultat(), uttakResultat))
            .medSkalHindreTilbaketrekk(bresAggregat.skalHindreTilbaketrekk().orElse(null))
            .create();
    }

    public List<BeregningsresultatPeriodeDto> lagPerioder(long behandlingId, BeregningsresultatEntitet beregningsresultat, Uttaksplan uttaksplan) {
        if (beregningsresultat == null) {
            return List.of();
        }
        var iayGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId);
        var beregningsresultatPerioder = beregningsresultat.getBeregningsresultatPerioder();
        var andelTilSisteUtbetalingsdatoMap = finnSisteUtbetalingdatoForAlleAndeler(
            beregningsresultatPerioder);

        LocalDateTimeline<BeregningsresultatPeriode> brpTimline = beregningsresultat.getBeregningsresultatTimeline();

        var dtoer = brpTimline.toSegments().stream()
            .map(seg -> BeregningsresultatPeriodeDto.build(seg.getFom(), seg.getTom())
                .medDagsats(seg.getValue().getDagsats())
                .medAndeler(lagAndeler(seg.getValue(), andelTilSisteUtbetalingsdatoMap, iayGrunnlag))
                .create())
            .collect(Collectors.toList());
        var resultatTimeline = toTimeline(dtoer);

        leggTilUttak(uttaksplan, resultatTimeline);

        return resultatTimeline.toSegments().stream().map(LocalDateSegment::getValue).collect(Collectors.toList());
    }

    private void leggTilUttak(Uttaksplan uttaksplan, LocalDateTimeline<BeregningsresultatPeriodeDto> resultatTimeline) {
        LocalDateTimeline<UttaksperiodeInfo> uttTimeline = getTimeline(uttaksplan);
        resultatTimeline.combine(uttTimeline, this::kombinerMedUttak, JoinStyle.LEFT_JOIN);
    }

    private LocalDateSegment<Void> kombinerMedUttak(LocalDateInterval interval,
                                                    LocalDateSegment<BeregningsresultatPeriodeDto> brpDto,
                                                    LocalDateSegment<UttaksperiodeInfo> uttPeriode) {
        var ut = uttPeriode == null ? null : uttPeriode.getValue();
        brpDto.getValue().getAndeler().forEach(a -> {
            if (ut != null) {
                // setter uttak rett på andel 'a' - Ok her
                var periode = new no.nav.k9.sak.typer.Periode(interval.getFomDato(), interval.getTomDato());
                final UtfallType utfallType = mapToUtfallType(ut.getUtfall());
                a.setUttak(List.of(new UttakDto(periode, utfallType, BigDecimal.ZERO))); // default uttak, overskrives under

                ut.getUtbetalingsgrader()
                    .stream()
                    .filter(ug -> matcherAndel(a, ug.getArbeidsforhold()))
                    .forEach(ug -> a.setUttak(List.of(new UttakDto(periode, utfallType, ug.getUtbetalingsgrad()))));
            }
        });
        return null;
    }

    private boolean matcherAndel(BeregningsresultatPeriodeAndelDto andel, Arbeidsforhold arbeidsforhold) {
        if (andel.getArbeidsgiverOrgnr() != null) {
            return Objects.equals(andel.getArbeidsgiverOrgnr().getOrgNummer(), arbeidsforhold.getOrganisasjonsnummer())
                && Objects.equals(andel.getArbeidsforholdId(), arbeidsforhold.getArbeidsforholdId())
                && matcherAktitivetStatus(arbeidsforhold, andel);
        }
        if (andel.getAktørId() != null) {
            return Objects.equals(andel.getAktørId().getAktørId(), arbeidsforhold.getAktørId())
                && Objects.equals(andel.getArbeidsforholdId(), arbeidsforhold.getArbeidsforholdId())
                && matcherAktitivetStatus(arbeidsforhold, andel);
        }
        return matcherAktitivetStatus(arbeidsforhold, andel);
    }

    private boolean matcherAktitivetStatus(Arbeidsforhold it, BeregningsresultatPeriodeAndelDto brukersAndel) {
        UttakArbeidType uttakArbeidType = UttakArbeidType.fraKode(it.getType());
        return uttakArbeidType.matcher(brukersAndel.getAktivitetStatus());
    }

    private UtfallType mapToUtfallType(Utfall utfall) {
        return switch (utfall) {
            case OPPFYLT -> UtfallType.INNVILGET;
            case IKKE_OPPFYLT -> UtfallType.AVSLÅTT;
            default -> throw new IllegalArgumentException("Ukjent utfall: " + utfall);
        };
    }

    private LocalDateTimeline<BeregningsresultatPeriodeDto> toTimeline(List<BeregningsresultatPeriodeDto> resultatDtoListe) {
        var segments = resultatDtoListe.stream().map(v -> new LocalDateSegment<>(v.getFom(), v.getTom(), v)).collect(Collectors.toList());
        return new LocalDateTimeline<>(segments);
    }

    List<BeregningsresultatPeriodeAndelDto> lagAndeler(BeregningsresultatPeriode beregningsresultatPeriode,
                                                       Map<Tuple<AktivitetStatus, Optional<String>>, Optional<LocalDate>> andelTilSisteUtbetalingsdatoMap,
                                                       Optional<InntektArbeidYtelseGrunnlag> iayGrunnlag) {

        var beregningsresultatAndelList = beregningsresultatPeriode.getBeregningsresultatAndelList();

        // grupper alle andeler som har samme aktivitetstatus og arbeidsforholdId og legg dem i en tuple med hendholdsvis brukers og arbeidsgivers
        // andel
        var andelListe = genererAndelListe(beregningsresultatAndelList);
        return andelListe.stream()
            .map(andelPar -> {
                BeregningsresultatAndel brukersAndel = andelPar.getElement1();
                Optional<BeregningsresultatAndel> arbeidsgiversAndel = andelPar.getElement2();
                Optional<Arbeidsgiver> arbeidsgiver = brukersAndel.getArbeidsgiver();
                BeregningsresultatPeriodeAndelDto.Builder dtoBuilder = BeregningsresultatPeriodeAndelDto.build()
                    .medRefusjon(arbeidsgiversAndel.map(BeregningsresultatAndel::getDagsats).orElse(0))
                    .medTilSøker(brukersAndel.getDagsats())
                    .medUtbetalingsgrad(brukersAndel.getUtbetalingsgrad())
                    .medSisteUtbetalingsdato(andelTilSisteUtbetalingsdatoMap.getOrDefault(genererAndelKey(brukersAndel), Optional.empty()).orElse(null))
                    .medAktivitetstatus(brukersAndel.getAktivitetStatus())
                    .medInntektskategori(brukersAndel.getInntektskategori())
                    .medArbeidsforholdId(brukersAndel.getArbeidsforholdRef() != null
                        ? brukersAndel.getArbeidsforholdRef().getReferanse()
                        : null)
                    .medAktørId(arbeidsgiver.filter(Arbeidsgiver::erAktørId).map(Arbeidsgiver::getAktørId).orElse(null))
                    .medArbeidsforholdType(brukersAndel.getArbeidsforholdType())
                    .medStillingsprosent(brukersAndel.getStillingsprosent());
                var internArbeidsforholdId = brukersAndel.getArbeidsforholdRef() != null ? brukersAndel.getArbeidsforholdRef().getReferanse() : null;
                dtoBuilder.medArbeidsforholdId(internArbeidsforholdId);
                iayGrunnlag.ifPresent(iay -> iay.getArbeidsforholdInformasjon().ifPresent(arbeidsforholdInformasjon -> {
                    if (internArbeidsforholdId != null && arbeidsgiver.isPresent()) {
                        var eksternArbeidsforholdRef = arbeidsforholdInformasjon.finnEkstern(arbeidsgiver.get(), brukersAndel.getArbeidsforholdRef());
                        dtoBuilder.medEksternArbeidsforholdId(eksternArbeidsforholdRef.getReferanse());
                    }
                }));
                arbeidsgiver.ifPresent(arb -> settArbeidsgiverfelter(arb, dtoBuilder));
                return dtoBuilder
                    .create();
            })
            .collect(Collectors.toList());
    }

    private void settArbeidsgiverfelter(Arbeidsgiver arb, BeregningsresultatPeriodeAndelDto.Builder dtoBuilder) {
        ArbeidsgiverOpplysninger opplysninger = arbeidsgiverTjeneste.hent(arb);
        if (opplysninger != null) {
            dtoBuilder.medArbeidsgiverNavn(opplysninger.getNavn());
            if (!arb.erAktørId()) {
                dtoBuilder.medArbeidsgiverOrgnr(new OrgNummer(arb.getOrgnr()));
            }
        } else {
            throw new IllegalStateException("Finner ikke arbeidsgivers identifikator");
        }
    }

    private Map<Tuple<AktivitetStatus, Optional<String>>, Optional<LocalDate>> finnSisteUtbetalingdatoForAlleAndeler(List<BeregningsresultatPeriode> beregningsresultatPerioder) {
        Collector<BeregningsresultatAndel, ?, Optional<LocalDate>> maxTomDatoCollector = Collectors.mapping(
            andel -> andel.getBeregningsresultatPeriode().getBeregningsresultatPeriodeTom(),
            Collectors.maxBy(Comparator.naturalOrder()));
        return beregningsresultatPerioder.stream()
            .flatMap(brp -> brp.getBeregningsresultatAndelList().stream())
            .filter(andel -> andel.getDagsats() > 0)
            .collect(Collectors.groupingBy(this::genererAndelKey, maxTomDatoCollector));
    }

    private Tuple<AktivitetStatus, Optional<String>> genererAndelKey(BeregningsresultatAndel andel) {
        return new Tuple<>(andel.getAktivitetStatus(), finnSekundærIdentifikator(andel));
    }

    private List<Tuple<BeregningsresultatAndel, Optional<BeregningsresultatAndel>>> genererAndelListe(List<BeregningsresultatAndel> beregningsresultatAndelList) {
        Map<Tuple<AktivitetStatus, Optional<String>>, List<BeregningsresultatAndel>> collect = beregningsresultatAndelList.stream()
            .collect(Collectors.groupingBy(this::genererAndelKey));

        return collect.values().stream().map(andeler -> {
            BeregningsresultatAndel brukerAndel = andeler.stream()
                .filter(BeregningsresultatAndel::erBrukerMottaker)
                .reduce(this::slåSammenAndeler)
                .orElseThrow(() -> new IllegalStateException("Utvilkerfeil: Mangler andel for bruker, men skal alltid ha andel for bruker her."));

            Optional<BeregningsresultatAndel> arbeidsgiverAndel = andeler.stream()
                .filter(a -> !a.erBrukerMottaker())
                .reduce(this::slåSammenAndeler);

            return new Tuple<>(brukerAndel, arbeidsgiverAndel);
        })
            .collect(Collectors.toList());
    }

    private Optional<String> finnSekundærIdentifikator(BeregningsresultatAndel andel) {
        // Denne metoden finner sekundæridentifikator for andelen, etter aktivitetstatus.
        // Mulige identifikatorer i prioritert rekkefølge:
        // 1. arbeidsforholdId
        // 2. orgNr
        if (andel.getArbeidsforholdRef() != null && andel.getArbeidsforholdRef().getReferanse() != null) {
            return Optional.of(andel.getArbeidsforholdRef().getReferanse());
        } else
            return andel.getArbeidsgiver().map(Arbeidsgiver::getIdentifikator);
    }

    private BeregningsresultatAndel slåSammenAndeler(BeregningsresultatAndel a, BeregningsresultatAndel b) {
        InternArbeidsforholdRef førsteArbeidsforholdId = a.getArbeidsforholdRef();
        InternArbeidsforholdRef andreArbeidsforholdId = b.getArbeidsforholdRef();
        boolean harUlikeArbeidsforholdIder = false;
        if (førsteArbeidsforholdId != null && andreArbeidsforholdId != null) {
            harUlikeArbeidsforholdIder = !Objects.equals(førsteArbeidsforholdId.getReferanse(), andreArbeidsforholdId.getReferanse());
        }
        if (harUlikeArbeidsforholdIder
            || a.getUtbetalingsgrad().compareTo(b.getUtbetalingsgrad()) != 0
            || a.getStillingsprosent().compareTo(b.getStillingsprosent()) != 0
            || !a.getBeregningsresultatPeriode().equals(b.getBeregningsresultatPeriode())) {
            throw new IllegalStateException(
                "Utviklerfeil: Andeler som slås sammen skal ikke ha ulikt arbeidsforhold, periode, stillingsprosent eller utbetalingsgrad");
        }
        BeregningsresultatAndel ny = new BeregningsresultatAndel(a, a.getBeregningsresultatPeriode());
        BeregningsresultatAndel.builder(ny)
            .medDagsats(a.getDagsats() + b.getDagsats())
            .medDagsatsFraBg(a.getDagsatsFraBg() + b.getDagsatsFraBg());
        return ny;
    }

    private LocalDateTimeline<UttaksperiodeInfo> getTimeline(Uttaksplan uttaksplan) {
        return new LocalDateTimeline<>(uttaksplan.getPerioder().entrySet().stream().map(e -> toSegment(e.getKey(), e.getValue())).collect(Collectors.toList()));
    }

}
