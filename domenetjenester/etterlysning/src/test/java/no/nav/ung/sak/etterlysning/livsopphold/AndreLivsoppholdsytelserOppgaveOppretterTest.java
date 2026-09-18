package no.nav.ung.sak.etterlysning.livsopphold;

import no.nav.ung.brukerdialog.kontrakt.oppgaver.OpprettOppgaveDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.livsopphold.BekreftAndreLivsoppholdsytelserOpphørOppgavetypeDataDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.livsopphold.BekreftAndreLivsoppholdsytelserOppgavetypeDataDto;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserAvklaringKildeType;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaring;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårPeriodeAvklaringForeslått;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlag;
import no.nav.ung.sak.behandlingslager.vilkårsavklaring.VilkårsavklaringGrunnlagRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.etterlysning.UngBrukerdialogOppgaveKlient;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AndreLivsoppholdsytelserOppgaveOppretterTest {

    private static final long BEHANDLING_ID = 1L;
    private static final LocalDate FOM = LocalDate.of(2026, 3, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 3, 31);

    @Mock
    private UngBrukerdialogOppgaveKlient oppgaveKlient;

    @Mock
    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;

    @Mock
    private Behandling behandling;

    @Mock
    private Fagsak fagsak;

    private AndreLivsoppholdsytelserOppgaveOppretter oppretter;

    @BeforeEach
    void setUp() {
        oppretter = new AndreLivsoppholdsytelserOppgaveOppretter(oppgaveKlient, vilkårsavklaringGrunnlagRepository);
        when(behandling.getId()).thenReturn(BEHANDLING_ID);
        when(behandling.getFagsak()).thenReturn(fagsak);
        when(fagsak.getYtelseType()).thenReturn(FagsakYtelseType.AKTIVITETSPENGER);
        lenient().when(fagsak.getSaksnummer()).thenReturn(new Saksnummer("123"));
    }

    @ParameterizedTest
    @EnumSource(value = AndreLivsoppholdsytelserIkkeOppfyltÅrsak.class, mode = EnumSource.Mode.EXCLUDE, names = {"AVKORTET", "UDEFINERT"})
    void oppretter_avslagsoppgave_for_hver_årsak(AndreLivsoppholdsytelserIkkeOppfyltÅrsak årsak) {
        var etterlysning = opprettEtterlysning();
        stubAvklaring(etterlysning, årsak, AndreLivsoppholdsytelserAvklaringKildeType.NAV, Avklaringtype.AVSLAG);

        oppretter.opprettOppgave(behandling, List.of(etterlysning), new AktørId("1234567890123"));

        var data = (BekreftAndreLivsoppholdsytelserOppgavetypeDataDto) capturerOppgave().oppgavetypeData();
        assertThat(data.fom()).isEqualTo(FOM);
        assertThat(data.tom()).isEqualTo(TOM);
        assertThat(data.ikkeOppfyltÅrsak().name()).isEqualTo(årsak.name());
        assertThat(data.kilde()).isEqualTo(no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.livsopphold.AndreLivsoppholdsytelserAvklaringKildeType.NAV);
    }

    @Test // Brukerdialog bruker dto-typen til å dekorere. Må derfor mappe til to ulike dtoer basert på avklaringType.
    void oppretter_opphørsoppgave_uten_tom_når_avklaringtype_er_opphør() {
        var etterlysning = opprettEtterlysning();
        stubAvklaring(etterlysning, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_DAGPENGER,
            AndreLivsoppholdsytelserAvklaringKildeType.NAV, Avklaringtype.OPPHØR);

        oppretter.opprettOppgave(behandling, List.of(etterlysning), new AktørId("1234567890123"));

        var oppgave = capturerOppgave();
        var data = (BekreftAndreLivsoppholdsytelserOpphørOppgavetypeDataDto) oppgave.oppgavetypeData();
        assertThat(data.fom()).isEqualTo(FOM);
        assertThat(oppgave.journalføring().saksnummer().getVerdi()).isEqualTo("123");
    }

    @Test
    void tar_med_kildefritekst_når_kilden_er_annet() {
        var etterlysning = opprettEtterlysning();
        stubAvklaring(etterlysning, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_TILTAKSPENGER,
            AndreLivsoppholdsytelserAvklaringKildeType.ANNET, Avklaringtype.AVSLAG);

        oppretter.opprettOppgave(behandling, List.of(etterlysning), new AktørId("1234567890123"));

        var data = (BekreftAndreLivsoppholdsytelserOppgavetypeDataDto) capturerOppgave().oppgavetypeData();
        assertThat(data.kilde()).isEqualTo(no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.livsopphold.AndreLivsoppholdsytelserAvklaringKildeType.ANNET);
        assertThat(data.kildeFritekst()).isEqualTo("kildefritekst");
    }

    @Test
    void avviser_avkortet_som_varslingsårsak() {
        var etterlysning = opprettEtterlysning();
        stubAvklaring(etterlysning, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.AVKORTET,
            AndreLivsoppholdsytelserAvklaringKildeType.NAV, Avklaringtype.AVSLAG);

        assertThatThrownBy(() -> oppretter.opprettOppgave(behandling, List.of(etterlysning), new AktørId("1234567890123")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("AVKORTET");
        verifyNoInteractions(oppgaveKlient);
    }

    @Test
    void krever_fritekst_til_varsel_når_årsaken_krever_det() {
        var etterlysning = opprettEtterlysning();
        stubAvklaring(etterlysning, AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_ANNEN_YTELSE,
            AndreLivsoppholdsytelserAvklaringKildeType.NAV, Avklaringtype.AVSLAG, null);

        assertThatThrownBy(() -> oppretter.opprettOppgave(behandling, List.of(etterlysning), new AktørId("1234567890123")))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("FritekstTilVarsel");
        verifyNoInteractions(oppgaveKlient);
    }

    @Test
    void feiler_når_etterlysningen_ikke_peker_på_en_foreslått_avklaring() {
        var etterlysning = opprettEtterlysning();
        when(vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(BEHANDLING_ID, VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> oppretter.opprettOppgave(behandling, List.of(etterlysning), new AktørId("1234567890123")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(etterlysning.getGrunnlagsreferanse().toString());
    }

    private OpprettOppgaveDto capturerOppgave() {
        var captor = ArgumentCaptor.forClass(OpprettOppgaveDto.class);
        verify(oppgaveKlient).opprettOppgave(captor.capture());
        return captor.getValue();
    }

    private void stubAvklaring(Etterlysning etterlysning,
                               AndreLivsoppholdsytelserIkkeOppfyltÅrsak årsak,
                               AndreLivsoppholdsytelserAvklaringKildeType kilde,
                               Avklaringtype avklaringtype) {
        stubAvklaring(etterlysning, årsak, kilde, avklaringtype, "fritekst til varsel");
    }

    private void stubAvklaring(Etterlysning etterlysning,
                               AndreLivsoppholdsytelserIkkeOppfyltÅrsak årsak,
                               AndreLivsoppholdsytelserAvklaringKildeType kilde,
                               Avklaringtype avklaringtype,
                               String fritekstTilVarsel) {
        var avklaring = new VilkårPeriodeAvklaringForeslått(
            etterlysning.getGrunnlagsreferanse(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            årsak.getKode(),
            "begrunnelse for avklaring",
            true,
            fritekstTilVarsel,
            null,
            kilde,
            kilde.kreverFritekst() ? "kildefritekst" : null,
            "A111111",
            LocalDateTime.now(),
            avklaringtype);

        var grunnlag = mock(VilkårsavklaringGrunnlag.class);
        when(grunnlag.getForeslåtteAvklaringer()).thenReturn(Set.<VilkårPeriodeAvklaring>of(avklaring));
        when(vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(BEHANDLING_ID, VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR))
            .thenReturn(Optional.of(grunnlag));
    }

    private static Etterlysning opprettEtterlysning() {
        var etterlysning = Etterlysning.opprettForType(
            BEHANDLING_ID,
            UUID.randomUUID(),
            UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            EtterlysningType.UTTALELSE_ANDRE_LIVSOPPHOLDSYTELSER
        );
        etterlysning.vent(LocalDateTime.now().plusDays(14));
        return etterlysning;
    }
}
