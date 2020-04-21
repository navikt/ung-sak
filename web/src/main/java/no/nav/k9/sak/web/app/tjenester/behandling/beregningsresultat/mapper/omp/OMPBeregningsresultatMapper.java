package no.nav.k9.sak.web.app.tjenester.behandling.beregningsresultat.mapper.omp;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.uttak.UtfallType;
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
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.beregningsresultat.BeregningsresultatDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.BeregningsresultatPeriodeAndelDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.BeregningsresultatPeriodeDto;
import no.nav.k9.sak.kontrakt.beregningsresultat.UttakDto;
import no.nav.k9.sak.kontrakt.uttak.OmsorgspengerUtfall;
import no.nav.k9.sak.kontrakt.uttak.UttakArbeidsforhold;
import no.nav.k9.sak.kontrakt.uttak.UttaksPlanOmsorgspengerAktivitet;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.OrgNummer;
import no.nav.k9.sak.web.app.tjenester.behandling.beregningsresultat.mapper.BeregningsresultatMapper;
import no.nav.k9.sak.ytelse.beregning.tilbaketrekk.Kopimaskin;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.api.ÅrskvantumResultat;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;
import no.nav.vedtak.util.Tuple;

@FagsakYtelseTypeRef("OMP")
@ApplicationScoped
public class OMPBeregningsresultatMapper implements BeregningsresultatMapper {

    private ArbeidsgiverTjeneste arbeidsgiverTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private ÅrskvantumTjeneste årskvantumTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    OMPBeregningsresultatMapper() {
        // For inject
    }

    @Inject
    public OMPBeregningsresultatMapper(ArbeidsgiverTjeneste arbeidsgiverTjeneste,
                                       InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                       ÅrskvantumTjeneste årskvantumTjeneste,
                                       SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.arbeidsgiverTjeneste = arbeidsgiverTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.årskvantumTjeneste = årskvantumTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public BeregningsresultatDto map(Behandling behandling,
                                     BehandlingBeregningsresultatEntitet beregningsresultatAggregat) {
        var ref = BehandlingReferanse.fra(behandling);
        var uttaksplan = Optional.ofNullable(årskvantumTjeneste.hentÅrskvantumForBehandling(ref));
        LocalDate opphørsdato = skjæringstidspunktTjeneste.getOpphørsdato(ref).orElse(null);
        return BeregningsresultatDto.build()
            .medOpphoersdato(opphørsdato)
            .medPerioder(lagPerioder(behandling.getId(), beregningsresultatAggregat.getBgBeregningsresultat(), uttaksplan))
            .medSkalHindreTilbaketrekk(beregningsresultatAggregat.skalHindreTilbaketrekk().orElse(null))
            .create();
    }

    public List<BeregningsresultatPeriodeDto> lagPerioder(long behandlingId, BeregningsresultatEntitet beregningsresultat,
                                                          Optional<ÅrskvantumResultat> uttaksplan) {
        var iayGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId);
        var beregningsresultatPerioder = beregningsresultat.getBeregningsresultatPerioder();
        var andelTilSisteUtbetalingsdatoMap = finnSisteUtbetalingdatoForAlleAndeler(
            beregningsresultatPerioder);

        LocalDateTimeline<BeregningsresultatPeriode> brpTimline = beregningsresultat.getBeregningsresultatTimeline();

        var dtoer = brpTimline.toSegments().stream()
            .map(seg -> BeregningsresultatPeriodeDto.build(seg.getFom(), seg.getTom())
                .medDagsats(seg.getValue().getDagsats())
                .medAndeler(lagAndeler(seg.getValue(), andelTilSisteUtbetalingsdatoMap, iayGrunnlag, uttaksplan))
                .create())
            .collect(Collectors.toList());
        var resultatTimeline = toTimeline(dtoer);

        return resultatTimeline.toSegments().stream().map(LocalDateSegment::getValue).collect(Collectors.toList());
    }

    private LocalDateTimeline<BeregningsresultatPeriodeDto> toTimeline(List<BeregningsresultatPeriodeDto> resultatDtoListe) {
        var segments = resultatDtoListe.stream().map(v -> new LocalDateSegment<>(v.getFom(), v.getTom(), v)).collect(Collectors.toList());
        return new LocalDateTimeline<>(segments);
    }

    List<BeregningsresultatPeriodeAndelDto> lagAndeler(BeregningsresultatPeriode beregningsresultatPeriode,
                                                       Map<Tuple<AktivitetStatus, Optional<String>>, Optional<LocalDate>> andelTilSisteUtbetalingsdatoMap,
                                                       Optional<InntektArbeidYtelseGrunnlag> iayGrunnlag,
                                                       Optional<ÅrskvantumResultat> uttaksplan) {

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
                    .medStillingsprosent(brukersAndel.getStillingsprosent())
                    .medUtbetalingsgrad(brukersAndel.getUtbetalingsgrad());
                uttaksplan.ifPresent(it -> mapUttakForAndel(beregningsresultatPeriode.getPeriode(), brukersAndel, it, dtoBuilder));
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

    private void mapUttakForAndel(DatoIntervallEntitet periode, BeregningsresultatAndel brukersAndel, ÅrskvantumResultat årskvantumResultat, BeregningsresultatPeriodeAndelDto.Builder dtoBuilder) {
        var uttaksPeriodeForAndel = årskvantumResultat.getUttaksplan().getAktiviteter().stream()
            .filter(it -> matcherArbeidsforhold(it.getArbeidsforhold(), brukersAndel))
            .map(UttaksPlanOmsorgspengerAktivitet::getUttaksperioder)
            .flatMap(Collection::stream)
            .filter(it -> DatoIntervallEntitet.fraOgMedTilOgMed(it.getFom(), it.getTom()).overlapper(periode))
            .collect(Collectors.toList());


        if (uttaksPeriodeForAndel.size() > 1) {
            throw new IllegalStateException("Fant to uttaksperioder for en andel");
        } else if (!uttaksPeriodeForAndel.isEmpty()) {
            var uttaksperiodeOmsorgspenger = uttaksPeriodeForAndel.get(0);
            var utfallType = OmsorgspengerUtfall.INNVILGET.equals(uttaksperiodeOmsorgspenger.getUtfall()) ? UtfallType.INNVILGET : UtfallType.AVSLÅTT;
            dtoBuilder.medUttak(new UttakDto(utfallType, uttaksperiodeOmsorgspenger.getUtbetalingsgrad()));
        }
    }

    private boolean matcherArbeidsforhold(UttakArbeidsforhold arbeidsforhold, BeregningsresultatAndel brukersAndel) {
        if (brukersAndel.getArbeidsgiver().isPresent()) {
            var arbeidsgiver = brukersAndel.getArbeidsgiver().get();
            if (arbeidsgiver.erAktørId()) {
                return arbeidsforhold.getAktørId() != null && arbeidsforhold.getAktørId().getId() != null
                    && arbeidsgiver.getAktørId().equals(arbeidsforhold.getAktørId())
                    && InternArbeidsforholdRef.ref(arbeidsforhold.getArbeidsforholdId()).equals(brukersAndel.getArbeidsforholdRef());
            } else if (arbeidsgiver.getErVirksomhet()) {
                return Objects.equals(arbeidsgiver.getOrgnr(), arbeidsforhold.getOrganisasjonsnummer())
                    && InternArbeidsforholdRef.ref(arbeidsforhold.getArbeidsforholdId()).equals(brukersAndel.getArbeidsforholdRef());
            }
        }
        return false;
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

}
