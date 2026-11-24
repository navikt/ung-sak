package no.nav.ung.ytelse.aktivitetspenger.perioder;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.kontrakt.vilkår.VilkårUtfallSamlet;
import no.nav.ung.sak.test.util.Whitebox;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.vilkår.VilkårTjeneste;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AktivitetspengerKvalifiserteYtelsesperioderTjenesteTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 6, 30);

    private final VilkårTjeneste vilkårTjeneste = mock(VilkårTjeneste.class);
    private final BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
    private final AktivitetspengerKvalifiserteYtelsesperioderTjeneste tjeneste = new AktivitetspengerKvalifiserteYtelsesperioderTjeneste(vilkårTjeneste, behandlingRepository);

    @Test
    void kontrollbehandling_skal_bruke_oppfylte_perioder_fra_originalbehandlingen_som_initielle_perioder() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.AKTIVITETSPENGER, new AktørId("1234567890123"), new Saksnummer("ABC123"), FOM, TOM);
        var originalbehandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD).build();
        Whitebox.setInternalState(originalbehandling, "id", 1L);
        var kontrollbehandling = Behandling.fraTidligereBehandling(originalbehandling, BehandlingType.KONTROLL).build();
        Whitebox.setInternalState(kontrollbehandling, "id", 2L);
        when(behandlingRepository.hentBehandling(2L)).thenReturn(kontrollbehandling);
        when(vilkårTjeneste.samletVilkårsresultat(1L)).thenReturn(new LocalDateTimeline<>(FOM, TOM, new VilkårUtfallSamlet(Utfall.OPPFYLT, List.of())));

        var initiellePerioder = tjeneste.finnInitiellPeriodeTidslinje(2L);

        assertThat(initiellePerioder).isEqualTo(new LocalDateTimeline<>(FOM, TOM, true));
    }
}
