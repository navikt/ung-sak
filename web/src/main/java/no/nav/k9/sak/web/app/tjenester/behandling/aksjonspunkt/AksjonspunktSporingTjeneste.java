package no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.k9.sak.domene.typer.tid.JsonObjectMapper;
import no.nav.k9.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

import java.util.Collection;

/**
 * Lagrer data som ble brukt i utførelse av aksjonspunktet i json-format. Brukes utelukkende til diagnostikkformål for enklere feilsøking.
 */
@Dependent
public class AksjonspunktSporingTjeneste {

    private AksjonspunktRepository aksjonspunktRepository;

    public AksjonspunktSporingTjeneste() {
    }

    @Inject
    public AksjonspunktSporingTjeneste(AksjonspunktRepository aksjonspunktRepository) {
        this.aksjonspunktRepository = aksjonspunktRepository;
    }

    void opprettSporinger(Collection<BekreftetAksjonspunktDto> dtoer, BehandlingskontrollKontekst kontekst) {
        // Ok å kjøre i loop her siden vi sjelden løser flere aksjonspunkter samtidig
        dtoer.forEach(
            a -> aksjonspunktRepository.lagreAksjonspunktSporing(a.getKode(), JsonObjectMapper.toJson(a, JsonMappingFeil.FACTORY::jsonMappingFeil), kontekst.getBehandlingId())
        );
    }



    interface JsonMappingFeil extends DeklarerteFeil {

        JsonMappingFeil FACTORY = FeilFactory.create(JsonMappingFeil.class);

        @TekniskFeil(feilkode = "K9-252295", feilmelding = "JSON-mapping feil: %s", logLevel = LogLevel.WARN)
        Feil jsonMappingFeil(JsonProcessingException e);
    }


}
