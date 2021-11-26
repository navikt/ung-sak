package no.nav.k9.sak.ytelse.pleiepengerbarn.vilkår;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

class PSBVilkårsPerioderTilVurderingTjenesteTest {

    private PSBVilkårsPerioderTilVurderingTjeneste tjeneste = new PSBVilkårsPerioderTilVurderingTjeneste();

    @Test
    void hensyntaFullstendigTidslinje() {
        var start1 = LocalDate.of(2021, 11, 8);
        var slutt1 = LocalDate.of(2021, 11, 12);
        var start2 = LocalDate.of(2021, 11, 15);
        var slutt2 = LocalDate.of(2021, 11, 21);

        var fullstendigePerioder = new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(start1, slutt1), DatoIntervallEntitet.fraOgMedTilOgMed(start2, slutt2)));

        var result = tjeneste.utledPeriodeEtterHensynÅHaHensyntattFullstendigTidslinje(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(start2, slutt2)), fullstendigePerioder);

        assertThat(result).contains(DatoIntervallEntitet.fraOgMedTilOgMed(start1, slutt2));
    }
}
