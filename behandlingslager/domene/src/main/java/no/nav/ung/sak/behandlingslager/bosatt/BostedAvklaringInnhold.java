package no.nav.ung.sak.behandlingslager.bosatt;

import no.nav.ung.kodeverk.bosatt.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.Periode;

// OBS: Det utføres set-operasjoner vha denne recorden.
// Feltene brukes for å avgjøre om en avklaring har endret innhold og må varsles på nytt.
// Referanse, vurdertAv og tidspunkt er derfor utelatt.
public record BostedAvklaringInnhold(
    Periode periode,
    BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak,
    String begrunnelse,
    boolean skalSendeVarsel,
    String fritekstTilVarsel,
    String begrunnelseIkkeVarsel,
    Avklaringtype avklaringtype
) {

    public DatoIntervallEntitet hentPeriodeSomDatoIntervallEntitet() {
        return DatoIntervallEntitet.fraOgMedTilOgMed(periode.getFom(), periode.getTom());
    }
}
