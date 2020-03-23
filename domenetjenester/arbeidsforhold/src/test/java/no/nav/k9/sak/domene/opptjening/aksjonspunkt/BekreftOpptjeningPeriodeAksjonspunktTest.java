package no.nav.k9.sak.domene.opptjening.aksjonspunkt;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.behandlingslager.virksomhet.VirksomhetEntitet;
import no.nav.k9.sak.behandlingslager.virksomhet.VirksomhetRepository;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtale;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyringBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittAnnenAktivitet;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.opptjening.BekreftOpptjeningPeriodeDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;
import no.nav.k9.sak.typer.OrgNummer;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Stillingsprosent;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class BekreftOpptjeningPeriodeAksjonspunktTest {
    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private BehandlingRepository behandlingRepository = new BehandlingRepository(repoRule.getEntityManager());
    private FagsakRepository fagsakRepository = new FagsakRepository(repoRule.getEntityManager());
    private VirksomhetRepository virksomhetRepository = new VirksomhetRepository();
    private VirksomhetTjeneste tjeneste;

    private BekreftOpptjeningPeriodeAksjonspunkt bekreftOpptjeningPeriodeAksjonspunkt;

    private AktørId AKTØRID = AktørId.dummy();
    private InntektArbeidYtelseTjeneste iayTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
    private final AksjonspunktutlederForVurderOppgittOpptjening vurderOpptjening = mock(AksjonspunktutlederForVurderOppgittOpptjening.class);

    @Before
    public void oppsett() {
        tjeneste = mock(VirksomhetTjeneste.class);
        VirksomhetEntitet.Builder builder = new VirksomhetEntitet.Builder();
        VirksomhetEntitet børreAs = builder.medOrgnr("23948923849283")
            .oppdatertOpplysningerNå()
            .medNavn("Børre AS")
            .build();
        virksomhetRepository.lagre(børreAs);
        Mockito.when(tjeneste.finnOrganisasjon(Mockito.any())).thenReturn(Optional.of(børreAs));
        bekreftOpptjeningPeriodeAksjonspunkt = new BekreftOpptjeningPeriodeAksjonspunkt(iayTjeneste, vurderOpptjening);
    }

    @Test
    public void skal_lagre_ned_bekreftet_kunstig_arbeidsforhold() {
        LocalDate iDag = LocalDate.now();
        final Behandling behandling = opprettBehandling(iDag);

        DatoIntervallEntitet periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.minusMonths(3), iDag.minusMonths(2));

        iayTjeneste.lagreArbeidsforhold(behandling.getId(), AKTØRID, ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty())
        .leggTil(ArbeidsforholdOverstyringBuilder
            .oppdatere(Optional.empty())
            .leggTilOverstyrtPeriode(periode1.getFomDato(), periode1.getTomDato())
            .medAngittStillingsprosent(new Stillingsprosent(100))
            .medArbeidsforholdRef(InternArbeidsforholdRef.nullRef())
            .medArbeidsgiver(Arbeidsgiver.virksomhet(OrgNummer.KUNSTIG_ORG))
            .medAngittArbeidsgiverNavn("Ambassade")));

        // simulerer svar fra GUI
        BekreftOpptjeningPeriodeDto dto = new BekreftOpptjeningPeriodeDto();
        dto.setAktivitetType(OpptjeningAktivitetType.ARBEID);
        dto.setArbeidsforholdRef(InternArbeidsforholdRef.nullRef().getReferanse());
        dto.setArbeidsgiverNavn("Ambassade");
        dto.setArbeidsgiverIdentifikator(OrgNummer.KUNSTIG_ORG);
        dto.setOriginalTom(periode1.getTomDato());
        dto.setOriginalFom(periode1.getFomDato());
        dto.setOpptjeningFom(periode1.getFomDato());
        dto.setOpptjeningTom(periode1.getTomDato());
        dto.setErGodkjent(true);
        dto.setErEndret(false);
        dto.setBegrunnelse("Ser greit ut");


        Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(iDag).build();

        //Act
        bekreftOpptjeningPeriodeAksjonspunkt.oppdater(behandling.getId(), behandling.getAktørId(), Collections.singletonList(dto), skjæringstidspunkt);

        InntektArbeidYtelseGrunnlag grunnlag = hentGrunnlag(behandling);
        assertThat(grunnlag.getBekreftetAnnenOpptjening(behandling.getAktørId())).isPresent();

        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getBekreftetAnnenOpptjening(behandling.getAktørId()));
        Collection<Yrkesaktivitet> yrkesaktiviteter = filter.getYrkesaktiviteter();

        assertThat(yrkesaktiviteter).hasSize(1);
        final List<DatoIntervallEntitet> perioder = filter.getAktivitetsAvtalerForArbeid().stream().map(AktivitetsAvtale::getPeriode).collect(Collectors.toList());
        assertThat(perioder).contains(periode1);
    }


    @Test
    public void skal_lagre_ned_bekreftet_aksjonspunkt() {
        LocalDate iDag = LocalDate.now();
        final Behandling behandling = opprettBehandling(iDag);

        DatoIntervallEntitet periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.minusMonths(3), iDag.minusMonths(2));
        DatoIntervallEntitet periode1_2 = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.minusMonths(2), iDag.minusMonths(1));

        OppgittOpptjeningBuilder oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode1, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode1, ArbeidType.MILITÆR_ELLER_SIVILTJENESTE));
        oppgitt.leggTilAnnenAktivitet(new OppgittAnnenAktivitet(periode1_2, ArbeidType.ETTERLØNN_SLUTTPAKKE));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        // simulerer svar fra GUI
        DatoIntervallEntitet periode2 = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.minusMonths(2), iDag.minusMonths(1));
        BekreftOpptjeningPeriodeDto dto = new BekreftOpptjeningPeriodeDto();
        dto.setAktivitetType(OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE);
        dto.setOriginalTom(periode1.getTomDato());
        dto.setOriginalFom(periode1.getFomDato());
        dto.setOpptjeningFom(periode2.getFomDato());
        dto.setOpptjeningTom(periode2.getTomDato());
        dto.setErGodkjent(true);
        dto.setErEndret(true);
        dto.setBegrunnelse("Ser greit ut");
        BekreftOpptjeningPeriodeDto dto2 = new BekreftOpptjeningPeriodeDto();
        dto2.setAktivitetType(OpptjeningAktivitetType.ETTERLØNN_SLUTTPAKKE);
        dto2.setOpptjeningFom(periode1_2.getFomDato());
        dto2.setOpptjeningTom(periode1_2.getTomDato());
        dto2.setOriginalFom(periode1_2.getFomDato());
        dto2.setOriginalTom(periode1_2.getTomDato());
        dto2.setErGodkjent(false);
        dto2.setBegrunnelse("Ser greit ut");
        dto2.setArbeidsgiverIdentifikator("test");
        dto2.setArbeidsgiverNavn("test");
        BekreftOpptjeningPeriodeDto dto3 = new BekreftOpptjeningPeriodeDto();
        dto3.setAktivitetType(OpptjeningAktivitetType.MILITÆR_ELLER_SIVILTJENESTE);
        dto3.setOriginalTom(periode1.getTomDato());
        dto3.setOriginalFom(periode1.getFomDato());
        dto3.setOpptjeningFom(periode1.getFomDato());
        dto3.setOpptjeningTom(periode1.getTomDato());
        dto3.setErGodkjent(true);
        dto3.setBegrunnelse("Ser greit ut");

        Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(iDag).build();

        //Act
        bekreftOpptjeningPeriodeAksjonspunkt.oppdater(behandling.getId(), behandling.getAktørId(), asList(dto, dto2, dto3), skjæringstidspunkt);

        InntektArbeidYtelseGrunnlag grunnlag = hentGrunnlag(behandling);
        assertThat(grunnlag.getBekreftetAnnenOpptjening(behandling.getAktørId())).isPresent();

        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getBekreftetAnnenOpptjening(behandling.getAktørId()));
        Collection<Yrkesaktivitet> yrkesaktiviteter = filter.getYrkesaktiviteter();

        assertThat(yrkesaktiviteter).hasSize(1);
        final List<DatoIntervallEntitet> perioder = filter.getAktivitetsAvtalerForArbeid().stream().map(AktivitetsAvtale::getPeriode).collect(Collectors.toList());
        assertThat(perioder).contains(periode1, periode2);
    }

    @Test
    public void skal_lagre_endring_i_periode_for_egen_næring() {
        LocalDate iDag = LocalDate.now();
        final Behandling behandling = opprettBehandling(iDag);

        when(vurderOpptjening.girAksjonspunktForOppgittNæring(any(), any(), any(), any())).thenReturn(true);
        DatoIntervallEntitet periode1 = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.minusMonths(3), iDag.minusMonths(2));
        DatoIntervallEntitet periode1_2 = DatoIntervallEntitet.fraOgMedTilOgMed(iDag.minusMonths(2), iDag.minusMonths(2));

        OppgittOpptjeningBuilder oppgitt = OppgittOpptjeningBuilder.ny();
        oppgitt.leggTilEgneNæringer(asList(OppgittOpptjeningBuilder.EgenNæringBuilder.ny()
            .medPeriode(periode1)));
        iayTjeneste.lagreOppgittOpptjening(behandling.getId(), oppgitt);

        BekreftOpptjeningPeriodeDto dto = new BekreftOpptjeningPeriodeDto();
        dto.setAktivitetType(OpptjeningAktivitetType.NÆRING);
        dto.setOriginalTom(periode1.getTomDato());
        dto.setOriginalFom(periode1.getFomDato());
        dto.setOpptjeningFom(periode1_2.getFomDato());
        dto.setOpptjeningTom(periode1_2.getTomDato());
        dto.setErGodkjent(true);
        dto.setErEndret(true);
        dto.setBegrunnelse("Ser greit ut");

        Skjæringstidspunkt skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(iDag).build();

        //Act
        bekreftOpptjeningPeriodeAksjonspunkt.oppdater(behandling.getId(), behandling.getAktørId(), asList(dto), skjæringstidspunkt);
        InntektArbeidYtelseGrunnlag grunnlag = hentGrunnlag(behandling);
        assertThat(grunnlag.getBekreftetAnnenOpptjening(behandling.getAktørId())).isPresent();

        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), grunnlag.getBekreftetAnnenOpptjening(behandling.getAktørId()));
        Collection<Yrkesaktivitet> yrkesaktiviteter = filter.getYrkesaktiviteter();

        assertThat(yrkesaktiviteter).hasSize(1);
        AktivitetsAvtale aktivitetsAvtale = filter.getAktivitetsAvtalerForArbeid().iterator().next();
        assertThat(DatoIntervallEntitet.fraOgMedTilOgMed(aktivitetsAvtale.getPeriode().getFomDato(), aktivitetsAvtale.getPeriode().getTomDato())).isEqualTo(periode1_2);
    }

    private InntektArbeidYtelseGrunnlag hentGrunnlag(final Behandling behandling) {
        return iayTjeneste.finnGrunnlag(behandling.getId()).orElseThrow();
    }

    private Behandling opprettBehandling(LocalDate iDag) {
        final Personinfo personinfo = new Personinfo.Builder()
            .medNavn("Navn navnesen")
            .medAktørId(AKTØRID)
            .medFødselsdato(iDag.minusYears(20))
            .medLandkode(Landkoder.NOR)
            .medKjønn(NavBrukerKjønn.KVINNE)
            .medPersonIdent(new PersonIdent("12312312312"))
            .medForetrukketSpråk(Språkkode.nb)
            .build();
        final Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, personinfo.getAktørId());
        fagsakRepository.opprettNy(fagsak);
        final Behandling.Builder builder = Behandling.forFørstegangssøknad(fagsak);
        final Behandling behandling = builder.build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }
}
