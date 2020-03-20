package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.k9.kodeverk.arbeidsforhold.PermisjonsbeskrivelseType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.testutilities.behandling.IAYRepositoryProvider;
import no.nav.k9.sak.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.InntektBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektspostBuilder;
import no.nav.k9.sak.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.iay.modell.Permisjon;
import no.nav.k9.sak.domene.iay.modell.PermisjonBuilder;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperioderTjeneste;
import no.nav.k9.sak.domene.opptjening.VurderingsStatus;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.OrgNummer;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Stillingsprosent;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class OpptjeningsperioderTjenesteImplTest {

    private static final String ORG_NUMMER = "974760673";

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private final LocalDate skjæringstidspunkt = LocalDate.now();
    private IAYRepositoryProvider repositoryProvider = new IAYRepositoryProvider(repoRule.getEntityManager());
    private final BehandlingRepository behandlingRepository = repositoryProvider.getBehandlingRepository();
    private final VirksomhetTjeneste virksomhetTjeneste = new VirksomhetTjeneste(null, repositoryProvider.getVirksomhetRepository());
    private FagsakRepository fagsakRepository = new FagsakRepository(repoRule.getEntityManager());
    private OpptjeningRepository opptjeningRepository = repositoryProvider.getOpptjeningRepository();
    private final AksjonspunktutlederForVurderOppgittOpptjening aksjonspunktutlederForVurderOpptjening = new AksjonspunktutlederForVurderOppgittOpptjening(
        opptjeningRepository, iayTjeneste, virksomhetTjeneste);
    private VilkårResultatRepository vilkårResultatRepository = new VilkårResultatRepository(repoRule.getEntityManager());
    private AksjonspunktutlederForVurderBekreftetOpptjening apbOpptjening = new AksjonspunktutlederForVurderBekreftetOpptjening(
        repositoryProvider.getOpptjeningRepository(), iayTjeneste);
    private OpptjeningsperioderTjeneste forSaksbehandlingTjeneste = new OpptjeningsperioderTjeneste(iayTjeneste, repositoryProvider.getOpptjeningRepository(),
        aksjonspunktutlederForVurderOpptjening, apbOpptjening);
    private InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.nyRef();
    private AktørId AKTØRID = AktørId.dummy();

    @Test
    public void skal_utlede_opptjening_aktivitet_periode_uten_overstyrt() {
        // Arrange
        final Behandling behandling = opprettBehandling(skjæringstidspunkt);

        DatoIntervallEntitet periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt.minusMonths(2));
        DatoIntervallEntitet periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(2), skjæringstidspunkt.minusMonths(1));

        OppgittOpptjeningBuilder oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode2, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        final Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet(ORG_NUMMER);
        InntektArbeidYtelseAggregatBuilder bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode1,
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN, virksomhet);
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        InntektArbeidYtelseAggregatBuilder saksbehandlet = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.of(bekreftet.build()),
            VersjonType.SAKSBEHANDLET);

        iayTjeneste.lagreIayAggregat(behandling.getId(), saksbehandlet);

        // Act
        BehandlingReferanse behandlingRef = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        InntektArbeidYtelseGrunnlag iayGrunnlag = iayTjeneste.hentGrunnlag(behandling.getId());
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForBeregning(behandlingRef, iayGrunnlag);

        // Assert
        assertThat(perioder).hasSize(2);
        OpptjeningsperiodeForSaksbehandling saksbehandletPeriode = perioder.stream().filter(p -> p.getOpptjeningsnøkkel()
            .getArbeidsforholdRef().map(r -> r.gjelderFor(ARBEIDSFORHOLD_ID)).orElse(false)).findFirst().get();
        assertThat(saksbehandletPeriode.getPeriode()).isEqualTo(periode1);
    }

    @Test
    public void skal_sammenstille_grunnlag_og_overstyrt_deretter_utlede_opptjening_aktivitet_periode() {
        // Arrange
        final Behandling behandling = opprettBehandling(skjæringstidspunkt);

        DatoIntervallEntitet periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt.minusMonths(2));
        DatoIntervallEntitet periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(2), skjæringstidspunkt.minusMonths(1));
        DatoIntervallEntitet periode3 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(1), skjæringstidspunkt.minusMonths(0));

        OppgittOpptjeningBuilder oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode2, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        final Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet(ORG_NUMMER);
        InntektArbeidYtelseAggregatBuilder bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode1,
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN, virksomhet);
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        InntektArbeidYtelseAggregatBuilder saksbehandlet = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.of(bekreftet.build()),
            VersjonType.SAKSBEHANDLET);
        var ref = saksbehandlet.medNyInternArbeidsforholdRef(virksomhet, EksternArbeidsforholdRef.ref("1"));

        YrkesaktivitetBuilder yrkesaktivitetBuilder = saksbehandlet.getAktørArbeidBuilder(AKTØRID)
            .getYrkesaktivitetBuilderForNøkkelAvType(new Opptjeningsnøkkel(ref, null, null),
                ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        yrkesaktivitetBuilder
            .leggTilAktivitetsAvtale(yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(periode3.getFomDato(), periode3.getTomDato()))
                .medProsentsats(BigDecimal.TEN));

        iayTjeneste.lagreIayAggregat(behandling.getId(), saksbehandlet);

        // Act
        BehandlingReferanse behandlingRef = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(behandlingRef);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.GODKJENT)).collect(Collectors.toList())).hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.UNDERKJENT)).collect(Collectors.toList())).hasSize(1);
    }

    @Test
    public void skal_sammenstille_grunnlag_og_utlede_opptjening_aktivitet_periode() {
        // Arrange
        Behandling behandling = opprettBehandling(skjæringstidspunkt);

        DatoIntervallEntitet periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt.minusMonths(2));
        DatoIntervallEntitet periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(2), skjæringstidspunkt.minusMonths(1));

        OppgittOpptjeningBuilder oppgitt = OppgittOpptjeningBuilder.ny();

        OppgittOpptjeningBuilder.EgenNæringBuilder egenNæringBuilder = OppgittOpptjeningBuilder.EgenNæringBuilder.ny();
        egenNæringBuilder
            .medRegnskapsførerNavn("Larsen")
            .medRegnskapsførerTlf("TELEFON")
            .medVirksomhet(ORG_NUMMER)
            .medPeriode(periode2);

        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode2, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        oppgitt.leggTilEgneNæringer(List.of(egenNæringBuilder));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        InntektArbeidYtelseAggregatBuilder bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode1,
            ArbeidType.ORDINÆRT_ARBEIDSFORHOLD, BigDecimal.TEN, Arbeidsgiver.virksomhet(ORG_NUMMER));
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        // Act
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.GODKJENT)).collect(Collectors.toList())).hasSize(2);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.TIL_VURDERING)).collect(Collectors.toList())).hasSize(1);
        assertThat(perioder.stream().filter(OpptjeningsperiodeForSaksbehandling::getErManueltRegistrert).collect(Collectors.toList())).isEmpty();
        assertThat(perioder.stream().filter(o -> !o.getErManueltRegistrert()).collect(Collectors.toList())).hasSize(3);
    }

    @Test
    public void skal_utlede_om_en_periode_er_blitt_endret() {
        // Arrange
        final Behandling behandling = opprettBehandling(skjæringstidspunkt);

        DatoIntervallEntitet periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt.minusMonths(2));
        DatoIntervallEntitet periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(2), skjæringstidspunkt.minusMonths(1));

        OppgittOpptjeningBuilder oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode2, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        InntektArbeidYtelseAggregatBuilder saksbehandlet = opprettOverstyrtOppgittOpptjening(periode1,
            ArbeidType.MILITÆR_ELLER_SIVILTJENESTE, AKTØRID, VersjonType.SAKSBEHANDLET);
        iayTjeneste.lagreIayAggregat(behandling.getId(), saksbehandlet);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        // Act
        // Assert
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref)
            .stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE)).collect(Collectors.toList());

        assertThat(perioder).hasSize(1);
        assertThat(perioder.get(0).getErPeriodeEndret()).isTrue();
        assertThat(perioder.get(0).getBegrunnelse()).isNotEmpty();
    }

    @Test
    public void skal_returnere_oat_frilans_ved_bekreftet_frilans() {
        // Arrange
        final Behandling behandling = opprettBehandling(skjæringstidspunkt);

        final Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORG_NUMMER);

        LocalDate fraOgMed = LocalDate.now().minusMonths(4);
        LocalDate tilOgMed = LocalDate.now().minusMonths(3);
        DatoIntervallEntitet periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);

        InntektArbeidYtelseAggregatBuilder bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode1,
            ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER, BigDecimal.TEN, arbeidsgiver);
        opprettInntektForFrilanser(bekreftet, AKTØRID, ARBEIDSFORHOLD_ID, periode1, arbeidsgiver);
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        opptjeningRepository.lagreOpptjeningsperiode(behandling, LocalDate.now().minusMonths(10), LocalDate.now().minusDays(1), false);

        // Act 1
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).collect(Collectors.toList()))
            .hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.TIL_VURDERING)).collect(Collectors.toList())).hasSize(1);
        assertThat(perioder.stream().filter(OpptjeningsperiodeForSaksbehandling::getErManueltRegistrert).collect(Collectors.toList())).isEmpty();
        assertThat(perioder.stream().filter(o -> !o.getErManueltRegistrert()).collect(Collectors.toList())).hasSize(1);

        // Act 2
        InntektArbeidYtelseAggregatBuilder saksbehandlet = opprettOverstyrtOppgittOpptjening(periode1,
            ArbeidType.MILITÆR_ELLER_SIVILTJENESTE, AKTØRID, VersjonType.SAKSBEHANDLET);
        iayTjeneste.lagreIayAggregat(behandling.getId(), saksbehandlet);
        perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).collect(Collectors.toList()))
            .hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.UNDERKJENT)).collect(Collectors.toList())).hasSize(1);
        assertThat(perioder.stream().filter(OpptjeningsperiodeForSaksbehandling::getErManueltRegistrert).collect(Collectors.toList())).hasSize(1);
    }

    @Test
    public void skal_sette_manuelt_behandlet_ved_underkjent_frilans() {
        // Arrange
        final Behandling behandling = opprettBehandling(skjæringstidspunkt);

        final Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORG_NUMMER);

        LocalDate fraOgMed = LocalDate.now().minusMonths(4);
        LocalDate tilOgMed = LocalDate.now().minusMonths(3);
        DatoIntervallEntitet periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);

        InntektArbeidYtelseAggregatBuilder bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode1,
            ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER, BigDecimal.TEN, arbeidsgiver);
        opprettInntektForFrilanser(bekreftet, AKTØRID, ARBEIDSFORHOLD_ID, periode1, arbeidsgiver);
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        OppgittOpptjeningBuilder oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode1, ArbeidType.FRILANSER));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);


        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        opptjeningRepository.lagreOpptjeningsperiode(behandling, LocalDate.now().minusMonths(10), LocalDate.now().minusDays(1), false);

        // Act 1
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).collect(Collectors.toList()))
            .hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.TIL_VURDERING)).collect(Collectors.toList())).hasSize(1);
        assertThat(perioder.stream().filter(OpptjeningsperiodeForSaksbehandling::getErManueltRegistrert).collect(Collectors.toList())).isEmpty();
        assertThat(perioder.stream().filter(o -> !o.getErManueltRegistrert()).collect(Collectors.toList())).hasSize(1);

        // Act 2
        InntektArbeidYtelseAggregatBuilder saksbehandlet = InntektArbeidYtelseAggregatBuilder
            .oppdatere(Optional.empty(), VersjonType.SAKSBEHANDLET);
        iayTjeneste.lagreIayAggregat(behandling.getId(), saksbehandlet);
        perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).collect(Collectors.toList()))
            .hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.UNDERKJENT)).collect(Collectors.toList())).hasSize(1);
        OpptjeningsperiodeForSaksbehandling frilansPeriode = perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).findFirst().get();
        assertThat(frilansPeriode.erManueltBehandlet()).isTrue();
    }


    @Test
    public void skal_returnere_oat_frilans_ved_bekreftet_frilans_for_vilkår() {
        // Arrange
        final Behandling behandling = opprettBehandling(skjæringstidspunkt);

        final Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(ORG_NUMMER);

        LocalDate fraOgMed = LocalDate.now().minusMonths(4);
        LocalDate tilOgMed = LocalDate.now().minusMonths(3);
        DatoIntervallEntitet periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);

        InntektArbeidYtelseAggregatBuilder bekreftet = opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AKTØRID, ARBEIDSFORHOLD_ID, periode1,
            ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER, BigDecimal.TEN, arbeidsgiver);
        opprettInntektForFrilanser(bekreftet, AKTØRID, ARBEIDSFORHOLD_ID, periode1, arbeidsgiver);
        iayTjeneste.lagreIayAggregat(behandling.getId(), bekreftet);

        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        opptjeningRepository.lagreOpptjeningsperiode(behandling, LocalDate.now().minusMonths(10), LocalDate.now().minusDays(1), false);

        // Act 1
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(ref, skjæringstidspunkt);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).collect(Collectors.toList()))
            .hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.FERDIG_VURDERT_UNDERKJENT)).collect(Collectors.toList()))
            .hasSize(1);

        // Act 2
        InntektArbeidYtelseAggregatBuilder saksbehandlet = opprettOverstyrtOppgittOpptjening(periode1, ArbeidType.FRILANSER, AKTØRID,
            VersjonType.SAKSBEHANDLET);
        iayTjeneste.lagreIayAggregat(behandling.getId(), saksbehandlet);
        perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(ref, skjæringstidspunkt);

        // Assert
        assertThat(perioder.stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.FRILANS)).collect(Collectors.toList()))
            .hasSize(1);
        assertThat(perioder.stream().filter(p -> p.getVurderingsStatus().equals(VurderingsStatus.FERDIG_VURDERT_GODKJENT)).collect(Collectors.toList()))
            .hasSize(1);
    }

    @Test
    public void skal_returnere_en_periode_med_fiktivt_bekreftet_arbeidsforhold() {
        // Arrange
        final Behandling behandling = opprettBehandling(skjæringstidspunkt);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        LocalDate fraOgMed = LocalDate.of(2015, 1, 4);
        LocalDate tilOgMed = skjæringstidspunkt.plusMonths(2);
        DatoIntervallEntitet periode = DatoIntervallEntitet.fraOgMedTilOgMed(fraOgMed, tilOgMed);
        InntektArbeidYtelseAggregatBuilder saksbehandlet = lagFiktivtArbeidsforholdSaksbehandlet(periode);
        iayTjeneste.lagreIayAggregat(behandling.getId(), saksbehandlet);
        ArbeidsforholdInformasjonBuilder informasjon = lagFiktivtArbeidsforholdOverstyring(fraOgMed, tilOgMed);
        iayTjeneste.lagreArbeidsforhold(behandling.getId(), AKTØRID, informasjon);

        // Act
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref);

        // Assert
        assertThat(perioder.size()).isEqualTo(1);
    }

    @Test
    public void skal_kunne_bygge_opptjeninsperiode_basert_på_arbeidsforhold_lagt_til_avsaksbehandler() {
        final Behandling behandling = opprettBehandling(skjæringstidspunkt);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        LocalDate start = LocalDate.now().minusMonths(5);

        ArbeidsforholdInformasjonBuilder arbeidsforholdInformasjonBuilder = ArbeidsforholdInformasjonBuilder.builder(Optional.empty());
        Arbeidsgiver virksomhet = Arbeidsgiver.virksomhet("912471691");

        ArbeidsforholdOverstyringBuilder arbeidsforholdOverstyringBuilder = arbeidsforholdInformasjonBuilder.getOverstyringBuilderFor(virksomhet, null);
        arbeidsforholdOverstyringBuilder.medHandling(ArbeidsforholdHandlingType.BASERT_PÅ_INNTEKTSMELDING);
        arbeidsforholdOverstyringBuilder.leggTilOverstyrtPeriode(start, LocalDate.MAX);
        arbeidsforholdOverstyringBuilder.medAngittStillingsprosent(Stillingsprosent.ZERO);
        arbeidsforholdInformasjonBuilder.leggTil(arbeidsforholdOverstyringBuilder);

        iayTjeneste.lagreArbeidsforhold(behandling.getId(), AKTØRID, arbeidsforholdInformasjonBuilder);


        // Act
        @SuppressWarnings("unused")
        List<OpptjeningsperiodeForSaksbehandling> perioder = forSaksbehandlingTjeneste.hentRelevanteOpptjeningAktiveterForSaksbehandling(ref);

    }

    private ArbeidsforholdInformasjonBuilder lagFiktivtArbeidsforholdOverstyring(LocalDate fraOgMed, LocalDate tilOgMed) {
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        return ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty())
            .leggTil(ArbeidsforholdOverstyringBuilder.oppdatere(Optional.empty())
                .medArbeidsgiver(arbeidsgiver)
                .medHandling(ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER)
                .leggTilOverstyrtPeriode(fraOgMed, tilOgMed)
                .medAngittStillingsprosent(new Stillingsprosent(BigDecimal.valueOf(100))));
    }

    private InntektArbeidYtelseAggregatBuilder lagFiktivtArbeidsforholdSaksbehandlet(DatoIntervallEntitet periode) {
        Arbeidsgiver arbeidsgiver = Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG);
        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.SAKSBEHANDLET);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(AKTØRID);
        YrkesaktivitetBuilder yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(
            new Opptjeningsnøkkel(null, OrgNummer.KUNSTIG_ORG, null), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();
        AktivitetsAvtaleBuilder aktivitetsAvtale = aktivitetsAvtaleBuilder
            .medPeriode(periode)
            .medProsentsats(BigDecimal.valueOf(100))
            .medBeskrivelse("Ser greit ut");
        AktivitetsAvtaleBuilder ansettelsesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(periode);
        yrkesaktivitetBuilder
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(arbeidsgiver)
            .medArbeidsforholdId(null)
            .leggTilAktivitetsAvtale(aktivitetsAvtale)
            .leggTilAktivitetsAvtale(ansettelsesperiode);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeid = aktørArbeidBuilder
            .leggTilYrkesaktivitet(yrkesaktivitetBuilder);
        builder.leggTilAktørArbeid(aktørArbeid);
        return builder;
    }


    private InntektArbeidYtelseAggregatBuilder opprettOverstyrtOppgittOpptjening(DatoIntervallEntitet periode, ArbeidType type, AktørId aktørId,
                                                                                 VersjonType register) {
        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder
            .oppdatere(Optional.empty(), register);

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(aktørId);
        YrkesaktivitetBuilder yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForType(type);

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();

        AktivitetsAvtaleBuilder aktivitetsAvtale = aktivitetsAvtaleBuilder
            .medPeriode(periode)
            .medBeskrivelse("Ser greit ut");

        yrkesaktivitetBuilder
            .medArbeidType(type)
            .leggTilAktivitetsAvtale(aktivitetsAvtale);
        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeid = aktørArbeidBuilder
            .leggTilYrkesaktivitet(yrkesaktivitetBuilder);

        builder.leggTilAktørArbeid(aktørArbeid);

        return builder;
    }

    private Behandling opprettBehandling(LocalDate skjæringstidspunkt) {
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, AKTØRID);
        @SuppressWarnings("unused")
        Long fagsakId = fagsakRepository.opprettNy(fagsak);
        final Behandling.Builder builder = Behandling.forFørstegangssøknad(fagsak);
        final Behandling behandling = builder.build();
        final Vilkårene nyttResultat = Vilkårene.builder().build();

        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        vilkårResultatRepository.lagre(behandling.getId(), nyttResultat);
        return behandling;
    }

    private InntektArbeidYtelseAggregatBuilder opprettInntektArbeidYtelseAggregatForYrkesaktivitet(AktørId aktørId, InternArbeidsforholdRef ref,
                                                                                                   DatoIntervallEntitet periode, ArbeidType type,
                                                                                                   BigDecimal prosentsats, Arbeidsgiver virksomhet1) {
        InntektArbeidYtelseAggregatBuilder builder = InntektArbeidYtelseAggregatBuilder.oppdatere(Optional.empty(), VersjonType.REGISTER);

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeidBuilder = builder.getAktørArbeidBuilder(aktørId);
        YrkesaktivitetBuilder yrkesaktivitetBuilder = aktørArbeidBuilder.getYrkesaktivitetBuilderForNøkkelAvType(
            new Opptjeningsnøkkel(ref, virksomhet1.getIdentifikator(), null), type);

        AktivitetsAvtaleBuilder aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder();
        PermisjonBuilder permisjonBuilder = yrkesaktivitetBuilder.getPermisjonBuilder();

        AktivitetsAvtaleBuilder aktivitetsAvtale = aktivitetsAvtaleBuilder
            .medPeriode(periode)
            .medProsentsats(prosentsats)
            .medBeskrivelse("Ser greit ut");
        AktivitetsAvtaleBuilder ansettelsesperiode = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(periode);

        Permisjon permisjon = permisjonBuilder
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

        InntektArbeidYtelseAggregatBuilder.AktørArbeidBuilder aktørArbeid = aktørArbeidBuilder
            .leggTilYrkesaktivitet(yrkesaktivitetBuilder);

        builder.leggTilAktørArbeid(aktørArbeid);

        return builder;
    }

    private void opprettInntektForFrilanser(InntektArbeidYtelseAggregatBuilder bekreftet, AktørId aktørId, InternArbeidsforholdRef ref, DatoIntervallEntitet periode,
                                            Arbeidsgiver virksomhet1) {
        InntektArbeidYtelseAggregatBuilder.AktørInntektBuilder ainntektBuilder = bekreftet.getAktørInntektBuilder(aktørId);
        InntektBuilder inntektBuilder = ainntektBuilder.getInntektBuilder(InntektsKilde.INNTEKT_OPPTJENING,
            new Opptjeningsnøkkel(ref, virksomhet1.getIdentifikator(), null));
        inntektBuilder.medArbeidsgiver(virksomhet1);
        inntektBuilder.leggTilInntektspost(InntektspostBuilder.ny().medInntektspostType(InntektspostType.LØNN)
            .medPeriode(periode.getFomDato(), periode.getTomDato()).medBeløp(BigDecimal.TEN));
        ainntektBuilder.leggTilInntekt(inntektBuilder);
        bekreftet.leggTilAktørInntekt(ainntektBuilder);
    }

}
