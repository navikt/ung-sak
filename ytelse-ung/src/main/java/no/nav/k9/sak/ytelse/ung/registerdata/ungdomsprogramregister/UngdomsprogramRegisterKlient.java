package no.nav.k9.sak.ytelse.ung.registerdata.ungdomsprogramregister;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.rest.OidcRestClient;
import no.nav.k9.felles.integrasjon.rest.ScopedRestIntegration;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.kontrakt.person.AktørIdDto;

@Dependent
@ScopedRestIntegration(scopeKey = "ungdomsprogramregister.scope", defaultScope = "api://prod-gcp.k9saksbehandling.ung-deltakelse-opplyser/.default")
public class UngdomsprogramRegisterKlient {
    private final OidcRestClient restClient;
    private final URI hentUri;

    @Inject
    public UngdomsprogramRegisterKlient(
        OidcRestClient restClient,
        @KonfigVerdi(value = "ungdomsprogramregister.url", defaultVerdi = "http://ung-deltakelse-opplyser.k9saksbehandling") String url) {
        this.restClient = restClient;
        hentUri = tilUri(url, "/register/hent/alle");

    }

    public DeltakerOpplysningerDTO hentForAktørId(String aktørId) {
        try {
            return restClient.post(hentUri, new AktørIdDto(aktørId), DeltakerOpplysningerDTO.class);
        } catch (Exception e) {
            throw UngdomsprogramRegisterFeil.FACTORY.feilVedKallTilUngRegister(e).toException();
        }

    }


    private static URI tilUri(String baseUrl, String path) {
        try {
            return new URI(baseUrl + "/" + path);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Ugyldig konfigurasjon for ungdomsprogram.register.url", e);
        }
    }

    public record DeltakerOpplysningerDTO(List<DeltakerProgramOpplysningDTO> opplysninger) {
    }

    public record DeltakerProgramOpplysningDTO(UUID id, String deltakerIdent, LocalDate fraOgMed, LocalDate tilOgMed) {
    }
}

