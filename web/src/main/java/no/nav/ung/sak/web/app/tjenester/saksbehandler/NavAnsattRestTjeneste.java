package no.nav.ung.sak.web.app.tjenester.saksbehandler;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.k9.sikkerhet.oidc.token.internal.JwtUtil;
import no.nav.ung.sak.kontrakt.abac.InnloggetAnsattDto;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;

import java.util.List;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.k9.felles.sikkerhet.abac.PepImpl.ENV;
import static no.nav.ung.abac.BeskyttetRessursKoder.APPLIKASJON;

@Path("/nav-ansatt")
@ApplicationScoped
@Transactional
public class NavAnsattRestTjeneste {
    public static final String NAV_ANSATT_PATH = "/nav-ansatt";

    private String gruppeIdSaksbehandler;
    private String gruppeIdVeileder;
    private String gruppeIdBeslutter;
    private String gruppeIdOverstyrer;
    private String gruppeIdEgenAnsatt;
    private String gruppeIdKode6;
    private String gruppeIdKode7;
    private boolean skalViseDetaljerteFeilmeldinger;

    public NavAnsattRestTjeneste() {
        //NOSONAR
    }

    @Inject
    public NavAnsattRestTjeneste(
        @KonfigVerdi(value = "bruker.gruppe.id.saksbehandler") String gruppeIdSaksbehandler,
        @KonfigVerdi(value = "bruker.gruppe.id.veileder") String gruppeIdVeileder,
        @KonfigVerdi(value = "bruker.gruppe.id.beslutter") String gruppeIdBeslutter,
        @KonfigVerdi(value = "bruker.gruppe.id.overstyrer") String gruppeIdOverstyrer,
        @KonfigVerdi(value = "bruker.gruppe.id.egenansatt") String gruppeIdEgenAnsatt,
        @KonfigVerdi(value = "bruker.gruppe.id.kode6") String gruppeIdKode6,
        @KonfigVerdi(value = "bruker.gruppe.id.kode7") String gruppeIdKode7,
        @KonfigVerdi(value = "vise.detaljerte.feilmeldinger", defaultVerdi = "true") Boolean viseDetaljerteFeilmeldinger
    ) {
        this.gruppeIdSaksbehandler = gruppeIdSaksbehandler;
        this.gruppeIdVeileder = gruppeIdVeileder;
        this.gruppeIdBeslutter = gruppeIdBeslutter;
        this.gruppeIdOverstyrer = gruppeIdOverstyrer;
        this.gruppeIdEgenAnsatt = gruppeIdEgenAnsatt;
        this.gruppeIdKode6 = gruppeIdKode6;
        this.gruppeIdKode7 = gruppeIdKode7;
        this.skalViseDetaljerteFeilmeldinger = BooleanUtils.toBoolean(viseDetaljerteFeilmeldinger);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        description = "Returnerer fullt navn for ident",
        tags = "nav-ansatt",
        summary = ("Ident hentes fra sikkerhetskonteksten som er tilgjengelig etter innlogging.")
    )
    @BeskyttetRessurs(action = READ, resource = APPLIKASJON, sporingslogg = false)
    public InnloggetAnsattDto innloggetBruker() {
        String ident = SubjectHandler.getSubjectHandler().getUid();
        String token = SubjectHandler.getSubjectHandler().getInternSsoToken();
        JwtUtil.CachedClaims claims = JwtUtil.CachedClaims.forToken(token);
        List<String> groupIds = claims.getGroups();
        String navn = claims.getName();

        if (ENV.isLocal()) {
            return mockInnloggetBrukerDto(ident);
        }

        return getInnloggetBrukerDto(ident, navn, groupIds);
    }

    private InnloggetAnsattDto getInnloggetBrukerDto(String ident, String navn, List<String> groupIds) {
        return InnloggetAnsattDto.builder()
            .setBrukernavn(ident)
            .setNavn(navn)
            .setKanSaksbehandle(groupIds.contains(gruppeIdSaksbehandler))
            .setKanVeilede(groupIds.contains(gruppeIdVeileder))
            .setKanBeslutte(groupIds.contains(gruppeIdBeslutter))
            .setKanOverstyre(groupIds.contains(gruppeIdOverstyrer))
            .setKanBehandleKodeEgenAnsatt(groupIds.contains(gruppeIdEgenAnsatt))
            .setKanBehandleKode6(groupIds.contains(gruppeIdKode6))
            .setKanBehandleKode7(groupIds.contains(gruppeIdKode7))
            .skalViseDetaljerteFeilmeldinger(this.skalViseDetaljerteFeilmeldinger)
            .create();
    }

    InnloggetAnsattDto mockInnloggetBrukerDto(String ident) {
        String navn = "Mocket saksbehandler";
        return InnloggetAnsattDto.builder()
            .setBrukernavn(ident)
            .setNavn(navn)
            .setKanSaksbehandle(true)
            .setKanVeilede(true)
            .setKanBeslutte(true)
            .setKanOverstyre(true)
            .setKanBehandleKodeEgenAnsatt(true)
            .setKanBehandleKode6(true)
            .setKanBehandleKode7(true)
            .skalViseDetaljerteFeilmeldinger(this.skalViseDetaljerteFeilmeldinger)
            .create();
    }
}
