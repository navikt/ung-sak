package no.nav.k9.sak.ytelse.pleiepengerbarn.prosess;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.BehandlingModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.impl.BehandlingModellImpl;

@ApplicationScoped
public class ProsessModell {

    private static final String YTELSE = "PSB";
    private static final FagsakYtelseType YTELSE_TYPE = FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

    @FagsakYtelseTypeRef(YTELSE)
    @BehandlingTypeRef("BT-002")
    @Produces
    @ApplicationScoped
    public BehandlingModell førstegangsbehandling() {
        var modellBuilder = BehandlingModellImpl.builder(BehandlingType.FØRSTEGANGSSØKNAD, YTELSE_TYPE);
        modellBuilder.medSteg(
            BehandlingStegType.START_STEG,
            BehandlingStegType.VURDER_UTLAND,
            BehandlingStegType.VURDER_KOMPLETTHET,
            BehandlingStegType.INIT_PERIODER,
            BehandlingStegType.INIT_VILKÅR,
            BehandlingStegType.INNHENT_REGISTEROPP,
            BehandlingStegType.INREG_AVSL,
            BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD,
            BehandlingStegType.KONTROLLER_FAKTA,
            BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT,
            BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR,
            BehandlingStegType.VURDER_OMSORG_FOR,
            BehandlingStegType.VURDER_MEDISINSKVILKÅR,
            BehandlingStegType.FASTSETT_OPPTJENINGSPERIODE,
            BehandlingStegType.VURDER_OPPTJENING_FAKTA,
            BehandlingStegType.VURDER_OPPTJENINGSVILKÅR,
            BehandlingStegType.KONTROLLER_FAKTA_UTTAK,
            BehandlingStegType.VURDER_UTTAK,
            BehandlingStegType.PRECONDITION_BEREGNING,
            BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING,
            BehandlingStegType.KONTROLLER_FAKTA_BEREGNING,
            BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG,
            BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG,
            BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG,
            BehandlingStegType.BEREGN_YTELSE,
            BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT,
            BehandlingStegType.SIMULER_OPPDRAG,
            BehandlingStegType.VURDER_FARESIGNALER,
            BehandlingStegType.FORESLÅ_VEDTAK,
            BehandlingStegType.FATTE_VEDTAK,
            BehandlingStegType.IVERKSETT_VEDTAK);
        return modellBuilder.build();
    }

    @FagsakYtelseTypeRef(YTELSE)
    @BehandlingTypeRef("BT-004")
    @Produces
    @ApplicationScoped
    public BehandlingModell revurdering() {
        var modellBuilder = BehandlingModellImpl.builder(BehandlingType.REVURDERING, YTELSE_TYPE);
        modellBuilder.medSteg(
            BehandlingStegType.START_STEG,
            BehandlingStegType.VARSEL_REVURDERING,
            BehandlingStegType.VURDER_UTLAND,
            BehandlingStegType.VURDER_KOMPLETTHET,
            BehandlingStegType.INIT_PERIODER,
            BehandlingStegType.INIT_VILKÅR,
            BehandlingStegType.INNHENT_REGISTEROPP,
            BehandlingStegType.INREG_AVSL,
            BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD,
            BehandlingStegType.KONTROLLER_FAKTA,
            BehandlingStegType.KONTROLLERER_SØKERS_OPPLYSNINGSPLIKT,
            BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR,
            BehandlingStegType.VURDER_OMSORG_FOR,
            BehandlingStegType.VURDER_MEDISINSKVILKÅR,
            BehandlingStegType.FASTSETT_OPPTJENINGSPERIODE,
            BehandlingStegType.VURDER_OPPTJENING_FAKTA,
            BehandlingStegType.VURDER_OPPTJENINGSVILKÅR,
            BehandlingStegType.KONTROLLER_FAKTA_UTTAK,
            BehandlingStegType.VURDER_UTTAK,
            BehandlingStegType.PRECONDITION_BEREGNING,
            BehandlingStegType.FASTSETT_SKJÆRINGSTIDSPUNKT_BEREGNING,
            BehandlingStegType.KONTROLLER_FAKTA_BEREGNING,
            BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG,
            BehandlingStegType.FORDEL_BEREGNINGSGRUNNLAG,
            BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG,
            BehandlingStegType.BEREGN_YTELSE,
            BehandlingStegType.VURDER_TILBAKETREKK,
            BehandlingStegType.HINDRE_TILBAKETREKK,
            BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT,
            BehandlingStegType.SIMULER_OPPDRAG,
            BehandlingStegType.FORESLÅ_VEDTAK,
            BehandlingStegType.FATTE_VEDTAK,
            BehandlingStegType.IVERKSETT_VEDTAK);
        return modellBuilder.build();
    }

}
