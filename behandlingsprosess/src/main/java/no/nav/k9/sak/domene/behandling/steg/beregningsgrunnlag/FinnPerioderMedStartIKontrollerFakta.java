package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.InntektsmeldingRelevantForVilkårsrevurdering;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilterProvider;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.KompletthetPeriode;

@Dependent
public class FinnPerioderMedStartIKontrollerFakta {


    private final VilkårResultatRepository vilkårResultatRepository;
    private final VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider;
    private final HarEndretInntektsmeldingVurderer harEndretInntektsmeldingVurderer;
    private final InntektArbeidYtelseTjeneste iayTjeneste;
    private final MottatteDokumentRepository mottatteDokumentRepository;
    private final BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;



    @Inject
    public FinnPerioderMedStartIKontrollerFakta(VilkårResultatRepository vilkårResultatRepository,
                                                VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider,
                                                InntektArbeidYtelseTjeneste iayTjeneste,
                                                MottatteDokumentRepository mottatteDokumentRepository,
                                                BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                                BehandlingRepository behandlingRepository,
                                                @Any Instance<InntektsmeldingRelevantForVilkårsrevurdering> inntektsmeldingRelevantForBeregningVilkårsvurdering
                                                ) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.vilkårPeriodeFilterProvider = vilkårPeriodeFilterProvider;
        this.iayTjeneste = iayTjeneste;
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.harEndretInntektsmeldingVurderer = new HarEndretInntektsmeldingVurderer(
            behandlingRepository,
            getInntektsmeldingFilter(inntektsmeldingRelevantForBeregningVilkårsvurdering),
            FinnPerioderMedStartIKontrollerFakta::erEndret);
    }

    private static HarEndretInntektsmeldingVurderer.InntektsmeldingFilter getInntektsmeldingFilter(Instance<InntektsmeldingRelevantForVilkårsrevurdering> inntektsmeldingRelevantForBeregningVilkårsvurdering) {
        return (BehandlingReferanse referanse, Collection<Inntektsmelding> sakInntektsmeldinger, DatoIntervallEntitet vilkårsPeriode) ->
            InntektsmeldingRelevantForVilkårsrevurdering.finnTjeneste(inntektsmeldingRelevantForBeregningVilkårsvurdering, VilkårType.BEREGNINGSGRUNNLAGVILKÅR, referanse.getFagsakYtelseType()).begrensInntektsmeldinger(referanse, sakInntektsmeldinger, vilkårsPeriode);
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

        // Filtrerer ut endringer i mottatte inntektsmeldinger

        var inntektsmeldinger = iayTjeneste.hentUnikeInntektsmeldingerForSak(ref.getSaksnummer());
        var mottatteInntektsmeldinger = finnMottatteInntektsmeldinger(ref);

        var utenEndringIInntektsmelding = forlengelserIOpptjeningRevurderingIBeregning.stream()
            .filter((p) -> erInntektsmeldingerLikForrigeVedtak(ref, p, inntektsmeldinger, mottatteInntektsmeldinger))
            .collect(Collectors.toCollection(TreeSet::new));

        if (utenEndringIInntektsmelding.isEmpty()) {
            return new TreeSet<>();
        }

        // Filtrerer ut endret kompletthetsvurdering
        var initiellKompletthetPerioder = beregningPerioderGrunnlagRepository.getInitiellVersjon(ref.getBehandlingId()).stream()
            .flatMap(gr -> gr.getKompletthetPerioder().stream())
            .collect(Collectors.toSet());
        var aktiveKompletthetPerioder = beregningPerioderGrunnlagRepository.hentGrunnlag(ref.getBehandlingId()).stream()
            .flatMap(gr -> gr.getKompletthetPerioder().stream())
            .collect(Collectors.toSet());

        return utenEndringIInntektsmelding.stream()
            .filter(p -> erKompletthetsvurderingLikForrigeVedtak(p, aktiveKompletthetPerioder, initiellKompletthetPerioder))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private List<MottattDokument> finnMottatteInntektsmeldinger(BehandlingReferanse ref) {
        return mottatteDokumentRepository.hentGyldigeDokumenterMedFagsakId(ref.getFagsakId())
            .stream()
            .filter(it -> Objects.equals(Brevkode.INNTEKTSMELDING, it.getType()))
            .toList();
    }

    private boolean erKompletthetsvurderingLikForrigeVedtak(PeriodeTilVurdering p, Set<KompletthetPeriode> aktiveKompletthetPerioder, Set<KompletthetPeriode> initiellKompletthetPerioder) {
        var initiellPeriode = initiellKompletthetPerioder.stream().filter(it -> it.getSkjæringstidspunkt().equals(p.getSkjæringstidspunkt())).findFirst();
        var aktivPeriode = aktiveKompletthetPerioder.stream().filter(it -> it.getSkjæringstidspunkt().equals(p.getSkjæringstidspunkt())).findFirst();

        if (aktivPeriode.isPresent()) {
            return initiellPeriode.isPresent() && aktivPeriode.get().getVurdering().equals(initiellPeriode.get().getVurdering());
        } else {
            return initiellPeriode.isEmpty();
        }

    }

    private boolean erInntektsmeldingerLikForrigeVedtak(BehandlingReferanse ref, PeriodeTilVurdering p, Set<Inntektsmelding> inntektsmeldings, List<MottattDokument> mottatteInntektsmeldinger) {
        return !harEndretInntektsmeldingVurderer.harEndringPåInntektsmeldingerTilBrukForPerioden(ref,
            p.getPeriode(), inntektsmeldings,
            mottatteInntektsmeldinger
        );
    }


    static boolean erEndret(Collection<Inntektsmelding> relevanteInntektsmeldinger, Collection<Inntektsmelding> relevanteInntektsmeldingerForrigeVedtak) {
        var unikeArbeidsforhold = finnUnikeArbeidsforholdIdentifikatorer(relevanteInntektsmeldinger);
        var unikeArbeidsforholdForrigeVedtak = finnUnikeArbeidsforholdIdentifikatorer(relevanteInntektsmeldingerForrigeVedtak);
        var erLikeStore = unikeArbeidsforhold.size() == unikeArbeidsforholdForrigeVedtak.size();
        var inneholderDeSamme = unikeArbeidsforhold.containsAll(unikeArbeidsforholdForrigeVedtak);
        return !(erLikeStore && inneholderDeSamme);
    }

    private static Set<String> finnUnikeArbeidsforholdIdentifikatorer(Collection<Inntektsmelding> relevanteInntektsmeldinger) {
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
