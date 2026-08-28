package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import no.nav.ung.kodeverk.bosatt.Kilde;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsfaktaOgAvklaring;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BostedsvilkårResultatPeriode;
import no.nav.ung.sak.etterlysning.EtterlysningData;

import java.time.LocalDateTime;

/**
 * Per-segment hjelpeobjekt som kombinerer avklaring og etterlysning,
 * og reduserer til et {@link StegUtfall}.
 * Bygges opp via tidslinje-combinators, inspirert av PgiØvreGrenseVurderer.
 */
class BostedAvklaringOgUttalelseOgResultat {

    private final BostedsfaktaOgAvklaring faktaOgAvklaring;
    private final EtterlysningData etterlysning;
    private final BostedsvilkårResultatPeriode resultat;

    BostedAvklaringOgUttalelseOgResultat(BostedsfaktaOgAvklaring faktaOgAvklaring) {
        this(faktaOgAvklaring, null, null);
    }

    private BostedAvklaringOgUttalelseOgResultat(BostedsfaktaOgAvklaring faktaOgAvklaring, EtterlysningData etterlysning, BostedsvilkårResultatPeriode resultat) {
        this.faktaOgAvklaring = faktaOgAvklaring;
        this.etterlysning = etterlysning;
        this.resultat = resultat;
    }

    BostedAvklaringOgUttalelseOgResultat medEtterlysning(EtterlysningData etterlysning) {
        return new BostedAvklaringOgUttalelseOgResultat(this.faktaOgAvklaring, etterlysning, this.resultat);
    }

    BostedAvklaringOgUttalelseOgResultat medResultat(BostedsvilkårResultatPeriode resultat) {
        return new BostedAvklaringOgUttalelseOgResultat(this.faktaOgAvklaring, this.etterlysning, resultat);
    }

    StegUtfall utledUtfall() {
        if (erVentende()) {
            return StegUtfall.VENTER_PÅ_UTTALELSE_FRA_BRUKER;
        } else if (erKildeSøknadOgIkkeTidligereVurdert() || harMottattSvarMedUttalelse() || erÅrsakAnnet() || erValgtÅIkkeVarsleNårIkkeOppfylt()) {
            return StegUtfall.VILKÅR_VURDERES_MANUELT;
        } else if (!faktaOgAvklaring.isErBosattITrondheim()) {
            return StegUtfall.OPPHØR_AUTOMATISK;
        }
        return StegUtfall.BOSATT_HELE_PERIODEN;
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

    private boolean erKildeSøknadOgIkkeTidligereVurdert() {
        return Kilde.SØKNAD.equals(faktaOgAvklaring.getKilde()) && resultat == null;
    }

    private boolean erValgtÅIkkeVarsleNårIkkeOppfylt() {
        var foreslåttAvklaring = faktaOgAvklaring.getForeslåttAvklaring();
        return foreslåttAvklaring != null && !foreslåttAvklaring.skalSendeVarsel();
    }

    private boolean erÅrsakAnnet() {
        return BostedsvilkårIkkeOppfyltÅrsak.ANNET.equals(faktaOgAvklaring.getIkkeOppfyltÅrsak());
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
        VENTER_PÅ_UTTALELSE_FRA_BRUKER,
        BOSATT_HELE_PERIODEN
    }
}

