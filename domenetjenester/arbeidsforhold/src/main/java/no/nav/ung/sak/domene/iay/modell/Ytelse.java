package no.nav.ung.sak.domene.iay.modell;

import no.nav.ung.kodeverk.Fagsystem;
import no.nav.ung.kodeverk.api.IndexKey;
import no.nav.ung.kodeverk.arbeidsforhold.RelatertYtelseTilstand;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import no.nav.ung.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Saksnummer;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Ytelse implements IndexKey {


    private YtelseGrunnlag ytelseGrunnlag;

    private FagsakYtelseType relatertYtelseType = FagsakYtelseType.UDEFINERT;

    @ChangeTracked
    private DatoIntervallEntitet periode;

    @ChangeTracked
    private RelatertYtelseTilstand status;

    /**
     * Saksnummer (fra Arena, Infotrygd, ..).
     */
    private Saksnummer saksnummer;

    @ChangeTracked
    private Fagsystem kilde;

    @ChangeTracked
    private Set<YtelseAnvist> ytelseAnvist = new LinkedHashSet<>();

    public Ytelse() {
        // hibernate
    }

    public Ytelse(Ytelse ytelse) {
        this.relatertYtelseType = ytelse.getYtelseType();
        this.status = ytelse.getStatus();
        this.periode = ytelse.getPeriode();
        this.saksnummer = ytelse.getSaksnummer();
        this.kilde = ytelse.getKilde();
        ytelse.getYtelseGrunnlag().ifPresent(yg -> this.ytelseGrunnlag = new YtelseGrunnlag(yg));
        this.ytelseAnvist = ytelse.getYtelseAnvist()
            .stream()
            .map(YtelseAnvist::new)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = {periode, relatertYtelseType, saksnummer};
        return IndexKeyComposer.createKey(keyParts);
    }

    public FagsakYtelseType getYtelseType() {
        return relatertYtelseType;
    }

    void setYtelseType(FagsakYtelseType relatertYtelseType) {
        this.relatertYtelseType = relatertYtelseType;
    }

    public RelatertYtelseTilstand getStatus() {
        return status;
    }

    void setStatus(RelatertYtelseTilstand status) {
        this.status = status;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    void setPeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    public Saksnummer getSaksnummer() {
        return saksnummer;
    }

    void medSakId(Saksnummer saksnummer) {
        this.saksnummer = saksnummer;
    }

    public Fagsystem getKilde() {
        return kilde;
    }

    void setKilde(Fagsystem kilde) {
        this.kilde = kilde;
    }

    public Optional<YtelseGrunnlag> getYtelseGrunnlag() {
        return Optional.ofNullable(ytelseGrunnlag);
    }

    void setYtelseGrunnlag(YtelseGrunnlag ytelseGrunnlag) {
        if (ytelseGrunnlag != null) {
            this.ytelseGrunnlag = ytelseGrunnlag;
        }
    }

    public Collection<YtelseAnvist> getYtelseAnvist() {
        return Collections.unmodifiableCollection(ytelseAnvist);
    }

    void leggTilYtelseAnvist(YtelseAnvist ytelseAnvist) {
        this.ytelseAnvist.add(ytelseAnvist);

    }

    void tilbakestillAnvisteYtelser() {
        ytelseAnvist.clear();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof Ytelse that))
            return false;
        return Objects.equals(relatertYtelseType, that.relatertYtelseType) &&
            Objects.equals(periode, that.periode) &&
            Objects.equals(saksnummer, that.saksnummer) &&
            Objects.equals(kilde, that.kilde);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relatertYtelseType, periode, saksnummer, kilde);
    }

    @Override
    public String toString() {
        return "YtelseEntitet{" + //$NON-NLS-1$
            "relatertYtelseType=" + relatertYtelseType + //$NON-NLS-1$
            ", periode=" + periode + //$NON-NLS-1$
            ", relatertYtelseStatus=" + status + //$NON-NLS-1$
            ", saksNummer='" + saksnummer + '\'' + //$NON-NLS-1$
            '}';
    }

}
