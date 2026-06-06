package no.nav.ung.sak.behandlingslager.inngangsvilkår;

import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.CdiDbAwareTest;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@CdiDbAwareTest
class InngangsvilkårVurderingRepositoryTest {

    @Inject
    private FagsakRepository fagsakRepository;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private InngangsvilkårVurderingRepository repository;

    private Behandling behandling;

    private static final DatoIntervallEntitet PERIODE_1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30));
    private static final DatoIntervallEntitet PERIODE_2 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2024, 7, 1), LocalDate.of(2024, 12, 31));
    private static final String VURDERT_AV = "saksbehandler1";
    private static final LocalDateTime VURDERT_TIDSPUNKT = LocalDateTime.of(2024, 6, 1, 10, 0);

    @BeforeEach
    void setUp() {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.AKTIVITETSPENGER, new AktørId("1"), new Saksnummer("SAK1"), LocalDate.now(), LocalDate.now().plusYears(1).minusDays(1));
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandlingRepository.lagre(behandling, new BehandlingLås(null));
    }

    @Test
    void skal_lagre_og_hente_bistandsvurdering() {
        var vurdering = new BistandsvilkårVurderingPeriode(PERIODE_1, true, null, VURDERT_AV, VURDERT_TIDSPUNKT);
        repository.lagreBistandsVurderinger(behandling.getId(), List.of(vurdering));

        var grunnlag = repository.hentGrunnlag(behandling.getId());

        assertThat(grunnlag).isPresent();
        var holder = grunnlag.get().getBistandsvilkårVurderingHolder();
        assertThat(holder).isPresent();
        assertThat(holder.get().getVurderinger()).hasSize(1);
        var lagretVurdering = holder.get().getVurderinger().get(0);
        assertThat(lagretVurdering.getPeriode()).isEqualTo(PERIODE_1);
        assertThat(lagretVurdering.isGodkjent()).isTrue();
        assertThat(lagretVurdering.getAvslagsårsak()).isNull();
        assertThat(lagretVurdering.getVurdertAv()).isEqualTo(VURDERT_AV);
        assertThat(lagretVurdering.getVurdertTidspunkt()).isEqualTo(VURDERT_TIDSPUNKT);
    }

    @Test
    void skal_lagre_bistandsvurdering_med_avslagsårsak() {
        var vurdering = new BistandsvilkårVurderingPeriode(PERIODE_1, false, Avslagsårsak.IKKE_14A_VEDTAK, VURDERT_AV, VURDERT_TIDSPUNKT);
        repository.lagreBistandsVurderinger(behandling.getId(), List.of(vurdering));

        var holder = repository.hentGrunnlag(behandling.getId())
            .flatMap(InngangsvilkårVurderingGrunnlag::getBistandsvilkårVurderingHolder)
            .orElseThrow();

        assertThat(holder.getVurderinger()).hasSize(1);
        var lagretVurdering = holder.getVurderinger().get(0);
        assertThat(lagretVurdering.isGodkjent()).isFalse();
        assertThat(lagretVurdering.getAvslagsårsak()).isEqualTo(Avslagsårsak.IKKE_14A_VEDTAK);
    }

    @Test
    void skal_lagre_og_hente_livsoppholdsytelsevurdering() {
        var vurdering = new AndreLivsoppholdsytelserVurderingPeriode(PERIODE_1, false, Avslagsårsak.SØKER_HAR_ANNEN_LIVSOPPHOLDSYTELSE, VURDERT_AV, VURDERT_TIDSPUNKT);
        repository.lagreLivsoppholdsVurderinger(behandling.getId(), List.of(vurdering));

        var grunnlag = repository.hentGrunnlag(behandling.getId());

        assertThat(grunnlag).isPresent();
        assertThat(grunnlag.get().getBistandsvilkårVurderingHolder()).as("bistandsholder skal være tom").isEmpty();
        var holder = grunnlag.get().getAndreLivsoppholdsytelserVurderingHolder();
        assertThat(holder).isPresent();
        assertThat(holder.get().getVurderinger()).hasSize(1);
        var lagretVurdering = holder.get().getVurderinger().get(0);
        assertThat(lagretVurdering.isGodkjent()).isFalse();
        assertThat(lagretVurdering.getAvslagsårsak()).isEqualTo(Avslagsårsak.SØKER_HAR_ANNEN_LIVSOPPHOLDSYTELSE);
    }

    @Test
    void oppdatering_av_bistand_bevarer_livsopphold_holder() {
        var livsoppholdVurdering = new AndreLivsoppholdsytelserVurderingPeriode(PERIODE_1, true, null, VURDERT_AV, VURDERT_TIDSPUNKT);
        repository.lagreLivsoppholdsVurderinger(behandling.getId(), List.of(livsoppholdVurdering));

        var bistandVurdering = new BistandsvilkårVurderingPeriode(PERIODE_1, true, null, VURDERT_AV, VURDERT_TIDSPUNKT);
        repository.lagreBistandsVurderinger(behandling.getId(), List.of(bistandVurdering));

        var grunnlag = repository.hentGrunnlag(behandling.getId()).orElseThrow();
        assertThat(grunnlag.getBistandsvilkårVurderingHolder()).isPresent();
        assertThat(grunnlag.getAndreLivsoppholdsytelserVurderingHolder())
            .as("livsopphold-holder skal bevares ved oppdatering av bistand")
            .isPresent();
    }

    @Test
    void oppdatering_av_livsopphold_bevarer_bistand_holder() {
        var bistandVurdering = new BistandsvilkårVurderingPeriode(PERIODE_1, true, null, VURDERT_AV, VURDERT_TIDSPUNKT);
        repository.lagreBistandsVurderinger(behandling.getId(), List.of(bistandVurdering));

        var livsoppholdVurdering = new AndreLivsoppholdsytelserVurderingPeriode(PERIODE_2, false, Avslagsårsak.SØKER_HAR_ANNEN_LIVSOPPHOLDSYTELSE, VURDERT_AV, VURDERT_TIDSPUNKT);
        repository.lagreLivsoppholdsVurderinger(behandling.getId(), List.of(livsoppholdVurdering));

        var grunnlag = repository.hentGrunnlag(behandling.getId()).orElseThrow();
        assertThat(grunnlag.getAndreLivsoppholdsytelserVurderingHolder()).isPresent();
        assertThat(grunnlag.getBistandsvilkårVurderingHolder())
            .as("bistand-holder skal bevares ved oppdatering av livsopphold")
            .isPresent();
    }

    @Test
    void oppdatering_erstatter_tidligere_bistandsvurdering() {
        repository.lagreBistandsVurderinger(behandling.getId(),
            List.of(new BistandsvilkårVurderingPeriode(PERIODE_1, true, null, VURDERT_AV, VURDERT_TIDSPUNKT)));

        repository.lagreBistandsVurderinger(behandling.getId(),
            List.of(
                new BistandsvilkårVurderingPeriode(PERIODE_1, false, Avslagsårsak.IKKE_14A_VEDTAK, "saksbehandler2", VURDERT_TIDSPUNKT.plusHours(1)),
                new BistandsvilkårVurderingPeriode(PERIODE_2, true, null, "saksbehandler2", VURDERT_TIDSPUNKT.plusHours(1))
            ));

        var holder = repository.hentGrunnlag(behandling.getId())
            .flatMap(InngangsvilkårVurderingGrunnlag::getBistandsvilkårVurderingHolder)
            .orElseThrow();

        assertThat(holder.getVurderinger()).hasSize(2);
    }

    @Test
    void skal_kopiere_grunnlag_til_ny_behandling() {
        repository.lagreBistandsVurderinger(behandling.getId(),
            List.of(new BistandsvilkårVurderingPeriode(PERIODE_1, true, null, VURDERT_AV, VURDERT_TIDSPUNKT)));
        repository.lagreLivsoppholdsVurderinger(behandling.getId(),
            List.of(new AndreLivsoppholdsytelserVurderingPeriode(PERIODE_1, false, Avslagsårsak.SØKER_HAR_ANNEN_LIVSOPPHOLDSYTELSE, VURDERT_AV, VURDERT_TIDSPUNKT)));

        var revurdering = Behandling.nyBehandlingFor(behandling.getFagsak(), BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(revurdering, new BehandlingLås(null));
        repository.kopier(behandling.getId(), revurdering.getId());

        var kopiert = repository.hentGrunnlag(revurdering.getId()).orElseThrow();
        assertThat(kopiert.getBistandsvilkårVurderingHolder()).isPresent();
        assertThat(kopiert.getAndreLivsoppholdsytelserVurderingHolder()).isPresent();

        var original = repository.hentGrunnlag(behandling.getId()).orElseThrow();
        assertThat(kopiert.getBistandsvilkårVurderingHolder().get().getId())
            .as("Kopiert grunnlag skal dele samme bistand-holder")
            .isEqualTo(original.getBistandsvilkårVurderingHolder().get().getId());
    }

    @Test
    void skal_ikke_kopiere_naar_ingen_grunnlag_eksisterer() {
        var revurdering = Behandling.nyBehandlingFor(behandling.getFagsak(), BehandlingType.REVURDERING).build();
        behandlingRepository.lagre(revurdering, new BehandlingLås(null));
        repository.kopier(behandling.getId(), revurdering.getId());

        assertThat(repository.hentGrunnlag(revurdering.getId())).isEmpty();
    }
}
