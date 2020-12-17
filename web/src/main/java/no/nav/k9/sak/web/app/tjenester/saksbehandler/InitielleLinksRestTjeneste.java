package no.nav.k9.sak.web.app.tjenester.saksbehandler;

import static no.nav.k9.abac.BeskyttetRessursKoder.APPLIKASJON;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.k9.sak.kontrakt.ResourceLink;
import no.nav.k9.sak.kontrakt.behandling.InitLinksDto;
import no.nav.k9.sak.web.app.tjenester.behandling.BehandlingDtoUtil;
import no.nav.k9.sak.web.app.tjenester.behandling.BehandlingRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.behandling.historikk.HistorikkRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.dokument.DokumentRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.fagsak.FagsakRestTjeneste;
import no.nav.k9.sak.web.app.tjenester.kodeverk.KodeverkRestTjeneste;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;


@Path("/init-fetch")
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class InitielleLinksRestTjeneste {

    public InitielleLinksRestTjeneste() {
        // for CDI proxy
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Returnerer ", tags = "init-fetch")
    @BeskyttetRessurs(action = READ, resource = APPLIKASJON)
    public InitLinksDto hentInitielleRessurser() {
        List<ResourceLink> lenkene = new ArrayList<>();
        lenkene.add(get(NavAnsattRestTjeneste.NAV_ANSATT_PATH, "nav-ansatt"));
        lenkene.add(get(KodeverkRestTjeneste.KODERVERK_PATH, "kodeverk"));
        lenkene.add(get(KodeverkRestTjeneste.ENHETER_PATH, "behandlende-enheter"));
        List<ResourceLink> saklenker = new ArrayList<>();
        saklenker.add(get(FagsakRestTjeneste.PATH, "fagsak"));
        saklenker.add(get(FagsakRestTjeneste.BRUKER_PATH, "sak-bruker"));
        saklenker.add(get(FagsakRestTjeneste.RETTIGHETER_PATH, "sak-rettigheter"));
        saklenker.add(get(HistorikkRestTjeneste.PATH, "sak-historikk"));
        saklenker.add(get(DokumentRestTjeneste.DOKUMENTER_PATH, "sak-dokumentliste"));
        saklenker.add(get(BehandlingRestTjeneste.BEHANDLINGER_ALLE, "sak-alle-behandlinger"));
        return new InitLinksDto(lenkene, saklenker);
    }

    static ResourceLink get(String path, String rel) {
        return ResourceLink.get(BehandlingDtoUtil.getApiPath(path), rel);
    }

    static ResourceLink post(String path, String rel) {
        return ResourceLink.post(BehandlingDtoUtil.getApiPath(path), rel, null);
    }

}
