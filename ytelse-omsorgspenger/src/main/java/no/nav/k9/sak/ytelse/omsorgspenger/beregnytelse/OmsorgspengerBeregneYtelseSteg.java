package no.nav.k9.sak.ytelse.omsorgspenger.beregnytelse;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.BEREGN_YTELSE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.aarskvantum.kontrakter.Aktivitet;
import no.nav.k9.aarskvantum.kontrakter.Arbeidsforhold;
import no.nav.k9.aarskvantum.kontrakter.FullUttaksplan;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.behandling.steg.beregnytelse.BeregneYtelseSteg;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.beregning.BeregnFeriepengerTjeneste;
import no.nav.k9.sak.ytelse.beregning.FastsettBeregningsresultatTjeneste;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultat;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;


@FagsakYtelseTypeRef(OMSORGSPENGER)
@BehandlingStegRef(value = BEREGN_YTELSE)
@BehandlingTypeRef
@ApplicationScoped
public class OmsorgspengerBeregneYtelseSteg implements BeregneYtelseSteg {

    private static final Logger log = LoggerFactory.getLogger(OmsorgspengerBeregneYtelseSteg.class);

    private BehandlingRepository behandlingRepository;
    private BeregningTjeneste kalkulusTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;
    private FastsettBeregningsresultatTjeneste fastsettBeregningsresultatTjeneste;
    private Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjeneste;
    private ÅrskvantumTjeneste årskvantumTjeneste;
    private VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste;
    private OmsorgspengerYtelseVerifiserer omsorgspengerYtelseVerifiserer;

    protected OmsorgspengerBeregneYtelseSteg() {
        // for proxy
    }

    @Inject
    public OmsorgspengerBeregneYtelseSteg(BehandlingRepositoryProvider repositoryProvider,
                                          BeregningTjeneste kalkulusTjeneste,
                                          ÅrskvantumTjeneste årskvantumTjeneste,
                                          FastsettBeregningsresultatTjeneste fastsettBeregningsresultatTjeneste,
                                          @Any Instance<BeregnFeriepengerTjeneste> beregnFeriepengerTjeneste,
                                          @FagsakYtelseTypeRef(OMSORGSPENGER) @BehandlingTypeRef VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste,
                                          OmsorgspengerYtelseVerifiserer omsorgspengerYtelseVerifiserer
    ) {
        this.årskvantumTjeneste = årskvantumTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.fastsettBeregningsresultatTjeneste = fastsettBeregningsresultatTjeneste;
        this.beregnFeriepengerTjeneste = beregnFeriepengerTjeneste;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.omsorgspengerYtelseVerifiserer = omsorgspengerYtelseVerifiserer;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);

        var beregningsgrunnlag = kalkulusTjeneste.hentEksaktFastsattForAllePerioder(ref);

