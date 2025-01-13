package no.nav.ung.sak.web.app.tjenester.saksbehandler;

import no.nav.k9.felles.testutilities.sikkerhet.StaticSubjectHandler;
import no.nav.k9.felles.testutilities.sikkerhet.SubjectHandlerUtils;
import no.nav.k9.sikkerhet.context.domene.IdentType;
import no.nav.k9.sikkerhet.context.domene.OidcCredential;
import no.nav.ung.sak.kontrakt.abac.InnloggetAnsattDto;
import no.nav.ung.sak.tilgangskontroll.tilganger.AnsattTilgangerTjeneste;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.security.auth.Subject;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class NavAnsattRestTjenesteTest {

    private static final String tokenHeader = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Inp4ZWcyV09OcFRrd041R21lWWN1VGR0QzZKMCJ9";

    @InjectMocks
    private NavAnsattRestTjeneste navAnsattRestTjeneste;

    @BeforeEach
    public void setUp() {
        var tilgangerTjeneste = new AnsattTilgangerTjeneste("Saksbehandler",
            "Veileder",
            "Beslutter",
            "Overstyrer",
            "EgenAnsatt",
            "Kode6",
            "Kode7","Drift");
        navAnsattRestTjeneste = new NavAnsattRestTjeneste(tilgangerTjeneste,true);
    }

    @AfterEach
    public void tearDown() {
        SubjectHandlerUtils.reset();
    }

    @Test
    public void testInnloggetBruker() {
        String ident = "testIdent";
        String navnet = "saksbehandler 1";
        List<String> groupIds = Arrays.asList("Saksbehandler", "Kode7");
        String token = createJWT(ident, navnet, groupIds);

        Subject subject = new SubjectHandlerUtils.SubjectBuilder(ident, IdentType.InternBruker).getSubject();
        subject.getPublicCredentials().add(new OidcCredential(token));
        SubjectHandlerUtils.useSubjectHandler(StaticSubjectHandler.class);
        SubjectHandlerUtils.setSubject(subject);

        InnloggetAnsattDto result = navAnsattRestTjeneste.innloggetBruker();

        assertEquals(ident, result.getBrukernavn());
        assertEquals(navnet, result.getNavn());
        assertTrue(result.getKanSaksbehandle());
        assertFalse(result.getKanVeilede());
        assertFalse(result.getKanBeslutte());
        assertFalse(result.getKanOverstyre());
        assertFalse(result.getKanBehandleKodeEgenAnsatt());
        assertFalse(result.getKanBehandleKode6());
        assertTrue(result.getKanBehandleKode7());
        assertTrue(result.getSkalViseDetaljerteFeilmeldinger());
    }

    public static String createJWT(String ident, String name, List<String> groupIds) {
        String jwtBodyContent = """
            {"name" : ":name", "groups": [:groups],"NAVident": ":ident"}
            """
            .replaceAll(":ident", ident)
            .replaceAll(":name", name)
            .replaceAll(":groups", groupIds.stream().map(it -> '"' + it + '"').collect(Collectors.joining(",")));
        return tokenHeader + "." + java.util.Base64.getUrlEncoder().encodeToString(jwtBodyContent.getBytes()).replaceAll("=", "") + "." + "DUMMYSIGNATURE";
    }
}
