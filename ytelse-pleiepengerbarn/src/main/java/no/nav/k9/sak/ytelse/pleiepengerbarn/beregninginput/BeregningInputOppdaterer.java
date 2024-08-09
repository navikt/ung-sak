package no.nav.k9.sak.ytelse.pleiepengerbarn.beregninginput;

import java.util.Arrays;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.exception.ManglerTilgangException;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.beregninginput.OverstyrInputForBeregningDto;
import no.nav.k9.sikkerhet.context.SubjectHandler;

@ApplicationScoped
@DtoTilServiceAdapter(dto = OverstyrInputForBeregningDto.class, adapter = AksjonspunktOppdaterer.class)
public class BeregningInputOppdaterer implements AksjonspunktOppdaterer<OverstyrInputForBeregningDto> {

    private BeregningInputLagreTjeneste beregningInputLagreTjeneste;
    private BeregningInputHistorikkTjeneste beregningInputHistorikkTjeneste;
    private Environment environment;

    BeregningInputOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BeregningInputOppdaterer(BeregningInputLagreTjeneste beregningInputLagreTjeneste,
                                    BeregningInputHistorikkTjeneste beregningInputHistorikkTjeneste) {
        this.beregningInputLagreTjeneste = beregningInputLagreTjeneste;
        this.beregningInputHistorikkTjeneste = beregningInputHistorikkTjeneste;
        this.environment = Environment.current();
    }

    @Override
    public OppdateringResultat oppdater(OverstyrInputForBeregningDto dto, AksjonspunktOppdaterParameter param) {
        if (!harTillatelseTilÅLøseAksjonspunkt()) {
            throw new ManglerTilgangException("K9-IF-01", "Har ikke tilgang til å løse aksjonspunkt.");
        }
        beregningInputLagreTjeneste.lagreInputOverstyringer(param.getRef(), dto);
        beregningInputHistorikkTjeneste.lagHistorikk(param.getBehandlingId());
        return OppdateringResultat.nyttResultat();
    }

    private boolean harTillatelseTilÅLøseAksjonspunkt() {
        return Arrays.stream(environment.getProperty("INFOTRYGD_MIGRERING_TILLATELSER", "").split(",")).anyMatch(id -> id.equalsIgnoreCase(SubjectHandler.getSubjectHandler().getUid()));
    }

}