        var fullUttaksplan = årskvantumTjeneste.hentFullUttaksplan(ref.getSaksnummer());
        var vurdertePerioder = vilkårsPerioderTilVurderingTjeneste.utled(behandlingId, VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        var aktiviteter = fullUttaksplan.getAktiviteter();
        var uttaksresultat = new UttakResultat(ref.getFagsakYtelseType(), new MapFraÅrskvantumResultat().mapFra(aktiviteter));

        if (harBådeNullArbeidsforholdsIdOgSpesifikkId(fullUttaksplan)) {
            throw new IllegalStateException("Overlappende periode har både spesifikk arbeidsforholdsId og null. Saken må rulle tilbake til start hvor det forventes aksjonspunkt og arbeidsgiver må kontaktes for å sende inn ny inntektsmeldinger.");
        }
        // Kalle regeltjeneste
        var beregningsresultat = fastsettBeregningsresultatTjeneste.fastsettBeregningsresultat(beregningsgrunnlag, uttaksresultat);

        // Beregn feriepenger
        var feriepengerTjeneste = FagsakYtelseTypeRef.Lookup.find(beregnFeriepengerTjeneste, ref.getFagsakYtelseType()).orElseThrow();
        feriepengerTjeneste.beregnFeriepenger(ref, beregningsresultat);

        // Verifiser beregningsresultat
        omsorgspengerYtelseVerifiserer.verifiser(behandling, beregningsresultat);
        if (harUtbetalingTilBruker(beregningsresultat, vurdertePerioder)) {
            log.info("Har utbetaling til bruker: {}", beregningsresultat);
        }

        // Lagre beregningsresultat
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean harBådeNullArbeidsforholdsIdOgSpesifikkId(FullUttaksplan fullUttaksplan) {
        var aktiviteter = fullUttaksplan.getAktiviteter();
        LocalDateTimeline<List<Arbeidsforhold>> arbeidsforholdTidslinje = lagArbeidsforholdTidslinje(aktiviteter);

        return arbeidsforholdTidslinje.toSegments()
            .stream()
            .anyMatch(this::harUgyldigKombinasjonAvArbeidsforholdReferanserHosArbeidsgiver);

    }

    private LocalDateTimeline<List<Arbeidsforhold>> lagArbeidsforholdTidslinje(List<Aktivitet> aktiviteter) {
        var segmenter = aktiviteter.stream()
            .filter(a -> a.getArbeidsforhold().getType().equals(UttakArbeidType.ARBEIDSTAKER.getKode()))
            .flatMap(a -> a.getUttaksperioder().stream()
                .map(p ->
                    new LocalDateSegment<>(p.getPeriode().getFom(), p.getPeriode().getTom(), a.getArbeidsforhold())))
            .toList();
        return LocalDateTimeline.buildGroupOverlappingSegments(segmenter);
    }

    private boolean harUgyldigKombinasjonAvArbeidsforholdReferanserHosArbeidsgiver(LocalDateSegment<List<Arbeidsforhold>> s) {
        return grupperPåArbeidsgiver(s.getValue()).values().stream().anyMatch(this::harBlandingAvSpesifikkeOgGenerelleArbeidsforhold);
    }

    private Map<String, List<Arbeidsforhold>> grupperPåArbeidsgiver(List<Arbeidsforhold> arbeidsforholdList) {
        return arbeidsforholdList.stream()
            .collect(Collectors.groupingBy(a -> a.getOrganisasjonsnummer() != null ? a.getOrganisasjonsnummer() : a.getAktørId()));
    }

    private boolean harBlandingAvSpesifikkeOgGenerelleArbeidsforhold(List<Arbeidsforhold> arbeidsforholdList) {
        return !(harKunGenerellArbeidsforhold(arbeidsforholdList) || harKunSpesifikkeArbeidsforhold(arbeidsforholdList));
    }

    private boolean harKunSpesifikkeArbeidsforhold(List<Arbeidsforhold> arbeidsforholdList) {
        return arbeidsforholdList.stream().allMatch(arbeidsforhold ->
            arbeidsforhold.getArbeidsforholdId() != null);
    }

    private boolean harKunGenerellArbeidsforhold(List<Arbeidsforhold> arbeidsforholdList) {
        return arbeidsforholdList.stream().allMatch(arbeidsforhold ->
            arbeidsforhold.getArbeidsforholdId() == null);
    }

    private boolean harUtbetalingTilBruker(BeregningsresultatEntitet beregningsresultat, NavigableSet<DatoIntervallEntitet> vurdertePerioder) {
        return beregningsresultat.getBeregningsresultatPerioder().stream()
            .filter(p -> vurdertePerioder.stream().anyMatch(vp -> vp.overlapper(p.getPeriode())))
            .anyMatch(p -> p.getBeregningsresultatAndelList().stream().anyMatch(a -> a.erBrukerMottaker() && a.getDagsats() > 0));
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        beregningsresultatRepository.deaktiverBeregningsresultat(behandling.getId(), kontekst.getSkriveLås());
    }
}
