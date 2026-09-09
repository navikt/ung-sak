package no.nav.ung.ytelse.aktivitetspenger.vilkår.avklaring;

import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.vilkår.*;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaringForeslått;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.EtterlysningData;
import no.nav.ung.sak.etterlysning.UttalelseData;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VilkårsavklaringUtfallUtlederTest {

    private static final DatoIntervallEntitet PERIODE = DatoIntervallEntitet.fraOgMedTilOgMed(
        LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));

    private static final IkkeOppfyltDetaljertÅrsak AUTOMATISERBAR = BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM;
    private static final IkkeOppfyltDetaljertÅrsak KREVER_FRITEKST = BostedsvilkårIkkeOppfyltÅrsak.ANNET;
    private static final IkkeOppfyltDetaljertÅrsak UTEN_AVSLAGSÅRSAK = BistandsvilkårIkkeOppfyltÅrsak.UDEFINERT;

    private static final String BOSTED_KILDE = BostedsavklaringKildeType.FOLKEREGISTER.getKode();
    private static final String BISTAND_KILDE = BistandsavklaringKildeType.BRUKER.getKode();

    @Test
    void skal_avslå_automatisk_når_varslet_avklaring_har_maskinell_årsak_uten_uttalelse() {
        var utleder = utleder(VilkårType.BOSTEDSVILKÅR, avklaring(VilkårType.BOSTEDSVILKÅR, AUTOMATISERBAR, BOSTED_KILDE, true))
            .medEtterlysning(etterlysning(EtterlysningStatus.MOTTATT_SVAR, false));

        assertThat(utleder.utledUtfall()).isEqualTo(VilkårsavklaringUtfall.AVSLÅS_AUTOMATISK);
    }

    @Test
    void skal_avslå_automatisk_når_etterlysning_er_utløpt_uten_svar() {
        var utleder = utleder(VilkårType.BOSTEDSVILKÅR, avklaring(VilkårType.BOSTEDSVILKÅR, AUTOMATISERBAR, BOSTED_KILDE, true))
            .medEtterlysning(etterlysning(EtterlysningStatus.UTLØPT, false));

        assertThat(utleder.utledUtfall()).isEqualTo(VilkårsavklaringUtfall.AVSLÅS_AUTOMATISK);
    }

    @Test
    void skal_vente_når_etterlysning_er_opprettet_eller_venter() {
        var avklaring = avklaring(VilkårType.BOSTEDSVILKÅR, AUTOMATISERBAR, BOSTED_KILDE, true);

        assertThat(utleder(VilkårType.BOSTEDSVILKÅR, avklaring)
            .medEtterlysning(etterlysning(EtterlysningStatus.OPPRETTET, false)).utledUtfall())
            .isEqualTo(VilkårsavklaringUtfall.VENTER_PÅ_UTTALELSE_FRA_BRUKER);
        assertThat(utleder(VilkårType.BOSTEDSVILKÅR, avklaring)
            .medEtterlysning(etterlysning(EtterlysningStatus.VENTER, true)).utledUtfall())
            .isEqualTo(VilkårsavklaringUtfall.VENTER_PÅ_UTTALELSE_FRA_BRUKER);
    }

    @Test
    void skal_vurderes_manuelt_når_bruker_har_gitt_uttalelse() {
        var utleder = utleder(VilkårType.BOSTEDSVILKÅR, avklaring(VilkårType.BOSTEDSVILKÅR, AUTOMATISERBAR, BOSTED_KILDE, true))
            .medEtterlysning(etterlysning(EtterlysningStatus.MOTTATT_SVAR, true));

        assertThat(utleder.utledUtfall()).isEqualTo(VilkårsavklaringUtfall.VILKÅR_VURDERES_MANUELT);
    }

    @Test
    void skal_vurderes_manuelt_når_det_ikke_finnes_foreslått_avklaring() {
        assertThat(utleder(VilkårType.BOSTEDSVILKÅR, null).utledUtfall())
            .isEqualTo(VilkårsavklaringUtfall.VILKÅR_VURDERES_MANUELT);
    }

    @Test
    void skal_vurderes_manuelt_når_årsak_krever_fritekst() {
        var utleder = utleder(VilkårType.BOSTEDSVILKÅR, avklaring(VilkårType.BOSTEDSVILKÅR, KREVER_FRITEKST, BOSTED_KILDE, true))
            .medEtterlysning(etterlysning(EtterlysningStatus.UTLØPT, false));

        assertThat(utleder.utledUtfall()).isEqualTo(VilkårsavklaringUtfall.VILKÅR_VURDERES_MANUELT);
    }

    @Test
    void skal_vurderes_manuelt_når_årsak_mangler_avslagsårsak() {
        var utleder = utleder(VilkårType.BISTANDSVILKÅR, avklaring(VilkårType.BISTANDSVILKÅR, UTEN_AVSLAGSÅRSAK, BISTAND_KILDE, true))
            .medEtterlysning(etterlysning(EtterlysningStatus.UTLØPT, false));

        assertThat(utleder.utledUtfall()).isEqualTo(VilkårsavklaringUtfall.VILKÅR_VURDERES_MANUELT);
    }

    @Test
    void skal_vurderes_manuelt_når_saksbehandler_har_valgt_å_ikke_varsle() {
        var utleder = utleder(VilkårType.BOSTEDSVILKÅR, avklaring(VilkårType.BOSTEDSVILKÅR, AUTOMATISERBAR, BOSTED_KILDE, false));

        assertThat(utleder.utledUtfall()).isEqualTo(VilkårsavklaringUtfall.VILKÅR_VURDERES_MANUELT);
    }

    private static VilkårsavklaringUtfallUtleder utleder(VilkårType vilkårType, VilkårPeriodeAvklaring avklaring) {
        return new VilkårsavklaringUtfallUtleder(vilkårType, avklaring);
    }

    private static VilkårPeriodeAvklaring avklaring(VilkårType vilkårType, IkkeOppfyltDetaljertÅrsak årsak, String kildeKode, boolean skalSendeVarsel) {
        return new VilkårPeriodeAvklaringForeslått(
            PERIODE,
            årsak.getKode(),
            "begrunnelse",
            skalSendeVarsel,
            skalSendeVarsel ? "fritekst" : null,
            skalSendeVarsel ? null : "begrunnelse for ikke varsel",
            AvklaringKilde.fraKode(vilkårType, kildeKode),
            null,
            "Z999999",
            LocalDateTime.of(2025, 1, 5, 12, 0),
            Avklaringtype.AVSLAG);
    }

    private static EtterlysningData etterlysning(EtterlysningStatus status, boolean harUttalelse) {
        var uttalelse = status == EtterlysningStatus.MOTTATT_SVAR
            ? new UttalelseData(harUttalelse, harUttalelse ? "uenig" : null, null)
            : null;
        return new EtterlysningData(status, LocalDateTime.of(2025, 1, 20, 12, 0), UUID.randomUUID(), PERIODE,
            LocalDateTime.of(2025, 1, 6, 12, 0), uttalelse);
    }
}
