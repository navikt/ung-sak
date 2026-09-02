package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsfaktaOgAvklaring;
import no.nav.ung.sak.etterlysning.EtterlysningData;

import java.time.LocalDateTime;

/**
 * Per-segment hjelpeobjekt som kombinerer avklaring og etterlysning, og reduserer til et {@link StegUtfall}.
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

    StegUtfall utledUtfall() {
        if (erVentende()) {
            return StegUtfall.VENTER_PÅ_UTTALELSE_FRA_BRUKER;
        }
        if (faktaOgAvklaring.harForeslåttAvklaring()) {
            if (harMottattSvarMedUttalelse() || erÅrsakAnnet() || erValgtÅIkkeVarsleNårIkkeOppfylt()) {
                return StegUtfall.VILKÅR_VURDERES_MANUELT;
            }
            return StegUtfall.OPPHØR_AUTOMATISK;
        }
        return StegUtfall.VILKÅR_VURDERES_MANUELT;
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

    private boolean erValgtÅIkkeVarsleNårIkkeOppfylt() {
        var foreslåttAvklaring = faktaOgAvklaring.getForeslåttAvklaring();
        return foreslåttAvklaring != null && !foreslåttAvklaring.skalSendeVarsel();
    }

    private boolean erÅrsakAnnet() {
        var ikkeOppfyltÅrsak = faktaOgAvklaring.harForeslåttAvklaring() ? faktaOgAvklaring.getForeslåttAvklaring().getIkkeOppfyltÅrsak() : null;
        return BostedsvilkårIkkeOppfyltÅrsak.ANNET.equals(ikkeOppfyltÅrsak);
    }

    private boolean erVentende() {
        return etterlysning != null
            && (etterlysning.status() == EtterlysningStatus.OPPRETTET
            || etterlysning.status() == EtterlysningStatus.VENTER);
    }

    private boolean harMottattSvarMedUttalelse() {
        return etterlysning != null
            && etterlysning.status() == EtterlysningStatus.MOTTATT_SVAR
            && etterlysning.uttalelseData() != null && etterlysning.uttalelseData().harUttalelse();
    }

    enum StegUtfall {
        OPPHØR_AUTOMATISK,
        VILKÅR_VURDERES_MANUELT,
        VENTER_PÅ_UTTALELSE_FRA_BRUKER
    }
}

