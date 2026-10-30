package no.nav.ung.ytelse.aktivitetspenger.vilkår.avklaring;

import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.vilkår.IkkeOppfyltDetaljertÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårJsonObjectMapper;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.Regelsporing;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.etterlysning.EtterlysningData;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static no.nav.ung.ytelse.aktivitetspenger.vilkår.avklaring.VilkårsavklaringUtfall.AVSLÅS_AUTOMATISK;
import static no.nav.ung.ytelse.aktivitetspenger.vilkår.avklaring.VilkårsavklaringUtfall.VENTER_PÅ_UTTALELSE_FRA_BRUKER;
import static no.nav.ung.ytelse.aktivitetspenger.vilkår.avklaring.VilkårsavklaringUtfall.VURDERES_MANUELT_ETTER_AVKLARING;
import static no.nav.ung.ytelse.aktivitetspenger.vilkår.avklaring.VilkårsavklaringUtfall.VURDERES_MANUELT_NY_PERIODE;

public class VilkårsavklaringUtfallUtleder {

    private static final VilkårJsonObjectMapper JSON = new VilkårJsonObjectMapper();

    private final VilkårType vilkårType;
    private final VilkårPeriodeAvklaring foreslåttAvklaring;
    private final EtterlysningData etterlysning;
    private final boolean nySøknadsperiode;

    public VilkårsavklaringUtfallUtleder(VilkårType vilkårType, VilkårPeriodeAvklaring foreslåttAvklaring) {
        this(vilkårType, foreslåttAvklaring, null, false);
    }

    private VilkårsavklaringUtfallUtleder(VilkårType vilkårType, VilkårPeriodeAvklaring foreslåttAvklaring, EtterlysningData etterlysning, boolean nySøknadsperiode) {
        this.vilkårType = Objects.requireNonNull(vilkårType, "vilkårType");
        this.foreslåttAvklaring = foreslåttAvklaring;
        this.etterlysning = etterlysning;
        this.nySøknadsperiode = nySøknadsperiode;
    }

    public VilkårsavklaringUtfallUtleder medEtterlysning(EtterlysningData etterlysning) {
        return new VilkårsavklaringUtfallUtleder(this.vilkårType, this.foreslåttAvklaring, etterlysning, this.nySøknadsperiode);
    }

    public VilkårsavklaringUtfallUtleder medNySøknadsperiode() {
        return new VilkårsavklaringUtfallUtleder(this.vilkårType, this.foreslåttAvklaring, this.etterlysning, true);
    }

    // Avklaringen og en ny søknadsperiode gir hvert sitt utfall, siden de vurderes i hvert sitt aksjonspunkt.
    public Set<VilkårsavklaringUtfall> utledUtfall() {
        var utfall = EnumSet.noneOf(VilkårsavklaringUtfall.class);
        if (erVentende()) {
            utfall.add(VENTER_PÅ_UTTALELSE_FRA_BRUKER);
        } else if (foreslåttAvklaring != null) {
            utfall.add(kanAvslåsAutomatisk() ? AVSLÅS_AUTOMATISK : VURDERES_MANUELT_ETTER_AVKLARING);
        }
        if (nySøknadsperiode) {
            utfall.add(VURDERES_MANUELT_NY_PERIODE);
        }
        return utfall;
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
        if (harMottattSvarMedUttalelse() || !foreslåttAvklaring.skalSendeVarsel()) {
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

    // Uttalelsens innhold er utelatt med vilje. Bare om deltakeren har uttalt seg, er relevant.
    public record RegelInput(UUID avklaringReferanse,
                              String ikkeOppfyltÅrsak,
                              boolean skalSendeVarsel,
                              EtterlysningStatus etterlysningStatus,
                              Boolean harUttalelse,
                              LocalDateTime frist,
                              boolean nySøknadsperiode) {
    }

    public record RegelEvaluering(Set<VilkårsavklaringUtfall> utfall) {
    }

    public RegelInput tilRegelInput() {
        var uttalelse = etterlysning != null ? etterlysning.uttalelseData() : null;
        return new RegelInput(
            foreslåttAvklaring != null ? foreslåttAvklaring.getReferanse() : null,
            foreslåttAvklaring != null ? foreslåttAvklaring.getIkkeOppfyltÅrsakKode() : null,
            foreslåttAvklaring != null && foreslåttAvklaring.skalSendeVarsel(),
            etterlysning != null ? etterlysning.status() : null,
            uttalelse != null ? uttalelse.harUttalelse() : null,
            getFrist(),
            nySøknadsperiode);
    }

    public RegelEvaluering tilRegelEvaluering() {
        return new RegelEvaluering(utledUtfall());
    }

    public Regelsporing tilRegelsporing() {
        return new Regelsporing(JSON.writeValueAsString(tilRegelInput()), JSON.writeValueAsString(tilRegelEvaluering()));
    }
}
