package no.nav.ung.sak.etterlysning;

import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;

public enum VilkårsavklaringUtfall {
    VENTER_PÅ_UTTALELSE_FRA_BRUKER,
    VILKÅR_VURDERES_MANUELT,
    AVSLÅS_AUTOMATISK,
    INGEN_AVKLARING;

    public static VilkårsavklaringUtfall utled(EtterlysningData etterlysning, VilkårPeriodeAvklaring avklaring, VilkårType vilkårType) {
        if (erVentende(etterlysning)) {
            return VENTER_PÅ_UTTALELSE_FRA_BRUKER;
        }
        if (avklaring == null) {
            return INGEN_AVKLARING;
        }
        if (harMottattSvarMedUttalelse(etterlysning) || avklaring.krevesManuellVurdering() || !kanAvgjøresAutomatisk(avklaring, vilkårType)) {
            return VILKÅR_VURDERES_MANUELT;
        }
        return AVSLÅS_AUTOMATISK;
    }

    private static boolean kanAvgjøresAutomatisk(VilkårPeriodeAvklaring avklaring, VilkårType vilkårType) {
        var årsak = vilkårType.ikkeOppfyltÅrsak(avklaring.getIkkeOppfyltÅrsakKode());
        return årsak.erGyldigAvklaringsårsak() && årsak.avslagsårsak().isPresent() && !årsak.krevesFritekst();
    }

    private static boolean erVentende(EtterlysningData etterlysning) {
        return etterlysning != null
            && (etterlysning.status() == EtterlysningStatus.OPPRETTET
            || etterlysning.status() == EtterlysningStatus.VENTER);
    }

    private static boolean harMottattSvarMedUttalelse(EtterlysningData etterlysning) {
        return etterlysning != null
            && etterlysning.status() == EtterlysningStatus.MOTTATT_SVAR
            && etterlysning.uttalelseData() != null && etterlysning.uttalelseData().harUttalelse();
    }
}
