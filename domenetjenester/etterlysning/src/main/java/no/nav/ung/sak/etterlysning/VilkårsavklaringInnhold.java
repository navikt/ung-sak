package no.nav.ung.sak.etterlysning;

import no.nav.ung.kodeverk.vilkår.IkkeOppfyltDetaljertÅrsak;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Periode;

/**
 * Innhold for en vilkårsavklaring, sett fra {@link VilkårsavklaringEtterlysningTjeneste} sitt ståsted.
 */
public interface VilkårsavklaringInnhold {

    Periode periode();
    IkkeOppfyltDetaljertÅrsak ikkeOppfyltÅrsak();
    boolean skalSendeVarsel();

    default DatoIntervallEntitet hentPeriodeSomDatoIntervallEntitet() {
        return DatoIntervallEntitet.fraOgMedTilOgMed(periode().getFom(), periode().getTom());
    }
}
