package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.HarEndretInntektsmeldingVurderer;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilterProvider;

@Dependent
public class FinnPerioderMedStartIKontrollerFakta {


    private final VilkårResultatRepository vilkårResultatRepository;
    private final VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider;
    private final HarEndretInntektsmeldingVurderer harEndretInntektsmeldingVurderer;
    private final InntektArbeidYtelseTjeneste iayTjeneste;


    @Inject
    public FinnPerioderMedStartIKontrollerFakta(VilkårResultatRepository vilkårResultatRepository,
                                                VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider,
                                                HarEndretInntektsmeldingVurderer harEndretInntektsmeldingVurderer, InntektArbeidYtelseTjeneste iayTjeneste) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.vilkårPeriodeFilterProvider = vilkårPeriodeFilterProvider;
        this.harEndretInntektsmeldingVurderer = harEndretInntektsmeldingVurderer;
        this.iayTjeneste = iayTjeneste;
    }

    /**
     * Finner perioder skal kopiere resultatet fra fastsett skjæringstidspunkt fra forrige behandling/kobling og starte prosessering av nytt beregningsgrunnlag
     * i steget kontroller fakta beregning
     *
     * @param ref                          Behandlingreferanse
     * @param allePerioder                 Alle perioder (vilkårsperioder)
     * @param forlengelseperioderBeregning Perioder med forlengelse i beregning
     * @return Perioder med start i kontroller fakta beregning
     */
    public NavigableSet<PeriodeTilVurdering> finnPerioder(BehandlingReferanse ref,
                                                          NavigableSet<PeriodeTilVurdering> allePerioder,
                                                          Set<PeriodeTilVurdering> forlengelseperioderBeregning) {
        var periodeFilter = vilkårPeriodeFilterProvider.getFilter(ref);
        periodeFilter.ignorerAvslåttePerioder();
        var oppfylteStpForrigeBehandling = finnStpForOppfylteVilkårsperioderForrigeBehandling(ref);
        var perioder = allePerioder.stream().map(PeriodeTilVurdering::getPeriode).collect(Collectors.toSet());
        var forlengelserIOpptjening = periodeFilter.filtrerPerioder(perioder, VilkårType.OPPTJENINGSVILKÅRET).stream()
            .filter(PeriodeTilVurdering::erForlengelse)
            .collect(Collectors.toSet());


        // Filtrerer ut perioder som er forlengelse i opptjening, men ikkje beregning
        var forlengelserIOpptjeningRevurderingIBeregning = allePerioder.stream()
            .filter(forlengelserIOpptjening::contains)
            .filter(periode -> !forlengelseperioderBeregning.contains(periode))
            .filter(periode -> oppfylteStpForrigeBehandling.contains(periode.getPeriode().getFomDato()))
            .collect(Collectors.toCollection(TreeSet::new));

        if (forlengelserIOpptjeningRevurderingIBeregning.isEmpty()) {
            return new TreeSet<>();
        }

        var inntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(ref.getSaksnummer());
        return forlengelserIOpptjeningRevurderingIBeregning.stream()
            .filter((p) -> !harRelevantEndringFraForrigeVedtak(ref, p, inntektsmeldinger)).collect(Collectors.toCollection(TreeSet::new));
    }

    private boolean harRelevantEndringFraForrigeVedtak(BehandlingReferanse ref, PeriodeTilVurdering p, Set<Inntektsmelding> inntektsmeldings) {
        return harEndretInntektsmeldingVurderer.harEndringPåInntektsmeldingerTilBrukForPerioden(ref,
            inntektsmeldings,
            p.getPeriode(),
            FinnPerioderMedStartIKontrollerFakta::erEndret
        );
    }


    static boolean erEndret(List<Inntektsmelding> relevanteInntektsmeldinger, List<Inntektsmelding> relevanteInntektsmeldingerForrigeVedtak) {
        var unikeArbeidsforhold = finnUnikeArbeidsforholdIdentifikatorer(relevanteInntektsmeldinger);
        var unikeArbeidsforholdForrigeVedtak = finnUnikeArbeidsforholdIdentifikatorer(relevanteInntektsmeldingerForrigeVedtak);
        var erLikeStore = unikeArbeidsforhold.size() == unikeArbeidsforholdForrigeVedtak.size();
        var inneholderDeSamme = unikeArbeidsforhold.containsAll(unikeArbeidsforholdForrigeVedtak);
        return !(erLikeStore && inneholderDeSamme);
    }

    private static Set<String> finnUnikeArbeidsforholdIdentifikatorer(List<Inntektsmelding> relevanteInntektsmeldinger) {
        return relevanteInntektsmeldinger.stream().map(
            im -> im.getArbeidsgiver().getIdentifikator() + im.getArbeidsforholdRef().getReferanse()
        ).collect(Collectors.toSet());
    }


    private Set<LocalDate> finnStpForOppfylteVilkårsperioderForrigeBehandling(BehandlingReferanse ref) {
        return vilkårResultatRepository.hentHvisEksisterer(ref.getOriginalBehandlingId().orElseThrow()).orElseThrow()
            .getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .stream()
            .flatMap(v -> v.getPerioder().stream())
            .filter(p -> p.getGjeldendeUtfall().equals(Utfall.OPPFYLT))
            .map(VilkårPeriode::getPeriode)
            .map(DatoIntervallEntitet::getFomDato)
            .collect(Collectors.toSet());
    }


}
