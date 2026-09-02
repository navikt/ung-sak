package no.nav.ung.ytelse.aktivitetspenger.mottak;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.kontrakt.vilkår.VilkårUtfallSamlet;
import no.nav.ung.sak.vilkår.VilkårTjeneste;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;

@Dependent
public class VirkingstidspunktUtleder {

    private BehandlingRepository behandlingRepository;
    private VilkårTjeneste vilkårTjeneste;

    @Inject
    public VirkingstidspunktUtleder(BehandlingRepository behandlingRepository, VilkårTjeneste vilkårTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårTjeneste = vilkårTjeneste;
    }

    public LocalDate utledVirkingstidspunkt(LocalDate søknadFomDato, long behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Optional<Behandling> sisteAvsluttedeBehandling = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteYtelsebehandling(behandling.getFagsakId());
        LocalDateTimeline<VilkårUtfallSamlet> tidslinjeInnvilgetVedtak = sisteAvsluttedeBehandling.map(avsluttetBehandling -> vilkårTjeneste.samletVilkårsresultat(avsluttetBehandling.getId()).filterValue(utfall -> utfall.getSamletUtfall() == Utfall.OPPFYLT)).orElse(LocalDateTimeline.empty());
        LocalDate sisteInnvilgedeDato = tidslinjeInnvilgetVedtak.isEmpty() ? null : tidslinjeInnvilgetVedtak.getMaxLocalDate();
        return utledVirkingstidspunkt(søknadFomDato, sisteInnvilgedeDato);
    }

    static LocalDate utledVirkingstidspunkt(LocalDate søknadFomDato, LocalDate sisteInnvilgedeDato) {
        LocalDate justertVirkningstidspunkt;
        if (sisteInnvilgedeDato == null || sisteInnvilgedeDato.isBefore(søknadFomDato)){
            justertVirkningstidspunkt = søknadFomDato;
        } else {
            justertVirkningstidspunkt = sisteInnvilgedeDato.plusDays(1);
        }
        while (justertVirkningstidspunkt.getDayOfWeek() == DayOfWeek.SATURDAY || justertVirkningstidspunkt.getDayOfWeek() == DayOfWeek.SUNDAY) {
            justertVirkningstidspunkt = justertVirkningstidspunkt.plusDays(1);
        }
        return justertVirkningstidspunkt;
    }
}
