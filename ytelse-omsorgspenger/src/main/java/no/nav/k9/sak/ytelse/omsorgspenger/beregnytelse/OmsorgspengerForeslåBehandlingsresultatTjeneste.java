package no.nav.k9.sak.ytelse.omsorgspenger.beregnytelse;

import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NavigableSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.aarskvantum.kontrakter.Aktivitet;
import no.nav.k9.aarskvantum.kontrakter.Utfall;
import no.nav.k9.aarskvantum.kontrakter.Uttaksperiode;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.revurdering.ytelse.RevurderingBehandlingsresultatutleder;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.behandling.steg.foreslåresultat.ForeslåBehandlingsresultatTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OmsorgspengerGrunnlagRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.repo.OppgittFraværPeriode;
import no.nav.k9.sak.ytelse.omsorgspenger.vilkår.OMPVilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;

@FagsakYtelseTypeRef(OMSORGSPENGER)
@ApplicationScoped
public class OmsorgspengerForeslåBehandlingsresultatTjeneste extends ForeslåBehandlingsresultatTjeneste {

    private static final Logger log = LoggerFactory.getLogger(OmsorgspengerForeslåBehandlingsresultatTjeneste.class);

    private OmsorgspengerGrunnlagRepository grunnlagRepository;
    private ÅrskvantumTjeneste årskvantumTjeneste;

    private VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste;
    private BeregningsresultatRepository beregningsresultatRepository;
    private VurderSøknadsfristTjeneste<OppgittFraværPeriode> vurderSøknadsfristTjeneste;

    OmsorgspengerForeslåBehandlingsresultatTjeneste() {
        // for proxy
    }

    @Inject
    public OmsorgspengerForeslåBehandlingsresultatTjeneste(BehandlingRepositoryProvider repositoryProvider,
                                                           VedtakVarselRepository vedtakVarselRepository,
                                                           OmsorgspengerGrunnlagRepository grunnlagRepository,
                                                           ÅrskvantumTjeneste årskvantumTjeneste,
                                                           @FagsakYtelseTypeRef(OMSORGSPENGER) VurderSøknadsfristTjeneste<OppgittFraværPeriode> vurderSøknadsfristTjeneste,
                                                           @FagsakYtelseTypeRef RevurderingBehandlingsresultatutleder revurderingBehandlingsresultatutleder,
                                                           @FagsakYtelseTypeRef(OMSORGSPENGER) OMPVilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste,
                                                           BeregningsresultatRepository beregningsresultatRepository) {
        super(repositoryProvider, vedtakVarselRepository, revurderingBehandlingsresultatutleder);
        this.grunnlagRepository = grunnlagRepository;
        this.årskvantumTjeneste = årskvantumTjeneste;
        this.vurderSøknadsfristTjeneste = vurderSøknadsfristTjeneste;
        this.vilkårsPerioderTilVurderingTjeneste = vilkårsPerioderTilVurderingTjeneste;
        this.beregningsresultatRepository = beregningsresultatRepository;
    }

    @Override
    protected DatoIntervallEntitet getMaksPeriode(Long behandlingId) {
        return grunnlagRepository.hentMaksPeriode(behandlingId).orElseThrow();
    }

    @Override
    protected boolean skalAvslåsBasertPåAndreForhold(BehandlingReferanse ref) {
        if (harSøknad(ref)) {
            return skalAvslåsBasertPåIngenTilkjentYtelseEtterBeregning(ref);
        }
        return skalAvslåsBasertPåAvslåtteUttaksperioder(ref);
    }

