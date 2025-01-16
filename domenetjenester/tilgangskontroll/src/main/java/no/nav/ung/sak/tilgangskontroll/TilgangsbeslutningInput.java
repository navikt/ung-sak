package no.nav.ung.sak.tilgangskontroll;

import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.PdpRequest;
import no.nav.ung.abac.BeskyttetRessursKoder;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.ung.sak.tilgangskontroll.api.AbacAttributter;
import no.nav.ung.sak.tilgangskontroll.api.AbacBehandlingStatus;
import no.nav.ung.sak.tilgangskontroll.api.AbacFagsakStatus;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;

import java.util.Arrays;
import java.util.List;

public class TilgangsbeslutningInput {

    private final List<AktørId> aktørIder;
    private final List<PersonIdent> personIdenter;
    private final Operasjon operasjon;
    private final Saksinformasjon saksinformasjon;

    public TilgangsbeslutningInput(PdpRequest pdpRequest) {
        personIdenter = pdpRequest.getListOfString(AbacAttributter.RESOURCE_FELLES_PERSON_FNR).stream().map(PersonIdent::new).toList();
        aktørIder = pdpRequest.getListOfString(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE).stream().map(AktørId::new).toList();
        operasjon = new Operasjon(pdpRequest);
        saksinformasjon = new Saksinformasjon(pdpRequest);
    }

    public List<AktørId> getAktørIder() {
        return aktørIder;
    }

    public List<PersonIdent> getPersonIdenter() {
        return personIdenter;
    }

    public Operasjon getOperasjon() {
        return operasjon;
    }

    public Saksinformasjon getSaksinformasjon() {
        return saksinformasjon;
    }

    public enum ResourceType {
        APPLIKASJON,
        FAGSAK,
        DRIFT,
        VENTEFRIST;

        static ResourceType fraKode(String resourceType) {
            return switch (resourceType) {
                case BeskyttetRessursKoder.APPLIKASJON -> APPLIKASJON;
                case BeskyttetRessursKoder.FAGSAK -> FAGSAK;
                case BeskyttetRessursKoder.DRIFT -> DRIFT;
                case BeskyttetRessursKoder.VENTEFRIST -> VENTEFRIST;
                default -> throw new IllegalArgumentException("Ikke-støttet verdi: " + resourceType);
            };
        }
    }

    public static class Operasjon {
        private final ResourceType resource;
        private final BeskyttetRessursActionAttributt action;

        public Operasjon(PdpRequest pdpRequest) {
            this.action = Arrays.stream(BeskyttetRessursActionAttributt.values())
                .filter(v -> v.getEksternKode() != null && v.getEksternKode().equals(pdpRequest.getString(AbacAttributter.XACML_1_0_ACTION_ACTION_ID)))
                .findFirst().orElseThrow();
            this.resource = ResourceType.fraKode(pdpRequest.getString(AbacAttributter.RESOURCE_FELLES_RESOURCE_TYPE));
        }

        public BeskyttetRessursActionAttributt getAction() {
            return action;
        }

        public ResourceType getResource() {
            return resource;
        }
    }

    public static class Saksinformasjon {
        private final String identAnsvarligSaksbehandler;
        private final AbacBehandlingStatus behandlingStatus;
        private final AbacFagsakStatus fagsakStatus;
        private final AksjonspunktType aksjonspunktType;

        public Saksinformasjon(PdpRequest pdpRequest) {
            this.identAnsvarligSaksbehandler = pdpRequest.getString(AbacAttributter.RESOURCE_K9_SAK_ANSVARLIG_SAKSBEHANDLER);
            this.behandlingStatus = Arrays.stream(AbacBehandlingStatus.values())
                .filter(v -> v.getEksternKode().equals(pdpRequest.getString(AbacAttributter.RESOURCE_K9_SAK_BEHANDLINGSSTATUS)))
                .findFirst().orElse(null);
            this.fagsakStatus = Arrays.stream(AbacFagsakStatus.values())
                .filter(v -> v.getEksternKode().equals(pdpRequest.getString(AbacAttributter.RESOURCE_K9_SAK_SAKSSTATUS)))
                .findFirst().orElse(null);
            this.aksjonspunktType = Arrays.stream(AksjonspunktType.values())
                .filter(v -> v.getOffisiellKode() != null && v.getOffisiellKode().equals(pdpRequest.getString(AbacAttributter.RESOURCE_K9_SAK_AKSJONSPUNKT_TYPE)))
                .findFirst().orElse(null);
        }

        public String getIdentAnsvarligSaksbehandler() {
            return identAnsvarligSaksbehandler;
        }

        public AbacBehandlingStatus getBehandlingStatus() {
            return behandlingStatus;
        }

        public AbacFagsakStatus getFagsakStatus() {
            return fagsakStatus;
        }

        public AksjonspunktType getAksjonspunktType() {
            return aksjonspunktType;
        }
    }
}
