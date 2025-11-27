package no.nav.ung.sak.domene.registerinnhenting;

import no.nav.ung.kodeverk.uttak.Tid;
import no.nav.ung.sak.behandling.FagsakTjeneste;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.InntektAbonnement;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.InntektAbonnementRepository;
import no.nav.ung.sak.domene.person.tps.TpsTjeneste;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.PersonIdent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InntektAbonnentTjenesteTest {

    @Mock
    private InntektAbonnementRepository inntektAbonnementRepository;

    @Mock
    private InntektAbonnentKlient inntektAbonnentKlient;

    @Mock
    private FagsakTjeneste fagsakTjeneste;

    @Mock
    private TpsTjeneste tpsTjeneste;

    private InntektAbonnentTjeneste inntektAbonnenentTjeneste;

    private AktørId aktørId;
    private PersonIdent personIdent;
    private Periode periode;
    private LocalDate tomFagsakPeriode;

    @BeforeEach
    void setUp() {
        inntektAbonnenentTjeneste = new InntektAbonnentTjeneste(
            inntektAbonnementRepository,
            inntektAbonnentKlient,
            fagsakTjeneste,
            tpsTjeneste
        );

        aktørId = new AktørId("1234567890123");
        personIdent = new PersonIdent("12345678901");
        periode = new Periode(
            LocalDate.now().minusMonths(1),
            LocalDate.now().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth()));
        tomFagsakPeriode = LocalDate.now().plusYears(1);
    }

    @Test
    void opprettelse_av_abonnement_skal_returnene_tidlig_dersom_det_finnes_et_abonnement_for_aktøren_i_samme_periode() {
        // Arrange
        var eksisterendeAbonnement = new InntektAbonnement("12345", aktørId);
        eksisterendeAbonnement.setPeriode(periode.getFom(),periode.getTom());
        when(inntektAbonnementRepository.hentAbonnementForAktør(aktørId))
            .thenReturn(Optional.of(eksisterendeAbonnement));

        // Act
        inntektAbonnenentTjeneste.opprettAbonnement(aktørId, periode);

        // Assert
        verify(inntektAbonnentKlient, never()).opprettAbonnement(any(), any(), any(), any(), any(), any(), anyInt());
        verify(inntektAbonnementRepository, never()).lagre(any());
    }

    @Test
    void opprettelse_av_abonnement_skal_kaste_feil_dersom_det_finnes_et_abonnement_for_aktøren_i_en_annen_periode() {
        // Arrange
        var annenPeriode = new Periode(LocalDate.now().minusYears(1), LocalDate.now().minusMonths(6));
        var eksisterendeAbonnement = new InntektAbonnement("12345", aktørId);
        eksisterendeAbonnement.setPeriode(annenPeriode.getFom(),annenPeriode.getTom());

        when(inntektAbonnementRepository.hentAbonnementForAktør(aktørId))
            .thenReturn(Optional.of(eksisterendeAbonnement));

        // Act & Assert
        assertThatThrownBy(() -> inntektAbonnenentTjeneste.opprettAbonnement(aktørId, periode))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("eksisterer en abbonnentId");
    }

    @Test
    void opprettelse_av_abonnement_skal_kaste_exception_når_ingen_åpen_fagsak_finnes() {
        // Arrange
        when(inntektAbonnementRepository.hentAbonnementForAktør(aktørId))
            .thenReturn(Optional.empty());
        when(fagsakTjeneste.finnFagsakerForAktør(aktørId))
            .thenReturn(List.of());

        // Act & Assert
        assertThatThrownBy(() -> inntektAbonnenentTjeneste.opprettAbonnement(aktørId, periode))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Fant ingen åpen fagsak");
    }

    @Test
    void skal_returnere_empty_når_ingen_hendelser() {
        // Arrange
        when(inntektAbonnentKlient.hentStartSekvensnummer(any(LocalDate.class)))
            .thenReturn(Optional.empty());

        // Act
        Optional<Long> resultat = inntektAbonnenentTjeneste.hentFørsteSekvensnummer();

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    void skal_returnere_første_sekvensnummer() {
        // Arrange
        long forventetSekvensnummer = 12345L;
        when(inntektAbonnentKlient.hentStartSekvensnummer(any(LocalDate.class)))
            .thenReturn(Optional.of(forventetSekvensnummer));

        // Act
        Optional<Long> resultat = inntektAbonnenentTjeneste.hentFørsteSekvensnummer();

        // Assert
        assertThat(resultat).isPresent();
        assertThat(resultat.get()).isEqualTo(forventetSekvensnummer);
    }

    @Test
    void skal_returnere_tom_liste_når_ingen_hendelser() {
        // Arrange
        long startSekvensnummer = 1000L;
        when(inntektAbonnentKlient.hentAbonnentHendelser(eq(startSekvensnummer), anyList()))
            .thenReturn(List.of());

        // Act
        List<InntektAbonnentTjeneste.InntektHendelse> resultat =
            inntektAbonnenentTjeneste.hentNyeInntektHendelser(startSekvensnummer);

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    void henting_av_nye_hendelser_skal_mappe_korrekt() {
        // Arrange
        long startSekvensnummer = 1000L;
        YearMonth yearMonth = YearMonth.of(2025, 11);

        var klientHendelse1 = new InntektAbonnentKlient.AbonnementHendelse(
            1001L,
            aktørId.getId(),
            yearMonth,
            LocalDate.now().atStartOfDay(),
            List.of("Ung")
        );

        var klientHendelse2 = new InntektAbonnentKlient.AbonnementHendelse(
            1002L,
            "9876543210987",
            yearMonth.plusMonths(1),
            LocalDate.now().atStartOfDay(),
            List.of("Ung")
        );

        when(inntektAbonnentKlient.hentAbonnentHendelser(startSekvensnummer, List.of("Ung")))
            .thenReturn(List.of(klientHendelse1, klientHendelse2));

        // Act
        List<InntektAbonnentTjeneste.InntektHendelse> resultat =
            inntektAbonnenentTjeneste.hentNyeInntektHendelser(startSekvensnummer);

        // Assert
        assertThat(resultat).hasSize(2);

        InntektAbonnentTjeneste.InntektHendelse hendelse1 = resultat.get(0);
        assertThat(hendelse1.sekvensnummer()).isEqualTo(1001L);
        assertThat(hendelse1.aktørId().getId()).isEqualTo(aktørId.getId());
        assertThat(hendelse1.periode().getFom()).isEqualTo(LocalDate.of(2025, 11, 1));
        assertThat(hendelse1.periode().getTom()).isEqualTo(LocalDate.of(2025, 11, 30));

        InntektAbonnentTjeneste.InntektHendelse hendelse2 = resultat.get(1);
        assertThat(hendelse2.sekvensnummer()).isEqualTo(1002L);
        assertThat(hendelse2.aktørId().getId()).isEqualTo("9876543210987");
        assertThat(hendelse2.periode().getFom()).isEqualTo(LocalDate.of(2025, 12, 1));
        assertThat(hendelse2.periode().getTom()).isEqualTo(LocalDate.of(2025, 12, 31));
    }

    @Test
    void avslutning_av_abonnement_skal_ikke_gjøre_noe_om_abonnement_ikke_finnes() {
        // Arrange
        when(inntektAbonnementRepository.hentAbonnementForAktør(aktørId))
            .thenReturn(Optional.empty());

        // Act
        inntektAbonnenentTjeneste.avsluttAbonnentHvisFinnes(aktørId);

        // Assert
        verify(inntektAbonnentKlient, never()).avsluttAbonnement(anyLong());
        verify(inntektAbonnementRepository, never()).slettAbonnement(any());
    }

    @Test
    void avslutning_av_abonnement_skal_slette_om_abonnement_finnes() {
        // Arrange
        String abonnementId = "99999";
        var eksisterendeAbonnement = new InntektAbonnement(abonnementId, aktørId);


        when(inntektAbonnementRepository.hentAbonnementForAktør(aktørId))
            .thenReturn(Optional.of(eksisterendeAbonnement));

        // Act
        inntektAbonnenentTjeneste.avsluttAbonnentHvisFinnes(aktørId);

        // Assert
        verify(inntektAbonnentKlient).avsluttAbonnement(Long.parseLong(abonnementId));
        verify(inntektAbonnementRepository).slettAbonnement(eksisterendeAbonnement);
    }

    @Test
    void avslutt_abonnement_skal_parse_abonnementId_riktig() {
        // Arrange
        String abonnementId = "123456789";
        var eksisterendeAbonnement = new InntektAbonnement(abonnementId, aktørId);

        when(inntektAbonnementRepository.hentAbonnementForAktør(aktørId))
            .thenReturn(Optional.of(eksisterendeAbonnement));

        ArgumentCaptor<Long> abonnementIdCaptor = ArgumentCaptor.forClass(Long.class);

        // Act
        inntektAbonnenentTjeneste.avsluttAbonnentHvisFinnes(aktørId);

        // Assert
        verify(inntektAbonnentKlient).avsluttAbonnement(abonnementIdCaptor.capture());
        assertThat(abonnementIdCaptor.getValue()).isEqualTo(123456789L);
    }
}

