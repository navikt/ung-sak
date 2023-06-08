package no.nav.k9.sak.behandling.revurdering.etterkontroll.tjeneste;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import no.nav.k9.formidling.kontrakt.kodeverk.DokumentMalType;
import no.nav.k9.formidling.kontrakt.kodeverk.IdType;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.Etterkontroll;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.KontrollType;
import no.nav.k9.sak.behandling.revurdering.etterkontroll.saksbehandlingstid.ForsinketSaksbehandlingEtterkontroll;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.dokument.bestill.DokumentBestillerApplikasjonTjeneste;
import no.nav.k9.sak.kontrakt.dokument.BestillBrevDto;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;

class ForsinketSaksbehandlingEtterkontrollTest {

    private final DokumentBestillerApplikasjonTjeneste dokumentbestiller = mock();
    private final TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad();
    private final ForsinketSaksbehandlingEtterkontroll forsinketSaksbehandlingKontroll =
        new ForsinketSaksbehandlingEtterkontroll(dokumentbestiller, scenarioBuilder.mockBehandlingRepository());

    @Test
    void skal_bestille_brev_hvis_behandling_er_åpen() {
        Behandling b = scenarioBuilder.medBehandlingStatus(BehandlingStatus.OPPRETTET).lagMocked();

        Etterkontroll etterkontroll = new Etterkontroll.Builder(b)
            .medKontrollType(KontrollType.FORSINKET_SAKSBEHANDLINGSTID)
            .medKontrollTidspunkt(LocalDateTime.now())
            .build();

        boolean resultat = forsinketSaksbehandlingKontroll.utfør(etterkontroll);
        assertThat(resultat).isTrue();

        verifyBestillDokument(b);
    }

    private void verifyBestillDokument(Behandling b) {
        ArgumentCaptor<BestillBrevDto> c = ArgumentCaptor.forClass(BestillBrevDto.class);
        verify(dokumentbestiller).bestillDokument(c.capture(), eq(HistorikkAktør.VEDTAKSLØSNINGEN));
        BestillBrevDto brev = c.getValue();

        assertThat(brev.getBrevmalkode()).isEqualTo(DokumentMalType.FORLENGET_DOK.getKode());
        assertThat(brev.getOverstyrtMottaker().id).isEqualTo(b.getAktørId().getId());
        assertThat(brev.getOverstyrtMottaker().type).isEqualTo(IdType.AKTØRID.toString());
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

}