    @Override
    protected boolean skalBehandlingenSettesTilDelvisInnvilget(BehandlingReferanse ref, Vilkårene vilkårene) {
        if (skalBehandlingenSettesTilAvslått(ref, vilkårene)) {
            throw new IllegalArgumentException("Skal ikke sjekke delvis avslått når det allerede er avklart at riktig status er helt avslått");
        }
        if (!harSøknad(ref)) {
            //utnytter (foreløpig?) delvis-statusen kun når søknad fra bruker håndteres i behandlingen
            return false;
        }

        //sjekker mot både avslag på vilkår og mot tilkjent ytelse. Dette fordi det er mulig å få 0 fra beregning (til bruker)
        //uten at noen vilkår er satt til avslått, og slike tilfeller teller også som avslag (delvis avslag når det gjelder kun enkelte perioder)

        //slik sjekken er implementert mot tilkjent ytelse, vil noen rene innvilgelser identifiseres som delvis avslag,
        //dette skjer når behandling inneholder kravperioder fra IM og søknad, og disse ikke overlapper fullstendig.
        //det er OK for nå, konsekvensen er at det blir (unødvendig) manuelle brev også i disse tilfellene.

        return harMinstEnAvslåttAktuellVilkårsperiode(ref, vilkårene) || beregningGirMinstEnUkedagUtenTilkjentYtelseTilBruker(ref);
    }

    private boolean harSøknad(BehandlingReferanse ref) {
        var kravDokumenter = vurderSøknadsfristTjeneste.hentPerioderTilVurdering(ref).keySet();
        return kravDokumenter.stream().anyMatch(dok -> KravDokumentType.SØKNAD == dok.getType());
    }

    private boolean harMinstEnAvslåttAktuellVilkårsperiode(BehandlingReferanse ref, Vilkårene vilkårene) {
        for (VilkårType vilkårtype : VilkårType.values()) {
            LocalDateTimeline<Void> aktuellVilkårsperiode = utledAktuellVilkårsperiode(ref, vilkårtype);
            LocalDateTimeline<Void> avslåtteVilkår = avslått(vilkårene.getVilkårTimeline(vilkårtype));
            if (aktuellVilkårsperiode.intersects(avslåtteVilkår)) {
                log.info("Delvis innvilget. Identifisert avslått vilkår {}. Vilkårsperioder {}. Avslåtte vilkår {}.", vilkårtype, aktuellVilkårsperiode, avslåtteVilkår);
                return true;
            }
        }
        return false;
    }

    private static LocalDateTimeline<Void> avslått(LocalDateTimeline<VilkårPeriode> vilkårresultat) {
        return new LocalDateTimeline<>(
            vilkårresultat.stream()
                .filter(vr -> vr.getValue().getGjeldendeUtfall() == no.nav.k9.kodeverk.vilkår.Utfall.IKKE_OPPFYLT)
                .map(vr -> new LocalDateSegment<Void>(vr.getFom(), vr.getTom(), null))
                .collect(Collectors.toList())
        );
    }

    private LocalDateTimeline<Void> utledAktuellVilkårsperiode(BehandlingReferanse ref, VilkårType vilkårtype) {
        NavigableSet<DatoIntervallEntitet> aktuelleVilkårsperioder = vilkårsPerioderTilVurderingTjeneste.utled(ref.getBehandlingId(), vilkårtype);
        return new LocalDateTimeline<>(
            aktuelleVilkårsperioder.stream()
                .map(vp -> new LocalDateSegment<Void>(vp.getFomDato(), vp.getTomDato(), null))
                .collect(Collectors.toList())
        );
    }

    private boolean beregningGirMinstEnUkedagUtenTilkjentYtelseTilBruker(BehandlingReferanse ref) {
        LocalDateTimeline<Void> tidslinjeHarYtelseTilBruker = lagTidslinjeDerTilkjentYtelseTilBrukerFinnes(ref);
        LocalDateTimeline<Void> tidslinjeVilkårsperioder = lagTidslinjeAktuelleBeregningsvilkårPerioder(ref);
        LocalDateTimeline<Void> tidslinjeTilkjentYtelseMangler = tidslinjeVilkårsperioder.disjoint(tidslinjeHarYtelseTilBruker);

        boolean manglerTilkjentYtelseTilBrukerForEnUkedag = tidslinjeTilkjentYtelseMangler.stream()
            .anyMatch(OmsorgspengerForeslåBehandlingsresultatTjeneste::inneholderUkedag);

        if (manglerTilkjentYtelseTilBrukerForEnUkedag) {
            log.info("Delvis innvilget. Identifisert tilkjent ytelse 0 til bruker. Beregningsvilkårsperioder {}. Perioder med ytelse til bruker {}.", tidslinjeVilkårsperioder, tidslinjeHarYtelseTilBruker);
        }
        return manglerTilkjentYtelseTilBrukerForEnUkedag;
    }

