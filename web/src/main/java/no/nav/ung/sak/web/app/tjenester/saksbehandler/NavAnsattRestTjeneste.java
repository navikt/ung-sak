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
import no.nav.ung.sak.tilgangskontroll.tilganger.AnsattTilgangerTjeneste;
import no.nav.ung.sak.tilgangskontroll.tilganger.TilgangerBruker;
import org.apache.commons.lang3.BooleanUtils;

import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.ung.abac.BeskyttetRessursKoder.APPLIKASJON;

@Path("/nav-ansatt")
@ApplicationScoped
@Transactional
public class NavAnsattRestTjeneste {
    public static final String NAV_ANSATT_PATH = "/nav-ansatt";

    private AnsattTilgangerTjeneste ansattTilgangerTjeneste;
    private boolean skalViseDetaljerteFeilmeldinger;

    public NavAnsattRestTjeneste() {
        //NOSONAR
    }

    @Inject
    public NavAnsattRestTjeneste(
        AnsattTilgangerTjeneste ansattTilgangerTjeneste, @KonfigVerdi(value = "vise.detaljerte.feilmeldinger", defaultVerdi = "true") Boolean viseDetaljerteFeilmeldinger
    ) {
        this.ansattTilgangerTjeneste = ansattTilgangerTjeneste;
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
        TilgangerBruker tilganger = ansattTilgangerTjeneste.tilgangerForBruker(ident, claims.getGroups());
        return getInnloggetBrukerDto(ident, claims.getName(), tilganger);
    }

    private InnloggetAnsattDto getInnloggetBrukerDto(String ident, String navn, TilgangerBruker tilganger) {
        return InnloggetAnsattDto.builder()
            .setBrukernavn(ident)
            .setKanSaksbehandle(tilganger.kanSaksbehandle())
            .setKanVeilede(tilganger.kanVeilede())
            .setKanBeslutte(tilganger.kanBeslutte())
            .setKanOverstyre(tilganger.kanOverstyre())
            .setKanBehandleKodeEgenAnsatt(tilganger.kanBehandleEgenAnsatt())
            .setKanBehandleKode6(tilganger.kanBehandleKode6())
            .setKanBehandleKode7(tilganger.kanBehandleKode7())
            .setNavn(navn)
            .skalViseDetaljerteFeilmeldinger(this.skalViseDetaljerteFeilmeldinger)
            .create();
    }

}
