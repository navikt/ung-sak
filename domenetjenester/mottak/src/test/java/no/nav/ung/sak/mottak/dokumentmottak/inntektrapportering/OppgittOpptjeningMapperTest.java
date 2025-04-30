package no.nav.ung.sak.mottak.dokumentmottak.inntektrapportering;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType;
import no.nav.abakus.iaygrunnlag.kodeverk.YtelseType;
import no.nav.k9.søknad.felles.type.Periode;
import no.nav.k9.søknad.ytelse.ung.v1.inntekt.OppgittInntekt;
import no.nav.k9.søknad.ytelse.ung.v1.inntekt.OppgittInntektForPeriode;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentStatus;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;


class OppgittOpptjeningMapperTest {

    public static final long FAGSAK_ID = 2L;
    public static final Saksnummer SAKSNUMMER = new Saksnummer("SAKEN");
    public static final long BEHANDLING_ID = 1L;
    public static final UUID BEHANDLING_UUID = UUID.randomUUID();
    public static final AktørId AKTØR_ID = AktørId.dummy();
    public static final BehandlingReferanse BEHANDLINGREFERANSE = BehandlingReferanse.fra(
        FagsakYtelseType.UNGDOMSYTELSE,
        BehandlingType.REVURDERING,
        BehandlingResultatType.IKKE_FASTSATT,
        AKTØR_ID,
        SAKSNUMMER,
        FAGSAK_ID,
        BEHANDLING_ID,
        BEHANDLING_UUID,
        Optional.of(5L),
        BehandlingStatus.UTREDES,
        null,
        DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now())
    );

    @Test
    void skal_mappe_oppgitt_frilans_og_arbeidstaker_inntekt() {
        // Arrange
        final var innsendingstidspunkt = LocalDateTime.now();
        final var journalpostId = 6L;
        final var mottattDokument = lagMottattDokument(innsendingstidspunkt, journalpostId);
        final var inntekt = BigDecimal.TEN;
        final var fom = LocalDate.now();
        final var tom = LocalDate.now().plusDays(1);
        final var oppgittInntekt = lagOppgittInntektForArbeidOgFrilans(inntekt, fom, tom);
        // Act
        final var result = OppgittOpptjeningMapper.mapRequest(BEHANDLINGREFERANSE, mottattDokument, oppgittInntekt);
        // Assert
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get().getSaksnummer()).isEqualTo(SAKSNUMMER.getVerdi());
        assertThat(result.get().getKoblingReferanse()).isEqualTo(BEHANDLING_UUID);
        assertThat(result.get().getYtelseType()).isEqualTo(YtelseType.UNGDOMSYTELSE);
        assertThat(result.get().harOppgittJournalpostId()).isEqualTo(true);

        final var oppgittOpptjening = result.get().getOppgittOpptjening();
        assertThat(oppgittOpptjening.getArbeidsforhold().size()).isEqualTo(1);
        final var oppgittArbeidsforhold = oppgittOpptjening.getArbeidsforhold().get(0);
        assertThat(oppgittArbeidsforhold.getInntekt().compareTo(inntekt)).isEqualTo(0);
        assertThat(oppgittArbeidsforhold.getArbeidTypeDto()).isEqualTo(ArbeidType.VANLIG);
        assertThat(oppgittArbeidsforhold.getPeriode().getFom()).isEqualTo(fom);
        assertThat(oppgittArbeidsforhold.getPeriode().getTom()).isEqualTo(tom);
    }


    private static OppgittInntekt lagOppgittInntektForArbeidOgFrilans(BigDecimal oppgittBeløp, LocalDate fom, LocalDate tom) {
        return OppgittInntekt.builder()
            .medOppgittePeriodeinntekter(Set.of(OppgittInntektForPeriode.builder(new Periode(fom, tom))
                .medArbeidstakerOgFrilansinntekt(oppgittBeløp)
                .build())).build();
    }

    private static MottattDokument lagMottattDokument(LocalDateTime innsendingstidspunkt, long journalpostId) {
        final var mottattDokument = new MottattDokument.Builder()
            .medFagsakId(FAGSAK_ID)
            .medJournalPostId(new JournalpostId(journalpostId))
            .medStatus(DokumentStatus.BEHANDLER)
            .medInnsendingstidspunkt(innsendingstidspunkt)
            .build();
        return mottattDokument;
    }
}
