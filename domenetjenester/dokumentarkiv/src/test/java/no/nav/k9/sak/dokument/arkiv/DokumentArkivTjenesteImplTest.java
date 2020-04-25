package no.nav.k9.sak.dokument.arkiv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.k9.kodeverk.dokument.ArkivFilType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.kodeverk.dokument.Kommunikasjonsretning;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.dokument.arkiv.saf.SafTjeneste;
import no.nav.k9.sak.dokument.arkiv.saf.graphql.DokumentoversiktFagsakQuery;
import no.nav.k9.sak.dokument.arkiv.saf.graphql.HentDokumentQuery;
import no.nav.k9.sak.dokument.arkiv.saf.rest.model.Bruker;
import no.nav.k9.sak.dokument.arkiv.saf.rest.model.BrukerIdType;
import no.nav.k9.sak.dokument.arkiv.saf.rest.model.Datotype;
import no.nav.k9.sak.dokument.arkiv.saf.rest.model.DokumentInfo;
import no.nav.k9.sak.dokument.arkiv.saf.rest.model.DokumentoversiktFagsak;
import no.nav.k9.sak.dokument.arkiv.saf.rest.model.Dokumentvariant;
import no.nav.k9.sak.dokument.arkiv.saf.rest.model.Journalpost;
import no.nav.k9.sak.dokument.arkiv.saf.rest.model.RelevantDato;
import no.nav.k9.sak.dokument.arkiv.saf.rest.model.Sak;
import no.nav.k9.sak.dokument.arkiv.saf.rest.model.Sakstype;
import no.nav.k9.sak.dokument.arkiv.saf.rest.model.VariantFormat;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.tjeneste.virksomhet.journal.v3.meldinger.HentDokumentResponse;

public class DokumentArkivTjenesteImplTest {

    private static final JournalpostId JOURNAL_ID = new JournalpostId("42");
    private static final String DOKUMENT_ID = "66";
    private static final Saksnummer KJENT_SAK = new Saksnummer("123456");
    private static final LocalDateTime TID_JOURNALFØRT = LocalDateTime.of(LocalDate.now(), LocalTime.of(10, 10));
    private static final LocalDateTime TID_REGISTRERT = LocalDateTime.of(LocalDate.now().minusDays(1), LocalTime.of(10, 10));

    private DokumentArkivTjeneste dokumentApplikasjonTjeneste;
    private SafTjeneste safTjeneste;

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Before
    public void setUp() {
        final FagsakRepository fagsakRepository = mock(FagsakRepository.class);
        final Fagsak fagsak = mock(Fagsak.class);
        final Optional<Fagsak> mock1 = Optional.of(fagsak);
        when(fagsakRepository.hentSakGittSaksnummer(any(Saksnummer.class))).thenReturn(mock1);

        safTjeneste = mock(SafTjeneste.class);
        dokumentApplikasjonTjeneste = new DokumentArkivTjeneste(safTjeneste);
    }

    @Test
    public void skalRetunereDokumentListeMedJournalpostTypeUt() {
        Journalpost journalpost = byggJournalpost(ArkivFilType.PDF, VariantFormat.ARKIV, TID_REGISTRERT, TID_JOURNALFØRT, Kommunikasjonsretning.UT);
        DokumentoversiktFagsak dokumentoversiktFagsak = new DokumentoversiktFagsak(List.of(journalpost));
        when(safTjeneste.dokumentoversiktFagsak(any(DokumentoversiktFagsakQuery.class))).thenReturn(dokumentoversiktFagsak);

        List<ArkivJournalPost> arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterForVisning(KJENT_SAK);

        assertThat(arkivDokuments).isNotEmpty();
        ArkivJournalPost arkivJournalPost = arkivDokuments.get(0);
        ArkivDokument arkivDokument = arkivJournalPost.getHovedDokument();
        assertThat(arkivJournalPost.getJournalpostId()).isEqualTo(JOURNAL_ID);
        assertThat(arkivDokument.getDokumentId()).isEqualTo(DOKUMENT_ID);
        assertThat(arkivJournalPost.getTidspunkt()).isEqualTo(TID_JOURNALFØRT);
        assertThat(arkivJournalPost.getKommunikasjonsretning()).isEqualTo(Kommunikasjonsretning.UT);
    }

