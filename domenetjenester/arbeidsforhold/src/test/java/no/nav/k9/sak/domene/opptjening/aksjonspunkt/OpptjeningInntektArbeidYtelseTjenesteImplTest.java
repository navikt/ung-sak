package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
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
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilter;
import no.nav.k9.sak.domene.opptjening.OppgittOpptjeningFilterProvider;
import no.nav.k9.sak.domene.opptjening.OpptjeningAktivitetPeriode;
import no.nav.k9.sak.domene.opptjening.OpptjeningInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.opptjening.VurderingsStatus;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class OpptjeningInntektArbeidYtelseTjenesteImplTest {

    public static final String NAV_ORG_NUMMER = "889640782";
    private InternArbeidsforholdRef ARBEIDSFORHOLD_ID = InternArbeidsforholdRef.nyRef();
    private AktørId AKTØRID = AktørId.dummy();
    private LocalDate skjæringstidspunkt = LocalDate.now();

    @Inject
    private EntityManager entityManager;

    private IAYRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private VilkårResultatRepository vilkårResultatRepository;
    private VirksomhetTjeneste virksomhetTjeneste = Mockito.mock(VirksomhetTjeneste.class);
    private OppgittOpptjeningFilterProvider oppgittOpptjeningFilterProvider = Mockito.mock(OppgittOpptjeningFilterProvider.class);
    private OpptjeningsperioderTjeneste opptjeningsperioderTjeneste;
    private OpptjeningInntektArbeidYtelseTjeneste opptjeningTjeneste;

    @BeforeEach
    public void setup() {
        repositoryProvider = new IAYRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        fagsakRepository = new FagsakRepository(entityManager);
        vilkårResultatRepository = new VilkårResultatRepository(entityManager);
        opptjeningsperioderTjeneste = new OpptjeningsperioderTjeneste(repositoryProvider.getOpptjeningRepository(), oppgittOpptjeningFilterProvider);
        opptjeningTjeneste = new OpptjeningInntektArbeidYtelseTjeneste(iayTjeneste, repositoryProvider.getOpptjeningRepository(), opptjeningsperioderTjeneste);

        when(oppgittOpptjeningFilterProvider.finnOpptjeningFilter(anyLong())).thenReturn(new OppgittOpptjeningFilter() {
        });
    }

    @Test
    void skal_vurdere_oppgitt_periode_for_egen_næring_som_til_vurdering() {
        // Arrange
        final Behandling behandling = opprettBehandling();

        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(skjæringstidspunkt.minusMonths(3), skjæringstidspunkt.minusDays(1));

        Virksomhet virksomhet = new Virksomhet.Builder()
            .medOrgnr(NAV_ORG_NUMMER)
            .medNavn("Virksomheten")
            .medRegistrert(LocalDate.now())
            .medOppstart(LocalDate.now())
            .build();
        when(virksomhetTjeneste.finnOrganisasjon(NAV_ORG_NUMMER)).thenReturn(Optional.of(virksomhet));

        OppgittOpptjeningBuilder oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilEgneNæringer(Collections.singletonList(OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
            .medVirksomhet(NAV_ORG_NUMMER)
            .medPeriode(periode)
            .medRegnskapsførerNavn("Børre Larsen")
            .medRegnskapsførerTlf("TELEFON")
            .medBegrunnelse("Hva mer?")));

        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        repositoryProvider.getOpptjeningRepository().lagreOpptjeningsperiode(behandling, periode.getFomDato(), periode.getTomDato(), false);

        // Assert
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        List<OpptjeningAktivitetPeriode> perioder = opptjeningTjeneste.hentRelevanteOpptjeningAktiveterForVilkårVurdering(ref, List.of(DatoIntervallEntitet.fraOgMed(skjæringstidspunkt)))
            .firstEntry().getValue()
            .stream().filter(p -> p.getOpptjeningAktivitetType().equals(OpptjeningAktivitetType.NÆRING)).collect(Collectors.toList());

        assertThat(perioder).hasSize(1);
        OpptjeningAktivitetPeriode aktivitetPeriode = perioder.get(0);
        assertThat(aktivitetPeriode.getPeriode()).isEqualTo(periode);
        assertThat(aktivitetPeriode.getVurderingsStatus()).isEqualTo(VurderingsStatus.TIL_VURDERING);
    }

    private Behandling opprettBehandling() {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.OMSORGSPENGER, AKTØRID);
        fagsakRepository.opprettNy(fagsak);
        var builder = Behandling.forFørstegangssøknad(fagsak);
        Behandling behandling = builder.build();
        Vilkårene nyttResultat = Vilkårene.builder().build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        vilkårResultatRepository.lagre(behandling.getId(), nyttResultat);

        return behandling;
    }
}
