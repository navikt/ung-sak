package no.nav.ung.sak.etterlysning;

import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaringForeslått;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VilkårsavklaringUtfallTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 1, 31);
    private static final DatoIntervallEntitet PERIODE = DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM);

    @Test
    void ingen_avklaring_gir_ingen_avklaring_uansett_etterlysning() {
        var resultat = VilkårsavklaringUtfall.utled(null, null, VilkårType.BISTANDSVILKÅR);

        assertThat(resultat).isEqualTo(VilkårsavklaringUtfall.INGEN_AVKLARING);
    }

    @Test
    void venter_pa_uttalelse_nar_etterlysning_er_opprettet_selv_med_avklaring() {
        var avklaring = avklaring(BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK, true);
        var etterlysning = etterlysning(EtterlysningStatus.OPPRETTET, null);

        var resultat = VilkårsavklaringUtfall.utled(etterlysning, avklaring, VilkårType.BISTANDSVILKÅR);

        assertThat(resultat).isEqualTo(VilkårsavklaringUtfall.VENTER_PÅ_UTTALELSE_FRA_BRUKER);
    }

    @Test
    void venter_pa_uttalelse_nar_etterlysning_venter() {
        var avklaring = avklaring(BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK, true);
        var etterlysning = etterlysning(EtterlysningStatus.VENTER, null);

        var resultat = VilkårsavklaringUtfall.utled(etterlysning, avklaring, VilkårType.BISTANDSVILKÅR);

        assertThat(resultat).isEqualTo(VilkårsavklaringUtfall.VENTER_PÅ_UTTALELSE_FRA_BRUKER);
    }

    @Test
    void avslas_automatisk_nar_arsak_gir_avslagsarsak_uten_fritekst_og_ingen_uttalelse() {
        var avklaring = avklaring(BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK, true);
        var etterlysning = etterlysning(EtterlysningStatus.MOTTATT_SVAR, new UttalelseData(false, null, null));

        var resultat = VilkårsavklaringUtfall.utled(etterlysning, avklaring, VilkårType.BISTANDSVILKÅR);

        assertThat(resultat).isEqualTo(VilkårsavklaringUtfall.AVSLÅS_AUTOMATISK);
    }

    @Test
    void vurderes_manuelt_nar_bruker_har_avgitt_uttalelse() {
        var avklaring = avklaring(BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK, true);
        var etterlysning = etterlysning(EtterlysningStatus.MOTTATT_SVAR, new UttalelseData(true, "uenig", null));

        var resultat = VilkårsavklaringUtfall.utled(etterlysning, avklaring, VilkårType.BISTANDSVILKÅR);

        assertThat(resultat).isEqualTo(VilkårsavklaringUtfall.VILKÅR_VURDERES_MANUELT);
    }

    @Test
    void vurderes_manuelt_nar_det_ikke_skal_varsles_selv_uten_etterlysning() {
        var avklaring = avklaring(BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK, false);

        var resultat = VilkårsavklaringUtfall.utled(null, avklaring, VilkårType.BISTANDSVILKÅR);

        assertThat(resultat).isEqualTo(VilkårsavklaringUtfall.VILKÅR_VURDERES_MANUELT);
    }

    @Test
    void vurderes_manuelt_nar_arsak_krever_fritekst() {
        var avklaring = avklaring(BistandsvilkårIkkeOppfyltÅrsak.ANNET, true, "fritekst til varsel");

        var resultat = VilkårsavklaringUtfall.utled(null, avklaring, VilkårType.BISTANDSVILKÅR);

        assertThat(resultat).isEqualTo(VilkårsavklaringUtfall.VILKÅR_VURDERES_MANUELT);
    }

    @Test
    void vurderes_manuelt_nar_arsak_ikke_er_en_gyldig_avklaringsarsak() {
        var avklaring = avklaring(BistandsvilkårIkkeOppfyltÅrsak.AVKORTET, true);

        var resultat = VilkårsavklaringUtfall.utled(null, avklaring, VilkårType.BISTANDSVILKÅR);

        assertThat(resultat)
            .as("automatisk avslag skal aldri være nåbart for en ugyldig avklaringsårsak, selv om den skulle finnes lagret")
            .isEqualTo(VilkårsavklaringUtfall.VILKÅR_VURDERES_MANUELT);
    }

    private static VilkårPeriodeAvklaringForeslått avklaring(BistandsvilkårIkkeOppfyltÅrsak årsak, boolean skalSendeVarsel) {
        return avklaring(årsak, skalSendeVarsel, null);
    }

    private static VilkårPeriodeAvklaringForeslått avklaring(BistandsvilkårIkkeOppfyltÅrsak årsak, boolean skalSendeVarsel, String fritekstTilVarsel) {
        return new VilkårPeriodeAvklaringForeslått(
            PERIODE,
            årsak.getKode(),
            "begrunnelse",
            skalSendeVarsel,
            fritekstTilVarsel,
            skalSendeVarsel ? null : "begrunnelse for at det ikke varsles",
            "A12345",
            LocalDateTime.now(),
            Avklaringtype.AVSLAG
        );
    }

    private static EtterlysningData etterlysning(EtterlysningStatus status, UttalelseData uttalelseData) {
        return new EtterlysningData(status, LocalDateTime.now(), UUID.randomUUID(), PERIODE, LocalDateTime.now(), uttalelseData);
    }
}
