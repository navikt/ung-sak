package no.nav.ung.sak.web.app.tjenester.behandling.aktivitetspenger;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.felles.testutilities.sikkerhet.StaticSubjectHandler;
import no.nav.k9.felles.testutilities.sikkerhet.SubjectHandlerUtils;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.BostedsavklaringKildeType;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.bosatt.BostedsPeriodeAvklaringForeslått;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.AktivitetspengerInngangsvilkårResultatGrunnlag;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.BostedsvilkårResultatPeriode;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.bosted.VurderingAvBostedsvilkårEtterAvklaringDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.VurderingAvVilkårPeriodeEtterAvklaringDto;
import no.nav.ung.sak.kontrakt.aktivitetspenger.ÅpenPeriode;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.ytelse.aktivitetspenger.del1.InngangsvilkårVurderingTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.del1.steg.bosatt.BostedAvklaringTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.historikkinnslag.VilkårsvurderingHistorikkinnslagTjeneste;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class VurderingAvBostedsvilkårEtterAvklaringOppdatererTest {

    private static final String SAKSBEHANDLER = "A111111";

    private static final Periode PERIODE_1 = new Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
    private static final Periode PERIODE_2 = new Periode(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));

    @Inject
    private EntityManager entityManager;

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;
    private BostedsGrunnlagRepository bostedsGrunnlagRepository;
    private HistorikkinnslagRepository historikkinnslagRepository;
    private VurderingAvBostedsvilkårEtterAvklaringOppdaterer oppdaterer;

    @Inject
    private VilkårsvurderingHistorikkinnslagTjeneste vilkårsvurderingHistorikkinnslagTjeneste;

    private Fagsak fagsak;

    @BeforeAll
    static void beforeAll() {
        SubjectHandlerUtils.useSubjectHandler(StaticSubjectHandler.class);
        SubjectHandlerUtils.setInternBruker(SAKSBEHANDLER);
    }

    @AfterAll
    static void afterAll() {
        SubjectHandlerUtils.reset();
    }

    @BeforeEach
    void setUp() {
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        fagsakRepository = repositoryProvider.getFagsakRepository();
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        inngangsvilkårVurderingRepository = new InngangsvilkårVurderingRepository(entityManager);
        bostedsGrunnlagRepository = new BostedsGrunnlagRepository(entityManager);
        var inngangsvilkårVurderingTjeneste = new InngangsvilkårVurderingTjeneste(inngangsvilkårVurderingRepository, behandlingRepository, vilkårResultatRepository);
        var bostedAvklaringTjeneste = new BostedAvklaringTjeneste(bostedsGrunnlagRepository, null, null, null);
        var vurderingAvVilkårEtterAvklaringTjeneste = new VurderingAvVilkårEtterAvklaringTjeneste(vilkårResultatRepository);
        historikkinnslagRepository = new HistorikkinnslagRepository(entityManager);

        oppdaterer = new VurderingAvBostedsvilkårEtterAvklaringOppdaterer(
            vurderingAvVilkårEtterAvklaringTjeneste,
            bostedAvklaringTjeneste,
            inngangsvilkårVurderingRepository,
            inngangsvilkårVurderingTjeneste,
            vilkårsvurderingHistorikkinnslagTjeneste);

        fagsak = Fagsak.opprettNy(FagsakYtelseType.AKTIVITETSPENGER, new AktørId("1122334455667"), new Saksnummer("BOSTEDOPP1"),
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        fagsakRepository.opprettNy(fagsak);
    }

    @Test
    void lagrer_vurdering_med_årsak_fra_avklaringen_ikke_fra_dto() {
        var behandling = opprettFørstegangsbehandling(PERIODE_1);
        lagreForeslåttAvklaring(behandling, PERIODE_1, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM);

        var dto = dto(vurdering(PERIODE_1, false, "flyttet ut av Trondheim", null));
        utførOppdatering(behandling, dto);

        var vurdering = hentBostedvurdering(behandling, PERIODE_1);
        assertThat(vurdering.isGodkjent()).isFalse();
        assertThat(vurdering.getIkkeOppfyltÅrsak()).isEqualTo(BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM);
        assertThat(vurdering.getVurdertAv()).isEqualTo(SAKSBEHANDLER);
        assertThat(historikkinnslagRepository.hent(behandling.getId()))
            .extracting(Historikkinnslag::getSkjermlenke)
            .containsExactly(SkjermlenkeType.BOSTEDSVILKÅR);
    }

    @Test
    void lagrer_kun_periodene_som_er_vurdert() {
        var behandling = opprettFørstegangsbehandling(PERIODE_1, PERIODE_2);
        lagreForeslåtteAvklaringer(behandling, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, PERIODE_1, PERIODE_2);

        var dto = dto(vurdering(PERIODE_1, false, "kun periode 1 vurdert nå", null));
        utførOppdatering(behandling, dto);

        var lagrede = inngangsvilkårVurderingRepository.hentEksisterendeGrunnlag(behandling.getId())
            .map(AktivitetspengerInngangsvilkårResultatGrunnlag::hentBostedsvilkårResultatPerioder)
            .orElseThrow();
        assertThat(lagrede).extracting(BostedsvilkårResultatPeriode::getPeriode)
            .containsExactly(tilDatoIntervallEntitet(PERIODE_1));
    }

    private void utførOppdatering(Behandling behandling, VurderingAvBostedsvilkårEtterAvklaringDto dto) {
        var param = oppdaterParameter(behandling, dto);
        oppdaterer.oppdater(dto, param);
    }

    private AksjonspunktOppdaterParameter oppdaterParameter(Behandling behandling, VurderingAvBostedsvilkårEtterAvklaringDto dto) {
        VilkårResultatBuilder vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårResultatRepository.hent(behandling.getId()));
        return new AksjonspunktOppdaterParameter(behandling, Optional.empty(), vilkårResultatBuilder, dto);
    }

    private BostedsvilkårResultatPeriode hentBostedvurdering(Behandling behandling, Periode periode) {
        return inngangsvilkårVurderingRepository.hentEksisterendeGrunnlag(behandling.getId())
            .map(AktivitetspengerInngangsvilkårResultatGrunnlag::hentBostedsvilkårResultatPerioder)
            .orElseThrow()
            .stream()
            .filter(v -> v.getPeriode().equals(tilDatoIntervallEntitet(periode)))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Fant ingen lagret vurdering for periode " + periode));
    }

    private Behandling opprettFørstegangsbehandling(Periode... vilkårsperioder) {
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));

        var vilkårResultatBuilder = Vilkårene.builder();
        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.BOSTEDSVILKÅR);
        for (var periode : vilkårsperioder) {
            vilkårBuilder.leggTil(new VilkårPeriodeBuilder()
                .medPeriode(tilDatoIntervallEntitet(periode))
                .medUtfall(Utfall.IKKE_VURDERT));
        }
        vilkårResultatBuilder.leggTil(vilkårBuilder);
        vilkårResultatRepository.lagre(behandling.getId(), vilkårResultatBuilder.build());
        return behandling;
    }

    private void lagreForeslåttAvklaring(Behandling behandling, Periode periode, BostedsvilkårIkkeOppfyltÅrsak årsak) {
        lagreForeslåtteAvklaringer(behandling, årsak, periode);
    }

    private void lagreForeslåtteAvklaringer(Behandling behandling, BostedsvilkårIkkeOppfyltÅrsak årsak, Periode... perioder) {
        bostedsGrunnlagRepository.lagreInformasjonFraSøknad(behandling.getId(), "123456789", perioder[0].getFom(), true);
        var avklaringer = Arrays.stream(perioder)
            .map(periode -> new BostedsPeriodeAvklaringForeslått(
                UUID.randomUUID(),
                DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom()),
                årsak,
                "begrunnelse for avklaring",
                true,
                årsak.kreverFritekst() ? "fritekst til varsel" : null,
                null,
                BostedsavklaringKildeType.FOLKEREGISTER,
                null,
                SAKSBEHANDLER,
                LocalDateTime.now(),
                Avklaringtype.OPPHØR))
            .collect(Collectors.toSet());
        bostedsGrunnlagRepository.lagreForeslåtteAvklaringer(behandling.getId(), avklaringer);
    }

    private static VurderingAvBostedsvilkårEtterAvklaringDto dto(VurderingAvVilkårPeriodeEtterAvklaringDto... perioder) {
        return new VurderingAvBostedsvilkårEtterAvklaringDto(List.of(perioder), "begrunnelse for aksjonspunktet");
    }

    private static VurderingAvVilkårPeriodeEtterAvklaringDto vurdering(Periode periode, boolean erVilkårOppfylt, String begrunnelse, String fritekstVurderingBrev) {
        return new VurderingAvVilkårPeriodeEtterAvklaringDto(new ÅpenPeriode(periode.getFom(), periode.getTom()), erVilkårOppfylt, begrunnelse, fritekstVurderingBrev);
    }

    private static DatoIntervallEntitet tilDatoIntervallEntitet(Periode periode) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
    }
}