    @Test
    public void skalRetunereDokumentListeMedJournalpostTypeInn() {
        Journalpost journalpost = byggJournalpost(ArkivFilType.PDF, VariantFormat.ARKIV, TID_REGISTRERT, TID_JOURNALFØRT, Kommunikasjonsretning.INN);
        DokumentoversiktFagsak dokumentoversiktFagsak = new DokumentoversiktFagsak(List.of(journalpost));
        when(safTjeneste.dokumentoversiktFagsak(any(DokumentoversiktFagsakQuery.class))).thenReturn(dokumentoversiktFagsak);

        List<ArkivJournalPost> arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterForVisning(KJENT_SAK);

        assertThat(arkivDokuments.get(0).getTidspunkt()).isEqualTo(TID_JOURNALFØRT);
        assertThat(arkivDokuments.get(0).getKommunikasjonsretning()).isEqualTo(Kommunikasjonsretning.INN);
    }

    @Test
    public void skalRetunereDokumentListeMedUansettInnhold() {
        List<Journalpost> journalposter = List.of(
            byggJournalpost(ArkivFilType.PDF, VariantFormat.ARKIV, TID_REGISTRERT, TID_JOURNALFØRT, Kommunikasjonsretning.INN),
            byggJournalpost(ArkivFilType.XLS, VariantFormat.ARKIV)
        );
        DokumentoversiktFagsak dokumentoversiktFagsak = new DokumentoversiktFagsak(journalposter);
        when(safTjeneste.dokumentoversiktFagsak(any(DokumentoversiktFagsakQuery.class))).thenReturn(dokumentoversiktFagsak);

        Optional<ArkivJournalPost> arkivDokument = dokumentApplikasjonTjeneste.hentJournalpostForSak(KJENT_SAK, JOURNAL_ID);

        assertThat(arkivDokument).isPresent();
        assertThat(arkivDokument.get().getAndreDokument()).isEmpty();
    }

    @Test
    public void skalRetunereDokumenterAvFiltypePDF() {
        List<Journalpost> journalposter = List.of(
            byggJournalpost(ArkivFilType.PDF, VariantFormat.ARKIV),
            byggJournalpost(ArkivFilType.XLS, VariantFormat.ARKIV)
        );
        DokumentoversiktFagsak dokumentoversiktFagsak = new DokumentoversiktFagsak(journalposter);
        when(safTjeneste.dokumentoversiktFagsak(any(DokumentoversiktFagsakQuery.class))).thenReturn(dokumentoversiktFagsak);

        List<ArkivJournalPost> arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterForVisning(KJENT_SAK);

        assertThat(arkivDokuments).hasSize(1);
    }

    @Test
    public void skalRetunereDokumenttypeInntektsmelding() {
        Journalpost journalpost = byggJournalpostMedFlereDokumenter(List.of(
            byggDokumentInfo(ArkivFilType.PDF, VariantFormat.ARKIV, Brevkode.INNTEKTSMELDING),
            byggDokumentInfo(ArkivFilType.PDF, VariantFormat.ARKIV, Brevkode.UDEFINERT)
        ));
        DokumentoversiktFagsak dokumentoversiktFagsak = new DokumentoversiktFagsak(List.of(journalpost));
        when(safTjeneste.dokumentoversiktFagsak(any(DokumentoversiktFagsakQuery.class))).thenReturn(dokumentoversiktFagsak);

        List<ArkivJournalPost> arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterForVisning(KJENT_SAK);

        assertThat(arkivDokuments).hasSize(1);
        ArkivJournalPost post = arkivDokuments.get(0);
        assertThat(post.getHovedDokument().getDokumentType()).isEqualTo(DokumentTypeId.INNTEKTSMELDING);
        assertThat(post.getAndreDokument().get(0).getDokumentType()).isEqualTo(DokumentTypeId.UDEFINERT);
    }

