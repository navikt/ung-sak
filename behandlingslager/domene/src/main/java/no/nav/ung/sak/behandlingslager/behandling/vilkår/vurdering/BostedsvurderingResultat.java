package no.nav.ung.sak.behandlingslager.behandling.vilkår.vurdering;

import no.nav.ung.kodeverk.bosatt.OpphørKilde;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;

import java.time.LocalDate;

public record BostedsvurderingResultat(
    long behandlingId,
    LocalDate fom,
    LocalDate opphørDato, Avslagsårsak avslagsårsak,
    OpphørKilde kilde,
    VilkårType vilkårType,
    String begrunnelse,
    String fritekstVurderingBrev) {}
