package no.nav.k9.sak.ytelse.beregning.grunnlag;

import java.time.LocalDate;
import java.util.Optional;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.sak.behandlingslager.BaseEntitet;
import no.nav.k9.sak.behandlingslager.kodeverk.AktivitetStatusKodeverdiConverter;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.Beløp;

@Entity(name = "AktivitetOverstyring")
@Table(name = "BG_OVST_AKTIVITET")
@Immutable
public class InputAktivitetOverstyring extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_BG_OVST_AKTIVITET")
    private Long id;

    @Embedded
    private Arbeidsgiver arbeidsgiver;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "verdi", column = @Column(name = "inntekt_pr_aar")))
    private Beløp inntektPrÅr;

    @Embedded
    @AttributeOverrides(@AttributeOverride(name = "verdi", column = @Column(name = "refusjon_pr_aar")))
    private Beløp refusjonPrÅr;

    @Column(name = "opphoer_refusjon")
    private LocalDate opphørRefusjon;

    @Column(name = "start_refusjon")
    private LocalDate startdatoRefusjon;

    @Convert(converter = AktivitetStatusKodeverdiConverter.class)
    @Column(name = "aktivitet_status", nullable = false)
    private AktivitetStatus aktivitetStatus;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "fomDato", column = @Column(name = "fom")),
        @AttributeOverride(name = "tomDato", column = @Column(name = "tom"))
    })
    private DatoIntervallEntitet periode;


    @Version
    @Column(name = "versjon", nullable = false)
    private long versjon;

    public InputAktivitetOverstyring() {
    }

    public InputAktivitetOverstyring(Arbeidsgiver arbeidsgiver,
                                     Beløp inntektPrÅr,
                                     Beløp refusjonPrÅr,
                                     LocalDate startdatoRefusjon,
                                     LocalDate opphørRefusjon,
                                     AktivitetStatus aktivitetStatus,
                                     DatoIntervallEntitet periode) {
        this.arbeidsgiver = arbeidsgiver;
        this.inntektPrÅr = inntektPrÅr;
        this.refusjonPrÅr = refusjonPrÅr;
        this.startdatoRefusjon = startdatoRefusjon;
        this.opphørRefusjon = opphørRefusjon;
        this.aktivitetStatus = aktivitetStatus;
        this.periode = periode;
    }

    public InputAktivitetOverstyring(InputAktivitetOverstyring inputAktivitetOverstyring) {
        this.aktivitetStatus = inputAktivitetOverstyring.getAktivitetStatus();
        this.arbeidsgiver = inputAktivitetOverstyring.getArbeidsgiver();
        this.inntektPrÅr = inputAktivitetOverstyring.getInntektPrÅr();
        this.refusjonPrÅr = inputAktivitetOverstyring.getRefusjonPrÅr();
        this.opphørRefusjon = inputAktivitetOverstyring.getOpphørRefusjon();
        this.startdatoRefusjon = inputAktivitetOverstyring.getStartdatoRefusjon().orElse(null);
        this.periode = inputAktivitetOverstyring.getPeriode();
    }

    public Beløp getInntektPrÅr() {
        return inntektPrÅr;
    }

    public Beløp getRefusjonPrÅr() {
        return refusjonPrÅr;
    }

    public AktivitetStatus getAktivitetStatus() {
        return aktivitetStatus;
    }

    public Arbeidsgiver getArbeidsgiver() {
        return arbeidsgiver;
    }

    public DatoIntervallEntitet getPeriode() {
        return periode;
    }

    public LocalDate getOpphørRefusjon() {
        return opphørRefusjon;
    }

    public Optional<LocalDate> getStartdatoRefusjon() {
        return Optional.ofNullable(startdatoRefusjon);
    }
}