    @Test
    public void skalRetunereDokumenterAvVariantFormatARKIV() {
        List<Journalpost> journalposter = List.of(
            byggJournalpost(ArkivFilType.PDF, VariantFormat.ARKIV),
            byggJournalpost(ArkivFilType.PDFA, VariantFormat.ARKIV),
            byggJournalpost(ArkivFilType.XML, VariantFormat.ORIGINAL)
        );
        DokumentoversiktFagsak dokumentoversiktFagsak = new DokumentoversiktFagsak(journalposter);
        when(safTjeneste.dokumentoversiktFagsak(any(DokumentoversiktFagsakQuery.class))).thenReturn(dokumentoversiktFagsak);

        List<ArkivJournalPost> arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterForVisning(KJENT_SAK);

        assertThat(arkivDokuments).hasSize(2);
    }

    @Test
    public void skalRetunereDokumentListeMedSisteTidspunktØverst() {
        List<Journalpost> journalposter = List.of(
            byggJournalpost(ArkivFilType.PDFA, VariantFormat.ARKIV, TID_REGISTRERT, TID_JOURNALFØRT, Kommunikasjonsretning.UT),
            byggJournalpost(ArkivFilType.PDFA, VariantFormat.ARKIV, TID_REGISTRERT, TID_JOURNALFØRT.minusDays(1), Kommunikasjonsretning.INN)
        );
        DokumentoversiktFagsak dokumentoversiktFagsak = new DokumentoversiktFagsak(journalposter);
        when(safTjeneste.dokumentoversiktFagsak(any(DokumentoversiktFagsakQuery.class))).thenReturn(dokumentoversiktFagsak);

        List<ArkivJournalPost> arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterForVisning(KJENT_SAK);

        assertThat(arkivDokuments.get(0).getTidspunkt()).isEqualTo(TID_JOURNALFØRT);
        assertThat(arkivDokuments.get(0).getKommunikasjonsretning()).isEqualTo(Kommunikasjonsretning.UT);
        assertThat(arkivDokuments.get(1).getTidspunkt()).isEqualTo(TID_JOURNALFØRT.minusDays(1));
        assertThat(arkivDokuments.get(1).getKommunikasjonsretning()).isEqualTo(Kommunikasjonsretning.INN);
    }

    @Test
    public void skalRetunereAlleDokumentTyper() {
        Journalpost journalpost = byggJournalpostMedFlereDokumenter(List.of(
            byggDokumentInfo(ArkivFilType.PDF, VariantFormat.ARKIV, Brevkode.INNTEKTSMELDING), // hoveddokument
            byggDokumentInfo(ArkivFilType.PDF, VariantFormat.ARKIV, Brevkode.UDEFINERT) // annet dokument
        ));
        DokumentoversiktFagsak dokumentoversiktFagsak = new DokumentoversiktFagsak(List.of(journalpost));
        when(safTjeneste.dokumentoversiktFagsak(any(DokumentoversiktFagsakQuery.class))).thenReturn(dokumentoversiktFagsak);

        Set<DokumentTypeId> arkivDokumentTypeIds = dokumentApplikasjonTjeneste.hentDokumentTypeIdForSak(KJENT_SAK, LocalDate.MIN);

        assertThat(arkivDokumentTypeIds).containsExactlyInAnyOrder(DokumentTypeId.INNTEKTSMELDING, DokumentTypeId.UDEFINERT);
    }

    @Test
    public void skalRetunereDokumentTyperSiden() {
        List<Journalpost> journalposter = List.of(
            byggJournalpost(ArkivFilType.PDFA, VariantFormat.ARKIV, Brevkode.INNTEKTSMELDING, TID_REGISTRERT, TID_JOURNALFØRT, Kommunikasjonsretning.UT),
            byggJournalpost(ArkivFilType.PDFA, VariantFormat.ARKIV, Brevkode.UDEFINERT, TID_REGISTRERT, TID_JOURNALFØRT.minusDays(1), Kommunikasjonsretning.INN)
        );

        DokumentoversiktFagsak dokumentoversiktFagsak = new DokumentoversiktFagsak(journalposter);
        when(safTjeneste.dokumentoversiktFagsak(any(DokumentoversiktFagsakQuery.class))).thenReturn(dokumentoversiktFagsak);

        Set<DokumentTypeId> arkivDokumentTypeIds = dokumentApplikasjonTjeneste.hentDokumentTypeIdForSak(KJENT_SAK, TID_JOURNALFØRT.toLocalDate());

        assertThat(arkivDokumentTypeIds).containsExactlyInAnyOrder(DokumentTypeId.INNTEKTSMELDING);
    }

