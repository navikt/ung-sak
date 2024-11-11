package no.nav.ung.sak.behandlingslager.behandling.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.aktør.Personinfo;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class BehandlingRevurderingRepositoryImplTest {

    private static final LocalDateTime NOW = LocalDateTime.now();

    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider;
    private VilkårResultatRepository vilkårResultatRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingRevurderingRepository behandlingRevurderingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private FagsakRepository fagsakRepository;

    private Behandling behandling;

    @BeforeEach
    public void setup(){
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        vilkårResultatRepository = new VilkårResultatRepository(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        behandlingRevurderingRepository = repositoryProvider.getBehandlingRevurderingRepository();
        behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        fagsakRepository = repositoryProvider.getFagsakRepository();
    }

    @Test
    public void skal_finne_henlagte_behandlinger_etter_forrige_ferdigbehandlede_søknad() {

        behandling = opprettRevurderingsKandidat();

        Long fagsakId = behandling.getFagsakId();

        Behandling revurderingsBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING)).build();
        behandlingRepository.lagreOgClear(revurderingsBehandling, behandlingRepository.taSkriveLås(revurderingsBehandling));

        revurderingsBehandling = behandlingRepository.hentBehandling(revurderingsBehandling.getId());
        oppdaterMedBehandlingsresultatAvslagOgLagre(revurderingsBehandling);
        revurderingsBehandling.avsluttBehandling();
        behandlingRepository.lagreOgClear(revurderingsBehandling, behandlingRepository.taSkriveLås(revurderingsBehandling));
        revurderingsBehandling = behandlingRepository.hentBehandling(revurderingsBehandling.getId());

        Behandling nyRevurderingsBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)).build();
        behandlingRepository.lagreOgClear(nyRevurderingsBehandling, behandlingRepository.taSkriveLås(nyRevurderingsBehandling));

        nyRevurderingsBehandling = behandlingRepository.hentBehandling(nyRevurderingsBehandling.getId());
        oppdaterMedBehandlingsresultatAvslagOgLagre(nyRevurderingsBehandling);
        nyRevurderingsBehandling.avsluttBehandling();
        behandlingRepository.lagreOgClear(nyRevurderingsBehandling, behandlingRepository.taSkriveLås(nyRevurderingsBehandling));
        nyRevurderingsBehandling = behandlingRepository.hentBehandling(nyRevurderingsBehandling.getId());

        Long revurderingsBehandlingId = revurderingsBehandling.getId();
        List<Behandling> result = behandlingRevurderingRepository.finnHenlagteBehandlingerEtterSisteInnvilgedeIkkeHenlagteBehandling(fagsakId);
        assertThat(result).isNotEmpty();
        result.forEach(r -> assertThat(r.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.HENLAGT_FEILOPPRETTET));
        assertThat(result).anySatisfy(r -> r.getId().equals(revurderingsBehandlingId));
        assertThat(result).hasSize(2);
    }

    @Test
    public void skal_finne_alle_innvilgete_avsluttede_behandling_som_ikke_er_henlagt() {

        behandling = opprettRevurderingsKandidat();

        Long fagsakId = behandling.getFagsakId();

        @SuppressWarnings("unused")
        Behandling revurderingsBehandling = opprettOgLagreRevurderingMedBehandlingÅrsak();

        Behandling nyRevurderingsBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER)).build();
        behandlingRepository.lagreOgClear(nyRevurderingsBehandling, behandlingRepository.taSkriveLås(nyRevurderingsBehandling));

        nyRevurderingsBehandling = behandlingRepository.hentBehandling(nyRevurderingsBehandling.getId());
        oppdaterMedBehandlingsresultatAvslagOgLagre(nyRevurderingsBehandling);
        nyRevurderingsBehandling.avsluttBehandling();
        behandlingRepository.lagreOgClear(nyRevurderingsBehandling, behandlingRepository.taSkriveLås(nyRevurderingsBehandling));
        nyRevurderingsBehandling = behandlingRepository.hentBehandling(nyRevurderingsBehandling.getId());

        List<Behandling> result = behandlingRepository.finnAlleAvsluttedeIkkeHenlagteBehandlinger(fagsakId);
        assertThat(result).isNotEmpty();
        result.forEach(r -> assertThat(r.getBehandlingResultatType()).isEqualTo(BehandlingResultatType.INNVILGET));
        assertThat(result).anySatisfy(r -> r.getId().equals(behandling.getId()));
        assertThat(result).hasSize(1);
    }

    @Test
    public void skal_finne_nyeste_innvilgete_avsluttede_behandling_som_ikke_er_henlagt() {
        var behandling = opprettRevurderingsKandidat();
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        Behandling henlagtBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandlingRepository.lagre(henlagtBehandling, behandlingRepository.taSkriveLås(henlagtBehandling));

        henlagtBehandling.setBehandlingResultatType(BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET);
        henlagtBehandling.avsluttBehandling();
        behandlingRepository.lagre(henlagtBehandling, behandlingRepository.taSkriveLås(henlagtBehandling));

        Optional<Behandling> resultatOpt = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(behandling.getFagsak().getId());
        assertThat(resultatOpt).hasValueSatisfying(resultat -> assertThat(resultat.getId()).isEqualTo(behandling.getId()));
    }


    private Behandling opprettOgLagreRevurderingMedBehandlingÅrsak() {
        Behandling revurderingsBehandling = Behandling.fraTidligereBehandling(behandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING)).build();
        behandlingRepository.lagreOgClear(revurderingsBehandling, behandlingRepository.taSkriveLås(revurderingsBehandling));

        revurderingsBehandling = behandlingRepository.hentBehandling(revurderingsBehandling.getId());
        oppdaterMedBehandlingsresultatAvslagOgLagre(revurderingsBehandling);
        revurderingsBehandling.avsluttBehandling();
        behandlingRepository.lagreOgClear(revurderingsBehandling, behandlingRepository.taSkriveLås(revurderingsBehandling));
        revurderingsBehandling = behandlingRepository.hentBehandling(revurderingsBehandling.getId());
        return revurderingsBehandling;
    }

    private Behandling opprettRevurderingsKandidat() {
        LocalDateTime tidligereTidspunkt = NOW.minusSeconds(1);

        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, lagPerson().getAktørId());
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var behandlingVedtak = BehandlingVedtak.builder(behandling.getId())
            .medVedtakstidspunkt(tidligereTidspunkt)
            .medAnsvarligSaksbehandler("asdf").build();
        behandlingVedtakRepository.lagre(behandlingVedtak, behandlingRepository.taSkriveLås(behandling));

        return behandling;
    }

    private void oppdaterMedBehandlingsresultatAvslagOgLagre(Behandling behandling) {
        final var resultatBuilder = Vilkårene.builder();
        final var vilkårBuilder = resultatBuilder
            .hentBuilderFor(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTil(new VilkårPeriodeBuilder().medPeriode(LocalDate.now(), LocalDate.now().plusDays(30)).medUtfall(Utfall.IKKE_OPPFYLT));
        final var vilkårResultat = resultatBuilder
            .leggTil(vilkårBuilder)
            .build();

        behandling.setBehandlingResultatType(BehandlingResultatType.HENLAGT_FEILOPPRETTET);
        behandlingRepository.lagre(behandling);
        vilkårResultatRepository.lagre(behandling.getId(), vilkårResultat);
    }

    private Personinfo lagPerson() {
        final Personinfo personinfo = new Personinfo.Builder()
            .medNavn("Navn navnesen")
            .medAktørId(AktørId.dummy())
            .medFødselsdato(LocalDate.now().minusYears(20))
            .medLandkode(Landkoder.NOR)
            .medKjønn(NavBrukerKjønn.KVINNE)
            .medPersonIdent(new PersonIdent("12345678901"))
            .medForetrukketSpråk(Språkkode.nb)
            .build();
        return personinfo;
    }
}
