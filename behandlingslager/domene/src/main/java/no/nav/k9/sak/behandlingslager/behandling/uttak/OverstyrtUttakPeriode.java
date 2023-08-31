package no.nav.k9.sak.behandlingslager.behandling.uttak;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;


public class OverstyrtUttakPeriode {

    private Long id;
    private BigDecimal søkersUttaksgrad;
    private Set<OverstyrtUttakUtbetalingsgrad> overstyrtUtbetalingsgrad;

    public OverstyrtUttakPeriode(Long id, BigDecimal søkersUttaksgrad, Set<OverstyrtUttakUtbetalingsgrad> overstyrtUtbetalingsgrad) {
        Objects.requireNonNull(overstyrtUtbetalingsgrad, "overstyrtUtbetalingsgrad");
        this.id = id;
        this.søkersUttaksgrad = søkersUttaksgrad;
        this.overstyrtUtbetalingsgrad = overstyrtUtbetalingsgrad;
    }

    public OverstyrtUttakPeriode(BigDecimal søkersUttaksgrad, Set<OverstyrtUttakUtbetalingsgrad> overstyrtUtbetalingsgrad) {
        this(null, søkersUttaksgrad, overstyrtUtbetalingsgrad);
    }

    public BigDecimal getSøkersUttaksgrad() {
        return søkersUttaksgrad;
    }

    public Set<OverstyrtUttakUtbetalingsgrad> getOverstyrtUtbetalingsgrad() {
        return overstyrtUtbetalingsgrad;
    }

    public Long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OverstyrtUttakPeriode that = (OverstyrtUttakPeriode) o;
        return Objects.equals(søkersUttaksgrad, that.søkersUttaksgrad) && overstyrtUtbetalingsgrad.equals(that.overstyrtUtbetalingsgrad);
    }

    @Override
    public int hashCode() {
        return Objects.hash(søkersUttaksgrad, overstyrtUtbetalingsgrad);
    }
}
