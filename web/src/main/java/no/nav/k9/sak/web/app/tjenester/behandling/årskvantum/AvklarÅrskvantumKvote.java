package no.nav.k9.sak.web.app.tjenester.behandling.årskvantum;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.kontrakt.uttak.AvklarÅrskvantumDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = AvklarÅrskvantumDto.class, adapter = AksjonspunktOppdaterer.class)
public class AvklarÅrskvantumKvote implements AksjonspunktOppdaterer<AvklarÅrskvantumDto> {


    AvklarÅrskvantumKvote() {
        // for CDI proxy
    }


    @Override
    public OppdateringResultat oppdater(AvklarÅrskvantumDto dto, AksjonspunktOppdaterParameter param) {
        var harNyeRammevedtakIInfotrygd = dto.getHarNyeRammevedtakIInfotrygd();

        // TODO: historikkinnslag

        if (harNyeRammevedtakIInfotrygd) {
            return OppdateringResultat.utenOveropp();
        } else {
            //FIXME fortsett behandling med avslag av perioder
            return OppdateringResultat.utenOveropp();
            //return OppdateringResultat.medFremoverHoppTotrinn(TransisjonIdentifikator.forId())
        }
    }
}
