package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsresultat;

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

import no.nav.folketrygdloven.beregningsgrunnlag.Kopimaskin;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverOpplysninger;
import no.nav.foreldrepenger.domene.arbeidsgiver.ArbeidsgiverTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.uttak.OpphørUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.uttaksplan.kontrakt.InnvilgetUttaksplanperiode;
import no.nav.foreldrepenger.domene.uttak.uttaksplan.kontrakt.UttakArbeidsforhold;
import no.nav.foreldrepenger.domene.uttak.uttaksplan.kontrakt.Uttaksplan;
import no.nav.foreldrepenger.domene.uttak.uttaksplan.kontrakt.Uttaksplanperiode;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.LocalDateTimeline.JoinStyle;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.uttak.UtfallType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.kontrakt.beregningsresultat.BeregningsresultatDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.BeregningsresultatPeriodeAndelDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.BeregningsresultatPeriodeDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.UttakDto;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.OrgNummer;
import no.nav.vedtak.util.Tuple;

@ApplicationScoped
public class BeregningsresultatMapper {

    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private OpphørUttakTjeneste opphørUttakTjeneste;

    BeregningsresultatMapper() {
        // For inject
    }

    @Inject
    public BeregningsresultatMapper(ArbeidsgiverTjeneste arbeidsgiverTjeneste,
                                    InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                    OpphørUttakTjeneste opphørUttakTjeneste) {
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.opphørUttakTjeneste = opphørUttakTjeneste;
    }

    public BeregningsresultatDto lagBeregningsresultatMedUttaksplan(
                                                                    Behandling behandling,
                                                                    BehandlingBeregningsresultatEntitet beregningsresultatAggregat,
                                                                    Optional<Uttaksplan> uttakResultat) {
        var ref = BehandlingReferanse.fra(behandling);
        LocalDate opphørsdato = opphørUttakTjeneste.getOpphørsdato(ref).orElse(null);
        return BeregningsresultatDto.build()
            .medOpphoersdato(opphørsdato)
            .medPerioder(lagPerioder(behandling.getId(), beregningsresultatAggregat.getBgBeregningsresultat(), uttakResultat))
            .medSkalHindreTilbaketrekk(beregningsresultatAggregat.skalHindreTilbaketrekk().orElse(null))
            .create();
    }

    public List<BeregningsresultatPeriodeDto> lagPerioder(long behandlingId, BeregningsresultatEntitet beregningsresultat,
                                                          Optional<Uttaksplan> uttaksplan) {
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

        if (uttaksplan.isPresent()) {
            leggTilUttak(uttaksplan, resultatTimeline);
        }

        return resultatTimeline.toSegments().stream().map(LocalDateSegment::getValue).collect(Collectors.toList());
    }

    private void leggTilUttak(Optional<Uttaksplan> uttaksplan, LocalDateTimeline<BeregningsresultatPeriodeDto> resultatTimeline) {
        @SuppressWarnings("unchecked")
        LocalDateTimeline<Uttaksplanperiode> uttTimeline = uttaksplan.map(Uttaksplan::getTimeline).orElse(LocalDateTimeline.EMPTY_TIMELINE);
        resultatTimeline.combine(uttTimeline, this::kombinerMedUttak, JoinStyle.LEFT_JOIN);
    }

    private LocalDateSegment<Void> kombinerMedUttak(@SuppressWarnings("unused") LocalDateInterval interval,
                                                    LocalDateSegment<BeregningsresultatPeriodeDto> brpDto,
                                                    LocalDateSegment<Uttaksplanperiode> uttPeriode) {
        var ut = uttPeriode == null ? null : uttPeriode.getValue();
        var innvilget = (InnvilgetUttaksplanperiode) ut;
        brpDto.getValue().getAndeler().forEach(a -> {
            if (ut != null) {
                // setter uttak rett på andel 'a' - Ok her
                a.setUttak(new UttakDto(ut.getUtfall(), BigDecimal.ZERO)); // default uttak, overskrives under
                if (UtfallType.INNVILGET.equals(ut.getUtfall())) {
                    var key = toUttakArbeidsforhold(a);
                    innvilget.getUtbetalingsgrad(key).ifPresent(utbet -> a.setUttak(new UttakDto(innvilget.getUtfall(), utbet.getUtbetalingsgrad())));
                }
            }
        });
        return null;
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
        BeregningsresultatAndel ny = Kopimaskin.deepCopy(a, a.getBeregningsresultatPeriode());
        BeregningsresultatAndel.builder(ny)
            .medDagsats(a.getDagsats() + b.getDagsats())
            .medDagsatsFraBg(a.getDagsatsFraBg() + b.getDagsatsFraBg());
        return ny;
    }

    private static UttakArbeidsforhold toUttakArbeidsforhold(BeregningsresultatPeriodeAndelDto a) {
        return new UttakArbeidsforhold(
            a.getArbeidsgiverOrgnr() == null ? null : a.getArbeidsgiverOrgnr().getId(),
            a.getAktørId(),
            mapUttakAktivitet(a.getAktivitetStatus()).toString(),
            a.getArbeidsforholdId());
    }

    private static UttakArbeidType mapUttakAktivitet(AktivitetStatus aktivitetStatus) {
        if (aktivitetStatus.erArbeidstaker()) {
            return UttakArbeidType.ARBEIDSTAKER;
        } else if (aktivitetStatus.erSelvstendigNæringsdrivende()) {
            return UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE;
        } else if (aktivitetStatus.erFrilanser()) {
            return UttakArbeidType.FRILANS;
        } else {
            return UttakArbeidType.ANNET;
        }
    }
}
