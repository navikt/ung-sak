package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.FastsettPGIPeriodeTjeneste;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningAvklaringsbehovDefinisjon;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilterProvider;

@Dependent
public class RyddOgGjenopprettBeregningTjeneste {

    private static final Logger log = LoggerFactory.getLogger(RyddOgGjenopprettBeregningTjeneste.class);
    private final BehandlingRepository behandlingRepository;
    private final BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private final BeregningsgrunnlagTjeneste kalkulusTjeneste;
    private final VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider;
    private final FastsettPGIPeriodeTjeneste fastsettPGIPeriodeTjeneste;
    private final VilkårResultatRepository vilkårResultatRepository;
    private final Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste;

    private final boolean enableFjernPerioder;

    @Inject
    public RyddOgGjenopprettBeregningTjeneste(BehandlingRepository behandlingRepository,
                                              BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                              BeregningsgrunnlagTjeneste kalkulusTjeneste,
                                              VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider, FastsettPGIPeriodeTjeneste fastsettPGIPeriodeTjeneste,
                                              VilkårResultatRepository vilkårResultatRepository,
                                              @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste,
                                              @KonfigVerdi(value = "FJERN_VILKÅRSPERIODER_BEREGNING", defaultVerdi = "false") boolean enableFjernPerioder) {
        this.behandlingRepository = behandlingRepository;

        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.vilkårPeriodeFilterProvider = vilkårPeriodeFilterProvider;
        this.fastsettPGIPeriodeTjeneste = fastsettPGIPeriodeTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.enableFjernPerioder = enableFjernPerioder;
    }

    /**
     * Resetter beregning til å vurderes på nytt
     *
     * @param kontekst Behandlingskontrollkontekst
     */
    public void ryddOgGjenopprett(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var referanse = BehandlingReferanse.fra(behandling);

        // 1. Setter alle perioder til vurdering
        ryddVedtaksresultatForPerioderTilVurdering(kontekst, referanse);

        // 2. gjenoppretter beregning til initiell referanse der perioden ikke lenger vurderes (flippet vurderingsstatus)
        gjenopprettVedEndretVurderingsstatus(kontekst, referanse);

        // 3. avbryter alle aksjonspunkt i beregning som er åpne (aksjonspunkt reutledes på nytt ved behov)
        abrytÅpneBeregningaksjonspunkter(kontekst, behandling);

        // 4. Dekativerer PGI-periode dersom ikke lenger relevant
        fastsettPGIPeriodeTjeneste.fjernPGIDersomIkkeRelevant(behandling.getId());
    }

    /**
     * Deaktiverer perioder som er avslått før vi kaller kalkulus
     *
     * @param referanse Behandlingreferanse
     */
    public void deaktiverAvslåtteEllerFjernetPerioder(BehandlingReferanse referanse) {
        // deaktiverer grunnlag for referanser som er avslått eller inaktive (fjernet skjæringstidspunkt)
        kalkulusTjeneste.deaktiverBeregningsgrunnlagForAvslåttEllerFjernetPeriode(referanse);
    }

