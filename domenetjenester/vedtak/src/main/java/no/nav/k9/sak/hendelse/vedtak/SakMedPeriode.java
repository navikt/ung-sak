package no.nav.k9.sak.hendelse.vedtak;

import java.util.NavigableSet;
import java.util.Objects;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Saksnummer;

public class SakMedPeriode {

    private Saksnummer saksnummer;
    private FagsakYtelseType ytelseType;
    private NavigableSet<DatoIntervallEntitet> perioder;

    private BehandlingÅrsakType behandlingÅrsakType;

    public SakMedPeriode(Saksnummer saksnummer, FagsakYtelseType ytelseType, NavigableSet<DatoIntervallEntitet> perioder, BehandlingÅrsakType behandlingÅrsakType) {
        this.saksnummer = Objects.requireNonNull(saksnummer);
        this.ytelseType = Objects.requireNonNull(ytelseType);
        this.perioder = perioder;
        this.behandlingÅrsakType = behandlingÅrsakType;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    public NavigableSet<DatoIntervallEntitet> getPerioder() {
        return perioder;
    }

    public BehandlingÅrsakType getBehandlingÅrsakType() {
        return behandlingÅrsakType;
    }

    @Override
    public String toString() {
        return saksnummer.toString() + "-" + ytelseType.getKode();
    }
}
