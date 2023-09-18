package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.død;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.alder.VurderAldersVilkårTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

class VilkårForlengingTjeneste {

    private final VurderAldersVilkårTjeneste vurderAldersVilkårTjeneste = new VurderAldersVilkårTjeneste();

    public void forlengOgVurderAldersvilkåret(VilkårResultatBuilder resultatBuilder, DatoIntervallEntitet periode, PersonopplysningEntitet brukerPersonopplysninger) {
        var aldersvilkår = VilkårType.ALDERSVILKÅR;
        var vilkårBuilder = resultatBuilder.hentBuilderFor(aldersvilkår);
        var fødselsdato = brukerPersonopplysninger.getFødselsdato();

        var set = new TreeSet<DatoIntervallEntitet>();
        set.add(periode);
        vurderAldersVilkårTjeneste.vurderPerioder(vilkårBuilder, set, fødselsdato);
        resultatBuilder.leggTil(vilkårBuilder);
    }

    public void forlengVilkårMedPeriodeVedDødsfall(Set<VilkårType> vilkår, VilkårResultatBuilder resultatBuilder, Vilkårene vilkårene, DatoIntervallEntitet periode, LocalDate dødsdato) {
        Objects.requireNonNull(dødsdato);
        for (VilkårType vilkårType : vilkår) {
            var vilkårBuilder = resultatBuilder.hentBuilderFor(vilkårType);
            var eksisterendeResultat = finnVurderingPåDødsdato(dødsdato, vilkårene.getVilkår(vilkårType).orElseThrow()).orElseThrow();

            vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periode).forlengelseAv(eksisterendeResultat));
            resultatBuilder.leggTil(vilkårBuilder);
        }
    }

    public Optional<VilkårPeriode> finnVurderingPåDødsdato(LocalDate dødsdato, Vilkår vilkår) {
        Optional<VilkårPeriode> vilkårPeriodeForDødsdato = vilkår.finnPeriodeSomInneholderDato(dødsdato);

        if (vilkårPeriodeForDødsdato.isEmpty()) {
            final DayOfWeek ukedag = dødsdato.getDayOfWeek();
            if (ukedag == DayOfWeek.SATURDAY) {
                vilkårPeriodeForDødsdato = vilkår.finnPeriodeSomInneholderDato(dødsdato.plusDays(2));
                if (vilkårPeriodeForDødsdato.isEmpty()) {
                    vilkårPeriodeForDødsdato = vilkår.finnPeriodeSomInneholderDato(dødsdato.minusDays(1));
                }
            }
            if (ukedag == DayOfWeek.SUNDAY) {
                vilkårPeriodeForDødsdato = vilkår.finnPeriodeSomInneholderDato(dødsdato.plusDays(1));
                if (vilkårPeriodeForDødsdato.isEmpty()) {
                    vilkårPeriodeForDødsdato = vilkår.finnPeriodeSomInneholderDato(dødsdato.minusDays(2));
                }
            }
        }

        return vilkårPeriodeForDødsdato;
    }
}


