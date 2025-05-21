package no.nav.ung.sak.inngangsvilkår.alder;

import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.NavigableSet;

public class VurderAldersVilkårTjeneste {

    public void vurderPerioder(VilkårBuilder vilkårBuilder, NavigableSet<DatoIntervallEntitet> perioderTilVurdering, LocalDate fødselsdato) {
        var førsteDagMedGodkjentAlder = fødselsdato.plusYears(18);
        var sisteDagMedGodkjentAlder = fødselsdato.plusYears(29).minusDays(1);


        var regelInput = """
            { "fødselsdato": ":fødselsdato", "førsteDagMedGodkjentAlder": ":førsteDagMedGodkjentAlder", "sisteDagMedGodkjentAlder: ":sisteDagMedGodkjentAlder"}""".stripLeading()
            .replaceFirst(":fødselsdato", fødselsdato.toString())
            .replaceFirst(":førsteDagMedGodkjentAlder", førsteDagMedGodkjentAlder.toString())
            .replaceFirst(":sisteDagMedGodkjentAlder", sisteDagMedGodkjentAlder.toString());

        final var godkjentIntervall = DatoIntervallEntitet.fraOgMedTilOgMed(førsteDagMedGodkjentAlder, sisteDagMedGodkjentAlder);
        for (var p : perioderTilVurdering) {
            VilkårPeriodeBuilder builder = vilkårBuilder.hentBuilderFor(p);
            if (godkjentIntervall.inkluderer(p.getFomDato())) {
                builder.medUtfall(Utfall.OPPFYLT).medRegelInput(regelInput);
            } else if (p.getFomDato().isBefore(godkjentIntervall.getFomDato())) {
                builder.medUtfall(Utfall.IKKE_OPPFYLT).medAvslagsårsak(Avslagsårsak.SØKER_UNDER_MINSTE_ALDER).medRegelInput(regelInput);
            } else if (p.getFomDato().isAfter(godkjentIntervall.getTomDato())) {
                builder.medUtfall(Utfall.IKKE_OPPFYLT).medAvslagsårsak(Avslagsårsak.SØKER_OVER_HØYESTE_ALDER).medRegelInput(regelInput);
            }
            vilkårBuilder.leggTil(builder);
        }
    }

}
