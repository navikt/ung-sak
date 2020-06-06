package no.nav.k9.sak.behandlingslager.behandling.vilkår.periode;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Properties;

import no.nav.k9.kodeverk.vilkår.Avslagsårsak;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class VilkårPeriodeBuilder {
    private final VilkårPeriode entitet;
    private boolean bygget = false;

    public VilkårPeriodeBuilder() {
        entitet = new VilkårPeriode();
    }

    public VilkårPeriodeBuilder(VilkårPeriode vilkårPeriode) {
        entitet = new VilkårPeriode(vilkårPeriode);
    }

    public VilkårPeriodeBuilder medPeriode(LocalDate fom, LocalDate tom) {
        this.entitet.setPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
        return this;
    }

    public VilkårPeriodeBuilder medAvslagsårsak(Avslagsårsak avslagsårsak) {
        this.entitet.setAvslagsårsak(avslagsårsak);
        return this;
    }

    public VilkårPeriodeBuilder medUtfall(Utfall utfall) {
        this.entitet.setUtfall(utfall);
        return this;
    }

    public VilkårPeriodeBuilder medMerknad(VilkårUtfallMerknad vilkårUtfallMerknad) {
        this.entitet.setUtfallMerknad(vilkårUtfallMerknad);
        return this;
    }

    public VilkårPeriodeBuilder medMerknadParametere(Properties merknadParametere) {
        this.entitet.setMerknadParametere(merknadParametere);
        return this;
    }

    public VilkårPeriodeBuilder medUtfallManuell(Utfall vilkårUtfallManuell) {
        this.entitet.setManueltVurdert(true);
        this.entitet.setUtfall(vilkårUtfallManuell);
        return this;
    }

    public VilkårPeriodeBuilder medUtfallOverstyrt(Utfall vilkårUtfallOverstyrt) {
        this.entitet.setOverstyrtUtfall(vilkårUtfallOverstyrt);
        return this;
    }

    public VilkårPeriodeBuilder medRegelEvaluering(String regelEvaluering) {
        this.entitet.setRegelEvaluering(regelEvaluering);
        return this;
    }

    public VilkårPeriodeBuilder medRegelInput(String regelInput) {
        this.entitet.setRegelInput(regelInput);
        return this;
    }

    public VilkårPeriodeBuilder medBegrunnelse(String begrunnelse) {
        this.entitet.setBegrunnelse(begrunnelse);
        return this;
    }

    public VilkårPeriodeBuilder tilbakestillManuellVurdering() {
        this.entitet.setManueltVurdert(false);
        return this;
    }

    public VilkårPeriode build() {
        if (bygget) {
            throw new IllegalStateException("Builderen har allerede blitt bygget.");
        }
        this.bygget = true;
        validerEntitet();
        return entitet;
    }

    private void validerEntitet() {
        Objects.requireNonNull(entitet.getUtfall(), "Utfall må være på plass.");
        Objects.requireNonNull(entitet.getPeriode(), "Periode må være på plass.");
    }
}
