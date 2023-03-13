package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.KONTROLLER_FAKTA_BEREGNING;
import static no.nav.k9.kodeverk.behandling.BehandlingStegType.PRECONDITION_BEREGNING;
import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VURDER_REF_BERGRUNN;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.FastsettPGIPeriodeTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningForBeregningTjeneste;
import no.nav.folketrygdloven.kalkulus.kodeverk.StegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningAvklaringsbehovDefinisjon;
import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.vilkår.PeriodeTilVurdering;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilterProvider;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.beregning.grunnlag.InputOverstyringPeriode;

@FagsakYtelseTypeRef
@BehandlingStegRef(value = PRECONDITION_BEREGNING)
@BehandlingTypeRef
@ApplicationScoped
public class VurderPreconditionBeregningSteg implements BeregningsgrunnlagSteg {
    private static final Logger log = LoggerFactory.getLogger(VurderPreconditionBeregningSteg.class);


    private VilkårResultatRepository vilkårResultatRepository;
    private BehandlingRepository behandlingRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste;
    private Instance<PreconditionBeregningAksjonspunktUtleder> aksjonspunktUtledere;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private BeregningsgrunnlagTjeneste kalkulusTjeneste;
    private BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository;
    private VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider;
    private FastsettPGIPeriodeTjeneste fastsettPGIPeriodeTjeneste;
    private KalkulusStartpunktUtleder kalkulusStartpunktUtleder;


    protected VurderPreconditionBeregningSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderPreconditionBeregningSteg(VilkårResultatRepository vilkårResultatRepository,
                                           BehandlingRepository behandlingRepository,
                                           InntektArbeidYtelseTjeneste iayTjeneste,
                                           @Any Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste,
                                           @Any Instance<VilkårsPerioderTilVurderingTjeneste> perioderTilVurderingTjeneste,
                                           @Any Instance<PreconditionBeregningAksjonspunktUtleder> aksjonspunktUtledere,
                                           BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                           BeregningsgrunnlagTjeneste kalkulusTjeneste,
                                           BeregningPerioderGrunnlagRepository beregningPerioderGrunnlagRepository,
                                           VilkårPeriodeFilterProvider vilkårPeriodeFilterProvider,
                                           FastsettPGIPeriodeTjeneste fastsettPGIPeriodeTjeneste,
                                           KalkulusStartpunktUtleder kalkulusStartpunktUtleder) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.behandlingRepository = behandlingRepository;
        this.iayTjeneste = iayTjeneste;
        this.opptjeningForBeregningTjeneste = opptjeningForBeregningTjeneste;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
        this.aksjonspunktUtledere = aksjonspunktUtledere;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.kalkulusTjeneste = kalkulusTjeneste;
        this.beregningPerioderGrunnlagRepository = beregningPerioderGrunnlagRepository;
        this.vilkårPeriodeFilterProvider = vilkårPeriodeFilterProvider;
        this.fastsettPGIPeriodeTjeneste = fastsettPGIPeriodeTjeneste;
        this.kalkulusStartpunktUtleder = kalkulusStartpunktUtleder;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var referanse = BehandlingReferanse.fra(behandling);

        // 1. Setter alle perioder til vurdering
        ryddVedtaksresultatForPerioderTilVurdering(kontekst, referanse);

        // 2. Kopierer grunnlag og vilkårsresultat for forlengelser
        kopierGrunnlagForForlengelseperioder(kontekst, referanse);

        // 3. Avslår perioder der vi har avslag før beregning eller ingen aktiviteter (ingen grunnlag for beregning)
        avslåBeregningVedBehov(kontekst, behandling, referanse);

        // 4. deaktiverer grunnlag for referanser som er avslått eller inaktive (fjernet skjæringstidspunkt)
        kalkulusTjeneste.deaktiverBeregningsgrunnlagForAvslåttEllerFjernetPeriode(referanse);

        // 5. gjenoppretter beregning til initiell referanse der perioden ikke lenger vurderes (flippet vurderingsstatus)
        gjenopprettVedEndretVurderingsstatus(kontekst, referanse);

        // 6. avbryter alle aksjonspunkt i beregning som er åpne (aksjonspunkt reutledes på nytt ved behov)
        abrytÅpneBeregningaksjonspunkter(kontekst, behandling);

        // 7. kopierer input overstyringer for migrering fra infotrygd
        kopierInputOverstyring(behandling);

        // 8. Dekativerer PGI-periode dersom ikke lenger relevant
        fastsettPGIPeriodeTjeneste.fjernPGIDersomIkkeRelevant(behandling.getId());

