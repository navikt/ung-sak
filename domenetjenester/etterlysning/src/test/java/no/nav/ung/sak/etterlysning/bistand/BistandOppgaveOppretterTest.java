package no.nav.ung.sak.etterlysning.bistand;

import no.nav.ung.brukerdialog.kontrakt.oppgaver.OpprettOppgaveDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.bistand.BekreftBistandOppgavetypeDataDto;
import no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.bistand.BekreftBistandOpphørOppgavetypeDataDto;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.BistandsavklaringKildeType;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
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
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Opphør lagres som en lukket periode med {@code avklaringtype = OPPHØR} — det er avklaringtypen,
 * ikke en åpen tom-dato, som avgjør hvilken av de to DTO-ene bruker får.
 */
@ExtendWith(MockitoExtension.class)
class BistandOppgaveOppretterTest {

    private static final Long BEHANDLING_ID = 1L;
    private static final LocalDate FOM = LocalDate.of(2025, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2025, 1, 31);
    private static final AktørId AKTØR_ID = new AktørId("1234567890123");

    @Mock
    private UngBrukerdialogOppgaveKlient oppgaveKlient;
    @Mock
    private VilkårsavklaringGrunnlagRepository vilkårsavklaringGrunnlagRepository;
    @Mock
    private Behandling behandling;
    @Mock
    private Fagsak fagsak;

    private BistandOppgaveOppretter oppretter;

    @BeforeEach
    void setUp() {
        oppretter = new BistandOppgaveOppretter(oppgaveKlient, vilkårsavklaringGrunnlagRepository);
        when(behandling.getId()).thenReturn(BEHANDLING_ID);
        when(behandling.getFagsak()).thenReturn(fagsak);
        when(fagsak.getYtelseType()).thenReturn(FagsakYtelseType.AKTIVITETSPENGER);
        // Leses først når DTO-en faktisk bygges, og aldri i testene som forventer at det kastes.
        lenient().when(fagsak.getSaksnummer()).thenReturn(new Saksnummer("123"));
    }

    @Test
    void avslag_gir_periode_dto_med_tom() {
        var etterlysning = etterlysning();
        stubAvklaring(etterlysning.getGrunnlagsreferanse(), Avklaringtype.AVSLAG,
            BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK, "Ingen oppfølging registrert.",
            BistandsavklaringKildeType.BRUKER, null);

        oppretter.opprettOppgave(behandling, List.of(etterlysning), AKTØR_ID);

        var dto = capturerOppgave();
        assertThat(dto.oppgavetypeData()).isEqualTo(new BekreftBistandOppgavetypeDataDto(
            FOM, TOM,
            no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.bistand.BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK,
            "Ingen oppfølging registrert.",
            no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.bistand.BistandsavklaringKildeType.BRUKER, null));
        assertThat(dto.journalføring().saksnummer().getVerdi()).isEqualTo("123");
    }

    @Test
    void opphør_gir_opphørs_dto_uten_tom() {
        var etterlysning = etterlysning();
        stubAvklaring(etterlysning.getGrunnlagsreferanse(), Avklaringtype.OPPHØR,
            BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK, "Oppfølgingsvedtaket er avsluttet.",
            BistandsavklaringKildeType.ANNET, "veilederen din");

        oppretter.opprettOppgave(behandling, List.of(etterlysning), AKTØR_ID);

        var dto = capturerOppgave();
        assertThat(dto.oppgavetypeData()).isEqualTo(new BekreftBistandOpphørOppgavetypeDataDto(
            FOM,
            no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.bistand.BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK,
            "Oppfølgingsvedtaket er avsluttet.",
            no.nav.ung.brukerdialog.kontrakt.oppgaver.typer.bistand.BistandsavklaringKildeType.ANNET, "veilederen din"));
    }

    @Test
    void avkortet_skal_aldri_varsles_og_kaster() {
        var etterlysning = etterlysning();
        stubAvklaring(etterlysning.getGrunnlagsreferanse(), Avklaringtype.AVSLAG,
            BistandsvilkårIkkeOppfyltÅrsak.AVKORTET, null, BistandsavklaringKildeType.BRUKER, null);

        assertThatIllegalStateException()
            .isThrownBy(() -> oppretter.opprettOppgave(behandling, List.of(etterlysning), AKTØR_ID))
            .withMessageContaining("AVKORTET");
    }

    @Test
    void manglende_fritekst_ved_ikke_14a_vedtak_kaster() {
        var etterlysning = etterlysning();
        stubAvklaring(etterlysning.getGrunnlagsreferanse(), Avklaringtype.AVSLAG,
            BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK, null, BistandsavklaringKildeType.BRUKER, null);

        assertThatNullPointerException()
            .isThrownBy(() -> oppretter.opprettOppgave(behandling, List.of(etterlysning), AKTØR_ID))
            .withMessageContaining("IKKE_14A_VEDTAK");
    }

    @Test
    void ukjent_grunnlagsreferanse_kaster() {
        var etterlysning = etterlysning();
        stubAvklaring(UUID.randomUUID(), Avklaringtype.AVSLAG,
            BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK, "Fritekst.", BistandsavklaringKildeType.BRUKER, null);

        assertThatIllegalStateException()
            .isThrownBy(() -> oppretter.opprettOppgave(behandling, List.of(etterlysning), AKTØR_ID))
            .withMessageContaining("Fant ikke periodeAvklaring");
    }

    private OpprettOppgaveDto capturerOppgave() {
        var captor = ArgumentCaptor.forClass(OpprettOppgaveDto.class);
        verify(oppgaveKlient).opprettOppgave(captor.capture());
        return captor.getValue();
    }

    private void stubAvklaring(UUID referanse, Avklaringtype avklaringtype, BistandsvilkårIkkeOppfyltÅrsak årsak,
                                String fritekstTilVarsel, BistandsavklaringKildeType kilde, String kildeFritekst) {
        VilkårPeriodeAvklaring avklaring = new VilkårPeriodeAvklaringForeslått(
            referanse,
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            årsak.getKode(),
            "Begrunnelse.",
            true,
            fritekstTilVarsel,
            null,
            kilde,
            kildeFritekst,
            "Z999999",
            LocalDateTime.now(),
            avklaringtype);

        var grunnlag = mock(VilkårsavklaringGrunnlag.class);
        when(grunnlag.getForeslåtteAvklaringer()).thenReturn(Set.of(avklaring));
        when(vilkårsavklaringGrunnlagRepository.hentGrunnlagHvisEksisterer(BEHANDLING_ID, VilkårType.BISTANDSVILKÅR))
            .thenReturn(Optional.of(grunnlag));
    }

    private static Etterlysning etterlysning() {
        var etterlysning = Etterlysning.opprettForType(
            BEHANDLING_ID,
            UUID.randomUUID(),
            UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            EtterlysningType.UTTALELSE_BISTAND
        );
        etterlysning.vent(LocalDateTime.now().plusDays(1));
        return etterlysning;
    }
}