    private static boolean inneholderUkedag(LocalDateSegment<?> segment) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(segment.getFom(), segment.getTom()).antallArbeidsdager() > 0;
    }

    private LocalDateTimeline<Void> lagTidslinjeAktuelleBeregningsvilkårPerioder(BehandlingReferanse ref) {
        NavigableSet<DatoIntervallEntitet> aktuellBeregningVilkårPerioder = vilkårsPerioderTilVurderingTjeneste.utled(ref.getBehandlingId(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        return new LocalDateTimeline<>(
            aktuellBeregningVilkårPerioder.stream()
                .map(vp -> new LocalDateSegment<Void>(vp.getFomDato(), vp.getTomDato(), null))
                .collect(Collectors.toList())
        );
    }

    private LocalDateTimeline<Void> lagTidslinjeDerTilkjentYtelseTilBrukerFinnes(BehandlingReferanse ref) {
        List<BeregningsresultatPeriode> beregningsresultatPerioder = beregningsresultatRepository.hentEndeligBeregningsresultat(ref.getBehandlingId())
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList());

        return new LocalDateTimeline<>(
            beregningsresultatPerioder.stream()
                .filter(br -> br.getBeregningsresultatAndelList().stream().anyMatch(a -> a.erBrukerMottaker() && a.getDagsats() > 0))
                .map(br -> new LocalDateSegment<Void>(br.getBeregningsresultatPeriodeFom(), br.getBeregningsresultatPeriodeTom(), null))
                .collect(Collectors.toList())
        );
    }

    private boolean skalAvslåsBasertPåIngenTilkjentYtelseEtterBeregning(BehandlingReferanse ref) {
        LocalDateTimeline<Void> tidslinjeHarYtelseTilBruker = lagTidslinjeDerTilkjentYtelseTilBrukerFinnes(ref);
        LocalDateTimeline<Void> tidslinjeAktuelleBeregningsvilkårPerioder = lagTidslinjeAktuelleBeregningsvilkårPerioder(ref);

        boolean minstEnDagHarTilkjentYtelse = tidslinjeHarYtelseTilBruker.intersects(tidslinjeAktuelleBeregningsvilkårPerioder);
        boolean harIngenTilkjentYtelseForAktuelleVilkårsperioder = !minstEnDagHarTilkjentYtelse;
        if (harIngenTilkjentYtelseForAktuelleVilkårsperioder) {
            log.info("Avslår behandling {}. Ingen aktuell vilkårsperiode endte med tilkjent ytelse", ref.getBehandlingUuid());
        }
        return harIngenTilkjentYtelseForAktuelleVilkårsperioder;
    }

    private boolean skalAvslåsBasertPåAvslåtteUttaksperioder(BehandlingReferanse ref) {
        var årskvantumForbrukteDager = årskvantumTjeneste.hentÅrskvantumForBehandling(ref.getBehandlingUuid());
        var sisteUttaksplan = årskvantumForbrukteDager.getSisteUttaksplan();
        if (sisteUttaksplan == null) {
            log.info("Avslår behandling. Har ingen uttaksplan for behandlingUuid={}", ref.getBehandlingUuid());
            return true;
        }

        List<Uttaksperiode> uttaksperioder = sisteUttaksplan.getAktiviteter()
            .stream()
            .map(Aktivitet::getUttaksperioder)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        boolean avslå = uttaksperioder.stream().allMatch(it -> Utfall.AVSLÅTT.equals(it.getUtfall()));

        if (avslå) {
            log.info("Avslår behandling {} pga alle uttaksperioder avslått: {}", ref.getBehandlingUuid(), uttaksperioder);
        }
        return avslå;
    }
}
