package no.nav.k9.sak.web.app.tjenester.behandling.sykdom;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.k9.sak.typer.Periode;

public class SykdomVurderingRestTjenesteTest {

    @Test
    public void verifyPerioderInneholderFørOgEtter18år() {
        assertThat(SykdomVurderingRestTjeneste.isPerioderInneholderFørOgEtter18år(List.of(new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 10))), LocalDate.of(2003, 1, 1))).isFalse();
        assertThat(SykdomVurderingRestTjeneste.isPerioderInneholderFørOgEtter18år(List.of(new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 10))), LocalDate.of(2003, 1, 2))).isTrue();
        assertThat(SykdomVurderingRestTjeneste.isPerioderInneholderFørOgEtter18år(List.of(new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 10))), LocalDate.of(2003, 1, 10))).isTrue();
        assertThat(SykdomVurderingRestTjeneste.isPerioderInneholderFørOgEtter18år(List.of(new Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 10))), LocalDate.of(2003, 1, 11))).isFalse();
    }
}
