package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bistandsvilkår;

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
class BistandAvklaringTjenesteTest {

    private static final LocalDate FOM = LocalDate.of(2024, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2024, 1, 31);

    @Inject
    private EntityManager entityManager;

    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private BistandAvklaringTjeneste tjeneste;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        vilkårsavklaringGrunnlagRepository = new VilkårsavklaringGrunnlagRepository(entityManager);

        tjeneste = new BistandAvklaringTjeneste(
            vilkårsavklaringGrunnlagRepository,
            null
        );

        behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad().lagre(entityManager);
    }

    @Test
    void skal_lagre_foreslatt_avklaring_pa_bistandsvilkaret() {
        var innhold = lagAvklaring(FOM, TOM, true);

        long behandlingId = behandling.getId();
        var lagret = vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandlingId, VilkårType.BISTANDSVILKÅR, new HashSet<>(List.of(innhold)));

        assertThat(lagret).hasSize(1);
        assertThat(tjeneste.hentForeslåtteAvklaringerSomInnhold(behandling.getId()).keySet())
            .extracting(VilkårsvarselInnhold::ikkeOppfyltÅrsak).map(IkkeOppfyltDetaljertÅrsak::getKode)
            .containsExactly(BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK.getKode());

        // Skal ikke berøre andre vilkårstyper
        assertThat(vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId(), VilkårType.BOSTEDSVILKÅR)).isEmpty();
    }

    @Test
    void skal_hente_seneste_avklaring_for_behandling() {
        var avklaring = lagreAvklaring(lagAvklaring(FOM, TOM, true));

        var senesteAvklaring = tjeneste.hentSenesteAvklaringForBehandling(behandling.getId()).orElseThrow();

        assertThat(senesteAvklaring.avklaringtype()).isEqualTo(Avklaringtype.AVSLAG);
        assertThat(senesteAvklaring.periode()).isEqualTo(avklaring.getPeriode());
    }

    @Test
    void skal_ferdigstille_foreslatte_avklaringer() {
        lagreAvklaring(lagAvklaring(FOM, TOM, true));

        tjeneste.ferdigstillForeslåtteAvklaringer(behandling.getId());

        var grunnlag = vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId(), VilkårType.BISTANDSVILKÅR).orElseThrow();
        // Foreslåtte avklaringer beholdes urørt etter ferdigstilling (jf. fase 0), men speiles inn i holderen
        assertThat(grunnlag.getForeslåtteAvklaringer()).hasSize(1);
        assertThat(grunnlag.getFerdigstilteAvklaringer())
            .extracting(VilkårPeriodeAvklaring::getPeriode)
            .containsExactly(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM));
    }

    @Test
    void skal_gjenbruke_referanse_naar_kun_begrunnelsen_er_endret() {
        tjeneste.lagreForeslåtteAvklaringer(behandling.getId(), Set.of(lagBistandAvklaring("begrunnelse")));
        var opprinneligReferanse = hentReferanser().getFirst();

        tjeneste.lagreForeslåtteAvklaringer(behandling.getId(), Set.of(lagBistandAvklaring("ny begrunnelse")));

        assertThat(hentReferanser())
            .as("begrunnelsen vises ikke for bruker, så referansen etterlysningen peker på skal overleve")
            .containsExactly(opprinneligReferanse);
    }

    @Test
    void skal_gi_ny_referanse_naar_varselet_er_endret() {
        tjeneste.lagreForeslåtteAvklaringer(behandling.getId(), Set.of(lagBistandAvklaring("begrunnelse")));
        var opprinneligReferanse = hentReferanser().getFirst();

        var medEndretPeriode = new BistandAvklaring(
            new BistandVarselInnhold(new Periode(FOM, TOM.minusDays(1)), BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK, true, null,
                BistandsavklaringKildeType.BRUKER, null, Avklaringtype.AVSLAG),
            "begrunnelse", null, "A12345", LocalDateTime.now());
        tjeneste.lagreForeslåtteAvklaringer(behandling.getId(), Set.of(medEndretPeriode));

        assertThat(hentReferanser())
            .as("endret periode endrer varselet, og brukeren må varsles på nytt")
            .doesNotContain(opprinneligReferanse);
    }

    private List<UUID> hentReferanser() {
        return vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId(), VilkårType.BISTANDSVILKÅR)
            .orElseThrow()
            .getForeslåtteAvklaringer()
            .stream()
            .map(VilkårPeriodeAvklaring::getReferanse)
            .toList();
    }

    private BistandAvklaring lagBistandAvklaring(String begrunnelse) {
        var innhold = new BistandVarselInnhold(new Periode(FOM, TOM), BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK, true, null,
            BistandsavklaringKildeType.BRUKER, null, Avklaringtype.AVSLAG);
        return new BistandAvklaring(innhold, begrunnelse, null, "A12345", LocalDateTime.now());
    }

    private VilkårPeriodeAvklaring lagreAvklaring(VilkårPeriodeAvklaringForeslått avklaring) {
        var lagret = vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), VilkårType.BISTANDSVILKÅR, Set.of(avklaring));
        return lagret.stream()
            .filter(a -> a.getPeriode().equals(avklaring.getPeriode()) && a.skalSendeVarsel() == avklaring.skalSendeVarsel())
            .findFirst()
            .orElseThrow();
    }

    private VilkårPeriodeAvklaringForeslått lagAvklaring(LocalDate fom, LocalDate tom, boolean skalSendeVarsel) {
        return new VilkårPeriodeAvklaringForeslått(
            UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom),
            BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK.getKode(),
            "begrunnelse",
            skalSendeVarsel,
            null,
            skalSendeVarsel ? null : "begrunnelse for at det ikke varsles",
            BistandsavklaringKildeType.BRUKER,
            null,
            "A12345",
            LocalDateTime.now(),
            Avklaringtype.AVSLAG
        );
    }
}
