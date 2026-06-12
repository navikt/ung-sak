package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import no.nav.ung.kodeverk.bosatt.FraflyttingsÅrsak;
import no.nav.ung.kodeverk.bosatt.Kilde;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsPeriodeAvklaring;
import no.nav.ung.sak.etterlysning.EtterlysningData;

import java.time.LocalDateTime;

/**
 * Per-segment hjelpeobjekt som kombinerer avklaring og etterlysning,
 * og reduserer til et {@link StegUtfall}.
 * Bygges opp via tidslinje-combinators, inspirert av PgiØvreGrenseVurderer.
 */
class BostedAvklaringOgUttalelseOgResultat {

    private final BostedsPeriodeAvklaring avklaring;
    private EtterlysningData etterlysning;

    BostedAvklaringOgUttalelseOgResultat(BostedsPeriodeAvklaring avklaring) {
        this.avklaring = avklaring;
    }

    BostedAvklaringOgUttalelseOgResultat medEtterlysning(EtterlysningData etterlysning) {
        this.etterlysning = etterlysning;
        return this;
    }

    StegUtfall utledUtfall() {
        if (erVentende()) {
            return StegUtfall.VENTER_PÅ_UTTALELSE_FRA_BRUKER;
        } else if (erKildeSøknad() || harMottattSvarMedUttalelse() || erÅrsakAnnet()) {
            return StegUtfall.VILKÅR_VURDERES_MANUELT;
        } else if (!avklaring.isErBosattITrondheim()) {
            return StegUtfall.OPPHØR_AUTOMATISK;
        }
        return StegUtfall.BOSATT_HELE_PERIODEN;
    }

    LocalDateTime getFrist() {
        return etterlysning != null ? etterlysning.frist() : null;
    }

    BostedsPeriodeAvklaring getAvklaring() {
        return avklaring;
    }

    EtterlysningData getEtterlysning() {
        return etterlysning;
    }

    private boolean erKildeSøknad() {
        return Kilde.SØKNAD.equals(avklaring.getKilde());
    }

    private boolean erÅrsakAnnet() {
        return FraflyttingsÅrsak.ANNET.equals(avklaring.getFraflyttingsÅrsak());
    }

    private boolean erVentende() {
        return etterlysning != null
            && (etterlysning.status() == EtterlysningStatus.OPPRETTET
            || etterlysning.status() == EtterlysningStatus.VENTER);
    }

    private boolean harMottattSvarMedUttalelse() {
        return etterlysning != null
            && etterlysning.status() == EtterlysningStatus.MOTTATT_SVAR
            && etterlysning.uttalelseData().harUttalelse();
    }

    enum StegUtfall {
        OPPHØR_AUTOMATISK,
        VILKÅR_VURDERES_MANUELT,
        VENTER_PÅ_UTTALELSE_FRA_BRUKER,
        BOSATT_HELE_PERIODEN
    }
}

