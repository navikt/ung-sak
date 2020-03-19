package no.nav.foreldrepenger.domene.personopplysning;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.AktørId;

public interface StandardPersonopplysningTjeneste {

    /**
     * Gir personopplysningene på utledet skjæringstidspunktet
     * @return personopplysninger
     */
    PersonopplysningerAggregat hentPersonopplysninger(BehandlingReferanse ref);
    
    /**
     * Gir personopplysningene på utledet skjæringstidspunktet
     * @return personopplysninger hvis finnes
     */
    Optional<PersonopplysningerAggregat> hentPersonopplysningerHvisEksisterer(BehandlingReferanse ref);

    /**
     * Filtrerer, og gir personopplysning-historikk som er gyldig for på gitt tidspunkt.
     */
    PersonopplysningerAggregat hentGjeldendePersoninformasjonPåTidspunkt(Long behandlingId, AktørId aktørId, LocalDate tidspunkt);

    /**
     * Filtrerer, og gir personopplysning-historikk som er gyldig for gitt tidspunkt.
     */
    Optional<PersonopplysningerAggregat> hentGjeldendePersoninformasjonPåTidspunktHvisEksisterer(Long behandlingId, AktørId aktørId, LocalDate tidspunkt);

    /**
     * Filtrerer, og gir personopplysning-historikk som er gyldig for angitt intervall.
     */
    Optional<PersonopplysningerAggregat> hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(Long behandlingId, AktørId aktørId, DatoIntervallEntitet forPeriode);

}
