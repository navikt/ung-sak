package no.nav.ung.sak.behandlingslager.ytelse.uttak;

import jakarta.persistence.*;
import no.nav.ung.kodeverk.hjemmel.Hjemmel;
import no.nav.ung.sak.behandlingslager.kodeverk.HjemmelKodeverdiConverter;
import no.nav.ung.sak.behandlingslager.kodeverk.UngdomsytelseUttakAvslagsårsakKodeverdiConverter;
import no.nav.ung.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.PostgreSQLRangeType;
import no.nav.ung.sak.behandlingslager.Range;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import org.hibernate.annotations.Type;

import java.time.LocalDate;

@Entity(name = "UngdomsytelseUttakPeriode")
@Table(name = "UNG_UTTAK_PERIODE")
public class UngdomsytelseUttakPeriode extends BaseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UNG_UTTAK_PERIODE")
    private Long id;

    @Type(PostgreSQLRangeType.class)
    @Column(name = "periode", columnDefinition = "daterange")
    private Range<LocalDate> periode;

    @Convert(converter = UngdomsytelseUttakAvslagsårsakKodeverdiConverter.class)
    @Column(name = "avslag_aarsak")
    private UngdomsytelseUttakAvslagsårsak avslagsårsak;

    @Convert(converter = HjemmelKodeverdiConverter.class)
    @Column(name = "hjemmel")
    private Hjemmel hjemmel = Hjemmel.UNG_FORSKRIFT_PARAGRAF_6;

    public UngdomsytelseUttakPeriode() {
    }

    public UngdomsytelseUttakPeriode(UngdomsytelseUttakPeriode ungdomsytelseUttakPeriode) {
        this.periode = ungdomsytelseUttakPeriode.getPeriode().toRange();
        this.avslagsårsak = ungdomsytelseUttakPeriode.getAvslagsårsak();
    }


    public UngdomsytelseUttakPeriode(DatoIntervallEntitet periode) {
        this.periode = Range.closed(periode.getFomDato(), periode.getTomDato());
    }


    public UngdomsytelseUttakPeriode(LocalDate fom, LocalDate tom, UngdomsytelseUttakAvslagsårsak avslagsårsak) {
        this.periode = Range.closed(fom, tom);
        this.avslagsårsak = avslagsårsak;
    }


    public DatoIntervallEntitet getPeriode() {
        return DatoIntervallEntitet.fra(periode);
    }


    public UngdomsytelseUttakAvslagsårsak getAvslagsårsak() {
        return avslagsårsak;
    }
}
