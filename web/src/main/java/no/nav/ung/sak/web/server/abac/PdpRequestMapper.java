package no.nav.ung.sak.web.server.abac;

import no.nav.k9.felles.sikkerhet.abac.PdpRequest;
import no.nav.sif.abac.kontrakt.abac.AbacBehandlingStatus;
import no.nav.sif.abac.kontrakt.abac.AbacFagsakStatus;
import no.nav.sif.abac.kontrakt.abac.AksjonspunktType;
import no.nav.sif.abac.kontrakt.abac.BeskyttetRessursActionAttributt;
import no.nav.sif.abac.kontrakt.abac.ResourceType;
import no.nav.sif.abac.kontrakt.abac.dto.OperasjonDto;
import no.nav.sif.abac.kontrakt.abac.dto.SaksinformasjonDto;
import no.nav.sif.abac.kontrakt.abac.dto.SaksinformasjonOgPersonerTilgangskontrollInputDto;
import no.nav.sif.abac.kontrakt.person.AktørId;
import no.nav.sif.abac.kontrakt.person.PersonIdent;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class PdpRequestMapper {

    public static SaksinformasjonOgPersonerTilgangskontrollInputDto map(PdpRequest pdpRequest){
        return new SaksinformasjonOgPersonerTilgangskontrollInputDto(
            pdpRequest.getListOfString(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE).stream().map(AktørId::new).toList(),
            pdpRequest.getListOfString(AbacAttributter.RESOURCE_FELLES_PERSON_FNR).stream().map(PersonIdent::new).toList(),
            operasjon(pdpRequest),
            saksinformasjon(pdpRequest)
        );
    }

    public static OperasjonDto operasjon(PdpRequest pdpRequest) {
        ResourceType resource = resourceTypeFraKode(pdpRequest.getString(AbacAttributter.RESOURCE_FELLES_RESOURCE_TYPE));
        Set<AksjonspunktType> aksjonspunktTyper = aksjonspunktTyperFraKoder(pdpRequest.getListOfString(AbacAttributter.RESOURCE_AKSJONSPUNKT_TYPE));
        return new OperasjonDto(resource, actionFraKode(pdpRequest.getString(AbacAttributter.XACML_1_0_ACTION_ACTION_ID)), aksjonspunktTyper);
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
            null);
    }

    private static Set<AksjonspunktType> aksjonspunktTyperFraKoder(List<String> koder) {
        Set<AksjonspunktType> resultat = EnumSet.noneOf(AksjonspunktType.class);
        for (String kode : koder) {
            resultat.add(aksjonspunktTypeFraKode(kode));
        }
        return resultat;

    }

    private static AksjonspunktType aksjonspunktTypeFraKode(String kode) {
        return switch (kode) {
            case null -> null;
            case "Autopunkt" -> AksjonspunktType.AUTOPUNKT;
            case "Manuell" -> AksjonspunktType.MANUELL;
            case "Overstyring" -> AksjonspunktType.OVERSTYRING;
            case "Saksbehandleroverstyring" -> AksjonspunktType.SAKSBEHANDLEROVERSTYRING;
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
