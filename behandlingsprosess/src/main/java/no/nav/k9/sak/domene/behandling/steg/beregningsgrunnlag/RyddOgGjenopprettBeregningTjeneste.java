package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.FastsettPGIPeriodeTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.ValiderAktiveReferanserTjeneste;
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

@Dependent
public class RyddOgGjenopprettBeregningTjeneste {

    private static final Set<VilkårType> VILKÅR_HVOR_AVSLAG_IKKE_SKAL_FJERNES = Set.of(
        VilkårType.OPPTJENINGSVILKÅRET,
        VilkårType.OPPTJENINGSPERIODEVILKÅR
    );

    private static final Logger log = LoggerFactory.getLogger(RyddOgGjenopprettBeregningTjeneste.class);
    private final BehandlingRepository behandlingRepository;
    private final BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private final BeregningsgrunnlagTjeneste kalkulusTjeneste;
    private final FastsettPGIPeriodeTjeneste fastsettPGIPeriodeTjeneste;
    private final ValiderAktiveReferanserTjeneste validerAktiveReferanserTjeneste;
    private final GjenopprettPerioderSomIkkeVurderesTjeneste gjenopprettPerioderSomIkkeVurderesTjeneste;
    private final VilkårResultatRepository vilkårResultatRepository;
    private final Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste;
    private final boolean enableFjernPerioder;
    private final boolean validerIngenLoseReferanser;

