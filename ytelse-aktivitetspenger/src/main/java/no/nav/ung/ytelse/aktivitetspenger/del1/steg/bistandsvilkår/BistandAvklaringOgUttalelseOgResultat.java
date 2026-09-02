package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bistandsvilkår;

import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultat;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.etterlysning.EtterlysningData;

import java.time.LocalDateTime;

/**
 * Per-segment hjelpeobjekt som kombinerer foreslått avklaring, etterlysning og tidligere vilkårsresultat,
 * og reduserer til et {@link StegUtfall}. Bygges opp via tidslinje-combinators.
 * <p>
 * Bevisst duplisert fra {@code BostedAvklaringOgUttalelseOgResultat} framfor å arve: bosted har en
 * fakta-akse (kilde søknad/saksbehandler, "bosatt hele perioden") som bistand ikke har.
 */
class BistandAvklaringOgUttalelseOgResultat {

    private final VilkårPeriodeAvklaring foreslåttAvklaring;
    private final EtterlysningData etterlysning;
    private final VilkårsvurderingResultat resultat;

    BistandAvklaringOgUttalelseOgResultat(VilkårPeriodeAvklaring foreslåttAvklaring) {
        this(foreslåttAvklaring, null, null);
    }

    private BistandAvklaringOgUttalelseOgResultat(VilkårPeriodeAvklaring foreslåttAvklaring, EtterlysningData etterlysning, VilkårsvurderingResultat resultat) {
        this.foreslåttAvklaring = foreslåttAvklaring;
        this.etterlysning = etterlysning;
        this.resultat = resultat;
    }

    BistandAvklaringOgUttalelseOgResultat medEtterlysning(EtterlysningData etterlysning) {
        return new BistandAvklaringOgUttalelseOgResultat(this.foreslåttAvklaring, etterlysning, this.resultat);
    }

    BistandAvklaringOgUttalelseOgResultat medResultat(VilkårsvurderingResultat resultat) {
        return new BistandAvklaringOgUttalelseOgResultat(this.foreslåttAvklaring, this.etterlysning, resultat);
    }

    StegUtfall utledUtfall() {
        if (erVentende()) {
            return StegUtfall.VENTER_PÅ_UTTALELSE_FRA_BRUKER;
        }
        if (foreslåttAvklaring != null) {
            if (harMottattSvarMedUttalelse() || erValgtÅIkkeVarsleNårIkkeOppfylt() || !girAutomatiskAvslag()) {
                return StegUtfall.VILKÅR_VURDERES_MANUELT;
            }
            return StegUtfall.AVSLAG_AUTOMATISK;
        }
        return erTidligereVurdertSomOppfylt() ? StegUtfall.OPPFYLT : StegUtfall.VILKÅR_VURDERES_MANUELT;
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

    private boolean erTidligereVurdertSomOppfylt() {
        return resultat != null && resultat.godkjent();
    }

    private boolean erValgtÅIkkeVarsleNårIkkeOppfylt() {
        return !foreslåttAvklaring.skalSendeVarsel();
    }

    /**
     * Kun {@link BistandsvilkårIkkeOppfyltÅrsak#IKKE_14A_VEDTAK} kan avslås maskinelt. Øvrige årsaker
     * (inkludert {@code UDEFINERT}) krever at saksbehandler tar stilling til vilkåret.
     */
    private boolean girAutomatiskAvslag() {
        return getIkkeOppfyltÅrsak() == BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK;
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
        VENTER_PÅ_UTTALELSE_FRA_BRUKER,
        VILKÅR_VURDERES_MANUELT,
        AVSLAG_AUTOMATISK,
        OPPFYLT
    }
}
