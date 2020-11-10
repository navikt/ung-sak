package no.nav.k9.sak.dokument.arkiv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.Rule;
import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.dokument.ArkivFilType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.dokument.Kommunikasjonsretning;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.saf.Arkivsaksystem;
import no.nav.saf.AvsenderMottaker;
import no.nav.saf.AvsenderMottakerIdType;
import no.nav.saf.Bruker;
import no.nav.saf.BrukerIdType;
import no.nav.saf.Datotype;
import no.nav.saf.DokumentInfo;
import no.nav.saf.Dokumentoversikt;
import no.nav.saf.DokumentoversiktFagsakQueryRequest;
import no.nav.saf.DokumentoversiktResponseProjection;
import no.nav.saf.Dokumentstatus;
import no.nav.saf.Dokumentvariant;
import no.nav.saf.Journalpost;
import no.nav.saf.Journalposttype;
import no.nav.saf.Journalstatus;
import no.nav.saf.Kanal;
import no.nav.saf.LogiskVedlegg;
import no.nav.saf.RelevantDato;
import no.nav.saf.Sak;
import no.nav.saf.SkjermingType;
import no.nav.saf.Tema;
import no.nav.saf.Variantformat;
import no.nav.vedtak.felles.integrasjon.saf.HentDokumentQuery;
import no.nav.vedtak.felles.integrasjon.saf.SafTjeneste;

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

    @BeforeEach
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
        Journalpost journalpost = byggJournalpost(ArkivFilType.PDF, Variantformat.ARKIV, TID_REGISTRERT, TID_JOURNALFØRT, Journalposttype.U);
        Dokumentoversikt dokumentoversikt = new Dokumentoversikt(List.of(journalpost), null);
        when(safTjeneste.dokumentoversiktFagsak(any(DokumentoversiktFagsakQueryRequest.class), any(DokumentoversiktResponseProjection.class)))
            .thenReturn(dokumentoversikt);

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
        Journalpost journalpost = byggJournalpost(ArkivFilType.PDF, Variantformat.ARKIV, TID_REGISTRERT, TID_JOURNALFØRT, Journalposttype.I);
        Dokumentoversikt dokumentoversikt = new Dokumentoversikt(List.of(journalpost), null);
        when(safTjeneste.dokumentoversiktFagsak(any(DokumentoversiktFagsakQueryRequest.class), any(DokumentoversiktResponseProjection.class)))
            .thenReturn(dokumentoversikt);

        List<ArkivJournalPost> arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterForVisning(KJENT_SAK);

        assertThat(arkivDokuments.get(0).getTidspunkt()).isEqualTo(TID_JOURNALFØRT);
        assertThat(arkivDokuments.get(0).getKommunikasjonsretning()).isEqualTo(Kommunikasjonsretning.INN);
    }

    @Test
    public void skalRetunereDokumentListeMedUansettInnhold() {
        List<Journalpost> journalposter = List.of(
            byggJournalpost(ArkivFilType.PDF, Variantformat.ARKIV, TID_REGISTRERT, TID_JOURNALFØRT, Journalposttype.I),
            byggJournalpost(ArkivFilType.XLS, Variantformat.ARKIV)
        );
        Dokumentoversikt dokumentoversikt = new Dokumentoversikt(journalposter, null);
        when(safTjeneste.dokumentoversiktFagsak(any(DokumentoversiktFagsakQueryRequest.class), any(DokumentoversiktResponseProjection.class)))
            .thenReturn(dokumentoversikt);

        Optional<ArkivJournalPost> arkivDokument = dokumentApplikasjonTjeneste.hentJournalpostForSak(KJENT_SAK, JOURNAL_ID);

        assertThat(arkivDokument).isPresent();
        assertThat(arkivDokument.get().getAndreDokument()).isEmpty();
    }

    @Test
    public void skalRetunereDokumenterAvFiltypePDF() {
        List<Journalpost> journalposter = List.of(
            byggJournalpost(ArkivFilType.PDF, Variantformat.ARKIV),
            byggJournalpost(ArkivFilType.XLS, Variantformat.ARKIV)
        );
        Dokumentoversikt dokumentoversikt = new Dokumentoversikt(journalposter, null);
        when(safTjeneste.dokumentoversiktFagsak(any(DokumentoversiktFagsakQueryRequest.class), any(DokumentoversiktResponseProjection.class)))
            .thenReturn(dokumentoversikt);

        List<ArkivJournalPost> arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterForVisning(KJENT_SAK);

        assertThat(arkivDokuments).hasSize(1);
    }

    @Test
    public void skalRetunereDokumenttypeInntektsmelding() {
        Journalpost journalpost = byggJournalpostMedFlereDokumenter(List.of(
            byggDokumentInfo(ArkivFilType.PDF, Variantformat.ARKIV, Brevkode.INNTEKTSMELDING),
            byggDokumentInfo(ArkivFilType.PDF, Variantformat.ARKIV, Brevkode.UDEFINERT)
        ));
        Dokumentoversikt dokumentoversikt = new Dokumentoversikt(List.of(journalpost), null);
        when(safTjeneste.dokumentoversiktFagsak(any(DokumentoversiktFagsakQueryRequest.class), any(DokumentoversiktResponseProjection.class)))
            .thenReturn(dokumentoversikt);

        List<ArkivJournalPost> arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterForVisning(KJENT_SAK);

        assertThat(arkivDokuments).hasSize(1);
    }

    @Test
    public void skalRetunereDokumenterAvVariantFormatARKIV() {
        List<Journalpost> journalposter = List.of(
            byggJournalpost(ArkivFilType.PDF, Variantformat.ARKIV),
            byggJournalpost(ArkivFilType.PDFA, Variantformat.ARKIV),
            byggJournalpost(ArkivFilType.XML, Variantformat.ORIGINAL)
        );
        Dokumentoversikt dokumentoversikt = new Dokumentoversikt(journalposter, null);
        when(safTjeneste.dokumentoversiktFagsak(any(DokumentoversiktFagsakQueryRequest.class), any(DokumentoversiktResponseProjection.class)))
            .thenReturn(dokumentoversikt);

        List<ArkivJournalPost> arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterForVisning(KJENT_SAK);

        assertThat(arkivDokuments).hasSize(2);
    }

    @Test
    public void skalRetunereDokumentListeMedSisteTidspunktØverst() {
        List<Journalpost> journalposter = List.of(
            byggJournalpost(ArkivFilType.PDFA, Variantformat.ARKIV, TID_REGISTRERT, TID_JOURNALFØRT, Journalposttype.U),
            byggJournalpost(ArkivFilType.PDFA, Variantformat.ARKIV, TID_REGISTRERT, TID_JOURNALFØRT.minusDays(1), Journalposttype.I)
        );
        Dokumentoversikt dokumentoversikt = new Dokumentoversikt(journalposter, null);
        when(safTjeneste.dokumentoversiktFagsak(any(DokumentoversiktFagsakQueryRequest.class), any(DokumentoversiktResponseProjection.class)))
            .thenReturn(dokumentoversikt);

        List<ArkivJournalPost> arkivDokuments = dokumentApplikasjonTjeneste.hentAlleDokumenterForVisning(KJENT_SAK);

        assertThat(arkivDokuments.get(0).getTidspunkt()).isEqualTo(TID_JOURNALFØRT);
        assertThat(arkivDokuments.get(0).getKommunikasjonsretning()).isEqualTo(Kommunikasjonsretning.UT);
        assertThat(arkivDokuments.get(1).getTidspunkt()).isEqualTo(TID_JOURNALFØRT.minusDays(1));
        assertThat(arkivDokuments.get(1).getKommunikasjonsretning()).isEqualTo(Kommunikasjonsretning.INN);
    }

    @Test
    public void skal_kalle_web_service_og_oversette_fra_() {
        // Arrange
        final byte[] bytesExpected = {1, 2, 7};
        when(safTjeneste.hentDokument(any(HentDokumentQuery.class))).thenReturn(bytesExpected);

        // Act
        byte[] bytesActual = dokumentApplikasjonTjeneste.hentDokumnet(new JournalpostId("123"), "456");

        // Assert
        assertThat(bytesActual).isEqualTo(bytesExpected);
    }

    // Master-bygger
    private Journalpost byggJournalpost(LocalDateTime registrertTid, LocalDateTime journalførtTid, Journalposttype journalposttype,
                                        List<DokumentInfo> dokumentInfoer) {
        var journalpost = new Journalpost();
        journalpost.setJournalpostId(JOURNAL_ID.getVerdi());
        journalpost.setTittel("tittel");
        journalpost.setJournalposttype(journalposttype);
        journalpost.setJournalstatus(Journalstatus.FERDIGSTILT);
        journalpost.setKanal(Kanal.ALTINN);
        journalpost.setTema(Tema.AAP);
        journalpost.setBehandlingstema("behandlingstema");
        journalpost.setSak(new Sak("arkivsaksystem", Arkivsaksystem.GSAK, new Date(), "fagsakId", "fagsaksystem"));
        journalpost.setBruker(new Bruker("id", BrukerIdType.AKTOERID));
        journalpost.setAvsenderMottaker(new AvsenderMottaker("fnr", AvsenderMottakerIdType.FNR, "Navn", "Land", true));
        journalpost.setJournalfoerendeEnhet("journalstatus");
        journalpost.setDokumenter(dokumentInfoer);
        journalpost.setRelevanteDatoer(List.of(
            new RelevantDato(toDate(journalførtTid), Datotype.DATO_JOURNALFOERT),
            new RelevantDato(toDate(registrertTid), Datotype.DATO_REGISTRERT)));
        journalpost.setEksternReferanseId("eksternReferanseId");

        return journalpost;
    }

    private DokumentInfo byggDokumentInfo(ArkivFilType arkivFilType, Variantformat variantFormat, Brevkode brevkode) {
        return new DokumentInfo(DOKUMENT_ID, "tittel", brevkode.getOffisiellKode(), Dokumentstatus.FERDIGSTILT, new Date(), "origJpId", SkjermingType.POL.name(),
            List.of(new LogiskVedlegg("id", "tittel")),
            List.of(new Dokumentvariant(variantFormat, "filnavn", "fluuid", arkivFilType.name(), true, SkjermingType.POL)));
    }

    // Hjelpebyggere
    private Journalpost byggJournalpost(ArkivFilType arkivFilType, Variantformat variantFormat) {
        return byggJournalpost(TID_JOURNALFØRT, TID_JOURNALFØRT, Journalposttype.U,
            List.of(byggDokumentInfo(arkivFilType, variantFormat, Brevkode.UDEFINERT)));
    }

    private Journalpost byggJournalpostMedFlereDokumenter(List<DokumentInfo> dokumentInfoer) {
        return byggJournalpost(TID_JOURNALFØRT, TID_JOURNALFØRT, Journalposttype.U, dokumentInfoer);
    }

    private Journalpost byggJournalpost(ArkivFilType arkivFilType,
                                        Variantformat variantFormat,
                                        LocalDateTime registrertTid,
                                        LocalDateTime journalførtTid,
                                        Journalposttype journalposttype) {
        return byggJournalpost(registrertTid, journalførtTid, journalposttype,
            List.of(byggDokumentInfo(arkivFilType, variantFormat, Brevkode.UDEFINERT)));
    }

    private Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