    @Test
    public void skal_kalle_web_service_og_oversette_fra_() {
        // Arrange

        final byte[] bytesExpected = {1, 2, 7};
        HentDokumentResponse response = new HentDokumentResponse();
        response.setDokument(bytesExpected);
        when(safTjeneste.hentDokument(any(HentDokumentQuery.class))).thenReturn(bytesExpected);

        // Act

        byte[] bytesActual = dokumentApplikasjonTjeneste.hentDokumnet(new JournalpostId("123"), "456");

        // Assert
        assertThat(bytesActual).isEqualTo(bytesExpected);
    }

    // Master-bygger
    private Journalpost byggJournalpost(LocalDateTime registrertTid, LocalDateTime journalførtTid, Kommunikasjonsretning kommunikasjonsretning,
                                        List<DokumentInfo> dokumentInfoer) {

        return new Journalpost(JOURNAL_ID.getVerdi(),
                "tittel",
                kommunikasjonsretning.getKode(),
                "journalstatus",
                "kanal",
                "tema",
                "behandlingstema",
                new Sak("arkivsaksystem", "arkivsaksnummer", "fagsaksystem", "fagsakId", Sakstype.GENERELL_SAK),
                new Bruker("id", BrukerIdType.AKTOERID),
                "journalforendeEnhet",
                dokumentInfoer,
                List.of(
                    new RelevantDato(journalførtTid, Datotype.DATO_JOURNALFOERT),
                    new RelevantDato(registrertTid, Datotype.DATO_REGISTRERT)));
    }

    private DokumentInfo byggDokumentInfo(ArkivFilType arkivFilType, VariantFormat variantFormat, Brevkode brevkode) {
        return new DokumentInfo(DOKUMENT_ID, "tittel", brevkode.getOffisiellKode(),
            List.of(new Dokumentvariant(variantFormat, "filnavn", arkivFilType.name(), true)));
    }

    // Hjelpebyggere
    private Journalpost byggJournalpost(ArkivFilType arkivFilType, VariantFormat variantFormat) {
        return byggJournalpost(TID_JOURNALFØRT, TID_JOURNALFØRT, Kommunikasjonsretning.UT,
            List.of(byggDokumentInfo(arkivFilType, variantFormat, Brevkode.UDEFINERT)));
    }

    private Journalpost byggJournalpostMedFlereDokumenter(List<DokumentInfo> dokumentInfoer) {
        return byggJournalpost(TID_JOURNALFØRT, TID_JOURNALFØRT, Kommunikasjonsretning.UT, dokumentInfoer);
    }

    private Journalpost byggJournalpost(ArkivFilType arkivFilType,
                                        VariantFormat variantFormat,
                                        Brevkode brevkode,
                                        LocalDateTime registrertTid,
                                        LocalDateTime journalførtTid,
                                        Kommunikasjonsretning kommunikasjonsretning) {
        return byggJournalpost(registrertTid, journalførtTid, kommunikasjonsretning,
            List.of(byggDokumentInfo(arkivFilType, variantFormat, brevkode)));
    }

    private Journalpost byggJournalpost(ArkivFilType arkivFilType,
                                        VariantFormat variantFormat,
                                        LocalDateTime registrertTid,
                                        LocalDateTime journalførtTid,
                                        Kommunikasjonsretning kommunikasjonsretning) {
        return byggJournalpost(registrertTid, journalførtTid, kommunikasjonsretning,
            List.of(byggDokumentInfo(arkivFilType, variantFormat, Brevkode.UDEFINERT)));
    }
}
