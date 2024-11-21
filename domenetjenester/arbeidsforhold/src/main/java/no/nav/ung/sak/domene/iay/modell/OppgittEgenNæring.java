package no.nav.ung.sak.domene.iay.modell;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import no.nav.abakus.iaygrunnlag.kodeverk.VirksomhetType;
import no.nav.ung.kodeverk.api.IndexKey;
import no.nav.ung.sak.behandlingslager.diff.ChangeTracked;
import no.nav.ung.sak.behandlingslager.diff.IndexKeyComposer;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.OrgNummer;

public class OppgittEgenNæring implements IndexKey {

    @ChangeTracked
    private DatoIntervallEntitet periode;

    private OrgNummer virksomhetOrgnr;

    private VirksomhetType virksomhetType = VirksomhetType.UDEFINERT;

    private String regnskapsførerNavn;

    private String regnskapsførerTlf;

    private LocalDate endringDato;

    private String begrunnelse;

    private BigDecimal bruttoInntekt;

    private Boolean nyoppstartet;

    private Boolean varigEndring;

    private Boolean nærRelasjon;

    private Boolean nyIArbeidslivet;

    private OppgittUtenlandskVirksomhet utenlandskVirksomhet = new OppgittUtenlandskVirksomhet();

    OppgittEgenNæring() {
    }

    /** deep-copy ctor. */
    OppgittEgenNæring(OppgittEgenNæring kopierFra) {
        this.periode = kopierFra.periode;
        this.virksomhetOrgnr = kopierFra.virksomhetOrgnr;
        this.virksomhetType = kopierFra.virksomhetType;
        this.regnskapsførerNavn = kopierFra.regnskapsførerNavn;
        this.regnskapsførerTlf = kopierFra.regnskapsførerTlf;
        this.endringDato = kopierFra.endringDato;
        this.begrunnelse = kopierFra.begrunnelse;
        this.bruttoInntekt = kopierFra.bruttoInntekt;
        this.nyoppstartet = kopierFra.nyoppstartet;
        this.varigEndring = kopierFra.varigEndring;
        this.nærRelasjon = kopierFra.nærRelasjon;
        this.nyIArbeidslivet = kopierFra.nyIArbeidslivet;
        this.utenlandskVirksomhet = kopierFra.utenlandskVirksomhet;
    }

    @Override
    public String getIndexKey() {
        Object[] keyParts = { periode, virksomhetOrgnr, utenlandskVirksomhet };
        return IndexKeyComposer.createKey(keyParts);
    }

    public LocalDate getFraOgMed() {
        return periode.getFomDato();
    }

    public LocalDate getTilOgMed() {
        return periode.getTomDato();
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public VirksomhetType getVirksomhetType() {
        return virksomhetType;
    }

    void setVirksomhetType(VirksomhetType virksomhetType) {
        this.virksomhetType = virksomhetType;
    }

    /** Samme som {@link #getVirksomhetOrgnr()} men returnerer string. */
    public String getOrgnr() {
        return virksomhetOrgnr == null ? null : virksomhetOrgnr.getId();
    }

    public OrgNummer getVirksomhetOrgnr() {
        return virksomhetOrgnr;
    }

    void setVirksomhetOrgnr(OrgNummer orgNr) {
        this.virksomhetOrgnr = orgNr;
    }

    public String getRegnskapsførerNavn() {
        return regnskapsførerNavn;
    }

    void setRegnskapsførerNavn(String regnskapsførerNavn) {
        this.regnskapsførerNavn = regnskapsførerNavn;
    }

    public String getRegnskapsførerTlf() {
        return regnskapsførerTlf;
    }

    void setRegnskapsførerTlf(String regnskapsførerTlf) {
        this.regnskapsførerTlf = regnskapsførerTlf;
    }

    public LocalDate getEndringDato() {
        return endringDato;
    }

    void setEndringDato(LocalDate endringDato) {
        this.endringDato = endringDato;
    }

    public BigDecimal getBruttoInntekt() {
        return bruttoInntekt;
    }

    void setBruttoInntekt(BigDecimal bruttoInntekt) {
        this.bruttoInntekt = bruttoInntekt;
    }

    public String getBegrunnelse() {
        return begrunnelse;
    }

    void setBegrunnelse(String begrunnelse) {
        this.begrunnelse = begrunnelse;
    }

    public Boolean getNyoppstartet() {
        return nyoppstartet;
    }

    void setNyoppstartet(Boolean nyoppstartet) {
        this.nyoppstartet = nyoppstartet;
    }

    void setNyIArbeidslivet(Boolean nyIArbeidslivet) {
        this.nyIArbeidslivet = nyIArbeidslivet;
    }

    public Boolean getNyIArbeidslivet() {
        return nyIArbeidslivet;
    }

    public Boolean getVarigEndring() {
        return varigEndring;
    }

    void setVarigEndring(Boolean varigEndring) {
        this.varigEndring = varigEndring;
    }

    public Boolean getNærRelasjon() {
        return nærRelasjon;
    }

    void setNærRelasjon(Boolean nærRelasjon) {
        this.nærRelasjon = nærRelasjon;
    }

    public OppgittUtenlandskVirksomhet getVirksomhet() {
        return utenlandskVirksomhet;
    }

    void setUtenlandskVirksomhet(OppgittUtenlandskVirksomhet utenlandskVirksomhet) {
        this.utenlandskVirksomhet = utenlandskVirksomhet;
    }

    void setPeriode(DatoIntervallEntitet periode) {
        this.periode = periode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || !(o instanceof OppgittEgenNæring that))
            return false;
        return Objects.equals(periode, that.periode) &&
            Objects.equals(virksomhetOrgnr, that.virksomhetOrgnr) &&
            Objects.equals(nyoppstartet, that.nyoppstartet) &&
            Objects.equals(virksomhetType, that.virksomhetType) &&
            Objects.equals(regnskapsførerNavn, that.regnskapsførerNavn) &&
            Objects.equals(regnskapsførerTlf, that.regnskapsførerTlf) &&
            Objects.equals(endringDato, that.endringDato) &&
            Objects.equals(begrunnelse, that.begrunnelse) &&
            Objects.equals(bruttoInntekt, that.bruttoInntekt) &&
            Objects.equals(utenlandskVirksomhet, that.utenlandskVirksomhet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, virksomhetOrgnr, virksomhetType,
            nyoppstartet,
            regnskapsførerNavn, regnskapsførerTlf, endringDato, begrunnelse,
            bruttoInntekt, utenlandskVirksomhet);
    }

    @Override
    public String toString() {
        return "EgenNæringEntitet{" +
            "periode=" + periode +
            ", virksomhet=" + virksomhetOrgnr +
            ", nyoppstartet=" + nyoppstartet +
            ", virksomhetType=" + virksomhetType +
            ", regnskapsførerNavn='" + regnskapsførerNavn + '\'' +
            ", regnskapsførerTlf='" + regnskapsførerTlf + '\'' +
            ", endringDato=" + endringDato +
            ", begrunnelse='" + begrunnelse + '\'' +
            ", bruttoInntekt=" + bruttoInntekt +
            ", utenlandskVirksomhet=" + utenlandskVirksomhet +
            '}';
    }
}
