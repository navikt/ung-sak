package no.nav.ung.sak.etterlysning;

import no.nav.ung.kodeverk.vilkår.IkkeOppfyltDetaljertÅrsak;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Periode;

/**
 * Varselinnholdet i en vilkårsavklaring — det brukeren faktisk får se. Likheten avgjør både om en etterlysning
 * kan beholdes og om avklaringens referanse gjenbrukes ved lagring, slik at de to alltid er enige.
 * Vilkåret som implementerer interfacet eier likhetssjekken, og må utelate felter som ikke vises for bruker
 * (begrunnelse, vurdert av/tidspunkt) — ellers varsles brukeren på nytt uten at varselet er endret.
 */
public interface VilkårsvarselInnhold {

    Periode periode();
    IkkeOppfyltDetaljertÅrsak ikkeOppfyltÅrsak();
    boolean skalSendeVarsel();

    default DatoIntervallEntitet hentPeriodeSomDatoIntervallEntitet() {
        return DatoIntervallEntitet.fraOgMedTilOgMed(periode().getFom(), periode().getTom());
    }
}
