package no.nav.k9.sak.behandlingslager.behandling.uttak;

import java.math.BigDecimal;
import java.util.Objects;

import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;


public class OverstyrtUttakUtbetalingsgrad {
    private UttakArbeidType aktivitetType;
    private String arbeidsgiverOrgNr;
    private String arbeidsgiverAktørId;
    private BigDecimal utbetalingsgrad;

    public OverstyrtUttakUtbetalingsgrad(UttakArbeidType aktivitetType, Arbeidsgiver arbeidsgiverId, BigDecimal utbetalingsgrad) {
        this.aktivitetType = aktivitetType;
        this.arbeidsgiverOrgNr = arbeidsgiverId != null ? arbeidsgiverId.getArbeidsgiverOrgnr() : null;
        this.arbeidsgiverAktørId = arbeidsgiverId != null ? arbeidsgiverId.getArbeidsgiverAktørId() : null;
        this.utbetalingsgrad = utbetalingsgrad;
    }

    public OverstyrtUttakUtbetalingsgrad(UttakArbeidType aktivitetType, String arbeidsgiverOrgNr, String arbeidsgiverAktørId, BigDecimal utbetalingsgrad) {
        this.aktivitetType = aktivitetType;
        this.arbeidsgiverOrgNr = arbeidsgiverOrgNr;
        this.arbeidsgiverAktørId = arbeidsgiverAktørId;
        this.utbetalingsgrad = utbetalingsgrad;
    }

    public UttakArbeidType getAktivitetType() {
        return aktivitetType;
    }

    public Arbeidsgiver getArbeidsgiverId() {
        if (arbeidsgiverOrgNr != null) {
            return Arbeidsgiver.virksomhet(arbeidsgiverOrgNr);
        } else if (arbeidsgiverAktørId != null) {
            return Arbeidsgiver.fra(new AktørId(arbeidsgiverAktørId));
        } else {
            return null;
        }
    }


    public BigDecimal getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OverstyrtUttakUtbetalingsgrad that = (OverstyrtUttakUtbetalingsgrad) o;
        return aktivitetType == that.aktivitetType
            && Objects.equals(arbeidsgiverOrgNr, that.arbeidsgiverOrgNr)
            && Objects.equals(arbeidsgiverAktørId, that.arbeidsgiverAktørId)
            && utbetalingsgrad.compareTo(that.utbetalingsgrad) == 0;
    }

    @Override
    public int hashCode() {
        //kan ikke ha utbetalingsgrad i hash når bruker compareTo i equals
        return Objects.hash(aktivitetType, arbeidsgiverOrgNr, arbeidsgiverAktørId);
    }
}
