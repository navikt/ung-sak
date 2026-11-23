package no.nav.ung.ytelse.aktivitetspenger.navkontor.steg;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.vilkår.AktivitetsavklaringKildeType;
import no.nav.ung.kodeverk.vilkår.AktivitetsvilkåretIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserAvklaringKildeType;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.AvklaringKilde;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.BistandsavklaringKildeType;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.BostedsavklaringKildeType;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.IkkeOppfyltDetaljertÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsvarselInnhold;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.inngangsvilkår.avklaring.ForeslåttVilkårsavklaring;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringTjeneste;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsvarselInnholdMapper;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.aktivitetsvilkår.AktivitetAvklaring;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.aktivitetsvilkår.AktivitetAvklaringDataMapper;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.aktivitetsvilkår.AktivitetVarselInnhold;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.andrelivsoppholdsytelser.AndreLivsoppholdsytelserAvklaring;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.andrelivsoppholdsytelser.AndreLivsoppholdsytelserAvklaringDataMapper;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.andrelivsoppholdsytelser.AndreLivsoppholdsytelserVarselInnhold;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.bistandsvilkår.BistandAvklaring;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.bistandsvilkår.BistandAvklaringDataMapper;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.bistandsvilkår.BistandVarselInnhold;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.bosatt.BostedAvklaring;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.bosatt.BostedVarselInnhold;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.bosatt.BostedsAvklaringDataMapper;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class VilkårsavklaringTjenesteTest {

    private static final LocalDate FOM = LocalDate.of(2024, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2024, 1, 31);

    @Inject
    private EntityManager entityManager;

    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private VilkårsavklaringTjeneste tjeneste;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        vilkårsavklaringGrunnlagRepository = new VilkårsavklaringGrunnlagRepository(entityManager);
        tjeneste = new VilkårsavklaringTjeneste(vilkårsavklaringGrunnlagRepository);
        behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad().lagre(entityManager);
    }

    @ParameterizedTest
    @EnumSource(Vilkår.class)
    void skal_lagre_foreslatt_avklaring_kun_paa_angitt_vilkaar(Vilkår vilkår) {
        tjeneste.lagreForeslåtteAvklaringer(behandling.getId(), vilkår.type, Set.of(lagAvklaring(vilkår, new Periode(FOM, TOM), "begrunnelse")), vilkår.mapper);

        assertThat(tjeneste.hentForeslåtteAvklaringerSomInnhold(behandling.getId(), vilkår.type, vilkår.mapper).keySet())
            .extracting(VilkårsvarselInnhold::ikkeOppfyltÅrsak)
            .containsExactly(vilkår.årsak);
        for (var annet : Vilkår.values()) {
            if (annet != vilkår) {
                assertThat(vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId(), annet.type)).isEmpty();
            }
        }
    }

    @ParameterizedTest
    @EnumSource(Vilkår.class)
    void skal_hente_seneste_avklaring_for_behandling(Vilkår vilkår) {
        tjeneste.lagreForeslåtteAvklaringer(behandling.getId(), vilkår.type, Set.of(lagAvklaring(vilkår, new Periode(FOM, TOM), "begrunnelse")), vilkår.mapper);

        var senesteAvklaring = tjeneste.hentSenesteForeslåtteAvklaringForBehandling(behandling.getId(), vilkår.type).orElseThrow();

        assertThat(senesteAvklaring.avklaringtype()).isEqualTo(Avklaringtype.AVSLAG);
        assertThat(senesteAvklaring.periode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM));
        assertThat(senesteAvklaring.kilde()).isEqualTo(vilkår.kilde);
        assertThat(senesteAvklaring.kildeFritekst()).isNull();
    }

    @Test
    void skal_ferdigstille_foreslatte_avklaringer_for_alle_vilkaar() {
        for (var vilkår : Vilkår.values()) {
            tjeneste.lagreForeslåtteAvklaringer(behandling.getId(), vilkår.type, Set.of(lagAvklaring(vilkår, new Periode(FOM, TOM), "begrunnelse")), vilkår.mapper);
        }

        tjeneste.ferdigstillForeslåtteAvklaringer(behandling.getId());

        for (var vilkår : Vilkår.values()) {
            var grunnlag = vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId(), vilkår.type).orElseThrow();
            assertThat(grunnlag.getForeslåtteAvklaringer()).hasSize(1);
            assertThat(grunnlag.getFerdigstilteAvklaringer())
                .extracting(VilkårPeriodeAvklaring::getPeriode)
                .containsExactly(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM));
        }
    }

    @ParameterizedTest
    @EnumSource(Vilkår.class)
    void skal_gjenbruke_referanse_naar_kun_begrunnelsen_er_endret(Vilkår vilkår) {
        tjeneste.lagreForeslåtteAvklaringer(behandling.getId(), vilkår.type, Set.of(lagAvklaring(vilkår, new Periode(FOM, TOM), "begrunnelse")), vilkår.mapper);
        var opprinneligReferanse = hentReferanser(vilkår).getFirst();

        tjeneste.lagreForeslåtteAvklaringer(behandling.getId(), vilkår.type, Set.of(lagAvklaring(vilkår, new Periode(FOM, TOM), "ny begrunnelse")), vilkår.mapper);

        assertThat(hentReferanser(vilkår))
            .as("begrunnelsen vises ikke for bruker, så referansen etterlysningen peker på skal overleve")
            .containsExactly(opprinneligReferanse);
    }

    @ParameterizedTest
    @EnumSource(Vilkår.class)
    void skal_gi_ny_referanse_naar_varselet_er_endret(Vilkår vilkår) {
        tjeneste.lagreForeslåtteAvklaringer(behandling.getId(), vilkår.type, Set.of(lagAvklaring(vilkår, new Periode(FOM, TOM), "begrunnelse")), vilkår.mapper);
        var opprinneligReferanse = hentReferanser(vilkår).getFirst();

        tjeneste.lagreForeslåtteAvklaringer(behandling.getId(), vilkår.type, Set.of(lagAvklaring(vilkår, new Periode(FOM, TOM.minusDays(1)), "begrunnelse")), vilkår.mapper);

        assertThat(hentReferanser(vilkår))
            .as("endret periode endrer varselet, og brukeren må varsles på nytt")
            .doesNotContain(opprinneligReferanse);
    }

    @Test
    void skal_feile_for_vilkaar_uten_avklaring() {
        assertThatThrownBy(() -> tjeneste.hentForeslåtteAvklaringerSomInnhold(behandling.getId(), VilkårType.ALDERSVILKÅR, Vilkår.BOSTED.mapper))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private List<UUID> hentReferanser(Vilkår vilkår) {
        return vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId(), vilkår.type)
            .orElseThrow()
            .getForeslåtteAvklaringer()
            .stream()
            .map(VilkårPeriodeAvklaring::getReferanse)
            .toList();
    }

    private static ForeslåttVilkårsavklaring lagAvklaring(Vilkår vilkår, Periode periode, String begrunnelse) {
        var vurdertTidspunkt = LocalDateTime.now();
        return switch (vilkår) {
            case AKTIVITET -> new AktivitetAvklaring(new AktivitetVarselInnhold(periode, AktivitetsvilkåretIkkeOppfyltÅrsak.ANNET, true, null,
                AktivitetsavklaringKildeType.BRUKER, null, Avklaringtype.AVSLAG), begrunnelse, null, "A12345", vurdertTidspunkt);
            case BOSTED -> new BostedAvklaring(new BostedVarselInnhold(periode, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, true, null,
                BostedsavklaringKildeType.BRUKER, null, Avklaringtype.AVSLAG), begrunnelse, null, "A12345", vurdertTidspunkt);
            case BISTAND -> new BistandAvklaring(new BistandVarselInnhold(periode, BistandsvilkårIkkeOppfyltÅrsak.KOMMET_I_ARBEID, true, null,
                BistandsavklaringKildeType.BRUKER, null, Avklaringtype.AVSLAG), begrunnelse, null, "A12345", vurdertTidspunkt);
            case ANDRE_LIVSOPPHOLDSYTELSER -> new AndreLivsoppholdsytelserAvklaring(new AndreLivsoppholdsytelserVarselInnhold(periode,
                AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_UFØRETRYGD, true, null,
                AndreLivsoppholdsytelserAvklaringKildeType.NAV, null, Avklaringtype.AVSLAG), begrunnelse, null, "A12345", vurdertTidspunkt);
        };
    }

    enum Vilkår {
        AKTIVITET(VilkårType.AKTIVITETSVILKÅR, AktivitetsvilkåretIkkeOppfyltÅrsak.ANNET, AktivitetsavklaringKildeType.BRUKER, AktivitetAvklaringDataMapper::mapTilAktivitetAvklaringInnhold),
        BOSTED(VilkårType.BOSTEDSVILKÅR, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, BostedsavklaringKildeType.BRUKER, BostedsAvklaringDataMapper::mapTilBostedAvklaringInnhold),
        BISTAND(VilkårType.BISTANDSVILKÅR, BistandsvilkårIkkeOppfyltÅrsak.KOMMET_I_ARBEID, BistandsavklaringKildeType.BRUKER, BistandAvklaringDataMapper::mapTilBistandAvklaringInnhold),
        ANDRE_LIVSOPPHOLDSYTELSER(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_UFØRETRYGD, AndreLivsoppholdsytelserAvklaringKildeType.NAV, AndreLivsoppholdsytelserAvklaringDataMapper::mapTilAvklaringInnhold);

        private final VilkårType type;
        private final IkkeOppfyltDetaljertÅrsak årsak;
        private final AvklaringKilde kilde;
        private final VilkårsvarselInnholdMapper mapper;

        Vilkår(VilkårType type, IkkeOppfyltDetaljertÅrsak årsak, AvklaringKilde kilde, VilkårsvarselInnholdMapper mapper) {
            this.type = type;
            this.årsak = årsak;
            this.kilde = kilde;
            this.mapper = mapper;
        }
    }
}
