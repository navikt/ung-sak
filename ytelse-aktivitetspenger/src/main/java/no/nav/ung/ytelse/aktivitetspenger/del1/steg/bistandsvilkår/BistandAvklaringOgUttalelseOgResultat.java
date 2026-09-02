package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bistandsvilkår;

import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.etterlysning.EtterlysningData;
import no.nav.ung.sak.etterlysning.VilkårsavklaringUtfall;

import java.time.LocalDateTime;

/**
 * Per-segment hjelpeobjekt som kombinerer foreslått avklaring og etterlysning, og reduserer til et
 * {@link VilkårsavklaringUtfall}. Bygges opp via tidslinje-combinators.
 */
class BistandAvklaringOgUttalelseOgResultat {

    private final VilkårPeriodeAvklaring foreslåttAvklaring;
    private final EtterlysningData etterlysning;

    BistandAvklaringOgUttalelseOgResultat(VilkårPeriodeAvklaring foreslåttAvklaring) {
        this(foreslåttAvklaring, null);
    }

    private BistandAvklaringOgUttalelseOgResultat(VilkårPeriodeAvklaring foreslåttAvklaring, EtterlysningData etterlysning) {
        this.foreslåttAvklaring = foreslåttAvklaring;
        this.etterlysning = etterlysning;
    }

    BistandAvklaringOgUttalelseOgResultat medEtterlysning(EtterlysningData etterlysning) {
        return new BistandAvklaringOgUttalelseOgResultat(this.foreslåttAvklaring, etterlysning);
    }

    VilkårsavklaringUtfall utledUtfall() {
        return VilkårsavklaringUtfall.utled(etterlysning, foreslåttAvklaring, VilkårType.BISTANDSVILKÅR);
    }

    LocalDateTime getFrist() {
        return etterlysning != null ? etterlysning.frist() : null;
    }

    VilkårPeriodeAvklaring getForeslåttAvklaring() {
        return foreslåttAvklaring;
    }

    EtterlysningData getEtterlysning() {
        return etterlysning;
    }

    BistandsvilkårIkkeOppfyltÅrsak getIkkeOppfyltÅrsak() {
        return foreslåttAvklaring == null ? null : BistandsvilkårIkkeOppfyltÅrsak.fraKode(foreslåttAvklaring.getIkkeOppfyltÅrsakKode());
    }
}

