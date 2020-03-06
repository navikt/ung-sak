package no.nav.folketrygdloven.beregningsgrunnlag.modell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import no.nav.foreldrepenger.behandlingslager.BaseEntitet;

public class BeregningAktivitetOverstyringer {

    private List<BeregningAktivitetOverstyring> overstyringer = new ArrayList<>();

    public List<BeregningAktivitetOverstyring> getOverstyringer() {
        return Collections.unmodifiableList(overstyringer);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final BeregningAktivitetOverstyringer kladd;

        private Builder() {
            kladd = new BeregningAktivitetOverstyringer();
        }

        public Builder leggTilOverstyring(BeregningAktivitetOverstyring beregningAktivitetOverstyring) {
            BeregningAktivitetOverstyring entitet = beregningAktivitetOverstyring;
            kladd.overstyringer.add(entitet);
            entitet.setBeregningAktivitetOverstyringer(kladd);
            return this;
        }

        public BeregningAktivitetOverstyringer build() {
            return kladd;
        }
    }
}
