package no.nav.ung.sak.web.server.abac;

import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
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
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

public class PdpRequestMapper {

    public static SaksinformasjonOgPersonerTilgangskontrollInputDto map(PdpRequest pdpRequest) {
        return new SaksinformasjonOgPersonerTilgangskontrollInputDto(
            pdpRequest.getAktørIder().stream().map(it -> new AktørId(it.aktørId())).toList(),
            pdpRequest.getFødselsnumre().stream().map(it -> new PersonIdent(it.fnr())).toList(),
            operasjon(pdpRequest),
            saksinformasjon(pdpRequest)
        );
    }

    public static OperasjonDto operasjon(PdpRequest pdpRequest) {
        ResourceType resource = resourceTypeFraKode(pdpRequest.getResourceType());
        Set<AksjonspunktType> aksjonspunktTyper = aksjonspunktTyperFraKoder(pdpRequest.getAksjonspunktTypeEksternKode());
        return new OperasjonDto(resource, mapAction(pdpRequest.getActionType()), aksjonspunktTyper);
    }

    public static SaksinformasjonDto saksinformasjon(PdpRequest pdpRequest) {
        return new SaksinformasjonDto(
            pdpRequest.getAnsvarligSaksbehandler(),
            Arrays.stream(AbacBehandlingStatus.values())
                .filter(v -> v.getEksternKode().equals(pdpRequest.getBehandlingStatusEksternKode()))
                .findFirst().orElse(null),
            Arrays.stream(AbacFagsakStatus.values())
                .filter(v -> v.getEksternKode().equals(pdpRequest.getFagsakStatusEksternKode()))
                .findFirst().orElse(null),
            null);
    }

    private static Set<AksjonspunktType> aksjonspunktTyperFraKoder(Collection<String> koder) {
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

    static ResourceType resourceTypeFraKode(BeskyttetRessursResourceType kode) {
        return switch (kode) {
            case APPLIKASJON -> ResourceType.APPLIKASJON;
            case FAGSAK -> ResourceType.FAGSAK;
            case DRIFT -> ResourceType.DRIFT;
            case VENTEFRIST -> ResourceType.VENTEFRIST;
            default -> throw new IllegalArgumentException("Ikke-støttet verdi: " + kode);
        };
    }

    static no.nav.sif.abac.kontrakt.abac.BeskyttetRessursActionAttributt mapAction(BeskyttetRessursActionType kode) {
        return switch (kode) {
            case READ -> BeskyttetRessursActionAttributt.READ;
            case UPDATE -> BeskyttetRessursActionAttributt.UPDATE;
            case CREATE -> BeskyttetRessursActionAttributt.CREATE;
            default -> throw new IllegalArgumentException("Ikke-styttet verdi: " + kode);
        };
    }

}
