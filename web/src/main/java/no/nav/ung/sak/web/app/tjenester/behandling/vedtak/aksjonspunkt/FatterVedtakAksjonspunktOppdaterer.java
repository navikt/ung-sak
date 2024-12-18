package no.nav.ung.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.ung.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.ung.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.domene.vedtak.VedtakAksjonspunktData;
import no.nav.ung.sak.domene.vedtak.impl.FatterVedtakAksjonspunkt;
import no.nav.ung.sak.kontrakt.vedtak.AksjonspunktGodkjenningDto;
import no.nav.ung.sak.kontrakt.vedtak.FatterVedtakAksjonspunktDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FatterVedtakAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class FatterVedtakAksjonspunktOppdaterer implements AksjonspunktOppdaterer<FatterVedtakAksjonspunktDto> {

    private FatterVedtakAksjonspunkt fatterVedtakAksjonspunkt;

    public FatterVedtakAksjonspunktOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public FatterVedtakAksjonspunktOppdaterer(FatterVedtakAksjonspunkt fatterVedtakAksjonspunkt) {
        this.fatterVedtakAksjonspunkt = fatterVedtakAksjonspunkt;
    }

    @Override
    public OppdateringResultat oppdater(FatterVedtakAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        Collection<AksjonspunktGodkjenningDto> aksjonspunktGodkjenningDtoList = dto.getAksjonspunktGodkjenningDtos() != null ?
            dto.getAksjonspunktGodkjenningDtos() : Collections.emptyList();

        Set<VedtakAksjonspunktData> aksjonspunkter = aksjonspunktGodkjenningDtoList.stream()
                .map(a -> {
                    // map til VedtakAksjonsonspunktData fra DTO
                    AksjonspunktDefinisjon aksDef = AksjonspunktDefinisjon.fraKode(a.getAksjonspunktKode());
                    return new VedtakAksjonspunktData(aksDef, a.isGodkjent(), a.getBegrunnelse(), fraDto(a.getArsaker()));
                })
                .collect(Collectors.toSet());

        Behandling behandling = param.getBehandling();
        fatterVedtakAksjonspunkt.oppdater(behandling, aksjonspunkter);

        return OppdateringResultat.nyttResultat();
    }

    private Collection<String> fraDto(Collection<VurderÅrsak> arsaker) {
        if (arsaker == null) {
            return Collections.emptySet();
        }
        return arsaker.stream().map(Kodeverdi::getKode).collect(Collectors.toSet());
    }
}
