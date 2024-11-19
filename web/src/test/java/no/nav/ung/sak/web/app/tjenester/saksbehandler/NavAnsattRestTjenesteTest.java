package no.nav.ung.sak.web.app.tjenester.saksbehandler;

import io.jsonwebtoken.security.SignatureAlgorithm;
import jakarta.xml.bind.DatatypeConverter;
import no.nav.k9.sikkerhet.context.SubjectHandler;
import no.nav.k9.sikkerhet.oidc.token.internal.JwtUtil;
import no.nav.ung.sak.kontrakt.abac.InnloggetAnsattDto;
import org.jose4j.base64url.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NavAnsattRestTjenesteTest {

    @Mock
    private SubjectHandler subjectHandler;

    @Mock
    private JwtUtil jwtUtil;

    private static final String tokenHeader = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Inp4ZWcyV09OcFRrd041R21lWWN1VGR0QzZKMCJ9";

    @InjectMocks
    private NavAnsattRestTjeneste navAnsattRestTjeneste;

    @BeforeEach
    public void setUp() {
        navAnsattRestTjeneste = new NavAnsattRestTjeneste(
            "Saksbehandler",
            "Veileder",
            "Beslutter",
            "Overstyrer",
            "EgenAnsatt",
            "Kode6",
            "Kode7",
            true
        );
    }

    @Test
    public void testInnloggetBruker() {
        String ident = "testIdent";

        // language=JSON
        String tokenBody = """
            {
              "aud": "a442457f-4e26-4ffa-93f1-9f9a1ef5866b",
              "iss": "https://login.microsoftonline.com/966ac572-f5b7-4bbe-aa88-c76419c0f851/v2.0",
              "iat": 1732022540,
              "nbf": 1732022540,
              "exp": 1732027627,
              "groups": [
                "dec3ee50-b683-4644-9507-520e8f054ac2"
              ],
              "name": "F_Z994376 E_Z994376",
              "sub": "8sAezJYfUBSk6QTLIbdpEiIKu87LDxKmxABTLFMnslo",
              "tid": "966ac572-f5b7-4bbe-aa88-c76419c0f851",
              "ver": "2.0",
              "NAVident": "Z994376",
              "azp_name": "dev-gcp:k9saksbehandling:ung-sak"
            }
            """;


        String token = tokenHeader + "." + Base64.encode(tokenBody.getBytes()).replace("=", "");

        List<String> groupIds = Arrays.asList("Saksbehandler", "Veileder");

        when(subjectHandler.getUid()).thenReturn(ident);
        when(subjectHandler.getInternSsoToken()).thenReturn(token);
        when(jwtUtil.getGroups(token)).thenReturn(groupIds);
        when(jwtUtil.getName(token)).thenReturn("Test Navn");

        InnloggetAnsattDto result = navAnsattRestTjeneste.innloggetBruker();

        assertEquals(ident, result.getBrukernavn());
        assertEquals("Test Navn", result.getNavn());
        assertEquals(true, result.getKanSaksbehandle());
        assertEquals(true, result.getKanVeilede());
        assertEquals(false, result.getKanBeslutte());
        assertEquals(false, result.getKanOverstyre());
        assertEquals(false, result.getKanBehandleKodeEgenAnsatt());
        assertEquals(false, result.getKanBehandleKode6());
        assertEquals(false, result.getKanBehandleKode7());
        assertEquals(true, result.getSkalViseDetaljerteFeilmeldinger());
    }

    public static String createJWT(String id, String issuer, String subject, long ttlMillis) {

        //The JWT signature algorithm we will be using to sign the token
        SignatureAlgorithm signatureAlgorithm = SignatureAlgori;

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        //We will sign our JWT with our ApiKey secret
        byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary("secret");
        Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getId());

        //Let's set the JWT Claims
        JwtBuilder builder = Jwts.builder().setId(id)
            .setIssuedAt(now)
            .setSubject(subject)
            .setIssuer(issuer)
            .signWith(signatureAlgorithm, signingKey);

        //if it has been specified, let's add the expiration
        if (ttlMillis > 0) {
            long expMillis = nowMillis + ttlMillis;
            Date exp = new Date(expMillis);
            builder.setExpiration(exp);
        }

        //Builds the JWT and serializes it to a compact, URL-safe string
        return builder.compact();
    }
}
