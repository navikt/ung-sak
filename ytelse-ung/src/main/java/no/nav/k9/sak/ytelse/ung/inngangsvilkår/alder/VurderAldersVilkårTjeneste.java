package no.nav.k9.sak.ytelse.ung.inngangsvilkår.alder;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.NavigableSet;

import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class VurderAldersVilkårTjeneste {

    public void vurderPerioder(VilkårBuilder vilkårBuilder, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, LocalDate fødselsdato) {
        var maksdato = fødselsdato.plusYears(29).with(TemporalAdjusters.lastDayOfMonth());
        var regelInput = "{ 'fødselsdato': '" + fødselsdato + "', ', 'maksdato': '" + maksdato + "' }";

        for (DatoIntervallEntitet periode : perioderTilVurdering) {
            vurderPeriode(vilkårBuilder, maksdato, regelInput, periode);
        }
    }

    private void vurderPeriode(VilkårBuilder vilkårBuilder, LocalDate sisteDagMedGodkjentAlder, String regelInput, DatoIntervallEntitet periode) {
        var førsteDagMedForHøyAlder = sisteDagMedGodkjentAlder.plusDays(1);
        if (periode.inkluderer(førsteDagMedForHøyAlder) && !periode.getFomDato().equals(førsteDagMedForHøyAlder)) {
            var builder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFomDato(), sisteDagMedGodkjentAlder));
            builder.medUtfall(Utfall.OPPFYLT)
                .medRegelInput(regelInput);

            vilkårBuilder.leggTil(builder);

            builder = vilkårBuilder.hentBuilderFor(DatoIntervallEntitet.fraOgMedTilOgMed(førsteDagMedForHøyAlder, periode.getTomDato()));
            builder.medUtfall(Utfall.IKKE_OPPFYLT)
                .medAvslagsårsak(Avslagsårsak.SØKER_OVER_HØYESTE_ALDER)
                .medRegelInput(regelInput);

            vilkårBuilder.leggTil(builder);

        } else {
            var builder = vilkårBuilder.hentBuilderFor(periode);
            if (periode.getFomDato().isAfter(sisteDagMedGodkjentAlder) || periode.getFomDato().isEqual(sisteDagMedGodkjentAlder)) {
                builder.medUtfall(Utfall.IKKE_OPPFYLT)
                    .medAvslagsårsak(Avslagsårsak.SØKER_OVER_HØYESTE_ALDER)
                    .medRegelInput(regelInput);
            } else {
                builder.medUtfall(Utfall.OPPFYLT)
                    .medRegelInput(regelInput);
            }
            vilkårBuilder.leggTil(builder);
        }
    }

}
