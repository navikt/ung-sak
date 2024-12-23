package no.nav.ung.sak.domene.iay.modell;

import java.util.Objects;
import java.util.Optional;

import no.nav.ung.kodeverk.api.IndexKey;
import no.nav.ung.kodeverk.arbeidsforhold.InntektPeriodeType;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import no.nav.ung.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.ung.sak.typer.Beløp;
import no.nav.ung.sak.typer.OrgNummer;

public class YtelseStørrelse implements IndexKey {

    @ChangeTracked
    private InntektPeriodeType hyppighet = InntektPeriodeType.UDEFINERT;

    @ChangeTracked
    private OrgNummer virksomhetOrgnr;

    @ChangeTracked
    private Beløp beløp;

    @ChangeTracked
    private Boolean erRefusjon;

    public YtelseStørrelse() {
        // hibernate
    }

    public YtelseStørrelse(YtelseStørrelse ytelseStørrelse) {
        ytelseStørrelse.getVirksomhet().ifPresent(tidligereVirksomhet -> this.virksomhetOrgnr = tidligereVirksomhet);
        this.beløp = ytelseStørrelse.getBeløp();
        this.hyppighet = ytelseStørrelse.getHyppighet();
        this.erRefusjon = ytelseStørrelse.getErRefusjon();
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { virksomhetOrgnr };
        return IndexKeyComposer.createKey(keyParts);
    }

    public Optional<String> getOrgnr() {
        return Optional.ofNullable(virksomhetOrgnr == null ? null : virksomhetOrgnr.getId());
    }

    /**
     * Returner orgnr dersom virksomhet. Null ellers.
     *
     * @see #getVirksomhet()
     */
    public OrgNummer getVirksomhetOrgnr() {
        return virksomhetOrgnr;
    }

    public Optional<OrgNummer> getVirksomhet() {
        return Optional.ofNullable(virksomhetOrgnr);
    }

    public Beløp getBeløp() {
        return beløp;
    }

    public InntektPeriodeType getHyppighet() {
        return hyppighet;
    }

    public Boolean getErRefusjon() {
        return erRefusjon;
    }

    void setVirksomhet(OrgNummer virksomhetOrgnr) {
        this.virksomhetOrgnr = virksomhetOrgnr;
    }

    void setBeløp(Beløp beløp) {
        this.beløp = beløp;
    }

    void setHyppighet(InntektPeriodeType hyppighet) {
        this.hyppighet = hyppighet;
    }

    public void setErRefusjon(Boolean erRefusjon) {
        this.erRefusjon = erRefusjon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof YtelseStørrelse))
            return false;
        YtelseStørrelse that = (YtelseStørrelse) o;
        return Objects.equals(virksomhetOrgnr, that.virksomhetOrgnr) &&
            Objects.equals(beløp, that.beløp) &&
            Objects.equals(hyppighet, that.hyppighet);
    }

    @Override
    public int hashCode() {

        return Objects.hash(virksomhetOrgnr, beløp, hyppighet);
    }

    @Override
    public String toString() {
        return "YtelseStørrelseEntitet{" +
            "virksomhet=" + virksomhetOrgnr +
            ", beløp=" + beløp +
            ", hyppighet=" + hyppighet +
            '}';
    }

    boolean hasValues() {
        return beløp != null || hyppighet != null || virksomhetOrgnr != null;
    }
}
