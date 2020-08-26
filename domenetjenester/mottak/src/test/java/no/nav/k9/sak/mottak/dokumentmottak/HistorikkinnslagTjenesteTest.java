package no.nav.k9.sak.mottak.dokumentmottak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import no.nav.k9.kodeverk.dokument.ArkivFilType;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.k9.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.k9.sak.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.saf.DokumentInfo;
import no.nav.saf.Dokumentvariant;
import no.nav.saf.Journalpost;
import no.nav.saf.SkjermingType;
import no.nav.saf.Variantformat;
import no.nav.vedtak.felles.integrasjon.saf.SafTjeneste;


public class HistorikkinnslagTjenesteTest {

    private static final JournalpostId JOURNALPOST_ID = new JournalpostId("5");
    private static final String HOVEDDOKUMENT_DOKUMENT_ID = "1";
    private static final String VEDLEGG_DOKUMENT_ID = "2";

    private HistorikkRepository historikkRepository;
    private SafTjeneste journalTjeneste;
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;

    @Before
    public void before() {
        historikkRepository = mock(HistorikkRepository.class);
        journalTjeneste = mock(SafTjeneste.class);
        historikkinnslagTjeneste = new HistorikkinnslagTjeneste(historikkRepository, journalTjeneste);
    }

    @Test
    public void skal_lagre_historikkinnslag_for_elektronisk_søknad() throws Exception {
        var scenario = TestScenarioBuilder.builderMedSøknad();

        Behandling behandling = scenario.lagMocked();
        // Arrange

        var hoveddokument = byggJournalMetadata(HOVEDDOKUMENT_DOKUMENT_ID, Variantformat.ORIGINAL, Variantformat.ARKIV);
        var vedlegg = byggJournalMetadata(VEDLEGG_DOKUMENT_ID, Variantformat.ORIGINAL);
        var respons = new Journalpost();
        respons.setJournalpostId(JOURNALPOST_ID.getVerdi());
        respons.setDokumenter(List.of(hoveddokument, vedlegg));

        when(journalTjeneste.hentJournalpostInfo(any(), any())).thenReturn(respons);

        // Act
        historikkinnslagTjeneste.opprettHistorikkinnslag(behandling, JOURNALPOST_ID, HistorikkinnslagType.BEH_STARTET);

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
        historikkinnslagTjeneste.opprettHistorikkinnslag(behandling, JOURNALPOST_ID, HistorikkinnslagType.BEH_STARTET);

        // Assert
        verify(historikkRepository, times(0)).lagre(any(Historikkinnslag.class));
    }

    private DokumentInfo byggJournalMetadata(String dokumentId, Variantformat... variantFormater) {
        var varianter = Arrays.stream(variantFormater)
            .map(variantFormat -> new Dokumentvariant(variantFormat, "filnavn", "filuuid",
                Variantformat.ORIGINAL.equals(variantFormat) ? ArkivFilType.XML.getKode() : ArkivFilType.PDF.getKode(),
                true, SkjermingType.POL))
            .collect(Collectors.toList());

        var dokumentInfo = new DokumentInfo();
        dokumentInfo.setDokumentvarianter(varianter);
        dokumentInfo.setDokumentInfoId(dokumentId);
        dokumentInfo.setBrevkode(DokumentTypeId.LEGEERKLÆRING.getOffisiellKode());
        return dokumentInfo;
    }
}
