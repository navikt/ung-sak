package no.nav.k9.sak.hendelse.vedtak;

import java.util.NavigableSet;
import java.util.Objects;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Saksnummer;

public class SakMedPeriode {

    private Saksnummer saksnummer;
    private NavigableSet<DatoIntervallEntitet> perioder;

    public SakMedPeriode(Saksnummer saksnummer, NavigableSet<DatoIntervallEntitet> perioder) {
        this.saksnummer = Objects.requireNonNull(saksnummer);
        this.perioder = perioder;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public NavigableSet<DatoIntervallEntitet> getPerioder() {
        return perioder;
    }
}
