package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang;

import java.util.Arrays;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.exception.ManglerTilgangException;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.infotrygd.ManglendePeriodeAnnenPartBekreftDto;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.k9.sikkerhet.oidc.token.context.ContextAwareTokenProvider;

@ApplicationScoped
@DtoTilServiceAdapter(dto = ManglendePeriodeAnnenPartBekreftDto.class, adapter = AksjonspunktOppdaterer.class)
public class ManglendePerioderAnnenPartInfotrygdOppdaterer implements AksjonspunktOppdaterer<ManglendePeriodeAnnenPartBekreftDto> {


    private Environment environment;
    private HistorikkTjenesteAdapter historikkTjenesteAdapter;

    public ManglendePerioderAnnenPartInfotrygdOppdaterer() {
    }

    @Inject
    public ManglendePerioderAnnenPartInfotrygdOppdaterer(HistorikkTjenesteAdapter historikkTjenesteAdapter) {
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.environment = Environment.current();
    }

    @Override
    public OppdateringResultat oppdater(ManglendePeriodeAnnenPartBekreftDto dto, AksjonspunktOppdaterParameter param) {
        if (!harTillatelseTilÅLøseAksjonspunkt()) {
            throw new ManglerTilgangException("K9-IF-01", "Har ikke tilgang til å løse aksjonspunkt.");
        }
        historikkTjenesteAdapter.tekstBuilder()
            .medBegrunnelse(dto.getBegrunnelse())
            .medSkjermlenke(SkjermlenkeType.INFOTRYGD_MIGRERING);
        return OppdateringResultat.nyttResultat();
    }

    private boolean harTillatelseTilÅLøseAksjonspunkt() {
        return Arrays.stream(environment.getProperty("INFOTRYGD_MIGRERING_TILLATELSER", "").split(",")).anyMatch(id -> id.equalsIgnoreCase(SubjectHandler.getSubjectHandler().getUid()));
    }

}
