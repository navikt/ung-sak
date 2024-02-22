package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.PRECONDITION_BEREGNING;

import java.util.Collections;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningInkonsistensTjeneste;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.vilkår.VilkårPeriodeFilterProvider;

@FagsakYtelseTypeRef
@BehandlingStegRef(value = PRECONDITION_BEREGNING)
@BehandlingTypeRef
@ApplicationScoped
public class VurderPreconditionBeregningSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private Instance<PreconditionBeregningAksjonspunktUtleder> aksjonspunktUtledere;
    private VurderAvslagGrunnetOpptjening vurderAvslagGrunnetOpptjening;
    private RyddOgGjenopprettBeregningTjeneste ryddOgGjenopprettBeregningTjeneste;
    private KopierBeregningTjeneste kopierBeregningTjeneste;
    private BeregningInkonsistensTjeneste inkonsistensTjeneste;
    private OpptjeningsaktiviteterPreconditionForBeregning opptjeningsaktiviteterPreconditionForBeregning;


    protected VurderPreconditionBeregningSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderPreconditionBeregningSteg(BehandlingRepository behandlingRepository,
                                           BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                           @Any Instance<PreconditionBeregningAksjonspunktUtleder> aksjonspunktUtledere,
                                           VurderAvslagGrunnetOpptjening vurderAvslagGrunnetOpptjening,
                                           RyddOgGjenopprettBeregningTjeneste ryddOgGjenopprettBeregningTjeneste,
                                           KopierBeregningTjeneste kopierBeregningTjeneste,
                                           BeregningInkonsistensTjeneste inkonsistensTjeneste,
                                           OpptjeningsaktiviteterPreconditionForBeregning opptjeningsaktiviteterPreconditionForBeregning) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.aksjonspunktUtledere = aksjonspunktUtledere;
        this.vurderAvslagGrunnetOpptjening = vurderAvslagGrunnetOpptjening;
        this.ryddOgGjenopprettBeregningTjeneste = ryddOgGjenopprettBeregningTjeneste;
        this.kopierBeregningTjeneste = kopierBeregningTjeneste;
        this.inkonsistensTjeneste = inkonsistensTjeneste;
        this.opptjeningsaktiviteterPreconditionForBeregning = opptjeningsaktiviteterPreconditionForBeregning;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var referanse = BehandlingReferanse.fra(behandling);

        var perioderTilVurdering = beregningsgrunnlagVilkårTjeneste.utledDetaljertPerioderTilVurdering(referanse);

        // 1. Rydder
        ryddOgGjenopprettBeregningTjeneste.ryddOgGjenopprett(kontekst, perioderTilVurdering);

        // 2. Avslå der opptjening er avslått
        vurderAvslagGrunnetOpptjening.vurderAvslagGrunnetAvslagIOpptjening(referanse, perioderTilVurdering);

        // 3. fjern eller initier perioder fra definerende vilkår
        ryddOgGjenopprettBeregningTjeneste.fjernEllerInitierPerioderFraDefinerendeVilkår(referanse);

        // 4. Rydder alle perioder ulik initiell
        ryddOgGjenopprettBeregningTjeneste.deaktiverAlleReferanserUlikInitiell(referanse);

        // 5 Vurder inkonsistens
        inkonsistensTjeneste.sjekkInkonsistensOgOpprettProsesstrigger(referanse, perioderTilVurdering);
        opptjeningsaktiviteterPreconditionForBeregning.sjekkOpptjeningsaktiviter(referanse, perioderTilVurdering);

        // 6. Kopier
        kopierBeregningTjeneste.kopierVurderinger(kontekst, perioderTilVurdering);


        // 7. Utled aksjonspunkt
        var aksjonspunkter = finnAksjonspunkter(referanse);

        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
    }


    private List<AksjonspunktResultat> finnAksjonspunkter(BehandlingReferanse behandlingReferanse) {
        FagsakYtelseType ytelseType = behandlingReferanse.getFagsakYtelseType();

        var tjeneste = FagsakYtelseTypeRef.Lookup.find(aksjonspunktUtledere, ytelseType);
        return tjeneste.map(utleder -> utleder.utledAksjonspunkterFor(new AksjonspunktUtlederInput(behandlingReferanse)))
            .orElse(Collections.emptyList());
    }


}
