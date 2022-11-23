package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.GodkjentOpplæringsinstitusjon;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.GodkjentOpplæringsinstitusjonPeriode;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.GodkjentOpplæringsinstitusjonRepository;

public class GodkjentOpplæringsinstitusjonTjenesteTest {

    private GodkjentOpplæringsinstitusjonRepository repository;

    private GodkjentOpplæringsinstitusjonTjeneste tjeneste;

    @BeforeEach
    public void setup() {
        repository = mock(GodkjentOpplæringsinstitusjonRepository.class);
        tjeneste = new GodkjentOpplæringsinstitusjonTjeneste(repository);
    }

    @Test
    public void skalHenteMedUuid() {
        LocalDate idag = LocalDate.now();
        GodkjentOpplæringsinstitusjon institusjon = new GodkjentOpplæringsinstitusjon(UUID.randomUUID(), "SFO", idag, idag);
        when(repository.hentMedUuid(institusjon.getUuid())).thenReturn(Optional.of(institusjon));

        Optional<GodkjentOpplæringsinstitusjon> resultat = tjeneste.hentMedUuid(institusjon.getUuid());
        assertThat(resultat).isPresent();
        assertThat(resultat.get().getUuid()).isEqualTo(institusjon.getUuid());
        assertThat(resultat.get().getNavn()).isEqualTo(institusjon.getNavn());
        assertThat(resultat.get().getPerioder()).hasSize(1);
        assertThat(resultat.get().getPerioder().get(0).getPeriode().getFomDato()).isEqualTo(idag);
        assertThat(resultat.get().getPerioder().get(0).getPeriode().getTomDato()).isEqualTo(idag);
    }

    @Test
    public void skalHenteAktivMedUuid() {
        LocalDate idag = LocalDate.now();
        GodkjentOpplæringsinstitusjon institusjon = new GodkjentOpplæringsinstitusjon(UUID.randomUUID(), "SFO", idag, idag);
        when(repository.hentMedUuid(institusjon.getUuid())).thenReturn(Optional.of(institusjon));

        Optional<GodkjentOpplæringsinstitusjon> resultat = tjeneste.hentAktivMedUuid(institusjon.getUuid(), new Periode(idag, idag));
        assertThat(resultat).isPresent();
    }

    @Test
    public void skalIkkeHenteMedUuidInaktivHelePerioden() {
        LocalDate igår = LocalDate.now().minusDays(1);
        LocalDate idag = LocalDate.now();
        GodkjentOpplæringsinstitusjon institusjon = new GodkjentOpplæringsinstitusjon(UUID.randomUUID(), "SFO", igår, igår);
        when(repository.hentMedUuid(institusjon.getUuid())).thenReturn(Optional.of(institusjon));

        Optional<GodkjentOpplæringsinstitusjon> resultat = tjeneste.hentAktivMedUuid(institusjon.getUuid(), new Periode(idag, idag));
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skalIkkekHenteMedUuidInaktivDelerAvPerioden1() {
        LocalDate igår = LocalDate.now().minusDays(1);
        LocalDate idag = LocalDate.now();
        LocalDate imorgen = LocalDate.now().plusDays(1);
        GodkjentOpplæringsinstitusjon institusjon = new GodkjentOpplæringsinstitusjon(UUID.randomUUID(), "SFO", igår, idag);
        when(repository.hentMedUuid(institusjon.getUuid())).thenReturn(Optional.of(institusjon));

        Optional<GodkjentOpplæringsinstitusjon> resultat = tjeneste.hentAktivMedUuid(institusjon.getUuid(), new Periode(idag, imorgen));
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skalIkkekHenteMedUuidInaktivDelerAvPerioden2() {
        LocalDate igår = LocalDate.now().minusDays(1);
        LocalDate imorgen = LocalDate.now().plusDays(1);
        GodkjentOpplæringsinstitusjonPeriode periode1 = new GodkjentOpplæringsinstitusjonPeriode(igår, igår);
        GodkjentOpplæringsinstitusjonPeriode periode2 = new GodkjentOpplæringsinstitusjonPeriode(imorgen, imorgen);
        GodkjentOpplæringsinstitusjon institusjon = new GodkjentOpplæringsinstitusjon(UUID.randomUUID(), "SFO", List.of(periode1, periode2));
        when(repository.hentMedUuid(institusjon.getUuid())).thenReturn(Optional.of(institusjon));

        Optional<GodkjentOpplæringsinstitusjon> resultat = tjeneste.hentAktivMedUuid(institusjon.getUuid(), new Periode(igår, imorgen));
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skalHenteAktive() {
        LocalDate igår = LocalDate.now().minusDays(1);
        LocalDate idag = LocalDate.now();
        GodkjentOpplæringsinstitusjon institusjon1 = new GodkjentOpplæringsinstitusjon(UUID.randomUUID(), "SFO", igår, igår);
        GodkjentOpplæringsinstitusjon institusjon2 = new GodkjentOpplæringsinstitusjon(UUID.randomUUID(), "SFO", idag, idag);
        when(repository.hentAlle()).thenReturn(List.of(institusjon1, institusjon2));

        List<GodkjentOpplæringsinstitusjon> resultat = tjeneste.hentAktive(new Periode(idag, idag));
        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).getUuid()).isEqualTo(institusjon2.getUuid());
    }

    @Test
    public void skalHenteAlle() {
        LocalDate igår = LocalDate.now().minusDays(1);
        LocalDate idag = LocalDate.now();
        GodkjentOpplæringsinstitusjon institusjon1 = new GodkjentOpplæringsinstitusjon(UUID.randomUUID(), "SFO", igår, igår);
        GodkjentOpplæringsinstitusjon institusjon2 = new GodkjentOpplæringsinstitusjon(UUID.randomUUID(), "SFO", idag, idag);
        when(repository.hentAlle()).thenReturn(List.of(institusjon1, institusjon2));

        List<GodkjentOpplæringsinstitusjon> resultat = tjeneste.hentAlle();
        assertThat(resultat).hasSize(2);
    }
}
