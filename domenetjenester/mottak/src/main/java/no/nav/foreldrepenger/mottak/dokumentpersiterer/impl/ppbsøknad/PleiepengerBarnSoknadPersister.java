package no.nav.foreldrepenger.mottak.dokumentpersiterer.impl.ppbsøknad;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.Fordeling;
import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.FordelingPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.fordeling.FordelingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittTilknytningEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.foreldrepenger.behandlingslager.geografisk.Landkoder;
import no.nav.foreldrepenger.behandlingslager.geografisk.Språkkode;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsgiver.VirksomhetTjeneste;
import no.nav.foreldrepenger.domene.person.tps.TpsTjeneste;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.søknad.felles.Språk;
import no.nav.k9.søknad.pleiepengerbarn.PleiepengerBarnSøknad;
import no.nav.k9.søknad.pleiepengerbarn.Utland;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
public class PleiepengerBarnSoknadPersister {

    private VirksomhetTjeneste virksomhetTjeneste;
    private SøknadRepository søknadRepository;
    private MedlemskapRepository medlemskapRepository;
    private FordelingRepository fordelingRepository;
    private TpsTjeneste tpsTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    PleiepengerBarnSoknadPersister() {
        // for CDI proxy
    }

    @Inject
    public PleiepengerBarnSoknadPersister(BehandlingRepositoryProvider repositoryProvider,
                                          VirksomhetTjeneste virksomhetTjeneste,
                                          InntektArbeidYtelseTjeneste iayTjeneste,
                                          FordelingRepository fordelingRepository,
                                          TpsTjeneste tpsTjeneste) {
        this.iayTjeneste = iayTjeneste;
        this.søknadRepository = repositoryProvider.getSøknadRepository();
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.virksomhetTjeneste = virksomhetTjeneste;
        this.fordelingRepository = fordelingRepository;
        this.tpsTjeneste = tpsTjeneste;
    }

    //@Override
    public void persister(PleiepengerBarnSøknad soknad, Behandling behandling) {
        // TODO:
        final boolean elektroniskSøknad = false;
        final SøknadEntitet.Builder søknadBuilder = new SøknadEntitet.Builder()
            .medElektroniskRegistrert(elektroniskSøknad)
            .medMottattDato(soknad.mottattDato.toLocalDate())
            .medErEndringssøknad(false)
            .medSøknadsdato(soknad.mottattDato.toLocalDate()) // TODO: Hva er dette? Dette feltet er datoen det gjelder fra for FP-endringssøknader.
            .medSpråkkode(getSpraakValg(soknad.språk));

        // Utgår for K9-ytelsene?
        //.medBegrunnelseForSenInnsending(wrapper.getBegrunnelseForSenSoeknad())
        //.medTilleggsopplysninger(wrapper.getTilleggsopplysninger())

        final Long behandlingId = behandling.getId();

        byggMedlemskap(soknad.utland, behandlingId, soknad.mottattDato.toLocalDate());

        final Set<FordelingPeriode> perioder = mapTilPerioder(soknad);
        final var fordeling = new Fordeling(perioder);
        fordelingRepository.lagreOgFlush(behandling, fordeling);

        final SøknadEntitet søknadEntitet = søknadBuilder
            .build();
        søknadRepository.lagreOgFlush(behandling, søknadEntitet);
    }

    private Set<FordelingPeriode> mapTilPerioder(PleiepengerBarnSøknad soknad) {
        return Set.of(new FordelingPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(soknad.periode.fraOgMed, soknad.periode.tilOgMed)));
    }

    private void byggMedlemskap(Utland utland, Long behandlingId, LocalDate forsendelseMottatt) {
        final MedlemskapOppgittTilknytningEntitet.Builder oppgittTilknytningBuilder = new MedlemskapOppgittTilknytningEntitet.Builder()
            .medOppgittDato(forsendelseMottatt);

        // TODO: Hva skal vi ha som "oppholdNå"?
        //Boolean iNorgeVedFoedselstidspunkt = medlemskap.isINorgeVedFoedselstidspunkt();
        //oppgittTilknytningBuilder.medOppholdNå(Boolean.TRUE.equals(iNorgeVedFoedselstidspunkt));

        if (utland != null) {
            utland.opphold.forEach((periode, opphold) -> {
                // TODO: "tidligereOpphold" må fjernes fra database og domeneobjekter. Ved bruk må skjæringstidspunkt spesifikt oppgis.
                //boolean tidligereOpphold = opphold.getPeriode().getFom().isBefore(mottattDato);
                oppgittTilknytningBuilder.leggTilOpphold(new MedlemskapOppgittLandOppholdEntitet.Builder()
                    .medLand(finnLandkode(opphold.land.landkode))
                    .medPeriode(
                        Objects.requireNonNull(periode.fraOgMed),
                        Objects.requireNonNullElse(periode.fraOgMed, Tid.TIDENES_ENDE)
                    )
                    //.erTidligereOpphold(tidligereOpphold)
                    .build()
                );
            });
            medlemskapRepository.lagreOppgittTilkytning(behandlingId, oppgittTilknytningBuilder.build());
        }
    }

    private Språkkode getSpraakValg(Språk spraak) {
        if (spraak != null) {
            return Språkkode.fraKode(spraak.dto.toUpperCase());
        }
        return Språkkode.UDEFINERT;
    }

    private Landkoder finnLandkode(String landKode) {
        return Landkoder.fraKode(landKode);
    }
}
