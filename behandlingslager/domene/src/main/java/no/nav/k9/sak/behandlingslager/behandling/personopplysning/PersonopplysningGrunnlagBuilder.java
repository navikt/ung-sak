package no.nav.k9.sak.behandlingslager.behandling.personopplysning;

import java.util.Optional;

public class PersonopplysningGrunnlagBuilder {

    private final PersonopplysningGrunnlagEntitet kladd;

    private PersonopplysningGrunnlagBuilder(PersonopplysningGrunnlagEntitet kladd) {
        this.kladd = kladd;
    }

    private static PersonopplysningGrunnlagBuilder nytt() {
        return new PersonopplysningGrunnlagBuilder(new PersonopplysningGrunnlagEntitet());
    }

    private static PersonopplysningGrunnlagBuilder oppdatere(PersonopplysningGrunnlagEntitet kladd) {
        return new PersonopplysningGrunnlagBuilder(new PersonopplysningGrunnlagEntitet(kladd));
    }

    public static PersonopplysningGrunnlagBuilder oppdatere(Optional<PersonopplysningGrunnlagEntitet> kladd) {
        return kladd.map(PersonopplysningGrunnlagBuilder::oppdatere).orElseGet(PersonopplysningGrunnlagBuilder::nytt);
    }

    public PersonopplysningGrunnlagBuilder medRegistrertVersjon(PersonInformasjonBuilder builder) {
        if (builder == null) {
            return this;
        }
        if (!builder.getType().equals(PersonopplysningVersjonType.REGISTRERT)) {
            throw new IllegalArgumentException("Utvikler-feil: forventet register personinfo");
        }
        kladd.setRegistrertePersonopplysninger(builder.build());
        return this;
    }

    public PersonopplysningGrunnlagBuilder medOverstyrtVersjon(PersonInformasjonBuilder builder) {
        if (builder == null) {
            return this;
        }
        if (!builder.getType().equals(PersonopplysningVersjonType.OVERSTYRT)) {
            throw new IllegalArgumentException("Utvikler-feil: forventet overstyrt personinfo");
        }
        kladd.setOverstyrtePersonopplysninger(builder.build());
        return this;
    }

    public PersonopplysningGrunnlagEntitet build() {
        return kladd;
    }

}
