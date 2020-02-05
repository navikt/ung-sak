package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem;

import java.time.LocalDate;

import no.nav.k9.kodeverk.medlem.MedlemskapDekningType;
import no.nav.k9.kodeverk.medlem.MedlemskapKildeType;
import no.nav.k9.kodeverk.medlem.MedlemskapType;

public class MedlemskapPerioderDto {
    private LocalDate fom;
    private LocalDate tom;
    private MedlemskapType medlemskapType;
    private MedlemskapDekningType dekningType;
    private MedlemskapKildeType kildeType;
    private LocalDate beslutningsdato;

    public MedlemskapPerioderDto() {
        // trengs for deserialisering av JSON
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public MedlemskapType getMedlemskapType() {
        return medlemskapType;
    }

    public MedlemskapDekningType getDekningType() {
        return dekningType;
    }

    public MedlemskapKildeType getKildeType() {
        return kildeType;
    }

    public LocalDate getBeslutningsdato() {
        return beslutningsdato;
    }

    void setFom(LocalDate fom) {
        this.fom = fom;
    }

    void setTom(LocalDate tom) {
        this.tom = tom;
    }

    void setMedlemskapType(MedlemskapType medlemskapType) {
        this.medlemskapType = medlemskapType;
    }

    void setDekningType(MedlemskapDekningType dekningType) {
        this.dekningType = dekningType;
    }

    void setKildeType(MedlemskapKildeType kildeType) {
        this.kildeType = kildeType;
    }

    void setBeslutningsdato(LocalDate beslutningsdato) {
        this.beslutningsdato = beslutningsdato;
    }
}