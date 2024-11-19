package no.nav.ung.sak.web.app.tjenester.saksbehandler;

import com.microsoft.graph.models.Group;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.nav.k9.felles.integrasjon.ldap.LdapBruker;
import no.nav.k9.felles.integrasjon.ldap.LdapBrukeroppslag;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessurs;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.ung.sak.kontrakt.abac.InnloggetAnsattDto;
import no.nav.ung.sak.web.app.tjenester.microsoftgraph.MSGraphBruker;
import no.nav.ung.sak.web.app.tjenester.microsoftgraph.MicrosoftGraphTjeneste;
import no.nav.ung.sak.web.app.util.LdapUtil;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;

import static no.nav.ung.abac.BeskyttetRessursKoder.APPLIKASJON;
import static no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.k9.felles.sikkerhet.abac.PepImpl.ENV;

@Path("/nav-ansatt")
@ApplicationScoped
@Transactional
public class NavAnsattRestTjeneste {
    public static final String NAV_ANSATT_PATH = "/nav-ansatt";
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(NavAnsattRestTjeneste.class);

    private MicrosoftGraphTjeneste msGraphTjeneste;

    private String gruppenavnSaksbehandler;
    private String gruppenavnVeileder;
    private String gruppenavnBeslutter;
    private String gruppenavnOverstyrer;
    private String gruppenavnEgenAnsatt;
    private String gruppenavnKode6;
    private String gruppenavnKode7;
    private boolean skalViseDetaljerteFeilmeldinger;

    public NavAnsattRestTjeneste() {
        //NOSONAR
    }

    @Inject
    public NavAnsattRestTjeneste(
        @KonfigVerdi(value = "bruker.gruppenavn.saksbehandler") String gruppenavnSaksbehandler,
        @KonfigVerdi(value = "bruker.gruppenavn.veileder") String gruppenavnVeileder,
        @KonfigVerdi(value = "bruker.gruppenavn.beslutter") String gruppenavnBeslutter,
        @KonfigVerdi(value = "bruker.gruppenavn.overstyrer") String gruppenavnOverstyrer,
        @KonfigVerdi(value = "bruker.gruppenavn.egenansatt") String gruppenavnEgenAnsatt,
        @KonfigVerdi(value = "bruker.gruppenavn.kode6") String gruppenavnKode6,
        @KonfigVerdi(value = "bruker.gruppenavn.kode7") String gruppenavnKode7,
        @KonfigVerdi(value = "vise.detaljerte.feilmeldinger", defaultVerdi = "true") Boolean viseDetaljerteFeilmeldinger,
        MicrosoftGraphTjeneste microsoftGraphTjeneste
    ) {
        this.gruppenavnSaksbehandler = gruppenavnSaksbehandler;
        this.gruppenavnVeileder = gruppenavnVeileder;
        this.gruppenavnBeslutter = gruppenavnBeslutter;
        this.gruppenavnOverstyrer = gruppenavnOverstyrer;
        this.gruppenavnEgenAnsatt = gruppenavnEgenAnsatt;
        this.gruppenavnKode6 = gruppenavnKode6;
        this.gruppenavnKode7 = gruppenavnKode7;
        this.skalViseDetaljerteFeilmeldinger = BooleanUtils.toBoolean(viseDetaljerteFeilmeldinger);
        this.msGraphTjeneste = microsoftGraphTjeneste;
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

        if (!ENV.isProd() && !ENV.isLocal()) {
            try {
                MSGraphBruker innloggetBruker = msGraphTjeneste.getUserInfoFromGraph(ident);
                return getInnloggetBrukerDto(ident, innloggetBruker);
            } catch (Exception e) {
                // TODO Fjern mocking når vi har på plass riktig tilgang til MS Graph
                log.error("Feil ved henting av brukerinfo fra MS Graph. Returnerer mocket bruker", e);
                return mockInnloggetBrukerDto(ident);
            }
        }

        // FIXME: Erstatt med Microsoft Graph.
        LdapBruker ldapBruker = new LdapBrukeroppslag().hentBrukerinformasjon(ident);
        return getInnloggetBrukerDto(ident, ldapBruker);
    }

    InnloggetAnsattDto getInnloggetBrukerDto(String ident, LdapBruker ldapBruker) {
        String navn = ldapBruker.getDisplayName();
        Collection<String> grupper = LdapUtil.filtrerGrupper(ldapBruker.getGroups());
        return InnloggetAnsattDto.builder()
            .setBrukernavn(ident)
            .setNavn(navn)
            .setKanSaksbehandle(grupper.contains(gruppenavnSaksbehandler))
            .setKanVeilede(grupper.contains(gruppenavnVeileder))
            .setKanBeslutte(grupper.contains(gruppenavnBeslutter))
            .setKanOverstyre(grupper.contains(gruppenavnOverstyrer))
            .setKanBehandleKodeEgenAnsatt(grupper.contains(gruppenavnEgenAnsatt))
            .setKanBehandleKode6(grupper.contains(gruppenavnKode6))
            .setKanBehandleKode7(grupper.contains(gruppenavnKode7))
            .skalViseDetaljerteFeilmeldinger(this.skalViseDetaljerteFeilmeldinger)
            .create();
    }

    InnloggetAnsattDto getInnloggetBrukerDto(String ident, MSGraphBruker bruker) {
        String navn = bruker.bruker().getDisplayName();
        List<String> groupNames = bruker.grupper().stream().map(Group::getDisplayName).toList();
        return InnloggetAnsattDto.builder()
            .setBrukernavn(ident)
            .setNavn(navn)
            .setKanSaksbehandle(groupNames.contains(gruppenavnSaksbehandler))
            .setKanVeilede(groupNames.contains(gruppenavnVeileder))
            .setKanBeslutte(groupNames.contains(gruppenavnBeslutter))
            .setKanOverstyre(groupNames.contains(gruppenavnOverstyrer))
            .setKanBehandleKodeEgenAnsatt(groupNames.contains(gruppenavnEgenAnsatt))
            .setKanBehandleKode6(groupNames.contains(gruppenavnKode6))
            .setKanBehandleKode7(groupNames.contains(gruppenavnKode7))
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
