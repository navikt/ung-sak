package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.k9.kodeverk.arbeidsforhold.PermisjonsbeskrivelseType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.organisasjon.Organisasjonstype;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.virksomhet.Virksomhet;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.k9.sak.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektspostBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilter;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilterProvider;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperioderTjeneste;
import no.nav.k9.sak.domene.opptjening.VurderingsStatus;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.OrgNummer;
import no.nav.k9.sak.typer.Stillingsprosent;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class OpptjeningsperioderTjenesteImplTest {

    private static final String ORG_NUMMER = "974760673";
    private InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.nyRef();
    private AktørId AKTØRID = AktørId.dummy();
    private LocalDate skjæringstidspunkt = LocalDate.now();

    @Inject
    private EntityManager entityManager;

    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private IAYRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private VirksomhetTjeneste virksomhetTjeneste = Mockito.mock(VirksomhetTjeneste.class);
    private FagsakRepository fagsakRepository;
    private OpptjeningRepository opptjeningRepository;
    private AksjonspunktutlederForVurderOppgittOpptjening aksjonspunktutlederForVurderOpptjening;
    private VilkårResultatRepository vilkårResultatRepository;
    private OppgittOpptjeningFilterProvider oppgittOpptjeningFilterProvider = Mockito.mock(OppgittOpptjeningFilterProvider.class);
    private AksjonspunktutlederForVurderBekreftetOpptjening apbOpptjening;
    private OpptjeningsperioderTjeneste forSaksbehandlingTjeneste;

    @BeforeEach
    public void setUp() {
        repositoryProvider = new IAYRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        fagsakRepository = new FagsakRepository(entityManager);
        opptjeningRepository = repositoryProvider.getOpptjeningRepository();
        aksjonspunktutlederForVurderOpptjening = new AksjonspunktutlederForVurderOppgittOpptjening(opptjeningRepository, iayTjeneste, virksomhetTjeneste, oppgittOpptjeningFilterProvider);
        vilkårResultatRepository = new VilkårResultatRepository(entityManager);
        apbOpptjening = new AksjonspunktutlederForVurderBekreftetOpptjening(repositoryProvider.getOpptjeningRepository(), iayTjeneste, oppgittOpptjeningFilterProvider);
        forSaksbehandlingTjeneste = new OpptjeningsperioderTjeneste(iayTjeneste, repositoryProvider.getOpptjeningRepository(), aksjonspunktutlederForVurderOpptjening, apbOpptjening, oppgittOpptjeningFilterProvider);
        when(oppgittOpptjeningFilterProvider.finnOpptjeningFilter(Mockito.anyLong())).thenReturn(new OppgittOpptjeningFilter() {
        });

        Virksomhet virksomhet = new Virksomhet.Builder().medOrgnr(ORG_NUMMER)
            .medOppstart(LocalDate.now())
            .medOrganisasjonstype(Organisasjonstype.VIRKSOMHET)
            .medRegistrert(LocalDate.now())
            .build();
        when(virksomhetTjeneste.hentOrganisasjon(ORG_NUMMER)).thenReturn(virksomhet);
    }

    @Test
    void skal_sammenstille_grunnlag_og_overstyrt_deretter_utlede_opptjening_aktivitet_periode() {
        // Arrange
        var behandling = opprettBehandling(skjæringstidspunkt);

        var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt.minusMonths(2));
        var periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(2), skjæringstidspunkt.minusMonths(1));
        var periode3 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(1), skjæringstidspunkt.minusMonths(0));
        var opptjening = opptjeningRepository.lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusMonths(10), skjæringstidspunkt.minusDays(1), false);

        var oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode2, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        var virksomhet = Arbeidsgiver.virksomhet(ORG_NUMMER);
        var bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode1,
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN, virksomhet);
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        var saksbehandlet = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.of(bekreftet.build()),
            VersjonType.SAKSBEHANDLET);
        var ref = saksbehandlet.medNyInternArbeidsforholdRef(virksomhet, EksternArbeidsforholdRef.ref("1"));

        var yrkesaktivitetBuilder = saksbehandlet.getAktørArbeidBuilder(AKTØRID)
            .getYrkesaktivitetBuilderForNøkkelAvType(new Opptjeningsnøkkel(ref, null, null),
                ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktivitetBuilder
            .leggTilAktivitetsAvtale(yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(periode3.getFomDato(), periode3.getTomDato()))
                .medProsentsats(BigDecimal.TEN));

        var behandlingId = behandling.getId();
        iayTjeneste.lagreIayAggregat(behandlingId, saksbehandlet);
        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandlingId);

        // Act
        var behandlingRef = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(behandlingRef, iayGrunnlag, opptjening, skjæringstidspunkt);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.GODKJENT)).collect(Collectors.toList())).hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.UNDERKJENT)).collect(Collectors.toList())).hasSize(1);
    }

    @Test
    void skal_sammenstille_grunnlag_og_utlede_opptjening_aktivitet_periode() {
        // Arrange
        Behandling behandling = opprettBehandling(skjæringstidspunkt);

        var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt.minusMonths(2));
        var periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(2), skjæringstidspunkt.minusMonths(1));
        var opptjening = opptjeningRepository.lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusMonths(10), skjæringstidspunkt.minusDays(1), false);

        var oppgitt = OppgittOpptjeningBuilder.ny();

        var egenNæringBuilder = OppgittOpptjeningBuilder.EgenNæringBuilder.ny();
        egenNæringBuilder
            .medRegnskapsførerNavn("Larsen")
            .medRegnskapsførerTlf("TELEFON")
            .medVirksomhet(ORG_NUMMER)
            .medPeriode(periode2);

        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode2, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        oppgitt.leggTilEgneNæringer(List.of(egenNæringBuilder));
        var behandlingId = behandling.getId();
        iayTjeneste.lagreOppgittOpptjening(behandlingId, oppgitt);

        var bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode1, ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN,
            Arbeidsgiver.virksomhet(ORG_NUMMER));
        iayTjeneste.lagreIayAggregat(behandlingId, bekreftet);
        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        // Act
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref, iayGrunnlag, opptjening, skjæringstidspunkt);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.GODKJENT)).collect(Collectors.toList())).hasSize(2);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.TIL_VURDERING)).collect(Collectors.toList())).hasSize(1);
        assertThat(perioder.stream().filter(OpptjeningsperiodeForSaksbehandling::getErManueltRegistrert).collect(Collectors.toList())).isEmpty();
        assertThat(perioder.stream().filter(o -> !o.getErManueltRegistrert()).collect(Collectors.toList())).hasSize(3);
    }

    @Test
    void skal_utlede_om_en_periode_er_blitt_endret() {
        // Arrange
        var behandling = opprettBehandling(skjæringstidspunkt);

        var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt.minusMonths(2));
        var periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(2), skjæringstidspunkt.minusMonths(1));
        var opptjening = opptjeningRepository.lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusMonths(10), skjæringstidspunkt.minusDays(1), false);

        var oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode2, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        var behandlingId = behandling.getId();
        iayTjeneste.lagreOppgittOpptjening(behandlingId, oppgitt);

        var saksbehandlet = opprettOverstyrtOppgittOpptjening(periode1, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE, AKTØRID, VersjonType.SAKSBEHANDLET);
        iayTjeneste.lagreIayAggregat(behandlingId, saksbehandlet);
        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandlingId);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        // Act
        // Assert
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref, iayGrunnlag, opptjening, skjæringstidspunkt)
            .stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE)).collect(Collectors.toList());

        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getErPeriodeEndret()).isTrue();
        assertThat(perioder.get(0).getBegrunnelse()).isNotEmpty();
    }

    @Test
    void skal_returnere_oat_frilans_ved_bekreftet_frilans() {
        // Arrange
        var behandling = opprettBehandling(skjæringstidspunkt);
        var arbeidsgiver = Arbeidsgiver.virksomhet(ORG_NUMMER);

        LocalDate fraOgMed = skjæringstidspunkt.minusMonths(4);
        LocalDate tilOgMed = skjæringstidspunkt.minusMonths(3);
        var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);

        var bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode1, ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER, BigDecimal.TEN, arbeidsgiver);
        opprettInntektForFrilanser(bekreftet, AKTØRID, ARBEIDSFORHOLD_ID, periode1, arbeidsgiver);
        var behandlingId = behandling.getId();
        iayTjeneste.lagreIayAggregat(behandlingId, bekreftet);
        var iayGrunnlag01 = iayTjeneste.hentGrunnlag(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        var opptjening = opptjeningRepository.lagreOpptjeningsperiode(behandling, skjæringstidspunkt.minusMonths(10), skjæringstidspunkt.minusDays(1), false);

        // Act 1
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref, iayGrunnlag01, opptjening, skjæringstidspunkt);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).collect(Collectors.toList()))
            .hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.TIL_VURDERING)).collect(Collectors.toList())).hasSize(1);
        assertThat(perioder.stream().filter(OpptjeningsperiodeForSaksbehandling::getErManueltRegistrert).collect(Collectors.toList())).isEmpty();
        assertThat(perioder.stream().filter(o -> !o.getErManueltRegistrert()).collect(Collectors.toList())).hasSize(1);

        // Act 2
        var saksbehandlet = opprettOverstyrtOppgittOpptjening(periode1,
            ArbeidType.MILITÆR_ELLER_SIVILTJENESTE, AKTØRID, VersjonType.SAKSBEHANDLET);
        iayTjeneste.lagreIayAggregat(behandlingId, saksbehandlet);
        var iayGrunnlag02 = iayTjeneste.hentGrunnlag(behandlingId);
        perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref, iayGrunnlag02, opptjening, skjæringstidspunkt);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).collect(Collectors.toList()))
            .hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.UNDERKJENT)).collect(Collectors.toList())).hasSize(1);
        assertThat(perioder.stream().filter(OpptjeningsperiodeForSaksbehandling::getErManueltRegistrert).collect(Collectors.toList())).hasSize(1);
    }

    @Test
    void skal_sette_manuelt_behandlet_ved_underkjent_frilans() {
        // Arrange
        var behandling = opprettBehandling(skjæringstidspunkt);

        var arbeidsgiver = Arbeidsgiver.virksomhet(ORG_NUMMER);

        LocalDate fraOgMed = LocalDate.now().minusMonths(4);
        LocalDate tilOgMed = LocalDate.now().minusMonths(3);
        var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);

        var bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode1,
            ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER, BigDecimal.TEN, arbeidsgiver);
        opprettInntektForFrilanser(bekreftet, AKTØRID, ARBEIDSFORHOLD_ID, periode1, arbeidsgiver);
        var behandlingId = behandling.getId();
        iayTjeneste.lagreIayAggregat(behandlingId, bekreftet);

        var oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode1, ArbeidType.FRILANSER));
        iayTjeneste.lagreOppgittOpptjening(behandlingId, oppgitt);


        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        var opptjening = opptjeningRepository.lagreOpptjeningsperiode(behandling, LocalDate.now().minusMonths(10), LocalDate.now().minusDays(1), false);
        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandlingId);
        // Act 1
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref, iayGrunnlag, opptjening, skjæringstidspunkt);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).collect(Collectors.toList()))
            .hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.TIL_VURDERING)).collect(Collectors.toList())).hasSize(1);
        assertThat(perioder.stream().filter(OpptjeningsperiodeForSaksbehandling::getErManueltRegistrert).collect(Collectors.toList())).isEmpty();
        assertThat(perioder.stream().filter(o -> !o.getErManueltRegistrert()).collect(Collectors.toList())).hasSize(1);

        // Act 2
        var saksbehandlet = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.SAKSBEHANDLET);
        iayTjeneste.lagreIayAggregat(behandlingId, saksbehandlet);
        var iayGrunnlag02 = iayTjeneste.hentGrunnlag(behandlingId);
        perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref, iayGrunnlag02, opptjening, skjæringstidspunkt);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).collect(Collectors.toList()))
            .hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.UNDERKJENT)).collect(Collectors.toList())).hasSize(1);
        var frilansPeriode = perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).findFirst().get();
        assertThat(frilansPeriode.erManueltBehandlet()).isTrue();
    }

    @Test
    void skal_returnere_oat_frilans_ved_bekreftet_frilans_for_vilkår() {
        // Arrange
        var behandling = opprettBehandling(skjæringstidspunkt);

        var arbeidsgiver = Arbeidsgiver.virksomhet(ORG_NUMMER);

        LocalDate fraOgMed = LocalDate.now().minusMonths(4);
        LocalDate tilOgMed = LocalDate.now().minusMonths(3);
        var periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);

        var bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode1,
            ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER, BigDecimal.TEN, arbeidsgiver);
        opprettInntektForFrilanser(bekreftet, AKTØRID, ARBEIDSFORHOLD_ID, periode1, arbeidsgiver);
        var behandlingId = behandling.getId();
        iayTjeneste.lagreIayAggregat(behandlingId, bekreftet);
        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandlingId);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        var opptjening = opptjeningRepository.lagreOpptjeningsperiode(behandling, LocalDate.now().minusMonths(10), LocalDate.now().minusDays(1), false);

        // Act 1
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(ref, iayGrunnlag, opptjening, skjæringstidspunkt);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).collect(Collectors.toList()))
            .hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.FERDIG_VURDERT_UNDERKJENT)).collect(Collectors.toList()))
            .hasSize(1);

        // Act 2
        var saksbehandlet = opprettOverstyrtOppgittOpptjening(periode1, ArbeidType.FRILANSER, AKTØRID,
            VersjonType.SAKSBEHANDLET);
        iayTjeneste.lagreIayAggregat(behandlingId, saksbehandlet);
        var iayGrunnlag2 = iayTjeneste.hentGrunnlag(behandlingId);
        perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(ref, iayGrunnlag2, opptjening, skjæringstidspunkt);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).collect(Collectors.toList()))
            .hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.FERDIG_VURDERT_GODKJENT)).collect(Collectors.toList()))
            .hasSize(1);
    }

    @Test
    void skal_returnere_en_periode_med_fiktivt_bekreftet_arbeidsforhold() {
        // Arrange
        var behandling = opprettBehandling(skjæringstidspunkt);
        var opptjening = opptjeningRepository.lagreOpptjeningsperiode(behandling, LocalDate.now().minusMonths(10), LocalDate.now().minusDays(1), false);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        LocalDate fraOgMed = LocalDate.of(2015, 1, 4);
        LocalDate tilOgMed = skjæringstidspunkt.plusMonths(2);
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);
        var saksbehandlet = lagFiktivtArbeidsforholdSaksbehandlet(periode);
        var behandlingId = behandling.getId();
        iayTjeneste.lagreIayAggregat(behandlingId, saksbehandlet);
        ArbeidsforholdInformasjonBuilder informasjon = lagFiktivtArbeidsforholdOverstyring(fraOgMed, tilOgMed);
        iayTjeneste.lagreArbeidsforhold(behandlingId, AKTØRID, informasjon);

        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandlingId);
        // Act
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref, iayGrunnlag, opptjening, skjæringstidspunkt);

        // Assert
        assertThat(perioder.size()).isEqualTo(1);
    }

    @Test
    void skal_kunne_bygge_opptjeninsperiode_basert_på_arbeidsforhold_lagt_til_avsaksbehandler() {
        var behandling = opprettBehandling(skjæringstidspunkt);
        var opptjening = opptjeningRepository.lagreOpptjeningsperiode(behandling, LocalDate.now().minusMonths(10), LocalDate.now().minusDays(1), false);
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        LocalDate start = LocalDate.now().minusMonths(5);

        var arbeidsforholdInformasjonBuilder = ArbeidsforholdInformasjonBuilder.builder(Optional.empty());
        var virksomhet = Arbeidsgiver.virksomhet("912471691");

        var arbeidsforholdOverstyringBuilder = arbeidsforholdInformasjonBuilder.getOverstyringBuilderFor(virksomhet, null);
        arbeidsforholdOverstyringBuilder.medHandling(ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING);
        arbeidsforholdOverstyringBuilder.leggTilOverstyrtPeriode(start, LocalDate.MAX);
        arbeidsforholdOverstyringBuilder.medAngittStillingsprosent(Stillingsprosent.ZERO);
        arbeidsforholdInformasjonBuilder.leggTil(arbeidsforholdOverstyringBuilder);

        var behandlingId = behandling.getId();
        iayTjeneste.lagreArbeidsforhold(behandling.getId(), AKTØRID, arbeidsforholdInformasjonBuilder);
        var iayGrunnlag = iayTjeneste.hentGrunnlag(behandlingId);

        // Act
        @SuppressWarnings("unused")
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref, iayGrunnlag, opptjening, skjæringstidspunkt);

    }

    private ArbeidsforholdInformasjonBuilder lagFiktivtArbeidsforholdOverstyring(LocalDate fraOgMed, LocalDate tilOgMed) {
        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        return ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty())
            .leggTil(ArbeidsforholdOverstyringBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(arbeidsgiver)
                .medHandling(ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER)
                .leggTilOverstyrtPeriode(fraOgMed, tilOgMed)
                .medAngittStillingsprosent(new Stillingsprosent(BigDecimal.valueOf(100))));
    }

    private InntektArbeidYtelseAggregatBuilder lagFiktivtArbeidsforholdSaksbehandlet(DatoIntervallEntitet periode) {
        var arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.SAKSBEHANDLET);
        var aktørArbeidBuilder = builder.getAktørArbeidBuilder(AKTØRID);
        var yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(
            new Opptjeningsnøkkel(null, OrgNummer.KUNSTIG_ORG, null), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();
        var aktivitetsAvtale = aktivitetsAvtaleBuilder
            .medPeriode(periode)
            .medProsentsats(BigDecimal.valueOf(100))
            .medBeskrivelse("Ser greit ut");
        var ansettelsesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(periode);
        yrkesaktivitetBuilder
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(null)
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode);
        var aktørArbeid = aktørArbeidBuilder
            .leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        builder.leggTilAktørArbeid(aktørArbeid);
        return builder;
    }


    private InntektArbeidYtelseAggregatBuilder opprettOverstyrtOppgittOpptjening(DatoIntervallEntitet periode, ArbeidType type, AktørId aktørId,
                                                                                 VersjonType register) {
        var builder = InntektArbeidYtelseAggregatBuilder
            .oppdatere(Optional.empty(), register);

        var aktørArbeidBuilder = builder.getAktørArbeidBuilder(aktørId);
        var yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForType(type);

        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();

        var aktivitetsAvtale = aktivitetsAvtaleBuilder
            .medPeriode(periode)
            .medBeskrivelse("Ser greit ut");

        yrkesaktivitetBuilder
            .medArbeidType(type)
            .leggTilAktivitetsAvtale(aktivitetsAvtale);
        var aktørArbeid = aktørArbeidBuilder
            .leggTilYrkesaktivitet(yrkesaktivitetBuilder);

        builder.leggTilAktørArbeid(aktørArbeid);

        return builder;
    }

    private Behandling opprettBehandling(@SuppressWarnings("unused") LocalDate skjæringstidspunkt) {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, AKTØRID);
        @SuppressWarnings("unused")
        Long fagsakId = fagsakRepository.opprettNy(fagsak);
        var builder = Behandling.forFørstegangssøknad(fagsak);
        var behandling = builder.build();
        var nyttResultat = Vilkårene.builder().build();

        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        vilkårResultatRepository.lagre(behandling.getId(), nyttResultat);
        return behandling;
    }

    private InntektArbeidYtelseAggregatBuilder opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AktørId aktørId, InternArbeidsforholdRef ref,
                                                                                                   DatoIntervallEntitet periode, ArbeidType type,
                                                                                                   BigDecimal prosentsats, Arbeidsgiver virksomhet1) {
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);

        var aktørArbeidBuilder = builder.getAktørArbeidBuilder(aktørId);
        var yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(
            new Opptjeningsnøkkel(ref, virksomhet1.getIdentifikator(), null), type);

        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();
        var permisjonBuilder = yrkesaktivitetBuilder.getPermisjonBuilder();

        var aktivitetsAvtale = aktivitetsAvtaleBuilder
            .medPeriode(periode)
            .medProsentsats(prosentsats)
            .medBeskrivelse("Ser greit ut");
        var ansettelsesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(periode);

        var permisjon = permisjonBuilder
            .medPermisjonsbeskrivelseType(PermisjonsbeskrivelseType.UTDANNINGSPERMISJON)
            .medPeriode(periode.getFomDato(), periode.getTomDato())
            .medProsentsats(BigDecimal.valueOf(100))
            .build();

        yrkesaktivitetBuilder
            .medArbeidType(type)
            .medArbeidsgiver(virksomhet1)
            .medArbeidsforholdId(ARBEIDSFORHOLD_ID)
            .leggTilPermisjon(permisjon)
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode);

        var aktørArbeid = aktørArbeidBuilder
            .leggTilYrkesaktivitet(yrkesaktivitetBuilder);

        builder.leggTilAktørArbeid(aktørArbeid);

        return builder;
    }

    private void opprettInntektForFrilanser(InntektArbeidYtelseAggregatBuilder bekreftet, AktørId aktørId, InternArbeidsforholdRef ref, DatoIntervallEntitet periode,
                                            Arbeidsgiver virksomhet1) {
        var ainntektBuilder = bekreftet.getAktørInntektBuilder(aktørId);
        var inntektBuilder = ainntektBuilder.getInntektBuilder(InntektsKilde.INNTEKT_OPPTJENING, new Opptjeningsnøkkel(ref, virksomhet1.getIdentifikator(), null));
        inntektBuilder.medArbeidsgiver(virksomhet1);
        inntektBuilder.leggTilInntektspost(InntektspostBuilder.ny().medInntektspostType(InntektspostType.LØNN)
            .medPeriode(periode.getFomDato(), periode.getTomDato()).medBeløp(BigDecimal.TEN));
        ainntektBuilder.leggTilInntekt(inntektBuilder);
        bekreftet.leggTilAktørInntekt(ainntektBuilder);
    }

}
