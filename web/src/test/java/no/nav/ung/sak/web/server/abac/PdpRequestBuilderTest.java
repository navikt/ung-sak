package no.nav.ung.sak.web.server.abac;

import no.nav.k9.felles.exception.ManglerTilgangException;
import no.nav.k9.felles.sikkerhet.abac.AbacAttributtSamling;
import no.nav.k9.felles.sikkerhet.abac.AbacDataAttributter;
import no.nav.k9.felles.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.k9.felles.sikkerhet.abac.PdpRequest;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.sif.abac.kontrakt.abac.AbacBehandlingStatus;
import no.nav.sif.abac.kontrakt.abac.AbacFagsakStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.sak.behandlingslager.pip.PipBehandlingsData;
import no.nav.ung.sak.behandlingslager.pip.PipRepository;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.sikkerhet.abac.AppAbacAttributtType;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class PdpRequestBuilderTest {

    private static final String DUMMY_ID_TOKEN = "dummyheader.dymmypayload.dummysignaturee";
    private static final String FAGSAK_ABAC_REFERANSE = "no.nav.abac.attributter.k9.fagsak";

    private static final Long FAGSAK_ID_1 = 10001L;
    private static final Long FAGSAK_ID_2 = 10002L;
    private static final Saksnummer SAKSNUMMER_1 = new Saksnummer("AAAA1");
    private static final Saksnummer SAKSNUMMER_2 = new Saksnummer("AAAA2");
    private static final Long BEHANDLING_ID = 333L;
    private static final JournalpostId JOURNALPOST_ID = new JournalpostId("444");

    private static final AktørId AKTØR_0 = AktørId.dummy();
    private static final AktørId AKTØR_1 = AktørId.dummy();
    private static final AktørId AKTØR_2 = AktørId.dummy();

    private static final String PERSON_0 = "00000000000";

    private final PipRepository pipRepository = Mockito.mock(PipRepository.class);
    private final AktørTjeneste aktørTjeneste = Mockito.mock(AktørTjeneste.class);

    private final AppPdpRequestBuilderImpl requestBuilder = new AppPdpRequestBuilderImpl(pipRepository, aktørTjeneste);

    @Test
    public void skal_hente_saksstatus_og_behandlingsstatus_når_behandlingId_er_input() {
        AbacAttributtSamling attributter = byggAbacAttributtSamling().leggTil(AbacDataAttributter.opprett()
            .leggTil(StandardAbacAttributtType.BEHANDLING_ID, BEHANDLING_ID));

        when(pipRepository.saksnumreForJournalpostId(any())).thenReturn(Collections.singleton(SAKSNUMMER_1));
        when(pipRepository.hentAktørIdKnyttetTilFagsaker(any())).thenReturn(Collections.singleton(AKTØR_1));
        BehandlingStatus behandligStatus = BehandlingStatus.OPPRETTET;
        String ansvarligSaksbehandler = "Z123456";
        FagsakStatus fagsakStatus = FagsakStatus.UNDER_BEHANDLING;
        when(pipRepository.hentDataForBehandling(any()))
            .thenReturn(Optional.of(new PipBehandlingsData(UUID.randomUUID(), behandligStatus, fagsakStatus, ansvarligSaksbehandler, SAKSNUMMER_1)));

        PdpRequest request = requestBuilder.lagPdpRequest(attributter);
        assertThat(request.getListOfString(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE)).containsOnly(AKTØR_1.getId());
        assertThat(request.getString(AbacAttributter.RESOURCE_ANSVARLIG_SAKSBEHANDLER)).isEqualTo(ansvarligSaksbehandler);
        assertThat(request.getString(AbacAttributter.RESOURCE_BEHANDLINGSSTATUS)).isEqualTo(AbacBehandlingStatus.OPPRETTET.getEksternKode());
        assertThat(request.getString(AbacAttributter.RESOURCE_SAKSSTATUS)).isEqualTo(AbacFagsakStatus.UNDER_BEHANDLING.getEksternKode());
    }

    @Test
    public void skal_angi_aktørId_gitt_journalpost_id_som_input() {
        AbacAttributtSamling attributter = byggAbacAttributtSamling().leggTil(AbacDataAttributter.opprett()
            .leggTil(StandardAbacAttributtType.JOURNALPOST_ID, JOURNALPOST_ID.getVerdi()));

        when(pipRepository.saksnumreForJournalpostId(any())).thenReturn(Collections.singleton(SAKSNUMMER_1));
        when(pipRepository.hentAktørIdKnyttetTilFagsaker(any())).thenReturn(Collections.singleton(AKTØR_1));

        PdpRequest request = requestBuilder.lagPdpRequest(attributter);
        assertThat(request.getListOfString(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE)).containsOnly(AKTØR_1.getId());
    }

    @Test
    public void skal_hente_fnr_fra_alle_tilknyttede_saker_når_det_kommer_inn_søk_etter_saker_for_fnr() {
        AbacAttributtSamling attributter = byggAbacAttributtSamling();
        attributter.leggTil(AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.SAKER_MED_FNR, PERSON_0));

        when(aktørTjeneste.hentAktørIdForPersonIdentSet(any())).thenReturn(Collections.singleton(AKTØR_0));

        Set<Saksnummer> saksnumre = new HashSet<>();
        saksnumre.add(SAKSNUMMER_1);
        saksnumre.add(SAKSNUMMER_2);
        when(pipRepository.saksnumreForSøker(any())).thenReturn(saksnumre);
        Set<AktørId> aktører = new HashSet<>();
        aktører.add(AKTØR_0);
        aktører.add(AKTØR_1);
        aktører.add(AKTØR_2);
        when(pipRepository.hentAktørIdKnyttetTilFagsaker(any())).thenReturn(aktører);

        PdpRequest request = requestBuilder.lagPdpRequest(attributter);
        assertThat(request.getListOfString(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE)).containsOnly(AKTØR_0.getId(), AKTØR_1.getId(),
            AKTØR_2.getId());
    }

    @Test
    public void skal_bare_sende_fnr_vider_til_pdp() {
        AbacAttributtSamling attributter = byggAbacAttributtSamling();
        attributter.leggTil(AbacDataAttributter.opprett().leggTil(StandardAbacAttributtType.FNR, PERSON_0));

        PdpRequest request = requestBuilder.lagPdpRequest(attributter);
        assertThat(request.getListOfString(AbacAttributter.RESOURCE_FELLES_PERSON_FNR)).containsOnly(PERSON_0);
    }

    @Test
    public void skal_ta_inn_aksjonspunkt_id_og_sende_videre_aksjonspunkt_typer() {
        AbacAttributtSamling attributter = byggAbacAttributtSamling();
        attributter.leggTil(AbacDataAttributter.opprett()
            .leggTil(StandardAbacAttributtType.AKSJONSPUNKT_KODE, "0000")
            .leggTil(StandardAbacAttributtType.AKSJONSPUNKT_KODE, "0001"));

        Set<String> koder = new HashSet<>();
        koder.add("0000");
        koder.add("0001");
        Set<String> svar = new HashSet<>();
        svar.add("Overstyring");
        svar.add("Manuell");
        Mockito.when(pipRepository.hentAksjonspunktTypeForAksjonspunktKoder(koder)).thenReturn(svar);

        PdpRequest request = requestBuilder.lagPdpRequest(attributter);
        assertThat(request.getListOfString(AbacAttributter.RESOURCE_AKSJONSPUNKT_TYPE)).containsOnly("Overstyring", "Manuell");
    }

    @Test
    public void skal_slå_opp_og_sende_videre_fnr_når_aktør_id_er_input() {
        AbacAttributtSamling attributter = byggAbacAttributtSamling();
        attributter.leggTil(AbacDataAttributter.opprett().leggTil(StandardAbacAttributtType.AKTØR_ID, AKTØR_1.getId()));

        PdpRequest request = requestBuilder.lagPdpRequest(attributter);
        assertThat(request.getListOfString(AbacAttributter.RESOURCE_FELLES_PERSON_AKTOERID_RESOURCE)).containsOnly(AKTØR_1.getId());
    }

    @Test
    public void skal_ikke_godta_at_det_sendes_inn_saksnummer_som_ikke_finnes() {

        // Arrange
        AbacAttributtSamling attributter = byggAbacAttributtSamling();
        attributter.leggTil(AbacDataAttributter.opprett().leggTil(StandardAbacAttributtType.SAKSNUMMER, new Saksnummer("ABC")));

        // Assert
        Assertions.assertThrows(UkjentFagsakException.class, () -> {

            // Act
            requestBuilder.lagPdpRequest(attributter);

        });
    }

    @Test
    public void skal_ikke_godta_at_det_sendes_inn_behandling_id_som_ikke_finnes() {

        // Arrange
        AbacAttributtSamling attributter = byggAbacAttributtSamling();
        attributter.leggTil(AbacDataAttributter.opprett().leggTil(StandardAbacAttributtType.BEHANDLING_ID, 1234L));
        String saksnummer = "ABC";
        when(pipRepository.hentDataForBehandling(1234L)).thenReturn(Optional.empty());

        // Assert
        Assertions.assertThrows(UkjentBehandlingException.class, () -> {

            // Act
            requestBuilder.lagPdpRequest(attributter);

        });
    }


    @Test
    public void skal_ikke_godta_at_det_sendes_inn_saksnummer_og_behandling_id_som_ikke_stemmer_overens() {

        // Arrange
        AbacAttributtSamling attributter = byggAbacAttributtSamling();
        attributter.leggTil(AbacDataAttributter.opprett().leggTil(StandardAbacAttributtType.SAKSNUMMER, new Saksnummer("XXX")));
        attributter.leggTil(AbacDataAttributter.opprett().leggTil(StandardAbacAttributtType.BEHANDLING_ID, 1234L));
        when(pipRepository.finnSaksnumerSomEksisterer(any())).thenReturn(Set.of(new Saksnummer("XXX")));
        when(pipRepository.hentDataForBehandling(1234L)).thenReturn(
            Optional.of(new PipBehandlingsData(UUID.randomUUID(), BehandlingStatus.OPPRETTET, FagsakStatus.OPPRETTET, "Z1234", new Saksnummer("ABC"))));

        // Assert
        Assertions.assertThrows(ManglerTilgangException.class, () -> {

            // Act
            requestBuilder.lagPdpRequest(attributter);

        });
    }

    private AbacAttributtSamling byggAbacAttributtSamling() {
        AbacAttributtSamling attributtSamling = AbacAttributtSamling.medJwtToken(DUMMY_ID_TOKEN);
        attributtSamling.setActionType(BeskyttetRessursActionAttributt.READ);
        attributtSamling.setResource(FAGSAK_ABAC_REFERANSE);
        return attributtSamling;
    }

}
