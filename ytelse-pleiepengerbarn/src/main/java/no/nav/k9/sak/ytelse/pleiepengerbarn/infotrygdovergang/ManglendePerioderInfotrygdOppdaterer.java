package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang;

import java.util.Arrays;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.felles.exception.ManglerTilgangException;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.infotrygd.ManglendePeriodeBekreftDto;
import no.nav.k9.sikkerhet.oidc.token.bruker.BrukerTokenProvider;

@ApplicationScoped
@DtoTilServiceAdapter(dto = ManglendePeriodeBekreftDto.class, adapter = AksjonspunktOppdaterer.class)
public class ManglendePerioderInfotrygdOppdaterer implements AksjonspunktOppdaterer<ManglendePeriodeBekreftDto>  {

    private Environment environment;
    private BrukerTokenProvider brukerTokenProvider;
    private HistorikkTjenesteAdapter historikkTjenesteAdapter;


    public ManglendePerioderInfotrygdOppdaterer() {
    }

    @Inject
    public ManglendePerioderInfotrygdOppdaterer(BrukerTokenProvider brukerTokenProvider, HistorikkTjenesteAdapter historikkTjenesteAdapter) {
        this.brukerTokenProvider = brukerTokenProvider;
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.environment = Environment.current();
    }

    @Override
    public OppdateringResultat oppdater(ManglendePeriodeBekreftDto dto, AksjonspunktOppdaterParameter param) {
        if (!harTillatelseTilÅLøseAksjonspunkt()) {
            throw new ManglerTilgangException("K9-IF-01", "Har ikke tilgang til å løse aksjonspunkt.");
        }
        historikkTjenesteAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse())
            .medSkjermlenke(SkjermlenkeType.INFOTRYGD_MIGRERING);
        return OppdateringResultat.utenOverhopp();
    }

    private boolean harTillatelseTilÅLøseAksjonspunkt() {
        return Arrays.stream(environment.getProperty("INFOTRYGD_MIGRERING_TILLATELSER", "").split(",")).anyMatch(id -> id.equalsIgnoreCase(brukerTokenProvider.getUserId()));
    }

}
