package no.nav.k9.sak.domene.registerinnhenting.impl.startpunkt;

import static java.util.Collections.emptyList;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.kodeverk.arbeidsforhold.InntektsmeldingInnsendingsårsak;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.k9.sak.domene.iay.modell.NaturalYtelse;
import no.nav.k9.sak.domene.iay.modell.Refusjon;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.vedtak.konfig.Tid;

@Dependent
class StartpunktUtlederInntektsmelding {
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;
    private BehandlingRepository behandlingRepository;

    StartpunktUtlederInntektsmelding() {
        // For CDI
    }

    @Inject
    StartpunktUtlederInntektsmelding(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                     InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                     BehandlingRepositoryProvider repositoryProvider) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    public StartpunktType utledStartpunkt(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag grunnlag1, InntektArbeidYtelseGrunnlag grunnlag2) {
        if (ref.getBehandlingType().equals(BehandlingType.FØRSTEGANGSSØKNAD)) {
            // Utlederen er nødt til å spesialhåndtere førstegangsbehandling, da videre kode forutsetter at det finnes et vedtak
            // Startpunkt i førstegangsbehandling skal imidlertid alltid være på starten, se EndringskontrollerImpl som overstyrer dette
            return StartpunktType.INIT_PERIODER;
        }

        List<Inntektsmelding> origIm = hentInntektsmeldingerFraGrunnlag(ref, grunnlag1, grunnlag2);
        List<Inntektsmelding> nyeIm = inntektsmeldingTjeneste.hentAlleInntektsmeldingerMottattEtterGjeldendeVedtak(ref);

        Map<ArbeidforholdNøkkel, Inntektsmelding> origImMap = indekserImMotArbeidsforhold(origIm);
        Map<ArbeidforholdNøkkel, Inntektsmelding> nyeImMap = indekserImMotArbeidsforhold(nyeIm);

        return nyeImMap.entrySet().stream()
            .map(nyIm -> finnStartpunktForNyIm(ref, nyIm, origImMap))
            .min(Comparator.comparingInt(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
    }

    private List<Inntektsmelding> hentInntektsmeldingerFraGrunnlag(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag grunnlag1, InntektArbeidYtelseGrunnlag grunnlag2) {
        Optional<InntektArbeidYtelseGrunnlag> origIayGrunnlag = finnIayGrunnlagForOrigBehandling(ref.getBehandlingId(), grunnlag1, grunnlag2);
        return origIayGrunnlag.map(InntektArbeidYtelseGrunnlag::getInntektsmeldinger)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .orElse(emptyList());
    }

    private StartpunktType finnStartpunktForNyIm(BehandlingReferanse ref, Map.Entry<ArbeidforholdNøkkel, Inntektsmelding> nyImEntry, Map<ArbeidforholdNøkkel, Inntektsmelding> origImMap) {
        if (erStartpunktForNyImBeregning(nyImEntry, origImMap, ref)) {
            return StartpunktType.BEREGNING;
        }
        return StartpunktType.UTTAKSVILKÅR;
    }

    private boolean erStartpunktForNyImBeregning(Map.Entry<ArbeidforholdNøkkel, Inntektsmelding> nyImEntry, Map<ArbeidforholdNøkkel, Inntektsmelding> origImMap, BehandlingReferanse ref) {
        Inntektsmelding nyIm = nyImEntry.getValue();
        if (nyIm.getInntektsmeldingInnsendingsårsak().equals(InntektsmeldingInnsendingsårsak.NY)) {
            return true;
        }
        Inntektsmelding origIM = origImMap.get(nyImEntry.getKey());
        if (origIM == null) {
            // Dersom IM ikke fins originalt, så regnes også i dette tilfelle ny IM
            return true;
        }

        List<NaturalYtelse> nyeNaturalYtelser = nyIm.getNaturalYtelser();
        List<NaturalYtelse> origNaturalYtelser = origIM.getNaturalYtelser();

        return nyIm.getInntektBeløp().getVerdi().compareTo(origIM.getInntektBeløp().getVerdi()) != 0
            || erEndringPåNaturalYtelser(nyeNaturalYtelser, origNaturalYtelser)
            || erEndringPåRefusjon(nyIm, origIM)
            || erGraderingPåAktivitetUtenDagsats(nyIm, ref);
    }


    private Optional<InntektArbeidYtelseGrunnlag> finnIayGrunnlagForOrigBehandling(Long behandlingId, InntektArbeidYtelseGrunnlag grunnlag1, InntektArbeidYtelseGrunnlag grunnlag2) {
        InntektArbeidYtelseGrunnlag gjeldendeGrunnlag = inntektArbeidYtelseTjeneste.finnGrunnlag(behandlingId).orElse(null);
        if (gjeldendeGrunnlag == null) {
            return Optional.empty();
        }

        if (Objects.equals(gjeldendeGrunnlag, grunnlag1)) {
            return Optional.of(grunnlag2);
        }
        if (Objects.equals(gjeldendeGrunnlag, grunnlag2)) {
            return Optional.of(grunnlag1);
        }
        return Optional.empty();

    }

    private Map<ArbeidforholdNøkkel, Inntektsmelding> indekserImMotArbeidsforhold(List<Inntektsmelding> origIM) {
        return origIM.stream()
            .collect(Collectors.toMap(ArbeidforholdNøkkel::new, im -> im));
    }

    private boolean erEndringPåNaturalYtelser(List<NaturalYtelse> nyA, List<NaturalYtelse> nyB) {
        return (nyA.size() != nyB.size()
            || !nyA.containsAll(nyB));
    }

    private boolean erEndringPåRefusjon(Inntektsmelding nyInntektsmelding, Inntektsmelding opprinneligInntektsmelding) {
        boolean erEndringPåBeløp = !Objects.equals(nyInntektsmelding.getRefusjonBeløpPerMnd(), opprinneligInntektsmelding.getRefusjonBeløpPerMnd())
            || !Objects.equals(nyInntektsmelding.getRefusjonOpphører(), opprinneligInntektsmelding.getRefusjonOpphører());

        boolean erEndringerPåEndringerRefusjon = erEndringerPåEndringerRefusjon(nyInntektsmelding.getEndringerRefusjon(), opprinneligInntektsmelding.getEndringerRefusjon());
        return erEndringPåBeløp || erEndringerPåEndringerRefusjon;
    }

    private boolean erEndringerPåEndringerRefusjon(List<Refusjon> nyInntektsmeldingEndringerRefusjon,
                                                   List<Refusjon> opprinneligInntektsmeldingEndringerRefusjon) {
        HashSet<Refusjon> nyttSett = new HashSet<>(nyInntektsmeldingEndringerRefusjon);
        HashSet<Refusjon> opprinneligSett = new HashSet<>(opprinneligInntektsmeldingEndringerRefusjon);

        return !nyttSett.equals(opprinneligSett);
    }

    private boolean erGraderingPåAktivitetUtenDagsats(Inntektsmelding nyIm, BehandlingReferanse ref) {
        if (nyIm.getGraderinger().isEmpty()) {
            return false;
        }
        Long originalBehandlingId = ref.getOriginalBehandlingId().orElse(null);
        if (originalBehandlingId == null) {
            return false;
        }
        Optional<Behandling> originalBehandling = this.behandlingRepository.hentBehandlingHvisFinnes(originalBehandlingId);
        if (originalBehandling.isEmpty() || originalBehandling.get().getBehandlingResultatType().isBehandlingsresultatAvslåttOrOpphørt()) {
            return false;
        }

        Optional<BeregningsresultatEntitet> originalBeregningsresultat = beregningsresultatRepository.hentBeregningsresultat(originalBehandlingId);

        return originalBeregningsresultat.filter(StartpunktutlederHjelper::finnesAktivitetHvorAlleHarDagsatsNull).isPresent();
    }

    private static class ArbeidforholdNøkkel {
        private final Arbeidsgiver arbeidsgiver;
        private final InternArbeidsforholdRef arbeidsforholdRef;

        ArbeidforholdNøkkel(Inntektsmelding inntektsmelding) {
            this.arbeidsgiver = inntektsmelding.getArbeidsgiver();
            this.arbeidsforholdRef = inntektsmelding.getArbeidsforholdRef();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ArbeidforholdNøkkel)) {
                return false;
            }
            ArbeidforholdNøkkel that = (ArbeidforholdNøkkel) o;

            return Objects.equals(arbeidsgiver, that.arbeidsgiver)
                && Objects.equals(arbeidsforholdRef, that.arbeidsforholdRef);
        }

        @Override
        public int hashCode() {
            return Objects.hash(arbeidsgiver, arbeidsforholdRef);
        }
    }
}


