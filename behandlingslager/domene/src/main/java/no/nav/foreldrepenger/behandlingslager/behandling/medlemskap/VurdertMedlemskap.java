package no.nav.foreldrepenger.behandlingslager.behandling.medlemskap;

import no.nav.k9.kodeverk.medlemskap.MedlemskapManuellVurderingType;

public interface VurdertMedlemskap {

    Boolean getOppholdsrettVurdering();

    Boolean getLovligOppholdVurdering();

    Boolean getBosattVurdering();

    MedlemskapManuellVurderingType getMedlemsperiodeManuellVurdering();

    Boolean getErEøsBorger();

    String getBegrunnelse();

}