        return BehandleStegResultat.utførtMedAksjonspunktResultater(finnAksjonspunkter(behandling));
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
                kontekst,
                referanse.getOriginalBehandlingId().orElseThrow(() -> new IllegalStateException("Kan ikke gjenopprette vilkårsresultat i førstegangsbehandling")),
                gjenopprettetPeriodeListe);
        }
    }

    /**
     * Kopierer overstyrt input i revurderinger som ikke er manuelt opprettet (se https://jira.adeo.no/browse/TSF-2658)
     *
     * @param behandling Behandling
     */
    private void kopierInputOverstyring(Behandling behandling) {
        var perioderTilVurdering = vurdertePerioder(VilkårType.BEREGNINGSGRUNNLAGVILKÅR, BehandlingReferanse.fra(behandling));
        if (behandling.erRevurdering() && !behandling.erManueltOpprettet() && !harEksisterendeOverstyringer(behandling, perioderTilVurdering)) {
            var kopiertInputOverstyring = behandling.getOriginalBehandlingId().flatMap(beregningPerioderGrunnlagRepository::hentGrunnlag)
                .stream()
                .flatMap(it -> it.getInputOverstyringPerioder().stream())
                .filter(it -> perioderTilVurdering.stream().anyMatch(p -> p.getFomDato().equals(it.getSkjæringstidspunkt())))
                .map(InputOverstyringPeriode::new)
                .toList();
            if (!kopiertInputOverstyring.isEmpty()) {
                beregningPerioderGrunnlagRepository.lagreInputOverstyringer(behandling.getId(), kopiertInputOverstyring);
            }
        }
    }

    private boolean harEksisterendeOverstyringer(Behandling behandling, Set<DatoIntervallEntitet> perioderTilVurdering) {
        return beregningPerioderGrunnlagRepository.hentGrunnlag(behandling.getId())
            .stream()
            .flatMap(it -> it.getInputOverstyringPerioder().stream())
            .anyMatch(it -> perioderTilVurdering.stream().anyMatch(p -> p.getFomDato().equals(it.getSkjæringstidspunkt())));
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

    private void avslåBeregningVedBehov(BehandlingskontrollKontekst kontekst, Behandling behandling, BehandlingReferanse referanse) {
        var periodeFilter = vilkårPeriodeFilterProvider.getFilter(referanse).ignorerForlengelseperioder();
        var vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId());
        var vilkåret = vilkårene.getVilkår(VilkårType.OPPTJENINGSVILKÅRET)
            .orElseThrow();
        var vurdertePerioderIOpptjening = vurdertePerioder(VilkårType.OPPTJENINGSVILKÅRET, referanse);

        var perioderTilVurderingIBeregning = periodeFilter.filtrerPerioder(vurdertePerioderIOpptjening, VilkårType.BEREGNINGSGRUNNLAGVILKÅR)
            .stream().map(PeriodeTilVurdering::getPeriode).toList();

        var opptjeningForBeregningTjeneste = finnOpptjeningForBeregningTjeneste(BehandlingReferanse.fra(behandling));
        var grunnlag = iayTjeneste.hentGrunnlag(behandling.getId());

        var avslåttePerioder = vilkåret.getPerioder()
            .stream()
            .filter(it -> perioderTilVurderingIBeregning.contains(it.getPeriode()))
            .filter(it -> Utfall.IKKE_OPPFYLT.equals(it.getGjeldendeUtfall()) || (ikkeInnvilgetEtter847(it) && ingenBeregningsAktiviteter(opptjeningForBeregningTjeneste, it, grunnlag, referanse)))
            .collect(Collectors.toList());

        var noeAvslått = !vilkåret.getPerioder().isEmpty() && !avslåttePerioder.isEmpty();

        if (noeAvslått) {
            log.info("Avslår beregning for perioder {}", avslåttePerioder);
            avslåBerregningsperioderDerHvorOpptjeningErAvslått(referanse, vilkårene, avslåttePerioder);
        }
    }

    private boolean ikkeInnvilgetEtter847(VilkårPeriode it) {
        return !(Utfall.OPPFYLT == it.getGjeldendeUtfall() && Set.of(VilkårUtfallMerknad.VM_7847_A, VilkårUtfallMerknad.VM_7847_B).contains(it.getMerknad()));
    }

    private boolean ingenBeregningsAktiviteter(OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste, VilkårPeriode it, InntektArbeidYtelseGrunnlag grunnlag, BehandlingReferanse referanse) {
        var opptjeningAktiviteter = opptjeningForBeregningTjeneste.hentEksaktOpptjeningForBeregning(referanse, grunnlag, it.getPeriode());
        return opptjeningAktiviteter.isEmpty();
    }

    private void avslåBerregningsperioderDerHvorOpptjeningErAvslått(BehandlingReferanse referanse, Vilkårene vilkårene, List<VilkårPeriode> avslåttePerioder) {

        VilkårResultatBuilder builder = Vilkårene.builderFraEksisterende(vilkårene);
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        for (VilkårPeriode vilkårPeriode : avslåttePerioder) {
            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(vilkårPeriode.getPeriode())
                .medUtfall(Utfall.IKKE_OPPFYLT)
                .medAvslagsårsak(Avslagsårsak.IKKE_TILSTREKKELIG_OPPTJENING));
        }
        builder.leggTil(vilkårBuilder);

        vilkårResultatRepository.lagre(referanse.getBehandlingId(), builder.build());
    }

    public Set<DatoIntervallEntitet> vurdertePerioder(VilkårType vilkårType, BehandlingReferanse ref) {
        var tjeneste = getPerioderTilVurderingTjeneste(ref);
        return tjeneste.utled(ref.getId(), vilkårType);
    }

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(BehandlingReferanse ref) {
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, perioderTilVurderingTjeneste, ref.getFagsakYtelseType(), ref.getBehandlingType())
            .orElseThrow(() -> new UnsupportedOperationException("VilkårsPerioderTilVurderingTjeneste ikke implementert for ytelse [" + ref.getFagsakYtelseType() + "], behandlingtype [" + ref.getBehandlingType() + "]"));
    }

    private OpptjeningForBeregningTjeneste finnOpptjeningForBeregningTjeneste(BehandlingReferanse behandlingRef) {
        FagsakYtelseType ytelseType = behandlingRef.getFagsakYtelseType();
        return FagsakYtelseTypeRef.Lookup.find(opptjeningForBeregningTjeneste, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + OpptjeningForBeregningTjeneste.class.getSimpleName() + " for ytelseType=" + ytelseType));
    }

    private List<AksjonspunktResultat> finnAksjonspunkter(Behandling behandling) {
        FagsakYtelseType ytelseType = behandling.getFagsakYtelseType();

        var tjeneste = FagsakYtelseTypeRef.Lookup.find(aksjonspunktUtledere, ytelseType);
        return tjeneste.map(utleder -> utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling))))
            .orElse(Collections.emptyList());
    }

    private void ryddVedtaksresultatForPerioderTilVurdering(BehandlingskontrollKontekst kontekst, BehandlingReferanse ref) {
        var periodeFilter = vilkårPeriodeFilterProvider.getFilter(ref);
        var allePerioder = beregningsgrunnlagVilkårTjeneste.utledDetaljertPerioderTilVurdering(ref, periodeFilter);
        var alleUnntattForlengelser = allePerioder.stream().filter(p -> !p.erForlengelse())
            .map(PeriodeTilVurdering::getPeriode)
            .collect(Collectors.toCollection(TreeSet::new));
        beregningsgrunnlagVilkårTjeneste.ryddVedtaksresultatOgVilkår(kontekst, alleUnntattForlengelser);
    }


    /**
     * Kopierer grunnlag og vilkårsresultat for forlengelser
     *
     * @param kontekst Behandlingskontrollkontekst
     * @param ref      behandlingreferanse
     */
    private void kopierGrunnlagForForlengelseperioder(BehandlingskontrollKontekst kontekst, BehandlingReferanse ref) {
        var perioderPrStartpunkt = kalkulusStartpunktUtleder.utledPerioderPrStartpunkt(ref);
        if (ref.getBehandlingType().equals(BehandlingType.REVURDERING)) {
            var periodeFilter = vilkårPeriodeFilterProvider.getFilter(ref);
            periodeFilter.ignorerAvslåttePerioder();
            var startpunktVurderRefusjon = perioderPrStartpunkt.get(VURDER_REF_BERGRUNN);
            if (!startpunktVurderRefusjon.isEmpty()) {
                log.info("Kopierer beregning for startpunkt vurder refusjon {}", startpunktVurderRefusjon);
                kalkulusTjeneste.kopier(ref, startpunktVurderRefusjon, StegType.VURDER_VILKAR_BERGRUNN);
                var originalBehandlingId = ref.getOriginalBehandlingId().orElseThrow();
                beregningsgrunnlagVilkårTjeneste.kopierVilkårresultatFraForrigeBehandling(
                    kontekst,
                    originalBehandlingId,
                    startpunktVurderRefusjon.stream().map(PeriodeTilVurdering::getPeriode).collect(Collectors.toSet()));
            }
            var startpunktKontrollerFakta = perioderPrStartpunkt.get(KONTROLLER_FAKTA_BEREGNING);
            if (!startpunktKontrollerFakta.isEmpty()) {
                log.info("Kopierer beregning for startpunkt kontroller fakta {}", startpunktKontrollerFakta);
                kalkulusTjeneste.kopier(ref, startpunktKontrollerFakta, StegType.FASTSETT_STP_BER);
            }
        }
    }

}
