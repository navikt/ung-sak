package no.nav.k9.sak.inngangsvilkår.verge;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.person.AvklarVergeDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarVergeDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarVerge implements AksjonspunktOppdaterer<AvklarVergeDto>  {

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;

    public AvklarVerge() {
    }

    @Inject
    public AvklarVerge(HistorikkTjenesteAdapter historikkTjenesteAdapter) {
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
    }

    @Override
    public OppdateringResultat oppdater(AvklarVergeDto dto, AksjonspunktOppdaterParameter param) {
        historikkTjenesteAdapter.tekstBuilder()
            .medHendelse(HistorikkinnslagType.REGISTRER_OM_VERGE)
            .medBegrunnelse(dto.getBegrunnelse())
            .medSkjermlenke(SkjermlenkeType.FAKTA_OM_VERGE);
        return OppdateringResultat.nyttResultat();
    }

}
