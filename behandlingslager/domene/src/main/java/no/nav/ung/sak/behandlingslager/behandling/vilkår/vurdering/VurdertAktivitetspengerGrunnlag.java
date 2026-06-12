package no.nav.ung.sak.behandlingslager.behandling.vilkår.vurdering;

import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.VilkårType;

public class VurdertAktivitetspengerGrunnlag {
    public VurdertAktivitetspengerGrunnlag(EntityManager entityManager) {

    }

    public void lagre(BostedsvurderingResultat vurdersresultat) {

    }

    public void deaktiverOppfylteOpphørsresultater(Long behandlingId, VilkårType vilkårType, LocalDateTimeline<Boolean> tidslinjeTilVurdering) {

    }
}
