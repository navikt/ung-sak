package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.alder;

import java.time.LocalDate;
import java.util.NavigableSet;

import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class VurderAldersVilkårTjeneste {

    public void vurderPerioder(VilkårBuilder vilkårBuilder, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, LocalDate fødselsdato, LocalDate dødsdato) {
        var maksdato = fødselsdato.plusYears(70);
        if (dødsdato != null && maksdato.isAfter(dødsdato)) {
            maksdato = dødsdato;
        }
        var regelInput = "{ 'fødselsdato': '" + fødselsdato + ", 'dødsdato': '" + dødsdato + "', ', 'maksdato': '" + maksdato + "' }";

        for (DatoIntervallEntitet periode : perioderTilVurdering) {
            vurderPeriode(vilkårBuilder, maksdato, dødsdato, regelInput, periode);
        }
    }

    private void vurderPeriode(VilkårBuilder vilkårBuilder, LocalDate maksdato, LocalDate dødsdato, String regelInput, DatoIntervallEntitet periode) {
        if (periode.overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(maksdato, maksdato)) && !periode.getFomDato().equals(maksdato)) {
            var builder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato(), maksdato.minusDays(1)));
            builder.medUtfall(Utfall.OPPFYLT)
                .medRegelInput(regelInput);

            vilkårBuilder.leggTil(builder);

            builder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fraOgMedTilOgMed(maksdato, periode.getTomDato()));
            builder.medUtfall(Utfall.IKKE_OPPFYLT)
                .medAvslagsårsak(utledAvslagsårsak(maksdato, dødsdato))
                .medRegelInput(regelInput);

            vilkårBuilder.leggTil(builder);

        } else {
            var builder = vilkårBuilder.hentBuilderFor(periode);
            if (periode.getFomDato().isAfter(maksdato) || periode.getFomDato().isEqual(maksdato)) {
                builder.medUtfall(Utfall.IKKE_OPPFYLT)
                    .medAvslagsårsak(utledAvslagsårsak(maksdato, dødsdato))
                    .medRegelInput(regelInput);
            } else {
                builder.medUtfall(Utfall.OPPFYLT)
                    .medRegelInput(regelInput);
            }
            vilkårBuilder.leggTil(builder);
        }
    }

    private Avslagsårsak utledAvslagsårsak(LocalDate maksdato, LocalDate dødsdato) {
        if (dødsdato == null) {
            return Avslagsårsak.SØKER_OVER_HØYESTE_ALDER;
        }
        if (maksdato.isBefore(dødsdato)) {
            return Avslagsårsak.SØKER_OVER_HØYESTE_ALDER;
        }
        return Avslagsårsak.SØKER_HAR_AVGÅTT_MED_DØDEN;
    }
}
