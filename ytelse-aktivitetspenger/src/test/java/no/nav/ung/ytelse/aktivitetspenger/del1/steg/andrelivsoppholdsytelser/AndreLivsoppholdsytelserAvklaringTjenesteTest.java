package no.nav.ung.ytelse.aktivitetspenger.del1.steg.andrelivsoppholdsytelser;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.vilkår.*;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaringForeslått;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.VilkårsvarselInnhold;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class AndreLivsoppholdsytelserAvklaringTjenesteTest {

    private static final LocalDate FOM = LocalDate.of(2024, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2024, 1, 31);
    private static final AndreLivsoppholdsytelserIkkeOppfyltÅrsak ÅRSAK = AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_DAGPENGER;

    @Inject
    private EntityManager entityManager;

    @Inject
    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private AndreLivsoppholdsytelserAvklaringTjeneste tjeneste;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        tjeneste = new AndreLivsoppholdsytelserAvklaringTjeneste(
            vilkårsavklaringGrunnlagRepository,
            null,
            null
        );

        behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad().lagre(entityManager);
    }

    @Test
    void skal_lagre_foreslatt_avklaring_pa_livsoppholdsvilkaret() {
        var innhold = lagAvklaring(FOM, TOM, true);

        long behandlingId = behandling.getId();
        var lagret = vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandlingId, VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR, new HashSet<>(List.of(innhold)));

        assertThat(lagret).hasSize(1);
        assertThat(tjeneste.hentForeslåtteAvklaringerSomInnhold(behandlingId).keySet())
            .extracting(VilkårsvarselInnhold::ikkeOppfyltÅrsak).map(IkkeOppfyltDetaljertÅrsak::getKode)
            .containsExactly(ÅRSAK.getKode());

        // Skal ikke berøre andre vilkårstyper
        assertThat(vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandlingId, VilkårType.BISTANDSVILKÅR)).isEmpty();
    }

    @Test
    void skal_hente_seneste_avklaring_med_kilde() {
        var avklaring = lagreAvklaring(lagAvklaring(FOM, TOM, true));

        var senesteAvklaring = tjeneste.hentSenesteForeslåtteAvklaringForBehandling(behandling.getId()).orElseThrow();

        assertThat(senesteAvklaring.avklaringtype()).isEqualTo(Avklaringtype.AVSLAG);
        assertThat(senesteAvklaring.periode()).isEqualTo(avklaring.getPeriode());
        assertThat(senesteAvklaring.kilde()).isEqualTo(AndreLivsoppholdsytelserAvklaringKildeType.NAV);
        assertThat(senesteAvklaring.kildeFritekst()).isNull();
    }

    @Test
    void skal_ferdigstille_foreslatte_avklaringer() {
        lagreAvklaring(lagAvklaring(FOM, TOM, true));

        tjeneste.ferdigstillForeslåtteAvklaringer(behandling.getId());

        var grunnlag = vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId(), VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR).orElseThrow();
        // Foreslåtte avklaringer beholdes urørt etter ferdigstilling (jf. fase 0), men speiles inn i holderen
        assertThat(grunnlag.getForeslåtteAvklaringer()).hasSize(1);
        assertThat(grunnlag.getFerdigstilteAvklaringer())
            .extracting(VilkårPeriodeAvklaring::getPeriode)
            .containsExactly(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM));
    }

    @Test
    void skal_gjenbruke_referanse_naar_kun_begrunnelsen_er_endret() {
        tjeneste.lagreForeslåtteAvklaringer(behandling.getId(), Set.of(lagDomeneAvklaring("begrunnelse")));
        var opprinneligReferanse = hentReferanser().getFirst();

        tjeneste.lagreForeslåtteAvklaringer(behandling.getId(), Set.of(lagDomeneAvklaring("ny begrunnelse")));

        assertThat(hentReferanser())
            .as("begrunnelsen vises ikke for bruker, så referansen etterlysningen peker på skal overleve")
            .containsExactly(opprinneligReferanse);
    }

    @Test
    void skal_gi_ny_referanse_naar_varselet_er_endret() {
        tjeneste.lagreForeslåtteAvklaringer(behandling.getId(), Set.of(lagDomeneAvklaring("begrunnelse")));
        var opprinneligReferanse = hentReferanser().getFirst();

        var medEndretÅrsak = new AndreLivsoppholdsytelserAvklaring(
            new AndreLivsoppholdsytelserVarselInnhold(new Periode(FOM, TOM), AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_UFØRETRYGD, true, null,
                AndreLivsoppholdsytelserAvklaringKildeType.NAV, null, Avklaringtype.AVSLAG),
            "begrunnelse", null, "A12345", LocalDateTime.now());
        tjeneste.lagreForeslåtteAvklaringer(behandling.getId(), Set.of(medEndretÅrsak));

        assertThat(hentReferanser())
            .as("en annen ytelse i varselet betyr at brukeren må varsles på nytt")
            .doesNotContain(opprinneligReferanse);
    }

    private List<UUID> hentReferanser() {
        return vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId(), VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR)
            .orElseThrow()
            .getForeslåtteAvklaringer()
            .stream()
            .map(VilkårPeriodeAvklaring::getReferanse)
            .toList();
    }

    private AndreLivsoppholdsytelserAvklaring lagDomeneAvklaring(String begrunnelse) {
        var innhold = new AndreLivsoppholdsytelserVarselInnhold(new Periode(FOM, TOM), ÅRSAK, true, null,
            AndreLivsoppholdsytelserAvklaringKildeType.NAV, null, Avklaringtype.AVSLAG);
        return new AndreLivsoppholdsytelserAvklaring(innhold, begrunnelse, null, "A12345", LocalDateTime.now());
    }

    private VilkårPeriodeAvklaring lagreAvklaring(VilkårPeriodeAvklaringForeslått avklaring) {
        var lagret = vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR, Set.of(avklaring));
        return lagret.stream()
            .filter(a -> a.getPeriode().equals(avklaring.getPeriode()) && a.skalSendeVarsel() == avklaring.skalSendeVarsel())
            .findFirst()
            .orElseThrow();
    }

    private VilkårPeriodeAvklaringForeslått lagAvklaring(LocalDate fom, LocalDate tom, boolean skalSendeVarsel) {
        return new VilkårPeriodeAvklaringForeslått(
            UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom),
            ÅRSAK.getKode(),
            "begrunnelse",
            skalSendeVarsel,
            null,
            skalSendeVarsel ? null : "begrunnelse for at det ikke varsles",
            AndreLivsoppholdsytelserAvklaringKildeType.NAV,
            null,
            "A12345",
            LocalDateTime.now(),
            Avklaringtype.AVSLAG
        );
    }
}
