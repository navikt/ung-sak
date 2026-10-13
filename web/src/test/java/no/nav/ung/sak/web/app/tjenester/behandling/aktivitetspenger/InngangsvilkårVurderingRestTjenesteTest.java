package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AndreLivsoppholdsytelserResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BostedsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.vurdering.AndreLivsoppholdsytelserVurderingDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.vurdering.GenerellInngangsvilkårVurderingDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.vurdering.InngangsvilkårVurderingDto;
import no.nav.ung.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.web.app.jackson.ObjectMapperResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class InngangsvilkårVurderingRestTjenesteTest {

    private static final String SAKSBEHANDLER = "A111111";
    private static final LocalDateTime VURDERT_TIDSPUNKT = LocalDateTime.of(2026, 4, 1, 12, 0);
    private static final Periode JAN = new Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
    private static final Periode FEB = new Periode(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28));

    @Inject
    private EntityManager entityManager;

    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;
    private InngangsvilkårVurderingRestTjeneste tjeneste;
    private Behandling behandling;
    private BehandlingUuidDto behandlingUuid;

    @BeforeEach
    void setUp() {
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        var behandlingRepository = repositoryProvider.getBehandlingRepository();
        inngangsvilkårVurderingRepository = new InngangsvilkårVurderingRepository(entityManager);
        perioderTilVurderingTjeneste = mock(VilkårsPerioderTilVurderingTjeneste.class);
        tjeneste = new InngangsvilkårVurderingRestTjeneste(behandlingRepository, inngangsvilkårVurderingRepository,
            new UnitTestLookupInstanceImpl<>(perioderTilVurderingTjeneste));

        var fagsak = Fagsak.opprettNy(FagsakYtelseType.AKTIVITETSPENGER, new AktørId("11223344"), new Saksnummer("VURDERINGER1"),
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        repositoryProvider.getFagsakRepository().opprettNy(fagsak);
        behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        behandlingUuid = new BehandlingUuidDto(behandling.getUuid());
        when(perioderTilVurderingTjeneste.utled(eq(behandling.getId()), any())).thenReturn(new TreeSet<>(List.of(tilIntervall(JAN), tilIntervall(FEB))));
    }

    @Test
    void uten_grunnlag_gir_tomme_lister() {
        assertThat(tjeneste.hentInngangsvilkårVurderinger(behandlingUuid)).isEmpty();
        assertThat(tjeneste.hentAndreLivsoppholdsytelserVurderinger(behandlingUuid)).isEmpty();
    }

    @Test
    void andre_livsoppholdsytelser_gir_ytelseNavn_og_fritekst_til_brev_sortert_på_periode() {
        lagreLivsoppholdVurderinger();

        var vurderinger = tjeneste.hentAndreLivsoppholdsytelserVurderinger(behandlingUuid);

        assertThat(vurderinger).containsExactly(livsoppholdOppfylt(), livsoppholdIkkeOppfylt());
    }

    @Test
    void alle_vilkår_gir_spesifikk_dto_for_andre_livsoppholdsytelser_og_generell_for_øvrige() {
        lagreLivsoppholdVurderinger();
        inngangsvilkårVurderingRepository.lagreBostedVurderinger(behandling.getId(), List.of(
            new BostedsvilkårResultatPeriode(tilIntervall(JAN), false, BostedsvilkårIkkeOppfyltÅrsak.ANNET, true, "bor utenfor", "fritekst bosted", SAKSBEHANDLER, VURDERT_TIDSPUNKT)));

        var vurderinger = tjeneste.hentInngangsvilkårVurderinger(behandlingUuid);

        assertThat(vurderinger).containsExactly(
            new GenerellInngangsvilkårVurderingDto(VilkårType.BOSTEDSVILKÅR, JAN, false, BostedsvilkårIkkeOppfyltÅrsak.ANNET.getKode(), true,
                "bor utenfor", "fritekst bosted", SAKSBEHANDLER, VURDERT_TIDSPUNKT),
            livsoppholdOppfylt(),
            livsoppholdIkkeOppfylt());
    }

    @Test
    void vurderinger_utenfor_perioder_til_vurdering_leveres_ikke_ut() {
        lagreLivsoppholdVurderinger();
        inngangsvilkårVurderingRepository.lagreBostedVurderinger(behandling.getId(), List.of(
            new BostedsvilkårResultatPeriode(tilIntervall(JAN), false, BostedsvilkårIkkeOppfyltÅrsak.ANNET, true, "bor utenfor", "fritekst bosted", SAKSBEHANDLER, VURDERT_TIDSPUNKT)));
        when(perioderTilVurderingTjeneste.utled(eq(behandling.getId()), any())).thenReturn(new TreeSet<>(List.of(tilIntervall(FEB))));

        assertThat(tjeneste.hentInngangsvilkårVurderinger(behandlingUuid)).containsExactly(livsoppholdIkkeOppfylt());
        assertThat(tjeneste.hentAndreLivsoppholdsytelserVurderinger(behandlingUuid)).containsExactly(livsoppholdIkkeOppfylt());
    }

    @Test
    void vurdering_klippes_til_perioden_som_er_til_vurdering() {
        lagreLivsoppholdVurderinger();
        var halvFeb = new Periode(LocalDate.of(2026, 2, 15), FEB.getTom());
        when(perioderTilVurderingTjeneste.utled(eq(behandling.getId()), any())).thenReturn(new TreeSet<>(List.of(tilIntervall(halvFeb))));

        var vurderinger = tjeneste.hentAndreLivsoppholdsytelserVurderinger(behandlingUuid);

        assertThat(vurderinger).hasSize(1);
        assertThat(vurderinger.get(0).periode()).isEqualTo(halvFeb);
    }

    @Test
    void serialiseres_med_vilkårType_som_diskriminator() throws Exception {
        var objectMapper = new ObjectMapperResolver().getDefaultObjectMapper();
        List<InngangsvilkårVurderingDto> vurderinger = List.of(
            livsoppholdIkkeOppfylt(),
            new GenerellInngangsvilkårVurderingDto(VilkårType.BISTANDSVILKÅR, JAN, true, null, true, "begrunnelse", null, SAKSBEHANDLER, VURDERT_TIDSPUNKT));

        var json = objectMapper.writerFor(objectMapper.getTypeFactory().constructCollectionType(List.class, InngangsvilkårVurderingDto.class))
            .writeValueAsString(vurderinger);

        var tre = objectMapper.readTree(json);
        assertThat(tre.get(0).get("vilkårType").asText()).isEqualTo(InngangsvilkårVurderingDto.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR);
        assertThat(tre.get(0).get("ytelseNavn").asText()).isEqualTo("livsoppholdytelse fra Tyskland");
        assertThat(tre.get(1).get("vilkårType").asText()).isEqualTo(InngangsvilkårVurderingDto.BISTANDSVILKÅR);
        assertThat(objectMapper.readValue(json, InngangsvilkårVurderingDto[].class)).containsExactlyElementsOf(vurderinger);
    }

    @Test
    void diskriminatorverdier_samsvarer_med_vilkårType_kode() {
        assertThat(InngangsvilkårVurderingDto.BOSTEDSVILKÅR).isEqualTo(VilkårType.BOSTEDSVILKÅR.getKode());
        assertThat(InngangsvilkårVurderingDto.BISTANDSVILKÅR).isEqualTo(VilkårType.BISTANDSVILKÅR.getKode());
        assertThat(InngangsvilkårVurderingDto.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR).isEqualTo(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR.getKode());
        assertThat(InngangsvilkårVurderingDto.AKTIVITETSVILKÅR).isEqualTo(VilkårType.AKTIVITETSVILKÅR.getKode());
    }

    private void lagreLivsoppholdVurderinger() {
        inngangsvilkårVurderingRepository.lagreYtelseVurderinger(behandling.getId(), List.of(
            new AndreLivsoppholdsytelserResultatPeriode(tilIntervall(FEB), false, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_ANNEN_YTELSE,
                true, "mottar livsoppholdytelse fra Tyskland", "fritekst til brev", "livsoppholdytelse fra Tyskland", SAKSBEHANDLER, VURDERT_TIDSPUNKT),
            new AndreLivsoppholdsytelserResultatPeriode(tilIntervall(JAN), true, null,
                true, "mottar ingenting", null, null, SAKSBEHANDLER, VURDERT_TIDSPUNKT)));
    }

    private static AndreLivsoppholdsytelserVurderingDto livsoppholdOppfylt() {
        return new AndreLivsoppholdsytelserVurderingDto(JAN, true, null, true,
            "mottar ingenting", null, null, SAKSBEHANDLER, VURDERT_TIDSPUNKT);
    }

    private static AndreLivsoppholdsytelserVurderingDto livsoppholdIkkeOppfylt() {
        return new AndreLivsoppholdsytelserVurderingDto(FEB, false, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_ANNEN_YTELSE, true,
            "mottar livsoppholdytelse fra Tyskland", "fritekst til brev", "livsoppholdytelse fra Tyskland", SAKSBEHANDLER, VURDERT_TIDSPUNKT);
    }

    private static DatoIntervallEntitet tilIntervall(Periode periode) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
    }
}
