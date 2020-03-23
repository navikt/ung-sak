package no.nav.k9.sak.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusTjeneste;
import no.nav.folketrygdloven.kalkulus.håndtering.v1.HåndterBeregningDto;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.FastsettBeregningsgrunnlagATFLDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FastsettBeregningsgrunnlagATFLDto.class, adapter = AksjonspunktOppdaterer.class)
public class FastsettBeregningsgrunnlagATFLOppdaterer implements AksjonspunktOppdaterer<FastsettBeregningsgrunnlagATFLDto> {

    private KalkulusTjeneste kalkulusTjeneste;

    protected FastsettBeregningsgrunnlagATFLOppdaterer () {
        // CDI
    }

    @Inject
    public FastsettBeregningsgrunnlagATFLOppdaterer(KalkulusTjeneste kalkulusTjeneste) {
        this.kalkulusTjeneste = kalkulusTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(FastsettBeregningsgrunnlagATFLDto dto, AksjonspunktOppdaterParameter param) {

        HåndterBeregningDto håndterBeregningDto = MapDtoTilRequest.map(dto);
        var resultat = kalkulusTjeneste.oppdaterBeregning(håndterBeregningDto, param.getRef());
        // TODO FIKS HISTORIKK

        OppdateringResultat.Builder builder = OppdateringResultat.utenTransisjon();
        håndterEventueltOverflødigAksjonspunkt(param.getBehandling())
            .ifPresent(ap -> builder.medEkstraAksjonspunktResultat(ap.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));
        return builder.build();
    }

    /*
    Ved tilbakehopp hender det at avbrutte aksjonspunkter gjenopprettes feilaktig. Denne funksjonen sørger for å rydde opp
    i dette for det ene scenarioet det er mulig i beregningsmodulen. Også implementert i det motsatte tilfellet.
    Se https://jira.adeo.no/browse/PFP-2042 for mer informasjon.
     */
    private Optional<Aksjonspunkt> håndterEventueltOverflødigAksjonspunkt(Behandling behandling) {
        return behandling.getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_TIDSBEGRENSET_ARBEIDSFORHOLD);
    }
}
