package no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.bosatt;

import no.nav.ung.kodeverk.bosatt.Kilde;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårJsonObjectMapper;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.Regelsporing;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BostedsvilkårResultatPeriode;
import no.nav.ung.sak.etterlysning.EtterlysningData;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Per-segment hjelpeobjekt som kombinerer avklaring og etterlysning, og reduserer til et {@link StegUtfall}.
 */
class BostedAvklaringOgUttalelseOgResultat {

    private static final VilkårJsonObjectMapper JSON = new VilkårJsonObjectMapper();

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
        } else if (erKildeSøknad() || harMottattSvarMedUttalelse() || erÅrsakAnnet() || erValgtÅIkkeVarsleNårIkkeOppfylt()) {
            return StegUtfall.VILKÅR_VURDERES_MANUELT;
        } else if (!faktaOgAvklaring.isErBosattITrondheim()) {
            return StegUtfall.OPPHØR_AUTOMATISK;
        }
        return StegUtfall.BOSATT_HELE_PERIODEN;
    }

    Regelsporing tilRegelsporing() {
        return new Regelsporing(JSON.writeValueAsString(tilRegelInput()), JSON.writeValueAsString(new RegelEvaluering(utledUtfall())));
    }

    RegelInput tilRegelInput() {
        var foreslått = faktaOgAvklaring.getForeslåttAvklaring();
        var uttalelse = etterlysning != null ? etterlysning.uttalelseData() : null;
        return new RegelInput(
            faktaOgAvklaring.getKilde(),
            faktaOgAvklaring.isErBosattITrondheim(),
            foreslått != null ? foreslått.getReferanse() : null,
            foreslått != null ? foreslått.getIkkeOppfyltÅrsakKode() : null,
            foreslått != null && foreslått.skalSendeVarsel(),
            etterlysning != null ? etterlysning.status() : null,
            uttalelse != null ? uttalelse.harUttalelse() : null,
            getFrist());
    }

    LocalDateTime getFrist() {
        return etterlysning != null ? etterlysning.frist() : null;
    }

    VilkårPeriodeAvklaring getForeslåttAvklaring() {
        return faktaOgAvklaring.getForeslåttAvklaring();
    }

    EtterlysningData getEtterlysning() {
        return etterlysning;
    }

    private boolean erKildeSøknad() {
        return faktaOgAvklaring.getKilde() == Kilde.SØKNAD;
    }

    private boolean erValgtÅIkkeVarsleNårIkkeOppfylt() {
        var foreslåttAvklaring = faktaOgAvklaring.getForeslåttAvklaring();
        return foreslåttAvklaring != null && !foreslåttAvklaring.skalSendeVarsel();
    }

    private boolean erÅrsakAnnet() {
        var foreslåttAvklaring = faktaOgAvklaring.getForeslåttAvklaring();
        return foreslåttAvklaring != null && BostedsvilkårIkkeOppfyltÅrsak.ANNET == BostedsvilkårIkkeOppfyltÅrsak.fraKode(foreslåttAvklaring.getIkkeOppfyltÅrsakKode());
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

    record RegelInput(Kilde kilde,
                      boolean erBosattITrondheim,
                      UUID avklaringReferanse,
                      String ikkeOppfyltÅrsak,
                      boolean skalSendeVarsel,
                      EtterlysningStatus etterlysningStatus,
                      Boolean harUttalelse,
                      LocalDateTime frist) {
    }

    record RegelEvaluering(StegUtfall utfall) {
    }

    enum StegUtfall {
        OPPHØR_AUTOMATISK,
        VILKÅR_VURDERES_MANUELT,
        VENTER_PÅ_UTTALELSE_FRA_BRUKER,
        BOSATT_HELE_PERIODEN
    }
}

