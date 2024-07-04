package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.medisinsk;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.Person;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomDokumentInformasjon;
import no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår.SykdomGrunnlagSammenlikningsresultat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MedisinskGrunnlagTjenesteTest {

    private MedisinskGrunnlagTjeneste medisinskGrunnlagTjeneste =
        new MedisinskGrunnlagTjeneste(null, new BehandlingRepositoryProvider(mock()));

    @Test
    void testHarNyeUklassifiserteDokumenter_NyeDokumenter() {
        // Arrange
        MedisinskGrunnlagsdata forrigeGrunnlag = createMedisinskGrunnlagsdata(List.of(
                createUklassifiserteDokumenter("1", "doc1"),
                createUklassifiserteDokumenter("1", "doc2")
            )
        );
        MedisinskGrunnlagsdata nyGrunnlag = createMedisinskGrunnlagsdata(List.of(
            createUklassifiserteDokumenter("1", "doc1"),
            createUklassifiserteDokumenter("1", "doc2"),
            createUklassifiserteDokumenter("2", "doc3")
        ));

        // Act
        SykdomGrunnlagSammenlikningsresultat resultat = medisinskGrunnlagTjeneste
            .sammenlignGrunnlag(Optional.of(forrigeGrunnlag), nyGrunnlag, true);

        // Assert
        assertThat(resultat.harNyeUklassifiserteDokumenter()).isTrue();
    }

    @Test
    void testHarNyeUklassifiserteDokumenter_IngenNyeDokumenter() {
        // Arrange
        MedisinskGrunnlagsdata forrigeGrunnlag = createMedisinskGrunnlagsdata(List.of(
                createUklassifiserteDokumenter("1", "doc1"),
                createUklassifiserteDokumenter("1", "doc2")
            )
        );
        MedisinskGrunnlagsdata nyGrunnlag = createMedisinskGrunnlagsdata(List.of(
            createUklassifiserteDokumenter("1", "doc1"),
            createUklassifiserteDokumenter("1", "doc2")
        ));

        // Act
        SykdomGrunnlagSammenlikningsresultat resultat = medisinskGrunnlagTjeneste.sammenlignGrunnlag(Optional.of(forrigeGrunnlag), nyGrunnlag, true);

        // Assert
        assertThat(resultat.harNyeUklassifiserteDokumenter()).isFalse();
    }

    @Test
    void testHarNyeUklassifiserteDokumenter_IngenTidligereDokumenter() {
        MedisinskGrunnlagsdata forrigeGrunnlag = createMedisinskGrunnlagsdata(Collections.emptyList());
        MedisinskGrunnlagsdata nyGrunnlag = createMedisinskGrunnlagsdata(List.of(
            createUklassifiserteDokumenter("1", "doc1"),
            createUklassifiserteDokumenter("1", "doc2")
        ));

        SykdomGrunnlagSammenlikningsresultat resultat = medisinskGrunnlagTjeneste.sammenlignGrunnlag(Optional.of(forrigeGrunnlag), nyGrunnlag, true);

        assertThat(resultat.harNyeUklassifiserteDokumenter()).isTrue();
    }

    @Test
    void testHarNyeUklassifiserteDokumenter_IngenNåEllerFør() {
        MedisinskGrunnlagsdata forrigeGrunnlag = createMedisinskGrunnlagsdata(Collections.emptyList());
        MedisinskGrunnlagsdata nyGrunnlag = createMedisinskGrunnlagsdata(Collections.emptyList());

        SykdomGrunnlagSammenlikningsresultat resultat = medisinskGrunnlagTjeneste.sammenlignGrunnlag(Optional.of(forrigeGrunnlag), nyGrunnlag, true);

        assertThat(resultat.harNyeUklassifiserteDokumenter()).isFalse();
    }

    @Test
    void testHarNyeUklassifiserteDokumenter_ManglerGammelDokument() {
        MedisinskGrunnlagsdata forrigeGrunnlag =  createMedisinskGrunnlagsdata(List.of(
                createUklassifiserteDokumenter("1", "doc1"),
                createUklassifiserteDokumenter("1", "doc2")
            )
        );
        MedisinskGrunnlagsdata nyGrunnlag =  createMedisinskGrunnlagsdata(List.of(
                createUklassifiserteDokumenter("1", "doc1")
            )
        );

        SykdomGrunnlagSammenlikningsresultat resultat = medisinskGrunnlagTjeneste.sammenlignGrunnlag(Optional.of(forrigeGrunnlag), nyGrunnlag, true);

        assertThat(resultat.harNyeUklassifiserteDokumenter()).isFalse();
    }

    private MedisinskGrunnlagsdata createMedisinskGrunnlagsdata(List<PleietrengendeSykdomDokument> uklassifiserteDokumenter) {
        return new MedisinskGrunnlagsdata(
            UUID.randomUUID(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            uklassifiserteDokumenter,
            false,
            null,
            null,
            "test",
            LocalDateTime.now()
        );
    }


    private PleietrengendeSykdomDokument createUklassifiserteDokumenter(String journalpostId, String dokumentId) {
        JournalpostId journalpostIdObj = new JournalpostId(journalpostId);
        PleietrengendeSykdomDokumentInformasjon informasjon = new PleietrengendeSykdomDokumentInformasjon(
            SykdomDokumentType.UKLASSIFISERT,
            false,
            LocalDate.now(),
            LocalDateTime.now(),
            0L,
            "",
            LocalDateTime.now()
        );

        UUID søkersBehandlingUuid = UUID.randomUUID();
        Saksnummer søkersSaksnummer = new Saksnummer("123456");
        Person søker = new Person(
            AktørId.dummy(),
            "01010101010"
        );

        return new PleietrengendeSykdomDokument(
            journalpostIdObj,
            dokumentId,
            informasjon,
            søkersBehandlingUuid,
            søkersSaksnummer,
            søker,
            "",
            LocalDateTime.now()
        );
    }
}
