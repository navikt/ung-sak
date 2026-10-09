package no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.bosatt;

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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class BostedAvklaringTjenesteTest {

    private static final LocalDate FOM = LocalDate.of(2024, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2024, 1, 31);

    @Inject
    private EntityManager entityManager;

    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    private BostedAvklaringTjeneste tjeneste;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        vilkårsavklaringGrunnlagRepository = new VilkårsavklaringGrunnlagRepository(entityManager);

        tjeneste = new BostedAvklaringTjeneste(
            vilkårsavklaringGrunnlagRepository,
            null,
            null
        );

        behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad().lagre(entityManager);
    }

    @Test
    void skal_lagre_foreslatt_avklaring_pa_bostedsvilkaret() {
        tjeneste.lagreForeslåtteAvklaringer(behandling.getId(), Set.of(lagBostedAvklaring("begrunnelse")));

        assertThat(tjeneste.hentForeslåtteAvklaringerSomInnhold(behandling.getId()).keySet())
            .extracting(VilkårsvarselInnhold::ikkeOppfyltÅrsak).map(IkkeOppfyltDetaljertÅrsak::getKode)
            .containsExactly(BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM.getKode());

        assertThat(vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId(), VilkårType.BISTANDSVILKÅR)).isEmpty();
    }

    @Test
    void skal_hente_seneste_avklaring_for_behandling() {
        var avklaring = lagreAvklaring(lagAvklaring(FOM, TOM, true));

        var senesteAvklaring = tjeneste.hentSenesteForeslåtteAvklaringForBehandling(behandling.getId()).orElseThrow();

        assertThat(senesteAvklaring.avklaringtype()).isEqualTo(Avklaringtype.AVSLAG);
        assertThat(senesteAvklaring.periode()).isEqualTo(avklaring.getPeriode());
        assertThat(senesteAvklaring.kilde()).isEqualTo(BostedsavklaringKildeType.BRUKER);
    }

    @Test
    void skal_ferdigstille_foreslatte_avklaringer() {
        lagreAvklaring(lagAvklaring(FOM, TOM, true));

        tjeneste.ferdigstillForeslåtteAvklaringer(behandling.getId());

        var grunnlag = vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId(), VilkårType.BOSTEDSVILKÅR).orElseThrow();
        // Foreslåtte avklaringer beholdes urørt etter ferdigstilling, men speiles inn i holderen
        assertThat(grunnlag.getForeslåtteAvklaringer()).hasSize(1);
        assertThat(grunnlag.getFerdigstilteAvklaringer())
            .extracting(VilkårPeriodeAvklaring::getPeriode)
            .containsExactly(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM));
    }

    @Test
    void skal_gjenbruke_referanse_naar_kun_begrunnelsen_er_endret() {
        tjeneste.lagreForeslåtteAvklaringer(behandling.getId(), Set.of(lagBostedAvklaring("begrunnelse")));
        var opprinneligReferanse = hentReferanser().getFirst();

        tjeneste.lagreForeslåtteAvklaringer(behandling.getId(), Set.of(lagBostedAvklaring("ny begrunnelse")));

        assertThat(hentReferanser())
            .as("begrunnelsen vises ikke for bruker, så referansen etterlysningen peker på skal overleve")
            .containsExactly(opprinneligReferanse);
    }

    @Test
    void skal_gi_ny_referanse_naar_varselet_er_endret() {
        tjeneste.lagreForeslåtteAvklaringer(behandling.getId(), Set.of(lagBostedAvklaring("begrunnelse")));
        var opprinneligReferanse = hentReferanser().getFirst();

        var medEndretÅrsak = new BostedAvklaring(
            new BostedVarselInnhold(new Periode(FOM, TOM), BostedsvilkårIkkeOppfyltÅrsak.STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM, true, null,
                BostedsavklaringKildeType.BRUKER, null, Avklaringtype.AVSLAG),
            "begrunnelse", null, "A12345", LocalDateTime.now());
        tjeneste.lagreForeslåtteAvklaringer(behandling.getId(), Set.of(medEndretÅrsak));

        assertThat(hentReferanser())
            .as("endret årsak endrer varselet, og brukeren må varsles på nytt")
            .doesNotContain(opprinneligReferanse);
    }

    private List<UUID> hentReferanser() {
        return vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId(), VilkårType.BOSTEDSVILKÅR)
            .orElseThrow()
            .getForeslåtteAvklaringer()
            .stream()
            .map(VilkårPeriodeAvklaring::getReferanse)
            .toList();
    }

    private BostedAvklaring lagBostedAvklaring(String begrunnelse) {
        var innhold = new BostedVarselInnhold(new Periode(FOM, TOM), BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, true, null,
            BostedsavklaringKildeType.BRUKER, null, Avklaringtype.AVSLAG);
        return new BostedAvklaring(innhold, begrunnelse, null, "A12345", LocalDateTime.now());
    }

    private VilkårPeriodeAvklaring lagreAvklaring(VilkårPeriodeAvklaringForeslått avklaring) {
        var lagret = vilkårsavklaringGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), VilkårType.BOSTEDSVILKÅR, Set.of(avklaring));
        return lagret.stream()
            .filter(a -> a.getPeriode().equals(avklaring.getPeriode()) && a.skalSendeVarsel() == avklaring.skalSendeVarsel())
            .findFirst()
            .orElseThrow();
    }

    private VilkårPeriodeAvklaringForeslått lagAvklaring(LocalDate fom, LocalDate tom, boolean skalSendeVarsel) {
        return new VilkårPeriodeAvklaringForeslått(
            UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom),
            BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM.getKode(),
            "begrunnelse",
            skalSendeVarsel,
            null,
            skalSendeVarsel ? null : "begrunnelse for at det ikke varsles",
            BostedsavklaringKildeType.BRUKER,
            null,
            "A12345",
            LocalDateTime.now(),
            Avklaringtype.AVSLAG
        );
    }
}
