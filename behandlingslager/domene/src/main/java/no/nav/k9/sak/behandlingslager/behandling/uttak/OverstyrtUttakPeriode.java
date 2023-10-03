package no.nav.k9.sak.behandlingslager.behandling.uttak;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;


public class OverstyrtUttakPeriode {

    private Long id;
    private BigDecimal søkersUttaksgrad;
    private Set<OverstyrtUttakUtbetalingsgrad> overstyrtUtbetalingsgrad;
    private String begrunnelse;

    public OverstyrtUttakPeriode(Long id, BigDecimal søkersUttaksgrad, Set<OverstyrtUttakUtbetalingsgrad> overstyrtUtbetalingsgrad, String begrunnelse) {
        Objects.requireNonNull(overstyrtUtbetalingsgrad, "overstyrtUtbetalingsgrad");
        this.id = id;
        this.søkersUttaksgrad = søkersUttaksgrad;
        this.overstyrtUtbetalingsgrad = overstyrtUtbetalingsgrad;
        this.begrunnelse = begrunnelse;
    }

    public OverstyrtUttakPeriode(BigDecimal søkersUttaksgrad, Set<OverstyrtUttakUtbetalingsgrad> overstyrtUtbetalingsgrad, String begrunnelse) {
        this(null, søkersUttaksgrad, overstyrtUtbetalingsgrad, begrunnelse);
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

    public String getBegrunnelse() {
        return begrunnelse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OverstyrtUttakPeriode that = (OverstyrtUttakPeriode) o;
        return nullsafeEqualByCompareTo(søkersUttaksgrad, that.søkersUttaksgrad)
            && overstyrtUtbetalingsgrad.equals(that.overstyrtUtbetalingsgrad)
            && begrunnelse.equals(that.begrunnelse);
    }

    private static boolean nullsafeEqualByCompareTo(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.compareTo(b) == 0;
    }

    @Override
    public int hashCode() {
        //kan ikke ha søkersUttaksgrad i hash når bruker compareTo i equals
        return Objects.hash(overstyrtUtbetalingsgrad, begrunnelse);
    }
}
