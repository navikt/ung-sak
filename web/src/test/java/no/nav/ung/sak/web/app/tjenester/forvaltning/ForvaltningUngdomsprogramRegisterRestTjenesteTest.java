package no.nav.ung.sak.web.app.tjenester.forvaltning;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.core.Response;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.kontrakt.KortTekst;
import no.nav.ung.sak.test.util.fagsak.FagsakBuilder;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.web.app.tjenester.forvaltning.dump.logg.DiagnostikkFagsakLogg;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramRegisterKlient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class ForvaltningUngdomsprogramRegisterRestTjenesteTest {

    private static final Saksnummer SAKSNUMMER = new Saksnummer("123456789");
    private static final AktørId AKTØR_ID = AktørId.dummy();
    private static final UUID DELTAKELSE_ID = UUID.randomUUID();
    private static final String BEGRUNNELSE = "Jira sak: ABCD-1234";

    @Inject
    private EntityManager entityManager;

    private FagsakRepository fagsakRepository;
    private UngdomsprogramRegisterKlient ungdomsprogramRegisterKlient;
    private ForvaltningUngdomsprogramRegisterRestTjeneste tjeneste;

    private final Fagsak fagsak = FagsakBuilder.nyFagsak(FagsakYtelseType.UNGDOMSYTELSE)
        .medSaksnummer(SAKSNUMMER)
        .medBruker(AKTØR_ID)
        .build();

    @BeforeEach
    void setup() {
        fagsakRepository = new FagsakRepository(entityManager);
        ungdomsprogramRegisterKlient = mock(UngdomsprogramRegisterKlient.class);

        tjeneste = new ForvaltningUngdomsprogramRegisterRestTjeneste(fagsakRepository, ungdomsprogramRegisterKlient, entityManager);

        fagsakRepository.opprettNy(fagsak);
    }

    @Test
    void skal_markere_deltakelse_som_søkt_og_logge_diagnostikk() {
        UngdomsprogramRegisterKlient.DeltakerProgramOpplysningDTO deltakelse = new UngdomsprogramRegisterKlient.DeltakerProgramOpplysningDTO(
            DELTAKELSE_ID, AKTØR_ID.getAktørId(), LocalDate.now().minusMonths(1), null, false, null
        );
        when(ungdomsprogramRegisterKlient.hentForAktørId(AKTØR_ID.getAktørId()))
            .thenReturn(new UngdomsprogramRegisterKlient.DeltakerOpplysningerDTO(List.of(deltakelse)));

        Response response = tjeneste.markerDeltakelseSomSøkt(new ForvaltningUngdomsprogramRegisterRestTjeneste.MarkerDeltakelseSøktRequest(SAKSNUMMER, new KortTekst(BEGRUNNELSE)));

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        verify(ungdomsprogramRegisterKlient).markerSomSøkt(AKTØR_ID.getAktørId(), DELTAKELSE_ID);

        List<DiagnostikkFagsakLogg> logger = entityManager.createQuery("from DiagnostikkFagsakLogg where fagsakId = :fagsakId", DiagnostikkFagsakLogg.class)
            .setParameter("fagsakId", fagsak.getId())
            .getResultList();
        assertThat(logger).hasSize(1);
    }

    @Test
    void skal_returnere_404_hvis_fagsak_ikke_finnes() {
        Response response = tjeneste.markerDeltakelseSomSøkt(new ForvaltningUngdomsprogramRegisterRestTjeneste.MarkerDeltakelseSøktRequest(new Saksnummer("999999999"), new KortTekst(BEGRUNNELSE)));

        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
        verify(ungdomsprogramRegisterKlient, never()).markerSomSøkt(eq(AKTØR_ID.getAktørId()), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void skal_returnere_404_hvis_ingen_deltakelse_finnes() {
        when(ungdomsprogramRegisterKlient.hentForAktørId(AKTØR_ID.getAktørId()))
            .thenReturn(new UngdomsprogramRegisterKlient.DeltakerOpplysningerDTO(List.of()));

        Response response = tjeneste.markerDeltakelseSomSøkt(new ForvaltningUngdomsprogramRegisterRestTjeneste.MarkerDeltakelseSøktRequest(SAKSNUMMER, new KortTekst(BEGRUNNELSE)));

        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    @Test
    void skal_returnere_400_hvis_flere_deltakelser_finnes() {
        UngdomsprogramRegisterKlient.DeltakerProgramOpplysningDTO deltakelse1 = new UngdomsprogramRegisterKlient.DeltakerProgramOpplysningDTO(
            UUID.randomUUID(), AKTØR_ID.getAktørId(), LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(1), false, null
        );
        UngdomsprogramRegisterKlient.DeltakerProgramOpplysningDTO deltakelse2 = new UngdomsprogramRegisterKlient.DeltakerProgramOpplysningDTO(
            DELTAKELSE_ID, AKTØR_ID.getAktørId(), LocalDate.now().minusMonths(1), null, false, null
        );
        when(ungdomsprogramRegisterKlient.hentForAktørId(AKTØR_ID.getAktørId()))
            .thenReturn(new UngdomsprogramRegisterKlient.DeltakerOpplysningerDTO(List.of(deltakelse1, deltakelse2)));

        Response response = tjeneste.markerDeltakelseSomSøkt(new ForvaltningUngdomsprogramRegisterRestTjeneste.MarkerDeltakelseSøktRequest(SAKSNUMMER, new KortTekst(BEGRUNNELSE)));

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
        verify(ungdomsprogramRegisterKlient, times(0)).markerSomSøkt(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
    }
}
