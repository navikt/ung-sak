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
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Per-segment hjelpeobjekt som kombinerer avklaring og etterlysning, og reduserer til {@link StegUtfall}.
 */
class BostedAvklaringOgUttalelseOgResultat {

    private static final VilkårJsonObjectMapper JSON = new VilkårJsonObjectMapper();

    private final BostedsfaktaOgAvklaring faktaOgAvklaring;
    private final EtterlysningData etterlysning;
    private final BostedsvilkårResultatPeriode resultat;
    private final boolean nySøknadsperiode;

    BostedAvklaringOgUttalelseOgResultat(BostedsfaktaOgAvklaring faktaOgAvklaring) {
        this(faktaOgAvklaring, null, null, false);
    }

    private BostedAvklaringOgUttalelseOgResultat(BostedsfaktaOgAvklaring faktaOgAvklaring, EtterlysningData etterlysning, BostedsvilkårResultatPeriode resultat, boolean nySøknadsperiode) {
        this.faktaOgAvklaring = faktaOgAvklaring;
        this.etterlysning = etterlysning;
        this.resultat = resultat;
        this.nySøknadsperiode = nySøknadsperiode;
    }

    BostedAvklaringOgUttalelseOgResultat medEtterlysning(EtterlysningData etterlysning) {
        return new BostedAvklaringOgUttalelseOgResultat(this.faktaOgAvklaring, etterlysning, this.resultat, this.nySøknadsperiode);
    }

    BostedAvklaringOgUttalelseOgResultat medResultat(BostedsvilkårResultatPeriode resultat) {
        return new BostedAvklaringOgUttalelseOgResultat(this.faktaOgAvklaring, this.etterlysning, resultat, this.nySøknadsperiode);
    }

    BostedAvklaringOgUttalelseOgResultat medNySøknadsperiode() {
        return new BostedAvklaringOgUttalelseOgResultat(this.faktaOgAvklaring, this.etterlysning, this.resultat, true);
    }

    // Avklaringen og en ny søknadsperiode gir hvert sitt utfall, siden de vurderes i hvert sitt aksjonspunkt.
    Set<StegUtfall> utledUtfall() {
        var utfall = EnumSet.noneOf(StegUtfall.class);
        utledUtfallForAvklaring().ifPresent(utfall::add);
        if (nySøknadsperiode) {
            utfall.add(StegUtfall.VURDERES_MANUELT_NY_PERIODE);
        }
        return utfall;
    }

    // Bostedsfakta fra en avklaring gir aldri bosatt i Trondheim, så en avklaring som ikke vurderes manuelt, gir opphør.
    private Optional<StegUtfall> utledUtfallForAvklaring() {
        if (erVentende()) {
            return Optional.of(StegUtfall.VENTER_PÅ_UTTALELSE_FRA_BRUKER);
        }
        if (faktaOgAvklaring.getForeslåttAvklaring() == null) {
            return Optional.empty();
        }
        if (harMottattSvarMedUttalelse() || erÅrsakAnnet() || erValgtÅIkkeVarsleNårIkkeOppfylt()) {
            return Optional.of(StegUtfall.VURDERES_MANUELT_ETTER_AVKLARING);
        }
        return Optional.of(StegUtfall.OPPHØR_AUTOMATISK);
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
            getFrist(),
            nySøknadsperiode);
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
                      LocalDateTime frist,
                      boolean nySøknadsperiode) {
    }

    record RegelEvaluering(Set<StegUtfall> utfall) {
    }

    enum StegUtfall {
        OPPHØR_AUTOMATISK,
        VURDERES_MANUELT_ETTER_AVKLARING,
        VENTER_PÅ_UTTALELSE_FRA_BRUKER,
        VURDERES_MANUELT_NY_PERIODE
    }
}

