package no.nav.k9.sak.ytelse.omsorgspenger.beregnytelse;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.BEREGN_YTELSE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.k9.aarskvantum.kontrakter.Aktivitet;
import no.nav.k9.aarskvantum.kontrakter.FullUttaksplan;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
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
    private boolean enableFeriepengerPåTversAvSaker;

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
                                          OmsorgspengerYtelseVerifiserer omsorgspengerYtelseVerifiserer,
                                          @KonfigVerdi(value = "ENABLE_FERIEPENGER_PAA_TVERS_AV_SAKER_OG_PR_AAR", defaultVerdi = "true") boolean enableFeriepengerPåTversAvSaker
                                          ) {
        this.årskvantumTjeneste = årskvantumTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.fastsettBeregningsresultatTjeneste = fastsettBeregningsresultatTjeneste;
        this.beregnFeriepengerTjeneste = beregnFeriepengerTjeneste;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.omsorgspengerYtelseVerifiserer = omsorgspengerYtelseVerifiserer;
        this.enableFeriepengerPåTversAvSaker = enableFeriepengerPåTversAvSaker;
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

        if (harBådeNullArbeidsforholdsIdOgSpesifikkId(vurdertePerioder, fullUttaksplan)) {
            throw new IllegalStateException("Sammenhengende periode har både spesifikk arbeidsforholdsId og null. Saken må rulle tilbake til start hvor det forventes aksjonspunkt og arbeidsgiver må kontaktes for å sende inn ny inntektsmeldinger.");
        }
        // Kalle regeltjeneste
        var beregningsresultat = fastsettBeregningsresultatTjeneste.fastsettBeregningsresultat(beregningsgrunnlag, uttaksresultat);

        // Beregn feriepenger
        var feriepengerTjeneste = FagsakYtelseTypeRef.Lookup.find(beregnFeriepengerTjeneste, ref.getFagsakYtelseType()).orElseThrow();
        if (enableFeriepengerPåTversAvSaker){
            feriepengerTjeneste.beregnFeriepengerV2(beregningsresultat);
        } else {
            feriepengerTjeneste.beregnFeriepenger(beregningsresultat);
        }

        // Verifiser beregningsresultat
        omsorgspengerYtelseVerifiserer.verifiser(behandling, beregningsresultat);
        if (harUtbetalingTilBruker(beregningsresultat, vurdertePerioder)) {
            log.info("Har utbetaling til bruker: {}", beregningsresultat);
        }

        // Lagre beregningsresultat
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean harBådeNullArbeidsforholdsIdOgSpesifikkId(NavigableSet<DatoIntervallEntitet> vurdertePerioder, FullUttaksplan fullUttaksplan) {
        var aktiviteter = fullUttaksplan.getAktiviteter();

        var arbeidsgiverMap = new HashMap<DatoIntervallEntitet, Map<String, Set<String>>>();

        for (DatoIntervallEntitet periode : vurdertePerioder) {

            var arbeidsgiverSetHashMap = new HashMap<String, Set<String>>();
            aktiviteter.stream()
                .filter(it -> UttakArbeidType.fraKode(it.getArbeidsforhold().getType()).erArbeidstakerEllerFrilans())
                .filter(it -> it.getUttaksperioder()
                    .stream()
                    .filter(at -> !Duration.ZERO.equals(at.getDelvisFravær()))
                    .anyMatch(at -> periode.overlapper(at.getPeriode().getFom(), at.getPeriode().getTom())))
                .map(Aktivitet::getArbeidsforhold)
                .forEach(it -> {
                    var utledetKey = utledKey(it);
                    var idSet = arbeidsgiverSetHashMap.getOrDefault(utledetKey, new HashSet<>());
                    idSet.add(it.getArbeidsforholdId());
                    arbeidsgiverSetHashMap.put(utledetKey, idSet);
                });
            arbeidsgiverMap.put(periode, arbeidsgiverSetHashMap);
        }

        return arbeidsgiverMap.values()
            .stream()
            .map(Map::entrySet)
            .flatMap(Collection::stream)
            .anyMatch(it -> it.getValue().contains(null) && it.getValue().size() > 1);
    }

    private String utledKey(no.nav.k9.aarskvantum.kontrakter.Arbeidsforhold it) {
        return it.getOrganisasjonsnummer() == null ? it.getAktørId() : it.getOrganisasjonsnummer();
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
