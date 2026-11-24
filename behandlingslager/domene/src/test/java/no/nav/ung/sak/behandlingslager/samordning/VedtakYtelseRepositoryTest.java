package no.nav.ung.sak.behandlingslager.samordning;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.db.util.CdiDbAwareTest;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@CdiDbAwareTest
class VedtakYtelseRepositoryTest {

    private static final AtomicInteger SAKSNUMMER_TELLER = new AtomicInteger();

    private static final LocalDate FOM = LocalDate.of(2025, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2025, 12, 31);
    private static final LocalDateTime VEDTATT = LocalDateTime.of(2025, 1, 10, 12, 0);

    @Inject
    private EntityManager entityManager;

    @Inject
    private VedtakYtelseRepository repository;

    private AktørId aktørId;
    private Saksnummer saksnummer;

    @BeforeEach
    void setUp() {
        aktørId = AktørId.dummy();
        saksnummer = new Saksnummer("SAMORD" + SAKSNUMMER_TELLER.incrementAndGet());
    }

    @Test
    void skal_lagre_og_hente_vedtak_med_anvisninger() {
        var januar = DatoIntervallEntitet.fraOgMedTilOgMed(FOM, LocalDate.of(2025, 1, 31));
        var februar = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 2, 28));
        var resten = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2025, 3, 1), TOM);
        var vedtak = vedtakBuilder(UUID.randomUUID(), VEDTATT, FagsakYtelseType.AKTIVITETSPENGER, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))
            .leggTilAnvisning(YtelseAnvist.builder(januar)
                .medDagsats(BigDecimal.valueOf(800))
                .medBeløp(BigDecimal.valueOf(18400))
                .medAntallBarn(2)
                .medDagsatsBarnetillegg(74)
                .build())
            .leggTilAnvisning(YtelseAnvist.builder(februar)
                .medDagsats(BigDecimal.ZERO)
                .medBeløp(BigDecimal.ZERO)
                .medAntallBarn(2)
                .medDagsatsBarnetillegg(74)
                .build())
            .leggTilAnvisning(YtelseAnvist.builder(resten)
                .medAntallBarn(2)
                .medDagsatsBarnetillegg(74)
                .build())
            .build();

        repository.lagre(vedtak);
        entityManager.clear();

        var lagret = repository.hentAktivtVedtak(saksnummer, FagsakYtelseType.AKTIVITETSPENGER).orElseThrow();
        assertThat(lagret.getAktørId()).isEqualTo(aktørId);
        assertThat(lagret.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM));
        assertThat(lagret.getVedtattTidspunkt()).isEqualTo(VEDTATT);
        assertThat(lagret.getAnvisninger()).hasSize(3);

        var jan = lagret.getAnvisninger().get(0);
        assertThat(jan.getPeriode()).isEqualTo(januar);
        assertThat(jan.getDagsats()).hasValueSatisfying(d -> assertThat(d).isEqualByComparingTo("800"));
        assertThat(jan.getAntallBarn()).contains(2);
        assertThat(jan.getDagsatsBarnetillegg()).contains(74);

        var feb = lagret.getAnvisninger().get(1);
        assertThat(feb.erBeregnet()).isTrue();
        assertThat(feb.getDagsats()).hasValueSatisfying(d -> assertThat(d).isEqualByComparingTo("0"));

        var ikkeBeregnet = lagret.getAnvisninger().get(2);
        assertThat(ikkeBeregnet.erBeregnet()).isFalse();
        assertThat(ikkeBeregnet.getDagsats()).isEmpty();
        assertThat(ikkeBeregnet.getBeløp()).isEmpty();
        assertThat(ikkeBeregnet.getAntallBarn()).contains(2);
    }

    @Test
    void nytt_vedtak_skal_deaktivere_forrige() {
        var første = innvilget(UUID.randomUUID(), VEDTATT, FOM, TOM);
        var andre = innvilget(UUID.randomUUID(), VEDTATT.plusMonths(1), FOM, TOM.minusMonths(6));

        repository.lagre(første);
        repository.lagre(andre);
        entityManager.clear();

        assertThat(aktivReferanse()).isEqualTo(andre.getVedtakReferanse());
        assertThat(repository.hentAlleVedtak(saksnummer, FagsakYtelseType.UNGDOMSYTELSE)).hasSize(2);
    }

    @Test
    void eldre_vedtak_skal_lagres_inaktivt_uavhengig_av_rekkefølge() {
        var eldre = innvilget(UUID.randomUUID(), VEDTATT, FOM, TOM);
        var nyere = innvilget(UUID.randomUUID(), VEDTATT.plusMonths(1), FOM, TOM.minusMonths(6));

        repository.lagre(nyere);
        repository.lagre(eldre);
        entityManager.clear();

        assertThat(aktivReferanse()).isEqualTo(nyere.getVedtakReferanse());
        var alle = repository.hentAlleVedtak(saksnummer, FagsakYtelseType.UNGDOMSYTELSE);
        assertThat(alle).hasSize(2);
        assertThat(alle).filteredOn(v -> v.getVedtakReferanse().equals(eldre.getVedtakReferanse()))
            .singleElement()
            .satisfies(v -> assertThat(v.erAktiv()).isFalse());
    }

    @Test
    void samme_referanse_skal_erstatte_raden() {
        var referanse = UUID.randomUUID();
        repository.lagre(innvilget(referanse, VEDTATT, FOM, TOM));
        entityManager.clear();

        var erstatning = innvilget(referanse, VEDTATT, FOM, LocalDate.of(2025, 6, 30));
        repository.lagre(erstatning);
        entityManager.clear();

        var alle = repository.hentAlleVedtak(saksnummer, FagsakYtelseType.UNGDOMSYTELSE);
        assertThat(alle).singleElement().satisfies(v -> {
            assertThat(v.erAktiv()).isTrue();
            assertThat(v.getPeriode().getTomDato()).isEqualTo(LocalDate.of(2025, 6, 30));
            assertThat(v.getAnvisninger()).singleElement()
                .satisfies(a -> assertThat(a.getPeriode().getTomDato()).isEqualTo(LocalDate.of(2025, 6, 30)));
        });
    }

    @Test
    void lagring_på_nytt_av_eldre_vedtak_skal_ikke_endre_aktivt_vedtak() {
        var eldre = UUID.randomUUID();
        var nyere = innvilget(UUID.randomUUID(), VEDTATT.plusMonths(1), FOM, TOM);
        repository.lagre(innvilget(eldre, VEDTATT, FOM, TOM));
        repository.lagre(nyere);
        entityManager.clear();

        repository.lagre(innvilget(eldre, VEDTATT, FOM, TOM));
        entityManager.clear();

        assertThat(aktivReferanse()).isEqualTo(nyere.getVedtakReferanse());
        assertThat(repository.hentAlleVedtak(saksnummer, FagsakYtelseType.UNGDOMSYTELSE)).hasSize(2);
    }

    @Test
    void vedtak_uten_innvilget_periode_skal_deaktivere_forrige_innvilgelse() {
        var innvilgelse = innvilget(UUID.randomUUID(), VEDTATT, FOM, TOM);
        var vedtaksdato = VEDTATT.plusMonths(2).toLocalDate();
        var avslag = vedtakBuilder(UUID.randomUUID(), VEDTATT.plusMonths(2), FagsakYtelseType.UNGDOMSYTELSE,
            DatoIntervallEntitet.fraOgMedTilOgMed(vedtaksdato, vedtaksdato))
            .build();

        repository.lagre(innvilgelse);
        repository.lagre(avslag);
        entityManager.clear();

        var aktivt = repository.hentAktivtVedtak(saksnummer, FagsakYtelseType.UNGDOMSYTELSE).orElseThrow();
        assertThat(aktivt.getVedtakReferanse()).isEqualTo(avslag.getVedtakReferanse());
        assertThat(aktivt.harInnvilgetPeriode()).isFalse();
        assertThat(aktivt.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(vedtaksdato, vedtaksdato));
    }

    @Test
    void skal_kun_finne_aktive_vedtak_som_overlapper() {
        var gammel = innvilget(UUID.randomUUID(), VEDTATT, FOM, TOM);
        var gjeldende = innvilget(UUID.randomUUID(), VEDTATT.plusMonths(1), FOM, LocalDate.of(2025, 6, 30));
        repository.lagre(gammel);
        repository.lagre(gjeldende);
        entityManager.clear();

        var ytelser = Set.of(FagsakYtelseType.UNGDOMSYTELSE, FagsakYtelseType.AKTIVITETSPENGER);

        assertThat(repository.hentAktiveVedtakSomOverlapper(List.of(aktørId), ytelser, LocalDate.of(2025, 6, 30), LocalDate.of(2025, 7, 31)))
            .extracting(VedtakYtelse::getVedtakReferanse)
            .containsExactly(gjeldende.getVedtakReferanse());
        assertThat(repository.hentAktiveVedtakSomOverlapper(List.of(aktørId), ytelser, LocalDate.of(2025, 7, 1), LocalDate.of(2025, 7, 31)))
            .isEmpty();
        assertThat(repository.hentAktiveVedtakSomOverlapper(List.of(aktørId), Set.of(FagsakYtelseType.AKTIVITETSPENGER), FOM, TOM))
            .isEmpty();
        assertThat(repository.hentAktiveVedtakSomOverlapper(List.of(AktørId.dummy()), ytelser, FOM, TOM))
            .isEmpty();
        assertThat(repository.hentAktiveVedtakSomOverlapper(List.of(), ytelser, FOM, TOM))
            .isEmpty();
    }

    @Test
    void skal_flytte_alle_vedtak_til_ny_aktørId_ved_aktørbytte() {
        var nyAktørId = AktørId.dummy();
        var annenAktør = AktørId.dummy();
        var gammelt = innvilget(UUID.randomUUID(), VEDTATT, FOM, TOM);
        var gjeldende = innvilget(UUID.randomUUID(), VEDTATT.plusMonths(1), FOM, TOM);
        var annenAktørsVedtak = VedtakYtelse.builder()
            .medAktørId(annenAktør)
            .medSaksnummer(new Saksnummer("SAMORD" + SAKSNUMMER_TELLER.incrementAndGet()))
            .medYtelseType(FagsakYtelseType.UNGDOMSYTELSE)
            .medVedtakReferanse(UUID.randomUUID())
            .medVedtattTidspunkt(VEDTATT)
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))
            .build();
        repository.lagre(gammelt);
        repository.lagre(gjeldende);
        repository.lagre(annenAktørsVedtak);

        int antall = repository.byttAktørId(aktørId, nyAktørId);
        entityManager.clear();

        var ytelser = Set.of(FagsakYtelseType.UNGDOMSYTELSE);
        assertThat(antall).isEqualTo(2);
        assertThat(repository.hentAlleVedtak(saksnummer, FagsakYtelseType.UNGDOMSYTELSE))
            .extracting(VedtakYtelse::getAktørId)
            .containsOnly(nyAktørId);
        assertThat(repository.hentAktiveVedtakSomOverlapper(List.of(nyAktørId), ytelser, FOM, TOM))
            .extracting(VedtakYtelse::getVedtakReferanse)
            .containsExactly(gjeldende.getVedtakReferanse());
        assertThat(repository.hentAktiveVedtakSomOverlapper(List.of(aktørId), ytelser, FOM, TOM)).isEmpty();
        assertThat(repository.hentAktiveVedtakSomOverlapper(List.of(annenAktør), ytelser, FOM, TOM))
            .extracting(VedtakYtelse::getVedtakReferanse)
            .containsExactly(annenAktørsVedtak.getVedtakReferanse());
    }

    @Test
    void skal_hente_referanser_til_alle_lagrede_vedtak_for_ytelsene() {
        var inaktivt = innvilget(UUID.randomUUID(), VEDTATT, FOM, TOM);
        var aktivt = innvilget(UUID.randomUUID(), VEDTATT.plusMonths(1), FOM, TOM);
        var annenYtelse = vedtakBuilder(UUID.randomUUID(), VEDTATT, FagsakYtelseType.OMSORGSPENGER, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM)).build();
        repository.lagre(inaktivt);
        repository.lagre(aktivt);
        repository.lagre(annenYtelse);
        entityManager.clear();

        var referanser = repository.hentVedtakReferanser(Set.of(FagsakYtelseType.UNGDOMSYTELSE, FagsakYtelseType.AKTIVITETSPENGER));

        assertThat(referanser)
            .contains(inaktivt.getVedtakReferanse(), aktivt.getVedtakReferanse())
            .doesNotContain(annenYtelse.getVedtakReferanse());
    }

    @Test
    void skal_avvise_anvisning_utenfor_vedtaksperioden() {
        var builder = vedtakBuilder(UUID.randomUUID(), VEDTATT, FagsakYtelseType.UNGDOMSYTELSE, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))
            .leggTilAnvisning(YtelseAnvist.builder(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM.plusDays(1))).build());

        assertThatThrownBy(builder::build).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void skal_avvise_overlappende_anvisninger() {
        var builder = vedtakBuilder(UUID.randomUUID(), VEDTATT, FagsakYtelseType.UNGDOMSYTELSE, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))
            .leggTilAnvisning(YtelseAnvist.builder(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, LocalDate.of(2025, 6, 30))).build())
            .leggTilAnvisning(YtelseAnvist.builder(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2025, 6, 30), TOM)).build());

        assertThatThrownBy(builder::build).isInstanceOf(IllegalStateException.class);
    }

    private UUID aktivReferanse() {
        return repository.hentAktivtVedtak(saksnummer, FagsakYtelseType.UNGDOMSYTELSE).orElseThrow().getVedtakReferanse();
    }

    private VedtakYtelse innvilget(UUID referanse, LocalDateTime vedtatt, LocalDate fom, LocalDate tom) {
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom);
        return vedtakBuilder(referanse, vedtatt, FagsakYtelseType.UNGDOMSYTELSE, periode)
            .leggTilAnvisning(YtelseAnvist.builder(periode).medAntallBarn(0).medDagsatsBarnetillegg(0).build())
            .build();
    }

    private VedtakYtelse.Builder vedtakBuilder(UUID referanse, LocalDateTime vedtatt, FagsakYtelseType ytelseType, DatoIntervallEntitet periode) {
        return VedtakYtelse.builder()
            .medAktørId(aktørId)
            .medSaksnummer(saksnummer)
            .medYtelseType(ytelseType)
            .medVedtakReferanse(referanse)
            .medVedtattTidspunkt(vedtatt)
            .medPeriode(periode);
    }
}