    /**
     * Fjerner perioder som er avslått i definerende vilkår no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste#definerendeVilkår() og initierer perioder som er innvilget dersom de ikke eksisterer
     * <p>
     *
     * @param behandlingReferanse Behandlingsreferanse
     */
    public void fjernEllerInitierPerioderFraDefinerendeVilkår(BehandlingReferanse behandlingReferanse) {
        if (!enableFjernPerioder) {
            return;
        }
        Vilkårene vilkårene = vilkårResultatRepository.hent(behandlingReferanse.getBehandlingId());
        var tilVurderingTjeneste = getPerioderTilVurderingTjeneste(behandlingReferanse);
        VilkårResultatBuilder resultatBuilder = Vilkårene.builderFraEksisterende(vilkårene)
            .medKantIKantVurderer(tilVurderingTjeneste.getKantIKantVurderer());
        VilkårBuilder vilkårBuilder = resultatBuilder.hentBuilderFor(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        fjernAvslåtte(tilVurderingTjeneste, vilkårene, vilkårBuilder);
        leggerTilTidligereFjernet(behandlingReferanse, tilVurderingTjeneste, vilkårene, vilkårBuilder);
        resultatBuilder.leggTil(vilkårBuilder, true);
        vilkårResultatRepository.lagre(behandlingReferanse.getBehandlingId(), resultatBuilder.build());

    }

    private static void leggerTilTidligereFjernet(BehandlingReferanse behandlingReferanse, VilkårsPerioderTilVurderingTjeneste tilVurderingTjeneste, Vilkårene vilkårene, VilkårBuilder vilkårBuilder) {
        var perioderSomSkalVurderes = finnPerioderSomSkalVurderes(behandlingReferanse, tilVurderingTjeneste, vilkårene);
        var bgPerioderSomMåGjenopprettes = finnPerioderSomMangler(vilkårene, perioderSomSkalVurderes);
        log.info("Legger til perioder for vurdering i beregning: " + bgPerioderSomMåGjenopprettes);
        bgPerioderSomMåGjenopprettes.stream().map(p -> vilkårBuilder.hentBuilderFor(p).medUtfall(Utfall.IKKE_VURDERT)).forEach(vilkårBuilder::leggTil);
    }

    private static List<DatoIntervallEntitet> finnPerioderSomMangler(Vilkårene vilkårene, Set<DatoIntervallEntitet> innvilgetPerioderTilVurdering) {
        var bgVilkår = vilkårene.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow(() -> new IllegalStateException("Hadde ikke Beregningsgrunnlagvilkår"));
        return bgVilkår.getPerioder().stream().map(VilkårPeriode::getPeriode)
            .filter(p -> innvilgetPerioderTilVurdering.stream().noneMatch(p::equals))
            .toList();
    }

    private static Set<DatoIntervallEntitet> finnPerioderSomSkalVurderes(BehandlingReferanse behandlingReferanse, VilkårsPerioderTilVurderingTjeneste tilVurderingTjeneste, Vilkårene vilkårene) {
        var innvilgetPerioder = finnDefinerendeVilkårsperioderMedUtfall(tilVurderingTjeneste, vilkårene, Utfall.OPPFYLT);
        var perioderPrVilkårstype = tilVurderingTjeneste.utledRådataTilUtledningAvVilkårsperioder(behandlingReferanse.getBehandlingId());
        var perioderTilVurderingIBeregning = perioderPrVilkårstype.get(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        return perioderTilVurderingIBeregning.stream().filter(p ->
            innvilgetPerioder.stream().anyMatch(p::equals)).collect(Collectors.toSet());
    }

    private static TreeSet<DatoIntervallEntitet> finnDefinerendeVilkårsperioderMedUtfall(VilkårsPerioderTilVurderingTjeneste tilVurderingTjeneste, Vilkårene vilkårene, Utfall utfall) {
        return tilVurderingTjeneste.definerendeVilkår().stream().flatMap(v -> vilkårene.getVilkår(v).stream())
            .flatMap(v -> v.getPerioder().stream().filter(p -> p.getGjeldendeUtfall().equals(utfall)).map(VilkårPeriode::getPeriode))
            .collect(Collectors.toCollection(TreeSet::new));
    }

    private static void fjernAvslåtte(VilkårsPerioderTilVurderingTjeneste tilVurderingTjeneste, Vilkårene vilkårene, VilkårBuilder vilkårBuilder) {
        var bgPerioderSomFjernes = finnPerioderSomKanFjernes(tilVurderingTjeneste, vilkårene);
        log.info("Fjerner perioder for vurdering i beregning: " + bgPerioderSomFjernes);
        bgPerioderSomFjernes.forEach(vilkårBuilder::tilbakestill);
    }

    private static List<DatoIntervallEntitet> finnPerioderSomKanFjernes(VilkårsPerioderTilVurderingTjeneste tilVurderingTjeneste, Vilkårene vilkårene) {
        var avslåttePerioder = finnDefinerendeVilkårsperioderMedUtfall(tilVurderingTjeneste, vilkårene, Utfall.IKKE_OPPFYLT);

        var bgVilkår = vilkårene.getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow(() -> new IllegalStateException("Hadde ikke Beregningsgrunnlagvilkår"));
        return bgVilkår.getPerioder().stream().filter(p -> p.getGjeldendeUtfall().equals(Utfall.IKKE_VURDERT))
            .map(VilkårPeriode::getPeriode).filter(p -> avslåttePerioder.stream().anyMatch(p::overlapper))
            .toList();
    }


    /**
     * Resetter beregningsgrunnlagreferanser og vilkårsresultat for perioder som ikke er til vurdering lenger i denne behandlingen
     * <p>
     * Rydding i kalkulus gjøres av no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagTjeneste#deaktiverBeregningsgrunnlagForAvslåttEllerFjernetPeriode(no.nav.k9.sak.behandling.BehandlingReferanse)
     *
     * @param kontekst  Behandlingskontrollkonteksts
     * @param referanse Behandlingreferanse
     */
    private void gjenopprettVedEndretVurderingsstatus(BehandlingskontrollKontekst kontekst, BehandlingReferanse referanse) {
        var gjenopprettetPeriodeListe = kalkulusTjeneste.gjenopprettTilInitiellDersomIkkeTilVurdering(referanse);
        if (!gjenopprettetPeriodeListe.isEmpty()) {
            log.info("Gjenoppretter initiell vurdering for perioder {}", gjenopprettetPeriodeListe);
            beregningsgrunnlagVilkårTjeneste.kopierVilkårresultatFraForrigeBehandling(
                kontekst.getBehandlingId(), referanse.getOriginalBehandlingId().orElseThrow(() -> new IllegalStateException("Kan ikke gjenopprette vilkårsresultat i førstegangsbehandling")),
                gjenopprettetPeriodeListe);
        }
    }

    private void abrytÅpneBeregningaksjonspunkter(BehandlingskontrollKontekst kontekst, Behandling behandling) {
        behandling.getAksjonspunkter().stream()
            .filter(this::erÅpentBeregningAksjonspunkt)
            .forEach(Aksjonspunkt::avbryt);
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    private boolean erÅpentBeregningAksjonspunkt(Aksjonspunkt a) {
        return a.getStatus().erÅpentAksjonspunkt() && Arrays.stream(BeregningAvklaringsbehovDefinisjon.values()).anyMatch(ab -> a.getAksjonspunktDefinisjon().getKode().equals(ab.getKode()));
    }


    private void ryddVedtaksresultatForPerioderTilVurdering(BehandlingskontrollKontekst kontekst, BehandlingReferanse ref) {
        var periodeFilter = vilkårPeriodeFilterProvider.getFilter(ref);
        var allePerioder = beregningsgrunnlagVilkårTjeneste.utledDetaljertPerioderTilVurdering(ref, periodeFilter);

        var alleUnntattForlengelser = allePerioder.stream().filter(p -> !p.erForlengelse())
            .map(PeriodeTilVurdering::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new));
        beregningsgrunnlagVilkårTjeneste.ryddVedtaksresultatOgVilkår(kontekst, alleUnntattForlengelser);
    }

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(BehandlingReferanse ref) {
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, perioderTilVurderingTjeneste, ref.getFagsakYtelseType(), ref.getBehandlingType())
            .orElseThrow(() -> new UnsupportedOperationException("VilkårsPerioderTilVurderingTjeneste ikke implementert for ytelse [" + ref.getFagsakYtelseType() + "], behandlingtype [" + ref.getBehandlingType() + "]"));
    }


}
