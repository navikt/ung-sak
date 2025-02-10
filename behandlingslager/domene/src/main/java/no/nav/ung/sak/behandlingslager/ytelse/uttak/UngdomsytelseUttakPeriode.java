package no.nav.ung.sak.behandlingslager.ytelse.uttak;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.hibernate.annotations.Type;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.PostgreSQLRangeType;
import no.nav.ung.sak.behandlingslager.Range;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;
import no.nav.ung.kodeverk.ungdomsytelse.UngdomsytelseUttakAvslagsårsakKodeverdiConverter;

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

    public UngdomsytelseUttakPeriode() {
    }

    public UngdomsytelseUttakPeriode(UngdomsytelseUttakPeriode ungdomsytelseUttakPeriode) {
        this.periode = ungdomsytelseUttakPeriode.getPeriode().toRange();
        this.avslagsårsak = ungdomsytelseUttakPeriode.getAvslagsårsak();
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
