package no.nav.k9.sak.behandlingslager.behandling.uttak;

import java.math.BigDecimal;

import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

@Entity(name = "OverstyrtUttakUtbetalingsgrad")
@Table(name = "OVERSTYRT_UTTAK_UTBETALINGSGRAD")
@DynamicInsert
@DynamicUpdate
public class OverstyrtUttakUtbetalingsgradEntitet extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_OVERSTYRT_UTTAK_UTBETALINGSGRAD")
    private Long id;

    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    @Column(name = "aktivitet_type", nullable = false)
    private UttakArbeidType aktivitetType;

    @Column(name = "arbeidsgiver_orgnr", nullable = true)
    private String arbeidsgiverOrgNr;

    @Column(name = "arbeidsgiver_aktoer_id", nullable = true)
    private String arbeidsgiverAktørId;

    @Column(name = "intern_arbeidsforhold_ref", nullable = true)
    private String internArbeidsforholdRef;

    @Column(name = "utbetalingsgrad")
    private BigDecimal utbetalingsgrad;

    public OverstyrtUttakUtbetalingsgradEntitet(UttakArbeidType aktivitetType, Arbeidsgiver arbeidsgiverId, InternArbeidsforholdRef internArbeidsforholdRef, BigDecimal utbetalingsgrad) {
        this.aktivitetType = aktivitetType;
        this.arbeidsgiverOrgNr = arbeidsgiverId != null ? arbeidsgiverId.getArbeidsgiverOrgnr() : null;
        this.arbeidsgiverAktørId = arbeidsgiverId != null ? arbeidsgiverId.getArbeidsgiverAktørId() : null;
        this.internArbeidsforholdRef = internArbeidsforholdRef != null ? internArbeidsforholdRef.getReferanse() : null;
        this.utbetalingsgrad = utbetalingsgrad;
    }

    OverstyrtUttakUtbetalingsgradEntitet(OverstyrtUttakUtbetalingsgradEntitet original) {
        this.aktivitetType = original.aktivitetType;
        this.arbeidsgiverOrgNr = original.arbeidsgiverOrgNr;
        this.arbeidsgiverAktørId = original.arbeidsgiverAktørId;
        this.internArbeidsforholdRef = original.internArbeidsforholdRef;
        this.utbetalingsgrad = original.utbetalingsgrad;
    }

    OverstyrtUttakUtbetalingsgradEntitet() {
    }


    public UttakArbeidType getAktivitetType() {
        return aktivitetType;
    }

    public Arbeidsgiver getArbeidsgiver() {
        if (arbeidsgiverOrgNr != null) {
            return Arbeidsgiver.virksomhet(arbeidsgiverOrgNr);
        } else if (arbeidsgiverAktørId != null) {
            return Arbeidsgiver.fra(new AktørId(arbeidsgiverAktørId));
        } else {
            return null;
        }
    }

    public InternArbeidsforholdRef getInternArbeidsforholdRef() {
        return internArbeidsforholdRef != null ? InternArbeidsforholdRef.ref(internArbeidsforholdRef) : null;
    }

    public BigDecimal getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    @Override
    public String toString() {
        return "OverstyrtUttakUtbetalingsgradEntitet{" +
            "aktivitetType=" + aktivitetType +
            ", arbeidsgiver='" + getArbeidsgiver().toString() + '\'' +
            ", internArbeidsforholdRef='" + internArbeidsforholdRef + '\'' +
            ", utbetalingsgrad=" + utbetalingsgrad +
            '}';
    }
}
