package no.nav.ung.sak.web.server.abac;

import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionType;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursResourceType;
import no.nav.k9.felles.sikkerhet.abac.PdpRequest;
import no.nav.sif.abac.kontrakt.abac.AbacBehandlingStatus;
import no.nav.sif.abac.kontrakt.abac.AbacFagsakStatus;
import no.nav.sif.abac.kontrakt.abac.AbacFagsakYtelseType;
import no.nav.sif.abac.kontrakt.abac.AksjonspunktType;
import no.nav.sif.abac.kontrakt.abac.BeskyttetRessursActionAttributt;
import no.nav.sif.abac.kontrakt.abac.ResourceType;
import no.nav.sif.abac.kontrakt.abac.dto.OperasjonDto;
import no.nav.sif.abac.kontrakt.abac.dto.SaksinformasjonDto;
import no.nav.sif.abac.kontrakt.abac.dto.SaksinformasjonOgPersonerTilgangskontrollInputDto;
import no.nav.sif.abac.kontrakt.person.AktørId;
import no.nav.sif.abac.kontrakt.person.PersonIdent;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
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
        String ansvarligSaksbehandler = pdpRequest.getAnsvarligSaksbehandler();
        AbacBehandlingStatus behandlingStatus = Arrays.stream(AbacBehandlingStatus.values())
            .filter(v -> v.getEksternKode().equals(pdpRequest.getBehandlingStatusEksternKode()))
            .findFirst().orElse(null);
        AbacFagsakStatus fagsakStatus = Arrays.stream(AbacFagsakStatus.values())
            .filter(v1 -> v1.getEksternKode().equals(pdpRequest.getFagsakStatusEksternKode()))
            .findFirst().orElse(null);
        AbacFagsakYtelseType ytelseType = map(pdpRequest.getFagsakYtelseTyper(), pdpRequest.getResourceType());
        return ansvarligSaksbehandler != null || behandlingStatus != null || fagsakStatus != null || ytelseType != null
            ? new SaksinformasjonDto(ansvarligSaksbehandler, behandlingStatus, fagsakStatus, ytelseType)
            : null;
    }

    private static AbacFagsakYtelseType map(Set<String> fagsakYtelseTyper, BeskyttetRessursResourceType ressursResourceType) {
        List<FagsakYtelseType> ytelsetyper = fagsakYtelseTyper.stream()
            .map(FagsakYtelseType::fraKode)
            .toList();
        boolean kreverYtelsetype = ressursResourceType == BeskyttetRessursResourceType.FAGSAK || ressursResourceType == BeskyttetRessursResourceType.VENTEFRIST;
        if (ytelsetyper.isEmpty() && !kreverYtelsetype) {
            return null;
        }
        if (ytelsetyper.size() != 1) {
            throw new IllegalArgumentException("Forventet nøyaktig én fagsakYtelseType, men har: " + ytelsetyper);
        }
        return AbacUtil.oversettYtelseType(ytelsetyper.getFirst());
    }


    private static Set<AksjonspunktType> aksjonspunktTyperFraKoder(Collection<String> koder) {
        Set<AksjonspunktType> resultat = EnumSet.noneOf(AksjonspunktType.class);
        for (String kode : koder) {
            resultat.add(aksjonspunktTypeFraKode(kode));
        }
        return resultat;

    }

    private static AksjonspunktType aksjonspunktTypeFraKode(String kode) {
        no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktType internAksjonspunktType = no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktType.fraKode(kode);

        return switch (internAksjonspunktType) {
            case AUTOPUNKT -> AksjonspunktType.AUTOPUNKT;
            case MANUELL -> AksjonspunktType.MANUELL;
            case OVERSTYRING -> AksjonspunktType.OVERSTYRING;
            case SAKSBEHANDLEROVERSTYRING -> AksjonspunktType.SAKSBEHANDLEROVERSTYRING;
            case DEL1_AUTOPUNKT -> AksjonspunktType.DEL1_AUTOPUNKT;
            case DEL1_MANUELL -> AksjonspunktType.DEL1_MANUELL;
            case DEL1_OVERSTYRING -> AksjonspunktType.DEL1_OVERSTYRING;
            case DEL1_SAKSBEHANDLEROVERSTYRING -> AksjonspunktType.DEL1_SAKSBEHANDLEROVERSTYRING;
            case UDEFINERT -> throw new IllegalStateException("Uforventet verdi: " + internAksjonspunktType);
        };
    }

    static ResourceType resourceTypeFraKode(BeskyttetRessursResourceType kode) {
        return switch (kode) {
            case APPLIKASJON -> ResourceType.APPLIKASJON;
            case FAGSAK -> ResourceType.FAGSAK;
            case DRIFT -> ResourceType.DRIFT;
            case VENTEFRIST -> ResourceType.VENTEFRIST;
            case UNGDOMSPROGRAM -> ResourceType.UNGDOMSPROGRAM;
            default -> throw new IllegalArgumentException("Ikke-støttet verdi: " + kode);
        };
    }

    static BeskyttetRessursActionAttributt mapAction(BeskyttetRessursActionType kode) {
        return switch (kode) {
            case READ -> BeskyttetRessursActionAttributt.READ;
            case UPDATE -> BeskyttetRessursActionAttributt.UPDATE;
            case CREATE -> BeskyttetRessursActionAttributt.CREATE;
            default -> throw new IllegalArgumentException("Ikke-styttet verdi: " + kode);
        };
    }

}
