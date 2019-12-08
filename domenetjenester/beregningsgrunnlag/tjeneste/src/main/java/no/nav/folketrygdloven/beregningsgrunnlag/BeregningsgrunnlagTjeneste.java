package no.nav.folketrygdloven.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.felles.BeregningsgrunnlagDiffSjekker;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetOverstyringerEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagTilstand;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningsgrunnlagRegelResultat;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSats;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningSatsType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;

/**
 * Fasade tjeneste for å delegere alle kall fra steg
 */
@ApplicationScoped
public class BeregningsgrunnlagTjeneste {

    private static final String UTVIKLER_FEIL_SKAL_HA_BEREGNINGSGRUNNLAG_HER = "Utvikler-feil: skal ha beregningsgrunnlag her";

    private BeregningsgrunnlagAksjonspunktUtleder aksjonspunktUtlederFaktaOmBeregning;

    private Instance<AksjonspunktUtlederFastsettBeregningsaktiviteter> aksjonspunktUtlederFastsettBeregningsaktiviteter;
    private AksjonspunktUtlederFordelBeregning aksjonspunktUtlederFordelBeregningsgrunnlag;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private FastsettBeregningAktiviteter fastsettBeregningAktiviteter = new FastsettBeregningAktiviteter();
    private FordelBeregningsgrunnlagTjeneste fordelBeregningsgrunnlagTjeneste;
    private ForeslåBeregningsgrunnlag foreslåBeregningsgrunnlag;
    private Instance<FullføreBeregningsgrunnlag> fullføreBeregningsgrunnlag;

    private OpprettBeregningsgrunnlagTjeneste opprettBeregningsgrunnlagTjeneste;
    private VurderBeregningsgrunnlagTjeneste vurderBeregningsgrunnlagTjeneste;

    public BeregningsgrunnlagTjeneste() {
        // CDI Proxy
    }

    @Inject
    public BeregningsgrunnlagTjeneste(BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                      @Any Instance<FullføreBeregningsgrunnlag> fullføreBeregningsgrunnlag,
                                      ForeslåBeregningsgrunnlag foreslåBeregningsgrunnlag,
                                      BeregningsgrunnlagAksjonspunktUtleder aksjonspunktUtlederFaktaOmBeregning,
                                      @Any Instance<AksjonspunktUtlederFastsettBeregningsaktiviteter> aksjonspunktUtlederFastsettBeregningsaktiviteter,
                                      OpprettBeregningsgrunnlagTjeneste opprettBeregningsgrunnlagTjeneste,
                                      AksjonspunktUtlederFordelBeregning aksjonspunktUtlederFordelBeregningsgrunnlag,
                                      FordelBeregningsgrunnlagTjeneste fordelBeregningsgrunnlagTjeneste,
                                      VurderBeregningsgrunnlagTjeneste vurderBeregningsgrunnlagTjeneste) {
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.fullføreBeregningsgrunnlag = fullføreBeregningsgrunnlag;
        this.foreslåBeregningsgrunnlag = foreslåBeregningsgrunnlag;
        this.aksjonspunktUtlederFaktaOmBeregning = aksjonspunktUtlederFaktaOmBeregning;
        this.aksjonspunktUtlederFastsettBeregningsaktiviteter = aksjonspunktUtlederFastsettBeregningsaktiviteter;
        this.opprettBeregningsgrunnlagTjeneste = opprettBeregningsgrunnlagTjeneste;
        this.fordelBeregningsgrunnlagTjeneste = fordelBeregningsgrunnlagTjeneste;
        this.aksjonspunktUtlederFordelBeregningsgrunnlag = aksjonspunktUtlederFordelBeregningsgrunnlag;
        this.vurderBeregningsgrunnlagTjeneste = vurderBeregningsgrunnlagTjeneste;
    }

