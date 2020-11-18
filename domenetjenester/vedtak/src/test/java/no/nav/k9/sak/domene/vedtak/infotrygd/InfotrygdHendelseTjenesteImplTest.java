package no.nav.k9.sak.domene.vedtak.infotrygd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.FeedDto;
import no.nav.foreldrepenger.kontrakter.feed.vedtak.v1.FeedElement;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingStegTilstand;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class InfotrygdHendelseTjenesteImplTest {

    @Mock
    private Behandling behandling;

    @Mock
    private AktørId aktørId;

    @Mock
    private BehandlingStegTilstand tilstand;

    @Mock
    private OidcRestClient oidcRestClient;

    private InfotrygdHendelseTjeneste tjeneste;

    private static final String BASE_URL_FEED = "https://infotrygd-hendelser-api-t10.nais.preprod.local/infotrygd/hendelser";
    private URI endpoint = URI.create(BASE_URL_FEED);

    @BeforeEach
    public void setUp() {
        tjeneste = new InfotrygdHendelseTjeneste(endpoint, oidcRestClient);
    }

    @Test
    public void skal_lese_fra_infotrygd_feed() {

        //Arrange
        FeedDto feed = lagTestData();
        LocalDateTime opprettetTidspunkt = LocalDateTime.of(2018, 5, 14, 10, 15, 30, 294);
        when(oidcRestClient.get(any(), any())).thenReturn(feed);
        when(behandling.getAktørId()).thenReturn(aktørId);
        when(aktørId.getId()).thenReturn("9000000001234");
        when(tilstand.getBehandlingSteg()).thenReturn(BehandlingStegType.FATTE_VEDTAK);
        when(tilstand.getOpprettetTidspunkt()).thenReturn(opprettetTidspunkt);

        //Act
        List<InfotrygdHendelse> infotrygdHendelse = tjeneste.hentHendelsesListFraInfotrygdFeed(behandling, opprettetTidspunkt.toLocalDate());

        //Assert
        assertThat(infotrygdHendelse).hasSize(2);
    }

    private FeedDto lagTestData() {
        return new FeedDto.Builder()
            .medTittel("enhetstest")
            .medElementer(Arrays.asList(
                lagElement(1, new InfotrygdAnnulert()),
                lagElement(2, new InfotrygdInnvilget())))
            .build();
    }

    private FeedElement lagElement(long sequence, Object melding) {
        String type;
        if (melding instanceof InfotrygdAnnulert) {
            type = "ANNULERT_v1";
        } else {
            type = "INNVILGET_v1";
        }
        return new FeedElement.Builder()
            .medSekvensId(sequence)
            .medType(type)
            .medInnhold(lagInnhold(melding))
            .build();
    }

    private Innhold lagInnhold(Object melding) {
        Innhold innhold = (Innhold) melding;
        innhold.setAktoerId("9000000001234");
        innhold.setFom(LocalDate.now());
        innhold.setIdentDato(konverterFomDatoTilString(LocalDate.now()));
        innhold.setTypeYtelse("Type");

        return innhold;

    }

    private String konverterFomDatoTilString(LocalDate dato) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return dato.format(formatter);
    }
}