    @Inject
    public RyddOgGjenopprettBeregningTjeneste(BehandlingRepository behandlingRepository,
                                              BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                              BeregningsgrunnlagTjeneste kalkulusTjeneste,
                                              FastsettPGIPeriodeTjeneste fastsettPGIPeriodeTjeneste,
                                              GjenopprettPerioderSomIkkeVurderesTjeneste gjenopprettPerioderSomIkkeVurderesTjeneste, VilkårResultatRepository vilkårResultatRepository,
                                              @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste,
                                              ValiderAktiveReferanserTjeneste validerAktiveReferanserTjeneste,
                                              @KonfigVerdi(value = "FJERN_VILKARSPERIODER_BEREGNING", defaultVerdi = "false") boolean enableFjernPerioder,
                                              @KonfigVerdi(value = "VALIDER_KALKULUS_REFERANSER", defaultVerdi = "false") boolean validerIngenLoseReferanser
    ) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.fastsettPGIPeriodeTjeneste = fastsettPGIPeriodeTjeneste;
        this.gjenopprettPerioderSomIkkeVurderesTjeneste = gjenopprettPerioderSomIkkeVurderesTjeneste;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.enableFjernPerioder = enableFjernPerioder;
        this.validerAktiveReferanserTjeneste = validerAktiveReferanserTjeneste;
        this.validerIngenLoseReferanser = validerIngenLoseReferanser;
    }

    /**
     * Resetter beregning til å vurderes på nytt. Utfører kun rydding internt i k9-sak. Rydding mot kalkulus gjøres i no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.RyddOgGjenopprettBeregningTjeneste#deaktiverAvslåtteEllerFjernetPerioder(no.nav.k9.sak.behandling.BehandlingReferanse)
     *
     * @param kontekst             Behandlingskontrollkontekst
     * @param perioderTilVurdering Perioder til vurdering
     */
    public void ryddOgGjenopprett(BehandlingskontrollKontekst kontekst, NavigableSet<PeriodeTilVurdering> perioderTilVurdering) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var referanse = BehandlingReferanse.fra(behandling);

        // 1. Setter perioder som skal vurderes i riktig tilstand
        ryddVedtaksresultatForPerioderTilVurdering(kontekst, perioderTilVurdering);

        // 2. gjenoppretter beregning til initiell referanse der perioden ikke lenger vurderes (flippet vurderingsstatus)
        gjenopprettPerioderSomIkkeVurderesTjeneste.gjenopprettVedEndretVurderingsstatus(referanse, perioderTilVurdering);

        // 3. avbryter alle aksjonspunkt i beregning som er åpne (aksjonspunkt reutledes på nytt ved behov)
        abrytÅpneBeregningaksjonspunkter(kontekst, behandling);

        // 4. Dekativerer PGI-periode dersom ikke lenger relevant
        fastsettPGIPeriodeTjeneste.fjernPGIDersomIkkeRelevant(behandling.getId());
    }


    /**
     * Deaktiverer perioder før vi kaller kalkulus
     *
     * @param referanse Behandlingreferanse
     */
    public void deaktiverAlleReferanserUlikInitiell(BehandlingReferanse referanse) {
        // deaktiverer grunnlag for referanser som er avslått eller inaktive (fjernet skjæringstidspunkt)
        kalkulusTjeneste.deaktiverBeregningsgrunnlagPerioderUlikInitiell(referanse);
        if (validerIngenLoseReferanser) {
            validerAktiveReferanserTjeneste.validerIngenLøseReferanser(referanse);
        }
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

        var perioderSomSkalInitieres = finnPerioderSomSkalInitieres(behandlingReferanse, vilkårene);
        perioderSomSkalInitieres.stream()
            .map(periode -> vilkårBuilder.hentBuilderFor(periode).medUtfall(Utfall.IKKE_VURDERT))
            .forEach(vilkårBuilder::leggTil);

        FinnPerioderSomSkalFjernesIBeregning finnPerioderSomSkalFjernesIBeregning = FinnPerioderSomSkalFjernesIBeregning
            .getFinnPerioderSkalIgnoreresIBeregning(behandlingReferanse);

        var aktuelleVilkårsperioder = getAktuelleVilkårsperioder(vilkårene, perioderSomSkalInitieres);

        finnPerioderSomSkalFjernesIBeregning
            .finnPerioderSomSkalFjernes(vilkårene, behandlingReferanse, aktuelleVilkårsperioder)
            .forEach(vilkårBuilder::tilbakestill);

        resultatBuilder.leggTil(vilkårBuilder, true);
        vilkårResultatRepository.lagre(behandlingReferanse.getBehandlingId(), resultatBuilder.build());

    }

    private static Set<DatoIntervallEntitet> getAktuelleVilkårsperioder(Vilkårene vilkårene, Set<DatoIntervallEntitet> perioderSomSkalInitieres) {
        var aktuelleVilkårsperioder = vilkårene
            .getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow(() -> new IllegalStateException("Hadde ikke beregningsGrunnlagvilkår"))
            .getPerioder().stream()
            .filter(vilkårPeriode -> Utfall.IKKE_VURDERT.equals(vilkårPeriode.getUtfall()))
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toCollection(HashSet::new));

        aktuelleVilkårsperioder.addAll(perioderSomSkalInitieres);

        return aktuelleVilkårsperioder;
    }

    /**
     * Finn perioder som tidligere har blitt fjernet
     * @param behandlingReferanse
     * @param vilkårene
     * @return perioderSomSkalInitieres
     */
    private Set<DatoIntervallEntitet> finnPerioderSomSkalInitieres(BehandlingReferanse behandlingReferanse, Vilkårene vilkårene) {
        var vilkårsPerioderTilVurderingTjeneste = getPerioderTilVurderingTjeneste(behandlingReferanse);
        var intiellePerioder = vilkårsPerioderTilVurderingTjeneste
            .utledFraDefinerendeVilkår(behandlingReferanse.getBehandlingId());

        var vilkårsPerioderIBeregning = vilkårene
            .getVilkår(VilkårType.BEREGNINGSGRUNNLAGVILKÅR).orElseThrow(() -> new IllegalStateException("Hadde ikke beregningsGrunnlagvilkår"))
            .getPerioder().stream()
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toSet());

        // Matcher kun skjæringstidspunkt her, siden definerende vilkår ikkje alltid vil matche eksakt (f.eks avslått deler av perioden i sykdom for psb)
        return intiellePerioder.stream()
            .filter(periode -> vilkårsPerioderIBeregning.stream().noneMatch(vp -> vp.getFomDato().isEqual(periode.getFomDato())))
            .collect(Collectors.toSet());
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


    /**
     * Setter perioder som skal vurderes i riktig tilstand.*
     *
     * @param kontekst             BehandlingskontrollKontekts
     * @param perioderTilVurdering Perioder til vurdering
     */
    private void ryddVedtaksresultatForPerioderTilVurdering(BehandlingskontrollKontekst kontekst, NavigableSet<PeriodeTilVurdering> perioderTilVurdering) {
        var perioder = perioderTilVurdering.stream().map(PeriodeTilVurdering::getPeriode).collect(Collectors.toCollection(TreeSet::new));
        beregningsgrunnlagVilkårTjeneste.ryddVedtaksresultatOgVilkår(kontekst, perioder);
    }

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(BehandlingReferanse ref) {
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, perioderTilVurderingTjeneste, ref.getFagsakYtelseType(), ref.getBehandlingType())
            .orElseThrow(() -> new UnsupportedOperationException("VilkårsPerioderTilVurderingTjeneste ikke implementert for ytelse [" + ref.getFagsakYtelseType() + "], behandlingtype [" + ref.getBehandlingType() + "]"));
    }


}
