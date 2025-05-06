package no.nav.ung.sak.innsyn.hendelse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import no.nav.k9.innsyn.InnsynHendelse;
import no.nav.k9.innsyn.sak.Behandling;
import no.nav.k9.innsyn.sak.Fagsak;
import no.nav.k9.innsyn.sak.InnsendingInfo;
import no.nav.k9.innsyn.sak.InnsendingStatus;
import no.nav.k9.innsyn.sak.InnsendingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.innsyn.BrukerdialoginnsynMeldingProducer;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.k9.søknad.JsonUtils;

class InnsynEventTjenesteTest {
    private final MottatteDokumentRepository mottatteDokumentRepository = mock();
    private final AksjonspunktTestSupport aksjonspunktTestSupport = new AksjonspunktTestSupport();
    private final BrukerdialoginnsynMeldingProducer producer = mock();
    private BehandlingRepository behandlingRepository;

    @Test
    void nyBehandlingEvent() {
        var mor = AktørId.dummy();
        var now = LocalDateTime.now();
        var søknadJpId = "123";
        var ettersendelseJpId = "456";
        var venteFrist = now.plusWeeks(4);

        TestScenarioBuilder testScenarioBuilder = TestScenarioBuilder
            .builderMedSøknad(FagsakYtelseType.PLEIEPENGER_SYKT_BARN, mor);

        behandlingRepository = testScenarioBuilder.mockBehandlingRepository();
        var behandling = testScenarioBuilder.lagMocked();
        Aksjonspunkt aksjonspunkt = aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKTUTTALELSE);
        aksjonspunktTestSupport.setFrist(aksjonspunkt,  venteFrist, Venteårsak.FOR_TIDLIG_SOKNAD, null);

        no.nav.ung.sak.behandlingslager.fagsak.Fagsak fagsak = behandling.getFagsak();

        //TODO egen test for mottatt, behandler og gyldig + ugyldig?
        when(mottatteDokumentRepository.hentMottatteDokumentForBehandling(
            anyLong(),
            anyLong(),
            anyList(),
            anyBoolean(),
            ArgumentMatchers.any(DokumentStatus[].class)))
            .thenReturn(List.of(
                byggMottattDokument(fagsak.getId(), behandling.getId(), now, søknadJpId, Brevkode.PLEIEPENGER_BARN_SOKNAD),
                byggMottattDokument(fagsak.getId(), behandling.getId(), now, ettersendelseJpId, Brevkode.ETTERSENDELSE_PLEIEPENGER_SYKT_BARN)
                ));

        var tjeneste = new InnsynEventTjeneste(
            mottatteDokumentRepository,
            producer);

        tjeneste.publiserBehandling(behandlingRepository.hentBehandling(behandling.getId()));

        var captor = ArgumentCaptor.forClass(String.class);
        verify(producer).send(eq(fagsak.getSaksnummer().getVerdi()), captor.capture());

        String json = captor.getValue();
        InnsynHendelse<Behandling> behandlingInnsynHendelse = JsonUtils.fromString(json, InnsynHendelse.class);
        assertThat(behandlingInnsynHendelse.getOppdateringstidspunkt()).isNotNull();

        var b = behandlingInnsynHendelse.getData();

        Fagsak sak = b.fagsak();
        assertThat(sak.saksnummer().verdi()).isEqualTo(fagsak.getSaksnummer().getVerdi());
        assertThat(sak.søkerAktørId().id()).isEqualTo(mor.getId());
        assertThat(sak.ytelseType().getKode()).isEqualTo(FagsakYtelseType.PLEIEPENGER_SYKT_BARN.getKode());

        assertThat(b.behandlingsId()).isEqualTo(behandling.getUuid());
        assertThat(b.erUtenlands()).isEqualTo(false);
        assertThat(b.status()).isEqualTo(no.nav.k9.innsyn.sak.BehandlingStatus.PÅ_VENT);
        assertThat(b.avsluttetTidspunkt()).isNull();
        assertThat(b.opprettetTidspunkt()).isEqualTo(behandling.getOpprettetDato().atZone(ZoneId.systemDefault()));

        var aksjonspunkter = b.aksjonspunkter();
        assertThat(aksjonspunkter).hasSize(1);
        assertThat(aksjonspunkter).allSatisfy(it -> {
            assertThat(it.tidsfrist()).isCloseTo(venteFrist.atZone(ZoneId.systemDefault()), within(1, ChronoUnit.MILLIS));
            assertThat(it.venteårsak()).isEqualTo(no.nav.k9.innsyn.sak.Aksjonspunkt.Venteårsak.FOR_TIDLIG_SOKNAD);
        });


        Set<InnsendingInfo> innsendinger = b.innsendinger();
        assertThat(innsendinger).hasSize(2);
        assertThat(innsendinger).allSatisfy(it -> {
            assertThat(it.mottattTidspunkt()).isCloseTo(now.atZone(ZoneId.systemDefault()), within(1, ChronoUnit.MILLIS));
            assertThat(it.status()).isEqualTo(InnsendingStatus.MOTTATT);
            assertThat(it.kildesystem()).isNull();

        });

        var søknadInnsending = innsendinger.stream().filter(it -> it.journalpostId().equals(søknadJpId)).findFirst().orElseThrow();
        assertThat(søknadInnsending.type()).isEqualTo(InnsendingType.SØKNAD);

        var ettersendelseInnsending = innsendinger.stream().filter(it -> it.journalpostId().equals(ettersendelseJpId)).findFirst().orElseThrow();
        assertThat(ettersendelseInnsending.type()).isEqualTo(InnsendingType.ETTERSENDELSE);


    }

    public static MottattDokument byggMottattDokument(Long fagsakId, Long behandlingId, LocalDateTime mottattDato, String journalpostId, Brevkode brevkode) {
        MottattDokument.Builder builder = new MottattDokument.Builder();
        builder.medMottattTidspunkt(mottattDato);
        builder.medType(brevkode);
        builder.medPayload("payload");
        builder.medFagsakId(fagsakId);
        if (journalpostId != null) {
            builder.medJournalPostId(new JournalpostId(journalpostId));
        }
        builder.medBehandlingId(behandlingId);
        return builder.build();
    }
}
