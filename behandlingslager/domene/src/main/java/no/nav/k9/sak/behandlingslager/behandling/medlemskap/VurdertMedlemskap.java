package no.nav.k9.sak.behandlingslager.behandling.medlemskap;

import java.time.LocalDateTime;

import no.nav.k9.kodeverk.medlem.MedlemskapManuellVurderingType;

public interface VurdertMedlemskap {

    Boolean getOppholdsrettVurdering();

    Boolean getLovligOppholdVurdering();

    Boolean getBosattVurdering();

    MedlemskapManuellVurderingType getMedlemsperiodeManuellVurdering();

    Boolean getErEøsBorger();

    String getBegrunnelse();

    String getVurdertAv();

    LocalDateTime getVurdertTidspunkt();

}
