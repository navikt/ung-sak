package no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.medisinsk.Pleiegrad;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell.LivetsSluttfaseDokumentasjon;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell.MedisinskVilkårResultat;
import no.nav.k9.sak.ytelse.pleiepengerlivetsslutt.inngangsvilkår.medisinsk.regelmodell.Pleielokasjon;

public class PleiegradKalkulator {

     static LocalDateTimeline<Pleiegrad> regnUtPleiegrad(MedisinskVilkårResultat vilkårresultat) {
        LocalDateTimeline<Pleiegrad> pleiegradFraDokumentasjon = vilkårresultat.tidslinjeLivetsSluttfaseDokumentasjon().mapValue(d -> d == LivetsSluttfaseDokumentasjon.DOKUMENTERT ? Pleiegrad.LIVETS_SLUTT_TILSYN : Pleiegrad.INGEN);
        LocalDateTimeline<Pleiegrad> pleiegradFratrekkInnleggelse = vilkårresultat.tidslinjePleielokasjon().filterValue(Pleielokasjon.INNLAGT::equals).mapValue(v -> Pleiegrad.INGEN);
        return pleiegradFraDokumentasjon.combine(pleiegradFratrekkInnleggelse, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.LEFT_JOIN);
    }
}
