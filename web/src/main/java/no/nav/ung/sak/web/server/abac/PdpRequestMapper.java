package no.nav.ung.sak.web.server.abac;

import no.nav.k9.felles.sikkerhet.abac.PdpRequest;
import no.nav.sif.abac.kontrakt.abac.*;
import no.nav.sif.abac.kontrakt.abac.dto.OperasjonDto;
import no.nav.sif.abac.kontrakt.abac.dto.SaksinformasjonDto;
import no.nav.sif.abac.kontrakt.abac.dto.SaksinformasjonTilgangskontrollInputDto;
import no.nav.sif.abac.kontrakt.person.AktørId;
import no.nav.sif.abac.kontrakt.person.PersonIdent;

import java.util.Arrays;

public class PdpRequestMapper {

    public static SaksinformasjonTilgangskontrollInputDto map(PdpRequest pdpRequest){
        return new SaksinformasjonTilgangskontrollInputDto(
            pdpRequest.getListOfString(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE).stream().map(AktørId::new).toList(),
            pdpRequest.getListOfString(AbacAttributter.RESOURCE_FELLES_PERSON_FNR).stream().map(PersonIdent::new).toList(),
            operasjon(pdpRequest),
            saksinformasjon(pdpRequest)
        );
    }

    public static OperasjonDto operasjon(PdpRequest pdpRequest) {
        ResourceType resource = resourceTypeFraKode(pdpRequest.getString(AbacAttributter.RESOURCE_FELLES_RESOURCE_TYPE));
        return new OperasjonDto(resource, actionFraKode(pdpRequest.getString(AbacAttributter.XACML_1_0_ACTION_ACTION_ID)));
    }

    public static SaksinformasjonDto saksinformasjon(PdpRequest pdpRequest) {
        return new SaksinformasjonDto(
            pdpRequest.getString(AbacAttributter.RESOURCE_ANSVARLIG_SAKSBEHANDLER),
            Arrays.stream(AbacBehandlingStatus.values())
                .filter(v -> v.getEksternKode().equals(pdpRequest.getString(AbacAttributter.RESOURCE_BEHANDLINGSSTATUS)))
                .findFirst().orElse(null),
            Arrays.stream(AbacFagsakStatus.values())
                .filter(v -> v.getEksternKode().equals(pdpRequest.getString(AbacAttributter.RESOURCE_SAKSSTATUS)))
                .findFirst().orElse(null),
            aksjonspunktTypeFraKode(pdpRequest.getString(AbacAttributter.RESOURCE_AKSJONSPUNKT_TYPE)));
    }

    private static AksjonspunktType aksjonspunktTypeFraKode(String kode) {
        return switch (kode) {
            case null -> null;
            case "AUTO" -> AksjonspunktType.AUTOPUNKT;
            case "MANU" -> AksjonspunktType.MANUELL;
            case "OVST" -> AksjonspunktType.OVERSTYRING;
            case "SAOV" -> AksjonspunktType.SAKSBEHANDLEROVERSTYRING;
            default -> throw new IllegalStateException("Unexpected value: " + kode);
        };
    }

    static ResourceType resourceTypeFraKode(String kode) {
        return switch (kode) {
            case no.nav.ung.abac.BeskyttetRessursKoder.APPLIKASJON -> ResourceType.APPLIKASJON;
            case no.nav.ung.abac.BeskyttetRessursKoder.FAGSAK -> ResourceType.FAGSAK;
            case no.nav.ung.abac.BeskyttetRessursKoder.DRIFT -> ResourceType.DRIFT;
            case no.nav.ung.abac.BeskyttetRessursKoder.VENTEFRIST -> ResourceType.VENTEFRIST;
            default -> throw new IllegalArgumentException("Ikke-støttet verdi: " + kode);
        };
    }

    static BeskyttetRessursActionAttributt actionFraKode(String kode) {
        return switch (kode) {
            case "read" -> BeskyttetRessursActionAttributt.READ;
            case "update" -> BeskyttetRessursActionAttributt.UPDATE;
            case "create" -> BeskyttetRessursActionAttributt.CREATE;
            default -> throw new IllegalArgumentException("Ikke-styttet verdi: " + kode);
        };
    }

}
