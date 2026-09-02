package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsfaktaOgAvklaring;
import no.nav.ung.sak.etterlysning.EtterlysningData;
import no.nav.ung.sak.etterlysning.VilkårsavklaringUtfall;

import java.time.LocalDateTime;

/**
 * Per-segment hjelpeobjekt som kombinerer avklaring og etterlysning, og reduserer til et {@link VilkårsavklaringUtfall}.
 */
class BostedAvklaringOgUttalelseOgResultat {

    private final BostedsfaktaOgAvklaring faktaOgAvklaring;
    private final EtterlysningData etterlysning;

    BostedAvklaringOgUttalelseOgResultat(BostedsfaktaOgAvklaring faktaOgAvklaring) {
        this(faktaOgAvklaring, null);
    }

    private BostedAvklaringOgUttalelseOgResultat(BostedsfaktaOgAvklaring faktaOgAvklaring, EtterlysningData etterlysning) {
        this.faktaOgAvklaring = faktaOgAvklaring;
        this.etterlysning = etterlysning;
    }

    BostedAvklaringOgUttalelseOgResultat medEtterlysning(EtterlysningData etterlysning) {
        return new BostedAvklaringOgUttalelseOgResultat(this.faktaOgAvklaring, etterlysning);
    }

    VilkårsavklaringUtfall utledUtfall() {
        var foreslåttAvklaring = faktaOgAvklaring.harForeslåttAvklaring() ? faktaOgAvklaring.getForeslåttAvklaring() : null;
        return VilkårsavklaringUtfall.utled(etterlysning, foreslåttAvklaring, VilkårType.BOSTEDSVILKÅR);
    }

    LocalDateTime getFrist() {
        return etterlysning != null ? etterlysning.frist() : null;
    }

    BostedsPeriodeAvklaring getForeslåttAvklaring() {
        return faktaOgAvklaring.getForeslåttAvklaring();
    }

    EtterlysningData getEtterlysning() {
        return etterlysning;
    }
}

