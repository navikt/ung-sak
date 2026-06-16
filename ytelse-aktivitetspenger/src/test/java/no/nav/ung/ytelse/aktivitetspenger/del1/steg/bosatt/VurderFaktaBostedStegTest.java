package no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.bosatt.Kilde;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.Startdatoer;
import no.nav.ung.sak.behandlingslager.behandling.startdato.SøktStartdato;
import no.nav.ung.sak.behandlingslager.behandling.søknadsperiode.AktivitetspengerSøktPeriode;
import no.nav.ung.sak.behandlingslager.behandling.søknadsperiode.AktivitetspengerSøktPeriodeRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class VurderFaktaBostedStegTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 1, 31);

    @Inject
    private EntityManager entityManager;

    @Inject
    private @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;

    @Inject
    private @Any Instance<ProsessTriggerPeriodeUtleder> prosessTriggerPeriodeUtledere;

    private BehandlingRepository behandlingRepository;
    private ProsessTriggereRepository prosessTriggereRepository;
    private BostedsGrunnlagRepository bostedsGrunnlagRepository;
    private StartdatoRepository startdatoRepository;
    private AktivitetspengerSøktPeriodeRepository aktivitetspengerSøktPeriodeRepository;
    private VurderFaktaBostedSteg steg;

    @BeforeEach
    void setUp() {
        behandlingRepository = new BehandlingRepository(entityManager);
        prosessTriggereRepository = new ProsessTriggereRepository(entityManager);
        bostedsGrunnlagRepository = new BostedsGrunnlagRepository(entityManager);
        startdatoRepository = new StartdatoRepository(entityManager);
        aktivitetspengerSøktPeriodeRepository = new AktivitetspengerSøktPeriodeRepository(entityManager);

        steg = new VurderFaktaBostedSteg(
            behandlingRepository,
            prosessTriggerPeriodeUtledere
        );
    }

    @Test
    void skal_opprette_aksjonspunkt_nar_det_finnes_trigger_med_endret_bosted() {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad().lagre(entityManager);
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(
            new Trigger(BehandlingÅrsakType.ENDRET_BOSTED, DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM))));

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe())
            .containsExactly(AksjonspunktDefinisjon.VURDER_FAKTA_OM_BOSTED);
    }

    @Test
    void skal_utfores_uten_aksjonspunkt_nar_det_ikke_finnes_trigger_med_endret_bosted() {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad().lagre(entityManager);

        var resultat = utførSteg(behandling);

        assertThat(resultat.getAksjonspunktListe()).isEmpty();
    }

    @Test
    void skal_lagre_fakta_fra_soknad_for_matchende_vilkarsperiode() {
        var behandling = AktivitetspengerTestScenarioBuilder.builderMedSøknad().lagre(entityManager);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM);
        var søktStartdato = new SøktStartdato(FOM, new JournalpostId("jp-1"));

        startdatoRepository.lagre(behandling.getId(), java.util.List.of(søktStartdato));
        startdatoRepository.lagreRelevanteSøknader(behandling.getId(), new Startdatoer(java.util.List.of(søktStartdato)));
        aktivitetspengerSøktPeriodeRepository.lagreNyPeriode(new AktivitetspengerSøktPeriode(
            behandling.getId(),
            new JournalpostId("jp-1"),
            LocalDateTime.now(),
            periode));
        bostedsGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "jp-1", new Periode(FOM, TOM), true);
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(
            new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, periode)));

        utførSteg(behandling);

        var lagretGrunnlag = bostedsGrunnlagRepository.hentGrunnlagHvisEksisterer(behandling.getId()).orElseThrow();
        var periodeAvklaring = lagretGrunnlag.hentOppgittOgForeslåttFaktaSomTidslinje().stream().findFirst().orElseThrow();
        assertThat(periodeAvklaring.getValue().isErBosattITrondheim()).isTrue();
        assertThat(periodeAvklaring.getValue().getKilde()).isEqualTo(Kilde.SØKNAD);
    }

    private BehandleStegResultat utførSteg(Behandling behandling) {
        var kontekst = new BehandlingskontrollKontekst(
            behandling.getFagsakId(),
            behandling.getAktørId(),
            behandlingRepository.taSkriveLås(behandling.getId()));
        return steg.utførSteg(kontekst);
    }
}

