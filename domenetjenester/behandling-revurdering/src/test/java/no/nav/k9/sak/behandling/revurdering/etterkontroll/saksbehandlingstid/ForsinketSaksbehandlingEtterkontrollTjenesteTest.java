package no.nav.k9.sak.behandling.revurdering.etterkontroll.saksbehandlingstid;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import no.nav.k9.formidling.kontrakt.kodeverk.DokumentMalType;
import no.nav.k9.formidling.kontrakt.kodeverk.IdType;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.Etterkontroll;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.KontrollType;
import no.nav.k9.sak.behandling.saksbehandlingstid.SaksbehandlingsfristUtleder;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.dokument.bestill.DokumentBestillerApplikasjonTjeneste;
import no.nav.k9.sak.kontrakt.dokument.BestillBrevDto;
import no.nav.k9.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;

class ForsinketSaksbehandlingEtterkontrollTjenesteTest {

    private final DokumentBestillerApplikasjonTjeneste dokumentbestiller = mock();
    private final SaksbehandlingsfristUtleder fristUtleder = mock();
    private final TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad();
    private final ForsinketSaksbehandlingEtterkontrollTjeneste forsinketSaksbehandlingKontroll =
        new ForsinketSaksbehandlingEtterkontrollTjeneste(
            dokumentbestiller,
            scenarioBuilder.mockBehandlingRepository(),
            new UnitTestLookupInstanceImpl<>(fristUtleder)
        );

    @BeforeEach
    void setup() {
        when(fristUtleder.utledFrist(any())).thenReturn(Optional.of(LocalDateTime.now()));
    }

    @Test
    void skal_bestille_brev_hvis_behandling_er_åpen() {
        Behandling b = scenarioBuilder.medBehandlingStatus(BehandlingStatus.OPPRETTET).lagMocked();

        Etterkontroll etterkontroll = spy(new Etterkontroll.Builder(b)
            .medKontrollType(KontrollType.FORSINKET_SAKSBEHANDLINGSTID)
            .medKontrollTidspunkt(LocalDateTime.now())
            .build());

        long etterkontrollId = 300623L;
        when(etterkontroll.getId()).thenReturn(etterkontrollId);

        boolean resultat = forsinketSaksbehandlingKontroll.utfør(etterkontroll);
        assertThat(resultat).isTrue();

        verifyBestillDokument(b, etterkontrollId);
    }

    private void verifyBestillDokument(Behandling b, long etterkontrollId) {
        ArgumentCaptor<BestillBrevDto> c = ArgumentCaptor.forClass(BestillBrevDto.class);
        verify(dokumentbestiller).bestillDokument(c.capture(), eq(HistorikkAktør.VEDTAKSLØSNINGEN));
        BestillBrevDto brev = c.getValue();

        assertThat(brev.getBrevmalkode()).isEqualTo(DokumentMalType.FORLENGET_DOK.getKode());
        assertThat(brev.getOverstyrtMottaker().id).isEqualTo(b.getAktørId().getId());
        assertThat(brev.getOverstyrtMottaker().type).isEqualTo(IdType.AKTØRID.toString());

        String uuidKilde = etterkontrollId + b.getUuid().toString() + DokumentMalType.FORLENGET_DOK.getKode();
        String uuid = UUID.nameUUIDFromBytes(uuidKilde.getBytes(StandardCharsets.UTF_8)).toString();

        assertThat(brev.getDokumentbestillingsId()).isEqualTo(uuid);
    }

    @Test
    void skal_ikke_bestille_brev_hvis_behandling_er_avsluttet() {
        Behandling b = scenarioBuilder.lagMocked();
        b.avsluttBehandling();

        Etterkontroll etterkontroll = new Etterkontroll.Builder(b)
            .medKontrollType(KontrollType.FORSINKET_SAKSBEHANDLINGSTID)
            .medKontrollTidspunkt(LocalDateTime.now())
            .build();


        boolean resultat = forsinketSaksbehandlingKontroll.utfør(etterkontroll);
        assertThat(resultat).isTrue();

        verifyNoInteractions(dokumentbestiller);
    }

    @Test
    void skal_ikke_bestille_brev_hvis_det_ikke_finnes_frist_lenger() {
        when(fristUtleder.utledFrist(any())).thenReturn(Optional.empty());
        Behandling b = scenarioBuilder.medBehandlingStatus(BehandlingStatus.OPPRETTET).lagMocked();

        Etterkontroll etterkontroll = new Etterkontroll.Builder(b)
            .medKontrollType(KontrollType.FORSINKET_SAKSBEHANDLINGSTID)
            .medKontrollTidspunkt(LocalDateTime.now())
            .build();

        boolean resultat = forsinketSaksbehandlingKontroll.utfør(etterkontroll);
        assertThat(resultat).isTrue();

        verifyNoInteractions(dokumentbestiller);
    }

}
