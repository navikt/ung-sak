package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak.kjøreplan;

import java.util.List;
import java.util.Map;
import java.util.NavigableSet;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Saksnummer;

public class KravPrioInput {

    private final long aktuellFagsakId;
    private final Saksnummer aktuellSak;
    private final Map<Long, NavigableSet<DatoIntervallEntitet>> utsattePerioderPerBehandling;
    private final LocalDateTimeline<Boolean> toOmsorgspersonerTidslinje;
    private final LocalDateTimeline<Boolean> tidslinjeMedFriKjøring;
    private final List<SakOgBehandlinger> sakOgBehandlinger;

    public KravPrioInput(long aktuellFagsakId, Saksnummer aktuellSak, Map<Long, NavigableSet<DatoIntervallEntitet>> utsattePerioderPerBehandling, LocalDateTimeline<Boolean> toOmsorgspersonerTidslinje, LocalDateTimeline<Boolean> tidslinjeMedFriKjøring, List<SakOgBehandlinger> sakOgBehandlinger) {
        this.aktuellFagsakId = aktuellFagsakId;
        this.aktuellSak = aktuellSak;
        this.utsattePerioderPerBehandling = utsattePerioderPerBehandling;
        this.toOmsorgspersonerTidslinje = toOmsorgspersonerTidslinje;
        this.tidslinjeMedFriKjøring = tidslinjeMedFriKjøring;
        this.sakOgBehandlinger = sakOgBehandlinger;
    }

    public Map<Long, NavigableSet<DatoIntervallEntitet>> getUtsattePerioderPerBehandling() {
        return utsattePerioderPerBehandling;
    }

    public LocalDateTimeline<Boolean> getToOmsorgspersonerTidslinje() {
        return toOmsorgspersonerTidslinje;
    }

    public LocalDateTimeline<Boolean> getTidslinjeMedFriKjøring() {
        return tidslinjeMedFriKjøring;
    }

    public List<SakOgBehandlinger> getSakOgBehandlinger() {
        return sakOgBehandlinger;
    }

    public long getAktuellFagsakId() {
        return aktuellFagsakId;
    }

    public Saksnummer getAktuellSak() {
        return aktuellSak;
    }
}
