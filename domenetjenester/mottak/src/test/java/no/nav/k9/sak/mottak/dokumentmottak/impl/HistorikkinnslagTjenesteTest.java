package no.nav.k9.sak.mottak.dokumentmottak.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.foreldrepenger.dokumentarkiv.journal.JournalMetadata;
import no.nav.foreldrepenger.dokumentarkiv.journal.JournalTjeneste;
import no.nav.k9.kodeverk.dokument.ArkivFilType;
import no.nav.k9.kodeverk.dokument.DokumentKategori;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.kodeverk.dokument.VariantFormat;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.JournalpostId;

public class HistorikkinnslagTjenesteTest {

    private static final JournalpostId JOURNALPOST_ID = new JournalpostId("5");
    private static final String HOVEDDOKUMENT_DOKUMENT_ID = "1";
    private static final String VEDLEGG_DOKUMENT_ID = "2";

    private HistorikkRepository historikkRepository;
    private JournalTjeneste journalTjeneste;
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;

    @Before
    public void before() {
        historikkRepository = mock(HistorikkRepository.class);
        journalTjeneste = mock(JournalTjeneste.class);
        historikkinnslagTjeneste = new HistorikkinnslagTjeneste(historikkRepository, journalTjeneste);
    }

    @Test
    public void skal_lagre_historikkinnslag_for_elektronisk_søknad() throws Exception {
        var scenario = TestScenarioBuilder.builderMedSøknad();

        Behandling behandling = scenario.lagMocked();
        // Arrange

        JournalMetadata journalMetadataHoveddokumentXml = byggJournalMetadata(JOURNALPOST_ID, HOVEDDOKUMENT_DOKUMENT_ID, ArkivFilType.XML, true, VariantFormat.FULLVERSJON);
        JournalMetadata journalMetadataHoveddokumentPdf = byggJournalMetadata(JOURNALPOST_ID, HOVEDDOKUMENT_DOKUMENT_ID, ArkivFilType.PDF, true, VariantFormat.ARKIV);
        JournalMetadata journalMetadataVedlegg = byggJournalMetadata(JOURNALPOST_ID, VEDLEGG_DOKUMENT_ID, ArkivFilType.XML, false, VariantFormat.FULLVERSJON);

        when(journalTjeneste.hentMetadata(JOURNALPOST_ID)).thenReturn(List.of(journalMetadataHoveddokumentXml, journalMetadataHoveddokumentPdf, journalMetadataVedlegg));

        // Act
        historikkinnslagTjeneste.opprettHistorikkinnslag(behandling, JOURNALPOST_ID);

        // Assert
        ArgumentCaptor<Historikkinnslag> captor = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkRepository, times(1)).lagre(captor.capture());
        Historikkinnslag historikkinnslag = captor.getValue();
        assertThat(historikkinnslag.getAktør()).isEqualTo(HistorikkAktør.SØKER);
        assertThat(historikkinnslag.getType()).isEqualTo(HistorikkinnslagType.BEH_STARTET);
        assertThat(historikkinnslag.getHistorikkinnslagDeler()).isNotEmpty();

        List<HistorikkinnslagDokumentLink> dokumentLinker = historikkinnslag.getDokumentLinker();
        assertThat(dokumentLinker).hasSize(2);
        assertThat(dokumentLinker.get(0).getDokumentId()).isEqualTo(HOVEDDOKUMENT_DOKUMENT_ID);
        assertThat(dokumentLinker.get(0).getJournalpostId()).isEqualTo(JOURNALPOST_ID);
        assertThat(dokumentLinker.get(0).getLinkTekst()).isEqualTo("Søknad");
        assertThat(dokumentLinker.get(1).getDokumentId()).isEqualTo(VEDLEGG_DOKUMENT_ID);
        assertThat(dokumentLinker.get(1).getJournalpostId()).isEqualTo(JOURNALPOST_ID);
        assertThat(dokumentLinker.get(1).getLinkTekst()).isEqualTo("Vedlegg");
    }

    @Test
    public void skal_ikke_lagre_historikkinnslag_når_det_allerede_finnes() throws Exception {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        Behandling behandling = scenario.lagMocked();

        Historikkinnslag eksisterendeHistorikkinnslag = new Historikkinnslag();
        eksisterendeHistorikkinnslag.setType(HistorikkinnslagType.BEH_STARTET);
        when(historikkRepository.hentHistorikk(behandling.getId())).thenReturn(Collections.singletonList(eksisterendeHistorikkinnslag));

        // Act
        historikkinnslagTjeneste.opprettHistorikkinnslag(behandling, JOURNALPOST_ID);

        // Assert
        verify(historikkRepository, times(0)).lagre(any(Historikkinnslag.class));
    }

    private JournalMetadata byggJournalMetadata(JournalpostId journalpostId, String dokumentId, ArkivFilType arkivFiltype, boolean hoveddokument, VariantFormat variantFormat) {
        JournalMetadata.Builder builderHoveddok = JournalMetadata.builder();
        builderHoveddok.medJournalpostId(journalpostId);
        builderHoveddok.medDokumentId(dokumentId);
        builderHoveddok.medVariantFormat(variantFormat);
        builderHoveddok.medDokumentType(DokumentTypeId.LEGEERKLÆRING);
        builderHoveddok.medDokumentKategori(DokumentKategori.BRV);
        builderHoveddok.medArkivFilType(arkivFiltype);
        builderHoveddok.medErHoveddokument(hoveddokument);
        builderHoveddok.medForsendelseMottatt(LocalDate.now());
        builderHoveddok.medBrukerIdentListe(Collections.singletonList("01234567890"));
        JournalMetadata journalMetadataHoveddokument = builderHoveddok.build();
        return journalMetadataHoveddokument;
    }
}
