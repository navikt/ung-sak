package no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.HentBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.FastsettBeregningsgrunnlagATFLHåndterer;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.web.app.tjenester.behandling.beregningsgrunnlag.historikk.FastsettBeregningsgrunnlagATFLHistorikkTjeneste;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FastsettBeregningsgrunnlagATFLDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FastsettBeregningsgrunnlagATFLDto.class, adapter = AksjonspunktOppdaterer.class)
public class FastsettBeregningsgrunnlagATFLOppdaterer implements AksjonspunktOppdaterer<FastsettBeregningsgrunnlagATFLDto> {

    private HentBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private FastsettBeregningsgrunnlagATFLHistorikkTjeneste fastsettBeregningsgrunnlagATFLHistorikkTjeneste;
    private FastsettBeregningsgrunnlagATFLHåndterer fastsettBeregningsgrunnlagATFLHåndterer;

    protected FastsettBeregningsgrunnlagATFLOppdaterer () {
        // CDI
    }

    @Inject
    public FastsettBeregningsgrunnlagATFLOppdaterer(HentBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                    FastsettBeregningsgrunnlagATFLHistorikkTjeneste fastsettBeregningsgrunnlagATFLHistorikkTjeneste,
                                                    FastsettBeregningsgrunnlagATFLHåndterer fastsettBeregningsgrunnlagATFLHåndterer) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.fastsettBeregningsgrunnlagATFLHistorikkTjeneste = fastsettBeregningsgrunnlagATFLHistorikkTjeneste;
        this.fastsettBeregningsgrunnlagATFLHåndterer = fastsettBeregningsgrunnlagATFLHåndterer;
    }

    @Override
    public OppdateringResultat oppdater(FastsettBeregningsgrunnlagATFLDto dto, AksjonspunktOppdaterParameter param) {

        BeregningsgrunnlagEntitet forrigeGrunnlag = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagAggregatForBehandling(param.getBehandlingId());
        fastsettBeregningsgrunnlagATFLHåndterer.håndter(param.getBehandlingId(), dto);
        fastsettBeregningsgrunnlagATFLHistorikkTjeneste.lagHistorikk(param, dto, forrigeGrunnlag);

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