    public List<BeregningAksjonspunktResultat> fastsettBeregningsaktiviteter(BeregningsgrunnlagInput input) {
        var ref = input.getBehandlingReferanse();
        FagsakYtelseType ytelseType = input.getFagsakYtelseType();

        BeregningAktivitetAggregatEntitet beregningAktivitetAggregat = fastsettBeregningAktiviteter.fastsettAktiviteter(input);
        BeregningsgrunnlagEntitet beregningsgrunnlag = opprettBeregningsgrunnlagTjeneste.fastsettSkjæringstidspunktOgStatuser(ref, beregningAktivitetAggregat, input.getIayGrunnlag());

        Optional<BeregningAktivitetOverstyringerEntitet> overstyrt = hentTidligereOverstyringer(ref);
        BeregningsgrunnlagGrunnlagBuilder builder = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty())
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .medRegisterAktiviteter(beregningAktivitetAggregat)
            .medOverstyring(overstyrt.orElse(null));

        var beregningsgrunnlagGrunnlag = beregningsgrunnlagRepository.lagre(ref.getBehandlingId(), builder, BeregningsgrunnlagTilstand.OPPRETTET);

        boolean erOverstyrt = overstyrt.isPresent();
        var aksjonspunktUtleder = FagsakYtelseTypeRef.Lookup.find(aksjonspunktUtlederFastsettBeregningsaktiviteter, ytelseType).orElseThrow();

