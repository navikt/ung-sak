package no.nav.foreldrepenger.domene.registerinnhenting.impl.startpunkt;

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

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.NaturalYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;
import no.nav.foreldrepenger.domene.typer.InternArbeidsforholdRef;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.iay.InntektsmeldingInnsendingsårsak;
import no.nav.vedtak.konfig.Tid;

@Dependent
class StartpunktUtlederInntektsmelding {
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    StartpunktUtlederInntektsmelding() {
        // For CDI
    }

    @Inject
    StartpunktUtlederInntektsmelding(InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                     InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                     BehandlingRepositoryProvider repositoryProvider) {
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
    }

    public StartpunktType utledStartpunkt(BehandlingReferanse ref, InntektArbeidYtelseGrunnlag grunnlag1, InntektArbeidYtelseGrunnlag grunnlag2) {
        if (ref.getBehandlingType().equals(BehandlingType.FØRSTEGANGSSØKNAD)) {
            // Utlederen er nødt til å spesialhåndtere førstegangsbehandling, da videre kode forutsetter at det finnes et vedtak
            // Startpunkt i førstegangsbehandling skal imidlertid alltid være på starten, se EndringskontrollerImpl som overstyrer dette
            return StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT;
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
        if (erStartpunktForNyImInngangsvilkår(ref, nyImEntry.getValue())) {//NOSONAR utrykket evaluerer ikke alltid til true
            return StartpunktType.INNGANGSVILKÅR_MEDLEMSKAP;
        }
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

    private boolean erStartpunktForNyImInngangsvilkår(BehandlingReferanse ref, Inntektsmelding nyIm) {
        // Samme logikk som 5045 AksjonspunktutlederForAvklarStartdatoForForeldrepengeperioden
        LocalDate førsteUttaksDato = endreDatoHvisLørdagEllerSøndag(ref.getFørsteUttaksdato());
        LocalDate startDatoIm = endreDatoHvisLørdagEllerSøndag(nyIm.getStartDatoPermisjon().orElse(Tid.TIDENES_BEGYNNELSE));
        return !førsteUttaksDato.equals(startDatoIm);
    }

    LocalDate endreDatoHvisLørdagEllerSøndag(LocalDate dato) {
        if (dato.getDayOfWeek().equals(DayOfWeek.SATURDAY)) {
            return dato.plusDays(2L);
        } else if (dato.getDayOfWeek().equals(DayOfWeek.SUNDAY)) {
            return dato.plusDays(1L);
        }
        return dato;
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
        Long orgigBehandlingId = ref.getOriginalBehandlingId().orElse(null);
        if (orgigBehandlingId == null) {
            return false;
        }
        Optional<Behandlingsresultat> originalBehandlingsresultat = this.behandlingsresultatRepository.hentHvisEksisterer(orgigBehandlingId);
        if (!originalBehandlingsresultat.isPresent() || originalBehandlingsresultat.get().isBehandlingsresultatAvslåttOrOpphørt()) {
            return false;
        }

        Optional<BeregningsresultatEntitet> origBeregningsresultatFP = beregningsresultatRepository.hentBeregningsresultat(orgigBehandlingId);

        if (!origBeregningsresultatFP.isPresent()) {
            return false;
        }

        return StartpunktutlederHjelper.finnesAktivitetHvorAlleHarDagsatsNull(origBeregningsresultatFP.get());
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


