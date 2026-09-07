package no.nav.ung.sak.etterlysning;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.kodeverk.vilkår.AvklaringKilde;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.BistandsavklaringKildeType;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.BostedsavklaringKildeType;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaringForeslått;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Verifiserer at to vilkår kan ha hver sin vilkårsavklaring — med hver sin etterlysning — på samme behandling,
 * uten å påvirke hverandre. Bruker BOSTEDSVILKÅR og BISTANDSVILKÅR som de to vilkårstypene.
 */
@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class VilkårsavklaringKombinasjonTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 1, 31);
    private static final DatoIntervallEntitet PERIODE = DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM);
    private static final BostedsvilkårIkkeOppfyltÅrsak BOSTED_ÅRSAK = BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM;
    private static final BistandsvilkårIkkeOppfyltÅrsak BISTAND_ÅRSAK = BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK;

    @Inject
    private EntityManager entityManager;

    @Inject
    private EtterlysningRepository etterlysningRepository;

    private VilkårsavklaringGrunnlagRepository grunnlagRepository;
    private VilkårsavklaringEtterlysningTjeneste etterlysningTjeneste;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        grunnlagRepository = new VilkårsavklaringGrunnlagRepository(entityManager);
        prosessTaskTjeneste = mock(ProsessTaskTjeneste.class);
        etterlysningTjeneste = new VilkårsavklaringEtterlysningTjeneste(etterlysningRepository, prosessTaskTjeneste);
        behandling = TestScenarioBuilder.builderMedSøknad().lagre(entityManager);
    }

    @Test
    void to_vilkarstyper_skal_ha_hvert_sitt_grunnlag_pa_samme_behandling() {
        var bosted = lagreAvklaring(VilkårType.BOSTEDSVILKÅR, BOSTED_ÅRSAK, BostedsavklaringKildeType.FOLKEREGISTER);
        var bistand = lagreAvklaring(VilkårType.BISTANDSVILKÅR, BISTAND_ÅRSAK, BistandsavklaringKildeType.BRUKER);

        assertThat(bosted.getReferanse()).isNotEqualTo(bistand.getReferanse());

        assertThat(hentForeslåtte(VilkårType.BOSTEDSVILKÅR))
            .extracting(VilkårPeriodeAvklaring::getIkkeOppfyltÅrsakKode)
            .containsExactly(BOSTED_ÅRSAK.getKode());
        assertThat(hentForeslåtte(VilkårType.BISTANDSVILKÅR))
            .extracting(VilkårPeriodeAvklaring::getIkkeOppfyltÅrsakKode)
            .containsExactly(BISTAND_ÅRSAK.getKode());
    }

    @Test
    void ferdigstilling_av_ett_vilkar_skal_ikke_pavirke_det_andre() {
        lagreAvklaring(VilkårType.BOSTEDSVILKÅR, BOSTED_ÅRSAK, BostedsavklaringKildeType.FOLKEREGISTER);
        lagreAvklaring(VilkårType.BISTANDSVILKÅR, BISTAND_ÅRSAK, BistandsavklaringKildeType.BRUKER);

        grunnlagRepository.ferdigstillForeslåtteAvklaringer(behandling.getId(), VilkårType.BISTANDSVILKÅR);

        var bistandGrunnlag = grunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId(), VilkårType.BISTANDSVILKÅR).orElseThrow();
        assertThat(bistandGrunnlag.getFerdigstilteAvklaringer())
            .extracting(VilkårPeriodeAvklaring::getIkkeOppfyltÅrsakKode)
            .containsExactly(BISTAND_ÅRSAK.getKode());

        var bostedGrunnlag = grunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId(), VilkårType.BOSTEDSVILKÅR).orElseThrow();
        assertThat(bostedGrunnlag.isAktiv()).isTrue();
        assertThat(bostedGrunnlag.getFerdigstilteAvklaringer())
            .as("bostedsavklaringen skal ikke ferdigstilles når bistandsavklaringen ferdigstilles")
            .isEmpty();
        assertThat(bostedGrunnlag.getForeslåtteAvklaringer())
            .extracting(VilkårPeriodeAvklaring::getIkkeOppfyltÅrsakKode)
            .containsExactly(BOSTED_ÅRSAK.getKode());
    }

    @Test
    void to_samtidige_etterlysninger_av_ulik_type_skal_leve_side_om_side() {
        var bosted = lagreAvklaring(VilkårType.BOSTEDSVILKÅR, BOSTED_ÅRSAK, BostedsavklaringKildeType.FOLKEREGISTER);
        var bistand = lagreAvklaring(VilkårType.BISTANDSVILKÅR, BISTAND_ÅRSAK, BistandsavklaringKildeType.BRUKER);

        etterlysningTjeneste.oppdaterEtterlysninger(behandling, EtterlysningType.UTTALELSE_BOSTED, Map.of(), TestVilkårsavklaringInnhold.tilMap(VilkårType.BOSTEDSVILKÅR, bosted));
        etterlysningTjeneste.oppdaterEtterlysninger(behandling, EtterlysningType.UTTALELSE_BISTAND, Map.of(), TestVilkårsavklaringInnhold.tilMap(VilkårType.BISTANDSVILKÅR, bistand));

        assertThat(etterlysningRepository.hentOpprettetEtterlysninger(behandling.getId(), EtterlysningType.UTTALELSE_BOSTED))
            .extracting(Etterlysning::getGrunnlagsreferanse)
            .containsExactly(bosted.getReferanse());
        assertThat(etterlysningRepository.hentOpprettetEtterlysninger(behandling.getId(), EtterlysningType.UTTALELSE_BISTAND))
            .extracting(Etterlysning::getGrunnlagsreferanse)
            .containsExactly(bistand.getReferanse());
    }

    @Test
    void endring_av_ett_vilkar_skal_kun_avbryte_egen_etterlysning() {
        var bosted = lagreAvklaring(VilkårType.BOSTEDSVILKÅR, BOSTED_ÅRSAK, BostedsavklaringKildeType.FOLKEREGISTER);
        var bistand = lagreAvklaring(VilkårType.BISTANDSVILKÅR, BISTAND_ÅRSAK, BistandsavklaringKildeType.BRUKER);

        etterlysningTjeneste.oppdaterEtterlysninger(behandling, EtterlysningType.UTTALELSE_BOSTED, Map.of(), TestVilkårsavklaringInnhold.tilMap(VilkårType.BOSTEDSVILKÅR, bosted));
        etterlysningTjeneste.oppdaterEtterlysninger(behandling, EtterlysningType.UTTALELSE_BISTAND, Map.of(), TestVilkårsavklaringInnhold.tilMap(VilkårType.BISTANDSVILKÅR, bistand));

        var endretBistand = lagreAvklaring(VilkårType.BISTANDSVILKÅR, BistandsvilkårIkkeOppfyltÅrsak.AVKORTET, BistandsavklaringKildeType.BRUKER);
        etterlysningTjeneste.oppdaterEtterlysninger(behandling, EtterlysningType.UTTALELSE_BISTAND, TestVilkårsavklaringInnhold.tilMap(VilkårType.BISTANDSVILKÅR, bistand), TestVilkårsavklaringInnhold.tilMap(VilkårType.BISTANDSVILKÅR, endretBistand));

        assertThat(etterlysningRepository.hentEtterlysningerSomSkalAvbrytes(behandling.getId()))
            .extracting(Etterlysning::getType)
            .containsExactly(EtterlysningType.UTTALELSE_BISTAND);

        assertThat(etterlysningRepository.hentOpprettetEtterlysninger(behandling.getId(), EtterlysningType.UTTALELSE_BOSTED))
            .as("bostedsetterlysningen skal være urørt")
            .extracting(Etterlysning::getStatus)
            .containsExactly(EtterlysningStatus.OPPRETTET);

        assertThat(etterlysningRepository.hentOpprettetEtterlysninger(behandling.getId(), EtterlysningType.UTTALELSE_BISTAND))
            .extracting(Etterlysning::getGrunnlagsreferanse)
            .containsExactly(endretBistand.getReferanse());
    }

    private List<VilkårPeriodeAvklaring> hentForeslåtte(VilkårType vilkårType) {
        return grunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId(), vilkårType)
            .map(g -> List.copyOf(g.getForeslåtteAvklaringer()))
            .orElse(List.of());
    }

    private VilkårPeriodeAvklaring lagreAvklaring(VilkårType vilkårType, Kodeverdi årsak, AvklaringKilde kilde) {
        var avklaring = new VilkårPeriodeAvklaringForeslått(
            PERIODE,
            årsak.getKode(),
            "begrunnelse",
            true,
            "fritekst til varsel",
            null,
            kilde,
            null,
            "A12345",
            LocalDateTime.now(),
            Avklaringtype.AVSLAG);
        return grunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), vilkårType, Set.of(avklaring))
            .stream()
            .findFirst()
            .orElseThrow();
    }
}
