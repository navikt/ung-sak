package no.nav.k9.sak.dokument.arkiv;

import static no.nav.vedtak.felles.integrasjon.felles.ws.DateUtil.convertToXMLGregorianCalendar;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.k9.kodeverk.dokument.ArkivFilType;
import no.nav.k9.kodeverk.dokument.DokumentKategori;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.kodeverk.dokument.Kommunikasjonsretning;
import no.nav.k9.kodeverk.dokument.VariantFormat;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentDokumentIkkeFunnet;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentJournalpostIkkeFunnet;
import no.nav.tjeneste.virksomhet.journal.v3.HentDokumentSikkerhetsbegrensning;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.Arkivfiltyper;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.Dokumentkategorier;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.Journalposttyper;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.Variantformater;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.DetaljertDokumentinformasjon;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.DokumentInnhold;
import no.nav.tjeneste.virksomhet.journal.v3.informasjon.hentkjernejournalpostliste.Journalpost;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentResponse;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentKjerneJournalpostListeRequest;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentKjerneJournalpostListeResponse;
import no.nav.vedtak.felles.integrasjon.journal.v3.JournalConsumer;

public class DokumentArkivTjenesteImplTest {

    private static final JournalpostId JOURNAL_ID = new JournalpostId("42");
    private static final String DOKUMENT_ID = "66";
    private static final Saksnummer KJENT_SAK = new Saksnummer("123456");
    private static final LocalDateTime NOW = LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 10));
    private static final LocalDateTime YESTERDAY = LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(10, 10));

    private DokumentArkivTjeneste dokumentApplikasjonTjeneste;
    private JournalConsumer mockJournalProxyService;
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Before
    public void setUp() {
        mockJournalProxyService = mock(JournalConsumer.class);
        final FagsakRepository fagsakRepository = mock(FagsakRepository.class);
        final Fagsak fagsak = mock(Fagsak.class);
        final Optional<Fagsak> mock1 = Optional.of(fagsak);
        when(fagsakRepository.hentSakGittSaksnummer(any(Saksnummer.class))).thenReturn(mock1);
        dokumentApplikasjonTjeneste = new DokumentArkivTjeneste(mockJournalProxyService, fagsakRepository);
    }

    @Test
    public void skalRetunereDokumentListeMedJournalpostTypeInn() throws Exception {
        HentKjerneJournalpostListeResponse hentJournalpostListeResponse = new HentKjerneJournalpostListeResponse();
        hentJournalpostListeResponse.getJournalpostListe().add(
            createJournalpost(ArkivFilType.PDF, VariantFormat.ARKIV, YESTERDAY, NOW,  "U"));
        when(mockJournalProxyService.hentKjerneJournalpostListe(any(HentKjerneJournalpostListeRequest.class))).thenReturn(hentJournalpostListeResponse);

        List<ArkivJournalPost> arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterForVisning(KJENT_SAK);

        assertThat(arkivDokuments).isNotEmpty();
        ArkivJournalPost arkivJournalPost = arkivDokuments.get(0);
        ArkivDokument arkivDokument = arkivJournalPost.getHovedDokument();
        assertThat(arkivJournalPost.getJournalpostId()).isEqualTo(JOURNAL_ID);
        assertThat(arkivDokument.getDokumentId()).isEqualTo(DOKUMENT_ID);
        assertThat(arkivJournalPost.getTidspunkt()).isEqualTo(YESTERDAY);
        assertThat(arkivJournalPost.getKommunikasjonsretning()).isEqualTo(Kommunikasjonsretning.UT);
    }

    @Test
    public void skalRetunereDokumentListeMedJournalpostTypeUt() throws Exception {
        HentKjerneJournalpostListeResponse hentJournalpostListeResponse = new HentKjerneJournalpostListeResponse();
        hentJournalpostListeResponse.getJournalpostListe().add(
            createJournalpost(ArkivFilType.PDFA, VariantFormat.ARKIV, YESTERDAY, NOW,"I"));
        when(mockJournalProxyService.hentKjerneJournalpostListe(any(HentKjerneJournalpostListeRequest.class))).thenReturn(hentJournalpostListeResponse);

        List<ArkivJournalPost> arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterForVisning(KJENT_SAK);

        assertThat(arkivDokuments.get(0).getTidspunkt()).isEqualTo(YESTERDAY);
        assertThat(arkivDokuments.get(0).getKommunikasjonsretning()).isEqualTo(Kommunikasjonsretning.INN);
    }

    @Test
    public void skalRetunereDokumentListeMedUansettInnhold() throws Exception {
        HentKjerneJournalpostListeResponse hentJournalpostListeResponse = new HentKjerneJournalpostListeResponse();
        hentJournalpostListeResponse.getJournalpostListe().addAll(Arrays.asList(
            createJournalpost(ArkivFilType.PDFA, VariantFormat.ARKIV, YESTERDAY, NOW,"I"),
            createJournalpost(ArkivFilType.XLS, VariantFormat.ARKIV)));
        when(mockJournalProxyService.hentKjerneJournalpostListe(any(HentKjerneJournalpostListeRequest.class))).thenReturn(hentJournalpostListeResponse);

        Optional<ArkivJournalPost> arkivDokument = dokumentApplikasjonTjeneste.hentJournalpostForSak(KJENT_SAK, JOURNAL_ID);

        assertThat(arkivDokument).isPresent();
        assertThat(arkivDokument.get().getAndreDokument()).isEmpty();
    }

    @Test
    public void skalRetunereDokumenterAvFiltypePDF() throws Exception {
        HentKjerneJournalpostListeResponse hentJournalpostListeResponse = new HentKjerneJournalpostListeResponse();
        hentJournalpostListeResponse.getJournalpostListe().addAll(Arrays.asList(createJournalpost(ArkivFilType.XML, VariantFormat.ARKIV),
            createJournalpost(ArkivFilType.PDF, VariantFormat.ARKIV),
            createJournalpost(ArkivFilType.XML, VariantFormat.ARKIV)));
        when(mockJournalProxyService.hentKjerneJournalpostListe(any(HentKjerneJournalpostListeRequest.class))).thenReturn(hentJournalpostListeResponse);

        List<ArkivJournalPost> arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterForVisning(KJENT_SAK);

        assertThat(arkivDokuments).hasSize(1);
    }

    @Test
    public void skalRetunereDokumenterAvVariantFormatARKIV() throws Exception {
        HentKjerneJournalpostListeResponse hentJournalpostListeResponse = new HentKjerneJournalpostListeResponse();
        hentJournalpostListeResponse.getJournalpostListe().addAll(Arrays.asList(createJournalpost(ArkivFilType.XML, VariantFormat.ORIGINAL),
            createJournalpost(ArkivFilType.PDF, VariantFormat.ARKIV),
            createJournalpost(ArkivFilType.PDFA, VariantFormat.ARKIV),
            createJournalpost(ArkivFilType.XML, VariantFormat.ORIGINAL)));
        when(mockJournalProxyService.hentKjerneJournalpostListe(any(HentKjerneJournalpostListeRequest.class))).thenReturn(hentJournalpostListeResponse);

        List<ArkivJournalPost> arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterForVisning(KJENT_SAK);

        assertThat(arkivDokuments).hasSize(2);
    }

    @Test
    public void skalRetunereDokumentListeMedSisteTidspunktØverst() throws Exception {
        HentKjerneJournalpostListeResponse hentJournalpostListeResponse = new HentKjerneJournalpostListeResponse();
        hentJournalpostListeResponse.getJournalpostListe().addAll(Arrays.asList(
            createJournalpost(ArkivFilType.PDFA, VariantFormat.ARKIV, NOW, NOW, "U"),
            createJournalpost(ArkivFilType.PDFA, VariantFormat.ARKIV, YESTERDAY.minusDays(1), YESTERDAY, "I")));
        when(mockJournalProxyService.hentKjerneJournalpostListe(any(HentKjerneJournalpostListeRequest.class))).thenReturn(hentJournalpostListeResponse);

        List<ArkivJournalPost> arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterForVisning(KJENT_SAK);

        assertThat(arkivDokuments.get(0).getTidspunkt()).isEqualTo(NOW);
        assertThat(arkivDokuments.get(0).getKommunikasjonsretning()).isEqualTo(Kommunikasjonsretning.UT);
        assertThat(arkivDokuments.get(1).getTidspunkt()).isEqualTo(YESTERDAY.minusDays(1));
        assertThat(arkivDokuments.get(1).getKommunikasjonsretning()).isEqualTo(Kommunikasjonsretning.INN);
    }

    @Test
    public void skalRetunereAlleDokumentTyper() throws Exception {
        HentKjerneJournalpostListeResponse hentJournalpostListeResponse = new HentKjerneJournalpostListeResponse();
        hentJournalpostListeResponse.getJournalpostListe().addAll(Arrays.asList(
            createJournalpost(ArkivFilType.PDFA, VariantFormat.ARKIV, NOW, NOW, "U"),
            createJournalpost(ArkivFilType.PDFA, VariantFormat.ARKIV, YESTERDAY.minusDays(1), YESTERDAY, "I")));
        hentJournalpostListeResponse.getJournalpostListe().get(0).withVedleggListe(createDokumentinfoRelasjon(ArkivFilType.PDFA.getOffisiellKode(), VariantFormat.ARKIV.getOffisiellKode()));
        when(mockJournalProxyService.hentKjerneJournalpostListe(any(HentKjerneJournalpostListeRequest.class))).thenReturn(hentJournalpostListeResponse);

        Set<DokumentTypeId> arkivDokumentTypeIds = dokumentApplikasjonTjeneste.hentDokumentTypeIdForSak(KJENT_SAK, LocalDate.MIN);

        assertThat(arkivDokumentTypeIds).hasSize(1);
    }

    @Test
    public void skalRetunereDokumentTyperSiden() throws Exception {
        HentKjerneJournalpostListeResponse hentJournalpostListeResponse = new HentKjerneJournalpostListeResponse();
        hentJournalpostListeResponse.getJournalpostListe().addAll(Arrays.asList(
            createJournalpost(ArkivFilType.PDFA, VariantFormat.ARKIV, NOW, NOW, "U"),
            createJournalpost(ArkivFilType.PDFA, VariantFormat.ARKIV, YESTERDAY.minusDays(1), YESTERDAY, "I")));
        hentJournalpostListeResponse.getJournalpostListe().get(0).withVedleggListe(createDokumentinfoRelasjon(ArkivFilType.PDFA.getOffisiellKode(), VariantFormat.ARKIV.getOffisiellKode()));

        when(mockJournalProxyService.hentKjerneJournalpostListe(any(HentKjerneJournalpostListeRequest.class))).thenReturn(hentJournalpostListeResponse);

        Set<DokumentTypeId> arkivDokumentTypeIds = dokumentApplikasjonTjeneste.hentDokumentTypeIdForSak(KJENT_SAK, NOW.toLocalDate());

        assertThat(arkivDokumentTypeIds).hasSize(1);
    }

    @Test
    public void skal_kalle_web_service_og_oversette_fra_() throws HentDokumentDokumentIkkeFunnet, HentDokumentJournalpostIkkeFunnet, HentDokumentSikkerhetsbegrensning {
        // Arrange

        final byte[] bytesExpected = {1, 2, 7};
        HentDokumentResponse response = new HentDokumentResponse();
        response.setDokument(bytesExpected);
        when(mockJournalProxyService.hentDokument(any())).thenReturn(response);

        // Act

        byte[] bytesActual = dokumentApplikasjonTjeneste.hentDokumnet(new JournalpostId("123"), "456");

        // Assert
        assertThat(bytesActual).isEqualTo(bytesExpected);
    }

    private Journalpost createJournalpost(ArkivFilType arkivFilTypeKonst, VariantFormat variantFormatKonst) throws DatatypeConfigurationException {
        return createJournalpost(arkivFilTypeKonst, variantFormatKonst, NOW, NOW, "U");
    }

    private Journalpost createJournalpost(ArkivFilType arkivFilTypeKonst, VariantFormat variantFormatKonst, LocalDateTime sendt, LocalDateTime mottatt, String kommunikasjonsretning) throws DatatypeConfigurationException {
        Journalpost journalpost = new Journalpost();
        journalpost.setJournalpostId(JOURNAL_ID.getVerdi());
        journalpost.setHoveddokument(createDokumentinfoRelasjon(arkivFilTypeKonst.getOffisiellKode(), variantFormatKonst.getOffisiellKode()));
        Journalposttyper kommunikasjonsretninger = new Journalposttyper();
        kommunikasjonsretninger.setValue(kommunikasjonsretning);
        journalpost.setJournalposttype(kommunikasjonsretninger);
        journalpost.setForsendelseJournalfoert(convertToXMLGregorianCalendar(sendt));
        journalpost.setForsendelseMottatt(convertToXMLGregorianCalendar(mottatt));
        return journalpost;
    }

    private DetaljertDokumentinformasjon createDokumentinfoRelasjon(String filtype, String variantformat) {
        DetaljertDokumentinformasjon dokumentinfoRelasjon = new DetaljertDokumentinformasjon();
        dokumentinfoRelasjon.setDokumentId(DOKUMENT_ID);
        Dokumentkategorier dokumentkategorier = new Dokumentkategorier();
        dokumentkategorier.setValue(DokumentKategori.SØKNAD.getOffisiellKode());
        dokumentinfoRelasjon.setDokumentkategori(dokumentkategorier);
        DokumentInnhold dokumentInnhold = new DokumentInnhold();
        Arkivfiltyper arkivfiltyper = new Arkivfiltyper();
        arkivfiltyper.setValue(filtype);
        dokumentInnhold.setArkivfiltype(arkivfiltyper);
        Variantformater variantformater = new Variantformater();
        variantformater.setValue(variantformat);
        dokumentInnhold.setVariantformat(variantformater);
        dokumentinfoRelasjon.getDokumentInnholdListe().add(dokumentInnhold);
        return dokumentinfoRelasjon;
    }
}
