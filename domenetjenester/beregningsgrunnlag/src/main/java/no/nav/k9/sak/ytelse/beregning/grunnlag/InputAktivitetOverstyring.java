package no.nav.k9.sak.ytelse.beregning.grunnlag;

import java.time.LocalDate;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Immutable;

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

    @Convert(converter= AktivitetStatusKodeverdiConverter.class)
    @Column(name="aktivitet_status", nullable = false)
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
                                     LocalDate opphørRefusjon, AktivitetStatus aktivitetStatus,
                                     DatoIntervallEntitet periode) {
        this.arbeidsgiver = arbeidsgiver;
        this.inntektPrÅr = inntektPrÅr;
        this.refusjonPrÅr = refusjonPrÅr;
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
}
