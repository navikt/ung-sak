package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.PRECONDITION_BEREGNING;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningForBeregningTjeneste;
import no.nav.folketrygdloven.kalkulus.kodeverk.StegType;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
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
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
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
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private boolean skalVentePåRegelendring;


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
                                           SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                           @KonfigVerdi(value = "VENT_REGELENDRING_8_41", defaultVerdi = "false") boolean skalVentePåRegelendring) {
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
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.skalVentePåRegelendring = skalVentePåRegelendring;
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

        // 6. kopierer input overstyringer for migrering fra infotrygd
        kopierInputOverstyring(behandling);

        // 7. Dekativerer PGI-periode dersom ikke lenger relevant
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

        if (skalVentePåRegelendring && måSettesPåVentGrunnetPåventendeRegelendring_8_41(behandling)) {
            return Collections.singletonList(AksjonspunktResultat.opprettForAksjonspunktMedFrist(AksjonspunktDefinisjon.AUTO_VENT_PÅ_LOVENDRING_8_41,
                Venteårsak.VENT_LOVENDRING_8_41, LocalDateTime.of(LocalDate.of(2023, 1, 3), LocalDateTime.now().toLocalTime())));
        }


        var tjeneste = FagsakYtelseTypeRef.Lookup.find(aksjonspunktUtledere, ytelseType);
        return tjeneste.map(utleder -> utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(BehandlingReferanse.fra(behandling))))
            .orElse(Collections.emptyList());
    }

    private boolean måSettesPåVentGrunnetPåventendeRegelendring_8_41(Behandling behandling) {
        // Sett påvirkede saker på vent midlertidig til ny lov om 8-41 er vedtatt.
        // Dette gjelder saker som beregning som næringsdrivende samt enten frilans eller arbeid (eller begge)
        // og har skjæringstidspunkt beregning likt 1.1.2023 eller senere
        var skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId());
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);
        var periodeFilter = vilkårPeriodeFilterProvider.getFilter(ref);
        periodeFilter.ignorerAvslåttePerioder().ignorerForlengelseperioder();

        var perioderTilVurdering = beregningsgrunnlagVilkårTjeneste.utledDetaljertPerioderTilVurdering(ref, periodeFilter);
        var iayGrunnlag = iayTjeneste.hentGrunnlag(ref.getBehandlingId());
        return perioderTilVurdering.stream().anyMatch(p -> skalSettesPåVent(p, ref, iayGrunnlag));
    }

    private boolean skalSettesPåVent(PeriodeTilVurdering p, BehandlingReferanse ref, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var opptjeningsperioder = finnOpptjeningForBeregningTjeneste(ref).hentEksaktOpptjeningForBeregning(ref, iayGrunnlag, p.getPeriode())
            .map(OpptjeningAktiviteter::getOpptjeningPerioder).orElse(Collections.emptyList());
        return SkalVentePåRegelendring_8_41.kanPåvirkesAvRegelendring(p.getSkjæringstidspunkt(), opptjeningsperioder);
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
        if (ref.getBehandlingType().equals(BehandlingType.REVURDERING)) {
            var periodeFilter = vilkårPeriodeFilterProvider.getFilter(ref);
            periodeFilter.ignorerAvslåttePerioder();
            var allePerioder = beregningsgrunnlagVilkårTjeneste.utledDetaljertPerioderTilVurdering(ref, periodeFilter);
            var forlengelseperioder = allePerioder.stream().filter(PeriodeTilVurdering::erForlengelse).collect(Collectors.toSet());
            if (!forlengelseperioder.isEmpty()) {
                log.info("Kopierer beregning for forlengelser {}", forlengelseperioder);
                kalkulusTjeneste.kopier(ref, forlengelseperioder, StegType.VURDER_VILKAR_BERGRUNN);
                var originalBehandlingId = ref.getOriginalBehandlingId().orElseThrow();
                beregningsgrunnlagVilkårTjeneste.kopierVilkårresultatFraForrigeBehandling(
                    kontekst,
                    originalBehandlingId,
                    forlengelseperioder.stream().map(PeriodeTilVurdering::getPeriode).collect(Collectors.toSet()));
            }
        }
    }


}
