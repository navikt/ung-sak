package no.nav.ung.ytelse.aktivitetspenger.vilkår.avklaring;

import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.vilkår.IkkeOppfyltDetaljertÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.etterlysning.EtterlysningData;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Per-segment hjelpeobjekt som bygges opp via tidslinje-combinators. Automatisk avslag krever at samtlige
 * forutsetninger er innfridd; alt annet faller tilbake til manuell vurdering.
 */
public class VilkårsavklaringUtfallUtleder {

    private final VilkårType vilkårType;
    private final VilkårPeriodeAvklaring foreslåttAvklaring;
    private final EtterlysningData etterlysning;

    public VilkårsavklaringUtfallUtleder(VilkårType vilkårType, VilkårPeriodeAvklaring foreslåttAvklaring) {
        this(vilkårType, foreslåttAvklaring, null);
    }

    private VilkårsavklaringUtfallUtleder(VilkårType vilkårType, VilkårPeriodeAvklaring foreslåttAvklaring, EtterlysningData etterlysning) {
        this.vilkårType = Objects.requireNonNull(vilkårType, "vilkårType");
        this.foreslåttAvklaring = foreslåttAvklaring;
        this.etterlysning = etterlysning;
    }

    public VilkårsavklaringUtfallUtleder medEtterlysning(EtterlysningData etterlysning) {
        return new VilkårsavklaringUtfallUtleder(this.vilkårType, this.foreslåttAvklaring, etterlysning);
    }

    public VilkårsavklaringUtfall utledUtfall() {
        if (erVentende()) {
            return VilkårsavklaringUtfall.VENTER_PÅ_UTTALELSE_FRA_BRUKER;
        }
        return kanAvslåsAutomatisk()
            ? VilkårsavklaringUtfall.AVSLÅS_AUTOMATISK
            : VilkårsavklaringUtfall.VILKÅR_VURDERES_MANUELT;
    }

    public LocalDateTime getFrist() {
        return etterlysning != null ? etterlysning.frist() : null;
    }

    public VilkårPeriodeAvklaring getForeslåttAvklaring() {
        return foreslåttAvklaring;
    }

    public EtterlysningData getEtterlysning() {
        return etterlysning;
    }

    private boolean kanAvslåsAutomatisk() {
        if (foreslåttAvklaring == null || harMottattSvarMedUttalelse() || !foreslåttAvklaring.skalSendeVarsel()) {
            return false;
        }
        var ikkeOppfyltÅrsak = IkkeOppfyltDetaljertÅrsak.fraKode(vilkårType, foreslåttAvklaring.getIkkeOppfyltÅrsakKode());
        return ikkeOppfyltÅrsak != null
            && !ikkeOppfyltÅrsak.kreverFritekst()
            && ikkeOppfyltÅrsak.avslagsårsak().isPresent();
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
}