        return aksjonspunktUtleder.utledAksjonspunkterFor(
            ref,
            beregningsgrunnlag,
            beregningAktivitetAggregat,
            erOverstyrt,
            input.medBeregningsgrunnlagGrunnlag(beregningsgrunnlagGrunnlag));
    }

    public void fastsettBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        var newInput = lagInputMedBeregningsgrunnlag(input);
        var ytelseType = input.getFagsakYtelseType();
        var fullføre = FagsakYtelseTypeRef.Lookup.find(fullføreBeregningsgrunnlag, ytelseType).orElseThrow();
        BeregningsgrunnlagEntitet fastsattBeregningsgrunnlag = fullføre.fullføreBeregningsgrunnlag(newInput, newInput.getBeregningsgrunnlagGrunnlag());

        Long behandlingId = input.getBehandlingReferanse().getBehandlingId();
        beregningsgrunnlagRepository.lagre(behandlingId, fastsattBeregningsgrunnlag, BeregningsgrunnlagTilstand.FASTSATT);
    }

    public BeregningSats finnEksaktSats(BeregningSatsType satsType, LocalDate dato) {
        return beregningsgrunnlagRepository.finnEksaktSats(satsType, dato);
    }

    public BeregningsgrunnlagRegelResultat fordelBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        var newInput = lagInputMedBeregningsgrunnlag(input);
        BeregningsgrunnlagRegelResultat vilkårVurderingResultat = vurderBeregningsgrunnlagTjeneste.vurderBeregningsgrunnlag(newInput, newInput.getBeregningsgrunnlagGrunnlag());
        BeregningsgrunnlagEntitet vurdertBeregningsgrunnlag = vilkårVurderingResultat.getBeregningsgrunnlag();
        Long behandlingId = input.getBehandlingReferanse().getBehandlingId();
        if (Boolean.FALSE.equals(vilkårVurderingResultat.getVilkårOppfylt())) {
            beregningsgrunnlagRepository.lagre(behandlingId, vurdertBeregningsgrunnlag, BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING);
            return vilkårVurderingResultat;
        } else {
            var fordeltBeregningsgrunnlag = fordelBeregningsgrunnlagTjeneste.fordelBeregningsgrunnlag(newInput, vurdertBeregningsgrunnlag);
            BeregningsgrunnlagGrunnlagEntitet nyttGrunnlag = BeregningsgrunnlagGrunnlagBuilder.oppdatere(beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(behandlingId))
                .medBeregningsgrunnlag(fordeltBeregningsgrunnlag)
                .build(behandlingId, BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING);
            List<BeregningAksjonspunktResultat> aksjonspunkter = aksjonspunktUtlederFordelBeregningsgrunnlag.utledAksjonspunkterFor(
                newInput.getBehandlingReferanse(),
                nyttGrunnlag,
                newInput.getAktivitetGradering(),
                input.getInntektsmeldinger());
            kopierBeregningsgrunnlagFraForrigeOmMulig(input, behandlingId, aksjonspunkter, fordeltBeregningsgrunnlag, BeregningsgrunnlagTilstand.OPPDATERT_MED_REFUSJON_OG_GRADERING, BeregningsgrunnlagTilstand.FASTSATT_INN);
            BeregningsgrunnlagRegelResultat beregningsgrunnlagRegelResultat = new BeregningsgrunnlagRegelResultat(fordeltBeregningsgrunnlag, aksjonspunkter);
            beregningsgrunnlagRegelResultat.setVilkårOppfylt(vilkårVurderingResultat.getVilkårOppfylt());
            return beregningsgrunnlagRegelResultat;
        }
    }

    public BeregningsgrunnlagRegelResultat foreslåBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        Long behandlingId = input.getBehandlingReferanse().getBehandlingId();
        var newInput = lagInputMedBeregningsgrunnlag(input);
        BeregningsgrunnlagRegelResultat resultat = foreslåBeregningsgrunnlag.foreslåBeregningsgrunnlag(newInput, newInput.getBeregningsgrunnlagGrunnlag());
        beregningsgrunnlagRepository.lagre(behandlingId, resultat.getBeregningsgrunnlag(), BeregningsgrunnlagTilstand.FORESLÅTT);
        return resultat;
    }

    public RyddBeregningsgrunnlag getRyddBeregningsgrunnlag(BehandlingskontrollKontekst kontekst) {
        return new RyddBeregningsgrunnlag(beregningsgrunnlagRepository, kontekst);
    }

    public List<BeregningAksjonspunktResultat> kontrollerFaktaBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        var ref = input.getBehandlingReferanse();
        BeregningsgrunnlagGrunnlagEntitet grunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(ref.getBehandlingId())
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Fant ikke beregningsgrunnlagEntitet for behandling " + ref.getBehandlingId()));

        var nyInput = input.medBeregningsgrunnlagGrunnlag(grunnlag);
        BeregningsgrunnlagEntitet beregningsgrunnlag = opprettBeregningsgrunnlagTjeneste.opprettOgLagreBeregningsgrunnlag(nyInput);

        BeregningsgrunnlagGrunnlagEntitet lagretGrunnlag = beregningsgrunnlagRepository.lagre(ref.getBehandlingId(), beregningsgrunnlag,
            BeregningsgrunnlagTilstand.OPPDATERT_MED_ANDELER);
        return aksjonspunktUtlederFaktaOmBeregning.utledAksjonspunkterFor(nyInput, lagretGrunnlag, harOverstyrtBergningsgrunnlag(nyInput.getBehandlingReferanse()));
    }

    public void kopierBeregningsresultatFraOriginalBehandling(Long originalBehandlingId, Long behandlingId) {
        beregningsgrunnlagRepository.kopierGrunnlagFraEksisterendeBehandling(originalBehandlingId, behandlingId, BeregningsgrunnlagTilstand.FASTSATT);
    }

    public List<BeregningAksjonspunktResultat> utledAksjonspunkterForFordelBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        var newInput = lagInputMedBeregningsgrunnlag(input);
        return aksjonspunktUtlederFordelBeregningsgrunnlag.utledAksjonspunkterFor(newInput.getBehandlingReferanse(), newInput.getBeregningsgrunnlagGrunnlag(), newInput.getAktivitetGradering(), input.getInntektsmeldinger());
    }

    private boolean harOverstyrtBergningsgrunnlag(BehandlingReferanse ref) {
        return beregningsgrunnlagRepository
            .hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(ref.getBehandlingId(), ref.getOriginalBehandlingId(),
                BeregningsgrunnlagTilstand.KOFAKBER_UT)
            .filter(grunnlagEntitet -> grunnlagEntitet
                .getBeregningsgrunnlag()
                .map(BeregningsgrunnlagEntitet::isOverstyrt).orElse(false))
            .isPresent();
    }

    private Optional<BeregningAktivitetOverstyringerEntitet> hentTidligereOverstyringer(BehandlingReferanse ref) {
        Optional<BeregningsgrunnlagGrunnlagEntitet> overstyrtGrunnlag = beregningsgrunnlagRepository
            .hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(ref.getBehandlingId(), ref.getOriginalBehandlingId(),
                BeregningsgrunnlagTilstand.FASTSATT_BEREGNINGSAKTIVITETER);
        return overstyrtGrunnlag
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getOverstyring);
    }

    private BeregningsgrunnlagInput lagInputMedBeregningsgrunnlag(BeregningsgrunnlagInput input) {
        Long behandlingId = input.getBehandlingReferanse().getBehandlingId();
        BeregningsgrunnlagGrunnlagEntitet grunnlagEntitet = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(behandlingId)
            .orElseThrow(() -> new IllegalStateException(UTVIKLER_FEIL_SKAL_HA_BEREGNINGSGRUNNLAG_HER));
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlagEntitet.getBeregningsgrunnlag()
            .orElseThrow(() -> new IllegalStateException(UTVIKLER_FEIL_SKAL_HA_BEREGNINGSGRUNNLAG_HER));
        var ref = oppdaterBehandlingreferanseMedSkjæringstidspunktBeregning(input.getBehandlingReferanse(), grunnlagEntitet.getGjeldendeAktiviteter(), beregningsgrunnlag);

        return input
                .medBehandlingReferanse(ref)
                .medBeregningsgrunnlagGrunnlag(grunnlagEntitet);
    }

    private BehandlingReferanse oppdaterBehandlingreferanseMedSkjæringstidspunktBeregning(BehandlingReferanse ref,
                                                                                          BeregningAktivitetAggregatEntitet beregningAktivitetAggregat,
                                                                                          BeregningsgrunnlagEntitet beregningsgrunnlag) {
        Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder()
            .medUtledetSkjæringstidspunkt(ref.getSkjæringstidspunkt().getUtledetSkjæringstidspunkt())
            .medSkjæringstidspunktOpptjening(beregningAktivitetAggregat.getSkjæringstidspunktOpptjening())
            .medSkjæringstidspunktBeregning(beregningsgrunnlag.getSkjæringstidspunkt()).build();
        return ref.medSkjæringstidspunkt(skjæringstidspunkt);
    }

    /**
     * Kun til test, overgang til ft-kalkulus.
     */
    public void lagreBeregningsgrunnlag(Long behandlingId, BeregningsgrunnlagEntitet beregningsgrunnlag, BeregningsgrunnlagTilstand tilstand) {
        beregningsgrunnlagRepository.lagre(behandlingId, beregningsgrunnlag, tilstand);
    }

    private void kopierBeregningsgrunnlagFraForrigeOmMulig(BeregningsgrunnlagInput input, Long behandlingId, List<BeregningAksjonspunktResultat> aksjonspunkter,
                                                           BeregningsgrunnlagEntitet nyttBg, BeregningsgrunnlagTilstand tilstandForSteg, BeregningsgrunnlagTilstand bekreftetTilstand) {
        Optional<BeregningsgrunnlagEntitet> forrigeBeregningsgrunnlag = beregningsgrunnlagRepository
            .hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(behandlingId, input.getBehandlingReferanse().getOriginalBehandlingId(), tilstandForSteg)
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
        boolean kanKopiereFraBekreftet = kanKopiereBeregningsgrunnlag(aksjonspunkter, nyttBg, forrigeBeregningsgrunnlag);
        if (kanKopiereFraBekreftet) {
            Optional<BeregningsgrunnlagEntitet> forrigeBekreftetBeregningsgrunnlag = beregningsgrunnlagRepository
                .hentSisteBeregningsgrunnlagGrunnlagEntitetForBehandlinger(behandlingId, input.getBehandlingReferanse().getOriginalBehandlingId(), bekreftetTilstand)
                .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
            forrigeBekreftetBeregningsgrunnlag.ifPresentOrElse(bg ->
                    beregningsgrunnlagRepository.lagre(behandlingId, bg, tilstandForSteg),
                () -> beregningsgrunnlagRepository.lagre(behandlingId, nyttBg, tilstandForSteg));
        } else {
            beregningsgrunnlagRepository.lagre(behandlingId, nyttBg, tilstandForSteg);
        }
    }

    private boolean kanKopiereBeregningsgrunnlag(List<BeregningAksjonspunktResultat> aksjonspunkter, BeregningsgrunnlagEntitet nyttBg, Optional<BeregningsgrunnlagEntitet> forrigeBeregningsgrunnlag) {
        return forrigeBeregningsgrunnlag.map(bg -> !BeregningsgrunnlagDiffSjekker.harSignifikantDiffIBeregningsgrunnlag(nyttBg, bg)).orElse(false) && !aksjonspunkter.isEmpty();
    }

}
