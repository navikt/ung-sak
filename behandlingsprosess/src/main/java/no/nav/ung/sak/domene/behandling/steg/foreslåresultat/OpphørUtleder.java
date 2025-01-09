package no.nav.ung.sak.domene.behandling.steg.foreslåresultat;

import java.util.Optional;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateSegmentCombinator;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.kontrakt.vilkår.VilkårUtfallSamlet;
import no.nav.ung.sak.vilkår.SamleVilkårResultat;

class OpphørUtleder {

    static Boolean erOpphør(Vilkårene vilkårene, Optional<Vilkårene> originalVilkårResultat) {
        return originalVilkårResultat
            .map((originaltResultat) -> {
                var samletOriginaltResultat = SamleVilkårResultat.samleVilkårUtfall(originaltResultat);
                var samletGjeldendeResultat = SamleVilkårResultat.samleVilkårUtfall(vilkårene);
                var tidslinjeForNyeAvslag = samletOriginaltResultat.crossJoin(samletGjeldendeResultat, erNyttAvslagKombinator());
                boolean forrigePeriodeInnvilget = false;
                for (var s : tidslinjeForNyeAvslag) {
                    if (forrigePeriodeInnvilget && erNyttAvslag(s)) {
                        return true;
                    }
                    forrigePeriodeInnvilget = samletOriginaltResultat.getSegment(s.getLocalDateInterval()).getValue().getSamletUtfall().equals(Utfall.OPPFYLT);
                }
                return false;

            }).orElse(false);
    }

    private static Boolean erNyttAvslag(LocalDateSegment<Boolean> s) {
        return s.getValue();
    }

    private static LocalDateSegmentCombinator<VilkårUtfallSamlet, VilkårUtfallSamlet, Boolean> erNyttAvslagKombinator() {
        return (di, lhs, rhs) -> {
            if (lhs == null) {
                return new LocalDateSegment<>(di, false);
            }
            if (lhs.getValue().getSamletUtfall().equals(Utfall.OPPFYLT) && rhs == null || rhs.getValue().getSamletUtfall().equals(Utfall.IKKE_OPPFYLT)) {
                return new LocalDateSegment<>(di, true);
            }
            return new LocalDateSegment<>(di, false);
        };
    }



}
